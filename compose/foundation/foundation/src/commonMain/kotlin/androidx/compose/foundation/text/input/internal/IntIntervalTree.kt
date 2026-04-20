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
import androidx.collection.MutableObjectList
import androidx.collection.mutableLongListOf
import androidx.collection.mutableObjectListOf
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
 * [Node]s are stored in [nodeInfo] with a stride of 4 [Long] entries:
 * 1. Offset 0 (`INFO_PARENT`): Color (1 bit), ID (31 bits), Deleted flag (1 bit), Parent index (31
 *    bits).
 * 2. Offset 1 (`LEFT_RIGHT`): Left child index (32 bits), Right child index (32 bits).
 * 3. Offset 2 (`START_END`): Packed [Interval] of start/end boundaries, and user flags.
 * 4. Offset 3 (`MIN_MAX`): Packed [Interval] of min/max values for the subtree.
 *
 * The corresponding data for each node is stored in the [items] list at `node.index / STRIDE`. The
 * [Node] is a value class that wraps an index pointing to the start of its information in the
 * [nodeInfo] list.
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

        /**
         * The color, identifier, deleted flag and parent index are stored at offset (0) in the
         * [Node]'s [STRIDE]-length block in the [nodeInfo] array.
         *
         * Bit Layout (64 bits total):
         * ```
         * +--------+---------------------------------+--------+---------------------------------+
         * | Bit 63 | Bits 32-62 (31 bits)            | Bit 31 | Bits 0-30 (31 bits)             |
         * +--------+---------------------------------+--------+---------------------------------+
         * | Color  | identifier                      | Deleted| Parent Index                    |
         * +--------+---------------------------------+--------+---------------------------------+
         * ```
         */
        private const val INFO_PARENT = 0

        /**
         * The left and right child indices are stored at offset (1) in the [Node]'s [STRIDE]-length
         * block in the [nodeInfo] array.
         *
         * Bit Layout (64 bits total):
         * ```
         * +------------------------------------------+------------------------------------------+
         * | Bits 32-63 (32 bits)                     | Bits 0-31 (32 bits)                      |
         * +------------------------------------------+------------------------------------------+
         * | Left Child Index                         | Right Child Index                        |
         * +------------------------------------------+------------------------------------------+
         * ```
         */
        private const val LEFT_RIGHT = 1

        /**
         * The start and end properties, along with two general-purpose flags, are stored at offset
         * (2) in the [Node]'s [STRIDE]-length block in the [nodeInfo] array. They are stored as a
         * [Long] interpreted as an [Interval].
         *
         * Bit Layout (64 bits total):
         * ```
         * +--------+---------------------------------+--------+---------------------------------+
         * | Bit 63 | Bits 32-62 (31 bits)            | Bit 31 | Bits 0-30 (31 bits)             |
         * +--------+---------------------------------+--------+---------------------------------+
         * | flag1  | start                           | flag2  | end                             |
         * +--------+---------------------------------+--------+---------------------------------+
         * ```
         *
         * @see Interval
         */
        private const val START_END = 2

        /**
         * The min and max properties are stored at offset (3) in the [Node]'s [STRIDE]-length block
         * in the [nodeInfo] array. They are stored as a [Long] interpreted as an [Interval].
         *
         * Note that while this is stored as an [Interval], the flags (`flag1` and `flag2`) in this
         * layout are meaningless for `MIN_MAX` as they are only applicable to the interval's
         * `START_END` values.
         *
         * Bit Layout (64 bits total):
         * ```
         * +--------+---------------------------------+--------+---------------------------------+
         * | Bit 63 | Bits 32-62 (31 bits)            | Bit 31 | Bits 0-30 (31 bits)             |
         * +--------+---------------------------------+--------+---------------------------------+
         * | unused | min                             | unused | max                             |
         * +--------+---------------------------------+--------+---------------------------------+
         * ```
         *
         * @see Interval
         */
        private const val MIN_MAX = 3

        /**
         * The total number of 64-bit Longs allocated for each [Node] sequentially in the flat
         * [nodeInfo] array. A [Node]'s `index` property points to the start of its block, and the
         * fields are accessed by adding the offsets (e.g., `index + INFO_PARENT`).
         */
        private const val STRIDE = 4
    }

    /**
     * The color of this [Node]. It can be [TreeColorRed] or [TreeColorBlack]. This is stored in bit
     * 63 (`flag1`) at the [INFO_PARENT] offset.
     */
    var Node.color: TreeColor
        get() =
            if (unpackFlag1(nodeInfo[index + INFO_PARENT])) {
                TreeColorRed
            } else {
                TreeColorBlack
            }
        set(value) {
            nodeInfo[index + INFO_PARENT] =
                packFlag1(nodeInfo[index + INFO_PARENT], value == TreeColorRed)
        }

    /**
     * Whether the [Node] is deleted or not. This is stored in bit 31 (`flag2`) at the [INFO_PARENT]
     * offset.
     */
    var Node.isDeleted: Boolean
        get() = unpackFlag2(nodeInfo[index + INFO_PARENT])
        set(value) {
            nodeInfo[index + INFO_PARENT] = packFlag2(nodeInfo[index + INFO_PARENT], value)
        }

    /**
     * Parent [Node] of this [Node]. This is stored as a 31-bit integer in the lower 31 bits (bits
     * 0-30) at the [INFO_PARENT] offset.
     */
    var Node.parent: Node
        get() = Node(unpackValue2(nodeInfo[index + INFO_PARENT]))
        set(value) {
            // Node index is always positive, we don't need to mask the value.index.
            nodeInfo[index + INFO_PARENT] = packValue2(nodeInfo[index + INFO_PARENT], value.index)
        }

    /**
     * The id associated with this [Node], which is a positive integer where 0 means an invalid id.
     * It is stored as a 31-bit [Int] in bits 32-62 at the [INFO_PARENT] offset.
     */
    val Node.id: Int
        get() = unpackValue1(nodeInfo[index + INFO_PARENT])

    /**
     * Left child [Node] of this [Node]. This is stored in the upper 32 bits (bits 32-63) at the
     * [LEFT_RIGHT] offset.
     */
    var Node.left: Node
        get() = Node(unpackInt1(nodeInfo[index + LEFT_RIGHT]))
        set(value) {
            nodeInfo[index + LEFT_RIGHT] =
                packInts(value.index, unpackInt2(nodeInfo[index + LEFT_RIGHT]))
        }

    /**
     * Right child [Node] of this [Node]. This is stored in the lower 32 bits (bits 0-31) at the
     * [LEFT_RIGHT] offset.
     */
    var Node.right: Node
        get() = Node(unpackInt2(nodeInfo[index + LEFT_RIGHT]))
        set(value) {
            nodeInfo[index + LEFT_RIGHT] =
                packInts(unpackInt1(nodeInfo[index + LEFT_RIGHT]), value.index)
        }

    /** The start index of the interval corresponding to this [Node], which is inclusive. */
    val Node.start: Int
        get() = startEnd.start

    /** The end index of the interval corresponding to this [Node], which is exclusive. */
    val Node.end: Int
        get() = startEnd.end

    /**
     * The [Interval] that stores the start and end indices of the interval corresponding to this
     * [Node].
     */
    var Node.startEnd: Interval
        get() = Interval(nodeInfo[index + START_END])
        set(startEnd) {
            nodeInfo[index + START_END] = startEnd.packed
        }

    /** The minimum start index of the subtree rooted at [Node]. */
    val Node.min: Int
        get() = minMax.start

    /** The maximum end index of the subtree rooted at [Node]. */
    val Node.max: Int
        get() = minMax.end

    /**
     * The [Interval] that stores the minimum start and maximum end indices of the subtree rooted at
     * [Node].
     */
    var Node.minMax: Interval
        get() = Interval(nodeInfo[index + MIN_MAX])
        set(minMax) {
            nodeInfo[index + MIN_MAX] = minMax.packed
        }

    /** The data associated with this [Node]. */
    val Node.item: T?
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
    fun Node.overlaps(start: Int, end: Int) = startEnd.overlaps(start, end)

    /** Creates a new [Node] with the given [start], [end], [item], [id] and [color]. */
    private fun Node(item: T?, interval: Interval, id: Int = 0, color: Int = TreeColorRed): Node {
        val index = nodeInfo.size
        nodeInfo.add(
            packValuesAndFlags(
                flag1 = color == TreeColorRed,
                val1 = id,
                // Not deleted by default
                flag2 = false,
                // Parent is terminator(index = 0) by default
                val2 = 0,
            )
        )
        // left, right both pointing to the terminator(index 0) by default.
        nodeInfo.add(0)
        // start, end
        nodeInfo.add(interval.packed)
        // min, max by default the same as start and end
        nodeInfo.add(interval.packed)
        items.add(item)
        return Node(index)
    }

    private val items: MutableObjectList<T?>
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
     * More details can be found in [rebalancePostAttach] and [rebalancePostDetach], where we need
     * to check the colors of uncle and sibling nodes, which may be the [terminator].
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
            items = MutableObjectList<T?>(source.items.size).also { it.addAll(source.items) }
            nodeInfo = MutableLongList(source.nodeInfo.size).also { it.addAll(source.nodeInfo) }
            terminator = source.terminator
            root = source.root
            deletedNodeCount = source.deletedNodeCount
        } else {
            items = mutableObjectListOf()
            nodeInfo = mutableLongListOf()
            terminator =
                Node(
                    item = null,
                    interval = Interval(start = Int.MAX_VALUE, end = Int.MIN_VALUE),
                    id = 0,
                    color = TreeColorBlack,
                )
            root = terminator
            deletedNodeCount = 0
        }
    }

    /**
     * Calls [block] for each the interval that overlaps with the range defined by [start] and
     * [end], in the order they are added. The overlap is inclusive on [start] and exclusive at
     * [end]. The block is passed with an [Long] instead of [Interval] to avoid boxing.
     */
    inline fun forEachIntervalInRange(
        start: Int,
        end: Int,
        block: (item: T, packedInterval: Long) -> Unit,
    ) {
        val nodes = tempArray
        forEachNodeInRange(start, end) { nodes.add(it) }
        nodes.sort()

        nodes.forEach {
            val node = Node(it)
            val item = node.item
            if (item != null) {
                block(item, node.startEnd.packed)
            }
        }
        nodes.clear()
    }

    /**
     * Finds each interval of type [R] that overlaps with the range defined by [start] and [end],
     * and maps it to a result of type [M].
     *
     * Intervals are processed in the order they were originally added to the tree.
     *
     * **Performance Note:** This method is more efficient than manually creating a [MutableList]
     * and using [forEachIntervalInRange] to fill it, because it pre-allocates the exact required
     * capacity for the resulting list.
     *
     * @param start The start of the range to check for overlaps.
     * @param end The end of the range to check for overlaps.
     * @return A list of mapped results of type [M].
     */
    inline fun <reified R, M> findIntervalsInRange(
        start: Int,
        end: Int,
        block: (item: R, packedInterval: Long) -> M,
    ): List<M> {
        val nodes = tempArray
        forEachNodeInRange(start, end) {
            if (Node(it).item is R) {
                nodes.add(it)
            }
        }
        nodes.sort()

        val result =
            List(nodes.size) {
                val node = Node(nodes[it])
                val item = node.item
                if (item != null) {
                    block(item as R, node.startEnd.packed)
                } else {
                    throw IllegalStateException("IntIntervalTree's item should not be null")
                }
            }
        nodes.clear()
        return result
    }

    /**
     * Calls [block] for all intervals stored in this [IntIntervalTree] in the order they were
     * added. This method returns the same result as [forEachIntervalInRange] with full range but is
     * optimized to be faster, especially when a large number of intervals are stored. The block is
     * passed with an [Long] for [Interval] to avoid boxing.
     */
    inline fun forAllIntervals(block: (item: T, packedInterval: Long) -> Unit) {
        if (root == terminator) return
        // Instead of traversing the entire tree, we directly iterate over the nodeInfo array,
        // which gives us the intervals sorted in their addition order.
        // Ignore the first node, which is the terminator
        var nodeIndex = STRIDE
        while (nodeIndex < nodeInfo.size) {
            val node = Node(nodeIndex)
            if (!node.isDeleted) {
                val item = node.item
                if (item != null) {
                    block(item, node.startEnd.packed)
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
        items.removeRange(1, items.size)
        deletedNodeCount = 0
    }

    /** Removes the nodes that are marked to be deleted from [nodeInfo]. */
    private fun cleanDeletedNodes() {
        if (deletedNodeCount == 0) return

        var deletedSoFar = 0
        val mapping = tempArray
        mapping.ensureCapacity(totalNodeCount)
        for (node in 0 until totalNodeCount) {
            // Deleted is stored as flag2
            val deleted = unpackFlag2(nodeInfo[node * STRIDE + INFO_PARENT])
            if (deleted) {
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
            // This method is performance-critical. We access the `nodeInfo` array directly instead
            // of using the `Node` getters/setters. This halves the number of memory reads and
            // writes.
            val infoParent = nodeInfo[nodeIndex + INFO_PARENT]
            // Deleted is stored as flag2.
            val deleted = unpackFlag2(infoParent)
            if (deleted) {
                nodeIndex += STRIDE
                continue
            }

            if (targetIndex != nodeIndex) {
                val parent = unpackValue2(infoParent)
                nodeInfo[targetIndex + INFO_PARENT] = packValue2(infoParent, map(parent))

                val leftRight = nodeInfo[nodeIndex + LEFT_RIGHT]
                val left = unpackInt1(leftRight)
                val right = unpackInt2(leftRight)
                nodeInfo[targetIndex + LEFT_RIGHT] = packInts(map(left), map(right))

                nodeInfo[targetIndex + START_END] = nodeInfo[nodeIndex + START_END]
                nodeInfo[targetIndex + MIN_MAX] = nodeInfo[nodeIndex + MIN_MAX]
                items[targetIndex / STRIDE] = items[nodeIndex / STRIDE]
            } else {
                // Even if targetIndex == index, we still need to remap parent, left, right indices
                val parent = unpackValue2(infoParent)
                nodeInfo[targetIndex + INFO_PARENT] = packValue2(infoParent, map(parent))

                val leftRight = nodeInfo[nodeIndex + LEFT_RIGHT]
                val left = unpackInt1(leftRight)
                val right = unpackInt2(leftRight)
                nodeInfo[targetIndex + LEFT_RIGHT] = packInts(map(left), map(right))
            }
            nodeIndex += STRIDE
            targetIndex += STRIDE
        }

        nodeInfo.removeRange(targetIndex, nodeInfo.size)
        items.removeRange(items.size - deletedNodeCount, items.size)
        deletedNodeCount = 0
        mapping.clear()
    }

    /**
     * Executes [block] for each interval(represented by [Node]) that overlaps with the interval
     * defined by [start] and [end]. [start] *must* be less than or equal to [end]. The overlap is
     * defined as [start] being inclusive, but [end] being exclusive.
     *
     * The block is passed with an [Int] instead of [Node] to avoid boxing.
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
     * The block is passed with an [Int] instead of [Node] to avoid boxing.
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
     * Maps the intervals overlapping the given [start] and [end] parameters using the [mapper]
     * function. The overlap check is **inclusive** of both [start] and [end].
     *
     * If an interval's mapped `end` becomes less than or equal to its `start`, it is removed from
     * the tree.
     *
     * This operation allows mapped intervals to change their `start` and `end` arbitrarily. The
     * tree is structured as a Binary Search Tree primarily sorted by the `start` index of each
     * interval. If an interval's new `start` violates this binary search tree ordering (i.e., its
     * `start` becomes greater than a later interval's `start` or smaller than a preceding
     * interval's `start`), the tree automatically detaches and reattaches the affected nodes to
     * safely restore the proper BST ordering.
     *
     * Logically, this operation is equivalent to finding, detaching, mapping, and reattaching all
     * overlapping intervals. However, this implementation is highly optimized: if mapped intervals
     * do not violate the tree's BST order properties (i.e., their new `start` values maintain the
     * ascending sequence compared to adjacent nodes and do not jump across unmapped nodes), they
     * are updated in-place without the overhead of structural removal and reinsertion.
     *
     * @param start The start index of the interval bounds to map.
     * @param end The end index of the interval bounds to map.
     * @param mapper The mapping function applied to each overlapping interval. To avoid boxing, the
     *   [Interval] is passed and returned as a [Long].
     */
    inline fun mapIntervals(start: Int, end: Int, mapper: (Long) -> Long) {
        // Keep the list of nodes in the range, we'll update the tree while mapping the nodes
        val nodes = tempArray
        // Note: forEachNodeMinMaxInRange will call block for nodes in the order sorted by their
        // start.
        forEachNodeMinMaxInRange(start, end) {
            val node = Node(it)
            if (node.start <= end && node.end >= start) {
                nodes.add(it)
            }
        }

        // After the map phase, the tree's sorted-by-start property may be temporarily broken:
        // 1. Nodes might be out of order (no longer sorted by their start index).
        // 2. Some nodes might have a collapsed range.
        //
        // For nodes that break the order property, we detach them from the tree
        // and then reattach them to restore the correct order.
        // For nodes with collapsed ranges, we simply detach them from the tree and then dispose.
        //
        // While the following algorithm might seem unconventional, its correctness is
        // straightforward to prove. This interval tree maintains three properties:
        // 1. Balanced (red-black tree properties)
        // 2. Min/Max invariants (all subtrees' start/end fall within their root node's min/max)
        // 3. Nodes are sorted by their start index
        //
        // The algorithms to maintain these properties are largely independent: tree rebalancing
        // doesn't depend on the nodes' order, and the min/max propagation algorithm doesn't rely on
        // the tree being balanced or sorted.
        //
        // Because of this, `detachNode` will remove a node without changing the order of the
        // remaining nodes, while the tree's balanced and min/max properties are still maintained.
        // Similarly, `attachNode` fix the order without breaking the balance and min/max
        // properties.

        // maxStart is used to check if the there are nodes that in wrong order after the mapping.
        var maxStart = start
        var reattachCount = 0
        for (index in 0 until nodes.size) {
            val node = Node(nodes[index])
            val newStartEnd = Interval(mapper(node.startEnd.packed))
            val oldStart = node.start
            node.startEnd = newStartEnd

            // Maintain the node's min, max invariant.
            updateNodeMinMax(node)

            val newStart = newStartEnd.start
            val newEnd = newStartEnd.end
            if (newStart >= newEnd) {
                // This node range collapsed, needs to remove.
                detachNode(node)
                // We must call disposeNode with cleanUp == false, and perform a single cleanup
                // after the loop. This ensures that node indices remain stable while we are
                // processing other nodes in [needsUpdate].
                disposeNode(node, false)
            } else if (
                newStart < maxStart || newStart > end || (newStart != oldStart && oldStart < start)
            ) {
                // A node is considered to violate the tree's ordering property if:
                //  1. Its `newStart` is less than `maxStart` (the maximum start value seen so far).
                //     This happens if a previous node's start was mapped to a larger value, or this
                //     node's start was mapped to a smaller value.
                //  2. Its old start is less than the mapping range start, or its new start is
                //     greater than the mapping end. (We can't be sure if it breaks the order, so we
                //     safely assume it does).
                //
                // Action:
                // We remove the out-of-order node temporarily. It cannot be reattached immediately
                // since `attachNode()` expects a correctly ordered tree. To safely restore the
                // tree's structure, all out-of-order nodes must be removed first and stored in the
                // `nodes` list for a future reattachment pass.
                detachNode(node)
                nodes[reattachCount++] = nodes[index]
            } else {
                // Update the maxStart we've seen so far.
                maxStart = newStart
            }
        }

        // Attach all the out-of-order nodes back to the tree.
        for (index in 0 until reattachCount) {
            val node = Node(nodes[index])
            node.color = TreeColorRed
            node.minMax = node.startEnd
            node.left = terminator
            node.right = terminator
            attachNode(node)
        }
        nodes.clear()
        cleanDeletedNodesIfNeeded()
    }

    /**
     * Adds the interval to the tree.
     *
     * @param item Data item to associate with the interval.
     * @param interval The interval to add.
     * @return true if the interval is added successfully, false otherwise
     */
    fun addInterval(item: T, interval: Interval): Boolean {
        if (interval.start >= interval.end) return false
        if (findNode(item, interval) != terminator) return false

        val node = Node(item, interval, 0, TreeColorRed)
        attachNode(node)
        return true
    }

    /**
     * Attaches the given [node] to the tree, while maintaining the tree's properties. It assumes
     * the given [Node] is initialized to be [TreeColorRed], no left or right child, and
     * [Node.minMax] equals to [Node.startEnd].
     */
    fun attachNode(node: Node) {
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
        rebalancePostAttach(node)
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
    fun removeInterval(item: T, interval: Interval): Boolean {
        if (interval.start >= interval.end) return false
        val node = findNode(item, interval)
        if (node == terminator) return false
        detachNode(node)
        disposeNode(node, true)
        return true
    }

    /** Helper method to find a specific Node given the range and data. */
    private fun findNode(item: T, interval: Interval): Node {
        if (root == terminator || !root.minMax.overlaps(interval.start, interval.end))
            return terminator

        val stack = tempArray
        stack.add(root)
        while (stack.isNotEmpty()) {
            val node = stack.pop()
            if (node.startEnd == interval && node.item == item) {
                stack.clear()
                return node
            }
            if (node.start >= interval.start) {
                val left = node.left
                // Prune if left's max is smaller than end.
                // There is no need to check left.min, because we know node.min >= start and
                // left.min == node.min.
                if (left != terminator && left.max >= interval.end) {
                    stack.add(left)
                }
            }

            if (node.start <= interval.start) {
                val right = node.right
                // Prune if right's min/max can't contain the target range.
                // We have to check right.max even if we know end <= node.max, because left.max
                // can be larger than right.max
                if (
                    right != terminator && right.min <= interval.start && right.max >= interval.end
                ) {
                    stack.add(right)
                }
            }
        }
        // [stack] should be empty at this point, no need to clean it.
        return terminator
    }

    /** Detach the given [target] from the tree structure maintaining the tree's properties. */
    private fun detachNode(target: Node) {
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

        // There are two cases:
        // Case 1: [target] has 0 or 1 child, in this case replace it with its child (or terminator
        // if [target] has no child). The [spliced] node is the target itself.
        // Case 2: [target] has 2 children. We swap it with the inorder successor first, since it's
        // inorder successor won't have left child. This reduces to case 1.
        if (target.left == terminator) {
            replacement = target.right
            replacementParent = target.parent
            transplant(target, target.right)
            updateNodeMinMaxPostDetach(replacementParent, terminator)
        } else if (target.right == terminator) {
            replacement = target.left
            replacementParent = target.parent
            transplant(target, target.left)
            updateNodeMinMaxPostDetach(replacementParent, terminator)
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

            spliced.left = target.left
            spliced.left.parent = spliced
            spliced.color = target.color
            spliced.minMax = target.minMax

            transplant(target, spliced)

            updateNodeMinMaxPostDetach(replacementParent, spliced)
        }

        if (splicedOriginalColor == TreeColorBlack) {
            // We've deleted a black node, we need to rebalance it.
            rebalancePostDetach(replacement, replacementParent)
        }
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
     * Marks the given [target] node as deleted, and conditionally cleans up the [Node]s that are
     * marked as deleted.
     *
     * Note that a cleanup will change the indices of existing [Node]s. All references to the
     * [Node]s might be updated after the cleanup.
     *
     * @param target The node to be removed. It cannot be the terminator.
     * @param cleanUp Whether to perform clean up for the removed node.
     */
    private fun disposeNode(target: Node, cleanUp: Boolean) {
        target.isDeleted = true
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
     * Rebalances the tree after a node is detached from the tree.
     *
     * @param target The node that takes the place of the detached node. It can be the [terminator].
     * @param targetParent The parent of [target]; this is needed when [target] == [terminator].
     */
    private fun rebalancePostDetach(target: Node, targetParent: Node) {
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
     * Rebalances the tree after a new node is attached to the tree.
     *
     * @param target The node that's just attached to the tree.
     */
    private fun rebalancePostAttach(target: Node) {
        var node = target

        while (node != root && node.parent.color == TreeColorRed) {
            // Because the root is always black, node.parent is red and can't be the root.
            // Therefore, the parent must have a parent.
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

        updateNodeMinMaxPostRotate(node)
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

        updateNodeMinMaxPostRotate(node)
    }

    /**
     * Helper method to update [Node]'s min/max after a [rotateLeft] or [rotateRight] performed on
     * the [node].
     */
    private fun updateNodeMinMaxPostRotate(node: Node) {
        // [updateNodeMinMax] will stop propagation early if a node's min/max remains unchanged.
        // Even if [node]'s min/max doesn't change, its new parent (which was previously its
        // left or right child) has a new set of children, so its min/max might have changed.
        // If the first call short-circuits, the second call ensures the parent is updated.
        // If the first call successfully propagated through the parent, the second call will
        // simply short-circuit, so there is no performance penalty.
        updateNodeMinMax(node)
        updateNodeMinMax(node.parent)
    }

    /**
     * Helper method to update [Node]'s min/max property after we detach a node from the tree during
     * [detachNode].
     *
     * @param replacementParent The parent of the node that took the original structural position of
     *   the detached node. The detached node can be the target node itself, or its inorder
     *   successor if the target node had 2 children. Since its child was changed to the replacement
     *   node, its min/max needs updating.
     * @param transplantedNode The successor node that was moved to replace the target node. This
     *   node is still in the tree but has new children, so its min/max needs updating. Pass
     *   [terminator] if no successor was moved.
     */
    private fun updateNodeMinMaxPostDetach(replacementParent: Node, transplantedNode: Node) {
        // The [transplantedNode] is an ancestor of [replacementParent] (unless it's the
        // [terminator]).
        // We must update [replacementParent] first because updates must propagate bottom-up.
        // If we updated the ancestor first, it would read stale min/max values from
        // [replacementParent], and then we'd end up wasting work recalculating it again
        // when we finally update [transplantedNode].
        //
        // We must also call [updateNodeMinMax] on both nodes because it uses short-circuit
        // logic. If [replacementParent]'s min/max doesn't change, propagation will stop before
        // it reaches [transplantedNode]. However, [transplantedNode] has new children and
        // might still need its own min/max updated. Calling both ensures neither is missed.
        updateNodeMinMax(replacementParent)
        updateNodeMinMax(transplantedNode)
    }

    /**
     * Updates the [min] and [max] values of the given [node] and its ancestors. This method is
     * optimized such that once it finds that a [Node]'s min/max has not changed, it stops the
     * propagation.
     */
    private fun updateNodeMinMax(node: Node) {
        var current = node
        while (current != terminator) {
            val previousMinMax = current.minMax
            val min = minOf(current.start, current.left.min, current.right.min)
            val max = maxOf(current.end, current.left.max, current.right.max)

            // Nothing updates, stop the propagation.
            if (previousMinMax.start == min && previousMinMax.end == max) break
            current.minMax = Interval(min, max)
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
            // Deleted is stored as flag2.
            if (unpackFlag2(nodeInfo[thisIndex + INFO_PARENT])) {
                thisIndex += STRIDE
                continue
            }

            if (unpackFlag2(other.nodeInfo[otherIndex + INFO_PARENT])) {
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
            if (!node.isDeleted) {
                result = 31 * result + node.start
                result = 31 * result + node.end
                result = 31 * result + node.item.hashCode()
            }
            nodeIndex += STRIDE
        }
        return result
    }

    /** Sync this tree with the [other] tree. */
    fun syncTo(other: IntIntervalTree<T>) {
        if (other.isEmpty()) {
            clear()
            return
        }

        nodeInfo.clear()
        nodeInfo.addAll(other.nodeInfo)
        items.clear()
        items.addAll(other.items)

        root = other.root
        deletedNodeCount = other.deletedNodeCount
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

/** The color of the red-black tree node. In addition to red and black. */
internal typealias TreeColor = Int

internal const val TreeColorRed = 0
internal const val TreeColorBlack = 1

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
