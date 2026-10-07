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

@file:OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalRemoteCreationComposeApi::class,
)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteComposeApplier
import androidx.compose.remote.creation.compose.layout.RemoteRootNode
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.trace
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tracing.traceAsync
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * Capture a single RemoteCompose document from the specified [content] Composable by rendering it
 * once inside a virtual display.
 *
 * This is a suspending function that performs the composition and rendering, returning a
 * [CapturedDocument] which contains the serialized bytes and metadata.
 *
 * @param context The Android [Context] to use.
 * @param creationDisplayInfo Details about the virtual display to capture for (size, density,
 *   etc.). Defaults to display metrics derived from [context].
 * @param remoteDensity The logical screen density and font scale to use for unit conversions.
 *   Defaults to density derived from [creationDisplayInfo]. Note: If passing custom values, they
 *   should typically match the density and font scale specified in [creationDisplayInfo] to avoid
 *   layout scaling discrepancies.
 * @param layoutDirection The layout direction (LTR or RTL) to use. Defaults to the layout direction
 *   of [context]'s configuration.
 * @param clock The clock used for the composition timeline. Defaults to [RemoteClock.SYSTEM].
 * @param profile The writing profile that determines supported operations. Defaults to
 *   [RcPlatformProfiles.ANDROIDX].
 * @param writerEvents Callback to handle non-serializable events (e.g. pending intents).
 * @param content The Composable content to render and capture.
 * @return A [CapturedDocument] containing the serialized document bytes.
 */
public suspend fun captureSingleRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo = createCreationDisplayInfo(context),
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection =
        toLayoutDirection(context.resources.configuration.layoutDirection),
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerEvents: WriterEvents = WriterEvents(),
    content: @Composable @RemoteComposable () -> Unit,
): CapturedDocument {
    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    val recomposerDispatcher =
        currentCoroutineContext()
            .toSingleThreadedRecomposerContext(name = "captureSingleRemoteDocument")
    val writeTracker = CaptureWriteTracker()
    val recomposer =
        Recomposer(currentCoroutineContext() + recomposerDispatcher + writeTracker.contextElement)
    val composition = Composition(applier, recomposer)
    val lifecycleOwner = HeadlessLifecycleOwner()

    try {
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
                writerEvents = writerEvents,
                layoutDirection = layoutDirection,
                remoteDensity = remoteDensity,
            )

        val initialSize = creationState.document.buffer.buffer.size()

        withContext(recomposerDispatcher) {
            composition.setContent {
                CompositionLocalProvider(
                    LocalRemoteComposeCreationState provides creationState,
                    LocalInspectionMode provides creationDisplayInfo.isInspectionMode,
                    LocalDensity provides
                        Density(
                            creationDisplayInfo.density.density,
                            creationDisplayInfo.density.fontScale,
                        ),
                    LocalRemoteDensity provides remoteDensity,
                    LocalContext provides context,
                    LocalConfiguration provides context.resources.configuration,
                    LocalLayoutDirection provides layoutDirection,
                    LocalFontWeightAdjustment provides
                        platformFontWeightAdjustment(context.resources.configuration),
                    LocalLifecycleOwner provides lifecycleOwner,
                    LocalIs24HourFormat provides DateFormat.is24HourFormat(context),
                    content = content,
                )
            }
        }

        // Unlike the streaming captureRemoteDocument, a single capture does not acquire
        // SnapshotWriteMonitor. Global writes made by effects are applied by the explicit
        // Snapshot.sendApplyNotifications() inside awaitRecomposerQuiescence, which is enough for a
        // one-shot capture and avoids starting a process-wide coalescing loop for a short-lived
        // call. writeTracker only records this capture's own writes, see CaptureWriteTracker.
        coroutineScope {
            lateinit var frameClock: BroadcastFrameClock
            val frameCounter = FrameCounter()
            frameClock = BroadcastFrameClock {
                launch(recomposerDispatcher) { frameCounter.sendFrame(frameClock, clock) }
            }
            try {
                traceAsync(
                    "CaptureRemoteDocument:captureSingleRemoteDocument:compositionInitialization",
                    ThreadLocalRandom.current().nextInt(),
                ) {
                    launch(recomposerDispatcher + frameClock) {
                        // Queued before the runner's first slice can request a frame, so it runs
                        // after the Recomposer registers its apply observer but before any effect
                        // resumes from withFrameNanos.
                        launch { writeTracker.start() }
                        recomposer.runRecomposeAndApplyChanges()
                    }

                    val maxIterations = MAX_IDLE_ITERATIONS
                    when (
                        awaitRecomposerQuiescence(
                            recomposer = recomposer,
                            frameClock = frameClock,
                            frameCounter = frameCounter,
                            recomposerDispatcher = recomposerDispatcher,
                            clock = clock,
                            maxIterations = maxIterations,
                            writeTracker = writeTracker,
                        )
                    ) {
                        QuiescenceResult.Quiescent -> {}
                        QuiescenceResult.Unstable ->
                            throw IllegalStateException(
                                "captureSingleRemoteDocument did not reach a quiescent Idle " +
                                    "state after $maxIterations iterations. This is likely " +
                                    "caused by unstable recomposition (e.g. a Composable or " +
                                    "effect continuously mutating state on every frame or " +
                                    "snapshot apply notification)."
                            )
                        QuiescenceResult.ContinuousFrames ->
                            throw IllegalStateException(
                                "captureSingleRemoteDocument content requested more than " +
                                    "$MAX_FRAMES frames without settling. This is likely " +
                                    "caused by unstable recomposition (e.g. an effect " +
                                    "mutating state on every frame), an infinite animation " +
                                    "or a withFrameNanos loop. Compose animations cannot be " +
                                    "captured as they run; use remote expressions to animate " +
                                    "remote documents."
                            )
                        QuiescenceResult.ApplyNotificationsTimedOut ->
                            throw IllegalStateException(
                                "captureSingleRemoteDocument timed out waiting for another " +
                                    "thread to finish notifying snapshot apply observers. A " +
                                    "snapshot apply observer elsewhere in the process may be " +
                                    "slow or blocked."
                            )
                    }
                }
            } finally {
                writeTracker.stop()
                frameClock.cancel()
                recomposer.cancel()
                currentCoroutineContext().cancelChildren()
            }
        }

        val document =
            withContext(recomposerDispatcher) {
                withRenderSnapshot {
                    val remoteCanvas = RemoteCanvas(creationState)

                    if (RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled) {
                        check(creationState.document.buffer.buffer.size() == initialSize) {
                            "Document was written to during composition. Expected size $initialSize, got ${creationState.document.buffer.buffer.size()}"
                        }
                    }

                    trace("CaptureRemoteDocument:captureSingleRemoteDocument:rootNodeRender") {
                        rootNode.render(creationState, remoteCanvas)
                        remoteCanvas.flush()
                    }

                    creationState.document.encodeToByteArray()
                }
            }

        return CapturedDocument(document, writerEvents.pendingIntents, writerEvents.lambdas)
    } finally {
        withContext(recomposerDispatcher + NonCancellable) {
            recomposer.cancel()
            lifecycleOwner.destroy()
            composition.dispose()
        }
    }
}

/**
 * Capture a stream of RemoteCompose documents by rendering the specified [content] Composable in a
 * virtual display and emitting the resulting byte arrays whenever recomposition occurs and the
 * layout visually changes.
 *
 * This API allows capturing dynamic Compose content (e.g., containing animations, transitions, or
 * state updates) as a Flow of serialized document byte arrays.
 *
 * Crucially, recomposition is handled cleanly, and duplicate documents (where nothing visually
 * changed in the layout tree) are automatically filtered out, so new byte arrays are only emitted
 * when the document actually changes.
 *
 * Remote documents are expected to change rarely; animate with remote expressions rather than
 * recomposition. Content that updates faster than a few documents in quick succession followed by
 * about one per second is throttled, and a warning is logged. The latest state is still emitted
 * once the throttle allows.
 *
 * @param creationDisplayInfo Details about the virtual display to capture for (size, density,
 *   etc.).
 * @param remoteDensity The logical screen density and font scale to use for unit conversions.
 *   Defaults to density derived from [creationDisplayInfo]. Note: If passing custom values, they
 *   should typically match the density and font scale specified in [creationDisplayInfo] to avoid
 *   layout scaling discrepancies.
 * @param layoutDirection The layout direction (LTR or RTL) to use. Defaults to the layout direction
 *   of [context]'s configuration when `null`.
 * @param writerEvents Callback to handle non-serializable events (e.g. pending intents).
 * @param context The Android [Context] to use.
 * @param clock The clock used for the recomposer timeline. Defaults to [RemoteClock.SYSTEM].
 * @param profile The writing profile that determines supported operations. Defaults to
 *   [RcPlatformProfiles.ANDROIDX].
 * @param coroutineContext The CoroutineContext to run recomposition and rendering on. Defaults to
 *   [Dispatchers.Default].
 * @param content The Composable content to render and capture.
 * @return A [Flow] of [ByteArray]s containing the serialized RemoteCompose documents.
 */
public fun captureRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo,
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection? = null,
    writerEvents: WriterEvents = WriterEvents(),
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    content: @Composable @RemoteComposable () -> Unit,
): Flow<ByteArray> =
    captureRemoteDocument(
        context = context,
        creationDisplayInfo = creationDisplayInfo,
        updateThrottle = CaptureUpdateThrottle.Default,
        remoteDensity = remoteDensity,
        layoutDirection = layoutDirection,
        writerEvents = writerEvents,
        clock = clock,
        profile = profile,
        coroutineContext = coroutineContext,
        content = content,
    )

/**
 * Like the public [captureRemoteDocument], but with [updateThrottle] deciding how often documents
 * are emitted instead of [CaptureUpdateThrottle.Default].
 *
 * TODO(b/567847315): Make the update strategy public together with [CaptureUpdateThrottle].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun captureRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo,
    updateThrottle: CaptureUpdateThrottle,
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection? = null,
    writerEvents: WriterEvents = WriterEvents(),
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    content: @Composable @RemoteComposable () -> Unit,
): Flow<ByteArray> = flow {
    require(RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled) {
        "captureRemoteDocument requires isEnforceCleanRecompositionEnabled to be true"
    }

    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    val recomposerDispatcher =
        (currentCoroutineContext() + coroutineContext).toSingleThreadedRecomposerContext(
            name = "captureRemoteDocument"
        )
    val writeTracker = CaptureWriteTracker()
    val recomposer =
        Recomposer(currentCoroutineContext() + recomposerDispatcher + writeTracker.contextElement)
    val composition = Composition(applier, recomposer)
    val lifecycleOwner = HeadlessLifecycleOwner()

    try {
        val layoutDirection =
            (layoutDirection ?: toLayoutDirection(context.resources.configuration.layoutDirection))
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
                writerEvents = writerEvents,
                layoutDirection = layoutDirection,
                remoteDensity = remoteDensity,
            )

        val initialSize = creationState.document.buffer.buffer.size()

        withContext(recomposerDispatcher) {
            composition.setContent {
                CompositionLocalProvider(
                    LocalRemoteComposeCreationState provides creationState,
                    LocalInspectionMode provides creationDisplayInfo.isInspectionMode,
                    LocalDensity provides
                        Density(
                            creationDisplayInfo.density.density,
                            creationDisplayInfo.density.fontScale,
                        ),
                    LocalRemoteDensity provides remoteDensity,
                    LocalContext provides context,
                    LocalConfiguration provides context.resources.configuration,
                    LocalLayoutDirection provides layoutDirection,
                    LocalFontWeightAdjustment provides
                        platformFontWeightAdjustment(context.resources.configuration),
                    LocalLifecycleOwner provides lifecycleOwner,
                    LocalIs24HourFormat provides DateFormat.is24HourFormat(context),
                    content = content,
                )
            }
        }

        coroutineScope {
            lateinit var frameClock: BroadcastFrameClock
            val frameCounter = FrameCounter()
            // Frames are sent as soon as an awaiter appears. The Recomposer itself awaits frames on
            // this clock (runRecomposeAndApplyChanges below), so pacing here with a time-based
            // delay would add latency to every recomposition and stall entirely on hosts whose
            // clock only advances explicitly (virtual-time test dispatchers, a paused Robolectric
            // looper). Emitted documents are rate limited separately by updateThrottle, between
            // renders rather than between frames.
            frameClock = BroadcastFrameClock {
                launch(recomposerDispatcher) { frameCounter.sendFrame(frameClock, clock) }
            }
            SnapshotWriteMonitor.acquire()
            try {
                launch(recomposerDispatcher + frameClock) {
                    // Queued before the runner's first slice can request a frame, so it runs after
                    // the Recomposer registers its apply observer but before any effect resumes
                    // from withFrameNanos.
                    launch { writeTracker.start() }
                    recomposer.runRecomposeAndApplyChanges()
                }

                // Regeneration must not be driven by observing Recomposer.currentState
                // transitions. currentState is a conflated StateFlow: a collector that is
                // descheduled (e.g. under CPU contention) while the recomposer cycles
                // Idle -> Recomposing -> Idle observes no value change at all, so
                // `filter { it == Idle }` never re-emits and the updated document is silently
                // dropped. Drive regeneration from snapshot apply notifications instead. A
                // CONFLATED channel collapses bursts of writes into a single regeneration but,
                // unlike a StateFlow value slot, can never lose the fact that state changed.
                val invalidations = Channel<Unit>(Channel.CONFLATED)
                // Seed so the initial document is always produced.
                invalidations.trySend(Unit)
                val renderTracker = RenderInvalidationTracker()
                val applyObserverHandle = Snapshot.registerApplyObserver { changed, _ ->
                    if (renderTracker.onApplied(changed)) {
                        invalidations.trySend(Unit)
                    }
                }

                try {
                    var previousBytes: ByteArray? = null
                    val rateLimiter = updateThrottle.newLimiter()
                    for (invalidation in invalidations) {
                        // Wait for the throttle before settling and rendering, so writes made
                        // meanwhile are conflated into the invalidation channel and the document
                        // rendered afterwards reflects the latest state. Only emitted documents
                        // count towards the limit.
                        rateLimiter?.awaitPermit()
                        // Let the recomposer observe and apply the invalidation before
                        // rendering. An unsettled composition (e.g. continuously animating
                        // content) still renders once the iteration or frame bound is hit, so
                        // streaming captures keep producing documents, at the rate allowed by
                        // updateThrottle.
                        awaitRecomposerQuiescence(
                            recomposer = recomposer,
                            frameClock = frameClock,
                            frameCounter = frameCounter,
                            recomposerDispatcher = recomposerDispatcher,
                            clock = clock,
                            writeTracker = writeTracker,
                        )

                        val bytes =
                            withContext(recomposerDispatcher) {
                                if (!renderTracker.beginRender(applier.changeCount)) {
                                    return@withContext null
                                }
                                val renderReads = newIdentitySet()
                                try {
                                    withRenderSnapshot(
                                        readObserver = { read -> renderReads.add(read) },
                                        writeObserver = { written ->
                                            renderTracker.onRenderWrite(written)
                                        },
                                    ) {
                                        creationState.document =
                                            profile.create(
                                                creationDisplayInfo.toCreationDisplayInfo(),
                                                writerEvents,
                                            )
                                        creationState.expressionCache.clear()
                                        creationState.intExpressionCache.clear()
                                        creationState.remoteVariableToId.clear()
                                        creationState.floatArrayCache.clear()
                                        creationState.longArrayCache.clear()
                                        val remoteCanvas = RemoteCanvas(creationState)

                                        check(
                                            creationState.document.buffer.buffer.size() ==
                                                initialSize
                                        ) {
                                            "Document was written to during composition. Expected size $initialSize, got ${creationState.document.buffer.buffer.size()}"
                                        }

                                        rootNode.render(creationState, remoteCanvas)
                                        remoteCanvas.flush()

                                        creationState.document.encodeToByteArray()
                                    }
                                } finally {
                                    renderTracker.endRender(renderReads)
                                }
                            } ?: continue

                        if (previousBytes?.contentEquals(bytes) != true) {
                            previousBytes = bytes
                            rateLimiter?.onEmitted()
                            emit(bytes)
                        }
                    }
                } finally {
                    applyObserverHandle.dispose()
                    invalidations.close()
                }
            } finally {
                writeTracker.stop()
                frameClock.cancel()
                recomposer.cancel()
                currentCoroutineContext().cancelChildren()
                SnapshotWriteMonitor.release()
            }
        }
    } finally {
        withContext(recomposerDispatcher + NonCancellable) {
            recomposer.cancel()
            lifecycleOwner.destroy()
            composition.dispose()
        }
    }
}

private const val MAX_IDLE_ITERATIONS = 100

/**
 * Sends and counts all frames for a capture, so that [awaitRecomposerQuiescence] can detect content
 * that never stops requesting frames. Every frame for a capture must be sent through [sendFrame],
 * including those from the [BroadcastFrameClock]'s `onNewAwaiters` callback.
 */
private class FrameCounter {
    private val sent = AtomicLong()
    @Volatile private var budget: Budget? = null

    private class Budget(val limit: Long, val job: Job) {
        @Volatile var isExhausted = false
    }

    /**
     * Sends a frame on [frameClock] at [clock]'s current time, cancelling a [withFrameBudget] it
     * exhausts.
     */
    fun sendFrame(frameClock: BroadcastFrameClock, clock: RemoteClock) {
        val count = sent.incrementAndGet()
        budget?.let {
            if (count > it.limit && !it.isExhausted) {
                it.isExhausted = true
                it.job.cancel(CancellationException("Frame budget exhausted"))
            }
        }
        frameClock.sendFrame(clock.nanoTime())
    }

    /**
     * Runs [block], or returns `null` once more than [maxFrames] frames are sent before it
     * completes.
     *
     * The block is cancelled from [sendFrame] rather than having it check the count, because the
     * block may be suspended on something that never resumes while frames are sent: with an effect
     * in a `withFrameNanos` loop, [Recomposer.currentState] stays [Recomposer.State.PendingWork]
     * and never emits again. Cancelling launches no coroutine, so it is safe on immediate
     * dispatchers.
     */
    suspend fun <T> withFrameBudget(maxFrames: Int, block: suspend CoroutineScope.() -> T): T? {
        var current: Budget? = null
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
private const val MAX_FRAMES = 100

/** Outcome of [awaitRecomposerQuiescence]. */
private enum class QuiescenceResult {
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
 *    tracked by [BroadcastFrameClock.hasAwaiters], not by [Recomposer.hasPendingWork]),
 * 2. Continuations resuming from `withFrameNanos {}` are dispatched onto [recomposerDispatcher]
 *    after [BroadcastFrameClock.sendFrame] completes, and
 * 3. Another thread concurrently inside [Snapshot.sendApplyNotifications] (e.g.
 *    [SnapshotWriteMonitor], or an app's own `GlobalSnapshotManager`) may have claimed pending
 *    global writes while its apply observer notifications are still in flight (tracked by
 *    [Snapshot.isApplyObserverNotificationPending]). Until they complete,
 *    [Recomposer.hasPendingWork] cannot yet reflect those writes, so this waits for them to drain
 *    via [awaitApplyObserverNotifications] before sampling.
 *
 * Limitation of (3): a `MutableSnapshot.apply()` on another thread also claims pending global
 * writes and notifies observers after releasing the global lock, but the runtime does not count
 * those notifications in [Snapshot.isApplyObserverNotificationPending]. This capture's own writes
 * claimed that way are also waited for via [writeTracker] (see [CaptureWriteTracker]); writes from
 * elsewhere in the process are not covered. This session's own render snapshots avoid widening the
 * window by only applying when they wrote state (see [withRenderSnapshot]).
 *
 * Note that [Recomposer.currentState] is only ever sampled by value here, never used to detect a
 * transition, so conflation of the underlying [StateFlow] cannot cause a missed wake-up.
 *
 * Content that never stops requesting frames (e.g. an infinite animation) can never be quiescent,
 * and the Recomposer may never even be observed [Recomposer.State.Idle] while [frameClock] keeps
 * being driven: it stays [Recomposer.State.PendingWork], so [Recomposer.currentState] never emits
 * again. Every frame sent for this capture (by this loop or by [frameClock]'s `onNewAwaiters`
 * callback) goes through [frameCounter], which cancels this wait with
 * [QuiescenceResult.ContinuousFrames] once more than [MAX_FRAMES] frames were sent during this
 * call, wherever it is suspended. A slow composition that sends no frames never trips it.
 *
 * @param frameCounter sends and counts every frame on [frameClock], see [FrameCounter].
 * @param maxIterations bound on the number of non-quiescent iterations before giving up.
 * @param writeTracker tracks this capture's undelivered global writes, see [CaptureWriteTracker].
 */
private suspend fun awaitRecomposerQuiescence(
    recomposer: Recomposer,
    frameClock: BroadcastFrameClock,
    frameCounter: FrameCounter,
    recomposerDispatcher: CoroutineContext,
    clock: RemoteClock,
    writeTracker: CaptureWriteTracker,
    maxIterations: Int = MAX_IDLE_ITERATIONS,
): QuiescenceResult =
    withContext(recomposerDispatcher) {
        // yield() so continuations queued on recomposerDispatcher (e.g. effects resuming from
        // withFrameNanos after sendFrame) run before quiescence is sampled. recomposerDispatcher is
        // usually a combined context, so the interceptor must be read out of it. Skip yield() for
        // immediate dispatchers like Dispatchers.Main.immediate on the main thread (where
        // continuations run inline), as yield() forces a Handler.post() via YieldContext, which
        // deadlocks if the main thread is blocked inside runBlocking/runTest.
        val interceptor = recomposerDispatcher[ContinuationInterceptor]
        val shouldYield =
            interceptor !is CoroutineDispatcher ||
                interceptor.isDispatchNeeded(EmptyCoroutineContext)
        // The frame budget covers the whole call, not each iteration, otherwise content that
        // animates forever would get MAX_FRAMES frames for every one of maxIterations iterations.
        frameCounter.withFrameBudget(MAX_FRAMES) {
            var idleIterations = 0
            while (idleIterations < maxIterations) {
                recomposer.currentState.first { it == Recomposer.State.Idle }
                if (shouldYield) yield()
                while (frameClock.hasAwaiters) {
                    frameCounter.sendFrame(frameClock, clock)
                    // Without yield() nothing else checks for cancellation by the frame budget.
                    ensureActive()
                    if (shouldYield) yield()
                }
                Snapshot.sendApplyNotifications()
                if (!awaitApplyObserverNotifications(canYield = shouldYield)) {
                    return@withFrameBudget QuiescenceResult.ApplyNotificationsTimedOut
                }
                writeTracker.awaitDelivery(canYield = shouldYield)
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
private suspend inline fun pollUntil(canYield: Boolean, isDone: () -> Boolean): Boolean {
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

/**
 * Called on the capture's thread when [CaptureWriteTracker] finds writes made by the capture that
 * have not been delivered to apply observers yet, before it waits for them. Lets tests hold back a
 * delivery until the capture is known to be waiting for it.
 */
@VisibleForTesting @Volatile internal var onCaptureAwaitingWriteDelivery: (() -> Unit)? = null

/**
 * Tracks global snapshot writes made by one capture's own coroutines (effects and their frame
 * callbacks) until an apply notification delivers them.
 *
 * A `MutableSnapshot.apply()` on another thread (e.g. a concurrent capture's recomposer, or a
 * render snapshot) claims every pending global write and delivers it to apply observers only after
 * releasing the global lock, outside [Snapshot.isApplyObserverNotificationPending]. If that claims
 * a write made by this capture's effect, [Snapshot.sendApplyNotifications] here finds nothing to
 * send and [Recomposer.hasPendingWork] stays `false` until the other thread delivers it, so the
 * capture could sample itself as quiescent and render stale state (b/571028326).
 *
 * Writes are attributed to the capture through [contextElement], a thread-local marker installed
 * while the capture's coroutines run (the Recomposer's effect context inherits it), so writes from
 * elsewhere in the process are ignored. Its apply observer must run after the Recomposer's, so that
 * once a write is delivered here the Recomposer has already seen it: [start] it after the
 * Recomposer registers its apply observer, and before effects can resume from a frame.
 *
 * Thread-safe: observers run on whichever thread writes or delivers.
 */
private class CaptureWriteTracker {
    private val activeTracker = ThreadLocal<CaptureWriteTracker?>()
    private val lock = Any()
    private val undelivered = newIdentitySet()
    private var writeObserver: ObserverHandle? = null
    private var applyObserver: ObserverHandle? = null
    private var isStopped = false

    /** Marks coroutines running in a context containing this element as this capture's. */
    val contextElement: CoroutineContext.Element = activeTracker.asContextElement(this)

    /**
     * Waits until every write made by this capture has been delivered to apply observers, sending
     * apply notifications between polls so writes made while the dispatcher is yielded are
     * delivered too.
     *
     * The wait is bounded by [pollUntil] and never fails the capture: the runtime calls write
     * observers after releasing the snapshot lock, so another thread can claim and deliver a write
     * before it is recorded here. Such an entry is indistinguishable from one still in flight, so
     * entries left at the deadline are dropped.
     */
    suspend fun awaitDelivery(canYield: Boolean) {
        if (synchronized(lock) { undelivered.isEmpty() }) return
        onCaptureAwaitingWriteDelivery?.invoke()
        val isDelivered =
            pollUntil(canYield) {
                Snapshot.sendApplyNotifications()
                synchronized(lock) { undelivered.isEmpty() }
            }
        if (!isDelivered) synchronized(lock) { undelivered.clear() }
    }

    /** Starts tracking, unless already started or [stop]ped. */
    fun start() {
        synchronized(lock) { if (isStopped || applyObserver != null) return }
        // Registered outside lock: registration takes the snapshot lock, which a writer may hold
        // while calling the write observer below.
        val apply = Snapshot.registerApplyObserver { changed, _ ->
            synchronized(lock) { if (undelivered.isNotEmpty()) undelivered.removeAll(changed) }
        }
        val write = Snapshot.registerGlobalWriteObserver { state ->
            if (activeTracker.get() === this) synchronized(lock) { undelivered.add(state) }
        }
        val isStarted =
            synchronized(lock) {
                if (isStopped) {
                    false
                } else {
                    applyObserver = apply
                    writeObserver = write
                    true
                }
            }
        if (!isStarted) {
            write.dispose()
            apply.dispose()
        }
    }

    /** Stops tracking and releases the observers. A later [start] does nothing. */
    fun stop() {
        val (write, apply) =
            synchronized(lock) {
                isStopped = true
                undelivered.clear()
                (writeObserver to applyObserver).also {
                    writeObserver = null
                    applyObserver = null
                }
            }
        write?.dispose()
        apply?.dispose()
    }
}

/**
 * Runs [block] inside a new mutable snapshot, applying it only if [block] wrote state.
 *
 * `MutableSnapshot.apply()` also claims any pending global snapshot writes, and delivers their
 * apply notifications without being tracked by [Snapshot.isApplyObserverNotificationPending]. An
 * unconditional apply after every render would therefore let this session take writes made by
 * another concurrent session's effects, which could then sample itself as quiescent before its
 * recomposer has seen them. Disposing a snapshot that wrote nothing avoids that.
 */
private fun <T> withRenderSnapshot(
    readObserver: ((Any) -> Unit)? = null,
    writeObserver: ((Any) -> Unit)? = null,
    block: () -> T,
): T {
    val snapshot =
        Snapshot.takeMutableSnapshot(readObserver = readObserver, writeObserver = writeObserver)
    try {
        val result = snapshot.enter(block)
        if (snapshot.hasPendingChanges()) {
            snapshot.apply().check()
        }
        return result
    } finally {
        snapshot.dispose()
    }
}

/**
 * Decides when the streaming [captureRemoteDocument] loop must wake up, and when it must actually
 * re-render and re-encode the document.
 * - Apply notifications consisting only of states written by this session's own render are ignored,
 *   otherwise regeneration would spin. Tracking the exact state objects mutated inside the render
 *   snapshot avoids swallowing global snapshot changes from other threads that happen to be
 *   coalesced during `apply()`.
 * - Any other apply notification wakes the loop so the recomposer is driven to quiescence.
 * - A render is only performed if composition applied changes to the node tree since the previous
 *   render ([RemoteComposeApplier.changeCount] moved), a state read by the previous render changed
 *   (e.g. inside a `RemoteCanvas` draw lambda, which runs at render time), or an external change
 *   landed while a render was in progress. Unrelated global snapshot writes elsewhere in the
 *   process therefore no longer cost a full render and encode.
 *
 * Limits: render inputs that are not snapshot state (e.g. plain fields mutated from an effect) are
 * not observed. Previously any unrelated snapshot write forced a re-render and would incidentally
 * pick such changes up; now they are only rendered alongside the next tree change or render-read
 * state change.
 *
 * Thread-safe: [onApplied] runs on whichever thread sends apply notifications.
 */
private class RenderInvalidationTracker {
    private val lock = Any()
    private val renderWrites = newIdentitySet()
    private var renderReads: Set<Any> = emptySet()
    private var isRendering = false
    private var renderInputsChanged = true
    private var lastRenderChangeCount = 0L

    /** Apply observer hook. Returns `true` if the capture loop should wake up. */
    fun onApplied(changed: Set<Any>): Boolean =
        synchronized(lock) {
            val hasExternalModification =
                renderWrites.isEmpty() || changed.any { it !in renderWrites }
            if (hasExternalModification && !renderInputsChanged) {
                renderInputsChanged =
                    isRendering || changed.any { it !in renderWrites && it in renderReads }
            }
            hasExternalModification
        }

    fun onRenderWrite(state: Any) {
        synchronized(lock) { renderWrites.add(state) }
    }

    /**
     * Returns `true` and marks a render as in progress if the document may have changed since the
     * previous render; `false` if rendering can be skipped.
     */
    fun beginRender(changeCount: Long): Boolean =
        synchronized(lock) {
            val shouldRender = renderInputsChanged || changeCount != lastRenderChangeCount
            if (shouldRender) {
                renderInputsChanged = false
                lastRenderChangeCount = changeCount
                isRendering = true
            }
            shouldRender
        }

    /** Ends a render started by [beginRender], recording the states it read. */
    fun endRender(reads: Set<Any>) {
        synchronized(lock) {
            renderReads = reads
            renderWrites.clear()
            isRendering = false
        }
    }
}

private fun newIdentitySet(): MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())

/**
 * Returns a [CoroutineContext] (without a [Job]) whose dispatcher executes at most one task at a
 * time so that recomposition, frame clock callbacks, effect continuations, and rendering are
 * serialized:
 * - When no [ContinuationInterceptor] is present (e.g. a raw background or Binder thread), falls
 *   back to a single-threaded slice of [Dispatchers.Default].
 * - When running with a [MainCoroutineDispatcher], preserves the context as-is since execution is
 *   already confined to the main thread.
 * - When running on a multi-threaded [CoroutineDispatcher] (where
 *   [CoroutineDispatcher.isDispatchNeeded] is true), constrains it via
 *   `limitedParallelism(parallelism = 1, name = name)`.
 * - For unconfined dispatchers (e.g. [Dispatchers.Unconfined], where `isDispatchNeeded` is false
 *   and `limitedParallelism` throws [UnsupportedOperationException]) or custom non-dispatcher
 *   [ContinuationInterceptor]s (such as Compose test interceptors), preserves the context as-is.
 */
private fun CoroutineContext.toSingleThreadedRecomposerContext(name: String): CoroutineContext {
    val baseContext = this.minusKey(Job)
    val interceptor = baseContext[ContinuationInterceptor]
    return when {
        interceptor == null ->
            baseContext + Dispatchers.Default.limitedParallelism(parallelism = 1, name = name)
        interceptor is MainCoroutineDispatcher -> baseContext
        interceptor is CoroutineDispatcher && interceptor.isDispatchNeeded(EmptyCoroutineContext) ->
            baseContext + interceptor.limitedParallelism(parallelism = 1, name = name)
        else -> baseContext
    }
}

/**
 * Reference-counted monitor that coalesces global snapshot writes into
 * [Snapshot.sendApplyNotifications] while at least one streaming [captureRemoteDocument] session is
 * active.
 *
 * Compose Runtime's [Snapshot.registerGlobalWriteObserver] is process-wide (written state objects
 * carry no per-session identity), and a single [Snapshot.sendApplyNotifications] call advances the
 * global snapshot and notifies every active [Recomposer], each of which filters the changed set
 * against its own recorded read set.
 *
 * Therefore, multiple concurrent sessions across threads share a single observer registration and a
 * single coalescing channel loop:
 * - When active session count transitions `0 -> 1`, one global write observer and consumer loop are
 *   started.
 * - While K >= 1 sessions are active across threads, all K sessions share that single observer and
 *   single coalesced notification dispatch.
 * - When active session count drops `1 -> 0` (in `finally`), the [ObserverHandle] is disposed and
 *   the consumer coroutine is cancelled so zero global observers or background coroutines remain
 *   when idle.
 *
 * See prior art in other Compose composition hosts across AndroidX:
 * - `androidx.glance.session.globalSnapshotMonitor` and
 *   `androidx.glance.session.GlobalSnapshotManager` (session-scoped suspend monitor and
 *   process-wide coalescing channel loop on `Dispatchers.Default`).
 * - `androidx.compose.ui.platform.GlobalSnapshotManager` (process-wide coalescing channel loop on
 *   `AndroidUiDispatcher.Main`).
 * - `androidx.glance.session.SessionWorker.runSession` (hosting `Recomposer`, frame clock, and
 *   snapshot monitor inside a coroutine scope).
 */
internal object SnapshotWriteMonitor {
    private val lock = Any()
    private var refCount = 0
    private var observerHandle: ObserverHandle? = null
    private var monitorScope: CoroutineScope? = null

    @get:VisibleForTesting
    internal val activeSessionCount: Int
        get() = synchronized(lock) { refCount }

    @get:VisibleForTesting
    internal val isRunning: Boolean
        get() = synchronized(lock) { observerHandle != null }

    fun acquire() {
        synchronized(lock) {
            if (refCount++ == 0) {
                val channel = Channel<Unit>(1)
                val sent = AtomicBoolean(false)
                val scope =
                    CoroutineScope(
                        Dispatchers.Default.limitedParallelism(
                            parallelism = 1,
                            name = "RemoteComposeSnapshotWriteMonitor",
                        ) + SupervisorJob()
                    )
                // consumeEach cancels the channel when the consumer completes or is cancelled, so
                // cancelling the scope in release() also tears down the channel.
                scope.launch {
                    channel.consumeEach {
                        sent.set(false)
                        Snapshot.sendApplyNotifications()
                    }
                }
                monitorScope = scope
                observerHandle = Snapshot.registerGlobalWriteObserver {
                    if (sent.compareAndSet(false, true)) {
                        if (channel.trySend(Unit).isFailure) {
                            sent.set(false)
                        }
                    }
                }
            }
        }
    }

    fun release() {
        synchronized(lock) {
            check(refCount > 0) {
                "SnapshotWriteMonitor.release() called without matching acquire()"
            }
            if (--refCount == 0) {
                observerHandle?.dispose()
                observerHandle = null
                monitorScope?.cancel()
                monitorScope = null
            }
        }
    }
}

/**
 * Lightweight, thread-safe [LifecycleOwner] for headless document capture.
 *
 * `androidx.lifecycle.LifecycleRegistry` enforces main-thread access (`enforceMainThreadIfNeeded`),
 * which throws [IllegalStateException] when a capture runs on a background thread (e.g. a Binder
 * thread or [Dispatchers.Default]). See also `androidx.lifecycle.LifecycleRegistry.createUnsafe` as
 * prior art for bypassing main-thread enforcement in off-main-thread / headless hosts. This
 * implementation avoids main-thread enforcement and uses per-registration tokens with
 * reference-identity matching so observers can safely remove themselves during event dispatch from
 * any thread.
 *
 * Callbacks are dispatched while holding the owner's lock. This deliberately serializes all
 * lifecycle callbacks, matching the single-threaded delivery guarantee observers get from
 * `LifecycleRegistry` on the main thread, at the cost that a callback must not block on another
 * thread that is itself adding or removing an observer on this owner. As with `LifecycleRegistry`,
 * downward events (`ON_PAUSE` .. `ON_DESTROY`) are delivered in reverse registration order.
 */
private class HeadlessLifecycleOwner : LifecycleOwner {
    private class Registration(val observer: LifecycleObserver) {
        @Volatile var active = true

        fun dispatch(owner: LifecycleOwner, event: Lifecycle.Event) {
            if (observer is DefaultLifecycleObserver) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> observer.onCreate(owner)
                    Lifecycle.Event.ON_START -> observer.onStart(owner)
                    Lifecycle.Event.ON_RESUME -> observer.onResume(owner)
                    Lifecycle.Event.ON_PAUSE -> observer.onPause(owner)
                    Lifecycle.Event.ON_STOP -> observer.onStop(owner)
                    Lifecycle.Event.ON_DESTROY -> observer.onDestroy(owner)
                    Lifecycle.Event.ON_ANY -> {}
                }
            }
            // Re-check: a DefaultLifecycleObserver callback above may have removed this observer.
            if (active && observer is LifecycleEventObserver) {
                observer.onStateChanged(owner, event)
            }
        }
    }

    private val lock = Any()
    @Volatile private var state = Lifecycle.State.RESUMED
    private val registrations = CopyOnWriteArrayList<Registration>()

    override val lifecycle: Lifecycle =
        object : Lifecycle() {
            override val currentState: State
                get() = state

            override fun addObserver(observer: LifecycleObserver) {
                synchronized(lock) {
                    if (state == State.DESTROYED) {
                        return
                    }
                    val registration = Registration(observer)
                    registrations.add(registration)
                    // Only bring the observer up to the current state, which is below RESUMED if
                    // it is added from a callback while destroy() is in progress.
                    for (event in arrayOf(Event.ON_CREATE, Event.ON_START, Event.ON_RESUME)) {
                        if (!registration.active || event.targetState > state) break
                        registration.dispatch(this@HeadlessLifecycleOwner, event)
                    }
                }
            }

            override fun removeObserver(observer: LifecycleObserver) {
                synchronized(lock) {
                    for (registration in registrations.toTypedArray()) {
                        if (registration.observer === observer) {
                            registration.active = false
                            registrations.remove(registration)
                            break
                        }
                    }
                }
            }
        }

    /**
     * Moves to [Lifecycle.State.DESTROYED] one event at a time. For each of `ON_PAUSE`, `ON_STOP`
     * and `ON_DESTROY`, [state] is first set to the event's target state and then the event is
     * delivered to every observer in reverse registration order, so observers see `STARTED` in
     * `onPause`, `CREATED` in `onStop` and `DESTROYED` in `onDestroy`.
     */
    fun destroy() {
        synchronized(lock) {
            for (event in
                arrayOf(
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY,
                )) {
                if (state <= event.targetState) continue
                state = event.targetState
                for (registration in registrations.toTypedArray().reversedArray()) {
                    if (registration.active) registration.dispatch(this, event)
                }
            }
            registrations.clear()
        }
    }
}

private fun platformFontWeightAdjustment(configuration: Configuration): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (configuration.fontWeightAdjustment != Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED) {
            configuration.fontWeightAdjustment
        } else {
            0
        }
    } else {
        0
    }
