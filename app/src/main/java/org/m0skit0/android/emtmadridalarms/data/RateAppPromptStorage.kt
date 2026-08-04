package org.m0skit0.android.emtmadridalarms.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import org.m0skit0.android.emtmadridalarms.utils.orDefault

private val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
private val RATE_PROMPT_THRESHOLD_SHOWN = intPreferencesKey("rate_prompt_threshold_shown")
internal val RATE_PROMPT_THRESHOLDS = listOf(2, 5, 10)

internal fun nextRatePromptThreshold(appOpenCount: Int, lastPromptedThreshold: Int): Int? =
    RATE_PROMPT_THRESHOLDS.firstOrNull { it in (lastPromptedThreshold + 1)..appOpenCount }

fun interface RecordAppOpen : suspend () -> Int
fun interface LastRatePromptShown : suspend () -> Int
fun interface MarkRatePromptShown : suspend (Int) -> Unit

internal fun recordAppOpen(dataStore: DataStore<Preferences>): RecordAppOpen = RecordAppOpen {
    var newCount = 0
    dataStore.edit { preferences ->
        newCount = (preferences[APP_OPEN_COUNT].orDefault { 0 }) + 1
        preferences[APP_OPEN_COUNT] = newCount
    }
    newCount
}

internal fun lastRatePromptShown(dataStore: DataStore<Preferences>): LastRatePromptShown =
    LastRatePromptShown {
        dataStore.data.first()[RATE_PROMPT_THRESHOLD_SHOWN].orDefault { 0 }
    }

internal fun markRatePromptShown(dataStore: DataStore<Preferences>): MarkRatePromptShown =
    MarkRatePromptShown { threshold ->
        dataStore.edit { preferences ->
            preferences[RATE_PROMPT_THRESHOLD_SHOWN] = threshold
        }
    }
