package org.m0skit0.android.emtmadridalarms.ui

import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest

fun interface AlarmRequestValidator : (String, String, String) -> String?

fun alarmRequestValidator(): AlarmRequestValidator = AlarmRequestValidator { line, stopId, minutes ->
    when {
        line.isBlank() -> "Enter a bus line."
        stopId.isBlank() -> "Enter a stop number."
        !stopId.all(Char::isDigit) -> "Stop number must contain only digits."
        else -> {
            val targetMinutes = minutes.toIntOrNull()
            if (targetMinutes == null || targetMinutes <= 0) "Minutes must be a positive number." else null
        }
    }
}

fun interface AlarmRequestBuilder : (AlarmState) -> Result<BusAlarmRequest>

fun alarmRequestBuilder(validator: AlarmRequestValidator): AlarmRequestBuilder =
    AlarmRequestBuilder { current ->
        val line = current.selectedLine
        val stop = current.selectedStop
        val error = when {
            line == null -> "Select a bus line from the list."
            stop == null -> "Select a stop for the selected line."
            else -> validator(line.label, stop.id, current.minutesInput)
        }
        if (error != null) return@AlarmRequestBuilder Result.failure(IllegalArgumentException(error))
        Result.success(
            BusAlarmRequest(
                line = line!!.label,
                stopId = stop!!.id,
                targetMinutes = current.minutesInput.toInt(),
            )
        )
    }
