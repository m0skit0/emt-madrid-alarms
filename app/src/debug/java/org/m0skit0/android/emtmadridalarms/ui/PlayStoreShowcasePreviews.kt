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

internal enum class PlayStoreShowcaseScene(val value: String) {
    Setup("setup"),
    Lines("lines"),
    Stops("stops"),
    Monitoring("monitoring"),
    Scheduled("scheduled"),
    Ringing("ringing");

    companion object {
        fun fromValue(value: String): PlayStoreShowcaseScene =
            entries.firstOrNull { it.value == value } ?: Setup
    }
}

internal enum class PlayStoreShowcaseMode(val value: String) {
    Phone("phone"),
    Tablet("tablet");

    companion object {
        fun fromValue(value: String): PlayStoreShowcaseMode =
            entries.firstOrNull { it.value == value } ?: Phone
    }
}

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
    isSetupHeaderDismissed = false,
)

private val showcaseMonitoringState = showcaseSetupState.copy(
    activeAlarm = BusAlarmRequest(line = "34", stopId = "5625", targetMinutes = 8),
    latestEtaSeconds = 420,
    latestDestination = "Las Aguilas",
    statusMessage = "Última actualización hace un momento.",
)

private val showcaseRingingState = showcaseMonitoringState.copy(
    isRinging = true,
    ringingAlarm = BusAlarmRequest(line = "34", stopId = "5625", targetMinutes = 8),
    latestEtaSeconds = 180,
)

@Preview(name = "Play 1", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun SetupShowcasePreview() {
    PlayStoreShowcaseScreen(scene = PlayStoreShowcaseScene.Setup)
}

@Preview(name = "Play 2", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun LinesShowcasePreview() {
    PlayStoreShowcaseScreen(scene = PlayStoreShowcaseScene.Lines)
}

@Preview(name = "Play 3", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun StopsShowcasePreview() {
    PlayStoreShowcaseScreen(scene = PlayStoreShowcaseScene.Stops)
}

@Preview(name = "Play 4", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun MonitoringShowcasePreview() {
    PlayStoreShowcaseScreen(scene = PlayStoreShowcaseScene.Monitoring)
}

@Preview(name = "Play 5", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun ScheduledShowcasePreview() {
    PlayStoreShowcaseScreen(scene = PlayStoreShowcaseScene.Scheduled)
}

@Preview(name = "Play 6", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun RingingShowcasePreview() {
    PlayStoreShowcaseScreen(scene = PlayStoreShowcaseScene.Ringing)
}

@Composable
internal fun PlayStoreShowcaseScreen(
    scene: PlayStoreShowcaseScene,
    mode: PlayStoreShowcaseMode = PlayStoreShowcaseMode.Phone,
) {
    val content: @Composable () -> Unit = {
        when (scene) {
            PlayStoreShowcaseScene.Setup -> SetupScreen(
                state = showcaseSetupState,
                dispatch = {},
                onSelectLine = {},
                onSelectStop = {},
            )

            PlayStoreShowcaseScene.Lines -> LineSelectionScreen(
                state = showcaseSetupState,
                dispatch = {},
                onBack = {},
                autoFocus = false,
            )

            PlayStoreShowcaseScene.Stops -> StopSelectionScreen(
                state = showcaseSetupState,
                dispatch = {},
                onBack = {},
                autoFocus = false,
            )

            PlayStoreShowcaseScene.Monitoring -> MonitoringScreen(
                state = showcaseMonitoringState,
                dispatch = {},
            )

            PlayStoreShowcaseScene.Scheduled -> SetupScreen(
                state = showcaseSetupState,
                dispatch = {},
                onSelectLine = {},
                onSelectStop = {},
                initialSelectedTab = 1,
            )

            PlayStoreShowcaseScene.Ringing -> RingingScreen(
                state = showcaseRingingState,
                dispatch = {},
            )
        }
    }

    when (mode) {
        PlayStoreShowcaseMode.Phone -> PhoneShowcaseFrame(
            title = when (scene) {
                PlayStoreShowcaseScene.Setup -> "Configura alertas en segundos"
                PlayStoreShowcaseScene.Lines -> "Busca por línea o destino"
                PlayStoreShowcaseScene.Stops -> "Confirma la parada exacta"
                PlayStoreShowcaseScene.Monitoring -> "Sigue la llegada en tiempo real"
                PlayStoreShowcaseScene.Scheduled -> "Gestiona varias alarmas"
                PlayStoreShowcaseScene.Ringing -> "Recibe el aviso final"
            },
            subtitle = when (scene) {
                PlayStoreShowcaseScene.Setup -> "Elige línea, parada y margen de llegada desde una sola pantalla."
                PlayStoreShowcaseScene.Lines -> "Encuentra rápidamente la ruta correcta antes de salir de casa."
                PlayStoreShowcaseScene.Stops -> "Filtra por número o nombre para evitar errores al programar la alarma."
                PlayStoreShowcaseScene.Monitoring -> "Consulta la estimación más reciente de EMT mientras el sistema monitoriza tu trayecto."
                PlayStoreShowcaseScene.Scheduled -> "Activa, pausa o elimina avisos programados para distintas líneas y paradas."
                PlayStoreShowcaseScene.Ringing -> "La app te alerta cuando el autobús entra en tu margen de llegada configurado."
            },
            content = content,
        )

        PlayStoreShowcaseMode.Tablet -> TabletShowcaseFrame(content = content)
    }
}

@Composable
private fun PhoneShowcaseFrame(
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

@Composable
private fun TabletShowcaseFrame(content: @Composable () -> Unit) {
    EmtTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            content()
        }
    }
}
