package org.m0skit0.android.emtmadridalarms.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.data.AlarmStorage
import org.m0skit0.android.emtmadridalarms.data.EmtRepository
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.domain.validateAlarmRequest
import org.m0skit0.android.emtmadridalarms.service.AlarmMonitorService

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = AlarmStorage(application.applicationContext)
    private val repository = EmtRepository()
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
            _state.update { it.copy(isLoadingLines = true, errorMessage = null) }
            runCatching { repository.lines() }
                .onSuccess { lines ->
                    _state.update { it.copy(lines = lines, isLoadingLines = false) }
                }
                .onFailure { error ->
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
            runCatching { repository.stopsForLine(line) }
                .onSuccess { stops ->
                    _state.update { it.copy(stops = stops, isLoadingStops = false) }
                }
                .onFailure { error ->
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
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        val request = BusAlarmRequest(
            line = selectedLine!!.label,
            stopId = selectedStop!!.id,
            targetMinutes = current.minutesInput.toInt(),
        )

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            storage.saveActiveAlarm(request)
            AlarmMonitorService.start(getApplication(), request)
            _state.update { it.copy(isLoading = false, activeAlarm = request) }
        }
    }

    private fun cancelAlarm() {
        viewModelScope.launch {
            AlarmMonitorService.cancel(getApplication())
            storage.clearActiveAlarm()
            storage.setRinging(false)
            _state.update { it.copy(activeAlarm = null, latestEtaSeconds = null, latestDestination = "", statusMessage = "") }
        }
    }

    private fun stopRinging() {
        viewModelScope.launch {
            AlarmMonitorService.stopRinging(getApplication())
            storage.setRinging(false)
            storage.clearActiveAlarm()
            _state.update { it.copy(isRinging = false, activeAlarm = null) }
        }
    }
}
