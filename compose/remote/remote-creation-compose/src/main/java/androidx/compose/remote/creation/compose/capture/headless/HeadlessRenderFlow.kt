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

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Runs [content] in a headless composition and emits what [render] produces from the settled
 * composition: the first render once content has settled, then a new one whenever composition or
 * state read by the previous render changes, skipping outputs equal to the previous one. This is
 * the whole lifecycle of a headless host that turns a composition into a stream of outputs (a
 * serialized document, a widget's views, ...); the host only supplies the [applier] its content
 * emits into, the [content] and the [render] step.
 *
 * Each collection creates its own [HeadlessSession] and [Composition] and disposes of them when
 * collection ends, after [maxOutputs] outputs or when the collector stops.
 *
 * A single-output flow (`maxOutputs == 1`) renders once, so it doesn't observe the reads of its
 * render and doesn't acquire [SnapshotWriteMonitor]: global writes made by effects are applied by
 * the explicit [androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications] inside
 * [awaitRecomposerQuiescence], which is enough for a one-shot render and avoids starting a
 * process-wide coalescing loop for a short-lived call ([CaptureWriteTracker] still tracks the
 * session's own writes).
 *
 * @param name names the session's dispatcher slice and trace sections.
 * @param applier the applier [content] emits into. [render] reads the tree it builds.
 * @param changeCount a counter that moves whenever composition applies a change [render] cares
 *   about, see [renderLoop].
 * @param maxOutputs how many outputs to emit before completing.
 * @param coroutineContext added to the collector's context for the session, e.g. a dispatcher;
 *   recomposition, effects and [render] run serialized on one thread of it (see
 *   [toSingleThreadedRecomposerContext]).
 * @param timeSource paces frames and the rate limiter, so that a test clock drives both.
 * @param nanoTime the time stamped on frames, see [HeadlessFrameClock].
 * @param newRateLimiter creates the per-collection limiter for emitted outputs, or `null` for none.
 *   While the limiter holds outputs back, the frame clock's pump is paced at its interval too.
 * @param applyRenderWrites whether state written by [render] is applied, see
 *   [RenderScope.renderIfChanged].
 * @param onNotQuiescent called with the result when content did not settle before a render, e.g. to
 *   fail a single-output host whose output nothing would correct. Rendering goes ahead if it
 *   returns.
 * @param isSameOutput whether two consecutive outputs are equal, so the second isn't emitted.
 * @param onDispose runs on the recomposer dispatcher when collection ends, before the composition
 *   is disposed.
 * @param render produces an output from the settled composition, given how many renders came before
 *   it. Runs on the recomposer dispatcher in a render snapshot; must not suspend.
 */
internal fun <T : Any> headlessRenderFlow(
    name: String,
    applier: Applier<*>,
    changeCount: () -> Long,
    maxOutputs: Int = Int.MAX_VALUE,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    timeSource: TimeSource = TimeSource.Monotonic,
    nanoTime: (() -> Long)? = null,
    newRateLimiter: (TimeSource) -> UpdateRateLimiter? = { null },
    applyRenderWrites: Boolean = false,
    onNotQuiescent: (QuiescenceResult) -> Unit = {},
    isSameOutput: (previous: T, output: T) -> Boolean = { previous, output -> previous == output },
    onDispose: () -> Unit = {},
    content: @Composable () -> Unit,
    render: (renderIndex: Int) -> T,
): Flow<T> = flow {
    require(maxOutputs > 0) { "maxOutputs must be positive, was $maxOutputs" }
    val isSingleOutput = maxOutputs == 1

    val parentContext = currentCoroutineContext() + coroutineContext.minusKey(Job)
    val session =
        HeadlessSession(
            parentContext = parentContext,
            name = name,
            nanoTime = nanoTime,
            timeSource = timeSource,
        )
    val recomposerDispatcher = session.recomposerDispatcher
    val composition = Composition(applier, session.recomposer)

    try {
        coroutineScope {
            // Frames are normally sent as soon as an awaiter appears. The Recomposer itself awaits
            // frames on this clock (started below), so pacing them with a time-based delay would
            // add latency to every recomposition and stall entirely on hosts whose clock only
            // advances explicitly (virtual-time test dispatchers, a paused Robolectric looper).
            // Emitted outputs are rate limited separately by the limiter, between renders rather
            // than between frames; only while the limiter is holding outputs back are the pump's
            // frames paced too (see beforeSettle).
            val frameClock = session.frameClock
            val writeMonitor =
                if (isSingleOutput) null else SnapshotWriteMonitor.acquireForSession(parentContext)
            try {
                session.start(this)
                // Set the content only once the Recomposer is running. A Recomposer invalidates
                // every composition it knows about when it starts, since it was not observing
                // snapshot state on their behalf until then; content set before that point is
                // therefore composed a second time before the first output is rendered. The
                // effects started by the initial composition then also run under the started
                // write tracker and after the session's invalidation observer is registered.
                session.awaitRunning()
                withContext(recomposerDispatcher) { composition.setContent(content) }

                var previous: T? = null
                var emittedOutputs = 0
                var renders = 0
                val rateLimiter = newRateLimiter(timeSource)
                session.renderLoop(
                    observeRenderReads = !isSingleOutput,
                    changeCount = changeCount,
                    beforeSettle = {
                        // Wait for the limiter before settling and rendering, so writes made
                        // meanwhile are conflated into the invalidation and the output rendered
                        // afterwards reflects the latest state. Only emitted outputs count
                        // towards the limit.
                        //
                        // While waiting for a permit, also pace the frames the clock's pump sends,
                        // so that content animating through recomposition does not keep the
                        // recomposer spinning at full speed for outputs that cannot be emitted
                        // yet. Pacing is lifted before settling: awaitQuiescence waits for the
                        // recomposer to go Idle, which needs the pump's frame, and bounds its own
                        // frames by MAX_FRAMES.
                        if (rateLimiter != null && !rateLimiter.hasPermit()) {
                            frameClock.minFramePeriod = rateLimiter.interval
                            try {
                                rateLimiter.awaitPermit()
                            } finally {
                                frameClock.minFramePeriod = Duration.ZERO
                            }
                        }
                    },
                ) {
                    if (quiescence != QuiescenceResult.Quiescent) onNotQuiescent(quiescence)

                    val output =
                        renderIfChanged(applyWrites = applyRenderWrites) { render(renders++) }
                            ?: return@renderLoop true

                    val last = previous
                    if (last == null || !isSameOutput(last, output)) {
                        previous = output
                        rateLimiter?.onEmitted()
                        emit(output)
                        if (++emittedOutputs >= maxOutputs) {
                            return@renderLoop false
                        }
                    }
                    true
                }
            } finally {
                session.close()
                currentCoroutineContext().cancelChildren()
                writeMonitor?.close()
            }
        }
    } finally {
        withContext(recomposerDispatcher + NonCancellable) {
            session.recomposer.cancel()
            onDispose()
            composition.dispose()
        }
    }
}
