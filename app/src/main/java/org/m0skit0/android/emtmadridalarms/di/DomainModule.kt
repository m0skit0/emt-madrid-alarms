package org.m0skit0.android.emtmadridalarms.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.domain.LoadBusArrivalsUseCase
import org.m0skit0.android.emtmadridalarms.domain.LoadBusLinesUseCase
import org.m0skit0.android.emtmadridalarms.domain.LoadBusStopsUseCase

val domainModule = module {
    singleOf(::LoadBusArrivalsUseCase)
    singleOf(::LoadBusLinesUseCase)
    singleOf(::LoadBusStopsUseCase)
}
