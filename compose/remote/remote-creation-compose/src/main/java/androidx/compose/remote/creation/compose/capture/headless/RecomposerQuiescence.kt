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

package androidx.compose.remote.creation.compose.capture.headless

import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

internal const val MAX_IDLE_ITERATIONS = 100

/**
 * `Recomposer.runRecomposeAndApplyChanges` updates `currentState` inside `withFrameNanos` before
 * `NextFrameEndCallbackQueue.markFrameComplete()` drains frame-end callbacks (such as
 * `LocalRetainedValuesStoreProvider`), leaving `currentState` at `PendingWork` even after
 * `hasPendingWork` becomes `false`. Toggling `pauseCompositionFrameClock` /
 * `resumeCompositionFrameClock` re-runs `deriveStateLocked()` once `hasPendingWork` is `false`.
 *
 * TODO(b/573873845): Remove once Compose Runtime re-derives the state after frame-end callbacks.
 */
private fun Recomposer.refreshStateIfIdle() {
    if (currentState.value == Recomposer.State.PendingWork && !hasPendingWork) {
        pauseCompositionFrameClock()
        resumeCompositionFrameClock()
    }
}

/**
 * Called after every frame sent by a capture's [HeadlessFrameClock]: refreshes the state now and,
 * if the frame left the recomposer with work to do, once more after that work has been dispatched
 * on [recomposerDispatcher].
 */
internal fun Recomposer.refreshStateIfIdle(recomposerDispatcher: CoroutineContext) {
    refreshStateIfIdle()
    if (currentState.value == Recomposer.State.PendingWork) {
        val dispatcher = recomposerDispatcher[ContinuationInterceptor] as? CoroutineDispatcher
        if (dispatcher != null && dispatcher.isDispatchNeeded(recomposerDispatcher)) {
            dispatcher.dispatch(recomposerDispatcher, Runnable { refreshStateIfIdle() })
        }
    }
}

/**
 * Upper bound for a single wait on another thread's in-flight apply observer notifications (see
 * [awaitApplyObserverNotifications]). Such notifications normally complete in microseconds; the
 * bound only matters if the notifying thread is descheduled or an observer is slow.
 */
private val APPLY_NOTIFICATION_WAIT: Duration = 50.milliseconds

/**
 * Upper bound on polls in a single [awaitApplyObserverNotifications] call. This is a second bound
 * that does not depend on the time source, so the wait terminates even if the monotonic clock does
 * not advance as expected on some host.
 */
private const val APPLY_NOTIFICATION_MAX_POLLS = 10_000

/**
 * Upper bound on frames sent within one [awaitRecomposerQuiescence] call.
 *
 * Remote documents animate on the player through remote expressions, and a captured document is
 * expected to change rarely, so settling should only ever take a handful of frames (e.g. effects
 * resuming from a one-shot `withFrameNanos`). Frames are sent as soon as they are awaited, not
 * paced to a display refresh rate, so content that requests a frame from every frame (an infinite
 * transition, or a `while (true) withFrameNanos {}` loop) never lets the recomposer settle.
 * Compose-driven animations are therefore not supported: a single capture fails, and a streaming
 * capture renders whatever state was reached.
 */
internal const val MAX_FRAMES = 100

/** Outcome of [awaitRecomposerQuiescence]. */
internal enum class QuiescenceResult {
    /** The recomposer settled and all observed writes have been composed. */
    Quiescent,

    /** The iteration bound was exhausted, e.g. content that mutates state on every frame. */
    Unstable,

    /**
     * Content requested more than [MAX_FRAMES] frames, e.g. an infinite animation. Compose-driven
     * animations cannot be captured as they run; remote documents should express animation with
     * remote expressions instead.
     */
    ContinuousFrames,

    /**
     * Another thread's apply observer notifications did not complete within
     * [APPLY_NOTIFICATION_WAIT]. Waiting is abandoned immediately rather than retried, so that a
     * slow or blocked foreign observer cannot stall the capture for [MAX_IDLE_ITERATIONS] waits.
     */
    ApplyNotificationsTimedOut,
}

/**
 * Suspends until [recomposer] has settled, i.e. it is [Recomposer.State.Idle] with no pending work,
 * [frameClock] has no awaiters, and no snapshot apply observer notifications are in flight.
 *
 * A single Idle await is insufficient because:
 * 1. Effects launched during composition suspend on the external [frameClock] (whose awaiters are
 *    tracked by [HeadlessFrameClock.hasAwaiters], not by [Recomposer.hasPendingWork]),
 * 2. Continuations resuming from `withFrameNanos {}` are dispatched onto [recomposerDispatcher]
 *    after [HeadlessFrameClock.sendFrame] completes, and
 * 3. Another thread concurrently inside [Snapshot.sendApplyNotifications] (e.g.
 *    [SnapshotWriteMonitor], or an app's own `GlobalSnapshotManager`) may have claimed pending
 *    global writes while its apply observer notifications are still in flight (tracked by
 *    [Snapshot.isApplyObserverNotificationPending]). Until they complete,
 *    [Recomposer.hasPendingWork] cannot yet reflect those writes, so this waits for them to drain
 *    via [awaitApplyObserverNotifications] before sampling.
 *
 * Limitation of (3): a `MutableSnapshot.apply()` on another thread also claims pending global
 * writes and notifies observers after releasing the global lock, but the runtime does not count
 * those notifications in [Snapshot.isApplyObserverNotificationPending] (b/573872754). This
 * capture's own writes claimed that way are also waited for via [writeTracker] (see
 * [CaptureWriteTracker]); writes from elsewhere in the process are not covered. This session's own
 * render snapshots do not widen the window, as they are never applied unless writes during render
 * are explicitly allowed (see [withRenderSnapshot]).
 *
 * Note that [Recomposer.currentState] is only ever sampled by value here, never used to detect a
 * transition, so conflation of the underlying [StateFlow] cannot cause a missed wake-up.
 *
 * Content that never stops requesting frames (e.g. an infinite animation) can never be quiescent,
 * and the Recomposer may never even be observed [Recomposer.State.Idle] while [frameClock] keeps
 * being driven: it stays [Recomposer.State.PendingWork], so [Recomposer.currentState] never emits
 * again. Every frame sent for this capture (by this loop or by [frameClock]'s pump) is counted by
 * [frameClock], which cancels this wait with [QuiescenceResult.ContinuousFrames] once more than
 * [MAX_FRAMES] frames were sent during this call, wherever it is suspended. A slow composition that
 * sends no frames never trips it.
 *
 * @param frameClock sends and counts every frame for the capture, see [HeadlessFrameClock].
 * @param maxIterations bound on the number of non-quiescent iterations before giving up.
 * @param writeTracker tracks this capture's undelivered global writes, see [CaptureWriteTracker].
 * @param pacedFrames how frames requested while settling are delivered. `false` (the default) sends
 *   them from this call as soon as they are requested, which settles as fast as possible. `true`
 *   leaves them to [frameClock]'s pump, so they are paced by [HeadlessFrameClock.minFramePeriod]:
 *   settling then takes as long as the content's frames take at that rate, which is what a
 *   long-lived host that limits its frame rate wants. Requires the pump to be running
 *   ([HeadlessFrameClock.runClock]). Frames count towards [maxFrames] either way.
 * @param maxFrames the frame budget for this call, see [MAX_FRAMES].
 */
internal suspend fun awaitRecomposerQuiescence(
    recomposer: Recomposer,
    frameClock: HeadlessFrameClock,
    recomposerDispatcher: CoroutineContext,
    writeTracker: CaptureWriteTracker,
    maxIterations: Int = MAX_IDLE_ITERATIONS,
    pacedFrames: Boolean = false,
    maxFrames: Int = MAX_FRAMES,
): QuiescenceResult =
    withContext(recomposerDispatcher) {
        // Yield so continuations queued on recomposerDispatcher (effects started by the last
        // composition, effects resuming from withFrameNanos after sendFrame) run before quiescence
        // is sampled, see drainRecomposerDispatcher. recomposerDispatcher is usually a combined
        // context, so the interceptor must be read out of it. Skip yielding for immediate
        // dispatchers like Dispatchers.Main.immediate on the main thread (where continuations run
        // inline), as yield() forces a Handler.post() via YieldContext, which deadlocks if the main
        // thread is blocked inside runBlocking/runTest.
        val interceptor = recomposerDispatcher[ContinuationInterceptor]
        val shouldYield =
            interceptor !is CoroutineDispatcher ||
                interceptor.isDispatchNeeded(EmptyCoroutineContext)
        // The frame budget covers the whole call, not each iteration, otherwise content that
        // animates forever would get MAX_FRAMES frames for every one of maxIterations iterations.
        frameClock.withFrameBudget(maxFrames) {
            var idleIterations = 0
            while (idleIterations < maxIterations) {
                recomposer.refreshStateIfIdle()
                awaitIdle(recomposer, frameClock, recomposerDispatcher, shouldYield, pacedFrames)
                drainRecomposerDispatcher(recomposer, frameClock, recomposerDispatcher, shouldYield)
                while (frameClock.hasAwaiters) {
                    if (pacedFrames) frameClock.awaitFrame() else frameClock.sendFrame()
                    // Without yield() nothing else checks for cancellation by the frame budget.
                    ensureActive()
                    drainRecomposerDispatcher(
                        recomposer,
                        frameClock,
                        recomposerDispatcher,
                        shouldYield,
                    )
                }
                Snapshot.sendApplyNotifications()
                if (!awaitApplyObserverNotifications(canYield = shouldYield)) {
                    return@withFrameBudget QuiescenceResult.ApplyNotificationsTimedOut
                }
                writeTracker.awaitDelivery(canYield = shouldYield)
                recomposer.refreshStateIfIdle()
                val isQuiescent =
                    recomposer.currentState.value == Recomposer.State.Idle &&
                        !recomposer.hasPendingWork &&
                        !frameClock.hasAwaiters
                if (isQuiescent) {
                    return@withFrameBudget QuiescenceResult.Quiescent
                }
                idleIterations++
            }
            QuiescenceResult.Unstable
        } ?: QuiescenceResult.ContinuousFrames
    }

/**
 * Waits until [recomposer] is [Recomposer.State.Idle], delivering the frames it asks for on the way
 * there: sent immediately, or awaited from the pump when [pacedFrames]. Between frames the
 * recomposer dispatcher is drained (see [drainRecomposerDispatcher]) so that work queued behind the
 * caller can request the next frame or bring the recomposer to idle. Once there is nothing to
 * deliver and a drain made no difference, [Recomposer.currentState] is waited on instead of polled:
 * right away on a dispatcher that cannot be yielded, whose queue one drain empties, and after
 * [APPLY_NOTIFICATION_MAX_POLLS] yields otherwise.
 *
 * Without this, a recomposer that is `PendingWork` because it awaits a frame itself, to recompose
 * after a state write (e.g. from a frame callback), would be waiting for the pump even when the
 * caller asked for immediate frames, and a paced pump would then dictate how long settling takes.
 */
private suspend fun awaitIdle(
    recomposer: Recomposer,
    frameClock: HeadlessFrameClock,
    recomposerDispatcher: CoroutineContext,
    canYield: Boolean,
    pacedFrames: Boolean,
) {
    var polls = 0
    var drained = false
    while (recomposer.currentState.value != Recomposer.State.Idle) {
        recomposer.throwIfShutDown()
        if (frameClock.hasAwaiters) {
            if (pacedFrames) frameClock.awaitFrame() else frameClock.sendFrame()
            currentCoroutineContext().ensureActive()
            drained = false
        } else if ((!canYield && drained) || ++polls > APPLY_NOTIFICATION_MAX_POLLS) {
            // Nothing to deliver and nothing left to drain (or yielding is not making progress):
            // wait for the state to change instead.
            awaitRecomposerIdle(recomposer)
            return
        }
        drainRecomposerDispatcher(recomposer, frameClock, recomposerDispatcher, canYield)
        drained = true
        recomposer.refreshStateIfIdle()
    }
}

/**
 * Suspends until [recomposer] is [Recomposer.State.Idle].
 *
 * @throws CancellationException if the recomposer shuts down first (its session was closed), as it
 *   will never be idle again.
 */
private suspend fun awaitRecomposerIdle(recomposer: Recomposer) {
    recomposer.currentState.first {
        it == Recomposer.State.Idle || it <= Recomposer.State.ShuttingDown
    }
    recomposer.throwIfShutDown()
}

/** See [awaitRecomposerIdle]. */
private fun Recomposer.throwIfShutDown() {
    if (currentState.value <= Recomposer.State.ShuttingDown) {
        throw CancellationException("Recomposer shut down while awaiting quiescence")
    }
}

/**
 * Yields until no task is queued behind the caller on [recomposerDispatcher], so that continuations
 * dispatched by the last composition or frame (an effect starting, or resuming from
 * `withFrameNanos`) have run before quiescence is sampled. An effect that `yield()`s before it
 * requests a frame is otherwise invisible to [awaitRecomposerQuiescence]: the recomposer is idle
 * and the frame clock has no awaiter yet. Only a [CountingSerialDispatcher] exposes its queue; on
 * other dispatchers this yields once, as before. Bounded by [APPLY_NOTIFICATION_MAX_POLLS] so
 * content that yields forever cannot stall the capture.
 *
 * Must run before [Snapshot.sendApplyNotifications] in an iteration, never between it and the
 * quiescence sample: the frame clock's pump may send frames while this yields, and state written by
 * their callbacks only becomes [Recomposer.hasPendingWork] once notifications are sent.
 *
 * @param canYield whether the current dispatcher may be released with [yield], see
 *   [awaitRecomposerQuiescence]. When `false`, only the thread's unconfined event loop is drained,
 *   see [drainUnconfinedEventLoop].
 */
private suspend fun drainRecomposerDispatcher(
    recomposer: Recomposer,
    frameClock: HeadlessFrameClock,
    recomposerDispatcher: CoroutineContext,
    canYield: Boolean,
) {
    if (!canYield) {
        drainUnconfinedEventLoop(recomposer, frameClock)
        return
    }
    val dispatcher = recomposerDispatcher[ContinuationInterceptor] as? CountingSerialDispatcher
    var polls = 0
    do {
        yield()
    } while (
        dispatcher != null && dispatcher.queuedTasks > 0 && ++polls < APPLY_NOTIFICATION_MAX_POLLS
    )
}

/**
 * Lets continuations queued on this thread's unconfined event loop run. A dispatcher that does not
 * need dispatch on the current thread ([Dispatchers.Unconfined], `Dispatchers.Main.immediate` on
 * the main thread) resumes continuations through that loop: nested resumptions (an effect launched
 * while composing, the recomposer woken by a frame request, the pump woken by the recomposer) are
 * queued until the running task suspends. `yield()` on the host dispatcher cannot be used for this,
 * as an immediate main dispatcher would post to the main looper, which deadlocks when the main
 * thread is blocked in `runBlocking`/`runTest`; `yield()` on [Dispatchers.Unconfined] instead
 * processes the loop in place.
 *
 * One pass runs the tasks queued so far; the tasks they queue in turn need another pass. The queue
 * length is not observable, so passes repeat while they visibly move the recomposer or the frame
 * clock, bounded by [APPLY_NOTIFICATION_MAX_POLLS].
 */
private suspend fun drainUnconfinedEventLoop(
    recomposer: Recomposer,
    frameClock: HeadlessFrameClock,
) {
    var polls = 0
    do {
        val state = recomposer.currentState.value
        val pending = recomposer.hasPendingWork
        val awaiters = frameClock.hasAwaiters
        val frames = frameClock.framesSent
        withContext(Dispatchers.Unconfined) { yield() }
        val changed =
            state != recomposer.currentState.value ||
                pending != recomposer.hasPendingWork ||
                awaiters != frameClock.hasAwaiters ||
                frames != frameClock.framesSent
    } while (changed && ++polls < APPLY_NOTIFICATION_MAX_POLLS)
}

/**
 * Waits for at most [APPLY_NOTIFICATION_WAIT] (or [APPLY_NOTIFICATION_MAX_POLLS] polls) until no
 * thread is notifying apply observers of global snapshot changes
 * ([Snapshot.isApplyObserverNotificationPending]).
 *
 * The flag is process-wide, so this may also wait on unrelated threads (e.g. an app's main-thread
 * `GlobalSnapshotManager`). A plain lock shared with [SnapshotWriteMonitor] would not cover those,
 * which is why this polls the runtime's counter instead. Polling is bounded and cancellable so that
 * a slow or blocked foreign observer can never hang the capture or stall the thread (possibly the
 * main thread) that [awaitRecomposerQuiescence] runs on.
 *
 * @param canYield whether the current dispatcher may be released with [yield] between polls. When
 *   `false` (immediate dispatchers), the thread is yielded with [Thread.yield] instead.
 * @return `true` if no notification is pending, `false` if the wait timed out.
 */
private suspend fun awaitApplyObserverNotifications(canYield: Boolean): Boolean =
    pollUntil(canYield) { !Snapshot.isApplyObserverNotificationPending }

/**
 * Polls [isDone] for at most [APPLY_NOTIFICATION_WAIT] (or [APPLY_NOTIFICATION_MAX_POLLS] polls),
 * yielding between polls as described for [awaitApplyObserverNotifications].
 *
 * @return `true` if [isDone] returned `true`, `false` if the wait timed out.
 */
internal suspend inline fun pollUntil(canYield: Boolean, isDone: () -> Boolean): Boolean {
    if (isDone()) return true
    val deadline = TimeSource.Monotonic.markNow() + APPLY_NOTIFICATION_WAIT
    var polls = 0
    while (!isDone()) {
        if (deadline.hasPassedNow() || ++polls > APPLY_NOTIFICATION_MAX_POLLS) return false
        if (canYield) {
            yield()
        } else {
            currentCoroutineContext().ensureActive()
            Thread.yield()
        }
    }
    return true
}
