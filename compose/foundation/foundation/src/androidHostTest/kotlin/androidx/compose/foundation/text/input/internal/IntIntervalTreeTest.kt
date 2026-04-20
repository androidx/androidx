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

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import kotlin.String
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntIntervalTreeTest {

    @Test
    fun basicAddAndGet() {
        val buffer = IntIntervalTree<String>()
        buffer.addIntervalAndVerifyIntegrity("style1", 0, 10)
        buffer.addIntervalAndVerifyIntegrity("style2", 5, 15)

        val styles = buffer.getStyles(0, 20)
        assertThat(styles)
            .containsExactly(
                AnnotatedString.Range("style1", 0, 10),
                AnnotatedString.Range("style2", 5, 15),
            )
    }

    @Test
    fun getStyles_exclusiveEnd_and_collapsedRanges() {
        val buffer = IntIntervalTree<String>()
        buffer.addIntervalAndVerifyIntegrity("style", 10, 20)

        // Exact match
        assertThat(buffer.getStyles(10, 20)).hasSize(1)

        // Collapsed query strictly inside style
        assertThat(buffer.getStyles(15, 15)).containsExactly(AnnotatedString.Range("style", 10, 20))

        // Collapsed query at style start
        assertThat(buffer.getStyles(10, 10)).containsExactly(AnnotatedString.Range("style", 10, 20))

        // Collapsed query at style end
        assertThat(buffer.getStyles(20, 20)).isEmpty()

        // Query range touching style start
        assertThat(buffer.getStyles(0, 10)).isEmpty()

        // Query range touching style end
        assertThat(buffer.getStyles(20, 30)).isEmpty()
    }

    @Test
    fun addStyle_collapsedStyleRange() {
        val buffer = IntIntervalTree<String>()
        assertFalse(buffer.addIntervalAndVerifyIntegrity("collapsed", 10, 10))
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun removeStyle_withSuccessorNotImmediateChild() {
        val buffer = IntIntervalTree<Int>()
        // We want to create a situation where a node to be deleted has two children,
        // and its successor is not its immediate right child.
        // Successor of 20 is 25.
        // Tree:
        //      20
        //     /  \
        //    10   40
        //        /  \
        //       25   50
        //        \
        //         30
        buffer.addIntervalAndVerifyIntegrity(20, 20, 21)
        buffer.addIntervalAndVerifyIntegrity(10, 10, 11)
        buffer.addIntervalAndVerifyIntegrity(40, 40, 41)
        buffer.addIntervalAndVerifyIntegrity(25, 25, 26)
        buffer.addIntervalAndVerifyIntegrity(50, 50, 51)
        buffer.addIntervalAndVerifyIntegrity(30, 30, 31)

        // Now delete 20. Its successor is 25.
        // 25 is 40's left child, not 20's immediate right child.
        buffer.removeIntervalAndVerifyIntegrity(20, 20, 21)

        assertThat(buffer.getAllStyles().map { it.item })
            .containsExactly(10, 40, 25, 50, 30)
            .inOrder()
    }

    @Test
    fun getStyles_witSameRanges() {
        val buffer = IntIntervalTree<Int>()

        buffer.addIntervalAndVerifyIntegrity(1, 10, 20)
        buffer.addIntervalAndVerifyIntegrity(2, 10, 20)
        buffer.addIntervalAndVerifyIntegrity(3, 10, 20)
        buffer.addIntervalAndVerifyIntegrity(4, 10, 20)
        buffer.addIntervalAndVerifyIntegrity(5, 10, 20)
        buffer.addIntervalAndVerifyIntegrity(6, 10, 20)

        // This verifies that the pruning logic in getStyles considered the case where multiple
        // style intervals' start are the same.
        assertThat(buffer.getStyles(10, 10).map { it.item })
            .containsExactly(1, 2, 3, 4, 5, 6)
            .inOrder()
    }

    @Test
    fun equals_sameStylesSameOrder() {
        val buffer1 = IntIntervalTree<String>()
        buffer1.addIntervalAndVerifyIntegrity("a", 0, 10)
        buffer1.addIntervalAndVerifyIntegrity("b", 10, 20)

        val buffer2 = IntIntervalTree<String>()
        buffer2.addIntervalAndVerifyIntegrity("a", 0, 10)
        buffer2.addIntervalAndVerifyIntegrity("b", 10, 20)

        assertThat(buffer1).isEqualTo(buffer2)
        assertThat(buffer1.hashCode()).isEqualTo(buffer2.hashCode())
    }

    @Test
    fun equals_sameStylesDifferentOrder() {
        val buffer1 = IntIntervalTree<String>()
        buffer1.addIntervalAndVerifyIntegrity("a", 0, 10)
        buffer1.addIntervalAndVerifyIntegrity("b", 10, 20)

        val buffer2 = IntIntervalTree<String>()
        buffer2.addIntervalAndVerifyIntegrity("b", 10, 20)
        buffer2.addIntervalAndVerifyIntegrity("a", 0, 10)

        // Implementation compares styles in addition order, so they should NOT be equal
        assertThat(buffer1).isNotEqualTo(buffer2)
    }

    @Test
    fun equals_withDeletedNodes() {
        val buffer1 = IntIntervalTree<String>()
        buffer1.addIntervalAndVerifyIntegrity("a", 0, 10)
        buffer1.addIntervalAndVerifyIntegrity("b", 10, 20)
        buffer1.addIntervalAndVerifyIntegrity("c", 20, 30)
        buffer1.removeIntervalAndVerifyIntegrity("b", 10, 20)

        val buffer2 = IntIntervalTree<String>()
        buffer2.addIntervalAndVerifyIntegrity("a", 0, 10)
        buffer2.addIntervalAndVerifyIntegrity("c", 20, 30)

        // Active styles are "a" and "c" in both, so they should be equal
        assertThat(buffer1).isEqualTo(buffer2)
        assertThat(buffer1.hashCode()).isEqualTo(buffer2.hashCode())
    }

    @Test
    fun copy_isEqualAndIndependent() {
        val original = IntIntervalTree<String>()
        original.addIntervalAndVerifyIntegrity("a", 0, 10)
        original.addIntervalAndVerifyIntegrity("b", 10, 20)

        val copy = original.copy()
        assertThat(copy).isEqualTo(original)
        assertThat(copy.hashCode()).isEqualTo(original.hashCode())

        // Modify copy, original should remain unchanged
        copy.addIntervalAndVerifyIntegrity("c", 20, 30)
        assertThat(copy).isNotEqualTo(original)
        assertThat(original.getAllStyles()).hasSize(2)
        assertThat(copy.getAllStyles()).hasSize(3)

        // Modify original, copy should remain unchanged
        original.addIntervalAndVerifyIntegrity("d", 30, 40)
        assertThat(original.getAllStyles()).hasSize(3)
        assertThat(copy.getAllStyles()).hasSize(3)
        assertThat(original.getAllStyles().last().item).isEqualTo("d")
        assertThat(copy.getAllStyles().last().item).isEqualTo("c")
    }

    @Test
    fun syncTo_isEqualAndIndependent() {
        val target = IntIntervalTree<String>()
        target.addIntervalAndVerifyIntegrity("a", 0, 10)

        val source = IntIntervalTree<String>()
        source.addIntervalAndVerifyIntegrity("b", 10, 20)
        source.addIntervalAndVerifyIntegrity("c", 20, 30)

        // syncTo should overwrite the target with the source
        target.syncTo(source)
        assertThat(target).isEqualTo(source)
        assertThat(target.hashCode()).isEqualTo(source.hashCode())

        // Modify target, source should remain unchanged
        target.addIntervalAndVerifyIntegrity("d", 30, 40)
        assertThat(target).isNotEqualTo(source)
        assertThat(source.getAllStyles()).hasSize(2)
        assertThat(target.getAllStyles()).hasSize(3)

        // Modify source, target should remain unchanged
        source.addIntervalAndVerifyIntegrity("e", 40, 50)
        assertThat(source.getAllStyles()).hasSize(3)
        assertThat(target.getAllStyles()).hasSize(3)
        assertThat(source.getAllStyles().last().item).isEqualTo("e")
        assertThat(target.getAllStyles().last().item).isEqualTo("d")
    }

    @Test
    fun testRebalancing_sequentialInsertions() {
        // The tree property should be maintained in the following test cases.
        val buffer = IntIntervalTree<Int>()
        // Increasing starts -> left rotations
        for (i in 0..99) {
            buffer.addIntervalAndVerifyIntegrity(i, i * 10, i * 10 + 5)
        }
        assertThat(buffer.getAllStyles()).hasSize(100)

        // Decreasing starts -> right rotations
        val buffer2 = IntIntervalTree<Int>()
        for (i in 99 downTo 0) {
            buffer2.addIntervalAndVerifyIntegrity(i, i * 10, i * 10 + 5)
        }
        assertThat(buffer2.getAllStyles()).hasSize(100)
    }

    @Test
    fun testEmptyAndReFill() {
        val buffer = IntIntervalTree<String>()
        buffer.addIntervalAndVerifyIntegrity("a", 0, 10)
        buffer.removeIntervalAndVerifyIntegrity("a", 0, 10)
        assertThat(buffer.getAllStyles()).isEmpty()

        buffer.addIntervalAndVerifyIntegrity("b", 0, 10)
        assertThat(buffer.getStyles(0, 10)).containsExactly(AnnotatedString.Range("b", 0, 10))
    }

    @Test
    fun testClear() {
        val buffer = IntIntervalTree<String>()
        buffer.addIntervalAndVerifyIntegrity("a", 0, 10)
        buffer.addIntervalAndVerifyIntegrity("b", 10, 20)
        assertThat(buffer.getAllStyles()).hasSize(2)

        buffer.clear()
        assertThat(buffer.getAllStyles()).isEmpty()
        assertThat(buffer.getStyles(0, 20)).isEmpty()

        buffer.addIntervalAndVerifyIntegrity("c", 5, 15)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("c", 5, 15))
    }

    @Test
    fun getStyles_afterMultipleInsertionsAndDeletions() {
        val buffer = IntIntervalTree<Int>()
        repeat(100) { buffer.addIntervalAndVerifyIntegrity(it, it, it + 1) }

        assertThat(buffer.getAllStyles())
            .isEqualTo((0..99).map { AnnotatedString.Range(it, it, it + 1) })

        repeat(100) {
            if (it % 5 == 0 || it % 7 == 0) {
                buffer.removeIntervalAndVerifyIntegrity(it, it, it + 1)
            }
        }

        val expected =
            (0..99)
                .filter { it % 5 != 0 && it % 7 != 0 }
                .map { AnnotatedString.Range(it, it, it + 1) }
        assertThat(buffer.getAllStyles()).isEqualTo(expected)

        for (start in 0 until 100) {
            for (end in start + 1 until 100) {
                val expectedRanges =
                    (start until end)
                        .filter { it % 5 != 0 && it % 7 != 0 }
                        .map { AnnotatedString.Range(it, it, it + 1) }
                assertThat(buffer.getStyles(start, end)).isEqualTo(expectedRanges)
            }
        }
    }

    @Test
    fun stressTest() {
        val tree = IntIntervalTree<Int>()
        val reference = ReferenceIntervalBuffer<Int>()
        val random = Random(42)

        repeat(1000) {
            val op = random.nextInt(4)
            when (op) {
                0 -> { // Add
                    val start = random.nextInt(0, 100)
                    val end = random.nextInt(start, 100 + 1)
                    val item = random.nextInt()
                    tree.addIntervalAndVerifyIntegrity(item, start, end)
                    reference.addInterval(item, start, end)
                }
                1 -> { // Remove
                    if (reference.intervals.isNotEmpty()) {
                        val index = random.nextInt(reference.intervals.size)
                        val (item, start, end) = reference.intervals[index]
                        tree.removeIntervalAndVerifyIntegrity(item, start, end)
                        reference.removeInterval(item, start, end)
                    }
                }
                2 -> { // Query
                    val start = random.nextInt(0, 100)
                    val end = random.nextInt(start, 100 + 1)
                    val treeResult = tree.getStyles(start, end)
                    val refResult = reference.getStyles(start, end)
                    assertThat(treeResult).isEqualTo(refResult)
                }
                3 -> { // Map
                    val start = random.nextInt(0, 100)
                    val end = random.nextInt(start, 100 + 1)

                    val offset = random.nextInt(-100, 100)
                    val mapper: (Long) -> Long = { packed ->
                        val interval = Interval(packed)
                        val newStart = max(0, interval.start + offset)
                        val newEnd = max(newStart, interval.end + offset)
                        Interval(newStart, newEnd, interval.flag1, interval.flag2).packed
                    }

                    tree.mapIntervals(start, end, mapper)
                    reference.mapIntervals(start, end, mapper)
                    verifyIntegrity(tree)
                }
            }
        }
        assertThat(tree.getAllStyles()).isEqualTo(reference.getAllStyles())
    }
}

/**
 * A simple implementation of the interval buffer that's inefficient but easy to ensure correctness.
 * Used as a reference for testing [IntIntervalTree].
 */
internal class ReferenceIntervalBuffer<T> {
    val intervals = mutableListOf<Triple<T, Int, Int>>()

    fun addInterval(item: T, start: Int, end: Int) {
        if (start >= end) return
        intervals.add(Triple(item, start, end))
    }

    fun removeInterval(item: T, start: Int, end: Int) {
        val iterator = intervals.iterator()
        while (iterator.hasNext()) {
            val (i, s, e) = iterator.next()
            if (i == item && s == start && e == end) {
                iterator.remove()
                return
            }
        }
    }

    fun getStyles(start: Int, end: Int): List<AnnotatedString.Range<T>> {
        return intervals
            .filter { (_, s, e) -> intersect(start, end, s, e) }
            .map { (i, s, e) -> AnnotatedString.Range(i, s, e) }
    }

    fun getAllStyles(): List<AnnotatedString.Range<T>> {
        return intervals.map { (i, s, e) -> AnnotatedString.Range(i, s, e) }
    }

    fun mapIntervals(start: Int, end: Int, block: (Long) -> Long) {
        val iterator = intervals.listIterator()
        while (iterator.hasNext()) {
            val (item, intervalStart, intervalEnd) = iterator.next()
            if (start <= intervalEnd && end >= intervalStart) {
                val packed = Interval(intervalStart, intervalEnd).packed
                val newPacked = block(packed)
                val newInterval = Interval(newPacked)
                if (newInterval.start >= newInterval.end) {
                    iterator.remove()
                } else {
                    iterator.set(Triple(item, newInterval.start, newInterval.end))
                }
            }
        }
    }
}

internal fun <T> IntIntervalTree<T>.addIntervalAndVerifyIntegrity(
    t: T,
    start: Int,
    end: Int,
): Boolean {
    val result = addInterval(t, Interval(start, end, false, true))
    verifyIntegrity(this)
    return result
}

internal fun <T> IntIntervalTree<T>.removeIntervalAndVerifyIntegrity(
    t: T,
    start: Int,
    end: Int,
): Boolean {
    val result = removeInterval(t, Interval(start, end, false, true))
    verifyIntegrity(this)
    return result
}

internal inline fun <reified T> IntIntervalTree<T>.getStyles(
    start: Int,
    end: Int,
): List<AnnotatedString.Range<T>> {
    return findIntervalsInRange<T, AnnotatedString.Range<T>>(start, end) { item, packed ->
        val interval = Interval(packed)
        AnnotatedString.Range(item, interval.start, interval.end)
    }
}

internal fun <T> IntIntervalTree<T>.getAllStyles(): List<AnnotatedString.Range<T>> {
    val result = mutableListOf<AnnotatedString.Range<T>>()
    forAllIntervals { item, packedInterval ->
        val interval = Interval(packedInterval)
        result.add(AnnotatedString.Range(item, interval.start, interval.end))
    }
    return result
}

internal fun verifyIntegrity(tree: IntIntervalTree<*>) {
    tree.verifyRedBlackProperties(tree.root)
}

internal fun IntIntervalTree<*>.verifyRedBlackProperties(node: Node): Int {
    if (node == terminator) {
        return 1 // Terminator is black
    }

    if (node.isDeleted) {
        throw AssertionError("Deleted node at ${node.index} shouldn't be reachable in traverse")
    }

    // 1. Red Rule: If a node is red, both its children must be black.
    if (node.color == TreeColorRed) {
        if (node.left.color != TreeColorBlack || node.right.color != TreeColorBlack) {
            throw AssertionError("Red violation at node ${node.start}: Red node has red children")
        }
    }

    // 2. Binary Search Tree Property: left.start <= node.start < right.start
    if (node.left != terminator && node.left.start > node.start) {
        throw AssertionError("BST violation: Left child ${node.left.start} > parent ${node.start}")
    }
    if (node.right != terminator && node.right.start < node.start) {
        throw AssertionError(
            "BST violation: Right child ${node.right.start} < parent ${node.start}"
        )
    }

    // 3. Interval Property: node.min and node.max must be correct
    val expectedMin = min(node.start, min(node.left.min, node.right.min))
    val expectedMax = max(node.end, max(node.left.max, node.right.max))
    if (node.min != expectedMin || node.max != expectedMax) {
        throw AssertionError(
            "Interval violation at ${node.start}: " +
                "Expected min/max [$expectedMin, $expectedMax], got [${node.min}, ${node.max}]"
        )
    }

    // 4. Black Depth Rule: Recursive check
    val leftHeight = verifyRedBlackProperties(node.left)
    val rightHeight = verifyRedBlackProperties(node.right)

    if (leftHeight != rightHeight) {
        throw AssertionError(
            "Black height violation at node ${node.start}: " +
                "Left height $leftHeight != Right height $rightHeight"
        )
    }

    // Return height: +1 if this node is black
    return if (node.color == TreeColorBlack) leftHeight + 1 else leftHeight
}
