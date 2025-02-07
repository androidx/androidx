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

package androidx.xr.compose.subspace.node

import androidx.xr.compose.subspace.layout.CombinedSubspaceModifier
import androidx.xr.compose.subspace.layout.Placeable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.findInstance
import androidx.xr.compose.subspace.layout.traverseSelfThenAncestors
import androidx.xr.compose.subspace.layout.traverseSelfThenDescendants
import androidx.xr.compose.unit.VolumeConstraints

private val SentinelHead =
    object : SubspaceModifier.Node() {
        override fun toString() = "<Head>"
    }

/** See [androidx.compose.ui.node.NodeChain] */
internal class SubspaceModifierNodeChain(private val subspaceLayoutNode: SubspaceLayoutNode) {
    private var current: MutableList<SubspaceModifier>? = null
    private var buffer: MutableList<SubspaceModifier>? = null
    internal val tail: SubspaceModifier.Node = subspaceLayoutNode.measurableLayout.tail
    internal var head: SubspaceModifier.Node = tail
    private var inMeasurePass: Boolean = false

    private fun padChain(): SubspaceModifier.Node {
        val currentHead = head
        currentHead.parent = SentinelHead
        SentinelHead.child = currentHead
        return SentinelHead
    }

    private fun trimChain(): SubspaceModifier.Node {
        val result = SentinelHead.child ?: tail
        result.parent = null
        SentinelHead.child = null
        return result
    }

    internal fun updateFrom(modifier: SubspaceModifier) {
        val paddedHead = padChain()
        val before = current
        val beforeSize = before?.size ?: 0
        val after = modifier.fillVector(buffer ?: mutableListOf())
        var i = 0

        if (beforeSize == 0) {
            // Common case where we are initializing the chain or the previous size is zero.
            var node = paddedHead
            while (i < after.size) {
                val next = after[i]
                val parent = node
                node = createAndInsertNodeAsChild(next, parent)
                i++
            }
        } else if (after.size == 0) {
            // Common case where we are removing all the modifiers.
            var node = paddedHead.child
            while (node != null && i < beforeSize) {
                node = removeNode(node).child
                i++
            }
        } else {
            // Find the diffs between before and after sets. This is not as complex as base Compose
            // which
            // does a full diff. Revisit this if we see any performance issues with dynamic
            // modifiers.
            var node = paddedHead

            // First match as many same-type modifiers at the beginning of the lists.
            checkNotNull(before) { "prior modifier list should be non-empty" }
            while (i < beforeSize && i < after.size && before[i]::class == after[i]::class) {
                node = checkNotNull(node.child) { "child should not be null" }
                if (before[i] != after[i]) {
                    updateNode(node, after[i])
                }
                i++
            }

            // Then remove the remaining existing modifiers when we have a structural change.
            var nodeToDelete = node.child
            var beforeIndex = i
            while (nodeToDelete != null && beforeIndex < beforeSize) {
                nodeToDelete = removeNode(nodeToDelete).child
                beforeIndex++
            }

            // Finally add the remaining new modifiers.
            while (i < after.size) {
                val next = after[i]
                val parent = node
                node = createAndInsertNodeAsChild(next, parent)
                i++
            }
        }

        current = after
        // Clear the before vector to allow old modifiers to be Garbage Collected.
        buffer = before?.also { it.clear() }
        head = trimChain()
    }

    /**
     * Marks all nodes in the chain as attached to a [SubspaceLayout].
     *
     * This should be called *before* [runOnAttach] is called. We check that the node is not already
     * attached as this method may be called more than necessary to ensure proper state.
     */
    internal fun markAsAttached() = headToTail { if (!it.isAttached) it.markAsAttached() }

    internal fun runOnAttach() = headToTail { if (it.isAttached) it.onAttach() }

    /**
     * Marks all nodes in the chain as detached from a [SubspaceLayout].
     *
     * This should be called *after* [runOnDetach] is called. We check that the node is attached as
     * this method may be called more than necessary to ensure proper state.
     */
    internal fun markAsDetached() = tailToHead { if (it.isAttached) it.markAsDetached() }

    internal fun runOnDetach() = tailToHead { if (it.isAttached) it.onDetach() }

    internal fun measureChain(
        constraints: VolumeConstraints,
        wrappedMeasureBlock: (VolumeConstraints) -> Placeable,
    ): Placeable {
        val layoutNode = getAll<SubspaceLayoutModifierNode>().firstOrNull()
        if (layoutNode == null || inMeasurePass) {
            inMeasurePass = false
            return wrappedMeasureBlock(constraints)
        }
        inMeasurePass = true
        val placeable = layoutNode.coordinator.measure(constraints)
        inMeasurePass = false
        return placeable
    }

    /** Executes [block] for each modifier in the chain starting at the head. */
    internal inline fun headToTail(block: (SubspaceModifier.Node) -> Unit) =
        head.traverseSelfThenDescendants().forEach(block)

    /** Executes [block] for each modifier in the chain starting at the tail. */
    internal inline fun tailToHead(block: (SubspaceModifier.Node) -> Unit) =
        tail.traverseSelfThenAncestors().forEach(block)

    /** Returns all nodes of the given type in the chain in the declared modifier order. */
    internal inline fun <reified T> getAll(): Sequence<T> =
        head.traverseSelfThenDescendants().filterIsInstance<T>()

    /**
     * Returns the last node of the given type in the chain if it exists, null otherwise.
     *
     * When considering only one instance of a modifier type, prefer the last instance.
     */
    internal inline fun <reified T> getLast(): T? =
        tail.traverseSelfThenAncestors().findInstance<T>()

    private fun createAndInsertNodeAsChild(
        element: SubspaceModifier,
        parent: SubspaceModifier.Node,
    ): SubspaceModifier.Node {
        val node = (element as SubspaceModifierElement<*>).create()
        if (node is SubspaceLayoutModifierNode) {
            node.coordinator?.layoutNode = subspaceLayoutNode
        }
        return insertChild(node, parent)
    }

    private fun insertChild(
        node: SubspaceModifier.Node,
        parent: SubspaceModifier.Node,
    ): SubspaceModifier.Node {
        val theChild = parent.child
        if (theChild != null) {
            theChild.parent = node
            node.child = theChild
        }
        parent.child = node
        node.parent = parent
        if (!node.isAttached) {
            node.markAsAttached()
            node.onAttach()
        }
        return node
    }

    private fun updateNode(node: SubspaceModifier.Node, modifier: SubspaceModifier) {
        (modifier as SubspaceModifierElement<*>).updateUnsafe(node)
    }

    private fun removeNode(node: SubspaceModifier.Node): SubspaceModifier.Node {
        val child = node.child
        val parent = node.parent
        if (child != null) {
            child.parent = parent
            node.child = null
        }
        if (parent != null) {
            parent.child = child
            node.parent = null
        }
        if (node.isAttached) {
            node.onDetach()
            node.markAsDetached()
        }
        return parent!!
    }
}

private fun <T : SubspaceModifier.Node> SubspaceModifierElement<T>.updateUnsafe(
    node: SubspaceModifier.Node
) {
    @Suppress("UNCHECKED_CAST") update(node as T)
}

private fun SubspaceModifier.fillVector(
    result: MutableList<SubspaceModifier>
): MutableList<SubspaceModifier> {
    val capacity = result.size.coerceAtLeast(16)
    val stack = ArrayList<SubspaceModifier>(capacity).also { it.add(this) }
    var predicate: ((SubspaceModifier) -> Boolean)? = null
    while (stack.isNotEmpty()) {
        when (val next = stack.removeAt(stack.size - 1)) {
            is CombinedSubspaceModifier -> {
                stack.add(next.inner)
                stack.add(next.outer)
            }
            is SubspaceModifierElement<*> -> result.add(next as SubspaceModifier)

            // some other [androidx.compose.ui.node.Modifier] implementation that we don't know
            // about...
            // late-allocate the predicate only once for the entire stack
            else ->
                next.all(
                    predicate
                        ?: { element: SubspaceModifier ->
                                result.add(element)
                                true
                            }
                            .also { predicate = it }
                )
        }
    }
    return result
}
