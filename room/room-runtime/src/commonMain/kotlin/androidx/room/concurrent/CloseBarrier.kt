/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.concurrent

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.loop

/**
 * A barrier that can be used to perform a cleanup action once, waiting for registered parties
 * (blockers) to finish using the protected resource.
 *
 * Usage is similar to a 'withLock':
 * ```
 * closeBarrier.ifNotClosed {
 *    // Will enter the block if close() has not been called,
 *    // while also preventing the close() action from occurring.
 * }
 * ```
 *
 * Ideally we would use a read-write mutex, but it does not exist yet, see
 * https://github.com/Kotlin/kotlinx.coroutines/issues/94.
 *
 * @param [closeAction] The action to be performed exactly once and when there are no pending
 *   blockers.
 */
internal class CloseBarrier(private val closeAction: () -> Unit) : SynchronizedObject() {
    private val blockers = atomic(0)
    private val closeInitiated = atomic(false)
    private val isClosed by closeInitiated

    /**
     * Blocks the [closeAction] from occurring.
     *
     * A call to this function must be balanced with [unblock] after.
     *
     * @return `true` if the block is registered and the resource is protected from closing, or
     *   `false` if [close] has been called and the block is not registered.
     * @see ifNotClosed
     */
    internal fun block(): Boolean =
        synchronized(this) {
            if (isClosed) {
                return false
            }
            blockers.incrementAndGet()
            return true
        }

    /**
     * Unblocks the [closeAction] from occurring.
     *
     * A call to this function must be balanced with [block] before.
     *
     * @see ifNotClosed
     */
    internal fun unblock(): Unit =
        synchronized(this) {
            blockers.decrementAndGet()
            check(blockers.value >= 0) { "Unbalanced call to unblock() detected." }
        }

    /**
     * Executes the [closeAction] once there are no blockers.
     *
     * If there are any pending blockers, it will wait until all blockers are unblocked, and then
     * execute the [closeAction]. In other words, executes the [closeAction] once no callers of this
     * object are performing the [ifNotClosed] action or alternatively all callers of [block] have
     * called their [unblock].
     */
    internal fun close() {
        synchronized(this) {
            if (!closeInitiated.compareAndSet(expect = false, update = true)) {
                // already closed, do nothing
                return
            }
        }
        blockers.loop { count ->
            if (count == 0) {
                return closeAction.invoke()
            }
        }
    }
}

/** Executes the [action] if [CloseBarrier.close] has not been called on this object. */
internal inline fun CloseBarrier.ifNotClosed(action: () -> Unit) {
    if (!block()) return
    try {
        action.invoke()
    } finally {
        unblock()
    }
}
