package org.m0skit0.android.emtmadridalarms.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.MAX_ACTIVE_ALARMS

class AlarmStorageTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeDataStore(scope: TestScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tmpFolder.newFile("test_alarm_prefs.preferences_pb") }
    )

    @Test
    fun `given an empty store, when alarmStateReader is collected, then activeAlarm is null and defaults are returned`() = runTest {
        val ds = makeDataStore(this)
        val result = alarmStateReader(ds)().first()
        result.activeAlarm shouldBe null
        result.activeAlarms shouldBe emptyList()
        result.isRinging shouldBe false
        result.ringingAlarm shouldBe null
        result.statusMessage shouldBe ""
    }

    @Test
    fun `given a saved alarm with zero targetMinutes, when alarmStateReader is collected, then activeAlarm is null`() = runTest {
        val ds = makeDataStore(this)
        saveActiveAlarm(ds)(BusAlarmRequest("1", "100", 0))
        val result = alarmStateReader(ds)().first()
        result.activeAlarm shouldBe null
    }

    @Test
    fun `given a valid alarm request, when saveActiveAlarm is called, then the request is persisted with ringing false and monitoring status`() = runTest {
        val ds = makeDataStore(this)
        val request = BusAlarmRequest("27", "100", 5)

        saveActiveAlarm(ds)(request)

        val result = alarmStateReader(ds)().first()
        result.activeAlarm shouldBe request
        result.activeAlarms shouldBe listOf(request)
        result.isRinging shouldBe false
        result.statusMessage shouldBe "Monitoring arrivals..."
        result.latestEtaSeconds shouldBe null
        result.latestDestination shouldBe ""
    }

    @Test
    fun `given a persisted alarm, when clearActiveAlarm is called, then activeAlarm and statusMessage are cleared`() = runTest {
        val ds = makeDataStore(this)
        saveActiveAlarm(ds)(BusAlarmRequest("1", "100", 5))

        clearActiveAlarm(ds)()

        val result = alarmStateReader(ds)().first()
        result.activeAlarm shouldBe null
        result.activeAlarms shouldBe emptyList()
        result.statusMessage shouldBe ""
    }

    @Test
    fun `given multiple valid alarm requests, when saveActiveAlarm is called, then all requests are persisted`() = runTest {
        val ds = makeDataStore(this)
        val first = BusAlarmRequest("27", "100", 5)
        val second = BusAlarmRequest("34", "200", 10)

        saveActiveAlarm(ds)(first)
        saveActiveAlarm(ds)(second)

        val result = alarmStateReader(ds)().first()
        result.activeAlarm shouldBe first
        result.activeAlarms shouldBe listOf(first, second)
    }

    @Test
    fun `given an active alarm, when saving same line and stop with different minutes, then duplicate is not persisted`() = runTest {
        val ds = makeDataStore(this)
        val first = BusAlarmRequest("27", "100", 5)
        val duplicate = BusAlarmRequest("27", "100", 10)

        saveActiveAlarm(ds)(first)
        saveActiveAlarm(ds)(duplicate)

        alarmStateReader(ds)().first().activeAlarms shouldBe listOf(first)
    }

    @Test
    fun `given five active alarms, when another alarm is saved, then active alarms remain capped`() = runTest {
        val ds = makeDataStore(this)
        val alarms = (1..MAX_ACTIVE_ALARMS).map { BusAlarmRequest(it.toString(), "100", 5) }
        val extra = BusAlarmRequest("99", "200", 10)

        alarms.forEach { saveActiveAlarm(ds)(it) }
        saveActiveAlarm(ds)(extra)

        alarmStateReader(ds)().first().activeAlarms shouldBe alarms
    }

    @Test
    fun `given multiple active alarms, when removeActiveAlarm is called, then only that request is removed`() = runTest {
        val ds = makeDataStore(this)
        val first = BusAlarmRequest("27", "100", 5)
        val second = BusAlarmRequest("34", "200", 10)
        saveActiveAlarm(ds)(first)
        saveActiveAlarm(ds)(second)

        removeActiveAlarm(ds)(first)

        val result = alarmStateReader(ds)().first()
        result.activeAlarm shouldBe second
        result.activeAlarms shouldBe listOf(second)
    }

    @Test
    fun `given a non-null eta, when saveLatestArrival is called, then eta and destination are stored with a last-updated status`() = runTest {
        val ds = makeDataStore(this)

        saveLatestArrival(ds)(120, "Centro")

        val result = alarmStateReader(ds)().first()
        result.latestEtaSeconds shouldBe 120
        result.latestDestination shouldBe "Centro"
        result.statusMessage shouldBe "Last updated just now."
    }

    @Test
    fun `given a previously stored eta, when saveLatestArrival is called with null eta, then eta is cleared and no-arrivals status is set`() = runTest {
        val ds = makeDataStore(this)
        saveLatestArrival(ds)(60, "Sol")

        saveLatestArrival(ds)(null, "")

        val result = alarmStateReader(ds)().first()
        result.latestEtaSeconds shouldBe null
        result.statusMessage shouldBe "No matching arrivals right now."
    }

    @Test
    fun `given a message, when saveStatus is called, then statusMessage is persisted`() = runTest {
        val ds = makeDataStore(this)

        saveStatus(ds)("Bus delayed")

        val result = alarmStateReader(ds)().first()
        result.statusMessage shouldBe "Bus delayed"
    }

    @Test
    fun `given an enabled alarm, when setAlarmEnabled is called with false, then the alarm is disabled`() = runTest {
        val ds = makeDataStore(this)
        val request = BusAlarmRequest("27", "100", 5)
        saveActiveAlarm(ds)(request)

        setAlarmEnabled(ds)(request, false)

        val result = alarmStateReader(ds)().first()
        result.activeAlarms shouldBe listOf(request.copy(isEnabled = false))
    }

    @Test
    fun `given a disabled alarm, when setAlarmEnabled is called with true, then the alarm is enabled`() = runTest {
        val ds = makeDataStore(this)
        val request = BusAlarmRequest("27", "100", 5, isEnabled = false)
        saveActiveAlarm(ds)(request)

        setAlarmEnabled(ds)(request, true)

        val result = alarmStateReader(ds)().first()
        result.activeAlarms shouldBe listOf(request.copy(isEnabled = true))
    }

    @Test
    fun `given no alarm matching the request, when setAlarmEnabled is called, then the alarm list is unchanged`() = runTest {
        val ds = makeDataStore(this)
        val saved = BusAlarmRequest("27", "100", 5)
        val other = BusAlarmRequest("34", "200", 10)
        saveActiveAlarm(ds)(saved)

        setAlarmEnabled(ds)(other, false)

        alarmStateReader(ds)().first().activeAlarms shouldBe listOf(saved)
    }

    @Test
    fun `given isRinging false, when setRinging is called with true, then isRinging and ringingAlarm are persisted`() = runTest {
        val ds = makeDataStore(this)
        val request = BusAlarmRequest("27", "100", 5)

        setRinging(ds)(true, request)

        val result = alarmStateReader(ds)().first()
        result.isRinging shouldBe true
        result.ringingAlarm shouldBe request
    }

    @Test
    fun `given isRinging true, when setRinging is called with false, then isRinging and ringingAlarm are cleared`() = runTest {
        val ds = makeDataStore(this)
        setRinging(ds)(true, BusAlarmRequest("27", "100", 5))

        setRinging(ds)(false, null)

        val result = alarmStateReader(ds)().first()
        result.isRinging shouldBe false
        result.ringingAlarm shouldBe null
    }
}
