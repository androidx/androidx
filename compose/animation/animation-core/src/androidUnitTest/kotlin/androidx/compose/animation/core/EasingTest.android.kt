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
import kotlin.math.max
import kotlin.math.ulp
import kotlin.test.Test
import kotlin.test.assertTrue

// This test can't be in commonTest because
// Float.ulp is jvm only: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.math/ulp.html
class EasingTestAndroid {
    private val ZeroEpsilon = 1.0f.ulp * 2.0f
    private val OneEpsilon = 1.0f + 1.0f.ulp * 2.0f

    @Test
    fun canSolveCubicForFractionsCloseToZeroOrOne() {
        // Only test curves defined in [0..1]
        // For instance, EaseInOutBack is defined in a larger domain, so exclude it from the list
        val curves =
            listOf(
                CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f),
                Ease,
                EaseIn,
                EaseInBack,
                EaseInCirc,
                EaseInCubic,
                EaseInExpo,
                EaseInOut,
                EaseInOutCirc,
                EaseInOutCubic,
                EaseInOutExpo,
                EaseInOutQuad,
                EaseInOutQuart,
                EaseInOutQuint,
                EaseInOutSine,
                EaseInOutQuad,
                EaseInOutQuart,
                EaseInOutQuint,
                EaseInSine,
                EaseOut,
                // Not included because it overshoots 1.0f on purpose, so it can't be tested the
                // same way as the other curves. See canSolveOvershootingCurve()
                // EaseOutBack,
                EaseOutCirc,
                EaseOutCubic,
                EaseOutExpo,
                EaseOutQuad,
                EaseOutQuart,
                EaseOutQuint,
                EaseOutSine
            )

        for (curve in curves) {
            for (i in 0x3f7f9d99..0x3f7fffff) {
                val fraction = floatFromBits(i)
                val t = curve.transform(fraction)
                assertTrue(
                    t in -ZeroEpsilon..OneEpsilon,
                    "f($fraction) = $t out of range for $curve | ${-ZeroEpsilon}..${OneEpsilon}"
                )
            }

            for (i in 0x0..0x6266) {
                val fraction = floatFromBits(i)
                val t = curve.transform(fraction)
                assertTrue(
                    t in -ZeroEpsilon..OneEpsilon,
                    "f($fraction) = $t out of range for $curve | ${-ZeroEpsilon}..${OneEpsilon}"
                )
            }

            // Test at 1.5058824E-7, small value but not too close to 0.0 either
            val fraction = floatFromBits(0x3421b161)
            val t = curve.transform(fraction)
            assertTrue(
                t in -ZeroEpsilon..OneEpsilon,
                "f($fraction) = $t out of range for $curve | ${-ZeroEpsilon}..${OneEpsilon}"
            )
        }
    }

    @Test
    fun canSolveOvershootingCurve() {
        // We only really care that we don't throw an exception
        var t = Float.MIN_VALUE
        for (i in 0x3f7f9d99..0x3f7fffff) {
            val fraction = floatFromBits(i)
            t = max(t, EaseOutBack.transform(fraction))
        }
        assertTrue(t > Float.MIN_VALUE)
    }
}
