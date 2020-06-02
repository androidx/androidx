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
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Holds a unit of squared dimensions, such as `1.value * 2.px`. [PxSquared], [PxCubed],
 * and [PxInverse] are used primarily for pixel calculations to ensure resulting
 * units are as expected. Many times, pixel calculations use scalars to determine the final
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
 * [PxCubed], and [PxInverse] are used primarily for pixel calculations to
 * ensure resulting units are as expected. Many times, pixel calculations use scalars to
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
 * [PxCubed], and [PxInverse] are used primarily for pixel calculations to
 * ensure resulting units are as expected. Many times, pixel calculations use scalars to
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
 * A two dimensional size using pixels for units
 */
@Immutable
data class PxSize @PublishedApi internal constructor(@PublishedApi internal val value: Long) {

    /**
     * The horizontal aspect of the size in pixels.
     */
    @Stable
    inline val width: Float
        get() = unpackFloat1(value)

    /**
     * The vertical aspect of the size in pixels.
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
 * A two-dimensional position using pixels for units
 */
@Immutable
data class PxPosition @PublishedApi internal constructor(@PublishedApi internal val value: Long) {
    /**
     * The horizontal aspect of the position in pixels
     */
    inline val x: Float
        get() = unpackFloat1(value)

    /**
     * The vertical aspect of the position in pixels
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
fun PxPosition.getDistance(): Float = sqrt(x * x + y * y)

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
 * A width of this PxBounds in pixels.
 */
@Stable
inline val PxBounds.width: Float get() = right - left

/**
 * A height of this PxBounds in pixels.
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