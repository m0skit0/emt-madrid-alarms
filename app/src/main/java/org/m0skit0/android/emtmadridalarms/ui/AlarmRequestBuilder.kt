package org.m0skit0.android.emtmadridalarms.ui

import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest

fun interface AlarmRequestValidator : (String, String, String) -> String?

fun alarmRequestValidator(
    enterBusLineMessage: String = "Enter a bus line.",
    enterStopNumberMessage: String = "Enter a stop number.",
    stopDigitsMessage: String = "Stop number must contain only digits.",
    positiveMinutesMessage: String = "Minutes must be a positive number.",
): AlarmRequestValidator = AlarmRequestValidator { line, stopId, minutes ->
    when {
        line.isBlank() -> enterBusLineMessage
        stopId.isBlank() -> enterStopNumberMessage
        !stopId.all(Char::isDigit) -> stopDigitsMessage
        else -> {
            val targetMinutes = minutes.toIntOrNull()
            if (targetMinutes == null || targetMinutes <= 0) positiveMinutesMessage else null
        }
    }
}

fun interface AlarmRequestBuilder : (AlarmState) -> Result<BusAlarmRequest>

fun alarmRequestBuilder(
    validator: AlarmRequestValidator,
    selectBusLineMessage: String = "Select a bus line from the list.",
    selectStopForLineMessage: String = "Select a stop for the selected line.",
): AlarmRequestBuilder =
    AlarmRequestBuilder { current ->
        val line = current.selectedLine
        val stop = current.selectedStop
        val error = when {
            line == null -> selectBusLineMessage
            stop == null -> selectStopForLineMessage
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
