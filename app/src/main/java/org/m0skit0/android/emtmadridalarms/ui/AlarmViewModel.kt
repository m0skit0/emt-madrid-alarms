package org.m0skit0.android.emtmadridalarms.ui

import timber.log.Timber
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.data.AlarmStateReader
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.CancelClicked
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.ErrorShown
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.LineChanged
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.LineSelected
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.MinutesChanged
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.RefreshLinesClicked
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.StartClicked
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.StopChanged
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.StopRingingClicked
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.StopSelected

private const val TAG = "AlarmViewModel"

class AlarmViewModel(
    private val _state: MutableStateFlow<AlarmState>,
    private val scope: CoroutineScope,
    private val alarmStateReader: AlarmStateReader,
    private val loadLines: LineLoader,
    private val loadStops: StopLoader,
    private val buildRequest: AlarmRequestBuilder,
    private val startAlarm: AlarmStarter,
    private val cancelAlarm: AlarmCanceller,
    private val stopRinging: RingingStop,
) : ViewModel() {
    val state: StateFlow<AlarmState> = _state.asStateFlow()

    init {
        loadLines()
        scope.launch {
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

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }

    fun dispatch(intent: AlarmIntent) {
        when (intent) {
            is LineChanged -> onLineChanged(intent.value)
            is LineSelected -> onLineSelected(intent.value)
            is StopChanged -> _state.update {
                it.copy(
                    stopInput = intent.value,
                    selectedStop = null,
                    errorMessage = null
                )
            }

            is StopSelected -> onStopSelected(intent.value)
            is MinutesChanged -> _state.update {
                it.copy(
                    minutesInput = intent.value.filter(Char::isDigit),
                    errorMessage = null
                )
            }

            RefreshLinesClicked -> loadLines()
            StartClicked -> onStartClicked()
            CancelClicked -> cancelAlarm()
            StopRingingClicked -> stopRinging()
            ErrorShown -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun onLineChanged(value: String) {
        _state.update {
            it.copy(
                lineInput = value,
                stopInput = "",
                selectedLine = null,
                selectedStop = null,
                stops = emptyList(),
                errorMessage = null
            )
        }
    }

    private fun onLineSelected(line: BusLine) {
        Timber.d("Line selected label=${line.label}")
        _state.update {
            it.copy(
                selectedLine = line,
                lineInput = line.displayName,
                selectedStop = null,
                stopInput = "",
                stops = emptyList(),
                isLoadingStops = true,
                errorMessage = null
            )
        }
        loadStops(line)
    }

    private fun onStopSelected(stop: BusStop) {
        Timber.d("Stop selected id=${stop.id} name=${stop.name}")
        _state.update {
            it.copy(
                selectedStop = stop,
                stopInput = stop.displayName,
                errorMessage = null
            )
        }
    }

    private fun onStartClicked() {
        buildRequest(_state.value)
            .onSuccess { startAlarm(it) }
            .onFailure { error ->
                Timber.w("Cannot start alarm: ${error.message}")
                _state.update { it.copy(errorMessage = error.message) }
            }
    }
}
