package org.m0skit0.android.emtmadridalarms.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.data.AlarmStorage
import org.m0skit0.android.emtmadridalarms.data.ApiLoggingInterceptor
import org.m0skit0.android.emtmadridalarms.data.EmtApi
import org.m0skit0.android.emtmadridalarms.data.EmtAuthTokenProvider
import org.m0skit0.android.emtmadridalarms.data.EmtCredentials
import org.m0skit0.android.emtmadridalarms.data.ArrivalsProvider
import org.m0skit0.android.emtmadridalarms.data.arrivalsFor
import org.m0skit0.android.emtmadridalarms.data.EmtDateProvider
import org.m0skit0.android.emtmadridalarms.data.EmtLineService
import org.m0skit0.android.emtmadridalarms.data.EmtRepository
import org.m0skit0.android.emtmadridalarms.data.StopsProvider
import org.m0skit0.android.emtmadridalarms.data.stopsForLine
import org.m0skit0.android.emtmadridalarms.data.todayDateRef
import org.m0skit0.android.emtmadridalarms.domain.AlarmStateStore
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRepository
import retrofit2.Retrofit

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
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
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
    singleOf(::EmtAuthTokenProvider)
    single<EmtDateProvider> { EmtDateProvider { todayDateRef() } }
    singleOf(::EmtLineService)
    single<StopsProvider> { StopsProvider { busLine -> stopsForLine(busLine, get(), get()) } }
    single<ArrivalsProvider> { ArrivalsProvider { request -> arrivalsFor(request, get(), get(), get()) } }
    singleOf(::EmtRepository) bind BusAlarmRepository::class
    single { AlarmStorage(androidContext()) } bind AlarmStateStore::class
}
