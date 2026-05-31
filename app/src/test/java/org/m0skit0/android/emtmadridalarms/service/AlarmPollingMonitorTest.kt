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
    private val arrivalAtTarget = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 600, distanceMeters = 0)
    private val arrivalOutsideWindow = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 900, distanceMeters = 0)
    private val unavailableArrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 999999, distanceMeters = 0)

    @Test
    fun `given an arrival at the target window, when polled, then the alarm is triggered and ringing is set`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<ClearActiveAlarm>(relaxed = true)
        var triggeredWith: BusArrival? = null

        val monitor = pollAlarm(
            loadBusArrivals = { listOf(arrivalAtTarget) },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            clearActiveAlarm = clearActiveAlarm,
        )

        monitor(request) { triggeredWith = it }

        triggeredWith shouldBe arrivalAtTarget
        coVerify { setRinging(true) }
        coVerify { clearActiveAlarm() }
    }

    @Test
    fun `given an arrival already inside the target window, when a later bus reaches the target, then the later bus triggers the alarm`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<ClearActiveAlarm>(relaxed = true)
        var callCount = 0
        var triggeredWith: BusArrival? = null

        val monitor = pollAlarm(
            loadBusArrivals = {
                callCount++
                if (callCount == 1) listOf(arrivalWithinWindow, arrivalOutsideWindow)
                else listOf(arrivalWithinWindow.copy(estimateSeconds = 240), arrivalAtTarget)
            },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            clearActiveAlarm = clearActiveAlarm,
        )

        monitor(request) { triggeredWith = it }

        callCount shouldBe 2
        triggeredWith shouldBe arrivalAtTarget
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
                else listOf(arrivalAtTarget)
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
    fun `given a sentinel arrival on the first poll, when polled again, then the arrival is skipped and alarm triggers later`() = runTest {
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
                when (callCount) {
                    1 -> listOf(unavailableArrival)
                    2 -> listOf(arrivalOutsideWindow)
                    else -> listOf(arrivalAtTarget)
                }
            },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            clearActiveAlarm = clearActiveAlarm,
        )

        monitor(request) { triggered = true }

        triggered shouldBe true
        callCount shouldBe 3
    }

    @Test
    fun `given a valid request, when polling starts, then the active alarm is persisted immediately`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<ClearActiveAlarm>(relaxed = true)

        val monitor = pollAlarm(
            loadBusArrivals = { listOf(arrivalAtTarget) },
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
