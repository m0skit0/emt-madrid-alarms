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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
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

    ErrorSnackbarEffect(state, snackbarHostState, viewModel::dispatch)
    NavigationEffect(state, navController)

    EmtTheme {
        Scaffold(
            topBar = { AppTopBar() },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            AlarmNavHost(
                navController = navController,
                state = state,
                dispatch = viewModel::dispatch,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ErrorSnackbarEffect(
    state: AlarmState,
    snackbarHostState: SnackbarHostState,
    dispatch: (AlarmIntent) -> Unit,
) {
    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        dispatch(AlarmIntent.ErrorShown)
    }
}

@Composable
private fun NavigationEffect(state: AlarmState, navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(state.isRinging) {
        val targetRoute = when {
            state.isRinging -> Routes.RINGING
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
}

@Composable
private fun AlarmNavHost(
    navController: NavController,
    state: AlarmState,
    dispatch: (AlarmIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController as NavHostController,
        startDestination = Routes.SETUP,
        modifier = modifier,
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                state = state,
                dispatch = dispatch,
                onSelectLine = { navController.navigate(Routes.SELECT_LINE) },
                onSelectStop = {
                    dispatch(AlarmIntent.StopPickerOpened)
                    navController.navigate(Routes.SELECT_STOP)
                },
            )
        }
        composable(Routes.SELECT_LINE) {
            LineSelectionScreen(state = state, dispatch = dispatch, onBack = { navController.popBackStack() })
        }
        composable(Routes.SELECT_STOP) {
            StopSelectionScreen(state = state, dispatch = dispatch, onBack = { navController.popBackStack() })
        }
        composable(Routes.MONITORING) {
            MonitoringScreen(state = state, dispatch = dispatch)
        }
        composable(Routes.RINGING) {
            RingingScreen(state = state, dispatch = dispatch)
        }
    }
}
