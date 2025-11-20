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

import androidx.annotation.ColorLong

/**
 * Converts the Color to a 64-bit [ColorLong] value that can be used by Android's framework.
 * [Color.value] isn't fully compatible with Android's 64-bit [ColorLong] values as some color
 * spaces differ, so this method handles the conversion.
 */
@ColorLong
fun Color.toColorLong(): Long {
    return if ((value and 0x3FUL) < 16UL) {
            value
        } else {
            (value and 0x3FUL.inv()) or ((value and 0x3FUL) - 1UL)
        }
        .toLong()
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
