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
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents a rotation component in three-dimensional space. Any vector can be provided and the
 * resulting quaternion will be normalized at construction time.
 *
 * @param x the x value of the quaternion.
 * @param y the y value of the quaternion.
 * @param z the z value of the quaternion.
 * @param w the rotation of the unit vector, in radians.
 */
public class Quaternion
@JvmOverloads
constructor(x: Float = 0F, y: Float = 0F, z: Float = 0F, w: Float = 1F) {
    /** The normalized x component of the quaternion. */
    public val x: Float
    /** The normalized y component of the quaternion. */
    public val y: Float
    /** The normalized z component of the quaternion. */
    public val z: Float
    /** The normalized w component of the quaternion. */
    public val w: Float

    init {
        val length = sqrt(x * x + y * y + z * z + w * w)
        this.x = x / length
        this.y = y / length
        this.z = z / length
        this.w = w / length
    }

    /** Returns a new quaternion with the inverse rotation. Assumes unit length. */
    public inline val inverse: Quaternion
        get() = Quaternion(-x, -y, -z, w)

    /**
     * Returns this quaternion as Euler angles (in degrees) applied in YXZ (yaw, pitch, roll) order.
     */
    public val eulerAngles: Vector3
        get() = toYawPitchRoll()

    /** Returns this quaternion as an axis/angle (in degrees) pair. */
    public val axisAngle: Pair<Vector3, Float>
        get() = toAxisAngle()

    /** Creates a new quaternion with the same values as the [other] quaternion. */
    public constructor(other: Quaternion) : this(other.x, other.y, other.z, other.w)

    /** Creates a new quaternion using the components of a [Vector4]. */
    internal constructor(vector: Vector4) : this(vector.x, vector.y, vector.z, vector.w)

    /** Flips the sign of the quaternion, but represents the same rotation. */
    public inline operator fun unaryMinus(): Quaternion = Quaternion(-x, -y, -z, -w)

    /** Returns a new quaternion with the sum of this quaternion and [other] quaternion. */
    public inline operator fun plus(other: Quaternion): Quaternion =
        Quaternion(x + other.x, y + other.y, z + other.z, w + other.w)

    /**
     * Returns a new quaternion with the difference of this quaternion and the [other] quaternion.
     */
    public inline operator fun minus(other: Quaternion): Quaternion =
        Quaternion(x - other.x, y - other.y, z - other.z, w - other.w)

    /** Rotates a [Vector3] by this quaternion. */
    public inline operator fun times(src: Vector3): Vector3 {
        val qx = x
        val qy = y
        val qz = z
        val qw = w
        val vx = src.x
        val vy = src.y
        val vz = src.z

        val rx = qy * vz - qz * vy + qw * vx
        val ry = qz * vx - qx * vz + qw * vy
        val rz = qx * vy - qy * vx + qw * vz
        val sx = qy * rz - qz * ry
        val sy = qz * rx - qx * rz
        val sz = qx * ry - qy * rx
        return Vector3(2 * sx + vx, 2 * sy + vy, 2 * sz + vz)
    }

    /**
     * Returns a new quaternion with the product of this quaternion and the [other] quaternion. The
     * order of the multiplication is `[this] * [other]`.
     */
    public inline operator fun times(other: Quaternion): Quaternion {
        val lx = this.x
        val ly = this.y
        val lz = this.z
        val lw = this.w
        val rx = other.x
        val ry = other.y
        val rz = other.z
        val rw = other.w

        return Quaternion(
            lw * rx + lx * rw + ly * rz - lz * ry,
            lw * ry - lx * rz + ly * rw + lz * rx,
            lw * rz + lx * ry - ly * rx + lz * rw,
            lw * rw - lx * rx - ly * ry - lz * rz,
        )
    }

    /** Returns a new quaternion with the product of this quaternion and a scalar amount. */
    public operator fun times(c: Float): Quaternion = Quaternion(x * c, y * c, z * c, w * c)

    /** Returns a new quaternion with this quaternion divided by a scalar amount. */
    public operator fun div(c: Float): Quaternion = Quaternion(x / c, y / c, z / c, w / c)

    /** Returns a new quaternion with a normalized rotation. */
    public fun toNormalized(): Quaternion {
        val norm = rsqrt(x * x + y * y + z * z + w * w)
        return this * norm
    }

    /** Returns the dot product of this quaternion and the [other] quaternion. */
    public inline infix fun dot(other: Quaternion): Float =
        x * other.x + y * other.y + z * other.z + w * other.w

    /**
     * Get a [Vector3] containing the pitch, yaw and roll in degrees, extracted in YXZ (yaw, pitch,
     * roll) order.
     */
    private fun toYawPitchRoll(): Vector3 {
        val test = w * x - y * z
        if (test > EULER_THRESHOLD) {
            // There is a singularity when the pitch is directly up, so calculate the
            // angles another way.
            return Vector3(90f, toDegrees(-2 * atan2(z, w)), 0f)
        }
        if (test < -EULER_THRESHOLD) {
            // There is a singularity when the pitch is directly down, so calculate the
            // angles another way.
            return Vector3(-90f, toDegrees(2 * atan2(z, w)), 0f)
        }

        val pitch = asin(2 * test)
        val yaw = atan2(2 * (w * y + x * z).toDouble(), 1.0 - 2 * (x * x + y * y)).toFloat()
        val roll = atan2(2 * (w * z + x * y).toDouble(), 1.0 - 2 * (x * x + z * z)).toFloat()

        return Vector3(toDegrees(pitch), toDegrees(yaw), toDegrees(roll))
    }

    /** Returns a Pair containing the axis of rotation and the angle of rotation in degrees. */
    private fun toAxisAngle(): Pair<Vector3, Float> {
        val normalized = this.toNormalized()
        val angleRadians = 2 * acos(normalized.w)
        val sinHalfAngle = sin(angleRadians / 2)
        val axis =
            if (sinHalfAngle < 0.0001f) {
                Vector3.Right // Default axis when angle is 0
            } else {
                Vector3(
                    normalized.x / sinHalfAngle,
                    normalized.y / sinHalfAngle,
                    normalized.z / sinHalfAngle,
                )
            }

        return Pair(axis, toDegrees(angleRadians))
    }

    /** Returns a copy of the quaternion. */
    @JvmOverloads
    public fun copy(
        x: Float = this.x,
        y: Float = this.y,
        z: Float = this.z,
        w: Float = this.w,
    ): Quaternion = Quaternion(x, y, z, w)

    /** Returns true if this quaternion is equal to the [other]. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Quaternion) return false

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

    /** Returns a new quaternion with the identity rotation. */
    public companion object {
        private const val EULER_THRESHOLD: Float = 0.49999994f
        private const val COS_THRESHOLD: Float = 0.9995f

        @JvmField public val Identity: Quaternion = Quaternion()

        /** Returns a new quaternion representing the rotation from one vector to another. */
        @JvmStatic
        public fun fromRotation(start: Vector3, end: Vector3): Quaternion {
            val startNorm = start.toNormalized()
            val endNorm = end.toNormalized()

            val cosTheta = startNorm.dot(endNorm)
            if (cosTheta < -COS_THRESHOLD) {
                // Special case when vectors in opposite directions: no "ideal" rotation axis
                // Guess one; any work as long as perpendicular to start
                var rotationAxis = Vector3.Backward.cross(startNorm)
                if (rotationAxis.lengthSquared < 0.01f) {
                    rotationAxis =
                        Vector3.Right.cross(
                            startNorm
                        ) // pick new rotation axis as the original was parallel
                }
                return Quaternion.Companion.fromAxisAngle(rotationAxis, 180f)
            }

            val rotationAxis = startNorm.cross(endNorm)

            return Quaternion(rotationAxis.x, rotationAxis.y, rotationAxis.z, 1 + cosTheta)
                .toNormalized()
        }

        /** Returns a new quaternion representing the rotation from one quaternion to another. */
        @JvmStatic
        public fun fromRotation(start: Quaternion, end: Quaternion): Quaternion =
            Quaternion(end * start.inverse).toNormalized()

        /** Returns a new quaternion with the specified forward and upward directions. */
        @JvmStatic
        public fun fromLookTowards(forward: Vector3, up: Vector3): Quaternion {
            val forwardNormalized = forward.toNormalized()
            val right = (up cross forwardNormalized).toNormalized()
            val upNormalized = (forwardNormalized cross right).toNormalized()

            val m00 = right.x
            val m01 = right.y
            val m02 = right.z
            val m10 = upNormalized.x
            val m11 = upNormalized.y
            val m12 = upNormalized.z
            val m20 = forwardNormalized.x
            val m21 = forwardNormalized.y
            val m22 = forwardNormalized.z

            val trace = m00 + m11 + m22
            return if (trace > 0) {
                val s = 0.5f / sqrt(trace + 1.0f)
                Quaternion((m12 - m21) * s, (m20 - m02) * s, (m01 - m10) * s, 0.25f / s)
            } else {
                if (m00 > m11 && m00 > m22) {
                    val s = sqrt(1.0f + m00 - m11 - m22) * 2.0f
                    Quaternion(0.25f * s, (m01 + m10) / s, (m02 + m20) / s, (m12 - m21) / s)
                } else if (m11 > m22) {
                    val s = sqrt(1.0f + m11 - m00 - m22) * 2.0f
                    Quaternion((m01 + m10) / s, 0.25f * s, (m12 + m21) / s, (m20 - m02) / s)
                } else {
                    val s = sqrt(1.0f + m22 - m00 - m11) * 2.0f
                    Quaternion((m02 + m20) / s, (m12 + m21) / s, 0.25f * s, (m01 - m10) / s)
                }
            }
        }

        /** Creates a new quaternion using an axis/angle to define the rotation. */
        @JvmStatic
        public fun fromAxisAngle(axis: Vector3, degrees: Float): Quaternion =
            Quaternion(
                sin(0.5f * toRadians(degrees)) * axis.toNormalized().x,
                sin(0.5f * toRadians(degrees)) * axis.toNormalized().y,
                sin(0.5f * toRadians(degrees)) * axis.toNormalized().z,
                cos(0.5f * toRadians(degrees)),
            )

        /**
         * Returns a new quaternion using Euler angles (in degrees) to define the rotation in YXZ
         * (yaw, pitch, roll) order.
         */
        @JvmStatic
        public fun fromEulerAngles(eulerAngles: Vector3): Quaternion =
            Quaternion(fromAxisAngle(Vector3.Up, eulerAngles.y)) *
                Quaternion(fromAxisAngle(Vector3.Right, eulerAngles.x)) *
                Quaternion(fromAxisAngle(Vector3.Backward, eulerAngles.z))

        /**
         * Returns a new quaternion using Euler angles (in degrees) to define the rotation in YXZ
         * (yaw, pitch, roll) order.
         */
        @JvmStatic
        public fun fromEulerAngles(pitch: Float, yaw: Float, roll: Float): Quaternion =
            Quaternion(fromAxisAngle(Vector3.Up, yaw)) *
                Quaternion(fromAxisAngle(Vector3.Right, pitch)) *
                Quaternion(fromAxisAngle(Vector3.Backward, roll))

        /**
         * Returns a new quaternion that is linearly interpolated between [start] and [end] using
         * the interpolation amount [ratio].
         *
         * If [ratio] is outside of the range `[0, 1]`, the returned quaternion will be
         * extrapolated.
         */
        @JvmStatic
        public fun lerp(start: Quaternion, end: Quaternion, ratio: Float): Quaternion =
            Quaternion(
                lerp(start.x, end.x, ratio),
                lerp(start.y, end.y, ratio),
                lerp(start.z, end.z, ratio),
                lerp(start.w, end.w, ratio),
            )

        /**
         * Returns a new quaternion that is spherically interpolated between [start] and [end] using
         * the interpolation amount [ratio]. If [ratio] is 0, this returns [start]. As [ratio]
         * approaches 1, the result of this function may approach either `+end` or `-end` (whichever
         * is closest to [start]).
         *
         * If [ratio] is outside of the range `[0, 1]`, the returned quaternion will be
         * extrapolated.
         */
        @JvmStatic
        public fun slerp(start: Quaternion, end: Quaternion, ratio: Float): Quaternion {
            val orientationStart = start
            var orientationEnd = end

            // cosTheta0 provides the angle between the rotations at t = 0
            var cosTheta0 = orientationStart.dot(orientationEnd)

            // Flip end rotation to get shortest path between the two rotations
            if (cosTheta0 < 0.0f) {
                orientationEnd = -orientationEnd
                cosTheta0 = -cosTheta0
            }

            // Small rotations can use linear interpolation
            if (cosTheta0 > COS_THRESHOLD) {
                return lerp(orientationStart, orientationEnd, ratio)
            }

            val sinTheta0 = sqrt(1.0 - cosTheta0 * cosTheta0)
            val theta0 = acos(cosTheta0)
            val thetaT = theta0 * ratio
            val sinThetaT = sin(thetaT)
            val costThetaT = cos(thetaT)

            val s1 = sinThetaT / sinTheta0
            val s0 = costThetaT - cosTheta0 * s1

            // Do component-wise multiplication since s0 and s1 could be near-zero which would cause
            // precision issues when (quat * 0.0f) is normalized due to division by near-zero
            // length.
            return Quaternion(
                orientationStart.x * s0.toFloat() + orientationEnd.x * s1.toFloat(),
                orientationStart.y * s0.toFloat() + orientationEnd.y * s1.toFloat(),
                orientationStart.z * s0.toFloat() + orientationEnd.z * s1.toFloat(),
                orientationStart.w * s0.toFloat() + orientationEnd.w * s1.toFloat(),
            )
        }

        /** Returns the angle between [start] and [end] quaternion in degrees. */
        @JvmStatic
        public fun angle(start: Quaternion, end: Quaternion): Float =
            toDegrees(2.0f * acos(abs(clamp(dot(start, end), -1.0f, 1.0f))))

        /** Returns the dot product of two quaternions. */
        @JvmStatic
        public fun dot(lhs: Quaternion, rhs: Quaternion): Float =
            lhs.x * rhs.x + lhs.y * rhs.y + lhs.z * rhs.z + lhs.w * rhs.w
    }
}
