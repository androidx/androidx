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
class FollowThresholdsTest {

    @Test
    fun followThresholds_companionZero_hasZeroValues() {
        assertThat(FollowThresholds.Zero.translationMeters).isEqualTo(0.0f)
        assertThat(FollowThresholds.Zero.pitchDegrees).isEqualTo(0.0f)
        assertThat(FollowThresholds.Zero.yawDegrees).isEqualTo(0.0f)
        assertThat(FollowThresholds.Zero.rollDegrees).isEqualTo(0.0f)
    }

    @Test
    fun followThresholds_equals_sameInstance_returnsTrue() {
        val thresholds = FollowThresholds()

        assertThat(thresholds).isEqualTo(thresholds)
    }

    @Test
    fun followThresholds_equals_sameProperties_returnsTrue() {
        val thresholds1 =
            FollowThresholds(
                translationMeters = 0.1f,
                pitchDegrees = 1.0f,
                yawDegrees = 2.0f,
                rollDegrees = 3.0f,
            )
        val thresholds2 =
            FollowThresholds(
                translationMeters = 0.1f,
                pitchDegrees = 1.0f,
                yawDegrees = 2.0f,
                rollDegrees = 3.0f,
            )

        assertThat(thresholds1).isEqualTo(thresholds2)
    }

    @Test
    fun followThresholds_equals_differentTranslationMeters_returnsFalse() {
        val base = FollowThresholds(translationMeters = 0.1f)
        val different = FollowThresholds(translationMeters = 0.5f)

        assertThat(base).isNotEqualTo(different)
    }

    @Test
    fun followThresholds_equals_differentPitchDegrees_returnsFalse() {
        val base = FollowThresholds(pitchDegrees = 1.0f)
        val different = FollowThresholds(pitchDegrees = 5.0f)

        assertThat(base).isNotEqualTo(different)
    }

    @Test
    fun followThresholds_equals_differentYawDegrees_returnsFalse() {
        val base = FollowThresholds(yawDegrees = 2.0f)
        val different = FollowThresholds(yawDegrees = 5.0f)

        assertThat(base).isNotEqualTo(different)
    }

    @Test
    fun followThresholds_equals_differentRollDegrees_returnsFalse() {
        val base = FollowThresholds(rollDegrees = 3.0f)
        val different = FollowThresholds(rollDegrees = 5.0f)

        assertThat(base).isNotEqualTo(different)
    }

    @Test
    fun followThresholds_equals_nullOrDifferentType_returnsFalse() {
        val thresholds = FollowThresholds()

        assertFalse(thresholds.equals(null))
        assertFalse(thresholds.equals("Dummy String"))
    }

    @Test
    fun followThresholds_hashCode_sameProperties_matches() {
        val thresholds1 = FollowThresholds()
        val thresholds2 = FollowThresholds()

        assertThat(thresholds1.hashCode()).isEqualTo(thresholds2.hashCode())
    }

    @Test
    fun followThresholds_hashCode_differentTranslationMeters_differs() {
        val thresholds1 = FollowThresholds(translationMeters = 0.1f)
        val thresholds2 = FollowThresholds(translationMeters = 0.5f)

        assertThat(thresholds1.hashCode()).isNotEqualTo(thresholds2.hashCode())
    }

    @Test
    fun followThresholds_hashCode_differentPitchDegrees_differs() {
        val thresholds1 = FollowThresholds(pitchDegrees = 1.0f)
        val thresholds2 = FollowThresholds(pitchDegrees = 5.0f)

        assertThat(thresholds1.hashCode()).isNotEqualTo(thresholds2.hashCode())
    }

    @Test
    fun followThresholds_hashCode_differentYawDegrees_differs() {
        val thresholds1 = FollowThresholds(yawDegrees = 2.0f)
        val thresholds2 = FollowThresholds(yawDegrees = 5.0f)

        assertThat(thresholds1.hashCode()).isNotEqualTo(thresholds2.hashCode())
    }

    @Test
    fun followThresholds_hashCode_differentRollDegrees_differs() {
        val thresholds1 = FollowThresholds(rollDegrees = 3.0f)
        val thresholds2 = FollowThresholds(rollDegrees = 5.0f)

        assertThat(thresholds1.hashCode()).isNotEqualTo(thresholds2.hashCode())
    }

    @Test
    fun followThresholds_toString_returnsExpectedString() {
        val thresholds =
            FollowThresholds(
                translationMeters = 0.1f,
                pitchDegrees = 1.0f,
                yawDegrees = 2.0f,
                rollDegrees = 3.0f,
            )

        assertThat(thresholds.toString())
            .isEqualTo(
                "FollowThresholds(translationMeters=0.1, pitchDegrees=1.0, yawDegrees=2.0, rollDegrees=3.0)"
            )
    }
}
