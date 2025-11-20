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
 * Represents a four-dimensional position in space.
 *
 * @param x X component of the vector.
 * @param y Y component of the vector.
 * @param z Z component of the vector.
 * @param w W component of the vector.
 */
public class Vector4
@JvmOverloads
constructor(
    public val x: Float = 0F,
    public val y: Float = 0F,
    public val z: Float = 0F,
    public val w: Float = 0F,
) {
    /** The squared length of the vector. */
    public inline val lengthSquared: Float
        get() = x * x + y * y + z * z + w * w

    /** The length of the vector. */
    public inline val length: Float
        get() = sqrt(lengthSquared)

    /** Creates a new vector with the same values as the [other] vector. */
    public constructor(other: Vector4) : this(other.x, other.y, other.z, other.w)

    /** Negates this vector. */
    public operator fun unaryMinus(): Vector4 = Vector4(-x, -y, -z, -w)

    /** Returns a new vector with the sum of this vector and the [other] vector. */
    public operator fun plus(other: Vector4): Vector4 =
        Vector4(x + other.x, y + other.y, z + other.z, w + other.w)

    /** Returns a new vector with the difference of this vector and the [other] vector. */
    public operator fun minus(other: Vector4): Vector4 =
        Vector4(x - other.x, y - other.y, z - other.z, w - other.w)

    /** Get a new vector multiplied by a scalar amount. */
    public operator fun times(c: Float): Vector4 = Vector4(x * c, y * c, z * c, w * c)

    /**
     * Returns a new vector with each component of this vector multiplied by each corresponding
     * component of the [other] vector.
     */
    public fun scale(other: Vector4): Vector4 =
        Vector4(x * other.x, y * other.y, z * other.z, w * other.w)

    /** Returns a new vector with this vector divided by a scalar amount. */
    public operator fun div(c: Float): Vector4 = Vector4(x / c, y / c, z / c, w / c)

    /** Returns the dot product of this vector and the [other] vector. */
    public infix fun dot(other: Vector4): Float =
        x * other.x + y * other.y + z * other.z + w * other.w

    /** Returns the component-wise multiplicative inverse of this vector. */
    public fun inverse(): Vector4 {
        if (this.x == 0f || this.y == 0f || this.z == 0f || this.w == 0f) {
            throw IllegalArgumentException(
                "Cannot take the multiplicative inverse if any component of the Vector4 is zero."
            )
        }
        return Vector4(1 / this.x, 1 / this.y, 1 / this.z, 1 / this.w)
    }

    /** Returns the normalized version of this vector. */
    public fun toNormalized(): Vector4 {
        val norm = rsqrt(lengthSquared)

        return Vector4(x * norm, y * norm, z * norm, w * norm)
    }

    /**
     * Returns a new vector with the each component of this vector clamped between corresponding
     * components of [min] and [max] vectors.
     */
    public fun clamp(min: Vector4, max: Vector4): Vector4 {
        val clampedX = clamp(x, min.x, max.x)
        val clampedY = clamp(y, min.y, max.y)
        val clampedZ = clamp(z, min.z, max.z)
        val clampedW = clamp(w, min.w, max.w)

        return Vector4(clampedX, clampedY, clampedZ, clampedW)
    }

    /** Returns a copy of the vector. */
    @JvmOverloads
    public fun copy(
        x: Float = this.x,
        y: Float = this.y,
        z: Float = this.z,
        w: Float = this.w,
    ): Vector4 = Vector4(x, y, z, w)

    /** Returns true if this vector is equal to the [other]. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector4) return false

        return this.x == other.x && this.y == other.y && this.z == other.z && this.w == other.w
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + w.hashCode()
        return result
    }

    override fun toString(): String = "[x=$x, y=$y, z=$z, w=$w]"

    public companion object {
        /** Vector with all components set to zero. */
        @JvmField public val Zero: Vector4 = Vector4(x = 0f, y = 0f, z = 0f, w = 0f)

        /** Vector with all components set to one. */
        @JvmField public val One: Vector4 = Vector4(x = 1f, y = 1f, z = 1f, w = 1f)

        /** Creates a new vector with all components set to [value]. */
        @JvmStatic public fun fromValue(value: Float): Vector4 = Vector4(value, value, value, value)

        /**
         * Returns the angle between this vector and [other] vector in degrees. The result is never
         * greater than 180 degrees.
         */
        @JvmStatic
        public fun angleBetween(vector1: Vector4, vector2: Vector4): Float {
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
        public fun distance(vector1: Vector4, vector2: Vector4): Float = (vector1 - vector2).length

        /**
         * Returns a new vector that is linearly interpolated between [start] and [end] using the
         * interpolation amount [ratio].
         *
         * If [ratio] is outside of the range `[0, 1]`, the returned vector will be extrapolated.
         */
        @JvmStatic
        public fun lerp(start: Vector4, end: Vector4, ratio: Float): Vector4 =
            Vector4(
                lerp(start.x, end.x, ratio),
                lerp(start.y, end.y, ratio),
                lerp(start.z, end.z, ratio),
                lerp(start.w, end.w, ratio),
            )

        /** Returns the minimum of each component of the two vectors. */
        @JvmStatic
        public fun min(a: Vector4, b: Vector4): Vector4 =
            Vector4(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), min(a.w, b.w))

        /** Returns the maximum of each component of the two vectors. */
        @JvmStatic
        public fun max(a: Vector4, b: Vector4): Vector4 =
            Vector4(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z), max(a.w, b.w))

        /** Returns the absolute values of each component of the vector. */
        @JvmStatic
        public fun abs(vector: Vector4): Vector4 =
            Vector4(abs(vector.x), abs(vector.y), abs(vector.z), abs(vector.w))
    }
}
