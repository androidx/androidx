/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.draganddrop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropModifierNode
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * A modifier that allows for receiving from a drag and drop gesture.
 *
 * Learn how to use [Modifier.dragAndDropTarget] to receive drag and drop events from inside your
 * app or from other apps:
 *
 * @sample androidx.compose.foundation.samples.TextDragAndDropTargetSample
 *
 * @param shouldStartDragAndDrop Allows the Composable to decide if it wants to receive from
 * a given drag and drop session by inspecting the [DragAndDropEvent] that started the session.
 *
 * @param target The [DragAndDropTarget] that will receive events for a given drag and drop
 * session.
 *
 * All drag and drop target modifiers in the hierarchy will be given an opportunity to participate
 * in a given drag and drop session via [shouldStartDragAndDrop].
 *
 * @see [DragAndDropModifierNode.acceptDragAndDropTransfer]
 */
@ExperimentalFoundationApi
fun Modifier.dragAndDropTarget(
    shouldStartDragAndDrop: (startEvent: DragAndDropEvent) -> Boolean,
    target: DragAndDropTarget,
): Modifier = this then DropTargetElement(
    target = target,
    shouldStartDragAndDrop = shouldStartDragAndDrop,
)

@ExperimentalFoundationApi
private class DropTargetElement(
    val shouldStartDragAndDrop: (event: DragAndDropEvent) -> Boolean,
    val target: DragAndDropTarget,
) : ModifierNodeElement<DragAndDropTargetNode>() {
    override fun create() = DragAndDropTargetNode(
        target = target,
        shouldStartDragAndDrop = shouldStartDragAndDrop,
    )

    override fun update(node: DragAndDropTargetNode) = with(node) {
        target = this@DropTargetElement.target
        shouldStartDragAndDrop = this@DropTargetElement.shouldStartDragAndDrop
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dropTarget"
        properties["target"] = target
        properties["shouldStartDragAndDrop"] = shouldStartDragAndDrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DropTargetElement) return false
        if (target != other.target) return false

        return shouldStartDragAndDrop == other.shouldStartDragAndDrop
    }

    override fun hashCode(): Int {
        var result = target.hashCode()
        result = 31 * result + shouldStartDragAndDrop.hashCode()
        return result
    }
}

@ExperimentalFoundationApi
private class DragAndDropTargetNode(
    var shouldStartDragAndDrop: (event: DragAndDropEvent) -> Boolean,
    var target: DragAndDropTarget
) : DelegatingNode() {
    init {
        delegate(
            DragAndDropModifierNode(
                shouldStartDragAndDrop = shouldStartDragAndDrop,
                target = target
            )
        )
    }
}
