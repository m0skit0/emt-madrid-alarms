package org.m0skit0.android.emtmadridalarms.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival
import org.m0skit0.android.emtmadridalarms.data.SaveStatus
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.service.AlarmPollingMonitor
import org.m0skit0.android.emtmadridalarms.service.AlarmSignalPlayer
import org.m0skit0.android.emtmadridalarms.service.MonitoringNotificationProvider
import org.m0skit0.android.emtmadridalarms.service.NotificationChannelsEnsurer
import org.m0skit0.android.emtmadridalarms.service.RingingNotificationProvider
import org.m0skit0.android.emtmadridalarms.service.alarmPollingMonitor
import org.m0skit0.android.emtmadridalarms.service.monitoringNotificationProvider
import org.m0skit0.android.emtmadridalarms.service.notificationChannelsEnsurer
import org.m0skit0.android.emtmadridalarms.service.ringingNotificationProvider

val serviceModule = module {
    single<MonitoringNotificationProvider> { monitoringNotificationProvider(androidContext()) }
    single<RingingNotificationProvider> { ringingNotificationProvider(androidContext()) }
    single<NotificationChannelsEnsurer> { notificationChannelsEnsurer(androidContext()) }
    single { AlarmSignalPlayer(androidContext()) }
    single<AlarmPollingMonitor> {
        alarmPollingMonitor(get(), get<SaveActiveAlarm>(), get<SaveStatus>(), get<SaveLatestArrival>(), get<SetRinging>(), get<ClearActiveAlarm>())
    }
}
