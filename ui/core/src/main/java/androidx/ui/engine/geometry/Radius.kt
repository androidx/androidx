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

package androidx.ui.engine.geometry

import androidx.ui.lerp
import androidx.ui.toStringAsFixed
import kotlin.math.truncate

/** A radius for either circular or elliptical shapes. */
data class Radius(
    /** The radius value on the horizontal axis. */
    val x: Float,
    /** The radius value on the vertical axis. */
    val y: Float
) {

    companion object {
        /** Constructs a circular radius. [x] and [y] will have the same radius value. */
        fun circular(radius: Float): Radius {
            return Radius(radius, radius)
        }

        /** Constructs an elliptical radius with the given radii. */
        fun elliptical(x: Float, y: Float): Radius {
            return Radius(x, y)
        }

        /**
         * A radius with [x] and [y] values set to zero.
         *
         * You can use [Radius.zero] with [RRect] to have right-angle corners.
         */
        val zero: Radius = circular(0.0f)
    }

    /**
     * Unary negation operator.
     *
     * Returns a Radius with the distances negated.
     *
     * Radiuses with negative values aren't geometrically meaningful, but could
     * occur as part of expressions. For example, negating a radius of one pixel
     * and then adding the result to another radius is equivalent to subtracting
     * a radius of one pixel from the other.
     */
    operator fun unaryMinus() = elliptical(-x, -y)

    /**
     * Binary subtraction operator.
     *
     * Returns a radius whose [x] value is the left-hand-side operand's [x]
     * minus the right-hand-side operand's [x] and whose [y] value is the
     * left-hand-side operand's [y] minus the right-hand-side operand's [y].
     */
    operator fun minus(other: Radius) = elliptical(x - other.x, y - other.y)

    /**
     * Binary addition operator.
     *
     * Returns a radius whose [x] value is the sum of the [x] values of the
     * two operands, and whose [y] value is the sum of the [y] values of the
     * two operands.
     */
    operator fun plus(other: Radius) = elliptical(x + other.x, y + other.y)

    /**
     * Multiplication operator.
     *
     * Returns a radius whose coordinates are the coordinates of the
     * left-hand-side operand (a radius) multiplied by the scalar
     * right-hand-side operand (a Float).
     */
    operator fun times(operand: Float) = elliptical(x * operand, y * operand)

    /**
     * Division operator.
     *
     * Returns a radius whose coordinates are the coordinates of the
     * left-hand-side operand (a radius) divided by the scalar right-hand-side
     * operand (a Float).
     */
    operator fun div(operand: Float) = elliptical(x / operand, y / operand)

    /**
     * Integer (truncating) division operator.
     *
     * Returns a radius whose coordinates are the coordinates of the
     * left-hand-side operand (a radius) divided by the scalar right-hand-side
     * operand (a Float), rounded towards zero.
     */
    fun truncDiv(operand: Float): Radius =
        elliptical(truncate(x / operand), truncate(y / operand))

    /**
     * Modulo (remainder) operator.
     *
     * Returns a radius whose coordinates are the remainder of dividing the
     * coordinates of the left-hand-side operand (a radius) by the scalar
     * right-hand-side operand (a Float).
     */
    operator fun rem(operand: Float) = elliptical(x % operand, y % operand)

    override fun toString(): String {
        return if (x == y) {
            "Radius.circular(${x.toStringAsFixed(1)})"
        } else {
            "Radius.elliptical(${x.toStringAsFixed(1)}, ${y.toStringAsFixed(1)})"
        }
    }

// TODO(Migration/Filip): I think data class handles this no need to re-implement
//    @override
//    bool operator ==(dynamic other) {
//        if (identical(this, other))
//            return true;
//        if (runtimeType != other.runtimeType)
//            return false;
//        final Radius typedOther = other;
//        return typedOther.x == x && typedOther.y == y;
//    }
//
//    @override
//    int get hashCode => hashValues(x, y);
}

/**
 * Linearly interpolate between two radii.
 *
 * The `t` argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning `a` (or something
 * equivalent to `a`), 1.0 meaning that the interpolation has finished,
 * returning `b` (or something equivalent to `b`), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid (and can
 * easily be generated by curves such as [Curves.elasticInOut]).
 *
 * Values for `t` are usually obtained from an [Animation<Float>], such as
 * an [AnimationController].
 */
fun lerp(a: Radius, b: Radius, t: Float): Radius {
    return Radius.elliptical(lerp(a.x, b.x, t), lerp(a.y, b.y, t))
}
