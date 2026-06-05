package org.m0skit0.android.emtmadridalarms.ui

import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.utils.orDefault

data class AlarmState(
    val lineInput: String = "",
    val stopInput: String = "",
    val minutesInput: String = "10",
    val isLoading: Boolean = false,
    val isLoadingLines: Boolean = false,
    val isLoadingStops: Boolean = false,
    val allLines: List<BusLine> = emptyList(),
    val allStops: List<BusStop> = emptyList(),
    val lines: List<BusLine> = emptyList(),
    val stops: List<BusStop> = emptyList(),
    val selectedLine: BusLine? = null,
    val selectedStop: BusStop? = null,
    val activeAlarm: BusAlarmRequest? = null,
    val activeAlarms: List<BusAlarmRequest> = activeAlarm?.let { listOf(it) }.orDefault { emptyList() },
    val latestEtaSeconds: Int? = null,
    val latestDestination: String = "",
    val statusMessage: String = "",
    val errorMessage: String? = null,
    val isRinging: Boolean = false,
    val ringingAlarm: BusAlarmRequest? = null,
)

sealed interface AlarmIntent {
    data class LineChanged(val value: String) : AlarmIntent
    data class LineSelected(val value: BusLine) : AlarmIntent
    data class StopChanged(val value: String) : AlarmIntent
    data class StopSelected(val value: BusStop) : AlarmIntent
    data class MinutesChanged(val value: String) : AlarmIntent
    data object RefreshLinesClicked : AlarmIntent
    data object StopPickerOpened : AlarmIntent
    data object StartClicked : AlarmIntent
    data class CancelAlarmClicked(val value: BusAlarmRequest) : AlarmIntent
    data class ToggleAlarmEnabled(val alarm: BusAlarmRequest, val enabled: Boolean) : AlarmIntent
    data object CancelClicked : AlarmIntent
    data object StopRingingClicked : AlarmIntent
    data object ErrorShown : AlarmIntent
}
