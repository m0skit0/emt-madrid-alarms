package org.m0skit0.android.emtmadridalarms.di

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.ui.AlarmState
import org.m0skit0.android.emtmadridalarms.ui.AlarmViewModel
import org.m0skit0.android.emtmadridalarms.ui.alarmCanceller
import org.m0skit0.android.emtmadridalarms.ui.alarmRequestBuilder
import org.m0skit0.android.emtmadridalarms.ui.alarmRequestValidator
import org.m0skit0.android.emtmadridalarms.ui.alarmStarter
import org.m0skit0.android.emtmadridalarms.ui.lineLoader
import org.m0skit0.android.emtmadridalarms.ui.ringingStop
import org.m0skit0.android.emtmadridalarms.ui.stopLoader

val presentationModule = module {
    viewModel {
        val state = MutableStateFlow(AlarmState())
        val scope = MainScope()
        AlarmViewModel(
            _state = state,
            scope = scope,
            alarmStateReader = get(),
            loadLines = lineLoader(get(), state, scope),
            loadStops = stopLoader(get(), state, scope),
            buildRequest = alarmRequestBuilder(alarmRequestValidator()),
            startAlarm = alarmStarter(androidContext(), get(), state, scope),
            cancelAlarm = alarmCanceller(androidContext(), get(), get(), state, scope),
            stopRinging = ringingStop(androidContext(), get(), get(), state, scope),
        )
    }
}
