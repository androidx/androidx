/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.subspace.animation.follow

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3

/**
 * A set of boolean flags which determine the dimensions of movement that are tracked, and will be
 * used to update the content.
 *
 * This is intended to be used with a [FollowMode]. These dimensions can be used to control how one
 * entity is follows another. For example, if a dev wants to place a marker on the floor showing a
 * user's position in a room, they might want to track only x and z. Possible values are:
 * isXTracked, isYTracked, isZTracked, isPitchTracked, isYawTracked, isRollTracked,
 * [TrackedDimensions.All], or [TrackedDimensions.RotationOnly].
 *
 * @param isXTracked Whether to track translation along the X axis.
 * @param isYTracked Whether to track translation along the Y axis.
 * @param isZTracked Whether to track translation along the Z axis.
 * @param isPitchTracked Whether to track pitch rotation.
 * @param isYawTracked Whether to track yaw rotation.
 * @param isRollTracked Whether to track roll rotation.
 */
// TODO(b/550528756): Add unit tests for TrackedDimensions
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TrackedDimensions(
    public val isXTracked: Boolean = false,
    public val isYTracked: Boolean = false,
    public val isZTracked: Boolean = false,
    public val isPitchTracked: Boolean = false,
    public val isYawTracked: Boolean = false,
    public val isRollTracked: Boolean = false,
) {
    internal fun getPoseByTrackedDimensions(pose: Pose, fallbackPose: Pose): Pose {
        // TODO(b/531806536): Check for Gimbal lock issues
        val translation =
            Vector3(
                x =
                    getTrackedValue(
                        isTracked = isXTracked,
                        currentValue = pose.translation.x,
                        fallbackValue = fallbackPose.translation.x,
                    ),
                y =
                    getTrackedValue(
                        isTracked = isYTracked,
                        currentValue = pose.translation.y,
                        fallbackValue = fallbackPose.translation.y,
                    ),
                z =
                    getTrackedValue(
                        isTracked = isZTracked,
                        currentValue = pose.translation.z,
                        fallbackValue = fallbackPose.translation.z,
                    ),
            )

        val rotation =
            if (isPitchTracked && isYawTracked && isRollTracked) {
                // Avoids conversion to Euler and back if avoidable.
                pose.rotation
            } else {
                val currentEuler = pose.rotation.eulerAngles
                val fallbackEuler = fallbackPose.rotation.eulerAngles
                Quaternion.fromEulerAngles(
                    pitch =
                        getTrackedValue(
                            isTracked = isPitchTracked,
                            currentValue = currentEuler.x,
                            fallbackValue = fallbackEuler.x,
                        ),
                    yaw =
                        getTrackedValue(
                            isTracked = isYawTracked,
                            currentValue = currentEuler.y,
                            fallbackValue = fallbackEuler.y,
                        ),
                    roll =
                        getTrackedValue(
                            isTracked = isRollTracked,
                            currentValue = currentEuler.z,
                            fallbackValue = fallbackEuler.z,
                        ),
                )
            }

        return Pose(translation = translation, rotation = rotation)
    }

    private fun getTrackedValue(
        isTracked: Boolean,
        currentValue: Float,
        fallbackValue: Float,
    ): Float {
        return if (isTracked) currentValue else fallbackValue
    }

    /**
     * Returns a copy of this object with the given values updated.
     *
     * @param isXTracked Whether to track translation along the X axis.
     * @param isYTracked Whether to track translation along the Y axis.
     * @param isZTracked Whether to track translation along the Z axis.
     * @param isPitchTracked Whether to track rotation around the X axis.
     * @param isYawTracked Whether to track rotation around the Y axis.
     * @param isRollTracked Whether to track rotation around the Z axis.
     */
    public fun copy(
        isXTracked: Boolean = this.isXTracked,
        isYTracked: Boolean = this.isYTracked,
        isZTracked: Boolean = this.isZTracked,
        isPitchTracked: Boolean = this.isPitchTracked,
        isYawTracked: Boolean = this.isYawTracked,
        isRollTracked: Boolean = this.isRollTracked,
    ): TrackedDimensions =
        TrackedDimensions(
            isXTracked = isXTracked,
            isYTracked = isYTracked,
            isZTracked = isZTracked,
            isPitchTracked = isPitchTracked,
            isYawTracked = isYawTracked,
            isRollTracked = isRollTracked,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackedDimensions) return false

        if (isXTracked != other.isXTracked) return false
        if (isYTracked != other.isYTracked) return false
        if (isZTracked != other.isZTracked) return false
        if (isPitchTracked != other.isPitchTracked) return false
        if (isYawTracked != other.isYawTracked) return false
        if (isRollTracked != other.isRollTracked) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isXTracked.hashCode()
        result = 31 * result + isYTracked.hashCode()
        result = 31 * result + isZTracked.hashCode()
        result = 31 * result + isPitchTracked.hashCode()
        result = 31 * result + isYawTracked.hashCode()
        result = 31 * result + isRollTracked.hashCode()
        return result
    }

    override fun toString(): String {
        return "TrackedDimensions(" +
            "x=${isXTracked}, " +
            "y=${isYTracked}, " +
            "z=${isZTracked}, " +
            "pitch=${isPitchTracked}, " +
            "yaw=${isYawTracked}, " +
            "roll=${isRollTracked})"
    }

    public companion object {
        /**
         * TrackedDimensions.All is provided as a convenient way to specify all 6 dimensions of a
         * pose.
         */
        public val All: TrackedDimensions =
            TrackedDimensions(
                isXTracked = true,
                isYTracked = true,
                isZTracked = true,
                isPitchTracked = true,
                isYawTracked = true,
                isRollTracked = true,
            )

        /**
         * TrackedDimensions.RotationOnly is provided as a convenient way to specify tracking only
         * rotation dimensions (pitch, yaw, roll).
         */
        public val RotationOnly: TrackedDimensions =
            TrackedDimensions(
                isXTracked = false,
                isYTracked = false,
                isZTracked = false,
                isPitchTracked = true,
                isYawTracked = true,
                isRollTracked = true,
            )
    }
}
