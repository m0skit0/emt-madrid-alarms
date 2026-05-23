package org.m0skit0.android.emtmadridalarms.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.data.linesMatch
import org.m0skit0.android.emtmadridalarms.data.normalizeLine
import org.m0skit0.android.emtmadridalarms.ui.alarmRequestValidator

class BusAlarmTest {
    @Test
    fun `given a numeric line with leading zeroes, when normalized, then leading zeroes are removed`() {
        normalizeLine("001") shouldBe "1"
        linesMatch("1", "001") shouldBe true
    }

    @Test
    fun `given a night line with leading zeroes, when normalized, then prefix is kept and numeric part is stripped`() {
        normalizeLine("n001") shouldBe "N1"
        linesMatch("N1", "n001") shouldBe true
    }

    @Test
    fun `given valid line stop and minutes, when validated, then no error is returned`() {
        alarmRequestValidator()("1", "62", "10") shouldBe null
    }

    @Test
    fun `given a blank line, when validated, then an error is returned`() {
        alarmRequestValidator()("", "62", "10") shouldBe "Enter a bus line."
    }

    @Test
    fun `given a stop with non-digit characters, when validated, then an error is returned`() {
        alarmRequestValidator()("1", "A62", "10") shouldBe "Stop number must contain only digits."
    }

    @Test
    fun `given zero minutes, when validated, then an error is returned`() {
        alarmRequestValidator()("1", "62", "0") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `given negative minutes, when validated, then an error is returned`() {
        alarmRequestValidator()("1", "62", "-5") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `given non-numeric minutes, when validated, then an error is returned`() {
        alarmRequestValidator()("1", "62", "abc") shouldBe "Minutes must be a positive number."
    }
}
