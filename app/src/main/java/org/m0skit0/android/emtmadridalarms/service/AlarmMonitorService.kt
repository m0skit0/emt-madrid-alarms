package org.m0skit0.android.emtmadridalarms.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.MainActivity
import org.m0skit0.android.emtmadridalarms.R
import org.m0skit0.android.emtmadridalarms.data.AlarmStorage
import org.m0skit0.android.emtmadridalarms.data.EmtRepository
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.shouldTriggerAlarm

class AlarmMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = EmtRepository()
    private lateinit var storage: AlarmStorage
    private var monitorJob: Job? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        storage = AlarmStorage(applicationContext)
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> cancelMonitoring()
            ACTION_STOP_RINGING -> stopRingingAndSelf()
            ACTION_START -> startMonitoring(intent.alarmRequest())
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        stopRinging()
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring(request: BusAlarmRequest?) {
        if (request == null) {
            stopSelf()
            return
        }

        monitorJob?.cancel()
        startForeground(NOTIFICATION_ID, monitoringNotification(request, "Waiting for EMT arrivals..."))
        monitorJob = scope.launch {
            storage.saveActiveAlarm(request)
            while (true) {
                try {
                    val arrivals = repository.arrivalsFor(request)
                    val nextArrival = arrivals.firstOrNull { it.estimateSeconds != 999999 }
                    storage.saveLatestArrival(
                        etaSeconds = nextArrival?.estimateSeconds,
                        destination = nextArrival?.destination.orEmpty(),
                    )

                    if (nextArrival != null && shouldTriggerAlarm(nextArrival.estimateSeconds, request.targetMinutes)) {
                        storage.setRinging(true)
                        storage.clearActiveAlarm()
                        storage.saveLatestArrival(nextArrival.estimateSeconds, nextArrival.destination)
                        launch(Dispatchers.Main) { startRinging(request) }
                        break
                    }
                } catch (error: Exception) {
                    storage.saveStatus(error.message ?: "Could not refresh EMT arrivals.")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun cancelMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        scope.launch {
            storage.clearActiveAlarm()
            storage.setRinging(false)
        }
        stopRinging()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startRinging(request: BusAlarmRequest) {
        startForeground(NOTIFICATION_ID, ringingNotification(request))
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Uri.EMPTY
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 900L, 600L), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0L, 900L, 600L), 0)
        }
    }

    private fun stopRingingAndSelf() {
        scope.launch {
            storage.setRinging(false)
            storage.clearActiveAlarm()
        }
        stopRinging()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopRinging() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun monitoringNotification(request: BusAlarmRequest, text: String): Notification {
        return baseNotification(CHANNEL_MONITORING)
            .setContentTitle("Monitoring bus ${request.line}")
            .setContentText("Stop ${request.stopId}, alarm at ${request.targetMinutes} min. $text")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(0, "Cancel", servicePendingIntent(ACTION_CANCEL, 2))
            .build()
    }

    private fun ringingNotification(request: BusAlarmRequest): Notification {
        return baseNotification(CHANNEL_ALARM)
            .setContentTitle("Bus ${request.line} is arriving")
            .setContentText("It is within ${request.targetMinutes} minutes of stop ${request.stopId}.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(0, "Stop", servicePendingIntent(ACTION_STOP_RINGING, 3))
            .build()
    }

    private fun baseNotification(channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setContentIntent(activityPendingIntent())
            .setOnlyAlertOnce(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    private fun activityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AlarmMonitorService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MONITORING, "Bus alarm monitoring", NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, "Bus alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rings when a monitored bus reaches the configured arrival time."
                enableVibration(true)
            },
        )
    }

    private fun Intent.alarmRequest(): BusAlarmRequest? {
        val line = getStringExtra(EXTRA_LINE).orEmpty()
        val stopId = getStringExtra(EXTRA_STOP_ID).orEmpty()
        val targetMinutes = getIntExtra(EXTRA_TARGET_MINUTES, 0)
        return if (line.isBlank() || stopId.isBlank() || targetMinutes <= 0) null else BusAlarmRequest(line, stopId, targetMinutes)
    }

    companion object {
        private const val CHANNEL_MONITORING = "bus_alarm_monitoring"
        private const val CHANNEL_ALARM = "bus_alarm_ringing"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 30_000L

        private const val ACTION_START = "org.m0skit0.android.emtmadridalarms.START"
        private const val ACTION_CANCEL = "org.m0skit0.android.emtmadridalarms.CANCEL"
        private const val ACTION_STOP_RINGING = "org.m0skit0.android.emtmadridalarms.STOP_RINGING"

        private const val EXTRA_LINE = "line"
        private const val EXTRA_STOP_ID = "stop_id"
        private const val EXTRA_TARGET_MINUTES = "target_minutes"

        fun start(context: Context, request: BusAlarmRequest) {
            val intent = Intent(context, AlarmMonitorService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_LINE, request.line)
                .putExtra(EXTRA_STOP_ID, request.stopId)
                .putExtra(EXTRA_TARGET_MINUTES, request.targetMinutes)
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            context.startService(Intent(context, AlarmMonitorService::class.java).setAction(ACTION_CANCEL))
        }

        fun stopRinging(context: Context) {
            context.startService(Intent(context, AlarmMonitorService::class.java).setAction(ACTION_STOP_RINGING))
        }
    }
}
