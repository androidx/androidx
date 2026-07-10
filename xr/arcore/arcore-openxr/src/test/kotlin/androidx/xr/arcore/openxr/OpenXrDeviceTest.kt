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

import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK], shadows = [OpenXrDeviceTest.ShadowOpenXrDevice::class])
public class OpenXrDeviceTest {

    @Implements(OpenXrDevice::class)
    public class ShadowOpenXrDevice {
        public companion object {
            internal var mockedDeviceState: DeviceState = DeviceState(TrackingState.PAUSED, null)

            @Implementation
            @JvmStatic
            @JvmName("nativeGetDeviceState")
            internal fun nativeGetDeviceState(monotonicTimeNs: Long): DeviceState {
                return mockedDeviceState
            }
        }
    }

    private lateinit var underTest: OpenXrDevice

    @Before
    fun setUp() {
        underTest = OpenXrDevice()
    }

    @Test
    fun update_withTracking_setsStateToTracking() {
        val expectedPose = Pose(androidx.xr.runtime.math.Vector3(1f, 2f, 3f))
        ShadowOpenXrDevice.mockedDeviceState = DeviceState(TrackingState.TRACKING, expectedPose)

        underTest.update(0L)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
        assertThat(underTest.devicePose).isEqualTo(expectedPose)
    }

    @Test
    fun update_withPaused_setsStateToPaused() {
        val expectedPose = Pose(androidx.xr.runtime.math.Vector3(4f, 5f, 6f))
        ShadowOpenXrDevice.mockedDeviceState = DeviceState(TrackingState.PAUSED, expectedPose)

        underTest.update(0L)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.PAUSED)
        assertThat(underTest.devicePose).isEqualTo(expectedPose)
    }

    @Test
    fun update_withTrackingDegraded_setsStateToDegraded() {
        val expectedPose = Pose(androidx.xr.runtime.math.Vector3(7f, 8f, 9f))
        ShadowOpenXrDevice.mockedDeviceState =
            DeviceState(TrackingState.TRACKING_DEGRADED, expectedPose)

        underTest.update(0L)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING_DEGRADED)
        assertThat(underTest.devicePose).isEqualTo(expectedPose)
    }

    @Test
    fun update_withPausedAndNullPose_keepsPreviousPose() {
        val initialPose = underTest.devicePose
        ShadowOpenXrDevice.mockedDeviceState = DeviceState(TrackingState.PAUSED, null)

        underTest.update(0L)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.PAUSED)
        assertThat(underTest.devicePose).isEqualTo(initialPose)
    }
}
