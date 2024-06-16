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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

internal interface DragAndDropManager {

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier list
     * that contains the modifiers required by drag and drop. (Eg, a root drag and drop modifier).
     */
    val modifier: Modifier

    /**
     * Initiates a drag-and-drop operation for transferring data.
     *
     * @param transferData the data to be transferred after successful completion of the drag and
     *   drop gesture.
     * @param decorationSize the size of the drag decoration to be drawn.
     * @param drawDragDecoration provides the visual representation of the item dragged during the
     *   drag and drop gesture.
     * @return true if the method completes successfully, or false if it fails anywhere. Returning
     *   false means the system was unable to do a drag because of another ongoing operation or some
     *   other reasons.
     */
    fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit,
    ): Boolean

    /**
     * Called to notify this [DragAndDropManager] that a [DragAndDropModifierNode] is interested in
     * receiving events for a particular drag and drop session.
     */
    fun registerNodeInterest(node: DragAndDropModifierNode)

    /**
     * Called to check if a [DragAndDropModifierNode] has previously registered interest for a drag
     * and drop session.
     */
    fun isInterestedNode(node: DragAndDropModifierNode): Boolean
}
