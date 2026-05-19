package org.m0skit0.android.emtmadridalarms

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
        if (currentRoute != targetRoute) {
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
                    SetupScreen(state = state, dispatch = viewModel::dispatch)
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
private fun SetupScreen(state: AlarmState, dispatch: (AlarmIntent) -> Unit) {
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
            text = "Enter a line, stop number, and how many minutes before arrival should trigger the alarm.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.lineInput,
            onValueChange = { dispatch(AlarmIntent.LineChanged(it)) },
            label = { Text("Bus line") },
            placeholder = { Text("1, 27, N1...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.stopInput,
            onValueChange = { dispatch(AlarmIntent.StopChanged(it.filter(Char::isDigit))) },
            label = { Text("Stop number") },
            placeholder = { Text("Example: 62") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
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
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isLoading) "Starting..." else "Start alarm")
        }
    }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
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
    const val MONITORING = "monitoring"
    const val RINGING = "ringing"
}
