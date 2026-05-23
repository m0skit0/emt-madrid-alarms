package org.m0skit0.android.emtmadridalarms.di

import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.data.ArrivalsProvider
import org.m0skit0.android.emtmadridalarms.data.LinesProvider
import org.m0skit0.android.emtmadridalarms.data.StopsProvider
import org.m0skit0.android.emtmadridalarms.domain.LoadBusArrivalsUseCase
import org.m0skit0.android.emtmadridalarms.domain.LoadBusLinesUseCase
import org.m0skit0.android.emtmadridalarms.domain.LoadBusStopsUseCase

val domainModule = module {
    single<LoadBusLinesUseCase> { LoadBusLinesUseCase { get<LinesProvider>()() } }
    single<LoadBusStopsUseCase> { LoadBusStopsUseCase { line -> get<StopsProvider>()(line) } }
    single<LoadBusArrivalsUseCase> { LoadBusArrivalsUseCase { request -> get<ArrivalsProvider>()(request) } }
}
