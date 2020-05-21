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

import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.geometry.Rect
import kotlin.math.roundToInt

/**
 * A density of the screen. Used for convert [Dp] to pixels.
 *
 * @param density The logical density of the display. This is a scaling factor for the [Dp] unit.
 * @param fontScale Current user preference for the scaling factor for fonts.
 */
@Stable
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
@Immutable
interface Density {

    /**
     * The logical density of the display. This is a scaling factor for the [Dp] unit.
     */
    @Stable
    val density: Float

    /**
     * Current user preference for the scaling factor for fonts.
     */
    @Stable
    val fontScale: Float

    /**
     * Convert [Dp] to pixels. Pixels are used to paint to Canvas.
     */
    @Stable
    fun Dp.toPx(): Float = value * density

    /**
     * Convert [Dp] to [IntPx] by rounding
     */
    @Stable
    fun Dp.toIntPx(): IntPx = toPx().roundToInt().ipx

    /**
     * Convert [Dp] to Sp. Sp is used for font size, etc.
     */
    @Stable
    fun Dp.toSp(): TextUnit = TextUnit.Sp(value / fontScale)

    /**
     * Convert Sp to pixels. Pixels are used to paint to Canvas.
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    @Stable
    fun TextUnit.toPx(): Float {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return value * fontScale * density
    }

    /**
     * Convert Sp to [IntPx] by rounding
     */
    @Stable
    fun TextUnit.toIntPx(): IntPx = toPx().roundToInt().ipx

    /**
     * Convert Sp to [Dp].
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    @Stable
    fun TextUnit.toDp(): Dp {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return Dp(value * fontScale)
    }

    /**
     * Convert [Px] to [Dp].
     */
    @Stable
    fun Px.toDp(): Dp = (value / density).dp

    /**
     * Convert [Px] to Sp.
     */
    @Stable
    fun Px.toSp(): TextUnit = (value / (fontScale * density)).sp

    /**
     * Convert [IntPx] to [Dp].
     */
    @Stable
    fun IntPx.toDp(): Dp = (value / density).dp

    /**
     * Convert [IntPx] to Sp.
     */
    @Stable
    fun IntPx.toSp(): TextUnit = (value / (fontScale * density)).sp

    /** Convert a [Float] pixel value to a Dp */
    @Stable
    fun Float.toDp(): Dp = (this / density).dp

    /** Convert a [Float] pixel value to a Sp */
    @Stable
    fun Float.toSp(): TextUnit = (this / (fontScale * density)).sp

    /** Convert a [Int] pixel value to a Dp */
    @Stable
    fun Int.toDp(): Dp = toFloat().toDp()

    /** Convert a [Int] pixel value to a Sp */
    @Stable
    fun Int.toSp(): TextUnit = toFloat().toSp()

    /**
     * Convert a [Bounds] to a [Rect].
     */
    @Stable
    fun Bounds.toRect(): Rect {
        return Rect(
            left.toPx(),
            top.toPx(),
            right.toPx(),
            bottom.toPx()
        )
    }
}
