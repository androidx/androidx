/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.internal.color

/**
 * In traditional color spaces, a color can be identified solely by the observer's measurement of
 * the color. Color appearance models such as CAM16 also use information about the environment where
 * the color was observed, known as the viewing conditions.
 *
 * For example, white under the traditional assumption of a midday sun white point is accurately
 * measured as a slightly chromatic blue by CAM16. (roughly, hue 203, chroma 3, lightness 100)
 *
 * Since we only require the default sRGB viewing conditions (background L* of 50.0), we hardcode
 * these pre-computed constants for performance and efficiency, completely avoiding runtime
 * calculation overhead.
 */
internal object ViewingConditions {
    /** Adapting luminance n coefficient */
    const val n = 0.18418651851244416

    /** Achromatic response of white point */
    const val aw = 29.98099719444734

    /** Background induction factor nbb */
    const val nbb = 1.0169191804458757

    /** Background induction factor ncb */
    const val ncb = 1.0169191804458757

    /** Exponential nonlinearity c */
    const val c = 0.69

    /** Surround coefficient nc */
    const val nc = 1.0

    /** Luminance level adaptation factor fl */
    const val fl = 0.3884814537800353

    /** Exponential factor z */
    const val z = 1.909169568483652

    /** Chromatic adaptation transform factor rgbD */
    val rgbD = doubleArrayOf(1.02117770275752, 0.9863077294280124, 0.9339605082802299)
}
