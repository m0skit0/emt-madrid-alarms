package org.m0skit0.android.emtmadridalarms.service

import android.util.Log
import kotlinx.coroutines.delay
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival
import org.m0skit0.android.emtmadridalarms.data.SaveStatus
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival
import org.m0skit0.android.emtmadridalarms.domain.LoadBusArrivalsUseCase
import kotlin.time.Duration.Companion.seconds

fun interface AlarmPollingMonitor : suspend (BusAlarmRequest, suspend (BusArrival) -> Unit) -> Unit

private const val TAG = "BusAlarm"
private val POLL_INTERVAL = 30.seconds

internal fun pollAlarm(
    loadBusArrivals: LoadBusArrivalsUseCase,
    saveActiveAlarm: SaveActiveAlarm,
    saveStatus: SaveStatus,
    saveLatestArrival: SaveLatestArrival,
    setRinging: SetRinging,
    clearActiveAlarm: ClearActiveAlarm,
): AlarmPollingMonitor = AlarmPollingMonitor { request, onTriggered ->
    saveActiveAlarm(request)
    var pollNumber = 0
    while (true) {
        pollNumber++
        val triggered = try {
            processPoll(
                pollNumber,
                request,
                loadBusArrivals,
                saveLatestArrival,
                setRinging,
                clearActiveAlarm,
                onTriggered
            )
        } catch (error: Exception) {
            Log.e(TAG, "Poll #$pollNumber failed: ${error.message}", error)
            saveStatus(error.message ?: "Could not refresh EMT arrivals.")
            false
        }
        if (triggered) break
        Log.d(TAG, "Poll #$pollNumber complete; waiting $POLL_INTERVAL")
        delay(POLL_INTERVAL.inWholeMilliseconds)
    }
}

private suspend fun processPoll(
    pollNumber: Int,
    request: BusAlarmRequest,
    loadBusArrivals: LoadBusArrivalsUseCase,
    saveLatestArrival: SaveLatestArrival,
    setRinging: SetRinging,
    clearActiveAlarm: ClearActiveAlarm,
    onTriggered: suspend (BusArrival) -> Unit,
): Boolean {
    Log.d(TAG, "Poll #$pollNumber: requesting arrivals line=${request.line} stop=${request.stopId}")
    val arrivals = loadBusArrivals(request)
    val nextArrival = arrivals.firstOrNull { it.estimateSeconds != 999999 }
    Log.d(
        TAG,
        "Poll #$pollNumber: arrivals=${arrivals.size}, next=${nextArrival?.estimateSeconds ?: "none"}s destination=${nextArrival?.destination.orEmpty()}"
    )
    saveLatestArrival(
        nextArrival?.estimateSeconds,
        nextArrival?.destination.orEmpty(),
    )
    val shouldTrigger =
        nextArrival?.let { shouldTriggerAlarm(it.estimateSeconds, request.targetMinutes) } == true
    Log.d(
        TAG,
        "Poll #$pollNumber: shouldTrigger=$shouldTrigger targetSeconds=${request.targetMinutes * 60}"
    )
    if (!shouldTrigger) return false
    checkNotNull(nextArrival)
    triggerAlarm(
        request,
        nextArrival,
        setRinging,
        clearActiveAlarm,
        saveLatestArrival,
        onTriggered
    )
    return true
}

private suspend fun triggerAlarm(
    request: BusAlarmRequest,
    arrival: BusArrival,
    setRinging: SetRinging,
    clearActiveAlarm: ClearActiveAlarm,
    saveLatestArrival: SaveLatestArrival,
    onTriggered: suspend (BusArrival) -> Unit,
) {
    Log.i(
        TAG,
        "Triggering alarm line=${request.line} stop=${request.stopId} etaSeconds=${arrival.estimateSeconds}"
    )
    setRinging(true)
    clearActiveAlarm()
    saveLatestArrival(arrival.estimateSeconds, arrival.destination)
    onTriggered(arrival)
}

private fun shouldTriggerAlarm(estimateSeconds: Int, targetMinutes: Int): Boolean {
    if (targetMinutes <= 0) return false
    if (estimateSeconds < 0 || estimateSeconds == 999999) return false
    return estimateSeconds <= targetMinutes * 60
}
