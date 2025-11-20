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

package androidx.camera.camera2.pipe.graph

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.util.Size
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.CameraGraphSimulator
import androidx.camera.camera2.pipe.testing.CameraPipeSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class CameraGraphCameraControls3ATest {
    private val testScope = TestScope()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val metadata =
        FakeCameraMetadata(
            mapOf(
                INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE to 1.0f,
            )
        )
    private val streamConfig1 = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)
    private val streamConfig2 = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)
    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(streamConfig1, streamConfig2))
    private val cameraPipeSimulator =
        CameraPipeSimulator.create(testScope, context, listOf(metadata))
    private val cameraGraph: CameraGraphSimulator =
        cameraPipeSimulator.createCameraGraph(graphConfig)

    @Before
    fun setup() {
        cameraGraph.start()
        cameraGraph.simulateCameraStarted()
        cameraGraph.initializeSurfaces()
        testScope.advanceUntilIdle()
        val stream1 = cameraGraph.streams[streamConfig1]!!.id
        cameraGraph.useSessionIn(testScope) {
            it.startRepeating(Request(streams = listOf(stream1)))
        }
        testScope.advanceUntilIdle()
        cameraGraph.simulateNextFrame()
        testScope.advanceUntilIdle()
    }

    @Test
    fun update3A_completesWithStatusOK() =
        testScope.runTest {
            val result3ADeferred = cameraGraph.update3A(aeMode = AeMode.OFF)
            advanceUntilIdle()

            val frame = cameraGraph.simulateNextFrame()
            frame.simulateTotalCaptureResult(
                mapOf(CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_OFF)
            )
            advanceUntilIdle()

            val result3A = result3ADeferred.await()
            assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
            assertThat(result3A.frameMetadata).isNotNull()
        }

    @Test
    fun lock3A_completesWithStatusOK() =
        testScope.runTest {
            val result3ADeferred =
                cameraGraph.lock3A(afLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN)
            advanceUntilIdle()

            cameraGraph
                .simulateNextFrame()
                .simulateTotalCaptureResult(
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED
                    )
                )
            advanceUntilIdle()

            cameraGraph
                .simulateNextFrame()
                .simulateTotalCaptureResult(
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    )
                )
            advanceUntilIdle()

            val result3A = result3ADeferred.await()
            assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
            assertThat(result3A.frameMetadata).isNotNull()
        }

    @Test
    fun setTorchOn_completesWithStatusOK() =
        testScope.runTest {
            val result3ADeferred = cameraGraph.setTorchOn()
            advanceUntilIdle()

            val frame = cameraGraph.simulateNextFrame()
            frame.simulateTotalCaptureResult(
                mapOf(
                    CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_TORCH,
                    CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_ON,
                )
            )
            advanceUntilIdle()

            val result3A = result3ADeferred.await()
            assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
            assertThat(result3A.frameMetadata).isNotNull()
        }

    @Test
    fun unlock3A_completesWithStatusOK() =
        testScope.runTest {
            val unlockResultDeferred = cameraGraph.unlock3A(ae = true)
            advanceUntilIdle()

            cameraGraph
                .simulateNextFrame()
                .simulateTotalCaptureResult(
                    mapOf(
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_SEARCHING
                    )
                )
            advanceUntilIdle()

            assertThat(unlockResultDeferred.await().status).isEqualTo(Result3A.Status.OK)
            assertThat(unlockResultDeferred.await().frameMetadata).isNotNull()
        }

    @Test
    fun lockThenUnlock_happensInOrder() =
        testScope.runTest {
            cameraGraph.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)
            cameraGraph.unlock3A(ae = true)

            advanceUntilIdle()

            val lockParams = cameraGraph.simulateNextFrame().requestSequence.requiredParameters
            val unlockParams = cameraGraph.simulateNextFrame().requestSequence.requiredParameters
            assertThat(lockParams).containsExactly(CaptureRequest.CONTROL_AE_LOCK, true)
            assertThat(unlockParams).containsExactly(CaptureRequest.CONTROL_AE_LOCK, false)
        }
}
