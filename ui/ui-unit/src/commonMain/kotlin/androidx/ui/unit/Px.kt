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

package androidx.ui.unit

import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.util.lerp
import androidx.ui.util.packFloats
import androidx.ui.util.unpackFloat1
import androidx.ui.util.unpackFloat2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Dimension value represented in pixels (px). Component APIs specify their
 * dimensions such as line thickness in DP with Dp objects, while drawing and layout are done
 * in pixel dimensions. When specific pixel dimensions are required, create a Px and convert
 * it to Dp using [Density.toDp]. Px are normally defined using [px], which can be applied to
 * [Int], [Double], and [Float].
 *     val leftMargin = 10.px
 *     val rightMargin = 10f.px
 *     val topMargin = 20.0.px
 *     val bottomMargin = 10.px
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Immutable
inline class Px(val value: Float) : Comparable<Px> {
    /**
     * Add two [Px]s together.
     */
    @Stable
    inline operator fun plus(other: Px) =
        Px(value = this.value + other.value)

    /**
     * Subtract a Px from another one.
     */
    @Stable
    inline operator fun minus(other: Px) =
        Px(value = this.value - other.value)

    /**
     * This is the same as multiplying the Px by -1.0.
     */
    @Stable
    inline operator fun unaryMinus() = Px(-value)

    /**
     * Divide a Px by a scalar.
     */
    @Stable
    inline operator fun div(other: Float): Px =
        Px(value = value / other)

    @Stable
    inline operator fun div(other: Int): Px =
        Px(value = value / other)

    /**
     * Divide by another Px to get a scalar.
     */
    @Stable
    inline operator fun div(other: Px): Float = value / other.value

    /**
     * Divide by [PxSquared] to get a [PxInverse].
     */
    @Stable
    inline operator fun div(other: PxSquared): PxInverse =
        PxInverse(value = value / other.value)

    /**
     * Multiply a Px by a scalar.
     */
    @Stable
    inline operator fun times(other: Float): Px =
        Px(value = value * other)

    @Stable
    inline operator fun times(other: Int): Px =
        Px(value = value * other)

    /**
     * Multiply by a Px to get a [PxSquared] result.
     */
    @Stable
    inline operator fun times(other: Px): PxSquared =
        PxSquared(value = value * other.value)

    /**
     * Multiply by a Px to get a [PxSquared] result.
     */
    @Stable
    inline operator fun times(other: PxSquared): PxCubed =
        PxCubed(value = value * other.value)

    /**
     * Compare [Px] with another [Px].
     */
    @Stable
    override /* TODO: inline */ operator fun compareTo(other: Px) = value.compareTo(other.value)

    /**
     * Compares this [Px] to another [IntPx]
     */
    @Stable
    inline operator fun compareTo(other: IntPx): Int = value.compareTo(other.value)

    /**
     * Add an [IntPx] to this [Px].
     */
    @Stable
    inline operator fun plus(other: IntPx) =
        Px(value = this.value + other.value)

    /**
     * Subtract an [IntPx] from this [Px].
     */
    @Stable
    inline operator fun minus(other: IntPx) =
        Px(value = this.value - other.value)

    @Stable
    override fun toString() = "$value.px"

    companion object {
        /**
         * Infinite px dimension.
         */
        @Stable
        val Infinity = Px(value = Float.POSITIVE_INFINITY)

        /**
         * Zero px dimension
         */
        @Stable
        val Zero = Px(0.0f)
    }
}

/**
 * Create a [Px] using an [Int]:
 *     val left = 10
 *     val x = left.px
 *     // -- or --
 *     val y = 10.px
 */
@Stable
inline val Int.px: Px get() = Px(value = this.toFloat())

/**
 * Create a [Px] using a [Double]:
 *     val left = 10.0
 *     val x = left.px
 *     // -- or --
 *     val y = 10.0.px
 */
@Stable
inline val Double.px: Px get() = Px(value = this.toFloat())

/**
 * Create a [Px] using a [Float]:
 *     val left = 10f
 *     val x = left.px
 *     // -- or --
 *     val y = 10f.px
 */
@Stable
inline val Float.px: Px get() = Px(value = this)

@Stable
inline operator fun Float.div(other: Px) =
    PxInverse(this / other.value)

@Stable
inline operator fun Double.div(other: Px) =
    PxInverse(this.toFloat() / other.value)

@Stable
inline operator fun Int.div(other: Px) =
    PxInverse(this / other.value)

@Stable
inline operator fun Float.times(other: Px) =
    Px(this * other.value)

@Stable
inline operator fun Double.times(other: Px) =
    Px(this.toFloat() * other.value)

@Stable
inline operator fun Int.times(other: Px) =
    Px(this * other.value)

@Stable
inline fun min(a: Px, b: Px): Px = Px(value = min(a.value, b.value))

@Stable
inline fun max(a: Px, b: Px): Px = Px(value = max(a.value, b.value))

@Stable
inline fun abs(x: Px): Px = Px(abs(x.value))

/**
 * Ensures that this value lies in the specified range [minimumValue]..[maximumValue].
 *
 * @return this value if it's in the range, or [minimumValue] if this value is less than
 * [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
 */
@Stable
inline fun Px.coerceIn(minimumValue: Px, maximumValue: Px): Px =
    Px(value = value.coerceIn(minimumValue.value, maximumValue.value))

/**
 * Ensures that this value is not less than the specified [minimumValue].
 *
 * @return this value if it's greater than or equal to the [minimumValue] or the
 * [minimumValue] otherwise.
 */
@Stable
inline fun Px.coerceAtLeast(minimumValue: Px): Px =
    Px(value = value.coerceAtLeast(minimumValue.value))

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 *
 * @return this value if it's less than or equal to the [maximumValue] or the
 * [maximumValue] otherwise.
 */
@Stable
inline fun Px.coerceAtMost(maximumValue: Px): Px =
    Px(value = value.coerceAtMost(maximumValue.value))

/**
 * Linearly interpolate between two [Px]s.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
@Stable
fun lerp(start: Px, stop: Px, fraction: Float): Px {
    return Px(lerp(start.value, stop.value, fraction))
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
@Immutable
inline class PxSquared(val value: Float) : Comparable<PxSquared> {
    /**
     * Add two DimensionSquares together.
     */
    @Stable
    inline operator fun plus(other: PxSquared) =
        PxSquared(value = value + other.value)

    /**
     * Subtract a DimensionSquare from another one.
     */
    @Stable
    inline operator fun minus(other: PxSquared) =
        PxSquared(value = value - other.value)

    /**
     * Divide a DimensionSquare by a scalar.
     */
    @Stable
    inline operator fun div(other: Float): PxSquared =
        PxSquared(value = value / other)

    /**
     * Divide by a [Px] to get a [Px] result.
     */
    @Stable
    inline operator fun div(other: Px): Px =
        Px(value = value / other.value)

    /**
     * Divide by a PxSquared to get a scalar result.
     */
    @Stable
    inline operator fun div(other: PxSquared): Float = value / other.value

    /**
     * Divide by a [PxCubed] to get a [PxInverse] result.
     */
    @Stable
    inline operator fun div(other: PxCubed): PxInverse =
        PxInverse(value / other.value)

    /**
     * Multiply by a scalar to get a PxSquared result.
     */
    @Stable
    inline operator fun times(other: Float): PxSquared =
        PxSquared(value = value * other)

    /**
     * Multiply by a scalar to get a PxSquared result.
     */
    @Stable
    inline operator fun times(other: Px): PxCubed =
        PxCubed(value = value * other.value)

    /**
     * Support comparing PxSquared with comparison operators.
     */
    @Stable
    override /* TODO: inline */ operator fun compareTo(other: PxSquared) =
        value.compareTo(other.value)

    @Stable
    override fun toString(): String = "$value.px^2"
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
@Immutable
inline class PxCubed(val value: Float) : Comparable<PxCubed> {
    /**
     * Add two PxCubed together.
     */
    @Stable
    inline operator fun plus(dimension: PxCubed) =
        PxCubed(value = value + dimension.value)

    /**
     * Subtract a PxCubed from another one.
     */
    @Stable
    inline operator fun minus(dimension: PxCubed) =
        PxCubed(value = value - dimension.value)

    /**
     * Divide a PxCubed by a scalar.
     */
    @Stable
    inline operator fun div(other: Float): PxCubed =
        PxCubed(value = value / other)

    /**
     * Divide by a [Px] to get a [PxSquared] result.
     */
    @Stable
    inline operator fun div(other: Px): PxSquared =
        PxSquared(value = value / other.value)

    /**
     * Divide by a [PxSquared] to get a [Px] result.
     */
    @Stable
    inline operator fun div(other: PxSquared): Px =
        Px(value = value / other.value)

    /**
     * Divide by a PxCubed to get a scalar result.
     */
    @Stable
    inline operator fun div(other: PxCubed): Float = value / other.value

    /**
     * Multiply by a scalar to get a PxCubed result.
     */
    @Stable
    inline operator fun times(other: Float): PxCubed =
        PxCubed(value = value * other)

    /**
     * Support comparing PxCubed with comparison operators.
     */
    @Stable
    override /* TODO: inline */ operator fun compareTo(other: PxCubed) =
        value.compareTo(other.value)

    @Stable
    override fun toString(): String = "$value.px^3"
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
@Immutable
inline class PxInverse(val value: Float) : Comparable<PxInverse> {
    /**
     * Add two PxInverse together.
     */
    @Stable
    inline operator fun plus(dimension: PxInverse) =
        PxInverse(value = value + dimension.value)

    /**
     * Subtract a PxInverse from another one.
     */
    @Stable
    inline operator fun minus(dimension: PxInverse) =
        PxInverse(value = value - dimension.value)

    /**
     * Divide a PxInverse by a scalar.
     */
    @Stable
    inline operator fun div(other: Float): PxInverse =
        PxInverse(value = value / other)

    /**
     * Multiply by a scalar to get a PxInverse result.
     */
    @Stable
    inline operator fun times(other: Float): PxInverse =
        PxInverse(value = value * other)

    /**
     * Multiply by a [Px] to get a scalar result.
     */
    @Stable
    inline operator fun times(other: Px): Float = value * other.value

    /**
     * Multiply by a [PxSquared] to get a [Px] result.
     */
    @Stable
    inline operator fun times(other: PxSquared): Px =
        Px(value = value * other.value)

    /**
     * Multiply by a [PxCubed] to get a [PxSquared] result.
     */
    @Stable
    inline operator fun times(other: PxCubed): PxSquared =
        PxSquared(value = value * other.value)

    /**
     * Support comparing PxInverse with comparison operators.
     */
    @Stable
    override /* TODO: inline */ operator fun compareTo(other: PxInverse) =
        value.compareTo(other.value)

    @Stable
    override fun toString(): String = "$value.px^-1"
}

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// Structures using Px
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

/**
 * A two dimensional size using [Px] for units
 */
@Immutable
data class PxSize @PublishedApi internal constructor(@PublishedApi internal val value: Long) {

    /**
     * The horizontal aspect of the size in [Px].
     */
    @Stable
    inline val width: Float
        get() = unpackFloat1(value)

    /**
     * The vertical aspect of the size in [Px].
     */
    @Stable
    inline val height: Float
        get() = unpackFloat2(value)

    /**
     * Returns a PxSize scaled by multiplying [width] and [height] by [other]
     */
    @Stable
    inline operator fun times(other: Int): PxSize =
        PxSize(width = width * other, height = height * other)

    /**
     * Returns a PxSize scaled  by multiplying [width] and [height] by [other]
     */
    @Stable
    inline operator fun times(other: Float): PxSize =
        PxSize(width = width * other, height = height * other)

    /**
     * Returns a PxSize scaled  by multiplying [width] and [height] by [other]
     */
    @Stable
    inline operator fun times(other: Double): PxSize = times(other.toFloat())

    /**
     * Returns a PxSize scaled  by dividing [width] and [height] by [other]
     */
    @Stable
    inline operator fun div(other: Int): PxSize =
        PxSize(width = width / other, height = height / other)

    /**
     * Returns a PxSize scaled  by dividing [width] and [height] by [other]
     */
    @Stable
    inline operator fun div(other: Float): PxSize =
        PxSize(width = width / other, height = height / other)

    /**
     * Returns a PxSize scaled  by dividing [width] and [height] by [other]
     */
    @Stable
    inline operator fun div(other: Double): PxSize = div(other.toFloat())

    @Stable
    override fun toString(): String = "$width x $height"

    companion object {
        /**
         * [PxSize] with zero values.
         */
        @Stable
        val Zero = PxSize(0f, 0f)

        /**
         * Default value indicating no specified size
         */
        @Stable
        val UnspecifiedSize = PxSize(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    }
}

/**
 * Constructs a [PxSize] from width and height Float values.
 */
@Stable
inline fun PxSize(width: Float, height: Float): PxSize = PxSize(packFloats(width, height))

/**
 * Constructs a [PxSize] from width and height [Px] values.
 */
@Stable
inline fun PxSize(width: Px, height: Px): PxSize = PxSize(packFloats(width.value, height.value))

/**
 * Returns a [PxSize] with [size]'s [PxSize.width] and [PxSize.height] multiplied by [this]
 */
@Stable
inline operator fun Int.times(size: PxSize) = size * this

/**
 * Returns a [PxSize] with [size]'s [PxSize.width] and [PxSize.height] multiplied by [this]
 */
@Stable
inline operator fun Float.times(size: PxSize) = size * this

/**
 * Returns a [PxSize] with [size]'s [PxSize.width] and [PxSize.height] multiplied by [this]
 */
@Stable
inline operator fun Double.times(size: PxSize) = size * this

/**
 * Returns the [PxPosition] of the center of the rect from the point of [0, 0]
 * with this [PxSize].
 */
@Stable
fun PxSize.center(): PxPosition {
    return PxPosition(width / 2f, height / 2f)
}

/**
 * Returns the smallest dimension size.
 */
@Stable
val PxSize.minDimension get() = min(width, height)

/**
 * A two-dimensional position using [Px] for units
 */
@Immutable
data class PxPosition @PublishedApi internal constructor(@PublishedApi internal val value: Long) {
    /**
     * The horizontal aspect of the position in [Px]
     */
    inline val x: Float
        get() = unpackFloat1(value)

    /**
     * The vertical aspect of the position in [Px]
     */
    inline val y: Float
        get() = unpackFloat2(value)

    /**
     * Subtract a [PxPosition] from another one.
     */
    @Stable
    inline operator fun minus(other: PxPosition) =
        PxPosition(x - other.x, y - other.y)

    /**
     * Add a [PxPosition] to another one.
     */
    @Stable
    inline operator fun plus(other: PxPosition) =
        PxPosition(x + other.x, y + other.y)

    /**
     * Subtract a [IntPxPosition] from this [PxPosition].
     */
    @Stable
    inline operator fun minus(other: IntPxPosition) =
        PxPosition(x - other.x.value, y - other.y.value)

    /**
     * Add a [IntPxPosition] to this [PxPosition].
     */
    @Stable
    inline operator fun plus(other: IntPxPosition) =
        PxPosition(x + other.x.value, y + other.y.value)

    /**
     * Returns a new PxPosition representing the negation of this point.
     */
    @Stable
    inline operator fun unaryMinus() = PxPosition(-x, -y)

    @Stable
    override fun toString(): String = "($x, $y)"

    companion object {
        @Stable
        val Origin = PxPosition(0.0f, 0.0f)
    }
}

/**
 * Constructs a [PxPosition] from [x] and [y] position pixel values.
 */
@Stable
inline fun PxPosition(x: Float, y: Float): PxPosition = PxPosition(packFloats(x, y))

/**
 * The magnitude of the offset represented by this [PxPosition].
 */
@Stable
fun PxPosition.getDistance(): Px = Px(sqrt(x * x + y * y))

/**
 * Convert a [PxPosition] to a [Offset].
 */
@Stable
inline fun PxPosition.toOffset(): Offset = Offset(x, y)

/**
 * Round a [PxPosition] down to the nearest [Int] coordinates.
 */
@Stable
inline fun PxPosition.round(): IntPxPosition = IntPxPosition(x.roundToInt().ipx, y.roundToInt().ipx)

/**
 * Linearly interpolate between two [PxPosition]s.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
@Stable
fun lerp(start: PxPosition, stop: PxPosition, fraction: Float): PxPosition =
    PxPosition(lerp(start.x, stop.x, fraction), lerp(start.y, stop.y, fraction))

/**
 * A four dimensional bounds using pixels for units
 */
@Immutable
data class PxBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Stable
inline fun PxBounds(topLeft: PxPosition, size: PxSize) =
    PxBounds(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height
    )

/**
 * A width of this PxBounds in [Px].
 */
@Stable
inline val PxBounds.width: Float get() = right - left

/**
 * A height of this PxBounds in [Px].
 */
@Stable
inline val PxBounds.height: Float get() = bottom - top

/**
 * Returns the [PxPosition] of the center of the [PxBounds].
 */
@Stable
inline fun PxBounds.center(): PxPosition {
    return PxPosition(left + width / 2f, top + height / 2f)
}

/**
 * Convert a [PxBounds] to a [PxSize].
 */
@Stable
fun PxBounds.toSize(): PxSize {
    return PxSize(width, height)
}

/**
 * Convert a [PxSize] to a [PxBounds]. The left and top are 0.px and the right and bottom
 * are the width and height, respectively.
 */
@Stable
fun PxSize.toBounds(): PxBounds {
    return PxBounds(0f, 0f, width, height)
}

/**
 * Convert a [PxBounds] to a [Rect].
 */
@Stable
fun PxBounds.toRect(): Rect {
    return Rect(
        left,
        top,
        right,
        bottom
    )
}

/**
 * Convert a [PxSize] to a [Rect].
 */
@Stable
fun PxSize.toRect(): Rect {
    return Rect(0f, 0f, width, height)
}