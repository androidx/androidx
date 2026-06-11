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

package androidx.compose.ui.platform

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.util.trace
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns a recomposer and frame clock shared by one or more scenes hosted by the same platform
 * container.
 *
 * This is an equivalent of the Android host-side recomposer/frame-clock machinery: Android drives
 * global snapshot notifications through `GlobalSnapshotManager`, drains dispatcher work on
 * the UI thread, then lets the recomposer resume frame-clock awaiters and apply changes.
 * Non-Android platforms do not have a shared Android-style View/Choreographer integration point,
 * so the host calls [performFrame] explicitly before driving scene measure/layout and draw.
 */
@InternalComposeUiApi
class FrameRecomposer(
    coroutineContext: CoroutineContext,
    private val invalidate: () -> Unit = {},
) : AutoCloseable {
    private val job = Job()
    private val coroutineScope = CoroutineScope(coroutineContext + job)
    private val frameClock = BroadcastFrameClock(::onNewAwaiters)
    private val effectDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val recomposeDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val recomposer = Recomposer(coroutineContext + job + effectDispatcher)

    /**
     * Registers `coroutineContext` with the shared [GlobalSnapshotManager] so ambient global writes
     * schedule apply notifications onto this host. Several [FrameRecomposer]s built on the same
     * host context share one observer and it's released only when the last of them is closed.
     */
    private val globalSnapshotRegistration = GlobalSnapshotManager.register(coroutineContext)

    init {
        coroutineScope.launch(
            recomposeDispatcher + frameClock,
            start = CoroutineStart.UNDISPATCHED
        ) {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    /**
     * Returns the composition context backed by this host's recomposer.
     */
    val compositionContext: CompositionContext
        get() = recomposer

    private var isInFrame = false

    private fun onNewAwaiters() {
        if (isInFrame) return
        invalidate()
    }

    private inline fun <T> postponeFrameInvalidation(crossinline block: () -> T): T =
        trace("FrameRecomposer:performFrame") {
            check(!isInFrame)
            isInFrame = true
            try {
                block()
            } finally {
                isInFrame = false
            }
        }

    /**
     * Performs one host frame. Platforms should call this once from their native frame callback
     * before running scene measure/layout and draw phases.
     *
     * The snapshot checkpoints are deliberate behavior parity with the old combined render call
     * and with Android's flow:
     * - the first call observes global snapshot writes that were scheduled before this native
     *   frame, like Android's `GlobalSnapshotManager` running on the UI dispatcher;
     * - [recomposeFrame] then flushes effects/recomposer tasks and sends the frame clock, matching
     *   the recomposer's frame-aligned work;
     * - the second call mirrors the runtime recomposer checkpoint after `sendFrame`, so state
     *   changes produced by frame awaiters are visible before platform layout/draw phases run.
     */
    fun performFrame(frameTimeNanos: Long) {
        Snapshot.sendApplyNotifications()
        recomposeFrame(frameTimeNanos)
        Snapshot.sendApplyNotifications()
    }

    /**
     * Advances only the host recomposer and frame clock by one frame at [frameTimeNanos].
     */
    private fun recomposeFrame(frameTimeNanos: Long) {
        postponeFrameInvalidation {
            // Flush composition effects (e.g. LaunchedEffect, coroutines launched in
            // rememberCoroutineScope()) queued by the previous turn must run before
            // recomposition tasks and frame-clock awaiters.
            performScheduledEffects()
            performScheduledRecomposerTasks()

            frameClock.sendFrame(frameTimeNanos)
        }
        if (frameClock.hasAwaiters) {
            invalidate()
        }
    }

    /**
     * Returns whether the host still has recomposition or frame-clock work to process.
     */
    fun hasPendingWork(): Boolean =
        recomposer.hasPendingWork ||
            effectDispatcher.hasImmediateTasks() ||
            recomposeDispatcher.hasImmediateTasks() ||
            frameClock.hasAwaiters

    /**
     * Cancels the host recomposer and releases host-owned resources.
     */
    override fun close() {
        globalSnapshotRegistration?.close()
        recomposer.cancel()
        job.cancel()
    }

    /**
     * Runs [block] with the [MonotonicFrameClock] owned by this host's recomposer.
     */
    suspend fun withMonotonicFrameClock(block: suspend () -> Unit) {
        val monotonicFrameClock = compositionContext.effectCoroutineContext[MonotonicFrameClock]
            ?: error("No MonotonicFrameClock found in FrameRecomposer.compositionContext")
        withContext(monotonicFrameClock) {
            block()
        }
    }

    /**
     * Enqueues host-owned work to run later in the current turn, before the next frame.
     */
    internal fun dispatch(block: () -> Unit) {
        effectDispatcher.dispatch(job, Runnable(block))
    }

    internal fun performScheduledRecomposerTasks(): Unit =
        trace("FrameRecomposer:performScheduledRecomposerTasks") {
            recomposeDispatcher.flush()
        }

    internal fun performScheduledEffects(): Unit =
        trace("FrameRecomposer:performScheduledEffects") {
            effectDispatcher.flush()
        }
}
