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
import org.m0skit0.android.emtmadridalarms.service.pollAlarm
import org.m0skit0.android.emtmadridalarms.service.NotificationChannelsEnsurer
import org.m0skit0.android.emtmadridalarms.service.RingingNotificationProvider
import org.m0skit0.android.emtmadridalarms.service.ensureNotificationChannels
import org.m0skit0.android.emtmadridalarms.service.monitoringNotification
import org.m0skit0.android.emtmadridalarms.service.ringingNotification

val serviceModule = module {
    single<MonitoringNotificationProvider> {
        MonitoringNotificationProvider { request, text, intent ->
            monitoringNotification(
                androidContext(),
                request,
                text,
                intent
            )
        }
    }
    single<RingingNotificationProvider> {
        RingingNotificationProvider { request, intent ->
            ringingNotification(
                androidContext(),
                request,
                intent
            )
        }
    }
    single<NotificationChannelsEnsurer> {
        NotificationChannelsEnsurer { ensureNotificationChannels(androidContext()) }
    }
    single { AlarmSignalPlayer(androidContext()) }
    single<AlarmPollingMonitor> {
        AlarmPollingMonitor { request, onTriggered ->
            pollAlarm(
                request,
                onTriggered,
                get(),
                get<SaveActiveAlarm>(),
                get<SaveStatus>(),
                get<SaveLatestArrival>(),
                get<SetRinging>(),
                get<ClearActiveAlarm>(),
            )
        }
    }
}
