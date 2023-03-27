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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IterableSubjectTest {

    @Test
    fun hasSize() {
        assertThat(listOf(1, 2, 3)).hasSize(3)
    }

    @Test
    fun hasSizeZero() {
        assertThat(emptyList<Any>()).hasSize(0)
    }

    @Test
    fun hasSizeFails() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).hasSize(4)
        }
    }

    @Test
    fun hasSizeNegative() {
        assertFailsWith<IllegalArgumentException> {
            assertThat(listOf(1, 2, 3)).hasSize(-1)
        }
    }

    @Test
    fun iterableContains() {
        assertThat(listOf(1, 2, 3)).contains(1)
    }

    @Test
    fun iterableContainsWithNull() {
        assertThat(listOf(1, null, 3)).contains(null)
    }

    @Test
    fun iterableContainsFailsWithSameToString() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1L, 2L, 3L, 2L)).contains(2)
        }
    }

    @Test
    fun iterableContainsFailsWithSameToStringAndNull() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, "null")).contains(null)
        }
    }

    @Test
    fun iterableContainsFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).contains(5)
        }
    }

    @Test
    fun iterableDoesNotContain() {
        assertThat(listOf(1, null, 3)).doesNotContain(5)
    }

    @Test
    fun iterableDoesNotContainNull() {
        assertThat(listOf(1, 2, 3)).doesNotContain(null)
    }

    @Test
    fun iterableDoesNotContainFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).doesNotContain(2)
        }
    }

    @Test
    fun doesNotContainDuplicates() {
        assertThat(listOf(1, 2, 3)).containsNoDuplicates()
    }

    @Test
    fun doesNotContainDuplicatesMixedTypes() {
        assertThat(listOf<Any>(1, 2, 2L, 3)).containsNoDuplicates()
    }

    @Test
    fun doesNotContainDuplicatesFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 2, 3)).containsNoDuplicates()
        }
    }

    @Test
    fun iterableContainsAnyOf() {
        assertThat(listOf(1, 2, 3)).containsAnyOf(1, 5)
    }

    @Test
    fun iterableContainsAnyOfWithNull() {
        assertThat(listOf(1, null, 3)).containsAnyOf(null, 5)
    }

    @Test
    fun iterableContainsAnyOfWithNullInThirdAndFinalPosition() {
        assertThat(listOf(1, null, 3)).containsAnyOf(4, 5, null as Int?)
    }

    @Test
    fun iterableContainsAnyOfFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsAnyOf(5, 6, 0)
        }
    }

    @Test
    fun iterableContainsAnyOfFailsWithSameToStringAndHomogeneousList() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1L, 2L, 3L)).containsAnyOf(5, 6, 0)
        }
    }

    @Test
    fun iterableContainsAnyOfFailsWithSameToStringAndHomogeneousListWithDuplicates() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(3L, 3L)).containsAnyOf(2, 3, 3)
        }
    }

    @Test
    fun iterableContainsAnyOfFailsWithSameToStringAndNullInSubject() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(null, "abc")).containsAnyOf("def", "null")
        }
    }

    @Test
    fun iterableContainsAnyOfFailsWithSameToStringAndNullInExpectation() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("null", "abc")).containsAnyOf("def", null)
        }
    }

    @Test
    fun iterableContainsAnyOfWithOneShotIterable() {
        val iterator = listOf(2 as Any, 1, "b").iterator()

        val iterable =
            object : Iterable<Any> {
                override fun iterator(): Iterator<Any> = iterator
            }

        assertThat(iterable).containsAnyOf(3, "a", 7, "b", 0)
    }

    @Test
    fun iterableContainsAnyInIterable() {
        assertThat(listOf(1, 2, 3)).containsAnyIn(listOf(1, 10, 100))

        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsAnyIn(listOf(5, 6, 0))
        }
    }

    @Test
    fun iterableContainsAnyInArray() {
        assertThat(listOf(1, 2, 3)).containsAnyIn(arrayOf(1, 10, 100))

        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsAnyIn(arrayOf(5, 6, 0))
        }
    }

    @Test
    fun iterableContainsAtLeast() {
        assertThat(listOf(1, 2, 3)).containsAtLeast(1, 2)
    }

    @Test
    fun iterableContainsAtLeastWithMany() {
        assertThat(listOf(1, 2, 3)).containsAtLeast(1, 2)
    }

    @Test
    fun iterableContainsAtLeastWithDuplicates() {
        assertThat(listOf(1, 2, 2, 2, 3)).containsAtLeast(2, 2)
    }

    @Test
    fun iterableContainsAtLeastWithNull() {
        assertThat(listOf(1, null, 3)).containsAtLeast(3, null as Int?)
    }

    @Test
    fun iterableContainsAtLeastWithNullAtThirdAndFinalPosition() {
        assertThat(listOf(1, null, 3)).containsAtLeast(1, 3, null as Any?)
    }

    /*
     * Test that we only call toString() if the assertion fails -- that is, not just if the elements
     * are out of order, but only if someone actually calls inOrder(). There are 2 reasons for this:
     *
     * 1. Calling toString() uses extra time and space. (To be fair, Iterable assertions often use a
     * lot of those already.)
     *
     * 2. Some toString() methods are buggy. Arguably we shouldn't accommodate these, especially since
     * those users are in for a nasty surprise if their tests actually fail someday, but I don't want
     * to bite that off now. (Maybe Fact should catch exceptions from toString()?)
     */
    @Test
    fun iterableContainsAtLeastElementsInOutOfOrderDoesNotStringify() {
        val o = CountsToStringCalls()
        val actual: List<Any> = listOf(o, 1)
        val expected: List<Any> = listOf(1, o)
        assertThat(actual).containsAtLeastElementsIn(expected)
        assertThat(o.calls).isEqualTo(0)

        assertFailsWith<AssertionError> {
            assertThat(actual).containsAtLeastElementsIn(expected).inOrder()
        }

        assertThat(o.calls > 0)
    }

    @Test
    fun iterableContainsAtLeastFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsAtLeast(1, 2, 4)
        }
    }

    @Test
    fun iterableContainsAtLeastWithExtras() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("y", "x")).containsAtLeast("x", "y", "z")
        }
    }

    @Test
    fun iterableContainsAtLeastWithExtraCopiesOfOutOfOrder() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("y", "x")).containsAtLeast("x", "y", "y")
        }
    }

    @Test
    fun iterableContainsAtLeastWithDuplicatesFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsAtLeast(1, 2, 2, 2, 3, 4)
        }
    }

    /*
     * Slightly subtle test to ensure that if multiple equal elements are found
     * to be missing we only reference it once in the output message.
     */
    @Test
    fun iterableContainsAtLeastWithDuplicateMissingElements() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2)).containsAtLeast(4, 4, 4)
        }
    }

    @Test
    fun iterableContainsAtLeastWithNullFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, null, 3)).containsAtLeast(1, null, null, 3)
        }
    }

    @Test
    fun iterableContainsAtLeastFailsWithSameToStringAndHomogeneousList() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1L, 2L)).containsAtLeast(1, 2)
        }
    }

    @Test
    fun iterableContainsAtLeastFailsWithSameToStringAndHomogeneousListWithDuplicates() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1L, 2L, 2L)).containsAtLeast(1, 1, 2)
        }
    }

    @Test
    fun iterableContainsAtLeastFailsWithSameToStringAndHomogeneousListWithNull() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("null", "abc")).containsAtLeast("abc", null)
        }
    }

    @Test
    fun iterableContainsAtLeastFailsWithSameToStringAndHeterogeneousListWithDuplicates() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 2L, 3L, 3L)).containsAtLeast(2L, 2L, 3, 3)
        }
    }

    @Test
    fun iterableContainsAtLeastFailsWithEmptyString() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("a", null)).containsAtLeast("", null)
        }
    }

    @Test
    fun iterableContainsAtLeastInOrder() {
        assertThat(listOf(3, 2, 5)).containsAtLeast(3, 2, 5).inOrder()
    }

    @Test
    fun iterableContainsAtLeastInOrderWithGaps() {
        assertThat(listOf(3, 2, 5)).containsAtLeast(3, 5).inOrder()
        assertThat(listOf(3, 2, 2, 4, 5)).containsAtLeast(3, 2, 2, 5).inOrder()
        assertThat(listOf(3, 1, 4, 1, 5)).containsAtLeast(3, 1, 5).inOrder()
        assertThat(listOf("x", "y", "y", "z")).containsAtLeast("x", "y", "z").inOrder()
        assertThat(listOf("x", "x", "y", "z")).containsAtLeast("x", "y", "z").inOrder()
        assertThat(listOf("z", "x", "y", "z")).containsAtLeast("x", "y", "z").inOrder()
        assertThat(listOf("x", "x", "y", "z", "x")).containsAtLeast("x", "y", "z", "x").inOrder()
    }

    @Test
    fun iterableContainsAtLeastInOrderWithNull() {
        assertThat(listOf(3, null, 5)).containsAtLeast(3, null, 5).inOrder()
        assertThat(listOf(3, null, 7, 5)).containsAtLeast(3, null, 5).inOrder()
    }

    @Test
    fun iterableContainsAtLeastInOrderWithFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, null, 3)).containsAtLeast(null, 1, 3).inOrder()
        }
    }

    @Test
    fun iterableContainsAtLeastInOrderWithFailureWithActualOrder() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, null, 3, 4)).containsAtLeast(null, 1, 3).inOrder()
        }
    }

    @Test
    fun iterableContainsAtLeastInOrderWithOneShotIterable() {
        val iterable = listOf(2, 1, null, 4, "a", 3, "b")
        val iterator = iterable.iterator()

        val oneShot =
            object : Iterable<Any?> {
                override fun iterator(): Iterator<Any?> = iterator
            }

        assertThat(oneShot).containsAtLeast(1, null, 3).inOrder()
    }

    @Test
    fun iterableContainsAtLeastInOrderWithOneShotIterableWrongOrder() {
        val iterator = listOf(2, 1, null, 4, "a", 3, "b").iterator()

        val iterable =
            object : Iterable<Any?> {
                override fun iterator(): Iterator<Any?> = iterator
            }
        assertFailsWith<AssertionError> {
            assertThat(iterable).containsAtLeast(1, 3, null as Any?).inOrder()
        }
    }

    @Test
    fun iterableContainsAtLeastInOrderWrongOrderAndMissing() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2)).containsAtLeast(2, 1, 3).inOrder()
        }
    }

    @Test
    fun iterableContainsAtLeastElementsInIterable() {
        assertThat(listOf(1, 2, 3)).containsAtLeastElementsIn(listOf(1, 2))

        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsAtLeastElementsIn(listOf(1, 2, 4))
        }
    }

    @Test
    fun iterableContainsAtLeastElementsInCanUseFactPerElement() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("abc")).containsAtLeastElementsIn(listOf("123\n456", "789"))
        }
    }

    @Test
    fun iterableContainsAtLeastElementsInArray() {
        assertThat(listOf(1, 2, 3)).containsAtLeastElementsIn(arrayOf(1, 2))

        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsAtLeastElementsIn(arrayOf(1, 2, 4))
        }
    }

    @Test
    fun iterableContainsNoneOf() {
        assertThat(listOf(1, 2, 3)).containsNoneOf(4, 5, 6)
    }

    @Test
    fun iterableContainsNoneOfFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsNoneOf(1, 2, 4)
        }
    }

    @Test
    fun iterableContainsNoneOfFailureWithDuplicateInSubject() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 2, 3)).containsNoneOf(1, 2, 4)
        }
    }

    @Test
    fun iterableContainsNoneOfFailureWithDuplicateInExpected() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsNoneOf(1, 2, 2, 4)
        }
    }

    @Test
    fun iterableContainsNoneOfFailureWithEmptyString() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("")).containsNoneOf("", null)
        }
    }

    @Test
    fun iterableContainsNoneInIterable() {
        assertThat(listOf(1, 2, 3)).containsNoneIn(listOf(4, 5, 6))
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsNoneIn(listOf(1, 2, 4))
        }
    }

    @Test
    fun iterableContainsNoneInArray() {
        assertThat(listOf(1, 2, 3)).containsNoneIn(arrayOf(4, 5, 6))
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsNoneIn(arrayOf(1, 2, 4))
        }
    }

        @Test
    fun iterableContainsExactlyArray() {
        val stringArray = arrayOf("a", "b")
        val iterable = listOf(stringArray)
        // This test fails w/o the explicit cast
        assertThat(iterable).containsExactly(stringArray as Any)
    }

    @Test
    fun arrayContainsExactly() {
        val iterable = listOf("a", "b")
        val array = arrayOf("a", "b")
        assertThat(iterable).containsExactly(*array)
    }

    @Test
    fun iterableContainsExactlyWithMany() {
        assertThat(listOf(1, 2, 3)).containsExactly(1, 2, 3)
    }

    @Test
    fun iterableContainsExactlyOutOfOrder() {
        assertThat(listOf(1, 2, 3, 4)).containsExactly(3, 1, 4, 2)
    }

    @Test
    fun iterableContainsExactlyWithDuplicates() {
        assertThat(listOf(1, 2, 2, 2, 3)).containsExactly(1, 2, 2, 2, 3)
    }

    @Test
    fun iterableContainsExactlyWithDuplicatesOutOfOrder() {
        assertThat(listOf(1, 2, 2, 2, 3)).containsExactly(2, 1, 2, 3, 2)
    }

    @Test
    fun iterableContainsExactlyWithOnlyNullPassedAsNullArray() {
        // Truth is tolerant of this erroneous varargs call.
        val actual = listOf(null as Any?)
        assertThat(actual).containsExactly(null as Array<Any?>?)
    }

    @Test
    fun iterableContainsExactlyWithOnlyNull() {
        val actual = listOf(null as Any?)
        assertThat(actual).containsExactly(null as Any?)
    }

    @Test
    fun iterableContainsExactlyWithNullSecond() {
        assertThat(listOf(1, null)).containsExactly(1, null)
    }

    @Test
    fun iterableContainsExactlyWithNullThird() {
        assertThat(listOf(1, 2, null)).containsExactly(1, 2, null)
    }

    @Test
    fun iterableContainsExactlyWithNull() {
        assertThat(listOf(1, null, 3)).containsExactly(1, null, 3)
    }

    @Test
    fun iterableContainsExactlyWithNullOutOfOrder() {
        assertThat(listOf(1, null, 3)).containsExactly(1, 3, null as Int?)
    }

    @Test
    fun iterableContainsExactlyOutOfOrderDoesNotStringify() {
        val o = CountsToStringCalls()
        val actual: List<Any> = listOf(o, 1)
        val expected: List<Any?> = listOf(1, o)
        assertThat(actual).containsExactlyElementsIn(expected)
        assertEquals(0, o.calls)

        assertFailsWith<AssertionError> {
            assertThat(actual).containsExactlyElementsIn(expected).inOrder()
        }

        assertTrue(o.calls > 0)
    }

    @Test
    fun iterableContainsExactlyWithEmptyString() {
        assertFailsWith<AssertionError> {
            assertThat(listOf<Any?>()).containsExactly("")
        }
    }

    @Test
    fun iterableContainsExactlyWithEmptyStringAndUnexpectedItem() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("a", null)).containsExactly("")
        }
    }

    @Test
    fun iterableContainsExactlyWithEmptyStringAndMissingItem() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("")).containsExactly("a", null)
        }
    }

    @Test
    fun iterableContainsExactlyWithEmptyStringAmongMissingItems() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("a")).containsExactly("", "b")
        }
    }

    @Test
    fun iterableContainsExactlySingleElement() {
        assertThat(listOf(1)).containsExactly(1)
        assertFailsWith<AssertionError> {
            assertThat(listOf(1)).containsExactly(2)
        }
    }

    @Test
    fun iterableContainsExactlySingleElementNoEqualsMagic() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1)).containsExactly(1L)
        }
    }

    @Test
    fun iterableContainsExactlyWithElementsThatThrowWhenYouCallHashCode() {
        val one = HashCodeThrower()
        val two = HashCodeThrower()
        assertThat(listOf(one, two)).containsExactly(two, one)
        assertThat(listOf(one, two)).containsExactly(one, two).inOrder()
        assertThat(listOf(one, two)).containsExactlyElementsIn(listOf(two, one))
        assertThat(listOf(one, two)).containsExactlyElementsIn(listOf(one, two)).inOrder()
    }

    @Test
    fun iterableContainsExactlyWithElementsThatThrowWhenYouCallHashCodeFailureTooMany() {
        val one = HashCodeThrower()
        val two = HashCodeThrower()
        assertFailsWith<AssertionError> {
            assertThat(listOf(one, two)).containsExactly(one)
        }
    }

    @Test
    fun iterableContainsExactlyWithElementsThatThrowWhenYouCallHashCodeOneMismatch() {
        val one = HashCodeThrower()
        val two = HashCodeThrower()
        assertFailsWith<AssertionError> {
            assertThat(listOf(one, one)).containsExactly(one, two)
        }
    }

    @Test
    fun iterableContainsExactlyElementsInInOrderPassesWithEmptyExpectedAndActual() {
        assertThat(emptyList<Any>()).containsExactlyElementsIn(emptyList<Any>()).inOrder()
    }

    @Test
    fun iterableContainsExactlyElementsInWithEmptyExpected() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("foo")).containsExactlyElementsIn(emptyList<String>())
        }
    }

    @Test
    fun iterableContainsExactlyElementsInErrorMessageIsInOrder() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("foo OR bar")).containsExactlyElementsIn(listOf("foo", "bar"))
        }
    }

    @Test
    fun iterableContainsExactlyMissingItemFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2)).containsExactly(1, 2, 4)
        }
    }

    @Test
    fun iterableContainsExactlyUnexpectedItemFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsExactly(1, 2)
        }
    }

    @Test
    fun iterableContainsExactlyWithDuplicatesNotEnoughItemsFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsExactly(1, 2, 2, 2, 3)
        }
    }

    @Test
    fun iterableContainsExactlyWithDuplicatesMissingItemFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsExactly(1, 2, 2, 2, 3, 4)
        }
    }

    @Test
    fun iterableContainsExactlyWithDuplicatesMissingItemsWithNewlineFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("a", "b", "foo\nbar"))
                .containsExactly("a", "b", "foo\nbar", "foo\nbar", "foo\nbar")
        }
    }

    @Test
    fun iterableContainsExactlyWithDuplicatesMissingAndExtraItemsWithNewlineFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("a\nb", "a\nb")).containsExactly("foo\nbar", "foo\nbar")
        }
    }

    @Test
    fun iterableContainsExactlyWithDuplicatesUnexpectedItemFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 2, 2, 2, 3)).containsExactly(1, 2, 2, 3)
        }
    }

    /*
   * Slightly subtle test to ensure that if multiple equal elements are found
   * to be missing we only reference it once in the output message.
   */
    @Test
    fun iterableContainsExactlyWithDuplicateMissingElements() {
        assertFailsWith<AssertionError> {
            assertThat(listOf<Any?>()).containsExactly(4, 4, 4)
        }
    }

    @Test
    fun iterableContainsExactlyWithNullFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, null, 3)).containsExactly(1, null, null, 3)
        }
    }

    @Test
    fun iterableContainsExactlyWithMissingAndExtraElements() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3)).containsExactly(1, 2, 4)
        }
    }

    @Test
    fun iterableContainsExactlyWithDuplicateMissingAndExtraElements() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3, 3)).containsExactly(1, 2, 4, 4)
        }
    }

    @Test
    fun iterableContainsExactlyWithCommaSeparatedVsIndividual() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("a, b")).containsExactly("a", "b")
        }
    }

    @Test
    fun iterableContainsExactlyFailsWithSameToStringAndHomogeneousList() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1L, 2L)).containsExactly(1, 2)
        }
    }

    @Test
    fun iterableContainsExactlyFailsWithSameToStringAndListWithNull() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1L, 2L)).containsExactly(null, 1, 2)
        }
    }

    @Test
    fun iterableContainsExactlyFailsWithSameToStringAndHeterogeneousList() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1L, 2)).containsExactly(1, null, 2L)
        }
    }

    @Test
    fun iterableContainsExactlyFailsWithSameToStringAndHomogeneousListWithDuplicates() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1L, 2L)).containsExactly(1, 2, 2)
        }
    }

    @Test
    fun iterableContainsExactlyFailsWithSameToStringAndHeterogeneousListWithDuplicates() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1L, 2)).containsExactly(1, null, null, 2L, 2L)
        }
    }

    @Test
    fun iterableContainsExactlyWithOneIterableGivesWarning() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3, 4)).containsExactly(listOf(1, 2, 3, 4))
        }
    }

    @Test
    fun iterableContainsExactlyElementsInWithOneIterableDoesNotGiveWarning() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3, 4)).containsExactlyElementsIn(listOf(1, 2, 3))
        }
    }

    @Test
    fun iterableContainsExactlyWithTwoIterableDoesNotGivesWarning() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3, 4)).containsExactly(
                listOf(1, 2),
                listOf(3, 4)
            )
        }
    }

    @Test
    fun iterableContainsExactlyWithOneNonIterableDoesNotGiveWarning() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3, 4)).containsExactly(1)
        }
    }

    @Test
    fun iterableContainsExactlyInOrder() {
        assertThat(listOf(3, 2, 5)).containsExactly(3, 2, 5).inOrder()
    }

    @Test
    fun iterableContainsExactlyInOrderWithNull() {
        assertThat(listOf(3, null, 5)).containsExactly(3, null, 5).inOrder()
    }

    @Test
    fun iterableContainsExactlyInOrderWithFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, null, 3)).containsExactly(null, 1, 3).inOrder()
        }
    }

    @Test
    fun iterableContainsExactlyInOrderWithOneShotIterable() {
        val iterator = listOf(1 as Any, null, 3).iterator()
        val iterable = object : Iterable<Any?> {
            override fun iterator(): Iterator<Any?> = iterator
        }
        assertThat(iterable).containsExactly(1, null, 3).inOrder()
    }

    @Test
    fun iterableContainsExactlyInOrderWithOneShotIterableWrongOrder() {
        val iterator = listOf(1 as Any, null, 3).iterator()
        val iterable = object : Iterable<Any?> {
            override fun iterator(): Iterator<Any?> = iterator
            override fun toString(): String = "BadIterable"
        }
        assertFailsWith<AssertionError> {
            assertThat(iterable).containsExactly(1, 3, null).inOrder()
        }
    }

    @Test
    fun iterableWithNoToStringOverride() {
        val iterable = object : Iterable<Int?> {
            override fun iterator(): Iterator<Int> = listOf(1, 2, 3).iterator()
        }
        assertFailsWith<AssertionError> {
            assertThat(iterable).containsExactly(1, 2).inOrder()
        }
    }

    @Test
    fun iterableContainsExactlyElementsInIterable() {
        assertThat(listOf(1, 2)).containsExactlyElementsIn(listOf(1, 2))

        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3, 4)).containsExactlyElementsIn(listOf(1, 2, 3))
        }
    }

    @Test
    fun iterableContainsExactlyElementsInArray() {
        assertThat(listOf(1, 2)).containsExactlyElementsIn(arrayOf<Any?>(1, 2))

        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2)).containsExactlyElementsIn(arrayOf<Int?>(1, 2, 4))
        }
    }

    @Test
    fun nullEqualToNull() {
        assertThat(null as Iterable<*>?).isEqualTo(null)
    }

    @Test
    fun nullEqualToSomething() {
        assertFailsWith<AssertionError> {
            assertThat(null as Iterable<*>?).isEqualTo(emptyList<Any>())
        }
    }

    @Test
    fun somethingEqualToNull() {
        assertFailsWith<AssertionError> {
            assertThat(emptyList<Any>()).isEqualTo(null)
        }
    }

    @Test
    fun somethingEqualToSomething() {
        assertFailsWith<AssertionError> {
            assertThat(emptyList<Any>()).isEqualTo(listOf("a"))
        }
    }

//    @Test
//    fun isEqualToNotConsistentWithEquals() {
//        val actual: TreeSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
//        val expected: TreeSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
//        actual.add("one")
//        expected.add("ONE")
//        /*
//     * Our contract doesn't guarantee that the following test will pass. It *currently* does,
//     * though, and if we change that behavior, we want this test to let us know.
//     */assertThat(actual).isEqualTo(expected)
//    }
//
//    @Test
//    fun isEqualToNotConsistentWithEquals_failure() {
//        val actual: TreeSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
//        val expected: TreeSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
//        actual.add("one")
//        expected.add("ONE")
//        actual.add("two")
//        expectFailureWhenTestingThat(actual).isEqualTo(expected)
//        // The exact message generated is unspecified.
//    }
//
    @Test
    fun iterableIsEmpty() {
        assertThat(emptyList<Any>()).isEmpty()
    }

    @Test
    fun iterableIsEmptyWithFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, null, 3)).isEmpty()
        }
    }

    @Test
    fun iterableIsNotEmpty() {
        assertThat(listOf("foo")).isNotEmpty()
    }

    @Test
    fun iterableIsNotEmptyWithFailure() {
        assertFailsWith<AssertionError> {
            assertThat(emptyList<Any>()).isNotEmpty()
        }
    }

    @Test
    fun iterableIsInStrictOrder() {
        assertThat(emptyList<Any>()).isInStrictOrder()
        assertThat(listOf(1)).isInStrictOrder()
        assertThat(listOf(1, 2, 3, 4)).isInStrictOrder()
    }

    @Test
    fun isInStrictOrderFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 2, 4)).isInStrictOrder()
        }
    }

    @Test
    fun iterableIsInOrder() {
        assertThat(listOf<Any?>()).isInOrder()
        assertThat(listOf(1)).isInOrder()
        assertThat(listOf(1, 1, 2, 3, 3, 3, 4)).isInOrder()
    }

    @Test
    fun isInOrderFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 3, 2, 4)).isInOrder()
        }
    }

    @Test
    fun isInOrderMultipleFailures() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 3, 2, 4, 0)).isInOrder()
        }
    }

    @Test
    fun iterableIsInStrictOrderWithComparator() {
        val emptyStrings: Iterable<String> = listOf()
        assertThat(emptyStrings).isInStrictOrder(COMPARE_AS_DECIMAL)
        assertThat(listOf("1")).isInStrictOrder(COMPARE_AS_DECIMAL)
        // Note: Use "10" and "20" to distinguish numerical and lexicographical ordering.
        assertThat(listOf("1", "2", "10", "20")).isInStrictOrder(COMPARE_AS_DECIMAL)
    }

    @Test
    fun iterableIsInStrictOrderWithComparatorFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("1", "2", "2", "10")).isInStrictOrder(COMPARE_AS_DECIMAL)
        }
    }

    @Test
    fun iterableIsInOrderWithComparator() {
        val emptyStrings: Iterable<String> = listOf()
        assertThat(emptyStrings).isInOrder(COMPARE_AS_DECIMAL)
        assertThat(listOf("1")).isInOrder(COMPARE_AS_DECIMAL)
        assertThat(listOf("1", "1", "2", "10", "10", "10", "20")).isInOrder(COMPARE_AS_DECIMAL)
    }

    @Test
    fun iterableIsInOrderWithComparatorFailure() {
        assertFailsWith<AssertionError> {
            assertThat(listOf("1", "10", "2", "20")).isInOrder(COMPARE_AS_DECIMAL)
        }
    }

    @Test
    fun iterableOrderedByBaseClassComparator() {
        val comparator = compareBy<Foo> { it.x }
        val targetList: Iterable<Bar> = listOf(Bar(1), Bar(2), Bar(3))
        assertThat(targetList).isInOrder(comparator)
        assertThat(targetList).isInStrictOrder(comparator)
    }

    @Test
    fun isIn() {
        val actual = listOf("a")
        val expectedA = listOf("a")
        val expectedB = listOf("b")
        val expected = listOf(expectedA, expectedB)
        assertThat(actual).isIn(expected)
    }

    @Suppress("DEPRECATION") // Testing a deprecated method
    @Test
    fun isNotIn() {
        val actual = listOf("a")
        assertThat(actual).isNotIn(listOf(listOf("b"), listOf("c")))

        assertFailsWith<AssertionError> {
            assertThat(actual).isNotIn(listOf("a", "b"))
        }
    }

    @Test
    fun isAnyOf() {
        val actual = listOf("a")
        val expectedA = listOf("a")
        val expectedB = listOf("b")
        assertThat(actual).isAnyOf(expectedA, expectedB)
    }

    @Suppress("DEPRECATION") // Testing a deprecated method
    @Test
    fun isNoneOf() {
        val actual = listOf("a")
        assertThat(actual).isNoneOf(listOf("b"), listOf("c"))

        assertFailsWith<AssertionError> {
            assertThat(actual).isNoneOf("a", "b")
        }
    }

    private companion object {
        private val COMPARE_AS_DECIMAL: Comparator<String?> =
            Comparator { a, b -> a!!.toInt().compareTo(b!!.toInt()) }
    }

    private class CountsToStringCalls {
        var calls = 0

        override fun toString(): String {
            calls++
            return super.toString()
        }
    }

    private class HashCodeThrower {
        override fun equals(other: Any?): Boolean = this === other

        override fun hashCode(): Int {
            throw UnsupportedOperationException()
        }

        override fun toString(): String = "HCT"
    }

    private open class Foo(val x: Int)

    private class Bar(x: Int) : Foo(x)
}
