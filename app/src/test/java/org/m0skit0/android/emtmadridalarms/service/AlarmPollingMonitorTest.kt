package org.m0skit0.android.emtmadridalarms.service

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.data.RemoveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveActiveAlarm
import org.m0skit0.android.emtmadridalarms.data.SaveLatestArrival
import org.m0skit0.android.emtmadridalarms.data.SaveStatus
import org.m0skit0.android.emtmadridalarms.data.SetRinging
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PollIntervalTest {
    @Test
    fun `given null estimate, when computing interval, then returns minimum interval`() {
        pollInterval(estimateSeconds = null, targetMinutes = 10) shouldBe 30.seconds
    }

    @Test
    fun `given zero targetMinutes, when computing interval, then returns minimum interval`() {
        pollInterval(estimateSeconds = 900, targetMinutes = 0) shouldBe 30.seconds
    }

    @Test
    fun `given estimate far above target, when computing interval, then returns maximum interval`() {
        // remaining = 3600 - 600 = 3000s → 3000/6 = 500s > 5min cap
        pollInterval(estimateSeconds = 3600, targetMinutes = 10) shouldBe 5.minutes
    }

    @Test
    fun `given estimate giving buffer of exactly max interval times divisor, when computing interval, then returns maximum interval`() {
        // remaining = 1800 + 600 = 2400s? Let's pick remaining = 1800s → 1800/6 = 300s = 5min = MAX
        // estimate = targetMinutes * 60 + remaining = 600 + 1800 = 2400s
        pollInterval(estimateSeconds = 2400, targetMinutes = 10) shouldBe 5.minutes
    }

    @Test
    fun `given estimate with moderate buffer, when computing interval, then returns proportional interval`() {
        // remaining = 1200 - 600 = 600s → 600/6 = 100s
        pollInterval(estimateSeconds = 1200, targetMinutes = 10) shouldBe 100.seconds
    }

    @Test
    fun `given estimate just above minimum threshold, when computing interval, then returns minimum interval`() {
        // remaining = 780 - 600 = 180s → 180/6 = 30s = MIN
        pollInterval(estimateSeconds = 780, targetMinutes = 10) shouldBe 30.seconds
    }

    @Test
    fun `given estimate at target, when computing interval, then returns minimum interval`() {
        // remaining = 600 - 600 = 0 → MIN
        pollInterval(estimateSeconds = 600, targetMinutes = 10) shouldBe 30.seconds
    }

    @Test
    fun `given estimate below target, when computing interval, then returns minimum interval`() {
        // remaining coerced to 0 → MIN
        pollInterval(estimateSeconds = 300, targetMinutes = 10) shouldBe 30.seconds
    }
}

class AlarmPollingMonitorTest {
    private val request = BusAlarmRequest(line = "1", stopId = "62", targetMinutes = 10)
    private val arrivalWithinWindow = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 300, distanceMeters = 0)
    private val arrivalAtTarget = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 600, distanceMeters = 0)
    private val arrivalWithinPollInterval = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 620, distanceMeters = 0)
    private val arrivalOutsideWindow = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 900, distanceMeters = 0)
    private val unavailableArrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 999999, distanceMeters = 0)

    @Test
    fun `given an arrival at the target window, when polled, then the alarm is triggered and ringing is set`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val removeActiveAlarm = mockk<RemoveActiveAlarm>(relaxed = true)
        var triggeredWith: BusArrival? = null

        val monitor = pollAlarm(
            loadBusArrivals = { listOf(arrivalAtTarget) },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            removeActiveAlarm = removeActiveAlarm,
        )

        monitor(request) { triggeredWith = it }

        triggeredWith shouldBe arrivalAtTarget
        coVerify { setRinging(true, request) }
        coVerify { removeActiveAlarm(request) }
    }

    @Test
    fun `given an arrival inside target plus poll interval, when polled, then the alarm is triggered`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val removeActiveAlarm = mockk<RemoveActiveAlarm>(relaxed = true)
        var triggeredWith: BusArrival? = null

        val monitor = pollAlarm(
            loadBusArrivals = { listOf(arrivalWithinPollInterval) },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            removeActiveAlarm = removeActiveAlarm,
        )

        monitor(request) { triggeredWith = it }

        triggeredWith shouldBe arrivalWithinPollInterval
        coVerify { setRinging(true, request) }
        coVerify { removeActiveAlarm(request) }
    }

    @Test
    fun `given an arrival already inside the target window, when a later bus reaches the target, then the later bus triggers the alarm`() = runTest {
        val saveActiveAlarm = mockk<SaveActiveAlarm>(relaxed = true)
        val saveStatus = mockk<SaveStatus>(relaxed = true)
        val saveLatestArrival = mockk<SaveLatestArrival>(relaxed = true)
        val setRinging = mockk<SetRinging>(relaxed = true)
        val removeActiveAlarm = mockk<RemoveActiveAlarm>(relaxed = true)
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
            removeActiveAlarm = removeActiveAlarm,
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
        val removeActiveAlarm = mockk<RemoveActiveAlarm>(relaxed = true)
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
            removeActiveAlarm = removeActiveAlarm,
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
        val removeActiveAlarm = mockk<RemoveActiveAlarm>(relaxed = true)
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
            removeActiveAlarm = removeActiveAlarm,
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
        val removeActiveAlarm = mockk<RemoveActiveAlarm>(relaxed = true)

        val monitor = pollAlarm(
            loadBusArrivals = { listOf(arrivalAtTarget) },
            saveActiveAlarm = saveActiveAlarm,
            saveStatus = saveStatus,
            saveLatestArrival = saveLatestArrival,
            setRinging = setRinging,
            removeActiveAlarm = removeActiveAlarm,
        )

        monitor(request) {}

        coVerify { saveActiveAlarm(request) }
    }
}
