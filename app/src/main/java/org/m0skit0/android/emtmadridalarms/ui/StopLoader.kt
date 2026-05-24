package org.m0skit0.android.emtmadridalarms.ui

import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.LoadBusStopsUseCase

fun interface StopLoader : (BusLine) -> Unit

internal fun stopLoader(
    loadBusStops: LoadBusStopsUseCase,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): StopLoader = StopLoader { line ->
    scope.launch {
        Timber.d("Loading stops for line=${line.label}")
        runCatching { loadBusStops(line) }
            .onSuccess { stops ->
                Timber.d("Loaded ${stops.size} stops for line=${line.label}")
                state.update { it.copy(stops = stops, isLoadingStops = false) }
            }
            .onFailure { error ->
                Timber.e(error, "Failed to load stops for line=${line.label}: ${error.message}")
                state.update {
                    it.copy(
                        isLoadingStops = false,
                        errorMessage = error.message ?: "Could not load stops for line ${line.label}.",
                    )
                }
            }
    }
}
