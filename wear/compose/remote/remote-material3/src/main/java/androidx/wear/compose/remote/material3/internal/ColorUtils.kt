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

package androidx.wear.compose.remote.material3.internal

import androidx.annotation.FloatRange
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.pow
import androidx.compose.remote.creation.compose.state.rf

/**
 * Returns a copy of this color with its luminance (tone) set to [newLuminance], keeping its alpha.
 *
 * This is the remote equivalent of `Color.setLuminance` in Wear Compose Material 3, which sets the
 * HCT tone. HCT can't be evaluated by the player, so instead this color is scaled in linear sRGB
 * until its CIE L* equals [newLuminance]. The tone matches HCT and the hue is kept, but chroma can
 * differ from HCT for saturated colors. For neutral colors, such as the default `onBackground`, the
 * result matches HCT. Channels are clamped to 1, so brightening a saturated color can give a lower
 * tone than requested. Black has no hue, so like HCT it becomes the gray of [newLuminance].
 *
 * A constant color gives a constant color. Other colors, such as named or themed colors, are
 * evaluated by the player, so the result follows changes to them.
 *
 * @param newLuminance 0 <= newLuminance <= 100; invalid values are corrected.
 */
internal fun RemoteColor.setLuminance(
    @FloatRange(from = 0.0, to = 100.0) newLuminance: Float
): RemoteColor {
    val targetY = lStarToY(newLuminance.coerceIn(0f, 100f)).rf
    val linearRed = red.srgbToLinear()
    val linearGreen = green.srgbToLinear()
    val linearBlue = blue.srgbToLinear()
    val y =
        linearRed * RedLuminanceWeight.rf +
            linearGreen * GreenLuminanceWeight.rf +
            linearBlue * BlueLuminanceWeight.rf
    val isBlack = y.isLessThan(MinLuminance.rf)
    val scale = targetY / max(y, MinLuminance.rf)

    fun RemoteFloat.withTargetLuminance(): RemoteFloat =
        isBlack.select(targetY, min(this * scale, 1f.rf)).linearToSrgb()

    return RemoteColor.rgb(
        red = linearRed.withTargetLuminance(),
        green = linearGreen.withTargetLuminance(),
        blue = linearBlue.withTargetLuminance(),
        alpha = alpha,
    )
}

/** Converts a CIE L* value in [0, 100] to relative luminance Y in [0, 1]. */
private fun lStarToY(lStar: Float): Float =
    if (lStar > 8f) {
        ((lStar + 16f) / 116f).let { it * it * it }
    } else {
        lStar / CieKappa
    }

/** Converts an sRGB channel in [0, 1] to linear sRGB. */
private fun RemoteFloat.srgbToLinear(): RemoteFloat =
    isLessThanOrEqualTo(0.04045f.rf)
        .select(this / 12.92f.rf, pow((this + 0.055f.rf) / 1.055f.rf, 2.4f.rf))

/** Converts a linear sRGB channel in [0, 1] to sRGB. */
private fun RemoteFloat.linearToSrgb(): RemoteFloat =
    isLessThanOrEqualTo(0.0031308f.rf)
        .select(this * 12.92f.rf, pow(this, (1f / 2.4f).rf) * 1.055f.rf - 0.055f.rf)

// Relative luminance weights of linear sRGB channels.
private const val RedLuminanceWeight = 0.2126f
private const val GreenLuminanceWeight = 0.7152f
private const val BlueLuminanceWeight = 0.0722f

// CIE constant used to convert L* to Y for very dark tones.
private const val CieKappa = 24389f / 27f

// Relative luminance below which a color is treated as black.
private const val MinLuminance = 1e-4f
