package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival

fun interface ArrivalsProvider : suspend (BusAlarmRequest) -> List<BusArrival>

internal fun arrivalsProvider(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    dateProvider: EmtDateProvider,
    validator: EmtResponseValidator,
): ArrivalsProvider = ArrivalsProvider { request -> arrivalsFor(request, api, authTokenProvider, dateProvider, validator) }

private const val TAG = "EmtArrivalService"

internal suspend fun arrivalsFor(
    request: BusAlarmRequest,
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    dateProvider: EmtDateProvider,
    validator: EmtResponseValidator,
): List<BusArrival> {
    val dateRef = dateProvider()
    Log.d(TAG, "Loading arrivals line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes} dateRef=$dateRef")
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
    Log.d(
        TAG,
        "Loaded arrivals raw=${rawArrivals.size} matching=${arrivals.size} estimates=${arrivals.take(4).joinToString { "${it.line}:${it.estimateSeconds}s" }}",
    )
    return arrivals
}
