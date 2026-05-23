package org.m0skit0.android.emtmadridalarms.ui

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.domain.LoadBusLinesUseCase

private const val TAG = "LineLoader"

fun interface LineLoader : () -> Unit

internal fun lineLoader(
    loadBusLines: LoadBusLinesUseCase,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): LineLoader = LineLoader {
    scope.launch {
        Log.d(TAG, "Loading bus lines")
        state.update { it.copy(isLoadingLines = true, errorMessage = null) }
        runCatching { loadBusLines() }
            .onSuccess { lines ->
                Log.d(TAG, "Loaded ${lines.size} bus lines")
                state.update { it.copy(lines = lines, isLoadingLines = false) }
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to load bus lines: ${error.message}", error)
                state.update {
                    it.copy(
                        isLoadingLines = false,
                        errorMessage = error.message ?: "Could not load EMT bus lines.",
                    )
                }
            }
    }
}
