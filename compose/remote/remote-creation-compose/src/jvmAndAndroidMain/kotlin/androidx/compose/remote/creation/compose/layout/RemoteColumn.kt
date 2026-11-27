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

import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.HeightModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.prepareModifiers
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.document.mvf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/** Receiver scope used by [RemoteColumn] for its content. */
@RemoteLayoutScopeMarker
@Immutable
public sealed interface RemoteColumnScope {
    /**
     * Sets the vertical weight of this element relative to its siblings in the [RemoteColumn].
     *
     * Example of distributing height proportionally across children using `weight`:
     *
     * @sample androidx.compose.remote.creation.compose.samples.RemoteColumnWeightSample
     * @param weight proportional height to allocate to this element
     * @return a [RemoteModifier] that allocates height proportional to `weight`
     */
    @Stable
    public fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, weight))
}

internal object RemoteColumnScopeInstance : RemoteColumnScope

internal class RemoteColumnNode : RemoteLayoutNode() {
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val scope = overriddenScope(remoteCanvas)
        val writeModifiers = scope.prepareModifiers(modifier)
        val spacedBy = (verticalArrangement as? RemoteSpaced)?.getSpacing(scope) ?: 0f.mvf
        creationState.document.startColumn(
            horizontalAlignment.toRemote(layoutDirection),
            verticalArrangement.toRemote(),
            spacedBy,
            componentId = writeModifiers.componentId,
        ) {
            writeModifiers(creationState.document)
        }
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
 * Example of arranging children vertically with spacing:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteColumnSample
 *
 * Example of distributing vertical space using [RemoteColumnScope.weight]:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteColumnWeightSample
 * @param modifier modifier to be applied to this column
 * @param verticalArrangement vertical arrangement of the children
 * @param horizontalAlignment horizontal alignment of the children
 * @param content content of the column, which has access to [RemoteColumnScope]
 */
@RemoteComposable
@Composable
public fun RemoteColumn(
    modifier: RemoteModifier = RemoteModifier,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    content: @RemoteComposable @Composable RemoteColumnScope.() -> Unit,
) {
    RemoteLayoutNode(
        factory = ::RemoteColumnNode,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(verticalArrangement) { nodeVerticalArrangement ->
                this.verticalArrangement = nodeVerticalArrangement
            }
            set(horizontalAlignment) { nodeHorizontalAlignment ->
                this.horizontalAlignment = nodeHorizontalAlignment
            }
        },
        content = { RemoteColumnScopeInstance.content() },
    )
}
