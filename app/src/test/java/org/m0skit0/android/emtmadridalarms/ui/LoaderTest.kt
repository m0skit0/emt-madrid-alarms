package org.m0skit0.android.emtmadridalarms.ui

import android.util.Log
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

class LineLoaderTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Before
    fun stubLog() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @Test
    fun `sets isLoadingLines true then populates lines on success`() = scope.runTest {
        val lines = listOf(BusLine("1", "1", "A", "B"))
        val state = MutableStateFlow(AlarmState())
        val loader = lineLoader(loadBusLines = { lines }, state = state, scope = scope)

        loader()
        dispatcher.scheduler.advanceUntilIdle()

        state.value.isLoadingLines shouldBe false
        state.value.lines shouldBe lines
        state.value.errorMessage shouldBe null
    }

    @Test
    fun `sets error message on failure`() = scope.runTest {
        val state = MutableStateFlow(AlarmState())
        val loader = lineLoader(
            loadBusLines = { error("network failure") },
            state = state,
            scope = scope,
        )

        loader()
        dispatcher.scheduler.advanceUntilIdle()

        state.value.isLoadingLines shouldBe false
        state.value.errorMessage shouldBe "network failure"
        state.value.lines shouldBe emptyList()
    }

    @Test
    fun `uses fallback error message when exception message is null`() = scope.runTest {
        val state = MutableStateFlow(AlarmState())
        val loader = lineLoader(
            loadBusLines = { throw RuntimeException() },
            state = state,
            scope = scope,
        )

        loader()
        dispatcher.scheduler.advanceUntilIdle()

        state.value.errorMessage shouldBe "Could not load EMT bus lines."
    }
}

class StopLoaderTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val line = BusLine("1", "1", "A", "B")

    @Before
    fun stubLog() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @Test
    fun `populates stops and clears isLoadingStops on success`() = scope.runTest {
        val stops = listOf(BusStop("62", "Gran Via", "Calle 1"))
        val state = MutableStateFlow(AlarmState(isLoadingStops = true))
        val loader = stopLoader(loadBusStops = { stops }, state = state, scope = scope)

        loader(line)
        dispatcher.scheduler.advanceUntilIdle()

        state.value.isLoadingStops shouldBe false
        state.value.stops shouldBe stops
        state.value.errorMessage shouldBe null
    }

    @Test
    fun `sets error message on failure`() = scope.runTest {
        val state = MutableStateFlow(AlarmState(isLoadingStops = true))
        val loader = stopLoader(
            loadBusStops = { error("timeout") },
            state = state,
            scope = scope,
        )

        loader(line)
        dispatcher.scheduler.advanceUntilIdle()

        state.value.isLoadingStops shouldBe false
        state.value.errorMessage shouldBe "timeout"
    }

    @Test
    fun `uses fallback error message when exception message is null`() = scope.runTest {
        val state = MutableStateFlow(AlarmState(isLoadingStops = true))
        val loader = stopLoader(
            loadBusStops = { throw RuntimeException() },
            state = state,
            scope = scope,
        )

        loader(line)
        dispatcher.scheduler.advanceUntilIdle()

        state.value.errorMessage shouldBe "Could not load stops for line ${line.label}."
    }
}
