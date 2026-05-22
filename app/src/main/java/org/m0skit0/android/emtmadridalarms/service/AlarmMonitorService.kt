package org.m0skit0.android.emtmadridalarms.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.android.ext.android.inject
import org.m0skit0.android.emtmadridalarms.domain.AlarmStateStore
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder

private const val TAG = "BusAlarm"

class AlarmMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storage: AlarmStateStore by inject()
    private val pollingMonitor: AlarmPollingMonitor by inject()
    private val monitoringNotification: MonitoringNotificationProvider by inject()
    private val ringingNotification: RingingNotificationProvider by inject()
    private val channelsEnsurer: NotificationChannelsEnsurer by inject()
    private val signalPlayer: AlarmSignalPlayer by inject()
    private val globalState: GlobalStateHolder by inject()

    private lateinit var startMonitoring: StartMonitoring
    private lateinit var cancelMonitoring: CancelMonitoring
    private lateinit var startRinging: StartRinging
    private lateinit var stopRingingAndSelf: StopRingingAndSelf
    private lateinit var stopSignal: StopSignal
    private lateinit var cancelJob: CancelJob

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        channelsEnsurer()

        // Functions with no internal dependencies
        stopSignal = stopSignalImpl(signalPlayer)
        cancelJob = cancelJobImpl(globalState)
        stopRingingAndSelf = stopRingingAndSelfImpl(this, scope, storage, signalPlayer)
        cancelMonitoring = cancelMonitoringImpl(this, scope, storage, signalPlayer, globalState)

        // Functions with internal dependencies
        startRinging = startRingingImpl(this, ringingNotification, signalPlayer)
        startMonitoring = startMonitoringImpl(this, scope, pollingMonitor, monitoringNotification, startRinging, globalState)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action} startId=$startId flags=$flags")
        when (intent?.action) {
            ACTION_CANCEL -> cancelMonitoring()
            ACTION_STOP_RINGING -> stopRingingAndSelf()
            ACTION_START -> startMonitoring(intent.alarmRequest())
            null -> Log.w(TAG, "Service restarted without action; no alarm restored yet")
            else -> Log.w(TAG, "Unknown service action=${intent.action}")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        cancelJob()
        stopSignal()
        scope.cancel()
        super.onDestroy()
    }
}
