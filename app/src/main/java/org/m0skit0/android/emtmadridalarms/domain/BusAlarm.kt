package org.m0skit0.android.emtmadridalarms.domain

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

fun interface LoadBusLinesUseCase : suspend () -> List<BusLine>

fun interface LoadBusStopsUseCase : suspend (BusLine) -> List<BusStop>

fun interface LoadBusArrivalsUseCase : suspend (BusAlarmRequest) -> List<BusArrival>
