package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

fun interface StopsProvider : suspend (BusLine) -> List<BusStop>

internal fun stopsProvider(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    validator: EmtResponseValidator,
): StopsProvider = StopsProvider { line -> stopsForLine(line, api, authTokenProvider, validator) }

private const val TAG = "EmtStopService"

internal suspend fun stopsForLine(
    line: BusLine,
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    validator: EmtResponseValidator,
): List<BusStop> {
    val accessToken = authTokenProvider()
    Log.d(TAG, "Loading stops for line=${line.label} id=${line.id}")
    val stops = listOf(1, 2)
        .flatMap { direction ->
            Log.d(TAG, "Loading stops for line=${line.label} direction=$direction")
            val response = api.lineStops(accessToken, line.id, direction)
            validator(response.code, response.description)
            val directionStops = response.data.flatMap { it.stops }
            Log.d(TAG, "Loaded stops direction=$direction raw=${directionStops.size}")
            directionStops
        }
        .mapNotNull { dto ->
            if (dto.stop.isBlank()) return@mapNotNull null
            BusStop(id = dto.stop, name = dto.name, address = dto.postalAddress)
        }
        .distinctBy { it.id }
        .sortedBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
    Log.d(TAG, "Loaded stops for line=${line.label} mapped=${stops.size}")
    return stops
}
