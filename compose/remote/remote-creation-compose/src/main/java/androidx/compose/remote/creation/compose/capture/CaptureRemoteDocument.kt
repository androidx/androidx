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
    androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi::class,
)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.CreationDisplayInfo
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
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.trace
import androidx.core.graphics.createBitmap
import androidx.tracing.traceAsync
import java.util.concurrent.ThreadLocalRandom
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * Capture a RemoteCompose document by rendering the specified [content] Composable in a virtual
 * display and returning the resulting bytes.
 *
 * This can be used for testing, or for generating documents on the fly to be sent to a remote
 * client.
 *
 * This API is experimental and is likely to change in the future before becoming API stable.
 *
 * @param context the Android [Context] to use for the capture.
 * @param creationDisplayInfo details about the virtual display to create.
 * @param profile the [Profile] to use for the capture, determining which operations are supported.
 * @param content the Composable content to render and capture.
 * @return a [ByteArray] containing the RemoteCompose document.
 */
public suspend fun captureSingleRemoteDocument(
    context: Context,
    creationDisplayInfo: CreationDisplayInfo =
        createCreationDisplayInfo(context).toCreationDisplayInfo(),
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    content: @Composable @RemoteComposable () -> Unit,
): CapturedDocument {
    val layoutDirection = toLayoutDirection(context.resources.configuration.layoutDirection)

    val remoteCreationDisplayInfo =
        creationDisplayInfo.toRemote(context.resources.configuration.fontScale)
    val remoteDensity =
        RemoteDensity(creationDisplayInfo.density.rf, context.resources.configuration.fontScale.rf)

    return traceAsync(
        "CaptureRemoteDocument:captureSingleRemoteDocument",
        ThreadLocalRandom.current().nextInt(),
    ) {
        captureSingleRemoteDocument(
            creationDisplayInfo = remoteCreationDisplayInfo,
            remoteDensity = remoteDensity,
            layoutDirection = layoutDirection,
            profile = profile,
            content = content,
            context = context,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    content: @Composable () -> Unit,
): CapturedDocument {
    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    val recomposerContext = currentCoroutineContext()
    val recomposer = Recomposer(recomposerContext)
    val composition = Composition(applier, recomposer)

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

        composition.setContent {
            CompositionLocalProvider(
                LocalRemoteComposeCreationState provides creationState,
                LocalDensity provides
                    Density(
                        creationDisplayInfo.density.density,
                        creationDisplayInfo.density.fontScale,
                    ),
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalLayoutDirection provides layoutDirection,
                content = content,
            )
        }

        coroutineScope {
            try {
                traceAsync(
                    "CaptureRemoteDocument:captureSingleRemoteDocument:compositionInitialization",
                    ThreadLocalRandom.current().nextInt(),
                ) {
                    lateinit var frameClock: BroadcastFrameClock
                    frameClock = BroadcastFrameClock {
                        launch(recomposerContext) { frameClock.sendFrame(clock.nanoTime()) }
                    }
                    launch(recomposerContext + frameClock) {
                        recomposer.runRecomposeAndApplyChanges()
                    }

                    recomposer.currentState.filter { it == Recomposer.State.Idle }.first()
                }
            } finally {
                recomposer.cancel()
            }
        }

        val document =
            Snapshot.withMutableSnapshot {
                val recordingCanvas =
                    RecordingCanvas(createBitmap(1, 1)).apply {
                        setRemoteComposeCreationState(creationState)
                    }

                val remoteCanvas = RemoteCanvas(recordingCanvas)

                if (RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled) {
                    check(creationState.document.buffer.buffer.size() == initialSize) {
                        "Document was written to during composition. Expected size $initialSize, got ${creationState.document.buffer.buffer.size()}"
                    }
                }

                trace("CaptureRemoteDocument:captureSingleRemoteDocument:rootNodeRender") {
                    rootNode.render(creationState, remoteCanvas)
                }

                creationState.document.encodeToByteArray()
            }

        return CapturedDocument(document, writerEvents.pendingIntents)
    } finally {
        composition.dispose()
        recomposer.cancel()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun captureRemoteDocument(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection? = null,
    writerEvents: WriterEvents,
    context: Context,
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    content: @Composable () -> Unit,
): Flow<ByteArray> = flow {
    require(RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled) {
        "captureRemoteDocument requires isEnforceCleanRecompositionEnabled to be true"
    }
    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    val limitedCoroutineContext =
        if (coroutineContext is CoroutineDispatcher) {
            coroutineContext.limitedParallelism(parallelism = 1, name = "captureRemoteDocument")
        } else {
            coroutineContext
        }

    val recomposerContext = currentCoroutineContext() + limitedCoroutineContext
    val recomposer = Recomposer(recomposerContext)
    val composition = Composition(applier, recomposer)

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

        composition.setContent {
            CompositionLocalProvider(
                LocalRemoteComposeCreationState provides creationState,
                LocalDensity provides
                    Density(
                        creationDisplayInfo.density.density,
                        creationDisplayInfo.density.fontScale,
                    ),
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalLayoutDirection provides layoutDirection,
                content = content,
            )
        }

        coroutineScope {
            lateinit var frameClock: BroadcastFrameClock
            frameClock = BroadcastFrameClock {
                launch(recomposerContext) { frameClock.sendFrame(clock.nanoTime()) }
            }

            launch(recomposerContext + frameClock) { recomposer.runRecomposeAndApplyChanges() }

            val documentFlow =
                recomposer.currentState
                    .filter { it == Recomposer.State.Idle }
                    .mapLatest {
                        Snapshot.withMutableSnapshot {
                            val recordingCanvas =
                                RecordingCanvas(createBitmap(1, 1)).apply {
                                    setRemoteComposeCreationState(creationState)
                                }

                            val remoteCanvas = RemoteCanvas(recordingCanvas)

                            check(creationState.document.buffer.buffer.size() == initialSize) {
                                "Document was written to during composition. Expected size $initialSize, got ${creationState.document.buffer.buffer.size()}"
                            }

                            rootNode.render(creationState, remoteCanvas)

                            creationState.document.encodeToByteArray()
                        }
                    }
                    .take(1)
            emitAll(documentFlow)
        }
    } finally {
        composition.dispose()
        recomposer.cancel()
    }
}

private fun CreationDisplayInfo.toRemote(fontScale: Float): RemoteCreationDisplayInfo =
    RemoteCreationDisplayInfo(
        width = this.width,
        height = this.height,
        densityDpi = this.densityDpi,
        fontScale = fontScale,
    )
