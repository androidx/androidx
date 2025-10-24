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

package androidx.compose.animation.core

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PathEasingTest {
    @Test
    fun pathEasing_Emphasized_BoundsCheck() {
        val path =
            Path().apply {
                moveTo(0f, 0f)
                cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
                cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
            }

        val easing = PathEasing(path)
        assertThat(easing.transform(0f)).isZero()
        assertThat(easing.transform(1f)).isEqualTo(1f)

        assertEquals(0.77283f, easing.transform(0.25f), 1e-4f)
        assertEquals(0.95061f, easing.transform(0.50f), 1e-4f)
        assertEquals(0.99139f, easing.transform(0.75f), 1e-4f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pathEasing_EmptyPath_InvalidPath() {
        val emptyPath = Path()
        PathEasing(emptyPath).transform(0.5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pathEasing_DoesNotStartAtZero() {
        val path =
            Path().apply {
                moveTo(0.1f, 0.0f)
                lineTo(1.0f, 1.0f)
            }
        PathEasing(path).transform(0.5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pathEasing_DoesNotEndAtOne() {
        val path = Path().apply { lineTo(0.9f, 1.0f) }
        PathEasing(path).transform(0.5f)
    }

    @Test
    fun pathEasing_CompareToCubicEasing() {
        val cubicEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        val path = Path().apply { cubicTo(0.4f, 0.0f, 0.2f, 1.0f, 1.0f, 1.0f) }

        val easing = PathEasing(path)
        for (i in 0..256) {
            val fraction = i / 256f
            assertEquals(cubicEasing.transform(fraction), easing.transform(fraction), 1e-6f)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun pathEasing_NonContinuousPath() {
        val path =
            Path().apply {
                moveTo(0.00f, 0.10f)
                lineTo(0.25f, 0.10f)
                // Gap from 0.25 to 0.50
                moveTo(0.50f, 0.40f)
                lineTo(0.75f, 0.40f)
                moveTo(0.75f, 1.00f)
                lineTo(1.00f, 1.00f)
            }

        val easing = PathEasing(path)
        assertEquals(0.1f, easing.transform(0.2f))
        // Crash
        easing.transform(0.4f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pathEasing_ClosedPath() {
        val path = Path().apply { addOval(Rect(0f, 0f, 1f, 1f)) }

        PathEasing(path).transform(0.5f)
    }

    @Test
    fun pathEasing_Overlapping_Curves() {
        val path =
            Path().apply {
                moveTo(0.00f, 0.10f)
                lineTo(0.25f, 0.10f)
                moveTo(0.10f, 0.30f) // Overlaps with the previous line
                lineTo(0.60f, 0.30f) // and the next line
                moveTo(0.50f, 0.40f)
                lineTo(0.75f, 0.40f)
                moveTo(0.75f, 1.00f)
                lineTo(1.00f, 1.00f)
            }

        val easing = PathEasing(path)

        // We don't specify which overlapping curve will be evaluated first
        assertThat(easing.transform(0.2f)).isAnyOf(0.10f, 0.30f)
    }

    @Test
    fun pathEasing_QuadTo() {
        val path = Path().apply { quadraticTo(1.0f, 0.0f, 1.0f, 1.0f) }

        val easing = PathEasing(path)
        var previousFraction = -Float.MAX_VALUE
        for (i in 0..256) {
            val fraction = i / 256f
            val newFraction = easing.transform(fraction)

            assertThat(newFraction).isAtLeast(0.0f)
            assertThat(newFraction).isGreaterThan(previousFraction)

            previousFraction = newFraction
        }
    }

    @Test
    fun pathEasing_QuadTo_OneToZero() {
        val path =
            Path().apply {
                moveTo(1.0f, 1.0f)
                quadraticTo(1.0f, 0.0f, 0.0f, 0.0f)
            }

        val easing = PathEasing(path)
        var previousFraction = -Float.MAX_VALUE
        for (i in 0..256) {
            val fraction = i / 256f
            val newFraction = easing.transform(fraction)

            assertThat(newFraction).isAtLeast(0.0f)
            assertThat(newFraction).isGreaterThan(previousFraction)

            previousFraction = newFraction
        }
    }

    @Test
    fun pathEasing_Overshoots() {
        val path = Path().apply { cubicTo(0.34f, 1.56f, 0.64f, 1.0f, 1.0f, 1.0f) }
        assertThat(PathEasing(path).transform(0.6f)).isGreaterThan(1.0f)
    }

    @Test
    fun pathEasing_Undershoots() {
        val path = Path().apply { cubicTo(0.68f, -0.6f, 0.32f, 1.6f, 1.0f, 1.0f) }
        assertThat(PathEasing(path).transform(0.1f)).isLessThan(0.0f)
    }
}
