package org.m0skit0.android.emtmadridalarms.di

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.R
import org.m0skit0.android.emtmadridalarms.ui.AlarmState
import org.m0skit0.android.emtmadridalarms.ui.AlarmViewModel
import org.m0skit0.android.emtmadridalarms.ui.alarmCanceller
import org.m0skit0.android.emtmadridalarms.ui.alarmEnabler
import org.m0skit0.android.emtmadridalarms.ui.alarmRequestBuilder
import org.m0skit0.android.emtmadridalarms.ui.alarmRequestValidator
import org.m0skit0.android.emtmadridalarms.ui.alarmStarter
import org.m0skit0.android.emtmadridalarms.ui.allStopLoader
import org.m0skit0.android.emtmadridalarms.ui.lineLoader
import org.m0skit0.android.emtmadridalarms.ui.ringingStop
import org.m0skit0.android.emtmadridalarms.ui.singleAlarmCanceller
import org.m0skit0.android.emtmadridalarms.ui.stopLoader

val presentationModule = module {
    viewModel {
        val state = MutableStateFlow(AlarmState())
        val scope = MainScope()
        AlarmViewModel(
            _state = state,
            scope = scope,
            alarmStateReader = get(),
            loadLines = lineLoader(get(), state, scope, androidContext().getString(R.string.error_load_bus_lines)),
            loadAllStops = allStopLoader(get(), state, scope, androidContext().getString(R.string.error_load_all_bus_stops)),
            loadStops = stopLoader(get(), state, scope) { lineLabel ->
                androidContext().getString(R.string.error_load_stops_for_line, lineLabel)
            },
            buildRequest = alarmRequestBuilder(
                validator = alarmRequestValidator(
                    enterBusLineMessage = androidContext().getString(R.string.error_enter_bus_line),
                    enterStopNumberMessage = androidContext().getString(R.string.error_enter_stop_number),
                    stopDigitsMessage = androidContext().getString(R.string.error_stop_number_digits),
                    positiveMinutesMessage = androidContext().getString(R.string.error_positive_minutes),
                ),
                selectBusLineMessage = androidContext().getString(R.string.error_select_bus_line_from_list),
                selectStopForLineMessage = androidContext().getString(R.string.error_select_stop_for_selected_line),
            ),
            startAlarm = alarmStarter(androidContext(), get(), state, scope),
            cancelSingleAlarm = singleAlarmCanceller(androidContext(), get(), state, scope),
            enableAlarm = alarmEnabler(androidContext(), get(), state, scope),
            cancelAlarm = alarmCanceller(androidContext(), get(), get(), state, scope),
            stopRinging = ringingStop(androidContext(), get(), state, scope),
            maxActiveAlarmsMessage = { max -> androidContext().getString(R.string.error_max_active_alarms, max) },
            duplicateAlarmMessage = { androidContext().getString(R.string.error_duplicate_alarm) },
        )
    }
}
