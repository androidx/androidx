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

class IntegerSubjectTest {
    @Test
    fun simpleEquality() {
        assertThat(4).isEqualTo(4)
    }

    @Test
    fun simpleInequality() {
        assertThat(4).isNotEqualTo(5)
    }

    @Test
    fun equalityWithLongs() {
        assertThat(0).isEqualTo(0L)
        assertFailsWith<AssertionError> {
            assertThat(0).isNotEqualTo(0L)
        }
    }

    @Test
    fun equalityFail() {
        assertFailsWith<AssertionError> {
            assertThat(4).isEqualTo(5)
        }
    }

    @Test
    fun inequalityFail() {
        assertFailsWith<AssertionError> {
            assertThat(4).isNotEqualTo(4)
        }
    }

    @Test
    fun equalityOfNulls() {
        assertThat(null as Int?).isEqualTo(null)
    }

    @Test
    fun equalityOfNullsFail_nullExpected() {
        assertFailsWith<AssertionError> {
            assertThat(5).isEqualTo(null)
        }
    }

    @Test
    fun inequalityOfNulls() {
        assertThat(4).isNotEqualTo(null)
        assertThat(null as Int?).isNotEqualTo(4)
    }

    @Test
    fun overflowOnPrimitives() {
        assertThat(Long.MIN_VALUE).isNotEqualTo(Int.MIN_VALUE)
        assertThat(Long.MAX_VALUE).isNotEqualTo(Int.MAX_VALUE)
        assertThat(Int.MIN_VALUE).isNotEqualTo(Long.MIN_VALUE)
        assertThat(Int.MAX_VALUE).isNotEqualTo(Long.MAX_VALUE)
        assertThat(Int.MIN_VALUE).isEqualTo(Int.MIN_VALUE.toLong())
        assertThat(Int.MAX_VALUE).isEqualTo(Int.MAX_VALUE.toLong())
    }

    @Test
    fun overflowOnPrimitives_shouldBeEqualAfterCast_min() {
        assertFailsWith<AssertionError> {
            assertThat(Int.MIN_VALUE).isNotEqualTo(Int.MIN_VALUE.toLong())
        }
    }

    @Test
    fun overflowOnPrimitives_shouldBeEqualAfterCast_max() {
        assertFailsWith<AssertionError> {
            assertThat(Int.MAX_VALUE).isNotEqualTo(Int.MAX_VALUE.toLong())
        }
    }

    @Test
    fun overflowBetweenIntegerAndLong_shouldBeDifferent_min() {
        assertFailsWith<AssertionError> {
            assertThat(Int.MIN_VALUE).isEqualTo(Long.MIN_VALUE)
        }
    }

    @Test
    fun overflowBetweenIntegerAndLong_shouldBeDifferent_max() {
        assertFailsWith<AssertionError> {
            assertThat(Int.MAX_VALUE).isEqualTo(Long.MAX_VALUE)
        }
    }

    @Test
    fun testAllCombinations_pass() {
        assertThat(42).isEqualTo(42L)
        assertThat(42L).isEqualTo(42)
    }

    @Test
    fun testNumericTypeWithSameValue_shouldBeEqual_int_long() {
        assertFailsWith<AssertionError> {
            assertThat(42).isNotEqualTo(42L)
        }
    }

    @Test
    fun testNumericTypeWithSameValue_shouldBeEqual_int_int() {
        assertFailsWith<AssertionError> {
            assertThat(42).isNotEqualTo(42)
        }
    }

    @Test
    fun testNumericPrimitiveTypes_isNotEqual_shouldFail_intToChar() {
        assertFailsWith<AssertionError> {
            assertThat(42).isNotEqualTo(42.toChar())
        }
    }

    @Test
    fun testNumericPrimitiveTypes_isNotEqual_shouldFail_charToInt() {
        // Uses Object overload rather than Integer.
        assertFailsWith<AssertionError> {
            assertThat(42.toChar()).isNotEqualTo(42)
        }
    }
}
