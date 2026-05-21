package org.m0skit0.android.emtmadridalarms

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.m0skit0.android.emtmadridalarms.di.dataModule
import org.m0skit0.android.emtmadridalarms.di.domainModule
import org.m0skit0.android.emtmadridalarms.di.presentationModule

class BusAlarmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BusAlarmApplication)
            modules(dataModule, domainModule, presentationModule)
        }
    }
}
