package org.m0skit0.android.emtmadridalarms.ui

import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.m0skit0.android.emtmadridalarms.domain.LoadBusLinesUseCase

fun interface LineLoader : () -> Unit

internal fun lineLoader(
    loadBusLines: LoadBusLinesUseCase,
    state: MutableStateFlow<AlarmState>,
    scope: CoroutineScope,
): LineLoader = LineLoader {
    scope.launch {
        Timber.d("Loading bus lines")
        state.update { it.copy(isLoadingLines = true, errorMessage = null) }
        runCatching { loadBusLines() }
            .onSuccess { lines ->
                Timber.d("Loaded ${lines.size} bus lines")
                state.update {
                    it.copy(
                        allLines = lines,
                        lines = it.selectedStop?.let { stop -> linesForStop(lines, stop) } ?: lines,
                        isLoadingLines = false,
                    )
                }
            }
            .onFailure { error ->
                Timber.e(error, "Failed to load bus lines: ${error.message}")
                state.update {
                    it.copy(
                        isLoadingLines = false,
                        errorMessage = error.message ?: "Could not load EMT bus lines.",
                    )
                }
            }
    }
}
