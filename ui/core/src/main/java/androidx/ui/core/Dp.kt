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

import androidx.ui.core.Dp.Companion.Hairline
import androidx.ui.lerp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Dimension value representing device-independent pixels (dp). Component APIs specify their
 * dimensions such as line thickness in DP with Dp objects. Hairline (1 pixel) thickness
 * may be specified with [Hairline], a dimension that take up no space. Dp are normally
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
data /*inline*/ class Dp(val value: Float) {
    /**
     * Add two [Dp]s together.
     */
    inline operator fun plus(other: Dp) =
        Dp(value = this.value + other.value)

    /**
     * Subtract a Dp from another one.
     */
    inline operator fun minus(other: Dp) =
        Dp(value = this.value - other.value)

    /**
     * This is the same as multiplying the Dp by -1.0.
     */
    inline operator fun unaryMinus() = Dp(-value)

    /**
     * Divide a Dp by a scalar.
     */
    inline operator fun div(other: Float): Dp =
        Dp(value = value / other)

    inline operator fun div(other: Int): Dp =
        Dp(value = value / other)

    /**
     * Divide by another Dp to get a scalar.
     */
    inline operator fun div(other: Dp): Float = value / other.value

    /**
     * Divide by [DpSquared] to get a [DpInverse].
     */
    inline operator fun div(other: DpSquared): DpInverse =
        DpInverse(value = value / other.value)

    /**
     * Multiply a Dp by a scalar.
     */
    inline operator fun times(other: Float): Dp =
        Dp(value = value * other)

    inline operator fun times(other: Int): Dp =
        Dp(value = value * other)

    /**
     * Multiply by a Dp to get a [DpSquared] result.
     */
    inline operator fun times(other: Dp): DpSquared =
        DpSquared(value = value * other.value)

    /**
     * Multiply by a Dp to get a [DpSquared] result.
     */
    inline operator fun times(other: DpSquared): DpCubed =
        DpCubed(value = value * other.value)

    /**
     * Support comparing Dimensions with comparison operators.
     */
    inline operator fun compareTo(other: Dp) = value.compareTo(other.value)

    companion object {
        /**
         * A dimension used to represent a hairline drawing element. Hairline elements take up no
         * space, but will draw a single pixel, independent of the device's resolution and density.
         */
        val Hairline = Dp(value = 0f)

        /**
         * Infinite dp dimension.
         */
        val Infinity = Dp(value = Float.POSITIVE_INFINITY)
    }
}

/**
 * Create a [Dp] using an [Int]:
 *     val left = 10
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.dp
 */
inline val Int.dp: Dp get() = Dp(value = this.toFloat())

/**
 * Create a [Dp] using a [Double]:
 *     val left = 10.0
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.0.dp
 */
inline val Double.dp: Dp get() = Dp(value = this.toFloat())

/**
 * Create a [Dp] using a [Float]:
 *     val left = 10f
 *     val x = left.dp
 *     // -- or --
 *     val y = 10f.dp
 */
inline val Float.dp: Dp get() = Dp(value = this)

inline operator fun Float.div(other: Dp) =
    DpInverse(this / other.value)

inline operator fun Double.div(other: Dp) =
    DpInverse(this.toFloat() / other.value)

inline operator fun Int.div(other: Dp) =
    DpInverse(this / other.value)

inline operator fun Float.times(other: Dp) =
    Dp(this * other.value)

inline operator fun Double.times(other: Dp) =
    Dp(this.toFloat() * other.value)

inline operator fun Int.times(other: Dp) =
    Dp(this * other.value)

inline fun min(a: Dp, b: Dp): Dp = Dp(value = min(a.value, b.value))

inline fun max(a: Dp, b: Dp): Dp = Dp(value = max(a.value, b.value))

/**
 * Ensures that this value lies in the specified range [minimumValue]..[maximumValue].
 *
 * @return this value if it's in the range, or [minimumValue] if this value is less than
 * [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
 */
inline fun Dp.coerceIn(minimumValue: Dp, maximumValue: Dp): Dp =
    Dp(value = value.coerceIn(minimumValue.value, maximumValue.value))

/**
 * Ensures that this value is not less than the specified [minimumValue].
 *
 * @return this value if it's greater than or equal to the [minimumValue] or the
 * [minimumValue] otherwise.
 */
inline fun Dp.coerceAtLeast(minimumValue: Dp): Dp =
    Dp(value = value.coerceAtLeast(minimumValue.value))

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 *
 * @return this value if it's less than or equal to the [maximumValue] or the
 * [maximumValue] otherwise.
 */
inline fun Dp.coerceAtMost(maximumValue: Dp): Dp =
    Dp(value = value.coerceAtMost(maximumValue.value))

/**
 *
 * Return whether `true` when it is finite or `false` when it is [Dp.Infinity]
 */
inline fun Dp.isFinite(): Boolean = value != Float.POSITIVE_INFINITY

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
    return Dp(lerp(a.value, b.value, t))
}

/**
 * Holds a unit of squared dimensions, such as `1.value * 2.dp`. [DpSquared], [DpCubed],
 * and [DpInverse] are used primarily for [Dp] calculations to ensure resulting
 * units are as expected. Many times, [Dp] calculations use scalars to determine the final
 * dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class DpSquared(val value: Float) {
    /**
     * Add two DimensionSquares together.
     */
    inline operator fun plus(other: DpSquared) =
        DpSquared(value = value + other.value)

    /**
     * Subtract a DimensionSquare from another one.
     */
    inline operator fun minus(other: DpSquared) =
        DpSquared(value = value - other.value)

    /**
     * Divide a DimensionSquare by a scalar.
     */
    inline operator fun div(other: Float): DpSquared =
        DpSquared(value = value / other)

    /**
     * Divide by a [Dp] to get a [Dp] result.
     */
    inline operator fun div(other: Dp): Dp =
        Dp(value = value / other.value)

    /**
     * Divide by a DpSquared to get a scalar result.
     */
    inline operator fun div(other: DpSquared): Float = value / other.value

    /**
     * Divide by a [DpCubed] to get a [DpInverse] result.
     */
    inline operator fun div(other: DpCubed): DpInverse =
        DpInverse(value / other.value)

    /**
     * Multiply by a scalar to get a DpSquared result.
     */
    inline operator fun times(other: Float): DpSquared =
        DpSquared(value = value * other)

    /**
     * Multiply by a scalar to get a DpSquared result.
     */
    inline operator fun times(other: Dp): DpCubed =
        DpCubed(value = value * other.value)

    /**
     * Support comparing DpSquared with comparison operators.
     */
    inline operator fun compareTo(other: DpSquared) =
        value.compareTo(other.value)
}

/**
 * Holds a unit of cubed dimensions, such as `1.value * 2.value * 3.dp`. [DpSquared],
 * [DpCubed], and [DpInverse] are used primarily for [Dp] calculations to
 * ensure resulting units are as expected. Many times, [Dp] calculations use scalars to
 * determine the final dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class DpCubed(val value: Float) {

    /**
     * Add two DpCubed together.
     */
    inline operator fun plus(dimension: DpCubed) =
        DpCubed(value = value + dimension.value)

    /**
     * Subtract a DpCubed from another one.
     */
    inline operator fun minus(dimension: DpCubed) =
        DpCubed(value = value - dimension.value)

    /**
     * Divide a DpCubed by a scalar.
     */
    inline operator fun div(other: Float): DpCubed =
        DpCubed(value = value / other)

    /**
     * Divide by a [Dp] to get a [DpSquared] result.
     */
    inline operator fun div(other: Dp): DpSquared =
        DpSquared(value = value / other.value)

    /**
     * Divide by a [DpSquared] to get a [Dp] result.
     */
    inline operator fun div(other: DpSquared): Dp =
        Dp(value = value / other.value)

    /**
     * Divide by a DpCubed to get a scalar result.
     */
    inline operator fun div(other: DpCubed): Float = value / other.value

    /**
     * Multiply by a scalar to get a DpCubed result.
     */
    inline operator fun times(other: Float): DpCubed =
        DpCubed(value = value * other)

    /**
     * Support comparing DpCubed with comparison operators.
     */
    inline operator fun compareTo(other: DpCubed) = value.compareTo(other.value)
}
/**
 * Holds a unit of an inverse dimensions, such as `1.dp / (2.value * 3.dp)`. [DpSquared],
 * [DpCubed], and [DpInverse] are used primarily for [Dp] calculations to
 * ensure resulting units are as expected. Many times, [Dp] calculations use scalars to
 * determine the final dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class DpInverse(val value: Float) {
    /**
     * Add two DpInverse together.
     */
    inline operator fun plus(dimension: DpInverse) =
        DpInverse(value = value + dimension.value)

    /**
     * Subtract a DpInverse from another one.
     */
    inline operator fun minus(dimension: DpInverse) =
        DpInverse(value = value - dimension.value)

    /**
     * Divide a DpInverse by a scalar.
     */
    inline operator fun div(other: Float): DpInverse =
        DpInverse(value = value / other)

    /**
     * Multiply by a scalar to get a DpInverse result.
     */
    inline operator fun times(other: Float): DpInverse =
        DpInverse(value = value * other)

    /**
     * Multiply by a [Dp] to get a scalar result.
     */
    inline operator fun times(other: Dp): Float = value * other.value

    /**
     * Multiply by a [DpSquared] to get a [Dp] result.
     */
    inline operator fun times(other: DpSquared): Dp =
        Dp(value = value * other.value)

    /**
     * Multiply by a [DpCubed] to get a [DpSquared] result.
     */
    inline operator fun times(other: DpCubed): DpSquared =
        DpSquared(value = value * other.value)

    /**
     * Support comparing DpInverse with comparison operators.
     */
    inline operator fun compareTo(other: DpInverse) =
        value.compareTo(other.value)
}

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// Structures using Dp
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

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
data class Position(val x: Dp, val y: Dp) {
    /**
     * Subtract a [Position] from another one.
     */
    inline operator fun minus(other: Position) =
        Position(x - other.x, y - other.y)

    /**
     * Add a [Position] to another one.
     */
    inline operator fun plus(other: Position) =
        Position(x + other.x, y + other.y)
}

/**
 * The magnitude of the offset represented by this [Position].
 */
fun Position.getDistance(): Dp {
    return Dp(sqrt(x.value * x.value + y.value * y.value))
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
inline val Bounds.width: Dp get() = right - left

/**
 * A height of this Bounds in [Dp].
 */
inline val Bounds.height: Dp get() = bottom - top

/**
 * Convert a [Bounds] to a [Size].
 */
inline fun Bounds.toSize(): Size {
    return Size(width, height)
}

/**
 * Convert a [Size] to a [Bounds].
 */
fun Size.toBounds(): Bounds {
    return Bounds(0.dp, 0.dp, width, height)
}
