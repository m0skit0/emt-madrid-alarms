package org.m0skit0.android.emtmadridalarms.state

import org.m0skit0.android.emtmadridalarms.data.EmtAuthTokenState
import org.m0skit0.android.emtmadridalarms.service.AlarmMonitorState

/**
 * The root data class for the entire application state.
 * It is composed of smaller, domain-specific state objects.
 */
data class AppState(
    val emtAuthToken: EmtAuthTokenState = EmtAuthTokenState(),
    val alarmMonitor: AlarmMonitorState = AlarmMonitorState()
)
