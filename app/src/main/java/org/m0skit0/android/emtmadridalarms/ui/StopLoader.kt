package org.m0skit0.android.emtmadridalarms.ui

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.LoadBusStopsUseCase

private const val TAG = "StopLoader"

fun interface StopLoader : (BusLine) -> Unit

internal fun stopLoader(
    loadBusStops: LoadBusStopsUseCase,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): StopLoader = StopLoader { line ->
    scope.launch {
        Log.d(TAG, "Loading stops for line=${line.label}")
        runCatching { loadBusStops(line) }
            .onSuccess { stops ->
                Log.d(TAG, "Loaded ${stops.size} stops for line=${line.label}")
                state.update { it.copy(stops = stops, isLoadingStops = false) }
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to load stops for line=${line.label}: ${error.message}", error)
                state.update {
                    it.copy(
                        isLoadingStops = false,
                        errorMessage = error.message ?: "Could not load stops for line ${line.label}.",
                    )
                }
            }
    }
}
