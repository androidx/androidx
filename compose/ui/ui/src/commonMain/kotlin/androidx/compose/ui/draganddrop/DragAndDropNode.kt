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

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.traverseChildren
import androidx.compose.ui.node.traverseSubtreeWithKey

/**
 * A [Modifier.Node] providing low level access to platform drag and drop operations.
 * In most cases, you will want to delegate to the [DragAndDropModifierNode] returned by
 * the eponymous factory method.
 */
interface DragAndDropModifierNode : DelegatableNode, DragAndDropTarget {
    /**
     * Begins a drag and drop operation with the contents of [dragAndDropInfo]
     */
    fun drag(dragAndDropInfo: DragAndDropInfo)
}

/**
 * Creates a [Modifier.Node] for integrating with platform level drag and drop events. All
 * [DragAndDropModifierNode] instances provided by this function may start drag and drop events
 * by calling [DragAndDropModifierNode.drag].
 * @param onDragAndDropEvent a provider of a [DragAndDropTarget] that allows this [Modifier.Node]
 * receive platform drag and drop events. If one is not provided, platform drag and drop events
 * will always be rejected.
 */
fun DragAndDropModifierNode(
    onDragAndDropEvent: (event: DragAndDropEvent) -> DragAndDropTarget? = { null }
): DragAndDropModifierNode = DragAndDropNode(onDragAndDropEvent)

/**
 * Core implementation of drag and drop. This [Modifier.Node] implements tree traversal for
 * drag and drop, as well as hit testing and propagation of events for drag or drop gestures.
 *
 * It uses the [DragAndDropEvent] as a representation of a single mutable drag and drop session.
 */
internal class DragAndDropNode(
    private val onDragAndDropEvent: (event: DragAndDropEvent) -> DragAndDropTarget?
) : Modifier.Node(),
    TraversableNode,
    DragAndDropModifierNode {
    companion object {
        private object DragAndDropTraversableKey

        private inline fun DragAndDropModifierNode.firstChildOrNull(
            crossinline predicate: (DragAndDropModifierNode) -> Boolean
        ): DragAndDropModifierNode? {
            var match: DragAndDropModifierNode? = null
            traverseSubtreeWithKey(DragAndDropTraversableKey) { child ->
                if (child is DragAndDropModifierNode && predicate(child)) {
                    match = child
                    return@traverseSubtreeWithKey false
                }
                true
            }
            return match
        }
    }

    override val traverseKey: Any = DragAndDropTraversableKey

    /** Child currently receiving drag gestures for dropping into **/
    private var lastChildDragAndDropModifierNode: DragAndDropModifierNode? = null

    /** This as a drop target if eligible for processing **/
    private var thisDragAndDropTarget: DragAndDropTarget? = null

    // start Node
    override fun onDetach() {
        // Clean up
        thisDragAndDropTarget = null
    }
    // end Node

    // start DragSource

    override fun drag(dragAndDropInfo: DragAndDropInfo) {
        requireOwner().drag(dragAndDropInfo)
    }

    // end DragSource

    // start DropTarget
    override fun onDragAndDropEvent(event: DragAndDropEvent, type: DragAndDropEventType): Boolean {
        when (type) {
            DragAndDropEventType.Started -> return onStarted(event)
            DragAndDropEventType.Dropped -> return onDropped(event)
            DragAndDropEventType.Entered -> onEntered(event)
            DragAndDropEventType.Moved -> onMoved(event)
            DragAndDropEventType.Exited -> onExited(event)
            DragAndDropEventType.Changed -> onChanged(event)
            DragAndDropEventType.Ended -> onEnded(event)
        }
        return false
    }

    private fun onStarted(event: DragAndDropEvent): Boolean {
        check(thisDragAndDropTarget == null) {
            "DragAndDropTarget self reference must be null at the start of a drag and drop session"
        }

        // Start receiving events
        thisDragAndDropTarget = onDragAndDropEvent(event)

        var handledByChild = false

        traverseChildren { child ->
            handledByChild = handledByChild or child.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Started
            ).also { accepted ->
                // TODO (TJ) add interested nodes to the Owner
                if (accepted) event.interestedNodes.add(child)
            }
            true
        }

        return handledByChild || thisDragAndDropTarget != null
    }

    private fun onEntered(event: DragAndDropEvent) {
        when (val self = thisDragAndDropTarget) {
            null -> lastChildDragAndDropModifierNode?.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Entered
            )

            else -> self.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Entered
            )
        }
    }

    private fun onMoved(event: DragAndDropEvent) {
        val currentChildNode: DragAndDropModifierNode? = lastChildDragAndDropModifierNode
        val newChildNode: DragAndDropModifierNode? = when {
            // Moved within child.
            currentChildNode?.contains(event.positionInRoot) == true -> currentChildNode
            // Position is now outside active child, maybe it entered a different one.
            else -> firstChildOrNull { child ->
                // Only dispatch to children who previously accepted the onStart gesture
                // TODO (TJ) read interested nodes from Owner
                event.interestedNodes.contains(child) && child.contains(event.positionInRoot)
            }
        }

        when {
            // Left us and went to a child.
            newChildNode != null && currentChildNode == null -> {
                thisDragAndDropTarget?.onDragAndDropEvent(
                    event = event,
                    type = DragAndDropEventType.Exited
                )
                newChildNode.dispatchEntered(event)
            }
            // Left the child and returned to us.
            newChildNode == null && currentChildNode != null -> {
                currentChildNode.onDragAndDropEvent(
                    event = event,
                    type = DragAndDropEventType.Exited
                )
                thisDragAndDropTarget?.dispatchEntered(event)
            }
            // Left one child and entered another.
            newChildNode != currentChildNode -> {
                currentChildNode?.onDragAndDropEvent(
                    event = event,
                    type = DragAndDropEventType.Exited
                )
                newChildNode?.dispatchEntered(event)
            }
            // Stayed in the same child.
            newChildNode != null -> newChildNode.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Moved
            )
            // Stayed in us.
            else -> thisDragAndDropTarget?.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Moved
            )
        }

        this@DragAndDropNode.lastChildDragAndDropModifierNode = newChildNode
    }

    private fun onChanged(event: DragAndDropEvent) {
        when (val self = thisDragAndDropTarget) {
            null -> lastChildDragAndDropModifierNode?.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Changed
            )

            else -> self.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Changed
            )
        }
    }

    private fun onExited(event: DragAndDropEvent) {
        thisDragAndDropTarget?.onDragAndDropEvent(
            event = event,
            type = DragAndDropEventType.Exited
        )
        lastChildDragAndDropModifierNode?.onDragAndDropEvent(
            event = event,
            type = DragAndDropEventType.Exited
        )
        lastChildDragAndDropModifierNode = null
    }

    private fun onDropped(event: DragAndDropEvent): Boolean {
        return when (val currentChildDropTarget = lastChildDragAndDropModifierNode) {
            null -> thisDragAndDropTarget?.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Dropped
            ) ?: false

            else -> currentChildDropTarget.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Dropped
            )
        }
    }

    private fun onEnded(event: DragAndDropEvent) {
        traverseChildren { child ->
            child.onDragAndDropEvent(
                event = event,
                type = DragAndDropEventType.Ended
            )
            true
        }
        thisDragAndDropTarget?.onDragAndDropEvent(
            event = event,
            type = DragAndDropEventType.Ended
        )
        thisDragAndDropTarget = null
        lastChildDragAndDropModifierNode = null
        // TODO (TJ) Clear interested nodes on the Owner
        event.interestedNodes.clear()
    }
    // end DropTarget
}

private fun DragAndDropTarget.dispatchEntered(event: DragAndDropEvent) = run {
    // Notify of entry
    onDragAndDropEvent(event, DragAndDropEventType.Entered)
    // Start move
    onDragAndDropEvent(event, DragAndDropEventType.Moved)
}

/**
 * Hit test for a [DragAndDropNode].
 */
private fun DragAndDropModifierNode.contains(position: Offset): Boolean {
    if (!node.isAttached) return false
    val currentCoordinates = requireLayoutNode().coordinates
    if (!currentCoordinates.isAttached) return false

    val (width, height) = currentCoordinates.size
    val (x1, y1) = currentCoordinates.positionInRoot()
    val x2 = x1 + width
    val y2 = y1 + height

    return position.x in x1..x2 && position.y in y1..y2
}
