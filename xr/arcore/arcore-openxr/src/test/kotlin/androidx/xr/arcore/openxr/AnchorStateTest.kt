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
import androidx.xr.arcore.runtime.TrackingState
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

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
}
