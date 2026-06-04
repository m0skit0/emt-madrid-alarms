package org.m0skit0.android.emtmadridalarms.service

import android.app.Service
import timber.log.Timber
import android.app.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.data.AlarmStateReader
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.RemoveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder
import org.m0skit0.android.emtmadridalarms.utils.orDefault

private const val TAG = "BusAlarm"
internal const val NOTIFICATION_ID = 1001

/**
 * Data class representing the state for the alarm monitoring job.
 */
data class AlarmMonitorState(
    val monitorJobs: Map<BusAlarmRequest, Job> = emptyMap()
)

fun interface StartMonitoring : (BusAlarmRequest?) -> Unit
fun interface CancelMonitoring : () -> Unit
fun interface CancelSingleMonitoring : (BusAlarmRequest?) -> Unit
fun interface StartRinging : (BusAlarmRequest) -> Unit
fun interface StopRingingAndSelf : () -> Unit
fun interface StopSignal : () -> Unit
fun interface CancelJob : () -> Unit
fun interface MonitorNotificationUpdater : (CoroutineScope) -> Unit

internal fun startMonitoring(
    service: Service,
    scope: CoroutineScope,
    pollingMonitor: AlarmPollingMonitor,
    monitoringNotification: MonitoringNotificationProvider,
    startRinging: StartRinging,
    globalState: GlobalStateHolder
): StartMonitoring = StartMonitoring { request ->
    if (request == null) {
        Timber.w("Cannot start monitoring: invalid or missing alarm request")
        service.stopSelf()
        return@StartMonitoring
    }
    Timber.d("Starting monitoring line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
    if (globalState.state.alarmMonitor.monitorJobs.containsKey(request)) {
        Timber.d("Monitoring already active for line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
        return@StartMonitoring
    }
    service.startForeground(
        NOTIFICATION_ID,
        monitoringNotification(
            (globalState.state.alarmMonitor.monitorJobs.keys + request).distinct(),
            "Waiting for EMT arrivals...",
            service.servicePendingIntent(ACTION_CANCEL, 2)
        ),
    )
    Timber.d("Foreground monitoring notification started")
    launchMonitorJob(scope, globalState, pollingMonitor, request, startRinging)
}

private fun launchMonitorJob(
    scope: CoroutineScope,
    globalState: GlobalStateHolder,
    pollingMonitor: AlarmPollingMonitor,
    request: BusAlarmRequest,
    startRinging: StartRinging,
) {
    val job = scope.launch(start = CoroutineStart.LAZY) {
        try {
            pollingMonitor(request) {
                Dispatchers.Main { startRinging(request) }
            }
        } finally {
            globalState.update { appState ->
                appState.copy(
                    alarmMonitor = appState.alarmMonitor.copy(
                        monitorJobs = appState.alarmMonitor.monitorJobs - request
                    )
                )
            }
        }
    }
    globalState.update { appState ->
        appState.copy(
            alarmMonitor = appState.alarmMonitor.copy(
                monitorJobs = appState.alarmMonitor.monitorJobs + (request to job)
            )
        )
    }
    job.start()
}

internal fun startRinging(
    service: Service,
    ringingNotification: RingingNotificationProvider,
    startSignal: StartSignal
): StartRinging = StartRinging { request ->
    service.startForeground(
        NOTIFICATION_ID,
        ringingNotification(request, service.servicePendingIntent(ACTION_STOP_RINGING, 3))
    )
    startSignal(request)
}

internal fun stopRingingAndSelf(
    service: Service,
    scope: CoroutineScope,
    setRinging: SetRinging,
    stopSignal: StopSignal,
    globalState: GlobalStateHolder,
): StopRingingAndSelf = StopRingingAndSelf {
    Timber.d("Stopping ringing")
    scope.launch {
        setRinging(false)
    }
    stopSignal()
    if (globalState.state.alarmMonitor.monitorJobs.isEmpty()) {
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        service.stopSelf()
    }
}

internal fun cancelMonitoring(
    service: Service,
    scope: CoroutineScope,
    clearActiveAlarm: ClearActiveAlarm,
    setRinging: SetRinging,
    stopSignal: StopSignal,
    globalState: GlobalStateHolder
): CancelMonitoring = CancelMonitoring {
    Timber.d("Cancelling monitoring")
    globalState.state.alarmMonitor.monitorJobs.values.forEach { it.cancel() }
    globalState.update { appState ->
        appState.copy(alarmMonitor = appState.alarmMonitor.copy(monitorJobs = emptyMap()))
    }
    scope.launch {
        clearActiveAlarm()
        setRinging(false)
    }
    stopSignal()
    service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    service.stopSelf()
}

internal fun cancelSingleMonitoring(
    service: Service,
    scope: CoroutineScope,
    removeActiveAlarm: RemoveActiveAlarm,
    globalState: GlobalStateHolder,
): CancelSingleMonitoring = CancelSingleMonitoring { request ->
    if (request == null) {
        Timber.w("Cannot cancel monitoring: invalid or missing alarm request")
        return@CancelSingleMonitoring
    }
    Timber.d("Cancelling monitoring line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
    globalState.state.alarmMonitor.monitorJobs[request]?.cancel()
    globalState.update { appState ->
        appState.copy(
            alarmMonitor = appState.alarmMonitor.copy(
                monitorJobs = appState.alarmMonitor.monitorJobs - request
            )
        )
    }
    scope.launch { removeActiveAlarm(request) }
    if (globalState.state.alarmMonitor.monitorJobs.isEmpty()) {
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        service.stopSelf()
    }
}

internal fun cancelJob(globalState: GlobalStateHolder): CancelJob = CancelJob {
    globalState.state.alarmMonitor.monitorJobs.values.forEach { it.cancel() }
}

internal fun monitorNotificationUpdater(
    service: Service,
    alarmStateReader: AlarmStateReader,
    monitoringNotification: MonitoringNotificationProvider,
): MonitorNotificationUpdater = MonitorNotificationUpdater { scope ->
    scope.launch {
        alarmStateReader().collect { state ->
            if (state.activeAlarms.isEmpty()) return@collect
            if (state.isRinging) return@collect
            val text = state.latestEtaSeconds
                ?.let { "Next bus: ${it.floorDiv(60)} min" }
                .orDefault { state.statusMessage.ifBlank { "Waiting for EMT arrivals..." } }
            val notification = monitoringNotification(
                state.activeAlarms,
                text,
                service.servicePendingIntent(ACTION_CANCEL, 2)
            )
            service
                .getSystemService(NotificationManager::class.java)
                ?.notify(NOTIFICATION_ID, notification)
        }
    }
}
