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

package androidx.compose.ui.inspection.test.internal

import androidx.compose.ui.inspection.inspector.RawParameter
import androidx.compose.ui.inspection.recompositions.*
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val ANCHOR1 = "anchor1"
private const val ANCHOR2 = "anchor2"
private const val VALUE1 = "value1"
private const val VALUE2 = "value2"

class StateReadCacheTest {

    @Test
    fun testCache() {
        val counts = mutableMapOf<Any, RecompositionDataWithStateReads>()
        val cache = StateReadCache(counts)
        val data1 = RecompositionDataWithStateReads()
        val data2 = RecompositionDataWithStateReads()
        counts[ANCHOR1] = data1
        counts[ANCHOR2] = data2
        cache.maxStateReads = 5
        data1.incrementCount()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())
        cache.addStateRead(ANCHOR1, VALUE2, Exception())
        assertThat(cache.currentStateReads).isEqualTo(2)
        data1.incrementCount()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())
        cache.addStateRead(ANCHOR1, VALUE2, Exception())
        assertThat(cache.currentStateReads).isEqualTo(4)
        data2.incrementCount()
        cache.addStateRead(ANCHOR2, VALUE1, Exception())
        assertThat(cache.currentStateReads).isEqualTo(5)
        data2.incrementCount()

        // When adding 1 more state read, the state reads for ANCHOR1 and recomposition 1
        // will be discarded i.e. 2 state reads are discarded.
        cache.addStateRead(ANCHOR2, VALUE1, Exception())
        assertThat(cache.currentStateReads).isEqualTo(4)

        // Attempt to read state reads for the 2 first recompositions but only 1 is left:
        val reads =
            cache.getReadsAndRemove(
                ANCHOR1,
                recompositionNumberStart = 1,
                recompositionNumberEnd = 2,
                includeExtra = false,
            )
        assertThat(reads.size).isEqualTo(1)
        assertThat(reads.single().recomposition).isEqualTo(2)
        // This should remove 2 state reads from the cache:
        assertThat(cache.currentStateReads).isEqualTo(2)
    }

    @Test
    fun testParameterChangesCacheAccounting() {
        val counts = mutableMapOf<Any, RecompositionDataWithStateReads>()
        val cache = StateReadCache(counts)
        val data1 = RecompositionDataWithStateReads()
        val data2 = RecompositionDataWithStateReads()
        counts[ANCHOR1] = data1
        counts[ANCHOR2] = data2
        cache.maxStateReads = 3
        cache.maxRecompositionsWithParameterChanges = 3

        // Before incrementCount (count == 0) or when skipped, parameter changes are ignored:
        cache.addParameterChanges(ANCHOR1, listOf(RawParameter("p1", 0)))
        assertThat(cache.currentRecompositionsWithParameterChanges).isEqualTo(0)

        // Recomposition 1 on ANCHOR1: parameter changes only (multiple params count as 1 entry).
        data1.incrementCount()
        cache.addParameterChanges(ANCHOR1, listOf(RawParameter("p1", 1), RawParameter("p2", 2)))
        assertThat(cache.currentRecompositionsWithParameterChanges).isEqualTo(1)
        assertThat(cache.currentStateReads).isEqualTo(0)

        // Recomposition 2 on ANCHOR1: both state read and parameter change on the same entry.
        data1.incrementCount()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())
        cache.addParameterChanges(ANCHOR1, listOf(RawParameter("p1", 2)))
        // Updating parameter changes a second time on the same recomposition must not double-count.
        cache.addParameterChanges(ANCHOR1, listOf(RawParameter("p1", 22)))
        assertThat(cache.currentRecompositionsWithParameterChanges).isEqualTo(2)
        assertThat(cache.currentStateReads).isEqualTo(1)

        // Recomposition 1 & 2 on ANCHOR2: parameter changes only -> triggers eviction of ANCHOR1#1.
        data2.incrementCount()
        cache.addParameterChanges(ANCHOR2, listOf(RawParameter("q", 1)))
        assertThat(cache.currentRecompositionsWithParameterChanges).isEqualTo(3)
        data2.incrementCount()
        cache.addParameterChanges(ANCHOR2, listOf(RawParameter("q", 2)))
        assertThat(cache.currentRecompositionsWithParameterChanges).isEqualTo(3)

        // ANCHOR1#1 was evicted, so only ANCHOR1#2 is left:
        val reads1 =
            cache.getReadsAndRemove(
                ANCHOR1,
                recompositionNumberStart = 0,
                recompositionNumberEnd = 5,
                includeExtra = false,
            )
        assertThat(reads1.map { it.recomposition }).containsExactly(2)
        assertThat(reads1.single().parameterChanges.single().value).isEqualTo(22)
        assertThat(cache.currentRecompositionsWithParameterChanges).isEqualTo(2)
        assertThat(cache.currentStateReads).isEqualTo(0)

        // Removing ANCHOR2 via removeAllExcept updates currentRecompositionsWithParameterChanges to
        // 0.
        cache.removeAllExcept(setOf(ANCHOR1))
        assertThat(cache.currentRecompositionsWithParameterChanges).isEqualTo(0)
    }

    @Test
    fun testRecompositionWithOnlyParameterChanges() {
        val counts = mutableMapOf<Any, RecompositionDataWithStateReads>()
        val cache = StateReadCache(counts)
        val data1 = RecompositionDataWithStateReads()
        counts[ANCHOR1] = data1

        // Recomposition 1 has only parameter changes, and recomposition 2 has only a state read:
        data1.incrementCount()
        cache.addParameterChanges(ANCHOR1, listOf(RawParameter("p1", 1)))
        data1.incrementCount()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())

        // Both recompositions are returned:
        val reads = cache.getReadsAndRemove(ANCHOR1, 1, 5, includeExtra = false)
        assertThat(reads.map { it.recomposition }).containsExactly(1, 2).inOrder()
        assertThat(reads[0].parameterChanges.single().value).isEqualTo(1)
        assertThat(reads[0].reads).isEmpty()
        assertThat(reads[1].parameterChanges).isEmpty()
        assertThat(reads[1].reads).hasSize(1)
        assertThat(cache.currentRecompositionsWithParameterChanges).isEqualTo(0)
        assertThat(cache.currentStateReads).isEqualTo(0)
    }

    @Test
    fun testInvalidationsAreNotKeptWithoutStateReads() {
        val counts = mutableMapOf<Any, RecompositionDataWithStateReads>()
        val cache = StateReadCache(counts)
        val data1 = RecompositionDataWithStateReads()
        counts[ANCHOR1] = data1

        // An invalidation followed by a recomposition without state reads or parameter changes
        // must not leave a record behind:
        data1.incrementCount()
        cache.addInvalidation(ANCHOR1, VALUE1)
        cache.addInvalidation(ANCHOR1, null)
        data1.incrementCount()
        assertThat(
                cache.getReadsAndRemove(
                    ANCHOR1,
                    recompositionNumberStart = 1,
                    recompositionNumberEnd = 5,
                    includeExtra = true,
                )
            )
            .isEmpty()

        // A state read of an invalidated state variable is marked as invalidated:
        cache.addInvalidation(ANCHOR1, VALUE1)
        data1.incrementCount()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())
        cache.addStateRead(ANCHOR1, VALUE2, Exception())
        val reads =
            cache.getReadsAndRemove(
                ANCHOR1,
                recompositionNumberStart = 1,
                recompositionNumberEnd = 5,
                includeExtra = true,
            )
        assertThat(reads.single().recomposition).isEqualTo(3)
        assertThat(reads.single().reads.map { it.invalidated }).containsExactly(true, false)
        assertThat(cache.currentStateReads).isEqualTo(0)
    }

    @Test
    fun testRecompositionsWithoutStateReads() {
        val counts = mutableMapOf<Any, RecompositionDataWithStateReads>()
        val cache = StateReadCache(counts)
        val data1 = RecompositionDataWithStateReads()
        counts[ANCHOR1] = data1

        // Observed recompositions 1 to 5 with state reads in recompositions 2 and 4:
        for (recomposition in 1..5) {
            data1.incrementCount()
            data1.markObserved()
            if (recomposition % 2 == 0) {
                cache.addStateRead(ANCHOR1, VALUE1, Exception())
            }
        }
        val reads1 = cache.getReadsAndRemove(ANCHOR1, 1, 10, includeExtra = false)
        assertThat(reads1.map { it.recomposition }).containsExactly(1, 2, 3, 4, 5).inOrder()
        assertThat(reads1.map { it.reads.size }).containsExactly(0, 1, 0, 1, 0).inOrder()

        // The same recompositions are not returned again:
        assertThat(cache.getReadsAndRemove(ANCHOR1, 1, 10, includeExtra = false)).isEmpty()

        // More state reads in the current recomposition are still returned:
        cache.addStateRead(ANCHOR1, VALUE2, Exception())
        val reads2 = cache.getReadsAndRemove(ANCHOR1, 1, 10, includeExtra = false)
        assertThat(reads2.map { it.recomposition }).containsExactly(5)
        assertThat(reads2.single().reads).hasSize(1)

        // Evicted recompositions are not returned as recompositions without state reads:
        cache.maxStateReads = 1
        data1.incrementCount()
        data1.markObserved()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())
        data1.incrementCount()
        data1.markObserved()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())
        assertThat(cache.purgedStateReads).isEqualTo(1)
        val reads3 = cache.getReadsAndRemove(ANCHOR1, 6, 7, includeExtra = false)
        assertThat(reads3.map { it.recomposition }).containsExactly(7)

        // Recompositions are not returned after observation stopped:
        cache.clear()
        data1.incrementCount()
        assertThat(cache.getReadsAndRemove(ANCHOR1, 1, 10, includeExtra = false)).isEmpty()
    }

    @Test
    fun testOverlappingRangesDoNotReturnReturnedRecompositionsAsEmpty() {
        val counts = mutableMapOf<Any, RecompositionDataWithStateReads>()
        val cache = StateReadCache(counts)
        val data1 = RecompositionDataWithStateReads()
        counts[ANCHOR1] = data1

        // 20 observed recompositions with state reads:
        repeat(20) {
            data1.incrementCount()
            data1.markObserved()
            cache.addStateRead(ANCHOR1, VALUE1, Exception())
        }
        assertThat(cache.currentStateReads).isEqualTo(20)

        // Studio fetches a window around recomposition 20 (16..24):
        val reads1 = cache.getReadsAndRemove(ANCHOR1, 16, 24, includeExtra = true)
        assertThat(reads1.map { it.recomposition }).containsExactly(16, 17, 18, 19, 20).inOrder()
        assertThat(reads1.all { it.reads.size == 1 }).isTrue()
        assertThat(cache.currentStateReads).isEqualTo(15)

        // 2 more recompositions happen (21 with a state read, 22 without):
        data1.incrementCount()
        data1.markObserved()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())
        data1.incrementCount()
        data1.markObserved()

        // Studio fetches a window around recomposition 22 (18..26); 18..20 were already returned
        // and must not be returned again as empty recompositions:
        val reads2 = cache.getReadsAndRemove(ANCHOR1, 18, 26, includeExtra = true)
        assertThat(reads2.map { it.recomposition }).containsExactly(21, 22).inOrder()
        assertThat(reads2.map { it.reads.size }).containsExactly(1, 0).inOrder()

        // When the cache evicts all remaining state reads below 16..22 (1..15), the RETURNED
        // markers at 16..22 are also skipped:
        cache.maxStateReads = 1
        data1.incrementCount()
        data1.markObserved()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())
        data1.incrementCount()
        data1.markObserved()
        cache.addStateRead(ANCHOR1, VALUE1, Exception())
        assertThat(cache.currentStateReads).isEqualTo(1)

        val reads3 = cache.getReadsAndRemove(ANCHOR1, 1, 30, includeExtra = true)
        assertThat(reads3.map { it.recomposition }).containsExactly(24)
        assertThat(reads3.single().reads).hasSize(1)
    }

    @Test
    fun testGetReadsAndRemoveRange() {
        val counts = mutableMapOf<Any, RecompositionDataWithStateReads>()
        val cache = StateReadCache(counts)
        val data1 = RecompositionDataWithStateReads()
        counts[ANCHOR1] = data1

        // State reads in recompositions 1, 3, 4, and 5, but not in recomposition 2:
        for (recomposition in 1..5) {
            data1.incrementCount()
            if (recomposition != 2) {
                cache.addStateRead(ANCHOR1, VALUE1, Exception())
            }
        }
        assertThat(cache.currentStateReads).isEqualTo(4)

        // A range starting at a recomposition without state reads:
        val reads1 = cache.getReadsAndRemove(ANCHOR1, 2, 3, includeExtra = false)
        assertThat(reads1.map { it.recomposition }).containsExactly(3)
        assertThat(cache.currentStateReads).isEqualTo(3)

        // Include extra state reads after the range, if there are no state reads in the range:
        val reads2 = cache.getReadsAndRemove(ANCHOR1, 2, 2, includeExtra = true)
        assertThat(reads2.map { it.recomposition }).containsExactly(4)
        assertThat(cache.currentStateReads).isEqualTo(2)

        // The range is limited by the current recomposition:
        val reads3 = cache.getReadsAndRemove(ANCHOR1, 1, 10, includeExtra = false)
        assertThat(reads3.map { it.recomposition }).containsExactly(1, 5).inOrder()
        assertThat(cache.currentStateReads).isEqualTo(0)

        // More state reads for the current recomposition can still be recorded:
        cache.addStateRead(ANCHOR1, VALUE2, Exception())
        val reads4 = cache.getReadsAndRemove(ANCHOR1, 5, 5, includeExtra = false)
        assertThat(reads4.single().recomposition).isEqualTo(5)
        assertThat(reads4.single().reads).hasSize(1)
        assertThat(reads3.last().reads).hasSize(1)
        assertThat(cache.currentStateReads).isEqualTo(0)
    }
}
