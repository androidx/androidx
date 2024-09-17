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

package androidx.compose.ui.graphics

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmField
import kotlin.math.max
import kotlin.math.min

// TODO: We should probably move this to androidx.collection

/**
 * Interval in an [IntervalTree]. The interval is defined between a [start] and an [end] coordinate,
 * whose meanings are defined by the caller. An interval can also hold arbitrary [data] to be used
 * to looking at the result of queries with [IntervalTree.findOverlaps].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
open class Interval<T>(val start: Float, val end: Float, val data: T? = null) {
    /** Returns trues if this interval overlaps with another interval. */
    fun overlaps(other: Interval<T>) = start <= other.end && end >= other.start

    /**
     * Returns trues if this interval overlaps with the interval defined by [start] and [end].
     * [start] must be less than or equal to [end].
     */
    fun overlaps(start: Float, end: Float) = this.start <= end && this.end >= start

    /** Returns true if this interval contains [value]. */
    operator fun contains(value: Float) = value in start..end

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Interval<*>

        if (start != other.start) return false
        if (end != other.end) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + (data?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Interval(start=$start, end=$end, data=$data)"
    }
}

/** Represents an empty/invalid interval. */
internal val EmptyInterval: Interval<Any?> = Interval(Float.MAX_VALUE, Float.MIN_VALUE, null)

/**
 * An interval tree holds a list of intervals and allows for fast queries of intervals that overlap
 * any given interval. This can be used for instance to perform fast spatial queries like finding
 * all the segments in a path that overlap with a given vertical interval.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class IntervalTree<T> {
    // Note: this interval tree is implemented as a binary red/black tree that gets
    // re-balanced on updates. There's nothing notable about this particular data
    // structure beyond what can be found in various descriptions of binary search
    // trees and red/black trees

    @JvmField
    internal val terminator = Node(Float.MAX_VALUE, Float.MIN_VALUE, null, TreeColor.Black)
    @JvmField internal var root = terminator
    @JvmField internal val stack = ArrayList<Node>()

    /**
     * Clears this tree and prepares it for reuse. After calling [clear], any call to [findOverlaps]
     * returns false.
     */
    fun clear() {
        root = terminator
    }

    /**
     * Finds the first interval that overlaps with the specified [interval]. If no overlap can be
     * found, return [EmptyInterval].
     */
    fun findFirstOverlap(interval: ClosedFloatingPointRange<Float>) =
        findFirstOverlap(interval.start, interval.endInclusive)

    /**
     * Finds the first interval that overlaps with the interval defined by [start] and [end]. If no
     * overlap can be found, return [EmptyInterval]. [start] *must* be lesser than or equal to
     * [end].
     */
    fun findFirstOverlap(start: Float, end: Float = start): Interval<T> {
        if (root !== terminator) {
            forEach(start, end) { interval ->
                return interval
            }
        }
        @Suppress("UNCHECKED_CAST") return EmptyInterval as Interval<T>
    }

    /**
     * Finds all the intervals that overlap with the specified [interval]. If [results] is
     * specified, [results] is returned, otherwise a new [MutableList] is returned.
     */
    fun findOverlaps(
        interval: ClosedFloatingPointRange<Float>,
        results: MutableList<Interval<T>> = mutableListOf()
    ) = findOverlaps(interval.start, interval.endInclusive, results)

    /**
     * Finds all the intervals that overlap with the interval defined by [start] and [end]. [start]
     * *must* be lesser than or equal to [end]. If [results] is specified, [results] is returned,
     * otherwise a new [MutableList] is returned.
     */
    fun findOverlaps(
        start: Float,
        end: Float = start,
        results: MutableList<Interval<T>> = mutableListOf()
    ): MutableList<Interval<T>> {
        forEach(start, end) { interval -> results.add(interval) }
        return results
    }

    /** Executes [block] for each interval that overlaps the specified [interval]. */
    internal inline fun forEach(
        interval: ClosedFloatingPointRange<Float>,
        block: (Interval<T>) -> Unit
    ) = forEach(interval.start, interval.endInclusive, block)

    /**
     * Executes [block] for each interval that overlaps with the interval defined by [start] and
     * [end]. [start] *must* be lesser than or equal to [end].
     */
    internal inline fun forEach(start: Float, end: Float = start, block: (Interval<T>) -> Unit) {
        if (root !== terminator) {
            val s = stack
            s.add(root)
            while (s.size > 0) {
                val node = s.removeAt(s.size - 1)
                if (node.overlaps(start, end)) block(node)
                if (node.left !== terminator && node.left.max >= start) {
                    s.add(node.left)
                }
                if (node.right !== terminator && node.right.min <= end) {
                    s.add(node.right)
                }
            }
            s.clear()
        }
    }

    /** Returns true if [value] is inside any of the intervals in this tree. */
    operator fun contains(value: Float) = findFirstOverlap(value, value) !== EmptyInterval

    /** Returns true if the specified [interval] overlaps with any of the intervals in this tree. */
    operator fun contains(interval: ClosedFloatingPointRange<Float>) =
        findFirstOverlap(interval.start, interval.endInclusive) !== EmptyInterval

    operator fun iterator(): Iterator<Interval<T>> {
        return object : Iterator<Interval<T>> {
            var next = root.lowestNode()

            override fun hasNext(): Boolean {
                return next !== terminator
            }

            override fun next(): Interval<T> {
                val node = next
                next = next.next()
                return node
            }
        }
    }

    /** Adds the specified [Interval] to the interval tree. */
    operator fun plusAssign(interval: Interval<T>) {
        addInterval(interval.start, interval.end, interval.data)
    }

    /**
     * Adds the interval defined between a [start] and an [end] coordinate.
     *
     * @param start The start coordinate of the interval
     * @param end The end coordinate of the interval, must be >= [start]
     * @param data Data to associate with the interval
     */
    fun addInterval(start: Float, end: Float, data: T?) {
        val node = Node(start, end, data, TreeColor.Red)

        // Update the tree without doing any balancing
        var current = root
        var parent = terminator

        while (current !== terminator) {
            parent = current
            current =
                if (node.start <= current.start) {
                    current.left
                } else {
                    current.right
                }
        }

        node.parent = parent

        if (parent === terminator) {
            root = node
        } else {
            if (node.start <= parent.start) {
                parent.left = node
            } else {
                parent.right = node
            }
        }

        updateNodeData(node)

        rebalance(node)
    }

    private fun rebalance(target: Node) {
        var node = target

        while (node !== root && node.parent.color == TreeColor.Red) {
            val ancestor = node.parent.parent
            if (node.parent === ancestor.left) {
                val right = ancestor.right
                if (right.color == TreeColor.Red) {
                    right.color = TreeColor.Black
                    node.parent.color = TreeColor.Black
                    ancestor.color = TreeColor.Red
                    node = ancestor
                } else {
                    if (node === node.parent.right) {
                        node = node.parent
                        rotateLeft(node)
                    }
                    node.parent.color = TreeColor.Black
                    ancestor.color = TreeColor.Red
                    rotateRight(ancestor)
                }
            } else {
                val left = ancestor.left
                if (left.color == TreeColor.Red) {
                    left.color = TreeColor.Black
                    node.parent.color = TreeColor.Black
                    ancestor.color = TreeColor.Red
                    node = ancestor
                } else {
                    if (node === node.parent.left) {
                        node = node.parent
                        rotateRight(node)
                    }
                    node.parent.color = TreeColor.Black
                    ancestor.color = TreeColor.Red
                    rotateLeft(ancestor)
                }
            }
        }

        root.color = TreeColor.Black
    }

    private fun rotateLeft(node: Node) {
        val right = node.right
        node.right = right.left

        if (right.left !== terminator) {
            right.left.parent = node
        }

        right.parent = node.parent

        if (node.parent === terminator) {
            root = right
        } else {
            if (node.parent.left === node) {
                node.parent.left = right
            } else {
                node.parent.right = right
            }
        }

        right.left = node
        node.parent = right

        updateNodeData(node)
    }

    private fun rotateRight(node: Node) {
        val left = node.left
        node.left = left.right

        if (left.right !== terminator) {
            left.right.parent = node
        }

        left.parent = node.parent

        if (node.parent === terminator) {
            root = left
        } else {
            if (node.parent.right === node) {
                node.parent.right = left
            } else {
                node.parent.left = left
            }
        }

        left.right = node
        node.parent = left

        updateNodeData(node)
    }

    private fun updateNodeData(node: Node) {
        var current = node
        while (current !== terminator) {
            current.min = min(current.start, min(current.left.min, current.right.min))
            current.max = max(current.end, max(current.left.max, current.right.max))
            current = current.parent
        }
    }

    internal enum class TreeColor {
        Red,
        Black
    }

    internal inner class Node(start: Float, end: Float, data: T?, var color: TreeColor) :
        Interval<T>(start, end, data) {
        var min: Float = start
        var max: Float = end

        var left: Node = terminator
        var right: Node = terminator
        var parent: Node = terminator

        fun lowestNode(): Node {
            var node = this
            while (node.left !== terminator) {
                node = node.left
            }
            return node
        }

        fun next(): Node {
            if (right !== terminator) {
                return right.lowestNode()
            }

            var a = this
            var b = parent
            while (b !== terminator && a === b.right) {
                a = b
                b = b.parent
            }

            return b
        }
    }
}
