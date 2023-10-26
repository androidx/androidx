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
 * This represents the target of a drag and drop action
 */
interface DragAndDropTarget {

    /** A drag and drop session has just been started. All eligible [DragAndDropTarget]
     * instances will be notified and allowed to register their intent to keep receiving
     * drag and drop events.
     *
     * @return true to indicates interest in the item being dragged, false otherwise.
     * A [DragAndDropTarget] that returns false will no longer see future events for this
     * drag and drop session.
     *
     * All [DragAndDropTarget] instances in the hierarchy will receive this event and be given
     * a chance to register their interest.
     */
    fun onStarted(event: DragAndDropEvent): Boolean

    /**
     * An item has been dropped inside this [DragAndDropTarget].
     *
     * @return true to indicate that the [DragAndDropEvent] was consumed; false indicates it was
     * rejected.
     *
     * Receiving this event is prerequisite on returning true in [onStarted].
     */
    fun onDropped(event: DragAndDropEvent): Boolean

    /**
     * An item being dropped has entered into the bounds of this [DragAndDropTarget].
     *
     * Receiving this event is prerequisite on returning true in [onStarted].
     */
    fun onEntered(event: DragAndDropEvent)

    /**
     * An item being dropped has moved within the bounds of this [DragAndDropTarget].
     *
     * Receiving this event is prerequisite on returning true in [onStarted].
     */
    fun onMoved(event: DragAndDropEvent)

    /**
     * An item being dropped has moved outside the bounds of this [DragAndDropTarget].
     *
     * Receiving this event is prerequisite on returning true in [onStarted].
     */
    fun onExited(event: DragAndDropEvent)

    /**
     * An event in the current drag and drop session has changed within this [DragAndDropTarget]
     * bounds. Perhaps a modifier key has been pressed or released.
     *
     * Receiving this event is prerequisite on returning true in [onStarted].
     */
    fun onChanged(event: DragAndDropEvent)

    /**
     * The drag and drop session has been completed. All [DragAndDropTarget] instances in the
     * hierarchy that previously returned true in [onStarted] will receive this event. This gives
     * an opportunity to reset the state for a [DragAndDropTarget].
     */
    fun onEnded(event: DragAndDropEvent)
}
