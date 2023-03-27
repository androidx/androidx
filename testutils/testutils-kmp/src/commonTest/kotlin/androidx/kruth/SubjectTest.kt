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
