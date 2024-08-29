/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.ui.Modifier
import androidx.compose.ui.util.unpackFloat1
import kotlin.math.min
import kotlin.math.sign

/**
 * This tracks the hit test results to allow for minimum touch target and single-pass hit testing.
 * If there is a hit at the minimum touch target, searching for a hit within the layout bounds can
 * still continue, but the near miss is still tracked.
 *
 * The List<T> interface should only be used after hit testing has completed.
 *
 * @see LayoutNode.hitTest
 * @see NodeCoordinator.hitTest
 */
internal class HitTestResult : List<Modifier.Node> {
    private var values = arrayOfNulls<Any>(16)
    // contains DistanceAndInLayer
    private var distanceFromEdgeAndFlags = LongArray(16)
    private var hitDepth = -1

    override var size: Int = 0
        private set

    /**
     * `true` when there has been a direct hit within touch bounds ([hit] called) or `false`
     * otherwise.
     */
    fun hasHit(): Boolean {
        val distance = findBestHitDistance()
        return distance.distance < 0f && distance.isInLayer && !distance.isInExpandedBounds
    }

    /**
     * Accepts all hits for a child and moves the hit depth. This should only be called within
     * [siblingHits] to allow multiple siblings to have hit results.
     */
    fun acceptHits() {
        hitDepth = size - 1
    }

    private fun resizeToHitDepth() {
        for (i in (hitDepth + 1)..lastIndex) {
            values[i] = null
        }
        size = hitDepth + 1
    }

    /**
     * Returns `true` if [distanceFromEdge] is less than the previous value passed in
     * [hitInMinimumTouchTarget] or [speculativeHit].
     */
    fun isHitInMinimumTouchTargetBetter(distanceFromEdge: Float, isInLayer: Boolean): Boolean {
        if (hitDepth == lastIndex) {
            return true
        }
        val distanceAndFlags = DistanceAndFlags(distanceFromEdge, isInLayer)
        val bestDistance = findBestHitDistance()
        return bestDistance > distanceAndFlags
    }

    private fun findBestHitDistance(): DistanceAndFlags {
        var bestDistance = DistanceAndFlags(Float.POSITIVE_INFINITY, false)
        for (i in hitDepth + 1..lastIndex) {
            val distance = DistanceAndFlags(distanceFromEdgeAndFlags[i])
            bestDistance = if (distance < bestDistance) distance else bestDistance
            if (bestDistance.distance < 0f && bestDistance.isInLayer) {
                return bestDistance
            }
        }
        return bestDistance
    }

    /**
     * Records [node] as a hit, adding it to the [HitTestResult] or replacing the existing one. Runs
     * [childHitTest] to do further hit testing for children.
     */
    inline fun hit(node: Modifier.Node, isInLayer: Boolean, childHitTest: () -> Unit) {
        hitInMinimumTouchTarget(node, -1f, isInLayer, childHitTest)
    }

    inline fun hitInMinimumTouchTarget(
        node: Modifier.Node,
        distanceFromEdge: Float,
        isInLayer: Boolean,
        childHitTest: () -> Unit
    ) = hitInMinimumTouchTarget(node, distanceFromEdge, isInLayer, false, childHitTest)

    /**
     * Records [node] as a hit with [distanceFromEdge] distance, replacing any existing record. Runs
     * [childHitTest] to do further hit testing for children.
     */
    inline fun hitInMinimumTouchTarget(
        node: Modifier.Node,
        distanceFromEdge: Float,
        isInLayer: Boolean,
        isInExpandedBounds: Boolean,
        childHitTest: () -> Unit
    ) {
        val startDepth = hitDepth
        hitDepth++
        ensureContainerSize()
        values[hitDepth] = node
        distanceFromEdgeAndFlags[hitDepth] =
            DistanceAndFlags(distanceFromEdge, isInLayer, isInExpandedBounds).packedValue
        resizeToHitDepth()
        childHitTest()
        hitDepth = startDepth
    }

    /**
     * Records a hit in an expanded touch bound. It's similar to a speculative hit, except that if
     * the previous hit is also in expanded touch, it'll share the pointer.
     */
    fun hitExpandedTouchBounds(node: Modifier.Node, isInLayer: Boolean, childHitTest: () -> Unit) {
        if (hitDepth == lastIndex) {
            // No hit test on siblings yet, simply record hit on this node.
            hitInMinimumTouchTarget(node, 0f, isInLayer, isInExpandedBounds = true, childHitTest)
            return
        }

        val previousDistance = findBestHitDistance()
        val previousHitDepth = hitDepth

        if (previousDistance.isInExpandedBounds) {
            // Previous hits was in expanded bounds. Share the pointer unless the childHitTest has
            // a direct hit.
            // Accept the previous hit result for now, and shuffle the array only when we have a
            // direct hit.
            hitDepth = lastIndex
            hitInMinimumTouchTarget(node, 0f, isInLayer, isInExpandedBounds = true, childHitTest)
            val newDistance = findBestHitDistance()
            if (newDistance.distance < 0) {
                // Have a direct hit, remove the previous hit result.
                val startIndex = previousHitDepth + 1
                val endIndex = hitDepth + 1
                removeNodesInRange(startIndex, endIndex)
                // Discard the remainder of the hits
                hitDepth = previousHitDepth + size - endIndex
            } else {
                // No direct hit, and we only hit the expanded bounds of this node.
                // Accept the new hit result.
                hitDepth = lastIndex
            }
            resizeToHitDepth()
            hitDepth = previousHitDepth
        } else if (previousDistance.distance > 0) {
            // Previous hit is out of expanded bounds, clear the previous hit and record a hit for
            // this node.
            hitInMinimumTouchTarget(node, 0f, isInLayer, isInExpandedBounds = true, childHitTest)
        }
        // If previous hit is a direct hit on sibling, do nothing.
        // This case should never happen, because when a node gets a direct hit, siblingHitTest will
        // stop the hit test for other children.
    }

    /**
     * Temporarily records [node] as a hit with [distanceFromEdge] distance and calls [childHitTest]
     * to record hits for children. If no children have hits, then the hit is discarded. If a child
     * had a hit, then [node] replaces an existing hit.
     */
    fun speculativeHit(
        node: Modifier.Node,
        distanceFromEdge: Float,
        isInLayer: Boolean,
        childHitTest: () -> Unit
    ) {
        if (hitDepth == lastIndex) {
            // Speculation is easy. We don't have to do any array shuffling.
            hitInMinimumTouchTarget(node, distanceFromEdge, isInLayer, childHitTest)
            if (hitDepth + 1 == lastIndex) {
                // Discard the hit because there were no child hits.
                resizeToHitDepth()
            } else if (findBestHitDistance().isInExpandedBounds) {
                // A parent can't intercept the event if child is at its expanded touch bounds.
                // Remove this node at hitDepth + 1 from the hit path.
                // Note: We don't need to check whether this node intercepts child events,
                // because speculativeHit() is only called when
                // node.interceptOutOfBoundsChildEvents() returns true.
                removeNodeAtDepth(hitDepth + 1)
            }
            return
        }

        // We have to track the speculation to the end of the array
        val previousDistance = findBestHitDistance()
        val previousHitDepth = hitDepth
        hitDepth = lastIndex

        hitInMinimumTouchTarget(node, distanceFromEdge, isInLayer, childHitTest)
        val newBestDistance = findBestHitDistance()
        if (hitDepth + 1 < lastIndex && previousDistance > newBestDistance) {
            // This was a successful hit, so we should remove the previous hit from the hit path.
            val startIndex = previousHitDepth + 1
            val endIndex =
                if (newBestDistance.isInExpandedBounds) {
                    // If the new hit is in expanded touch bounds, this node can't intercept the
                    // event. We'll remove this node as well.
                    hitDepth + 2
                } else {
                    hitDepth + 1
                }
            removeNodesInRange(startIndex, endIndex)

            // Discard the remainder of the hits
            hitDepth = previousHitDepth + size - endIndex
        }
        resizeToHitDepth()
        hitDepth = previousHitDepth
    }

    /**
     * Util method to remove a node at the given depth. The given depth must be in the range of [0,
     * size).
     */
    private fun removeNodeAtDepth(depth: Int) {
        if (depth < lastIndex) {
            removeNodesInRange(depth, depth + 1)
        }
        values[lastIndex] = null
        size = lastIndex
    }

    /**
     * Util method to remove nodes at the given depth range. It only updates the [values] and
     * [distanceFromEdgeAndFlags], the caller must update the [hitDepth] and [size] accordingly.
     */
    private fun removeNodesInRange(startDepth: Int, endDepth: Int) {
        if (startDepth >= endDepth) {
            return
        }
        values.copyInto(
            destination = values,
            destinationOffset = startDepth,
            startIndex = endDepth,
            endIndex = size
        )
        distanceFromEdgeAndFlags.copyInto(
            destination = distanceFromEdgeAndFlags,
            destinationOffset = startDepth,
            startIndex = endDepth,
            endIndex = size
        )
    }

    /**
     * Allow multiple sibling children to have a target hit within a Layout. Use [acceptHits] within
     * [block] to mark a child's hits as accepted and proceed to hit test more children.
     */
    inline fun siblingHits(block: () -> Unit) {
        val depth = hitDepth
        block()
        hitDepth = depth
    }

    private fun ensureContainerSize() {
        if (hitDepth >= values.size) {
            val newSize = values.size + 16
            values = values.copyOf(newSize)
            distanceFromEdgeAndFlags = distanceFromEdgeAndFlags.copyOf(newSize)
        }
    }

    override fun contains(element: Modifier.Node): Boolean = indexOf(element) != -1

    override fun containsAll(elements: Collection<Modifier.Node>): Boolean {
        elements.forEach {
            if (!contains(it)) {
                return false
            }
        }
        return true
    }

    override fun get(index: Int): Modifier.Node = values[index] as Modifier.Node

    override fun indexOf(element: Modifier.Node): Int {
        for (i in 0..lastIndex) {
            if (values[i] == element) {
                return i
            }
        }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): Iterator<Modifier.Node> = HitTestResultIterator()

    override fun lastIndexOf(element: Modifier.Node): Int {
        for (i in lastIndex downTo 0) {
            if (values[i] == element) {
                return i
            }
        }
        return -1
    }

    override fun listIterator(): ListIterator<Modifier.Node> = HitTestResultIterator()

    override fun listIterator(index: Int): ListIterator<Modifier.Node> =
        HitTestResultIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<Modifier.Node> =
        SubList(fromIndex, toIndex)

    /** Clears all entries to make an empty list. */
    fun clear() {
        hitDepth = -1
        resizeToHitDepth()
    }

    private inner class HitTestResultIterator(
        var index: Int = 0,
        val minIndex: Int = 0,
        val maxIndex: Int = size
    ) : ListIterator<Modifier.Node> {
        override fun hasNext(): Boolean = index < maxIndex

        override fun hasPrevious(): Boolean = index > minIndex

        @Suppress("UNCHECKED_CAST")
        override fun next(): Modifier.Node = values[index++] as Modifier.Node

        override fun nextIndex(): Int = index - minIndex

        @Suppress("UNCHECKED_CAST")
        override fun previous(): Modifier.Node = values[--index] as Modifier.Node

        override fun previousIndex(): Int = index - minIndex - 1
    }

    private inner class SubList(val minIndex: Int, val maxIndex: Int) : List<Modifier.Node> {
        override val size: Int
            get() = maxIndex - minIndex

        override fun contains(element: Modifier.Node): Boolean = indexOf(element) != -1

        override fun containsAll(elements: Collection<Modifier.Node>): Boolean {
            elements.forEach {
                if (!contains(it)) {
                    return false
                }
            }
            return true
        }

        override fun get(index: Int): Modifier.Node = values[index + minIndex] as Modifier.Node

        override fun indexOf(element: Modifier.Node): Int {
            for (i in minIndex..maxIndex) {
                if (values[i] == element) {
                    return i - minIndex
                }
            }
            return -1
        }

        override fun isEmpty(): Boolean = size == 0

        override fun iterator(): Iterator<Modifier.Node> =
            HitTestResultIterator(minIndex, minIndex, maxIndex)

        override fun lastIndexOf(element: Modifier.Node): Int {
            for (i in maxIndex downTo minIndex) {
                if (values[i] == element) {
                    return i - minIndex
                }
            }
            return -1
        }

        override fun listIterator(): ListIterator<Modifier.Node> =
            HitTestResultIterator(minIndex, minIndex, maxIndex)

        override fun listIterator(index: Int): ListIterator<Modifier.Node> =
            HitTestResultIterator(minIndex + index, minIndex, maxIndex)

        override fun subList(fromIndex: Int, toIndex: Int): List<Modifier.Node> =
            SubList(minIndex + fromIndex, minIndex + toIndex)
    }
}

private const val IS_IN_LAYER: Long = 1L
private const val IS_IN_EXPANDED_BOUNDS: Long = 1 shl 1

@kotlin.jvm.JvmInline
internal value class DistanceAndFlags(val packedValue: Long) {
    val distance: Float
        get() = unpackFloat1(packedValue)

    val isInLayer: Boolean
        get() = (packedValue and IS_IN_LAYER) != 0L

    val isInExpandedBounds: Boolean
        get() = (packedValue and IS_IN_EXPANDED_BOUNDS) != 0L

    operator fun compareTo(other: DistanceAndFlags): Int {
        val thisIsInLayer = isInLayer
        val otherIsInLayer = other.isInLayer
        if (thisIsInLayer != otherIsInLayer) {
            return if (thisIsInLayer) -1 else 1
        }
        val distanceDiff = sign(this.distance - other.distance).toInt()
        // One has a direct hit, use distance for comparison.
        if (min(this.distance, other.distance) < 0f) {
            return distanceDiff
        }
        // Both are out of bounds hit, the one in the expanded touch bounds wins.
        if (this.isInExpandedBounds != other.isInExpandedBounds) {
            return if (this.isInExpandedBounds) -1 else 1
        }
        return distanceDiff
    }
}

private fun DistanceAndFlags(
    distance: Float,
    isInLayer: Boolean,
    isInExpandedBounds: Boolean = false
): DistanceAndFlags {
    val v1 = distance.toRawBits().toLong()
    var v2 = if (isInLayer) IS_IN_LAYER else 0L
    v2 = v2 or if (isInExpandedBounds) IS_IN_EXPANDED_BOUNDS else 0L
    return DistanceAndFlags(v1.shl(32) or (v2 and 0xFFFFFFFF))
}
