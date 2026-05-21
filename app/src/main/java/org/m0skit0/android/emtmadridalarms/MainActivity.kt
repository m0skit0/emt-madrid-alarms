package org.m0skit0.android.emtmadridalarms

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.ui.AlarmIntent
import org.m0skit0.android.emtmadridalarms.ui.AlarmState
import org.m0skit0.android.emtmadridalarms.ui.AlarmViewModel

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent { EmtMadridAlarmsApp() }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (permissionState != PermissionChecker.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun EmtMadridAlarmsApp(viewModel: AlarmViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dispatch(AlarmIntent.ErrorShown)
    }

    LaunchedEffect(state.isRinging, state.activeAlarm) {
        val targetRoute = when {
            state.isRinging -> Routes.RINGING
            state.activeAlarm != null -> Routes.MONITORING
            else -> Routes.SETUP
        }
        val currentRoute = backStackEntry?.destination?.route
        if (currentRoute in Routes.ALARM_ROUTES && currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    EmtTheme {
        Scaffold(
            topBar = { AppTopBar() },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.SETUP,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.SETUP) {
                    SetupScreen(
                        state = state,
                        dispatch = viewModel::dispatch,
                        onSelectLine = { navController.navigate(Routes.SELECT_LINE) },
                        onSelectStop = { navController.navigate(Routes.SELECT_STOP) },
                    )
                }
                composable(Routes.SELECT_LINE) {
                    LineSelectionScreen(
                        state = state,
                        dispatch = viewModel::dispatch,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.SELECT_STOP) {
                    StopSelectionScreen(
                        state = state,
                        dispatch = viewModel::dispatch,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.MONITORING) {
                    MonitoringScreen(state = state, dispatch = viewModel::dispatch)
                }
                composable(Routes.RINGING) {
                    RingingScreen(state = state, dispatch = viewModel::dispatch)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar() {
    TopAppBar(title = { Text("EMT Madrid Alarms") })
}

@Composable
private fun SetupScreen(
    state: AlarmState,
    dispatch: (AlarmIntent) -> Unit,
    onSelectLine: () -> Unit,
    onSelectStop: () -> Unit,
) {
    ScreenColumn {
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

@Composable
private fun LineSelectionScreen(
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
private fun StopSelectionScreen(
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
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            options
        } else {
            options.filter { optionText(it).contains(trimmedQuery, ignoreCase = true) }
        }
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

@Composable
private fun MonitoringScreen(state: AlarmState, dispatch: (AlarmIntent) -> Unit) {
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
private fun RingingScreen(state: AlarmState, dispatch: (AlarmIntent) -> Unit) {
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

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun EmtTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

private object Routes {
    const val SETUP = "setup"
    const val SELECT_LINE = "select-line"
    const val SELECT_STOP = "select-stop"
    const val MONITORING = "monitoring"
    const val RINGING = "ringing"

    val ALARM_ROUTES = setOf(SETUP, MONITORING, RINGING)
}
