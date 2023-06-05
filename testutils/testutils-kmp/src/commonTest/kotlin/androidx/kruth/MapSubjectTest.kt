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

class MapSubjectTest {

    @Test
    fun containsExactlyWithNullKey() {
        val actual = mapOf<String?, String>(null to "value")
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyWithNullValue() {
        val actual = mapOf<String, String?>("key" to null)
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyEmpty() {
        val actual = mapOf<String, Int>()
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyEntriesInEmpty_fails() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf("jan" to 1)).containsExactlyEntriesIn(emptyMap())
        }
    }

    @Test
    fun containsExactlyOneEntry() {
        val actual = mapOf("jan" to 1)
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyMultipleEntries() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyNotInOrder() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyBadNumberOfArgs() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3, "april" to 4, "may" to 5)
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyInOrderWithReversedMap_fails() {
        assertFailsWith<AssertionError> {
            assertThat(mutableMapOf("jan" to 1, "feb" to 2, "march" to 3))
                .containsExactlyEntriesIn(mutableMapOf("march" to 3, "feb" to 2, "jan" to 1))
                .inOrder()
        }
    }

    @Test
    fun isEmpty() {
        assertThat(mapOf<Any, Any>()).isEmpty()
    }

    @Test
    fun isEmptyWithFailure() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf(1 to 5)).isEmpty()
        }
    }

    @Test
    fun isNotEmpty() {
        assertThat(mapOf(1 to 5)).isNotEmpty()
    }

    @Test
    fun isNotEmptyWithFailure() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf<Any, Any>()).isNotEmpty()
        }
    }

    @Test
    fun hasSize() {
        assertThat(mapOf(1 to 2, 3 to 4)).hasSize(2)
    }

    @Test
    fun hasSizeZero() {
        assertThat(mapOf<Any, Any>()).hasSize(0)
    }

    @Test
    fun hasSizeNegative() {
        assertFailsWith<IllegalArgumentException> {
            assertThat(mapOf(1 to 2)).hasSize(-1)
        }
    }

    @Test
    fun containsKey() {
        assertThat(mapOf("kurt" to "kluever")).containsKey("kurt")
    }

    @Test
    fun containsKeyFailure() {
        val actual = mapOf("kurt" to "kluever")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsKey("greg")
        }
    }

    @Test
    fun containsKeyNullFailure() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf("kurt" to "kluever")).containsKey(null)
        }
    }

    @Test
    fun containsKey_failsWithSameToString() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf(1L to "value1", 2L to "value2", "1" to "value3")).containsKey(1)
        }
    }

    @Test
    fun containsKey_failsWithNullStringAndNull() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf("null" to "value1")).containsKey(null)
        }
    }

    @Test
    fun containsNullKey() {
        assertThat(mapOf(null to "null")).containsKey(null)
    }

    @Test
    fun failMapContainsKey() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf("a" to "A")).containsKey("b")
        }
    }

    @Test
    fun failMapContainsKeyWithNull() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf("a" to "A")).containsKey(null)
        }
    }
}
