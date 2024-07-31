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
import kotlin.math.hypot

/**
 * A two-dimensional vector, i.e. an (x, y) coordinate pair. It can be used to represent either:
 * 1) A two-dimensional offset, i.e. the difference between two points
 * 2) A point in space, i.e. treating the vector as an offset from the origin
 */
public abstract class Vec internal constructor() {
    /** The [Vec]'s offset in the x-direction */
    public abstract val x: Float

    /** The [Vec]'s offset in the y-direction */
    public abstract val y: Float

    /** The length of the [Vec]. */
    @FloatRange(from = 0.0) public fun computeMagnitude(): Float = hypot(x, y)

    /** The squared length of the [Vec]. */
    @FloatRange(from = 0.0) public fun computeMagnitudeSquared(): Float = x * x + y * y

    /**
     * The direction of the vec, represented as the angle between the positive x-axis and this vec.
     * If either component of the vector is NaN, this returns a NaN angle; otherwise, the returned
     * value will lie in the interval [-π, π], and will have the same sign as the vector's
     * y-component.
     *
     * Following the behavior of `atan2`, this will return either ±0 or ±π for the zero vector,
     * depending on the signs of the zeros.
     */
    @FloatRange(from = -Math.PI, to = Math.PI)
    @AngleRadiansFloat
    public fun computeDirection(): Float = atan2(y, x)

    /**
     * Returns a newly allocated vector with the same direction as this one, but with a magnitude of
     * `1`. This is equivalent to (but faster than) calling [ImmutableVec.fromDirectionAndMagnitude]
     * with [computeDirection] and `1`.
     *
     * In keeping with the above equivalence, this will return <±1, ±0> for the zero vector,
     * depending on the signs of the zeros.
     *
     * For performance-sensitive code, use [computeUnitVec] with a pre-allocated instance of
     * [MutableVec].
     */
    public fun computeUnitVec(): ImmutableVec =
        VecNative.unitVec(this.x, this.y, ImmutableVec::class.java)

    /**
     * Modifies [outVec] into a vector with the same direction as this one, but with a magnitude of
     * `1`. Returns [outVec]. This is equivalent to (but faster than) calling
     * [MutableVec.fromDirectionAndMagnitude] with [computeDirection] and `1`.
     *
     * In keeping with the above equivalence, this will return <±1, ±0> for the zero vector,
     * depending on the signs of the zeros.
     */
    public fun computeUnitVec(outVec: MutableVec): MutableVec {
        VecNative.populateUnitVec(x, y, outVec)
        return outVec
    }

    /**
     * Returns a newly allocated vector with the same magnitude as this one, but rotated by
     * (positive) 90 degrees. For performance-sensitive code, use [computeOrthogonal] with a
     * pre-allocated instance of [MutableVec].
     */
    public fun computeOrthogonal(): ImmutableVec = ImmutableVec(-y, x)

    /**
     * Modifies [outVec] into a vector with the same magnitude as this one, but rotated by
     * (positive) 90 degrees. Returns [outVec].
     */
    public fun computeOrthogonal(outVec: MutableVec): MutableVec {
        outVec.x = -y
        outVec.y = x
        return outVec
    }

    /**
     * Returns a newly allocated vector with the same magnitude, but pointing in the opposite
     * direction. For performance-sensitive code, use [computeNegation] with a pre-allocated
     * instance of [MutableVec].
     */
    public fun computeNegation(): ImmutableVec = ImmutableVec(-x, -y)

    /**
     * Modifies [outVec] into a vector with the same magnitude, but pointing in the opposite
     * direction. Returns [outVec].
     */
    public fun computeNegation(outVec: MutableVec): MutableVec {
        outVec.x = -x
        outVec.y = -y
        return outVec
    }

    /**
     * Returns an immutable copy of this object. This will return itself if called on an immutable
     * instance.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public abstract fun asImmutable(): ImmutableVec

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
    @JvmOverloads
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
