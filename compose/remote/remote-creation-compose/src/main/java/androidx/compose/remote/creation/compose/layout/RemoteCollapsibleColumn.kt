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

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.CollapsiblePriorityModifier
import androidx.compose.remote.creation.compose.modifier.HeightModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Scope for the children of [RemoteCollapsibleColumn]. */
@LayoutScopeMarker
@Immutable
public class RemoteCollapsibleColumnScope {
    /**
     * Sets the vertical weight of the child.
     *
     * @param weight The weight of the child.
     */
    public fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, weight))

    /**
     * Sets the vertical weight of the child.
     *
     * @param weight The weight of the child.
     */
    public fun RemoteModifier.weight(weight: Float): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, RemoteFloat(weight)))

    /**
     * Sets the collapsible priority of the child.
     *
     * Priority determines the order in which children are hidden when space is limited. Higher
     * priority items remain visible longer. Items with the same priority are hidden sequentially in
     * reverse layout order (last child with same priority is hidden first).
     *
     * @param priority The priority of the child. Can be any float value; higher values are
     *   prioritized to remain visible longer.
     */
    public fun RemoteModifier.collapsiblePriority(priority: Float): RemoteModifier =
        then(CollapsiblePriorityModifier(CollapsiblePriority.VERTICAL, RemoteFloat(priority)))
}

internal class RemoteCollapsibleColumnNode : RemoteComposeNode() {
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start
    var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val scope = overriddenScope(creationState)
        val recordingModifier = scope.toRecordingModifier(modifier)
        (verticalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.space.getFloatIdForCreationState(creationState))
        }
        creationState.document.startCollapsibleColumn(
            recordingModifier,
            horizontalAlignment.toRemote(layoutDirection),
            verticalArrangement.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endCollapsibleColumn()
    }
}

/**
 * A collapsible column layout that organizes its children vertically.
 *
 * When available vertical space is insufficient, children are hidden (collapsed) rather than shrunk
 * or wrapped. Children are hidden based on their
 * [RemoteCollapsibleColumnScope.collapsiblePriority], with lower priority items being hidden first.
 * Children without priority set have the highest priority.
 *
 * Children can be configured with [RemoteCollapsibleColumnScope.weight] and
 * [RemoteCollapsibleColumnScope.collapsiblePriority] to control how they behave when the column is
 * collapsed.
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteCollapsibleColumnSample
 * @param modifier The modifier to apply to this layout.
 * @param verticalArrangement The vertical arrangement of the children.
 * @param horizontalAlignment The horizontal alignment of the children.
 * @param content The children of the column.
 */
@RemoteComposable
@Composable
public fun RemoteCollapsibleColumn(
    modifier: RemoteModifier = RemoteModifier,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    content: @Composable RemoteCollapsibleColumnScope.() -> Unit,
) {
    val scope = remember { RemoteCollapsibleColumnScope() }
    val layoutDirection = LocalLayoutDirection.current
    RemoteComposeNode(
        factory = ::RemoteCollapsibleColumnNode,
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
        content = { scope.content() },
    )
}
