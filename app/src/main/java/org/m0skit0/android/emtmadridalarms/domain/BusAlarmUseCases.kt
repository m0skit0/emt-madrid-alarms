package org.m0skit0.android.emtmadridalarms.domain

import org.m0skit0.android.emtmadridalarms.data.ArrivalsProvider
import org.m0skit0.android.emtmadridalarms.data.LinesProvider
import org.m0skit0.android.emtmadridalarms.data.StopsProvider

fun interface LoadBusLinesUseCase : suspend () -> List<BusLine>

fun interface LoadBusStopsUseCase : suspend (BusLine) -> List<BusStop>

fun interface LoadBusArrivalsUseCase : suspend (BusAlarmRequest) -> List<BusArrival>

internal fun loadBusLinesUseCase(linesProvider: LinesProvider): LoadBusLinesUseCase =
    LoadBusLinesUseCase { linesProvider() }

internal fun loadBusStopsUseCase(stopsProvider: StopsProvider): LoadBusStopsUseCase =
    LoadBusStopsUseCase { line -> stopsProvider(line) }

internal fun loadBusArrivalsUseCase(arrivalsProvider: ArrivalsProvider): LoadBusArrivalsUseCase =
    LoadBusArrivalsUseCase { request -> arrivalsProvider(request) }
