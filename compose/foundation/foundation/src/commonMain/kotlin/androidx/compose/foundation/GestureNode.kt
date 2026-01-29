/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.traverseAncestors

/**
 * Creates a [DelegatableNode] that attaches [gestureConnection] to allow high level gesture
 * coordination. A [gestureConnection] can be used by nodes that perform input handling (e.g. use
 * [androidx.compose.ui.node.PointerInputModifierNode] to allow further communication between
 * complex high level gestures.
 *
 * @param gestureConnection The [GestureConnection] to attach to the nodes tree.
 */
internal fun gestureNode(gestureConnection: GestureConnection): DelegatableNode =
    GestureNode(gestureConnection)

/**
 * Allows high level gesture coordination between gesture handlers (e.g. clicks, drags, scrolls).
 */
internal interface GestureConnection {
    /**
     * Allows this connection to demonstrate interest over a pointer event change. Interest signals
     * that a connection is interested in this event, and it needs additional information (e.g. time
     * or more events) in order to decide if it will consume it. At a given moment in a gesture
     * handler recognition process they can query their parent to see if they're also interested in
     * this specific change. If so they can decide to give priority to the other gestures in the
     * chain.
     *
     * @param event The [PointerInputChange] this connection will react to.
     */
    fun isInterested(event: PointerInputChange): Boolean = false

    /**
     * Allows this connection to demonstrate interest over an indirect pointer event change.
     * Interest signals that a connection is interested in this event, and it needs additional
     * information (e.g. time or more events) in order to decide if it will consume it. At a given
     * moment in a gesture handler recognition process they can query their parent to see if they're
     * also interested in this specific change. If so they can decide to give priority to the other
     * gestures in the chain.
     *
     * @param event The [IndirectPointerInputChange] this connection will react to.
     */
    fun isInterested(event: IndirectPointerInputChange): Boolean = false
}

/** Searches the tree for a parent [GestureConnection]. Returns null if there isn't one. */
internal val DelegatableNode.parentGestureConnection: GestureConnection?
    get() = (findNearestAncestor(GestureNode.TraverseKey) as? GestureNode)?.gestureConnection

/**
 * Executes [block] for all ancestors with a registered [GestureConnection].
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will continue
 * (true = continue, false = cancel).
 */
internal fun DelegatableNode.traverseAncestorGestureConnections(
    block: (GestureConnection) -> Boolean
) {
    traverseAncestors(GestureNode.TraverseKey) { node ->
        check(node is GestureNode) { "Node is not a GestureNode instance" }
        block(node.gestureConnection)
    }
}

/** Represents a Node that interprets and process gesture data. */
private class GestureNode(val gestureConnection: GestureConnection) :
    TraversableNode, DelegatableNode, Modifier.Node() {
    override val traverseKey: Any
        get() = TraverseKey

    companion object TraverseKey
}
