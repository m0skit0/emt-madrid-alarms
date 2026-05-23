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
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival
import org.m0skit0.android.emtmadridalarms.data.SaveStatus
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.domain.LoadBusArrivalsUseCase
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder

private const val TAG = "BusAlarm"

class AlarmMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveActiveAlarm: SaveActiveAlarm by inject()
    private val clearActiveAlarm: ClearActiveAlarm by inject()
    private val saveLatestArrival: SaveLatestArrival by inject()
    private val saveStatus: SaveStatus by inject()
    private val setRinging: SetRinging by inject()
    private val loadBusArrivals: LoadBusArrivalsUseCase by inject()
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

        val pollingMonitor = AlarmPollingMonitor { request, onTriggered ->
            pollAlarm(request, onTriggered, loadBusArrivals, saveActiveAlarm, saveStatus, saveLatestArrival, setRinging, clearActiveAlarm)
        }

        stopSignal = stopSignal(signalPlayer)
        cancelJob = cancelJob(globalState)
        stopRingingAndSelf = stopRingingAndSelf(this, scope, setRinging, clearActiveAlarm, signalPlayer)
        cancelMonitoring = cancelMonitoring(this, scope, clearActiveAlarm, setRinging, signalPlayer, globalState)

        startRinging = startRinging(this, ringingNotification, signalPlayer)
        startMonitoring = startMonitoring(this, scope, pollingMonitor, monitoringNotification, startRinging, globalState)
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
