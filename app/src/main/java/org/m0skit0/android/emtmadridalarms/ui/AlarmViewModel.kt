package org.m0skit0.android.emtmadridalarms.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.data.AlarmStateReader
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.domain.LoadBusLinesUseCase
import org.m0skit0.android.emtmadridalarms.domain.LoadBusStopsUseCase

private const val TAG = "AlarmViewModel"

class AlarmViewModel(
    appContext: Context,
    alarmStateReader: AlarmStateReader,
    saveActiveAlarm: SaveActiveAlarm,
    clearActiveAlarm: ClearActiveAlarm,
    setRinging: SetRinging,
    loadBusLines: LoadBusLinesUseCase,
    loadBusStops: LoadBusStopsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AlarmState())
    val state: StateFlow<AlarmState> = _state.asStateFlow()

    private val loadLines: LineLoader = lineLoader(loadBusLines, _state, viewModelScope)
    private val loadStops: StopLoader = stopLoader(loadBusStops, _state, viewModelScope)
    private val startAlarm: AlarmStarter = alarmStarter(appContext, saveActiveAlarm, _state, viewModelScope)
    private val cancelAlarm: AlarmCanceller = alarmCanceller(appContext, clearActiveAlarm, setRinging, _state, viewModelScope)
    private val stopRinging: RingingStop = ringingStop(appContext, clearActiveAlarm, setRinging, _state, viewModelScope)

    init {
        loadLines()
        viewModelScope.launch {
            alarmStateReader().collect { persisted ->
                _state.update { current ->
                    current.copy(
                        activeAlarm = persisted.activeAlarm,
                        latestEtaSeconds = persisted.latestEtaSeconds,
                        latestDestination = persisted.latestDestination,
                        statusMessage = persisted.statusMessage,
                        isRinging = persisted.isRinging,
                    )
                }
            }
        }
    }

    fun dispatch(intent: AlarmIntent) {
        when (intent) {
            is AlarmIntent.LineChanged -> onLineChanged(intent.value)
            is AlarmIntent.LineSelected -> onLineSelected(intent.value)
            is AlarmIntent.StopChanged -> _state.update { it.copy(stopInput = intent.value, selectedStop = null, errorMessage = null) }
            is AlarmIntent.StopSelected -> onStopSelected(intent.value)
            is AlarmIntent.MinutesChanged -> _state.update { it.copy(minutesInput = intent.value.filter(Char::isDigit), errorMessage = null) }
            AlarmIntent.RefreshLinesClicked -> loadLines()
            AlarmIntent.StartClicked -> onStartClicked()
            AlarmIntent.CancelClicked -> cancelAlarm()
            AlarmIntent.StopRingingClicked -> stopRinging()
            AlarmIntent.ErrorShown -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun onLineChanged(value: String) {
        _state.update {
            it.copy(lineInput = value, stopInput = "", selectedLine = null, selectedStop = null, stops = emptyList(), errorMessage = null)
        }
    }

    private fun onLineSelected(line: BusLine) {
        Log.d(TAG, "Line selected label=${line.label}")
        _state.update {
            it.copy(selectedLine = line, lineInput = line.displayName, selectedStop = null, stopInput = "", stops = emptyList(), isLoadingStops = true, errorMessage = null)
        }
        loadStops(line)
    }

    private fun onStopSelected(stop: BusStop) {
        Log.d(TAG, "Stop selected id=${stop.id} name=${stop.name}")
        _state.update { it.copy(selectedStop = stop, stopInput = stop.displayName, errorMessage = null) }
    }

    private fun onStartClicked() {
        val (request, error) = buildAlarmRequest(_state.value)
        if (error != null) {
            Log.w(TAG, "Cannot start alarm: $error")
            _state.update { it.copy(errorMessage = error) }
            return
        }
        startAlarm(checkNotNull(request))
    }
}

private fun buildAlarmRequest(current: AlarmState): Pair<org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest?, String?> {
    val line = current.selectedLine
    val stop = current.selectedStop
    val error = when {
        line == null -> "Select a bus line from the list."
        stop == null -> "Select a stop for the selected line."
        else -> validateAlarmRequest(line.label, stop.id, current.minutesInput)
    }
    if (error != null) return Pair(null, error)
    return Pair(
        org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest(
            line = line!!.label,
            stopId = stop!!.id,
            targetMinutes = current.minutesInput.toInt(),
        ),
        null,
    )
}

private fun validateAlarmRequest(line: String, stopId: String, minutes: String): String? {
    if (line.isBlank()) return "Enter a bus line."
    if (stopId.isBlank()) return "Enter a stop number."
    if (!stopId.all(Char::isDigit)) return "Stop number must contain only digits."
    val targetMinutes = minutes.toIntOrNull()
    if (targetMinutes == null || targetMinutes <= 0) return "Minutes must be a positive number."
    return null
}
