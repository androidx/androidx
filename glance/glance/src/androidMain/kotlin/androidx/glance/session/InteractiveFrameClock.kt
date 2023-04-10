/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.session

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A frame clock implementation that supports interactive mode.
 *
 * By default, this frame clock sends frames at its baseline rate. When startInteractive() is
 * called, the frame clock sends frames at its interactive rate so that awaiters can respond more
 * quickly to user interactions. After the interactive timeout is passed, the frame rate is reset to
 * its baseline.
 */
internal class InteractiveFrameClock(
    private val scope: CoroutineScope,
    private val baselineHz: Int = 5,
    private val interactiveHz: Int = 20,
    private val interactiveTimeoutMs: Long = 5_000,
    private val nanoTime: () -> Long = { System.nanoTime() }
) : MonotonicFrameClock {
    companion object {
        private const val NANOSECONDS_PER_SECOND = 1_000_000_000L
        private const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
        private const val TAG = "InteractiveFrameClock"
        private const val DEBUG = false
    }
    private val frameClock: BroadcastFrameClock = BroadcastFrameClock { onNewAwaiters() }
    private val lock = Any()
    private var currentHz = baselineHz
    private var lastFrame = 0L
    private var interactiveCoroutine: CancellableContinuation<Unit>? = null

    /**
     * Set the frame rate to [interactiveHz]. After [interactiveTimeoutMs] has passed, the frame
     * rate is reset to [baselineHz]. If this function is called concurrently with itself, the
     * previous call is cancelled and a new interactive period is started.
     */
    suspend fun startInteractive() = withTimeoutOrNull(interactiveTimeoutMs) {
        stopInteractive()
        suspendCancellableCoroutine { co ->
            if (DEBUG) Log.d(TAG, "Starting interactive mode at ${interactiveHz}hz")
            synchronized(lock) {
                currentHz = interactiveHz
                interactiveCoroutine = co
            }

            co.invokeOnCancellation {
                if (DEBUG) Log.d(TAG, "Resetting frame rate to baseline at ${baselineHz}hz")
                synchronized(lock) {
                    currentHz = baselineHz
                    interactiveCoroutine = null
                }
            }
        }
    }

    /**
     * Cancel the call to startInteractive() if running, and reset the frame rate to baseline.
     */
    fun stopInteractive() {
        synchronized(lock) {
            interactiveCoroutine?.cancel()
        }
    }

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        if (DEBUG) Log.d(TAG, "received frame to run")
        return frameClock.withFrameNanos(onFrame)
    }

    private fun onNewAwaiters() {
        val now = nanoTime()
        val period: Long
        val minPeriod: Long
        synchronized(lock) {
            period = now - lastFrame
            minPeriod = NANOSECONDS_PER_SECOND / currentHz
        }
        if (period >= minPeriod) {
            sendFrame(now)
        } else {
            scope.launch {
                delay((minPeriod - period) / NANOSECONDS_PER_MILLISECOND)
                sendFrame(nanoTime())
            }
        }
    }

    private fun sendFrame(now: Long) {
        if (DEBUG) Log.d(TAG, "Sending next frame")
        frameClock.sendFrame(now)
        synchronized(lock) {
            lastFrame = now
        }
    }

    @VisibleForTesting
    internal fun currentHz() = synchronized(lock) { currentHz }
}