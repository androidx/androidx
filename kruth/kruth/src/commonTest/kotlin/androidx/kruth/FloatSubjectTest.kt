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

class FloatSubjectTest {

    @Test
    fun testFloatConstants_matchNextAfter() {
        assertThat(Float.MAX_VALUE.nextDown()).isEqualTo(NEARLY_MAX)
        assertThat((-1.0f * Float.MAX_VALUE).nextUp()).isEqualTo(NEGATIVE_NEARLY_MAX)
        assertThat(Float.MIN_VALUE.nextUp()).isEqualTo(JUST_OVER_MIN)
        assertThat((-1.0f * Float.MIN_VALUE).nextDown()).isEqualTo(JUST_UNDER_NEGATIVE_MIN)
        assertThat(1.23f).isEqualTo(GOLDEN)
        assertThat(1.23f.nextUp()).isEqualTo(JUST_OVER_GOLDEN)
    }

    @Test
    fun testJ2clCornerCaseZero() {
        assertThatIsEqualToFails(-0.0f, 0.0f)
    }

    @Test
    fun j2clCornerCaseDoubleVsFloat() {
        assertFailsWith<AssertionError> { assertThat(1.23f).isEqualTo(1.23) }
    }

    @Test
    fun isWithinOf() {
        assertThat(2.0f).isWithin(0.0f).of(2.0f)
        assertThat(2.0f).isWithin(0.00001f).of(2.0f)
        assertThat(2.0f).isWithin(1000.0f).of(2.0f)
        assertThat(2.0f).isWithin(1.00001f).of(3.0f)
        assertThatIsWithinFails(2.0f, 0.99999f, 3.0f)
        assertThatIsWithinFails(2.0f, 1000.0f, 1003.0f)
        assertThatIsWithinFails(2.0f, 1000.0f, Float.POSITIVE_INFINITY)
        assertThatIsWithinFails(2.0f, 1000.0f, Float.NaN)
        assertThatIsWithinFails(Float.NEGATIVE_INFINITY, 1000.0f, 2.0f)
        assertThatIsWithinFails(Float.NaN, 1000.0f, 2.0f)
    }

    private fun assertThatIsWithinFails(actual: Float, tolerance: Float, expected: Float) {
        assertFailsWith<AssertionError> { assertThat(actual).isWithin(tolerance).of(expected) }
    }

    @Test
    fun isNotWithinOf() {
        assertThatIsNotWithinFails(2.0f, 0.0f, 2.0f)
        assertThatIsNotWithinFails(2.0f, 0.00001f, 2.0f)
        assertThatIsNotWithinFails(2.0f, 1000.0f, 2.0f)
        assertThatIsNotWithinFails(2.0f, 1.00001f, 3.0f)
        assertThat(2.0f).isNotWithin(0.99999f).of(3.0f)
        assertThat(2.0f).isNotWithin(1000.0f).of(1003.0f)
        assertThatIsNotWithinFails(2.0f, 0.0f, Float.POSITIVE_INFINITY)
        assertThatIsNotWithinFails(2.0f, 0.0f, Float.NaN)
        assertThatIsNotWithinFails(Float.NEGATIVE_INFINITY, 1000.0f, 2.0f)
        assertThatIsNotWithinFails(Float.NaN, 1000.0f, 2.0f)
    }

    private fun assertThatIsNotWithinFails(actual: Float, tolerance: Float, expected: Float) {
        assertFailsWith<AssertionError> { assertThat(actual).isNotWithin(tolerance).of(expected) }
    }

    @Test
    fun negativeTolerances() {
        isWithinNegativeToleranceThrowsIAE(5.0f, -0.5f, 4.9f)
        isWithinNegativeToleranceThrowsIAE(5.0f, -0.5f, 4.0f)

        isNotWithinNegativeToleranceThrowsIAE(5.0f, -0.5f, 4.9f)
        isNotWithinNegativeToleranceThrowsIAE(5.0f, -0.5f, 4.0f)

        isWithinNegativeToleranceThrowsIAE(+0.0f, -0.00001f, +0.0f)
        isWithinNegativeToleranceThrowsIAE(+0.0f, -0.00001f, -0.0f)
        isWithinNegativeToleranceThrowsIAE(-0.0f, -0.00001f, +0.0f)
        isWithinNegativeToleranceThrowsIAE(-0.0f, -0.00001f, -0.0f)

        isNotWithinNegativeToleranceThrowsIAE(+0.0f, -0.00001f, +1.0f)
        isNotWithinNegativeToleranceThrowsIAE(+0.0f, -0.00001f, -1.0f)
        isNotWithinNegativeToleranceThrowsIAE(-0.0f, -0.00001f, +1.0f)
        isNotWithinNegativeToleranceThrowsIAE(-0.0f, -0.00001f, -1.0f)

        isNotWithinNegativeToleranceThrowsIAE(+1.0f, -0.00001f, +0.0f)
        isNotWithinNegativeToleranceThrowsIAE(+1.0f, -0.00001f, -0.0f)
        isNotWithinNegativeToleranceThrowsIAE(-1.0f, -0.00001f, +0.0f)
        isNotWithinNegativeToleranceThrowsIAE(-1.0f, -0.00001f, -0.0f)

        // You know what's worse than zero? Negative zero.
        isWithinNegativeToleranceThrowsIAE(+0.0f, -0.0f, +0.0f)
        isWithinNegativeToleranceThrowsIAE(+0.0f, -0.0f, -0.0f)
        isWithinNegativeToleranceThrowsIAE(-0.0f, -0.0f, +0.0f)
        isWithinNegativeToleranceThrowsIAE(-0.0f, -0.0f, -0.0f)

        isNotWithinNegativeToleranceThrowsIAE(+1.0f, -0.0f, +0.0f)
        isNotWithinNegativeToleranceThrowsIAE(+1.0f, -0.0f, -0.0f)
        isNotWithinNegativeToleranceThrowsIAE(-1.0f, -0.0f, +0.0f)
        isNotWithinNegativeToleranceThrowsIAE(-1.0f, -0.0f, -0.0f)
    }

    private fun isWithinNegativeToleranceThrowsIAE(
        actual: Float,
        tolerance: Float,
        expected: Float,
    ) {
        assertFailsWith<IllegalArgumentException>(
            assert = { e ->
                assertThat(e)
                    .hasMessageThat()
                    .isEqualTo("Tolerance ($tolerance) cannot be negative")
            }
        ) {
            assertThat(actual).isWithin(tolerance).of(expected)
        }
    }

    private fun isNotWithinNegativeToleranceThrowsIAE(
        actual: Float,
        tolerance: Float,
        expected: Float,
    ) {
        assertFailsWith<IllegalArgumentException>(
            assert = { e ->
                assertThat(e)
                    .hasMessageThat()
                    .isEqualTo("Tolerance ($tolerance) cannot be negative")
            }
        ) {
            assertThat(actual).isNotWithin(tolerance).of(expected)
        }
    }

    @Test
    fun nanTolerances() {
        assertFailsWith<IllegalArgumentException>(
            assert = { e -> assertThat(e).hasMessageThat().isEqualTo("Tolerance cannot be NaN") }
        ) {
            assertThat(1.0f).isWithin(Float.NaN).of(1.0f)
        }

        assertFailsWith<IllegalArgumentException>(
            assert = { e -> assertThat(e).hasMessageThat().isEqualTo("Tolerance cannot be NaN") }
        ) {
            assertThat(1.0f).isNotWithin(Float.NaN).of(2.0f)
        }
    }

    @Test
    fun infiniteTolerances() {
        assertFailsWith<IllegalArgumentException>(
            assert = { e ->
                assertThat(e).hasMessageThat().isEqualTo("Tolerance cannot be POSITIVE_INFINITY")
            }
        ) {
            assertThat(1.0f).isWithin(Float.POSITIVE_INFINITY).of(1.0f)
        }

        assertFailsWith<IllegalArgumentException>(
            assert = { e ->
                assertThat(e).hasMessageThat().isEqualTo("Tolerance cannot be POSITIVE_INFINITY")
            }
        ) {
            assertThat(1.0f).isNotWithin(Float.POSITIVE_INFINITY).of(2.0f)
        }
    }

    @Test
    fun isWithinOfZero() {
        assertThat(+0.0f).isWithin(0.00001f).of(+0.0f)
        assertThat(+0.0f).isWithin(0.00001f).of(-0.0f)
        assertThat(-0.0f).isWithin(0.00001f).of(+0.0f)
        assertThat(-0.0f).isWithin(0.00001f).of(-0.0f)

        assertThat(+0.0f).isWithin(0.0f).of(+0.0f)
        assertThat(+0.0f).isWithin(0.0f).of(-0.0f)
        assertThat(-0.0f).isWithin(0.0f).of(+0.0f)
        assertThat(-0.0f).isWithin(0.0f).of(-0.0f)
    }

    @Test
    fun isNotWithinOfZero() {
        assertThat(+0.0f).isNotWithin(0.00001f).of(+1.0f)
        assertThat(+0.0f).isNotWithin(0.00001f).of(-1.0f)
        assertThat(-0.0f).isNotWithin(0.00001f).of(+1.0f)
        assertThat(-0.0f).isNotWithin(0.00001f).of(-1.0f)

        assertThat(+1.0f).isNotWithin(0.00001f).of(+0.0f)
        assertThat(+1.0f).isNotWithin(0.00001f).of(-0.0f)
        assertThat(-1.0f).isNotWithin(0.00001f).of(+0.0f)
        assertThat(-1.0f).isNotWithin(0.00001f).of(-0.0f)

        assertThat(+1.0f).isNotWithin(0.0f).of(+0.0f)
        assertThat(+1.0f).isNotWithin(0.0f).of(-0.0f)
        assertThat(-1.0f).isNotWithin(0.0f).of(+0.0f)
        assertThat(-1.0f).isNotWithin(0.0f).of(-0.0f)

        assertThatIsNotWithinFails(-0.0f, 0.0f, 0.0f)
    }

    @Test
    fun isWithinZeroTolerance() {
        val max = Float.MAX_VALUE
        assertThat(max).isWithin(0.0f).of(max)
        assertThat(NEARLY_MAX).isWithin(0.0f).of(NEARLY_MAX)
        assertThatIsWithinFails(max, 0.0f, NEARLY_MAX)
        assertThatIsWithinFails(NEARLY_MAX, 0.0f, max)

        val negativeMax = -1.0f * Float.MAX_VALUE
        assertThat(negativeMax).isWithin(0.0f).of(negativeMax)
        assertThat(NEGATIVE_NEARLY_MAX).isWithin(0.0f).of(NEGATIVE_NEARLY_MAX)
        assertThatIsWithinFails(negativeMax, 0.0f, NEGATIVE_NEARLY_MAX)
        assertThatIsWithinFails(NEGATIVE_NEARLY_MAX, 0.0f, negativeMax)

        val min = Float.MIN_VALUE
        assertThat(min).isWithin(0.0f).of(min)
        assertThat(JUST_OVER_MIN).isWithin(0.0f).of(JUST_OVER_MIN)
        assertThatIsWithinFails(min, 0.0f, JUST_OVER_MIN)
        assertThatIsWithinFails(JUST_OVER_MIN, 0.0f, min)

        val negativeMin = -1.0f * Float.MIN_VALUE
        assertThat(negativeMin).isWithin(0.0f).of(negativeMin)
        assertThat(JUST_UNDER_NEGATIVE_MIN).isWithin(0.0f).of(JUST_UNDER_NEGATIVE_MIN)
        assertThatIsWithinFails(negativeMin, 0.0f, JUST_UNDER_NEGATIVE_MIN)
        assertThatIsWithinFails(JUST_UNDER_NEGATIVE_MIN, 0.0f, negativeMin)
    }

    @Test
    fun isNotWithinZeroTolerance() {
        val max = Float.MAX_VALUE
        assertThatIsNotWithinFails(max, 0.0f, max)
        assertThatIsNotWithinFails(NEARLY_MAX, 0.0f, NEARLY_MAX)
        assertThat(max).isNotWithin(0.0f).of(NEARLY_MAX)
        assertThat(NEARLY_MAX).isNotWithin(0.0f).of(max)

        val min = Float.MIN_VALUE
        assertThatIsNotWithinFails(min, 0.0f, min)
        assertThatIsNotWithinFails(JUST_OVER_MIN, 0.0f, JUST_OVER_MIN)
        assertThat(min).isNotWithin(0.0f).of(JUST_OVER_MIN)
        assertThat(JUST_OVER_MIN).isNotWithin(0.0f).of(min)
    }

    @Test
    fun isWithinNonFinite() {
        assertThatIsWithinFails(Float.NaN, 0.00001f, Float.NaN)
        assertThatIsWithinFails(Float.NaN, 0.00001f, Float.POSITIVE_INFINITY)
        assertThatIsWithinFails(Float.NaN, 0.00001f, Float.NEGATIVE_INFINITY)
        assertThatIsWithinFails(Float.NaN, 0.00001f, +0.0f)
        assertThatIsWithinFails(Float.NaN, 0.00001f, -0.0f)
        assertThatIsWithinFails(Float.NaN, 0.00001f, +1.0f)
        assertThatIsWithinFails(Float.NaN, 0.00001f, -0.0f)
        assertThatIsWithinFails(Float.POSITIVE_INFINITY, 0.00001f, Float.POSITIVE_INFINITY)
        assertThatIsWithinFails(Float.POSITIVE_INFINITY, 0.00001f, Float.NEGATIVE_INFINITY)
        assertThatIsWithinFails(Float.POSITIVE_INFINITY, 0.00001f, +0.0f)
        assertThatIsWithinFails(Float.POSITIVE_INFINITY, 0.00001f, -0.0f)
        assertThatIsWithinFails(Float.POSITIVE_INFINITY, 0.00001f, +1.0f)
        assertThatIsWithinFails(Float.POSITIVE_INFINITY, 0.00001f, -0.0f)
        assertThatIsWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, Float.NEGATIVE_INFINITY)
        assertThatIsWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, +0.0f)
        assertThatIsWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, -0.0f)
        assertThatIsWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, +1.0f)
        assertThatIsWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, -0.0f)
        assertThatIsWithinFails(+1.0f, 0.00001f, Float.NaN)
        assertThatIsWithinFails(+1.0f, 0.00001f, Float.POSITIVE_INFINITY)
        assertThatIsWithinFails(+1.0f, 0.00001f, Float.NEGATIVE_INFINITY)
    }

    @Test
    fun isNotWithinNonFinite() {
        assertThatIsNotWithinFails(Float.NaN, 0.00001f, Float.NaN)
        assertThatIsNotWithinFails(Float.NaN, 0.00001f, Float.POSITIVE_INFINITY)
        assertThatIsNotWithinFails(Float.NaN, 0.00001f, Float.NEGATIVE_INFINITY)
        assertThatIsNotWithinFails(Float.NaN, 0.00001f, +0.0f)
        assertThatIsNotWithinFails(Float.NaN, 0.00001f, -0.0f)
        assertThatIsNotWithinFails(Float.NaN, 0.00001f, +1.0f)
        assertThatIsNotWithinFails(Float.NaN, 0.00001f, -0.0f)
        assertThatIsNotWithinFails(Float.POSITIVE_INFINITY, 0.00001f, Float.POSITIVE_INFINITY)
        assertThatIsNotWithinFails(Float.POSITIVE_INFINITY, 0.00001f, Float.NEGATIVE_INFINITY)
        assertThatIsNotWithinFails(Float.POSITIVE_INFINITY, 0.00001f, +0.0f)
        assertThatIsNotWithinFails(Float.POSITIVE_INFINITY, 0.00001f, -0.0f)
        assertThatIsNotWithinFails(Float.POSITIVE_INFINITY, 0.00001f, +1.0f)
        assertThatIsNotWithinFails(Float.POSITIVE_INFINITY, 0.00001f, -0.0f)
        assertThatIsNotWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, Float.NEGATIVE_INFINITY)
        assertThatIsNotWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, +0.0f)
        assertThatIsNotWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, -0.0f)
        assertThatIsNotWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, +1.0f)
        assertThatIsNotWithinFails(Float.NEGATIVE_INFINITY, 0.00001f, -0.0f)
        assertThatIsNotWithinFails(+1.0f, 0.00001f, Float.NaN)
        assertThatIsNotWithinFails(+1.0f, 0.00001f, Float.POSITIVE_INFINITY)
        assertThatIsNotWithinFails(+1.0f, 0.00001f, Float.NEGATIVE_INFINITY)
    }

    @Test
    fun isEqualTo() {
        assertThat(GOLDEN).isEqualTo(GOLDEN)
        assertThatIsEqualToFails(GOLDEN, JUST_OVER_GOLDEN)
        assertThat(Float.POSITIVE_INFINITY).isEqualTo(Float.POSITIVE_INFINITY)
        assertThat(Float.NaN).isEqualTo(Float.NaN)
        assertThat(null as Float?).isEqualTo(null)
        assertThat(1.0f).isEqualTo(1)
    }

    private fun assertThatIsEqualToFails(actual: Float, expected: Float) {
        assertFailsWith<AssertionError> { assertThat(actual).isEqualTo(expected) }
    }

    @Test
    fun isNotEqualTo() {
        assertThatIsNotEqualToFails(GOLDEN)
        assertThat(GOLDEN).isNotEqualTo(JUST_OVER_GOLDEN)
        assertThatIsNotEqualToFails(Float.POSITIVE_INFINITY)
        assertThatIsNotEqualToFails(Float.NaN)
        assertThat(-0.0f).isNotEqualTo(0.0f)
        assertThatIsNotEqualToFails(null)
        assertThat(1.23f).isNotEqualTo(1.23)
        assertThat(1.0f).isNotEqualTo(2)
    }

    private fun assertThatIsNotEqualToFails(value: Float?) {
        assertFailsWith<AssertionError> { assertThat(value).isNotEqualTo(value) }
    }

    @Test
    fun isZero() {
        assertThat(0.0f).isZero()
        assertThat(-0.0f).isZero()
        assertThatIsZeroFails(Float.MIN_VALUE)
        assertThatIsZeroFails(-1.23f)
        assertThatIsZeroFails(Float.POSITIVE_INFINITY)
        assertThatIsZeroFails(Float.NaN)
        assertThatIsZeroFails(null)
    }

    private fun assertThatIsZeroFails(actual: Float?) {
        assertFailsWith<AssertionError> { assertThat(actual).isZero() }
    }

    @Test
    fun isNonZero() {
        assertThatIsNonZeroFails(0.0f)
        assertThatIsNonZeroFails(-0.0f)
        assertThat(Float.MIN_VALUE).isNonZero()
        assertThat(-1.23f).isNonZero()
        assertThat(Float.POSITIVE_INFINITY).isNonZero()
        assertThat(Float.NaN).isNonZero()
        assertThatIsNonZeroFails(null)
    }

    private fun assertThatIsNonZeroFails(actual: Float?) {
        assertFailsWith<AssertionError> { assertThat(actual).isNonZero() }
    }

    @Test
    fun isPositiveInfinity() {
        assertThat(Float.POSITIVE_INFINITY).isPositiveInfinity()
        assertThatIsPositiveInfinityFails(1.23f)
        assertThatIsPositiveInfinityFails(Float.NEGATIVE_INFINITY)
        assertThatIsPositiveInfinityFails(Float.NaN)
        assertThatIsPositiveInfinityFails(null)
    }

    private fun assertThatIsPositiveInfinityFails(actual: Float?) {
        assertFailsWith<AssertionError> { assertThat(actual).isPositiveInfinity() }
    }

    @Test
    fun isNegativeInfinity() {
        assertThat(Float.NEGATIVE_INFINITY).isNegativeInfinity()
        assertThatIsNegativeInfinityFails(1.23f)
        assertThatIsNegativeInfinityFails(Float.POSITIVE_INFINITY)
        assertThatIsNegativeInfinityFails(Float.NaN)
        assertThatIsNegativeInfinityFails(null)
    }

    private fun assertThatIsNegativeInfinityFails(actual: Float?) {
        assertFailsWith<AssertionError> { assertThat(actual).isNegativeInfinity() }
    }

    @Test
    fun isNaN() {
        assertThat(Float.NaN).isNaN()
        assertThatIsNaNFails(1.23f)
        assertThatIsNaNFails(Float.POSITIVE_INFINITY)
        assertThatIsNaNFails(Float.NEGATIVE_INFINITY)
        assertThatIsNaNFails(null)
    }

    private fun assertThatIsNaNFails(actual: Float?) {
        assertFailsWith<AssertionError> { assertThat(actual).isNaN() }
    }

    @Test
    fun isFinite() {
        assertThat(1.23f).isFinite()
        assertThat(Float.MAX_VALUE).isFinite()
        assertThat(-1.0 * Float.MIN_VALUE).isFinite()
        assertThatIsFiniteFails(Float.POSITIVE_INFINITY)
        assertThatIsFiniteFails(Float.NEGATIVE_INFINITY)
        assertThatIsFiniteFails(Float.NaN)
        assertThatIsFiniteFails(null)
    }

    private fun assertThatIsFiniteFails(actual: Float?) {
        assertFailsWith<AssertionError> { assertThat(actual).isFinite() }
    }

    @Test
    fun isNotNaN() {
        assertThat(1.23f).isNotNaN()
        assertThat(Float.MAX_VALUE).isNotNaN()
        assertThat(-1.0 * Float.MIN_VALUE).isNotNaN()
        assertThat(Float.POSITIVE_INFINITY).isNotNaN()
        assertThat(Float.NEGATIVE_INFINITY).isNotNaN()
    }

    @Test
    fun isNotNaNIsNaN() {
        assertFailsWith<AssertionError> { assertThat(Float.NaN).isNotNaN() }
    }

    @Test
    fun isNotNaNIsNull() {
        assertFailsWith<AssertionError> { assertThat(null as Float?).isNotNaN() }
    }

    @Test
    fun isGreaterThan_int_strictly() {
        assertFailsWith<AssertionError> { assertThat(2.0f).isGreaterThan(3) }
    }

    @Test
    fun isGreaterThan_int() {
        assertFailsWith<AssertionError> { assertThat(2.0f).isGreaterThan(2) }
    }

    @Test
    fun isLessThan_int_strictly() {
        assertFailsWith<AssertionError> { assertThat(2.0f).isLessThan(1) }
    }

    @Test
    fun isLessThan_int() {
        assertFailsWith<AssertionError> { assertThat(2.0f).isLessThan(2) }
    }

    @Test
    fun isAtLeast_int() {
        assertFailsWith<AssertionError> { assertThat(2.0f).isAtLeast(3) }
    }

    @Test
    fun isAtLeast_int_withNoExactFloatRepresentation() {
        assertFailsWith<AssertionError> { assertThat(1.07374182E9f).isAtLeast((1 shl 30) + 1) }
    }

    @Test
    fun isAtMost_int() {
        assertFailsWith<AssertionError> { assertThat(2.0f).isAtMost(1) }
    }

    @Test
    fun isAtMost_int_withNoExactFloatRepresentation() {
        assertFailsWith<AssertionError> { assertThat(1.07374182E9f).isAtMost((1 shl 30) - 1) }
    }
}

private const val NEARLY_MAX = 3.4028233E38f
private const val NEGATIVE_NEARLY_MAX = -3.4028233E38f
private const val JUST_OVER_MIN = 2.8E-45f
private const val JUST_UNDER_NEGATIVE_MIN = -2.8E-45f
private const val GOLDEN = 1.23f
private const val JUST_OVER_GOLDEN = 1.2300001f
