/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.pointerinput

import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputNode
import androidx.ui.core.isAttached

/**
 * Organizes pointers and the [PointerInputNode]s that they hit into a hierarchy such that
 * [PointerInputChange]s can be dispatched to the [PointerInputNode]s in a hierarchical fashion.
 */
internal class HitPathTracker {

    internal val root: Node = Node()

    /**
     * Associates [pointerId] to [pointerInputNodes] and tracks them.
     *
     * @param pointerId The id of the pointer that was hit tested against [PointerInputNode]s
     * @param pointerInputNodes The [PointerInputNode]s that were hit by [pointerId]
     */
    fun addHitPath(pointerId: Int, pointerInputNodes: List<PointerInputNode>) {
        var parent = root
        var merging = true
        eachPin@ for (pointerInputNode in pointerInputNodes) {
            if (merging) {
                val node = parent.children.find { it.pointerInputNode == pointerInputNode }
                if (node != null) {
                    node.pointerIds.add(pointerId)
                    parent = node
                    continue@eachPin
                } else {
                    merging = false
                }
            }
            val node = Node(pointerInputNode).apply {
                pointerIds.add(pointerId)
            }
            parent.children.add(node)
            parent = node
        }
    }

    /**
     * Dispatches [pointerInputChanges] through the hierarchy; first down the hierarchy, passing
     * [downPass] to each [PointerInputNode], and then up the hierarchy with [upPass] if [upPass]
     * is not null.
     */
    fun dispatchChanges(
        pointerInputChanges: List<PointerInputChange>,
        downPass: PointerEventPass,
        upPass: PointerEventPass? = null
    ): List<PointerInputChange> {
        val idToChangesMap = pointerInputChanges.associateTo(mutableMapOf()) {
            it.id to it
        }
        root.dispatchChanges(idToChangesMap, downPass, upPass)
        return idToChangesMap.values.toList()
    }

    /**
     * Removes [PointerInputNode]s that have been removed from the component hierarchy.
     */
    fun removeDetachedPointerInputNodes() {
        root.removeDetachedPointerInputNodes()
    }

    /**
     * Removes the [pointerId] and any [PointerInputNode]s that are no longer associated with any
     * remaining [pointerId].
     */
    fun removePointerId(pointerId: Int) {
        root.removePointerId(pointerId)
    }
}

internal class Node(
    val pointerInputNode: PointerInputNode? = null
) {
    val pointerIds: MutableSet<Int> = mutableSetOf()
    val children: MutableSet<Node> = mutableSetOf()

    fun dispatchChanges(
        pointerInputChanges: MutableMap<Int, PointerInputChange>,
        downPass: PointerEventPass,
        upPass: PointerEventPass?
    ) {
        // Filter for changes that are associated with pointer ids that are relevant to this node.
        val relevantChanges = if (pointerInputNode == null) {
            pointerInputChanges
        } else {
            pointerInputChanges.filterTo(mutableMapOf()) { entry ->
                pointerIds.contains(entry.key)
            }
        }

        // Invoke the pointer PointerInputNode's pointerInputHandler with the downPass and update
        // the change with the result.
        if (pointerInputNode != null) {
            for (entry in relevantChanges)
                entry.setValue(pointerInputNode.pointerInputHandler.invoke(entry.value, downPass))
        }

        // Call children recursively with the relevant changes.
        children.forEach { it.dispatchChanges(relevantChanges, downPass, upPass) }

        // Invoke the pointer PointerInputNode's pointerInputHandler with the upPass (if it exists)
        // and update the change with the result.
        if (pointerInputNode != null && upPass != null) {
            for (entry in relevantChanges)
                entry.setValue(pointerInputNode.pointerInputHandler.invoke(entry.value, upPass))
        }

        // Update the pointerInputChanges with those relevant to us, and return it.
        pointerInputChanges.putAll(relevantChanges)
    }

    fun removeDetachedPointerInputNodes() {
        children.removeAll {
            it.pointerInputNode != null && !it.pointerInputNode.isAttached()
        }
        children.forEach {
            it.removeDetachedPointerInputNodes()
        }
    }

    fun removePointerId(pointerId: Int) {
        children.forEach {
            it.pointerIds.remove(pointerId)
        }
        children.removeAll {
            it.pointerInputNode != null && it.pointerIds.isEmpty()
        }
        children.forEach {
            it.removePointerId(pointerId)
        }
    }

    override fun toString(): String {
        return "Node(pointerInputNode=$pointerInputNode, children=$children, " +
                "pointerIds=$pointerIds)"
    }
}