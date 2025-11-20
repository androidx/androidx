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

import androidx.compose.runtime.internal.AwaiterQueue
import kotlin.coroutines.resumeWithException
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

    private class FrameAwaiter<R>(onFrame: (Long) -> R, continuation: CancellableContinuation<R>) :
        AwaiterQueue.Awaiter() {

        private var continuation: CancellableContinuation<R>? = continuation
        private var onFrame: ((Long) -> R)? = onFrame

        override fun cancel() {
            onFrame = null
            continuation = null
        }

        override fun resumeWithException(exception: Throwable) {
            continuation?.resumeWithException(exception)
        }

        fun resume(timeNanos: Long) {
            val onFrame = onFrame ?: return
            continuation?.resumeWith(runCatching { onFrame(timeNanos) })
        }
    }

    private val queue = AwaiterQueue<FrameAwaiter<*>>()

    /** `true` if there are any callers of [withFrameNanos] awaiting to run for a pending frame. */
    public val hasAwaiters: Boolean
        get() = queue.hasAwaiters

    /**
     * Send a frame for time [timeNanos] to all current callers of [withFrameNanos]. The `onFrame`
     * callback for each caller is invoked synchronously during the call to [sendFrame].
     */
    public fun sendFrame(timeNanos: Long) {
        queue.flushAndDispatchAwaiters { awaiter -> awaiter.resume(timeNanos) }
    }

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
        suspendCancellableCoroutine { co ->
            val cancellationHandle = queue.addAwaiter(FrameAwaiter(onFrame, co), onNewAwaiters)
            co.invokeOnCancellation { cancellationHandle.cancel() }
        }

    /**
     * Permanently cancel this [BroadcastFrameClock] and cancel all current and future awaiters with
     * [cancellationException].
     */
    public fun cancel(
        cancellationException: CancellationException = CancellationException("clock cancelled")
    ) {
        queue.fail(cancellationException)
    }
}
