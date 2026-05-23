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
        result.isRinging shouldBe false
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
        result.statusMessage shouldBe ""
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
    fun `given isRinging false, when setRinging is called with true, then isRinging is persisted as true`() = runTest {
        val ds = makeDataStore(this)

        setRinging(ds)(true)

        val result = alarmStateReader(ds)().first()
        result.isRinging shouldBe true
    }

    @Test
    fun `given isRinging true, when setRinging is called with false, then isRinging is persisted as false`() = runTest {
        val ds = makeDataStore(this)
        setRinging(ds)(true)

        setRinging(ds)(false)

        val result = alarmStateReader(ds)().first()
        result.isRinging shouldBe false
    }
}
