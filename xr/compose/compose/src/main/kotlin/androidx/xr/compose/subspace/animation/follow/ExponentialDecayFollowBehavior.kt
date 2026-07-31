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

import androidx.annotation.RestrictTo
import androidx.compose.runtime.withFrameNanos
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ExponentialDecayFollowBehavior : FollowBehavior() {
    override suspend fun configure(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
        dimensions: TrackedDimensions,
    ) = coroutineScope {
        val isAnimating = AtomicBoolean(false)
        val initialPoseMeter: Pose = trailingEntity.poseInMeters
        val followTargetFlow = target as? FollowTargetFlow ?: return@coroutineScope
        var currentTargetPoseMeter = Pose.Identity

        withContext(dispatcherOverride) {
            val pose: Pose = target.poseUpdates.first()
            currentTargetPoseMeter =
                getPoseByTrackedDimensions(
                    pose = pose,
                    dimensions = dimensions,
                    fallbackPose = initialPoseMeter,
                )
            trailingEntity.poseInMeters = currentTargetPoseMeter
            trailingEntity.enabled = true

            followTargetFlow.poseUpdates.collect { pose ->
                currentTargetPoseMeter =
                    getPoseByTrackedDimensions(
                        pose = pose,
                        dimensions = dimensions,
                        fallbackPose = initialPoseMeter,
                    )

                if (
                    !hasSignificantPoseChange(
                        pose1 = trailingEntity.poseInMeters,
                        pose2 = currentTargetPoseMeter,
                        translationThreshold = TRANSLATION_THRESHOLD,
                        rotationThreshold = ROTATION_THRESHOLD,
                    )
                ) {
                    return@collect
                }

                if (!isAnimating.compareAndSet(false, true)) {
                    return@collect
                }

                launch {
                    try {
                        animate(trailingEntity, targetPoseProvider = { currentTargetPoseMeter })
                    } finally {
                        isAnimating.set(false)
                    }
                }
            }
        }
    }

    /**
     * Animates the trailing entity to smoothly follow the target.
     *
     * On each frame, this calculates the elapsed time since the last frame and uses an exponential
     * decay formula to determine the next position. The animation stops when the entity is close
     * enough to the target.
     */
    private suspend fun animate(trailingEntity: CoreGroupEntity, targetPoseProvider: () -> Pose) {
        // The baseline starts uninitialized.
        var lastFrameTimeNanos: Long = 0L

        while (true) {
            // Suspend exactly ONCE per frame
            val currentFrameTimeNanos: Long = withFrameNanos { it }

            // On the first frame, sync the baseline to the Compose frame clock.
            if (lastFrameTimeNanos == 0L) {
                lastFrameTimeNanos = currentFrameTimeNanos
            }

            val dtSeconds = calculateDtSeconds(lastFrameTimeNanos, currentFrameTimeNanos)
            lastFrameTimeNanos = currentFrameTimeNanos

            val decayFactor = calculateDecayFactor(dtSeconds)

            // Interpolate between the current pose and the target pose using the decay factor.
            val currentPose = trailingEntity.poseInMeters
            val targetPose = targetPoseProvider()
            val nextPose = Pose.lerp(currentPose, targetPose, decayFactor)
            trailingEntity.poseInMeters = nextPose

            // If the gap to the target is significant, keep the animation going.
            if (
                hasSignificantPoseChange(
                    pose1 = nextPose,
                    pose2 = targetPose,
                    translationThreshold = SETTLE_TRANSLATION_THRESHOLD,
                    rotationThreshold = SETTLE_ROTATION_THRESHOLD,
                )
            ) {
                continue
            }

            trailingEntity.poseInMeters = targetPose
            break
        }
    }

    /**
     * Calculates elapsed time (dt) in seconds, clamped between 1ms and 100ms to prevent large
     * motion jumps due to frame drops or thread suspension.
     */
    private fun calculateDtSeconds(lastFrameTimeNanos: Long, currentFrameTimeNanos: Long): Float {
        val dtNanos: Long =
            (currentFrameTimeNanos - lastFrameTimeNanos).coerceIn(1_000_000L, 100_000_000L)

        return dtNanos / 1_000_000_000f
    }

    /**
     * Calculates the frame-rate independent interpolation factor using exponential decay.
     *
     * The general formula for exponential decay interpolation is: 1 - base^dtSeconds
     *
     * where "base" is a constant that corresponds to the level of friction in the system. Here, we
     * use (1 / LERP_DIVISOR) as our decay base, giving: decayFactor = 1 - (1 /
     * LERP_DIVISOR)^dtSeconds
     *
     * The decay factor will start at zero, quickly approach 1 and level off.
     */
    private fun calculateDecayFactor(dtSeconds: Float): Float {
        return 1.0f - (1.0f / LERP_DIVISOR).pow(dtSeconds)
    }

    private fun hasSignificantPoseChange(
        pose1: Pose,
        pose2: Pose,
        translationThreshold: Float,
        rotationThreshold: Float,
    ): Boolean {
        val translationDelta: Float = (pose1.translation - pose2.translation).length
        val rotationDelta: Float = Quaternion.angle(pose1.rotation, pose2.rotation)

        return translationDelta > translationThreshold || rotationDelta > rotationThreshold
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExponentialDecayFollowBehavior) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    private companion object {
        private const val LERP_DIVISOR = 18000f
        private const val SETTLE_TRANSLATION_THRESHOLD: Float = 0.01f
        private const val SETTLE_ROTATION_THRESHOLD: Float = 0.01f
        private const val TRANSLATION_THRESHOLD: Float = 0.1f
        private const val ROTATION_THRESHOLD: Float = 3f
    }
}
