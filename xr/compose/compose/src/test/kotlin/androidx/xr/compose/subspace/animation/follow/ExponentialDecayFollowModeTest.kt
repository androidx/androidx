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

import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalFollowingSubspaceApi::class)
class ExponentialDecayFollowModeTest {

    @Test
    fun exponentialDecayFollowMode_equals_sameInstance_returnsTrue() {
        val mode = ExponentialDecayFollowMode()

        assertThat(mode).isEqualTo(mode)
    }

    @Test
    fun exponentialDecayFollowMode_equals_sameProperties_returnsTrue() {
        val mode1 =
            ExponentialDecayFollowMode(
                dimensions = TrackedDimensions.All,
                halfLifeMs = 200L,
                startDelay = 50L,
                startThresholds = FollowThresholds(translationMeters = 0.1f),
                settleThresholds = FollowThresholds(translationMeters = 0.01f),
            )
        val mode2 =
            ExponentialDecayFollowMode(
                dimensions = TrackedDimensions.All,
                halfLifeMs = 200L,
                startDelay = 50L,
                startThresholds = FollowThresholds(translationMeters = 0.1f),
                settleThresholds = FollowThresholds(translationMeters = 0.01f),
            )

        assertThat(mode1).isEqualTo(mode2)
    }

    @Test
    fun exponentialDecayFollowMode_equals_differentDimensions_returnsFalse() {
        val modeAll = ExponentialDecayFollowMode(dimensions = TrackedDimensions.All)
        val modeRotationOnly =
            ExponentialDecayFollowMode(dimensions = TrackedDimensions.RotationOnly)

        assertThat(modeAll).isNotEqualTo(modeRotationOnly)
    }

    @Test
    fun exponentialDecayFollowMode_equals_differentHalfLifeMs_returnsFalse() {
        val mode1 = ExponentialDecayFollowMode(halfLifeMs = 200L)
        val mode2 = ExponentialDecayFollowMode(halfLifeMs = 500L)

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun exponentialDecayFollowMode_equals_differentStartDelay_returnsFalse() {
        val mode1 = ExponentialDecayFollowMode(startDelay = 0L)
        val mode2 = ExponentialDecayFollowMode(startDelay = 100L)

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun exponentialDecayFollowMode_equals_differentStartThresholds_returnsFalse() {
        val mode1 =
            ExponentialDecayFollowMode(startThresholds = FollowThresholds(translationMeters = 0.1f))
        val mode2 =
            ExponentialDecayFollowMode(startThresholds = FollowThresholds(translationMeters = 0.5f))

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun exponentialDecayFollowMode_equals_differentSettleThresholds_returnsFalse() {
        val mode1 =
            ExponentialDecayFollowMode(
                settleThresholds = FollowThresholds(translationMeters = 0.01f)
            )
        val mode2 =
            ExponentialDecayFollowMode(
                settleThresholds = FollowThresholds(translationMeters = 0.05f)
            )

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun exponentialDecayFollowMode_equals_sameDimensionsDifferentMode_returnsFalse() {
        val mode1 = ExponentialDecayFollowMode(TrackedDimensions.All)
        val mode2 = SnapFollowMode(TrackedDimensions.All)

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun exponentialDecayFollowMode_equals_nullOrDifferentType_returnsFalse() {
        val mode = ExponentialDecayFollowMode(TrackedDimensions.All)

        assertFalse(mode.equals(null))
        assertFalse(mode.equals("Dummy String"))
    }

    @Test
    fun exponentialDecayFollowMode_hashCode_sameProperties_matches() {
        val mode1 =
            ExponentialDecayFollowMode(
                dimensions = TrackedDimensions.All,
                halfLifeMs = 200L,
                startDelay = 50L,
                startThresholds = FollowThresholds(translationMeters = 0.1f),
                settleThresholds = FollowThresholds(translationMeters = 0.01f),
            )
        val mode2 =
            ExponentialDecayFollowMode(
                dimensions = TrackedDimensions.All,
                halfLifeMs = 200L,
                startDelay = 50L,
                startThresholds = FollowThresholds(translationMeters = 0.1f),
                settleThresholds = FollowThresholds(translationMeters = 0.01f),
            )

        assertThat(mode1.hashCode()).isEqualTo(mode2.hashCode())
    }

    @Test
    fun exponentialDecayFollowMode_hashCode_differentDimensions_differs() {
        val modeAll = ExponentialDecayFollowMode(dimensions = TrackedDimensions.All)
        val modeRotationOnly =
            ExponentialDecayFollowMode(dimensions = TrackedDimensions.RotationOnly)

        assertThat(modeAll.hashCode()).isNotEqualTo(modeRotationOnly.hashCode())
    }

    @Test
    fun exponentialDecayFollowMode_hashCode_differentHalfLifeMs_differs() {
        val mode1 = ExponentialDecayFollowMode(halfLifeMs = 200L)
        val mode2 = ExponentialDecayFollowMode(halfLifeMs = 500L)

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }

    @Test
    fun exponentialDecayFollowMode_hashCode_differentStartDelay_differs() {
        val mode1 = ExponentialDecayFollowMode(startDelay = 0L)
        val mode2 = ExponentialDecayFollowMode(startDelay = 100L)

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }

    @Test
    fun exponentialDecayFollowMode_hashCode_differentStartThresholds_differs() {
        val mode1 =
            ExponentialDecayFollowMode(startThresholds = FollowThresholds(translationMeters = 0.1f))
        val mode2 =
            ExponentialDecayFollowMode(startThresholds = FollowThresholds(translationMeters = 0.5f))

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }

    @Test
    fun exponentialDecayFollowMode_hashCode_differentSettleThresholds_differs() {
        val mode1 =
            ExponentialDecayFollowMode(
                settleThresholds = FollowThresholds(translationMeters = 0.01f)
            )
        val mode2 =
            ExponentialDecayFollowMode(
                settleThresholds = FollowThresholds(translationMeters = 0.05f)
            )

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }
}
