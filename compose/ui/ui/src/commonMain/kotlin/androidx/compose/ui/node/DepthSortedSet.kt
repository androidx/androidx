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

import androidx.collection.MutableObjectIntMap
import androidx.collection.mutableObjectIntMapOf
import androidx.compose.ui.internal.checkPrecondition

// Each set gets its own bit in LayoutNode.depthSortedSetMembership. When a node is in a set, that
// set's bit is turned on. This lets contains() answer with a single bit check instead of a search.
// The three sets of one owner use these three bits.
private const val LookaheadAndAncestorMeasureSetBit = 1
private const val LookaheadAndAncestorPlaceSetBit = 1 shl 1
private const val ApproachSetBit = 1 shl 2

/**
 * [DepthSortedSet.compact] needs one more temporary bit per set, to mark nodes it has already kept.
 * It uses the set's bit moved up by this many places, so it never overlaps the real bits.
 */
private const val CompactionScratchShift = 16

/** How many slots the arrays get the first time a node is added. */
private const val InitialHeapCapacity = 16

// Shared empty arrays. A set that is never used never allocates its own arrays.
private val EmptyHeapKeys = LongArray(0)
private val EmptyHeapNodes = arrayOfNulls<LayoutNode>(0)

/**
 * When the arrays are full, the set first checks how many entries are out of date (see
 * [DepthSortedSet]). If there are at least this many more out-of-date entries than real ones, it
 * throws the old ones away instead of making the arrays bigger.
 */
private const val CompactionSlack = 16

/**
 * Packs a node's depth and its add order into one number that sorts the way we want. The depth goes
 * in the top 32 bits and the add order goes in the bottom 32 bits. So comparing two keys compares
 * depth first, and only looks at the add order when the depths are the same.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun heapKey(depth: Int, sequence: Int): Long =
    (depth.toLong() shl 32) or (sequence.toLong() and 0xFFFFFFFFL)

/** Gets the depth back out of a key made by [heapKey]. */
@Suppress("NOTHING_TO_INLINE")
private inline fun heapKeyDepth(key: Long): Int = (key ushr 32).toInt()

/**
 * A set of [LayoutNode]s that hands them back in order of [LayoutNode.depth], smallest depth first.
 * Compose keeps the nodes that need to be measured or laid out again in these sets. Handing out
 * parents before their children means a parent is always done before the children it affects.
 *
 * Nodes can be added and removed while the set is being emptied with [popEach].
 *
 * While a node is in the set:
 * 1) it must stay attached ([LayoutNode.isAttached] is `true`), and
 * 2) its [LayoutNode.depth] must not change. If the depth changed, the node could come out of the
 *    set at the wrong time.
 *
 * Nodes with the same depth come out in the order they were added. If a node is removed and added
 * again at the same depth before the set is empty, it may keep its old place in line.
 *
 * ## How it works
 *
 * The set is a "binary min-heap". A heap is a list kept in a special order: the entry with the
 * smallest key is always at the front, and adding or removing an entry only moves about log2(n)
 * other entries. (For 1,000 entries, that is about 10 moves.) The heap is stored in two arrays of
 * the same length: [keys] holds each entry's key (see [heapKey]) and [nodes] holds the node at the
 * same position. Arrays are reused, so once they are big enough, using the set does not allocate
 * any memory.
 *
 * To know whether a node is in the set, we don't search the heap. Each set owns one bit,
 * [membershipBit], in [LayoutNode.depthSortedSetMembership]. The bit is on while the node is in the
 * set. That makes [contains] a single bit check.
 *
 * [remove] only turns the bit off. It does not dig the entry out of the heap, because that would be
 * slow. The old entry stays in the heap and is thrown away later when it reaches the front. This is
 * called "lazy deletion."
 *
 * Because of lazy deletion, the heap can hold entries that are out of date. An entry is only real
 * if all three of these are true:
 * 1) the node's bit for this set is on,
 * 2) the node is still attached to the same [Owner] as the nodes that were added to this set, and
 * 3) the depth saved in the entry matches the node's depth right now.
 *
 * Any other entry is thrown away when it reaches the front. Rule 2 is needed because the sets of
 * every owner use the same bits. For example, a node can be removed from this set and then moved
 * into a popup, which has a different owner. That popup's set can turn the same bit back on while
 * this set still holds the old entry. Rule 3 is needed because [add] always puts in a new entry at
 * the node's current depth, so an entry with a different depth is left over from an earlier time
 * the node was in the set.
 *
 * When the set becomes empty, all entries are dropped at once.
 *
 * Changing a node's depth while it is in the set is not supported. With [extraAssertions] on, this
 * fails the depth check. With it off, the node's entry is thrown away, and a later [pop] fails a
 * check.
 *
 * @param extraAssertions turns on extra, slower checks that catch misuse.
 * @param membershipBit the bit in [LayoutNode.depthSortedSetMembership] that this set owns. Sets
 *   that could hold the same node must use different bits.
 */
internal class DepthSortedSet(
    private val extraAssertions: Boolean,
    private val membershipBit: Int,
) {
    // Remembers the depth each node had when it was added, so we can check that it didn't change.
    // A changed depth would break the order of the heap. Only created when extraAssertions is on.
    private var mapOfOriginalDepth: MutableObjectIntMap<LayoutNode>? = null

    /** The key of each heap entry, made by [heapKey]. Slots past [heapSize] are not used. */
    private var keys = EmptyHeapKeys

    /** The node of each heap entry. `nodes[i]` goes with `keys[i]`. */
    private var nodes = EmptyHeapNodes

    /** How many entries are in the heap, counting out-of-date ones. */
    private var heapSize = 0

    /** How many nodes are really in the set. Always less than or equal to [heapSize]. */
    private var size = 0

    /**
     * The add order given to the next entry. It goes up by one on every add, so nodes with the same
     * depth come out in the order they were added. It starts over at 0 when the set becomes empty.
     */
    private var nextSequence = 0

    /** The [Owner] of the nodes in this set. It is saved the first time a node is added. */
    private var owner: Owner? = null

    /** Returns `true` if [node] is in the set. This is a single bit check. */
    fun contains(node: LayoutNode): Boolean {
        val contains = node.depthSortedSetMembership and membershipBit != 0
        if (extraAssertions) {
            checkPrecondition(contains == safeMapOfOriginalDepth().containsKey(node)) {
                "inconsistency in DepthSortedSet"
            }
        }
        return contains
    }

    /** Adds [node] to the set. Does nothing if it is already in the set. */
    fun add(node: LayoutNode) {
        checkPrecondition(node.isAttached) { "DepthSortedSet.add called on an unattached node" }
        if (extraAssertions) {
            val map = safeMapOfOriginalDepth()
            val usedDepth = map.getOrDefault(node, Int.MAX_VALUE)
            if (usedDepth == Int.MAX_VALUE) {
                map[node] = node.depth
            } else {
                checkPrecondition(usedDepth == node.depth) { "invalid node depth" }
            }
        }
        val nodeOwner = node.owner
        val owner = owner
        if (owner == null) {
            this.owner = nodeOwner
        } else if (extraAssertions) {
            checkPrecondition(owner === nodeOwner) { "DepthSortedSet used with two owners" }
        }
        val membership = node.depthSortedSetMembership
        // Already in the set, so there is nothing to do.
        if (membership and membershipBit != 0) return
        node.depthSortedSetMembership = membership or membershipBit
        size++
        // Always add a new entry at the node's current depth. If an old entry for this node is
        // still in the heap, it will be thrown away or skipped later.
        push(node)
    }

    /**
     * Removes [node] from the set and returns `true` if it was in the set. This only turns off the
     * node's bit. Its entry stays in the heap until it reaches the front and is thrown away.
     */
    fun remove(node: LayoutNode): Boolean {
        checkPrecondition(node.isAttached) { "DepthSortedSet.remove called on an unattached node" }
        val membership = node.depthSortedSetMembership
        val contains = membership and membershipBit != 0
        if (contains) {
            node.depthSortedSetMembership = membership and membershipBit.inv()
            size--
            if (size == 0) {
                // Nothing real is left, so every remaining entry is out of date. Drop them all now
                // instead of one at a time.
                clearHeap()
            }
        }
        if (extraAssertions) {
            val map = safeMapOfOriginalDepth()
            if (map.contains(node)) {
                val usedDepth = map[node]
                map.remove(node)
                checkPrecondition(usedDepth == if (contains) node.depth else Int.MAX_VALUE) {
                    "invalid node depth"
                }
            }
        }
        return contains
    }

    /**
     * Removes and returns the node with the smallest depth. Among nodes with the same depth, the
     * one added first comes out first. Throws [NoSuchElementException] if the set is empty.
     */
    fun pop(): LayoutNode {
        val bit = membershipBit
        // Out-of-date entries may be at the front. Throw them away until we find a real one.
        while (heapSize > 0) {
            val node = nodes[0]!!
            if (node.depthSortedSetMembership and bit == 0 || node.owner !== owner) {
                // The node was removed after this entry was added. It may also have been moved to
                // another owner whose set uses the same bit, so its bit could be on again.
                removeTop()
                continue
            }
            if (heapKeyDepth(keys[0]) != node.depth) {
                // This entry is left over from an earlier time the node was in the set, at a
                // different depth. add() put in a new entry at the current depth, unless the depth
                // changed while the node was in the set (which is not allowed).
                if (extraAssertions) {
                    checkPrecondition(safeMapOfOriginalDepth()[node] == node.depth) {
                        "invalid node depth"
                    }
                }
                removeTop()
                continue
            }
            // A real entry. Take it off the heap and turn off the node's bit.
            removeTop()
            remove(node)
            return node
        }
        checkPrecondition(size == 0) { "inconsistency in DepthSortedSet" }
        throw NoSuchElementException("DepthSortedSet is empty")
    }

    /**
     * Pops nodes one at a time and calls [block] with each one, until the set is empty. [block] may
     * add nodes to the set or remove them.
     */
    inline fun popEach(crossinline block: (LayoutNode) -> Unit) {
        while (isNotEmpty()) {
            val node = pop()
            block(node)
        }
    }

    fun isEmpty(): Boolean = size == 0

    @Suppress("NOTHING_TO_INLINE") inline fun isNotEmpty(): Boolean = !isEmpty()

    private fun safeMapOfOriginalDepth(): MutableObjectIntMap<LayoutNode> {
        if (mapOfOriginalDepth == null) {
            mapOfOriginalDepth = mutableObjectIntMapOf()
        }
        return mapOfOriginalDepth!!
    }

    /** Adds a heap entry for [node] at its current depth, making the arrays bigger if needed. */
    private fun push(node: LayoutNode) {
        if (heapSize == keys.size) {
            // The arrays are full. If most entries are out of date, throw those away first. This
            // stops the arrays from growing forever when nodes are added and removed many times
            // without the set ever becoming empty.
            if (heapSize - size >= size + CompactionSlack) {
                compact()
            }
            if (heapSize == keys.size) {
                // Still full, so make the arrays bigger. Doubling the size means this happens
                // only a few times before the arrays are big enough.
                if (keys.isEmpty()) {
                    keys = LongArray(InitialHeapCapacity)
                    nodes = arrayOfNulls(InitialHeapCapacity)
                } else {
                    val newCapacity = keys.size * 2
                    keys = keys.copyOf(newCapacity)
                    nodes = nodes.copyOf(newCapacity)
                }
            }
        }
        // Put the new entry at the end, then move it up to its right place.
        val index = heapSize++
        keys[index] = heapKey(node.depth, nextSequence++)
        nodes[index] = node
        siftUp(index)
    }

    /**
     * Removes the entry at the front of the heap. The last entry is moved to the front and then
     * moved down to its right place.
     */
    private fun removeTop() {
        val last = --heapSize
        if (last > 0) {
            keys[0] = keys[last]
            nodes[0] = nodes[last]
            // Clear the old slot so the array doesn't keep the node alive.
            nodes[last] = null
            siftDown(0)
        } else {
            nodes[0] = null
        }
    }

    /**
     * Drops every heap entry. The arrays are kept so they can be reused, but their nodes are
     * cleared so the set doesn't keep removed nodes alive.
     */
    private fun clearHeap() {
        nodes.fill(null, 0, heapSize)
        heapSize = 0
        nextSequence = 0
    }

    /**
     * Throws away every out-of-date entry, then puts the heap back in order. An entry is thrown
     * away if its node is no longer in the set, if its depth is from an earlier time the node was
     * in the set, or if it is a second entry for a node that already has one at its current depth.
     */
    fun compact() {
        if (heapSize == size) return
        val keys = keys
        val nodes = nodes
        val bit = membershipBit
        // A temporary bit that marks nodes we have already kept, so a node with two entries at its
        // current depth is only kept once.
        val scratch = bit shl CompactionScratchShift
        var newSize = 0
        // Slide every entry we keep toward the front of the arrays.
        for (i in 0 until heapSize) {
            val node = nodes[i]!!
            val key = keys[i]
            val membership = node.depthSortedSetMembership
            val keep =
                when {
                    membership and bit == 0 || node.owner !== owner -> false
                    heapKeyDepth(key) != node.depth -> false
                    membership and scratch == 0 -> {
                        node.depthSortedSetMembership = membership or scratch
                        true
                    }
                    else -> false
                }
            if (keep) {
                keys[newSize] = key
                nodes[newSize] = node
                newSize++
            }
        }
        nodes.fill(null, newSize, heapSize)
        heapSize = newSize
        // Turn the temporary bit back off.
        for (i in 0 until newSize) {
            val node = nodes[i]!!
            node.depthSortedSetMembership = node.depthSortedSetMembership and scratch.inv()
        }
        // Put the entries back in heap order. Moving each entry down, starting from the last one
        // that has a child and going back to the front, takes time in proportion to the number of
        // entries.
        for (i in (newSize ushr 1) - 1 downTo 0) {
            siftDown(i)
        }
    }

    /**
     * Moves the entry at [index] toward the front until its parent's key is not bigger than its
     * own. In the arrays, the parent of the entry at `i` is at `(i - 1) / 2`.
     */
    private fun siftUp(index: Int) {
        val keys = keys
        val nodes = nodes
        val key = keys[index]
        val node = nodes[index]
        var i = index
        while (i > 0) {
            val parent = (i - 1) ushr 1
            val parentKey = keys[parent]
            if (parentKey <= key) break
            keys[i] = parentKey
            nodes[i] = nodes[parent]
            i = parent
        }
        keys[i] = key
        nodes[i] = node
    }

    /**
     * Moves the entry at [index] toward the back until neither of its children has a smaller key.
     * In the arrays, the children of the entry at `i` are at `2 * i + 1` and `2 * i + 2`.
     */
    private fun siftDown(index: Int) {
        val keys = keys
        val nodes = nodes
        val size = heapSize
        val half = size ushr 1
        val key = keys[index]
        val node = nodes[index]
        var i = index
        while (i < half) {
            var child = 2 * i + 1
            var childKey = keys[child]
            val right = child + 1
            if (right < size) {
                val rightKey = keys[right]
                if (rightKey < childKey) {
                    child = right
                    childKey = rightKey
                }
            }
            if (key <= childKey) break
            keys[i] = childKey
            nodes[i] = nodes[child]
            i = child
        }
        keys[i] = key
        nodes[i] = node
    }

    override fun toString(): String {
        // Only used for debugging. Lists the real nodes in the order they would come out. The
        // heap isn't fully sorted, so the real entries are sorted here first.
        val order = IntArray(heapSize)
        var count = 0
        for (i in 0 until heapSize) {
            val node = nodes[i]!!
            if (
                node.depthSortedSetMembership and membershipBit != 0 &&
                    node.owner === owner &&
                    heapKeyDepth(keys[i]) == node.depth
            ) {
                order[count++] = i
            }
        }
        for (i in 1 until count) {
            val index = order[i]
            var j = i - 1
            while (j >= 0 && keys[order[j]] > keys[index]) {
                order[j + 1] = order[j]
                j--
            }
            order[j + 1] = index
        }
        return buildString {
            append('[')
            var first = true
            for (i in 0 until count) {
                val node = nodes[order[i]]!!
                var duplicate = false
                for (k in 0 until i) {
                    if (nodes[order[k]] === node) {
                        duplicate = true
                        break
                    }
                }
                if (duplicate) continue
                if (!first) append(", ")
                first = false
                append(node)
            }
            append(']')
        }
    }
}

internal class DepthSortedSetsForDifferentPasses(extraAssertions: Boolean) {
    /**
     * [lookaheadAndAncestorMeasureSet] contains nodes that are in lookaheadScope as well as nodes
     * that are ancestors of LookaheadScope or simply outside lookahead scope.
     *
     * // TODO: Check how this works with measure in parent placement The invalidation order is
     * always: [lookaheadAndAncestorMeasureSet] then [lookaheadAndAncestorPlaceSet] before
     * [approachSet].
     *
     * When no LookaheadScope is present in the tree, [approachSet] will be empty.
     */
    private val lookaheadAndAncestorMeasureSet =
        DepthSortedSet(extraAssertions, LookaheadAndAncestorMeasureSetBit)

    /**
     * [lookaheadAndAncestorPlaceSet] contains nodes that require lookahead placement invalidation
     * or nodes outside lookaheadScope that could affect the placement of lookahead scope.
     */
    private val lookaheadAndAncestorPlaceSet =
        DepthSortedSet(extraAssertions, LookaheadAndAncestorPlaceSetBit)

    /**
     * [approachSet] contains nodes that only requires approach invalidations. Only nodes in a
     * LookaheadScope can ever be put in this set.
     */
    private val approachSet = DepthSortedSet(extraAssertions, ApproachSetBit)

    /**
     * Checks if the given node exists in the corresponding set based on the provided
     * [affectsLookahead].
     */
    fun contains(node: LayoutNode, affectsLookahead: Boolean): Boolean {
        val isAncestor = node.lookaheadRoot == null
        val containedInLookaheadAndAncestors =
            lookaheadAndAncestorMeasureSet.contains(node) ||
                lookaheadAndAncestorPlaceSet.contains(node)
        return if (affectsLookahead) {
            !isAncestor && containedInLookaheadAndAncestors
        } else {
            (isAncestor && containedInLookaheadAndAncestors) || approachSet.contains(node)
        }
    }

    /** Checks if the node exists in either set. */
    fun contains(node: LayoutNode): Boolean =
        lookaheadAndAncestorMeasureSet.contains(node) ||
            lookaheadAndAncestorPlaceSet.contains(node) ||
            approachSet.contains(node)

    /**
     * Adds the given node to the corresponding set based on whether its lookahead
     * measurement/placement should be invalidated.
     *
     * Note: When [affectsLookahead] is true, both lookahead and main measure/layout will be
     * triggered as needed (i.e. if the FooPending flag is dirty). Otherwise, lookahead
     * remeasurement/relayout will be skipped.
     */
    fun add(node: LayoutNode, invalidation: Invalidation) {
        when (invalidation) {
            Invalidation.LookaheadMeasurement -> {
                lookaheadAndAncestorMeasureSet.add(node)
                approachSet.add(node)
            }
            Invalidation.LookaheadPlacement -> {
                lookaheadAndAncestorPlaceSet.add(node)
                approachSet.add(node)
            }
            Invalidation.Measurement -> {
                if (node.lookaheadRoot != null) {
                    approachSet.add(node)
                } else {
                    lookaheadAndAncestorMeasureSet.add(node)
                }
            }
            Invalidation.Placement -> {
                if (node.lookaheadRoot != null) {
                    approachSet.add(node)
                } else {
                    lookaheadAndAncestorPlaceSet.add(node)
                }
            }
        }
    }

    /** Remove the [node] from all the sets. */
    fun remove(node: LayoutNode): Boolean {
        val removedFromLookaheadMeasureSet = lookaheadAndAncestorMeasureSet.remove(node)
        val removedFromLookaheadPlaceSet = lookaheadAndAncestorPlaceSet.remove(node)
        return approachSet.remove(node) ||
            removedFromLookaheadMeasureSet ||
            removedFromLookaheadPlaceSet
    }

    /**
     * Pops nodes that require lookahead remeasurement/replacement first until the lookaheadSet is
     * empty, before handling nodes that only require invalidation for the main pass.
     */
    inline fun popEach(
        crossinline block:
            (node: LayoutNode, affectsLookahead: Boolean, relayoutNeeded: Boolean) -> Unit
    ) {
        // Sequence for invalidation: lookaheadAndAncestorMeasureSet, lookaheadAndAncestorPlaceSet,
        // approachSet
        while (true) {
            val affectsLookahead: Boolean
            val relayoutNeeded: Boolean
            val node: LayoutNode
            if (lookaheadAndAncestorMeasureSet.isNotEmpty()) {
                relayoutNeeded = false
                node = lookaheadAndAncestorMeasureSet.pop()
                affectsLookahead = node.lookaheadRoot != null
            } else {
                if (lookaheadAndAncestorPlaceSet.isNotEmpty()) {
                    relayoutNeeded = true
                    node = lookaheadAndAncestorPlaceSet.pop()
                    affectsLookahead = node.lookaheadRoot != null
                } else if (approachSet.isNotEmpty()) {
                    affectsLookahead = false
                    node = approachSet.pop()
                    relayoutNeeded = true
                } else {
                    break
                }
            }
            block(node, affectsLookahead, relayoutNeeded)
        }
    }

    fun isEmpty(): Boolean =
        lookaheadAndAncestorMeasureSet.isEmpty() &&
            approachSet.isEmpty() &&
            lookaheadAndAncestorPlaceSet.isEmpty()

    val affectsLookaheadMeasure: Boolean
        get() =
            // If lookahead measurement has been requested, approach set will add
            // corresponding nodes as well. Therefore if approachSet is empty, no
            // lookahead measurement has been requested.
            approachSet.isNotEmpty() &&
                // If approachSet is not empty, it may be an approach animation,
                // check lookaheadAndAncestorMeasureSet to determine if any lookahead
                // invalidation is needed.
                lookaheadAndAncestorMeasureSet.isNotEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()
}
