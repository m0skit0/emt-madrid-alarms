package org.m0skit0.android.emtmadridalarms.service

import android.app.Service
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.m0skit0.android.emtmadridalarms.domain.AlarmStateStore
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder

private const val TAG = "BusAlarm"
private const val NOTIFICATION_ID = 1001

/**
 * Data class representing the state for the alarm monitoring job.
 */
data class AlarmMonitorState(
    val monitorJob: Job? = null
)

fun interface StartMonitoring : (BusAlarmRequest?) -> Unit
fun interface CancelMonitoring : () -> Unit
fun interface StartRinging : (BusAlarmRequest) -> Unit
fun interface StopRingingAndSelf : () -> Unit
fun interface StopSignal : () -> Unit
fun interface CancelJob : () -> Unit

internal fun startMonitoringImpl(
    service: Service,
    scope: CoroutineScope,
    pollingMonitor: AlarmPollingMonitor,
    monitoringNotification: MonitoringNotificationProvider,
    startRinging: StartRinging,
    globalState: GlobalStateHolder
): StartMonitoring = StartMonitoring { request ->
    if (request == null) {
        Log.w(TAG, "Cannot start monitoring: invalid or missing alarm request")
        service.stopSelf()
        return@StartMonitoring
    }
    Log.d(TAG, "Starting monitoring line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
    globalState.state.alarmMonitor.monitorJob?.cancel()
    service.startForeground(
        NOTIFICATION_ID,
        monitoringNotification(request, "Waiting for EMT arrivals...", service.servicePendingIntent(ACTION_CANCEL, 2)),
    )
    Log.d(TAG, "Foreground monitoring notification started")
    globalState.update { appState ->
        appState.copy(
            alarmMonitor = appState.alarmMonitor.copy(
                monitorJob = scope.launch {
                    pollingMonitor(request) {
                        withContext(Dispatchers.Main) { startRinging(request) }
                    }
                }
            )
        )
    }
}

internal fun cancelMonitoringImpl(
    service: Service,
    scope: CoroutineScope,
    storage: AlarmStateStore,
    signalPlayer: AlarmSignalPlayer,
    globalState: GlobalStateHolder
): CancelMonitoring = CancelMonitoring {
    Log.d(TAG, "Cancelling monitoring")
    globalState.state.alarmMonitor.monitorJob?.cancel()
    globalState.update { appState ->
        appState.copy(alarmMonitor = appState.alarmMonitor.copy(monitorJob = null))
    }
    scope.launch {
        storage.clearActiveAlarm()
        storage.setRinging(false)
    }
    signalPlayer.stop()
    service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    service.stopSelf()
}

internal fun startRingingImpl(
    service: Service,
    ringingNotification: RingingNotificationProvider,
    signalPlayer: AlarmSignalPlayer
): StartRinging = StartRinging { request ->
    service.startForeground(NOTIFICATION_ID, ringingNotification(request, service.servicePendingIntent(ACTION_STOP_RINGING, 3)))
    signalPlayer.start(request)
}

internal fun stopRingingAndSelfImpl(
    service: Service,
    scope: CoroutineScope,
    storage: AlarmStateStore,
    signalPlayer: AlarmSignalPlayer
): StopRingingAndSelf = StopRingingAndSelf {
    Log.d(TAG, "Stopping ringing and service")
    scope.launch {
        storage.setRinging(false)
        storage.clearActiveAlarm()
    }
    signalPlayer.stop()
    service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    service.stopSelf()
}

internal fun stopSignalImpl(signalPlayer: AlarmSignalPlayer): StopSignal = StopSignal {
    signalPlayer.stop()
}

internal fun cancelJobImpl(globalState: GlobalStateHolder): CancelJob = CancelJob {
    globalState.state.alarmMonitor.monitorJob?.cancel()
}
