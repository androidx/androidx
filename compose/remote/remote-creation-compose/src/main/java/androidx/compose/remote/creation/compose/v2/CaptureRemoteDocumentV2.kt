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

@file:OptIn(ExperimentalCoroutinesApi::class)

package androidx.compose.remote.creation.compose.v2

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.widgets.toLayoutDirection
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
import androidx.core.graphics.createBitmap
import kotlin.coroutines.CoroutineContext
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
import kotlinx.coroutines.yield

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun captureSingleRemoteDocumentV2(
    creationDisplayInfo: CreationDisplayInfo,
    layoutDirection: LayoutDirection? = null,
    context: Context,
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerEvents: WriterEvents = WriterEvents(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
    content: @Composable () -> Unit,
): CapturedDocument {
    val document =
        captureRemoteDocumentV2(
                creationDisplayInfo = creationDisplayInfo,
                layoutDirection = layoutDirection,
                clock = clock,
                writerEvents = writerEvents,
                profile = profile,
                coroutineContext = coroutineContext,
                context = context,
                content = content,
            )
            .first()
    return CapturedDocument(document, writerEvents.pendingIntents)
}

/**
 * Captures a RemoteCompose document using the V2 implementation. Emits a new [ByteArray] every time
 * the composition changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun captureRemoteDocumentV2(
    creationDisplayInfo: CreationDisplayInfo,
    layoutDirection: LayoutDirection? = null,
    writerEvents: WriterEvents,
    context: Context,
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    content: @Composable () -> Unit,
): Flow<ByteArray> = flow {
    val rootNode = RemoteRootNodeV2()
    val applier = RemoteComposeApplierV2(rootNode)

    val recomposer = Recomposer(currentCoroutineContext())
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
            )

        composition.setContent {
            CompositionLocalProvider(
                LocalRemoteComposeCreationState provides creationState,
                LocalDensity provides Density(creationDisplayInfo.density),
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalLayoutDirection provides layoutDirection,
                content = content,
            )
        }

        val frameClock = BroadcastFrameClock()
        coroutineScope {
            launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }

            // Make sure runRecomposeAndApplyChanges will pick this up
            yield()
            frameClock.sendFrame(clock.nanoTime())

            // Launch a collector for recomposer state to trigger renders
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

                            rootNode.render(creationState, remoteCanvas)

                            // This is only safe for the first document
                            // since some ids might be generated early
                            creationState.document.encodeToByteArray()
                        }
                    }
                    // Avoid additional takes until id generation is cleaned up
                    .take(1)
            emitAll(documentFlow)
        }
    } finally {
        composition.dispose()
        recomposer.cancel()
    }
}
