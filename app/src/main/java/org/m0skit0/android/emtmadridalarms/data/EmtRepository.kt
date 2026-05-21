package org.m0skit0.android.emtmadridalarms.data

import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRepository
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

class EmtRepository(
    private val lineService: EmtLineService,
    private val stopsProvider: StopsProvider,
    private val arrivalsProvider: ArrivalsProvider,
) : BusAlarmRepository {
    override suspend fun lines(): List<BusLine> = lineService.lines()

    override suspend fun stopsForLine(line: BusLine): List<BusStop> = stopsProvider(line)

    override suspend fun arrivalsFor(request: BusAlarmRequest): List<BusArrival> = arrivalsProvider(request)
}
