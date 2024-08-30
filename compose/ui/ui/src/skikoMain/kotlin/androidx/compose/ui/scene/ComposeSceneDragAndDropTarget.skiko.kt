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

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropNode
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.node.DragAndDropOwner

/**
 * The object provided by [ComposeScene] to allow reporting drop-target events to it.
 */
@InternalComposeUiApi
class ComposeSceneDragAndDropTarget internal constructor(
    private val dragAndDropOwner: () -> DragAndDropOwner,
) : DragAndDropTarget {
    private var startedNode: DragAndDropNode? = null
    private val currentNode: DragAndDropNode
        get() = dragAndDropOwner().rootDragAndDropNode

    val hasEligibleDropTarget: Boolean
        get() = currentNode.hasEligibleDropTarget

    fun acceptDragAndDropTransfer(startEvent: DragAndDropEvent): Boolean {
        ensureStarted(null, startEvent)
        return currentNode.acceptDragAndDropTransfer(startEvent)
    }

    override fun onDrop(event: DragAndDropEvent): Boolean {
        val node = currentNode
        ensureStarted(node, event)
        return node.onDrop(event)
    }

    override fun onStarted(event: DragAndDropEvent) {
        ensureStarted(currentNode, event)
    }

    override fun onEntered(event: DragAndDropEvent) {
        val node = currentNode
        ensureStarted(node, event)
        node.onEntered(event)
    }

    override fun onMoved(event: DragAndDropEvent) {
        val node = currentNode
        ensureStarted(node, event)
        node.onMoved(event)
    }

    override fun onExited(event: DragAndDropEvent) {
        val node = currentNode
        ensureStarted(node, event)
        node.onExited(event)
    }

    override fun onChanged(event: DragAndDropEvent) {
        val node = currentNode
        ensureStarted(node, event)
        node.onChanged(event)
    }

    override fun onEnded(event: DragAndDropEvent) {
        ensureStarted(null, event)
    }

    private fun ensureStarted(node: DragAndDropNode?, event: DragAndDropEvent) {
        if (startedNode != node) {
            startedNode?.onEnded(event)
            startedNode = node
            startedNode?.onStarted(event)
        }
    }
}
