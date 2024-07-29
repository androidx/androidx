/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PointTest {

    @Test
    fun isAlmostEqual_whenNoToleranceGiven_returnsCorrectValue() {
        val point = ImmutablePoint(1f, 2f)

        assertThat(point.isAlmostEqual(point)).isTrue()
        assertThat(point.isAlmostEqual(ImmutablePoint(1f, 2f))).isTrue()
        assertThat(point.isAlmostEqual(ImmutablePoint(1.00001f, 1.99999f))).isTrue()
        assertThat(point.isAlmostEqual(ImmutablePoint(1f, 1.99f))).isFalse()
        assertThat(point.isAlmostEqual(ImmutablePoint(1.01f, 2f))).isFalse()
        assertThat(point.isAlmostEqual(ImmutablePoint(1.01f, 1.99f))).isFalse()
    }

    @Test
    fun isAlmostEqual_withToleranceGiven_returnsCorrectValue() {
        val point = ImmutablePoint(1f, 2f)

        assertThat(point.isAlmostEqual(point, tolerance = 0.00000001f)).isTrue()
        assertThat(point.isAlmostEqual(ImmutablePoint(1f, 2f), tolerance = 0.00000001f)).isTrue()
        assertThat(point.isAlmostEqual(ImmutablePoint(1.00001f, 1.99999f), tolerance = 0.000001f))
            .isFalse()
        assertThat(point.isAlmostEqual(ImmutablePoint(1f, 1.99f), tolerance = 0.02f)).isTrue()
        assertThat(point.isAlmostEqual(ImmutablePoint(1.01f, 2f), tolerance = 0.02f)).isTrue()
        assertThat(point.isAlmostEqual(ImmutablePoint(1.01f, 1.99f), tolerance = 0.02f)).isTrue()
        assertThat(point.isAlmostEqual(ImmutablePoint(2.5f, 0.5f), tolerance = 2f)).isTrue()
    }

    @Test
    fun isAlmostEqual_whenSameInterface_returnsTrue() {
        val point = MutablePoint(1f, 2f)
        val other = ImmutablePoint(0.99999f, 2.00001f)
        assertThat(point.isAlmostEqual(other)).isTrue()
    }
}
