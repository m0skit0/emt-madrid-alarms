package org.m0skit0.android.emtmadridalarms.ui

import io.kotest.matchers.shouldBe
import org.junit.Test

class FuzzySearchTest {

    // --- fuzzyScore ---

    @Test
    fun `given query equals target when scoring then returns 0`() {
        fuzzyScore("526", "526") shouldBe 0
    }

    @Test
    fun `given query equals target case insensitive when scoring then returns 0`() {
        fuzzyScore("abc", "ABC") shouldBe 0
    }

    @Test
    fun `given target contains query as substring when scoring then returns 1`() {
        fuzzyScore("526", "1526 · Puerta del Sol") shouldBe 1
    }

    @Test
    fun `given query chars appear in order but not contiguous when scoring then returns 2`() {
        fuzzyScore("526", "5x2x6") shouldBe 2
    }

    @Test
    fun `given query chars in order with extra chars interspersed when scoring then returns 2`() {
        fuzzyScore("abc", "a1b2c3") shouldBe 2
    }

    @Test
    fun `given query char missing from target when scoring then returns null`() {
        fuzzyScore("526", "52") shouldBe null
    }

    @Test
    fun `given query chars not in order in target when scoring then returns null`() {
        fuzzyScore("526", "265") shouldBe null
    }

    @Test
    fun `given mixed alphanumeric query when scoring fuzzy match then returns non-null`() {
        fuzzyScore("ab3", "a-b--3") shouldBe 2
    }

    @Test
    fun `given query with spaces when scoring against target without spaces then spaces are ignored`() {
        fuzzyScore("cmd for", "comandante fortea") shouldBe 2
    }

    @Test
    fun `given query longer than target when scoring then returns null`() {
        fuzzyScore("abcdef", "abc") shouldBe null
    }

    // --- fuzzyFilter ---

    @Test
    fun `given blank query when filtering then returns all options`() {
        val options = listOf("526 · Sol", "562 · Gran Via", "256 · Atocha")
        fuzzyFilter(options, "   ", { it }) shouldBe options
    }

    @Test
    fun `given exact match when filtering then exact match appears first`() {
        val options = listOf("562 · Gran Via", "526 · Sol", "256 · Atocha")
        val result = fuzzyFilter(options, "526", { it })
        result.first() shouldBe "526 · Sol"
    }

    @Test
    fun `given substring match when filtering then substring match ranked before fuzzy`() {
        val options = listOf("256 · Atocha", "1526 · Gran Via", "562 · Sol")
        val result = fuzzyFilter(options, "526", { it })
        result.first() shouldBe "1526 · Gran Via"
    }

    @Test
    fun `given no matches when filtering then returns empty list`() {
        val options = listOf("111 · Sol", "222 · Gran Via")
        fuzzyFilter(options, "999", { it }) shouldBe emptyList()
    }

    @Test
    fun `given multiple fuzzy matches when filtering then all are included`() {
        val options = listOf("526 · Sol", "5x2x6 · Gran Via", "5xx26 · Atocha", "999 · Retiro")
        val result = fuzzyFilter(options, "526", { it })
        result.size shouldBe 3
        result.none { it == "999 · Retiro" } shouldBe true
    }

    @Test
    fun `given case insensitive query when filtering then matches regardless of case`() {
        val options = listOf("ABC · Line A", "XYZ · Line B")
        fuzzyFilter(options, "abc", { it }).size shouldBe 1
        fuzzyFilter(options, "ABC", { it }).size shouldBe 1
    }

    @Test
    fun `given custom text extractor when filtering then uses extractor for matching`() {
        data class Item(val id: String, val label: String)
        val options = listOf(Item("526", "Sol"), Item("5x2x6", "Gran Via"), Item("999", "Retiro"))
        val result = fuzzyFilter(options, "526", { it.id })
        result.map { it.id } shouldBe listOf("526", "5x2x6")
    }
}
