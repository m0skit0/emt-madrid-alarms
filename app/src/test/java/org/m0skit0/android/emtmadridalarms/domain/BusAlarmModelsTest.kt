package org.m0skit0.android.emtmadridalarms.domain

import io.kotest.matchers.shouldBe
import org.junit.Test

class BusLineTest {
    @Test
    fun `given a line with a label, when displayName is read, then label and route are shown`() {
        val line = BusLine(id = "001", label = "1", nameA = "Plaza Mayor", nameB = "Atocha")
        line.displayName shouldBe "1 · Plaza Mayor - Atocha"
    }

    @Test
    fun `given a line with a blank label, when displayName is read, then normalized id is used`() {
        val line = BusLine(id = "001", label = "", nameA = "Plaza Mayor", nameB = "Atocha")
        line.displayName shouldBe "1 · Plaza Mayor - Atocha"
    }

    @Test
    fun `given a line with blank nameA and nameB, when displayName is read, then route separator is omitted`() {
        val line = BusLine(id = "1", label = "1", nameA = "", nameB = "")
        line.displayName shouldBe "1"
    }

    @Test
    fun `given a line with blank nameB, when displayName is read, then nameB is omitted from route`() {
        val line = BusLine(id = "1", label = "1", nameA = "Plaza Mayor", nameB = "")
        line.displayName shouldBe "1 · Plaza Mayor"
    }
}

class BusStopTest {
    @Test
    fun `given a stop with id name and address, when displayName is read, then all parts are shown`() {
        val stop = BusStop(id = "62", name = "Gran Via", address = "Calle Gran Via 1")
        stop.displayName shouldBe "62 · Gran Via · Calle Gran Via 1"
    }

    @Test
    fun `given a stop with a blank name, when displayName is read, then name is omitted`() {
        val stop = BusStop(id = "62", name = "", address = "Calle Gran Via 1")
        stop.displayName shouldBe "62 · Calle Gran Via 1"
    }

    @Test
    fun `given a stop with a blank address, when displayName is read, then address is omitted`() {
        val stop = BusStop(id = "62", name = "Gran Via", address = "")
        stop.displayName shouldBe "62 · Gran Via"
    }

    @Test
    fun `given a stop with padded address whitespace, when displayName is read, then address is trimmed`() {
        val stop = BusStop(id = "62", name = "Gran Via", address = "  Calle Gran Via 1  ")
        stop.displayName shouldBe "62 · Gran Via · Calle Gran Via 1"
    }

    @Test
    fun `given a stop with blank name and address, when displayName is read, then only id is shown`() {
        val stop = BusStop(id = "62", name = "", address = "")
        stop.displayName shouldBe "62"
    }
}

class BusArrivalTest {
    @Test
    fun `given 130 estimateSeconds, when estimateMinutes is read, then floor division returns 2`() {
        val arrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 130, distanceMeters = 0)
        arrival.estimateMinutes shouldBe 2
    }

    @Test
    fun `given less than 60 estimateSeconds, when estimateMinutes is read, then zero is returned`() {
        val arrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 59, distanceMeters = 0)
        arrival.estimateMinutes shouldBe 0
    }

    @Test
    fun `given an exact multiple of 60 estimateSeconds, when estimateMinutes is read, then exact minutes are returned`() {
        val arrival = BusArrival(line = "1", stopId = "62", destination = "A", estimateSeconds = 600, distanceMeters = 0)
        arrival.estimateMinutes shouldBe 10
    }
}
