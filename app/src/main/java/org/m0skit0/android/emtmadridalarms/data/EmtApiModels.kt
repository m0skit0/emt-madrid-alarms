package org.m0skit0.android.emtmadridalarms.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

@Serializable
data class EmtLinesResponse(
    val code: String? = null,
    val description: String? = null,
    val data: List<EmtLineDto> = emptyList(),
)

@Serializable
data class EmtLineDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val line: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val label: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val nameA: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val nameB: String = "",
)

@Serializable
data class EmtLineStopsResponse(
    val code: String? = null,
    val description: String? = null,
    val data: List<EmtLineStopsData> = emptyList(),
)

@Serializable
data class EmtStopsListResponse(
    val code: String? = null,
    val description: String? = null,
    val data: List<EmtStopListDto> = emptyList(),
)

@Serializable
data class EmtStopListDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val node: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val name: String = "",
    val lines: List<String> = emptyList(),
)

@Serializable
data class EmtLineStopsData(
    val stops: List<EmtStopDto> = emptyList(),
)

@Serializable
data class EmtStopDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val stop: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val name: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val postalAddress: String = "",
)
