package org.m0skit0.android.emtmadridalarms.service

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest

private const val TAG = "BusAlarm"

fun startAlarmService(context: Context, request: BusAlarmRequest) {
    Log.d(TAG, "Requesting service start line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
    ContextCompat.startForegroundService(context, startAlarmIntent(context, request))
}

fun cancelAlarmService(context: Context) {
    Log.d(TAG, "Requesting service cancel")
    context.startService(android.content.Intent(context, AlarmMonitorService::class.java).setAction(ACTION_CANCEL))
}

fun stopAlarmRinging(context: Context) {
    Log.d(TAG, "Requesting stop ringing")
    context.startService(android.content.Intent(context, AlarmMonitorService::class.java).setAction(ACTION_STOP_RINGING))
}
