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
import androidx.compose.ui.util.fastForEach
import java.util.Comparator
import java.util.SortedSet

/**
 * A set of [SubspaceLayoutNode]s ordered by their [SubspaceLayoutNode.depth]. A
 * [SubspaceLayoutNode] added to this set must remain attached and its depth must not change, as
 * this would violate the comparator's contract and could lead to inconsistent state.
 *
 * Based on [androidx.compose.ui.node.DepthSortedSet].
 */
internal class SubspaceDepthSortedSet(
    private val extraAssertions: Boolean = false,
    private val set: SortedSet<SubspaceLayoutNode> =
        sortedSetOf(compareBy({ it.depth }, { it.hashCode() })),
) : SortedSet<SubspaceLayoutNode> {
    // Stores the depth used when the node was added to the set to assert it wasn't changed.
    // This is crucial because changing the depth can break the comparator's contract for the
    // SortedSet.
    private val mapOfOriginalDepth: MutableObjectIntMap<SubspaceLayoutNode> by lazy {
        mutableObjectIntMapOf()
    }

    fun drain(action: (SubspaceLayoutNode) -> Unit) {
        while (isNotEmpty()) {
            action(removeFirst())
        }
    }

    override fun removeFirst(): SubspaceLayoutNode = first().also { remove(it) }

    override fun removeLast(): SubspaceLayoutNode = last().also { remove(it) }

    override val size: Int
        get() = set.size

    override fun contains(element: SubspaceLayoutNode): Boolean =
        set.contains(element).also { contains ->
            if (extraAssertions) {
                check(contains == (element in mapOfOriginalDepth)) { "inconsistency in SortedSet" }
            }
        }

    override fun containsAll(elements: Collection<SubspaceLayoutNode?>): Boolean {
        return elements.all { contains(it) }
    }

    override fun add(element: SubspaceLayoutNode) =
        set.add(element).also {
            if (extraAssertions) {
                check(mapOfOriginalDepth.getOrPut(element) { element.depth } == element.depth) {
                    "invalid node depth"
                }
            }
        }

    override fun addAll(elements: Collection<SubspaceLayoutNode?>): Boolean {
        val initialSize = size
        elements.forEach { it?.let(::add) }
        return size > initialSize
    }

    override fun remove(element: SubspaceLayoutNode): Boolean =
        set.remove(element).also { removed ->
            if (removed && extraAssertions) {
                check(mapOfOriginalDepth[element] == element.depth) { "invalid node depth" }
                mapOfOriginalDepth -= element
            }
        }

    override fun removeAll(elements: Collection<SubspaceLayoutNode?>): Boolean {
        val initialSize = size
        elements.forEach(::remove)
        return size < initialSize
    }

    override fun retainAll(elements: Collection<SubspaceLayoutNode?>): Boolean {
        val initialSize = size
        set.filter { it !in elements }.fastForEach(::remove)
        return size < initialSize
    }

    override fun comparator(): Comparator<in SubspaceLayoutNode>? = set.comparator()

    override fun subSet(
        fromElement: SubspaceLayoutNode?,
        toElement: SubspaceLayoutNode?,
    ): SubspaceDepthSortedSet? =
        set.subSet(fromElement, toElement)?.let { nextSet ->
            SubspaceDepthSortedSet(extraAssertions = extraAssertions, set = nextSet)
        }

    override fun headSet(toElement: SubspaceLayoutNode?): SubspaceDepthSortedSet? =
        set.headSet(toElement)?.let { nextSet ->
            SubspaceDepthSortedSet(extraAssertions = extraAssertions, set = nextSet)
        }

    override fun tailSet(fromElement: SubspaceLayoutNode?): SubspaceDepthSortedSet? =
        set.tailSet(fromElement)?.let { nextSet ->
            SubspaceDepthSortedSet(extraAssertions = extraAssertions, set = nextSet)
        }

    override fun first(): SubspaceLayoutNode = set.first()

    override fun last(): SubspaceLayoutNode = set.last()

    override fun isEmpty(): Boolean = set.isEmpty()

    override fun clear() = set.clear().also { mapOfOriginalDepth.clear() }

    override fun iterator(): MutableIterator<SubspaceLayoutNode> =
        object : MutableIterator<SubspaceLayoutNode> {
            val backingIterator = set.iterator()
            var lastReturned: SubspaceLayoutNode? = null

            override fun hasNext(): Boolean = backingIterator.hasNext()

            override fun next(): SubspaceLayoutNode =
                backingIterator.next().also { lastReturned = it }

            override fun remove() {
                val elementToRemove =
                    checkNotNull(lastReturned) { "next() must be called before remove()" }
                backingIterator.remove()
                if (extraAssertions) {
                    check(mapOfOriginalDepth[elementToRemove] == elementToRemove.depth) {
                        "invalid node depth"
                    }
                    mapOfOriginalDepth -= elementToRemove
                }
                lastReturned = null
            }
        }
}
