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
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalFollowingSubspaceApi::class)
class SoftFollowModeTest {

    @Test
    fun followModeSoft_default_createsSoftFollowModeWithDefaults() {
        val mode = FollowMode.soft()

        assertIs<SoftFollowMode>(mode)
        assertThat(mode).isEqualTo(SoftFollowMode())
    }

    @Test
    fun followModeSoft_customArguments_propagatesAllParameters() {
        val customDimensions = TrackedDimensions.RotationOnly
        val customThresholds = FollowThresholds(translationMeters = 0.5f)
        val mode =
            FollowMode.soft(
                dimensions = customDimensions,
                halfLifeMs = 500L,
                startDelay = 100L,
                startThresholds = customThresholds,
            )

        assertIs<SoftFollowMode>(mode)
        assertThat(mode)
            .isEqualTo(
                SoftFollowMode(
                    dimensions = customDimensions,
                    halfLifeMs = 500L,
                    startDelay = 100L,
                    startThresholds = customThresholds,
                )
            )
    }

    @Test
    fun softFollowMode_delegatesToExponentialDecayFollowMode() {
        val customDimensions = TrackedDimensions.RotationOnly
        val customThresholds = FollowThresholds(translationMeters = 0.5f)
        val mode =
            SoftFollowMode(
                dimensions = customDimensions,
                halfLifeMs = 500L,
                startDelay = 100L,
                startThresholds = customThresholds,
            )

        assertIs<ExponentialDecayFollowMode>(mode.proxyMode)
        assertThat(mode.proxyMode)
            .isEqualTo(
                ExponentialDecayFollowMode(
                    dimensions = customDimensions,
                    halfLifeMs = 500L,
                    startDelay = 100L,
                    startThresholds = customThresholds,
                    settleThresholds = SoftFollowMode.DEFAULT_SETTLE_THRESHOLDS,
                )
            )
    }

    @Test
    fun softFollowMode_equals_sameInstance_returnsTrue() {
        val mode = SoftFollowMode()

        assertThat(mode).isEqualTo(mode)
    }

    @Test
    fun softFollowMode_equals_sameProperties_returnsTrue() {
        val mode1 = SoftFollowMode()
        val mode2 = SoftFollowMode()

        assertThat(mode1).isEqualTo(mode2)
    }

    @Test
    fun softFollowMode_equals_differentHalfLifeMs_returnsFalse() {
        val mode1 = SoftFollowMode(halfLifeMs = 80L)
        val mode2 = SoftFollowMode(halfLifeMs = 120L)

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun softFollowMode_equals_differentStartDelay_returnsFalse() {
        val mode1 = SoftFollowMode(startDelay = 0L)
        val mode2 = SoftFollowMode(startDelay = 100L)

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun softFollowMode_equals_differentStartTranslationThreshold_returnsFalse() {
        val mode1 = SoftFollowMode(startThresholds = FollowThresholds(translationMeters = 0.1f))
        val mode2 = SoftFollowMode(startThresholds = FollowThresholds(translationMeters = 0.5f))

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun softFollowMode_equals_differentStartPitchThreshold_returnsFalse() {
        val mode1 = SoftFollowMode(startThresholds = FollowThresholds(pitchDegrees = 1.0f))
        val mode2 = SoftFollowMode(startThresholds = FollowThresholds(pitchDegrees = 5.0f))

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun softFollowMode_equals_differentStartYawThreshold_returnsFalse() {
        val mode1 = SoftFollowMode(startThresholds = FollowThresholds(yawDegrees = 2.0f))
        val mode2 = SoftFollowMode(startThresholds = FollowThresholds(yawDegrees = 5.0f))

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun softFollowMode_equals_differentStartRollThreshold_returnsFalse() {
        val mode1 = SoftFollowMode(startThresholds = FollowThresholds(rollDegrees = 3.0f))
        val mode2 = SoftFollowMode(startThresholds = FollowThresholds(rollDegrees = 5.0f))

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun softFollowMode_equals_differentDimensions_returnsFalse() {
        val modeAll = SoftFollowMode(dimensions = TrackedDimensions.All)
        val modeRotationOnly = SoftFollowMode(dimensions = TrackedDimensions.RotationOnly)

        assertThat(modeAll).isNotEqualTo(modeRotationOnly)
    }

    @Test
    fun softFollowMode_equals_nullOrDifferentType_returnsFalse() {
        val mode = SoftFollowMode()

        assertFalse(mode.equals(null))
        assertFalse(mode.equals("Dummy String"))
    }

    @Test
    fun softFollowMode_hashCode_sameProperties_matches() {
        val mode1 = SoftFollowMode()
        val mode2 = SoftFollowMode()

        assertThat(mode1.hashCode()).isEqualTo(mode2.hashCode())
    }

    @Test
    fun softFollowMode_hashCode_differentHalfLifeMs_differs() {
        val mode1 = SoftFollowMode(halfLifeMs = 80L)
        val mode2 = SoftFollowMode(halfLifeMs = 120L)

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }

    @Test
    fun softFollowMode_hashCode_differentStartDelay_differs() {
        val mode1 = SoftFollowMode(startDelay = 0L)
        val mode2 = SoftFollowMode(startDelay = 100L)

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }

    @Test
    fun softFollowMode_hashCode_differentStartTranslationThreshold_differs() {
        val mode1 = SoftFollowMode(startThresholds = FollowThresholds(translationMeters = 0.1f))
        val mode2 = SoftFollowMode(startThresholds = FollowThresholds(translationMeters = 0.5f))

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }

    @Test
    fun softFollowMode_hashCode_differentStartPitchThreshold_differs() {
        val mode1 = SoftFollowMode(startThresholds = FollowThresholds(pitchDegrees = 1.0f))
        val mode2 = SoftFollowMode(startThresholds = FollowThresholds(pitchDegrees = 5.0f))

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }

    @Test
    fun softFollowMode_hashCode_differentStartYawThreshold_differs() {
        val mode1 = SoftFollowMode(startThresholds = FollowThresholds(yawDegrees = 2.0f))
        val mode2 = SoftFollowMode(startThresholds = FollowThresholds(yawDegrees = 5.0f))

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }

    @Test
    fun softFollowMode_hashCode_differentStartRollThreshold_differs() {
        val mode1 = SoftFollowMode(startThresholds = FollowThresholds(rollDegrees = 3.0f))
        val mode2 = SoftFollowMode(startThresholds = FollowThresholds(rollDegrees = 5.0f))

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
    }

    @Test
    fun softFollowMode_hashCode_differentDimensions_differs() {
        val modeAll = SoftFollowMode(dimensions = TrackedDimensions.All)
        val modeRotationOnly = SoftFollowMode(dimensions = TrackedDimensions.RotationOnly)

        assertThat(modeAll.hashCode()).isNotEqualTo(modeRotationOnly.hashCode())
    }
}
