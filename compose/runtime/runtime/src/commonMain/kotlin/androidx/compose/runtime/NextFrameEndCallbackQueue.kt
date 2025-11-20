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

package androidx.compose.runtime

import androidx.compose.runtime.internal.AtomicBoolean
import androidx.compose.runtime.internal.AwaiterQueue
import kotlinx.coroutines.CancellationException

internal class NextFrameEndCallbackQueue(onNewAwaiters: () -> Unit) {

    private val isFrameOngoing = AtomicBoolean(false)
    private val frameEndQueue = AwaiterQueue<NextFrameEndAwaiter>()
    private val onNewAwaiters: () -> Unit = { if (!isFrameOngoing.get()) onNewAwaiters() }

    /**
     * `true` if there are any callers of [scheduleFrameEndCallback] awaiting to run for a pending
     * frame.
     */
    val hasAwaiters: Boolean
        get() = frameEndQueue.hasAwaiters

    fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle {
        return frameEndQueue.addAwaiter(NextFrameEndAwaiter(action), onNewAwaiters)
    }

    fun markFrameStarted() {
        isFrameOngoing.set(true)
    }

    fun markFrameComplete() {
        isFrameOngoing.set(false)
        frameEndQueue.flushAndDispatchAwaiters { it.resume() }
    }

    fun cancel(
        cancellationException: CancellationException = CancellationException("scheduler cancelled")
    ) {
        frameEndQueue.fail(cancellationException)
    }

    private class NextFrameEndAwaiter(onNextFrameEnd: () -> Unit) : AwaiterQueue.Awaiter() {

        private var onNextFrameEnd: (() -> Unit)? = onNextFrameEnd

        override fun cancel() {
            onNextFrameEnd = null
        }

        fun resume() {
            onNextFrameEnd?.invoke()
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    }
}
