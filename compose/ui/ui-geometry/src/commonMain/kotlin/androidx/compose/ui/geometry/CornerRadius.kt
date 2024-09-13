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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.ui.geometry

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Constructs a Radius with the given [x] and [y] parameters for the size of the radius along the x
 * and y axis respectively. By default the radius along the Y axis matches that of the given x-axis
 * unless otherwise specified. Negative radii values are clamped to 0.
 */
@Stable inline fun CornerRadius(x: Float, y: Float = x) = CornerRadius(packFloats(x, y))

/**
 * A radius for either circular or elliptical (oval) shapes.
 *
 * Note consumers should create an instance of this class through the corresponding function
 * constructor as it is represented as an inline class with 2 float parameters packed into a single
 * long to reduce allocation overhead
 */
@Immutable
@kotlin.jvm.JvmInline
value class CornerRadius(val packedValue: Long) {
    /** The radius value on the horizontal axis. */
    @Stable
    inline val x: Float
        get() = unpackFloat1(packedValue)

    /** The radius value on the vertical axis. */
    @Stable
    inline val y: Float
        get() = unpackFloat2(packedValue)

    @Stable inline operator fun component1(): Float = x

    @Stable inline operator fun component2(): Float = y

    /**
     * Returns a copy of this Radius instance optionally overriding the radius parameter for the x
     * or y axis
     */
    fun copy(x: Float = unpackFloat1(packedValue), y: Float = unpackFloat2(packedValue)) =
        CornerRadius(packFloats(x, y))

    companion object {
        /**
         * A radius with [x] and [y] values set to zero.
         *
         * You can use [CornerRadius.Zero] with [RoundRect] to have right-angle corners.
         */
        @Stable val Zero: CornerRadius = CornerRadius(0x0L)
    }

    /** Whether this corner radius is 0 in x, y, or both. */
    @Stable
    inline fun isZero(): Boolean {
        // account for +/- 0.0f
        val v = packedValue and DualUnsignedFloatMask
        return ((v - 0x00000001_00000001L) and v.inv() and 0x80000000_80000000UL.toLong()) != 0L
    }

    /** Whether this corner radius describes a quarter circle (x == y). */
    @Stable
    inline fun isCircular(): Boolean {
        return (packedValue ushr 32) == (packedValue and 0xffff_ffffL)
    }

    /**
     * Unary negation operator.
     *
     * Returns a Radius with the distances negated.
     *
     * Radiuses with negative values aren't geometrically meaningful, but could occur as part of
     * expressions. For example, negating a radius of one pixel and then adding the result to
     * another radius is equivalent to subtracting a radius of one pixel from the other.
     */
    @Stable inline operator fun unaryMinus() = CornerRadius(packedValue xor DualFloatSignBit)

    /**
     * Binary subtraction operator.
     *
     * Returns a radius whose [x] value is the left-hand-side operand's [x] minus the
     * right-hand-side operand's [x] and whose [y] value is the left-hand-side operand's [y] minus
     * the right-hand-side operand's [y].
     */
    @Stable
    operator fun minus(other: CornerRadius): CornerRadius {
        return CornerRadius(
            packFloats(
                unpackFloat1(packedValue) - unpackFloat1(other.packedValue),
                unpackFloat2(packedValue) - unpackFloat2(other.packedValue)
            )
        )
    }

    /**
     * Binary addition operator.
     *
     * Returns a radius whose [x] value is the sum of the [x] values of the two operands, and whose
     * [y] value is the sum of the [y] values of the two operands.
     */
    @Stable
    operator fun plus(other: CornerRadius): CornerRadius {
        return CornerRadius(
            packFloats(
                unpackFloat1(packedValue) + unpackFloat1(other.packedValue),
                unpackFloat2(packedValue) + unpackFloat2(other.packedValue)
            )
        )
    }

    /**
     * Multiplication operator.
     *
     * Returns a radius whose coordinates are the coordinates of the left-hand-side operand (a
     * radius) multiplied by the scalar right-hand-side operand (a Float).
     */
    @Stable
    operator fun times(operand: Float) =
        CornerRadius(
            packFloats(unpackFloat1(packedValue) * operand, unpackFloat2(packedValue) * operand)
        )

    /**
     * Division operator.
     *
     * Returns a radius whose coordinates are the coordinates of the left-hand-side operand (a
     * radius) divided by the scalar right-hand-side operand (a Float).
     */
    @Stable
    operator fun div(operand: Float) =
        CornerRadius(
            packFloats(unpackFloat1(packedValue) / operand, unpackFloat2(packedValue) / operand)
        )

    override fun toString(): String {
        return if (x == y) {
            "CornerRadius.circular(${x.toStringAsFixed(1)})"
        } else {
            "CornerRadius.elliptical(${x.toStringAsFixed(1)}, ${y.toStringAsFixed(1)})"
        }
    }
}

/**
 * Linearly interpolate between two radii.
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
fun lerp(start: CornerRadius, stop: CornerRadius, fraction: Float): CornerRadius {
    return CornerRadius(
        packFloats(
            lerp(unpackFloat1(start.packedValue), unpackFloat1(stop.packedValue), fraction),
            lerp(unpackFloat2(start.packedValue), unpackFloat2(stop.packedValue), fraction)
        )
    )
}
