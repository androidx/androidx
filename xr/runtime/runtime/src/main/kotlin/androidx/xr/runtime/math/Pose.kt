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

import androidx.xr.runtime.math.Quaternion.Companion.fromAxisAngle
import androidx.xr.runtime.math.Quaternion.Companion.fromLookTowards
import kotlin.math.abs
import kotlin.math.asin

/**
 * Represents an immutable rigid transformation from one coordinate space to another.
 *
 * @property translation the translation component of this pose
 * @property rotation the rotation component of this pose
 */
public class Pose
@JvmOverloads
constructor(
    public val translation: Vector3 = Vector3(),
    public val rotation: Quaternion = Quaternion(),
) {

    /** Returns a pose that performs the opposite transformation. */
    public val inverse: Pose
        get() = invert()

    /** The up vector in the local coordinate system. */
    public inline val up: Vector3
        get() = rotation * Vector3.Up

    /** The down vector in the local coordinate system. */
    public inline val down: Vector3
        get() = rotation * Vector3.Down

    /** The left vector in the local coordinate system. */
    public inline val left: Vector3
        get() = rotation * Vector3.Left

    /** The right vector in the local coordinate system. */
    public inline val right: Vector3
        get() = rotation * Vector3.Right

    /** The forward vector in the local coordinate system. */
    public inline val forward: Vector3
        get() = rotation * Vector3.Forward

    /** The backward vector in the local coordinate system. */
    public inline val backward: Vector3
        get() = rotation * Vector3.Backward

    /** Creates a new pose with the same values as the [other] pose. */
    public constructor(other: Pose) : this(other.translation, other.rotation)

    /** Returns the result of composing `this` with [other]. */
    public infix fun compose(other: Pose): Pose =
        Pose(rotation * other.translation + this.translation, rotation * other.rotation)

    /** Returns a pose that performs the opposite transformation. */
    private fun invert(): Pose {
        val outRotation = rotation.inverse
        val outTranslation = -(outRotation * translation)

        return Pose(outTranslation, outRotation)
    }

    /** Translates this pose by the given [translation]. */
    public fun translate(translation: Vector3): Pose =
        Pose(this.translation + translation, this.rotation)

    /** Rotates this pose by the given [rotation]. */
    public fun rotate(rotation: Quaternion): Pose = Pose(this.translation, this.rotation * rotation)

    /**
     * Transforms the provided [point] by the pose by applying both the [rotation] and the
     * [translation] components of the pose. This is because a point represents a specific location
     * in space. It needs to account for the position, scale and orientation of the space it is in.
     */
    public infix fun transformPoint(point: Vector3): Vector3 = rotation * point + translation

    /**
     * Transforms the provided [vector] by the pose by only applying the [rotation] component of the
     * pose. This is because a vector represents a direction and magnitude, not a specific location.
     * It only needs to account for the scale and orientation of the space it is in since it has no
     * position.
     */
    public infix fun transformVector(vector: Vector3): Vector3 = rotation * vector

    /**
     * Calculates a rotation to align this pose's local Z-axis [forward] with the given pose's [up]
     * direction.
     *
     * @param other The given pose.
     * @return A Quaternion representing the rotation to apply to the pose.
     */
    public fun getForwardVectorToUpRotation(other: Pose): Quaternion {
        val otherPoseUp = other.up.toNormalized()
        val poseVectorX = this.right.toNormalized()
        // Y vector(poseUp) = Z vector (planeNormal) x X vector (entityRight)
        var poseUp = otherPoseUp.cross(poseVectorX)
        if (poseUp.lengthSquared < EPSILON) {
            // Fallback: Use the existing pose's up vector to derive a new basis
            // This prevents the NaN crash while maintaining the best possible orientation
            poseUp = this.up
        }
        return fromLookTowards(otherPoseUp, poseUp)
    }

    /**
     * Calculates a rotation to align this pose's local Y-axis [up] with the given pose's up
     * direction.
     *
     * @param other The given pose.
     * @return A Quaternion representing the rotation to apply to the pose.
     */
    public fun getUpVectorToUpRotation(other: Pose): Quaternion {
        val otherPoseUp: Vector3 = other.up.toNormalized()
        val poseUp: Vector3 = this.up.toNormalized()
        val otherPoseRight: Vector3 = other.right.toNormalized()
        var newPose: Pose = this
        // Handle the case where the pose's up vector is orthogonal to the plane normal.
        // Rotate 90 degrees around the plane's right vector to tip the pose's up vector
        // out of the plane.
        if (abs(otherPoseUp.dot(poseUp)) < EPSILON) {
            val angle = Math.toDegrees(asin(poseUp.cross(otherPoseUp).length.toDouble())).toFloat()
            newPose = this.rotate(fromAxisAngle(otherPoseRight, angle))
        }

        // Get the x-vector of the pose so that we can use it to create the z-vector that is
        // in the direction of the pose.
        val poseRight = newPose.right.toNormalized()
        // The forward direction (z-vector) is the cross product of the pose x-vector and the
        // y-vector.
        var forwardDirection = poseRight.cross(otherPoseUp)

        // Handle the edge case where poseRight is parallel to planeNormal.
        // In this case, the cross product is zero, so fall back to the
        // pose's current forward vector.
        if (forwardDirection.lengthSquared < EPSILON) {
            forwardDirection = newPose.forward
        }
        return Quaternion.fromLookTowards(forwardDirection, otherPoseUp)
    }

    /**
     * Returns a copy of the pose.
     *
     * @param translation the new translation for the copied pose
     * @param rotation the new rotation for the copied pose
     */
    @JvmOverloads
    public fun copy(
        translation: Vector3 = this.translation,
        rotation: Quaternion = this.rotation,
    ): Pose = Pose(translation, rotation)

    /** Returns true if this pose is equal to the [other]. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pose) return false

        return this.translation == other.translation && this.rotation == other.rotation
    }

    override fun hashCode(): Int = 31 * translation.hashCode() + rotation.hashCode()

    override fun toString(): String = "Pose{\n\tTranslation=$translation\n\tRotation=$rotation\n}"

    public companion object {
        /** Returns a new pose using the identity rotation. */
        @JvmField public val Identity: Pose = Pose()
        private const val EPSILON = 1e-6f

        /**
         * Returns a new pose oriented to look at [target] from [eye] position with [up] as the up
         * vector.
         *
         * @param eye the position from which to look at [target]
         * @param target the target position to look at
         * @param up a vector indicating the general "up" direction
         * @return the pose oriented to look at [target] from [eye] position with [up] as the up
         *   vector
         */
        @JvmStatic
        @JvmOverloads
        public fun fromLookAt(eye: Vector3, target: Vector3, up: Vector3 = Vector3.Up): Pose {
            val forward = (target - eye).toNormalized()
            val rotation = Quaternion.fromLookTowards(forward, up)

            return Pose(eye, rotation)
        }

        /**
         * Returns the distance between the two poses.
         *
         * @param lhs the first pose
         * @param rhs the second pose
         */
        @JvmStatic
        public fun distance(lhs: Pose, rhs: Pose): Float =
            Vector3.Companion.distance(lhs.translation, rhs.translation)

        /**
         * Returns a new pose that is linearly interpolated between [start] and [end] using the
         * interpolation amount [ratio]. The position is [lerped][Vector3.lerp], but the rotation
         * will be [slerped][Quaternion.slerp] if the angles are far apart.
         *
         * If [ratio] is outside of the range `[0, 1]`, the returned pose will be extrapolated.
         *
         * @param start the starting pose
         * @param end the ending pose
         * @param ratio the interpolation ratio
         */
        @JvmStatic
        public fun lerp(start: Pose, end: Pose, ratio: Float): Pose {
            val interpolatedPosition =
                Vector3.Companion.lerp(start.translation, end.translation, ratio)

            val interpolatedRotation =
                if (start.rotation.dot(end.rotation) < 0.9995f) { // Check if angle is large
                    Quaternion.Companion.slerp(start.rotation, end.rotation, ratio)
                } else {
                    // If the angle is small, lerp can be used for efficiency.
                    Quaternion.Companion.lerp(start.rotation, end.rotation, ratio)
                }

            return Pose(interpolatedPosition, interpolatedRotation)
        }
    }
}
