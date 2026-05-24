package org.m0skit0.android.emtmadridalarms.data

import timber.log.Timber
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival

fun interface ArrivalsProvider : suspend (BusAlarmRequest) -> List<BusArrival>

private const val TAG = "EmtArrivalService"

internal fun arrivalsFor(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    dateProvider: EmtDateProvider,
    validator: EmtResponseValidator,
): ArrivalsProvider = ArrivalsProvider { request ->
    val dateRef = dateProvider()
    Timber.d("Loading arrivals line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes} dateRef=$dateRef")
    val response = api.arrivals(
        accessToken = authTokenProvider(),
        stopId = request.stopId,
        lineArrive = request.line.trim(),
        body = ArrivalsRequestBody(incidencesDate = dateRef),
    )
    validator(response.code, response.description)

    val rawArrivals = response.data.flatMap { it.arrivals }
    val arrivals = rawArrivals
        .map {
            BusArrival(
                line = it.line,
                stopId = it.stop,
                destination = it.destination,
                estimateSeconds = it.estimateSeconds,
                distanceMeters = it.distanceMeters,
            )
        }
        .filter { linesMatch(request.line, it.line) }
        .sortedBy { it.estimateSeconds }
    Timber.d("Loaded arrivals raw=${rawArrivals.size} matching=${arrivals.size} estimates=${
        arrivals.take(4).joinToString {
            "${it.line}:${it.estimateSeconds}s"
        }
    }")
    arrivals
}
