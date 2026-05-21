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
import org.m0skit0.android.emtmadridalarms.domain.AlarmStateStore
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.domain.LoadBusLinesUseCase
import org.m0skit0.android.emtmadridalarms.domain.LoadBusStopsUseCase
import org.m0skit0.android.emtmadridalarms.domain.validateAlarmRequest
import org.m0skit0.android.emtmadridalarms.service.AlarmMonitorService

class AlarmViewModel(
    private val appContext: Context,
    private val storage: AlarmStateStore,
    private val loadBusLines: LoadBusLinesUseCase,
    private val loadBusStops: LoadBusStopsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AlarmState())
    val state: StateFlow<AlarmState> = _state.asStateFlow()

    init {
        loadLines()
        viewModelScope.launch {
            storage.state.collect { persisted ->
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
            is AlarmIntent.LineChanged -> _state.update {
                it.copy(
                    lineInput = intent.value,
                    stopInput = "",
                    selectedLine = null,
                    selectedStop = null,
                    stops = emptyList(),
                    errorMessage = null,
                )
            }
            is AlarmIntent.LineSelected -> selectLine(intent.value)
            is AlarmIntent.StopChanged -> _state.update { it.copy(stopInput = intent.value, selectedStop = null, errorMessage = null) }
            is AlarmIntent.StopSelected -> selectStop(intent.value)
            is AlarmIntent.MinutesChanged -> _state.update { it.copy(minutesInput = intent.value.filter(Char::isDigit), errorMessage = null) }
            AlarmIntent.RefreshLinesClicked -> loadLines()
            AlarmIntent.StartClicked -> startAlarm()
            AlarmIntent.CancelClicked -> cancelAlarm()
            AlarmIntent.StopRingingClicked -> stopRinging()
            AlarmIntent.ErrorShown -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadLines() {
        viewModelScope.launch {
            Log.d(TAG, "Loading bus lines")
            _state.update { it.copy(isLoadingLines = true, errorMessage = null) }
            runCatching { loadBusLines() }
                .onSuccess { lines ->
                    Log.d(TAG, "Loaded bus lines count=${lines.size}")
                    _state.update { it.copy(lines = lines, isLoadingLines = false) }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load bus lines: ${error.message}", error)
                    _state.update {
                        it.copy(
                            isLoadingLines = false,
                            errorMessage = error.message ?: "Could not load EMT bus lines.",
                        )
                    }
                }
        }
    }

    private fun selectLine(line: BusLine) {
        Log.d(TAG, "Selected line label=${line.label} id=${line.id}")
        _state.update {
            it.copy(
                selectedLine = line,
                lineInput = line.displayName,
                selectedStop = null,
                stopInput = "",
                stops = emptyList(),
                isLoadingStops = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            runCatching { loadBusStops(line) }
                .onSuccess { stops ->
                    Log.d(TAG, "Loaded stops for line=${line.label} count=${stops.size}")
                    _state.update { it.copy(stops = stops, isLoadingStops = false) }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load stops for line=${line.label}: ${error.message}", error)
                    _state.update {
                        it.copy(
                            isLoadingStops = false,
                            errorMessage = error.message ?: "Could not load stops for line ${line.label}.",
                        )
                    }
                }
        }
    }

    private fun selectStop(stop: BusStop) {
        Log.d(TAG, "Selected stop id=${stop.id} name=${stop.name}")
        _state.update {
            it.copy(selectedStop = stop, stopInput = stop.displayName, errorMessage = null)
        }
    }

    private fun startAlarm() {
        val current = _state.value
        val selectedLine = current.selectedLine
        val selectedStop = current.selectedStop
        val validationError = when {
            selectedLine == null -> "Select a bus line from the list."
            selectedStop == null -> "Select a stop for the selected line."
            else -> validateAlarmRequest(selectedLine.label, selectedStop.id, current.minutesInput)
        }
        if (validationError != null) {
            Log.w(TAG, "Cannot start alarm: $validationError")
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        val request = BusAlarmRequest(
            line = selectedLine!!.label,
            stopId = selectedStop!!.id,
            targetMinutes = current.minutesInput.toInt(),
        )

        viewModelScope.launch {
            Log.d(TAG, "Starting alarm request line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            storage.saveActiveAlarm(request)
            AlarmMonitorService.start(appContext, request)
            _state.update { it.copy(isLoading = false, activeAlarm = request) }
            Log.d(TAG, "Alarm start requested")
        }
    }

    private fun cancelAlarm() {
        viewModelScope.launch {
            Log.d(TAG, "Cancelling alarm from UI")
            AlarmMonitorService.cancel(appContext)
            storage.clearActiveAlarm()
            storage.setRinging(false)
            _state.update { it.copy(activeAlarm = null, latestEtaSeconds = null, latestDestination = "", statusMessage = "") }
        }
    }

    private fun stopRinging() {
        viewModelScope.launch {
            Log.d(TAG, "Stopping ringing from UI")
            AlarmMonitorService.stopRinging(appContext)
            storage.setRinging(false)
            storage.clearActiveAlarm()
            _state.update { it.copy(isRinging = false, activeAlarm = null) }
        }
    }

    private companion object {
        const val TAG = "AlarmViewModel"
    }
}
