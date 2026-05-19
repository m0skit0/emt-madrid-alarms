package org.m0skit0.android.emtmadridalarms.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
}

@Serializable
data class EmtLoginResponse(
    val code: String? = null,
    val description: String? = null,
    val data: List<EmtLoginData> = emptyList(),
)

@Serializable
data class EmtLoginData(
    val accessToken: String? = null,
    val tokenSecExpiration: Int? = null,
)

@Serializable
data class ArrivalsRequestBody(
    val cultureInfo: String = "EN",
    @SerialName("Text_StopRequired_YN") val stopRequired: String = "Y",
    @SerialName("Text_EstimationsRequired_YN") val estimationsRequired: String = "Y",
    @SerialName("Text_IncidencesRequired_YN") val incidencesRequired: String = "N",
    @SerialName("DateTime_Referenced_Incidencies_YYYYMMDD") val incidencesDate: String = "",
)

@Serializable
data class EmtArrivalsResponse(
    val code: String? = null,
    val description: String? = null,
    val data: List<EmtArrivalsData> = emptyList(),
)

@Serializable
data class EmtArrivalsData(
    @SerialName("Arrive") val arrivals: List<EmtArrivalDto> = emptyList(),
)

@Serializable
data class EmtArrivalDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val line: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val stop: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val destination: String = "",
    @SerialName("estimateArrive")
    @Serializable(with = FlexibleIntSerializer::class)
    val estimateSeconds: Int = 999999,
    @SerialName("DistanceBus")
    @Serializable(with = FlexibleIntSerializer::class)
    val distanceMeters: Int = -1,
)
