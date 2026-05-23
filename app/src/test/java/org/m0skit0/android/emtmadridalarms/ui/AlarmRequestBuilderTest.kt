package org.m0skit0.android.emtmadridalarms.ui

import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

class AlarmRequestValidatorTest {
    private val validator = alarmRequestValidator()

    @Test
    fun `returns null for valid inputs`() {
        validator("1", "62", "10") shouldBe null
    }

    @Test
    fun `rejects blank line`() {
        validator("", "62", "10") shouldBe "Enter a bus line."
    }

    @Test
    fun `rejects blank stop`() {
        validator("1", "", "10") shouldBe "Enter a stop number."
    }

    @Test
    fun `rejects stop with non-digit characters`() {
        validator("1", "A62", "10") shouldBe "Stop number must contain only digits."
    }

    @Test
    fun `rejects zero minutes`() {
        validator("1", "62", "0") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `rejects negative minutes`() {
        validator("1", "62", "-1") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `rejects non-numeric minutes`() {
        validator("1", "62", "abc") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `rejects whitespace-only line`() {
        validator("   ", "62", "10") shouldBe "Enter a bus line."
    }
}

class AlarmRequestBuilderTest {
    private val validator = alarmRequestValidator()
    private val builder = alarmRequestBuilder(validator)

    private val line = BusLine(id = "001", label = "1", nameA = "A", nameB = "B")
    private val stop = BusStop(id = "62", name = "Stop", address = "Street")

    @Test
    fun `returns success with correct request when state is valid`() {
        val state = AlarmState(selectedLine = line, selectedStop = stop, minutesInput = "10")
        builder(state).shouldBeSuccess { request ->
            request shouldBe BusAlarmRequest(line = "1", stopId = "62", targetMinutes = 10)
        }
    }

    @Test
    fun `returns failure when no line selected`() {
        val state = AlarmState(selectedLine = null, selectedStop = stop, minutesInput = "10")
        builder(state).shouldBeFailure { error ->
            error.message shouldContain "bus line"
        }
    }

    @Test
    fun `returns failure when no stop selected`() {
        val state = AlarmState(selectedLine = line, selectedStop = null, minutesInput = "10")
        builder(state).shouldBeFailure { error ->
            error.message shouldContain "stop"
        }
    }

    @Test
    fun `returns failure when minutes is invalid`() {
        val state = AlarmState(selectedLine = line, selectedStop = stop, minutesInput = "0")
        builder(state).shouldBeFailure { error ->
            error.message shouldContain "positive"
        }
    }

    @Test
    fun `delegates field validation to injected validator`() {
        val alwaysError = AlarmRequestValidator { _, _, _ -> "custom error" }
        val builderWithCustomValidator = alarmRequestBuilder(alwaysError)
        val state = AlarmState(selectedLine = line, selectedStop = stop, minutesInput = "10")
        builderWithCustomValidator(state).shouldBeFailure { error ->
            error.message shouldBe "custom error"
        }
    }
}
