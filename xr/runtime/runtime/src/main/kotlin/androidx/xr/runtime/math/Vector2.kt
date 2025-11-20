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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.xr.runtime.math

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Represents a position in the 2D plane.
 *
 * @property x X component of the vector.
 * @property y Y component of the vector.
 */
public class Vector2 @JvmOverloads constructor(public val x: Float = 0F, public val y: Float = 0F) {
    /** The squared length of the vector. */
    public inline val lengthSquared: Float
        get() = x * x + y * y

    /** The length of the vector. */
    public inline val length: Float
        get() = sqrt(lengthSquared)

    /** Creates a new vector with the same values as the [other] vector. */
    public constructor(other: Vector2) : this(other.x, other.y)

    /** Negates the values of this vector. */
    public inline operator fun unaryMinus(): Vector2 = Vector2(-x, -y)

    /** Returns a new vector with the sum of this vector and the [other] vector. */
    public operator fun plus(other: Vector2): Vector2 = Vector2(this.x + other.x, this.y + other.y)

    /** Returns a new vector with the difference of this vector and the [other] vector. */
    public inline operator fun minus(other: Vector2): Vector2 =
        Vector2(this.x - other.x, this.y - other.y)

    /** Returns a new vector multiplied by a scalar amount */
    public inline operator fun times(c: Float): Vector2 = Vector2(x * c, y * c)

    /**
     * Returns a new vector with each component of this vector multiplied by each corresponding
     * component of the [other] vector.
     */
    public inline fun scale(other: Vector2): Vector2 = Vector2(this.x * other.x, this.y * other.y)

    /** Returns a new vector with this vector divided by a scalar amount. */
    public inline operator fun div(c: Float): Vector2 = Vector2(x / c, y / c)

    /** Returns the component-wise multiplicative inverse of this vector. */
    public fun inverse(): Vector2 {
        if (this.x == 0f || this.y == 0f) {
            throw IllegalArgumentException(
                "Cannot take the multiplicative inverse if any component of the Vector2 is zero."
            )
        }
        return Vector2(1 / this.x, 1 / this.y)
    }

    /** Returns a normalized version of this vector. */
    public fun toNormalized(): Vector2 {
        val norm = rsqrt(lengthSquared)

        return Vector2(x * norm, y * norm)
    }

    /** Returns the cross product of this vector and the [other] vector. */
    public inline infix fun cross(other: Vector2): Float = this.x * other.y - this.y * other.x

    /** Returns the dot product of this vector and the [other] vector. */
    public inline infix fun dot(other: Vector2): Float = x * other.x + y * other.y

    /** Returns a new vector with the values clamped between [min] and [max] vectors. */
    public fun clamp(min: Vector2, max: Vector2): Vector2 {
        var clampedX = max(x, min.x)
        var clampedY = max(y, min.y)

        clampedX = min(clampedX, max.x)
        clampedY = min(clampedY, max.y)

        return Vector2(clampedX, clampedY)
    }

    /** Returns a copy of the vector. */
    @JvmOverloads
    public inline fun copy(x: Float = this.x, y: Float = this.y): Vector2 = Vector2(x, y)

    /** Returns true if this vector is equal to the [other]. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector2) return false

        return this.x == other.x && this.y == other.y
    }

    override fun hashCode(): Int = 31 * x.hashCode() + y.hashCode()

    override fun toString(): String = "[x=$x, y=$y]"

    public companion object {
        /** Vector with all components set to zero. */
        @JvmField public val Zero: Vector2 = Vector2(x = 0f, y = 0f)

        /** Vector with all components set to one. */
        @JvmField public val One: Vector2 = Vector2(x = 1f, y = 1f)

        /** Vector with y set to one and all other components set to zero. */
        @JvmField public val Up: Vector2 = Vector2(x = 0f, y = 1f)

        /** Vector with y set to negative one and all other components set to zero. */
        @JvmField public val Down: Vector2 = Vector2(x = 0f, y = -1f)

        /** Vector with x set to negative one and all other components set to zero. */
        @JvmField public val Left: Vector2 = Vector2(x = -1f, y = 0f)

        /** Vector with x set to one and all other components set to zero. */
        @JvmField public val Right: Vector2 = Vector2(x = 1f, y = 0f)

        /** Returns the distance between this vector and the [other] vector. */
        @JvmStatic
        public fun distance(vector1: Vector2, vector2: Vector2): Float = (vector1 - vector2).length

        /** Returns the angle between this vector and the [other] vector. */
        @JvmStatic
        public fun angularDistance(vector1: Vector2, vector2: Vector2): Float {
            val dot = vector1 dot vector2
            val magnitude = vector1.length * vector2.length

            if (magnitude < 1e-10f) {
                return 0.0f
            }

            // Clamp due to floating point precision errors that could cause dot to be > mag.
            // Would cause acos to return NaN.
            val cos = clamp(dot / magnitude, -1.0f, 1.0f)
            val angleRadians = acos(cos)

            return toDegrees(angleRadians)
        }

        /**
         * Returns a new vector that is linearly interpolated between [start] and [end] using the
         * interpolation amount [ratio].
         *
         * If [ratio] is outside of the range `[0, 1]`, the returned vector will be extrapolated.
         */
        @JvmStatic
        public fun lerp(start: Vector2, end: Vector2, ratio: Float): Vector2 =
            Vector2(lerp(start.x, end.x, ratio), lerp(start.y, end.y, ratio))

        /** Returns the absolute values of each component of the vector. */
        @JvmStatic public fun abs(vector: Vector2): Vector2 = Vector2(abs(vector.x), abs(vector.y))
    }
}
