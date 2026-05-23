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
    fun `given valid line stop and minutes, when validated, then null is returned`() {
        validator("1", "62", "10") shouldBe null
    }

    @Test
    fun `given a blank line, when validated, then an error is returned`() {
        validator("", "62", "10") shouldBe "Enter a bus line."
    }

    @Test
    fun `given a blank stop, when validated, then an error is returned`() {
        validator("1", "", "10") shouldBe "Enter a stop number."
    }

    @Test
    fun `given a stop with non-digit characters, when validated, then an error is returned`() {
        validator("1", "A62", "10") shouldBe "Stop number must contain only digits."
    }

    @Test
    fun `given zero minutes, when validated, then an error is returned`() {
        validator("1", "62", "0") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `given negative minutes, when validated, then an error is returned`() {
        validator("1", "62", "-1") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `given non-numeric minutes, when validated, then an error is returned`() {
        validator("1", "62", "abc") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `given a whitespace-only line, when validated, then an error is returned`() {
        validator("   ", "62", "10") shouldBe "Enter a bus line."
    }
}

class AlarmRequestBuilderTest {
    private val validator = alarmRequestValidator()
    private val builder = alarmRequestBuilder(validator)

    private val line = BusLine(id = "001", label = "1", nameA = "A", nameB = "B")
    private val stop = BusStop(id = "62", name = "Stop", address = "Street")

    @Test
    fun `given valid line stop and minutes in state, when built, then a success with the correct request is returned`() {
        val state = AlarmState(selectedLine = line, selectedStop = stop, minutesInput = "10")
        builder(state).shouldBeSuccess { request ->
            request shouldBe BusAlarmRequest(line = "1", stopId = "62", targetMinutes = 10)
        }
    }

    @Test
    fun `given no selected line in state, when built, then a failure referencing bus line is returned`() {
        val state = AlarmState(selectedLine = null, selectedStop = stop, minutesInput = "10")
        builder(state).shouldBeFailure { error ->
            error.message shouldContain "bus line"
        }
    }

    @Test
    fun `given no selected stop in state, when built, then a failure referencing stop is returned`() {
        val state = AlarmState(selectedLine = line, selectedStop = null, minutesInput = "10")
        builder(state).shouldBeFailure { error ->
            error.message shouldContain "stop"
        }
    }

    @Test
    fun `given invalid minutes in state, when built, then a failure referencing positive is returned`() {
        val state = AlarmState(selectedLine = line, selectedStop = stop, minutesInput = "0")
        builder(state).shouldBeFailure { error ->
            error.message shouldContain "positive"
        }
    }

    @Test
    fun `given a custom validator that always errors, when built, then its error message is propagated`() {
        val alwaysError = AlarmRequestValidator { _, _, _ -> "custom error" }
        val builderWithCustomValidator = alarmRequestBuilder(alwaysError)
        val state = AlarmState(selectedLine = line, selectedStop = stop, minutesInput = "10")
        builderWithCustomValidator(state).shouldBeFailure { error ->
            error.message shouldBe "custom error"
        }
    }
}
