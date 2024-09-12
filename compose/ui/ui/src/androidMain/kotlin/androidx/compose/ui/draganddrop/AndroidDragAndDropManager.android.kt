/*
 * Copyright 2024 The Android Open Source Project
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

import android.view.DragEvent
import android.view.View
import androidx.collection.ArraySet
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/** A Class that provides access [View.OnDragListener] APIs for a [DragAndDropNode]. */
internal class AndroidDragAndDropManager(
    private val startDrag:
        (
            transferData: DragAndDropTransferData,
            decorationSize: Size,
            drawDragDecoration: DrawScope.() -> Unit
        ) -> Boolean
) : View.OnDragListener, DragAndDropManager {

    private val rootDragAndDropNode = DragAndDropNode()

    /**
     * A collection [DragAndDropNode] instances that registered interested in a drag and drop
     * session by returning true in [DragAndDropNode.onStarted].
     */
    private val interestedTargets = ArraySet<DragAndDropTarget>()

    override val modifier: Modifier =
        object : ModifierNodeElement<DragAndDropNode>() {
            override fun create() = rootDragAndDropNode

            override fun update(node: DragAndDropNode) = Unit

            override fun InspectorInfo.inspectableProperties() {
                name = "RootDragAndDropNode"
            }

            override fun hashCode(): Int = rootDragAndDropNode.hashCode()

            override fun equals(other: Any?) = other === this
        }

    override val isRequestDragAndDropTransferRequired: Boolean
        get() = true

    override fun requestDragAndDropTransfer(node: DragAndDropNode, offset: Offset) {
        var isTransferStarted = false
        val dragAndDropSourceScope =
            object : DragAndDropStartTransferScope {
                override fun startDragAndDropTransfer(
                    transferData: DragAndDropTransferData,
                    decorationSize: Size,
                    drawDragDecoration: DrawScope.() -> Unit
                ): Boolean {
                    isTransferStarted =
                        startDrag(
                            transferData,
                            decorationSize,
                            drawDragDecoration,
                        )
                    return isTransferStarted
                }
            }
        with(node) { dragAndDropSourceScope.startDragAndDropTransfer(offset) { isTransferStarted } }
    }

    override fun onDrag(view: View, event: DragEvent): Boolean {
        val dragAndDropEvent = DragAndDropEvent(dragEvent = event)
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                val accepted = rootDragAndDropNode.acceptDragAndDropTransfer(dragAndDropEvent)
                interestedTargets.forEach { it.onStarted(dragAndDropEvent) }
                accepted
            }
            DragEvent.ACTION_DROP -> {
                rootDragAndDropNode.onDrop(dragAndDropEvent)
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                rootDragAndDropNode.onEntered(dragAndDropEvent)
                false
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                rootDragAndDropNode.onMoved(dragAndDropEvent)
                false
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                rootDragAndDropNode.onExited(dragAndDropEvent)
                false
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                rootDragAndDropNode.onEnded(dragAndDropEvent)
                interestedTargets.clear()
                false
            }
            else -> false
        }
    }

    override fun registerTargetInterest(target: DragAndDropTarget) {
        interestedTargets.add(target)
    }

    override fun isInterestedTarget(target: DragAndDropTarget): Boolean {
        return interestedTargets.contains(target)
    }
}
