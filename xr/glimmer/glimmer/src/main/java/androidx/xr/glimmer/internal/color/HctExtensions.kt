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

import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Returns a new [Color] with the given chroma and tone, keeping the hue of this color, based on HCT
 * color space calculations.
 *
 * @param newTone Perceptual lightness L* in L*a*b* space, ranging from 0.0 (black) to 100.0
 *   (white).
 * @param newChroma Chroma value in HCT color space. Defaults to [MAX_CHROMA] for maximized chroma.
 * @return A new Compose [Color] representing the solved HCT value.
 */
internal fun Color.withToneAndChroma(
    @FloatRange(from = 0.0, to = 100.0) newTone: Float,
    @FloatRange(from = 0.0, to = 200.0) newChroma: Float = MAX_CHROMA,
): Color {
    val hue = HctUtils.argbToHue(toArgb())
    val solvedArgb = HctSolver.solveToInt(hue, newChroma.toDouble(), newTone.toDouble())
    return Color(solvedArgb).copy(alpha = this.alpha)
}

internal const val MAX_CHROMA = 200.0f
