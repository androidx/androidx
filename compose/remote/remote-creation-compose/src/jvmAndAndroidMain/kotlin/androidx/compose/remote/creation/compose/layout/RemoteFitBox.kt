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
import androidx.compose.remote.creation.compose.modifier.prepareModifiers
import androidx.compose.runtime.Composable

internal class RemoteFitBoxNode : RemoteLayoutNode() {
    var contentAlignment: RemoteAlignment = RemoteAlignment.Center

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val scope = overriddenScope(remoteCanvas)
        val writeModifiers = scope.prepareModifiers(modifier)
        creationState.document.startFitBox(
            contentAlignment.horizontal.toRemote(layoutDirection),
            contentAlignment.vertical.toRemote(),
        ) {
            writeModifiers(creationState.document)
        }
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
 * Interactive example switching container constraints so `RemoteFitBox` selects the fitting child:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteFitBoxSample
 * @param modifier modifier applied to this layout
 * @param contentAlignment default alignment inside the layout
 * @param content composable children to lay out inside this `RemoteFitBox`, which have access to
 *   [RemoteBoxScope]
 */
@RemoteComposable
@Composable
public fun RemoteFitBox(
    modifier: RemoteModifier = RemoteModifier,
    contentAlignment: RemoteAlignment = RemoteAlignment.Center,
    content: @RemoteComposable @Composable RemoteBoxScope.() -> Unit,
) {
    RemoteLayoutNode(
        factory = ::RemoteFitBoxNode,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(contentAlignment) { nodeContentAlignment ->
                this.contentAlignment = nodeContentAlignment
            }
        },
        content = { RemoteBoxScopeInstance.content() },
    )
}
