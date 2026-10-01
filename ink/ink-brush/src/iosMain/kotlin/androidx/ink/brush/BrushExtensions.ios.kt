/*
 * Copyright (C) 2026 The Android Open Source Project
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

@file:OptIn(InkInternalOnlyApi::class)

package androidx.ink.brush

import androidx.ink.brush.color.Color as ComposeColor
import androidx.ink.brush.color.colorspace.ColorSpaces as ComposeColorSpaces
import androidx.ink.nativeloader.InkInternalOnlyApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.UIKit.UIColor

/** The brush color as a [UIColor] instance in extended sRGB. */
public fun Brush.createUiColor(): UIColor = internalColor.toUiColor()

/**
 * Creates a copy of `this` [Brush] and allows named properties to be altered while keeping the rest
 * unchanged. The color is specified as a [UIColor] instance, which can encode several different
 * color spaces, but stores both color-spaces supported by Ink (sRGB and Display P3) colors as
 * extended sRGB, so this always converts to a Display P3 color in Ink. Will raise
 * [IllegalArgumentException] if the [UIColor] cannot be converted to extended sRGB.
 */
public fun Brush.copyWithUiColor(
    color: UIColor,
    family: BrushFamily = this.family,
    size: Float = this.size,
    epsilon: Float = this.epsilon,
): Brush = copy(family = family, color = color.toComposeColor(), size, epsilon)

/**
 * Returns a new [Brush] with the color specified by a [UIColor] instance. The color is specified as
 * a [UIColor] instance, which can encode several different color spaces, but stores both
 * color-spaces supported by Ink (sRGB and Display P3) colors as extended sRGB, so this always
 * converts to a Display P3 color in Ink. Will raise [IllegalArgumentException] if the [UIColor]
 * cannot be converted to extended sRGB.
 */
public fun Brush.Companion.createWithUiColor(
    family: BrushFamily,
    color: UIColor,
    size: Float,
    epsilon: Float,
): Brush =
    Brush(family = family, composeColor = color.toComposeColor(), size = size, epsilon = epsilon)

@OptIn(ExperimentalForeignApi::class)
internal fun UIColor.toComposeColor(): ComposeColor {
    val components = DoubleArray(4)
    components.usePinned { pinned ->
        require(
            this@toComposeColor.getRed(
                red = pinned.addressOf(0),
                green = pinned.addressOf(1),
                blue = pinned.addressOf(2),
                alpha = pinned.addressOf(3),
            )
        ) {
            "UIColor cannot be converted to extended sRGB."
        }
    }
    return ComposeColor(
            red = components[0].toFloat(),
            green = components[1].toFloat(),
            blue = components[2].toFloat(),
            alpha = components[3].toFloat(),
            colorSpace = ComposeColorSpaces.ExtendedSrgb,
        )
        .convert(ComposeColorSpaces.DisplayP3)
}

@OptIn(ExperimentalForeignApi::class)
internal fun ComposeColor.toUiColor(): UIColor =
    convert(ComposeColorSpaces.DisplayP3).let {
        UIColor.colorWithDisplayP3Red(
            displayP3Red = it.red.toDouble(),
            green = it.green.toDouble(),
            blue = it.blue.toDouble(),
            alpha = it.alpha.toDouble(),
        )
    }
