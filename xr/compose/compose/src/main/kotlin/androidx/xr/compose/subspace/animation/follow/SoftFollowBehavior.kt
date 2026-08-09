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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Creates a behavior where the content smoothly animates to follow the user's movements, creating a
 * comfortable "soft follow" effect. This is the implementation for SoftFollowing which is
 * accessible through the public interface as FollowBehavior.Soft()
 *
 * @param durationMs Amount of milliseconds it takes for the content to catch up to the user.
 *   Default is [FollowBehavior.DEFAULT_SOFT_DURATION_MS] milliseconds. A value less than
 *   [FollowBehavior.MIN_SOFT_DURATION_MS] will be rounded up to
 *   [FollowBehavior.MIN_SOFT_DURATION_MS] to allow enough time to complete the content movement.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SoftFollowBehavior(private val durationMs: Int = DEFAULT_SOFT_DURATION_MS) :
    FollowBehavior() {
    private val animationDurationMs: Int = durationMs.coerceAtLeast(MIN_SOFT_DURATION_MS)
    private var trailingEntity: CoreGroupEntity? = null
    private val animationProgress = Animatable(initialValue = ANIMATION_START_VALUE)

    override suspend fun start(
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
        dimensions: TrackedDimensions,
    ) = coroutineScope {
        this@SoftFollowBehavior.trailingEntity = trailingEntity
        val initialPose = trailingEntity.poseInMeters

        if (target is FollowTargetFlow) {
            withContext(dispatcherOverride) {
                // The first device pose received is handled differently than the rest. There is no
                // animation to the trailingEntity, it will instantly appear at the device location.
                // It will also be made visible, enabled, at this time.
                val pose = target.poseUpdates.first()
                var currentTargetPoseMeter: Pose =
                    getPoseByTrackedDimensions(
                        pose = pose,
                        dimensions = dimensions,
                        fallbackPose = initialPose,
                    )
                trailingEntity.poseInMeters = currentTargetPoseMeter
                trailingEntity.enabled = true
                var lastIntendedEndPoseMeter: Pose = currentTargetPoseMeter

                target.poseUpdates.collect { pose ->
                    // Determine the target pose using the source pose but ignoring the
                    // dimensions we are not tracking.
                    currentTargetPoseMeter =
                        getPoseByTrackedDimensions(
                            pose = pose,
                            dimensions = dimensions,
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
        if (other !is SoftFollowBehavior) return false

        return durationMs == other.durationMs
    }

    override fun hashCode(): Int {
        var result = javaClass.hashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }

    private companion object {
        private const val TRANSLATION_THRESHOLD: Float = 0.1f
        private const val ROTATION_THRESHOLD: Float = 3f
        private const val ANIMATION_START_VALUE: Float = 0f
        private const val ANIMATION_END_VALUE: Float = 1f

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
