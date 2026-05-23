package org.m0skit0.android.emtmadridalarms.domain

fun interface LoadBusLinesUseCase : suspend () -> List<BusLine>

fun interface LoadBusStopsUseCase : suspend (BusLine) -> List<BusStop>

fun interface LoadBusArrivalsUseCase : suspend (BusAlarmRequest) -> List<BusArrival>
