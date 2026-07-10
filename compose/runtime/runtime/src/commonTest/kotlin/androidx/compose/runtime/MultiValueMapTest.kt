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

package androidx.compose.runtime

import androidx.compose.runtime.collection.MultiValueMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MultiValueMapTest {
    @Test
    fun create() {
        val map = MultiValueMap<Unit, Unit>()
        assertNotNull(map)
    }

    @Test
    fun canInsertMultipleValuesForTheSameKey() {
        val map = MultiValueMap<Int, String>()
        map.add(1, "A")
        map.add(1, "B")
        map.add(1, "C")
        var aCount = 0
        var bCount = 0
        var cCount = 0
        map.forEachValue(1) {
            when (it) {
                "A" -> aCount++
                "B" -> bCount++
                "C" -> cCount++
            }
        }
        assertEquals(1, aCount)
        assertEquals(1, bCount)
        assertEquals(1, cCount)
    }

    @Test
    fun canRemoveValuesOneByOne() {
        val map = MultiValueMap<Int, String>()
        map.add(1, "A")
        map.add(1, "B")
        map.add(1, "C")
        fun check(expectA: Int, expectB: Int, expectC: Int) {
            var aCount = 0
            var bCount = 0
            var cCount = 0
            map.forEachValue(1) {
                when (it) {
                    "A" -> aCount++
                    "B" -> bCount++
                    "C" -> cCount++
                }
            }
            assertEquals(expectA, aCount)
            assertEquals(expectB, bCount)
            assertEquals(expectC, cCount)
        }

        check(1, 1, 1)
        map.removeValueIf(1) { it == "B" }
        check(1, 0, 1)
        map.removeValueIf(1) { it == "C" }
        check(1, 0, 0)
        map.removeValueIf(1) { it == "A" }
        check(0, 0, 0)
    }
}
