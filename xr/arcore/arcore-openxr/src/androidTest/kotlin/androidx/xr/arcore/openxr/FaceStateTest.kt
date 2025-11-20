/*
 * Copyright 2025 The Android Open Source Project
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
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FaceStateTest {
    @Test
    fun constructor_noArguments_returnsInvalidWithZeroedArrays() {
        val underTest = FaceState()

        assertThat(underTest.isValid).isFalse()
        assertThat(underTest.parameters.size).isEqualTo(OpenXrFace.XR_FACE_PARAMETER_COUNT_ANDROID)
        assertThat(underTest.regionConfidences.size)
            .isEqualTo(OpenXrFace.XR_FACE_REGION_CONFIDENCE_COUNT_ANDROID)
        assertTrue(underTest.parameters.all { it == 0.0f })
        assertTrue(underTest.regionConfidences.all { it == 0.0f })
    }

    @Test
    fun fromOpenXrFaceTrackingState_withPausedValue_convertsTrackingState() {
        val trackingState: TrackingState = TrackingState.fromOpenXrFaceTrackingState(0)

        assertThat(trackingState).isEqualTo(TrackingState.PAUSED)
    }

    @Test
    fun fromOpenXrFaceTrackingState_withTrackingValue_convertsTrackingState() {
        val trackingState: TrackingState = TrackingState.fromOpenXrFaceTrackingState(2)

        assertThat(trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun fromOpenXrFaceTrackingState_withStoppedValue_convertsTrackingState() {
        val trackingState: TrackingState = TrackingState.fromOpenXrFaceTrackingState(1)

        assertThat(trackingState).isEqualTo(TrackingState.STOPPED)
    }

    @Test
    fun fromOpenXrFaceTrackingState_withInvalidValue_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { TrackingState.fromOpenXrFaceTrackingState(3) }
    }
}
