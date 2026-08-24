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

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SoftFollowModeTest {

    @Test
    fun softFollowMode_equals_sameInstance_returnsTrue() {
        val mode = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.All)

        assertThat(mode).isEqualTo(mode)
    }

    @Test
    fun softFollowMode_equals_sameProperties_returnsTrue() {
        val mode1 = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.All)
        val mode2 = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.All)

        assertThat(mode1).isEqualTo(mode2)
    }

    @Test
    fun softFollowMode_equals_differentDurationMs_returnsFalse() {
        val mode1 = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.All)
        val mode2 = SoftFollowMode(durationMs = 2000, dimensions = TrackedDimensions.All)

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun softFollowMode_equals_differentDimensions_returnsFalse() {
        val modeAll = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.All)
        val modeRotationOnly =
            SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.RotationOnly)

        assertThat(modeAll).isNotEqualTo(modeRotationOnly)
    }

    @Test
    fun softFollowMode_equals_nullOrDifferentType_returnsFalse() {
        val mode = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.All)

        assertFalse(mode.equals(null))
        assertFalse(mode.equals("Dummy String"))
    }

    @Test
    fun softFollowMode_hashCode_sameProperties_matches() {
        val mode1 = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.All)
        val mode2 = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.All)

        assertThat(mode1.hashCode()).isEqualTo(mode2.hashCode())
    }

    @Test
    fun softFollowMode_hashCode_differentProperties_differs() {
        val mode1 = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.All)
        val mode2 = SoftFollowMode(durationMs = 2000, dimensions = TrackedDimensions.All)
        val mode3 = SoftFollowMode(durationMs = 1500, dimensions = TrackedDimensions.RotationOnly)

        assertThat(mode1.hashCode()).isNotEqualTo(mode2.hashCode())
        assertThat(mode1.hashCode()).isNotEqualTo(mode3.hashCode())
    }
}
