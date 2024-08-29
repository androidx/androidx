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

package androidx.compose.animation.core

import androidx.compose.ui.util.floatFromBits
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EasingUnitTest {

    @Test
    fun cubicBezierStartsAt0() {
        val easing = FastOutSlowInEasing
        assertThat(easing.transform(0f)).isZero()
    }

    @Test
    fun cubicBezierEndsAt1() {
        val easing = FastOutLinearInEasing
        assertThat(easing.transform(1f)).isEqualTo(1.0f)
    }

    @Test
    fun cubicBezierDoesntExceed1() {
        val easing = CubicBezierEasing(0f, 0f, 0.15f, 1f)
        assertThat(easing.transform(0.999999f) <= 1.0f).isTrue()
    }

    @Test
    fun cubicBezierDoesExceed1() {
        val easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
        assertThat(easing.transform(0.6f)).isGreaterThan(1.0f)
    }

    @Test
    fun cubicBezierEquals() {
        val curve1 = CubicBezierEasing(1f, 2f, 3f, 4f)
        val curve1Dup = CubicBezierEasing(1f, 2f, 3f, 4f)
        val curve2 = CubicBezierEasing(0f, 2f, 3f, 4f)
        val curve3 = CubicBezierEasing(1f, 0f, 3f, 4f)
        val curve4 = CubicBezierEasing(1f, 2f, 0f, 4f)
        val curve5 = CubicBezierEasing(1f, 2f, 3f, 0f)
        val curve6 = CubicBezierEasing(4f, 3f, 2f, 1f)

        assertEquals(curve1, curve1Dup)
        assertNotEquals(curve1, curve2)
        assertNotEquals(curve1, curve3)
        assertNotEquals(curve1, curve4)
        assertNotEquals(curve1, curve5)
        assertNotEquals(curve1, curve6)

        assertEquals(curve1.hashCode(), curve1Dup.hashCode())
        assertNotEquals(curve1.hashCode(), curve2.hashCode())
        assertNotEquals(curve1.hashCode(), curve3.hashCode())
        assertNotEquals(curve1.hashCode(), curve4.hashCode())
        assertNotEquals(curve1.hashCode(), curve5.hashCode())
        assertNotEquals(curve1.hashCode(), curve6.hashCode())
    }

    @Test
    fun canSolveCubicForSmallFractions() {
        val curve = CubicBezierEasing(0.3f, 0.0f, 0.7f, 1.0f)

        val testValues =
            intArrayOf(
                0x3e800000, // 0.25f
                0x3e000000, // 0.125f
                0x3d800000, // 0.0625f
                0x3a800000, // 0.0009765625f
                0x36000000, // 0.0000019073486328125f
                0x34800000, // 2.384185791015625e-7f
                // Values from here are below the epsilon we use in our computations
                0x34000000, // 1.1920928955078125e-7f
                0x34210fb0, // 1.50000005305628292263e-7f
                0x33800000, // 5.9604644775390625e-8f
                0x33000000, // 2.98023223876953125e-8f
                0x00000000, // 0.0f
            )

        for (i in testValues) {
            val t = curve.transform(floatFromBits(i))
            assertTrue(t in 0.0f..1.0f)
        }
    }
}
