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

class MathUtilTest {
    @Test
    fun floatEquals() {
        assertThat(equalWithinTolerance(1.3f, 1.3f, 0.00000000000001f)).isTrue()
        assertThat(equalWithinTolerance(1.3f, 1.3f, 0.0f)).isTrue()
        assertThat(
            equalWithinTolerance(
                0.0f,
                1.0f + 2.0f - 3.0f,
                0.00000000000000000000000000000001f
            )
        )
            .isTrue()
        assertThat(equalWithinTolerance(1.3f, 1.303f, 0.004f)).isTrue()
        assertThat(equalWithinTolerance(1.3f, 1.303f, 0.002f)).isFalse()
        assertThat(equalWithinTolerance(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0.01f))
            .isFalse()
        assertThat(equalWithinTolerance(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.01f))
            .isFalse()
        assertThat(equalWithinTolerance(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.01f))
            .isFalse()
        assertThat(equalWithinTolerance(Float.NaN, Float.NaN, 0.01f)).isFalse()
    }

    @Test
    fun doubleEquals() {
        assertThat(equalWithinTolerance(1.3, 1.3, 0.00000000000001)).isTrue()
        assertThat(equalWithinTolerance(1.3, 1.3, 0.0)).isTrue()
        assertThat(equalWithinTolerance(0.0, 1.0 + 2.0 - 3.0, 0.00000000000000000000000000000001))
            .isTrue()
        assertThat(equalWithinTolerance(1.3, 1.303, 0.004)).isTrue()
        assertThat(equalWithinTolerance(1.3, 1.303, 0.002)).isFalse()
        assertThat(equalWithinTolerance(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.01))
            .isFalse()
        assertThat(equalWithinTolerance(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.01))
            .isFalse()
        assertThat(equalWithinTolerance(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.01))
            .isFalse()
        assertThat(equalWithinTolerance(Double.NaN, Double.NaN, 0.01)).isFalse()
    }

    @Test
    fun floatNotEquals() {
        assertThat(notEqualWithinTolerance(1.3f, 1.3f, 0.00000000000001f)).isFalse()
        assertThat(notEqualWithinTolerance(1.3f, 1.3f, 0.0f)).isFalse()
        assertThat(
            notEqualWithinTolerance(0.0f, 1.0f + 2.0f - 3.0f, 0.00000000000000000000000000000001f)
        )
            .isFalse()
        assertThat(notEqualWithinTolerance(1.3f, 1.303f, 0.004f)).isFalse()
        assertThat(notEqualWithinTolerance(1.3f, 1.303f, 0.002f)).isTrue()
        assertThat(notEqualWithinTolerance(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0.01f))
            .isFalse()
        assertThat(notEqualWithinTolerance(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.01f))
            .isFalse()
        assertThat(notEqualWithinTolerance(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.01f))
            .isFalse()
        assertThat(notEqualWithinTolerance(Float.NaN, Float.NaN, 0.01f)).isFalse()
    }

    @Test
    fun doubleNotEquals() {
        assertThat(notEqualWithinTolerance(1.3, 1.3, 0.00000000000001)).isFalse()
        assertThat(notEqualWithinTolerance(1.3, 1.3, 0.0)).isFalse()
        assertThat(
            notEqualWithinTolerance(0.0, 1.0 + 2.0 - 3.0, 0.00000000000000000000000000000001)
        )
            .isFalse()
        assertThat(notEqualWithinTolerance(1.3, 1.303, 0.004)).isFalse()
        assertThat(notEqualWithinTolerance(1.3, 1.303, 0.002)).isTrue()
        assertThat(
            notEqualWithinTolerance(
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                0.01
            )
        )
            .isFalse()
        assertThat(
            notEqualWithinTolerance(
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                0.01
            )
        )
            .isFalse()
        assertThat(
            notEqualWithinTolerance(
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                0.01
            )
        )
            .isFalse()
        assertThat(notEqualWithinTolerance(Double.NaN, Double.NaN, 0.01)).isFalse()
    }
}
