package org.m0skit0.android.emtmadridalarms.ui

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

class LineLoaderTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun `given a successful lines fetch, when loader is invoked, then state contains the lines and isLoadingLines is false`() = scope.runTest {
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
    fun `given a failing lines fetch, when loader is invoked, then state contains the error message`() = scope.runTest {
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
    fun `given an exception with a null message, when loader is invoked, then a fallback error message is shown`() = scope.runTest {
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

    @Test
    fun `given a successful all stops fetch, when all stop loader is invoked, then state contains all stops and isLoadingStops is false`() = scope.runTest {
        val stops = listOf(BusStop("62", "Gran Via", "Calle 1", lineLabels = setOf("1")))
        val state = MutableStateFlow(AlarmState())
        val loader = allStopLoader(loadAllBusStops = { stops }, state = state, scope = scope)

        loader()
        dispatcher.scheduler.advanceUntilIdle()

        state.value.isLoadingStops shouldBe false
        state.value.allStops shouldBe stops
        state.value.stops shouldBe stops
        state.value.errorMessage shouldBe null
    }

    @Test
    fun `given a selected line, when all stop loader succeeds, then line-specific stops are not overwritten`() = scope.runTest {
        val existingStops = listOf(BusStop("1", "Existing", "Street"))
        val allStops = listOf(BusStop("62", "Gran Via", "Calle 1", lineLabels = setOf("1")))
        val state = MutableStateFlow(AlarmState(selectedLine = line, stops = existingStops))
        val loader = allStopLoader(loadAllBusStops = { allStops }, state = state, scope = scope)

        loader()
        dispatcher.scheduler.advanceUntilIdle()

        state.value.allStops shouldBe allStops
        state.value.stops shouldBe existingStops
    }

    @Test
    fun `given a failing all stops fetch, when all stop loader is invoked, then state contains the error message`() = scope.runTest {
        val state = MutableStateFlow(AlarmState())
        val loader = allStopLoader(
            loadAllBusStops = { error("stop timeout") },
            state = state,
            scope = scope,
        )

        loader()
        dispatcher.scheduler.advanceUntilIdle()

        state.value.isLoadingStops shouldBe false
        state.value.errorMessage shouldBe "stop timeout"
    }

    @Test
    fun `given a successful stops fetch, when loader is invoked, then state contains the stops and isLoadingStops is false`() = scope.runTest {
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
    fun `given a failing stops fetch, when loader is invoked, then state contains the error message`() = scope.runTest {
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
    fun `given an exception with a null message, when loader is invoked, then a fallback error message is shown`() = scope.runTest {
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
