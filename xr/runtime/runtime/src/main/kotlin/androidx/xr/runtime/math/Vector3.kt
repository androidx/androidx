/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.math

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Represents a three-dimensional position in space.
 *
 * The coordinate system is right-handed. The [x]-axis points to the right, the [y]-axis up and the
 * [z]-axis back.
 *
 * @property x the value of the horizontal component.
 * @property y the value of the vertical component.
 * @property z the value of the forward component.
 */
public class Vector3
@JvmOverloads
constructor(public val x: Float = 0F, public val y: Float = 0F, public val z: Float = 0F) {
    /** The squared length of the vector. */
    public inline val lengthSquared: Float
        get() = x * x + y * y + z * z

    /** The length of the vector. */
    public inline val length: Float
        get() = sqrt(lengthSquared)

    /** Creates a new vector with the same values as the [other] vector. */
    public constructor(other: Vector3) : this(other.x, other.y, other.z)

    /** Negates this vector. */
    public operator fun unaryMinus(): Vector3 = Vector3(-x, -y, -z)

    /** Returns a new vector with the sum of this vector and the [other] vector. */
    public operator fun plus(other: Vector3): Vector3 =
        Vector3(x + other.x, y + other.y, z + other.z)

    /** Returns a new vector with the difference of this vector and the [other] vector. */
    public operator fun minus(other: Vector3): Vector3 =
        Vector3(x - other.x, y - other.y, z - other.z)

    /** Get a new vector multiplied by a scalar amount. */
    public operator fun times(c: Float): Vector3 = Vector3(x * c, y * c, z * c)

    /**
     * Returns a new vector with each component of this vector multiplied by each corresponding
     * component of the [other] vector.
     */
    public fun scale(other: Vector3): Vector3 = Vector3(x * other.x, y * other.y, z * other.z)

    /** Returns a new vector with this vector divided by a scalar amount. */
    public operator fun div(c: Float): Vector3 = Vector3(x / c, y / c, z / c)

    /** Returns the dot product of this vector and the [other] vector. */
    public infix fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    /** Returns the cross product of this vector and the [other] vector. */
    public infix fun cross(other: Vector3): Vector3 =
        Vector3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    /** Returns the component-wise multiplicative inverse of this vector. */
    public fun inverse(): Vector3 {
        if (this.x == 0f || this.y == 0f || this.z == 0f) {
            throw IllegalArgumentException(
                "Cannot take the multiplicative inverse if any component of the Vector3 is zero."
            )
        }
        return Vector3(1 / this.x, 1 / this.y, 1 / this.z)
    }

    /** Returns the normalized version of this vector. */
    public fun toNormalized(): Vector3 {
        val norm = rsqrt(lengthSquared)

        return Vector3(x * norm, y * norm, z * norm)
    }

    /** Returns a new vector with its values clamped between [min] and [max] vectors. */
    public fun clamp(min: Vector3, max: Vector3): Vector3 {
        var clampedX = clamp(x, min.x, max.x)
        var clampedY = clamp(y, min.y, max.y)
        var clampedZ = clamp(z, min.z, max.z)

        return Vector3(clampedX, clampedY, clampedZ)
    }

    /** Returns a copy of the vector. */
    @JvmOverloads
    public fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z): Vector3 =
        Vector3(x, y, z)

    /** Returns true if this vector is equal to [other]. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector3) return false

        return this.x == other.x && this.y == other.y && this.z == other.z
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    override fun toString(): String = "[x=$x, y=$y, z=$z]"

    public companion object {
        /** Vector with all components set to zero. */
        @JvmField public val Zero: Vector3 = Vector3(x = 0f, y = 0f, z = 0f)

        /** Vector with all components set to one. */
        @JvmField public val One: Vector3 = Vector3(x = 1f, y = 1f, z = 1f)

        /** Vector with y set to one and all other components set to zero. */
        @JvmField public val Up: Vector3 = Vector3(x = 0f, y = 1f, z = 0f)

        /** Vector with y set to negative one and all other components set to zero. */
        @JvmField public val Down: Vector3 = Vector3(x = 0f, y = -1f, z = 0f)

        /** Vector with x set to negative one and all other components set to zero. */
        @JvmField public val Left: Vector3 = Vector3(x = -1f, y = 0f, z = 0f)

        /** Vector with x set to one and all other components set to zero. */
        @JvmField public val Right: Vector3 = Vector3(x = 1f, y = 0f, z = 0f)

        /** Vector with z set to one and all other components set to zero. */
        @JvmField public val Backward: Vector3 = Vector3(x = 0f, y = 0f, z = 1f)

        /** Vector with z set to negative one and all other components set to zero. */
        @JvmField public val Forward: Vector3 = Vector3(x = 0f, y = 0f, z = -1f)

        /** Creates a new vector with all components set to [value]. */
        @JvmStatic public fun fromValue(value: Float): Vector3 = Vector3(value, value, value)

        /**
         * Returns the angle between this vector and the [other] vector in degrees. The result is
         * never greater than 180 degrees.
         */
        @JvmStatic
        public fun angleBetween(vector1: Vector3, vector2: Vector3): Float {
            val dot = vector1 dot vector2
            val magnitude = vector1.length * vector2.length

            if (magnitude < 1e-10f) {
                return 0.0f
            }

            // Clamp due to floating point precision errors that could cause dot to be > mag.
            // Would cause acos to return NaN.
            val cos = clamp(dot / magnitude, -1.0f, 1.0f)

            return acos(cos)
        }

        /** Returns the distance between this vector and the [other] vector. */
        @JvmStatic
        public fun distance(vector1: Vector3, vector2: Vector3): Float = (vector1 - vector2).length

        /**
         * Returns a new vector that is linearly interpolated between [start] and [end] using the
         * interpolated amount [ratio].
         *
         * If [ratio] is outside of the range `[0, 1]`, the returned vector will be extrapolated.
         */
        @JvmStatic
        public fun lerp(start: Vector3, end: Vector3, ratio: Float): Vector3 =
            Vector3(
                lerp(start.x, end.x, ratio),
                lerp(start.y, end.y, ratio),
                lerp(start.z, end.z, ratio),
            )

        /** Returns the minimum of each component of the two vectors. */
        @JvmStatic
        public fun min(a: Vector3, b: Vector3): Vector3 =
            Vector3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))

        /** Returns the maximum of each component of the two vectors. */
        @JvmStatic
        public fun max(a: Vector3, b: Vector3): Vector3 =
            Vector3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))

        /** Computes the vector projected from [vector] onto [planeNormal]. */
        @JvmStatic
        public fun projectOnPlane(vector: Vector3, planeNormal: Vector3): Vector3 =
            vector - planeNormal * (vector dot planeNormal) / (planeNormal dot planeNormal)

        /** Returns the absolute values of each component of the vector. */
        @JvmStatic
        public fun abs(vector: Vector3): Vector3 =
            Vector3(abs(vector.x), abs(vector.y), abs(vector.z))
    }
}
