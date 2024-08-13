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

package androidx.compose.ui.geometry

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackAbsFloat1
import androidx.compose.ui.util.unpackAbsFloat2
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.max
import kotlin.math.min

/** Constructs a [Size] from the given width and height */
@Stable inline fun Size(width: Float, height: Float) = Size(packFloats(width, height))

/**
 * Holds a 2D floating-point size.
 *
 * You can think of this as an [Offset] from the origin.
 */
@Immutable
@kotlin.jvm.JvmInline
value class Size @PublishedApi internal constructor(@PublishedApi internal val packedValue: Long) {
    @Stable
    inline val width: Float
        get() = unpackFloat1(packedValue)

    @Stable
    inline val height: Float
        get() = unpackFloat2(packedValue)

    @Stable inline operator fun component1(): Float = width

    @Stable inline operator fun component2(): Float = height

    /** Returns a copy of this Size instance optionally overriding the width or height parameter */
    fun copy(width: Float = unpackFloat1(packedValue), height: Float = unpackFloat2(packedValue)) =
        Size(packFloats(width, height))

    companion object {

        /** An empty size, one with a zero width and a zero height. */
        @Stable val Zero = Size(0x0L)

        /**
         * A size whose [width] and [height] are unspecified. This is a sentinel value used to
         * initialize a non-null parameter. Access to width or height on an unspecified size is not
         * allowed.
         */
        @Stable val Unspecified = Size(UnspecifiedPackedFloats)
    }

    /**
     * Whether this size encloses a non-zero area.
     *
     * Negative areas are considered empty.
     */
    @Stable
    fun isEmpty(): Boolean {
        // Mask the sign bits, shift them to the right and replicate them by multiplying by -1.
        // This will give us a mask of 0xffff_ffff for negative packed floats, and 0x0000_0000
        // for positive packed floats. We invert the mask and do an and operation with the
        // original value to set any negative float to 0.0f.
        val v = packedValue and ((packedValue and DualFloatSignBit ushr 31) * -0x1).inv()
        // At this point any negative float is set to 0, so the sign bit is always 0.
        // We take the 2 packed floats and "and" them together: if any of the two floats
        // is 0.0f (either because the original value is 0.0f or because it was negative and
        // we turned it into 0.0f with the line above), the result of the and operation will
        // be 0 and we know our Size is empty.
        val w = (v ushr 32) and (v and 0xffffffffL)
        // We treat Size.Unspecified as being empty
        return (w == 0L) or (packedValue == UnspecifiedPackedFloats)
    }

    /**
     * Multiplication operator.
     *
     * Returns a [Size] whose dimensions are the dimensions of the left-hand-side operand (a [Size])
     * multiplied by the scalar right-hand-side operand (a [Float]).
     */
    @Stable
    operator fun times(operand: Float): Size =
        Size(packFloats(unpackFloat1(packedValue) * operand, unpackFloat2(packedValue) * operand))

    /**
     * Division operator.
     *
     * Returns a [Size] whose dimensions are the dimensions of the left-hand-side operand (a [Size])
     * divided by the scalar right-hand-side operand (a [Float]).
     */
    @Stable
    operator fun div(operand: Float): Size =
        Size(packFloats(unpackFloat1(packedValue) / operand, unpackFloat2(packedValue) / operand))

    /** The lesser of the magnitudes of the [width] and the [height]. */
    @Stable
    val minDimension: Float
        get() = min(unpackAbsFloat1(packedValue), unpackAbsFloat2(packedValue))

    /** The greater of the magnitudes of the [width] and the [height]. */
    @Stable
    val maxDimension: Float
        get() = max(unpackAbsFloat1(packedValue), unpackAbsFloat2(packedValue))

    override fun toString() =
        if (isSpecified) {
            "Size(${width.toStringAsFixed(1)}, ${height.toStringAsFixed(1)})"
        } else {
            // In this case reading the width or height properties will throw, and they don't
            // contain meaningful values as strings anyway.
            "Size.Unspecified"
        }
}

/** `false` when this is [Size.Unspecified]. */
@Stable
inline val Size.isSpecified: Boolean
    get() = packedValue != 0x7fc00000_7fc00000L // NaN_NaN, see UnspecifiedPackedFloats

/** `true` when this is [Size.Unspecified]. */
@Stable
inline val Size.isUnspecified: Boolean
    get() = packedValue == 0x7fc00000_7fc00000L // NaN_NaN, see UnspecifiedPackedFloats

/**
 * If this [Size]&nbsp;[isSpecified] then this is returned, otherwise [block] is executed and its
 * result is returned.
 */
inline fun Size.takeOrElse(block: () -> Size): Size = if (isSpecified) this else block()

/**
 * Linearly interpolate between two sizes
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning that the
 * interpolation has not started, returning [start] (or something equivalent to [start]), 1.0
 * meaning that the interpolation has finished, returning [stop] (or something equivalent to
 * [stop]), and values in between meaning that the interpolation is at the relevant point on the
 * timeline between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and 1.0, so
 * negative values and values greater than 1.0 are valid (and can easily be generated by curves).
 *
 * Values for [fraction] are usually obtained from an [Animation<Float>], such as an
 * `AnimationController`.
 */
@Stable
fun lerp(start: Size, stop: Size, fraction: Float): Size =
    Size(
        packFloats(
            lerp(unpackFloat1(start.packedValue), unpackFloat1(stop.packedValue), fraction),
            lerp(unpackFloat2(start.packedValue), unpackFloat2(stop.packedValue), fraction)
        )
    )

/** Returns a [Size] with [size]'s [Size.width] and [Size.height] multiplied by [this] */
@Stable inline operator fun Int.times(size: Size) = size * this.toFloat()

/** Returns a [Size] with [size]'s [Size.width] and [Size.height] multiplied by [this] */
@Stable inline operator fun Double.times(size: Size) = size * this.toFloat()

/** Returns a [Size] with [size]'s [Size.width] and [Size.height] multiplied by [this] */
@Stable inline operator fun Float.times(size: Size) = size * this

/** Convert a [Size] to a [Rect]. */
@Stable fun Size.toRect(): Rect = Rect(Offset.Zero, this)

/** Returns the [Offset] of the center of the rect from the point of [0, 0] with this [Size]. */
@Stable
val Size.center: Offset
    get() = Offset(unpackFloat1(packedValue) / 2f, unpackFloat2(packedValue) / 2f)
