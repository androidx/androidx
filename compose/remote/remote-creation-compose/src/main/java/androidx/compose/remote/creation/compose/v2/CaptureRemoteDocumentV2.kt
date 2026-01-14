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

package androidx.compose.remote.creation.compose.v2

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Captures a RemoteCompose document using the V2 implementation. Emits a new [ByteArray] every time
 * the composition changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun captureRemoteDocumentV2(
    creationDisplayInfo: CreationDisplayInfo,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    context: CoroutineContext = Dispatchers.Main,
    content: @Composable () -> Unit,
): Flow<ByteArray> = callbackFlow {
    val rootNode = RemoteRootNodeV2()
    val applier = RemoteComposeApplierV2(rootNode)

    val launchContext = context.minusKey(kotlinx.coroutines.Job)
    val recomposer = Recomposer(launchContext)
    val composition = Composition(applier, recomposer)

    val writerEvents = WriterEvents()

    val creationState = RemoteComposeCreationState(creationDisplayInfo, profile, writerEvents)

    composition.setContent {
        CompositionLocalProvider(
            LocalRemoteComposeCreationState provides creationState,
            content = content,
        )
    }

    launch(launchContext) { recomposer.runRecomposeAndApplyChanges() }

    // Launch a collector for recomposer state to trigger renders
    launch(launchContext) {
        recomposer.currentState.collect { state ->
            if (state == Recomposer.State.Idle) {
                Snapshot.withMutableSnapshot {
                    // Create a fresh writer for each emission to ensure a complete document is
                    // captured
                    val writer = profile.create(creationDisplayInfo, writerEvents)
                    creationState.document = writer

                    rootNode.render(creationState)
                    trySend(writer.encodeToByteArray())
                }
            }
        }
    }

    awaitClose {
        composition.dispose()
        recomposer.cancel()
    }
}
