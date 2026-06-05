package org.m0skit0.android.emtmadridalarms.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.data.EmtApi
import org.m0skit0.android.emtmadridalarms.data.EmtCredentials
import org.m0skit0.android.emtmadridalarms.data.alarmStateReader
import org.m0skit0.android.emtmadridalarms.data.allStops
import org.m0skit0.android.emtmadridalarms.data.arrivalsFor
import org.m0skit0.android.emtmadridalarms.data.clearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.linesForToday
import org.m0skit0.android.emtmadridalarms.data.loggingInterceptor
import org.m0skit0.android.emtmadridalarms.data.provideToken
import org.m0skit0.android.emtmadridalarms.data.removeActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.requireEmtSuccess
import org.m0skit0.android.emtmadridalarms.data.saveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.saveLatestArrival
import org.m0skit0.android.emtmadridalarms.data.saveStatus
import org.m0skit0.android.emtmadridalarms.data.setAlarmEnabled
import org.m0skit0.android.emtmadridalarms.data.setRinging
import org.m0skit0.android.emtmadridalarms.data.stopsForLine
import org.m0skit0.android.emtmadridalarms.data.todayDateRef
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit.SECONDS

private val android.content.Context.alarmDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "bus_alarm")

val dataModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
        }
    }
    single { loggingInterceptor() }
    single {
        OkHttpClient.Builder()
            .addInterceptor(get<Interceptor>())
            .connectTimeout(15, SECONDS)
            .readTimeout(30, SECONDS)
            .callTimeout(40, SECONDS)
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl("https://openapi.emtmadrid.es/")
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single { get<Retrofit>().create(EmtApi::class.java) }
    single { EmtCredentials.fromBuildConfig() }
    single { provideToken(get(), get(), get()) }
    single { todayDateRef() }
    single { requireEmtSuccess() }
    single { linesForToday(get(), get(), get(), get()) }
    single { allStops(get(), get(), get()) }
    single { stopsForLine(get(), get(), get()) }
    single { arrivalsFor(get(), get(), get(), get()) }
    single<DataStore<Preferences>> { androidContext().alarmDataStore }
    single { alarmStateReader(get()) }
    single { saveActiveAlarm(get()) }
    single { removeActiveAlarm(get()) }
    single { clearActiveAlarm(get()) }
    single { saveLatestArrival(get()) }
    single { saveStatus(get()) }
    single { setRinging(get()) }
    single { setAlarmEnabled(get()) }
}
