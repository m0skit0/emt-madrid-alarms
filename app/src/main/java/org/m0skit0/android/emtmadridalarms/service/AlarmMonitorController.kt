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
import org.m0skit0.android.emtmadridalarms.domain.LoadBusArrivalsUseCase

private const val TAG = "BusAlarm"
private const val NOTIFICATION_ID = 1001

class AlarmMonitorController(
    private val service: Service,
    private val scope: CoroutineScope,
    private val loadBusArrivals: LoadBusArrivalsUseCase,
    private val storage: AlarmStateStore,
    private val monitoringNotification: MonitoringNotificationProvider,
    private val ringingNotification: RingingNotificationProvider,
    private val signalPlayer: AlarmSignalPlayer,
) {
    private var monitorJob: Job? = null

    fun startMonitoring(request: BusAlarmRequest?) {
        if (request == null) {
            Log.w(TAG, "Cannot start monitoring: invalid or missing alarm request")
            service.stopSelf()
            return
        }
        Log.d(TAG, "Starting monitoring line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
        monitorJob?.cancel()
        service.startForeground(
            NOTIFICATION_ID,
            monitoringNotification(request, "Waiting for EMT arrivals...", service.servicePendingIntent(ACTION_CANCEL, 2)),
        )
        Log.d(TAG, "Foreground monitoring notification started")
        monitorJob = scope.launch {
            AlarmPollingMonitor(loadBusArrivals, storage).monitor(request) {
                withContext(Dispatchers.Main) { startRinging(request) }
            }
        }
    }

    fun cancelMonitoring() {
        Log.d(TAG, "Cancelling monitoring")
        monitorJob?.cancel()
        monitorJob = null
        scope.launch {
            storage.clearActiveAlarm()
            storage.setRinging(false)
        }
        signalPlayer.stop()
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        service.stopSelf()
    }

    fun startRinging(request: BusAlarmRequest) {
        service.startForeground(NOTIFICATION_ID, ringingNotification(request, service.servicePendingIntent(ACTION_STOP_RINGING, 3)))
        signalPlayer.start(request)
    }

    fun stopRingingAndSelf() {
        Log.d(TAG, "Stopping ringing and service")
        scope.launch {
            storage.setRinging(false)
            storage.clearActiveAlarm()
        }
        signalPlayer.stop()
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        service.stopSelf()
    }

    fun stopSignal() {
        signalPlayer.stop()
    }

    fun cancelJob() {
        monitorJob?.cancel()
    }
}
