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

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A FollowMode controls the motion of content as it is following another target. Currently, the
 * options include "soft", which gradually catches up to the target and "snap", which does not
 * continuously follow the target.
 */
@ExperimentalFollowingSubspaceApi
public abstract class FollowMode internal constructor() {
    internal abstract suspend fun start(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
    )

    protected val dispatcherOverride: CoroutineDispatcher
        get() = Companion.dispatcherOverride

    public companion object {
        /** The default duration, in milliseconds, for a soft follow animation. */
        internal val DEFAULT_SOFT_DURATION_MS: Int = 1500

        /** The minimum allowable duration in milliseconds for a soft follow animation. */
        internal val MIN_SOFT_DURATION_MS: Int = 100

        /**
         * Places content once based on the target's initial pose and does not follow subsequent
         * movements.
         *
         * @param dimensions A set of boolean flags which determine the dimensions of movement that
         *   are tracked. By default, all dimensions are tracked.
         */
        public fun snap(dimensions: TrackedDimensions = TrackedDimensions.All): FollowMode =
            SnapFollowMode(dimensions)

        /**
         * Follows the target as closely as possible.
         *
         * In contrast to [soft], where the content lags behind the target, [tight] follow matches
         * the target's movement instantly.
         *
         * @param dimensions A set of boolean flags which determine the dimensions of movement that
         *   are tracked. By default, all dimensions are tracked.
         */
        public fun tight(dimensions: TrackedDimensions = TrackedDimensions.All): FollowMode =
            TightFollowMode(dimensions)

        /**
         * Creates a mode where the content smoothly animates to follow the target's movements.
         *
         * This mode is driven by a first-order exponential decay model, which smoothly decelerates
         * the trailing entity as it approaches the target.
         *
         * @param dimensions A set of boolean flags which determine the dimensions of movement that
         *   are tracked. Defaults to [TrackedDimensions.All].
         * @param halfLifeMs Time in milliseconds it takes for the content to cover half the
         *   distance to the target. Defaults to [SoftFollowMode.DEFAULT_HALF_LIFE_MS].
         * @param startDelay Time in milliseconds to wait before starting the follow movement.
         *   Defaults to [SoftFollowMode.DEFAULT_START_DELAY].
         * @param startThresholds A set of thresholds that must be exceeded before movement starts.
         *   Defaults to [SoftFollowMode.DEFAULT_START_THRESHOLDS].
         * @return A [FollowMode] instance configured for soft following.
         */
        public fun soft(
            dimensions: TrackedDimensions = TrackedDimensions.All,
            @IntRange(from = 1) halfLifeMs: Long = SoftFollowMode.DEFAULT_HALF_LIFE_MS,
            @IntRange(from = 0) startDelay: Long = SoftFollowMode.DEFAULT_START_DELAY,
            startThresholds: FollowThresholds = SoftFollowMode.DEFAULT_START_THRESHOLDS,
        ): FollowMode =
            SoftFollowMode(
                dimensions = dimensions,
                halfLifeMs = halfLifeMs,
                startDelay = startDelay,
                startThresholds = startThresholds,
            )

        /**
         * Creates a mode where the content smoothly animates to follow the target's movements.
         *
         * @param durationMs Amount of milliseconds it takes for the content to catch up to the
         *   user.
         * @param dimensions A set of boolean flags which determine the dimensions of movement that
         *   are tracked. By default, all dimensions are tracked.
         * @return A [FollowMode] instance configured for soft following.
         */
        @Deprecated(
            message =
                "durationMs is deprecated as follow motion is now based on exponential decay. " +
                    "Use halfLifeMs instead.",
            replaceWith = ReplaceWith("FollowMode.soft(dimensions = dimensions)"),
        )
        public fun soft(
            @IntRange(from = 100) durationMs: Int,
            dimensions: TrackedDimensions = TrackedDimensions.All,
        ): FollowMode = soft(dimensions = dimensions)

        /**
         * Creates a mode where the content animates to follow the target's movements using an
         * exponential decay algorithm.
         *
         * This mode is driven by a first-order exponential decay model, matching the system's
         * native HeadFollower implementation.
         *
         * @param dimensions A set of boolean flags which determine the dimensions of movement that
         *   are tracked. By default, all dimensions are tracked.
         * @return A [FollowMode] instance configured for exponential decay.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun exponentialDecay(
            dimensions: TrackedDimensions = TrackedDimensions.All
        ): FollowMode = ExponentialDecayFollowMode(dimensions = dimensions)

        @VisibleForTesting
        internal var dispatcherOverride: CoroutineDispatcher = Dispatchers.Default
    }
}
