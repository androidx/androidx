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

package androidx.compose.ui.scene

import androidx.collection.ArraySet
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.draganddrop.DragAndDropModifierNode
import androidx.compose.ui.draganddrop.DragAndDropNode
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.PlatformContext

/**
 * The object provided by [ComposeScene] to allow reporting drop-target events to it.
 */
@InternalComposeUiApi
class ComposeSceneDropTarget internal constructor(
    private val activeDragAndDropManager: () -> OwnerDragAndDropManager,
) {
    fun onEntered(event: DragAndDropEvent): Boolean =
        activeDragAndDropManager().onEntered(event)

    fun onExited(event: DragAndDropEvent): Unit =
        activeDragAndDropManager().onExited(event)

    fun onMoved(event: DragAndDropEvent): Unit =
        activeDragAndDropManager().onMoved(event)

    fun onChanged(event: DragAndDropEvent): Unit =
        activeDragAndDropManager().onChanged(event)

    fun onDrop(event: DragAndDropEvent): Boolean =
        activeDragAndDropManager().onDrop(event)
}

/**
 * The actual [DragAndDropManager] implementation tied to a specific
 * [androidx.compose.ui.node.RootNodeOwner].
 */
internal class OwnerDragAndDropManager(
    private val platformContext: PlatformContext
) : DragAndDropManager {
    private val rootDragAndDropNode = DragAndDropNode { null }
    private val interestedNodes = ArraySet<DragAndDropModifierNode>()

    override val modifier: Modifier = DragAndDropModifier(rootDragAndDropNode)

    override fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ): Boolean {
        return platformContext.startDrag(transferData, decorationSize, drawDragDecoration)
    }

    override fun registerNodeInterest(node: DragAndDropModifierNode) {
        interestedNodes.add(node)
    }

    override fun isInterestedNode(node: DragAndDropModifierNode): Boolean {
        return interestedNodes.contains(node)
    }

    fun onEntered(event: DragAndDropEvent): Boolean {
        val accepted = rootDragAndDropNode.acceptDragAndDropTransfer(event)
        interestedNodes.forEach { it.onStarted(event) }
        rootDragAndDropNode.onEntered(event)
        return accepted
    }

    fun onExited(event: DragAndDropEvent) {
        rootDragAndDropNode.onExited(event)
        endDrag(event)
    }

    fun onMoved(event: DragAndDropEvent) {
        rootDragAndDropNode.onMoved(event)
    }

    fun onChanged(event: DragAndDropEvent) {
        rootDragAndDropNode.onChanged(event)
    }

    fun onDrop(event: DragAndDropEvent): Boolean {
        val accepted = rootDragAndDropNode.onDrop(event)
        endDrag(event)
        return accepted
    }

    private fun endDrag(event: DragAndDropEvent) {
        rootDragAndDropNode.onEnded(event)
        interestedNodes.clear()
    }
}

private class DragAndDropModifier(
    val dragAndDropNode: DragAndDropNode
) : ModifierNodeElement<DragAndDropNode>() {
    override fun create() = dragAndDropNode

    override fun update(node: DragAndDropNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "RootDragAndDropNode"
    }

    override fun hashCode(): Int = dragAndDropNode.hashCode()

    override fun equals(other: Any?) = other === this
}
