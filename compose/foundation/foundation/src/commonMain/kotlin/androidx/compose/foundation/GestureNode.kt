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
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.node.traverseChildren
import kotlin.jvm.JvmInline

/**
 * Creates a [DelegatableNode] that attaches [gestureConnection] to allow high level gesture
 * coordination. A [gestureConnection] can be used by nodes that perform input handling (e.g. use
 * [androidx.compose.ui.node.PointerInputModifierNode] to allow further communication between
 * complex high level gestures.
 *
 * Gesture Nodes should be used with nodes that are currently enabled and ready to recognize
 * gestures. In case the node is not enabled, a [GestureConnection] should not be attached. If a
 * gesture changes from enabled to disabled, the gestureNode should be detached (undelegated).
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
     * Provides information regarding the gesture recognition process for this connection. Because
     * all gestures are backed by the pointer input system, this state should be set as soon as the
     * gesture starts its recognition process (i.e. the initial pass). Similarly, resetting the
     * state should happen when the gesture finishes its recognition process (i.e. final pass)
     */
    val gestureState: GestureState
}

/*
 * Executes [block] for all ancestors with a registered [GestureConnection].
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will continue
 * (true = continue, false = cancel).
 */
@Suppress("UNCHECKED_CAST")
internal fun DelegatableNode.traverseAncestorGestures(block: (GestureConnection) -> Boolean) {
    traverseAncestors(GestureNode.TraverseKey) { node ->
        check(node is GestureNode) { "Node is not a GestureNode instance" }
        val connection = node.gestureConnection as? GestureConnection
        if (connection == null) {
            true
        } else {
            block(connection)
        }
    }
}

/*
 * Executes [block] for all children with a registered [GestureConnection].
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will continue
 * (true = continue, false = cancel).
 */
@Suppress("UNCHECKED_CAST")
internal fun DelegatableNode.traverseChildrenGestures(block: (GestureConnection) -> Boolean) {
    traverseChildren(GestureNode.TraverseKey) { node ->
        check(node is GestureNode) { "Node is not a GestureNode instance" }
        val connection = node.gestureConnection as? GestureConnection
        if (connection == null) {
            true
        } else {
            block(connection)
        }
    }
}

/** Represents a Node that interprets and process gesture data. */
private class GestureNode(val gestureConnection: GestureConnection) :
    TraversableNode, DelegatableNode, Modifier.Node() {
    override val traverseKey: Any
        get() = TraverseKey

    companion object TraverseKey
}

/**
 * Represents the current status of a high level gesture, if the gesture is enabled. No
 * [GestureConnection] should be attached in case a gesture is not enabled.
 */
@JvmInline
internal value class GestureState private constructor(private val status: String) {
    companion object {
        /** Gesture is enabled but no gesture is in progress. */
        val Idle = GestureState("idle")

        /** Gesture is waiting for a trigger condition (e.g. touch slop) */
        val Waiting = GestureState("waiting")

        /** Gesture is ongoing (e.g. dragging) */
        val Recognized = GestureState("recognized")
    }
}
