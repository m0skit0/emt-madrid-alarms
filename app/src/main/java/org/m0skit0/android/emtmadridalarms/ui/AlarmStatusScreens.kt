package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun MonitoringScreen(state: AlarmState, dispatch: (AlarmIntent) -> Unit) {
    val alarm = state.activeAlarm
    ScreenColumn {
        Text(
            text = "Monitoring",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        if (alarm == null) {
            Text("No active alarm.")
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Line ${alarm.line}", style = MaterialTheme.typography.titleLarge)
                    Text("Stop ${alarm.stopId}")
                    Text("Alarm triggers at ${alarm.targetMinutes} minutes or less")
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Latest EMT estimate", style = MaterialTheme.typography.titleMedium)
                val eta = state.latestEtaSeconds
                Text(if (eta == null) "Waiting for a matching arrival..." else "${eta / 60} min ${eta % 60} sec")
                if (state.latestDestination.isNotBlank()) Text("Destination: ${state.latestDestination}")
                if (state.statusMessage.isNotBlank()) Text(state.statusMessage, style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(
            onClick = { dispatch(AlarmIntent.CancelClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancel alarm")
        }
    }
}

@Composable
internal fun RingingScreen(state: AlarmState, dispatch: (AlarmIntent) -> Unit) {
    ScreenColumn {
        Text(
            text = "Bus arriving",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "The monitored bus is within your configured arrival window.",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.latestEtaSeconds != null) {
            Text("Last estimate: ${state.latestEtaSeconds / 60} min ${state.latestEtaSeconds % 60} sec")
        }
        Button(
            onClick = { dispatch(AlarmIntent.StopRingingClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Stop alarm")
        }
    }
}
