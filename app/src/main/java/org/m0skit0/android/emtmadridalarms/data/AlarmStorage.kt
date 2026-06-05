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
    val ENABLED = booleanPreferencesKey("enabled")
    val LATEST_ETA_SECONDS = intPreferencesKey("latest_eta_seconds")
    val LATEST_DESTINATION = stringPreferencesKey("latest_destination")
    val STATUS_MESSAGE = stringPreferencesKey("status_message")
    val IS_RINGING = booleanPreferencesKey("is_ringing")
    val RINGING_LINE = stringPreferencesKey("ringing_line")
    val RINGING_STOP_ID = stringPreferencesKey("ringing_stop_id")
    val RINGING_TARGET_MINUTES = intPreferencesKey("ringing_target_minutes")
}

data class AlarmStorageState(
    val activeAlarm: BusAlarmRequest? = null,
    val activeAlarms: List<BusAlarmRequest> = activeAlarm?.let { listOf(it) } ?: emptyList(),
    val latestEtaSeconds: Int? = null,
    val latestDestination: String = "",
    val statusMessage: String = "",
    val isRinging: Boolean = false,
    val ringingAlarm: BusAlarmRequest? = null,
)

fun interface AlarmStateReader : () -> Flow<PersistedAlarmState>
fun interface SaveActiveAlarm : suspend (BusAlarmRequest) -> Unit
fun interface RemoveActiveAlarm : suspend (BusAlarmRequest) -> Unit
fun interface ClearActiveAlarm : suspend () -> Unit
fun interface SaveLatestArrival : suspend (Int?, String) -> Unit
fun interface SaveStatus : suspend (String) -> Unit
fun interface SetRinging : suspend (Boolean, BusAlarmRequest?) -> Unit
fun interface SetAlarmEnabled : suspend (BusAlarmRequest, Boolean) -> Unit

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
            ringingAlarm = ringingAlarm(preferences),
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
        clearRingingAlarm(preferences)
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
        clearRingingAlarm(preferences)
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

internal fun setRinging(dataStore: DataStore<Preferences>): SetRinging = SetRinging { isRinging, alarm ->
    dataStore.edit { preferences ->
        preferences[Keys.IS_RINGING] = isRinging
        if (isRinging && alarm != null) {
            preferences[Keys.RINGING_LINE] = alarm.line
            preferences[Keys.RINGING_STOP_ID] = alarm.stopId
            preferences[Keys.RINGING_TARGET_MINUTES] = alarm.targetMinutes
        } else {
            clearRingingAlarm(preferences)
        }
    }
}

private fun activeAlarms(preferences: Preferences): List<BusAlarmRequest> =
    (0 until MAX_ACTIVE_ALARMS).mapNotNull { index ->
        val line = preferences[lineKey(index)].orEmpty()
        val stopId = preferences[stopIdKey(index)].orEmpty()
        val targetMinutes = preferences[targetMinutesKey(index)] ?: 0
        val isEnabled = preferences[enabledKey(index)] ?: true
        if (line.isNotBlank() && stopId.isNotBlank() && targetMinutes > 0) {
            BusAlarmRequest(line = line, stopId = stopId, targetMinutes = targetMinutes, isEnabled = isEnabled)
        } else {
            null
        }
    }

private fun writeActiveAlarms(preferences: MutablePreferences, alarms: List<BusAlarmRequest>) {
    (0 until MAX_ACTIVE_ALARMS).forEach { index ->
        preferences.remove(lineKey(index))
        preferences.remove(stopIdKey(index))
        preferences.remove(targetMinutesKey(index))
        preferences.remove(enabledKey(index))
    }
    alarms.take(MAX_ACTIVE_ALARMS).forEachIndexed { index, alarm ->
        preferences[lineKey(index)] = alarm.line
        preferences[stopIdKey(index)] = alarm.stopId
        preferences[targetMinutesKey(index)] = alarm.targetMinutes
        preferences[enabledKey(index)] = alarm.isEnabled
    }
}

private fun ringingAlarm(preferences: Preferences): BusAlarmRequest? {
    val line = preferences[Keys.RINGING_LINE].orEmpty()
    val stopId = preferences[Keys.RINGING_STOP_ID].orEmpty()
    val targetMinutes = preferences[Keys.RINGING_TARGET_MINUTES] ?: 0
    return if (line.isNotBlank() && stopId.isNotBlank() && targetMinutes > 0) {
        BusAlarmRequest(line = line, stopId = stopId, targetMinutes = targetMinutes)
    } else {
        null
    }
}

private fun clearRingingAlarm(preferences: MutablePreferences) {
    preferences.remove(Keys.RINGING_LINE)
    preferences.remove(Keys.RINGING_STOP_ID)
    preferences.remove(Keys.RINGING_TARGET_MINUTES)
}

internal fun setAlarmEnabled(dataStore: DataStore<Preferences>): SetAlarmEnabled = SetAlarmEnabled { request, enabled ->
    dataStore.edit { preferences ->
        val updated = activeAlarms(preferences).map { alarm ->
            if (alarm.hasSameLineAndStop(request)) alarm.copy(isEnabled = enabled) else alarm
        }
        writeActiveAlarms(preferences, updated)
    }
}

private fun lineKey(index: Int) = if (index == 0) Keys.LINE else stringPreferencesKey("line_$index")
private fun stopIdKey(index: Int) = if (index == 0) Keys.STOP_ID else stringPreferencesKey("stop_id_$index")
private fun targetMinutesKey(index: Int) = if (index == 0) Keys.TARGET_MINUTES else intPreferencesKey("target_minutes_$index")
private fun enabledKey(index: Int) = if (index == 0) Keys.ENABLED else booleanPreferencesKey("enabled_$index")
