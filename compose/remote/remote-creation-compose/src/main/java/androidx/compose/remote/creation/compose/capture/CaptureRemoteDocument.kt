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

@file:OptIn(ExperimentalRemoteCreationComposeApi::class)
@file:JvmName("CaptureRemoteDocumentKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.capture.headless.CaptureWriteTracker
import androidx.compose.remote.creation.compose.capture.headless.HeadlessLifecycleOwner
import androidx.compose.remote.creation.compose.capture.headless.HeadlessSession
import androidx.compose.remote.creation.compose.capture.headless.MAX_FRAMES
import androidx.compose.remote.creation.compose.capture.headless.MAX_IDLE_ITERATIONS
import androidx.compose.remote.creation.compose.capture.headless.QuiescenceResult
import androidx.compose.remote.creation.compose.capture.headless.SnapshotWriteMonitor
import androidx.compose.remote.creation.compose.capture.headless.awaitRecomposerQuiescence
import androidx.compose.remote.creation.compose.capture.headless.renderLoop
import androidx.compose.remote.creation.compose.capture.headless.toSingleThreadedRecomposerContext
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteComposeApplier
import androidx.compose.remote.creation.compose.layout.RemoteRootNode
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.retain.LocalRetainedValuesStoreProvider
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.trace
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.savedstate.SavedState
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.write
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.AbstractLongTimeSource
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

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

    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    // One time source for pacing frames and throttling documents, so a test clock drives both.
    val timeSource = RemoteClockTimeSource(clock)
    val session =
        HeadlessSession(
            parentContext = currentCoroutineContext() + coroutineContext.minusKey(Job),
            name = traceName,
            nanoTime = clock::nanoTime,
            timeSource = timeSource,
        )
    val recomposerDispatcher = session.recomposerDispatcher
    val recomposer = session.recomposer
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

        coroutineScope {
            // Frames are normally sent as soon as an awaiter appears. The Recomposer itself awaits
            // frames on this clock (started below), so pacing them with a time-based delay would
            // add latency to every recomposition and stall entirely on hosts whose clock only
            // advances explicitly (virtual-time test dispatchers, a paused Robolectric looper).
            // Emitted documents are rate limited separately by updateThrottle, between renders
            // rather than between frames; only while that throttle is holding documents back are
            // the pump's frames paced too (see the loop).
            val frameClock = session.frameClock
            val writeMonitor = if (isSingleDocument) null else SnapshotWriteMonitor.acquire()
            try {
                session.start(this)
                // Set the content only once the Recomposer is running. A Recomposer invalidates
                // every composition it knows about when it starts, since it was not observing
                // snapshot state on their behalf until then; content set before that point is
                // therefore composed a second time before the first document is rendered. The
                // effects started by the initial composition then also run under the started
                // write tracker and after the session's invalidation observer is registered.
                session.awaitRunning()
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

                var previousBytes: ByteArray? = null
                var emittedDocuments = 0
                var renderedDocuments = 0
                val rateLimiter = updateThrottle.newLimiter(timeSource)
                // A single-document capture renders once and never re-renders, so it skips the
                // render read observation.
                session.renderLoop(
                    observeRenderReads = !isSingleDocument,
                    changeCount = { applier.changeCount },
                    beforeSettle = {
                        // Wait for the throttle before settling and rendering, so writes made
                        // meanwhile are conflated into the invalidation and the document rendered
                        // afterwards reflects the latest state. Only emitted documents count
                        // towards the limit.
                        //
                        // While waiting for a permit, also pace the frames the clock's pump sends,
                        // so that content animating through recomposition does not keep the
                        // recomposer spinning at full speed for documents that cannot be emitted
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
                    // A streaming capture renders an unsettled composition (e.g. continuously
                    // animating content) once the iteration or frame bound is hit, so it keeps
                    // producing documents at the rate allowed by updateThrottle. A single capture
                    // fails instead, as nothing would ever correct the document.
                    if (isSingleDocument) quiescence.throwIfNotQuiescent()

                    val bytes =
                        // Writes during render fail under enforced clean recomposition (required
                        // for streaming); a legacy single-shot capture still applies them.
                        renderIfChanged(
                            applyWrites =
                                !RemoteComposeCreationComposeFlags
                                    .isEnforceCleanRecompositionEnabled
                        ) {
                            // The first render writes into the document created with
                            // creationState, which composition left untouched (checked below).
                            // Later renders need a fresh document.
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
                                RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled
                            ) {
                                check(creationState.document.buffer.buffer.size() == initialSize) {
                                    "Document was written to during composition. " +
                                        "Expected size $initialSize, got " +
                                        "${creationState.document.buffer.buffer.size()}"
                                }
                            }

                            trace("CaptureRemoteDocument:$traceName:rootNodeRender") {
                                rootNode.render(creationState, remoteCanvas)
                                remoteCanvas.flush()
                            }

                            creationState.document.encodeToByteArray()
                        } ?: return@renderLoop true

                    if (previousBytes?.contentEquals(bytes) != true) {
                        previousBytes = bytes
                        rateLimiter?.onEmitted()
                        emit(bytes)
                        if (++emittedDocuments >= maxDocuments) {
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
 * Measures throttle intervals and frame pacing with the capture's [RemoteClock], the clock that
 * also stamps its frames, so that a test clock drives frames, pacing and throttling alike.
 */
private class RemoteClockTimeSource(private val clock: RemoteClock) :
    AbstractLongTimeSource(DurationUnit.NANOSECONDS) {
    override fun read(): Long = clock.nanoTime()
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
