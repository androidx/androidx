/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import kotlin.math.abs
import kotlin.math.atan2

/**
 * A two-dimensional vector, i.e. an (x, y) coordinate pair. It can be used to represent either:
 * 1) A two-dimensional offset, i.e. the difference between two points
 * 2) A point in space, i.e. treating the vector as an offset from the origin
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public interface Vec {
    /** The [Vec]'s offset in the x-direction */
    public val x: Float

    /** The [Vec]'s offset in the y-direction */
    public val y: Float

    /** The length of the [Vec]. */
    public val magnitude: Float

    /** The squared length of the [Vec]. */
    public val magnitudeSquared: Float

    /**
     * The direction of the vec, represented as the angle between the positive x-axis and this vec.
     * The [direction] value will lie in the interval [-π, π], and will have the same sign as the
     * vec's y-component.
     */
    public val direction: Float
        @FloatRange(from = -Math.PI, to = Math.PI) @AngleRadiansFloat get() = atan2(y, x)

    /** Returns a vector with the same direction as this one, but with a magnitude of 1. */
    public val unitVec: ImmutableVec
        get() = VecNative.unitVec(this.x, this.y, ImmutableVec::class.java)

    /**
     * Modifies [output] into a vector with the same direction as this one, but with a magnitude
     * of 1.
     */
    public fun populateUnitVec(output: MutableVec) {
        VecNative.populateUnitVec(x, y, output)
    }

    /**
     * Returns a vector with the same magnitude as this one, but rotated by (positive) 90 degrees.
     */
    public val orthogonal: ImmutableVec
        get() = ImmutableVec(-y, x)

    /**
     * Modifies [output] into a vector with the same magnitude as this one, but rotated by
     * (positive) 90 degrees.
     */
    public fun populateOrthogonal(output: MutableVec) {
        output.x = -y
        output.y = x
    }

    /** Returns a vector with the same magnitude, but pointing in the opposite direction. */
    public val negation: ImmutableVec
        get() = ImmutableVec(-x, -y)

    /**
     * Modifies [output] into a vector with the same magnitude, but pointing in the opposite
     * direction.
     */
    public fun populateNegation(output: MutableVec) {
        output.x = -x
        output.y = -y
    }

    /**
     * Returns an immutable copy of this object. This will return itself if called on an immutable
     * instance.
     */
    public val asImmutable: ImmutableVec

    /**
     * Returns an [ImmutableVec] with some or all of its values taken from `this`. For each value,
     * the returned [ImmutableVec] will use the given value; if no value is given, it will instead
     * be set to the value on `this`. If `this` is an [ImmutableVec], and the result would be an
     * identical [ImmutableVec], then `this` is returned. This occurs when either no values are
     * given, or when all given values are structurally equal to the values in `this`.
     */
    @JvmSynthetic public fun asImmutable(x: Float = this.x, y: Float = this.y): ImmutableVec

    /**
     * Returns true if the angle formed by `this` and [other] is within [angleTolerance] of 0
     * radians or π radians (0 degrees or 180 degrees).
     */
    public fun isParallelTo(
        other: Vec,
        @AngleRadiansFloat @FloatRange(from = 0.0) angleTolerance: Float,
    ): Boolean {
        val absoluteAngle = absoluteAngleBetween(this, other)
        return absoluteAngle < angleTolerance || Math.PI - absoluteAngle < angleTolerance
    }

    /**
     * Returns true if the angle formed by `this` and [other] is within [angleTolerance] of ±π/2
     * radians (±90 degrees).
     */
    public fun isPerpendicularTo(
        other: Vec,
        @AngleRadiansFloat @FloatRange(from = 0.0) angleTolerance: Float,
    ): Boolean {
        val absoluteAngle = absoluteAngleBetween(this, other)
        return abs(absoluteAngle - (Math.PI / 2)) < angleTolerance
    }

    /**
     * Compares this [Vec] with [other], and returns true if the difference between [x] and
     * [other.x] is less than [tolerance], and likewise for [y].
     */
    public fun isAlmostEqual(
        other: Vec,
        @FloatRange(from = 0.0) tolerance: Float = 0.0001f,
    ): Boolean = (abs(x - other.x) < tolerance) && (abs(y - other.y) < tolerance)

    public companion object {

        /** The origin of the coordinate system, i.e. (0, 0). */
        @JvmField public val ORIGIN: ImmutableVec = ImmutableVec(0f, 0f)

        /** Adds the x and y values of both [Vec] objects and stores the result in [output]. */
        @JvmStatic
        public fun add(lhs: Vec, rhs: Vec, output: MutableVec) {
            output.x = lhs.x + rhs.x
            output.y = lhs.y + rhs.y
        }

        /**
         * Subtracts the x and y values of [rhs] from the x and y values of [lhs] and stores the
         * result in [output].
         */
        @JvmStatic
        public fun subtract(lhs: Vec, rhs: Vec, output: MutableVec) {
            output.x = lhs.x - rhs.x
            output.y = lhs.y - rhs.y
        }

        /**
         * Multiplies the x and y values of the [Vec] by the Float and stores the result in
         * [output].
         */
        @JvmStatic
        public fun multiply(lhs: Vec, rhs: Float, output: MutableVec) {
            output.x = lhs.x * rhs
            output.y = lhs.y * rhs
        }

        /**
         * Multiplies the x and y values of the [Vec] by the Float and stores the result in
         * [output].
         */
        @JvmStatic
        public fun multiply(lhs: Float, rhs: Vec, output: MutableVec) {
            multiply(rhs, lhs, output)
        }

        /**
         * Divides the x and y values of the [Vec] by the Float and stores the result in [output].
         */
        @JvmStatic
        public fun divide(lhs: Vec, rhs: Float, output: MutableVec) {
            if (rhs == 0f) {
                throw IllegalArgumentException("Cannot divide by zero")
            }
            output.x = lhs.x / rhs
            output.y = lhs.y / rhs
        }

        /**
         * Returns the dot product (⋅) of the two vectors. The dot product has the property that,
         * for vectors a and b: a ⋅ b = ‖a‖ * ‖b‖ * cos(θ) where ‖d‖ is the magnitude of the vector,
         * and θ is the angle from a to b.
         */
        @JvmStatic public fun dotProduct(lhs: Vec, rhs: Vec): Float = lhs.x * rhs.x + lhs.y * rhs.y

        /**
         * Returns the determinant (×) of the two vectors. The determinant can be thought of as the
         * z-component of the 3D cross product of the two vectors, if they were placed on the
         * xy-plane in 3D space. The determinant has the property that, for vectors a and b: a × b =
         * ‖a‖ * ‖b‖ * sin(θ) where ‖d‖ is the magnitude of the vector, and θ is the signed angle
         * from a to b.
         */
        @JvmStatic
        public fun determinant(lhs: Vec, rhs: Vec): Float {
            return lhs.x * rhs.y - lhs.y * rhs.x
        }

        /**
         * Returns the absolute angle between the given vectors. The return value will lie in the
         * interval [0, π].
         */
        @AngleRadiansFloat
        @FloatRange(from = 0.0, to = Math.PI)
        @JvmStatic
        public fun absoluteAngleBetween(lhs: Vec, rhs: Vec): Float {
            return VecNative.absoluteAngleBetween(lhs.x, lhs.y, rhs.x, rhs.y)
        }

        /**
         * Returns the signed angle between the given vectors. The return value will lie in the
         * interval (-π, π].
         */
        @AngleRadiansFloat
        @FloatRange(from = -Math.PI, to = Math.PI, fromInclusive = false)
        @JvmStatic
        public fun signedAngleBetween(lhs: Vec, rhs: Vec): Float {
            return VecNative.signedAngleBetween(lhs.x, lhs.y, rhs.x, rhs.y)
        }

        /**
         * Returns true if [first] and [second] have the same values for all properties of [Vec].
         */
        internal fun areEquivalent(first: Vec, second: Vec): Boolean {
            return first.x == second.x && first.y == second.y
        }

        /** Returns a hash code for [vec] using its [Vec] properties. */
        internal fun hash(vec: Vec) = 31 * vec.x.hashCode() + vec.y.hashCode()

        /** Returns a string representation for [vec] using its [Vec] properties. */
        internal fun string(vec: Vec): String = "Vec(x=${vec.x}, y=${vec.y})"
    }
}
