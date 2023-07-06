/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.kruth

import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Supplemental tests to [MapSubjectTest].
 */
class MapSubjectKruthTest {
    @Test
    fun containsExactlyEntriesIn_mapInOrder() {
        // LinkedHashMap preserves ordering of entries, keys and values.
        val map = LinkedHashMap<Int, Int>()
        repeat(5) { map[it] = it }
        val correctMap = LinkedHashMap<Int, Int>()
        repeat(5) { correctMap[it] = it }
        val reversedMap = LinkedHashMap<Int, Int>()
        repeat(5) { reversedMap[4 - it] = 4 - it }

        val mapInOrderCorrect = assertThat(map).containsExactlyEntriesIn(correctMap)
        mapInOrderCorrect.inOrder()

        val mapInOrderReversed = assertThat(map).containsExactlyEntriesIn(reversedMap)
        assertFailsWith<AssertionError> {
            mapInOrderReversed.inOrder()
        }
    }
}
