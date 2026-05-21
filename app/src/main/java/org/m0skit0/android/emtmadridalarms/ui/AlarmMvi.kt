package org.m0skit0.android.emtmadridalarms.ui

import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

data class AlarmState(
    val lineInput: String = "",
    val stopInput: String = "",
    val minutesInput: String = "10",
    val isLoading: Boolean = false,
    val isLoadingLines: Boolean = false,
    val isLoadingStops: Boolean = false,
    val lines: List<BusLine> = emptyList(),
    val stops: List<BusStop> = emptyList(),
    val selectedLine: BusLine? = null,
    val selectedStop: BusStop? = null,
    val activeAlarm: BusAlarmRequest? = null,
    val latestEtaSeconds: Int? = null,
    val latestDestination: String = "",
    val statusMessage: String = "",
    val errorMessage: String? = null,
    val isRinging: Boolean = false,
)

sealed interface AlarmIntent {
    data class LineChanged(val value: String) : AlarmIntent
    data class LineSelected(val value: BusLine) : AlarmIntent
    data class StopChanged(val value: String) : AlarmIntent
    data class StopSelected(val value: BusStop) : AlarmIntent
    data class MinutesChanged(val value: String) : AlarmIntent
    data object RefreshLinesClicked : AlarmIntent
    data object StartClicked : AlarmIntent
    data object CancelClicked : AlarmIntent
    data object StopRingingClicked : AlarmIntent
    data object ErrorShown : AlarmIntent
}
