package org.m0skit0.android.emtmadridalarms.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.ui.AlarmViewModel

val presentationModule = module {
    viewModel {
        AlarmViewModel(
            appContext = androidContext(),
            storage = get(),
            loadBusLines = get(),
            loadBusStops = get(),
        )
    }
}
