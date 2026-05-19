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
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.validateAlarmRequest
import org.m0skit0.android.emtmadridalarms.service.AlarmMonitorService

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = AlarmStorage(application.applicationContext)
    private val _state = MutableStateFlow(AlarmState())
    val state: StateFlow<AlarmState> = _state.asStateFlow()

    init {
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
            is AlarmIntent.LineChanged -> _state.update { it.copy(lineInput = intent.value, errorMessage = null) }
            is AlarmIntent.StopChanged -> _state.update { it.copy(stopInput = intent.value, errorMessage = null) }
            is AlarmIntent.MinutesChanged -> _state.update { it.copy(minutesInput = intent.value.filter(Char::isDigit), errorMessage = null) }
            AlarmIntent.StartClicked -> startAlarm()
            AlarmIntent.CancelClicked -> cancelAlarm()
            AlarmIntent.StopRingingClicked -> stopRinging()
            AlarmIntent.ErrorShown -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun startAlarm() {
        val current = _state.value
        val validationError = validateAlarmRequest(current.lineInput, current.stopInput, current.minutesInput)
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        val request = BusAlarmRequest(
            line = current.lineInput.trim(),
            stopId = current.stopInput.trim(),
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
