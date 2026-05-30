package org.m0skit0.android.emtmadridalarms.domain

import org.m0skit0.android.emtmadridalarms.data.normalizeLine

data class BusAlarmRequest(
    val line: String,
    val stopId: String,
    val targetMinutes: Int,
)

data class PersistedAlarmState(
    val activeAlarm: BusAlarmRequest?,
    val latestEtaSeconds: Int?,
    val latestDestination: String,
    val statusMessage: String,
    val isRinging: Boolean,
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
