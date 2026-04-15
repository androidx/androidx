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
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.HeightModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Receiver scope used by [RemoteColumn] for its content. */
public class RemoteColumnScope {
    /**
     * Sets the vertical weight of this element relative to its siblings in the [RemoteColumn].
     *
     * @param weight The proportional height to allocate to this element.
     */
    public fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, weight))

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun RemoteModifier.weight(weight: Float): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, RemoteFloat(weight)))
}

internal class RemoteColumnNode : RemoteComposeNode() {
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start
    var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        (verticalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.space.getFloatIdForCreationState(creationState))
        }
        creationState.document.startColumn(
            recordingModifier,
            horizontalAlignment.toRemote(layoutDirection),
            verticalArrangement.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endColumn()
    }
}

/**
 * A layout composable that positions its children in a vertical sequence.
 *
 * `RemoteColumn` allows you to arrange children vertically and control their [verticalArrangement]
 * (spacing) and [horizontalAlignment].
 *
 * @param modifier The modifier to be applied to this column.
 * @param verticalArrangement The vertical arrangement of the children.
 * @param horizontalAlignment The horizontal alignment of the children.
 * @param content The content of the column, which has access to [RemoteColumnScope].
 */
@RemoteComposable
@Composable
public fun RemoteColumn(
    modifier: RemoteModifier = RemoteModifier,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    content: @Composable RemoteColumnScope.() -> Unit,
) {
    val scope = remember { RemoteColumnScope() }
    val layoutDirection = LocalLayoutDirection.current
    RemoteComposeNode(
        factory = ::RemoteColumnNode,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(verticalArrangement) { nodeVerticalArrangement ->
                this.verticalArrangement = nodeVerticalArrangement
            }
            set(horizontalAlignment) { nodeHorizontalAlignment ->
                this.horizontalAlignment = nodeHorizontalAlignment
            }
            set(layoutDirection) { nodeLayoutDirection ->
                this.layoutDirection = nodeLayoutDirection
            }
        },
        content = { scope.content() },
    )
}
