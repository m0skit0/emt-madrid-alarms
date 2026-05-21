package org.m0skit0.android.emtmadridalarms.domain

import kotlinx.coroutines.flow.Flow

interface AlarmStateStore {
    val state: Flow<PersistedAlarmState>

    suspend fun saveActiveAlarm(request: BusAlarmRequest)
    suspend fun clearActiveAlarm()
    suspend fun saveLatestArrival(etaSeconds: Int?, destination: String)
    suspend fun saveStatus(message: String)
    suspend fun setRinging(isRinging: Boolean)
}
