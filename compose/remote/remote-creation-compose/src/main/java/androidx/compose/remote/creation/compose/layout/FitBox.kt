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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

internal class RemoteFitBoxNode : RemoteComposeNode() {
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top
    var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startFitBox(
            recordingModifier,
            horizontalAlignment.toRemote(layoutDirection),
            verticalArrangement.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endFitBox()
    }
}

/**
 * FitBox implements a Box layout, delegating to the foundation Box layout as needed. This allows
 * FitBox to both work as a normal Box when called within a normal Compose tree, and capture the
 * layout information when called within a capture pass for RemoteCompose.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun FitBox(
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.CenterHorizontally,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Center,
    content: @RemoteComposable @Composable () -> Unit = {},
) {
    val layoutDirection = LocalLayoutDirection.current
    RemoteComposeNode(
        factory = ::RemoteFitBoxNode,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(horizontalAlignment) { nodeHorizontalAlignment ->
                this.horizontalAlignment = nodeHorizontalAlignment
            }
            set(verticalArrangement) { nodeVerticalArrangement ->
                this.verticalArrangement = nodeVerticalArrangement
            }
            set(layoutDirection) { nodeLayoutDirection ->
                this.layoutDirection = nodeLayoutDirection
            }
        },
        content = content,
    )
}
