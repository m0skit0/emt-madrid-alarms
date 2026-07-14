package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.m0skit0.android.emtmadridalarms.R
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.MAX_ACTIVE_ALARMS

@Composable
internal fun SetupScreen(
    state: AlarmState,
    dispatch: (AlarmIntent) -> Unit,
    onSelectLine: () -> Unit,
    onSelectStop: () -> Unit,
    initialSelectedTab: Int = 0,
) {
    var selectedTab by remember(initialSelectedTab) { mutableIntStateOf(initialSelectedTab) }
    ScreenColumn {
        if (!state.isSetupHeaderDismissed) {
            SetupHeader(onDismiss = { dispatch(AlarmIntent.SetupHeaderDismissed) })
        }
        SetupTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        when (selectedTab) {
            0 -> AddAlarmTab(state, dispatch, onSelectLine, onSelectStop)
            else -> ScheduledAlarmsTab(state.activeAlarms, dispatch)
        }
    }
}

@Composable
private fun SetupTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = { Text(stringResource(R.string.tab_add_alarm)) },
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = { Text(stringResource(R.string.tab_scheduled_alarms)) },
        )
    }
}

@Composable
private fun AddAlarmTab(
    state: AlarmState,
    dispatch: (AlarmIntent) -> Unit,
    onSelectLine: () -> Unit,
    onSelectStop: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectionSummaryCard(state)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PickerField(
                    value = state.selectedLine?.displayName.orEmpty(),
                    label = stringResource(R.string.label_bus_line),
                    placeholder = if (state.isLoadingLines) stringResource(R.string.placeholder_loading_lines) else stringResource(R.string.placeholder_select_line),
                    enabled = !state.isLoadingLines,
                    onClick = onSelectLine,
                )
                PickerField(
                    value = state.selectedStop?.displayName.orEmpty(),
                    label = stringResource(R.string.label_bus_stop),
                    placeholder = when {
                        state.isLoadingStops -> stringResource(R.string.placeholder_loading_stops)
                        else -> stringResource(R.string.placeholder_select_stop)
                    },
                    enabled = !state.isLoadingStops,
                    onClick = onSelectStop,
                )
                AlarmInputControls(state, dispatch)
            }
        }
    }
}

@Composable
private fun ScheduledAlarmsTab(activeAlarms: List<BusAlarmRequest>, dispatch: (AlarmIntent) -> Unit) {
    ActiveAlarmsCard(activeAlarms, dispatch)
}

@Composable
private fun ActiveAlarmsCard(activeAlarms: List<BusAlarmRequest>, dispatch: (AlarmIntent) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.title_active_alarms, activeAlarms.size, MAX_ACTIVE_ALARMS),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (activeAlarms.isEmpty()) {
                Text(stringResource(R.string.text_no_active_alarm))
            } else {
                activeAlarms.forEach { alarm ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.text_active_alarm_item, alarm.line, alarm.stopId, alarm.targetMinutes),
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Switch(
                            checked = alarm.isEnabled,
                            onCheckedChange = { enabled ->
                                dispatch(AlarmIntent.ToggleAlarmEnabled(alarm, enabled))
                            },
                        )
                        IconButton(
                            onClick = { dispatch(AlarmIntent.CancelAlarmClicked(alarm)) },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { dispatch(AlarmIntent.CancelClicked) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.button_delete_all_alarms))
                }
            }
        }
    }
}

@Composable
private fun SetupHeader(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.setup_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.setup_header_dismiss),
                    )
                }
            }
            Text(
                text = stringResource(R.string.setup_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SetupFactChip(stringResource(R.string.label_bus_line))
                SetupFactChip(stringResource(R.string.label_bus_stop))
                SetupFactChip(stringResource(R.string.label_trigger_minutes))
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun SetupFactChip(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SelectionSummaryCard(state: AlarmState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.title_active_alarms, state.activeAlarms.size, MAX_ACTIVE_ALARMS),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = state.selectedLine?.displayName.orEmpty().ifBlank { stringResource(R.string.placeholder_select_line) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = state.selectedStop?.displayName.orEmpty().ifBlank { stringResource(R.string.placeholder_select_stop) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.text_alarm_triggers, state.minutesInput.toIntOrNull() ?: 10),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun AlarmInputControls(state: AlarmState, dispatch: (AlarmIntent) -> Unit) {
    if (!state.isLoadingLines && state.lines.isEmpty()) {
        OutlinedButton(
            onClick = { dispatch(AlarmIntent.RefreshLinesClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.button_reload_lines))
        }
    }
    OutlinedTextField(
        value = state.minutesInput,
        onValueChange = { dispatch(AlarmIntent.MinutesChanged(it)) },
        label = { Text(stringResource(R.string.label_trigger_minutes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { dispatch(AlarmIntent.StartClicked) },
        enabled = !state.isLoading &&
                !state.isLoadingLines &&
                !state.isLoadingStops &&
                state.selectedLine != null &&
                state.selectedStop != null &&
                state.activeAlarms.size < MAX_ACTIVE_ALARMS,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (state.isLoading) stringResource(R.string.button_starting) else stringResource(R.string.button_add_alarm))
    }
}

@Composable
private fun PickerField(
    value: String,
    label: String,
    placeholder: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled, onClick = onClick),
        )
    }
}
