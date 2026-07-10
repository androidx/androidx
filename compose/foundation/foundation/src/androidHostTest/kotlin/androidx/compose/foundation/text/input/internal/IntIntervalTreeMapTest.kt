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
import kotlin.random.Random
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntIntervalTreeMapTest {

    @Test
    fun mapIntervals_basic() {
        val tree = IntIntervalTree<String>()
        tree.addIntervalAndVerifyIntegrity("a", 0, 5)
        tree.addIntervalAndVerifyIntegrity("b", 10, 15)
        tree.addIntervalAndVerifyIntegrity("c", 20, 30)
        tree.addIntervalAndVerifyIntegrity("d", 30, 35)

        // Mapping only elements overlapping with [10, 25] (which covers "b" and "c")
        tree.mapIntervals(10, 25) {
            val interval = Interval(it)
            val newStart = if (interval.start in 10 until 25) interval.start + 5 else interval.start
            val newEnd = if (interval.end in 10 until 25) interval.end + 5 else interval.end
            Interval(newStart, newEnd, interval.flag1, interval.flag2).packed
        }

        assertThat(tree.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("a", 0, 5),
                AnnotatedString.Range("b", 15, 20),
                AnnotatedString.Range("c", 25, 30),
                AnnotatedString.Range("d", 30, 35),
            )
            .inOrder()
        // We didn't change the order of the intervals, the tree properties should remain.
        verifyIntegrity(tree)
    }

    @Test
    fun mapIntervals_removeCollapsed() {
        val tree = IntIntervalTree<String>()
        tree.addIntervalAndVerifyIntegrity("a", 0, 10)
        tree.addIntervalAndVerifyIntegrity("b", 10, 20)
        tree.addIntervalAndVerifyIntegrity("c", 20, 30)

        // Map "b" so that it becomes collapsed (e.g. start=10, end=20 -> start=10, end=10)
        tree.mapIntervals(10, 20) {
            val interval = Interval(it)
            val newEnd = if (interval.end == 20) 10 else interval.end
            Interval(interval.start, newEnd, interval.flag1, interval.flag2).packed
        }

        assertThat(tree.getAllStyles())
            .containsExactly(AnnotatedString.Range("a", 0, 10), AnnotatedString.Range("c", 20, 30))
            .inOrder()
        verifyIntegrity(tree)
    }

    @Test
    fun mapIntervals_reorderNodes() {
        val tree = IntIntervalTree<String>()
        tree.addIntervalAndVerifyIntegrity("a", 0, 10)
        tree.addIntervalAndVerifyIntegrity("b", 10, 20)
        tree.addIntervalAndVerifyIntegrity("c", 20, 30)
        tree.addIntervalAndVerifyIntegrity("d", 30, 40)

        // Map "c" so its start becomes 5, making it out of order compared to "b"
        tree.mapIntervals(10, 40) {
            val interval = Interval(it)
            val newStart = if (interval.start == 20) 5 else interval.start
            val newEnd = if (interval.end == 30) 15 else interval.end
            Interval(newStart, newEnd, interval.flag1, interval.flag2).packed
        }

        assertThat(tree.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("a", 0, 10),
                AnnotatedString.Range("b", 10, 20),
                AnnotatedString.Range("c", 5, 15),
                AnnotatedString.Range("d", 30, 40),
            )
            .inOrder()
        verifyIntegrity(tree)
    }

    @Test
    fun mapIntervals_reverseNodeOrder() {
        val tree = IntIntervalTree<String>()
        tree.addIntervalAndVerifyIntegrity("a", 0, 5)
        tree.addIntervalAndVerifyIntegrity("b", 10, 15)
        tree.addIntervalAndVerifyIntegrity("c", 20, 25)
        tree.addIntervalAndVerifyIntegrity("d", 30, 35)
        tree.addIntervalAndVerifyIntegrity("e", 40, 45)

        tree.mapIntervals(0, 40) {
            val interval = Interval(it)
            val newStart = 40 - interval.start
            val newEnd = newStart + 5
            Interval(newStart, newEnd, interval.flag1, interval.flag2).packed
        }

        assertThat(tree.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("a", 40, 45),
                AnnotatedString.Range("b", 30, 35),
                AnnotatedString.Range("c", 20, 25),
                AnnotatedString.Range("d", 10, 15),
                AnnotatedString.Range("e", 0, 5),
            )
            .inOrder()

        verifyIntegrity(tree)
    }

    @Test
    fun mapIntervals_multipleNodesReorderAndCollapse() {
        val tree = IntIntervalTree<String>()
        tree.addIntervalAndVerifyIntegrity("a", 10, 20)
        tree.addIntervalAndVerifyIntegrity("b", 20, 30)
        tree.addIntervalAndVerifyIntegrity("c", 30, 40)
        tree.addIntervalAndVerifyIntegrity("d", 40, 50)
        tree.addIntervalAndVerifyIntegrity("e", 50, 60)

        tree.mapIntervals(20, 50) {
            val interval = Interval(it)
            when (interval.start) {
                20 -> Interval(20, 20, interval.flag1, interval.flag2).packed // Collapse "b"
                30 -> Interval(60, 70, interval.flag1, interval.flag2).packed // Move "c" after "e"
                40 -> Interval(0, 5, interval.flag1, interval.flag2).packed // Move "d" before "a"
                else -> it
            }
        }

        assertThat(tree.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("a", 10, 20),
                AnnotatedString.Range("c", 60, 70),
                AnnotatedString.Range("d", 0, 5),
                AnnotatedString.Range("e", 50, 60),
            )
            .inOrder()
        verifyIntegrity(tree)
    }

    @Test
    fun mapIntervals_protectsUnmappedNodesOutsideBounds() {
        val tree = IntIntervalTree<String>()
        tree.addIntervalAndVerifyIntegrity("unmapped_left", 0, 5)
        tree.addIntervalAndVerifyIntegrity("mapped_jump_left", 10, 20)
        tree.addIntervalAndVerifyIntegrity("unmapped_middle", 25, 30)
        tree.addIntervalAndVerifyIntegrity("mapped_jump_right", 35, 45)
        tree.addIntervalAndVerifyIntegrity("unmapped_right", 50, 55)

        tree.mapIntervals(10, 20) {
            val interval = Interval(it)
            Interval(2, 8, interval.flag1, interval.flag2).packed
        }

        tree.mapIntervals(35, 45) {
            val interval = Interval(it)
            Interval(60, 70, interval.flag1, interval.flag2).packed
        }

        assertThat(tree.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("unmapped_left", 0, 5),
                AnnotatedString.Range("mapped_jump_left", 2, 8),
                AnnotatedString.Range("unmapped_middle", 25, 30),
                AnnotatedString.Range("mapped_jump_right", 60, 70),
                AnnotatedString.Range("unmapped_right", 50, 55),
            )
            .inOrder()
        verifyIntegrity(tree)
    }

    @Test
    fun mapIntervals_boundaryNode_movesToExtremeLeft() {
        val tree = IntIntervalTree<String>()
        tree.addIntervalAndVerifyIntegrity("a", 10, 15)
        // Overlaps mapping range (20-30) from the left
        tree.addIntervalAndVerifyIntegrity("b", 15, 25)
        tree.addIntervalAndVerifyIntegrity("c", 22, 28) // Fully inside
        tree.addIntervalAndVerifyIntegrity("d", 25, 35) // Overlaps mapping range from the right
        tree.addIntervalAndVerifyIntegrity("e", 40, 45)

        // Move `b` to the left of `a` (newStart = 0).
        // This tests that a boundary node jumping drastically to the left
        // correctly detaches and reattaches without breaking the tree.
        tree.mapIntervals(20, 30) {
            val interval = Interval(it)
            val newStart = if (interval.start == 15) 0 else interval.start
            val newEnd = if (interval.start == 15) 5 else interval.end
            Interval(newStart, newEnd, interval.flag1, interval.flag2).packed
        }

        assertThat(tree.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("a", 10, 15),
                // 'b' bounds changed, but insertion order remains!
                AnnotatedString.Range("b", 0, 5),
                AnnotatedString.Range("c", 22, 28),
                AnnotatedString.Range("d", 25, 35),
                AnnotatedString.Range("e", 40, 45),
            )
            .inOrder()
        verifyIntegrity(tree)
    }

    @Test
    fun mapIntervals_boundaryNode_movesToExtremeRight() {
        val tree = IntIntervalTree<String>()
        tree.addIntervalAndVerifyIntegrity("a", 10, 15)
        // Overlaps mapping range (20-30) from the left
        tree.addIntervalAndVerifyIntegrity("b", 15, 25)
        tree.addIntervalAndVerifyIntegrity("c", 22, 28) // Fully inside
        tree.addIntervalAndVerifyIntegrity("d", 25, 35) // Overlaps mapping range from the right
        tree.addIntervalAndVerifyIntegrity("e", 40, 45)

        // Move `b` to the right of `e` (newStart = 50).
        // This tests that a boundary node jumping drastically to the right
        // correctly detaches and reattaches without breaking the tree.
        tree.mapIntervals(20, 30) {
            val interval = Interval(it)
            val newStart = if (interval.start == 15) 50 else interval.start
            val newEnd = if (interval.start == 15) 60 else interval.end
            Interval(newStart, newEnd, interval.flag1, interval.flag2).packed
        }

        assertThat(tree.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("a", 10, 15),
                // 'b' bounds changed, but insertion order remains
                AnnotatedString.Range("b", 50, 60),
                AnnotatedString.Range("c", 22, 28),
                AnnotatedString.Range("d", 25, 35),
                AnnotatedString.Range("e", 40, 45),
            )
            .inOrder()
        verifyIntegrity(tree)
    }

    @Test
    fun mapIntervals_updatesFlags() {
        val tree = IntIntervalTree<String>()
        tree.addInterval("a", Interval(0, 10, flag1 = false, flag2 = false))
        tree.addInterval("b", Interval(10, 20, flag1 = true, flag2 = false))
        tree.addInterval("c", Interval(20, 30, flag1 = false, flag2 = true))
        tree.addInterval("d", Interval(30, 40, flag1 = true, flag2 = true))

        verifyIntegrity(tree)

        tree.mapIntervals(0, 40) {
            val interval = Interval(it)
            // Flip both flags
            Interval(interval.start, interval.end, !interval.flag1, !interval.flag2).packed
        }

        val result = mutableListOf<Pair<String, Interval>>()
        tree.forAllIntervals { item, packed -> result.add(Pair(item, Interval(packed))) }

        assertThat(result)
            .containsExactly(
                Pair("a", Interval(0, 10, flag1 = true, flag2 = true)),
                Pair("b", Interval(10, 20, flag1 = false, flag2 = true)),
                Pair("c", Interval(20, 30, flag1 = true, flag2 = false)),
                Pair("d", Interval(30, 40, flag1 = false, flag2 = false)),
            )
            .inOrder()
        verifyIntegrity(tree)
    }

    @Test
    fun mapIntervals_inPlaceUpdate() {
        val tree = IntIntervalTree<String>()
        tree.addIntervalAndVerifyIntegrity("a", 10, 20)
        tree.addIntervalAndVerifyIntegrity("b", 20, 30)
        tree.addIntervalAndVerifyIntegrity("c", 30, 40)

        // Map range 0..50 fully covers all nodes.
        // MaxStart initializes to 0.
        // We shift each interval to the right slightly but strictly increasing,
        // which never violates newStart >= maxStart or crosses bounds.
        tree.mapIntervals(0, 50) {
            val interval = Interval(it)
            Interval(interval.start + 1, interval.end + 1, interval.flag1, interval.flag2).packed
        }

        // Verification: Bounds actually changed
        assertThat(tree.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("a", 11, 21),
                AnnotatedString.Range("b", 21, 31),
                AnnotatedString.Range("c", 31, 41),
            )
            .inOrder()

        verifyIntegrity(tree)
    }

    @Test
    fun stressTest_mapIntervals() {
        val random = Random(42)

        val tree = IntIntervalTree<String>()
        val reference = ReferenceIntervalBuffer<String>()

        // 1. Initial Insertions
        repeat(1000) {
            val start = random.nextInt(0, 1000)
            val end = start + random.nextInt(1, 100)
            val item = "item_$it"
            tree.addIntervalAndVerifyIntegrity(item, start, end)
            reference.addInterval(item, start, end)
        }

        // 2. Perform a series of randomized mapIntervals operations
        repeat(5000) {
            val mapStart = random.nextInt(0, 1000)
            val mapEnd = mapStart + random.nextInt(10, 200)

            // Pre-generate a random mapping for all intervals overlapping the map region.
            // This ensures that the randomized behavior remains perfectly consistent between
            // the two trees, regardless of the internal iteration order.
            val mappingDict = mutableMapOf<Long, Long>()
            for ((_, intervalStart, intervalEnd) in reference.intervals) {
                if (mapStart <= intervalEnd && mapEnd >= intervalStart) {
                    val key = Interval(intervalStart, intervalEnd).packed
                    if (key !in mappingDict) {
                        val op = random.nextInt(4)
                        var newStart = intervalStart
                        var newEnd = intervalEnd

                        when (op) {
                            0 -> { // Shift left
                                val shift = random.nextInt(1, 50)
                                newStart -= shift
                                newEnd -= shift
                            }
                            1 -> { // Shift right
                                val shift = random.nextInt(1, 50)
                                newStart += shift
                                newEnd += shift
                            }
                            2 -> { // Expand
                                newStart -= random.nextInt(1, 20)
                                newEnd += random.nextInt(1, 20)
                            }
                            3 -> { // Collapse
                                newEnd = newStart
                            }
                        }
                        mappingDict[key] = Interval(newStart, newEnd).packed
                    }
                }
            }

            val mapper: (Long) -> Long = { packed ->
                val interval = Interval(packed)
                val key = Interval(interval.start, interval.end).packed
                mappingDict[key] ?: packed
            }

            tree.mapIntervals(mapStart, mapEnd, mapper)
            reference.mapIntervals(mapStart, mapEnd, mapper)

            verifyIntegrity(tree)

            val expected = reference.getAllStyles()
            val actual = tree.getAllStyles()
            assertThat(actual).containsExactlyElementsIn(expected).inOrder()
        }
    }
}
