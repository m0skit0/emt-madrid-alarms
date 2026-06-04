package org.m0skit0.android.emtmadridalarms.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.android.ext.android.inject
import org.m0skit0.android.emtmadridalarms.data.AlarmStateReader
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.RemoveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder

private const val TAG = "BusAlarm"

class AlarmMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clearActiveAlarm: ClearActiveAlarm by inject()
    private val removeActiveAlarm: RemoveActiveAlarm by inject()
    private val setRinging: SetRinging by inject()
    private val pollingMonitor: AlarmPollingMonitor by inject()
    private val monitoringNotification: MonitoringNotificationProvider by inject()
    private val ringingNotification: RingingNotificationProvider by inject()
    private val channelsEnsurer: NotificationChannelsEnsurer by inject()
    private val startSignal: StartSignal by inject()
    private val stopSignal: StopSignal by inject()
    private val globalState: GlobalStateHolder by inject()
    private val alarmStateReader: AlarmStateReader by inject()

    private lateinit var startMonitoring: StartMonitoring
    private lateinit var cancelMonitoring: CancelMonitoring
    private lateinit var cancelSingleMonitoring: CancelSingleMonitoring
    private lateinit var startRinging: StartRinging
    private lateinit var stopRingingAndSelf: StopRingingAndSelf
    private lateinit var cancelJob: CancelJob
    private lateinit var monitorNotificationUpdater: MonitorNotificationUpdater

    override fun onCreate() {
        super.onCreate()
        Timber.d("Service created")
        channelsEnsurer()

        cancelJob = cancelJob(globalState)
        stopRingingAndSelf = stopRingingAndSelf(this, scope, setRinging, stopSignal, globalState)
        cancelMonitoring = cancelMonitoring(this, scope, clearActiveAlarm, setRinging, stopSignal, globalState)
        cancelSingleMonitoring = cancelSingleMonitoring(this, scope, removeActiveAlarm, globalState)
        startRinging = startRinging(this, ringingNotification, startSignal)
        startMonitoring = startMonitoring(this, scope, pollingMonitor, monitoringNotification, startRinging, globalState)
        monitorNotificationUpdater = monitorNotificationUpdater(this, alarmStateReader, monitoringNotification)

        monitorNotificationUpdater(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand action=${intent?.action} startId=$startId flags=$flags")
        when (intent?.action) {
            ACTION_CANCEL -> cancelMonitoring()
            ACTION_CANCEL_ALARM -> cancelSingleMonitoring(intent.alarmRequest())
            ACTION_STOP_RINGING -> stopRingingAndSelf()
            ACTION_START -> startMonitoring(intent.alarmRequest())
            null -> Timber.w("Service restarted without action; no alarm restored yet")
            else -> Timber.w("Unknown service action=${intent.action}")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.d("Service destroyed")
        cancelJob()
        stopSignal()
        scope.cancel()
        super.onDestroy()
    }
}
