package org.m0skit0.android.emtmadridalarms.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.data.AlarmStateReader
import org.m0skit0.android.emtmadridalarms.data.ApiLoggingInterceptor
import org.m0skit0.android.emtmadridalarms.data.ArrivalsProvider
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.EmtApi
import org.m0skit0.android.emtmadridalarms.data.EmtAuthTokenProvider
import org.m0skit0.android.emtmadridalarms.data.EmtCredentials
import org.m0skit0.android.emtmadridalarms.data.EmtDateProvider
import org.m0skit0.android.emtmadridalarms.data.EmtResponseValidator
import org.m0skit0.android.emtmadridalarms.data.LinesProvider
import org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival
import org.m0skit0.android.emtmadridalarms.data.SaveStatus
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.data.StopsProvider
import org.m0skit0.android.emtmadridalarms.data.alarmStateReader
import org.m0skit0.android.emtmadridalarms.data.arrivalsProvider
import org.m0skit0.android.emtmadridalarms.data.clearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.emtAuthTokenProvider
import org.m0skit0.android.emtmadridalarms.data.emtDateProvider
import org.m0skit0.android.emtmadridalarms.data.emtResponseValidator
import org.m0skit0.android.emtmadridalarms.data.linesProvider
import org.m0skit0.android.emtmadridalarms.data.saveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.saveLatestArrival
import org.m0skit0.android.emtmadridalarms.data.saveStatus
import org.m0skit0.android.emtmadridalarms.data.setRinging
import org.m0skit0.android.emtmadridalarms.data.stopsProvider
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit.SECONDS

val dataModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
        }
    }
    singleOf(::ApiLoggingInterceptor)
    single {
        OkHttpClient.Builder()
            .addInterceptor(get<ApiLoggingInterceptor>())
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
    single<EmtAuthTokenProvider> { emtAuthTokenProvider(get(), get(), get()) }
    single<EmtDateProvider> { emtDateProvider() }
    single<EmtResponseValidator> { emtResponseValidator() }
    single<LinesProvider> { linesProvider(get(), get(), get(), get()) }
    single<StopsProvider> { stopsProvider(get(), get(), get()) }
    single<ArrivalsProvider> { arrivalsProvider(get(), get(), get(), get()) }
    single<AlarmStateReader> { alarmStateReader(androidContext()) }
    single<SaveActiveAlarm> { saveActiveAlarm(androidContext()) }
    single<ClearActiveAlarm> { clearActiveAlarm(androidContext()) }
    single<SaveLatestArrival> { saveLatestArrival(androidContext()) }
    single<SaveStatus> { saveStatus(androidContext()) }
    single<SetRinging> { setRinging(androidContext()) }
}
