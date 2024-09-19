/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/** A platform implementation for drag and drop functionality. */
internal interface DragAndDropManager {

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier list
     * that contains the modifiers required by drag and drop. (Eg, a root drag and drop modifier).
     */
    val modifier: Modifier

    /**
     * Returns a boolean value indicating whether requesting drag and drop transfer is required. If
     * it's not, the transfer might be initiated only be system and calling
     * [requestDragAndDropTransfer] will be ignored.
     */
    val isRequestDragAndDropTransferRequired: Boolean

    /**
     * Requests a drag and drop transfer. It might ignored in case if the operation performed by
     * system. [isRequestDragAndDropTransferRequired] can be used to check if it should be used
     * explicitly.
     */
    fun requestDragAndDropTransfer(node: DragAndDropNode, offset: Offset)

    /**
     * Called to notify this [DragAndDropManager] that a [DragAndDropTarget] is interested in
     * receiving events for a particular drag and drop session.
     */
    fun registerTargetInterest(target: DragAndDropTarget)

    /**
     * Called to check if a [DragAndDropTarget] has previously registered interest for a drag and
     * drop session.
     */
    fun isInterestedTarget(target: DragAndDropTarget): Boolean
}
