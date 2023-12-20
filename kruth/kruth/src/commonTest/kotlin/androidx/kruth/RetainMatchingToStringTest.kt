/*
 * Copyright 2022 The Android Open Source Project
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
import kotlin.test.assertContentEquals

class RetainMatchingToStringTest {

    private val cases: List<Triple<List<Any?>, List<Any?>, List<Any?>>> =
        listOf(
            Triple(listOf(1L, 2L, 2L), listOf(2, 3), listOf(2L, 2L)),
            Triple(listOf(1L, 2L, 2L), listOf(2, 2, 3), listOf(2L, 2L)),
            Triple(listOf(1L, 2L), listOf(2, 3), listOf(2L)),
            Triple(listOf(1L, 2L), listOf(2, 2, 3), listOf(2L)),
            Triple(listOf(1L, 2L, 3L), listOf(1, 2, 3), listOf(1L, 2L, 3L)),
            Triple(listOf(1L, 2L, 3L), listOf(1, 2), listOf(1L, 2L)),
            Triple(listOf(1L, 2L, null), listOf(1, null), listOf(1L)),
            Triple(listOf(1L, 2L, null, null), listOf(1, null), listOf(1L)),
            Triple(listOf(1L, 2L, null, null), listOf(1, null, null), listOf(1L)),
            Triple(listOf(1L, 2L, null), listOf(1, null, null), listOf(1L)),
            Triple(listOf(1L, "null"), listOf(1, null), listOf(1L, "null")),
            Triple(listOf(1L, "null", "null"), listOf(1, null), listOf(1L, "null", "null")),
            Triple(listOf(1L, "null", "null"), listOf(1, null, null), listOf(1L, "null", "null")),
            Triple(listOf(1L, "null"), listOf(1, null, null), listOf(1L, "null")),
        )

    @Test
    fun retainMatchingToString() {
        cases.forEach { (items, itemsToCheck, expected) ->
            val actual = items.retainMatchingToString(itemsToCheck)
            assertContentEquals(
                expected = expected,
                actual = actual,
                message = "Items: $items. Items to check: $itemsToCheck. " +
                    "Expected: $expected. Actual: $actual",
            )
        }
    }
}
