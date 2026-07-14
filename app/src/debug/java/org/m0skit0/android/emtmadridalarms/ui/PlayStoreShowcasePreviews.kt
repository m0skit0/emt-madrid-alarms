package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

private val showcaseLines = listOf(
    BusLine(id = "34", label = "34", nameA = "Cibeles", nameB = "Las Aguilas"),
    BusLine(id = "27", label = "27", nameA = "Plaza Castilla", nameB = "Embajadores"),
    BusLine(id = "6", label = "6", nameA = "Benavente", nameB = "Orcasitas"),
)

private val showcaseStops = listOf(
    BusStop(id = "5625", name = "Avenida de Portugal", address = "Avenida de Portugal, 155", lineLabels = setOf("34")),
    BusStop(id = "73", name = "Cibeles", address = "Plaza de Cibeles", lineLabels = setOf("34", "27")),
    BusStop(id = "4012", name = "Metro Aluche", address = "Avenida de los Poblados", lineLabels = setOf("34")),
)

private val showcaseSetupState = AlarmState(
    minutesInput = "8",
    allLines = showcaseLines,
    allStops = showcaseStops,
    lines = showcaseLines,
    stops = showcaseStops,
    selectedLine = showcaseLines.first(),
    selectedStop = showcaseStops.first(),
    activeAlarms = listOf(
        BusAlarmRequest(line = "34", stopId = "5625", targetMinutes = 8),
        BusAlarmRequest(line = "27", stopId = "73", targetMinutes = 5, isEnabled = false),
    ),
)

private val showcaseMonitoringState = showcaseSetupState.copy(
    activeAlarm = BusAlarmRequest(line = "34", stopId = "5625", targetMinutes = 8),
    latestEtaSeconds = 420,
    latestDestination = "Las Aguilas",
    statusMessage = "Ultima actualizacion hace un momento.",
)

private val showcaseRingingState = showcaseMonitoringState.copy(
    isRinging = true,
    ringingAlarm = BusAlarmRequest(line = "34", stopId = "5625", targetMinutes = 8),
    latestEtaSeconds = 180,
)

@Preview(name = "Play 1", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun SetupShowcasePreview() {
    ShowcaseFrame(
        title = "Configura alertas en segundos",
        subtitle = "Elige linea, parada y margen de llegada desde una sola pantalla.",
    ) {
        SetupScreen(
            state = showcaseSetupState,
            dispatch = {},
            onSelectLine = {},
            onSelectStop = {},
            initialSelectedTab = 1,
        )
    }
}

@Preview(name = "Play 2", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun LinesShowcasePreview() {
    ShowcaseFrame(
        title = "Busca por linea o destino",
        subtitle = "Encuentra rapidamente la ruta correcta antes de salir de casa.",
    ) {
        LineSelectionScreen(
            state = showcaseSetupState,
            dispatch = {},
            onBack = {},
        )
    }
}

@Preview(name = "Play 3", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun StopsShowcasePreview() {
    ShowcaseFrame(
        title = "Confirma la parada exacta",
        subtitle = "Filtra por numero o nombre para evitar errores al programar la alarma.",
    ) {
        StopSelectionScreen(
            state = showcaseSetupState,
            dispatch = {},
            onBack = {},
        )
    }
}

@Preview(name = "Play 4", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun MonitoringShowcasePreview() {
    ShowcaseFrame(
        title = "Sigue la llegada en tiempo real",
        subtitle = "Consulta la estimacion mas reciente de EMT mientras el sistema monitoriza tu trayecto.",
    ) {
        MonitoringScreen(
            state = showcaseMonitoringState,
            dispatch = {},
        )
    }
}

@Preview(name = "Play 5", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun ScheduledShowcasePreview() {
    ShowcaseFrame(
        title = "Gestiona varias alarmas",
        subtitle = "Activa, pausa o elimina avisos programados para distintas lineas y paradas.",
    ) {
        SetupScreen(
            state = showcaseSetupState,
            dispatch = {},
            onSelectLine = {},
            onSelectStop = {},
        )
    }
}

@Preview(name = "Play 6", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun RingingShowcasePreview() {
    ShowcaseFrame(
        title = "Recibe el aviso final",
        subtitle = "La app te alerta cuando el autobus entra en tu margen de llegada configurado.",
    ) {
        RingingScreen(
            state = showcaseRingingState,
            dispatch = {},
        )
    }
}

@Composable
private fun ShowcaseFrame(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    EmtTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "EMT Madrid Alarms",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp)),
                    ) {
                        content()
                    }
                }
            }
            Text(
                text = "Alarmas inteligentes para tus trayectos de EMT",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
