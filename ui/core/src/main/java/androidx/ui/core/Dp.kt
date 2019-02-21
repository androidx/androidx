/*
 * Copyright 2018 The Android Open Source Project
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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.ui.core

import androidx.ui.engine.geometry.Rect
import androidx.ui.lerpFloat
import kotlin.math.sqrt

/**
 * Dimension value representing device-independent pixels (dp). Component APIs specify their
 * dimensions such as line thickness in DP with Dp objects. Hairline (1 pixel) thickness
 * may be specified with [Hairline], a dimension that take up no space. Dimensions are normally
 * defined using [dp], which can be applied to [Int], [Double], and [Float].
 *     val leftMargin = 10.dp
 *     val rightMargin = 10f.dp
 *     val topMargin = 20.0.dp
 *     val bottomMargin = 10.dp
 * Drawing is done in pixels. To retrieve the pixel size of a Dp, use [toPx]:
 *     val lineThicknessPx = lineThickness.toPx(context)
 * [toPx] is normally needed only for painting operations.
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
data /*inline*/ class Dp(val dp: Float) {
    companion object {
        /**
         * A dimension used to represent a hairline drawing element. Hairline elements take up no
         * space, but will draw a single pixel, independent of the device's resolution and density.
         */
        val Hairline = Dp(dp = 0f)

        /**
         * Infinite dp dimension.
         */
        val Infinity = Dp(dp = Float.POSITIVE_INFINITY)
    }
}

// TODO(mount): regain the inline in the below extension properties. These don't work with jococo
/**
 * Create a [Dp] using an [Int]:
 *     val left = 10
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.dp
 */
/*inline*/ val Int.dp: Dp get() = Dp(dp = this.toFloat())

/**
 * Create a [Dp] using a [Double]:
 *     val left = 10.0
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.0.dp
 */
/*inline*/ val Double.dp: Dp get() = Dp(dp = this.toFloat())

/**
 * Create a [Dp] using a [Float]:
 *     val left = 10f
 *     val x = left.dp
 *     // -- or --
 *     val y = 10f.dp
 */
/*inline*/ val Float.dp: Dp get() = Dp(dp = this)

/*inline*/ operator fun Float.div(by: Dp) =
    DpInverse(this / by.dp)

/*inline*/ operator fun Double.div(by: Dp) =
    DpInverse(this.toFloat() / by.dp)

/*inline*/ operator fun Int.div(by: Dp) =
    DpInverse(this / by.dp)

/*inline*/ operator fun Float.times(by: Dp) =
    Dp(this * by.dp)

/*inline*/ operator fun Double.times(by: Dp) =
    Dp(this.toFloat() * by.dp)

/*inline*/ operator fun Int.times(by: Dp) =
    Dp(this * by.dp)

/**
 * Add two [Dp]s together.
 */
/*inline*/ operator fun Dp.plus(dp: Dp) =
    Dp(dp = this.dp + dp.dp)

/**
 * Subtract a Dp from another one.
 */
/*inline*/ operator fun Dp.minus(dp: Dp) =
    Dp(dp = this.dp - dp.dp)

/**
 * This is the same as multiplying the Dp by -1.0.
 */
/*inline*/ operator fun Dp.unaryMinus() = Dp(-dp)

/**
 * Divide a Dp by a scalar.
 */
/*inline*/ operator fun Dp.div(by: Float): Dp =
    Dp(dp = dp / by)

/*inline*/ operator fun Dp.div(by: Int): Dp =
    Dp(dp = dp / by)

/**
 * Divide by another Dp to get a scalar.
 */
/*inline*/ operator fun Dp.div(by: Dp): Float = dp / by.dp

/**
 * Divide by [DpSquared] to get a [DpInverse].
 */
/*inline*/ operator fun Dp.div(by: DpSquared): DpInverse =
    DpInverse(idp = dp / by.dp2)

/**
 * Multiply a Dp by a scalar.
 */
/*inline*/ operator fun Dp.times(by: Float): Dp =
    Dp(dp = dp * by)

/*inline*/ operator fun Dp.times(by: Int): Dp =
    Dp(dp = dp * by)

/**
 * Multiply by a Dp to get a [DpSquared] result.
 */
/*inline*/ operator fun Dp.times(by: Dp): DpSquared =
    DpSquared(dp2 = dp * by.dp)

/**
 * Multiply by a Dp to get a [DpSquared] result.
 */
/*inline*/ operator fun Dp.times(by: DpSquared): DpCubed =
    DpCubed(dp3 = dp * by.dp2)

/**
 * Support comparing Dimensions with comparison operators.
 */
/*inline*/ operator fun Dp.compareTo(other: Dp) = dp.compareTo(other.dp)

/*inline*/ fun min(dp1: Dp, dp2: Dp): Dp {
    return if (dp1 < dp2) {
        dp1
    } else {
        dp2
    }
}

/*inline*/ fun max(dp1: Dp, dp2: Dp): Dp {
    return if (dp1 > dp2) {
        dp1
    } else {
        dp2
    }
}

/**
 * Ensures that this value lies in the specified range [minimumValue]..[maximumValue].
 *
 * @return this value if it's in the range, or [minimumValue] if this value is less than
 * [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
 */
fun Dp.coerceIn(minimumValue: Dp, maximumValue: Dp): Dp {
    if (minimumValue > maximumValue) {
        throw IllegalArgumentException(
            "Cannot coerce value to an empty range: maximum " +
                    "$maximumValue is less than minimum $minimumValue."
        )
    }
    return when {
        this < minimumValue -> minimumValue
        this > maximumValue -> maximumValue
        else -> this
    }
}

/**
 * Ensures that this value is not less than the specified [minimumValue].
 *
 * @return this value if it's greater than or equal to the [minimumValue] or the
 * [minimumValue] otherwise.
 */
fun Dp.coerceAtLeast(minimumValue: Dp): Dp {
    return if (this < minimumValue) minimumValue else this
}

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 *
 * @return this value if it's less than or equal to the [maximumValue] or the
 * [maximumValue] otherwise.
 */
fun Dp.coerceAtMost(maximumValue: Dp): Dp {
    return if (this > maximumValue) maximumValue else this
}

/**
 * Linearly interpolate between two [Dp]s.
 *
 * The `t` argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning `a` (or something
 * equivalent to `a`), 1.0 meaning that the interpolation has finished,
 * returning `b` (or something equivalent to `b`), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
fun lerp(a: Dp, b: Dp, t: Float): Dp {
    return Dp(lerpFloat(a.dp, b.dp, t))
}

/**
 * A two dimensional size using [Dp] for units
 */
data class Size(val width: Dp, val height: Dp)

/**
 * Returns the [Position] of the center of the rect from the point of [0, 0]
 * with this [Size].
 */
fun Size.center(): Position {
    return Position(width / 2f, height / 2f)
}

/**
 * A two-dimensional position using [Dp] for units
 */
data class Position(val x: Dp, val y: Dp)

/**
 * The magnitude of the offset represented by this [Position].
 */
fun Position.getDistance(): Dp {
    return Dp(sqrt(x.dp * x.dp + y.dp * y.dp))
}

/**
 * Linearly interpolate between two [Position]s.
 *
 * The `t` argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning `a` (or something
 * equivalent to `a`), 1.0 meaning that the interpolation has finished,
 * returning `b` (or something equivalent to `b`), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
fun lerp(a: Position, b: Position, t: Float): Position =
    Position(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

/**
 * Subtract a [Position] from another one.
 */
/*inline*/ operator fun Position.minus(other: Position) =
    Position(x - other.x, y - other.y)

/**
 * Subtract a [Dp] from the [Position]
 */
/*inline*/ operator fun Position.minus(dp: Dp) =
    Position(x - dp, y - dp)

/**
 * Holds a unit of squared dimensions, such as `1.dp * 2.dp`. [DpSquared], [DpCubed],
 * and [DpInverse] are used primarily for [Dp] calculations to ensure resulting
 * units are as expected. Many times, [Dp] calculations use scalars to determine the final
 * dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class DpSquared(val dp2: Float)

/**
 * Add two DimensionSquares together.
 */
/*inline*/ operator fun DpSquared.plus(dimension: DpSquared) =
    DpSquared(dp2 = dp2 + dimension.dp2)

/**
 * Subtract a DimensionSquare from another one.
 */
/*inline*/ operator fun DpSquared.minus(dimension: DpSquared) =
    DpSquared(dp2 = dp2 - dimension.dp2)

/**
 * Divide a DimensionSquare by a scalar.
 */
/*inline*/ operator fun DpSquared.div(by: Float): DpSquared =
    DpSquared(dp2 = dp2 / by)

/**
 * Divide by a [Dp] to get a [Dp] result.
 */
/*inline*/ operator fun DpSquared.div(by: Dp): Dp =
    Dp(dp = dp2 / by.dp)

/**
 * Divide by a DpSquared to get a scalar result.
 */
/*inline*/ operator fun DpSquared.div(by: DpSquared): Float = dp2 / by.dp2

/**
 * Divide by a [DpCubed] to get a [DpInverse] result.
 */
/*inline*/ operator fun DpSquared.div(by: DpCubed): DpInverse =
    DpInverse(dp2 / by.dp3)

/**
 * Multiply by a scalar to get a DpSquared result.
 */
/*inline*/ operator fun DpSquared.times(by: Float): DpSquared =
    DpSquared(dp2 = dp2 * by)

/**
 * Multiply by a scalar to get a DpSquared result.
 */
/*inline*/ operator fun DpSquared.times(by: Dp): DpCubed =
    DpCubed(dp3 = dp2 * by.dp)

/**
 * Support comparing DpSquared with comparison operators.
 */
/*inline*/ operator fun DpSquared.compareTo(other: DpSquared) =
    dp2.compareTo(other.dp2)

/**
 * Holds a unit of cubed dimensions, such as `1.dp * 2.dp * 3.dp`. [DpSquared],
 * [DpCubed], and [DpInverse] are used primarily for [Dp] calculations to
 * ensure resulting units are as expected. Many times, [Dp] calculations use scalars to
 * determine the final dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class DpCubed(val dp3: Float)

/**
 * Add two DpCubed together.
 */
/*inline*/ operator fun DpCubed.plus(dimension: DpCubed) =
    DpCubed(dp3 = dp3 + dimension.dp3)

/**
 * Subtract a DpCubed from another one.
 */
/*inline*/ operator fun DpCubed.minus(dimension: DpCubed) =
    DpCubed(dp3 = dp3 - dimension.dp3)

/**
 * Divide a DpCubed by a scalar.
 */
/*inline*/ operator fun DpCubed.div(by: Float): DpCubed =
    DpCubed(dp3 = dp3 / by)

/**
 * Divide by a [Dp] to get a [DpSquared] result.
 */
/*inline*/ operator fun DpCubed.div(by: Dp): DpSquared =
    DpSquared(dp2 = dp3 / by.dp)

/**
 * Divide by a [DpSquared] to get a [Dp] result.
 */
/*inline*/ operator fun DpCubed.div(by: DpSquared): Dp =
    Dp(dp = dp3 / by.dp2)

/**
 * Divide by a DpCubed to get a scalar result.
 */
/*inline*/ operator fun DpCubed.div(by: DpCubed): Float = dp3 / by.dp3

/**
 * Multiply by a scalar to get a DpCubed result.
 */
/*inline*/ operator fun DpCubed.times(by: Float): DpCubed =
    DpCubed(dp3 = dp3 * by)

/**
 * Support comparing DpCubed with comparison operators.
 */
/*inline*/ operator fun DpCubed.compareTo(other: DpCubed) = dp3.compareTo(other.dp3)

/**
 * Holds a unit of an inverse dimensions, such as `1.dp / (2.dp * 3.dp)`. [DpSquared],
 * [DpCubed], and [DpInverse] are used primarily for [Dp] calculations to
 * ensure resulting units are as expected. Many times, [Dp] calculations use scalars to
 * determine the final dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class DpInverse(val idp: Float)

/**
 * Add two DpInverse together.
 */
/*inline*/ operator fun DpInverse.plus(dimension: DpInverse) =
    DpInverse(idp = idp + dimension.idp)

/**
 * Subtract a DpInverse from another one.
 */
/*inline*/ operator fun DpInverse.minus(dimension: DpInverse) =
    DpInverse(idp = idp - dimension.idp)

/**
 * Divide a DpInverse by a scalar.
 */
/*inline*/ operator fun DpInverse.div(by: Float): DpInverse =
    DpInverse(idp = idp / by)

/**
 * Multiply by a scalar to get a DpInverse result.
 */
/*inline*/ operator fun DpInverse.times(by: Float): DpInverse =
    DpInverse(idp = idp * by)

/**
 * Multiply by a [Dp] to get a scalar result.
 */
/*inline*/ operator fun DpInverse.times(by: Dp): Float = idp * by.dp

/**
 * Multiply by a [DpSquared] to get a [Dp] result.
 */
/*inline*/ operator fun DpInverse.times(by: DpSquared): Dp =
    Dp(dp = idp * by.dp2)

/**
 * Multiply by a [DpCubed] to get a [DpSquared] result.
 */
/*inline*/ operator fun DpInverse.times(by: DpCubed): DpSquared =
    DpSquared(dp2 = idp * by.dp3)

/**
 * Support comparing DpInverse with comparison operators.
 */
/*inline*/ operator fun DpInverse.compareTo(other: DpInverse) =
    idp.compareTo(other.idp)

/**
 * A size in Pixels
 */
data class PixelSize(val width: Float, val height: Float)

/**
 * Convert a [PixelSize] to a [Rect].
 */
fun PixelSize.toRect(): Rect {
    return Rect(0f, 0f, width, height)
}

/**
 * A four dimensional bounds using [Dp] for units
 */
data class Bounds(
    val left: Dp,
    val top: Dp,
    val right: Dp,
    val bottom: Dp
)

/**
 * A width of this Bounds in [Dp].
 */
val Bounds.width: Dp get() = right - left

/**
 * A height of this Bounds in [Dp].
 */
val Bounds.height: Dp get() = bottom - top

/**
 * Convert a [Bounds] to a [Size].
 */
fun Bounds.toSize(): Size {
    return Size(width, height)
}

/**
 * Convert a [Size] to a [Bounds].
 */
fun Size.toBounds(): Bounds {
    return Bounds(0.dp, 0.dp, width, height)
}
