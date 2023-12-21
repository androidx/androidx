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
import kotlin.test.fail

class MapSubjectTest {

    @Test
    fun containsExactlyWithNullKey() {
        val actual = mapOf<String?, String?>(null to "value")
        assertThat(actual).containsExactly(null to "value")
        assertThat(actual).containsExactly(null to "value").inOrder()
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyWithNullValue() {
        val actual = mapOf<String?, String?>("key" to null)
        assertThat(actual).containsExactly("key" to null)
        assertThat(actual).containsExactly("key" to null).inOrder()
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyEmpty() {
        val actual = emptyMap<String, Int>()
        assertThat(actual).containsExactly()
        assertThat(actual).containsExactly().inOrder()
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyEmpty_fails() {
        val actual = mapOf("jan" to 1)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly()
        }
    }

    @Test
    fun containsExactlyEntriesInEmpty_fails() {
        val actual = mapOf("jan" to 1)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactlyEntriesIn(emptyMap())
        }
    }

    @Test
    fun containsExactlyOneEntry() {
        val actual = mapOf("jan" to 1)
        assertThat(actual).containsExactly("jan" to 1)
        assertThat(actual).containsExactly("jan" to 1).inOrder()
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyMultipleEntries() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertThat(actual).containsExactly("march" to 3, "jan" to 1, "feb" to 2)
        assertThat(actual).containsExactly("jan" to 1, "feb" to 2, "march" to 3).inOrder()
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
    }

    @Test
    fun containsExactlyDuplicateKeys() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        try {
            assertThat(actual).containsExactly("jan" to 1, "jan" to 2, "jan" to 3)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected)
                .hasMessageThat()
                .isEqualTo("Duplicate keys ([jan x 3]) cannot be passed to containsExactly().")
        }
    }

    @Test
    fun containsExactlyMultipleDuplicateKeys() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        try {
            assertThat(actual).containsExactly("jan" to 1, "jan" to 1, "feb" to 2, "feb" to 2)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected)
                .hasMessageThat()
                .isEqualTo(
                    "Duplicate keys ([jan x 2, feb x 2]) cannot be passed to containsExactly()."
                )
        }
    }

    @Test
    fun containsExactlyExtraKey() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("feb" to 2, "jan" to 1)
        }
    }

    @Test
    fun containsExactlyExtraKeyInOrder() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("feb" to 2, "jan" to 1).inOrder()
        }
    }

    @Test
    fun containsExactlyMissingKey() {
        val actual = mapOf("jan" to 1, "feb" to 2)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("jan" to 1, "march" to 3, "feb" to 2)
        }
    }

    @Test
    fun containsExactlyWrongValue() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("jan" to 1, "march" to 33, "feb" to 2)
        }
    }

    @Test
    fun containsExactlyWrongValueWithNull() {
        // Test for https://github.com/google/truth/issues/468
        val actual = mapOf<String, Int?>("jan" to 1, "feb" to 2, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("jan" to 1, "march" to null, "feb" to 2)
        }
    }

    @Test
    fun containsExactlyExtraKeyAndMissingKey() {
        val actual = mapOf("jan" to 1, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("jan" to 1, "feb" to 2)
        }
    }

    @Test
    fun containsExactlyExtraKeyAndWrongValue() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("jan" to 1, "march" to 33)
        }
    }

    @Test
    fun containsExactlyMissingKeyAndWrongValue() {
        val actual = mapOf("jan" to 1, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("jan" to 1, "march" to 33, "feb" to 2)
        }
    }

    @Test
    fun containsExactlyExtraKeyAndMissingKeyAndWrongValue() {
        val actual = mapOf("jan" to 1, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("march" to 33, "feb" to 2)
        }
    }

    @Test
    fun containsExactlyNotInOrder() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertThat(actual).containsExactlyEntriesIn(actual)
        assertThat(actual).containsExactlyEntriesIn(actual).inOrder()
        assertThat(actual).containsExactly("jan" to 1, "march" to 3, "feb" to 2)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly("jan" to 1, "march" to 3, "feb" to 2).inOrder()
        }
    }

    @Test
    fun containsExactlyWrongValue_sameToStringForValues() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf<String, Any>("jan" to 1L, "feb" to 2L))
                .containsExactly("jan" to 1, "feb" to 2)
        }
    }

    @Test
    fun containsExactlyWrongValue_sameToStringForKeys() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf(1L to "jan", 1 to "feb"))
                .containsExactly(1 to "jan", 1L to "feb")
        }
    }

    @Test
    fun containsExactlyExtraKeyAndMissingKey_failsWithSameToStringForKeys() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf(1L to "jan", 2 to "feb"))
                .containsExactly(1 to "jan", 2 to "feb")
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
