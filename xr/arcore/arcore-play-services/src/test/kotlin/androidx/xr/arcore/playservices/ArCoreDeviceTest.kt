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

package androidx.xr.arcore.playservices

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Session as ArCore1xSession
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ArCoreDeviceTest {
    private lateinit var underTest: ArCoreDevice
    private lateinit var mockSession: ArCore1xSession
    private lateinit var mockCamera: Camera
    private lateinit var mockFrame: Frame

    @Before
    fun setUp() {
        underTest = ArCoreDevice()
        mockSession = mock<ArCore1xSession>()
        mockCamera = mock<Camera>()
        mockFrame = mock<Frame>()
    }

    @Test
    fun update_updatesDevicePose() {
        val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion(1f, 1f, 1f, 1f))
        whenever(mockSession.update()).thenReturn(mockFrame)
        whenever(mockFrame.camera).thenReturn(mockCamera)
        whenever(mockCamera.pose).thenReturn(expectedPose.toARCorePose())
        check(underTest.devicePose != expectedPose)

        val frame = mockSession.update()
        underTest.update(frame)

        assertThat(underTest.devicePose).isEqualTo(expectedPose)
    }
}
