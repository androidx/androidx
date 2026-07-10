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

package androidx.camera.camera2.pipe.compat

import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraInterop
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class Camera2SystemStateTest {
    private val testScope = TestScope()
    private val fakeThreads = FakeThreads.fromTestScope(testScope)
    private val cameraSystemCallbacks: CameraInterop.CameraSystemCallbacks = mock()
    private val cameraInteropConfig =
        CameraPipe.CameraInteropConfig(cameraSystemCallbacks = cameraSystemCallbacks)
    private val camera2SystemState = Camera2SystemState(cameraInteropConfig, fakeThreads)

    private val cameraId0 = CameraId.fromCamera2Id("0")
    private val cameraId1 = CameraId.fromCamera2Id("1")
    private val graphId0 = CameraGraphId.nextId()
    private val graphId1 = CameraGraphId.nextId()

    @Test
    fun testCameraOpeningTriggersStarting() =
        testScope.runTest {
            camera2SystemState.onCameraOpening(cameraId0)
            verify(cameraSystemCallbacks, times(1)).onCameraSystemStarting()
        }

    @Test
    fun testMultipleCameraOpeningsTriggerStartingOnlyOnce() =
        testScope.runTest {
            camera2SystemState.onCameraOpening(cameraId0)
            camera2SystemState.onCameraOpening(cameraId1)
            verify(cameraSystemCallbacks, times(1)).onCameraSystemStarting()
        }

    @Test
    fun testCameraClosedTriggersStopped() =
        testScope.runTest {
            camera2SystemState.onCameraOpening(cameraId0)
            camera2SystemState.onCameraClosed(cameraId0)
            verify(cameraSystemCallbacks, times(1)).onCameraSystemStopped()
        }

    @Test
    fun testStoppedIsDeferredIfGraphIsActive() =
        testScope.runTest {
            camera2SystemState.onCameraOpening(cameraId0)
            camera2SystemState.onGraphStarting(graphId0)

            camera2SystemState.onCameraClosed(cameraId0)
            // System should still be active because graph0 is active.
            verify(cameraSystemCallbacks, never()).onCameraSystemStopped()

            camera2SystemState.onGraphStopped(graphId0)
            advanceUntilIdle()
            verify(cameraSystemCallbacks, times(1)).onCameraSystemStopped()
        }

    @Test
    fun testStartingIsCalledBeforeStopped() =
        testScope.runTest {
            camera2SystemState.onCameraOpening(cameraId0)
            camera2SystemState.onCameraClosed(cameraId0)

            verify(cameraSystemCallbacks, times(1)).onCameraSystemStarting()
            verify(cameraSystemCallbacks, times(1)).onCameraSystemStopped()
        }

    @Test
    fun testConcurrentCamerasAndGraphs() =
        testScope.runTest {
            camera2SystemState.onCameraOpening(cameraId0)
            camera2SystemState.onGraphStarting(graphId0)
            camera2SystemState.onCameraOpening(cameraId1)
            camera2SystemState.onGraphStarting(graphId1)

            verify(cameraSystemCallbacks, times(1)).onCameraSystemStarting()

            camera2SystemState.onCameraClosed(cameraId0)
            camera2SystemState.onGraphStopped(graphId0)
            advanceUntilIdle()
            verify(cameraSystemCallbacks, never()).onCameraSystemStopped()

            camera2SystemState.onCameraClosed(cameraId1)
            camera2SystemState.onGraphStopped(graphId1)
            advanceUntilIdle()
            verify(cameraSystemCallbacks, times(1)).onCameraSystemStopped()
        }
}
