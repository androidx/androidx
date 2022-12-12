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

//    @Test
//    fun iterableContainsAtLeast() {
//        assertThat(listOf(1, 2, 3)).containsAtLeast(1, 2)
//    }
//
//    @Test
//    fun iterableContainsAtLeastWithMany() {
//        assertThat(listOf(1, 2, 3)).containsAtLeast(1, 2)
//    }
//
//    @Test
//    fun iterableContainsAtLeastWithDuplicates() {
//        assertThat(listOf(1, 2, 2, 2, 3)).containsAtLeast(2, 2)
//    }
//
//    @Test
//    fun iterableContainsAtLeastWithNull() {
//        assertThat(listOf(1, null, 3)).containsAtLeast(3, null as Int?)
//    }
//
//    @Test
//    fun iterableContainsAtLeastWithNullAtThirdAndFinalPosition() {
//        assertThat(listOf(1, null, 3)).containsAtLeast(1, 3, null as Any?)
//    }
//
//    /*
//   * Test that we only call toString() if the assertion fails -- that is, not just if the elements
//   * are out of order, but only if someone actually calls inOrder(). There are 2 reasons for this:
//   *
//   * 1. Calling toString() uses extra time and space. (To be fair, Iterable assertions often use a
//   * lot of those already.)
//   *
//   * 2. Some toString() methods are buggy. Arguably we shouldn't accommodate these, especially since
//   * those users are in for a nasty surprise if their tests actually fail someday, but I don't want
//   * to bite that off now. (Maybe Fact should catch exceptions from toString()?)
//   */
//    @Test
//    fun iterableContainsAtLeastElementsInOutOfOrderDoesNotStringify() {
//        val o = CountsToStringCalls()
//        val actual: List<Any> = listOf(o, 1)
//        val expected: List<Any> = listOf(1, o)
//        assertThat(actual).containsAtLeastElementsIn(expected)
//        assertThat(o.calls).isEqualTo(0)
//        expectFailureWhenTestingThat(actual).containsAtLeastElementsIn(expected).inOrder()
//        assertThat(o.calls).isGreaterThan(0)
//    }

//    @Test
//    fun iterableContainsAtLeastFailure() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsAtLeast(1, 2, 4)
//        assertFailureKeys("missing (1)", "---", "expected to contain at least", "but was")
//        assertFailureValue("missing (1)", "4")
//        assertFailureValue("expected to contain at least", "[1, 2, 4]")
//    }
//
//    @Test
//    fun iterableContainsAtLeastWithExtras() {
//        expectFailureWhenTestingThat(listOf("y", "x")).containsAtLeast("x", "y", "z")
//        assertFailureValue("missing (1)", "z")
//    }
//
//    @Test
//    fun iterableContainsAtLeastWithExtraCopiesOfOutOfOrder() {
//        expectFailureWhenTestingThat(listOf("y", "x")).containsAtLeast("x", "y", "y")
//        assertFailureValue("missing (1)", "y")
//    }
//
//    @Test
//    fun iterableContainsAtLeastWithDuplicatesFailure() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsAtLeast(1, 2, 2, 2, 3, 4)
//        assertFailureValue("missing (3)", "2 [2 copies], 4")
//    }
//
//    /*
//   * Slightly subtle test to ensure that if multiple equal elements are found
//   * to be missing we only reference it once in the output message.
//   */
//    @Test
//    fun iterableContainsAtLeastWithDuplicateMissingElements() {
//        expectFailureWhenTestingThat(listOf(1, 2)).containsAtLeast(4, 4, 4)
//        assertFailureValue("missing (3)", "4 [3 copies]")
//    }
//
//    @Test
//    fun iterableContainsAtLeastWithNullFailure() {
//        expectFailureWhenTestingThat(listOf(1, null, 3)).containsAtLeast(1, null, null, 3)
//        assertFailureValue("missing (1)", "null")
//    }
//
//    @Test
//    fun iterableContainsAtLeastFailsWithSameToStringAndHomogeneousList() {
//        expectFailureWhenTestingThat(listOf(1L, 2L)).containsAtLeast(1, 2)
//        assertFailureValue("missing (2)", "1, 2 (java.lang.Integer)")
//        assertFailureValue("though it did contain (2)", "1, 2 (java.lang.Long)")
//    }
//
//    @Test
//    fun iterableContainsAtLeastFailsWithSameToStringAndHomogeneousListWithDuplicates() {
//        expectFailureWhenTestingThat(listOf(1L, 2L, 2L)).containsAtLeast(1, 1, 2)
//        assertFailureValue("missing (3)", "1 [2 copies], 2 (java.lang.Integer)")
//        assertFailureValue("though it did contain (3)", "1, 2 [2 copies] (java.lang.Long)")
//    }
//
//    @Test
//    fun iterableContainsAtLeastFailsWithSameToStringAndHomogeneousListWithNull() {
//        expectFailureWhenTestingThat(listOf("null", "abc")).containsAtLeast("abc", null)
//        assertFailureValue("missing (1)", "null (null type)")
//        assertFailureValue("though it did contain (1)", "null (java.lang.String)")
//    }
//
//    @Test
//    fun iterableContainsAtLeastFailsWithSameToStringAndHeterogeneousListWithDuplicates() {
//        expectFailureWhenTestingThat(listOf(1, 2, 2L, 3L, 3L)).containsAtLeast(2L, 2L, 3, 3)
//        assertFailureValue("missing (3)", "2 (java.lang.Long), 3 (java.lang.Integer) [2 copies]")
//        assertFailureValue(
//            "though it did contain (3)", "2 (java.lang.Integer), 3 (java.lang.Long) [2 copies]"
//        )
//    }
//
//    @Test
//    fun iterableContainsAtLeastFailsWithEmptyString() {
//        expectFailureWhenTestingThat(listOf("a", null)).containsAtLeast("", null)
//        assertFailureKeys("missing (1)", "---", "expected to contain at least", "but was")
//        assertFailureValue("missing (1)", "")
//    }
//
//    @Test
//    fun iterableContainsAtLeastInOrder() {
//        assertThat(listOf(3, 2, 5)).containsAtLeast(3, 2, 5).inOrder()
//    }
//
//    @Test
//    fun iterableContainsAtLeastInOrderWithGaps() {
//        assertThat(listOf(3, 2, 5)).containsAtLeast(3, 5).inOrder()
//        assertThat(listOf(3, 2, 2, 4, 5)).containsAtLeast(3, 2, 2, 5).inOrder()
//        assertThat(listOf(3, 1, 4, 1, 5)).containsAtLeast(3, 1, 5).inOrder()
//        assertThat(listOf("x", "y", "y", "z")).containsAtLeast("x", "y", "z").inOrder()
//        assertThat(listOf("x", "x", "y", "z")).containsAtLeast("x", "y", "z").inOrder()
//        assertThat(listOf("z", "x", "y", "z")).containsAtLeast("x", "y", "z").inOrder()
//        assertThat(listOf("x", "x", "y", "z", "x")).containsAtLeast("x", "y", "z", "x").inOrder()
//    }
//
//    @Test
//    fun iterableContainsAtLeastInOrderWithNull() {
//        assertThat(listOf(3, null, 5)).containsAtLeast(3, null, 5).inOrder()
//        assertThat(listOf(3, null, 7, 5)).containsAtLeast(3, null, 5).inOrder()
//    }
//
//    @Test
//    fun iterableContainsAtLeastInOrderWithFailure() {
//        expectFailureWhenTestingThat(listOf(1, null, 3)).containsAtLeast(null, 1, 3).inOrder()
//        assertFailureKeys(
//            "required elements were all found, but order was wrong",
//            "expected order for required elements",
//            "but was"
//        )
//        assertFailureValue("expected order for required elements", "[null, 1, 3]")
//        assertFailureValue("but was", "[1, null, 3]")
//    }
//
//    @Test
//    fun iterableContainsAtLeastInOrderWithFailureWithActualOrder() {
//        expectFailureWhenTestingThat(listOf(1, 2, null, 3, 4)).containsAtLeast(null, 1, 3).inOrder()
//        assertFailureKeys(
//            "required elements were all found, but order was wrong",
//            "expected order for required elements",
//            "but order was",
//            "full contents"
//        )
//        assertFailureValue("expected order for required elements", "[null, 1, 3]")
//        assertFailureValue("but order was", "[1, null, 3]")
//        assertFailureValue("full contents", "[1, 2, null, 3, 4]")
//    }
//
//    @Test
//    fun iterableContainsAtLeastInOrderWithOneShotIterable() {
//        val iterable: Iterable<Any> =
//            Arrays.< Object > asList < kotlin . Any ? > 2, 1, null, 4, "a", 3, "b")
//        val iterator = iterable.iterator()
//        val oneShot: Iterable<Any> = object : Iterable<Any?> {
//            override fun iterator(): Iterator<Any> {
//                return iterator
//            }
//
//            override fun toString(): String {
//                return Iterables.toString(iterable)
//            }
//        }
//        assertThat(oneShot).containsAtLeast(1, null, 3).inOrder()
//    }
//
//    @Test
//    fun iterableContainsAtLeastInOrderWithOneShotIterableWrongOrder() {
//        val iterator: Iterator<Any> = listOf(2 as Any, 1, null, 4, "a", 3, "b").iterator()
//        val iterable: Iterable<Any> = object : Iterable<Any?> {
//            override fun iterator(): Iterator<Any> {
//                return iterator
//            }
//
//            override fun toString(): String {
//                return "BadIterable"
//            }
//        }
//        expectFailureWhenTestingThat(iterable).containsAtLeast(1, 3, null as Any?).inOrder()
//        assertFailureKeys(
//            "required elements were all found, but order was wrong",
//            "expected order for required elements",
//            "but was"
//        )
//        assertFailureValue("expected order for required elements", "[1, 3, null]")
//        assertFailureValue("but was", "BadIterable") // TODO(b/231966021): Output its elements.
//    }
//
//    @Test
//    fun iterableContainsAtLeastInOrderWrongOrderAndMissing() {
//        expectFailureWhenTestingThat(listOf(1, 2)).containsAtLeast(2, 1, 3).inOrder()
//    }
//
//    @Test
//    fun iterableContainsAtLeastElementsInIterable() {
//        assertThat(listOf(1, 2, 3)).containsAtLeastElementsIn(listOf(1, 2))
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsAtLeastElementsIn(listOf(1, 2, 4))
//        assertFailureKeys("missing (1)", "---", "expected to contain at least", "but was")
//        assertFailureValue("missing (1)", "4")
//        assertFailureValue("expected to contain at least", "[1, 2, 4]")
//    }
//
//    @Test
//    fun iterableContainsAtLeastElementsInCanUseFactPerElement() {
//        expectFailureWhenTestingThat(listOf("abc"))
//            .containsAtLeastElementsIn(listOf("123\n456", "789"))
//        assertFailureKeys(
//            "missing (2)",
//            "#1",
//            "#2",
//            "---",
//            "expected to contain at least",
//            "but was"
//        )
//        assertFailureValue("#1", "123\n456")
//        assertFailureValue("#2", "789")
//    }
//
//    @Test
//    fun iterableContainsAtLeastElementsInArray() {
//        assertThat(listOf(1, 2, 3)).containsAtLeastElementsIn(arrayOf(1, 2))
//        expectFailureWhenTestingThat(listOf(1, 2, 3))
//            .containsAtLeastElementsIn(arrayOf(1, 2, 4))
//        assertFailureKeys("missing (1)", "---", "expected to contain at least", "but was")
//        assertFailureValue("missing (1)", "4")
//        assertFailureValue("expected to contain at least", "[1, 2, 4]")
//    }
//
//    @Test
//    fun iterableContainsNoneOf() {
//        assertThat(listOf(1, 2, 3)).containsNoneOf(4, 5, 6)
//    }
//
//    @Test
//    fun iterableContainsNoneOfFailure() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsNoneOf(1, 2, 4)
//        assertFailureKeys("expected not to contain any of", "but contained", "full contents")
//        assertFailureValue("expected not to contain any of", "[1, 2, 4]")
//        assertFailureValue("but contained", "[1, 2]")
//        assertFailureValue("full contents", "[1, 2, 3]")
//    }
//
//    @Test
//    fun iterableContainsNoneOfFailureWithDuplicateInSubject() {
//        expectFailureWhenTestingThat(listOf(1, 2, 2, 3)).containsNoneOf(1, 2, 4)
//        assertFailureValue("but contained", "[1, 2]")
//    }
//
//    @Test
//    fun iterableContainsNoneOfFailureWithDuplicateInExpected() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsNoneOf(1, 2, 2, 4)
//        assertFailureValue("but contained", "[1, 2]")
//    }
//
//    @Test
//    fun iterableContainsNoneOfFailureWithEmptyString() {
//        expectFailureWhenTestingThat(listOf("")).containsNoneOf("", null)
//        assertFailureKeys("expected not to contain any of", "but contained", "full contents")
//        assertFailureValue("expected not to contain any of", "[\"\" (empty String), null]")
//        assertFailureValue("but contained", "[\"\" (empty String)]")
//        assertFailureValue("full contents", "[]")
//    }
//
//    @Test
//    fun iterableContainsNoneInIterable() {
//        assertThat(listOf(1, 2, 3)).containsNoneIn(listOf(4, 5, 6))
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsNoneIn(listOf(1, 2, 4))
//        assertFailureKeys("expected not to contain any of", "but contained", "full contents")
//        assertFailureValue("expected not to contain any of", "[1, 2, 4]")
//        assertFailureValue("but contained", "[1, 2]")
//        assertFailureValue("full contents", "[1, 2, 3]")
//    }
//
//    @Test
//    fun iterableContainsNoneInArray() {
//        assertThat(listOf(1, 2, 3)).containsNoneIn(arrayOf(4, 5, 6))
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsNoneIn(arrayOf(1, 2, 4))
//    }
//
//    @Test
//    fun iterableContainsExactlyArray() {
//        val stringArray = arrayOf("a", "b")
//        val iterable: ImmutableList<Array<String>> = listOf(stringArray)
//        // This test fails w/o the explicit cast
//        assertThat(iterable).containsExactly(stringArray as Any)
//    }
//
//    @Test
//    fun arrayContainsExactly() {
//        val iterable: ImmutableList<String> = listOf("a", "b")
//        val array = arrayOf("a", "b")
//        assertThat(iterable).containsExactly(array as Array<Any>)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithMany() {
//        assertThat(listOf(1, 2, 3)).containsExactly(1, 2, 3)
//    }
//
//    @Test
//    fun iterableContainsExactlyOutOfOrder() {
//        assertThat(listOf(1, 2, 3, 4)).containsExactly(3, 1, 4, 2)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithDuplicates() {
//        assertThat(listOf(1, 2, 2, 2, 3)).containsExactly(1, 2, 2, 2, 3)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithDuplicatesOutOfOrder() {
//        assertThat(listOf(1, 2, 2, 2, 3)).containsExactly(2, 1, 2, 3, 2)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithOnlyNullPassedAsNullArray() {
//        // Truth is tolerant of this erroneous varargs call.
//        val actual: Iterable<Any> = listOf(null as Any?)
//        assertThat(actual).containsExactly(null as Array<Any?>?)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithOnlyNull() {
//        val actual: Iterable<Any> = listOf(null as Any?)
//        assertThat(actual).containsExactly(null as Any?)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithNullSecond() {
//        assertThat(listOf(1, null)).containsExactly(1, null)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithNullThird() {
//        assertThat(listOf(1, 2, null)).containsExactly(1, 2, null)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithNull() {
//        assertThat(listOf(1, null, 3)).containsExactly(1, null, 3)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithNullOutOfOrder() {
//        assertThat(listOf(1, null, 3)).containsExactly(1, 3, null as Int?)
//    }
//
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

//    @Test
//    fun iterableContainsExactlyWithEmptyString() {
//        expectFailureWhenTestingThat(listOf()).containsExactly("")
//        assertFailureValue("missing (1)", "")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithEmptyStringAndUnexpectedItem() {
//        expectFailureWhenTestingThat(listOf("a", null)).containsExactly("")
//        assertFailureKeys("missing (1)", "unexpected (2)", "---", "expected", "but was")
//        assertFailureValue("missing (1)", "")
//        assertFailureValue("unexpected (2)", "a, null")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithEmptyStringAndMissingItem() {
//        expectFailureWhenTestingThat(listOf("")).containsExactly("a", null)
//        assertFailureValue("missing (2)", "a, null")
//        assertFailureValue("unexpected (1)", "")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithEmptyStringAmongMissingItems() {
//        expectFailureWhenTestingThat(listOf("a")).containsExactly("", "b")
//        assertFailureKeys(
//            "missing (2)", "#1", "#2", "", "unexpected (1)", "#1", "---", "expected", "but was"
//        )
//        assertFailureValueIndexed("#1", 0, "")
//        assertFailureValueIndexed("#2", 0, "b")
//        assertFailureValueIndexed("#1", 1, "a")
//    }
//
//    @Test
//    fun iterableContainsExactlySingleElement() {
//        assertThat(listOf(1)).containsExactly(1)
//        expectFailureWhenTestingThat(listOf(1)).containsExactly(2)
//        assertFailureKeys("value of", "expected", "but was")
//        assertFailureValue("value of", "iterable.onlyElement()")
//    }
//
//    @Test
//    fun iterableContainsExactlySingleElementNoEqualsMagic() {
//        expectFailureWhenTestingThat(listOf(1)).containsExactly(1L)
//        assertFailureValueIndexed("an instance of", 0, "java.lang.Long")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithElementsThatThrowWhenYouCallHashCode() {
//        val one = HashCodeThrower()
//        val two = HashCodeThrower()
//        assertThat(listOf(one, two)).containsExactly(two, one)
//        assertThat(listOf(one, two)).containsExactly(one, two).inOrder()
//        assertThat(listOf(one, two)).containsExactlyElementsIn(listOf(two, one))
//        assertThat(listOf(one, two)).containsExactlyElementsIn(listOf(one, two)).inOrder()
//    }
//
//    @Test
//    fun iterableContainsExactlyWithElementsThatThrowWhenYouCallHashCodeFailureTooMany() {
//        val one = HashCodeThrower()
//        val two = HashCodeThrower()
//        expectFailureWhenTestingThat(listOf(one, two)).containsExactly(one)
//    }
//
//    @Test
//    fun iterableContainsExactlyWithElementsThatThrowWhenYouCallHashCodeOneMismatch() {
//        val one = HashCodeThrower()
//        val two = HashCodeThrower()
//        expectFailureWhenTestingThat(listOf(one, one)).containsExactly(one, two)
//    }
//
//    private class HashCodeThrower() {
//        override fun equals(other: Any?): Boolean {
//            return this === other
//        }
//
//        override fun hashCode(): Int {
//            throw java.lang.UnsupportedOperationException()
//        }
//
//        override fun toString(): String {
//            return "HCT"
//        }
//    }
//
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

//    @Test
//    fun iterableContainsExactlyMissingItemFailure() {
//        expectFailureWhenTestingThat(listOf(1, 2)).containsExactly(1, 2, 4)
//        assertFailureValue("missing (1)", "4")
//    }

//    @Test
//    fun iterableContainsExactlyUnexpectedItemFailure() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsExactly(1, 2)
//        assertFailureValue("unexpected (1)", "3")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithDuplicatesNotEnoughItemsFailure() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsExactly(1, 2, 2, 2, 3)
//        assertFailureValue("missing (2)", "2 [2 copies]")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithDuplicatesMissingItemFailure() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsExactly(1, 2, 2, 2, 3, 4)
//        assertFailureValue("missing (3)", "2 [2 copies], 4")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithDuplicatesMissingItemsWithNewlineFailure() {
//        expectFailureWhenTestingThat(listOf("a", "b", "foo\nbar"))
//            .containsExactly("a", "b", "foo\nbar", "foo\nbar", "foo\nbar")
//        assertFailureKeys("missing (2)", "#1 [2 copies]", "---", "expected", "but was")
//        assertFailureValue("#1 [2 copies]", "foo\nbar")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithDuplicatesMissingAndExtraItemsWithNewlineFailure() {
//        expectFailureWhenTestingThat(listOf("a\nb", "a\nb")).containsExactly("foo\nbar", "foo\nbar")
//        assertFailureKeys(
//            "missing (2)",
//            "#1 [2 copies]",
//            "",
//            "unexpected (2)",
//            "#1 [2 copies]",
//            "---",
//            "expected",
//            "but was"
//        )
//        assertFailureValueIndexed("#1 [2 copies]", 0, "foo\nbar")
//        assertFailureValueIndexed("#1 [2 copies]", 1, "a\nb")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithDuplicatesUnexpectedItemFailure() {
//        expectFailureWhenTestingThat(listOf(1, 2, 2, 2, 2, 3)).containsExactly(1, 2, 2, 3)
//        assertFailureValue("unexpected (2)", "2 [2 copies]")
//    }
//
//    /*
//   * Slightly subtle test to ensure that if multiple equal elements are found
//   * to be missing we only reference it once in the output message.
//   */
//    @Test
//    fun iterableContainsExactlyWithDuplicateMissingElements() {
//        expectFailureWhenTestingThat(listOf()).containsExactly(4, 4, 4)
//        assertFailureValue("missing (3)", "4 [3 copies]")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithNullFailure() {
//        expectFailureWhenTestingThat(listOf(1, null, 3)).containsExactly(1, null, null, 3)
//        assertFailureValue("missing (1)", "null")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithMissingAndExtraElements() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3)).containsExactly(1, 2, 4)
//        assertFailureValue("missing (1)", "4")
//        assertFailureValue("unexpected (1)", "3")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithDuplicateMissingAndExtraElements() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3, 3)).containsExactly(1, 2, 4, 4)
//        assertFailureValue("missing (2)", "4 [2 copies]")
//        assertFailureValue("unexpected (2)", "3 [2 copies]")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithCommaSeparatedVsIndividual() {
//        expectFailureWhenTestingThat(listOf("a, b")).containsExactly("a", "b")
//        assertFailureKeys(
//            "missing (2)", "#1", "#2", "", "unexpected (1)", "#1", "---", "expected", "but was"
//        )
//        assertFailureValueIndexed("#1", 0, "a")
//        assertFailureValueIndexed("#2", 0, "b")
//        assertFailureValueIndexed("#1", 1, "a, b")
//    }
//
//    @Test
//    fun iterableContainsExactlyFailsWithSameToStringAndHomogeneousList() {
//        expectFailureWhenTestingThat(listOf(1L, 2L)).containsExactly(1, 2)
//        assertFailureValue("missing (2)", "1, 2 (java.lang.Integer)")
//        assertFailureValue("unexpected (2)", "1, 2 (java.lang.Long)")
//    }
//
//    @Test
//    fun iterableContainsExactlyFailsWithSameToStringAndListWithNull() {
//        expectFailureWhenTestingThat(listOf(1L, 2L)).containsExactly(null, 1, 2)
//        assertFailureValue(
//            "missing (3)", "null (null type), 1 (java.lang.Integer), 2 (java.lang.Integer)"
//        )
//        assertFailureValue("unexpected (2)", "1, 2 (java.lang.Long)")
//    }
//
//    @Test
//    fun iterableContainsExactlyFailsWithSameToStringAndHeterogeneousList() {
//        expectFailureWhenTestingThat(listOf(1L, 2)).containsExactly(1, null, 2L)
//        assertFailureValue(
//            "missing (3)", "1 (java.lang.Integer), null (null type), 2 (java.lang.Long)"
//        )
//        assertFailureValue("unexpected (2)", "1 (java.lang.Long), 2 (java.lang.Integer)")
//    }
//
//    @Test
//    fun iterableContainsExactlyFailsWithSameToStringAndHomogeneousListWithDuplicates() {
//        expectFailureWhenTestingThat(listOf(1L, 2L)).containsExactly(1, 2, 2)
//        assertFailureValue("missing (3)", "1, 2 [2 copies] (java.lang.Integer)")
//        assertFailureValue("unexpected (2)", "1, 2 (java.lang.Long)")
//    }
//
//    @Test
//    fun iterableContainsExactlyFailsWithSameToStringAndHeterogeneousListWithDuplicates() {
//        expectFailureWhenTestingThat(listOf(1L, 2)).containsExactly(1, null, null, 2L, 2L)
//        assertFailureValue(
//            "missing (5)",
//            "1 (java.lang.Integer), null (null type) [2 copies], 2 (java.lang.Long) [2 copies]"
//        )
//        assertFailureValue("unexpected (2)", "1 (java.lang.Long), 2 (java.lang.Integer)")
//    }
//
//    @Test
//    fun iterableContainsExactlyWithOneIterableGivesWarning() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3, 4)).containsExactly(listOf(1, 2, 3, 4))
//        assertThat(expectFailure.getFailure())
//            .hasMessageThat()
//            .contains(CONTAINS_EXACTLY_ITERABLE_WARNING)
//    }
//
    @Test
    fun iterableContainsExactlyElementsInWithOneIterableDoesNotGiveWarning() {
        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3, 4)).containsExactlyElementsIn(listOf(1, 2, 3))
        }
    }

//    @Test
//    fun iterableContainsExactlyWithTwoIterableDoesNotGivesWarning() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3, 4)).containsExactly(listOf(1, 2), listOf(3, 4))
//        assertThat(expectFailure.getFailure())
//            .hasMessageThat()
//            .doesNotContain(CONTAINS_EXACTLY_ITERABLE_WARNING)
//    }
//
//    private val CONTAINS_EXACTLY_ITERABLE_WARNING =
//        ("Passing an iterable to the varargs method containsExactly(Object...) is "
//            + "often not the correct thing to do. Did you mean to call "
//            + "containsExactlyElementsIn(Iterable) instead?")
//
//    @Test
//    fun iterableContainsExactlyWithOneNonIterableDoesNotGiveWarning() {
//        expectFailureWhenTestingThat(listOf(1, 2, 3, 4)).containsExactly(1)
//        assertFailureValue("unexpected (3)", "2, 3, 4")
//    }
//
//    @Test
//    fun iterableContainsExactlyInOrder() {
//        assertThat(listOf(3, 2, 5)).containsExactly(3, 2, 5).inOrder()
//    }
//
//    @Test
//    fun iterableContainsExactlyInOrderWithNull() {
//        assertThat(listOf(3, null, 5)).containsExactly(3, null, 5).inOrder()
//    }
//
//    @Test
//    fun iterableContainsExactlyInOrderWithFailure() {
//        expectFailureWhenTestingThat(listOf(1, null, 3)).containsExactly(null, 1, 3).inOrder()
//        assertFailureKeys("contents match, but order was wrong", "expected", "but was")
//        assertFailureValue("expected", "[null, 1, 3]")
//    }
//
//    @Test
//    fun iterableContainsExactlyInOrderWithOneShotIterable() {
//        val iterator: Iterator<Any> = listOf(1 as Any, null, 3).iterator()
//        val iterable: Iterable<Any> = object : Iterable<Any?> {
//            override fun iterator(): Iterator<Any> {
//                return iterator
//            }
//        }
//        assertThat(iterable).containsExactly(1, null, 3).inOrder()
//    }
//
//    @Test
//    fun iterableContainsExactlyInOrderWithOneShotIterableWrongOrder() {
//        val iterator: Iterator<Any> = listOf(1 as Any, null, 3).iterator()
//        val iterable: Iterable<Any> = object : Iterable<Any?> {
//            override fun iterator(): Iterator<Any> {
//                return iterator
//            }
//
//            override fun toString(): String {
//                return "BadIterable"
//            }
//        }
//        expectFailureWhenTestingThat(iterable).containsExactly(1, 3, null).inOrder()
//        assertFailureKeys("contents match, but order was wrong", "expected", "but was")
//        assertFailureValue("expected", "[1, 3, null]")
//    }
//
//    @Test
//    fun iterableWithNoToStringOverride() {
//        val iterable: Iterable<Int> = object : Iterable<Int?> {
//            override fun iterator(): Iterator<Int> {
//                return Iterators.forArray(1, 2, 3)
//            }
//        }
//        expectFailureWhenTestingThat(iterable).containsExactly(1, 2).inOrder()
//        assertFailureValue("but was", "[1, 2, 3]")
//    }
//
    @Test
    fun iterableContainsExactlyElementsInIterable() {
        assertThat(listOf(1, 2)).containsExactlyElementsIn(listOf(1, 2))

        assertFailsWith<AssertionError> {
            assertThat(listOf(1, 2, 3, 4)).containsExactlyElementsIn(listOf(1, 2, 3))
        }
    }

//    @Test
//    fun iterableContainsExactlyElementsInArray() {
//        assertThat(listOf(1, 2)).containsExactlyElementsIn(arrayOf(1, 2))
//
//        assertFailsWith<AssertionError> {
//            assertThat(listOf(1, 2)).containsExactlyElementsIn(arrayOf<Int?>(1, 2, 4))
//        }
//    }

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

//    @Test
//    fun iterableIsInStrictOrder() {
//        assertThat(emptyList<Any>()).isInStrictOrder()
//        assertThat(listOf(1)).isInStrictOrder()
//        assertThat(listOf(1, 2, 3, 4)).isInStrictOrder()
//    }

//    @Test
//    fun isInStrictOrderFailure() {
//        expectFailureWhenTestingThat(listOf(1, 2, 2, 4)).isInStrictOrder()
//        assertFailureKeys(
//            "expected to be in strict order", "but contained", "followed by", "full contents"
//        )
//        assertFailureValue("but contained", "2")
//        assertFailureValue("followed by", "2")
//        assertFailureValue("full contents", "[1, 2, 2, 4]")
//    }
//
//    @Test
//    fun isInStrictOrderWithNonComparableElementsFailure() {
//        try {
//            assertThat(listOf(1 as Any, "2", 3, "4")).isInStrictOrder()
//            fail("Should have thrown.")
//        } catch (expected: java.lang.ClassCastException) {
//        }
//    }
//
//    @Test
//    fun iterableIsInOrder() {
//        assertThat(listOf()).isInOrder()
//        assertThat(listOf(1)).isInOrder()
//        assertThat(listOf(1, 1, 2, 3, 3, 3, 4)).isInOrder()
//    }
//
//    @Test
//    fun isInOrderFailure() {
//        expectFailureWhenTestingThat(listOf(1, 3, 2, 4)).isInOrder()
//        assertFailureKeys(
//            "expected to be in order",
//            "but contained",
//            "followed by",
//            "full contents"
//        )
//        assertFailureValue("but contained", "3")
//        assertFailureValue("followed by", "2")
//        assertFailureValue("full contents", "[1, 3, 2, 4]")
//    }
//
//    @Test
//    fun isInOrderMultipleFailures() {
//        expectFailureWhenTestingThat(listOf(1, 3, 2, 4, 0)).isInOrder()
//    }
//
//    @Test
//    fun isInOrderWithNonComparableElementsFailure() {
//        try {
//            assertThat(listOf(1 as Any, "2", 2, "3")).isInOrder()
//            fail("Should have thrown.")
//        } catch (expected: java.lang.ClassCastException) {
//        }
//    }
//
//    @Test
//    fun iterableIsInStrictOrderWithComparator() {
//        val emptyStrings: Iterable<String> = listOf()
//        assertThat(emptyStrings).isInStrictOrder(COMPARE_AS_DECIMAL)
//        assertThat(listOf("1")).isInStrictOrder(COMPARE_AS_DECIMAL)
//        // Note: Use "10" and "20" to distinguish numerical and lexicographical ordering.
//        assertThat(listOf("1", "2", "10", "20")).isInStrictOrder(COMPARE_AS_DECIMAL)
//    }
//
//    @Test
//    fun iterableIsInStrictOrderWithComparatorFailure() {
//        expectFailureWhenTestingThat(
//            listOf(
//                "1",
//                "2",
//                "2",
//                "10"
//            )
//        ).isInStrictOrder(COMPARE_AS_DECIMAL)
//        assertFailureKeys(
//            "expected to be in strict order", "but contained", "followed by", "full contents"
//        )
//        assertFailureValue("but contained", "2")
//        assertFailureValue("followed by", "2")
//        assertFailureValue("full contents", "[1, 2, 2, 10]")
//    }
//
//    @Test
//    fun iterableIsInOrderWithComparator() {
//        val emptyStrings: Iterable<String> = listOf()
//        assertThat(emptyStrings).isInOrder(COMPARE_AS_DECIMAL)
//        assertThat(listOf("1")).isInOrder(COMPARE_AS_DECIMAL)
//        assertThat(listOf("1", "1", "2", "10", "10", "10", "20")).isInOrder(COMPARE_AS_DECIMAL)
//    }
//
//    @Test
//    fun iterableIsInOrderWithComparatorFailure() {
//        expectFailureWhenTestingThat(listOf("1", "10", "2", "20")).isInOrder(COMPARE_AS_DECIMAL)
//        assertFailureKeys(
//            "expected to be in order",
//            "but contained",
//            "followed by",
//            "full contents"
//        )
//        assertFailureValue("but contained", "10")
//        assertFailureValue("followed by", "2")
//        assertFailureValue("full contents", "[1, 10, 2, 20]")
//    }
//
//    private val COMPARE_AS_DECIMAL: Comparator<String> =
//        Comparator<String?> { a, b ->
//            java.lang.Integer.valueOf(a).compareTo(java.lang.Integer.valueOf(b))
//        }
//
//    private class Foo private constructor(val x: Int)
//
//    private class Bar(x: Int) : Foo(x)
//
//    private val FOO_COMPARATOR: Comparator<Foo> = object : Comparator<Foo?>() {
//        fun compare(a: Foo, b: Foo): Int {
//            return if (a.x < b.x) -1 else if (a.x > b.x) 1 else 0
//        }
//    }
//
//    @Test
//    fun iterableOrderedByBaseClassComparator() {
//        val targetList: Iterable<Bar> = listOf(Bar(1), Bar(2), Bar(3))
//        assertThat(targetList).isInOrder(FOO_COMPARATOR)
//        assertThat(targetList).isInStrictOrder(FOO_COMPARATOR)
//    }
//
//    @Test
//    fun isIn() {
//        val actual: ImmutableList<String> = listOf("a")
//        val expectedA: ImmutableList<String> = listOf("a")
//        val expectedB: ImmutableList<String> = listOf("b")
//        val expected: ImmutableList<ImmutableList<String>> = listOf(expectedA, expectedB)
//        assertThat(actual).isIn(expected)
//    }
//
//    @Test
//    fun isNotIn() {
//        val actual: ImmutableList<String> = listOf("a")
//        assertThat(actual).isNotIn(listOf(listOf("b"), listOf("c")))
//        expectFailureWhenTestingThat(actual).isNotIn(listOf("a", "b"))
//        assertThat(expectFailure.getFailure())
//            .hasMessageThat()
//            .isEqualTo(
//                "The actual value is an Iterable, and you've written a test that compares it to some "
//                    + "objects that are not Iterables. Did you instead mean to check whether its "
//                    + "*contents* match any of the *contents* of the given values? If so, call "
//                    + "containsNoneOf(...)/containsNoneIn(...) instead. Non-iterables: [a, b]"
//            )
//    }
//
//    @Test
//    fun isAnyOf() {
//        val actual: ImmutableList<String> = listOf("a")
//        val expectedA: ImmutableList<String> = listOf("a")
//        val expectedB: ImmutableList<String> = listOf("b")
//        assertThat(actual).isAnyOf(expectedA, expectedB)
//    }
//
//    @Test
//    fun isNoneOf() {
//        val actual: ImmutableList<String> = listOf("a")
//        assertThat(actual).isNoneOf(listOf("b"), listOf("c"))
//        expectFailureWhenTestingThat(actual).isNoneOf("a", "b")
//        assertThat(expectFailure.getFailure())
//            .hasMessageThat()
//            .isEqualTo(
//                ("The actual value is an Iterable, and you've written a test that compares it to some "
//                    + "objects that are not Iterables. Did you instead mean to check whether its "
//                    + "*contents* match any of the *contents* of the given values? If so, call "
//                    + "containsNoneOf(...)/containsNoneIn(...) instead. Non-iterables: [a, b]")
//            )
//    }
//
    private class CountsToStringCalls {
        var calls = 0

        override fun toString(): String {
            calls++
            return super.toString()
        }
    }
}
