package org.m0skit0.android.emtmadridalarms.service

import android.app.Service
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.m0skit0.android.emtmadridalarms.domain.AlarmStateStore
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.LoadBusArrivalsUseCase

class AlarmMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadBusArrivals: LoadBusArrivalsUseCase by inject()
    private val storage: AlarmStateStore by inject()
    private var monitorJob: Job? = null
    private lateinit var notifications: AlarmNotificationFactory
    private lateinit var signalPlayer: AlarmSignalPlayer

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        notifications = AlarmNotificationFactory(this)
        signalPlayer = AlarmSignalPlayer(applicationContext)
        notifications.ensureChannels()
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
        monitorJob?.cancel()
        signalPlayer.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring(request: BusAlarmRequest?) {
        if (request == null) {
            Log.w(TAG, "Cannot start monitoring: invalid or missing alarm request")
            stopSelf()
            return
        }

        Log.d(TAG, "Starting monitoring line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
        monitorJob?.cancel()
        startForeground(
            NOTIFICATION_ID,
            notifications.monitoringNotification(request, "Waiting for EMT arrivals...", servicePendingIntent(ACTION_CANCEL, 2)),
        )
        Log.d(TAG, "Foreground monitoring notification started")
        monitorJob = scope.launch {
            AlarmPollingMonitor(loadBusArrivals, storage).monitor(request) {
                withContext(Dispatchers.Main) { startRinging(request) }
            }
        }
    }

    private fun cancelMonitoring() {
        Log.d(TAG, "Cancelling monitoring")
        monitorJob?.cancel()
        monitorJob = null
        scope.launch {
            storage.clearActiveAlarm()
            storage.setRinging(false)
        }
        signalPlayer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startRinging(request: BusAlarmRequest) {
        startForeground(NOTIFICATION_ID, notifications.ringingNotification(request, servicePendingIntent(ACTION_STOP_RINGING, 3)))
        signalPlayer.start(request)
    }

    private fun stopRingingAndSelf() {
        Log.d(TAG, "Stopping ringing and service")
        scope.launch {
            storage.setRinging(false)
            storage.clearActiveAlarm()
        }
        signalPlayer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AlarmMonitorService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun Intent.alarmRequest(): BusAlarmRequest? {
        val line = getStringExtra(EXTRA_LINE).orEmpty()
        val stopId = getStringExtra(EXTRA_STOP_ID).orEmpty()
        val targetMinutes = getIntExtra(EXTRA_TARGET_MINUTES, 0)
        return if (line.isBlank() || stopId.isBlank() || targetMinutes <= 0) null else BusAlarmRequest(line, stopId, targetMinutes)
    }

    companion object {
        private const val TAG = "BusAlarm"
        private const val NOTIFICATION_ID = 1001

        private const val ACTION_START = "org.m0skit0.android.emtmadridalarms.START"
        private const val ACTION_CANCEL = "org.m0skit0.android.emtmadridalarms.CANCEL"
        private const val ACTION_STOP_RINGING = "org.m0skit0.android.emtmadridalarms.STOP_RINGING"

        private const val EXTRA_LINE = "line"
        private const val EXTRA_STOP_ID = "stop_id"
        private const val EXTRA_TARGET_MINUTES = "target_minutes"

        fun start(context: Context, request: BusAlarmRequest) {
            Log.d(TAG, "Requesting service start line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
            val intent = Intent(context, AlarmMonitorService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_LINE, request.line)
                .putExtra(EXTRA_STOP_ID, request.stopId)
                .putExtra(EXTRA_TARGET_MINUTES, request.targetMinutes)
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            Log.d(TAG, "Requesting service cancel")
            context.startService(Intent(context, AlarmMonitorService::class.java).setAction(ACTION_CANCEL))
        }

        fun stopRinging(context: Context) {
            Log.d(TAG, "Requesting stop ringing")
            context.startService(Intent(context, AlarmMonitorService::class.java).setAction(ACTION_STOP_RINGING))
        }
    }
}
