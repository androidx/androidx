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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.CancelTraversal
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.ContinueTraversal
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.js.JsName
import kotlin.jvm.JvmName

/**
 * A [Modifier.Node] providing low level access to platform drag and drop operations. In most cases,
 * you will want to delegate to the [DragAndDropModifierNode] returned by the eponymous factory
 * method.
 */
@Deprecated(
    message =
        "This interface is deprecated in favor to " +
            "DragAndDropSourceModifierNode and DragAndDropTargetModifierNode",
    replaceWith = ReplaceWith("DragAndDropSourceModifierNode")
)
interface DragAndDropModifierNode : DelegatableNode, DragAndDropTarget {
    /**
     * Begins a drag and drop session for transferring data.
     *
     * @param transferData the data to be transferred after successful completion of the drag and
     *   drop gesture.
     * @param decorationSize the size of the drag decoration to be drawn.
     * @param drawDragDecoration provides the visual representation of the item dragged during the
     *   drag and drop gesture.
     */
    @Deprecated("Use DragAndDropSourceModifierNode.requestDragAndDropTransfer instead")
    fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit,
    )

    /**
     * The entry point to register interest in a drag and drop session for receiving data.
     *
     * @return true to indicate interest in the contents of a drag and drop session, false indicates
     *   no interest. If false is returned, this [Modifier] will not receive any [DragAndDropTarget]
     *   events.
     *
     * All [DragAndDropModifierNode] instances in the hierarchy will be given an opportunity to
     * participate in a drag and drop session via this method.
     */
    fun acceptDragAndDropTransfer(startEvent: DragAndDropEvent): Boolean
}

/**
 * A [Modifier.Node] that can be used as a source for platform drag and drop operations. In most
 * cases, you will want to delegate to the [DragAndDropSourceModifierNode] returned by the eponymous
 * factory method.
 */
sealed interface DragAndDropSourceModifierNode : LayoutAwareModifierNode {
    /**
     * Returns a boolean value indicating whether requesting drag and drop transfer is required.
     *
     * This variable is used to check if the platform requires drag and drop transfer initiated by
     * application explicitly, for example via a custom gesture.
     *
     * @see requestDragAndDropTransfer
     */
    val isRequestDragAndDropTransferRequired: Boolean

    /**
     * Requests a drag and drop transfer. [isRequestDragAndDropTransferRequired] can be used to
     * check if it required to be performed.
     *
     * @param offset the offset value representing position of the input pointer.
     */
    fun requestDragAndDropTransfer(offset: Offset)
}

/**
 * A [Modifier.Node] that can be used as a target for platform drag and drop operations. In most
 * cases, you will want to delegate to the [DragAndDropTargetModifierNode] returned by the eponymous
 * factory method.
 *
 * This interface does not define any additional methods or properties. It simply serves as a marker
 * interface to identify nodes that can be used as drag and drop target modifiers.
 */
sealed interface DragAndDropTargetModifierNode : LayoutAwareModifierNode

/**
 * Creates a [Modifier.Node] for starting platform drag and drop sessions with the intention of
 * transferring data. A drag and stop session is started by calling [DragAndDropModifierNode.drag].
 */
@Deprecated(
    message = "Use DragAndDropSourceModifierNode instead",
    replaceWith = ReplaceWith("DragAndDropSourceModifierNode")
)
@Suppress("DEPRECATION")
@JsName("funDragAndDropModifierNode1")
fun DragAndDropModifierNode(): DragAndDropModifierNode = DragAndDropNode(onStartTransfer = null)

/**
 * Creates a [Modifier.Node] for receiving transfer data from platform drag and drop sessions. All
 * [DragAndDropModifierNode] instances provided by this function may also start drag and drop
 * sessions by calling [DragAndDropModifierNode.drag].
 *
 * @param shouldStartDragAndDrop allows for inspecting the start [DragAndDropEvent] for a given
 *   session to decide whether or not the provided [DragAndDropTarget] would like to receive from
 *   it.
 * @param target allows for receiving events and transfer data from a given drag and drop session.
 */
@Deprecated(
    message = "Use DragAndDropTargetModifierNode instead",
    replaceWith = ReplaceWith("DragAndDropTargetModifierNode")
)
@Suppress("DEPRECATION")
@JsName("funDragAndDropModifierNode2")
fun DragAndDropModifierNode(
    shouldStartDragAndDrop: (event: DragAndDropEvent) -> Boolean,
    target: DragAndDropTarget
): DragAndDropModifierNode =
    DragAndDropNode(
        onDropTargetValidate = { event -> if (shouldStartDragAndDrop(event)) target else null }
    )

/**
 * Creates a [DragAndDropSourceModifierNode] for starting platform drag and drop sessions with the
 * intention of transferring data.
 *
 * @param onStartTransfer the callback function that is invoked when drag and drop session starts.
 *   It takes an [Offset] parameter representing the start position of the drag.
 */
fun DragAndDropSourceModifierNode(
    onStartTransfer: DragAndDropStartTransferScope.(Offset) -> Unit,
): DragAndDropSourceModifierNode = DragAndDropNode(onStartTransfer = onStartTransfer)

/**
 * Creates a [DragAndDropTargetModifierNode] for receiving transfer data from platform drag and drop
 * sessions.
 *
 * @param shouldStartDragAndDrop allows for inspecting the start [DragAndDropEvent] for a given
 *   session to decide whether or not the provided [DragAndDropTarget] would like to receive from
 *   it.
 * @param target allows for receiving events and transfer data from a given drag and drop session.
 */
fun DragAndDropTargetModifierNode(
    shouldStartDragAndDrop: (event: DragAndDropEvent) -> Boolean,
    target: DragAndDropTarget
): DragAndDropTargetModifierNode =
    DragAndDropNode(
        onDropTargetValidate = { event -> if (shouldStartDragAndDrop(event)) target else null }
    )

/**
 * Core implementation of drag and drop. This [Modifier.Node] implements tree traversal for drag and
 * drop, as well as hit testing and propagation of events for drag or drop gestures.
 *
 * It uses the [DragAndDropEvent] as a representation of a single mutable drag and drop session.
 *
 * The implementation implicitly maintains a sorted tree of nodes where the order of traversal is
 * determined by the proximity to the last event. That is, after finding a receiving node, the next
 * event will follow the same path the previous event did unless a fork is found and another node
 * should receive the event.
 *
 * This optimizes traversal for the common case of move events where the event remains within a
 * single node, or moves to a sibling of the node.
 *
 * This intended to be used directly only by [DragAndDropManager].
 */
@Suppress("DEPRECATION")
internal class DragAndDropNode(
    private var onStartTransfer: (DragAndDropStartTransferScope.(Offset) -> Unit)? = null,
    private val onDropTargetValidate: ((DragAndDropEvent) -> DragAndDropTarget?)? = null,
) :
    Modifier.Node(),
    TraversableNode,
    DragAndDropModifierNode,
    DragAndDropSourceModifierNode,
    DragAndDropTargetModifierNode,
    DragAndDropTarget {
    private companion object {
        private object DragAndDropTraversableKey
    }

    override val traverseKey: Any = DragAndDropTraversableKey

    private val dragAndDropManager: DragAndDropManager
        get() = requireOwner().dragAndDropManager

    /** Child currently receiving drag gestures for dropping into */
    private var lastChildDragAndDropModifierNode: DragAndDropNode? = null

    /** This as a drop target if eligible for processing */
    private var thisDragAndDropTarget: DragAndDropTarget? = null

    /**
     * Indicates whether there is a child that is eligible to receive a drop gesture immediately.
     * This is true if the last move happened over a child that is interested in receiving a drop.
     */
    @get:JvmName("hasEligibleDropTarget")
    val hasEligibleDropTarget: Boolean
        get() = lastChildDragAndDropModifierNode != null || thisDragAndDropTarget != null

    internal var size: IntSize = IntSize.Zero

    // start Node

    override fun onDetach() {
        // Clean up
        thisDragAndDropTarget = null
        lastChildDragAndDropModifierNode = null
    }

    // end Node

    // start LayoutAwareModifierNode

    override fun onRemeasured(size: IntSize) {
        this.size = size
    }

    // end LayoutAwareModifierNode

    // start DragAndDropSourceModifierNode

    override val isRequestDragAndDropTransferRequired: Boolean
        get() = dragAndDropManager.isRequestDragAndDropTransferRequired

    override fun requestDragAndDropTransfer(offset: Offset) {
        checkPrecondition(onStartTransfer != null)
        dragAndDropManager.requestDragAndDropTransfer(this, offset)
    }

    // end DragAndDropSourceModifierNode

    /**
     * Initiates a drag-and-drop operation for transferring data.
     *
     * @param offset the offset value representing position of the input pointer.
     * @param isTransferStarted a lambda function that returns true if the drag-and-drop transfer
     *   has started, or false otherwise.
     */
    fun DragAndDropStartTransferScope.startDragAndDropTransfer(
        offset: Offset,
        isTransferStarted: () -> Boolean
    ) {
        val nodeCoordinates = requireLayoutNode().coordinates
        traverseSelfAndDescendants { currentNode ->
            // TODO: b/303904810 unattached nodes should not be found from an attached
            //  root drag and drop node
            if (!currentNode.isAttached) {
                return@traverseSelfAndDescendants SkipSubtreeAndContinueTraversal
            }

            val onStartTransfer =
                currentNode.onStartTransfer ?: return@traverseSelfAndDescendants ContinueTraversal

            if (offset != Offset.Unspecified) {
                val currentCoordinates = currentNode.requireLayoutNode().coordinates
                val localPosition = currentCoordinates.localPositionOf(nodeCoordinates, offset)
                if (!currentNode.size.toSize().toRect().contains(localPosition)) {
                    return@traverseSelfAndDescendants ContinueTraversal
                }

                onStartTransfer.invoke(this, localPosition)
            } else {
                onStartTransfer.invoke(this, Offset.Unspecified)
            }

            if (isTransferStarted()) {
                CancelTraversal
            } else {
                ContinueTraversal
            }
        }
    }

    // start DragAndDropModifierNode

    @Deprecated("Use DragAndDropSourceModifierNode.requestDragAndDropTransfer instead")
    override fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ) {
        checkPrecondition(onStartTransfer == null)
        onStartTransfer = {
            startDragAndDropTransfer(transferData, decorationSize, drawDragDecoration)
        }
        dragAndDropManager.requestDragAndDropTransfer(this, Offset.Unspecified)
        onStartTransfer = null
    }

    /**
     * The entry point to register interest in a drag and drop session for receiving data.
     *
     * @return true to indicate interest in the contents of a drag and drop session, false indicates
     *   no interest. If false is returned, this [Modifier] will not receive any [DragAndDropTarget]
     *   events.
     */
    override fun acceptDragAndDropTransfer(startEvent: DragAndDropEvent): Boolean {
        var handled = false
        traverseSelfAndDescendants { currentNode ->
            // TODO: b/303904810 unattached nodes should not be found from an attached
            //  root drag and drop node
            if (!currentNode.isAttached) {
                return@traverseSelfAndDescendants SkipSubtreeAndContinueTraversal
            }

            checkPrecondition(currentNode.thisDragAndDropTarget == null) {
                "DragAndDropTarget self reference must be null" +
                    " at the start of a drag and drop session"
            }

            // Start receiving events
            currentNode.thisDragAndDropTarget = currentNode.onDropTargetValidate?.invoke(startEvent)

            val accepted = currentNode.thisDragAndDropTarget != null
            if (accepted) {
                dragAndDropManager.registerTargetInterest(currentNode)
            }
            handled = handled || accepted
            ContinueTraversal
        }
        return handled
    }

    // end DragAndDropModifierNode

    // start DropTarget

    override fun onStarted(event: DragAndDropEvent) {
        when (val self = thisDragAndDropTarget) {
            null -> lastChildDragAndDropModifierNode?.onStarted(event = event)
            else -> self.onStarted(event = event)
        }
    }

    override fun onEntered(event: DragAndDropEvent) {
        when (val self = thisDragAndDropTarget) {
            null -> lastChildDragAndDropModifierNode?.onEntered(event = event)
            else -> self.onEntered(event = event)
        }
    }

    override fun onMoved(event: DragAndDropEvent) {
        val currentChildNode: DragAndDropNode? = lastChildDragAndDropModifierNode
        val newChildNode: DragAndDropNode? =
            when {
                // Moved within child.
                currentChildNode?.contains(event.positionInRoot) == true -> currentChildNode
                // Position is now outside active child, maybe it entered a different one.
                else ->
                    firstDescendantOrNull { child ->
                        // Only dispatch to children who previously accepted the onStart gesture
                        dragAndDropManager.isInterestedTarget(child) &&
                            child.contains(event.positionInRoot)
                    }
            }

        when {
            // Left us and went to a child.
            newChildNode != null && currentChildNode == null -> {
                newChildNode.dispatchEntered(event)
                thisDragAndDropTarget?.onExited(event = event)
            }
            // Left the child and returned to us.
            newChildNode == null && currentChildNode != null -> {
                thisDragAndDropTarget?.dispatchEntered(event)
                currentChildNode.onExited(event = event)
            }
            // Left one child and entered another.
            newChildNode != currentChildNode -> {
                newChildNode?.dispatchEntered(event)
                currentChildNode?.onExited(event = event)
            }
            // Stayed in the same child.
            newChildNode != null -> newChildNode.onMoved(event = event)
            // Stayed in us.
            else -> thisDragAndDropTarget?.onMoved(event = event)
        }

        this@DragAndDropNode.lastChildDragAndDropModifierNode = newChildNode
    }

    override fun onChanged(event: DragAndDropEvent) {
        when (val self = thisDragAndDropTarget) {
            null -> lastChildDragAndDropModifierNode?.onChanged(event = event)
            else -> self.onChanged(event = event)
        }
    }

    override fun onExited(event: DragAndDropEvent) {
        thisDragAndDropTarget?.onExited(event = event)
        lastChildDragAndDropModifierNode?.onExited(event = event)
        lastChildDragAndDropModifierNode = null
    }

    override fun onDrop(event: DragAndDropEvent): Boolean {
        return when (val currentChildDropTarget = lastChildDragAndDropModifierNode) {
            null -> thisDragAndDropTarget?.onDrop(event = event) ?: false
            else -> currentChildDropTarget.onDrop(event = event)
        }
    }

    override fun onEnded(event: DragAndDropEvent) = traverseSelfAndDescendants { currentNode ->
        // TODO: b/303904810 unattached nodes should not be found from an attached
        //  root drag and drop node
        if (!currentNode.node.isAttached) {
            return@traverseSelfAndDescendants SkipSubtreeAndContinueTraversal
        }
        currentNode.thisDragAndDropTarget?.onEnded(event = event)
        currentNode.thisDragAndDropTarget = null
        currentNode.lastChildDragAndDropModifierNode = null
        ContinueTraversal
    }
    // end DropTarget
}

private fun DragAndDropTarget.dispatchEntered(event: DragAndDropEvent) = run {
    // Notify of entry
    onEntered(event = event)
    // Start move
    onMoved(event = event)
}

/** Hit test for a [DragAndDropNode]. */
private fun DragAndDropNode.contains(positionInRoot: Offset): Boolean {
    if (!node.isAttached) return false
    val currentCoordinates = requireLayoutNode().coordinates
    if (!currentCoordinates.isAttached) return false

    val (x1, y1) = currentCoordinates.positionInRoot()

    // Use measured size instead of size from currentCoordinates because it might be different
    //  (eg if padding is applied)
    val x2 = x1 + size.width
    val y2 = y1 + size.height

    return positionInRoot.x in x1..x2 && positionInRoot.y in y1..y2
}

private fun <T : TraversableNode> T.traverseSelfAndDescendants(
    block: (T) -> TraverseDescendantsAction
) {
    if (block(this) != ContinueTraversal) return
    traverseDescendants(block)
}

private inline fun <T : TraversableNode> T.firstDescendantOrNull(
    crossinline predicate: (T) -> Boolean
): T? {
    // TODO: b/303904810 unattached nodes should not be found from an attached
    //  root drag and drop node
    if (!node.isAttached) return null
    var match: T? = null
    traverseDescendants { child ->
        if (predicate(child)) {
            match = child
            return@traverseDescendants CancelTraversal
        }
        ContinueTraversal
    }
    return match
}
