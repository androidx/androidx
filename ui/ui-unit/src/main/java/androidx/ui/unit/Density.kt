/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.unit

import android.content.Context
import androidx.ui.geometry.Rect

/**
 * Creates a [Density] for this [Context].
 *
 * @param context density values will be extracted from this [Context]
 */
fun Density(context: Context): Density =
    Density(
        context.resources.displayMetrics.density,
        context.resources.configuration.fontScale
    )

/**
 * A density of the screen. Used for convert [Dp] to pixels.
 *
 * @param density The logical density of the display. This is a scaling factor for the [Dp] unit.
 * @param fontScale Current user preference for the scaling factor for fonts.
 */
fun Density(density: Float, fontScale: Float = 1f): Density =
    DensityImpl(density, fontScale)

private data class DensityImpl(
    override val density: Float,
    override val fontScale: Float
) : Density

/**
 * A density of the screen. Used for the conversions between [Dp], [Px], [IntPx] and [TextUnit].
 *
 * @sample androidx.ui.unit.samples.WithDensitySample
 */
interface Density {

    /**
     * The logical density of the display. This is a scaling factor for the [Dp] unit.
     */
    val density: Float

    /**
     * Current user preference for the scaling factor for fonts.
     */
    val fontScale: Float

    /**
     * Convert [Dp] to [Px]. Pixels are used to paint to Canvas.
     */
    fun Dp.toPx(): Px = Px(value * density)

    /**
     * Convert [Dp] to [IntPx] by rounding
     */
    fun Dp.toIntPx(): IntPx = toPx().round()

    /**
     * Convert [Dp] to Sp. Sp is used for font size, etc.
     */
    fun Dp.toSp(): TextUnit = TextUnit.Sp(value / fontScale)

    /**
     * Convert Sp to [Px]. Pixels are used to paint to Canvas.
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    fun TextUnit.toPx(): Px {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return Px(value * fontScale * density)
    }

    /**
     * Convert Sp to [IntPx] by rounding
     */
    fun TextUnit.toIntPx(): IntPx = toPx().round()

    /**
     * Convert Sp to [Dp].
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    fun TextUnit.toDp(): Dp {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return Dp(value * fontScale)
    }

    /**
     * Convert [Px] to [Dp].
     */
    fun Px.toDp(): Dp = (value / density).dp

    /**
     * Convert [Px] to Sp.
     */
    fun Px.toSp(): TextUnit = (value / (fontScale * density)).sp

    /**
     * Convert [IntPx] to [Dp].
     */
    fun IntPx.toDp(): Dp = (value / density).dp

    /**
     * Convert [IntPx] to Sp.
     */
    fun IntPx.toSp(): TextUnit = (value / (fontScale * density)).sp

    /** Convert a [Float] pixel value to a Dp */
    fun Float.toDp(): Dp = (this / density).dp

    /** Convert a [Float] pixel value to a Sp */
    fun Float.toSp(): TextUnit = (this / (fontScale * density)).sp

    /** Convert a [Int] pixel value to a Dp */
    fun Int.toDp(): Dp = toFloat().toDp()

    /** Convert a [Int] pixel value to a Sp */
    fun Int.toSp(): TextUnit = toFloat().toSp()

    /**
     * Convert a [Size] to a [PxSize].
     */
    fun Size.toPx(): PxSize =
        PxSize(width.toPx(), height.toPx())

    /**
     * Convert a [Bounds] to a [Rect].
     */
    fun Bounds.toRect(): Rect {
        return Rect(
            left.toPx().value,
            top.toPx().value,
            right.toPx().value,
            bottom.toPx().value
        )
    }
}
