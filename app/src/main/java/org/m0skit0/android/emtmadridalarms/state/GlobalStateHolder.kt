package org.m0skit0.android.emtmadridalarms.state

import java.util.concurrent.atomic.AtomicReference
import org.m0skit0.android.emtmadridalarms.data.EmtAuthTokenState
import org.m0skit0.android.emtmadridalarms.service.AlarmMonitorState

/**
 * A thread-safe holder for the application's global mutable state.
 *
 * @param initialState The initial state of the application.
 */
class GlobalStateHolder(initialState: AppState) {

    private val _state = AtomicReference(initialState)
    val state: AppState
        get() = _state.get()

    /**
     * Atomically updates the state using a lock-free optimistic retry loop.
     *
     * This function repeatedly:
     * 1. Reads the current state.
     * 2. Applies the `block` to compute the new state.
     * 3. Tries to atomically set the new state using `compareAndSet`.
     * If another thread modified the state in the meantime, `compareAndSet` fails,
     * and the loop retries with the newly updated state. This ensures updates are
     * never lost.
     */
    fun update(block: (AppState) -> AppState) {
        while (true) {
            val old = _state.get()
            val new = block(old)
            if (_state.compareAndSet(old, new)) {
                return
            }
        }
    }
}
