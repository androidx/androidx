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
@file:JvmName("CaptureRemoteDocumentKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteComposeApplier
import androidx.compose.remote.creation.compose.layout.RemoteRootNode
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.retain.LocalRetainedValuesStoreProvider
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.trace
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.write
import androidx.tracing.traceAsync
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * Capture a single RemoteCompose document from the specified [content] Composable by rendering it
 * once without requiring an Android `Context`.
 *
 * Composition and rendering run on the calling coroutine's dispatcher (serialized to one task at a
 * time, see [toSingleThreadedRecomposerContext]), unlike [captureRemoteDocument], which defaults to
 * [Dispatchers.Default].
 *
 * @throws IllegalStateException if [content] does not settle, e.g. because of an effect that
 *   mutates state on every frame or a Compose animation. Animate remote documents with remote
 *   expressions instead.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun captureSingleRemoteDocument(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile,
    writerCallback: Any? = null,
    platformImageProvider: PlatformImageProvider = NoOpPlatformImageProvider,
    savedState: SavedState? = null,
    retainedValuesStore: RetainedValuesStore? = null,
    content: @Composable @RemoteComposable () -> Unit,
): ByteArray =
    captureRemoteDocumentImpl(
            creationDisplayInfo = creationDisplayInfo,
            updateThrottle = CaptureUpdateThrottle.None,
            maxDocuments = 1,
            remoteDensity = remoteDensity,
            layoutDirection = layoutDirection,
            clock = clock,
            profile = profile,
            writerCallback = writerCallback,
            platformImageProvider = platformImageProvider,
            coroutineContext = EmptyCoroutineContext,
            savedState = savedState,
            retainedValuesStore = retainedValuesStore,
            content = content,
        )
        .first()

/**
 * Capture a stream of RemoteCompose documents from the specified [content] Composable without
 * requiring an Android `Context`.
 *
 * Composition and rendering run on [coroutineContext] (serialized to one task at a time, see
 * [toSingleThreadedRecomposerContext]), which defaults to [Dispatchers.Default]. Pass
 * [EmptyCoroutineContext] to run on the collecting coroutine's dispatcher instead.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun captureRemoteDocument(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    updateThrottle: CaptureUpdateThrottle = CaptureUpdateThrottle.Default,
    remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile,
    writerCallback: Any? = null,
    platformImageProvider: PlatformImageProvider = NoOpPlatformImageProvider,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    savedState: SavedState? = null,
    retainedValuesStore: RetainedValuesStore? = null,
    content: @Composable @RemoteComposable () -> Unit,
): Flow<ByteArray> =
    captureRemoteDocumentImpl(
        creationDisplayInfo = creationDisplayInfo,
        updateThrottle = updateThrottle,
        maxDocuments = Int.MAX_VALUE,
        remoteDensity = remoteDensity,
        layoutDirection = layoutDirection,
        clock = clock,
        profile = profile,
        writerCallback = writerCallback,
        platformImageProvider = platformImageProvider,
        coroutineContext = coroutineContext,
        savedState = savedState,
        retainedValuesStore = retainedValuesStore,
        content = content,
    )

/**
 * Shared implementation of [captureSingleRemoteDocument] and [captureRemoteDocument].
 *
 * The flow completes after [maxDocuments] documents have been emitted. A single-document capture
 * (`maxDocuments == 1`) differs from a streaming one in three ways only:
 * - it fails instead of rendering when the composition does not settle, since there will be no
 *   later document to correct an unsettled one;
 * - it does not acquire [SnapshotWriteMonitor]: global writes made by effects are applied by the
 *   explicit [Snapshot.sendApplyNotifications] inside [awaitRecomposerQuiescence], which is enough
 *   for a one-shot capture and avoids starting a process-wide coalescing loop for a short-lived
 *   call ([CaptureWriteTracker] still tracks this capture's own writes);
 * - it may run without [RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled].
 */
private fun captureRemoteDocumentImpl(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    updateThrottle: CaptureUpdateThrottle,
    maxDocuments: Int,
    remoteDensity: RemoteDensity,
    layoutDirection: LayoutDirection,
    clock: RemoteClock,
    profile: Profile,
    writerCallback: Any?,
    platformImageProvider: PlatformImageProvider,
    coroutineContext: CoroutineContext,
    savedState: SavedState?,
    retainedValuesStore: RetainedValuesStore?,
    content: @Composable @RemoteComposable () -> Unit,
): Flow<ByteArray> = flow {
    require(maxDocuments > 0) { "maxDocuments must be positive, was $maxDocuments" }
    val isSingleDocument = maxDocuments == 1
    require(
        isSingleDocument || RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled
    ) {
        "captureRemoteDocument requires isEnforceCleanRecompositionEnabled to be true"
    }
    val traceName = if (isSingleDocument) "captureSingleRemoteDocument" else "captureRemoteDocument"
    val settleTraceName = if (isSingleDocument) "compositionInitialization" else "settle"

    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    val recomposerDispatcher =
        (currentCoroutineContext() + coroutineContext).toSingleThreadedRecomposerContext(
            name = traceName
        )
    val writeTracker = CaptureWriteTracker()
    val recomposer =
        Recomposer(currentCoroutineContext() + recomposerDispatcher + writeTracker.contextElement)
    val composition = Composition(applier, recomposer)
    val lifecycleOwner = HeadlessLifecycleOwner(savedState)
    val saveableStateRegistry = savedState?.let {
        SaveableStateRegistry(it.toSaveableStateMap(), ::canBeSavedToBundle)
    }

    try {
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
                writerCallback = writerCallback,
                remoteDensity = remoteDensity,
                layoutDirection = layoutDirection,
                platformImageProvider = platformImageProvider,
            )

        val initialSize = creationState.document.buffer.buffer.size()

        withContext(recomposerDispatcher) {
            composition.setContent {
                CompositionLocalProvider(
                    LocalRemoteComposeCreationState provides creationState,
                    LocalRemoteDensity provides remoteDensity,
                    LocalLayoutDirection provides layoutDirection,
                    LocalLifecycleOwner provides lifecycleOwner,
                    LocalSavedStateRegistryOwner provides lifecycleOwner,
                    LocalSaveableStateRegistry provides saveableStateRegistry,
                ) {
                    if (retainedValuesStore != null) {
                        LocalRetainedValuesStoreProvider(retainedValuesStore, content)
                    } else {
                        content()
                    }
                }
            }
        }

        coroutineScope {
            // Frames are sent as soon as an awaiter appears. The Recomposer itself awaits frames on
            // this clock (runRecomposeAndApplyChanges below), so pacing here with a time-based
            // delay would add latency to every recomposition and stall entirely on hosts whose
            // clock only advances explicitly (virtual-time test dispatchers, a paused Robolectric
            // looper). Emitted documents are rate limited separately by updateThrottle, between
            // renders rather than between frames.
            val frameClock =
                HeadlessFrameClock(nanoTime = clock::nanoTime) {
                    recomposer.refreshStateIfIdle(recomposerDispatcher)
                }
            if (!isSingleDocument) SnapshotWriteMonitor.acquire()
            try {
                // The pump sends frames requested while nothing is driving the composition to
                // quiescence, e.g. between throttled renders. It is started before the Recomposer
                // so that it is parked by the time the first frame is requested.
                launch(recomposerDispatcher) { frameClock.runClock() }
                launch(recomposerDispatcher + frameClock) {
                    // Queued before the runner's first slice can request a frame, so it runs after
                    // the Recomposer registers its apply observer but before any effect resumes
                    // from withFrameNanos.
                    launch { writeTracker.start() }
                    recomposer.runRecomposeAndApplyChanges()
                }

                // Regeneration must not be driven by observing Recomposer.currentState transitions.
                // currentState is a conflated StateFlow: a collector that is descheduled (e.g.
                // under CPU contention) while the recomposer cycles Idle -> Recomposing -> Idle
                // observes no value change at all, so `filter { it == Idle }` never re-emits and
                // the updated document is silently dropped. Drive regeneration from snapshot apply
                // notifications instead. A CONFLATED channel collapses bursts of writes into a
                // single regeneration but, unlike a StateFlow value slot, can never lose the fact
                // that state changed.
                val invalidations = Channel<Unit>(Channel.CONFLATED)
                // Seed so the initial document is always produced.
                invalidations.trySend(Unit)
                // A single-document capture renders once and never re-renders, so it skips the
                // render invalidation bookkeeping below: no read observer and no apply observer.
                val renderTracker = RenderInvalidationTracker()
                // Observes the snapshot state read while rendering (e.g. inside RemoteCanvas draw
                // lambdas), the way Compose UI's OwnerSnapshotObserver observes layout and draw.
                // Notifications run inline on the applying thread: the callback only flags the
                // tracker and wakes the loop. The callback instance must be stable, as the observer
                // keys its scope maps by it.
                val renderReadObserver =
                    if (isSingleDocument) null else SnapshotStateObserver { it() }
                val onRenderReadChanged: (RenderInvalidationTracker) -> Unit = { tracker ->
                    if (tracker.onRenderReadChanged()) invalidations.trySend(Unit)
                }
                // Registered before the apply observer below, so that when onApplied wakes the loop
                // the read observer has already recorded the same notification.
                renderReadObserver?.start()
                val applyObserverHandle =
                    if (isSingleDocument) null
                    else
                        Snapshot.registerApplyObserver { changed, _ ->
                            if (renderTracker.onApplied(changed)) {
                                invalidations.trySend(Unit)
                            }
                        }

                try {
                    var previousBytes: ByteArray? = null
                    var emittedDocuments = 0
                    var renderedDocuments = 0
                    val rateLimiter = updateThrottle.newLimiter()
                    for (invalidation in invalidations) {
                        // Wait for the throttle before settling and rendering, so writes made
                        // meanwhile are conflated into the invalidation channel and the document
                        // rendered afterwards reflects the latest state. Only emitted documents
                        // count towards the limit.
                        rateLimiter?.awaitPermit()
                        // Let the recomposer observe and apply the invalidation before rendering.
                        // A streaming capture renders an unsettled composition (e.g. continuously
                        // animating content) once the iteration or frame bound is hit, so it keeps
                        // producing documents at the rate allowed by updateThrottle. A single
                        // capture fails instead, as nothing would ever correct the document.
                        val quiescence =
                            traceAsync(
                                "CaptureRemoteDocument:$traceName:$settleTraceName",
                                ThreadLocalRandom.current().nextInt(),
                            ) {
                                awaitRecomposerQuiescence(
                                    recomposer = recomposer,
                                    frameClock = frameClock,
                                    recomposerDispatcher = recomposerDispatcher,
                                    writeTracker = writeTracker,
                                )
                            }
                        if (isSingleDocument) quiescence.throwIfNotQuiescent()

                        val bytes =
                            withContext(recomposerDispatcher) {
                                if (!renderTracker.beginRender(applier.changeCount)) {
                                    return@withContext null
                                }
                                try {
                                    withRenderSnapshot(
                                        writeObserver = { written ->
                                            renderTracker.onRenderWrite(written)
                                        }
                                    ) {
                                        // The first render writes into the document created with
                                        // creationState, which composition left untouched (checked
                                        // below). Later renders need a fresh document.
                                        if (renderedDocuments++ > 0) {
                                            creationState.document =
                                                profile.create(
                                                    creationDisplayInfo.toCreationDisplayInfo(),
                                                    writerCallback,
                                                )
                                            creationState.clearDocumentCaches()
                                        }
                                        val remoteCanvas = RemoteCanvas(creationState)

                                        if (
                                            RemoteComposeCreationComposeFlags
                                                .isEnforceCleanRecompositionEnabled
                                        ) {
                                            check(
                                                creationState.document.buffer.buffer.size() ==
                                                    initialSize
                                            ) {
                                                "Document was written to during composition. " +
                                                    "Expected size $initialSize, got " +
                                                    "${creationState.document.buffer.buffer.size()}"
                                            }
                                        }

                                        val render = {
                                            trace(
                                                "CaptureRemoteDocument:$traceName:rootNodeRender"
                                            ) {
                                                rootNode.render(creationState, remoteCanvas)
                                                remoteCanvas.flush()
                                            }
                                        }
                                        if (renderReadObserver == null) {
                                            render()
                                        } else {
                                            renderReadObserver.observeReads(
                                                renderTracker,
                                                onRenderReadChanged,
                                                render,
                                            )
                                        }

                                        creationState.document.encodeToByteArray()
                                    }
                                } finally {
                                    renderTracker.endRender()
                                }
                            } ?: continue

                        if (previousBytes?.contentEquals(bytes) != true) {
                            previousBytes = bytes
                            rateLimiter?.onEmitted()
                            emit(bytes)
                            if (++emittedDocuments >= maxDocuments) {
                                break
                            }
                        }
                    }
                } finally {
                    applyObserverHandle?.dispose()
                    renderReadObserver?.stop()
                    renderReadObserver?.clear()
                    invalidations.close()
                }
            } finally {
                writeTracker.stop()
                frameClock.cancel()
                recomposer.cancel()
                currentCoroutineContext().cancelChildren()
                if (!isSingleDocument) SnapshotWriteMonitor.release()
            }
        }
    } finally {
        withContext(recomposerDispatcher + NonCancellable) {
            recomposer.cancel()
            // Clear savedState before writing back so that if all registered providers were
            // unregistered during recomposition, stale entries from a previous capture are not left
            // behind in the reused SavedState bundle (SavedStateRegistryController.performSave is a
            // no-op when its saved state is empty).
            savedState?.write { clear() }
            // Perform save before lifecycleOwner.destroy() and composition.dispose() because
            // teardown dispatches ON_DESTROY / RememberObserver.onForgotten(), which unregisters
            // providers from SavedStateRegistry and SaveableStateRegistry.
            if (savedState != null && saveableStateRegistry != null) {
                savedState.saveSaveableState(saveableStateRegistry.performSave())
            }
            lifecycleOwner.performSave()
            lifecycleOwner.destroy()
            composition.dispose()
        }
    }
}

/**
 * Throws an [IllegalStateException] describing why a single-document capture could not settle,
 * unless this is [QuiescenceResult.Quiescent].
 */
private fun QuiescenceResult.throwIfNotQuiescent() {
    when (this) {
        QuiescenceResult.Quiescent -> {}
        QuiescenceResult.Unstable ->
            throw IllegalStateException(
                "captureSingleRemoteDocument did not reach a quiescent Idle state after " +
                    "$MAX_IDLE_ITERATIONS iterations. This is likely caused by unstable " +
                    "recomposition (e.g. a Composable or effect continuously mutating state on " +
                    "every frame or snapshot apply notification)."
            )
        QuiescenceResult.ContinuousFrames ->
            throw IllegalStateException(
                "captureSingleRemoteDocument content requested more than $MAX_FRAMES frames " +
                    "without settling. This is likely caused by unstable recomposition (e.g. an " +
                    "effect mutating state on every frame), an infinite animation or a " +
                    "withFrameNanos loop. Compose animations cannot be captured as they run; use " +
                    "remote expressions to animate remote documents."
            )
        QuiescenceResult.ApplyNotificationsTimedOut ->
            throw IllegalStateException(
                "captureSingleRemoteDocument timed out waiting for another thread to finish " +
                    "notifying snapshot apply observers. A snapshot apply observer elsewhere in " +
                    "the process may be slow or blocked."
            )
    }
}

private const val MAX_IDLE_ITERATIONS = 100

/**
 * `Recomposer.runRecomposeAndApplyChanges` updates `currentState` inside `withFrameNanos` before
 * `NextFrameEndCallbackQueue.markFrameComplete()` drains frame-end callbacks (such as
 * `LocalRetainedValuesStoreProvider`), leaving `currentState` at `PendingWork` even after
 * `hasPendingWork` becomes `false`. Toggling `pauseCompositionFrameClock` /
 * `resumeCompositionFrameClock` re-runs `deriveStateLocked()` once `hasPendingWork` is `false`.
 *
 * TODO(b/567847315): Remove once Compose Runtime re-derives the state after frame-end callbacks.
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
private fun Recomposer.refreshStateIfIdle(recomposerDispatcher: CoroutineContext) {
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
 * again. Every frame sent for this capture (by this loop or by [frameClock]'s pump) is counted by
 * [frameClock], which cancels this wait with [QuiescenceResult.ContinuousFrames] once more than
 * [MAX_FRAMES] frames were sent during this call, wherever it is suspended. A slow composition that
 * sends no frames never trips it.
 *
 * @param frameClock sends and counts every frame for the capture, see [HeadlessFrameClock].
 * @param maxIterations bound on the number of non-quiescent iterations before giving up.
 * @param writeTracker tracks this capture's undelivered global writes, see [CaptureWriteTracker].
 */
private suspend fun awaitRecomposerQuiescence(
    recomposer: Recomposer,
    frameClock: HeadlessFrameClock,
    recomposerDispatcher: CoroutineContext,
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
        frameClock.withFrameBudget(MAX_FRAMES) {
            var idleIterations = 0
            while (idleIterations < maxIterations) {
                recomposer.refreshStateIfIdle()
                recomposer.currentState.first { it == Recomposer.State.Idle }
                if (shouldYield) yield()
                while (frameClock.hasAwaiters) {
                    frameClock.sendFrame()
                    // Without yield() nothing else checks for cancellation by the frame budget.
                    ensureActive()
                    if (shouldYield) yield()
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
private fun <T> withRenderSnapshot(writeObserver: ((Any) -> Unit)? = null, block: () -> T): T {
    val snapshot = Snapshot.takeMutableSnapshot(writeObserver = writeObserver)
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
 * Decides when the [captureRemoteDocument] loop must wake up, and when it must actually re-render
 * and re-encode the document.
 * - Apply notifications consisting only of states written by this session's own render are ignored,
 *   otherwise regeneration would spin. Tracking the exact state objects mutated inside the render
 *   snapshot avoids swallowing global snapshot changes from other threads that happen to be
 *   coalesced during `apply()`.
 * - Any other apply notification wakes the loop so the recomposer is driven to quiescence.
 * - A render is only performed if composition applied changes to the node tree since the previous
 *   render ([RemoteComposeApplier.changeCount] moved), a state read by the previous render changed
 *   (e.g. inside a `RemoteCanvas` draw lambda, which runs at render time), or an external change
 *   landed while a render was in progress. Unrelated global snapshot writes elsewhere in the
 *   process therefore do not cost a full render and encode.
 *
 * The states read by a render are tracked by a [SnapshotStateObserver], the same mechanism Compose
 * UI uses for reads during layout and draw (`OwnerSnapshotObserver`): the render runs inside
 * [SnapshotStateObserver.observeReads] with this tracker as the scope, and the observer calls
 * [onRenderReadChanged] when any of those states is later changed. This tracker only keeps what the
 * observer cannot know: whether a render is in progress, and which states that render wrote.
 *
 * Limits: render inputs that are not snapshot state (e.g. plain fields mutated from an effect) are
 * not observed. Previously any unrelated snapshot write forced a re-render and would incidentally
 * pick such changes up; now they are only rendered alongside the next tree change or render-read
 * state change.
 *
 * Thread-safe: [onApplied] and [onRenderReadChanged] run on whichever thread sends apply
 * notifications.
 */
private class RenderInvalidationTracker {
    private val lock = Any()
    private val renderWrites = newIdentitySet()
    private var isRendering = false
    private var renderInputsChanged = true
    private var lastRenderChangeCount = 0L

    /** Apply observer hook. Returns `true` if the capture loop should wake up. */
    fun onApplied(changed: Set<Any>): Boolean =
        synchronized(lock) {
            val hasExternalModification =
                renderWrites.isEmpty() || changed.any { it !in renderWrites }
            // A change that lands while a render is in progress may have been missed by both that
            // render's snapshot and the observer's (just cleared) read set, so render again.
            if (hasExternalModification && isRendering) renderInputsChanged = true
            hasExternalModification
        }

    /**
     * [SnapshotStateObserver] hook: a state read by the previous render changed. Returns `true` if
     * the capture loop should wake up.
     *
     * Ignored while rendering: the notification is then either for the render's own writes to a
     * state it also read (which must not trigger another render, or regeneration would spin) or for
     * an external change, which [onApplied] accounts for. Both hooks are apply observers invoked
     * for the same notification, in registration order, and each wakes the loop when it detects a
     * change, so their relative order cannot lose a change: at worst the later one causes a
     * redundant render.
     */
    fun onRenderReadChanged(): Boolean =
        synchronized(lock) {
            if (isRendering) return false
            renderInputsChanged = true
            true
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

    /** Ends a render started by [beginRender]. */
    fun endRender() {
        synchronized(lock) {
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
private class HeadlessLifecycleOwner(private val savedState: SavedState? = null) :
    LifecycleOwner, SavedStateRegistryOwner {
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
    @Volatile private var state = Lifecycle.State.INITIALIZED
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

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(savedState)
        synchronized(lock) {
            for (event in
                arrayOf(
                    Lifecycle.Event.ON_CREATE,
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_RESUME,
                )) {
                state = event.targetState
                for (registration in registrations.toTypedArray()) {
                    if (registration.active) registration.dispatch(this, event)
                }
            }
        }
    }

    fun performSave() {
        val outState = savedState ?: return
        savedStateRegistryController.performSave(outState)
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
