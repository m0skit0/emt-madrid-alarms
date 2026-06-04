package org.m0skit0.android.emtmadridalarms.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber
import androidx.core.app.NotificationCompat
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.ui.MainActivity

fun interface MonitoringNotificationProvider :
        (List<BusAlarmRequest>, String, PendingIntent) -> Notification

fun interface RingingNotificationProvider : (BusAlarmRequest, PendingIntent) -> Notification

fun interface NotificationChannelsEnsurer : () -> Unit

private const val TAG = "BusAlarm"
private const val CHANNEL_MONITORING = "bus_alarm_monitoring"
private const val CHANNEL_ALARM = "bus_alarm_ringing"

internal fun monitoringNotification(context: Context): MonitoringNotificationProvider =
    MonitoringNotificationProvider { alarms, text, cancelIntent ->
        val title = "Monitoring ${alarms.size} ${if (alarms.size == 1) "alarm" else "alarms"}"
        baseNotification(context, CHANNEL_MONITORING)
            .setContentTitle(title)
            .setContentText(collapsedAlarmText(alarms, text))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(text)
                    .also { style -> alarms.forEach { style.addLine(it.notificationText()) } }
            )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(0, "Cancel", cancelIntent)
            .build()
    }

private fun collapsedAlarmText(alarms: List<BusAlarmRequest>, text: String): String = when {
    alarms.isEmpty() -> text
    alarms.size <= 2 -> alarms.joinToString(separator = "; ") { it.notificationText() }
    else -> "${alarms.first().notificationText()}; +${alarms.size - 1} more"
}

private fun BusAlarmRequest.notificationText(): String = "Line $line - Stop $stopId - $targetMinutes min"

internal fun ringingNotification(context: Context): RingingNotificationProvider =
    RingingNotificationProvider { request, stopIntent ->
        baseNotification(context, CHANNEL_ALARM)
            .setContentTitle("Bus ${request.line} is arriving")
            .setContentText("It is within ${request.targetMinutes} minutes of stop ${request.stopId}.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

internal fun ensureNotificationChannels(context: Context): NotificationChannelsEnsurer =
    NotificationChannelsEnsurer {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@NotificationChannelsEnsurer
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITORING,
                "Bus alarm monitoring",
                NotificationManager.IMPORTANCE_LOW
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM,
                "Bus alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rings when a monitored bus reaches the configured arrival time."
                enableVibration(true)
            },
        )
        Timber.d("Notification channels ensured")
    }

private fun baseNotification(context: Context, channelId: String): NotificationCompat.Builder =
    NotificationCompat.Builder(context, channelId)
        .setContentIntent(activityPendingIntent(context))
        .setOnlyAlertOnce(false)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

private fun activityPendingIntent(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context,
        1,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )
