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
import org.m0skit0.android.emtmadridalarms.domain.MAX_ACTIVE_ALARMS
import org.m0skit0.android.emtmadridalarms.domain.hasSameLineAndStop
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.CancelAlarmClicked
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.CancelClicked
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.ToggleAlarmEnabled
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.ErrorShown
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.LineChanged
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.LineSelected
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.MinutesChanged
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.RefreshLinesClicked
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.StartClicked
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.StopChanged
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.StopPickerOpened
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.StopRingingClicked
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent.StopSelected

private const val TAG = "AlarmViewModel"

class AlarmViewModel(
    private val _state: MutableStateFlow<AlarmState>,
    private val scope: CoroutineScope,
    private val alarmStateReader: AlarmStateReader,
    private val loadLines: LineLoader,
    private val loadAllStops: AllStopLoader,
    private val loadStops: StopLoader,
    private val buildRequest: AlarmRequestBuilder,
    private val startAlarm: AlarmStarter,
    private val cancelSingleAlarm: SingleAlarmCanceller,
    private val enableAlarm: AlarmEnabler,
    private val cancelAlarm: AlarmCanceller,
    private val stopRinging: RingingStop,
    private val maxActiveAlarmsMessage: (Int) -> String = { "You can have up to $it active alarms." },
    private val duplicateAlarmMessage: () -> String = { "An alarm for this line and stop is already scheduled." },
) : ViewModel() {
    val state: StateFlow<AlarmState> = _state.asStateFlow()

    init {
        loadLines()
        scope.launch {
            alarmStateReader().collect { persisted ->
                _state.update { current ->
                    current.copy(
                        activeAlarm = persisted.activeAlarm,
                        activeAlarms = persisted.activeAlarms,
                        latestEtaSeconds = persisted.latestEtaSeconds,
                        latestDestination = persisted.latestDestination,
                        statusMessage = persisted.statusMessage,
                        isRinging = persisted.isRinging,
                        ringingAlarm = persisted.ringingAlarm,
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
                    lines = if (it.selectedLine == null) it.allLines else it.lines,
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
            StopPickerOpened -> onStopPickerOpened()
            StartClicked -> onStartClicked()
            is CancelAlarmClicked -> cancelSingleAlarm(intent.value)
            is ToggleAlarmEnabled -> enableAlarm(intent.alarm, intent.enabled)
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
                lines = it.allLines,
                selectedLine = null,
                selectedStop = null,
                stops = emptyList(),
                errorMessage = null
            )
        }
    }

    private fun onLineSelected(line: BusLine) {
        Timber.d("Line selected label=${line.label}")
        val current = _state.value
        val selectedStop = current.selectedStop?.takeIf { it.lineLabels.isEmpty() || stopServesLine(it, line) }
        _state.update {
            it.copy(
                selectedLine = line,
                lineInput = line.displayName,
                selectedStop = selectedStop,
                stopInput = selectedStop?.displayName.orEmpty(),
                stops = emptyList(),
                isLoadingStops = true,
                errorMessage = null
            )
        }
        loadStops(line)
    }

    private fun onStopSelected(stop: BusStop) {
        Timber.d("Stop selected id=${stop.id} name=${stop.name}")
        val current = _state.value
        val selectedLine = current.selectedLine?.takeIf { stop.lineLabels.isEmpty() || stopServesLine(stop, it) }
        val candidateLines = current.allLines.ifEmpty { current.lines }
        val relatedLines = linesForStop(candidateLines, stop).ifEmpty { candidateLines }
        _state.update {
            it.copy(
                selectedStop = stop,
                stopInput = stop.displayName,
                selectedLine = selectedLine,
                lineInput = selectedLine?.displayName.orEmpty(),
                lines = relatedLines,
                errorMessage = null
            )
        }
    }

    private fun onStopPickerOpened() {
        val current = _state.value
        when {
            current.selectedLine == null && current.allStops.isNotEmpty() -> {
                _state.update { it.copy(stops = it.allStops, errorMessage = null) }
            }

            current.selectedLine == null && !current.isLoadingStops -> loadAllStops()
        }
    }

    private fun onStartClicked() {
        if (_state.value.activeAlarms.size >= MAX_ACTIVE_ALARMS) {
            _state.update { it.copy(errorMessage = maxActiveAlarmsMessage(MAX_ACTIVE_ALARMS)) }
            return
        }
        buildRequest(_state.value)
            .onSuccess { request ->
                if (_state.value.activeAlarms.any { it.hasSameLineAndStop(request) }) {
                    _state.update { it.copy(errorMessage = duplicateAlarmMessage()) }
                    return@onSuccess
                }
                startAlarm(request)
            }
            .onFailure { error ->
                Timber.w("Cannot start alarm: ${error.message}")
                _state.update { it.copy(errorMessage = error.message) }
            }
    }
}
