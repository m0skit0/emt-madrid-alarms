package org.m0skit0.android.emtmadridalarms.domain

import org.m0skit0.android.emtmadridalarms.data.normalizeLine
import org.m0skit0.android.emtmadridalarms.utils.orDefault

const val MAX_ACTIVE_ALARMS = 5

data class BusAlarmRequest(
    val line: String,
    val stopId: String,
    val targetMinutes: Int,
    val isEnabled: Boolean = true,
)

fun BusAlarmRequest.hasSameLineAndStop(other: BusAlarmRequest): Boolean =
    line == other.line && stopId == other.stopId

data class PersistedAlarmState(
    val activeAlarm: BusAlarmRequest?,
    val latestEtaSeconds: Int?,
    val latestDestination: String,
    val statusMessage: String,
    val isRinging: Boolean,
    val activeAlarms: List<BusAlarmRequest> = activeAlarm?.let { listOf(it) }.orDefault { emptyList() },
    val ringingAlarm: BusAlarmRequest? = null,
    val isSetupHeaderDismissed: Boolean = false,
)

data class BusLine(
    val id: String,
    val label: String,
    val nameA: String,
    val nameB: String,
) {
    val displayName: String = buildString {
        append(label.ifBlank { normalizeLine(id) })
        val route = listOf(nameA, nameB).filter { it.isNotBlank() }.joinToString(" - ")
        if (route.isNotBlank()) append(" · ").append(route)
    }
}

data class BusStop(
    val id: String,
    val name: String,
    val address: String,
    val lineLabels: Set<String> = emptySet(),
) {
    val displayName: String = buildString {
        append(id)
        if (name.isNotBlank()) append(" · ").append(name)
        if (address.isNotBlank()) append(" · ").append(address.trim())
    }
}

data class BusArrival(
    val line: String,
    val stopId: String,
    val destination: String,
    val estimateSeconds: Int,
    val distanceMeters: Int,
) {
    val estimateMinutes: Int = estimateSeconds.floorDiv(60)
}
