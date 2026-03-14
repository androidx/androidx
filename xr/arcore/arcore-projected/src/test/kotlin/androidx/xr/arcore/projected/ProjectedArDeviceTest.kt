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

package androidx.xr.arcore.projected

import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectedArDeviceTest {
    private lateinit var projectedArDevice: ProjectedArDevice

    @Before
    fun setUp() {
        projectedArDevice = ProjectedArDevice()
    }

    @Test
    fun init_default_returnsStoppedAndIdentity() {
        assertThat(projectedArDevice.trackingState).isEqualTo(TrackingState.STOPPED)
        assertThat(projectedArDevice.devicePose).isEqualTo(Pose())
    }

    @Test
    fun update_tracking_updatesStateAndPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 1f, 0f, 0f))

        projectedArDevice.update(TrackingState.TRACKING, pose)

        assertThat(projectedArDevice.trackingState).isEqualTo(TrackingState.TRACKING)
        assertThat(projectedArDevice.devicePose).isEqualTo(pose)
    }

    @Test
    fun update_trackingDegraded_updatesStateAndPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 1f, 0f, 0f))

        projectedArDevice.update(TrackingState.TRACKING_DEGRADED, pose)

        assertThat(projectedArDevice.trackingState).isEqualTo(TrackingState.TRACKING_DEGRADED)
        assertThat(projectedArDevice.devicePose).isEqualTo(pose)
    }
}
