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

private val NEARLY_MAX = 1.7976931348623155E308
private val NEGATIVE_NEARLY_MAX = -1.7976931348623155E308
private val OVER_MIN = 9.9E-324
private val UNDER_NEGATIVE_MIN = -9.9E-324
private val GOLDEN = 1.23
private val OVER_GOLDEN = 1.2300000000000002

/**
 * Approximate mirror of truth library for DoubleSubjectTest
 */
class DoubleSubjectTest {
    /**
     * Test case utilizes the private function [Subject.standardIsEqualTo] found in [Subject]
     * which relies on the [Double.compareTo] built-in
     *
     * Also asserts Kotlin behavior where -0.0 is not equal to 0.0 when dynamically typed
     */
    @Test
    fun doubleCornerCaseZero() {
        var dynamicZeroDouble: Any = 0.0
        var dynamicNegativeZeroDouble: Any = -0.0
        assertThat(dynamicZeroDouble == dynamicNegativeZeroDouble).isFalse()
        assertThatIsEqualToFails(-0.0, 0.0)
    }

    @Test
    fun doubleIsWithinOf() {
        assertThat(2.0).isWithin(0.0).of(2.0)
        assertThat(2.0).isWithin(0.00001).of(2.0)
        assertThat(2.0).isWithin(1000.0).of(2.0)
        assertThat(2.0).isWithin(1.00001).of(3.0)
        assertThatIsWithinFails(2.0, 0.99999, 3.0)
        assertThatIsWithinFails(2.0, 1000.0, 1003.0)
        assertThatIsWithinFails(2.0, 1000.0, Double.POSITIVE_INFINITY)
        assertThatIsWithinFails(2.0, 1000.0, Double.NaN)
        assertThatIsWithinFails(Double.NEGATIVE_INFINITY, 1000.0, 2.0)
        assertThatIsWithinFails(Double.NaN, 1000.0, 2.0)
    }

    private fun assertThatIsWithinFails(actual: Double, tolerance: Double, expected: Double) {
        assertFailsWith<AssertionError> {
            assertThat(actual).isWithin(tolerance).of(expected)
        }
    }

    @Test
    fun doubleIsNotWithinOf() {
        assertThatIsNotWithinFails(2.0, 0.0, 2.0)
        assertThatIsNotWithinFails(2.0, 0.00001, 2.0)
        assertThatIsNotWithinFails(2.0, 1000.0, 2.0)
        assertThatIsNotWithinFails(2.0, 1.00001, 3.0)
        assertThat(2.0).isNotWithin(0.99999).of(3.0)
        assertThat(2.0).isNotWithin(1000.0).of(1003.0)
        assertThatIsNotWithinFails(2.0, 0.0, Double.POSITIVE_INFINITY)
        assertThatIsNotWithinFails(2.0, 0.0, Double.NaN)
        assertThatIsNotWithinFails(Double.NEGATIVE_INFINITY, 1000.0, 2.0)
        assertThatIsNotWithinFails(Double.NaN, 1000.0, 2.0)
    }

    private fun assertThatIsNotWithinFails(actual: Double, tolerance: Double, expected: Double) {
        assertFailsWith<AssertionError> {
            assertThat(actual).isNotWithin(tolerance).of(expected)
        }
    }

    @Test
    fun doubleNegativeTolerances() {
        isWithinNegativeToleranceThrowsIAE(5.0, -0.5, 4.9)
        isWithinNegativeToleranceThrowsIAE(5.0, -0.5, 4.0)
        isNotWithinNegativeToleranceThrowsIAE(5.0, -0.5, 4.9)
        isNotWithinNegativeToleranceThrowsIAE(5.0, -0.5, 4.0)
        isWithinNegativeToleranceThrowsIAE(+0.0, -0.00001, +0.0)
        isWithinNegativeToleranceThrowsIAE(+0.0, -0.00001, -0.0)
        isWithinNegativeToleranceThrowsIAE(-0.0, -0.00001, +0.0)
        isWithinNegativeToleranceThrowsIAE(-0.0, -0.00001, -0.0)
        isNotWithinNegativeToleranceThrowsIAE(+0.0, -0.00001, +1.0)
        isNotWithinNegativeToleranceThrowsIAE(+0.0, -0.00001, -1.0)
        isNotWithinNegativeToleranceThrowsIAE(-0.0, -0.00001, +1.0)
        isNotWithinNegativeToleranceThrowsIAE(-0.0, -0.00001, -1.0)
        isNotWithinNegativeToleranceThrowsIAE(+1.0, -0.00001, +0.0)
        isNotWithinNegativeToleranceThrowsIAE(+1.0, -0.00001, -0.0)
        isNotWithinNegativeToleranceThrowsIAE(-1.0, -0.00001, +0.0)
        isNotWithinNegativeToleranceThrowsIAE(-1.0, -0.00001, -0.0)

        // You know what's worse than zero? Negative zero.
        isWithinNegativeToleranceThrowsIAE(+0.0, -0.0, +0.0)
        isWithinNegativeToleranceThrowsIAE(+0.0, -0.0, -0.0)
        isWithinNegativeToleranceThrowsIAE(-0.0, -0.0, +0.0)
        isWithinNegativeToleranceThrowsIAE(-0.0, -0.0, -0.0)
        isNotWithinNegativeToleranceThrowsIAE(+1.0, -0.0, +0.0)
        isNotWithinNegativeToleranceThrowsIAE(+1.0, -0.0, -0.0)
        isNotWithinNegativeToleranceThrowsIAE(-1.0, -0.0, +0.0)
        isNotWithinNegativeToleranceThrowsIAE(-1.0, -0.0, -0.0)
    }

    private fun isWithinNegativeToleranceThrowsIAE(
        actual: Double,
        tolerance: Double,
        expected: Double,
    ) {
        try {
            assertThat(actual).isWithin(tolerance).of(expected)
            fail("Expected IllegalArgumentException to be thrown but wasn't")
        } catch (iae: IllegalArgumentException) {
            assertThat(iae)
                .hasMessageThat()
                .isEqualTo("tolerance $tolerance cannot be negative")
        }
    }

    private fun isNotWithinNegativeToleranceThrowsIAE(
        actual: Double,
        tolerance: Double,
        expected: Double,
    ) {
        val exception = assertFailsWith<IllegalArgumentException> {
            assertThat(actual)
                .isNotWithin(tolerance)
                .of(expected)
        }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("tolerance $tolerance cannot be negative")
    }

    @Test
    fun doubleNanTolerances() {
        try {
            assertThat(1.0).isWithin(Double.NaN).of(1.0)
            fail("Expected IllegalArgumentException to be thrown but wasn't")
        } catch (iae: IllegalArgumentException) {
            assertThat(iae).hasMessageThat().isEqualTo("tolerance cannot be NaN")
        }
        try {
            assertThat(1.0).isNotWithin(Double.NaN).of(2.0)
            fail("Expected IllegalArgumentException to be thrown but wasn't")
        } catch (iae: IllegalArgumentException) {
            assertThat(iae).hasMessageThat().isEqualTo("tolerance cannot be NaN")
        }
    }

    @Test
    fun doubleInfiniteTolerances() {
        try {
            assertThat(1.0).isWithin(Double.POSITIVE_INFINITY).of(1.0)
            fail("Expected IllegalArgumentException to be thrown but wasn't")
        } catch (iae: IllegalArgumentException) {
            assertThat(iae).hasMessageThat().isEqualTo("tolerance cannot be POSITIVE_INFINITY")
        }
        try {
            assertThat(1.0).isNotWithin(Double.POSITIVE_INFINITY).of(2.0)
            fail("Expected IllegalArgumentException to be thrown but wasn't")
        } catch (iae: IllegalArgumentException) {
            assertThat(iae).hasMessageThat().isEqualTo("tolerance cannot be POSITIVE_INFINITY")
        }
    }

    @Test
    fun doubleIsWithinOfZero() {
        assertThat(+0.0).isWithin(0.00001).of(+0.0)
        assertThat(+0.0).isWithin(0.00001).of(-0.0)
        assertThat(-0.0).isWithin(0.00001).of(+0.0)
        assertThat(-0.0).isWithin(0.00001).of(-0.0)
        assertThat(+0.0).isWithin(0.0).of(+0.0)
        assertThat(+0.0).isWithin(0.0).of(-0.0)
        assertThat(-0.0).isWithin(0.0).of(+0.0)
        assertThat(-0.0).isWithin(0.0).of(-0.0)
    }

    @Test
    fun doubleIsNotWithinOfZero() {
        assertThat(+0.0).isNotWithin(0.00001).of(+1.0)
        assertThat(+0.0).isNotWithin(0.00001).of(-1.0)
        assertThat(-0.0).isNotWithin(0.00001).of(+1.0)
        assertThat(-0.0).isNotWithin(0.00001).of(-1.0)
        assertThat(+1.0).isNotWithin(0.00001).of(+0.0)
        assertThat(+1.0).isNotWithin(0.00001).of(-0.0)
        assertThat(-1.0).isNotWithin(0.00001).of(+0.0)
        assertThat(-1.0).isNotWithin(0.00001).of(-0.0)
        assertThat(+1.0).isNotWithin(0.0).of(+0.0)
        assertThat(+1.0).isNotWithin(0.0).of(-0.0)
        assertThat(-1.0).isNotWithin(0.0).of(+0.0)
        assertThat(-1.0).isNotWithin(0.0).of(-0.0)
        assertThatIsNotWithinFails(-0.0, 0.0, 0.0)
    }

    @Test
    fun doubleIsWithinZeroTolerance() {
        val max = Double.MAX_VALUE
        assertThat(max).isWithin(0.0).of(max)
        assertThat(NEARLY_MAX).isWithin(0.0).of(NEARLY_MAX)
        assertThatIsWithinFails(max, 0.0, NEARLY_MAX)
        assertThatIsWithinFails(NEARLY_MAX, 0.0, max)
        val negativeMax = -1.0 * Double.MAX_VALUE
        assertThat(negativeMax).isWithin(0.0).of(negativeMax)
        assertThat(NEGATIVE_NEARLY_MAX).isWithin(0.0).of(NEGATIVE_NEARLY_MAX)
        assertThatIsWithinFails(negativeMax, 0.0, NEGATIVE_NEARLY_MAX)
        assertThatIsWithinFails(NEGATIVE_NEARLY_MAX, 0.0, negativeMax)
        val min = Double.MIN_VALUE
        assertThat(min).isWithin(0.0).of(min)
        assertThat(OVER_MIN).isWithin(0.0).of(OVER_MIN)
        assertThatIsWithinFails(min, 0.0, OVER_MIN)
        assertThatIsWithinFails(OVER_MIN, 0.0, min)
        val negativeMin = -1.0 * Double.MIN_VALUE
        assertThat(negativeMin).isWithin(0.0).of(negativeMin)
        assertThat(UNDER_NEGATIVE_MIN).isWithin(0.0).of(UNDER_NEGATIVE_MIN)
        assertThatIsWithinFails(negativeMin, 0.0, UNDER_NEGATIVE_MIN)
        assertThatIsWithinFails(UNDER_NEGATIVE_MIN, 0.0, negativeMin)
    }

    @Test
    fun doubleIsNotWithinZeroTolerance() {
        val max = Double.MAX_VALUE
        assertThatIsNotWithinFails(max, 0.0, max)
        assertThatIsNotWithinFails(NEARLY_MAX, 0.0, NEARLY_MAX)
        assertThat(max).isNotWithin(0.0).of(NEARLY_MAX)
        assertThat(NEARLY_MAX).isNotWithin(0.0).of(max)
        val min = Double.MIN_VALUE
        assertThatIsNotWithinFails(min, 0.0, min)
        assertThatIsNotWithinFails(OVER_MIN, 0.0, OVER_MIN)
        assertThat(min).isNotWithin(0.0).of(OVER_MIN)
        assertThat(OVER_MIN).isNotWithin(0.0).of(min)
    }

    @Test
    fun doubleIsWithinNonFinite() {
        assertThatIsWithinFails(Double.NaN, 0.00001, Double.NaN)
        assertThatIsWithinFails(Double.NaN, 0.00001, Double.POSITIVE_INFINITY)
        assertThatIsWithinFails(Double.NaN, 0.00001, Double.NEGATIVE_INFINITY)
        assertThatIsWithinFails(Double.NaN, 0.00001, +0.0)
        assertThatIsWithinFails(Double.NaN, 0.00001, -0.0)
        assertThatIsWithinFails(Double.NaN, 0.00001, +1.0)
        assertThatIsWithinFails(Double.NaN, 0.00001, -0.0)
        assertThatIsWithinFails(Double.POSITIVE_INFINITY, 0.00001, Double.POSITIVE_INFINITY)
        assertThatIsWithinFails(Double.POSITIVE_INFINITY, 0.00001, Double.NEGATIVE_INFINITY)
        assertThatIsWithinFails(Double.POSITIVE_INFINITY, 0.00001, +0.0)
        assertThatIsWithinFails(Double.POSITIVE_INFINITY, 0.00001, -0.0)
        assertThatIsWithinFails(Double.POSITIVE_INFINITY, 0.00001, +1.0)
        assertThatIsWithinFails(Double.POSITIVE_INFINITY, 0.00001, -0.0)
        assertThatIsWithinFails(Double.NEGATIVE_INFINITY, 0.00001, Double.NEGATIVE_INFINITY)
        assertThatIsWithinFails(Double.NEGATIVE_INFINITY, 0.00001, +0.0)
        assertThatIsWithinFails(Double.NEGATIVE_INFINITY, 0.00001, -0.0)
        assertThatIsWithinFails(Double.NEGATIVE_INFINITY, 0.00001, +1.0)
        assertThatIsWithinFails(Double.NEGATIVE_INFINITY, 0.00001, -0.0)
        assertThatIsWithinFails(+1.0, 0.00001, Double.NaN)
        assertThatIsWithinFails(+1.0, 0.00001, Double.POSITIVE_INFINITY)
        assertThatIsWithinFails(+1.0, 0.00001, Double.NEGATIVE_INFINITY)
    }

    @Test
    fun doubleIsNotWithinNonFinite() {
        assertThatIsNotWithinFails(Double.NaN, 0.00001, Double.NaN)
        assertThatIsNotWithinFails(Double.NaN, 0.00001, Double.POSITIVE_INFINITY)
        assertThatIsNotWithinFails(Double.NaN, 0.00001, Double.NEGATIVE_INFINITY)
        assertThatIsNotWithinFails(Double.NaN, 0.00001, +0.0)
        assertThatIsNotWithinFails(Double.NaN, 0.00001, -0.0)
        assertThatIsNotWithinFails(Double.NaN, 0.00001, +1.0)
        assertThatIsNotWithinFails(Double.NaN, 0.00001, -0.0)
        assertThatIsNotWithinFails(Double.POSITIVE_INFINITY, 0.00001, Double.POSITIVE_INFINITY)
        assertThatIsNotWithinFails(Double.POSITIVE_INFINITY, 0.00001, Double.NEGATIVE_INFINITY)
        assertThatIsNotWithinFails(Double.POSITIVE_INFINITY, 0.00001, +0.0)
        assertThatIsNotWithinFails(Double.POSITIVE_INFINITY, 0.00001, -0.0)
        assertThatIsNotWithinFails(Double.POSITIVE_INFINITY, 0.00001, +1.0)
        assertThatIsNotWithinFails(Double.POSITIVE_INFINITY, 0.00001, -0.0)
        assertThatIsNotWithinFails(Double.NEGATIVE_INFINITY, 0.00001, Double.NEGATIVE_INFINITY)
        assertThatIsNotWithinFails(Double.NEGATIVE_INFINITY, 0.00001, +0.0)
        assertThatIsNotWithinFails(Double.NEGATIVE_INFINITY, 0.00001, -0.0)
        assertThatIsNotWithinFails(Double.NEGATIVE_INFINITY, 0.00001, +1.0)
        assertThatIsNotWithinFails(Double.NEGATIVE_INFINITY, 0.00001, -0.0)
        assertThatIsNotWithinFails(+1.0, 0.00001, Double.NaN)
        assertThatIsNotWithinFails(+1.0, 0.00001, Double.POSITIVE_INFINITY)
        assertThatIsNotWithinFails(+1.0, 0.00001, Double.NEGATIVE_INFINITY)
    }

    @Test
    fun doubleIsEqualTo() {
        assertThat(1.23).isEqualTo(1.23)
        assertThatIsEqualToFails(GOLDEN, OVER_GOLDEN)
        assertThat(Double.POSITIVE_INFINITY).isEqualTo(Double.POSITIVE_INFINITY)
        assertThat(Double.NaN).isEqualTo(Double.NaN)
        assertThat(null as Double?).isEqualTo(null)
        assertThat(1.0).isEqualTo(1)
    }

    private fun assertThatIsEqualToFails(actual: Double, expected: Double) {
        assertFailsWith<AssertionError> {
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun doubleIsNotEqualTo() {
        assertThatIsNotEqualToFails(1.23)
        assertThat(GOLDEN).isNotEqualTo(OVER_GOLDEN)
        assertThatIsNotEqualToFails(Double.POSITIVE_INFINITY)
        assertThatIsNotEqualToFails(Double.NaN)
        assertThat(-0.0).isNotEqualTo(0.0)
        assertThatIsNotEqualToFails(null)
        assertThat(1.23).isNotEqualTo(1.23f)
        assertThat(1.0).isNotEqualTo(2)
    }

    private fun assertThatIsNotEqualToFails(value: Double?) {
        assertFailsWith<AssertionError> {
            assertThat(value).isNotEqualTo(value)
        }
    }

    @Test
    fun doubleIsZero() {
        assertThat(0.0).isZero()
        assertThat(-0.0).isZero()
        assertThatIsZeroFails(Double.MIN_VALUE)
        assertThatIsZeroFails(-1.23)
        assertThatIsZeroFails(Double.POSITIVE_INFINITY)
        assertThatIsZeroFails(Double.NaN)
        assertThatIsZeroFails(null)
    }

    private fun assertThatIsZeroFails(value: Double?) {
        assertFailsWith<AssertionError> {
            assertThat(value).isZero()
        }
    }

    @Test
    fun doubleIsNonZero() {
        assertThatIsNonZeroFails(0.0)
        assertThatIsNonZeroFails(-0.0)
        assertThat(Double.MIN_VALUE).isNonZero()
        assertThat(-1.23).isNonZero()
        assertThat(Double.POSITIVE_INFINITY).isNonZero()
        assertThat(Double.NaN).isNonZero()
        assertThatIsNonZeroFails(null)
    }

    private fun assertThatIsNonZeroFails(value: Double?) {
        assertFailsWith<AssertionError> {
            assertThat(value).isNonZero()
        }
    }

    @Test
    fun doubleIsPositiveInfinity() {
        assertThat(Double.POSITIVE_INFINITY).isPositiveInfinity()
        assertThatIsPositiveInfinityFails(1.23)
        assertThatIsPositiveInfinityFails(Double.NEGATIVE_INFINITY)
        assertThatIsPositiveInfinityFails(Double.NaN)
        assertThatIsPositiveInfinityFails(null)
    }

    private fun assertThatIsPositiveInfinityFails(value: Double?) {
        assertFailsWith<AssertionError> {
            assertThat(value).isPositiveInfinity()
        }
    }

    @Test
    fun doubleIsNegativeInfinity() {
        assertThat(Double.NEGATIVE_INFINITY).isNegativeInfinity()
        assertThatIsNegativeInfinityFails(1.23)
        assertThatIsNegativeInfinityFails(Double.POSITIVE_INFINITY)
        assertThatIsNegativeInfinityFails(Double.NaN)
        assertThatIsNegativeInfinityFails(null)
    }

    private fun assertThatIsNegativeInfinityFails(value: Double?) {
        assertFailsWith<AssertionError> {
            assertThat(value).isNegativeInfinity()
        }
    }

    @Test
    fun doubleIsNaN() {
        assertThat(Double.NaN).isNaN()
        assertThatIsNaNFails(1.23)
        assertThatIsNaNFails(Double.POSITIVE_INFINITY)
        assertThatIsNaNFails(Double.NEGATIVE_INFINITY)
        assertThatIsNaNFails(null)
    }

    private fun assertThatIsNaNFails(value: Double?) {
        assertFailsWith<AssertionError> {
            assertThat(value).isNaN()
        }
    }

    @Test
    fun doubleIsFinite() {
        assertThat(1.23).isFinite()
        assertThat(Double.MAX_VALUE).isFinite()
        assertThat(-1.0 * Double.MIN_VALUE).isFinite()
        assertThatIsFiniteFails(Double.POSITIVE_INFINITY)
        assertThatIsFiniteFails(Double.NEGATIVE_INFINITY)
        assertThatIsFiniteFails(Double.NaN)
        assertThatIsFiniteFails(null)
    }

    private fun assertThatIsFiniteFails(value: Double?) {
        assertFailsWith<AssertionError> {
            assertThat(value).isFinite()
        }
    }

    @Test
    fun doubleIsNotNaN() {
        assertThat(1.23).isNotNaN()
        assertThat(Double.MAX_VALUE).isNotNaN()
        assertThat(-1.0 * Double.MIN_VALUE).isNotNaN()
        assertThat(Double.POSITIVE_INFINITY).isNotNaN()
        assertThat(Double.NEGATIVE_INFINITY).isNotNaN()
    }

    @Test
    fun doubleIsNotNaNIsNaN() {
        assertFailsWith<AssertionError> {
            assertThat(Double.NaN).isNotNaN()
        }
    }

    @Test
    fun doubleIsNotNaNIsNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as Double?).isNotNull()
        }
    }

    @Test
    fun doubleIsGreaterThan_int_strictly() {
        assertFailsWith<AssertionError> {
            assertThat(2.0).isGreaterThan(3)
        }
    }

    @Test
    fun doubleIsGreaterThan_int() {
        assertFailsWith<AssertionError> {
            assertThat(2.0).isGreaterThan(2)
        }
        assertThat(2.0).isGreaterThan(1)
    }

    @Test
    fun doubleIsLessThan_int_strictly() {
        assertFailsWith<AssertionError> {
            assertThat(2.0).isLessThan(1)
        }
    }

    @Test
    fun doubleIsLessThan_int() {
        assertFailsWith<AssertionError> {
            assertThat(2.0).isLessThan(2)
        }
        assertThat(2.0).isLessThan(3)
    }

    @Test
    fun doubleIsAtLeast_int() {
        assertFailsWith<AssertionError> {
            assertThat(2.0).isAtLeast(3)
        }
        assertThat(2.0).isAtLeast(2)
        assertThat(2.0).isAtLeast(1)
    }

    @Test
    fun doubleIsAtMost_int() {
        assertFailsWith<AssertionError> {
            assertThat(2.0).isAtMost(1)
        }
        assertThat(2.0).isAtMost(2)
        assertThat(2.0).isAtMost(3)
    }
}
