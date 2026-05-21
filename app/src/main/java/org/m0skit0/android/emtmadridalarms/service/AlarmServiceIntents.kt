package org.m0skit0.android.emtmadridalarms.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest

internal const val ACTION_START = "org.m0skit0.android.emtmadridalarms.START"
internal const val ACTION_CANCEL = "org.m0skit0.android.emtmadridalarms.CANCEL"
internal const val ACTION_STOP_RINGING = "org.m0skit0.android.emtmadridalarms.STOP_RINGING"

private const val EXTRA_LINE = "line"
private const val EXTRA_STOP_ID = "stop_id"
private const val EXTRA_TARGET_MINUTES = "target_minutes"

internal fun Intent.alarmRequest(): BusAlarmRequest? {
    val line = getStringExtra(EXTRA_LINE).orEmpty()
    val stopId = getStringExtra(EXTRA_STOP_ID).orEmpty()
    val targetMinutes = getIntExtra(EXTRA_TARGET_MINUTES, 0)
    return if (line.isBlank() || stopId.isBlank() || targetMinutes <= 0) null
    else BusAlarmRequest(line, stopId, targetMinutes)
}

internal fun startAlarmIntent(context: Context, request: BusAlarmRequest): Intent =
    Intent(context, AlarmMonitorService::class.java)
        .setAction(ACTION_START)
        .putExtra(EXTRA_LINE, request.line)
        .putExtra(EXTRA_STOP_ID, request.stopId)
        .putExtra(EXTRA_TARGET_MINUTES, request.targetMinutes)

internal fun Context.servicePendingIntent(action: String, requestCode: Int): PendingIntent {
    val intent = Intent(this, AlarmMonitorService::class.java).setAction(action)
    return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
}
