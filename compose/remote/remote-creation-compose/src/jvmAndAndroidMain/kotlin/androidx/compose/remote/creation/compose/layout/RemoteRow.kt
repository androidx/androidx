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
import androidx.compose.remote.creation.compose.modifier.AlignByBaselineModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.WidthModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/** Receiver scope used by [RemoteRow] for its content. */
@RemoteLayoutScopeMarker
@Immutable
public sealed interface RemoteRowScope {
    /**
     * Sets the horizontal weight of this element relative to its siblings in the [RemoteRow].
     *
     * Example of distributing width proportionally across children using `weight`:
     *
     * @sample androidx.compose.remote.creation.compose.samples.RemoteRowWeightSample
     * @param weight proportional width to allocate to this element
     * @return a [RemoteModifier] that allocates width proportional to `weight`
     */
    @Stable
    public fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(WidthModifier(Type.WEIGHT, weight))

    /**
     * Aligns this layout with sibling layouts in a [RemoteRow] by their shared first baseline.
     *
     * @sample androidx.compose.remote.creation.compose.samples.RemoteRowAlignByBaselineSample
     * @return a [RemoteModifier] that aligns the layout by its first baseline
     */
    @Stable
    public fun RemoteModifier.alignByBaseline(): RemoteModifier = then(AlignByBaselineModifier)
}

internal object RemoteRowScopeInstance : RemoteRowScope

internal class RemoteRowNode : RemoteLayoutNode() {
    var horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start
    var verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val scope = overriddenScope(remoteCanvas)
        val recordingModifier = scope.toRecordingModifier(modifier)
        (horizontalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.getSpacingFloatId(scope))
        }
        creationState.document.startRow(
            recordingModifier,
            horizontalArrangement.toRemote(layoutDirection),
            verticalAlignment.toRemote(),
        )
        renderChildren(
            creationState,
            remoteCanvas,
            reversed = shouldReverse(horizontalArrangement, layoutDirection),
        )
        creationState.document.endRow()
    }
}

/**
 * A layout composable that positions its children in a horizontal sequence.
 *
 * `RemoteRow` allows you to arrange children horizontally and control their [horizontalArrangement]
 * (spacing) and [verticalAlignment].
 *
 * Example of arranging children horizontally with spacing:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteRowSample
 *
 * Example of distributing horizontal space using [RemoteRowScope.weight]:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteRowWeightSample
 * @param modifier modifier to be applied to this row
 * @param horizontalArrangement horizontal arrangement of the children
 * @param verticalAlignment vertical alignment of the children
 * @param content content of the row, which has access to [RemoteRowScope]
 */
@RemoteComposable
@Composable
public fun RemoteRow(
    modifier: RemoteModifier = RemoteModifier,
    horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top,
    content: @RemoteComposable @Composable RemoteRowScope.() -> Unit,
) {
    RemoteLayoutNode(
        factory = ::RemoteRowNode,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(horizontalArrangement) { nodeHorizontalArrangement ->
                this.horizontalArrangement = nodeHorizontalArrangement
            }
            set(verticalAlignment) { nodeVerticalAlignment ->
                this.verticalAlignment = nodeVerticalAlignment
            }
        },
        content = { RemoteRowScopeInstance.content() },
    )
}
