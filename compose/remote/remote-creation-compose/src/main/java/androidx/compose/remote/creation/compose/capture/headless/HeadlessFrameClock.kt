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

package androidx.compose.remote.creation.compose.capture.headless

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.suspendCancellableCoroutine

private val NotStarted = Any() // Clock created, runClock() not called yet
private val NotStartedFrameRequested = Any() // Frame requested before runClock() was called
private val Idle = Any() // No frame requested, or in the middle of sending one
private val FrameRequested = Any() // Frame requested, the pump will send it on its next loop
private val Cancelled = Any() // The pump completed, or cancel() was called

/** Value of `HeadlessFrameClock.frameEvents` once the clock is cancelled. */
private const val CANCELLED = -1L

/**
 * A [MonotonicFrameClock] for headless compositions that sends a frame as soon as one is requested,
 * counts every frame it sends, and can bound the number of frames sent while a block runs (see
 * [withFrameBudget]).
 *
 * Modelled on `androidx.appstate.transform.HeadlessTransformClock`. Constructing the clock starts
 * nothing: frames requested by awaiters are sent by a single pump coroutine while a caller runs
 * [runClock], on the caller's dispatcher. Frames can also be sent synchronously with [sendFrame],
 * e.g. while driving a composition to quiescence; both paths share the same counter and budget, so
 * content that never stops requesting frames is detected wherever its frames come from. Once a
 * budget is exhausted the pump stops sending frames until the next [withFrameBudget] begins, so
 * such content does not keep the composition busy between the caller's attempts to settle it;
 * awaiters simply wait for that next attempt.
 *
 * Frame times come from [nanoTime] rather than a wall clock so that compositions driven by a test
 * clock see consistent frame times.
 *
 * Frames sent by the pump can be paced with [minFramePeriod], e.g. to stop content that animates
 * through recomposition from keeping a recomposer busy while its documents are being throttled
 * anyway. Pacing never applies to [sendFrame], so a caller driving the composition to quiescence is
 * not slowed down; a caller that wants the paced frames instead waits for them with [awaitFrame].
 *
 * Thread-safe: [sendFrame], [cancel] and frame requests may come from any thread; the pump itself
 * runs on whichever dispatcher [runClock] is called from.
 *
 * @param nanoTime the time stamped on each frame.
 * @param timeSource measures the time between pump frames for [minFramePeriod].
 * @param onFrameSent invoked after every frame, on the sending thread.
 */
internal class HeadlessFrameClock(
    private val nanoTime: () -> Long,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val onFrameSent: () -> Unit = {},
) : MonotonicFrameClock {

    private val state = AtomicReference<Any>(NotStarted)
    private val broadcast = BroadcastFrameClock(::requestFrame)
    private val sent = AtomicLong()
    /**
     * Wakes [awaitFrame]: moves after each frame has been delivered, and after the pump served a
     * request and found no awaiter left, so that a waiter re-checks instead of waiting for a frame
     * nobody asks for. [CANCELLED] once the clock is.
     */
    private val frameEvents = MutableStateFlow(0L)
    @Volatile private var budget: Budget? = null
    /** Set by [sendFrame] when it exhausts a budget; cleared when the next budget begins. */
    @Volatile private var isBudgetExhausted = false
    private var lastPumpFrame: TimeMark? = null

    /**
     * Minimum time between two frames sent by the pump. [Duration.ZERO] (the default) sends frames
     * as soon as they are requested. Takes effect from the next pump frame; a wait already in
     * progress is not shortened.
     */
    @Volatile var minFramePeriod: Duration = Duration.ZERO

    private class Budget(val limit: Long, val job: Job) {
        @Volatile var isExhausted = false
    }

    /** `true` if a [withFrameNanos] caller is currently waiting for a frame. */
    val hasAwaiters: Boolean
        get() = broadcast.hasAwaiters

    /** Total number of frames sent so far. */
    val framesSent: Long
        get() = sent.get()

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        broadcast.withFrameNanos(onFrame)

    /**
     * Pumps requested frames in the calling coroutine, on the caller's dispatcher, until this clock
     * is [cancel]led or the caller is. Returns immediately if the clock was already cancelled.
     *
     * A clock can only be pumped once: it is permanently cancelled when [runClock] returns.
     *
     * @throws IllegalStateException if [runClock] is already running for this clock.
     * @throws CancellationException when the clock is cancelled while pumping frames.
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
     * Sends a frame to the current awaiters now, if there are any, counting it towards the active
     * [withFrameBudget]. A frame with no awaiters is not sent and not counted.
     *
     * The awaiter check is best-effort: a caller racing the pump (or another caller) may both pass
     * it. [BroadcastFrameClock.sendFrame] is synchronized, so the second send finds no awaiters and
     * only inflates the frame count by one. In practice the pump and the callers share one
     * single-threaded dispatcher.
     */
    fun sendFrame() {
        if (!broadcast.hasAwaiters) return
        val count = sent.incrementAndGet()
        budget?.let {
            if (count > it.limit && !it.isExhausted) {
                it.isExhausted = true
                isBudgetExhausted = true
                it.job.cancel(CancellationException("Frame budget exhausted"))
            }
        }
        broadcast.sendFrame(nanoTime())
        signalFrameEvent()
        onFrameSent()
    }

    /**
     * Suspends until a frame is sent after this call, by the pump or by [sendFrame], and its
     * callbacks have run. Also resumes when the pump serves a request and finds that its awaiter
     * has gone meanwhile (cancelled while the pump was pacing), so a caller waiting on behalf of an
     * awaiter must re-check [hasAwaiters]. Does not request a frame: with no awaiter and no pending
     * request this suspends until one appears and is served.
     *
     * @throws CancellationException if the caller is cancelled, including by an exhausted
     *   [withFrameBudget], or if the clock is [cancel]led, like [withFrameNanos].
     */
    suspend fun awaitFrame() {
        val before = frameEvents.value
        val event = if (before == CANCELLED) before else frameEvents.first { it != before }
        if (event == CANCELLED) throw CancellationException("HeadlessFrameClock was cancelled")
    }

    private fun signalFrameEvent() {
        frameEvents.update { if (it == CANCELLED) it else it + 1 }
    }

    /**
     * Runs [block], or returns `null` once more than [maxFrames] frames are sent before it
     * completes.
     *
     * The block is cancelled from [sendFrame] rather than having it check the count, because the
     * block may be suspended on something that never resumes while frames are sent: with an effect
     * in a `withFrameNanos` loop, `Recomposer.currentState` stays `PendingWork` and never emits
     * again. Cancelling launches no coroutine, so it is safe on immediate dispatchers.
     *
     * Budgets do not nest; the innermost call wins.
     */
    suspend fun <T> withFrameBudget(maxFrames: Int, block: suspend CoroutineScope.() -> T): T? {
        var current: Budget? = null
        if (isBudgetExhausted) {
            isBudgetExhausted = false
            // Awaiters that arrived while paused never re-trigger onNewAwaiters; wake the pump.
            if (broadcast.hasAwaiters) requestFrame()
        }
        return try {
            coroutineScope {
                val newBudget = Budget(sent.get() + maxFrames, coroutineContext.job)
                current = newBudget
                budget = newBudget
                try {
                    block()
                } finally {
                    budget = null
                }
            }
        } catch (e: CancellationException) {
            if (current?.isExhausted != true) throw e
            currentCoroutineContext().ensureActive()
            null
        }
    }

    /**
     * Stops the clock and resumes awaiters with a [CancellationException]. Callbacks of a frame
     * already being dispatched still run, as [BroadcastFrameClock.sendFrame] invokes them
     * synchronously.
     */
    fun cancel() {
        val previous = state.getAndSet(Cancelled)
        // Always fail awaiters: the state may already be Cancelled because the pump coroutine was
        // cancelled while parked (see park), in which case nobody has failed them yet.
        // BroadcastFrameClock.cancel is idempotent.
        broadcast.cancel(CancellationException("HeadlessFrameClock was cancelled"))
        frameEvents.value = CANCELLED
        if (previous === Cancelled) return
        @Suppress("UNCHECKED_CAST") // Parked state holds the pump's continuation.
        (previous as? CancellableContinuation<Unit>)?.resume(Unit)
    }

    /** Claims the pump for the caller of [runClock]. Returns `false` if already cancelled. */
    private fun claimPump(): Boolean {
        while (true) {
            when (val current = state.get()) {
                NotStarted -> if (state.compareAndSet(current, Idle)) return true
                NotStartedFrameRequested ->
                    if (state.compareAndSet(current, FrameRequested)) return true
                Cancelled -> return false
                else -> error("runClock() is already running; use one clock per composition.")
            }
        }
    }

    private suspend fun pumpFrames(): Nothing {
        while (true) {
            currentCoroutineContext().ensureActive()
            when (val current = state.get()) {
                // Consume the request before sending, otherwise a request arriving while the frame
                // is dispatched would be dropped. A request arriving during Idle is picked up on
                // the next loop.
                FrameRequested ->
                    if (state.compareAndSet(current, Idle) && !isBudgetExhausted) {
                        awaitFramePeriod()
                        val before = sent.get()
                        sendFrame()
                        // The awaiter went away while pacing: wake awaitFrame callers to re-check.
                        if (sent.get() == before) signalFrameEvent()
                    }
                Idle -> park()
                Cancelled -> throw CancellationException("HeadlessFrameClock was cancelled")
                else -> error("Unexpected state while pumping frames: $current")
            }
        }
    }

    /** Waits until [minFramePeriod] has elapsed since the previous pump frame, if it is set. */
    private suspend fun awaitFramePeriod() {
        val period = minFramePeriod
        if (period > Duration.ZERO) {
            val remaining = lastPumpFrame?.let { period - it.elapsedNow() }
            // delay() may run on a different clock than timeSource (e.g. virtual time), so the
            // mark is taken after the wait rather than advanced by the period.
            if (remaining != null && remaining > Duration.ZERO) delay(remaining)
        }
        lastPumpFrame = timeSource.markNow()
    }

    private suspend fun park() {
        suspendCancellableCoroutine { continuation ->
            if (state.compareAndSet(Idle, continuation)) {
                // Parked -> Cancelled if the pump coroutine is cancelled while parked.
                continuation.invokeOnCancellation { state.compareAndSet(continuation, Cancelled) }
            } else {
                // A request or cancellation raced the park; resume and loop.
                continuation.resume(Unit)
            }
        }
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
                // Parked: wake the pump.
                else ->
                    if (state.compareAndSet(current, FrameRequested)) {
                        @Suppress("UNCHECKED_CAST") // Parked state holds the pump's continuation.
                        (current as CancellableContinuation<Unit>).resume(Unit)
                        return
                    }
            }
        }
    }
}
