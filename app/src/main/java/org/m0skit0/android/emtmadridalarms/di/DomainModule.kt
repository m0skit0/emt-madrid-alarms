package org.m0skit0.android.emtmadridalarms.di

import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.data.ArrivalsProvider
import org.m0skit0.android.emtmadridalarms.data.LinesProvider
import org.m0skit0.android.emtmadridalarms.data.StopsProvider
import org.m0skit0.android.emtmadridalarms.domain.LoadBusArrivalsUseCase
import org.m0skit0.android.emtmadridalarms.domain.LoadBusLinesUseCase
import org.m0skit0.android.emtmadridalarms.domain.LoadBusStopsUseCase
import org.m0skit0.android.emtmadridalarms.domain.loadBusArrivalsUseCase
import org.m0skit0.android.emtmadridalarms.domain.loadBusLinesUseCase
import org.m0skit0.android.emtmadridalarms.domain.loadBusStopsUseCase

val domainModule = module {
    single<LoadBusLinesUseCase> { loadBusLinesUseCase(get<LinesProvider>()) }
    single<LoadBusStopsUseCase> { loadBusStopsUseCase(get<StopsProvider>()) }
    single<LoadBusArrivalsUseCase> { loadBusArrivalsUseCase(get<ArrivalsProvider>()) }
}
