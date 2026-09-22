/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appstate.transform

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import kotlin.coroutines.resume
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine

private val NotStarted = Any() // Clock created, not called yet
private val NotStartedFrameRequested = Any() // Frame requested before `runClock` was called
private val Idle = Any() // Either have no work, or in the middle of sending a frame
private val FrameRequested = Any() // New frame requested, will be sent in next loop
private val Cancelled = Any() // Either coroutine died, or `cancel()` was called

/**
 * A [MonotonicFrameClock] that sends a frame as soon as one is requested, for headless compositions
 * that reconcile state outside of a UI display pipeline.
 *
 * Constructing a clock starts nothing. Frames are dispatched only while a caller runs [runClock],
 * and stop once that caller is cancelled or [cancel] is called.
 */
internal class HeadlessTransformClock(timeSource: TimeSource = TimeSource.Monotonic) :
    MonotonicFrameClock {

    private val state = AtomicReference(NotStarted)
    private val startMark = timeSource.markNow()
    private val broadcast = BroadcastFrameClock(::requestFrame)

    private var lastFrameTimeNanos = 0L
    private var lastOffsetNanos = 0

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        broadcast.withFrameNanos(onFrame)

    /**
     * Pumps frames in the calling coroutine, on the caller's context, until this clock is canceled
     * or the caller is.
     *
     * This clock will only be usable for a single call to [runClock] as it will be permanently be
     * canceled once frames are pumped.
     *
     * @throws IllegalStateException if a pump is already running for this clock.
     * @throws CancellationException when the clock is canceled while pumping frames.
     */
    suspend fun runClock() {
        if (!claimPump()) return
        try {
            pumpFrames()
        } finally {
            cancel()
        }
    }

    /**
     * Stops the clock and resumes awaiters with a [CancellationException]. Callbacks of a frame
     * already being dispatched still run, as `sendFrame` invokes them synchronously.
     */
    fun cancel() {
        val previous = state.getAndSet(Cancelled)
        if (previous === Cancelled) return
        broadcast.cancel(CancellationException("HeadlessTransformClock was cancelled"))
        @Suppress("UNCHECKED_CAST") // needed due to variance
        (previous as? CancellableContinuation<Unit>)?.resume(Unit)
    }

    /**
     * The `pump` is the single coroutine executing the while loop in [claimPump] invoked by
     * [runClock].
     */
    private fun claimPump(): Boolean {
        while (true) {
            when (val current = state.get()) {
                NotStarted -> if (state.compareAndSet(current, Idle)) return true
                NotStartedFrameRequested ->
                    if (state.compareAndSet(current, FrameRequested)) return true
                Cancelled -> return false
                else ->
                    error(
                        "runClock() is already running; one clock per composition should be used."
                    )
            }
        }
    }

    private suspend fun pumpFrames(): Nothing {
        while (true) {
            currentCoroutineContext().ensureActive()
            when (val current = state.get()) {
                // We need to consume the request before dispatching via `sendFrame`, otherwise a
                // request that arrives during `sendFrame` will be dropped. This way a request
                // arriving during the `Idle` state will be picked up in the next frame.
                FrameRequested ->
                    if (state.compareAndSet(current, Idle)) {
                        broadcast.sendFrame(nextFrameTimeNanos())
                    }
                Idle -> park()
                Cancelled -> throw CancellationException("HeadlessTransformClock was cancelled")
                else -> error("Unexpected state while pumping frames: $current")
            }
        }
    }

    private suspend fun park() {
        suspendCancellableCoroutine { continuation ->
            if (state.compareAndSet(Idle, continuation)) {
                // register cancellation cleanup if we go from Parked state -> Cancelled
                continuation.invokeOnCancellation { state.compareAndSet(continuation, Cancelled) }
            } else {
                // resume because we have new work
                continuation.resume(Unit)
            }
        }
    }

    // TODO(elifbilgin@): Will be revised once new scheduler API is ready.
    private fun nextFrameTimeNanos(): Long {
        // Nanoseconds since the clock was created
        val elapsedNanos = startMark.elapsedNow().inWholeNanoseconds

        // Since we only have millisecond resolution on some platforms, ensure the nanos form always
        // increases by incrementing a nano offset if we collide with the previous timestamp.
        val offset =
            if (elapsedNanos == lastFrameTimeNanos) {
                lastOffsetNanos + 1
            } else {
                lastFrameTimeNanos = elapsedNanos
                0
            }
        lastOffsetNanos = offset

        return elapsedNanos + offset
    }

    /** Invoked by [broadcast] when its awaiter count goes from zero to one. */
    private fun requestFrame() {
        while (true) {
            when (val current = state.get()) {
                FrameRequested,
                NotStartedFrameRequested,
                Cancelled -> return
                NotStarted -> if (state.compareAndSet(current, NotStartedFrameRequested)) return
                Idle -> if (state.compareAndSet(current, FrameRequested)) return
                // No work available, suspending "Parked" state
                else ->
                    if (state.compareAndSet(current, FrameRequested)) {
                        @Suppress("UNCHECKED_CAST") // needed due to variance
                        (current as CancellableContinuation<Unit>).resume(Unit)
                        return
                    }
            }
        }
    }
}
