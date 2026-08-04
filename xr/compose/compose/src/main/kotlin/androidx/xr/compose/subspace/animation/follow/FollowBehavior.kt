/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A FollowBehavior controls the motion of content as it is following another target, such as a
 * user's head. Currently, the options include "soft", which gradually catches up to the target and
 * "static", which does not continuously follow the target.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class FollowBehavior internal constructor() {
    internal abstract suspend fun start(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
        dimensions: TrackedDimensions = TrackedDimensions.All,
    )

    protected val dispatcherOverride: CoroutineDispatcher
        get() = Companion.dispatcherOverride

    public companion object {
        /** The default duration, in milliseconds, for a soft follow animation. */
        public const val DEFAULT_SOFT_DURATION_MS: Int = 1500
        /** The minimum allowable duration in milliseconds for a soft follow animation. */
        public const val MIN_SOFT_DURATION_MS: Int = 100

        /**
         * The content is placed once based on the target's initial pose and does not follow
         * subsequent movements.
         */
        public val Static: FollowBehavior = StaticFollowBehavior
        /** The content follows the target as closely as possible. */
        public val Tight: FollowBehavior = TightFollowBehavior

        /**
         * Creates a behavior where the content smoothly animates to follow the target's movements.
         *
         * This behavior is driven by a critically damped spring physics model, which uses
         * exponential decay to smoothly decelerate the trailing entity as it approaches the target.
         * The entity will accelerate to catch up and then decelerate without overshoot, simulating
         * real-world physical inertia.
         *
         * The use of this spring/exponential decay model is not optional, but the total duration of
         * the motion can be configured via [durationMs].
         *
         * @param durationMs Amount of milliseconds it takes for the content to catch up to the
         *   user. Default is [DEFAULT_SOFT_DURATION_MS] milliseconds. A value less than
         *   [MIN_SOFT_DURATION_MS] will be rounded up to [MIN_SOFT_DURATION_MS] to allow enough
         *   time to complete the content movement.
         * @return A [FollowBehavior] instance configured for soft following.
         */
        public fun Soft(
            @IntRange(from = MIN_SOFT_DURATION_MS.toLong())
            durationMs: Int = DEFAULT_SOFT_DURATION_MS
        ): FollowBehavior = SoftFollowBehavior(durationMs)

        /**
         * Creates a behavior where the content animates to follow the target's movements using an
         * exponential decay algorithm.
         *
         * This behavior is driven by a first-order exponential decay model, matching the system's
         * native HeadFollower implementation.
         *
         * @return A [FollowBehavior] instance configured for exponential decay.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun ExponentialDecay(): FollowBehavior = ExponentialDecayFollowBehavior()

        @VisibleForTesting
        internal var dispatcherOverride: CoroutineDispatcher = Dispatchers.Default
    }
}

/** Helper to return the tracked value if enabled, otherwise the fallback (initial) value. */
internal fun getTrackedValue(isTracked: Boolean, currentValue: Float, fallbackValue: Float): Float {
    return if (isTracked) currentValue else fallbackValue
}

internal fun getPoseByTrackedDimensions(
    pose: Pose,
    dimensions: TrackedDimensions,
    fallbackPose: Pose,
): Pose {
    // TODO(b/531806536): Check for Gimbal lock issues
    val currentEuler = pose.rotation.eulerAngles
    val fallbackEuler = fallbackPose.rotation.eulerAngles

    return Pose(
        translation =
            Vector3(
                x =
                    getTrackedValue(
                        isTracked = dimensions.isTranslationXTracked,
                        currentValue = pose.translation.x,
                        fallbackValue = fallbackPose.translation.x,
                    ),
                y =
                    getTrackedValue(
                        isTracked = dimensions.isTranslationYTracked,
                        currentValue = pose.translation.y,
                        fallbackValue = fallbackPose.translation.y,
                    ),
                z =
                    getTrackedValue(
                        isTracked = dimensions.isTranslationZTracked,
                        currentValue = pose.translation.z,
                        fallbackValue = fallbackPose.translation.z,
                    ),
            ),
        rotation =
            Quaternion.fromEulerAngles(
                pitch =
                    getTrackedValue(
                        isTracked = dimensions.isRotationXTracked,
                        currentValue = currentEuler.x,
                        fallbackValue = fallbackEuler.x,
                    ),
                yaw =
                    getTrackedValue(
                        isTracked = dimensions.isRotationYTracked,
                        currentValue = currentEuler.y,
                        fallbackValue = fallbackEuler.y,
                    ),
                roll =
                    getTrackedValue(
                        isTracked = dimensions.isRotationZTracked,
                        currentValue = currentEuler.z,
                        fallbackValue = fallbackEuler.z,
                    ),
            ),
    )
}

/**
 * A set of boolean flags which determine the dimensions of movement that are tracked.
 *
 * This is intended to be used with a [FollowBehavior]. These dimensions can be used to control how
 * one entity is follows another. For example, if a dev wants to place a marker on the floor showing
 * a user's position in a room, they might want to track only translationX and translationZ.
 * Possible values are: isTranslationXTracked, isTranslationYTracked, isTranslationZTracked,
 * isRotationXTracked, isRotationYTracked, isRotationZTracked or [TrackedDimensions.All].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TrackedDimensions(
    public val isTranslationXTracked: Boolean = false,
    public val isTranslationYTracked: Boolean = false,
    public val isTranslationZTracked: Boolean = false,
    public val isRotationXTracked: Boolean = false,
    public val isRotationYTracked: Boolean = false,
    public val isRotationZTracked: Boolean = false,
) {
    /**
     * returns a copy of this object with the given values updated.
     *
     * @param isTranslationXTracked Whether to track translation along the X axis.
     * @param isTranslationYTracked Whether to track translation along the Y axis.
     * @param isTranslationZTracked Whether to track translation along the Z axis.
     * @param isRotationXTracked Whether to track rotation around the X axis.
     * @param isRotationYTracked Whether to track rotation around the Y axis.
     * @param isRotationZTracked Whether to track rotation around the Z axis.
     */
    public fun copy(
        isTranslationXTracked: Boolean = this.isTranslationXTracked,
        isTranslationYTracked: Boolean = this.isTranslationYTracked,
        isTranslationZTracked: Boolean = this.isTranslationZTracked,
        isRotationXTracked: Boolean = this.isRotationXTracked,
        isRotationYTracked: Boolean = this.isRotationYTracked,
        isRotationZTracked: Boolean = this.isRotationZTracked,
    ): TrackedDimensions =
        TrackedDimensions(
            isTranslationXTracked = isTranslationXTracked,
            isTranslationYTracked = isTranslationYTracked,
            isTranslationZTracked = isTranslationZTracked,
            isRotationXTracked = isRotationXTracked,
            isRotationYTracked = isRotationYTracked,
            isRotationZTracked = isRotationZTracked,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackedDimensions) return false

        if (isTranslationXTracked != other.isTranslationXTracked) return false
        if (isTranslationYTracked != other.isTranslationYTracked) return false
        if (isTranslationZTracked != other.isTranslationZTracked) return false
        if (isRotationXTracked != other.isRotationXTracked) return false
        if (isRotationYTracked != other.isRotationYTracked) return false
        if (isRotationZTracked != other.isRotationZTracked) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isTranslationXTracked.hashCode()
        result = 31 * result + isTranslationYTracked.hashCode()
        result = 31 * result + isTranslationZTracked.hashCode()
        result = 31 * result + isRotationXTracked.hashCode()
        result = 31 * result + isRotationYTracked.hashCode()
        result = 31 * result + isRotationZTracked.hashCode()
        return result
    }

    override fun toString(): String {
        return "TrackedDimensions(" +
            "translationX=${isTranslationXTracked}, " +
            "translationY=${isTranslationYTracked}, " +
            "translationZ=${isTranslationZTracked}, " +
            "rotationX=${isRotationXTracked}, " +
            "rotationY=${isRotationYTracked}, " +
            "rotationZ=${isRotationZTracked})"
    }

    public companion object {
        /**
         * TrackedDimensions.ALL is provided as a convenient way to specify all 6 dimensions of a
         * pose.
         */
        public val All: TrackedDimensions =
            TrackedDimensions(
                isTranslationXTracked = true,
                isTranslationYTracked = true,
                isTranslationZTracked = true,
                isRotationXTracked = true,
                isRotationYTracked = true,
                isRotationZTracked = true,
            )

        /**
         * TrackedDimensions.RotationOnly is provided as a convenient way to specify tracking only
         * rotation dimensions (rotationX, rotationY, rotationZ).
         */
        public val RotationOnly: TrackedDimensions =
            TrackedDimensions(
                isTranslationXTracked = false,
                isTranslationYTracked = false,
                isTranslationZTracked = false,
                isRotationXTracked = true,
                isRotationYTracked = true,
                isRotationZTracked = true,
            )
    }
}
