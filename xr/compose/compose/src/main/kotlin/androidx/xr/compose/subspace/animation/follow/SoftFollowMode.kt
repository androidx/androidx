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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Creates a mode where the content smoothly animates to follow the user's movements, creating a
 * comfortable "soft follow" effect. This is the implementation for SoftFollowing which is
 * accessible through the public interface as FollowMode.soft()
 *
 * @param durationMs Amount of milliseconds it takes for the content to catch up to the user.
 *   Default is [FollowMode.DEFAULT_SOFT_DURATION_MS] milliseconds. A value less than
 *   [FollowMode.MIN_SOFT_DURATION_MS] will be rounded up to [FollowMode.MIN_SOFT_DURATION_MS] to
 *   allow enough time to complete the content movement.
 * @param dimensions A set of boolean flags which determine the dimensions of movement that are
 *   tracked. By default, all dimensions are tracked.
 */
@OptIn(ExperimentalFollowingSubspaceApi::class)
internal class SoftFollowMode(
    private val durationMs: Int = DEFAULT_SOFT_DURATION_MS,
    private val dimensions: TrackedDimensions = TrackedDimensions.All,
) : FollowMode() {
    private val animationDurationMs: Int = durationMs.coerceAtLeast(MIN_SOFT_DURATION_MS)
    private var trailingEntity: CoreGroupEntity? = null
    private val animationProgress = Animatable(initialValue = ANIMATION_START_VALUE)

    override suspend fun start(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
    ) = coroutineScope {
        this@SoftFollowMode.trailingEntity = trailingEntity
        val initialPose = trailingEntity.poseInMeters

        if (target is FollowTargetFlow) {
            withContext(dispatcherOverride) {
                val poseUpdatesFlow = target.poseUpdates(session)

                // The first device pose received is handled differently than the rest. There is no
                // animation to the trailingEntity, it will instantly appear at the device location.
                // It will also be made visible, enabled, at this time.
                // TODO: b/548122230 Avoid double flow subscription in following subspace.
                val pose = poseUpdatesFlow.first()
                var currentTargetPoseMeter: Pose =
                    dimensions.getPoseByTrackedDimensions(pose = pose, fallbackPose = initialPose)
                trailingEntity.poseInMeters = currentTargetPoseMeter
                trailingEntity.enabled = true
                var lastIntendedEndPoseMeter: Pose = currentTargetPoseMeter

                poseUpdatesFlow.collect { pose ->
                    // Determine the target pose using the source pose but ignoring the
                    // dimensions we are not tracking.
                    currentTargetPoseMeter =
                        dimensions.getPoseByTrackedDimensions(
                            pose = pose,
                            fallbackPose = initialPose,
                        )

                    // If the target has moved significantly enough, start the animation over.
                    if (
                        hasSignificantPoseChange(
                            pose1 = lastIntendedEndPoseMeter,
                            pose2 = currentTargetPoseMeter,
                        )
                    ) {
                        lastIntendedEndPoseMeter = currentTargetPoseMeter

                        launch {
                            animationProgress.snapTo(targetValue = ANIMATION_START_VALUE)
                            animate(endPoseMeter = lastIntendedEndPoseMeter)
                        }
                    }
                }
            }
        }
    }

    private suspend fun animate(endPoseMeter: Pose) {
        val startPoseMeter = trailingEntity?.poseInMeters ?: return

        animationProgress.animateTo(
            targetValue = ANIMATION_END_VALUE,
            animationSpec =
                tween(durationMillis = animationDurationMs, easing = Easing { smoothstep(it) }),
        ) {
            val nextPoseMeters =
                Pose.lerp(start = startPoseMeter, end = endPoseMeter, ratio = this.value)
            trailingEntity?.poseInMeters = nextPoseMeters
        }
    }

    private fun hasSignificantPoseChange(pose1: Pose, pose2: Pose): Boolean {
        // Check the translation and rotation difference between two poses.
        val translationDelta = (pose1.translation - pose2.translation).length
        val rotationDelta = Quaternion.angle(pose1.rotation, pose2.rotation)

        return translationDelta > TRANSLATION_THRESHOLD || rotationDelta > ROTATION_THRESHOLD
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SoftFollowMode) return false

        if (durationMs != other.durationMs) return false
        if (dimensions != other.dimensions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = durationMs.hashCode()
        result = 31 * result + dimensions.hashCode()
        return result
    }

    private companion object {
        private val TRANSLATION_THRESHOLD: Float = 0.1f
        private val ROTATION_THRESHOLD: Float = 3f
        private val ANIMATION_START_VALUE: Float = 0f
        private val ANIMATION_END_VALUE: Float = 1f

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
