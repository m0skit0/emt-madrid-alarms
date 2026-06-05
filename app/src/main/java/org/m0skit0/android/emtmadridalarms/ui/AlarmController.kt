package org.m0skit0.android.emtmadridalarms.ui

import android.content.Context
import android.widget.Toast
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.R
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.RemoveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.MAX_ACTIVE_ALARMS
import org.m0skit0.android.emtmadridalarms.domain.hasSameLineAndStop
import org.m0skit0.android.emtmadridalarms.service.cancelAlarmService
import org.m0skit0.android.emtmadridalarms.service.startAlarmService
import org.m0skit0.android.emtmadridalarms.service.stopAlarmRinging

private const val TAG = "AlarmController"

fun interface AlarmStarter : (BusAlarmRequest) -> Unit
fun interface AlarmCanceller : () -> Unit
fun interface SingleAlarmCanceller : (BusAlarmRequest) -> Unit
fun interface RingingStop : () -> Unit

internal fun alarmStarter(
    context: Context,
    saveActiveAlarm: SaveActiveAlarm,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): AlarmStarter = AlarmStarter { request ->
    scope.launch {
        Timber.d("Starting alarm line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
        state.update { it.copy(isLoading = true, errorMessage = null) }
        saveActiveAlarm(request)
        startAlarmService(context, request)
        state.update {
            val activeAlarms = if (it.activeAlarms.any { alarm -> alarm.hasSameLineAndStop(request) }) {
                it.activeAlarms
            } else {
                (it.activeAlarms + request).take(MAX_ACTIVE_ALARMS)
            }
            it.copy(
                isLoading = false,
                activeAlarm = activeAlarms.firstOrNull(),
                activeAlarms = activeAlarms,
                selectedLine = null,
                selectedStop = null,
                lineInput = "",
                stopInput = "",
                lines = it.allLines,
                stops = emptyList(),
            )
        }
        Toast.makeText(context, context.getString(R.string.toast_alarm_added), Toast.LENGTH_SHORT).show()
        Timber.d("Alarm start requested")
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
        Timber.d("Cancelling alarm")
        cancelAlarmService(context)
        clearActiveAlarm()
        setRinging(false)
        state.update {
            it.copy(
                activeAlarm = null,
                activeAlarms = emptyList(),
                latestEtaSeconds = null,
                latestDestination = "",
                statusMessage = ""
            )
        }
    }
}

internal fun singleAlarmCanceller(
    context: Context,
    removeActiveAlarm: RemoveActiveAlarm,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): SingleAlarmCanceller = SingleAlarmCanceller { request ->
    scope.launch {
        Timber.d("Cancelling alarm line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
        cancelAlarmService(context, request)
        removeActiveAlarm(request)
        state.update {
            val activeAlarms = it.activeAlarms - request
            it.copy(
                activeAlarm = activeAlarms.firstOrNull(),
                activeAlarms = activeAlarms,
                latestEtaSeconds = it.latestEtaSeconds.takeIf { activeAlarms.isNotEmpty() },
                latestDestination = it.latestDestination.takeIf { activeAlarms.isNotEmpty() }.orEmpty(),
                statusMessage = it.statusMessage.takeIf { activeAlarms.isNotEmpty() }.orEmpty(),
            )
        }
    }
}

internal fun ringingStop(
    context: Context,
    setRinging: SetRinging,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): RingingStop = RingingStop {
    scope.launch {
        Timber.d("Stopping ringing")
        stopAlarmRinging(context)
        setRinging(false)
        state.update { it.copy(isRinging = false) }
    }
}
