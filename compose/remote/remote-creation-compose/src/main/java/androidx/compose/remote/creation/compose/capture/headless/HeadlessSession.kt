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
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
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
 * @param nanoTime the time stamped on frames, see [HeadlessFrameClock].
 * @param timeSource measures the time between paced frames, see [HeadlessFrameClock].
 */
internal class HeadlessSession(
    parentContext: CoroutineContext,
    name: String,
    nanoTime: () -> Long,
    timeSource: TimeSource,
) : AutoCloseable {
    private enum class State {
        Created,
        Started,
        Closed,
    }

    private val lock = Any()
    private var state = State.Created

    /** Single-threaded context that recomposition, frames and effects run on. */
    val recomposerDispatcher: CoroutineContext =
        parentContext.toSingleThreadedRecomposerContext(name)

    /** Tracks this session's own global snapshot writes, see [CaptureWriteTracker]. */
    private val writeTracker = CaptureWriteTracker()

    /** The recomposer; create compositions from it with `Composition(applier, recomposer)`. */
    val recomposer = Recomposer(parentContext + recomposerDispatcher + writeTracker.contextElement)

    /** The frame clock the recomposer and its effects await frames on once [start]ed. */
    val frameClock =
        HeadlessFrameClock(nanoTime = nanoTime, timeSource = timeSource) {
            recomposer.refreshStateIfIdle(recomposerDispatcher)
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
        scope.launch(recomposerDispatcher + frameClock) {
            // Queued before the runner's first slice can request a frame, so it runs after the
            // Recomposer registers its apply observer but before any effect resumes from
            // withFrameNanos.
            launch { writeTracker.start() }
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    /**
     * Drives the recomposer until it settles, see [awaitRecomposerQuiescence].
     *
     * @throws IllegalStateException if the session is not started, or already closed.
     */
    suspend fun awaitQuiescence(maxIterations: Int = MAX_IDLE_ITERATIONS): QuiescenceResult {
        synchronized(lock) {
            check(state == State.Started) { "Cannot await quiescence: session is $state" }
        }
        return awaitRecomposerQuiescence(
            recomposer = recomposer,
            frameClock = frameClock,
            recomposerDispatcher = recomposerDispatcher,
            writeTracker = writeTracker,
            maxIterations = maxIterations,
        )
    }

    /**
     * Stops the write tracker, the frame clock and the recomposer. Idempotent; a session closed
     * before it was started can no longer be started.
     */
    override fun close() {
        synchronized(lock) {
            if (state == State.Closed) return
            state = State.Closed
        }
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
