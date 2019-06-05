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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.ui.core

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.lerp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Dimension value represented in pixels (px). Component APIs specify their
 * dimensions such as line thickness in DP with Dp objects, while drawing and layout are done
 * in pixel dimensions. When specific pixel dimensions are required, create a Px and convert
 * it to Dp using [toDp]. Px are normally defined using [px], which can be applied to [Int],
 * [Double], and [Float].
 *     val leftMargin = 10.px
 *     val rightMargin = 10f.px
 *     val topMargin = 20.0.px
 *     val bottomMargin = 10.px
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
data /*inline*/ class Px(val value: Float) {
    /**
     * Add two [Px]s together.
     */
    inline operator fun plus(other: Px) =
        Px(value = this.value + other.value)

    /**
     * Subtract a Px from another one.
     */
    inline operator fun minus(other: Px) =
        Px(value = this.value - other.value)

    /**
     * This is the same as multiplying the Px by -1.0.
     */
    inline operator fun unaryMinus() = Px(-value)

    /**
     * Divide a Px by a scalar.
     */
    inline operator fun div(other: Float): Px =
        Px(value = value / other)

    inline operator fun div(other: Int): Px =
        Px(value = value / other)

    /**
     * Divide by another Px to get a scalar.
     */
    inline operator fun div(other: Px): Float = value / other.value

    /**
     * Divide by [PxSquared] to get a [PxInverse].
     */
    inline operator fun div(other: PxSquared): PxInverse =
        PxInverse(value = value / other.value)

    /**
     * Multiply a Px by a scalar.
     */
    inline operator fun times(other: Float): Px =
        Px(value = value * other)

    inline operator fun times(other: Int): Px =
        Px(value = value * other)

    /**
     * Multiply by a Px to get a [PxSquared] result.
     */
    inline operator fun times(other: Px): PxSquared =
        PxSquared(value = value * other.value)

    /**
     * Multiply by a Px to get a [PxSquared] result.
     */
    inline operator fun times(other: PxSquared): PxCubed =
        PxCubed(value = value * other.value)

    /**
     * Compare [Px] with another [Px].
     */
    inline operator fun compareTo(other: Px) = value.compareTo(other.value)

    /**
     * Add an [IntPx] to this [Px].
     */
    inline operator fun plus(other: IntPx) =
        Px(value = this.value + other.value)

    /**
     * Subtract an [IntPx] from this [Px].
     */
    inline operator fun minus(other: IntPx) =
        Px(value = this.value - other.value)

    companion object {
        /**
         * Infinite px dimension.
         */
        val Infinity = Px(value = Float.POSITIVE_INFINITY)
    }
}

/**
 * Create a [Px] using an [Int]:
 *     val left = 10
 *     val x = left.px
 *     // -- or --
 *     val y = 10.px
 */
inline val Int.px: Px get() = Px(value = this.toFloat())

/**
 * Create a [Px] using a [Double]:
 *     val left = 10.0
 *     val x = left.px
 *     // -- or --
 *     val y = 10.0.px
 */
inline val Double.px: Px get() = Px(value = this.toFloat())

/**
 * Create a [Px] using a [Float]:
 *     val left = 10f
 *     val x = left.px
 *     // -- or --
 *     val y = 10f.px
 */
inline val Float.px: Px get() = Px(value = this)

inline operator fun Float.div(other: Px) =
    PxInverse(this / other.value)

inline operator fun Double.div(other: Px) =
    PxInverse(this.toFloat() / other.value)

inline operator fun Int.div(other: Px) =
    PxInverse(this / other.value)

inline operator fun Float.times(other: Px) =
    Px(this * other.value)

inline operator fun Double.times(other: Px) =
    Px(this.toFloat() * other.value)

inline operator fun Int.times(other: Px) =
    Px(this * other.value)

inline fun min(a: Px, b: Px): Px = Px(value = min(a.value, b.value))

inline fun max(a: Px, b: Px): Px = Px(value = max(a.value, b.value))

/**
 * Ensures that this value lies in the specified range [minimumValue]..[maximumValue].
 *
 * @return this value if it's in the range, or [minimumValue] if this value is less than
 * [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
 */
inline fun Px.coerceIn(minimumValue: Px, maximumValue: Px): Px =
    Px(value = value.coerceIn(minimumValue.value, maximumValue.value))

/**
 * Ensures that this value is not less than the specified [minimumValue].
 *
 * @return this value if it's greater than or equal to the [minimumValue] or the
 * [minimumValue] otherwise.
 */
inline fun Px.coerceAtLeast(minimumValue: Px): Px =
    Px(value = value.coerceAtLeast(minimumValue.value))

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 *
 * @return this value if it's less than or equal to the [maximumValue] or the
 * [maximumValue] otherwise.
 */
inline fun Px.coerceAtMost(maximumValue: Px): Px =
    Px(value = value.coerceAtMost(maximumValue.value))

/**
 * Linearly interpolate between two [Px]s.
 *
 * The `t` argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning `a` (or something
 * equivalent to `a`), 1.0 meaning that the interpolation has finished,
 * returning `b` (or something equivalent to `b`), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
fun lerp(a: Px, b: Px, t: Float): Px {
    return Px(lerp(a.value, b.value, t))
}

/**
 * Holds a unit of squared dimensions, such as `1.value * 2.px`. [PxSquared], [PxCubed],
 * and [PxInverse] are used primarily for [Px] calculations to ensure resulting
 * units are as expected. Many times, [Px] calculations use scalars to determine the final
 * dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class PxSquared(val value: Float) {
    /**
     * Add two DimensionSquares together.
     */
    inline operator fun plus(other: PxSquared) =
        PxSquared(value = value + other.value)

    /**
     * Subtract a DimensionSquare from another one.
     */
    inline operator fun minus(other: PxSquared) =
        PxSquared(value = value - other.value)

    /**
     * Divide a DimensionSquare by a scalar.
     */
    inline operator fun div(other: Float): PxSquared =
        PxSquared(value = value / other)

    /**
     * Divide by a [Px] to get a [Px] result.
     */
    inline operator fun div(other: Px): Px =
        Px(value = value / other.value)

    /**
     * Divide by a PxSquared to get a scalar result.
     */
    inline operator fun div(other: PxSquared): Float = value / other.value

    /**
     * Divide by a [PxCubed] to get a [PxInverse] result.
     */
    inline operator fun div(other: PxCubed): PxInverse =
        PxInverse(value / other.value)

    /**
     * Multiply by a scalar to get a PxSquared result.
     */
    inline operator fun times(other: Float): PxSquared =
        PxSquared(value = value * other)

    /**
     * Multiply by a scalar to get a PxSquared result.
     */
    inline operator fun times(other: Px): PxCubed =
        PxCubed(value = value * other.value)

    /**
     * Support comparing PxSquared with comparison operators.
     */
    inline operator fun compareTo(other: PxSquared) =
        value.compareTo(other.value)
}

/**
 * Holds a unit of cubed dimensions, such as `1.value * 2.value * 3.px`. [PxSquared],
 * [PxCubed], and [PxInverse] are used primarily for [Px] calculations to
 * ensure resulting units are as expected. Many times, [Px] calculations use scalars to
 * determine the final dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class PxCubed(val value: Float) {
    /**
     * Add two PxCubed together.
     */
    inline operator fun plus(dimension: PxCubed) =
        PxCubed(value = value + dimension.value)

    /**
     * Subtract a PxCubed from another one.
     */
    inline operator fun minus(dimension: PxCubed) =
        PxCubed(value = value - dimension.value)

    /**
     * Divide a PxCubed by a scalar.
     */
    inline operator fun div(other: Float): PxCubed =
        PxCubed(value = value / other)

    /**
     * Divide by a [Px] to get a [PxSquared] result.
     */
    inline operator fun div(other: Px): PxSquared =
        PxSquared(value = value / other.value)

    /**
     * Divide by a [PxSquared] to get a [Px] result.
     */
    inline operator fun div(other: PxSquared): Px =
        Px(value = value / other.value)

    /**
     * Divide by a PxCubed to get a scalar result.
     */
    inline operator fun div(other: PxCubed): Float = value / other.value

    /**
     * Multiply by a scalar to get a PxCubed result.
     */
    inline operator fun times(other: Float): PxCubed =
        PxCubed(value = value * other)

    /**
     * Support comparing PxCubed with comparison operators.
     */
    inline operator fun compareTo(other: PxCubed) = value.compareTo(other.value)
}

/**
 * Holds a unit of an inverse dimensions, such as `1.px / (2.value * 3.px)`. [PxSquared],
 * [PxCubed], and [PxInverse] are used primarily for [Px] calculations to
 * ensure resulting units are as expected. Many times, [Px] calculations use scalars to
 * determine the final dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class PxInverse(val value: Float) {
    /**
     * Add two PxInverse together.
     */
    inline operator fun plus(dimension: PxInverse) =
        PxInverse(value = value + dimension.value)

    /**
     * Subtract a PxInverse from another one.
     */
    inline operator fun minus(dimension: PxInverse) =
        PxInverse(value = value - dimension.value)

    /**
     * Divide a PxInverse by a scalar.
     */
    inline operator fun div(other: Float): PxInverse =
        PxInverse(value = value / other)

    /**
     * Multiply by a scalar to get a PxInverse result.
     */
    inline operator fun times(other: Float): PxInverse =
        PxInverse(value = value * other)

    /**
     * Multiply by a [Px] to get a scalar result.
     */
    inline operator fun times(other: Px): Float = value * other.value

    /**
     * Multiply by a [PxSquared] to get a [Px] result.
     */
    inline operator fun times(other: PxSquared): Px =
        Px(value = value * other.value)

    /**
     * Multiply by a [PxCubed] to get a [PxSquared] result.
     */
    inline operator fun times(other: PxCubed): PxSquared =
        PxSquared(value = value * other.value)

    /**
     * Support comparing PxInverse with comparison operators.
     */
    inline operator fun compareTo(other: PxInverse) =
        value.compareTo(other.value)
}

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// Structures using Px
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

/**
 * A two dimensional size using [Px] for units
 */
data class PxSize(val width: Px, val height: Px)

/**
 * Returns the [PxPosition] of the center of the rect from the point of [0, 0]
 * with this [PxSize].
 */
fun PxSize.center(): PxPosition {
    return PxPosition(width / 2f, height / 2f)
}

/**
 * Returns the smallest dimension size.
 */
val PxSize.minDimension get() = min(width, height)

/**
 * A two-dimensional position using [Px] for units
 */
data class PxPosition(val x: Px, val y: Px) {
    /**
     * Subtract a [PxPosition] from another one.
     */
    inline operator fun minus(other: PxPosition) =
        PxPosition(x - other.x, y - other.y)

    /**
     * Add a [PxPosition] to another one.
     */
    inline operator fun plus(other: PxPosition) =
        PxPosition(x + other.x, y + other.y)

    /**
     * Subtract a [IntPxPosition] from this [PxPosition].
     */
    inline operator fun minus(other: IntPxPosition) =
        PxPosition(x - other.x, y - other.y)

    /**
     * Add a [IntPxPosition] to this [PxPosition].
     */
    inline operator fun plus(other: IntPxPosition) =
        PxPosition(x + other.x, y + other.y)

    /**
     * Returns a new PxPosition representing the negation of this point.
     */
    inline operator fun unaryMinus() = PxPosition(-x, -y)

    companion object {
        val Origin = PxPosition(0.px, 0.px)
    }
}

/**
 * The magnitude of the offset represented by this [PxPosition].
 */
fun PxPosition.getDistance(): Px {
    return Px(sqrt(x.value * x.value + y.value * y.value))
}

/**
 * Convert a [PxPosition] to a [Offset].
 */
inline fun PxPosition.toOffset(): Offset {
    return Offset(x.value, y.value)
}

/**
 * Linearly interpolate between two [PxPosition]s.
 *
 * The `t` argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning `a` (or something
 * equivalent to `a`), 1.0 meaning that the interpolation has finished,
 * returning `b` (or something equivalent to `b`), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
fun lerp(a: PxPosition, b: PxPosition, t: Float): PxPosition =
    PxPosition(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

/**
 * A four dimensional bounds using [Px] for units
 */
data class PxBounds(
    val left: Px,
    val top: Px,
    val right: Px,
    val bottom: Px
)

/**
 * A width of this PxBounds in [Px].
 */
inline val PxBounds.width: Px get() = right - left

/**
 * A height of this PxBounds in [Px].
 */
inline val PxBounds.height: Px get() = bottom - top

/**
 * Convert a [PxBounds] to a [PxSize].
 */
fun PxBounds.toSize(): PxSize {
    return PxSize(width, height)
}

/**
 * Convert a [PxSize] to a [PxBounds]. The left and top are 0.px and the right and bottom
 * are the width and height, respectively.
 */
fun PxSize.toBounds(): PxBounds {
    return PxBounds(0.px, 0.px, width, height)
}

/**
 * Convert a [PxBounds] to a [Rect].
 */
fun PxBounds.toRect(): Rect {
    return Rect(
        left.value,
        top.value,
        right.value,
        bottom.value
    )
}

/**
 * Convert a [PxSize] to a [Rect].
 */
fun PxSize.toRect(): Rect {
    return Rect(0f, 0f, width.value, height.value)
}