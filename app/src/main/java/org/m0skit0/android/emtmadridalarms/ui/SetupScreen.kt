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
            label = "Bus line",
            placeholder = if (state.isLoadingLines) "Loading lines..." else "Select bus line",
            enabled = !state.isLoadingLines,
            onClick = onSelectLine,
        )
        PickerField(
            value = state.selectedStop?.displayName.orEmpty(),
            label = "Bus stop",
            placeholder = when {
                state.selectedLine == null -> "Select a line first"
                state.isLoadingStops -> "Loading stops..."
                else -> "Select bus stop"
            },
            enabled = state.selectedLine != null,
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
        text = "Wake me when my bus is close",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Search for a line, then pick one of the stops served by that line.",
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
            Text("Reload bus lines")
        }
    }
    OutlinedTextField(
        value = state.minutesInput,
        onValueChange = { dispatch(AlarmIntent.MinutesChanged(it)) },
        label = { Text("Trigger at minutes before arrival") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { dispatch(AlarmIntent.StartClicked) },
        enabled = !state.isLoading && !state.isLoadingStops && state.selectedLine != null && state.selectedStop != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (state.isLoading) "Starting..." else "Start alarm")
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
