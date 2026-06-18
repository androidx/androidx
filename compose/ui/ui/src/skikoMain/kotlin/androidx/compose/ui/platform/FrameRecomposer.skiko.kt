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
import androidx.compose.ui.internal.getCurrentThreadId
import androidx.compose.ui.util.trace
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns a [Recomposer] and frame clock shared by one or more scenes hosted by the same platform
 * container - the non-Android analog of Android's host-side recomposer/frame-clock machinery
 * (`AndroidComposeView` + the host recomposer + `Choreographer`).
 *
 * Two work queues mirror `AndroidUiDispatcher`'s two queues:
 * - [trampolineDispatcher] (Android's `toRunTrampolined`): coroutine dispatch, composition effects
 *   (`LaunchedEffect`, `rememberCoroutineScope` launches) and the recomposer's effect context;
 * - [frameDispatcher] (Android's `toRunOnFrame`), together with [frameClock]: `withFrameNanos`
 *   awaiters and recomposition (the recomposition loop runs on `frameDispatcher + frameClock`).
 *
 * Both are [FlushCoroutineDispatcher]s layered over the host's real dispatcher, so on a host with
 * a live native loop they drain automatically; [performFrame] and the scene phases also roll them
 * synchronously via [performTrampolineDispatch] / [performFrameDispatch].
 *
 * Android drives frames through `Choreographer.doFrame`; non-Android platforms have no such hook,
 * so the host calls [performFrame] explicitly before driving scene measure/layout and draw.
 *
 * The host dispatcher must be confined to a single thread, so [composeThreadId] is stable.
 * It is recorded whenever the recomposer runs on the host thread (via [performFrameDispatch]).
 */
@InternalComposeUiApi
class FrameRecomposer(
    coroutineContext: CoroutineContext,
    private val invalidate: () -> Unit = {},
) : AutoCloseable {
    private val job = Job()
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    /**
     * Trampoline queue (Android's `toRunTrampolined`):
     *   - Coroutine dispatch
     *   - Composition effects
     *   - Scheduled apply notifications
     * Rolled synchronously by [performTrampolineDispatch].
     */
    private val trampolineDispatcher = FlushCoroutineDispatcher(coroutineScope)

    /**
     * Frame queue (Android's `toRunOnFrame`): `withFrameNanos` awaiters and recomposition tasks.
     * Rolled synchronously by [performFrameDispatch].
     */
    private val frameDispatcher = FlushCoroutineDispatcher(coroutineScope)

    /**
     * The clock that drives the recomposition loop.
     * Its `withFrameNanos` awaiters are resumed by [performFrame].
     */
    private val frameClock = BroadcastFrameClock(::onNewAwaiters)

    private val recomposer = Recomposer(coroutineContext + job + trampolineDispatcher)

    /**
     * Id of the host (compose) thread. Snapshot-observer callbacks run inline when on this thread,
     * otherwise they are posted to the shared [effectDispatcher].
     */
    private var composeThreadId: Long? by atomic(null)

    /**
     * Registers `coroutineContext` with the shared [GlobalSnapshotManager] so ambient global writes
     * schedule apply notifications onto this host. Several [FrameRecomposer]s built on the same
     * host context share one observer and it's released only when the last of them is closed.
     */
    private val globalSnapshotRegistration = GlobalSnapshotManager.register(coroutineContext)

    init {
        // The host must carry a (single-thread) continuation interceptor that work is dispatched
        // through. It need not be a CoroutineDispatcher directly - e.g. tests wrap it with an
        // ApplyingContinuationInterceptor that delegates to the test dispatcher.
        requireNotNull(coroutineContext[ContinuationInterceptor]) {
            "FrameRecomposer requires a ContinuationInterceptor in its coroutineContext"
        }
        coroutineScope.launch(
            frameDispatcher + frameClock,
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
     * Performs one host frame. Platforms call this once from their native frame callback before
     * running [androidx.compose.ui.scene.ComposeScene] measure/layout and draw phases.
     */
    fun performFrame(frameTimeNanos: Long) {
        postponeFrameInvalidation {
            performFrameDispatch()

            frameClock.sendFrame(frameTimeNanos)
        }
        if (frameClock.hasAwaiters) {
            invalidate()
        }
    }

    /**
     * Returns whether the host still has recomposition or loop work to process.
     */
    fun hasPendingWork(): Boolean =
        recomposer.hasPendingWork ||
            trampolineDispatcher.hasImmediateTasks() ||
            frameDispatcher.hasImmediateTasks() ||
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
     * Runs [block] on the compose thread: inline when already on it, otherwise [dispatch]ed onto
     * the shared trampoline queue.
     */
    internal fun runOnComposeThread(block: () -> Unit) {
        if (composeThreadId == getCurrentThreadId()) block() else dispatch(block)
    }

    /**
     * Enqueues [block] onto the trampoline queue; it runs on the next loop turn or the next
     * [performTrampolineDispatch].
     */
    internal fun dispatch(block: () -> Unit) {
        trampolineDispatcher.dispatch(job, Runnable(block))
    }

    /**
     * Synchronously rolls the frame loop: drains the [frameDispatcher] queue (pending
     * `withFrameNanos` / recompose tasks) after first rolling the trampoline loop via
     * [performTrampolineDispatch].
     */
    internal fun performFrameDispatch(): Unit =
        trace("FrameRecomposer:performFrameDispatch") {
            composeThreadId = getCurrentThreadId()
            performTrampolineDispatch()
            frameDispatcher.flush()
        }

    /**
     * Synchronously rolls the trampoline loop: first flushes pending snapshot apply notifications
     * (so writes made since the last turn are visible to the queued work), then drains the
     * [trampolineDispatcher] queue (coroutine dispatch / composition effects).
     */
    internal fun performTrampolineDispatch(): Unit =
        trace("FrameRecomposer:performTrampolineDispatch") {
            Snapshot.sendApplyNotifications()

            trampolineDispatcher.flush()
        }
}
