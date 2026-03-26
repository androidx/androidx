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

package androidx.compose.foundation.text.input.internal

import androidx.collection.MutableIntList
import androidx.collection.MutableLongList
import androidx.collection.mutableLongListOf
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.math.min

/**
 * An [IntIntervalTree] implemented as a red-black tree that holds a list of intervals and allows
 * for fast queries of intervals that overlap any given range. It is also order-aware; intervals are
 * returned in the order they were added.
 *
 * Instead of creating an object-based tree, this implementation utilizes two lists, [nodeInfo] and
 * [items], to store the tree structure. [nodeInfo] is a [MutableLongList] that stores each [Node]'s
 * start and end range, the subtree's min/max range, along with parent, left, and right child [Node]
 * indices.
 *
 * [MutableLongList] is used to store packed pairs of [Int] values. This provides better performance
 * than [MutableIntList] because it reduces the number of array boundary checks (one check per two
 * [Int] fields).
 *
 * [Node]s are stored in [nodeInfo] with a stride of 4 [Long] entries: `[packInts(color, parent),
 * packInts(left, right), packInts(start, end), packInts(min, max)]`. The corresponding data for
 * each node is stored in the [items] list at `node.index / STRIDE`. The [Node] is a value class
 * that wraps an index pointing to the start of its information in the [nodeInfo] list.
 *
 * This approach provides two key benefits:
 * 1) The order in which intervals are added is preserved by the order in which nodes are added to
 *    the [nodeInfo] list. This makes it highly efficient to return all styles in their original
 *    added order, which is frequently required by the text renderer.
 * 2) It significantly improves the copy performance of the [IntIntervalTree]. Unlike an object-tree
 *    implementation that must traverse the entire tree and allocate new objects, this
 *    implementation only needs to copy the [nodeInfo] and [items] lists.
 *
 * This data structure is **NOT** thread-safe and is not intended to be called from multiple
 * threads.
 *
 * @param source The [IntIntervalTree] to copy from.
 */
internal class IntIntervalTree<T>(source: IntIntervalTree<T>? = null) {
    companion object {

        /**
         * If the node size is less than this threshold, we won't call [cleanDeletedNodes] for
         * better performance.
         */
        private const val NODE_CLEANUP_SIZE_THRESHOLD = 64

        private const val COLOR_PARENT = 0
        private const val LEFT_RIGHT = 1
        private const val START_END = 2
        private const val MIN_MAX = 3
        private const val STRIDE = 4
    }

    /**
     * The color of this [Node], it can be [TreeColorRed], [TreeColorBlack] or [TreeColorDeleted]
     * representing that this [Node] has been marked as deleted.
     */
    var Node.color: TreeColor
        get() = unpackInt1(nodeInfo[index + COLOR_PARENT])
        set(value) {
            nodeInfo[index + COLOR_PARENT] =
                packInts(value, unpackInt2(nodeInfo[index + COLOR_PARENT]))
        }

    /** Parent [Node] of this [Node]. */
    var Node.parent: Node
        get() = Node(unpackInt2(nodeInfo[index + COLOR_PARENT]))
        set(value) {
            nodeInfo[index + COLOR_PARENT] =
                packInts(unpackInt1(nodeInfo[index + COLOR_PARENT]), value.index)
        }

    /** Left child [Node] of this [Node]. */
    var Node.left: Node
        get() = Node(unpackInt1(nodeInfo[index + LEFT_RIGHT]))
        set(value) {
            nodeInfo[index + LEFT_RIGHT] =
                packInts(value.index, unpackInt2(nodeInfo[index + LEFT_RIGHT]))
        }

    /** Right child [Node] of this [Node]. */
    var Node.right: Node
        get() = Node(unpackInt2(nodeInfo[index + LEFT_RIGHT]))
        set(value) {
            nodeInfo[index + LEFT_RIGHT] =
                packInts(unpackInt1(nodeInfo[index + LEFT_RIGHT]), value.index)
        }

    /** The start index of the interval corresponding to this [Node]. */
    var Node.start: Int
        get() = unpackInt1(nodeInfo[index + START_END])
        set(value) {
            nodeInfo[index + START_END] = packInts(value, unpackInt2(nodeInfo[index + START_END]))
        }

    /** The end index of the interval corresponding to this [Node]. */
    var Node.end: Int
        get() = unpackInt2(nodeInfo[index + START_END])
        set(value) {
            nodeInfo[index + START_END] = packInts(unpackInt1(nodeInfo[index + START_END]), value)
        }

    /** The minimum of the [start] indices of this [Node]'s subtree. */
    var Node.min: Int
        get() = unpackInt1(nodeInfo[index + MIN_MAX])
        set(value) {
            nodeInfo[index + MIN_MAX] = packInts(value, unpackInt2(nodeInfo[index + MIN_MAX]))
        }

    /** The maximum of the [end] indices of this [Node]'s subtree. */
    var Node.max: Int
        get() = unpackInt2(nodeInfo[index + MIN_MAX])
        set(value) {
            nodeInfo[index + MIN_MAX] = packInts(unpackInt1(nodeInfo[index + MIN_MAX]), value)
        }

    /** The data associated with this [Node]. */
    private val Node.item: T?
        get() = items[index / STRIDE]

    /**
     * Returns the lowest [Node] in the subtree of this [Node], or the node itself if it doesn't
     * have a left child.
     */
    fun Node.lowestNode(): Node {
        var node = this
        while (node.left != terminator) {
            node = node.left
        }
        return node
    }

    /** Returns the next [Node] in the inorder traversal of the subtree of this [Node]. */
    fun Node.next(): Node {
        if (right != terminator) {
            return right.lowestNode()
        }

        var current = this
        var parent = this.parent
        while (parent != terminator && current == parent.right) {
            current = parent
            parent = parent.parent
        }

        return parent
    }

    /**
     * Returns true if this interval overlaps with the interval defined by [start] and [end].
     * [start] must be less than or equal to [end]. The overlap is inclusive on [start] but
     * exclusive on [end].
     */
    fun Node.overlaps(start: Int, end: Int) = intersect(start, end, this.start, this.end)

    /** Creates a new [Node] with the given [start], [end], [item] and [color]. */
    private fun Node(start: Int, end: Int, item: T?, color: Int = TreeColorRed): Node {
        val index = nodeInfo.size
        // color, parent(terminator at index 0 by default)
        nodeInfo.add(packInts(color, 0))
        // left, right both pointing to the terminator(index 0) by default.
        nodeInfo.add(0)
        // start, end
        nodeInfo.add(packInts(start, end))
        // min, max
        nodeInfo.add(packInts(start, end))
        items.add(item)
        return Node(index)
    }

    private val items: MutableList<T?>
    private val nodeInfo: MutableLongList

    /**
     * The total number of nodes allocated in [nodeInfo], including nodes marked as deleted but not
     * yet removed. The [terminator] is also included.
     */
    private val totalNodeCount: Int
        get() = nodeInfo.size / STRIDE

    /** The number of nodes marked for deletion but not yet removed from [nodeInfo]. */
    private var deletedNodeCount: Int

    /** The root [Node] of this [IntIntervalTree]. */
    var root: Node

    /**
     * A sentinel node that represents a null leaf. It helps keep the code clean and avoids branch
     * misses (using null introduces many if/else branches).
     *
     * More details can be found in [rebalanceAfterInsertion] and [rebalanceAfterDeletion], where we
     * need to check the colors of uncle and sibling nodes, which may be the [terminator].
     *
     * Note that the [terminator]'s parent, left, and right pointers are not meaningful as it is a
     * shared sentinel node.
     */
    val terminator: Node

    private var _tempArray: NodeList? = null
    private val tempArray
        get() = _tempArray ?: NodeList().also { _tempArray = it }

    init {
        if (source != null) {
            items = source.items.toMutableList()
            nodeInfo = MutableLongList(source.nodeInfo.size).also { it.addAll(source.nodeInfo) }
            terminator = source.terminator
            root = source.root
            deletedNodeCount = source.deletedNodeCount
        } else {
            items = mutableListOf()
            nodeInfo = mutableLongListOf()
            terminator = Node(Int.MAX_VALUE, Int.MIN_VALUE, null, TreeColorBlack)
            root = terminator
            deletedNodeCount = 0
        }
    }

    /**
     * Calls [block] for each the interval that overlaps with the range defined by [start] and
     * [end], in the order they are added. The overlap is inclusive on [start] and exclusive at
     * [end].
     */
    fun forEachIntervalInRange(
        start: Int,
        end: Int,
        block: (item: T, start: Int, end: Int) -> Unit,
    ) {
        val nodes = tempArray
        forEachNodeInRange(start, end) { nodes.add(it) }
        nodes.sort()

        nodes.forEach {
            val node = Node(it)
            val item = node.item
            if (item != null) {
                block(item, node.start, node.end)
            }
        }
        nodes.clear()
    }

    /**
     * Calls [block] for all intervals stored in this [IntIntervalTree] in the order they were
     * added. This method returns the same result as [forEachIntervalInRange] with full range but is
     * optimized to be faster, especially when a large number of intervals are stored.
     */
    fun forAllIntervals(block: (item: T, start: Int, end: Int) -> Unit) {
        if (root == terminator) return
        // Instead of traverse the entire tree. We directly compare the nodeInfo array, which is
        // the intervals sorted in the adding order.
        // Ignore the first node, which is the terminator
        var nodeIndex = STRIDE
        while (nodeIndex < nodeInfo.size) {
            val node = Node(nodeIndex)
            if (node.color != TreeColorDeleted) {
                val start = node.start
                val end = node.end
                val item = node.item
                if (item != null) {
                    block(item, start, end)
                }
            }
            nodeIndex += STRIDE
        }
    }

    /** Clears this tree and prepares it for reuse. */
    fun clear() {
        root = terminator
        // Don't directly call clear which will also remove the terminator.
        nodeInfo.removeRange(STRIDE, nodeInfo.size)
        items.subList(1, items.size).clear()
        deletedNodeCount = 0
    }

    /** Remove the nodes that's marked to be deleted from the [nodeInfo]. */
    private fun cleanDeletedNodes() {
        if (deletedNodeCount == 0) return

        var deletedSoFar = 0
        val mapping = tempArray
        mapping.ensureCapacity(totalNodeCount)
        for (node in 0 until totalNodeCount) {
            if (unpackInt1(nodeInfo[node * STRIDE + COLOR_PARENT]) == TreeColorDeleted) {
                deletedSoFar++
            }
            mapping.add((node - deletedSoFar) * STRIDE)
        }

        fun map(index: Int) = mapping[index / STRIDE]

        root = Node(map(root.index))

        // The first node is terminator, ignore it in the loop.
        var nodeIndex = STRIDE
        var targetIndex = STRIDE
        while (nodeIndex < nodeInfo.size) {
            if (unpackInt1(nodeInfo[nodeIndex + COLOR_PARENT]) == TreeColorDeleted) {
                nodeIndex += STRIDE
                continue
            }

            if (targetIndex != nodeIndex) {
                val colorParent = nodeInfo[nodeIndex + COLOR_PARENT]
                val color = unpackInt1(colorParent)
                val parent = unpackInt2(colorParent)
                nodeInfo[targetIndex + COLOR_PARENT] = packInts(color, map(parent))

                val leftRight = nodeInfo[nodeIndex + LEFT_RIGHT]
                val left = unpackInt1(leftRight)
                val right = unpackInt2(leftRight)
                nodeInfo[targetIndex + LEFT_RIGHT] = packInts(map(left), map(right))

                nodeInfo[targetIndex + START_END] = nodeInfo[nodeIndex + START_END]
                nodeInfo[targetIndex + MIN_MAX] = nodeInfo[nodeIndex + MIN_MAX]
                items[targetIndex / STRIDE] = items[nodeIndex / STRIDE]
            } else {
                // Even if targetIndex == index, we still need to remap parent, left, right indices
                val colorParent = nodeInfo[nodeIndex + COLOR_PARENT]
                val color = unpackInt1(colorParent)
                val parent = unpackInt2(colorParent)
                nodeInfo[targetIndex + COLOR_PARENT] = packInts(color, map(parent))

                val leftRight = nodeInfo[nodeIndex + LEFT_RIGHT]
                val left = unpackInt1(leftRight)
                val right = unpackInt2(leftRight)
                nodeInfo[targetIndex + LEFT_RIGHT] = packInts(map(left), map(right))
            }
            nodeIndex += STRIDE
            targetIndex += STRIDE
        }

        nodeInfo.removeRange(targetIndex, nodeInfo.size)
        items.subList(items.size - deletedNodeCount, items.size).clear()
        deletedNodeCount = 0
        mapping.clear()
    }

    /**
     * Executes [block] for each interval(represented by [Node]) that overlaps with the interval
     * defined by [start] and [end]. [start] *must* be less than or equal to [end]. The overlap is
     * defined as [start] being inclusive, but [end] being exclusive.
     *
     * The block is passed with an [Int] instead of [Node] to avoid boxing. Note: The [Node] is
     * exposed for advanced usages where the caller wants to modify the tree directly, but it's the
     * caller's responsibility to not break any red-black tree properties.
     */
    private inline fun forEachNodeInRange(start: Int, end: Int = start, block: (Int) -> Unit) {
        forEachNodeMinMaxInRange(start, end) {
            val node = Node(it)
            if (node.overlaps(start, end)) {
                block(node.index)
            }
        }
    }

    /**
     * Executes [block] for each interval(represented by [Node]) whose [min] and [max] overlaps with
     * the interval defined by [start] and [end]. [start] *must* be lesser than or equal to [end].
     * The overlap is inclusive on both ends. Each interval is called in the order sorted by the
     * interval's start.
     *
     * The block is passed with an [Int] instead of [Node] to avoid boxing. Note: The [Node] is
     * exposed for advance usages where the caller wants to modify the tree directly, but it's
     * caller's responsibility to not break any red-black tree properties.
     */
    private inline fun forEachNodeMinMaxInRange(
        start: Int,
        end: Int = start,
        block: (Int) -> Unit,
    ) {
        if (root == terminator || root.max < start || root.min > end) return
        var visitedState = Unvisited
        var node = root
        while (node != terminator) {
            if (visitedState == Unvisited) {
                // First time visited this node, check the left child first.
                // We should check node.left.max >= start && node.left.min <= end, but we know that
                // node.min == node.left.min and node.min <= end.
                if (node.left != terminator && node.left.max >= start) {
                    // Has left child, and it overlaps with the target range. Go to the left child.
                    node = node.left
                    visitedState = Unvisited
                } else {
                    // Left child is either terminator or doesn't overlap with the target range.
                    // Mark it as visited.
                    visitedState = LeftVisited
                }
            } else if (visitedState == LeftVisited) {
                // Visited the current node since the left child is already visited.
                block(node.index)
                // Visit the right child is similar to how we traverse the left child.
                // But we have to check both ends of the right child.
                // Note: node.max doesn't always equal to node.right.max
                if (node.right != terminator && node.right.max >= start && node.right.min <= end) {
                    node = node.right
                    visitedState = Unvisited
                } else {
                    visitedState = RightVisited
                }
            } else if (visitedState == RightVisited) {
                // Both children visited, go back to parent node.
                if (node.parent != terminator) {
                    visitedState =
                        if (node == node.parent.left) {
                            LeftVisited
                        } else {
                            RightVisited
                        }
                }
                // Always go to parent node even if it's terminator.
                node = node.parent
            }
        }
    }

    /**
     * Map the start, end values of all intervals overlapping with the given [start] and [end] using
     * the [mapper] function.
     *
     * If an interval's end becomes less than or equal to its start after mapping, it is removed
     * from the tree.
     *
     * This operation assumes that after the mapping the order of the intervals is preserved.
     *
     * It's the caller's responsibility to ensure that:
     * 1. The [mapper] function is pure, has no side effects, and returns the same value for the
     *    same input.
     * 2. [mapper] is a monotonic function. If `x1 <= x2` then `mapper(x1) <= mapper(x2)`.
     * 3. After the mapping, the order of the intervals (sorted by start) is preserved.
     *
     * e.g. For an IntIntervalTree that stores the following intervals: `[0, 5], [10, 15], [20, 25],
     * [30, 35]`
     *
     * Calling `mapIntervals(10, 25) { it + 5 }` is safe because the order of the intervals is
     * preserved: `[0, 5], `**`[15, 20], [25, 30]`**`, [30, 35]`
     *
     * Calling `mapIntervals(10, 25) { it + 20 }` is not safe: `[0, 5], `**`[30, 35], [40, 45]`**`,
     * [30, 35]` Because after [20, 25] is mapped to [40, 45] its start is larger than 30.
     */
    inline fun mapIntervals(start: Int, end: Int, mapper: (Int) -> Int) {
        val toRemove = tempArray
        forEachNodeMinMaxInRange(start, end) {
            val node = Node(it)
            node.start = mapper(node.start)
            node.end = mapper(node.end)
            node.min = mapper(node.min)
            node.max = mapper(node.max)

            if (node.end <= node.start) {
                toRemove.add(node)
            }
        }

        // Cautious: we need to call removeNode with cleanUp == false, and do a cleanup manually.
        // Because the Node indices will change after clean up, making the following removeNode
        // incorrect.
        toRemove.forEach { removeNode(Node(it), cleanUp = false) }
        toRemove.clear()
        cleanDeletedNodesIfNeeded()
    }

    /**
     * Adds the interval defined between a [start] and an [end] coordinate.
     *
     * @param start The start index of the interval
     * @param end The end index of the interval, must be > [start] or it'll return false and do
     *   nothing
     * @param item Data item to associate with the interval
     * @return true if the interval is added successfully, false otherwise
     */
    fun addInterval(item: T, start: Int, end: Int): Boolean {
        if (start >= end) return false
        if (findNode(item, start, end) != terminator) return false

        val node = Node(start, end, item, TreeColorRed)

        // Update the tree without doing any balancing
        var current = root
        var parent = terminator

        while (current != terminator) {
            parent = current
            current =
                if (node.start <= current.start) {
                    current.left
                } else {
                    current.right
                }
        }

        node.parent = parent

        if (parent == terminator) {
            root = node
        } else {
            if (node.start <= parent.start) {
                parent.left = node
            } else {
                parent.right = node
            }
        }

        updateNodeMinMax(parent)

        rebalanceAfterInsertion(node)
        return true
    }

    /**
     * Removes the interval defined between a [start] and an [end] coordinate.
     *
     * @param start The start index of the interval
     * @param end The end index of the interval, must be > [start] or it'll return false and do
     *   nothing
     * @param item Data item associated with the interval
     * @return true if the interval is removed successfully, false otherwise
     */
    fun removeInterval(item: T, start: Int, end: Int): Boolean {
        if (start >= end) return false
        val node = findNode(item, start, end)
        if (node == terminator) return false
        removeNode(node)
        return true
    }

    /** Helper method to find a specific Node given the range and data. */
    private fun findNode(item: T, start: Int, end: Int): Node {
        if (root == terminator || root.max < end || root.min > start) return terminator

        val stack = tempArray
        stack.add(root)
        while (stack.isNotEmpty()) {
            val node = stack.pop()
            if (node.start == start && node.end == end && node.item == item) {
                stack.clear()
                return node
            }
            if (node.start >= start) {
                val left = node.left
                // Prune if left's max is smaller than end.
                // There is no need to check left.min, because we know node.min >= start and
                // left.min == node.min.
                if (left != terminator && left.max >= end) {
                    stack.add(left)
                }
            }

            if (node.start <= start) {
                val right = node.right
                // Prune if right's min/max can't contain the target range.
                // We have to check right.max even if we know end <= node.max, because left.max
                // can be larger than right.max
                if (right != terminator && right.min <= start && right.max >= end) {
                    stack.add(right)
                }
            }
        }
        // [stack] should be empty at this point, no need to clean it.
        return terminator
    }

    /**
     * Removes the given [target] from the tree while maintaining the tree's properties, and marks
     * it as deleted. Be aware that this method might clean up [Node]s that are marked as deleted,
     * which will change the indices of existing [Node]s.
     *
     * @param target The node to be removed. It cannot be the terminator.
     * @param cleanUp Whether to perform clean up for the removed node.
     */
    private fun removeNode(target: Node, cleanUp: Boolean = true) {
        // [spliced] is the node to be "spliced out" of its original structural position.
        // If [target] has two children, [spliced] is its inorder successor (which will
        // be moved to [target]'s position). Otherwise, [spliced] is [target] itself.
        //
        // Note: [target] is always the node removed from the collection.
        // Its entry in [nodeInfo] is marked as deleted but not physically removed,
        // ensuring other nodes' indices remain stable to preserve insertion order.
        var spliced = target
        // The original color of [spliced]. If we removed a black node, we need to rebalance the
        // tree.
        var splicedOriginalColor = spliced.color

        // The node that takes [spliced]'s original place. Rebalancing starts here.
        val replacement: Node
        // The parent of [replacement]. We track this explicitly because [replacement]
        // might be the [terminator], which does not store its own parent pointer.
        val replacementParent: Node

        // There are two case:
        // Case 1: [target] has 0 or 1 child, in this case replace it with its child (or terminator
        // if [target] has no child). The [spliced] node is the target itself.
        // Case 2: [target] has 2 children. We swap it with the inorder successor first, since it's
        // inorder successor won't have left child. It also falls in the case 1.
        if (target.left == terminator) {
            replacement = target.right
            replacementParent = target.parent
            transplant(target, target.right)
        } else if (target.right == terminator) {
            replacement = target.left
            replacementParent = target.parent
            transplant(target, target.left)
        } else {
            // Logically, we swap [target] with its inorder successor and then "remove" the
            // successor from its original structural position. [spliced] refers to the
            // successor node being moved.
            //
            // Case 1: [spliced] is the immediate right child of [target].
            // We move [spliced] into [target]'s position directly.
            //
            // Case 2: [spliced] is further down the right subtree.
            // We first replace [spliced] with its own right child, then move [spliced]
            // into [target]'s position, effectively replacing [target] with [spliced].
            spliced = target.right.lowestNode()
            splicedOriginalColor = spliced.color
            replacement = spliced.right
            if (spliced.parent == target) {
                replacementParent = spliced
            } else {
                replacementParent = spliced.parent
                transplant(spliced, spliced.right)
                spliced.right = target.right
                spliced.right.parent = spliced
            }

            transplant(target, spliced)
            spliced.left = target.left
            spliced.left.parent = spliced
            spliced.color = target.color
            // Also update the node min, max to [target]'s original value.
            spliced.min = target.min
            spliced.max = target.max
        }

        // We need to update min/max value of the tree starting from replacementParent.
        updateNodeMinMax(replacementParent)

        if (splicedOriginalColor == TreeColorBlack) {
            // We've deleted a black node, we need to rebalance it.
            rebalanceAfterDeletion(replacement, replacementParent)
        }
        deleteNode(target, cleanUp)
    }

    /**
     * Replaces the subtree rooted at [target] with the subtree rooted at [replacement].
     *
     * This method updates the parent pointer of [replacement] and the corresponding child pointer
     * of [target]'s parent.
     *
     * Note: This method does NOT update the [min] and [max] values of the nodes or their ancestors.
     * The caller is responsible for recomputing them (e.g., via [updateNodeMinMax]).
     *
     * @param target The node to be replaced.
     * @param replacement The node (or subtree) that will replace [target]. Can be [terminator].
     */
    private fun transplant(target: Node, replacement: Node) {
        if (target == replacement) return
        if (target.parent == terminator) {
            root = replacement
        } else if (target == target.parent.left) {
            target.parent.left = replacement
        } else {
            target.parent.right = replacement
        }
        if (replacement != terminator) {
            replacement.parent = target.parent
        }
    }

    /**
     * Mark the given [node] as deleted. This method does **NOT** remove the [node] from tree, see
     * [removeNode] if you want to remove the [node] from the tree.
     *
     * @param cleanUp Whether to perform clean up after marking the [node] as deleted.
     */
    private fun deleteNode(node: Node, cleanUp: Boolean) {
        node.color = TreeColorDeleted
        deletedNodeCount++
        if (cleanUp) {
            cleanDeletedNodesIfNeeded()
        }
    }

    private fun cleanDeletedNodesIfNeeded() {
        if (
            totalNodeCount > NODE_CLEANUP_SIZE_THRESHOLD && deletedNodeCount >= totalNodeCount / 2
        ) {
            cleanDeletedNodes()
        }
    }

    /**
     * Rebalance the tree after deleting a node.
     *
     * @param target The node that takes the place of the deleted node. It can be the [terminator].
     * @param targetParent the parent of [target].
     */
    private fun rebalanceAfterDeletion(target: Node, targetParent: Node) {
        var node = target
        var parent = targetParent
        // In the following loop, [node] points to the first node whose black height is less than
        // its sibling.
        // If [node] is red, flipping it to black is enough to resolve the issue.
        // Otherwise, complicated rebalancing is needed.
        while (node != root && node.color == TreeColorBlack) {
            if (node == parent.left) {
                var sibling = parent.right
                // The sibling is red, we rotate the tree so that sibling turns into black.
                if (sibling.color == TreeColorRed) {
                    sibling.color = TreeColorBlack
                    parent.color = TreeColorRed
                    rotateLeft(parent)
                    sibling = parent.right
                }
                // [sibling] is always black now.
                if (sibling.left.color == TreeColorBlack && sibling.right.color == TreeColorBlack) {
                    // [sibling]'s both children are black, flip [sibling] to be red.
                    // Now the [parent] is balanced but the black height is reduced by one. Check
                    // if [parent.parent] is balanced.
                    // Note that flipping [sibling]'s color could introduce color violation, but if
                    // [parent] is red, the following step will set it to black and quickly fix
                    // the color violation.
                    sibling.color = TreeColorRed
                    node = parent
                    parent = node.parent
                } else {
                    if (sibling.right.color == TreeColorBlack) {
                        // Set sibling to red, and then rotate sibling to sibling.right's position,
                        // then update sibling to be the original sibling.left.
                        // After the rotation, sibling.right is always red.
                        sibling.left.color = TreeColorBlack
                        sibling.color = TreeColorRed
                        rotateRight(sibling)
                        sibling = parent.right
                    }

                    // At this point, sibling.right is always red.
                    sibling.color = parent.color
                    parent.color = TreeColorBlack
                    sibling.right.color = TreeColorBlack
                    rotateLeft(parent)
                    node = root
                }
            } else {
                // Mirror the above logic for the right child.
                var sibling = parent.left
                if (sibling.color == TreeColorRed) {
                    sibling.color = TreeColorBlack
                    parent.color = TreeColorRed
                    rotateRight(parent)
                    sibling = parent.left
                }
                if (sibling.right.color == TreeColorBlack && sibling.left.color == TreeColorBlack) {
                    sibling.color = TreeColorRed
                    node = parent
                    parent = node.parent
                } else {
                    if (sibling.left.color == TreeColorBlack) {
                        sibling.right.color = TreeColorBlack
                        sibling.color = TreeColorRed
                        rotateLeft(sibling)
                        sibling = parent.left
                    }
                    sibling.color = parent.color
                    parent.color = TreeColorBlack
                    sibling.left.color = TreeColorBlack
                    rotateRight(parent)
                    node = root
                }
            }
        }
        node.color = TreeColorBlack
    }

    /**
     * Rebalance the tree after inserting a new node.
     *
     * @param target The node that's just inserted.
     */
    private fun rebalanceAfterInsertion(target: Node) {
        var node = target

        while (node != root && node.parent.color == TreeColorRed) {
            // Because the root is always black, node.parent is red and can't be the root.
            // then the parent must have a parent.
            val ancestor = node.parent.parent
            if (node.parent == ancestor.left) {
                val right = ancestor.right
                if (right.color == TreeColorRed) {
                    right.color = TreeColorBlack
                    node.parent.color = TreeColorBlack
                    ancestor.color = TreeColorRed
                    node = ancestor
                } else {
                    if (node == node.parent.right) {
                        node = node.parent
                        rotateLeft(node)
                    }
                    node.parent.color = TreeColorBlack
                    ancestor.color = TreeColorRed
                    rotateRight(ancestor)
                }
            } else {
                val left = ancestor.left
                if (left.color == TreeColorRed) {
                    left.color = TreeColorBlack
                    node.parent.color = TreeColorBlack
                    ancestor.color = TreeColorRed
                    node = ancestor
                } else {
                    if (node == node.parent.left) {
                        node = node.parent
                        rotateRight(node)
                    }
                    node.parent.color = TreeColorBlack
                    ancestor.color = TreeColorRed
                    rotateLeft(ancestor)
                }
            }
        }

        root.color = TreeColorBlack
    }

    private fun rotateLeft(node: Node) {
        val right = node.right
        node.right = right.left

        if (right.left != terminator) {
            right.left.parent = node
        }

        right.parent = node.parent

        if (node.parent == terminator) {
            root = right
        } else {
            if (node.parent.left == node) {
                node.parent.left = right
            } else {
                node.parent.right = right
            }
        }

        right.left = node
        node.parent = right

        updateNodeMinMax(node)
    }

    private fun rotateRight(node: Node) {
        val left = node.left
        node.left = left.right

        if (left.right != terminator) {
            left.right.parent = node
        }

        left.parent = node.parent

        if (node.parent == terminator) {
            root = left
        } else {
            if (node.parent.right == node) {
                node.parent.right = left
            } else {
                node.parent.left = left
            }
        }

        left.right = node
        node.parent = left

        updateNodeMinMax(node)
    }

    private fun updateNodeMinMax(node: Node) {
        var current = node
        while (current != terminator) {
            current.min = min(current.start, min(current.left.min, current.right.min))
            current.max = max(current.end, max(current.left.max, current.right.max))
            current = current.parent
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntIntervalTree<T>) return false

        // Note: Do not use member extensions (e.g., Node.min) on nodes from the [other] buffer.
        // Member extensions use [this] as the dispatch receiver, which would incorrectly
        // access [this.nodeInfo] instead of [other.nodeInfo].
        val thisMinMax = nodeInfo[root.index + MIN_MAX]
        val otherMinMax = other.nodeInfo[other.root.index + MIN_MAX]
        if (thisMinMax != otherMinMax) return false

        // Don't have the same amount of nodes in the tree.
        if (totalNodeCount - deletedNodeCount != other.totalNodeCount - other.deletedNodeCount) {
            return false
        }

        // Instead of traverse the entire tree. We directly compare the nodeInfo array, which is
        // the intervals in their added order.

        // We don't need to compare the first node, which is always terminator
        var thisIndex = STRIDE
        var otherIndex = STRIDE
        while (thisIndex < nodeInfo.size && otherIndex < other.nodeInfo.size) {
            if (unpackInt1(nodeInfo[thisIndex + COLOR_PARENT]) == TreeColorDeleted) {
                thisIndex += STRIDE
                continue
            }

            if (unpackInt1(other.nodeInfo[otherIndex + COLOR_PARENT]) == TreeColorDeleted) {
                otherIndex += STRIDE
                continue
            }

            val thisStartEnd = nodeInfo[thisIndex + START_END]
            val otherStartEnd = other.nodeInfo[otherIndex + START_END]

            if (thisStartEnd != otherStartEnd) return false
            if (items[thisIndex / STRIDE] != other.items[otherIndex / STRIDE]) return false
            thisIndex += STRIDE
            otherIndex += STRIDE
        }
        // Both tree should have the same amount of nodes. So they must be equal at this point.
        return true
    }

    override fun hashCode(): Int {
        var result = 0
        var nodeIndex = STRIDE
        // The first node is always terminator, ignore it
        while (nodeIndex < nodeInfo.size) {
            val node = Node(nodeIndex)
            if (node.color != TreeColorDeleted) {
                result = 31 * result + node.start
                result = 31 * result + node.end
                result = 31 * result + node.item.hashCode()
            }
            nodeIndex += STRIDE
        }
        return result
    }

    /** Create a copy of this [IntIntervalTree]. */
    fun copy(): IntIntervalTree<T> {
        // Always cleanup the deleted node before clone to save memory.
        cleanDeletedNodes()
        return IntIntervalTree(this)
    }

    fun isEmpty(): Boolean {
        return root == terminator
    }

    /** Helper method that add the node to the list */
    fun NodeList.add(node: Node) {
        add(node.index)
    }

    /** Helper method that remove the last node from the list and return it. */
    fun NodeList.pop(): Node {
        return Node(removeAt(size - 1))
    }
}

internal typealias NodeList = MutableIntList

/**
 * The color of the red-black tree node. In addition to red and black. We also reused this field to
 * mark nodes that's been deleted. A better implementation will pack color and deletion information
 * to a bitwise flags. But the current implementation is simpler and more performant.
 */
internal typealias TreeColor = Int

internal const val TreeColorRed = 0
internal const val TreeColorBlack = 1
internal const val TreeColorDeleted = 2

/**
 * Some constants used denote the traverse state of the tree [Node]. Check
 * [IntIntervalTree.forEachNodeMinMaxInRange] for more information. [Unvisited] denotes that the
 * current [Node] is not visited yet. [LeftVisited] denotes that the left child is already visited.
 * [RightVisited] denotes that the right child is already visited.
 */
internal const val Unvisited = 0
internal const val LeftVisited = 1
internal const val RightVisited = 2

/**
 * The node in the red-black tree. The index points to the [IntIntervalTree.nodeInfo] list where its
 * information is stored. In other words, n-th Node added to the [IntIntervalTree] has index = n *
 * STRIDE (instead of n). And it's corresponding item is stored in the [IntIntervalTree.items] list
 * at index / STRIDE. We did so to avoid the need of multiplication every time we need to access the
 * node's information.
 */
@JvmInline internal value class Node(val index: Int)

/**
 * Helper function that checks if the range [lStart, lEnd) intersects with the range [rStart, rEnd).
 *
 * @return [lStart, lEnd) intersects with range [rStart, rEnd), vice versa.
 */
internal fun intersect(lStart: Int, lEnd: Int, rStart: Int, rEnd: Int): Boolean {
    // We can check if two ranges intersect just by performing the following operation:
    //
    //     lStart < rEnd && rStart < lEnd
    //
    // This operation handles all cases, including when one of the ranges is fully included in the
    // other ranges. This is however not enough in this particular case because our ranges are open
    // at the end, but closed at the start.
    //
    // This means the test above would fail cases like: [1, 4) intersect [1, 1)
    // To address this we check if either one of the ranges is a "point" (empty selection). If
    // that's the case and both ranges share the same start point, then they intersect.
    //
    // In addition, we use bitwise operators (or, and) instead of boolean operators (||, &&) to
    // generate branchless code.
    return ((lStart == lEnd) or (rStart == rEnd) and (lStart == rStart)) or
        ((lStart < rEnd) and (rStart < lEnd))
}
