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

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @Stable public class RemoteBoxScope

internal class RemoteBoxNode : RemoteComposeNode() {
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start
    var verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top
    var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startBox(
            recordingModifier,
            horizontalAlignment.toRemote(layoutDirection),
            verticalAlignment.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endBox()
    }
}

/**
 * A layout composable that positions its children relative to its own edges.
 *
 * `RemoteBox` allows you to wrap multiple children and position them using [contentAlignment]. In
 * Remote Compose, this layout is recorded as a Box command.
 *
 * @param modifier The modifier to be applied to this box.
 * @param contentAlignment The default alignment inside the Box.
 * @param content The content of the box.
 */
@RemoteComposable
@Composable
public fun RemoteBox(
    modifier: RemoteModifier = RemoteModifier,
    contentAlignment: RemoteAlignment = RemoteAlignment.TopStart,
    content: @Composable () -> Unit,
) {
    val scope = remember { RemoteBoxScope() }
    val layoutDirection = LocalLayoutDirection.current
    RemoteComposeNode(
        factory = ::RemoteBoxNode,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(contentAlignment.horizontal) { nodeHorizontalAlignment ->
                this.horizontalAlignment = nodeHorizontalAlignment
            }
            set(contentAlignment.vertical) { nodeVerticalAlignment ->
                this.verticalAlignment = nodeVerticalAlignment
            }
            set(layoutDirection) { nodeLayoutDirection ->
                this.layoutDirection = nodeLayoutDirection
            }
        },
        content = content,
    )
}

/**
 * A version of [RemoteBox] with no content, often used as a spacer or a background placeholder.
 *
 * @param modifier The modifier to be applied to this box.
 */
@RemoteComposable
@Composable
public fun RemoteBox(modifier: RemoteModifier = RemoteModifier) {
    RemoteComposeNode(
        factory = ::RemoteBoxNode,
        update = { set(modifier) { nodeModifier -> this.modifier = nodeModifier } },
    )
}
