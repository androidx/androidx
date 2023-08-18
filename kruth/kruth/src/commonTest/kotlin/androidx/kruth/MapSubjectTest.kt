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
    fun containsAtLeastWithNullKey() {
        val actual =
            mapOf(
                null to "value",
                "unexpectedKey" to "unexpectedValue",
            )

        val expected = mapOf<String?, String>(null to "value")

        assertThat(actual).containsAtLeast(null to "value")
        assertThat(actual).containsAtLeast(null to "value").inOrder()
        assertThat(actual).containsAtLeastEntriesIn(expected)
        assertThat(actual).containsAtLeastEntriesIn(expected).inOrder()
    }

    @Test
    fun containsAtLeastWithNullValue() {
        val actual =
            mapOf(
                "key" to null,
                "unexpectedKey" to "unexpectedValue",
            )

        val expected = mapOf<String, String?>("key" to null)

        assertThat(actual).containsAtLeast("key" to null)
        assertThat(actual).containsAtLeast("key" to null).inOrder()
        assertThat(actual).containsAtLeastEntriesIn(expected)
        assertThat(actual).containsAtLeastEntriesIn(expected).inOrder()
    }

    @Test
    fun containsAtLeastEmpty() {
        val actual = mapOf("key" to 1)
        assertThat(actual).containsAtLeastEntriesIn(emptyMap())
        assertThat(actual).containsAtLeastEntriesIn(emptyMap()).inOrder()
    }

    @Test
    fun containsAtLeastOneEntry() {
        val actual = mapOf("jan" to 1)
        assertThat(actual).containsAtLeast("jan" to 1)
        assertThat(actual).containsAtLeast("jan" to 1).inOrder()
        assertThat(actual).containsAtLeastEntriesIn(actual)
        assertThat(actual).containsAtLeastEntriesIn(actual).inOrder()
    }

    @Test
    fun containsAtLeastMultipleEntries() {
        val actual = mapOf("jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4)
        assertThat(actual).containsAtLeast("apr" to 4, "jan" to 1, "feb" to 2)
        assertThat(actual).containsAtLeast("jan" to 1, "feb" to 2, "apr" to 4).inOrder()
        assertThat(actual).containsAtLeastEntriesIn(mapOf("apr" to 4, "jan" to 1, "feb" to 2))
        assertThat(actual).containsAtLeastEntriesIn(actual).inOrder()
    }

    @Test
    fun containsAtLeastDuplicateKeys() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        try {
            assertThat(actual).containsAtLeast("jan" to 1, "jan" to 2, "jan" to 3)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected)
                .hasMessageThat()
                .isEqualTo("Duplicate keys ([jan x 3]) cannot be passed to containsAtLeast().")
        }
    }

    @Test
    fun containsAtLeastMultipleDuplicateKeys() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        try {
            assertThat(actual).containsAtLeast("jan" to 1, "jan" to 1, "feb" to 2, "feb" to 2)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected)
                .hasMessageThat()
                .isEqualTo(
                    "Duplicate keys ([jan x 2, feb x 2]) cannot be passed to containsAtLeast()."
                )
        }
    }

    @Test
    fun containsAtLeastMissingKey() {
        val actual = mapOf("jan" to 1, "feb" to 2)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsAtLeast("jan" to 1, "march" to 3)
        }
    }

    @Test
    fun containsAtLeastWrongValue() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsAtLeast("jan" to 1, "march" to 33)
        }
    }

    @Test
    fun containsAtLeastWrongValueWithNull() {
        // Test for https://github.com/google/truth/issues/468
        val actual = mapOf<String, Int?>("jan" to 1, "feb" to 2, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsAtLeast("jan" to 1, "march" to null)
        }
    }

    @Test
    fun containsAtLeastExtraKeyAndMissingKeyAndWrongValue() {
        val actual = mapOf("jan" to 1, "march" to 3)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsAtLeast("march" to 33, "feb" to 2)
        }
    }

    @Test
    fun containsAtLeastNotInOrder() {
        val actual = mapOf("jan" to 1, "feb" to 2, "march" to 3)
        assertThat(actual).containsAtLeast("march" to 3, "feb" to 2)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsAtLeast("march" to 3, "feb" to 2).inOrder()
        }
    }

    @Test
    fun containsAtLeastWrongValue_sameToStringForValues() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf<String, Any>("jan" to 1L, "feb" to 2L, "mar" to 3L))
                .containsAtLeast("jan" to 1, "feb" to 2)
        }
    }

    @Test
    fun containsAtLeastWrongValue_sameToStringForKeys() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf(1L to "jan", 1 to "feb")).containsAtLeast(1 to "jan", 1L to "feb")
        }
    }

    @Test
    fun containsAtLeastExtraKeyAndMissingKey_failsWithSameToStringForKeys() {
        assertFailsWith<AssertionError> {
            assertThat(mapOf(1L to "jan", 2 to "feb")).containsAtLeast(1 to "jan", 2 to "feb")
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
    fun doesNotContainKey() {
        val actual = mapOf("kurt" to "kluever")
        assertThat(actual).doesNotContainKey("greg")
        assertThat(actual).doesNotContainKey(null)
    }

    @Test
    fun doesNotContainKeyFailure() {
        val actual = mapOf("kurt" to "kluever")
        assertFailsWith<AssertionError> {
            assertThat(actual).doesNotContainKey("kurt")
        }
    }

    @Test
    fun doesNotContainNullKey() {
        val actual = mapOf<String?, String>(null to "null")
        assertFailsWith<AssertionError> {
            assertThat(actual).doesNotContainKey(null)
        }
    }

    @Test
    fun containsEntry() {
        val actual = mapOf("kurt" to "kluever")
        assertThat(actual).containsEntry("kurt" to "kluever")
    }

    @Test
    fun containsEntryFailure() {
        val actual = mapOf("kurt" to "kluever")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry("greg" to "kick")
        }
    }

    @Test
    fun containsEntry_failsWithSameToStringOfKey() {
        val actual = mapOf<Number, String>(1L to "value1", 2L to "value2")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry(1 to "value1")
        }
    }

    @Test
    fun containsEntry_failsWithSameToStringOfValue() {
        // Does not contain the correct key, but does contain a value which matches by toString.
        assertFailsWith<AssertionError> {
            assertThat(mapOf<Int, String?>(1 to "null")).containsEntry(2 to null)
        }
    }

    @Test
    fun containsNullKeyAndValue() {
        val actual = mapOf<String?, String?>("kurt" to "kluever")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry(null to null)
        }
    }

    @Test
    fun containsNullEntry() {
        val actual = mapOf<String?, String?>(null to null)
        assertThat(actual).containsEntry(null to null)
    }

    @Test
    fun containsNullEntryValue() {
        val actual = mapOf<String?, String?>(null to null)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry("kurt" to null)
        }
    }

    @Test
    fun containsNullEntryKey() {
        val actual = mapOf<String?, String?>(null to null)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry(null to "kluever")
        }
    }

    @Test
    fun containsExactly_bothExactAndToStringKeyMatches_showsExactKeyMatch() {
        val actual = mapOf<Number, String>(1 to "actual int", 1L to "actual long")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry(1L to "expected long")
        }
    }

    @Test
    fun doesNotContainEntry() {
        val actual = mapOf<String?, String?>("kurt" to "kluever")
        assertThat(actual).doesNotContainEntry("greg" to "kick")
        assertThat(actual).doesNotContainEntry(null to null)
        assertThat(actual).doesNotContainEntry("kurt" to null)
        assertThat(actual).doesNotContainEntry(null to "kluever")
    }

    @Test
    fun doesNotContainEntryFailure() {
        val actual = mapOf<String?, String?>("kurt" to "kluever")
        assertFailsWith<AssertionError> {
            assertThat(actual).doesNotContainEntry("kurt" to "kluever")
        }
    }

    @Test
    fun doesNotContainNullEntry() {
        val actual = mapOf<String?, String?>(null to null)
        assertThat(actual).doesNotContainEntry("kurt" to null)
        assertThat(actual).doesNotContainEntry(null to "kluever")
    }

    @Test
    fun doesNotContainNullEntryFailure() {
        val actual = mapOf<String?, String?>(null to null)
        assertFailsWith<AssertionError> {
            assertThat(actual).doesNotContainEntry(null to null)
        }
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
