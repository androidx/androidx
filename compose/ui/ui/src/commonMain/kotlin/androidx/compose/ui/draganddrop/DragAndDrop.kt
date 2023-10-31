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

import androidx.compose.ui.geometry.Offset

/**
 * Definition for a type representing transferable data. It could be a remote URI,
 * rich text data on the clip board, a local file, or more.
 */
expect class DragAndDropTransferData

/**
 * A representation of an event sent by the platform during a drag and drop operation.
 */
expect class DragAndDropEvent

/**
 * Returns the position of this [DragAndDropEvent] relative to the root Compose View in the
 * layout hierarchy.
 */
internal expect val DragAndDropEvent.positionInRoot: Offset

/**
 * A factory method for creating a [DragAndDropTarget] to receive transfer data from a
 * drag and drop session.
 *
 * @param onDropped The item has been dropped inside this [DragAndDropTarget].
 * returning true indicates that the [DragAndDropEvent] was consumed, false indicates it was
 * rejected.
 * @see [DragAndDropTarget.onDropped]
 *
 * @param onStarted The drag and drop session has begun. This gives this [DragAndDropTarget]
 * an opportunity to present itself in a way to indicate it is capable of receiving a
 * drag and drop gesture.
 * @see [DragAndDropTarget.onStarted]
 *
 * @param onEntered The item being dropped has entered into the bounds of this [DragAndDropTarget].
 * @see [DragAndDropTarget.onEntered]
 *
 * @param onMoved The item being dropped has moved within the bounds of this [DragAndDropTarget].
 * @see [DragAndDropTarget.onMoved]
 *
 * @param onChanged The event in the current drag and drop session has changed within
 * the bounds of this [DragAndDropTarget].
 * @see [DragAndDropTarget.onChanged]
 *
 * @param onExited The item being dropped has moved outside the bounds of this [DragAndDropTarget].
 * @see [DragAndDropTarget.onExited]
 *
 * @param onEnded The drag and drop gesture is complete.
 * @see [DragAndDropTarget.onEnded]
 */
fun DragAndDropTarget(
    onDropped: (event: DragAndDropEvent) -> Boolean,
    onStarted: (event: DragAndDropEvent) -> Unit = {},
    onEntered: (event: DragAndDropEvent) -> Unit = {},
    onMoved: (event: DragAndDropEvent) -> Unit = {},
    onChanged: (event: DragAndDropEvent) -> Unit = {},
    onExited: (event: DragAndDropEvent) -> Unit = {},
    onEnded: (event: DragAndDropEvent) -> Unit = {},
): DragAndDropTarget = object : DragAndDropTarget {
    override fun onStarted(event: DragAndDropEvent) =
        onStarted.invoke(event)

    override fun onDropped(event: DragAndDropEvent): Boolean =
        onDropped.invoke(event)

    override fun onEntered(event: DragAndDropEvent) =
        onEntered.invoke(event)

    override fun onMoved(event: DragAndDropEvent) =
        onMoved.invoke(event)

    override fun onExited(event: DragAndDropEvent) =
        onExited.invoke(event)

    override fun onChanged(event: DragAndDropEvent) =
        onChanged.invoke(event)

    override fun onEnded(event: DragAndDropEvent) =
        onEnded.invoke(event)
}

/**
 * Provides a means of receiving a transfer data from a drag and drop session.
 */
interface DragAndDropTarget {

    /** A drag and drop session has just been started and this [DragAndDropTarget] is eligible
     * to receive it. This gives an opportunity to set the state for a [DragAndDropTarget] in
     * preparation for consuming a drag and drop session.
     */
    fun onStarted(event: DragAndDropEvent)

    /**
     * An item has been dropped inside this [DragAndDropTarget].
     *
     * @return true to indicate that the [DragAndDropEvent] was consumed; false indicates it was
     * rejected.
     */
    fun onDropped(event: DragAndDropEvent): Boolean

    /**
     * An item being dropped has entered into the bounds of this [DragAndDropTarget].
     */
    fun onEntered(event: DragAndDropEvent)

    /**
     * An item being dropped has moved within the bounds of this [DragAndDropTarget].
     */
    fun onMoved(event: DragAndDropEvent)

    /**
     * An item being dropped has moved outside the bounds of this [DragAndDropTarget].
     */
    fun onExited(event: DragAndDropEvent)

    /**
     * An event in the current drag and drop session has changed within this [DragAndDropTarget]
     * bounds. Perhaps a modifier key has been pressed or released.
     */
    fun onChanged(event: DragAndDropEvent)

    /**
     * The drag and drop session has been completed. All [DragAndDropTarget] instances in the
     * hierarchy that previously received an [onStarted] event will receive this event. This gives
     * an opportunity to reset the state for a [DragAndDropTarget].
     */
    fun onEnded(event: DragAndDropEvent)
}
