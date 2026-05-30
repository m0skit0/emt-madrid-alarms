package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.m0skit0.android.emtmadridalarms.R

@Composable
internal fun SetupScreen(
    state: AlarmState,
    dispatch: (AlarmIntent) -> Unit,
    onSelectLine: () -> Unit,
    onSelectStop: () -> Unit,
) {
    ScreenColumn {
        SetupHeader()
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

@Composable
private fun SetupHeader() {
    Image(
        painter = painterResource(R.drawable.logo),
        contentDescription = null,
        modifier = Modifier.size(96.dp),
    )
    Text(
        text = stringResource(R.string.setup_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(R.string.setup_description),
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(12.dp))
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
        enabled = !state.isLoading && !state.isLoadingLines && !state.isLoadingStops && state.selectedLine != null && state.selectedStop != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (state.isLoading) stringResource(R.string.button_starting) else stringResource(R.string.button_start_alarm))
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
