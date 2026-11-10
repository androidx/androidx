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

import java.lang.management.ManagementFactory
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DepthSortedSetTest {

    @Test
    fun sortedByDepth() {
        val owner = MockOwner()
        val root = LayoutNode()
        root.attach(owner)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        child1.add(child2)
        val child3 = LayoutNode()
        child2.add(child3)

        val set = DepthSortedSet()
        set.add(child2)
        set.add(child3)
        set.add(root)
        set.add(child1)

        assertEquals(root, set.pop())
        assertEquals(child1, set.pop())
        assertEquals(child2, set.pop())
        assertEquals(child3, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun sortedByDepthWithItemsOfTheSameDepth() {
        val owner = MockOwner()
        val root = LayoutNode()
        root.attach(owner)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        root.add(child2)
        val child3 = LayoutNode()
        child1.add(child3)

        val set = DepthSortedSet()
        set.add(child1)
        set.add(child3)
        set.add(child2)
        set.add(root)

        assertEquals(root, set.pop())
        val result = set.pop()
        if (result === child1) {
            assertEquals(child2, set.pop())
        } else {
            assertEquals(child2, result)
            assertEquals(child1, set.pop())
        }
        assertEquals(child3, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun modifyingSetWhileWeIterate() {
        val owner = MockOwner()
        val root = LayoutNode()
        root.attach(owner)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        child1.add(child2)
        val child3 = LayoutNode()
        child2.add(child3)

        val set = DepthSortedSet()
        set.add(child1)
        set.add(child3)
        var expected: LayoutNode? = child1

        set.popEach {
            assertEquals(expected, it)
            if (expected === child1) {
                set.add(child2)
                set.add(root)
                // now we expect root
                expected = root
            } else if (expected === root) {
                // remove child3 so we will never reach it
                set.remove(child3)
                // now we expect the last item
                expected = child2
            } else {
                assertEquals(child2, it)
                // no other items expected, child3 was removed already
                expected = null
            }
        }
        // assert we iterated the whole set
        assertEquals(null, expected)
    }

    @Test(expected = IllegalStateException::class)
    fun addingNotAttachedNodeThrows() {
        val set = DepthSortedSet()
        set.add(LayoutNode())
    }

    @Test(expected = IllegalStateException::class)
    fun modifyingDepthAfterAddingThrows() {
        val owner = MockOwner()
        val root = LayoutNode()
        root.attach(owner)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        child1.add(child2)

        val set = DepthSortedSet()
        set.add(child2)

        // change depth of child2
        child1.removeAt(0, 1)
        root.add(child2)
        // now it is on the same level as child1

        assertTrue(set.isNotEmpty())
        // throws because we changed the depth
        set.pop()
    }

    @Test
    fun itemsOfTheSameDepthArePoppedInInsertionOrder() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val children = List(5) { LayoutNode().also { root.add(it) } }

        val set = DepthSortedSet()
        set.add(children[3])
        set.add(children[0])
        set.add(children[4])
        set.add(children[1])
        set.add(children[2])

        assertEquals(children[3], set.pop())
        assertEquals(children[0], set.pop())
        assertEquals(children[4], set.pop())
        assertEquals(children[1], set.pop())
        assertEquals(children[2], set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun containsReflectsMembership() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val child = LayoutNode()
        root.add(child)

        val set = DepthSortedSet()
        assertFalse(set.contains(child))
        set.add(child)
        assertTrue(set.contains(child))
        assertTrue(set.remove(child))
        assertFalse(set.contains(child))
        assertFalse(set.remove(child))
        set.add(child)
        assertEquals(child, set.pop())
        assertFalse(set.contains(child))
        assertTrue(set.isEmpty())
    }

    @Test
    fun addingTwiceKeepsOneEntry() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val child = LayoutNode()
        root.add(child)

        val set = DepthSortedSet()
        set.add(child)
        set.add(child)
        set.add(root)

        assertEquals(root, set.pop())
        assertEquals(child, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test(expected = NoSuchElementException::class)
    fun poppingEmptySetThrows() {
        DepthSortedSet().pop()
    }

    @Test
    fun removedNodeReAddedAtShallowerDepthIsPoppedAtNewDepth() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val a = LayoutNode()
        root.add(a)
        val b = LayoutNode()
        a.add(b)
        val moved = LayoutNode()
        b.add(moved)
        val other = LayoutNode()
        a.add(other)

        val set = DepthSortedSet()
        set.add(moved) // depth 3
        set.add(other) // depth 2
        set.remove(moved)
        b.removeAt(0, 1)
        root.add(moved) // depth 1
        set.add(moved)

        assertEquals(moved, set.pop())
        assertEquals(other, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun removedNodeReAddedAtDeeperDepthIsPoppedAtNewDepth() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val a = LayoutNode()
        root.add(a)
        val b = LayoutNode()
        a.add(b)
        val moved = LayoutNode()
        root.add(moved)
        val other = LayoutNode()
        a.add(other)

        val set = DepthSortedSet()
        set.add(moved) // depth 1
        set.add(other) // depth 2
        set.remove(moved)
        root.removeAt(1, 1)
        b.add(moved) // depth 3
        set.add(moved)

        assertEquals(other, set.pop())
        assertEquals(moved, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun nodeReAddedAtDeeperDepthIsPoppedAfterEarlierPeerOfThatDepth() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val a = LayoutNode()
        root.add(a)
        val b = LayoutNode()
        a.add(b)
        val other = LayoutNode()
        b.add(other)
        val moved = LayoutNode()
        root.add(moved)

        val set = DepthSortedSet()
        set.add(moved) // depth 1
        set.add(other) // depth 3
        set.remove(moved)
        root.removeAt(1, 1)
        b.add(moved) // depth 3
        set.add(moved)

        assertEquals(other, set.pop())
        assertEquals(moved, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun nodeMovedToAnotherOwnerIsNotPoppedByTheFirstOwnersSet() {
        val rootA = LayoutNode()
        rootA.attach(MockOwner())
        val rootB = LayoutNode()
        rootB.attach(MockOwner())
        val moved = LayoutNode()
        rootA.add(moved)
        val stays = LayoutNode()
        rootA.add(stays)

        val setsA = DepthSortedSetsForDifferentPasses(extraAssertions = true)
        val setsB = DepthSortedSetsForDifferentPasses(extraAssertions = true)
        setsA.add(moved, Invalidation.Measurement)
        setsA.add(stays, Invalidation.Measurement)
        // setsA is not empty, so it still holds an out-of-date entry for `moved`.
        setsA.remove(moved)
        rootA.removeAt(0, 1)
        rootB.add(moved)
        setsB.add(moved, Invalidation.Measurement)

        val poppedA = mutableListOf<LayoutNode>()
        setsA.popEach { node, _, _ -> poppedA += node }
        assertEquals(listOf(stays), poppedA)
        assertTrue(setsA.isEmpty())
        assertTrue(setsB.contains(moved))

        val poppedB = mutableListOf<LayoutNode>()
        setsB.popEach { node, _, _ -> poppedB += node }
        assertEquals(listOf(moved), poppedB)
        assertTrue(setsB.isEmpty())
    }

    @Test
    fun setsWithDifferentBitsHaveIndependentMembership() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val child = LayoutNode()
        root.add(child)

        val first = DepthSortedSet(extraAssertions = true, membershipBit = 1)
        val second = DepthSortedSet(extraAssertions = true, membershipBit = 2)
        first.add(child)
        second.add(child)
        assertTrue(first.remove(child))
        assertFalse(first.contains(child))
        assertTrue(second.contains(child))
        assertEquals(child, second.pop())
        assertTrue(second.isEmpty())
    }

    @Test
    fun repeatedRemoveAndAddWithoutPopKeepsEachNodeOnce() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val nodes = buildTree(root, count = 200, random = Random(1))

        val set = DepthSortedSet()
        nodes.forEach { set.add(it) }
        // Enough rounds that the set throws away its out-of-date entries many times.
        repeat(20) { nodes.forEach { set.remove(it) } }
        repeat(20) {
            nodes.forEach {
                set.remove(it)
                set.add(it)
            }
        }

        val popped = mutableListOf<LayoutNode>()
        set.popEach { popped += it }
        assertEquals(nodes.toSet(), popped.toSet())
        assertEquals(nodes.size, popped.size)
        for (i in 1 until popped.size) {
            assertTrue(popped[i - 1].depth <= popped[i].depth)
        }
    }

    @Test
    fun randomOperationsMatchReferenceModel() {
        val random = Random(42)
        val root = LayoutNode()
        root.attach(MockOwner())
        val nodes = buildTree(root, count = 300, random = random)

        val set = DepthSortedSet()
        // A simple model to compare against: each node in the set and the order it was added.
        val reference = LinkedHashMap<LayoutNode, Int>()
        var sequence = 0
        repeat(20_000) {
            val node = nodes[random.nextInt(nodes.size)]
            when (random.nextInt(4)) {
                0,
                1 -> {
                    set.add(node)
                    if (node !in reference) reference[node] = sequence++
                }
                2 -> assertEquals(reference.remove(node) != null, set.remove(node))
                else -> {
                    if (reference.isEmpty()) {
                        assertTrue(set.isEmpty())
                    } else {
                        val popped = set.pop()
                        val minDepth = reference.keys.minOf { it.depth }
                        assertEquals(minDepth, popped.depth)
                        assertTrue(reference.remove(popped) != null)
                    }
                }
            }
            assertEquals(reference.isEmpty(), set.isEmpty())
            assertEquals(node in reference, set.contains(node))
        }
        val remaining = mutableListOf<LayoutNode>()
        set.popEach { remaining += it }
        assertEquals(reference.keys, remaining.toSet())
        assertEquals(reference.size, remaining.size)
    }

    @Test
    fun popOrderIsDeterministic() {
        fun popOrder(): List<Int> {
            val random = Random(7)
            val root = LayoutNode()
            root.attach(MockOwner())
            val nodes = buildTree(root, count = 100, random = random)
            val set = DepthSortedSet()
            nodes.shuffled(random).forEach { set.add(it) }
            val order = mutableListOf<Int>()
            set.popEach { order += nodes.indexOf(it) }
            return order
        }
        assertEquals(popOrder(), popOrder())
    }

    @Test
    fun addAndPopDoNotAllocateAfterWarmUp() {
        val threadBean = ManagementFactory.getThreadMXBean()
        assumeTrue(threadBean is com.sun.management.ThreadMXBean)
        threadBean as com.sun.management.ThreadMXBean
        assumeTrue(threadBean.isThreadAllocatedMemorySupported)
        threadBean.isThreadAllocatedMemoryEnabled = true
        val root = LayoutNode()
        root.attach(MockOwner())
        val nodes = buildTree(root, count = 100, random = Random(3)).toTypedArray()
        val set = DepthSortedSet(extraAssertions = false, membershipBit = 1)

        repeat(1000) { addRemoveAndPopAll(set, nodes) }
        val before = threadBean.currentThreadAllocatedBytes
        assertTrue(before >= 0)
        val cycles = 10_000
        repeat(cycles) { addRemoveAndPopAll(set, nodes) }
        val allocated = threadBean.currentThreadAllocatedBytes - before
        // If each round allocated anything, it would be at least one object (16 bytes or more) per
        // round. The limit leaves room for a one-time allocation made by the JVM itself.
        assertTrue("allocated $allocated bytes in $cycles cycles", allocated < cycles)
    }

    @Test
    fun sortedSetsForDifferentPassesTrackMembershipPerSet() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val child = LayoutNode()
        root.add(child)

        val sets = DepthSortedSetsForDifferentPasses(extraAssertions = true)
        sets.add(child, Invalidation.Placement)
        sets.add(root, Invalidation.Measurement)
        assertTrue(sets.contains(child))
        assertTrue(sets.contains(child, affectsLookahead = false))
        assertFalse(sets.contains(child, affectsLookahead = true))

        val popped = mutableListOf<Pair<LayoutNode, Boolean>>()
        sets.popEach { node, _, relayoutNeeded -> popped += node to relayoutNeeded }
        assertEquals(listOf(root to false, child to true), popped)
        assertFalse(sets.contains(child))
        assertTrue(sets.isEmpty())
    }

    @Test
    fun compactRemovesStaleEntriesAndPreservesOrder() {
        val root = LayoutNode()
        root.attach(MockOwner())
        val nodes = buildTree(root, count = 20, random = Random(5))

        val set = DepthSortedSet()
        set.compact() // No-op on empty set.

        nodes.forEach { set.add(it) }
        set.compact() // No-op when heapSize == size.

        val removed = nodes.filterIndexed { index, _ -> index % 2 == 0 }
        val remaining = nodes.filterIndexed { index, _ -> index % 2 != 0 }
        removed.forEach { set.remove(it) }
        set.compact()

        val popped = mutableListOf<LayoutNode>()
        set.popEach { popped += it }
        assertEquals(remaining.toSet(), popped.toSet())
        assertEquals(remaining.size, popped.size)
        for (i in 1 until popped.size) {
            assertTrue(popped[i - 1].depth <= popped[i].depth)
        }
    }
}

private fun addRemoveAndPopAll(set: DepthSortedSet, nodes: Array<LayoutNode>) {
    var i = 0
    while (i < nodes.size) {
        set.add(nodes[i])
        i++
    }
    i = 0
    while (i < nodes.size) {
        set.remove(nodes[i])
        i += 3
    }
    i = 0
    while (i < nodes.size) {
        set.add(nodes[i])
        i += 6
    }
    while (set.isNotEmpty()) set.pop()
}

/** Attaches [count] nodes under [root] at random depths and returns them. */
private fun buildTree(root: LayoutNode, count: Int, random: Random): List<LayoutNode> {
    val all = mutableListOf(root)
    repeat(count) {
        val node = LayoutNode()
        all[random.nextInt(all.size)].add(node)
        all += node
    }
    return all.subList(1, all.size).toList()
}

private fun DepthSortedSet() = DepthSortedSet(extraAssertions = true, membershipBit = 1)
