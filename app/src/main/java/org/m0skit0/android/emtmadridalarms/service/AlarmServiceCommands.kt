package org.m0skit0.android.emtmadridalarms.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest

private const val TAG = "BusAlarm"

fun startAlarmService(context: Context, request: BusAlarmRequest) {
    Log.d(
        TAG,
        "Requesting service start line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}"
    )
    ContextCompat.startForegroundService(context, startAlarmIntent(context, request))
}

fun cancelAlarmService(context: Context) {
    Log.d(TAG, "Requesting service cancel")
    Intent(context, AlarmMonitorService::class.java)
        .setAction(ACTION_CANCEL)
        .let(context::startService)
}

fun stopAlarmRinging(context: Context) {
    Log.d(TAG, "Requesting stop ringing")
    Intent(context, AlarmMonitorService::class.java)
        .setAction(ACTION_STOP_RINGING)
        .let(context::startService)
}
