package org.m0skit0.android.emtmadridalarms.state

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GlobalStateHolderTest {
    @Test
    fun `initial state is returned on first read`() {
        val initial = AppState()
        val holder = GlobalStateHolder(initial)
        holder.state shouldBe initial
    }

    @Test
    fun `update applies block and stores new state`() {
        val holder = GlobalStateHolder(AppState())
        holder.update { it.copy() }
        holder.state shouldNotBe null
    }

    @Test
    fun `update reflects changed sub-state`() {
        val holder = GlobalStateHolder(AppState())
        holder.update { it.copy(alarmStorage = it.alarmStorage.copy(isRinging = true)) }
        holder.state.alarmStorage.isRinging shouldBe true
    }

    @Test
    fun `multiple sequential updates accumulate correctly`() {
        val holder = GlobalStateHolder(AppState())
        repeat(5) {
            holder.update { it.copy(alarmStorage = it.alarmStorage.copy(isRinging = true)) }
        }
        holder.state.alarmStorage.isRinging shouldBe true
    }

    @Test
    fun `concurrent updates do not lose writes`() {
        val threads = 100
        val executor = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(threads)
        val holder = GlobalStateHolder(AppState())

        repeat(threads) { i ->
            executor.submit {
                holder.update { it.copy(alarmStorage = it.alarmStorage.copy(statusMessage = "msg-$i")) }
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS) shouldBe true
        holder.state.alarmStorage.statusMessage shouldNotBe ""
        executor.shutdown()
    }
}
