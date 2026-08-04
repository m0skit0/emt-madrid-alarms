package org.m0skit0.android.emtmadridalarms.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RateAppPromptStorageTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeDataStore(scope: TestScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tmpFolder.newFile("test_rate_prompt_prefs.preferences_pb") }
    )

    @Test
    fun `given no prior opens, when recordAppOpen is called, then count is one`() = runTest {
        val ds = makeDataStore(this)

        recordAppOpen(ds)() shouldBe 1
    }

    @Test
    fun `given prior opens, when recordAppOpen is called, then count increments by one`() = runTest {
        val ds = makeDataStore(this)

        recordAppOpen(ds)()
        val result = recordAppOpen(ds)()

        result shouldBe 2
    }

    @Test
    fun `given an empty store, when lastRatePromptShown is called, then it returns zero`() = runTest {
        val ds = makeDataStore(this)

        lastRatePromptShown(ds)() shouldBe 0
    }

    @Test
    fun `given a stored threshold, when lastRatePromptShown is called, then it returns the threshold`() = runTest {
        val ds = makeDataStore(this)

        markRatePromptShown(ds)(5)

        lastRatePromptShown(ds)() shouldBe 5
    }

    @Test
    fun `given an app open count below the first threshold, when nextRatePromptThreshold is called, then it returns null`() {
        nextRatePromptThreshold(appOpenCount = 1, lastPromptedThreshold = 0) shouldBe null
    }

    @Test
    fun `given an app open count at the first threshold, when nextRatePromptThreshold is called, then it returns two`() {
        nextRatePromptThreshold(appOpenCount = 2, lastPromptedThreshold = 0) shouldBe 2
    }

    @Test
    fun `given the two threshold already prompted, when nextRatePromptThreshold is called at two opens, then it returns null`() {
        nextRatePromptThreshold(appOpenCount = 2, lastPromptedThreshold = 2) shouldBe null
    }

    @Test
    fun `given the two threshold already prompted, when nextRatePromptThreshold is called at five opens, then it returns five`() {
        nextRatePromptThreshold(appOpenCount = 5, lastPromptedThreshold = 2) shouldBe 5
    }

    @Test
    fun `given the five threshold already prompted, when nextRatePromptThreshold is called at ten opens, then it returns ten`() {
        nextRatePromptThreshold(appOpenCount = 10, lastPromptedThreshold = 5) shouldBe 10
    }

    @Test
    fun `given all thresholds prompted, when nextRatePromptThreshold is called at more opens, then it returns null`() {
        nextRatePromptThreshold(appOpenCount = 11, lastPromptedThreshold = 10) shouldBe null
    }

    @Test
    fun `given a last prompted threshold ahead of the count, when nextRatePromptThreshold is called, then it returns null`() {
        nextRatePromptThreshold(appOpenCount = 5, lastPromptedThreshold = 10) shouldBe null
    }
}
