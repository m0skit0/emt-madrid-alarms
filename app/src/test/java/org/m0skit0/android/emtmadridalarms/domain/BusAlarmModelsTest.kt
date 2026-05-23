package org.m0skit0.android.emtmadridalarms.domain

import io.kotest.matchers.shouldBe
import org.junit.Test

class BusLineTest {
    @Test
    fun `displayName uses label when present`() {
        val line = BusLine(id = "001", label = "1", nameA = "Plaza Mayor", nameB = "Atocha")
        line.displayName shouldBe "1 · Plaza Mayor - Atocha"
    }

    @Test
    fun `displayName falls back to normalized id when label is blank`() {
        val line = BusLine(id = "001", label = "", nameA = "Plaza Mayor", nameB = "Atocha")
        line.displayName shouldBe "1 · Plaza Mayor - Atocha"
    }

    @Test
    fun `displayName omits route separator when both names are blank`() {
        val line = BusLine(id = "1", label = "1", nameA = "", nameB = "")
        line.displayName shouldBe "1"
    }

    @Test
    fun `displayName omits blank nameB from route`() {
        val line = BusLine(id = "1", label = "1", nameA = "Plaza Mayor", nameB = "")
        line.displayName shouldBe "1 · Plaza Mayor"
    }
}

class BusStopTest {
    @Test
    fun `displayName includes id, name and address`() {
        val stop = BusStop(id = "62", name = "Gran Via", address = "Calle Gran Via 1")
        stop.displayName shouldBe "62 · Gran Via · Calle Gran Via 1"
    }

    @Test
    fun `displayName omits blank name`() {
        val stop = BusStop(id = "62", name = "", address = "Calle Gran Via 1")
        stop.displayName shouldBe "62 · Calle Gran Via 1"
    }

    @Test
    fun `displayName omits blank address`() {
        val stop = BusStop(id = "62", name = "Gran Via", address = "")
        stop.displayName shouldBe "62 · Gran Via"
    }

    @Test
    fun `displayName trims address whitespace`() {
        val stop = BusStop(id = "62", name = "Gran Via", address = "  Calle Gran Via 1  ")
        stop.displayName shouldBe "62 · Gran Via · Calle Gran Via 1"
    }

    @Test
    fun `displayName is just id when name and address are blank`() {
        val stop = BusStop(id = "62", name = "", address = "")
        stop.displayName shouldBe "62"
    }
}

class BusArrivalTest {
    @Test
    fun `estimateMinutes is floor division of estimateSeconds`() {
        val arrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 130, distanceMeters = 0)
        arrival.estimateMinutes shouldBe 2
    }

    @Test
    fun `estimateMinutes is zero for less than 60 seconds`() {
        val arrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 59, distanceMeters = 0)
        arrival.estimateMinutes shouldBe 0
    }

    @Test
    fun `estimateMinutes is exact for round minutes`() {
        val arrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 600, distanceMeters = 0)
        arrival.estimateMinutes shouldBe 10
    }
}
