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

package androidx.compose.remote.creation.compose.layout

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
        val scope = overriddenScope(creationState)
        val recordingModifier = scope.toRecordingModifier(modifier)
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
 * Layout container that renders the first child that fits within available constraints.
 *
 * Functions as a [RemoteBox] and acts as an alternative-selector container during playback.
 *
 * Selection behavior:
 * - Measures children sequentially and renders only the first child whose width and height fit
 *   within available constraints.
 * - Matches container dimensions to the measured size of the selected child.
 * - Hides completely (`GONE`) if no child fits within the constraints.
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteFitBoxSample
 * @param modifier modifier applied to this layout
 * @param horizontalAlignment horizontal alignment for children
 * @param verticalArrangement vertical arrangement for children
 * @param content composable children to lay out inside this [RemoteFitBox]
 */
@RemoteComposable
@Composable
public fun RemoteFitBox(
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
