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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@Suppress("deprecation")
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraDeviceWrapperTest {
    private val cameraId = CameraId("0")
    private val cameraMetadata = FakeCameraMetadata(cameraId = cameraId)
    private val cameraDevice: CameraDevice = mock()
    private val cameraErrorListener: CameraErrorListener = mock()
    private val testScope = TestScope()
    private val fakeThreads = FakeThreads.fromTestScope(testScope)

    private val androidCameraDevice =
        AndroidCameraDevice(
            cameraMetadata,
            cameraDevice,
            cameraId,
            cameraErrorListener,
            threads = fakeThreads,
        )

    private val sessionStateCallback1: CameraCaptureSessionWrapper.StateCallback = mock()
    private val sessionStateCallback2: CameraCaptureSessionWrapper.StateCallback = mock()

    @Test
    fun testCreateCaptureSession() =
        testScope.runTest {
            androidCameraDevice.createCaptureSession(emptyList(), sessionStateCallback1)
            advanceUntilIdle()

            verify(cameraDevice, times(1)).createCaptureSession(any(), any(), any())
        }

    @Test
    fun testCaptureSessionGetsFinalizedWhenDeviceClosed() =
        testScope.runTest {
            androidCameraDevice.onDeviceClosing()
            androidCameraDevice.onDeviceClosed()
            advanceUntilIdle()

            assertFalse(
                androidCameraDevice.createCaptureSession(emptyList(), sessionStateCallback1)
            )
            verify(sessionStateCallback1, times(1)).onSessionFinalized()
        }

    @Test
    fun testCreateSecondCaptureSessionDisconnectsAndFinalizesTheFirstOne() =
        testScope.runTest {
            androidCameraDevice.createCaptureSession(emptyList(), sessionStateCallback1)
            advanceUntilIdle()

            verify(cameraDevice, times(1)).createCaptureSession(any(), any(), any())

            whenever(cameraDevice.createCaptureSession(any(), any(), any())).thenAnswer {
                val callback = it.arguments[1] as CameraCaptureSession.StateCallback
                val session: CameraCaptureSession = mock()
                callback.onConfigured(session)
            }
            androidCameraDevice.createCaptureSession(emptyList(), sessionStateCallback2)
            verify(sessionStateCallback1, times(1)).onSessionDisconnected()
            advanceUntilIdle()
            verify(sessionStateCallback1, times(1)).onSessionFinalized()
        }
}
