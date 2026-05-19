package org.m0skit0.android.emtmadridalarms.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest

private val Context.alarmDataStore by preferencesDataStore(name = "bus_alarm")

class AlarmStorage(private val context: Context) {
    val state: Flow<PersistedAlarmState> = context.alarmDataStore.data.map { preferences ->
        val line = preferences[Keys.LINE].orEmpty()
        val stopId = preferences[Keys.STOP_ID].orEmpty()
        val targetMinutes = preferences[Keys.TARGET_MINUTES] ?: 0
        PersistedAlarmState(
            activeAlarm = if (line.isNotBlank() && stopId.isNotBlank() && targetMinutes > 0) {
                BusAlarmRequest(line = line, stopId = stopId, targetMinutes = targetMinutes)
            } else {
                null
            },
            latestEtaSeconds = preferences[Keys.LATEST_ETA_SECONDS]?.takeIf { it >= 0 },
            latestDestination = preferences[Keys.LATEST_DESTINATION].orEmpty(),
            statusMessage = preferences[Keys.STATUS_MESSAGE].orEmpty(),
            isRinging = preferences[Keys.IS_RINGING] ?: false,
        )
    }

    suspend fun saveActiveAlarm(request: BusAlarmRequest) {
        context.alarmDataStore.edit { preferences ->
            preferences[Keys.LINE] = request.line
            preferences[Keys.STOP_ID] = request.stopId
            preferences[Keys.TARGET_MINUTES] = request.targetMinutes
            preferences[Keys.IS_RINGING] = false
            preferences[Keys.STATUS_MESSAGE] = "Monitoring arrivals..."
            preferences.remove(Keys.LATEST_ETA_SECONDS)
            preferences.remove(Keys.LATEST_DESTINATION)
        }
    }

    suspend fun clearActiveAlarm() {
        context.alarmDataStore.edit { preferences ->
            preferences.remove(Keys.LINE)
            preferences.remove(Keys.STOP_ID)
            preferences.remove(Keys.TARGET_MINUTES)
            preferences.remove(Keys.LATEST_ETA_SECONDS)
            preferences.remove(Keys.LATEST_DESTINATION)
            preferences.remove(Keys.STATUS_MESSAGE)
        }
    }

    suspend fun saveLatestArrival(etaSeconds: Int?, destination: String) {
        context.alarmDataStore.edit { preferences ->
            if (etaSeconds == null) {
                preferences.remove(Keys.LATEST_ETA_SECONDS)
            } else {
                preferences[Keys.LATEST_ETA_SECONDS] = etaSeconds
            }
            preferences[Keys.LATEST_DESTINATION] = destination
            preferences[Keys.STATUS_MESSAGE] = if (etaSeconds == null) "No matching arrivals right now." else "Last updated just now."
        }
    }

    suspend fun saveStatus(message: String) {
        context.alarmDataStore.edit { preferences ->
            preferences[Keys.STATUS_MESSAGE] = message
        }
    }

    suspend fun setRinging(isRinging: Boolean) {
        context.alarmDataStore.edit { preferences ->
            preferences[Keys.IS_RINGING] = isRinging
        }
    }

    private object Keys {
        val LINE = stringPreferencesKey("line")
        val STOP_ID = stringPreferencesKey("stop_id")
        val TARGET_MINUTES = intPreferencesKey("target_minutes")
        val LATEST_ETA_SECONDS = intPreferencesKey("latest_eta_seconds")
        val LATEST_DESTINATION = stringPreferencesKey("latest_destination")
        val STATUS_MESSAGE = stringPreferencesKey("status_message")
        val IS_RINGING = booleanPreferencesKey("is_ringing")
    }
}

data class PersistedAlarmState(
    val activeAlarm: BusAlarmRequest?,
    val latestEtaSeconds: Int?,
    val latestDestination: String,
    val statusMessage: String,
    val isRinging: Boolean,
)
