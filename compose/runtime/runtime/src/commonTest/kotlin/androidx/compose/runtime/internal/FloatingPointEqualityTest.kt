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

package androidx.compose.runtime.internal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FloatingPointEqualityTest {

    @Test
    fun testFloat_arbitraryValueEquality() {
        assertEqualsWithNanFix(12345f, 12345f)
        assertEqualsWithNanFix(-98.076f, -98.076f)
        assertNotEqualsWithNanFix(12.34f, 12.30f)
    }

    @Test
    fun testFloat_inequalityFromLossOfPrecision() {
        assertNotEqualsWithNanFix(0.5f, 0.01f * 5)
    }

    @Test
    fun testFloat_nanConstant_doesNotEqualItself() {
        assertNotEqualsWithNanFix(Float.NaN, Float.NaN)
    }

    @Test
    fun testFloat_nonCanonicalNans_areNotEqual() {
        val unconventionalNan = Float.fromBits(0x7FC0ABCD)
        assertNotEqualsWithNanFix(unconventionalNan, unconventionalNan)
    }

    @Test
    fun testFloat_negativeZero_doesEqualsPositiveZero() {
        assertEqualsWithNanFix(Float.NegativeZero, 0f)
    }

    @Test
    fun testFloat_negativeZero_equalsNegativeZero() {
        assertEqualsWithNanFix(Float.NegativeZero, Float.NegativeZero)
    }

    @Test
    fun testFloat_positiveZero_equalsPositiveZero() {
        assertEqualsWithNanFix(0f, 0f)
    }

    @Test
    fun testFloat_positiveInfinity_EqualsPositiveInfinity() {
        assertEqualsWithNanFix(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    }

    @Test
    fun testFloat_negativeInfinity_doesNotEqualPositiveInfinity() {
        assertNotEqualsWithNanFix(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
    }

    @Test
    fun testFloat_negativeInfinity_equalsNegativeInfinity() {
        assertEqualsWithNanFix(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
    }

    @Test
    fun testDouble_arbitraryValueEquality() {
        assertEqualsWithNanFix(12345.0, 12345.0)
        assertEqualsWithNanFix(-98.076, -98.076)
        assertNotEqualsWithNanFix(12.34, 12.30)
    }

    @Test
    fun testDouble_inequalityFromLossOfPrecision() {
        assertNotEqualsWithNanFix(0.5, 0.01 * 5)
    }

    @Test
    fun testDouble_nanConstant_doesNotEqualItself() {
        assertNotEqualsWithNanFix(Double.NaN, Double.NaN)
    }

    @Test
    fun testDouble_nonCanonicalNans_areNotEqual() {
        val unconventionalNan = Double.fromBits(0x7FF0ABCDEF123456)
        assertNotEqualsWithNanFix(unconventionalNan, unconventionalNan)
    }

    @Test
    fun testDouble_negativeZero_doesEqualsPositiveZero() {
        assertEqualsWithNanFix(Double.NegativeZero, 0f)
    }

    @Test
    fun testDouble_negativeZero_equalsNegativeZero() {
        assertEqualsWithNanFix(Double.NegativeZero, Double.NegativeZero)
    }

    @Test
    fun testDouble_positiveZero_equalsPositiveZero() {
        assertEqualsWithNanFix(0f, 0f)
    }

    @Test
    fun testDouble_positiveInfinity_EqualsPositiveInfinity() {
        assertEqualsWithNanFix(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    }

    @Test
    fun testDouble_negativeInfinity_doesNotEqualPositiveInfinity() {
        assertNotEqualsWithNanFix(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
    }

    @Test
    fun testDouble_negativeInfinity_equalsNegativeInfinity() {
        assertEqualsWithNanFix(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
    }

    private fun assertNotEqualsWithNanFix(first: Float, second: Float) =
        assertFalse(
            message =
                "$first (${first.bitString}) == $second (${second.bitString}) " +
                    "returned true, expected false",
            actual = first == second,
        )

    private fun assertEqualsWithNanFix(first: Float, second: Float) =
        assertTrue(
            message =
                "$first (${first.bitString}) == $second (${second.bitString}) " +
                    "returned false, expected true",
            actual = first == second,
        )

    private fun assertNotEqualsWithNanFix(first: Double, second: Double) =
        assertFalse(
            message =
                "$first (${first.bitString}) == $second (${second.bitString}) " +
                    "returned true, expected false",
            actual = first == second,
        )

    private fun assertEqualsWithNanFix(first: Double, second: Double) =
        assertTrue(
            message =
                "$first (${first.bitString}) == $second (${second.bitString}) " +
                    "returned false, expected true",
            actual = first == second,
        )

    private val Float.bitString
        get() = "0x" + toBits().toUInt().toString(16).padStart(length = 8, '0')

    private val Double.bitString
        get() = "0x" + toBits().toULong().toString(16).padStart(length = 16, '0')

    private val Float.Companion.NegativeZero: Float
        get() = Float.fromBits(0b1 shl 31)

    private val Double.Companion.NegativeZero: Float
        get() = Float.fromBits(0b1 shl 63)
}
