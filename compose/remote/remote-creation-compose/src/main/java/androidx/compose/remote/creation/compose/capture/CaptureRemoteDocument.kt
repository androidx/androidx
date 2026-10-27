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
import androidx.compose.remote.creation.compose.capture.headless.MAX_FRAMES
import androidx.compose.remote.creation.compose.capture.headless.MAX_IDLE_ITERATIONS
import androidx.compose.remote.creation.compose.capture.headless.QuiescenceResult
import androidx.compose.remote.creation.compose.capture.headless.headlessRenderFlow
import androidx.compose.remote.creation.compose.capture.headless.toSingleThreadedRecomposerContext
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteComposeApplier
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.layout.RemoteRootNode
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tracing.Trace
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.AbstractLongTimeSource
import kotlin.time.DurationUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

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
    layoutDirection: RemoteLayoutDirection = RemoteLayoutDirection.Ltr,
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile,
    writerCallback: Any? = null,
    platformImageProvider: PlatformImageProvider = NoOpPlatformImageProvider,
    onDispose: () -> Unit = {},
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
            onDispose = onDispose,
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
    layoutDirection: RemoteLayoutDirection = RemoteLayoutDirection.Ltr,
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile,
    writerCallback: Any? = null,
    platformImageProvider: PlatformImageProvider = NoOpPlatformImageProvider,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    onDispose: () -> Unit = {},
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
        onDispose = onDispose,
        content = content,
    )

/**
 * Shared implementation of [captureSingleRemoteDocument] and [captureRemoteDocument].
 *
 * The headless composition lifecycle (session, settling, re-rendering on change, throttling,
 * skipping unchanged output, teardown) is [headlessRenderFlow]'s, which is not specific to
 * RemoteCompose. What is specific lives here: content emits [RemoteRootNode]s through a
 * [RemoteComposeApplier] under the RemoteCompose `CompositionLocal`s, and each render writes the
 * node tree into a fresh document and encodes it.
 *
 * The flow completes after [maxDocuments] documents have been emitted. A single-document capture
 * (`maxDocuments == 1`) also fails instead of rendering when the composition does not settle, since
 * there will be no later document to correct an unsettled one, and may run without
 * [RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled].
 */
private fun captureRemoteDocumentImpl(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    updateThrottle: CaptureUpdateThrottle,
    maxDocuments: Int,
    remoteDensity: RemoteDensity,
    layoutDirection: RemoteLayoutDirection,
    clock: RemoteClock,
    profile: Profile,
    writerCallback: Any?,
    platformImageProvider: PlatformImageProvider,
    coroutineContext: CoroutineContext,
    onDispose: () -> Unit,
    content: @Composable @RemoteComposable () -> Unit,
): Flow<ByteArray> = flow {
    require(maxDocuments > 0) { "maxDocuments must be positive, was $maxDocuments" }
    val isSingleDocument = maxDocuments == 1
    val enforceCleanRecomposition =
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled
    require(isSingleDocument || enforceCleanRecomposition) {
        "captureRemoteDocument requires isEnforceCleanRecompositionEnabled to be true"
    }
    val traceName = if (isSingleDocument) "captureSingleRemoteDocument" else "captureRemoteDocument"

    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)
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

    emitAll(
        headlessRenderFlow(
            name = traceName,
            applier = applier,
            changeCount = { applier.changeCount },
            maxOutputs = maxDocuments,
            coroutineContext = coroutineContext,
            // One time source for pacing frames and throttling documents, so a test clock drives
            // both.
            timeSource = RemoteClockTimeSource(clock),
            nanoTime = clock::nanoTime,
            newRateLimiter = { timeSource ->
                updateThrottle.newLimiter(timeSource, profile.platform)
            },
            // Writes during render fail under enforced clean recomposition (required for
            // streaming); a legacy single-shot capture still applies them.
            applyRenderWrites = !enforceCleanRecomposition,
            onNotQuiescent = { if (isSingleDocument) it.throwIfNotQuiescent() },
            isSameOutput = ByteArray::contentEquals,
            onDispose = onDispose,
            content = {
                CompositionLocalProvider(
                    LocalRemoteComposeCreationState provides creationState,
                    LocalRemoteDensity provides remoteDensity,
                    LocalRemoteLayoutDirection provides layoutDirection,
                    content = content,
                )
            },
        ) { renderIndex ->
            // The first render writes into the document created with creationState, which
            // composition left untouched (checked below). Later renders need a fresh document.
            if (renderIndex > 0) {
                creationState.document =
                    profile.create(creationDisplayInfo.toCreationDisplayInfo(), writerCallback)
                creationState.clearDocumentCaches()
            }
            val remoteCanvas = RemoteCanvas(creationState)

            if (enforceCleanRecomposition) {
                check(creationState.document.buffer.buffer.size() == initialSize) {
                    "Document was written to during composition. Expected size $initialSize, " +
                        "got ${creationState.document.buffer.buffer.size()}"
                }
            }

            Trace.beginSection("CaptureRemoteDocument:$traceName:rootNodeRender")
            try {
                rootNode.render(creationState, remoteCanvas)
                remoteCanvas.flush()
            } finally {
                Trace.endSection()
            }

            creationState.document.encodeToByteArray()
        }
    )
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
