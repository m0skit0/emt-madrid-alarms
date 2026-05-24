package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

@Composable
internal fun LineSelectionScreen(
    state: AlarmState,
    dispatch: (AlarmIntent) -> Unit,
    onBack: () -> Unit,
) {
    SearchSelectionScreen(
        title = "Select bus line",
        placeholder = "Search line or destination",
        isLoading = state.isLoadingLines,
        options = state.lines,
        optionText = BusLine::displayName,
        emptyText = "No lines found",
        onBack = onBack,
        onOptionSelected = {
            dispatch(AlarmIntent.LineSelected(it))
            onBack()
        },
    )
}

@Composable
internal fun StopSelectionScreen(
    state: AlarmState,
    dispatch: (AlarmIntent) -> Unit,
    onBack: () -> Unit,
) {
    SearchSelectionScreen(
        title = "Select bus stop",
        placeholder = "Search stop number or name",
        isLoading = state.isLoadingStops,
        options = state.stops,
        optionText = BusStop::displayName,
        emptyText = if (state.selectedLine == null) "Select a line first" else "No stops found",
        onBack = onBack,
        onOptionSelected = {
            dispatch(AlarmIntent.StopSelected(it))
            onBack()
        },
    )
}

@Composable
private fun <T> SearchSelectionScreen(
    title: String,
    placeholder: String,
    isLoading: Boolean,
    options: List<T>,
    optionText: (T) -> String,
    onOptionSelected: (T) -> Unit,
    emptyText: String = "No results",
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val filteredOptions = remember(query, options) {
        fuzzyFilter(options, query, optionText)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(placeholder) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        when {
            isLoading -> SearchResultRow(text = "Loading...")
            filteredOptions.isEmpty() -> SearchResultRow(text = emptyText)
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredOptions) { option ->
                    SearchResultRow(
                        text = optionText(option),
                        onClick = {
                            onOptionSelected(option)
                            keyboardController?.hide()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    text: String,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = clickableModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
