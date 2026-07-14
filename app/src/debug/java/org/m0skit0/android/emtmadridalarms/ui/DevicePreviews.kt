package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Preview(name = "Small phone", group = "Devices", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "7-inch tablet", group = "Devices", widthDp = 600, heightDp = 960, showBackground = true)
@Preview(name = "10-inch tablet", group = "Devices", widthDp = 800, heightDp = 1280, showBackground = true)
private annotation class DevicePreviews

private val previewLines = listOf(
    BusLine(id = "34", label = "34", nameA = "Cibeles", nameB = "Las Águilas"),
    BusLine(id = "27", label = "27", nameA = "Plaza Castilla", nameB = "Embajadores"),
    BusLine(id = "6", label = "6", nameA = "Benavente", nameB = "Orcasitas"),
)

private val previewStops = listOf(
    BusStop(id = "5625", name = "Avenida de Portugal", address = "Avenida de Portugal, 155", lineLabels = setOf("34")),
    BusStop(id = "73", name = "Cibeles", address = "Plaza de Cibeles", lineLabels = setOf("34", "27")),
    BusStop(id = "4012", name = "Metro Aluche", address = "Avenida de los Poblados", lineLabels = setOf("34")),
)

private val previewSetupState = AlarmState(
    minutesInput = "10",
    allLines = previewLines,
    allStops = previewStops,
    lines = previewLines,
    stops = previewStops,
    selectedLine = previewLines.first(),
    selectedStop = previewStops.first(),
)

private val previewMonitoringState = previewSetupState.copy(
    activeAlarm = BusAlarmRequest(line = "34", stopId = "5625", targetMinutes = 10),
    latestEtaSeconds = 720,
    latestDestination = "Las Águilas",
)

private val previewRingingState = previewMonitoringState.copy(
    activeAlarm = null,
    latestEtaSeconds = 540,
    isRinging = true,
)

@DevicePreviews
@Composable
private fun SetupScreenPreview() {
    PreviewSurface {
        SetupScreen(
            state = previewSetupState,
            dispatch = {},
            onSelectLine = {},
            onSelectStop = {},
        )
    }
}

@DevicePreviews
@Composable
private fun LineSelectionScreenPreview() {
    PreviewSurface {
        LineSelectionScreen(
            state = previewSetupState,
            dispatch = {},
            onBack = {},
        )
    }
}

@DevicePreviews
@Composable
private fun StopSelectionScreenPreview() {
    PreviewSurface {
        StopSelectionScreen(
            state = previewSetupState,
            dispatch = {},
            onBack = {},
        )
    }
}

@DevicePreviews
@Composable
private fun MonitoringScreenPreview() {
    PreviewSurface {
        MonitoringScreen(
            state = previewMonitoringState,
            dispatch = {},
        )
    }
}

@DevicePreviews
@Composable
private fun RingingScreenPreview() {
    PreviewSurface {
        RingingScreen(
            state = previewRingingState,
            dispatch = {},
        )
    }
}

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    EmtTheme(content = content)
}
