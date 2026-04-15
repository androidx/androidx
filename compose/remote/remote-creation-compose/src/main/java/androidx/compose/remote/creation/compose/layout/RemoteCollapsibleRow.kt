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
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.CollapsiblePriorityModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.WidthModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCollapsibleRowScope {
    public fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(WidthModifier(Type.WEIGHT, weight))

    public fun RemoteModifier.weight(weight: Float): RemoteModifier =
        then(WidthModifier(Type.WEIGHT, RemoteFloat(weight)))

    public fun RemoteModifier.priority(priority: Float): RemoteModifier =
        then(CollapsiblePriorityModifier(CollapsiblePriority.HORIZONTAL, RemoteFloat(priority)))
}

internal class RemoteCollapsibleRowNode : RemoteComposeNode() {
    var horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start
    var verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top
    var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        (horizontalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.space.getFloatIdForCreationState(creationState))
        }
        creationState.document.startCollapsibleRow(
            recordingModifier,
            horizontalArrangement.toRemote(layoutDirection),
            verticalAlignment.toRemote(),
        )
        renderChildren(
            creationState,
            remoteCanvas,
            reversed = shouldReverse(horizontalArrangement, layoutDirection),
        )
        creationState.document.endCollapsibleRow()
    }
}

/**
 * RemoteRow implements a row layout, delegating to the foundation Row layout as needed. This allows
 * RemoteRow to both work as a normal Row when called within a normal Compose tree, and capture the
 * layout information when called within a capture pass for RemoteCompose.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteCollapsibleRow(
    modifier: RemoteModifier = RemoteModifier,
    horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top,
    content: @Composable RemoteCollapsibleRowScope.() -> Unit,
) {
    val scope = remember { RemoteCollapsibleRowScope() }
    val layoutDirection = LocalLayoutDirection.current
    RemoteComposeNode(
        factory = ::RemoteCollapsibleRowNode,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(horizontalArrangement) { nodeHorizontalArrangement ->
                this.horizontalArrangement = nodeHorizontalArrangement
            }
            set(verticalAlignment) { nodeVerticalAlignment ->
                this.verticalAlignment = nodeVerticalAlignment
            }
            set(layoutDirection) { nodeLayoutDirection ->
                this.layoutDirection = nodeLayoutDirection
            }
        },
        content = { scope.content() },
    )
}
