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

import androidx.annotation.VisibleForTesting
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session

/**
 * Creates a mode where the content smoothly animates to follow the user's movements, creating a
 * comfortable "soft follow" effect. This is the implementation for SoftFollowing which is
 * accessible through the public interface as FollowMode.soft()
 *
 * @param dimensions A set of boolean flags which determine the dimensions of movement that are
 *   tracked. By default, all dimensions are tracked.
 * @param halfLifeMs Time in milliseconds it takes for the content to cover half the distance to the
 *   user.
 * @param startDelay Time in milliseconds to wait before starting the follow movement.
 * @param startThresholds A set of thresholds that must be exceeded before movement starts.
 */
@OptIn(ExperimentalFollowingSubspaceApi::class)
internal class SoftFollowMode(
    private val dimensions: TrackedDimensions = TrackedDimensions.All,
    private val halfLifeMs: Long = DEFAULT_HALF_LIFE_MS,
    private val startDelay: Long = DEFAULT_START_DELAY,
    private val startThresholds: FollowThresholds = DEFAULT_START_THRESHOLDS,
) : FollowMode() {

    @VisibleForTesting
    internal val proxyMode =
        ExponentialDecayFollowMode(
            dimensions = dimensions,
            halfLifeMs = halfLifeMs,
            startDelay = startDelay,
            startThresholds = startThresholds,
            settleThresholds = DEFAULT_SETTLE_THRESHOLDS,
        )

    override suspend fun start(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
    ) {
        proxyMode.start(session, trailingEntity, target)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SoftFollowMode) return false

        return dimensions == other.dimensions &&
            halfLifeMs == other.halfLifeMs &&
            startDelay == other.startDelay &&
            startThresholds == other.startThresholds
    }

    override fun hashCode(): Int {
        var result = dimensions.hashCode()
        result = 31 * result + halfLifeMs.hashCode()
        result = 31 * result + startDelay.hashCode()
        result = 31 * result + startThresholds.hashCode()
        return result
    }

    internal companion object {
        internal val DEFAULT_HALF_LIFE_MS: Long = 200L
        internal val DEFAULT_START_DELAY: Long = 300L
        internal val DEFAULT_START_THRESHOLDS: FollowThresholds =
            FollowThresholds(
                translationMeters = 0.1f,
                pitchDegrees = 3f,
                yawDegrees = 3f,
                rollDegrees = 3f,
            )
        internal val DEFAULT_SETTLE_THRESHOLDS: FollowThresholds =
            FollowThresholds(
                translationMeters = 0.01f,
                pitchDegrees = 0.01f,
                yawDegrees = 0.01f,
                rollDegrees = 0.01f,
            )
    }
}
