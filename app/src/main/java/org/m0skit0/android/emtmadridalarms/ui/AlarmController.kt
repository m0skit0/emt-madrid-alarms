package org.m0skit0.android.emtmadridalarms.ui

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.service.cancelAlarmService
import org.m0skit0.android.emtmadridalarms.service.startAlarmService
import org.m0skit0.android.emtmadridalarms.service.stopAlarmRinging

private const val TAG = "AlarmController"

fun interface AlarmStarter : (BusAlarmRequest) -> Unit
fun interface AlarmCanceller : () -> Unit
fun interface RingingStop : () -> Unit

internal fun alarmStarter(
    context: Context,
    saveActiveAlarm: SaveActiveAlarm,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): AlarmStarter = AlarmStarter { request ->
    scope.launch {
        Log.d(TAG, "Starting alarm line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
        state.update { it.copy(isLoading = true, errorMessage = null) }
        saveActiveAlarm(request)
        startAlarmService(context, request)
        state.update { it.copy(isLoading = false, activeAlarm = request) }
        Log.d(TAG, "Alarm start requested")
    }
}

internal fun alarmCanceller(
    context: Context,
    clearActiveAlarm: ClearActiveAlarm,
    setRinging: SetRinging,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): AlarmCanceller = AlarmCanceller {
    scope.launch {
        Log.d(TAG, "Cancelling alarm")
        cancelAlarmService(context)
        clearActiveAlarm()
        setRinging(false)
        state.update { it.copy(activeAlarm = null, latestEtaSeconds = null, latestDestination = "", statusMessage = "") }
    }
}

internal fun ringingStop(
    context: Context,
    clearActiveAlarm: ClearActiveAlarm,
    setRinging: SetRinging,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): RingingStop = RingingStop {
    scope.launch {
        Log.d(TAG, "Stopping ringing")
        stopAlarmRinging(context)
        setRinging(false)
        clearActiveAlarm()
        state.update { it.copy(isRinging = false, activeAlarm = null) }
    }
}
