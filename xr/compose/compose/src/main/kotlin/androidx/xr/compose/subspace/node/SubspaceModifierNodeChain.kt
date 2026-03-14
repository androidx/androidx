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
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.SubspacePlaceable
import androidx.xr.compose.subspace.layout.requireCoordinator
import androidx.xr.compose.unit.VolumeConstraints

private val SentinelHead =
    object : SubspaceModifier.Node() {
        override fun toString() = "<Head>"
    }

// Phase values passed to autoInvalidateNode.
private const val Insert = 0
private const val Update = 1
private const val Remove = 2

/** See [androidx.compose.ui.node.NodeChain] */
internal class SubspaceModifierNodeChain(private val subspaceLayoutNode: SubspaceLayoutNode) {
    private var current: MutableList<SubspaceModifier>? = null
    private var buffer: MutableList<SubspaceModifier>? = null
    internal val tail: SubspaceModifier.Node = subspaceLayoutNode.measurableLayout.tail
    internal var head: SubspaceModifier.Node = tail
    private var inMeasurePass: Boolean = false

    private val logger: Logger?
        get() = subspaceLayoutNode.owner?.logger

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
        var before = current
        val beforeSize = before?.size ?: 0
        val after = modifier.fillVector(buffer ?: mutableListOf())
        var i = 0

        if (beforeSize == 0) {
            // Common case where we are initializing the chain or the previous size is zero.
            var node = paddedHead
            while (i < after.size) {
                val next = after[i]
                val parent = node
                node = createAndInsertNodeAsChild(next, parent, i)
                i++
            }
        } else if (after.size == 0) {
            // Common case where we are removing all the modifiers.
            var node = paddedHead.child
            while (node != null && i < beforeSize) {
                logger?.nodeRemoved(node, subspaceLayoutNode, i)
                node = removeNode(node).child
                i++
            }
        } else if (after.size == beforeSize) {
            // Find the diffs between before and after sets.
            var node = paddedHead

            // First match as many same-type modifiers at the beginning of the lists.
            checkNotNull(before) { "prior modifier list should be non-empty" }
            while (i < beforeSize && i < after.size && before[i]::class == after[i]::class) {
                node = checkNotNull(node.child) { "child should not be null" }
                if (before[i] != after[i]) {
                    logger?.nodeUpdated(node, subspaceLayoutNode, i)
                    updateNode(node, after[i])
                }
                i++
            }

            // Uncommon case - in the same position of same size lists a new modifier type was
            // detected.
            // Structurally update the rest of the list.
            if (i < beforeSize) {
                structuralUpdate(i, before, after, node)
            }
        } else {
            // Uncommon case - sizes are different, a structural update will be necessary to
            // construct the
            // new list.
            before = before ?: mutableListOf()
            structuralUpdate(0, before, after, paddedHead)
        }

        current = after
        // Clear the before vector to allow old modifiers to be Garbage Collected.
        buffer = before?.also { it.clear() }
        head = trimChain()
        syncAggregateChildKindSet()
    }

    /** Finds the first node of the given [SubspaceNodeKind] in the chain (closest to the head). */
    internal inline fun <reified T> firstOf(type: SubspaceNodeKind<T>): T? {
        if (head.aggregateChildKindSet and type.mask == 0) return null

        var node: SubspaceModifier.Node? = head
        while (node != null) {
            if (node.kindSet and type.mask != 0) {
                if (node is T) return node
            }
            node = node.child
        }
        return null
    }

    /** Finds the last node of the given [SubspaceNodeKind] in the chain (closest to the tail). */
    internal inline fun <reified T> lastOf(type: SubspaceNodeKind<T>): T? {
        if (head.aggregateChildKindSet and type.mask == 0) return null

        // Start from tail and walk up
        var node: SubspaceModifier.Node? = tail
        while (node != null) {
            if (node.kindSet and type.mask != 0) {
                if (node is T) return node
            }
            node = node.parent
        }
        return null
    }

    /** Iterates nodes of the given [SubspaceNodeKind] from Head to Tail. */
    internal inline fun <reified T> forEachOf(type: SubspaceNodeKind<T>, block: (T) -> Unit) {
        if (head.aggregateChildKindSet and type.mask == 0) return

        var node: SubspaceModifier.Node? = head
        while (node != null) {
            if (node.kindSet and type.mask != 0) {
                if (node is T) block(node)
            }
            node = node.child
        }
    }

    /** Executes [block] for every modifier in the chain starting at the head. */
    internal inline fun headToTail(block: (SubspaceModifier.Node) -> Unit) {
        var node: SubspaceModifier.Node? = head
        while (node != null) {
            block(node)
            node = node.child
        }
    }

    /** Executes [block] for every modifier in the chain starting at the tail. */
    internal inline fun tailToHead(block: (SubspaceModifier.Node) -> Unit) {
        var node: SubspaceModifier.Node? = tail
        while (node != null) {
            block(node)
            node = node.parent
        }
    }

    /**
     * Marks all nodes in the chain as attached to a
     * [androidx.xr.compose.subspace.layout.SubspaceLayout].
     *
     * This should be called *before* [runOnAttach] is called. We check that the node is not already
     * attached as this method may be called more than necessary to ensure proper state.
     */
    internal fun markAsAttached() = headToTail { if (!it.isAttached) it.markAsAttached() }

    internal fun runOnAttach() = headToTail { if (it.isAttached) it.onAttach() }

    /**
     * Marks all nodes in the chain as detached from a
     * [androidx.xr.compose.subspace.layout.SubspaceLayout].
     *
     * This should be called *after* [runOnDetach] is called. We check that the node is attached as
     * this method may be called more than necessary to ensure proper state.
     */
    internal fun markAsDetached() = tailToHead { if (it.isAttached) it.markAsDetached() }

    internal fun runOnDetach() = tailToHead { if (it.isAttached) it.onDetach() }

    internal fun measureChain(constraints: VolumeConstraints): SubspacePlaceable? {
        val layoutNode = firstOf(SubspaceNodes.Layout)
        if (layoutNode == null || inMeasurePass) {
            inMeasurePass = false
            return null
        }
        try {
            inMeasurePass = true
            return layoutNode.requireCoordinator().measure(constraints)
        } finally {
            inMeasurePass = false
        }
    }

    internal fun invalidateCompositionLocals() {
        forEachOf(SubspaceNodes.Locals) { autoInvalidateNode(it as SubspaceModifier.Node, Update) }
    }

    private fun createAndInsertNodeAsChild(
        element: SubspaceModifier,
        parent: SubspaceModifier.Node,
        index: Int,
    ): SubspaceModifier.Node {
        val node = (element as SubspaceModifierNodeElement<*>).create()
        node.layoutNode = subspaceLayoutNode
        logger?.nodeInserted(node, subspaceLayoutNode, index)
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
        autoInvalidateNode(node, Insert)
        return node
    }

    private fun updateNode(node: SubspaceModifier.Node, modifier: SubspaceModifier) {
        (modifier as SubspaceModifierNodeElement<*>).updateUnsafe(node)
        autoInvalidateNode(node, Update)
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
        autoInvalidateNode(node, Remove)
        if (node.isAttached) {
            node.onDetach()
            node.markAsDetached()
        }
        return parent!!
    }

    private fun autoInvalidateNode(node: SubspaceModifier.Node, phase: Int) {
        if (!node.isAttached) return
        if (phase == Update && !node.shouldAutoInvalidate) return

        val selfKindSet = calculateSubspaceNodeKindSetFrom(node)

        if (SubspaceNodes.Layout in selfKindSet) {
            if (phase == Update) {
                subspaceLayoutNode.requestMeasure()
            } else {
                subspaceLayoutNode.parent?.requestMeasure()
            }
        }

        if (SubspaceNodes.ParentData in selfKindSet) {
            subspaceLayoutNode.parent?.requestMeasure()
        }

        if (SubspaceNodes.CoreEntity in selfKindSet) {
            subspaceLayoutNode.requestEntityUpdate()
        }

        if (SubspaceNodes.LayoutAware in selfKindSet) {
            if (phase != Remove) {
                // TODO(mrw): Don't do a full relayout, just dispatch the callbacks.
                subspaceLayoutNode.requestLayout()
            }
        }
    }

    private fun syncAggregateChildKindSet() {
        var node: SubspaceModifier.Node? = tail.parent

        while (node != null) {
            val newAggregate = node.kindSet or (node.child?.aggregateChildKindSet ?: 0)

            // If this node's aggregate type has not changed, we do not
            // need to continue updating its parent's aggregate types.
            if (newAggregate == node.aggregateChildKindSet) {
                break
            }

            node.aggregateChildKindSet = newAggregate
            node = node.parent
        }
    }

    /**
     * This method utilizes a modified Myers Diff Algorithm which will diff the two modifier chains
     * and execute a minimal number of insertions/deletions. We make no attempt to execute "moves"
     * as part of this diff. If a modifier moves that is no different than it being inserted in the
     * new location and removed in the old location.
     *
     * @param tail - The Node that corresponds to the _end_ of the [before] list.
     */
    private fun structuralUpdate(
        offset: Int,
        before: MutableList<SubspaceModifier>,
        after: MutableList<SubspaceModifier>,
        tail: SubspaceModifier.Node,
    ) {
        val differ = getDiffer(tail, offset, before, after)
        executeDiff(before.size - offset, after.size - offset, differ)
    }

    private var cachedDiffer: Differ? = null

    private fun getDiffer(
        head: SubspaceModifier.Node,
        offset: Int,
        before: MutableList<SubspaceModifier>,
        after: MutableList<SubspaceModifier>,
    ): Differ {
        val current = cachedDiffer
        @Suppress("IfThenToElvis")
        return if (current == null) {
            Differ(head, offset, before, after).also { cachedDiffer = it }
        } else {
            current.also {
                it.node = head
                it.offset = offset
                it.before = before
                it.after = after
            }
        }
    }

    private inner class Differ(
        var node: SubspaceModifier.Node,
        var offset: Int,
        var before: MutableList<SubspaceModifier>,
        var after: MutableList<SubspaceModifier>,
    ) : DiffCallback {
        override fun areItemsTheSame(oldIndex: Int, newIndex: Int): Boolean {
            val prev = before[offset + oldIndex]
            val next = after[offset + newIndex]
            return prev == next || prev::class == next::class
        }

        override fun insert(newIndex: Int) {
            node = createAndInsertNodeAsChild(after[offset + newIndex], node, offset + newIndex)
        }

        override fun remove(atIndex: Int, oldIndex: Int) {
            logger?.nodeRemoved(node, subspaceLayoutNode, atIndex)
            node = removeNode(node.child!!)
        }

        override fun same(oldIndex: Int, newIndex: Int) {
            node = node.child!!
            val prev = before[offset + oldIndex]
            val next = after[offset + newIndex]
            if (oldIndex != newIndex) {
                logger?.nodeMoved(node, subspaceLayoutNode, oldIndex, newIndex)
            }
            if (prev != next) {
                logger?.nodeUpdated(node, subspaceLayoutNode, newIndex)
                updateNode(node, next)
            }
        }
    }
}

private fun <T : SubspaceModifier.Node> SubspaceModifierNodeElement<T>.updateUnsafe(
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
            is SubspaceModifierNodeElement<*> -> result.add(next as SubspaceModifier)

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
