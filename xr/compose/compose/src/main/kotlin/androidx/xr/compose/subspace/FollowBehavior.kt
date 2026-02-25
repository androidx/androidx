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
import androidx.annotation.VisibleForTesting
import androidx.xr.arcore.ArDevice
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import java.lang.Runnable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly

/**
 * A FollowBehavior controls the motion of content as it is following another target, such as a
 * user's head. Currently the options include "soft", which gradually catches up to the target and
 * "static", which does not continuously follow the target.
 */
@ExperimentalFollowingSubspaceApi
public sealed class FollowBehavior protected constructor() {
    protected var targetCurrentPose: Pose = Pose.Identity

    internal abstract suspend fun configure(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
        dimensions: TrackedDimensions = TrackedDimensions.All,
    )

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
         * Creates a behavior where the content smoothly animates to follow the target's movements,
         * creating a comfortable "soft follow" effect. This is implemented with the Hermite easing
         * algorithm, which accelerates the content then slows it down towards the end of the
         * motion, giving it a sense of real world physics. The use of the Hermite algorithm is not
         * optional but the total duration of the motion can be modified.
         *
         * @param durationMs Amount of milliseconds it takes for the content to catch up to the
         *   user. Default is `DEFAULT_SOFT_DURATION_MS` milliseconds. A value less than
         *   `MIN_SOFT_DURATION_MS` will be rounded up to `MIN_SOFT_DURATION_MS` to allow enough
         *   time to complete the content movement.
         * @return A [FollowBehavior] instance configured for soft following.
         */
        public fun Soft(
            @IntRange(from = MIN_SOFT_DURATION_MS.toLong())
            durationMs: Int = DEFAULT_SOFT_DURATION_MS
        ): FollowBehavior = SoftFollowBehavior(durationMs)

        @TestOnly
        @VisibleForTesting
        internal var dispatcherOverride: CoroutineDispatcher = Dispatchers.Default
    }
}

/**
 * Creates a behavior where the content smoothly animates to follow the user's movements, creating a
 * comfortable "soft follow" effect. This is the implementation for SoftFollowing which is
 * accessible through the public interface as FollowBehavior.soft()
 *
 * @param durationMs Amount of milliseconds it takes for the content to catch up to the user.
 *   Default is [DEFAULT_SOFT_DURATION_MS] milliseconds. A value less than [MIN_SOFT_DURATION_MS]
 *   will be rounded up to [MIN_SOFT_DURATION_MS] to allow enough time to complete the content
 *   movement.
 */
@OptIn(ExperimentalFollowingSubspaceApi::class)
internal class SoftFollowBehavior(private val durationMs: Int = DEFAULT_SOFT_DURATION_MS) :
    FollowBehavior() {

    private var currentAnimationJob: Job? = null
    private var trailingEntity: CoreGroupEntity? = null
    private var startPose: Pose = Pose.Identity
    private var endPose: Pose = Pose.Identity
    // Ensure at least one frame of animation
    private val totalFrames: Int =
        (durationMs / TIME_BETWEEN_ANIMATION_TICKS).toInt().coerceAtLeast(1)
    private var currentFrame: Int = 0

    override suspend fun configure(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
        dimensions: TrackedDimensions,
    ) = coroutineScope {
        this@SoftFollowBehavior.trailingEntity = trailingEntity
        val initialPose = trailingEntity.poseInMeters

        if (target is FollowTargetFlow) {
            withContext(dispatcherOverride) {
                target.poseUpdates.collect { pose ->
                    // Determine the target pose using the source pose but ignoring the
                    // dimensions we are not tracking.
                    targetCurrentPose = applyTrackedDimensions(pose, dimensions, initialPose)

                    // If the target has moved significantly enough, start the animation over.
                    if (shouldStartAnimation()) {
                        currentAnimationJob?.cancel()
                        startPose = trailingEntity?.poseInMeters ?: Pose.Identity
                        endPose = targetCurrentPose
                        currentFrame = 1
                        currentAnimationJob = this.launch { animate() }
                    }
                }
            }
        }
    }

    private fun applyTrackedDimensions(
        pose: Pose,
        dimensions: TrackedDimensions,
        fallbackPose: Pose,
    ): Pose {
        return Pose(
            translation =
                Vector3(
                    x =
                        getTrackedValue(
                            dimensions.isTranslationXTracked,
                            pose.translation.x,
                            fallbackPose.translation.x,
                        ),
                    y =
                        getTrackedValue(
                            dimensions.isTranslationYTracked,
                            pose.translation.y,
                            fallbackPose.translation.y,
                        ),
                    z =
                        getTrackedValue(
                            dimensions.isTranslationZTracked,
                            pose.translation.z,
                            fallbackPose.translation.z,
                        ),
                ),
            rotation =
                Quaternion(
                    x =
                        getTrackedValue(
                            dimensions.isRotationXTracked,
                            pose.rotation.x,
                            fallbackPose.rotation.x,
                        ),
                    y =
                        getTrackedValue(
                            dimensions.isRotationYTracked,
                            pose.rotation.y,
                            fallbackPose.rotation.y,
                        ),
                    z =
                        getTrackedValue(
                            dimensions.isRotationZTracked,
                            pose.rotation.z,
                            fallbackPose.rotation.z,
                        ),
                    w = pose.rotation.w,
                ),
        )
    }

    /*
     * Helper to return the tracked value if enabled, otherwise the fallback (initial) value.
     */
    private fun getTrackedValue(
        isTracked: Boolean,
        currentValue: Float,
        fallbackValue: Float,
    ): Float {
        return if (isTracked) currentValue else fallbackValue
    }

    // TODO(b/451647909): Investigate if Compose's built in animation APIs could handle the logic.
    private suspend fun animate() {
        while (currentFrame <= totalFrames) {
            // Calculate the raw and linear progress of the animation (a value from 0.0 to 1.0).
            val linearProgress: Float = currentFrame.toFloat() / totalFrames
            val easedProgress: Float = smoothstep(linearProgress)

            val nextPose = Pose.lerp(startPose, endPose, easedProgress)
            trailingEntity?.poseInMeters = nextPose
            currentFrame++

            delay(TIME_BETWEEN_ANIMATION_TICKS)
        }
    }

    private fun shouldStartAnimation(): Boolean {
        // Check the current position of the target entity compared to where the trailingEntity
        // is planning to be (endPose).
        val translationDelta = (endPose.translation - targetCurrentPose.translation).length
        val rotationDelta = Quaternion.angle(endPose.rotation, targetCurrentPose.rotation)

        return translationDelta > TRANSLATION_THRESHOLD || rotationDelta > ROTATION_THRESHOLD
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SoftFollowBehavior) return false
        return durationMs == other.durationMs
    }

    override fun hashCode(): Int {
        var result = javaClass.hashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }

    private companion object {
        private const val TIME_BETWEEN_ANIMATION_TICKS: Long = 10
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
 * This is the implementation for StaticFollowBehavior which is accessible through the public
 * interface as FollowBehavior.static()
 */
@OptIn(ExperimentalFollowingSubspaceApi::class)
internal object StaticFollowBehavior : FollowBehavior() {
    override suspend fun configure(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
        dimensions: TrackedDimensions,
    ) {
        if (target is FollowTargetFlow) {
            withContext(dispatcherOverride) {
                target.poseUpdates.collect { targetCurrentPose ->
                    // Update the trailingEntity just once.
                    if (trailingEntity.poseInMeters == Pose.Identity) {
                        trailingEntity.poseInMeters = targetCurrentPose
                        currentCoroutineContext().cancel()
                    }
                }
            }
        }
    }
}

/**
 * This is the implementation for TightFollowing which is accessible through the public interface as
 * FollowBehavior.tight()
 */
@OptIn(ExperimentalFollowingSubspaceApi::class)
internal object TightFollowBehavior : FollowBehavior() {
    override suspend fun configure(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
        dimensions: TrackedDimensions,
    ) {
        if (target is FollowTargetFlow) {
            withContext(dispatcherOverride) {
                target.poseUpdates.collect { pose -> trailingEntity.poseInMeters = pose }
            }
        }
    }
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
@ExperimentalFollowingSubspaceApi
public class TrackedDimensions(
    public val isTranslationXTracked: Boolean = false,
    public val isTranslationYTracked: Boolean = false,
    public val isTranslationZTracked: Boolean = false,
    public val isRotationXTracked: Boolean = false,
    public val isRotationYTracked: Boolean = false,
    public val isRotationZTracked: Boolean = false,
) {
    /** returns a copy of this object with the given values updated. */
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
    }
}

/**
 * A FollowTarget can be used with [androidx.xr.compose.spatial.FollowingSubspace] to have a set of
 * content follow a target such as an anchor or AR device.
 */
public sealed interface FollowTarget {
    public companion object {
        /**
         * By designating content to follow the AR device, it will keep that content near the device
         * camera and typically within the field of view, even as the device moves around.
         *
         * @param session The current [Session] instance.
         */
        public fun ArDevice(session: Session): FollowTarget = ArDeviceTarget(session)

        /**
         * Targeting an anchor allows content to be positioned relative to that anchor's location.
         *
         * @param anchorEntity represents the anchor which this
         *   [androidx.xr.compose.spatial.FollowingSubspace] will be tethered to. As the anchor
         *   moves, so will the [androidx.xr.compose.spatial.FollowingSubspace]
         */
        public fun Anchor(anchorEntity: AnchorEntity): FollowTarget = AnchorTarget(anchorEntity)
    }
}

internal interface FollowTargetFlow : FollowTarget {
    val poseUpdates: Flow<Pose>
}

/** A concrete [FollowTarget] that wraps the head pose updates from [ArDevice]. */
internal class ArDeviceTarget(private val session: Session) : FollowTargetFlow {
    // Distance to stay away from the target when following it.
    val offset: Pose = DEFAULT_OFFSET

    override val poseUpdates: Flow<Pose> =
        ArDevice.getInstance(session)
            .state
            // TODO(b/448689233): Initial head pose data is not reliable.
            .onStart { delay(INITIAL_POSE_DELAY_MS) }
            .map { state ->
                session.scene.perceptionSpace.transformPoseTo(
                    state.devicePose,
                    session.scene.activitySpace,
                )
            }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArDeviceTarget) return false
        return session == other.session
    }

    override fun hashCode(): Int {
        return session.hashCode()
    }

    internal companion object {
        const val INITIAL_POSE_DELAY_MS: Long = 1000
        // Distance to stay away from the target in meters.
        val DEFAULT_OFFSET: Pose = Pose(translation = Vector3(0f, 0f, -.5f))
    }
}

/**
 * A Trackable Anchor entity that wraps an [AnchorEntity] from SceneCore and implements
 * [FollowTarget] to provide a stream of pose updates.
 *
 * This implementation is designed to be constructed directly from an existing [AnchorEntity]
 * instance provided by the developer.
 */
internal class AnchorTarget(val anchorEntity: AnchorEntity) : FollowTargetFlow {
    private val pose: Pose
        get() = anchorEntity.getPose(Space.ACTIVITY)

    /**
     * A Flow that emits the latest pose updates whenever the underlying [AnchorEntity] is updated
     * by the system's perception stack.
     */
    override val poseUpdates: Flow<Pose> = callbackFlow {
        // Send the initial pose immediately upon collection.
        trySend(pose)

        val updateListener = Runnable { trySend(pose) }
        anchorEntity.setOnOriginChangedListener(updateListener)

        // Unregister the listener when the collector cancels or finishes.
        awaitClose { anchorEntity.setOnOriginChangedListener(null) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnchorTarget) return false
        return anchorEntity == other.anchorEntity
    }

    override fun hashCode(): Int {
        return anchorEntity.hashCode()
    }
}
