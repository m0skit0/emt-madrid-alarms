package org.m0skit0.android.emtmadridalarms.ui

import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest

data class AlarmState(
    val lineInput: String = "",
    val stopInput: String = "",
    val minutesInput: String = "10",
    val isLoading: Boolean = false,
    val activeAlarm: BusAlarmRequest? = null,
    val latestEtaSeconds: Int? = null,
    val latestDestination: String = "",
    val statusMessage: String = "",
    val errorMessage: String? = null,
    val isRinging: Boolean = false,
)

sealed interface AlarmIntent {
    data class LineChanged(val value: String) : AlarmIntent
    data class StopChanged(val value: String) : AlarmIntent
    data class MinutesChanged(val value: String) : AlarmIntent
    data object StartClicked : AlarmIntent
    data object CancelClicked : AlarmIntent
    data object StopRingingClicked : AlarmIntent
    data object ErrorShown : AlarmIntent
}
