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
