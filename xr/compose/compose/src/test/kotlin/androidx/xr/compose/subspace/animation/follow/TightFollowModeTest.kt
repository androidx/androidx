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
class TightFollowModeTest {

    @Test
    fun tightFollowMode_equals_sameInstance_returnsTrue() {
        val mode = TightFollowMode(TrackedDimensions.All)

        assertThat(mode).isEqualTo(mode)
    }

    @Test
    fun tightFollowMode_equals_sameDimensions_returnsTrue() {
        val mode1 = TightFollowMode(TrackedDimensions.All)
        val mode2 = TightFollowMode(TrackedDimensions.All)

        assertThat(mode1).isEqualTo(mode2)
    }

    @Test
    fun tightFollowMode_equals_sameDimensionsDifferentMode_returns() {
        val mode1 = TightFollowMode(TrackedDimensions.All)
        val mode2 = SnapFollowMode(TrackedDimensions.All)

        assertThat(mode1).isNotEqualTo(mode2)
    }

    @Test
    fun tightFollowMode_equals_differentDimensions_returnsFalse() {
        val modeAll = TightFollowMode(TrackedDimensions.All)
        val modeRotationOnly = TightFollowMode(TrackedDimensions.RotationOnly)

        assertThat(modeAll).isNotEqualTo(modeRotationOnly)
    }

    @Test
    fun tightFollowMode_equals_nullOrDifferentType_returnsFalse() {
        val mode = TightFollowMode(TrackedDimensions.All)

        assertFalse(mode.equals(null))
        assertFalse(mode.equals("Dummy String"))
    }

    @Test
    fun tightFollowMode_hashCode_sameDimensions_matches() {
        val mode1 = TightFollowMode(TrackedDimensions.All)
        val mode2 = TightFollowMode(TrackedDimensions.All)

        assertThat(mode1.hashCode()).isEqualTo(mode2.hashCode())
    }

    @Test
    fun tightFollowMode_hashCode_differentDimensions_differs() {
        val modeAll = TightFollowMode(TrackedDimensions.All)
        val modeRotationOnly = TightFollowMode(TrackedDimensions.RotationOnly)

        assertThat(modeAll.hashCode()).isNotEqualTo(modeRotationOnly.hashCode())
    }
}
