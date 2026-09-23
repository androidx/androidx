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
 * This is the implementation for TightFollowing which is accessible through the public interface as
 * FollowMode.tight
 */
@OptIn(ExperimentalFollowingSubspaceApi::class)
internal class TightFollowMode(private val dimensions: TrackedDimensions = TrackedDimensions.All) :
    FollowMode() {

    @VisibleForTesting
    internal val proxyMode =
        ExponentialDecayFollowMode(
            dimensions = dimensions,
            halfLifeMillis = HALF_LIFE_MILLIS,
            startDelay = START_DELAY,
            startThresholds = START_THRESHOLDS,
            settleThresholds = SETTLE_THRESHOLDS,
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
        if (other !is TightFollowMode) return false

        return dimensions == other.dimensions
    }

    override fun hashCode(): Int = dimensions.hashCode()

    internal companion object {
        internal val HALF_LIFE_MILLIS: Long = 87L
        internal val START_DELAY: Long = 0L
        internal val START_THRESHOLDS: FollowThresholds = FollowThresholds.Zero
        internal val SETTLE_THRESHOLDS: FollowThresholds = FollowThresholds.Zero
    }
}
