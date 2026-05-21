package org.m0skit0.android.emtmadridalarms.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.data.linesMatch
import org.m0skit0.android.emtmadridalarms.data.normalizeLine

class BusAlarmTest {
    @Test
    fun `numeric line normalization removes leading zeroes`() {
        assertEquals("1", normalizeLine("001"))
        assertTrue(linesMatch("1", "001"))
    }

    @Test
    fun `night line normalization keeps prefix and removes numeric leading zeroes`() {
        assertEquals("N1", normalizeLine("n001"))
        assertTrue(linesMatch("N1", "n001"))
    }

    @Test
    fun `alarm triggers when estimate is inside target window`() {
        assertTrue(shouldTriggerAlarm(600, 10))
        assertTrue(shouldTriggerAlarm(599, 10))
        assertFalse(shouldTriggerAlarm(601, 10))
    }

    @Test
    fun `alarm ignores unavailable estimate sentinel`() {
        assertFalse(shouldTriggerAlarm(999999, 10))
    }

    @Test
    fun `request validation accepts valid values`() {
        assertNull(validateAlarmRequest("1", "62", "10"))
    }

    @Test
    fun `request validation rejects invalid values`() {
        assertEquals("Enter a bus line.", validateAlarmRequest("", "62", "10"))
        assertEquals("Stop number must contain only digits.", validateAlarmRequest("1", "A62", "10"))
        assertEquals("Minutes must be a positive number.", validateAlarmRequest("1", "62", "0"))
    }
}

private fun shouldTriggerAlarm(estimateSeconds: Int, targetMinutes: Int): Boolean {
    if (targetMinutes <= 0) return false
    if (estimateSeconds < 0 || estimateSeconds == 999999) return false
    return estimateSeconds <= targetMinutes * 60
}

private fun validateAlarmRequest(line: String, stopId: String, minutes: String): String? {
    if (line.isBlank()) return "Enter a bus line."
    if (stopId.isBlank()) return "Enter a stop number."
    if (!stopId.all(Char::isDigit)) return "Stop number must contain only digits."
    val targetMinutes = minutes.toIntOrNull()
    if (targetMinutes == null || targetMinutes <= 0) return "Minutes must be a positive number."
    return null
}
