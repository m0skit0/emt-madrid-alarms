package org.m0skit0.android.emtmadridalarms

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import org.m0skit0.android.emtmadridalarms.di.dataModule
import org.m0skit0.android.emtmadridalarms.di.domainModule
import org.m0skit0.android.emtmadridalarms.di.presentationModule
import org.m0skit0.android.emtmadridalarms.di.serviceModule
import org.m0skit0.android.emtmadridalarms.di.stateModule

class BusAlarmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin {
            androidContext(this@BusAlarmApplication)
            modules(dataModule, domainModule, presentationModule, serviceModule, stateModule)
        }
    }
}
