package org.m0skit0.android.emtmadridalarms.service

import android.util.Log
import kotlinx.coroutines.delay
import org.m0skit0.android.emtmadridalarms.domain.AlarmStateStore
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival
import org.m0skit0.android.emtmadridalarms.domain.LoadBusArrivalsUseCase
import org.m0skit0.android.emtmadridalarms.domain.shouldTriggerAlarm

class AlarmPollingMonitor(
    private val loadBusArrivals: LoadBusArrivalsUseCase,
    private val storage: AlarmStateStore,
) {
    suspend fun monitor(request: BusAlarmRequest, onTriggered: suspend (BusArrival) -> Unit) {
        storage.saveActiveAlarm(request)
        var pollNumber = 0
        while (true) {
            pollNumber++
            try {
                Log.d(TAG, "Poll #$pollNumber: requesting arrivals line=${request.line} stop=${request.stopId}")
                val arrivals = loadBusArrivals(request)
                val nextArrival = arrivals.firstOrNull { it.estimateSeconds != 999999 }
                Log.d(
                    TAG,
                    "Poll #$pollNumber: arrivals=${arrivals.size}, next=${nextArrival?.estimateSeconds ?: "none"}s destination=${nextArrival?.destination.orEmpty()}",
                )
                storage.saveLatestArrival(
                    etaSeconds = nextArrival?.estimateSeconds,
                    destination = nextArrival?.destination.orEmpty(),
                )

                val shouldTrigger = nextArrival?.let { shouldTriggerAlarm(it.estimateSeconds, request.targetMinutes) } == true
                Log.d(TAG, "Poll #$pollNumber: shouldTrigger=$shouldTrigger targetSeconds=${request.targetMinutes * 60}")
                if (shouldTrigger) {
                    checkNotNull(nextArrival)
                    Log.i(TAG, "Triggering alarm line=${request.line} stop=${request.stopId} etaSeconds=${nextArrival.estimateSeconds}")
                    storage.setRinging(true)
                    storage.clearActiveAlarm()
                    storage.saveLatestArrival(nextArrival.estimateSeconds, nextArrival.destination)
                    onTriggered(nextArrival)
                    break
                }
            } catch (error: Exception) {
                Log.e(TAG, "Poll #$pollNumber failed: ${error.message}", error)
                storage.saveStatus(error.message ?: "Could not refresh EMT arrivals.")
            }
            Log.d(TAG, "Poll #$pollNumber complete; waiting ${POLL_INTERVAL_MS}ms")
            delay(POLL_INTERVAL_MS)
        }
    }

    private companion object {
        const val TAG = "BusAlarm"
        const val POLL_INTERVAL_MS = 30_000L
    }
}
