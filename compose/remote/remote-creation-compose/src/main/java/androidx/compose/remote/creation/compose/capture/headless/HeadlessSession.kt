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
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

/**
 * Wires together the pieces needed to run a [Recomposer] without a UI: a single-threaded
 * [recomposerDispatcher] derived from [parentContext], a [HeadlessFrameClock], a
 * [CaptureWriteTracker] installed in the recomposer's effect context, and the [recomposer] itself.
 *
 * [close] cancels the recomposer, the frame clock and the write tracker; it does not dispose
 * compositions created from [recomposer], which remain the caller's responsibility.
 *
 * @param parentContext the context the session runs in; its `Job` is used as the recomposer's
 *   parent, its dispatcher (if any) is narrowed to one thread.
 * @param name names the single-threaded dispatcher slice, for debugging.
 * @param nanoTime the time stamped on frames, see [HeadlessFrameClock]; defaults to nanoseconds
 *   elapsed on [timeSource] since the session was created.
 * @param timeSource measures the time between paced frames, see [HeadlessFrameClock].
 */
internal class HeadlessSession(
    parentContext: CoroutineContext,
    name: String,
    nanoTime: (() -> Long)? = null,
    timeSource: TimeSource = TimeSource.Monotonic,
) : AutoCloseable {
    private enum class State {
        Created,
        Started,
        Closed,
    }

    private val lock = Any()
    private var state = State.Created
    private val started = CompletableDeferred<Unit>()

    /** Single-threaded context that recomposition, frames and effects run on. */
    val recomposerDispatcher: CoroutineContext =
        parentContext.toSingleThreadedRecomposerContext(name)

    /** Tracks this session's own global snapshot writes, see [CaptureWriteTracker]. */
    private val writeTracker = CaptureWriteTracker()

    /** The recomposer; create compositions from it with `Composition(applier, recomposer)`. */
    val recomposer = Recomposer(parentContext + recomposerDispatcher + writeTracker.contextElement)

    /** The frame clock the recomposer and its effects await frames on once [start]ed. */
    val frameClock =
        HeadlessFrameClock(
            nanoTime =
                nanoTime
                    ?: timeSource.markNow().let { start ->
                        { start.elapsedNow().inWholeNanoseconds }
                    },
            timeSource = timeSource,
        ) {
            recomposer.refreshStateIfIdle(recomposerDispatcher)
        }

    private val invalidationChannel = Channel<Unit>(Channel.CONFLATED)
    private var applyObserver: ObserverHandle? = null

    /**
     * Receives an element whenever snapshot state was applied since the previous receive, from
     * [start] until [close]. Conflated: a burst of applies is one element, but an apply can never
     * be missed, unlike a `Recomposer.currentState` transition. Drives [renderLoop].
     *
     * The observer behind it is registered after the recomposer's own, so that by the time a
     * receiver wakes the recomposer has recorded the same change, and before any content can run an
     * effect, so that no write is claimed by the registration itself.
     */
    val invalidations: ReceiveChannel<Unit>
        get() = invalidationChannel

    /**
     * Called on the applying thread for every apply notification, before [invalidations] is
     * signalled. At most one listener; see [renderLoop].
     */
    @Volatile var onApplied: (() -> Unit)? = null

    /** Signals [invalidations] without a snapshot apply, e.g. for a change observed elsewhere. */
    fun invalidate() {
        invalidationChannel.trySend(Unit)
    }

    /**
     * Launches the frame clock's pump and `recomposer.runRecomposeAndApplyChanges()` in [scope].
     * The pump is started first so that it is parked by the time the first frame is requested, and
     * the write tracker is started after the recomposer registers its apply observer but before any
     * effect can resume from a frame.
     *
     * The launched coroutines complete when the session is [close]d, which is the only way to stop
     * them; a `coroutineScope` that calls [start] therefore does not return until then.
     *
     * @throws IllegalStateException if the session was already started or closed.
     */
    fun start(scope: CoroutineScope) {
        transition(from = State.Created, to = State.Started) { "start" }
        scope.launch(recomposerDispatcher) { frameClock.runClock() }
        val runner =
            scope.launch(recomposerDispatcher + frameClock) {
                // Queued before the runner's first slice can request a frame, so it runs after the
                // Recomposer registers its apply observer but before any effect resumes from
                // withFrameNanos.
                launch {
                    val observer = Snapshot.registerApplyObserver { _, _ ->
                        onApplied?.invoke()
                        invalidationChannel.trySend(Unit)
                    }
                    val closed =
                        synchronized(lock) {
                            (state == State.Closed).also { if (!it) applyObserver = observer }
                        }
                    if (closed) {
                        observer.dispose()
                        started.cancel(CancellationException("HeadlessSession was closed"))
                    } else {
                        writeTracker.start()
                        started.complete(Unit)
                    }
                }
                recomposer.runRecomposeAndApplyChanges()
            }
        runner.invokeOnCompletion { cause ->
            if (!started.isCompleted) {
                started.cancel(
                    cause as? CancellationException
                        ?: CancellationException("HeadlessSession stopped before running", cause)
                )
            }
        }
    }

    /**
     * Suspends until the recomposer launched by [start] is running, i.e. [Recomposer.currentState]
     * is at least [Recomposer.State.Idle] and the session's invalidation and write observers are
     * registered. Set content after this: a `Recomposer` invalidates every composition it knows
     * about when it starts, so content set earlier is composed twice. Needed because [start] only
     * launches the recomposer; whether it has run by the time the caller continues depends on the
     * dispatcher (on a non-immediate main dispatcher, for example, a
     * `withContext(recomposerDispatcher)` from the main thread does not dispatch).
     *
     * @throws IllegalStateException if the session is not started, or already closed.
     * @throws CancellationException if the session is closed while waiting.
     */
    suspend fun awaitRunning() {
        synchronized(lock) {
            check(state == State.Started) { "Cannot await a running recomposer: session is $state" }
        }
        started.await()
        if (isClosed || recomposer.currentState.value <= Recomposer.State.ShuttingDown) {
            throw CancellationException("HeadlessSession was closed")
        }
    }

    /**
     * Drives the recomposer until it settles, see [awaitRecomposerQuiescence].
     *
     * @param maxIterations how many non-quiescent iterations settling may take before giving up
     *   with [QuiescenceResult.Unstable].
     * @param pacedFrames whether frames requested while settling are left to the frame clock's
     *   pump, paced by [HeadlessFrameClock.minFramePeriod], instead of being sent immediately.
     * @param maxFrames how many frames content may request before settling gives up with
     *   [QuiescenceResult.ContinuousFrames].
     * @throws IllegalStateException if the session is not started, or already closed.
     * @throws CancellationException if the session is closed while settling.
     */
    suspend fun awaitQuiescence(
        maxIterations: Int = MAX_IDLE_ITERATIONS,
        pacedFrames: Boolean = false,
        maxFrames: Int = MAX_FRAMES,
    ): QuiescenceResult {
        synchronized(lock) {
            check(state == State.Started) { "Cannot await quiescence: session is $state" }
        }
        return awaitRecomposerQuiescence(
            recomposer = recomposer,
            frameClock = frameClock,
            recomposerDispatcher = recomposerDispatcher,
            writeTracker = writeTracker,
            maxIterations = maxIterations,
            pacedFrames = pacedFrames,
            maxFrames = maxFrames,
        )
    }

    /** Whether [close] has been called. */
    val isClosed: Boolean
        get() = synchronized(lock) { state == State.Closed }

    /**
     * Stops the write tracker, the frame clock and the recomposer. Idempotent; a session closed
     * before it was started can no longer be started.
     */
    override fun close() {
        val observer: ObserverHandle?
        synchronized(lock) {
            if (state == State.Closed) return
            state = State.Closed
            observer = applyObserver
            applyObserver = null
        }
        started.cancel(CancellationException("HeadlessSession was closed"))
        observer?.dispose()
        invalidationChannel.close()
        writeTracker.stop()
        frameClock.cancel()
        recomposer.cancel()
    }

    private inline fun transition(from: State, to: State, action: () -> String) {
        synchronized(lock) {
            check(state == from) { "Cannot ${action()} HeadlessSession: session is $state" }
            state = to
        }
    }
}
