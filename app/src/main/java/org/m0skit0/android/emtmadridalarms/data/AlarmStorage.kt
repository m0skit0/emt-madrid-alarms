package org.m0skit0.android.emtmadridalarms.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.MAX_ACTIVE_ALARMS
import org.m0skit0.android.emtmadridalarms.domain.PersistedAlarmState
import org.m0skit0.android.emtmadridalarms.domain.hasSameLineAndStop

private object Keys {
    val LINE = stringPreferencesKey("line")
    val STOP_ID = stringPreferencesKey("stop_id")
    val TARGET_MINUTES = intPreferencesKey("target_minutes")
    val LATEST_ETA_SECONDS = intPreferencesKey("latest_eta_seconds")
    val LATEST_DESTINATION = stringPreferencesKey("latest_destination")
    val STATUS_MESSAGE = stringPreferencesKey("status_message")
    val IS_RINGING = booleanPreferencesKey("is_ringing")
}

data class AlarmStorageState(
    val activeAlarm: BusAlarmRequest? = null,
    val activeAlarms: List<BusAlarmRequest> = activeAlarm?.let { listOf(it) } ?: emptyList(),
    val latestEtaSeconds: Int? = null,
    val latestDestination: String = "",
    val statusMessage: String = "",
    val isRinging: Boolean = false,
)

fun interface AlarmStateReader : () -> Flow<PersistedAlarmState>
fun interface SaveActiveAlarm : suspend (BusAlarmRequest) -> Unit
fun interface RemoveActiveAlarm : suspend (BusAlarmRequest) -> Unit
fun interface ClearActiveAlarm : suspend () -> Unit
fun interface SaveLatestArrival : suspend (Int?, String) -> Unit
fun interface SaveStatus : suspend (String) -> Unit
fun interface SetRinging : suspend (Boolean) -> Unit

internal fun alarmStateReader(dataStore: DataStore<Preferences>): AlarmStateReader = AlarmStateReader {
    dataStore.data.map { preferences ->
        val activeAlarms = activeAlarms(preferences)
        PersistedAlarmState(
            activeAlarm = activeAlarms.firstOrNull(),
            latestEtaSeconds = preferences[Keys.LATEST_ETA_SECONDS]?.takeIf { it >= 0 },
            latestDestination = preferences[Keys.LATEST_DESTINATION].orEmpty(),
            statusMessage = preferences[Keys.STATUS_MESSAGE].orEmpty(),
            isRinging = preferences[Keys.IS_RINGING] ?: false,
            activeAlarms = activeAlarms,
        )
    }
}

internal fun saveActiveAlarm(dataStore: DataStore<Preferences>): SaveActiveAlarm = SaveActiveAlarm { request ->
    dataStore.edit { preferences ->
        val activeAlarms = activeAlarms(preferences)
        val nextAlarms = if (activeAlarms.any { it.hasSameLineAndStop(request) } || activeAlarms.size >= MAX_ACTIVE_ALARMS) {
            activeAlarms
        } else {
            activeAlarms + request
        }
        writeActiveAlarms(preferences, nextAlarms)
        preferences[Keys.IS_RINGING] = false
        preferences[Keys.STATUS_MESSAGE] = "Monitoring arrivals..."
        if (activeAlarms.isEmpty()) {
            preferences.remove(Keys.LATEST_ETA_SECONDS)
            preferences.remove(Keys.LATEST_DESTINATION)
        }
    }
}

internal fun removeActiveAlarm(dataStore: DataStore<Preferences>): RemoveActiveAlarm = RemoveActiveAlarm { request ->
    dataStore.edit { preferences ->
        writeActiveAlarms(preferences, activeAlarms(preferences) - request)
    }
}

internal fun clearActiveAlarm(dataStore: DataStore<Preferences>): ClearActiveAlarm = ClearActiveAlarm {
    dataStore.edit { preferences ->
        writeActiveAlarms(preferences, emptyList())
        preferences.remove(Keys.LATEST_ETA_SECONDS)
        preferences.remove(Keys.LATEST_DESTINATION)
        preferences.remove(Keys.STATUS_MESSAGE)
    }
}

internal fun saveLatestArrival(dataStore: DataStore<Preferences>): SaveLatestArrival =
    SaveLatestArrival { etaSeconds, destination ->
        dataStore.edit { preferences ->
            if (etaSeconds == null) {
                preferences.remove(Keys.LATEST_ETA_SECONDS)
            } else {
                preferences[Keys.LATEST_ETA_SECONDS] = etaSeconds
            }
            preferences[Keys.LATEST_DESTINATION] = destination
            preferences[Keys.STATUS_MESSAGE] =
                if (etaSeconds == null) "No matching arrivals right now." else "Last updated just now."
        }
    }

internal fun saveStatus(dataStore: DataStore<Preferences>): SaveStatus = SaveStatus { message ->
    dataStore.edit { preferences ->
        preferences[Keys.STATUS_MESSAGE] = message
    }
}

internal fun setRinging(dataStore: DataStore<Preferences>): SetRinging = SetRinging { isRinging ->
    dataStore.edit { preferences ->
        preferences[Keys.IS_RINGING] = isRinging
    }
}

private fun activeAlarms(preferences: Preferences): List<BusAlarmRequest> =
    (0 until MAX_ACTIVE_ALARMS).mapNotNull { index ->
        val line = preferences[lineKey(index)].orEmpty()
        val stopId = preferences[stopIdKey(index)].orEmpty()
        val targetMinutes = preferences[targetMinutesKey(index)] ?: 0
        if (line.isNotBlank() && stopId.isNotBlank() && targetMinutes > 0) {
            BusAlarmRequest(line = line, stopId = stopId, targetMinutes = targetMinutes)
        } else {
            null
        }
    }

private fun writeActiveAlarms(preferences: MutablePreferences, alarms: List<BusAlarmRequest>) {
    (0 until MAX_ACTIVE_ALARMS).forEach { index ->
        preferences.remove(lineKey(index))
        preferences.remove(stopIdKey(index))
        preferences.remove(targetMinutesKey(index))
    }
    alarms.take(MAX_ACTIVE_ALARMS).forEachIndexed { index, alarm ->
        preferences[lineKey(index)] = alarm.line
        preferences[stopIdKey(index)] = alarm.stopId
        preferences[targetMinutesKey(index)] = alarm.targetMinutes
    }
}

private fun lineKey(index: Int) = if (index == 0) Keys.LINE else stringPreferencesKey("line_$index")
private fun stopIdKey(index: Int) = if (index == 0) Keys.STOP_ID else stringPreferencesKey("stop_id_$index")
private fun targetMinutesKey(index: Int) = if (index == 0) Keys.TARGET_MINUTES else intPreferencesKey("target_minutes_$index")
