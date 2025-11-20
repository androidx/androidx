/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.xr.runtime.TrackingState
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AnchorStateTest {

    @Test
    fun constructor_noArguments_returnsZeroVectorAndIdentityQuaternion() {
        val pose = AnchorState().pose!!

        assertThat(pose.translation.x).isEqualTo(0)
        assertThat(pose.translation.y).isEqualTo(0)
        assertThat(pose.translation.z).isEqualTo(0)
        assertThat(pose.rotation.x).isEqualTo(0)
        assertThat(pose.rotation.y).isEqualTo(0)
        assertThat(pose.rotation.z).isEqualTo(0)
        assertThat(pose.rotation.w).isEqualTo(1)
    }

    @Test
    fun constructor_noArguments_returnsPausedTrackingState() {
        val underTest = AnchorState()

        assertThat(underTest.trackingState).isEqualTo(TrackingState.PAUSED)
    }

    @Test
    fun constructor_TrackingAndNullPose_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            AnchorState(trackingState = TrackingState.TRACKING, pose = null)
        }
    }

    @Test
    fun constructor_PausedAndNullPose_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            AnchorState(trackingState = TrackingState.PAUSED, pose = null)
        }
    }

    @Test
    fun fromOpenXrLocationFlags_IncorrectBitPositions_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TrackingState.fromOpenXrLocationFlags(0x0000FFFF)
        }
    }

    @Test
    fun fromOpenXrLocationFlags_OnlyValidBitsFlipped_returnsPausedTrackingState() {
        val trackingState = TrackingState.fromOpenXrLocationFlags(0x00000003) // 0b...0011

        assertThat(trackingState).isEqualTo(TrackingState.PAUSED)
    }

    @Test
    fun fromOpenXrLocationFlags_ValidAndTrackingBitsFlipped_returnsTrackingTrackingState() {
        val trackingState = TrackingState.fromOpenXrLocationFlags(0x0000000F) // 0b...1111

        assertThat(trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun fromOpenXrLocationFlags_OnlyTrackingBitsFlipped_returnsStoppedTrackingState() {
        val trackingState = TrackingState.fromOpenXrLocationFlags(0x0000000C) // 0b...1100

        assertThat(trackingState).isEqualTo(TrackingState.STOPPED)
    }

    @Test
    fun fromOpenXrLocationFlags_OneTrackingAndValidBitFlipped_returnsStoppedTrackingState() {
        val trackingState = TrackingState.fromOpenXrLocationFlags(0x0000000A) // 0b...1010

        assertThat(trackingState).isEqualTo(TrackingState.STOPPED)
    }
}
