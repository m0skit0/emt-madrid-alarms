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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.m0skit0.android.emtmadridalarms.R
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest

@Composable
internal fun MonitoringScreen(state: AlarmState, dispatch: (AlarmIntent) -> Unit) {
    ScreenColumn {
        Text(
            text = stringResource(R.string.title_monitoring),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        if (state.activeAlarm == null) {
            Text(stringResource(R.string.text_no_active_alarm))
        } else {
            AlarmDetailsCard(state.activeAlarm)
        }
        EtaStatusCard(state)
        OutlinedButton(
            onClick = { dispatch(AlarmIntent.CancelClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.button_cancel_alarm))
        }
    }
}

@Composable
private fun AlarmDetailsCard(alarm: BusAlarmRequest) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.text_line, alarm.line), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.text_stop, alarm.stopId))
            Text(stringResource(R.string.text_alarm_triggers, alarm.targetMinutes))
        }
    }
}

@Composable
private fun EtaStatusCard(state: AlarmState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.text_latest_estimate), style = MaterialTheme.typography.titleMedium)
            val eta = state.latestEtaSeconds
            Text(if (eta == null) stringResource(R.string.text_waiting_arrival) else stringResource(R.string.text_eta_format, eta / 60, eta % 60))
            if (state.latestDestination.isNotBlank()) Text(stringResource(R.string.text_destination, state.latestDestination))
            if (state.statusMessage.isNotBlank()) Text(
                state.statusMessage,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun RingingScreen(state: AlarmState, dispatch: (AlarmIntent) -> Unit) {
    ScreenColumn {
        Text(
            text = stringResource(R.string.title_ringing),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.text_bus_arriving),
            style = MaterialTheme.typography.bodyLarge,
        )
        state.ringingAlarm?.let { AlarmDetailsCard(it) }
        if (state.latestEtaSeconds != null) {
            Text(stringResource(R.string.text_last_estimate, state.latestEtaSeconds / 60, state.latestEtaSeconds % 60))
        }
        Button(
            onClick = { dispatch(AlarmIntent.StopRingingClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.button_stop_alarm))
        }
    }
}
