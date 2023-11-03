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

import androidx.kruth.Fact.Companion.simpleFact
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * A mirror of existing tests in official Truth for parity. For additional tests in Kruth, see
 * [SubjectKruthTest].
 *
 * Partially migrated from Truth's source in Github:
 * https://github.com/google/truth/blob/master/core/src/test/java/com/google/common/truth/SubjectTest.java
 *
 * Note: This does not include assertions against failure messages, as Kruth does not currently
 * implement the same fact system Truth has yet.
 *
 * @see SubjectKruthTest for additional tests on top of these migrated ones for Kruth.
 */
class SubjectTest {

    @Test
    fun isNull() {
        val o: Any? = null
        assertThat(o).isNull()
    }

    @Test
    fun isNullFail() {
        val o = Any()
        assertFailsWith<AssertionError> {
            assertThat(o).isNull()
        }
    }

    @Test
    fun isNullWhenSubjectForbidsIsEqualTo() {
        ForbidsEqualityChecksSubject(null).isNull()
    }

    @Test
    fun isNullWhenSubjectForbidsIsEqualToFail() {
        assertFailsWith<AssertionError> {
            ForbidsEqualityChecksSubject(Any()).isNull()
        }
    }

    @Test
    fun stringIsNullFail() {
        assertFailsWith<AssertionError> {
            assertThat("foo").isNull()
        }
    }

    @Test
    fun isNullBadEqualsImplementation() {
        assertFailsWith<AssertionError> {
            assertThat(ThrowsOnEqualsNull()).isNull()
        }
    }

    @Test
    fun isNotNull() {
        val o = Any()
        assertThat(o).isNotNull()
    }

    @Test
    fun isNotNullFail() {
        val o: Any? = null
        assertFailsWith<AssertionError> {
            assertThat(o).isNotNull()
        }
    }

    @Test
    fun isNotNullBadEqualsImplementation() {
        assertThat(ThrowsOnEqualsNull()).isNotNull()
    }

    @Test
    fun isNotNullWhenSubjectForbidsIsEqualTo() {
        ForbidsEqualityChecksSubject(Any()).isNotNull()
    }

    @Test
    fun isNotNullWhenSubjectForbidsIsEqualToFail() {
        assertFailsWith<AssertionError> {
            ForbidsEqualityChecksSubject(null).isNotNull()
        }
    }

    @Test
    fun toStringsAreIdentical() {
        class IntWrapper(val wrapped: Int) {
            override fun toString(): String = wrapped.toString()
        }

        val wrapper = IntWrapper(5)
        assertFailsWith<AssertionError> {
            assertThat(5).isEqualTo(wrapper)
        }
    }

    @Test
    fun isEqualToWithNulls() {
        val o: Any? = null
        assertThat(o).isEqualTo(null)
    }

    @Test
    fun isEqualToFailureWithNulls() {
        val o: Any? = null
        assertFailsWith<AssertionError> {
            assertThat(o).isEqualTo("a")
        }
    }

    @Test
    fun isEqualToStringWithNullVsNull() {
        assertFailsWith<AssertionError> {
            assertThat("null").isEqualTo(null)
        }
    }

    @Test
    fun isEqualToWithSameObject() {
        val a: Any = Any()
        val b: Any = a
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun isEqualToFailureWithObjects() {
        val a: Any = Any()
        val b: Any = Any()
        assertFailsWith<AssertionError> {
            assertThat(a).isEqualTo(b)
        }
    }

    @Test
    fun isEqualToFailureWithDifferentTypesAndSameToString() {
        val a: Any = "true"
        val b: Any = true
        assertFailsWith<AssertionError> {
            assertThat(a).isEqualTo(b)
        }
    }

    @Test
    fun isEqualToNullBadEqualsImplementation() {
        assertFailsWith<AssertionError> {
            assertThat(ThrowsOnEqualsNull()).isEqualTo(null)
        }
    }

    @Test
    fun isEqualToSameInstanceBadEqualsImplementation() {
        val o: Any = ThrowsOnEquals()
        assertThat(o).isEqualTo(o)
    }

    @Test
    fun isEqualToNestedArrays() {
        fun getArray(): Array<*> =
            arrayOf(
                intArrayOf(1, 2, 3),
                arrayOf(
                    intArrayOf(1, 2, 3),
                    arrayOf("a", null, "b"),
                ),
                listOf(1, 2, 3),
                "a",
            )

        assertThat(getArray()).isEqualTo(getArray())
    }

    @Test
    fun isEqualToNestedArraysFailsNotEqual() {
        fun getArray(arg: Int): Array<*> =
            arrayOf(
                intArrayOf(1, 2, 3),
                arrayOf(
                    intArrayOf(1, arg, 3),
                    arrayOf("a", null, "b"),
                ),
                listOf(1, 2, 3),
                "a",
            )

        assertFailsWith<AssertionError> {
            assertThat(getArray(arg = 10)).isEqualTo(getArray(arg = 20))
        }
    }

    @Test
    fun isEqualToByteArray() {
        assertThat(byteArrayOf(0, 1, 2)).isEqualTo(byteArrayOf(0, 1, 2))
    }

    @Test
    fun isEqualToByteArrayEmpty() {
        assertThat(byteArrayOf()).isEqualTo(byteArrayOf())
    }

    @Test
    fun isEqualToByteArrayFailsNotEqual() {
        assertFailsWith<AssertionError> {
            assertThat(byteArrayOf(0, 1, 2)).isEqualTo(byteArrayOf(0, 1, 3))
        }
    }

    @Test
    fun isEqualToIntArray() {
        assertThat(intArrayOf(0, 1, 2)).isEqualTo(intArrayOf(0, 1, 2))
    }

    @Test
    fun isEqualToIntArrayEmpty() {
        assertThat(intArrayOf()).isEqualTo(intArrayOf())
    }

    @Test
    fun isEqualToIntArrayFailsNotEqual() {
        assertFailsWith<AssertionError> {
            assertThat(intArrayOf(0, 1, 2)).isEqualTo(intArrayOf(0, 1, 3))
        }
    }

    @Test
    fun isEqualToLongArray() {
        assertThat(longArrayOf(0, 1, 2)).isEqualTo(longArrayOf(0, 1, 2))
    }

    @Test
    fun isEqualToLongArrayEmpty() {
        assertThat(longArrayOf()).isEqualTo(longArrayOf())
    }

    @Test
    fun isEqualToLongArrayFailsNotEqual() {
        assertFailsWith<AssertionError> {
            assertThat(longArrayOf(0, 1, 2)).isEqualTo(longArrayOf(0, 1, 3))
        }
    }

    @Test
    fun isEqualToFloatArray() {
        assertThat(floatArrayOf(0F, 1F, 2F)).isEqualTo(floatArrayOf(0F, 1F, 2F))
    }

    @Test
    fun isEqualToFloatArrayEmpty() {
        assertThat(floatArrayOf()).isEqualTo(floatArrayOf())
    }

    @Test
    fun isEqualToFloatArrayEmptyNotEqual() {
        assertFailsWith<AssertionError> {
            assertThat(floatArrayOf(0F, 1F, 2F)).isEqualTo(floatArrayOf(0F, 1F, 3F))
        }
    }

    @Test
    fun isEqualToDoubleArray() {
        assertThat(doubleArrayOf(0.0, 1.0, 2.0)).isEqualTo(doubleArrayOf(0.0, 1.0, 2.0))
    }

    @Test
    fun isEqualToDoubleArrayEmpty() {
        assertThat(doubleArrayOf()).isEqualTo(doubleArrayOf())
    }

    @Test
    fun isEqualToDoubleArrayFailsNotEqual() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(0.0, 1.0, 2.0)).isEqualTo(doubleArrayOf(0.0, 1.0, 3.0))
        }
    }

    @Test
    fun isEqualToShortArray() {
        assertThat(shortArrayOf(0, 1, 2)).isEqualTo(shortArrayOf(0, 1, 2))
    }

    @Test
    fun isEqualToShortArrayEmpty() {
        assertThat(shortArrayOf()).isEqualTo(shortArrayOf())
    }

    @Test
    fun isEqualToShortArrayFailsNotEqual() {
        assertFailsWith<AssertionError> {
            assertThat(shortArrayOf(0, 1, 2)).isEqualTo(shortArrayOf(0, 1, 3))
        }
    }

    @Test
    fun isEqualToCharArray() {
        assertThat(charArrayOf('a', 'b', 'c')).isEqualTo(charArrayOf('a', 'b', 'c'))
    }

    @Test
    fun isEqualToCharArrayEmpty() {
        assertThat(charArrayOf()).isEqualTo(charArrayOf())
    }

    @Test
    fun isEqualToCharArrayFailsNotEqual() {
        assertFailsWith<AssertionError> {
            assertThat(charArrayOf('a', 'b', 'c')).isEqualTo(charArrayOf('a', 'b', 'd'))
        }
    }

    @Test
    fun isNotEqualToWithNulls() {
        val o: Any? = null
        assertThat(o).isNotEqualTo("a")
    }

    @Test
    fun isNotEqualToFailureWithNulls() {
        val o: Any? = null
        assertFailsWith<AssertionError> {
            assertThat(o).isNotEqualTo(null)
        }
    }

    @Test
    fun isNotEqualToWithObjects() {
        val a = Any()
        val b = Any()
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun isNotEqualToFailureWithObjects() {
        val o: Any = 1000
        assertFailsWith<AssertionError> {
            assertThat(o).isNotEqualTo(1000)
        }
    }

    @Test
    fun isNotEqualToFailureWithSameObject() {
        assertFailsWith<AssertionError> {
            assertThat(object1).isNotEqualTo(object1)
        }
    }

    @Test
    fun isNotEqualToWithDifferentTypesAndSameToString() {
        val a: Any = "true"
        val b: Any = true
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun isNotEqualToNullBadEqualsImplementation() {
        assertThat(ThrowsOnEqualsNull()).isNotEqualTo(null)
    }

    @Test
    fun isNotEqualToSameInstanceBadEqualsImplementation() {
        val o: Any = ThrowsOnEquals()
        assertFailsWith<AssertionError> {
            assertThat(o).isNotEqualTo(o)
        }
    }

    @Test
    fun isSameInstanceAsWithNulls() {
        val o: Any? = null
        assertThat(o).isSameInstanceAs(null)
    }

    @Test
    fun isSameInstanceAsFailureWithNulls() {
        val o: Any? = null
        assertFailsWith<AssertionError> {
            assertThat(o).isSameInstanceAs("a")
        }
    }

    @Test
    fun isSameInstanceAsWithSameObject() {
        val a = Any()
        assertThat(a).isSameInstanceAs(a)
    }

    @Test
    fun isSameInstanceAsFailureWithObjects() {
        assertFailsWith<AssertionError> {
            assertThat(object1).isSameInstanceAs(object2)
        }
    }

    @Test
    fun isSameInstanceAsFailureWithComparableObjects_nonString() {
        class AlwaysEqual : Comparable<AlwaysEqual> {
            override fun compareTo(other: AlwaysEqual): Int = 0
        }

        assertFailsWith<AssertionError> {
            assertThat(AlwaysEqual()).isSameInstanceAs(AlwaysEqual())
        }
    }

    @Test
    fun isSameInstanceAsFailureWithComparableObjects() {
        val a: Any = "ab"
        val b: Any = buildString { append("ab") }
        assertFailsWith<AssertionError> {
            assertThat(a).isSameInstanceAs(b)
        }
    }

    @Test
    fun isSameInstanceAsFailureWithDifferentTypesAndSameToString() {
        val a: Any = "true"
        val b: Any = true
        assertFailsWith<AssertionError> {
            assertThat(a).isSameInstanceAs(b)
        }
    }

    @Test
    fun isNotSameInstanceAsWithNulls() {
        val o: Any? = null
        assertThat(o).isNotSameInstanceAs("a")
    }

    @Test
    fun isNotSameInstanceAsFailureWithNulls() {
        val o: Any? = null
        assertFailsWith<AssertionError> {
            assertThat(o).isNotSameInstanceAs(null)
        }
    }

    @Test
    fun isNotSameInstanceAsWithObjects() {
        val a = Any()
        val b = Any()
        assertThat(a).isNotSameInstanceAs(b)
    }

    @Test
    fun isNotSameInstanceAsFailureWithSameObject() {
        assertFailsWith<AssertionError> {
            assertThat(object1).isNotSameInstanceAs(object1)
        }
    }

    @Test
    fun isNotSameInstanceAsWithComparableObjects_nonString() {
        class AlwaysEqual : Comparable<AlwaysEqual> {
            override fun compareTo(other: AlwaysEqual): Int = 0
        }

        assertThat(AlwaysEqual()).isNotSameInstanceAs(AlwaysEqual())
    }

    @Test
    fun isNotSameInstanceAsWithComparableObjects() {
        val a: Any = "ab"
        val b: Any = buildString { append("ab") }
        assertThat(a).isNotSameInstanceAs(b)
    }

    @Test
    fun isNotSameInstanceAsWithDifferentTypesAndSameToString() {
        val a: Any = "true"
        val b: Any = true
        assertThat(a).isNotSameInstanceAs(b)
    }

    @Test
    fun isInstanceOfExactType() {
        assertThat("a").isInstanceOf<String>()
    }

    @Test
    fun isInstanceOfSuperclass() {
        assertThat(3).isInstanceOf<Number>()
    }

    @Test
    fun isInstanceOfImplementedInterface() {
        assertThat("a").isInstanceOf<CharSequence>()
    }

    @Test
    fun isInstanceOfUnrelatedClass() {
        assertFailsWith<AssertionError> {
            assertThat(4.5).isInstanceOf<Long>()
        }
    }

    @Test
    fun isInstanceOfUnrelatedInterface() {
        assertFailsWith<AssertionError> {
            assertThat(4.5).isInstanceOf<CharSequence>()
        }
    }

    @Test
    fun isInstanceOfClassForNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as Any?).isInstanceOf<Long>()
        }
    }

    @Test
    fun isInstanceOfInterfaceForNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as Any?).isInstanceOf<CharSequence>()
        }
    }

    @Test
    fun isNotInstanceOfExactType() {
        assertFailsWith<AssertionError> {
            assertThat(5).isNotInstanceOf<Int>()
        }
    }

    @Test
    fun isNotInstanceOfSuperclass() {
        assertFailsWith<AssertionError> {
            assertThat(5).isNotInstanceOf<Number>()
        }
    }

    @Test
    fun isNotInstanceOfImplementedInterface() {
        assertFailsWith<AssertionError> {
            assertThat("a").isNotInstanceOf<CharSequence>()
        }
    }

    @Test
    fun isNotInstanceOfPrimitiveType() {
        assertFailsWith<AssertionError> {
            assertThat(1).isNotInstanceOf<Int>()
        }
    }

    @Test
    fun disambiguationWithSameToString() {
        assertFailsWith<AssertionError> {
            assertThat(StringBuilder("foo")).isEqualTo(StringBuilder("foo"))
        }
    }

    @Test
    fun isIn() {
        assertThat("b").isIn(oneShotIterable("a", "b", "c"))
    }

    @Test
    fun isInJustTwo() {
        assertThat("b").isIn(oneShotIterable("a", "b"))
    }

    @Test
    fun isInFailure() {
        assertFailsWith<AssertionError> {
            assertThat("x").isIn(oneShotIterable("a", "b", "c"))
        }
    }

    @Test
    fun isInNullInListWithNull() {
        assertThat(null as String?).isIn(oneShotIterable("a", "b", null as String?))
    }

    @Test
    fun isInNonnullInListWithNull() {
        assertThat("b").isIn(oneShotIterable("a", "b", null as String?))
    }

    @Test
    fun isInNullFailure() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).isIn(oneShotIterable("a", "b", "c"))
        }
    }

    @Test
    fun isInEmptyFailure() {
        assertFailsWith<AssertionError> {
            assertThat("b").isIn(emptyList<String>())
        }
    }

    @Test
    fun isAnyOf() {
        assertThat("b").isAnyOf("a", "b", "c")
    }

    @Test
    fun isAnyOfJustTwo() {
        assertThat("b").isAnyOf("a", "b")
    }

    @Test
    fun isAnyOfFailure() {
        assertFailsWith<AssertionError> {
            assertThat("x").isAnyOf("a", "b", "c")
        }
    }

    @Test
    fun isAnyOfNullInListWithNull() {
        assertThat(null as String?).isAnyOf("a", "b", null as String?)
    }

    @Test
    fun isAnyOfNonnullInListWithNull() {
        assertThat("b").isAnyOf("a", "b", null as String?)
    }

    @Test
    fun isAnyOfNullFailure() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).isAnyOf("a", "b", "c")
        }
    }

    @Test
    fun isNotIn() {
        assertThat("x").isNotIn(oneShotIterable("a", "b", "c"))
    }

    @Test
    fun isNotInFailure() {
        assertFailsWith<AssertionError> {
            assertThat("b").isNotIn(oneShotIterable("a", "b", "c"))
        }
    }

    @Test
    fun isNotInNull() {
        assertThat(null as String?).isNotIn(oneShotIterable("a", "b", "c"))
    }

    @Test
    fun isNotInNullFailure() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).isNotIn(oneShotIterable("a", "b", null as String?))
        }
    }

    @Test
    fun isNotInEmpty() {
        assertThat("b").isNotIn(emptyList<String>())
    }

    @Test
    fun isNoneOf() {
        assertThat("x").isNoneOf("a", "b", "c")
    }

    @Test
    fun isNoneOfFailure() {
        assertFailsWith<AssertionError> {
            assertThat("b").isNoneOf("a", "b", "c")
        }
    }

    @Test
    fun isNoneOfNull() {
        assertThat(null as String?).isNoneOf("a", "b", "c")
    }

    @Test
    fun isNoneOfNullFailure() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).isNoneOf("a", "b", null as String?)
        }
    }

    @Test
    fun failWithActual_printsAllMessagesPlusActualValue() {
        val subject =
            object : Subject<Int>(
                actual = 0,
                metadata = FailureMetadata(messagesToPrepend = listOf("msg1", "msg2")),
            ) {
                fun fail() {
                    failWithActual(simpleFact("msg3"), simpleFact("msg4"))
                }
            }

        assertFailsWithMessage(
            """
                msg1
                msg2
                msg3
                msg4
                but was: 0
            """.trimIndent()
        ) { subject.fail() }
    }

    @Test
    fun failWithActual_printsAllMessagesPlusMultilineActualValue() {
        val subject =
            object : Subject<String>(
                actual = "a\nb",
                metadata = FailureMetadata(messagesToPrepend = listOf("msg1", "msg2")),
            ) {
                fun fail() {
                    failWithActual(simpleFact("msg3"), simpleFact("msg4"))
                }
            }

        assertFailsWithMessage(
            """
                msg1
                msg2
                msg3
                msg4
                but was:
                    a
                    b
            """.trimIndent()
        ) { subject.fail() }
    }

    @Test
    fun failWithoutActual_printsAllMessagesPlusActualValue() {
        val subject =
            object : Subject<Int>(
                actual = 0,
                metadata = FailureMetadata(messagesToPrepend = listOf("msg1", "msg2")),
            ) {
                fun fail() {
                    failWithoutActual(simpleFact("msg3"), simpleFact("msg4"))
                }
            }

        assertFailsWithMessage(
            """
                msg1
                msg2
                msg3
                msg4
            """.trimIndent()
        ) { subject.fail() }
    }

    @Test
    fun failWithoutActual_printsAllMessagesPlusMultilineActualValue() {
        val subject =
            object : Subject<String>(
                actual = "a\nb",
                metadata = FailureMetadata(messagesToPrepend = listOf("msg1", "msg2")),
            ) {
                fun fail() {
                    failWithoutActual(simpleFact("msg3"), simpleFact("msg4"))
                }
            }

        assertFailsWithMessage(
            """
                msg1
                msg2
                msg3
                msg4
            """.trimIndent()
        ) { subject.fail() }
    }

    private fun <T> oneShotIterable(vararg values: T): Iterable<T> =
        object : Iterable<T> {
            private val iterator = values.iterator()

            override fun iterator(): Iterator<T> = iterator
            override fun toString(): String = values.contentToString()
        }
}

@Suppress("EqualsOrHashCode")
private class ThrowsOnEquals {
    override fun equals(other: Any?): Boolean {
        throw UnsupportedOperationException()
    }
}

@Suppress("EqualsOrHashCode")
private class ThrowsOnEqualsNull {
    override fun equals(other: Any?): Boolean {
        // buggy implementation but one that we're working around, at least for now
        checkNotNull(other)
        return super.equals(other)
    }
}

/**
 * Copied from Truth.
 */
private class ForbidsEqualityChecksSubject(actual: Any?) : Subject<Any>(actual) {
    // Not sure how to feel about this, but people do it:
    override fun isEqualTo(expected: Any?) {
        throw UnsupportedOperationException()
    }

    override fun isNotEqualTo(unexpected: Any?) {
        throw UnsupportedOperationException()
    }
}

private val object1: Any =
    object : Any() {
        override fun toString(): String = "Object 1"
    }

private val object2: Any =
    object : Any() {
        override fun toString(): String = "Object 2"
    }
