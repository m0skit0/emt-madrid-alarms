package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun EmtMadridAlarmsApp(viewModel: AlarmViewModel = koinViewModel()) {
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
