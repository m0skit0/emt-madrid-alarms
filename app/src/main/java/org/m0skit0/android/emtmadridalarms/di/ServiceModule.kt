package org.m0skit0.android.emtmadridalarms.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.service.ensureNotificationChannels
import org.m0skit0.android.emtmadridalarms.service.monitoringNotification
import org.m0skit0.android.emtmadridalarms.service.pollInterval
import org.m0skit0.android.emtmadridalarms.service.pollAlarm
import org.m0skit0.android.emtmadridalarms.service.ringingNotification
import org.m0skit0.android.emtmadridalarms.service.startSignal
import org.m0skit0.android.emtmadridalarms.service.stopSignal

val serviceModule = module {
    single { monitoringNotification(androidContext()) }
    single { ringingNotification(androidContext()) }
    single { ensureNotificationChannels(androidContext()) }
    single { startSignal(androidContext(), get()) }
    single { stopSignal(get()) }
    single { pollInterval() }
    single { pollAlarm(get(), get(), get(), get(), get(), get(), get()) }
}
