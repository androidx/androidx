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

import androidx.compose.runtime.withFrameNanos
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.pow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFollowingSubspaceApi::class)
internal class ExponentialDecayFollowMode(
    private val dimensions: TrackedDimensions = TrackedDimensions.All,
    private val halfLifeMs: Long = SoftFollowMode.DEFAULT_HALF_LIFE_MS,
    private val startDelay: Long = SoftFollowMode.DEFAULT_START_DELAY,
    private val startThresholds: FollowThresholds = SoftFollowMode.DEFAULT_START_THRESHOLDS,
    private val settleThresholds: FollowThresholds = SoftFollowMode.DEFAULT_SETTLE_THRESHOLDS,
) : FollowMode() {

    override suspend fun start(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
    ) = coroutineScope {
        val isAnimating = AtomicBoolean(false)
        val initialPoseMeter: Pose = trailingEntity.poseInMeters
        val followTargetFlow = target as? FollowTargetFlow ?: return@coroutineScope
        var currentTargetPoseMeter: Pose

        withContext(dispatcherOverride) {
            val poseUpdatesFlow = followTargetFlow.poseUpdates(session)

            // The first device pose received is handled differently than the rest. There is no
            // animation to the trailingEntity, it will instantly appear at the device location.
            // It will also be made visible, enabled, at this time.
            // TODO: b/548122230 Avoid double flow subscription in following subspace.
            val pose: Pose = poseUpdatesFlow.first()
            currentTargetPoseMeter =
                dimensions.getPoseByTrackedDimensions(pose = pose, fallbackPose = initialPoseMeter)
            trailingEntity.poseInMeters = currentTargetPoseMeter
            trailingEntity.enabled = true

            poseUpdatesFlow.collect { pose ->
                currentTargetPoseMeter =
                    dimensions.getPoseByTrackedDimensions(
                        pose = pose,
                        fallbackPose = initialPoseMeter,
                    )

                if (
                    !hasExceededThresholds(
                        pose1 = trailingEntity.poseInMeters,
                        pose2 = currentTargetPoseMeter,
                        thresholds = startThresholds,
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
        var totalElapsedSeconds: Float = 0.0f
        val startDelaySeconds: Float = startDelay / 1000.0f

        while (true) {
            val currentFrameTimeNanos: Long = withFrameNanos { it }

            // On the first frame, sync the baseline to the Compose frame clock.
            if (lastFrameTimeNanos == 0L) {
                lastFrameTimeNanos = currentFrameTimeNanos
            }

            val dtSeconds = calculateDtSeconds(lastFrameTimeNanos, currentFrameTimeNanos)
            lastFrameTimeNanos = currentFrameTimeNanos
            totalElapsedSeconds += dtSeconds

            if (totalElapsedSeconds < startDelaySeconds) {
                continue
            }

            val decayFactor = calculateDecayFactor(dtSeconds)

            // Interpolate between the current pose and the target pose using the decay factor.
            val currentPose = trailingEntity.poseInMeters
            val targetPose = targetPoseProvider()
            val nextPose = Pose.lerp(currentPose, targetPose, decayFactor)
            trailingEntity.poseInMeters = nextPose

            // If the gap to the target is significant, keep the animation going.
            if (hasExceededThresholds(nextPose, targetPose, settleThresholds)) {
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
     * Calculates the frame-rate independent interpolation factor using exponential decay half-life.
     *
     * Over each [halfLifeMs] interval, exactly 50% of the remaining distance to the target is
     * covered: decayFactor = 1 - 0.5^(dtSeconds / halfLifeSeconds)
     */
    private fun calculateDecayFactor(dtSeconds: Float): Float {
        val halfLifeSeconds = halfLifeMs / 1000f
        if (halfLifeSeconds <= 0.001f) return 1.0f
        return (1.0f - 0.5f.pow(dtSeconds / halfLifeSeconds)).coerceIn(0.0f, 1.0f)
    }

    private fun hasExceededThresholds(
        pose1: Pose,
        pose2: Pose,
        thresholds: FollowThresholds,
    ): Boolean {
        val translationDelta: Float = (pose1.translation - pose2.translation).length
        if (translationDelta > thresholds.translationMeters) {
            return true
        }

        val euler1 = pose1.rotation.eulerAngles
        val euler2 = pose2.rotation.eulerAngles
        val deltaPitch = angleDeltaDegrees(euler1.x, euler2.x)
        val deltaYaw = angleDeltaDegrees(euler1.y, euler2.y)
        val deltaRoll = angleDeltaDegrees(euler1.z, euler2.z)

        if (deltaPitch > thresholds.pitchDegrees) return true
        if (deltaYaw > thresholds.yawDegrees) return true
        if (deltaRoll > thresholds.rollDegrees) return true
        return false
    }

    private fun angleDeltaDegrees(a: Float, b: Float): Float {
        var diff = abs(a - b) % 360.0f
        if (diff > 180.0f) {
            diff = 360.0f - diff
        }
        return diff
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExponentialDecayFollowMode) return false

        return halfLifeMs == other.halfLifeMs &&
            startDelay == other.startDelay &&
            startThresholds == other.startThresholds &&
            settleThresholds == other.settleThresholds &&
            dimensions == other.dimensions
    }

    override fun hashCode(): Int {
        var result = halfLifeMs.hashCode()
        result = 31 * result + startDelay.hashCode()
        result = 31 * result + startThresholds.hashCode()
        result = 31 * result + settleThresholds.hashCode()
        result = 31 * result + dimensions.hashCode()
        return result
    }
}
