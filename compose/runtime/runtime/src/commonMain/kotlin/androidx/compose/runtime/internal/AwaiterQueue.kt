/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.internal

import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.OneShotCancellationHandle
import androidx.compose.runtime.internal.AtomicAwaitersCount.Companion.COUNT_BITS
import androidx.compose.runtime.internal.AtomicAwaitersCount.Companion.VERSION_BITS
import androidx.compose.runtime.internal.AwaiterQueue.Awaiter
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.resumeWithException
import kotlin.jvm.JvmInline

internal class AwaiterQueue<A : Awaiter> {

    private val lock = makeSynchronizedObject()
    private var failureCause: Throwable? = null
    private val pendingAwaitersCountUnlocked = AtomicAwaitersCount()
    private var awaiters = mutableObjectListOf<A>()
    private var spareList = mutableObjectListOf<A>()

    /**
     * @return `true` if there are any callers of [addAwaiter] are waiting to resume from a call to
     *   [flushAndDispatchAwaiters].
     */
    val hasAwaiters: Boolean
        get() = pendingAwaitersCountUnlocked.hasAwaiters()

    fun addAwaiter(awaiter: A, onFirstAwaiter: (() -> Unit)?): CancellationHandle {
        var awaitersVersion = -1
        var hasNewAwaiters = false
        synchronized(lock) {
            val cause = failureCause
            if (cause != null) {
                awaiter.resumeWithException(cause)
                return CancellationHandle.Empty
            }
            awaitersVersion =
                pendingAwaitersCountUnlocked.incrementCountAndGetVersion(
                    ifFirstAwaiter = { hasNewAwaiters = true }
                )
            awaiters.add(awaiter)
        }

        if (hasNewAwaiters && onFirstAwaiter != null) {
            try {
                onFirstAwaiter()
            } catch (t: Throwable) {
                fail(t)
            }
        }

        return OneShotCancellationHandle {
            awaiter.cancel()
            pendingAwaitersCountUnlocked.decrementCount(awaitersVersion)
        }
    }

    fun flushAndDispatchAwaiters(resume: (A) -> Unit) {
        synchronized(lock) {
            // Rotate the lists so that if a resumed continuation on an immediate dispatcher
            // bound to the thread calling sendFrame immediately awaits again we don't disrupt
            // iteration of resuming the rest.
            val toResume = awaiters
            awaiters = spareList
            spareList = toResume
            pendingAwaitersCountUnlocked.incrementVersionAndResetCount()

            for (i in 0 until toResume.size) {
                resume(toResume[i])
            }
            toResume.clear()
        }
    }

    fun fail(cause: Throwable) {
        synchronized(lock) {
            if (failureCause != null) return
            failureCause = cause
            awaiters.forEach { awaiter -> awaiter.resumeWithException(cause) }
            awaiters.clear()
            pendingAwaitersCountUnlocked.incrementVersionAndResetCount()
        }
    }

    abstract class Awaiter {

        abstract fun cancel()

        abstract fun resumeWithException(exception: Throwable)
    }
}

/**
 * [AwaiterQueue] tracks the number of pending [Awaiter]s using this atomic type. This count is made
 * up of two components: The count itself ([COUNT_BITS] bits) and a version ([VERSION_BITS] bits).
 *
 * The count is incremented when a new awaiter is added, and decremented when an awaiter is
 * cancelled. When the pending awaiters are processed, this count is reset to zero. To prevent a
 * race condition that can cause an inaccurate count when awaiters are removed, cancelled awaiters
 * only decrement their count when the version of the counter has not changed. The version is
 * incremented every time the awaiters are dispatched and the count resets to zero.
 *
 * The number of bits required to track the version is very small, and the version is allowed and
 * expected to roll over. By allocating 4 bits for the version, cancellation events can be correctly
 * counted as long as the cancellation callback completes within 16 frame dispatches. Most cancelled
 * awaiters will invoke their cancellation logic almost immediately, so even a narrow version range
 * can be highly effective.
 */
@Suppress("NOTHING_TO_INLINE")
@JvmInline
private value class AtomicAwaitersCount private constructor(private val value: AtomicInt) {
    constructor() : this(AtomicInt(0))

    inline fun hasAwaiters(): Boolean = value.get().count > 0

    inline fun incrementVersionAndResetCount() {
        update { pack(version = it.version + 1, count = 0) }
    }

    @OptIn(ExperimentalContracts::class)
    inline fun incrementCountAndGetVersion(ifFirstAwaiter: () -> Unit): Int {
        contract { callsInPlace(ifFirstAwaiter, InvocationKind.AT_MOST_ONCE) }
        val newValue = update { it + 1 }
        if (newValue.count == 1) ifFirstAwaiter()
        return newValue.version
    }

    inline fun decrementCount(version: Int) {
        update { value -> if (value.version == version) value - 1 else value }
    }

    private inline fun update(calculation: (Int) -> Int): Int {
        var oldValue: Int
        var newValue: Int
        do {
            oldValue = value.get()
            newValue = calculation(oldValue)
        } while (!value.compareAndSet(oldValue, newValue))
        return newValue
    }

    /**
     * Bitpacks [version] and [count] together. The topmost bit is always 0 to enforce this value
     * always being positive. [version] takes the next [VERSION_BITS] topmost bits, and [count]
     * takes the remaining [COUNT_BITS] bits.
     *
     * `| 0 | version | count |`
     */
    private fun pack(version: Int, count: Int): Int {
        val versionComponent = (version and (-1 shl VERSION_BITS).inv()) shl COUNT_BITS
        val countComponent = count and (-1 shl COUNT_BITS).inv()
        return versionComponent or countComponent
    }

    private inline val Int.version: Int
        get() = (this ushr COUNT_BITS) and (-1 shl VERSION_BITS).inv()

    private inline val Int.count: Int
        get() = this and (-1 shl COUNT_BITS).inv()

    override fun toString(): String {
        val current = value.get()
        return "AtomicAwaitersCount(version = ${current.version}, count = ${current.count})"
    }

    companion object {
        private const val VERSION_BITS = 4
        private const val COUNT_BITS = Int.SIZE_BITS - VERSION_BITS - 1
    }
}
