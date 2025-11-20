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

package androidx.xr.compose.subspace

import androidx.annotation.IntRange
import androidx.xr.arcore.ArDevice
import androidx.xr.compose.spatial.ExperimentalUserSubspaceApi
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.scene
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay

/**
 * A LockingBehavior controls the motion of content as it is following (or "locked" to) another
 * entity, such as a user's head. Currently the options include "static", which does not
 * continuously follow the target, and "lazy", which gradually catches up to the target.
 */
@ExperimentalUserSubspaceApi
public abstract class LockingBehavior internal constructor() {
    protected var targetCurrentPose: Pose = Pose.Identity

    internal abstract suspend fun configure(
        session: Session,
        trailingEntity: CoreGroupEntity,
        lockTo: BodyPart = BodyPart.Head,
        lockDimensions: LockDimensions = LockDimensions.All,
    )

    public companion object {
        public const val DEFAULT_LAZY_DURATION_MS: Int = 1500
        public const val MIN_LAZY_DURATION_MS: Long = 100

        /**
         * The content is placed once based on the user's initial pose and does not follow
         * subsequent movements.
         */
        public fun static(): LockingBehavior = StaticLockingBehavior()

        /**
         * Creates a behavior where the content smoothly animates to follow the user's movements,
         * creating a comfortable "lazy follow" effect. This is implemented with the Hermite easing
         * algorithm, which accelerates the content then slows it down towards the end of the
         * motion, giving it a sense of real world physics. The use of the Hermite algorithm is not
         * optional but the total duration of the motion can be modified.
         *
         * @param durationMs Amount of milliseconds it takes for the content to catch up to the
         *   user. Default is `DEFAULT_LAZY_DURATION_MS` milliseconds. A value less than
         *   `MIN_LAZY_DURATION_MS` will be rounded up to `MIN_LAZY_DURATION_MS` to allow enough
         *   time to complete the content movement.
         * @return A [LockingBehavior] instance configured for lazy locking.
         */
        public fun lazy(
            @IntRange(from = MIN_LAZY_DURATION_MS) durationMs: Int = DEFAULT_LAZY_DURATION_MS
        ): LockingBehavior = LazyLockingBehavior(durationMs)
    }
}

/**
 * Creates a behavior where the content smoothly animates to follow the user's movements, creating a
 * comfortable "lazy follow" effect. This is the implementation for LazyLocking which is accessible
 * through the public interface as LockingBehavior.lazy()
 *
 * @param durationMs Amount of milliseconds it takes for the content to catch up to the user.
 *   Default is 1500 milliseconds. A value less than 100 will be rounded up to 100 to allow enough
 *   time to complete the content movement.
 */
@OptIn(ExperimentalUserSubspaceApi::class)
internal class LazyLockingBehavior(private val durationMs: Int = DEFAULT_LAZY_DURATION_MS) :
    LockingBehavior() {

    private var trailingEntity: CoreGroupEntity? = null
    private var startPose: Pose = Pose.Identity
    // TODO(b/451647909): Investigate if Compose's built in animation APIs could handle the logic.
    private var isAnimating: Boolean = false
    // Ensure at least one frame of animation
    private val totalFrames: Int =
        (durationMs / TIME_BETWEEN_ANIMATION_TICKS).toInt().coerceAtLeast(1)
    private var currentFrame: Int = 0

    override suspend fun configure(
        session: Session,
        trailingEntity: CoreGroupEntity,
        lockTo: BodyPart,
        lockDimensions: LockDimensions,
    ) {
        this.trailingEntity = trailingEntity
        if (lockTo != BodyPart.Head) return

        // TODO(b/448689233): Initial head pose data is not reliable.
        // Skipping over it with delay.
        delay(1000)

        val arDevice = ArDevice.getInstance(session)
        arDevice.state.collect { state ->
            val headPose =
                session.scene.perceptionSpace.transformPoseTo(
                    state.devicePose,
                    session.scene.activitySpace,
                )

            // Determine the target pose using the source pose but ignoring the
            // dimensions not specified in parameter lockDimensions.
            targetCurrentPose =
                Pose(
                    translation =
                        Vector3(
                            x =
                                if (lockDimensions.isTranslationXTracked) {
                                    headPose.translation.x
                                } else {
                                    0f
                                },
                            y =
                                if (lockDimensions.isTranslationYTracked) {
                                    headPose.translation.y
                                } else {
                                    0f
                                },
                            z =
                                if (lockDimensions.isTranslationZTracked) {
                                    headPose.translation.z
                                } else {
                                    0f
                                },
                        ),
                    rotation =
                        Quaternion(
                            x =
                                if (lockDimensions.isRotationXTracked) {
                                    headPose.rotation.x
                                } else {
                                    0f
                                },
                            y =
                                if (lockDimensions.isRotationYTracked) {
                                    headPose.rotation.y
                                } else {
                                    0f
                                },
                            z =
                                if (lockDimensions.isRotationZTracked) {
                                    headPose.rotation.z
                                } else {
                                    0f
                                },
                            w = headPose.rotation.w,
                        ),
                )
            handleNewPose()
        }
    }

    private suspend fun handleNewPose() {
        // If animation is in progress in another thread, do nothing.
        if (isAnimating) {
            return
        }

        // If the target has moved significantly enough, start the animation over.
        if (shouldStartAnimation()) {
            // The current pose of the trailingEntity
            startPose = trailingEntity?.poseInMeters ?: Pose.Identity
            currentFrame = 1
            isAnimating = true
            animate()
        }
    }

    private suspend fun animate() {
        // If the animation has finished, update state and stop.
        if (currentFrame > totalFrames) {
            isAnimating = false
            return
        }

        // Calculate the raw and linear progress of the animation (a value from 0.0 to 1.0).
        val linearProgress: Float = currentFrame.toFloat() / totalFrames
        val easedProgress: Float = smoothstep(linearProgress)

        val nextPose = Pose.lerp(startPose, targetCurrentPose, easedProgress)
        trailingEntity?.poseInMeters = nextPose
        currentFrame++

        // TODO(b/451648700): Should consider locking onto the animation cycle that already
        // exists instead of creating a new one.
        delay(TIME_BETWEEN_ANIMATION_TICKS)
        // A recursive suspend call.
        animate()
    }

    private suspend fun shouldStartAnimation(): Boolean {
        val trailingEntityCurrentPose = trailingEntity?.poseInMeters ?: Pose.Identity

        val translationDelta =
            (trailingEntityCurrentPose.translation - targetCurrentPose.translation).length
        val rotationDelta =
            Quaternion.angle(trailingEntityCurrentPose.rotation, targetCurrentPose.rotation)

        return translationDelta > TRANSLATION_THRESHOLD || rotationDelta > ROTATION_THRESHOLD
    }

    private companion object {
        private const val TIME_BETWEEN_ANIMATION_TICKS: Long = 50
        private const val TRANSLATION_THRESHOLD: Float = 0.1f
        private const val ROTATION_THRESHOLD: Float = 3f

        /**
         * Applies Smoothstep function (a specific implementation of a Cubic Hermite interpolation
         * curve). to a linear value. This creates a smooth S-curve effect that goes through
         * "ease-in, accelerate, then ease-out" effect for animations.
         *
         * The function uses the formula `f(t) = 3t² - 2t³`. The coefficients 3 and 2 are
         * mathematically derived to be the simplest polynomial that satisfies four essential
         * conditions for a smooth transition:
         * 1. It starts at 0 (f(0) = 0).
         * 2. It ends at 1 (f(1) = 1).
         * 3. Its rate of change (speed) is 0 at the start (f'(0) = 0).
         * 4. Its rate of change (speed) is 0 at the end (f'(1) = 0).
         *
         * @param x A value between 0.0 and 1.0.
         * @return The smoothed value, also between 0.0 and 1.0.
         */
        private fun smoothstep(x: Float): Float {
            return x * x * (3f - 2f * x)
        }
    }
}

/**
 * This is the implementation for StaticLocking which is accessible through the public interface as
 * LockingBehavior.static()
 */
@OptIn(ExperimentalUserSubspaceApi::class)
internal class StaticLockingBehavior() : LockingBehavior() {
    override suspend fun configure(
        session: Session,
        trailingEntity: CoreGroupEntity,
        lockTo: BodyPart,
        lockDimensions: LockDimensions,
    ) {
        // TODO(b/448689233): Initial head pose data is not reliable.
        // Skipping over it with delay.
        delay(1000)

        val arDevice = ArDevice.getInstance(session)
        arDevice.state.collect { state ->
            val targetCurrentPose =
                session.scene.perceptionSpace.transformPoseTo(
                    state.devicePose,
                    session.scene.activitySpace,
                )

            // Update the trailingEntity just once.
            if (trailingEntity.poseInMeters == Pose.Identity) {
                trailingEntity.poseInMeters = targetCurrentPose
                coroutineContext.cancel()
            }
        }
    }
}

/** A type-safe representation of a part of the user's body that can be used as a lock target. */
@JvmInline
@ExperimentalUserSubspaceApi
public value class BodyPart private constructor(private val value: Int) {
    public companion object {
        /**
         * The "Head" represents both where the user is located and where they are looking.
         *
         * This is typically tracked in order to move content to where it's always in the user's
         * field of view. Please configure headTracking to use this feature. This configure can be
         * set within the session as follows session.config.headTracking ==
         * Config.HeadTrackingMode.LAST_KNOWN
         *
         * **Permissions:** Note that this requires the `android.permission.HEAD_TRACKING`
         * permission.
         */
        public val Head: BodyPart = BodyPart(1)
    }
}

/**
 * A set of boolean flags which determine the dimensions of movement that are tracked.
 *
 * This is intended to be used with a [LockingBehavior]. These dimensions can be used to control how
 * one entity is locked to another. For example, if a dev wants to place a marker on the floor
 * showing a user's position in a room, they might want to track only translationX and translationZ.
 * Possible values are: translationX, translationY, translationZ, rotationX, rotationY, rotationZ or
 * "ALL".
 */
@ExperimentalUserSubspaceApi
public class LockDimensions(
    public val isTranslationXTracked: Boolean = false,
    public val isTranslationYTracked: Boolean = false,
    public val isTranslationZTracked: Boolean = false,
    public val isRotationXTracked: Boolean = false,
    public val isRotationYTracked: Boolean = false,
    public val isRotationZTracked: Boolean = false,
) {
    public companion object {
        /**
         * LockDimensions.ALL is provided as a convenient way to specify all 6 dimensions of a pose.
         */
        public val All: LockDimensions =
            LockDimensions(
                isTranslationXTracked = true,
                isTranslationYTracked = true,
                isTranslationZTracked = true,
                isRotationXTracked = true,
                isRotationYTracked = true,
                isRotationZTracked = true,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LockDimensions) return false

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
        return "LockDimensions(" +
            "translationX=${isTranslationXTracked}, " +
            "translationY=${isTranslationYTracked}, " +
            "translationZ=${isTranslationZTracked}, " +
            "rotationX=${isRotationXTracked}, " +
            "rotationY=${isRotationYTracked}, " +
            "rotationZ=${isRotationZTracked})"
    }
}
