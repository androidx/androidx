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
import androidx.compose.ui.draganddrop.DragAndDropEventType
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
 * @param onStarted Allows the Composable to decide if it wants to receive the drop gesture.
 * returning true indicates interest in a [DragAndDropEvent], false indicates no interest. If false
 * is returned, this [Modifier] will not receive any events for this drag and drop session until the
 * [onEnded] signal. All drag and drop target modifiers in the hierarchy will receive this event.
 * @see [DragAndDropEventType.Started]
 *
 * @param onDropped The item has been dropped inside this Composable.
 * returning true indicates that the [DragAndDropEvent] was consumed, false indicates it was
 * rejected. Receiving this event is prerequisite on returning true in [onStarted].
 * @see [DragAndDropEventType.Dropped]
 *
 * @param onEntered The item being dropped has entered into this Composable's bounds.
 * Receiving this event is prerequisite on returning true in [onStarted].
 * @see [DragAndDropEventType.Entered]
 *
 * @param onMoved The item being dropped has moved within this Composable's bounds.
 * Receiving this event is prerequisite on returning true in [onStarted].
 * @see [DragAndDropEventType.Moved]
 *
 * @param onChanged The event in the current drag and drop session has changed within
 * this Composable's bounds.
 * @see [DragAndDropEventType.Changed]
 *
 * Receiving this event is prerequisite on returning true in [onStarted].
 * @param onExited The item being dropped has moved outside this Composable's bounds.
 * Receiving this event is prerequisite on returning true in [onStarted].
 * @see [DragAndDropEventType.Exited]
 *
 * @param onEnded The drag and drop gesture is complete.
 * All drag and drop target modifiers in the hierarchy will receive this event.
 * @see [DragAndDropEventType.Ended]
 */
@ExperimentalFoundationApi
fun Modifier.dragAndDropTarget(
    onStarted: (event: DragAndDropEvent) -> Boolean,
    onDropped: (event: DragAndDropEvent) -> Boolean,
    onEntered: (event: DragAndDropEvent) -> Unit = {},
    onMoved: (event: DragAndDropEvent) -> Unit = {},
    onChanged: (event: DragAndDropEvent) -> Unit = {},
    onExited: (event: DragAndDropEvent) -> Unit = {},
    onEnded: (event: DragAndDropEvent) -> Unit = {},
): Modifier = this then DropTargetElement(
    onStarted = onStarted,
    onDropped = onDropped,
    onEntered = onEntered,
    onMoved = onMoved,
    onChanged = onChanged,
    onExited = onExited,
    onEnded = onEnded
)

@ExperimentalFoundationApi
private class DropTargetElement(
    val onStarted: (event: DragAndDropEvent) -> Boolean,
    val onDropped: (event: DragAndDropEvent) -> Boolean,
    val onEntered: (event: DragAndDropEvent) -> Unit = {},
    val onMoved: (event: DragAndDropEvent) -> Unit = {},
    val onChanged: (event: DragAndDropEvent) -> Unit = {},
    val onExited: (event: DragAndDropEvent) -> Unit = {},
    val onEnded: (event: DragAndDropEvent) -> Unit = {},
) : ModifierNodeElement<DragAndDropTargetNode>() {
    override fun create() = DragAndDropTargetNode(
        onStarted = onStarted,
        onDropped = onDropped,
        onEntered = onEntered,
        onMoved = onMoved,
        onChanged = onChanged,
        onExited = onExited,
        onEnded = onEnded,
    )

    override fun update(node: DragAndDropTargetNode) = with(node) {
        onStarted = this@DropTargetElement.onStarted
        onDropped = this@DropTargetElement.onDropped
        onEntered = this@DropTargetElement.onEntered
        onMoved = this@DropTargetElement.onMoved
        onChanged = this@DropTargetElement.onChanged
        onExited = this@DropTargetElement.onExited
        onEnded = this@DropTargetElement.onEnded
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dropTarget"
        properties["onDragStarted"] = onStarted
        properties["onDropped"] = onDropped
        properties["onEntered"] = onEntered
        properties["onMoved"] = onMoved
        properties["onChanged"] = onChanged
        properties["onExited"] = onExited
        properties["onEnded"] = onEnded
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DropTargetElement) return false

        if (onStarted != other.onStarted) return false
        if (onDropped != other.onDropped) return false
        if (onEntered != other.onEntered) return false
        if (onMoved != other.onMoved) return false
        if (onChanged != other.onChanged) return false
        if (onExited != other.onExited) return false
        return onEnded == other.onEnded
    }

    override fun hashCode(): Int {
        var result = onStarted.hashCode()
        result = 31 * result + onDropped.hashCode()
        result = 31 * result + onEntered.hashCode()
        result = 31 * result + onMoved.hashCode()
        result = 31 * result + onChanged.hashCode()
        result = 31 * result + onExited.hashCode()
        result = 31 * result + onEnded.hashCode()
        return result
    }
}

@ExperimentalFoundationApi
private class DragAndDropTargetNode(
    var onStarted: (event: DragAndDropEvent) -> Boolean,
    var onDropped: (event: DragAndDropEvent) -> Boolean,
    var onEntered: (event: DragAndDropEvent) -> Unit,
    var onMoved: (event: DragAndDropEvent) -> Unit,
    var onChanged: (event: DragAndDropEvent) -> Unit,
    var onExited: (event: DragAndDropEvent) -> Unit,
    var onEnded: (event: DragAndDropEvent) -> Unit,
) : DelegatingNode(), DragAndDropTarget {

    init {
        delegate(
            DragAndDropModifierNode { event ->
                when {
                    onStarted(event) -> this@DragAndDropTargetNode
                    else -> null
                }
            }
        )
    }

    override fun onDragAndDropEvent(event: DragAndDropEvent, type: DragAndDropEventType): Boolean {
        when (type) {
            DragAndDropEventType.Started -> return onStarted.invoke(event)
            DragAndDropEventType.Dropped -> return onDropped.invoke(event)
            DragAndDropEventType.Entered -> onEntered.invoke(event)
            DragAndDropEventType.Moved -> onMoved.invoke(event)
            DragAndDropEventType.Exited -> onExited.invoke(event)
            DragAndDropEventType.Changed -> onChanged.invoke(event)
            DragAndDropEventType.Ended -> onEnded.invoke(event)
        }
        return false
    }
}
