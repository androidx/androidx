/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.internal.AtomicInt
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.resumeWithException
import kotlin.jvm.JvmInline
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A simple frame clock.
 *
 * This implementation is intended for low-contention environments involving low total numbers of
 * threads in a pool on the order of ~number of CPU cores available for UI recomposition work, while
 * avoiding additional allocation where possible.
 *
 * [onNewAwaiters] will be invoked whenever the number of awaiters has changed from 0 to 1. If
 * [onNewAwaiters] **fails** by throwing an exception it will permanently fail this
 * [BroadcastFrameClock]; all current and future awaiters will resume with the thrown exception.
 */
public class BroadcastFrameClock(private val onNewAwaiters: (() -> Unit)? = null) :
    MonotonicFrameClock {

    private class FrameAwaiter<R>(onFrame: (Long) -> R, continuation: CancellableContinuation<R>) {
        private var onFrame: ((Long) -> R)? = onFrame
        private var continuation: (CancellableContinuation<R>)? = continuation

        fun cancel() {
            onFrame = null
            continuation = null
        }

        fun resume(timeNanos: Long) {
            val onFrame = onFrame ?: return
            continuation?.resumeWith(runCatching { onFrame(timeNanos) })
        }

        fun resumeWithException(exception: Throwable) {
            continuation?.resumeWithException(exception)
        }
    }

    private val lock = makeSynchronizedObject()
    private var failureCause: Throwable? = null
    private val pendingAwaitersCountUnlocked = AtomicAwaitersCount()
    private var awaiters = mutableObjectListOf<FrameAwaiter<*>>()
    private var spareList = mutableObjectListOf<FrameAwaiter<*>>()

    /** `true` if there are any callers of [withFrameNanos] awaiting to run for a pending frame. */
    public val hasAwaiters: Boolean
        get() = pendingAwaitersCountUnlocked.hasAwaiters()

    /**
     * Send a frame for time [timeNanos] to all current callers of [withFrameNanos]. The `onFrame`
     * callback for each caller is invoked synchronously during the call to [sendFrame].
     */
    public fun sendFrame(timeNanos: Long) {
        synchronized(lock) {
            // Rotate the lists so that if a resumed continuation on an immediate dispatcher
            // bound to the thread calling sendFrame immediately awaits again we don't disrupt
            // iteration of resuming the rest.
            val toResume = awaiters
            awaiters = spareList
            spareList = toResume
            pendingAwaitersCountUnlocked.incrementVersionAndResetCount()

            for (i in 0 until toResume.size) {
                toResume[i].resume(timeNanos)
            }
            toResume.clear()
        }
    }

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
        suspendCancellableCoroutine { co ->
            val awaiter = FrameAwaiter(onFrame, co)
            var hasNewAwaiters = false
            var awaitersVersion = -1
            synchronized(lock) {
                val cause = failureCause
                if (cause != null) {
                    co.resumeWithException(cause)
                    return@suspendCancellableCoroutine
                }
                awaitersVersion =
                    pendingAwaitersCountUnlocked.incrementCountAndGetVersion(
                        ifFirstAwaiter = { hasNewAwaiters = true }
                    )
                awaiters.add(awaiter)
            }

            co.invokeOnCancellation {
                awaiter.cancel()
                pendingAwaitersCountUnlocked.decrementCount(awaitersVersion)
            }

            // Wake up anything that was waiting for someone to schedule a frame
            if (hasNewAwaiters && onNewAwaiters != null) {
                try {
                    // BUG: Kotlin 1.4.21 plugin doesn't smart cast for a direct onNewAwaiters()
                    // here
                    onNewAwaiters.invoke()
                } catch (t: Throwable) {
                    // If onNewAwaiters fails, we permanently fail the BroadcastFrameClock.
                    fail(t)
                }
            }
        }

    private fun fail(cause: Throwable) {
        synchronized(lock) {
            if (failureCause != null) return
            failureCause = cause
            awaiters.forEach { awaiter -> awaiter.resumeWithException(cause) }
            awaiters.clear()
            pendingAwaitersCountUnlocked.incrementVersionAndResetCount()
        }
    }

    /**
     * Permanently cancel this [BroadcastFrameClock] and cancel all current and future awaiters with
     * [cancellationException].
     */
    public fun cancel(
        cancellationException: CancellationException = CancellationException("clock cancelled")
    ) {
        fail(cancellationException)
    }

    /**
     * [BroadcastFrameClock] tracks the number of pending [FrameAwaiter]s using this atomic type.
     * This count is made up of two components: The count itself ([COUNT_BITS] bits) and a version
     * ([VERSION_BITS] bits).
     *
     * The count is incremented when a new awaiter is added, and decremented when an awaiter is
     * cancelled. When the pending awaiters are processed, this count is reset to zero. To prevent a
     * race condition that can cause an inaccurate count when awaiters are removed, cancelled
     * awaiters only decrement their count when the version of the counter has not changed. The
     * version is incremented every time the awaiters are dispatched and the count resets to zero.
     *
     * The number of bits required to track the version is very small, and the version is allowed
     * and expected to roll over. By allocating 4 bits for the version, cancellation events can be
     * correctly counted as long as the cancellation callback completes within 16 [sendFrame]
     * invocations. Most cancelled awaiters will invoke their cancellation logic almost immediately,
     * so even a narrow version range can be highly effective.
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
         * Bitpacks [version] and [count] together. The topmost bit is always 0 to enforce this
         * value always being positive. [version] takes the next [VERSION_BITS] topmost bits, and
         * [count] takes the remaining [COUNT_BITS] bits.
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
}
