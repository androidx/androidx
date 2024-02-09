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

import kotlin.test.Test
import kotlin.test.assertFailsWith

class LongSubjectTest {

    @Test
    fun simpleEquality() {
        assertThat(4L).isEqualTo(4L)
    }

    @Test
    fun simpleInequality() {
        assertThat(4L).isNotEqualTo(5L)
    }

    @Test
    fun equalityWithInts() {
        assertThat(0L).isEqualTo(0)
        assertFailsWith<AssertionError> {
            assertThat(0L).isNotEqualTo(0)
        }
    }

    @Test
    fun equalityFail() {
        assertFailsWith<AssertionError> {
            assertThat(4L).isEqualTo(5L)
        }
    }

    @Test
    fun inequalityFail() {
        assertFailsWith<AssertionError> {
            assertThat(4L).isNotEqualTo(4L)
        }
    }

    @Test
    fun equalityOfNulls() {
        assertThat(null as Long?).isEqualTo(null)
    }

    @Test
    fun equalityOfNullsFail_nullActual() {
        assertFailsWith<AssertionError> {
            assertThat(null as Long?).isEqualTo(5L)
        }
    }

    @Test
    fun equalityOfNullsFail_nullExpected() {
        assertFailsWith<AssertionError> {
            assertThat(5L).isEqualTo(null)
        }
    }

    @Test
    fun inequalityOfNulls() {
        assertThat(4L).isNotEqualTo(null)
        assertThat(null as Int?).isNotEqualTo(4L)
    }

    @Test
    fun inequalityOfNullsFail() {
        assertFailsWith<AssertionError> {
            assertThat(null as Long?).isNotEqualTo(null)
        }
    }

    @Test
    fun testNumericTypeWithSameValue_shouldBeEqual_long_long() {
        assertFailsWith<AssertionError> {
            assertThat(42L).isNotEqualTo(42L)
        }
    }

    @Test
    fun testNumericTypeWithSameValue_shouldBeEqual_long_int() {
        assertFailsWith<AssertionError> {
            assertThat(42L).isNotEqualTo(42)
        }
    }

    @Test
    fun isGreaterThan_int_strictly() {
        assertFailsWith<AssertionError> {
            assertThat(2L).isGreaterThan(3)
        }
    }

    @Test
    fun isGreaterThan_int() {
        assertFailsWith<AssertionError> {
            assertThat(2L).isGreaterThan(2)
        }
        assertThat(2L).isGreaterThan(1)
    }

    @Test
    fun isLessThan_int_strictly() {
        assertFailsWith<AssertionError> {
            assertThat(2L).isLessThan(1)
        }
    }

    @Test
    fun isLessThan_int() {
        assertFailsWith<AssertionError> {
            assertThat(2L).isLessThan(2)
        }
        assertThat(2L).isLessThan(3)
    }

    @Test
    fun isAtLeast_int() {
        assertFailsWith<AssertionError> {
            assertThat(2L).isAtLeast(3)
        }
        assertThat(2L).isAtLeast(2)
        assertThat(2L).isAtLeast(1)
    }

    @Test
    fun isAtMost_int() {
        assertFailsWith<AssertionError> {
            assertThat(2L).isAtMost(1)
        }
        assertThat(2L).isAtMost(2)
        assertThat(2L).isAtMost(3)
    }

    @Test
    fun isWithinOf() {
        assertThat(20000L).isWithin(0L).of(20000L)
        assertThat(20000L).isWithin(1L).of(20000L)
        assertThat(20000L).isWithin(10000L).of(20000L)
        assertThat(20000L).isWithin(10000L).of(30000L)
        assertThat(Long.MIN_VALUE).isWithin(1L).of(Long.MIN_VALUE + 1)
        assertThat(Long.MAX_VALUE).isWithin(1L).of(Long.MAX_VALUE - 1)
        assertThat(Long.MAX_VALUE / 2).isWithin(Long.MAX_VALUE).of(-Long.MAX_VALUE / 2)
        assertThat(-Long.MAX_VALUE / 2).isWithin(Long.MAX_VALUE).of(Long.MAX_VALUE / 2)
        assertThatIsWithinFails(20000L, 9999L, 30000L)
        assertThatIsWithinFails(20000L, 10000L, 30001L)
        assertThatIsWithinFails(Long.MIN_VALUE, 0L, Long.MAX_VALUE)
        assertThatIsWithinFails(Long.MAX_VALUE, 0L, Long.MIN_VALUE)
        assertThatIsWithinFails(Long.MIN_VALUE, 1L, Long.MIN_VALUE + 2)
        assertThatIsWithinFails(Long.MAX_VALUE, 1L, Long.MAX_VALUE - 2)
        // Don't fall for rollover
        assertThatIsWithinFails(Long.MIN_VALUE, 1L, Long.MAX_VALUE)
        assertThatIsWithinFails(Long.MAX_VALUE, 1L, Long.MIN_VALUE)
    }

    private fun assertThatIsWithinFails(actual: Long, tolerance: Long, expected: Long) {
        assertFailsWith<AssertionError> {
            assertThat(actual).isWithin(tolerance).of(expected)
        }
    }

    @Test
    fun isNotWithinOf() {
        assertThatIsNotWithinFails(20000L, 0L, 20000L)
        assertThatIsNotWithinFails(20000L, 1L, 20000L)
        assertThatIsNotWithinFails(20000L, 10000L, 20000L)
        assertThatIsNotWithinFails(20000L, 10000L, 30000L)
        assertThatIsNotWithinFails(Long.MIN_VALUE, 1L, Long.MIN_VALUE + 1)
        assertThatIsNotWithinFails(Long.MAX_VALUE, 1L, Long.MAX_VALUE - 1)
        assertThatIsNotWithinFails(Long.MAX_VALUE / 2, Long.MAX_VALUE, -Long.MAX_VALUE / 2)
        assertThatIsNotWithinFails(-Long.MAX_VALUE / 2, Long.MAX_VALUE, Long.MAX_VALUE / 2)
        assertThat(20000L).isNotWithin(9999L).of(30000L)
        assertThat(20000L).isNotWithin(10000L).of(30001L)
        assertThat(Long.MIN_VALUE).isNotWithin(0L).of(Long.MAX_VALUE)
        assertThat(Long.MAX_VALUE).isNotWithin(0L).of(Long.MIN_VALUE)
        assertThat(Long.MIN_VALUE).isNotWithin(1L).of(Long.MIN_VALUE + 2)
        assertThat(Long.MAX_VALUE).isNotWithin(1L).of(Long.MAX_VALUE - 2)
        // Don't fall for rollover
        assertThat(Long.MIN_VALUE).isNotWithin(1L).of(Long.MAX_VALUE)
        assertThat(Long.MAX_VALUE).isNotWithin(1L).of(Long.MIN_VALUE)
    }

    private fun assertThatIsNotWithinFails(actual: Long, tolerance: Long, expected: Long) {
        assertFailsWith<AssertionError> {
            assertThat(actual).isNotWithin(tolerance).of(expected)
        }
    }

    @Test
    fun isWithinIntegers() {
        assertThat(20000L).isWithin(0).of(20000)
        assertThat(20000L).isWithin(1).of(20000)
        assertThat(20000L).isWithin(10000).of(20000)
        assertThat(20000L).isWithin(10000).of(30000)
        assertThat(20000L).isNotWithin(0).of(200000)
        assertThat(20000L).isNotWithin(1).of(200000)
        assertThat(20000L).isNotWithin(10000).of(200000)
        assertThat(20000L).isNotWithin(10000).of(300000)
    }

    @Test
    fun isWithinNegativeTolerance() {
        isWithinNegativeToleranceThrowsIAE(0L, -10, 5)
        isWithinNegativeToleranceThrowsIAE(0L, -10, 20)
        isNotWithinNegativeToleranceThrowsIAE(0L, -10, 5)
        isNotWithinNegativeToleranceThrowsIAE(0L, -10, 20)
    }

    private fun isWithinNegativeToleranceThrowsIAE(
        actual: Long,
        tolerance: Long,
        expected: Long,
    ) {
        assertFailsWith<IllegalArgumentException>(
            assert = { e ->
                assertThat(e)
                    .hasMessageThat()
                    .isEqualTo("tolerance ($tolerance) cannot be negative")
            }
        ) {
            assertThat(actual).isWithin(tolerance).of(expected)
        }
    }

    private fun isNotWithinNegativeToleranceThrowsIAE(
        actual: Long,
        tolerance: Long,
        expected: Long,
    ) {
        assertFailsWith<IllegalArgumentException>(
            assert = { e ->
                assertThat(e)
                    .hasMessageThat()
                    .isEqualTo("tolerance ($tolerance) cannot be negative")
            }
        ) {
            assertThat(actual).isNotWithin(tolerance).of(expected)
        }
    }
}
