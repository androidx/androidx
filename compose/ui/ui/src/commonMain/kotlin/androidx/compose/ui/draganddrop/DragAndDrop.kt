/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.draganddrop

import androidx.collection.ArraySet
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Definition for a type representing transferable data. It could be a remote URI,
 * rich text data on the clip board, a local file, or more.
 */
expect class DragAndDropTransfer

@kotlin.jvm.JvmInline
value class DragAndDropEventType private constructor(private val value: Int) {

    override fun toString(): String = when (value) {
        1 -> "Started"
        2 -> "Entered"
        3 -> "Moved"
        4 -> "Exited"
        5 -> "Changed"
        6 -> "Dropped"
        7 -> "Ended"
        else -> "Unknown"
    }
    companion object {
        /**
         * An unknown drag and drop type.
         */
        val Unknown = DragAndDropEventType(0)

        /**
         * A drag and drop session has just been started. All eligible listeners will be notified and
         * allowed to register their intent to keep receiving drag and drop events.
         */
        val Started = DragAndDropEventType(1)

        /**
         * A drag and drop event has just entered the bounds of this listener.
         */
        val Entered = DragAndDropEventType(2)

        /**
         * A drag and drop event has moved within the bounds of this listener.
         */
        val Moved = DragAndDropEventType(3)

        /**
         * A drag and drop event has just left the bounds of this listener.
         */
        val Exited = DragAndDropEventType(4)

        /**
         * A drag and drop event has changed within the bounds of this listener. Perhaps a modifier
         * key has been pressed or released.
         */
        val Changed = DragAndDropEventType(5)

        /**
         * A drag and drop event is being concluded inside the bounds of this listener. The listener
         * has the option to accept or reject the drag.
         */
        val Dropped = DragAndDropEventType(6)

        /**
         * A previously started drag and drop session has been concluded. All eligible listeners
         * will be notified of this event. This gives an opportunity to reset a listener's state.
         */
        val Ended = DragAndDropEventType(7)
    }
}

/**
 * A representation of an event sent by the platform during a drag and drop operation.
 */
expect class DragAndDropEvent {
    // TODO: Move this to the Owner interface
    /**
     * A collection [DragAndDropModifierNode] instances that registered interested in a
     * drag and drop session by returning true in [DragAndDropModifierNode.onDragAndDropEvent]
     * with a [DragAndDropEventType.Started] type.
     */
    internal val interestedNodes: ArraySet<DragAndDropModifierNode>
}

/**
 * Returns the position of this [DragAndDropEvent] relative to the root Compose View in the
 * layout hierarchy.
 */
internal expect val DragAndDropEvent.positionInRoot: Offset

/**
 * Metadata summarizing the properties used during a drag event
 */
class DragAndDropInfo(
    /**
     * The size of the drag shadow for the item that was dragged
     */
    val size: Size,
    /**
     * The data to transfer after the drag and drop event completes
     */
    val transfer: DragAndDropTransfer,
    /**
     * A [DrawScope] receiving lambda to draw the drag shadow for the drag and drop operation
     */
    val onDrawDragShadow: DrawScope.() -> Unit,
)

/**
 * This represents the target of a drag and drop action
 */
fun interface DragAndDropTarget {

    /** An event has occurred in a drag and drop session.
     * @return When the [type] is [DragAndDropEventType.Started], true indicates interest in the
     * [DragAndDropEvent], while false indicates disinterest. A [DragAndDropTarget] that returns
     * false for an event should no longer see future events for this drag and drop session.
     * When the [type] is [DragAndDropEventType.Dropped], true indicates acceptance of the
     * [DragAndDropEvent] while false a rejection of the event.
     * For all other [DragAndDropEventType] values, false is returned.
     */
    fun onDragAndDropEvent(
        /**
         * The event containing information about the drag and drop action
         */
        event: DragAndDropEvent,
        /**
         * The type of the [DragAndDropEvent]
         */
        type: DragAndDropEventType
    ): Boolean
}
