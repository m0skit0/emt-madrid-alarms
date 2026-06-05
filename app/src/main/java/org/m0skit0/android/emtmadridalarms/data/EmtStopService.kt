package org.m0skit0.android.emtmadridalarms.data

import timber.log.Timber
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.utils.orDefault

fun interface AllStopsProvider : suspend () -> List<BusStop>

fun interface StopsProvider : suspend (BusLine) -> List<BusStop>

private const val TAG = "EmtStopService"

internal fun stopsForLine(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    validator: EmtResponseValidator,
): StopsProvider = StopsProvider { line ->
    val accessToken = authTokenProvider()
    Timber.d("Loading stops for line=${line.label} id=${line.id}")
    val stops = listOf(1, 2)
        .flatMap { direction ->
            Timber.d("Loading stops for line=${line.label} direction=$direction")
            val response = api.lineStops(accessToken, line.id, direction)
            validator(response.code, response.description)
            val directionStops = response.data.flatMap { it.stops }
            Timber.d("Loaded stops direction=$direction raw=${directionStops.size}")
            directionStops
        }
        .mapNotNull { dto ->
            if (dto.stop.isBlank()) return@mapNotNull null
            BusStop(
                id = dto.stop,
                name = dto.name,
                address = dto.postalAddress,
                lineLabels = setOf(normalizeLine(line.label), normalizeLine(line.id)),
            )
        }
        .distinctBy { it.id }
        .sortedBy { it.id.toIntOrNull().orDefault { Int.MAX_VALUE } }
    Timber.d("Loaded stops for line=${line.label} mapped=${stops.size}")
    stops
}

internal fun allStops(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    validator: EmtResponseValidator,
): AllStopsProvider = AllStopsProvider {
    Timber.d("Loading all EMT stops")
    val response = api.stopsList(authTokenProvider())
    validator(response.code, response.description)

    val stops = response.data
        .mapNotNull { dto ->
            if (dto.node.isBlank()) return@mapNotNull null
            BusStop(
                id = dto.node,
                name = dto.name,
                address = "",
                lineLabels = dto.lines.mapNotNull(::lineLabelFromStopListEntry).toSet(),
            )
        }
        .distinctBy { it.id }
        .sortedBy { it.id.toIntOrNull().orDefault { Int.MAX_VALUE } }
    Timber.d("Loaded all EMT stops raw=${response.data.size} mapped=${stops.size}")
    stops
}

private fun lineLabelFromStopListEntry(entry: String): String? =
    normalizeLine(entry.substringBefore("/")).takeIf { it.isNotBlank() }
