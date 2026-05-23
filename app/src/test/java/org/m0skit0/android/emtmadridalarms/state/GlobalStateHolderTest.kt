package org.m0skit0.android.emtmadridalarms.state

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GlobalStateHolderTest {
    @Test
    fun `given an initial state, when state is read, then the initial state is returned`() {
        val initial = AppState()
        val holder = GlobalStateHolder(initial)
        holder.state shouldBe initial
    }

    @Test
    fun `given a holder with default state, when update is called, then state is not null`() {
        val holder = GlobalStateHolder(AppState())
        holder.update { it.copy() }
        holder.state shouldNotBe null
    }

    @Test
    fun `given a holder with default state, when a sub-state field is updated, then the new value is reflected`() {
        val holder = GlobalStateHolder(AppState())
        holder.update { it.copy(alarmStorage = it.alarmStorage.copy(isRinging = true)) }
        holder.state.alarmStorage.isRinging shouldBe true
    }

    @Test
    fun `given a holder with default state, when update is called multiple times sequentially, then all writes accumulate`() {
        val holder = GlobalStateHolder(AppState())
        repeat(5) {
            holder.update { it.copy(alarmStorage = it.alarmStorage.copy(isRinging = true)) }
        }
        holder.state.alarmStorage.isRinging shouldBe true
    }

    @Test
    fun `given 100 concurrent writers, when all updates complete, then no writes are lost`() {
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
