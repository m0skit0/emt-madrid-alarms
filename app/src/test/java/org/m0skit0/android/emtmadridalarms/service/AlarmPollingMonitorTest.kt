package org.m0skit0.android.emtmadridalarms.service

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival
import org.m0skit0.android.emtmadridalarms.data.SaveStatus
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival

class AlarmPollingMonitorTest {
    private val request = BusAlarmRequest(line = "1", stopId = "62", targetMinutes = 10)
    private val arrivalWithinWindow = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 300, distanceMeters = 0)
    private val unavailableArrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 999999, distanceMeters = 0)

    @Test
    fun `given an arrival within the target window, when polled, then the alarm is triggered and ringing is set`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<ClearActiveAlarm>(relaxed = true)
        var triggeredWith: BusArrival? = null

        val monitor = pollAlarm(
            loadBusArrivals = { listOf(arrivalWithinWindow) },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            clearActiveAlarm = clearActiveAlarm,
        )

        monitor(request) { triggeredWith = it }

        triggeredWith shouldBe arrivalWithinWindow
        coVerify { setRinging(true) }
        coVerify { clearActiveAlarm() }
    }

    @Test
    fun `given loadBusArrivals throws on the first call, when polled, then the error status is saved`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<ClearActiveAlarm>(relaxed = true)
        var callCount = 0

        val monitor = pollAlarm(
            loadBusArrivals = {
                callCount++
                if (callCount == 1) throw RuntimeException("server error")
                else listOf(arrivalWithinWindow)
            },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            clearActiveAlarm = clearActiveAlarm,
        )

        monitor(request) {}

        coVerify { saveStatus("server error") }
    }

    @Test
    fun `given a sentinel arrival on the first poll, when polled again, then the arrival is skipped and alarm triggers on second poll`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<ClearActiveAlarm>(relaxed = true)
        var callCount = 0
        var triggered = false

        val monitor = pollAlarm(
            loadBusArrivals = {
                callCount++
                if (callCount == 1) listOf(unavailableArrival)
                else listOf(arrivalWithinWindow)
            },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            clearActiveAlarm = clearActiveAlarm,
        )

        monitor(request) { triggered = true }

        triggered shouldBe true
        callCount shouldBe 2
    }

    @Test
    fun `given a valid request, when polling starts, then the active alarm is persisted immediately`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<ClearActiveAlarm>(relaxed = true)

        val monitor = pollAlarm(
            loadBusArrivals = { listOf(arrivalWithinWindow) },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            clearActiveAlarm = clearActiveAlarm,
        )

        monitor(request) {}

        coVerify { saveActiveAlarm(request) }
    }
}
