package org.m0skit0.android.emtmadridalarms.domain

data class BusAlarmRequest(
    val line: String,
    val stopId: String,
    val targetMinutes: Int,
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

fun normalizeLine(line: String): String {
    val value = line.trim().uppercase()
    if (value.isEmpty()) return value
    if (value.all(Char::isDigit)) return value.trimStart('0').ifEmpty { "0" }

    val match = Regex("^([A-Z]+)0+([0-9]+)$").matchEntire(value)
    return if (match != null) {
        match.groupValues[1] + match.groupValues[2].trimStart('0').ifEmpty { "0" }
    } else {
        value
    }
}

fun linesMatch(userLine: String, apiLine: String): Boolean = normalizeLine(userLine) == normalizeLine(apiLine)

fun shouldTriggerAlarm(estimateSeconds: Int, targetMinutes: Int): Boolean {
    if (targetMinutes <= 0) return false
    if (estimateSeconds < 0 || estimateSeconds == 999999) return false
    return estimateSeconds <= targetMinutes * 60
}

fun validateAlarmRequest(line: String, stopId: String, minutes: String): String? {
    if (line.isBlank()) return "Enter a bus line."
    if (stopId.isBlank()) return "Enter a stop number."
    if (!stopId.all(Char::isDigit)) return "Stop number must contain only digits."
    val targetMinutes = minutes.toIntOrNull()
    if (targetMinutes == null || targetMinutes <= 0) return "Minutes must be a positive number."
    return null
}
