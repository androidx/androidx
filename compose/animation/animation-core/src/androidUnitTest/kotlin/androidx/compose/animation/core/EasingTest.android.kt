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
import kotlin.math.ulp
import kotlin.test.Test
import kotlin.test.assertTrue

// This test can't be in commonTest because
// Float.ulp is jvm only: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.math/ulp.html
class EasingTestAndroid {
    private val ZeroEpsilon = -(1.0f.ulp * 2.0f)
    private val OneEpsilon = 1.0f + 1.0f.ulp * 2.0f

    @Test
    fun canSolveCubicForFractionsCloseToOne() {
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
                EaseOutCirc,
                EaseOutCubic,
                EaseOutExpo,
                EaseOutQuad,
                EaseOutQuart,
                EaseOutQuint,
                EaseOutSine
            )

        for (curve in curves) {
            // Test the last 16 ulps until 1.0f
            for (i in 0x3f7ffff0..0x3f7fffff) {
                val fraction = floatFromBits(i)
                val t = curve.transform(fraction)
                assertTrue(
                    t in -ZeroEpsilon..OneEpsilon,
                    "f($fraction) = $t out of range for $curve | ${-ZeroEpsilon}..${OneEpsilon}"
                )
            }
        }
    }
}
