/*
 * Copyright 2024 The Android Open Source Project
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

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.LinkedListMultimap
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MultimapSubjectTest {

    @Test
    fun listMultimapIsEqualTo_passes() {
        val multimapA =
            ImmutableListMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "russell", "cobain")
                .build()
        val multimapB =
            ImmutableListMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "russell", "cobain")
                .build()
        assertThat(multimapA == multimapB).isTrue()
        assertThat(multimapA).isEqualTo(multimapB)
    }

    @Test
    fun listMultimapIsEqualTo_fails() {
        val multimapA =
            ImmutableListMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "russell", "cobain")
                .build()
        val multimapB =
            ImmutableListMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "cobain", "russell")
                .build()

        assertFailsWith<AssertionError> {
            assertThat(multimapA).isEqualTo(multimapB)
        }
    }

    @Test
    fun setMultimapIsEqualTo_passes() {
        val multimapA =
            ImmutableSetMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "russell", "cobain")
                .build()
        val multimapB =
            ImmutableSetMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "cobain", "russell")
                .build()
        assertThat(multimapA == multimapB).isTrue()
        assertThat(multimapA).isEqualTo(multimapB)
    }

    @Test
    fun setMultimapIsEqualTo_fails() {
        val multimapA =
            ImmutableSetMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "russell", "cobain")
                .build()
        val multimapB =
            ImmutableSetMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "russell")
                .build()
        assertFailsWith<AssertionError> {
            assertThat(multimapA).isEqualTo(multimapB)
        }
    }

    @Test
    fun setMultimapIsEqualToListMultimap_fails() {
        val multimapA =
            ImmutableSetMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "russell", "cobain")
                .build()
        val multimapB =
            ImmutableListMultimap.builder<String, String>()
                .putAll("kurt", "kluever", "russell", "cobain")
                .build()
        assertFailsWith<AssertionError> {
            assertThat(multimapA).isEqualTo(multimapB)
        }
    }

    @Test
    fun isEqualTo_failsWithSameToString() {
        assertFailsWith<AssertionError> {
            assertThat(ImmutableMultimap.of(1, "a", 1, "b", 2, "c"))
                .isEqualTo(ImmutableMultimap.of(1L, "a", 1L, "b", 2L, "c"))
        }
    }

    @Test
    fun multimapIsEmpty() {
        val multimap = ImmutableMultimap.of<String, String>()
        assertThat(multimap).isEmpty()
    }

    @Test
    fun multimapIsEmptyWithFailure() {
        val multimap = ImmutableMultimap.of(1, 5)
        assertFailsWith<AssertionError> {
            assertThat(multimap).isEmpty()
        }
    }

    @Test
    fun multimapIsNotEmpty() {
        val multimap = ImmutableMultimap.of(1, 5)
        assertThat(multimap).isNotEmpty()
    }

    @Test
    fun multimapIsNotEmptyWithFailure() {
        val multimap = ImmutableMultimap.of<Int, Int>()
        assertFailsWith<AssertionError> {
            assertThat(multimap).isNotEmpty()
        }
    }

    @Test
    fun hasSize() {
        assertThat(ImmutableMultimap.of(1, 2, 3, 4)).hasSize(2)
    }

    @Test
    fun hasSizeZero() {
        assertThat(ImmutableMultimap.of<Any, Any>()).hasSize(0)
    }

    @Test
    fun hasSizeNegative() {
        assertFailsWith<IllegalArgumentException> {
            assertThat(ImmutableMultimap.of(1, 2)).hasSize(-1)
        }
    }

    @Test
    fun containsKey() {
        val multimap = ImmutableMultimap.of("kurt", "kluever")
        assertThat(multimap).containsKey("kurt")
    }

    @Test
    fun containsKeyFailure() {
        val multimap = ImmutableMultimap.of("kurt", "kluever")
        assertFailsWith<AssertionError> {
            assertThat(multimap).containsKey("daniel")
        }
    }

    @Test
    fun containsKeyNull() {
        val multimap = HashMultimap.create<String?, String>()
        multimap.put(null, "null")
        assertThat(multimap).containsKey(null)
    }

    @Test
    fun containsKeyNullFailure() {
        val multimap = ImmutableMultimap.of("kurt", "kluever")
        assertFailsWith<AssertionError> {
            assertThat(multimap).containsKey(null)
        }
    }

    @Test
    fun containsKey_failsWithSameToString() {
        assertFailsWith<AssertionError> {
            assertThat(
                ImmutableMultimap.of(1L, "value1a", 1L, "value1b", 2L, "value2", "1", "value3")
            ).containsKey(1)
        }
    }

    @Test
    fun doesNotContainKey() {
        val multimap = ImmutableMultimap.of("kurt", "kluever")
        assertThat(multimap).doesNotContainKey("daniel")
        assertThat(multimap).doesNotContainKey(null)
    }

    @Test
    fun doesNotContainKeyFailure() {
        val multimap = ImmutableMultimap.of("kurt", "kluever")
        assertFailsWith<AssertionError> {
            assertThat(multimap).doesNotContainKey("kurt")
        }
    }

    @Test
    fun doesNotContainNullKeyFailure() {
        val multimap = HashMultimap.create<String?, String>()
        multimap.put(null, "null")
        assertFailsWith<AssertionError> {
            assertThat(multimap).doesNotContainKey(null)
        }
    }

    @Test
    fun containsEntry() {
        val multimap = ImmutableMultimap.of("kurt", "kluever")
        assertThat(multimap).containsEntry("kurt", "kluever")
    }

    @Test
    fun containsEntryFailure() {
        val multimap = ImmutableMultimap.of("kurt", "kluever")
        assertFailsWith<AssertionError> {
            assertThat(multimap).containsEntry("daniel", "ploch")
        }
    }

    @Test
    fun containsEntryWithNullValueNullExpected() {
        val actual = ArrayListMultimap.create<String, String?>()
        actual.put("a", null)
        assertThat(actual).containsEntry("a", null)
    }

    @Test
    fun failContainsEntry() {
        val actual = ImmutableMultimap.of("a", "A")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry("b", "B")
        }
    }

    @Test
    fun failContainsEntryFailsWithWrongValueForKey() {
        val actual = ImmutableMultimap.of("a", "A")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry("a", "a")
        }
    }

    @Test
    fun failContainsEntryWithNullValuePresentExpected() {
        val actual = ArrayListMultimap.create<String, String?>()
        actual.put("a", null)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry("a", "A")
        }
    }

    @Test
    fun failContainsEntryWithPresentValueNullExpected() {
        val actual = ImmutableMultimap.of("a", "A")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry("a", null)
        }
    }

    @Test
    fun failContainsEntryFailsWithWrongKeyForValue() {
        val actual = ImmutableMultimap.of("a", "A")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsEntry("b", "A")
        }
    }

    @Test
    fun containsEntry_failsWithSameToString() {
        assertFailsWith<AssertionError> {
            assertThat(
                ImmutableMultimap.builder<Any, Any>()
                    .put(1, "1")
                    .put(1, 1L)
                    .put(1L, 1)
                    .put(2, 3)
                    .build()
            ).containsEntry(1, 1)
        }
    }

    @Test
    fun doesNotContainEntry() {
        val multimap = ImmutableMultimap.of("kurt", "kluever")
        assertThat(multimap).doesNotContainEntry("daniel", "ploch")
    }

    @Test
    fun doesNotContainEntryFailure() {
        val multimap = ImmutableMultimap.of("kurt", "kluever")
        assertFailsWith<AssertionError> {
            assertThat(multimap).doesNotContainEntry("kurt", "kluever")
        }
    }

    @Test
    fun valuesForKey() {
        val multimap = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertThat(multimap).valuesForKey(3).hasSize(3)
        assertThat(multimap).valuesForKey(4).containsExactly("four", "five")
        assertThat(multimap).valuesForKey(3).containsAtLeast("one", "six").inOrder()
        assertThat(multimap).valuesForKey(5).isEmpty()
    }

    @Test
    fun valuesForKeyListMultimap() {
        val multimap = ImmutableListMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertThat(multimap).valuesForKey(4).isInStrictOrder()
    }

    @Test
    fun containsExactlyEntriesIn() {
        val listMultimap =
            ImmutableListMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val setMultimap = ImmutableSetMultimap.copyOf(listMultimap)
        assertThat(listMultimap).containsExactlyEntriesIn(setMultimap)
    }

    @Test
    fun containsExactlyNoArg() {
        val actual = ImmutableMultimap.of<Int, String>()
        assertThat(actual).containsExactly()
        assertThat(actual).containsExactly().inOrder()

        assertFailsWith<AssertionError> {
            assertThat(ImmutableMultimap.of(42, "Answer", 42, "6x7")).containsExactly()
        }
    }

    @Test
    fun containsExactlyEmpty() {
        val actual = ImmutableListMultimap.of<Int, String>()
        val expected = ImmutableSetMultimap.of<Int, String>()
        assertThat(actual).containsExactlyEntriesIn(expected)
        assertThat(actual).containsExactlyEntriesIn(expected).inOrder()
    }

    @Test
    fun containsExactlyRejectsNull() {
        val multimap = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertFailsWith<NullPointerException> {
            assertThat(multimap).containsExactlyEntriesIn(null)
        }
    }

    @Test
    fun containsExactlyRespectsDuplicates() {
        val actual = ImmutableListMultimap.of(3, "one", 3, "two", 3, "one", 4, "five", 4, "five")
        val expected = ImmutableListMultimap.of(3, "two", 4, "five", 3, "one", 4, "five", 3, "one")
        assertThat(actual).containsExactlyEntriesIn(expected)
    }

    @Test
    fun containsExactlyRespectsDuplicatesFailure() {
        val actual = ImmutableListMultimap.of(3, "one", 3, "two", 3, "one", 4, "five", 4, "five")
        val expected = ImmutableSetMultimap.copyOf(actual)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactlyEntriesIn(expected)
        }
    }

    @Test
    fun containsExactlyFailureMissing() {
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val actual = LinkedListMultimap.create(expected)
        actual.remove(3, "six")
        actual.remove(4, "five")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactlyEntriesIn(expected)
        }
    }

    @Test
    fun containsExactlyFailureExtra() {
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val actual = LinkedListMultimap.create(expected)
        actual.put(4, "nine")
        actual.put(5, "eight")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactlyEntriesIn(expected)
        }
    }

    @Test
    fun containsExactlyFailureBoth() {
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val actual = LinkedListMultimap.create(expected)
        actual.remove(3, "six")
        actual.remove(4, "five")
        actual.put(4, "nine")
        actual.put(5, "eight")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactlyEntriesIn(expected)
        }
    }

    @Test
    fun containsExactlyFailureWithEmptyStringMissing() {
        assertFailsWith<AssertionError> {
            assertThat(ImmutableMultimap.of<Any, Any>()).containsExactly("" to "a")
        }
    }

    @Test
    fun containsExactlyFailureWithEmptyStringExtra() {
        assertFailsWith<AssertionError> {
            assertThat(ImmutableMultimap.of("a", "", "", "")).containsExactly("a" to "")
        }
    }

    @Test
    fun containsExactlyFailureWithEmptyStringBoth() {
        assertFailsWith<AssertionError> {
            assertThat(ImmutableMultimap.of("a", "")).containsExactly("" to "a")
        }
    }

    @Test
    fun containsExactlyInOrder() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertThat(actual).containsExactlyEntriesIn(expected).inOrder()
    }

    @Test
    fun containsExactlyInOrderDifferentTypes() {
        val listMultimap =
            ImmutableListMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val setMultimap = ImmutableSetMultimap.copyOf(listMultimap)
        assertThat(listMultimap).containsExactlyEntriesIn(setMultimap).inOrder()
    }

    @Test
    fun containsExactlyInOrderFailure() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val expected = ImmutableMultimap.of(4, "four", 3, "six", 4, "five", 3, "two", 3, "one")
        assertThat(actual).containsExactlyEntriesIn(expected)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactlyEntriesIn(expected).inOrder()
        }
    }

    @Test
    fun containsExactlyInOrderFailureValuesOnly() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val expected = ImmutableMultimap.of(3, "six", 3, "two", 3, "one", 4, "five", 4, "four")
        assertThat(actual).containsExactlyEntriesIn(expected)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactlyEntriesIn(expected).inOrder()
        }
    }

    @Test
    fun containsExactlyVararg() {
        val listMultimap = ImmutableListMultimap.of(1, "one", 3, "six", 3, "two")
        assertThat(listMultimap).containsExactly(1 to "one", 3 to "six", 3 to "two")
    }

    @Test
    fun containsExactlyVarargWithNull() {
        val listMultimap =
            LinkedListMultimap.create(ImmutableListMultimap.of(1, "one", 3, "six", 3, "two"))
        listMultimap.put(4, null)
        assertThat(listMultimap).containsExactly(1 to "one", 3 to "six", 3 to "two", 4 to null)
    }

    @Test
    fun containsExactlyVarargFailureMissing() {
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val actual = LinkedListMultimap.create(expected)
        actual.remove(3, "six")
        actual.remove(4, "five")
        assertFailsWith<AssertionError> {
            assertThat(actual)
                .containsExactly(3 to "one", 3 to "six", 3 to "two", 4 to "five", 4 to "four")
        }
    }

    @Test
    fun containsExactlyVarargFailureExtra() {
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val actual = LinkedListMultimap.create(expected)
        actual.put(4, "nine")
        actual.put(5, "eight")
        assertFailsWith<AssertionError> {
            assertThat(actual)
                .containsExactly(3 to "one", 3 to "six", 3 to "two", 4 to "five", 4 to "four")
        }
    }

    @Test
    fun containsExactlyVarargFailureBoth() {
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val actual = LinkedListMultimap.create(expected)
        actual.remove(3, "six")
        actual.remove(4, "five")
        actual.put(4, "nine")
        actual.put(5, "eight")
        assertFailsWith<AssertionError> {
            assertThat(actual)
                .containsExactly(3 to "one", 3 to "six", 3 to "two", 4 to "five", 4 to "four")
        }
    }

    @Test
    fun containsExactlyVarargRespectsDuplicates() {
        val actual = ImmutableListMultimap.of(3, "one", 3, "two", 3, "one", 4, "five", 4, "five")
        assertThat(actual)
            .containsExactly(3 to "two", 4 to "five", 3 to "one", 4 to "five", 3 to "one")
    }

    @Test
    fun containsExactlyVarargRespectsDuplicatesFailure() {
        val actual = ImmutableListMultimap.of(3, "one", 3, "two", 3, "one", 4, "five", 4, "five")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactly(3 to "one", 3 to "two", 4 to "five")
        }
    }

    @Test
    fun containsExactlyVarargInOrder() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertThat(actual)
            .containsExactly(3 to "one", 3 to "six", 3 to "two", 4 to "five", 4 to "four")
            .inOrder()
    }

    @Test
    fun containsExactlyVarargInOrderFailure() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertThat(actual)
            .containsExactly(4 to "four", 3 to "six", 4 to "five", 3 to "two", 3 to "one")
        assertFailsWith<AssertionError> {
            assertThat(actual)
                .containsExactly(4 to "four", 3 to "six", 4 to "five", 3 to "two", 3 to "one")
                .inOrder()
        }
    }

    @Test
    fun containsExactlyVarargInOrderFailureValuesOnly() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertThat(actual)
            .containsExactly(3 to "six", 3 to "two", 3 to "one", 4 to "five", 4 to "four")
        assertFailsWith<AssertionError> {
            assertThat(actual)
                .containsExactly(3 to "six", 3 to "two", 3 to "one", 4 to "five", 4 to "four")
                .inOrder()
        }
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun containsExactlyEntriesIn_homogeneousMultimap_failsWithSameToString() {
        assertFailsWith<AssertionError> {
            assertThat<Any, Any>(ImmutableMultimap.of(1, "a", 1, "b", 2, "c"))
                .containsExactlyEntriesIn(ImmutableMultimap.of(1L, "a", 1L, "b", 2L, "c"))
        }
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun containsExactlyEntriesIn_heterogeneousMultimap_failsWithSameToString() {
        assertFailsWith<AssertionError> {
            assertThat<Any, Any>(ImmutableMultimap.of(1, "a", 1, "b", 2L, "c"))
                .containsExactlyEntriesIn(ImmutableMultimap.of(1L, "a", 1L, "b", 2, "c"))
        }
    }

    @Test
    fun containsAtLeastEntriesIn() {
        val actual = ImmutableListMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val expected = ImmutableSetMultimap.of(3, "one", 3, "six", 3, "two", 4, "five")
        assertThat(actual).containsAtLeastEntriesIn(expected)
    }

    @Test
    fun containsAtLeastEmpty() {
        val actual = ImmutableListMultimap.of(3, "one")
        val expected = ImmutableSetMultimap.of<Int, String>()
        assertThat(actual).containsAtLeastEntriesIn(expected)
        assertThat(actual).containsAtLeastEntriesIn(expected).inOrder()
    }

    @Test
    fun containsAtLeastRejectsNull() {
        val multimap = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertFailsWith<NullPointerException> {
            assertThat(multimap).containsAtLeastEntriesIn(null)
        }
    }

    @Test
    fun containsAtLeastRespectsDuplicates() {
        val actual = ImmutableListMultimap.of(3, "one", 3, "two", 3, "one", 4, "five", 4, "five")
        val expected = ImmutableListMultimap.of(3, "two", 4, "five", 3, "one", 4, "five", 3, "one")
        assertThat(actual).containsAtLeastEntriesIn(expected)
    }

    @Test
    fun containsAtLeastRespectsDuplicatesFailure() {
        val expected = ImmutableListMultimap.of(3, "one", 3, "two", 3, "one", 4, "five", 4, "five")
        val actual = ImmutableSetMultimap.copyOf(expected)
        assertFailsWith<AssertionError> { assertThat(actual).containsAtLeastEntriesIn(expected) }
    }

    @Test
    fun containsAtLeastFailureMissing() {
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val actual = LinkedListMultimap.create(expected)
        actual.remove(3, "six")
        actual.remove(4, "five")
        actual.put(50, "hawaii")
        assertFailsWith<AssertionError> { assertThat(actual).containsAtLeastEntriesIn(expected) }
    }

    @Test
    fun containsAtLeastFailureWithEmptyStringMissing() {
        assertFailsWith<AssertionError> {
            assertThat(ImmutableMultimap.of("key", "value")).containsAtLeast("" to "a")
        }
    }

    @Test
    fun containsAtLeastInOrder() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 4, "five", 4, "four")
        assertThat(actual).containsAtLeastEntriesIn(expected).inOrder()
    }

    @Test
    fun containsAtLeastInOrderDifferentTypes() {
        val actual = ImmutableListMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val expected = ImmutableSetMultimap.of(3, "one", 3, "six", 4, "five", 4, "four")
        assertThat(actual).containsAtLeastEntriesIn(expected).inOrder()
    }

    @Test
    fun containsAtLeastInOrderFailure() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val expected = ImmutableMultimap.of(4, "four", 3, "six", 3, "two", 3, "one")
        assertThat(actual).containsAtLeastEntriesIn(expected)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsAtLeastEntriesIn(expected).inOrder()
        }
    }

    @Test
    fun containsAtLeastInOrderFailureValuesOnly() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val expected = ImmutableMultimap.of(3, "six", 3, "one", 4, "five", 4, "four")
        assertThat(actual).containsAtLeastEntriesIn(expected)
        assertFailsWith<AssertionError> {
            assertThat(actual).containsAtLeastEntriesIn(expected).inOrder()
        }
    }

    @Test
    fun containsAtLeastVararg() {
        val listMultimap = ImmutableListMultimap.of(1, "one", 3, "six", 3, "two", 3, "one")
        assertThat(listMultimap).containsAtLeast(1 to "one", 3 to "six", 3 to "two")
    }

    @Test
    fun containsAtLeastVarargWithNull() {
        val listMultimap =
            LinkedListMultimap.create(ImmutableListMultimap.of(1, "one", 3, "six", 3, "two"))
        listMultimap.put(4, null)
        assertThat(listMultimap).containsAtLeast(1 to "one", 3 to "two", 4 to null)
    }

    @Test
    fun containsAtLeastVarargFailureMissing() {
        val expected = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        val actual = LinkedListMultimap.create(expected)
        actual.remove(3, "six")
        actual.remove(4, "five")
        actual.put(3, "nine")
        assertFailsWith<AssertionError> {
            assertThat(actual)
                .containsAtLeast(3 to "one", 3 to "six", 3 to "two", 4 to "five", 4 to "four")
        }
    }

    @Test
    fun containsAtLeastVarargRespectsDuplicates() {
        val actual = ImmutableListMultimap.of(3, "one", 3, "two", 3, "one", 4, "five", 4, "five")
        assertThat(actual).containsAtLeast(3 to "two", 4 to "five", 3 to "one", 3 to "one")
    }

    @Test
    fun containsAtLeastVarargRespectsDuplicatesFailure() {
        val actual = ImmutableListMultimap.of(3, "one", 3, "two", 4, "five", 4, "five")
        assertFailsWith<AssertionError> {
            assertThat(actual).containsAtLeast(3 to "one", 3 to "one", 3 to "one", 4 to "five")
        }
    }

    @Test
    fun containsAtLeastVarargInOrder() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertThat(actual)
            .containsAtLeast(3 to "one", 3 to "six", 4 to "five", 4 to "four")
            .inOrder()
    }

    @Test
    fun containsAtLeastVarargInOrderFailure() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertThat(actual).containsAtLeast(4 to "four", 3 to "six", 3 to "two", 3 to "one")
        assertFailsWith<AssertionError> {
            assertThat(actual)
                .containsAtLeast(4 to "four", 3 to "six", 3 to "two", 3 to "one")
                .inOrder()
        }
    }

    @Test
    fun containsAtLeastVarargInOrderFailureValuesOnly() {
        val actual = ImmutableMultimap.of(3, "one", 3, "six", 3, "two", 4, "five", 4, "four")
        assertThat(actual).containsAtLeast(3 to "two", 3 to "one", 4 to "five", 4 to "four")
        assertFailsWith<AssertionError> {
            assertThat(actual)
                .containsAtLeast(3 to "two", 3 to "one", 4 to "five", 4 to "four")
                .inOrder()
        }
    }
}
