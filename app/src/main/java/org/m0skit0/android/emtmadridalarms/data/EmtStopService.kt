package org.m0skit0.android.emtmadridalarms.data

import timber.log.Timber
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.utils.orDefault

fun interface AllStopsProvider : suspend () -> List<BusStop>

fun interface StopsProvider : suspend (BusLine) -> List<BusStop>

internal fun stopsForLine(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    validator: EmtResponseValidator,
): StopsProvider = StopsProvider { line ->
    val accessToken = authTokenProvider()
    Timber.d("Loading stops for line=${line.label} id=${line.id}")
    val lineLabels = setOf(normalizeLine(line.label), normalizeLine(line.id))
    listOf(1, 2)
        .flatMap { direction -> loadDirectionStops(api, validator, accessToken, line, direction) }
        .mapNotNull { dto -> busStopFromLineStop(dto, lineLabels) }
        .distinctBy { it.id }
        .sortedByBusStopId()
        .also { Timber.d("Loaded stops for line=${line.label} mapped=${it.size}") }
}

internal fun allStops(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    validator: EmtResponseValidator,
): AllStopsProvider = AllStopsProvider {
    Timber.d("Loading all EMT stops")
    val response = api.stopsList(authTokenProvider())
    validator(response.code, response.description)

    response.data
        .mapNotNull(::busStopFromStopListEntry)
        .distinctBy { it.id }
        .sortedByBusStopId()
        .also { Timber.d("Loaded all EMT stops raw=${response.data.size} mapped=${it.size}") }
}

private suspend fun loadDirectionStops(
    api: EmtApi,
    validator: EmtResponseValidator,
    accessToken: String,
    line: BusLine,
    direction: Int,
): List<EmtStopDto> {
    Timber.d("Loading stops for line=${line.label} direction=$direction")
    val response = api.lineStops(accessToken, line.id, direction)
    validator(response.code, response.description)
    return response.data
        .flatMap(EmtLineStopsData::stops)
        .also { Timber.d("Loaded stops direction=$direction raw=${it.size}") }
}

private fun busStopFromLineStop(dto: EmtStopDto, lineLabels: Set<String>): BusStop? {
    if (dto.stop.isBlank()) return null
    return BusStop(
        id = dto.stop,
        name = dto.name,
        address = dto.postalAddress,
        lineLabels = lineLabels,
    )
}

private fun busStopFromStopListEntry(dto: EmtStopListDto): BusStop? {
    if (dto.node.isBlank()) return null
    return BusStop(
        id = dto.node,
        name = dto.name,
        address = "",
        lineLabels = dto.lines.mapNotNull(::lineLabelFromStopListEntry).toSet(),
    )
}

private fun List<BusStop>.sortedByBusStopId(): List<BusStop> =
    sortedBy { it.id.toIntOrNull().orDefault { Int.MAX_VALUE } }

private fun lineLabelFromStopListEntry(entry: String): String? =
    normalizeLine(entry.substringBefore("/")).takeIf { it.isNotBlank() }
