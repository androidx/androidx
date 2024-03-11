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

import kotlin.Float.Companion.NEGATIVE_INFINITY
import kotlin.Float.Companion.NaN
import kotlin.Float.Companion.POSITIVE_INFINITY
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PrimitiveFloatArraySubjectTest {

    @Test
    fun testFloatConstants_matchNextAfter() {
        assertThat(2.2F.nextUp()).isEqualTo(JUST_OVER_2POINT2)
        assertThat(3.3f.nextUp()).isEqualTo(JUST_OVER_3POINT3)
        assertThat((3.3f + DEFAULT_TOLERANCE).nextDown()).isEqualTo(TOLERABLE_3POINT3)
        assertThat((3.3f + DEFAULT_TOLERANCE).nextUp()).isEqualTo(INTOLERABLE_3POINT3)
        assertThat(Long.MIN_VALUE.toFloat().nextDown()).isEqualTo(UNDER_LONG_MIN)
        assertThat((2.2f + DEFAULT_TOLERANCE).nextDown()).isEqualTo(TOLERABLE_2POINT2)
        assertThat((2.2f + DEFAULT_TOLERANCE).nextUp()).isEqualTo(INTOLERABLE_2POINT2)
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Success() {
        val actual =
            floatArrayOf(2.2f, 5.4f, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN, 0.0f, -0.0f)
        val expected =
            floatArrayOf(2.2f, 5.4f, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN, 0.0f, -0.0f)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_NotEqual() {
        assertFailsWith<AssertionError> {
            assertThat(floatArrayOf(2.2f)).isEqualTo(floatArrayOf(JUST_OVER_2POINT2))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_DifferentOrder() {
        assertFailsWith<AssertionError> {
            assertThat(floatArrayOf(2.2f, 3.3f)).isEqualTo(floatArrayOf(3.3f, 2.2f))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_Longer() {
        assertFailsWith<AssertionError> {
            assertThat(floatArrayOf(2.2f, 3.3f)).isEqualTo(floatArrayOf(2.2f, 3.3f, 4.4f))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_Shorter() {
        assertFailsWith<AssertionError> {
            assertThat(floatArrayOf(2.2f, 3.3f)).isEqualTo(floatArrayOf(2.2f))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_PlusMinusZero() {
        assertFailsWith<AssertionError> {
            assertThat(floatArrayOf(0.0f)).isEqualTo(floatArrayOf(-0.0f))
        }
    }

    @Test
    fun isEqualTo_WithoutToleranceParameter_Fail_NotAnfloatArrayOf() {
        assertFailsWith<AssertionError> {
            assertThat(floatArrayOf(2.2f, 3.3f, 4.4f)).isEqualTo(Any())
        }
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_FailEquals() {
        val actual =
            floatArrayOf(2.2f, 5.4f, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN, 0.0f, -0.0f)
        val expected =
            floatArrayOf(2.2f, 5.4f, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN, 0.0f, -0.0f)
        assertFailsWith<AssertionError> {
            assertThat(actual).isNotEqualTo(expected)
        }
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_NotEqual() {
        assertThat(floatArrayOf(2.2f)).isNotEqualTo(floatArrayOf(JUST_OVER_2POINT2))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_DifferentOrder() {
        assertThat(floatArrayOf(2.2f, 3.3f)).isNotEqualTo(floatArrayOf(3.3f, 2.2f))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_Longer() {
        assertThat(floatArrayOf(2.2f, 3.3f)).isNotEqualTo(floatArrayOf(2.2f, 3.3f, 4.4f))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_Shorter() {
        assertThat(floatArrayOf(2.2f, 3.3f)).isNotEqualTo(floatArrayOf(2.2f))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_PlusMinusZero() {
        assertThat(floatArrayOf(0.0f)).isNotEqualTo(floatArrayOf(-0.0f))
    }

    @Test
    fun isNotEqualTo_WithoutToleranceParameter_Success_NotAnfloatArrayOf() {
        assertThat(floatArrayOf(2.2f, 3.3f, 4.4f)).isNotEqualTo(Any())
    }

    private companion object {
        private const val DEFAULT_TOLERANCE = 0.000005f
        private const val JUST_OVER_2POINT2 = 2.2000003f
        private const val JUST_OVER_3POINT3 = 3.3000002f
        private const val TOLERABLE_3POINT3 = 3.3000047f
        private const val INTOLERABLE_3POINT3 = 3.3000052f
        private const val UNDER_LONG_MIN = -9.223373E18f
        private const val TOLERABLE_2POINT2 = 2.2000048f
        private const val INTOLERABLE_2POINT2 = 2.2000053f
    }
}
