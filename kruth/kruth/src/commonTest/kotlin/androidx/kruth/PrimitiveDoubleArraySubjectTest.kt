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

import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PrimitiveDoubleArraySubjectTest {

    @Test
    fun testDoubleConstants_matchNextAfter() {
        assertThat((2.0 + DEFAULT_TOLERANCE).nextDown()).isEqualTo(TOLERABLE_2)
        assertThat((2.2 + DEFAULT_TOLERANCE).nextDown()).isEqualTo(TOLERABLE_2POINT2)
        assertThat((2.2 + DEFAULT_TOLERANCE).nextUp()).isEqualTo(INTOLERABLE_2POINT2)
        assertThat(2.2.nextUp()).isEqualTo(OVER_2POINT2)
        assertThat((3.3 + DEFAULT_TOLERANCE).nextDown()).isEqualTo(TOLERABLE_3POINT3)
        assertThat((3.3 + DEFAULT_TOLERANCE).nextUp()).isEqualTo(INTOLERABLE_3POINT3)
        assertThat(Long.MIN_VALUE.toDouble().nextDown()).isEqualTo(UNDER_MIN_OF_LONG)
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Success() {
        assertThat(doubleArrayOf(2.2, 5.4, POSITIVE_INFINITY, NEGATIVE_INFINITY, 0.0, -0.0))
            .isEqualTo(doubleArrayOf(2.2, 5.4, POSITIVE_INFINITY, NEGATIVE_INFINITY, 0.0, -0.0))
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_NaN_Success() {
        val actual = doubleArrayOf(2.2, 5.4, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN, 0.0, -0.0)
        val expected = doubleArrayOf(2.2, 5.4, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN, 0.0, -0.0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_NotEqual() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(2.2)).isEqualTo(doubleArrayOf(OVER_2POINT2))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_DifferentOrder() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(2.2, 3.3)).isEqualTo(doubleArrayOf(3.3, 2.2))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_Longer() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(2.2, 3.3)).isEqualTo(doubleArrayOf(2.2, 3.3, 4.4))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_Shorter() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(2.2, 3.3)).isEqualTo(doubleArrayOf(2.2))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_PlusMinusZero() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(0.0)).isEqualTo(doubleArrayOf(-0.0))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_NotAndoubleArrayOf() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(2.2, 3.3, 4.4)).isEqualTo(Any())
        }
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_FailEquals() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(2.2, 5.4, POSITIVE_INFINITY, NEGATIVE_INFINITY))
                .isNotEqualTo(doubleArrayOf(2.2, 5.4, POSITIVE_INFINITY, NEGATIVE_INFINITY))
        }
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_NaN_plusZero_FailEquals() {
        val actual = doubleArrayOf(2.2, 5.4, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN, 0.0, -0.0)
        val expected = doubleArrayOf(2.2, 5.4, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN, 0.0, -0.0)
        assertFailsWith<AssertionError> {
            assertThat(actual).isNotEqualTo(expected)
        }
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_NotEqual() {
        assertThat(doubleArrayOf(2.2)).isNotEqualTo(doubleArrayOf(OVER_2POINT2))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_DifferentOrder() {
        assertThat(doubleArrayOf(2.2, 3.3)).isNotEqualTo(doubleArrayOf(3.3, 2.2))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_Longer() {
        assertThat(doubleArrayOf(2.2, 3.3)).isNotEqualTo(doubleArrayOf(2.2, 3.3, 4.4))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_Shorter() {
        assertThat(doubleArrayOf(2.2, 3.3)).isNotEqualTo(doubleArrayOf(2.2))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_PlusMinusZero() {
        assertThat(doubleArrayOf(0.0)).isNotEqualTo(doubleArrayOf(-0.0))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_NotAndoubleArrayOf() {
        assertThat(doubleArrayOf(2.2, 3.3, 4.4)).isNotEqualTo(Any())
    }

    @Test
    fun smallDifferenceInLongRepresentation() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(-4.4501477170144023E-308))
                .isEqualTo(doubleArrayOf(-4.450147717014402E-308))
        }
    }

    @Test
    fun noCommas() {
        assertFailsWith<AssertionError> {
            assertThat(doubleArrayOf(10000.0)).isEqualTo(doubleArrayOf(20000.0))
        }
    }

    private companion object {
        private const val DEFAULT_TOLERANCE = 0.000005
        private const val OVER_2POINT2 = 2.2000000000000006
        private const val TOLERABLE_2 = 2.0000049999999994
        private const val TOLERABLE_2POINT2 = 2.2000049999999995
        private const val INTOLERABLE_2POINT2 = 2.2000050000000004
        private const val TOLERABLE_3POINT3 = 3.300004999999999
        private const val INTOLERABLE_3POINT3 = 3.300005
        private const val UNDER_MIN_OF_LONG = -9.223372036854778E18
    }
}
