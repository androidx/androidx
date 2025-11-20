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

package androidx.camera.camera2.pipe.framegraph

import android.content.Context
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.hardware.camera2.CaptureRequest
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.CameraGraphSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeMetadata.Companion.TEST_KEY
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FrameGraphBuffersTest {
    private val testScope = TestScope()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val metadata =
        FakeCameraMetadata(
            mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        )
    private val stream1Config =
        CameraStream.Config.create(Size(1280, 720), StreamFormat.YUV_420_888)
    private val stream2Config =
        CameraStream.Config.create(Size(1920, 1080), StreamFormat.YUV_420_888)
    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(stream1Config, stream2Config))
    private val simulator = CameraGraphSimulator.create(testScope, context, metadata, graphConfig)
    private val frameGraphBuffers = FrameGraphBuffers(simulator, testScope)
    private val streamIdList = simulator.streams.streamIds.toList()
    private val streamId1 = streamIdList[0]
    private val streamId2 = streamIdList[1]

    @Before
    fun setup() {
        simulator.start()
        simulator.simulateCameraStarted()
        simulator.initializeSurfaces()
        testScope.advanceUntilIdle()
    }

    @Test
    fun attachActualChange_repeatingRequestUpdated() =
        testScope.runTest {
            frameGraphBuffers.attach(
                setOf(streamId1, streamId2),
                mapOf(CAPTURE_REQUEST_KEY to 2, TEST_KEY to 5),
                1,
            )
            advanceUntilIdle()

            val frame = simulator.simulateNextFrame()
            val parameters: Map<CaptureRequest.Key<*>, Any> = mapOf(CAPTURE_REQUEST_KEY to 2)
            val extras: Map<Metadata.Key<*>, Any> = mapOf(TEST_KEY to 5)
            assertEquals(listOf(streamId1, streamId2), frame.request.streams)
            assertEquals(parameters, frame.request.parameters)
            assertEquals(extras, frame.request.extras)
        }

    @Test
    fun detachActualChange_repeatingRequestUpdated() =
        testScope.runTest {
            val frameBuffer1 =
                frameGraphBuffers.attach(
                    setOf(streamId1),
                    mapOf(CAPTURE_REQUEST_KEY to 2, TEST_KEY to 5),
                    1,
                )
            val frameBuffer2 =
                frameGraphBuffers.attach(setOf(streamId2), mapOf(TEST_NULLABLE_KEY to 42), 1)
            var parameters: Map<CaptureRequest.Key<*>, Any> =
                mapOf(CAPTURE_REQUEST_KEY to 2, TEST_NULLABLE_KEY to 42)
            val extras: Map<Metadata.Key<*>, Any> = mapOf(TEST_KEY to 5)
            advanceUntilIdle()

            assertEquals(
                listOf(streamId1, streamId2),
                simulator.simulateNextFrame().request.streams,
            )
            assertEquals(parameters, simulator.simulateNextFrame().request.parameters)
            assertEquals(extras, simulator.simulateNextFrame().request.extras)

            frameBuffer1.close()
            advanceUntilIdle()

            parameters = mapOf(TEST_NULLABLE_KEY to 42)
            assertEquals(listOf(streamId2), simulator.simulateNextFrame().request.streams)
            assertEquals(parameters, simulator.simulateNextFrame().request.parameters)
            assertEquals(emptyMap(), simulator.simulateNextFrame().request.extras)

            frameBuffer2.close()
        }

    companion object {
        private val CAPTURE_REQUEST_KEY = CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
        private val TEST_NULLABLE_KEY = CaptureRequest.BLACK_LEVEL_LOCK
    }
}
