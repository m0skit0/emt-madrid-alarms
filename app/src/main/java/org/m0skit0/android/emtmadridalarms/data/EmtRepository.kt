package org.m0skit0.android.emtmadridalarms.data

import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRepository
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

class EmtRepository(
    private val lineService: EmtLineService,
    private val stopService: EmtStopService,
    private val api: EmtApi,
    private val authTokenProvider: EmtAuthTokenProvider,
    private val dateProvider: EmtDateProvider,
) : BusAlarmRepository {
    override suspend fun lines(): List<BusLine> = lineService.lines()

    override suspend fun stopsForLine(line: BusLine): List<BusStop> = stopService.stopsForLine(line)

    override suspend fun arrivalsFor(request: BusAlarmRequest): List<BusArrival> =
        arrivalsFor(request, api, authTokenProvider, dateProvider)
}
