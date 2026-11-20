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

import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.CollapsiblePriorityModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/** Scope for the children of [RemoteCollapsibleRow]. */
@RemoteLayoutScopeMarker
@Immutable
public sealed interface RemoteCollapsibleRowScope : RemoteRowScope {
    /**
     * Sets the collapsible priority of the child.
     *
     * Priority determines the order in which children are hidden when space is limited. Higher
     * priority items remain visible longer. Items with the same priority are hidden sequentially in
     * reverse layout order (last child with same priority is hidden first).
     *
     * Example of assigning collapsible priorities to children in a [RemoteCollapsibleRow]:
     *
     * @sample androidx.compose.remote.creation.compose.samples.RemoteCollapsibleRowSample
     * @param priority priority of the child; higher values are prioritized to remain visible longer
     * @return a [RemoteModifier] that assigns horizontal collapsible `priority` to the child
     */
    @Stable
    public fun RemoteModifier.collapsiblePriority(priority: Float): RemoteModifier =
        then(CollapsiblePriorityModifier(CollapsiblePriority.HORIZONTAL, RemoteFloat(priority)))
}

internal object RemoteCollapsibleRowScopeInstance : RemoteCollapsibleRowScope

internal class RemoteCollapsibleRowNode : RemoteLayoutNode() {
    var horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start
    var verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val scope = overriddenScope(remoteCanvas)
        val recordingModifier = scope.toRecordingModifier(modifier)
        (horizontalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.getSpacingFloatId(scope))
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
 * A collapsible row layout that organizes its children horizontally.
 *
 * When available horizontal space is insufficient, children are hidden (collapsed) rather than
 * shrunk or wrapped. Children are hidden based on their
 * [RemoteCollapsibleRowScope.collapsiblePriority], with lower priority items being hidden first.
 * Children without priority set have the highest priority.
 *
 * Children can be configured with [RemoteCollapsibleRowScope.weight] and
 * [RemoteCollapsibleRowScope.collapsiblePriority] to control how they behave when the row is
 * collapsed.
 *
 * Example of hiding lower-priority children when horizontal space is constrained:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteCollapsibleRowSample
 * @param modifier modifier to apply to this layout
 * @param horizontalArrangement horizontal arrangement of the children
 * @param verticalAlignment vertical alignment of the children
 * @param content children of the row
 */
@RemoteComposable
@Composable
public fun RemoteCollapsibleRow(
    modifier: RemoteModifier = RemoteModifier,
    horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top,
    content: @RemoteComposable @Composable RemoteCollapsibleRowScope.() -> Unit,
) {
    RemoteLayoutNode(
        factory = ::RemoteCollapsibleRowNode,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(horizontalArrangement) { nodeHorizontalArrangement ->
                this.horizontalArrangement = nodeHorizontalArrangement
            }
            set(verticalAlignment) { nodeVerticalAlignment ->
                this.verticalAlignment = nodeVerticalAlignment
            }
        },
        content = { RemoteCollapsibleRowScopeInstance.content() },
    )
}
