/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.graphics

import android.os.Build
import androidx.annotation.ColorLong
import androidx.compose.ui.graphics.colorspace.ColorSpaces

/**
 * Converts the [Color] to a 64-bit [ColorLong] value that can be used by Android's framework.
 * [Color.value] isn't fully compatible with Android's 64-bit [ColorLong] values as some color
 * spaces differ, so this method handles the conversion. Color spaces that are not supported by the
 * current Android API level will safely fallback to the [ColorSpaces.Srgb] color space.
 */
@ColorLong
fun Color.toColorLong(): Long {
    val id = (this.value and 0x3FUL).toInt()

    if (id <= 15) return this.value.toLong()

    if (id == ColorSpaces.Unspecified.id) return this.toArgb().toLong()

    if (
        (id == ColorSpaces.Bt2020Hlg.id || id == ColorSpaces.Bt2020Pq.id) &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    ) {
        return this.toArgb().toLong()
    }

    if (id == ColorSpaces.Oklab.id && Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
        return this.toArgb().toLong()
    }

    return ((this.value and 0x3FUL.inv()) or ((this.value and 0x3FUL) - 1UL)).toLong()
}

/**
 * Creates a Color from an Android 64-bit color value. This differs from the [Color] constructor
 * accepting a [Long] in that the constructor assumes the incoming value is a 32-bit ARGB color,
 * while this is a 64-bit [ColorLong] color from Android. [Color.value] isn't fully compatible with
 * Android's 64-bit [ColorLong] values as some color spaces differ, so this method handles the
 * conversion.
 */
fun Color.Companion.fromColorLong(@ColorLong colorLong: Long): Color {
    val color =
        if (colorLong and 0x3F < 16) {
            colorLong
        } else {
            (colorLong and 0x3F.inv()) or ((colorLong and 0x3F) + 1)
        }
    return Color(color.toULong())
}

/**
 * Converts this [Color] to a platform-compatible [@ColorLong] suitable for use with
 * [android.graphics.Paint] and [android.graphics.Shader] APIs. Color spaces that are not supported
 * for rendering (e.g., CIE XYZ, CIE Lab, OkLab) are converted to [ColorSpaces.Srgb] before
 * encoding.
 */
@ColorLong
internal fun Color.toSupportedColorLong(): Long {
    return if (isColorSpaceSupported()) {
        this.toColorLong()
    } else {
        this.convert(ColorSpaces.Srgb).toColorLong()
    }
}

// Color spaces not supported for platform @ColorLong rendering operations
// (Paint, Shader, etc.)
internal fun Color.isColorSpaceSupported(): Boolean {
    val id = (this.value and 0x3FUL).toInt()
    return !(id == ColorSpaces.Oklab.id ||
        id == ColorSpaces.CieXyz.id ||
        id == ColorSpaces.CieLab.id)
}
