package org.m0skit0.android.emtmadridalarms.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.data.linesMatch
import org.m0skit0.android.emtmadridalarms.data.normalizeLine
import org.m0skit0.android.emtmadridalarms.ui.alarmRequestValidator

class BusAlarmTest {
    @Test
    fun `numeric line normalization removes leading zeroes`() {
        normalizeLine("001") shouldBe "1"
        linesMatch("1", "001") shouldBe true
    }

    @Test
    fun `night line normalization keeps prefix and removes numeric leading zeroes`() {
        normalizeLine("n001") shouldBe "N1"
        linesMatch("N1", "n001") shouldBe true
    }

    @Test
    fun `alarm request validator accepts valid values`() {
        alarmRequestValidator()("1", "62", "10") shouldBe null
    }

    @Test
    fun `alarm request validator rejects blank line`() {
        alarmRequestValidator()("", "62", "10") shouldBe "Enter a bus line."
    }

    @Test
    fun `alarm request validator rejects non-digit stop`() {
        alarmRequestValidator()("1", "A62", "10") shouldBe "Stop number must contain only digits."
    }

    @Test
    fun `alarm request validator rejects zero minutes`() {
        alarmRequestValidator()("1", "62", "0") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `alarm request validator rejects negative minutes`() {
        alarmRequestValidator()("1", "62", "-5") shouldBe "Minutes must be a positive number."
    }

    @Test
    fun `alarm request validator rejects non-numeric minutes`() {
        alarmRequestValidator()("1", "62", "abc") shouldBe "Minutes must be a positive number."
    }
}
