package org.m0skit0.android.emtmadridalarms.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.ui.MainActivity

class AlarmNotificationFactory(private val context: Context) {
    fun monitoringNotification(request: BusAlarmRequest, text: String, cancelIntent: PendingIntent): Notification {
        return baseNotification(CHANNEL_MONITORING)
            .setContentTitle("Monitoring bus ${request.line}")
            .setContentText("Stop ${request.stopId}, alarm at ${request.targetMinutes} min. $text")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(0, "Cancel", cancelIntent)
            .build()
    }

    fun ringingNotification(request: BusAlarmRequest, stopIntent: PendingIntent): Notification {
        return baseNotification(CHANNEL_ALARM)
            .setContentTitle("Bus ${request.line} is arriving")
            .setContentText("It is within ${request.targetMinutes} minutes of stop ${request.stopId}.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MONITORING, "Bus alarm monitoring", NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, "Bus alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rings when a monitored bus reaches the configured arrival time."
                enableVibration(true)
            },
        )
        Log.d(TAG, "Notification channels ensured")
    }

    private fun baseNotification(channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setContentIntent(activityPendingIntent())
            .setOnlyAlertOnce(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    private fun activityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val TAG = "BusAlarm"
        const val CHANNEL_MONITORING = "bus_alarm_monitoring"
        const val CHANNEL_ALARM = "bus_alarm_ringing"
    }
}
