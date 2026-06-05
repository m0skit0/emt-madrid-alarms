package org.m0skit0.android.emtmadridalarms.service

import timber.log.Timber
import kotlinx.coroutines.delay
import org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival
import org.m0skit0.android.emtmadridalarms.data.SetAlarmEnabled
import org.m0skit0.android.emtmadridalarms.data.SaveStatus
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival
import org.m0skit0.android.emtmadridalarms.domain.LoadBusArrivalsUseCase
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun interface AlarmPollingMonitor : suspend (BusAlarmRequest, suspend (BusArrival) -> Unit) -> Unit

fun interface PollInterval : (Int?, Int) -> Duration

private val MIN_POLL_INTERVAL = 30.seconds
private val MAX_POLL_INTERVAL = 5.minutes

internal fun pollInterval(): PollInterval = PollInterval { estimateSeconds, targetMinutes ->
    if (estimateSeconds == null || targetMinutes <= 0) return@PollInterval MIN_POLL_INTERVAL
    val remainingSeconds = (estimateSeconds - targetMinutes * 60L).coerceAtLeast(0L)
    (remainingSeconds / 6).seconds.coerceIn(MIN_POLL_INTERVAL, MAX_POLL_INTERVAL)
}

private data class PollResult(
    val triggered: Boolean,
    val trackedArrival: BusArrival?,
)

internal fun pollAlarm(
    pollInterval: PollInterval,
    loadBusArrivals: LoadBusArrivalsUseCase,
    saveStatus: SaveStatus,
    saveLatestArrival: SaveLatestArrival,
    setRinging: SetRinging,
    setAlarmEnabled: SetAlarmEnabled,
): AlarmPollingMonitor = AlarmPollingMonitor { request, onTriggered ->
    runPollLoop(request, pollInterval, loadBusArrivals, saveStatus, saveLatestArrival, setRinging, setAlarmEnabled, onTriggered)
}

private suspend fun runPollLoop(
    request: BusAlarmRequest,
    pollInterval: PollInterval,
    loadBusArrivals: LoadBusArrivalsUseCase,
    saveStatus: SaveStatus,
    saveLatestArrival: SaveLatestArrival,
    setRinging: SetRinging,
    setAlarmEnabled: SetAlarmEnabled,
    onTriggered: suspend (BusArrival) -> Unit,
) {
    var pollNumber = 0
    var trackedArrival: BusArrival? = null
    while (true) {
        pollNumber++
        val result = try {
            processPoll(pollNumber, request, trackedArrival, loadBusArrivals, saveLatestArrival, setRinging, setAlarmEnabled, onTriggered)
        } catch (error: Exception) {
            Timber.e(error, "Poll #$pollNumber failed: ${error.message}")
            saveStatus(error.message ?: "Could not refresh EMT arrivals.")
            PollResult(triggered = false, trackedArrival = trackedArrival)
        }
        trackedArrival = result.trackedArrival
        val triggered = result.triggered
        if (triggered) break
        val interval = pollInterval(trackedArrival?.estimateSeconds, request.targetMinutes)
        Timber.d("Poll #$pollNumber complete; waiting $interval")
        delay(interval.inWholeMilliseconds)
    }
}

private suspend fun processPoll(
    pollNumber: Int,
    request: BusAlarmRequest,
    trackedArrival: BusArrival?,
    loadBusArrivals: LoadBusArrivalsUseCase,
    saveLatestArrival: SaveLatestArrival,
    setRinging: SetRinging,
    setAlarmEnabled: SetAlarmEnabled,
    onTriggered: suspend (BusArrival) -> Unit,
): PollResult {
    val validArrivals = loadValidArrivals(pollNumber, request, loadBusArrivals)
    val nextTrackedArrival = selectTrackedArrival(validArrivals, trackedArrival, request.targetMinutes)
    val latestArrival = nextTrackedArrival ?: validArrivals.firstOrNull()
    saveLatestArrival(latestArrival?.estimateSeconds, latestArrival?.destination.orEmpty())

    val shouldTrigger = nextTrackedArrival?.let { shouldTriggerAlarm(it.estimateSeconds, request.targetMinutes) } == true
    Timber.d("Poll #$pollNumber: shouldTrigger=$shouldTrigger targetSeconds=${triggerThresholdSeconds(request.targetMinutes)}")
    if (!shouldTrigger) return PollResult(triggered = false, trackedArrival = nextTrackedArrival)
    checkNotNull(nextTrackedArrival)
    triggerAlarm(request, nextTrackedArrival, setRinging, setAlarmEnabled, saveLatestArrival, onTriggered)
    return PollResult(triggered = true, trackedArrival = nextTrackedArrival)
}

private suspend fun loadValidArrivals(
    pollNumber: Int,
    request: BusAlarmRequest,
    loadBusArrivals: LoadBusArrivalsUseCase,
): List<BusArrival> {
    Timber.d("Poll #$pollNumber: requesting arrivals line=${request.line} stop=${request.stopId}")
    val arrivals = loadBusArrivals(request).filter { isValidArrival(it.estimateSeconds) }
    val nextArrival = arrivals.firstOrNull()
    Timber.d("Poll #$pollNumber: arrivals=${arrivals.size}, next=${nextArrival?.estimateSeconds ?: "none"}s destination=${nextArrival?.destination.orEmpty()}")
    return arrivals
}

private fun selectTrackedArrival(
    arrivals: List<BusArrival>,
    trackedArrival: BusArrival?,
    targetMinutes: Int,
): BusArrival? {
    if (targetMinutes <= 0) return null
    if (trackedArrival != null) return findClosestTrackedArrival(arrivals, trackedArrival)
    val targetSeconds = targetMinutes * 60
    return arrivals.firstOrNull { it.estimateSeconds >= targetSeconds }
}

private fun findClosestTrackedArrival(arrivals: List<BusArrival>, trackedArrival: BusArrival): BusArrival? {
    val sameDestination = arrivals.filter { it.destination == trackedArrival.destination }
    val candidates = sameDestination.ifEmpty { arrivals }
    return candidates.minByOrNull { abs(it.estimateSeconds - trackedArrival.estimateSeconds) }
}

private suspend fun triggerAlarm(
    request: BusAlarmRequest,
    arrival: BusArrival,
    setRinging: SetRinging,
    setAlarmEnabled: SetAlarmEnabled,
    saveLatestArrival: SaveLatestArrival,
    onTriggered: suspend (BusArrival) -> Unit,
) {
    Timber.i("Triggering alarm line=${request.line} stop=${request.stopId} etaSeconds=${arrival.estimateSeconds}")
    setRinging(true, request)
    setAlarmEnabled(request, false)
    saveLatestArrival(arrival.estimateSeconds, arrival.destination)
    onTriggered(arrival)
}

private fun shouldTriggerAlarm(estimateSeconds: Int, targetMinutes: Int): Boolean {
    if (targetMinutes <= 0) return false
    if (!isValidArrival(estimateSeconds)) return false
    return estimateSeconds <= triggerThresholdSeconds(targetMinutes)
}

private fun triggerThresholdSeconds(targetMinutes: Int): Long = targetMinutes * 60L + MIN_POLL_INTERVAL.inWholeSeconds

private fun isValidArrival(estimateSeconds: Int): Boolean = estimateSeconds >= 0 && estimateSeconds != 999999
