package org.m0skit0.android.emtmadridalarms.service

import android.util.Log
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival

class AlarmPollingMonitorTest {
    private val request = BusAlarmRequest(line = "1", stopId = "62", targetMinutes = 10)
    private val arrivalWithinWindow = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 300, distanceMeters = 0)
    private val arrivalOutsideWindow = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 900, distanceMeters = 0)
    private val unavailableArrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 999999, distanceMeters = 0)

    @Before
    fun stubLog() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @Test
    fun `triggers alarm and invokes callback when arrival is within window`() = runTest {
        val saveActiveAlarm = mockk<org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<org.m0skit0.android.emtmadridalarms.data.SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<org.m0skit0.android.emtmadridalarms.data.SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm>(relaxed = true)
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
    fun `saves status message on loadBusArrivals failure`() = runTest {
        val saveActiveAlarm = mockk<org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<org.m0skit0.android.emtmadridalarms.data.SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<org.m0skit0.android.emtmadridalarms.data.SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm>(relaxed = true)
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
    fun `skips sentinel arrival and does not trigger`() = runTest {
        val saveActiveAlarm = mockk<org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<org.m0skit0.android.emtmadridalarms.data.SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<org.m0skit0.android.emtmadridalarms.data.SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm>(relaxed = true)
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
    fun `saves active alarm on start`() = runTest {
        val saveActiveAlarm = mockk<org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<org.m0skit0.android.emtmadridalarms.data.SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<org.m0skit0.android.emtmadridalarms.data.SetRinging>(relaxed = true)
        val clearActiveAlarm = mockk<org.m0skit0.android.emtmadridalarms.data.ClearActiveAlarm>(relaxed = true)

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
