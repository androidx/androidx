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

package androidx.compose.ui.inspection.recompositions

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
}
