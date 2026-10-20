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

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

/** One iteration of [renderLoop]: the settled composition is ready to be read. */
internal interface RenderScope {
    /** How the composition settled before this iteration, see [HeadlessSession.awaitQuiescence]. */
    val quiescence: QuiescenceResult

    /**
     * Runs [block], which reads the composition's output (an applier's tree, snapshot state), on
     * the recomposer dispatcher and returns its result, or returns `null` without running it if
     * nothing can have changed since the previous render: no change was applied to the composition
     * ([renderLoop]'s `changeCount` did not move) and, when render reads are observed, no state
     * read by the previous render was written.
     *
     * The block runs inside a render snapshot (see [withRenderSnapshot]) so it sees a consistent
     * view of state while other threads keep writing. It must not write state unless [applyWrites]
     * is `true`. It must not suspend or return `null`.
     */
    suspend fun <T : Any> renderIfChanged(applyWrites: Boolean = false, block: () -> T): T?
}

/**
 * Drives this session's composition for as long as [body] asks for it: settles the recomposer, runs
 * [body] (which renders through [RenderScope.renderIfChanged]), then waits for the next change
 * before doing it again.
 *
 * Changes are detected from snapshot apply notifications ([HeadlessSession.invalidations]), not
 * from `Recomposer.currentState` transitions: `currentState` is a conflated `StateFlow`, so a
 * collector that is descheduled while the recomposer cycles `Idle -> Recomposing -> Idle` sees
 * nothing and would drop the update. A conflated channel collapses bursts of writes into one
 * iteration but can never lose the fact that state changed. State read by the previous render (not
 * by composition) is observed too when [observeRenderReads] is set, the way Compose UI observes
 * reads during layout and draw, so a change to it re-renders without a recomposition. See
 * [RenderInvalidationTracker].
 *
 * The session must be [HeadlessSession.start]ed and content set before calling this. Returns when
 * [body] returns `false` or the session is [HeadlessSession.close]d, at whichever point of the loop
 * the close interrupts it (waiting for a change, settling, [beforeSettle] or [body]). One loop per
 * session.
 *
 * @param pacedFrames whether settling waits for the frame clock's pump instead of sending frames,
 *   see [HeadlessSession.awaitQuiescence]. When set, an iteration that did not settle because
 *   content keeps requesting frames is followed by another one as soon as [body] returns, so an
 *   animating composition is rendered at the pump's pace rather than only when state is written.
 * @param maxIterations passed to [HeadlessSession.awaitQuiescence].
 * @param maxFrames passed to [HeadlessSession.awaitQuiescence]. With [pacedFrames], a small value
 *   makes an animating composition render every few frames instead of once it settles.
 * @param observeRenderReads whether snapshot state read inside [RenderScope.renderIfChanged]
 *   invalidates the next render when written. Off for a host that renders once.
 * @param changeCount a counter that moves whenever composition applied changes the render cares
 *   about; defaults to `Recomposer.changeCount`. An applier's own change count is more precise.
 * @param cancelRenderOnInvalidation whether a change that supersedes the render cancels [body]
 *   while it is running, so that the next iteration renders the newer state right away instead of
 *   after the stale render completes. A change supersedes the render once it is composed and alters
 *   what [RenderScope.renderIfChanged] would see: a change to the tree or to state the render read.
 *   Other writes, elsewhere in the process or applied before the render started, let it finish. For
 *   hosts whose render is long and whose output only matters when current (e.g. inflating remote
 *   views for a widget). The cancelled body must leave no partial output behind.
 *   [RenderScope.renderIfChanged] itself never observes the cancellation mid-block, as the block
 *   does not suspend; it takes effect at the next suspension point.
 * @param beforeSettle runs at the start of every iteration, before settling, e.g. to wait for a
 *   rate limiter so that writes made meanwhile are conflated into this iteration.
 * @param body renders and decides whether to continue. Runs in the caller's context, as a child
 *   coroutine when [cancelRenderOnInvalidation] is set.
 */
internal suspend fun HeadlessSession.renderLoop(
    pacedFrames: Boolean = false,
    maxIterations: Int = MAX_IDLE_ITERATIONS,
    maxFrames: Int = MAX_FRAMES,
    observeRenderReads: Boolean = true,
    changeCount: () -> Long = { recomposer.changeCount },
    cancelRenderOnInvalidation: Boolean = false,
    beforeSettle: suspend () -> Unit = {},
    body: suspend RenderScope.() -> Boolean,
) {
    check(onApplied == null) { "HeadlessSession already has a render loop" }
    val tracker = RenderInvalidationTracker()
    // Notifications run inline on the applying thread: the callbacks only flag the tracker and
    // wake the loop. The read callback instance must be stable, as the observer keys its scope
    // maps by it.
    val readObserver = if (observeRenderReads) SnapshotStateObserver { it() } else null
    val onRenderReadChanged: (RenderInvalidationTracker) -> Unit = { changed ->
        changed.onRenderReadChanged()
        // The session's apply observer, registered earlier, wakes the loop for the same
        // notification; the loop may already have sampled the tracker by the time this runs (an
        // apply() on another thread is not counted by isApplyObserverNotificationPending), so wake
        // it again to be sure the flag is seen.
        invalidate()
    }
    readObserver?.start()
    onApplied = tracker::onApplied
    try {
        var again = true
        // The first iteration renders unconditionally; later ones wait for a change.
        while (again) {
            beforeSettle()
            if (isClosed) return
            val quiescence =
                awaitQuiescence(
                    maxIterations = maxIterations,
                    pacedFrames = pacedFrames,
                    maxFrames = maxFrames,
                )
            // Changes applied while settling are composed already; their pending signal would
            // only buy a spurious iteration (and, for a host that waits in beforeSettle, a
            // needless wait). Drop it; whether anything is left to do is checked after the render.
            val changeSeen = invalidations.tryReceive().isSuccess
            val scope =
                RenderScopeImpl(
                    quiescence,
                    this,
                    tracker,
                    readObserver,
                    onRenderReadChanged,
                    changeCount,
                )
            val outcome =
                if (cancelRenderOnInvalidation) {
                    scope.runCancellable(
                        settle = {
                            // A closed session's invalidations end the select on the next pass.
                            if (!isClosed) {
                                awaitQuiescence(
                                    maxIterations = maxIterations,
                                    pacedFrames = pacedFrames,
                                    maxFrames = maxFrames,
                                )
                            }
                        },
                        body = body,
                    )
                } else {
                    scope.body()
                }
            when (outcome) {
                false -> return
                null -> {
                    // Superseded: the change that cancelled the body starts the next iteration.
                    again = true
                    continue
                }
                true -> {}
            }
            again =
                if (
                    pacedFrames &&
                        quiescence != QuiescenceResult.Quiescent &&
                        frameClock.hasAwaiters
                ) {
                    // Content still animating: render again after the pump's next frames rather
                    // than only when it writes state, which a pure withFrameNanos loop never does.
                    invalidations.tryReceive()
                    true
                } else if (
                    (changeSeen || scope.changeSeen) &&
                        (tracker.needsRender(changeCount()) || recomposer.hasPendingWork)
                ) {
                    // A dropped or consumed signal was not stale after all: a change landed after
                    // the recomposer last looked (pending), or composed after the render began.
                    true
                } else {
                    invalidations.receiveCatching().isSuccess
                }
        }
    } catch (e: CancellationException) {
        // Closing the session cancels its frame clock and recomposer, which ends a settle in
        // progress with a CancellationException: that is the loop ending, not the caller's.
        currentCoroutineContext().ensureActive()
        if (!isClosed) throw e
    } finally {
        onApplied = null
        readObserver?.stop()
        readObserver?.clear()
    }
}

/** Result of [HeadlessSession.invalidations] in [runCancellable]'s select. */
private object Invalidated

private object Closed

/**
 * Runs [body] as a child coroutine and returns its result, or `null` if a change superseded the
 * render, in which case the body is cancelled and joined. A change signalled while the body runs is
 * composed first ([settle]) and supersedes the render only if it altered what a render sees, see
 * [RenderScopeImpl.isSuperseded]; otherwise the body goes on. Returns `false` if the session's
 * invalidations closed meanwhile, which ends the loop.
 */
private suspend fun RenderScopeImpl.runCancellable(
    settle: suspend () -> Unit,
    body: suspend RenderScope.() -> Boolean,
): Boolean? = coroutineScope {
    val render = async { body() }
    var result: Boolean? = null
    while (true) {
        val outcome =
            select<Any?> {
                render.onAwait { it }
                session.invalidations.onReceiveCatching { received ->
                    if (received.isClosed) Closed else Invalidated
                }
            }
        if (outcome is Boolean) {
            result = outcome
            break
        }
        if (outcome == Closed) {
            render.cancelAndJoin()
            result = false
            break
        }
        changeSeen = true
        settle()
        if (!render.isCompleted && isSuperseded()) {
            render.cancelAndJoin()
            result = null
            break
        }
    }
    result
}

private class RenderScopeImpl(
    override val quiescence: QuiescenceResult,
    val session: HeadlessSession,
    private val tracker: RenderInvalidationTracker,
    private val readObserver: SnapshotStateObserver?,
    private val onRenderReadChanged: (RenderInvalidationTracker) -> Unit,
    private val changeCount: () -> Long,
) : RenderScope {
    /** Whether a signal from [HeadlessSession.invalidations] was consumed during this iteration. */
    @Volatile var changeSeen = false

    /**
     * Whether a change composed since the last render would make the next [renderIfChanged] render
     * again: the tree changed, a state the render read did, or a change landed mid-render.
     */
    fun isSuperseded(): Boolean = tracker.needsRender(changeCount())

    override suspend fun <T : Any> renderIfChanged(applyWrites: Boolean, block: () -> T): T? =
        withContext(session.recomposerDispatcher) {
            if (!tracker.beginRender(changeCount())) return@withContext null
            try {
                withRenderSnapshot(applyWrites) {
                    if (readObserver == null) {
                        block()
                    } else {
                        var result: T? = null
                        readObserver.observeReads(tracker, onRenderReadChanged) { result = block() }
                        checkNotNull(result) { "renderIfChanged block returned null" }
                    }
                }
            } finally {
                tracker.endRender()
            }
        }
}
