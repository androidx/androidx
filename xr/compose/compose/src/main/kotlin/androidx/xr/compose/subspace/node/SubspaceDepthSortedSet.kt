/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.collection.MutableObjectIntMap
import androidx.collection.mutableObjectIntMapOf
import java.util.TreeSet

private val SubspaceDepthComparator: Comparator<SubspaceLayoutNode> = Comparator { a, b ->
    val depthDiff = a.depth.compareTo(b.depth)
    if (depthDiff != 0) {
        depthDiff
    } else {
        a.hashCode().compareTo(b.hashCode())
    }
}

/**
 * A set of [SubspaceLayoutNode]s ordered by their [SubspaceLayoutNode.depth]. It allows for
 * modifications (additions and removals) while iterating via [popEach]. A [SubspaceLayoutNode]
 * added to this set must remain attached and its depth must not change, as this would violate the
 * comparator's contract and could lead to inconsistent state.
 *
 * Based on [androidx.compose.ui.node.DepthSortedSet].
 */
internal class SubspaceDepthSortedSet(private val extraAssertions: Boolean = false) {
    // Stores the depth used when the node was added to the set to assert it wasn't changed.
    // This is crucial because changing the depth can break the comparator's contract for the
    // TreeSet.
    private var mapOfOriginalDepth: MutableObjectIntMap<SubspaceLayoutNode>? = null

    private val set = TreeSet(SubspaceDepthComparator)

    fun contains(node: SubspaceLayoutNode): Boolean {
        val contains = set.contains(node)
        if (extraAssertions) {
            check(contains == safeMapOfOriginalDepth().containsKey(node)) {
                "inconsistency in TreeSet"
            }
        }
        return contains
    }

    fun add(node: SubspaceLayoutNode) {
        if (extraAssertions) {
            val map = safeMapOfOriginalDepth()
            val usedDepth = map.getOrDefault(node, Int.MAX_VALUE)
            if (usedDepth == Int.MAX_VALUE) {
                map[node] = node.depth
            } else {
                check(usedDepth == node.depth) { "invalid node depth" }
            }
        }
        set.add(node)
    }

    fun remove(node: SubspaceLayoutNode): Boolean {
        // It's possible we are removing a node that has just been detached.
        if (extraAssertions) {
            val map = safeMapOfOriginalDepth()
            if (map.contains(node)) {
                val usedDepth = map[node]
                map.remove(node)
                val contains = set.remove(node)
                check(usedDepth == if (contains) node.depth else Int.MAX_VALUE) {
                    "invalid node depth"
                }
                return contains
            }
        }
        return set.remove(node)
    }

    fun pop(): SubspaceLayoutNode {
        val node = set.first()
        remove(node)
        return node
    }

    inline fun popEach(crossinline block: (SubspaceLayoutNode) -> Unit) {
        while (isNotEmpty()) {
            val node = pop()
            block(node)
        }
    }

    fun isEmpty(): Boolean = set.isEmpty()

    @Suppress("NOTHING_TO_INLINE") inline fun isNotEmpty(): Boolean = !isEmpty()

    fun toList(): List<SubspaceLayoutNode> = set.toList()

    private fun safeMapOfOriginalDepth(): MutableObjectIntMap<SubspaceLayoutNode> {
        if (mapOfOriginalDepth == null) {
            mapOfOriginalDepth = mutableObjectIntMapOf()
        }
        return mapOfOriginalDepth!!
    }

    override fun toString(): String {
        return set.toString()
    }

    fun clear() {
        set.clear()
    }
}
