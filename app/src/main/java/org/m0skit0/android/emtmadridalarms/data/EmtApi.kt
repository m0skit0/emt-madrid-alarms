package org.m0skit0.android.emtmadridalarms.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface EmtApi {
    @GET("v2/mobilitylabs/user/login/")
    suspend fun login(
        @Header("email") email: String?,
        @Header("password") password: String?,
        @Header("X-ClientId") clientId: String?,
        @Header("passKey") passKey: String?,
    ): EmtLoginResponse

    @POST("v2/transport/busemtmad/stops/{stopId}/arrives/{lineArrive}/")
    suspend fun arrivals(
        @Header("accessToken") accessToken: String,
        @Path("stopId") stopId: String,
        @Path("lineArrive") lineArrive: String,
        @Body body: ArrivalsRequestBody,
    ): EmtArrivalsResponse

    @GET("v2/transport/busemtmad/lines/info/{dateRef}/")
    suspend fun lines(
        @Header("accessToken") accessToken: String,
        @Path("dateRef") dateRef: String,
    ): EmtLinesResponse

    @GET("v1/transport/busemtmad/lines/{lineId}/stops/{direction}/")
    suspend fun lineStops(
        @Header("accessToken") accessToken: String,
        @Path("lineId") lineId: String,
        @Path("direction") direction: Int,
    ): EmtLineStopsResponse

    @POST("v1/transport/busemtmad/stops/list/")
    suspend fun stopsList(
        @Header("accessToken") accessToken: String,
        @Body body: List<String> = emptyList(),
    ): EmtStopsListResponse
}
