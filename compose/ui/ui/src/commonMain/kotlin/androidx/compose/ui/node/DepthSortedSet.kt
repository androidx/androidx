/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.node

/**
 * The set of [LayoutNode]s which orders items by their [LayoutNode.depth] and
 * allows modifications(additions and removals) while we iterate through it via [popEach].
 * While [LayoutNode] is added to the set it should always be:
 * 1) attached [LayoutNode.isAttached] == true
 * 2) maintaining the same [LayoutNode.depth]
 * as any of this modifications can break the comparator's contract which can cause
 * to not find the item in the tree set, which we previously added.
 */
internal class DepthSortedSet(
    private val extraAssertions: Boolean
) {
    // stores the depth used when the node was added into the set so we can assert it wasn't
    // changed since then. we need to enforce this as changing the depth can break the contract
    // used in comparator for building the tree in TreeSet.
    // Created and used only when extraAssertions == true
    private val mapOfOriginalDepth by lazy(LazyThreadSafetyMode.NONE) {
        mutableMapOf<LayoutNode, Int>()
    }
    private val DepthComparator: Comparator<LayoutNode> = object : Comparator<LayoutNode> {
        override fun compare(l1: LayoutNode, l2: LayoutNode): Int {
            val depthDiff = l1.depth.compareTo(l2.depth)
            if (depthDiff != 0) {
                return depthDiff
            }
            return l1.hashCode().compareTo(l2.hashCode())
        }
    }
    private val set = TreeSet(DepthComparator)

    fun contains(node: LayoutNode): Boolean {
        val contains = set.contains(node)
        if (extraAssertions) {
            check(contains == mapOfOriginalDepth.containsKey(node))
        }
        return contains
    }

    fun add(node: LayoutNode) {
        check(node.isAttached)
        if (extraAssertions) {
            val usedDepth = mapOfOriginalDepth[node]
            if (usedDepth == null) {
                mapOfOriginalDepth[node] = node.depth
            } else {
                check(usedDepth == node.depth)
            }
        }
        set.add(node)
    }

    fun remove(node: LayoutNode): Boolean {
        check(node.isAttached)
        val contains = set.remove(node)
        if (extraAssertions) {
            val usedDepth = mapOfOriginalDepth.remove(node)
            if (contains) {
                check(usedDepth == node.depth)
            } else {
                check(usedDepth == null)
            }
        }
        return contains
    }

    fun pop(): LayoutNode {
        val node = set.first()
        remove(node)
        return node
    }

    inline fun popEach(crossinline block: (LayoutNode) -> Unit) {
        while (isNotEmpty()) {
            val node = pop()
            block(node)
        }
    }

    fun isEmpty(): Boolean = set.isEmpty()

    @Suppress("NOTHING_TO_INLINE")
    inline fun isNotEmpty(): Boolean = !isEmpty()

    override fun toString(): String {
        return set.toString()
    }
}

internal class DepthSortedSetsForDifferentPasses(extraAssertions: Boolean) {
    private val lookaheadSet = DepthSortedSet(extraAssertions)
    private val set = DepthSortedSet(extraAssertions)

    /**
     * Checks if the given node exists in the corresponding set based on the provided
     * [affectsLookahead].
     */
    fun contains(node: LayoutNode, affectsLookahead: Boolean): Boolean {
        val constainsInLookahead = lookaheadSet.contains(node)
        return if (affectsLookahead) {
            constainsInLookahead
        } else {
            constainsInLookahead || set.contains(node)
        }
    }

    /**
     * Checks if the node exists in either set.
     */
    fun contains(node: LayoutNode): Boolean = lookaheadSet.contains(node) || set.contains(node)

    /**
     * Adds the given node to the corresponding set based on whether its lookahead
     * measurement/placement should be invalidated.
     *
     * Note: When [affectsLookahead] is true, both lookahead and main measure/layout will be
     * triggered as needed (i.e. if the FooPending flag is dirty). Otherwise, lookahead
     * remeasurement/relayout will be skipped.
     */
    fun add(node: LayoutNode, affectsLookahead: Boolean) {
        if (affectsLookahead) {
            lookaheadSet.add(node)
        } else {
            if (!lookaheadSet.contains(node)) {
                // Only add the node to set if it's not already in the lookahead set. Nodes in
                // lookaheadSet will get a remeasure/relayout call after lookahead.
                set.add(node)
            }
        }
    }

    fun remove(node: LayoutNode, affectsLookahead: Boolean): Boolean {
        val contains = if (affectsLookahead) {
            lookaheadSet.remove(node)
        } else {
            set.remove(node)
        }
        return contains
    }

    fun remove(node: LayoutNode): Boolean {
        val containsInLookahead = lookaheadSet.remove(node)
        return set.remove(node) || containsInLookahead
    }

    fun pop(): LayoutNode {
        if (lookaheadSet.isNotEmpty()) {
            return lookaheadSet.pop()
        }
        return set.pop()
    }

    /**
     * Pops nodes that require lookahead remeasurement/replacement first until the lookaheadSet
     * is empty, before handling nodes that only require invalidation for the main pass.
     */
    inline fun popEach(crossinline block: (node: LayoutNode, affectsLookahead: Boolean) -> Unit) {
        while (isNotEmpty()) {
            val affectsLookahead = lookaheadSet.isNotEmpty()
            val node = if (affectsLookahead) lookaheadSet.pop() else set.pop()
            block(node, affectsLookahead)
        }
    }

    fun isEmpty(): Boolean = set.isEmpty() && lookaheadSet.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()
}
