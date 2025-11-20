/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.brush.compose

import androidx.annotation.CheckResult
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily

/**
 * The brush color as an [androidx.compose.ui.graphics.Color] value, which can express colors in
 * several different color spaces. sRGB and Display P3 are supported; a color in any other color
 * space will be converted to Display P3.
 */
public val Brush.composeColor: ComposeColor
    // Make sure to call the ULong overload of the constructor, as the Long overload is really just
    // a
    // convenience wrapper of the Int overload, which is only for the sRGB color space in ARGB
    // format
    // for cases where alpha > 0x80.
    get() = ComposeColor(colorLong.toULong())

/**
 * Creates a copy of `this` [Brush] and allows named properties to be altered while keeping the rest
 * unchanged. The color is specified as an [androidx.compose.ui.graphics.Color] value, which can
 * encode several different color spaces. sRGB and Display P3 are supported; a color in any other
 * color space will be converted to Display P3.
 */
@CheckResult
public fun Brush.copyWithComposeColor(
    color: ComposeColor,
    family: BrushFamily = this.family,
    size: Float = this.size,
    epsilon: Float = this.epsilon,
): Brush = copyWithColorLong(color.value.toLong(), family, size, epsilon)

/**
 * Set the color on a [Brush.Builder] as an [androidx.compose.ui.graphics.Color] value. sRGB and
 * Display P3 are supported; a color in any other color space will be converted to Display P3.
 */
public fun Brush.Builder.setComposeColor(color: ComposeColor): Brush.Builder =
    setColorLong(color.value.toLong())

/**
 * Returns a new [Brush] with the color specified by an [androidx.compose.ui.graphics.Color] value,
 * which can encode several different color spaces. sRGB and Display P3 are supported; a color in
 * any other color space will be converted to Display P3.
 */
@CheckResult
public fun Brush.Companion.createWithComposeColor(
    family: BrushFamily,
    color: ComposeColor,
    size: Float,
    epsilon: Float,
): Brush = createWithColorLong(family, color.value.toLong(), size, epsilon)
