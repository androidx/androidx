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

package androidx.compose.remote.creation.compose.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ListUtilsTest {
    @Test
    fun fastForEach_iteratesInOrder() {
        val result = mutableListOf<Int>()
        listOf(1, 2, 3).fastForEach { result.add(it) }
        assertThat(result).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun fastForEachReversed_iteratesInReverseOrder() {
        val result = mutableListOf<Int>()
        listOf(1, 2, 3).fastForEachReversed { result.add(it) }
        assertThat(result).containsExactly(3, 2, 1).inOrder()
    }

    @Test
    fun fastMap_transformsElements() {
        assertThat(listOf(1, 2, 3).fastMap { it * 2 }).containsExactly(2, 4, 6).inOrder()
    }

    @Test
    fun fastFilter_retainsMatchingElements() {
        assertThat(listOf(1, 2, 3, 4).fastFilter { it % 2 == 0 }).containsExactly(2, 4).inOrder()
    }

    @Test
    fun fastFold_accumulatesLeftToRight() {
        assertThat(listOf("a", "b", "c").fastFold("") { acc, s -> acc + s }).isEqualTo("abc")
    }

    @Test
    fun fastAll_checksAllElements() {
        assertThat(listOf(2, 4, 6).fastAll { it % 2 == 0 }).isTrue()
        assertThat(listOf(2, 3, 6).fastAll { it % 2 == 0 }).isFalse()
    }

    @Test
    fun fastFirstOrNull_returnsFirstMatchOrNull() {
        assertThat(listOf(1, 3, 4, 6).fastFirstOrNull { it % 2 == 0 }).isEqualTo(4)
        assertThat(listOf(1, 3, 5).fastFirstOrNull { it % 2 == 0 }).isNull()
    }
}
