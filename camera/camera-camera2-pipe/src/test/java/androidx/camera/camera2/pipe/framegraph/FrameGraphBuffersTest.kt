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
import androidx.camera.camera2.pipe.CameraBackendFactory
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.graph.CameraGraphImpl
import androidx.camera.camera2.pipe.graph.GraphState3A
import androidx.camera.camera2.pipe.graph.Listener3A
import androidx.camera.camera2.pipe.graph.SessionLock
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.graph.SurfaceGraph
import androidx.camera.camera2.pipe.internal.CameraBackendsImpl
import androidx.camera.camera2.pipe.internal.CameraGraphParametersImpl
import androidx.camera.camera2.pipe.internal.CameraPipeLifetime
import androidx.camera.camera2.pipe.internal.FrameCaptureQueue
import androidx.camera.camera2.pipe.internal.FrameDistributor
import androidx.camera.camera2.pipe.internal.ImageSourceMap
import androidx.camera.camera2.pipe.media.ImageReaderImageSources
import androidx.camera.camera2.pipe.testing.CameraControllerSimulator
import androidx.camera.camera2.pipe.testing.FakeAudioRestrictionController
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeMetadata.Companion.TEST_KEY
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
class FrameGraphBuffersTest {
    private val testScope = TestScope()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val metadata =
        FakeCameraMetadata(
            mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        )
    private val fakeGraphProcessor = FakeGraphProcessor()
    private val cameraSurfaceManager = CameraSurfaceManager()

    private val stream1Config =
        CameraStream.Config.create(Size(1280, 720), StreamFormat.YUV_420_888)
    private val stream2Config =
        CameraStream.Config.create(Size(1920, 1080), StreamFormat.YUV_420_888)

    private val graphId = CameraGraphId.nextId()
    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(stream1Config, stream2Config))
    private val threads = FakeThreads.fromTestScope(testScope)
    private val cameraPipeLifetime = CameraPipeLifetime()
    private val backend = FakeCameraBackend(fakeCameras = mapOf(metadata.camera to metadata))
    private val backends =
        CameraBackendsImpl(
            defaultBackendId = backend.id,
            cameraBackends = mapOf(backend.id to CameraBackendFactory { backend }),
            context,
            threads,
            cameraPipeLifetime,
        )
    private val cameraContext = CameraBackendsImpl.CameraBackendContext(context, threads, backends)
    private val imageSources = ImageReaderImageSources(threads)
    private val frameCaptureQueue = FrameCaptureQueue()
    private val cameraController =
        CameraControllerSimulator(cameraContext, graphId, graphConfig, fakeGraphProcessor)
    private val cameraControllerProvider: () -> CameraControllerSimulator = { cameraController }
    private val streamGraph = StreamGraphImpl(metadata, graphConfig, cameraControllerProvider)
    private val imageSourceMap = ImageSourceMap(graphConfig, streamGraph, imageSources)
    private val frameDistributor = FrameDistributor(imageSourceMap.imageSources, frameCaptureQueue)
    private val surfaceGraph =
        SurfaceGraph(streamGraph, cameraControllerProvider, cameraSurfaceManager, emptyMap())
    private val audioRestriction = FakeAudioRestrictionController()
    private val sessionLock = SessionLock()
    private val cameraGraphParameters =
        CameraGraphParametersImpl(sessionLock, fakeGraphProcessor, testScope)
    private val cameraGraph =
        CameraGraphImpl(
            graphConfig,
            metadata,
            fakeGraphProcessor,
            fakeGraphProcessor,
            streamGraph,
            surfaceGraph,
            cameraController,
            GraphState3A(),
            Listener3A(),
            frameDistributor,
            frameCaptureQueue,
            audioRestriction,
            graphId,
            cameraGraphParameters,
            sessionLock,
        )
    private val frameGraphBuffers = FrameGraphBuffers(cameraGraph, testScope)
    private val streamId1: StreamId = StreamId(1)
    private val streamId2: StreamId = StreamId(2)

    @Test
    fun attachActualChange_repeatingRequestUpdated() =
        testScope.runTest {
            frameGraphBuffers.attach(
                setOf(streamId1, streamId2),
                mapOf(CAPTURE_REQUEST_KEY to 2, TEST_KEY to 5),
                1,
            )
            advanceUntilIdle()

            val parameters: Map<CaptureRequest.Key<*>, Any> = mapOf(CAPTURE_REQUEST_KEY to 2)
            val extras: Map<Metadata.Key<*>, Any> = mapOf(TEST_KEY to 5)
            assertEquals(listOf(streamId1, streamId2), fakeGraphProcessor.repeatingRequest?.streams)
            assertEquals(parameters, fakeGraphProcessor.repeatingRequest?.parameters)
            assertEquals(extras, fakeGraphProcessor.repeatingRequest?.extras)
        }

    @Test
    fun detachActualChange_repeatingRequestUpdated() =
        testScope.runTest {
            val frameBuffer =
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
            assertEquals(listOf(streamId1, streamId2), fakeGraphProcessor.repeatingRequest?.streams)
            assertEquals(parameters, fakeGraphProcessor.repeatingRequest?.parameters)
            assertEquals(extras, fakeGraphProcessor.repeatingRequest?.extras)

            frameBuffer.close()
            advanceUntilIdle()

            parameters = mapOf(TEST_NULLABLE_KEY to 42)
            assertEquals(listOf(streamId2), fakeGraphProcessor.repeatingRequest?.streams)
            assertEquals(parameters, fakeGraphProcessor.repeatingRequest?.parameters)
            assertEquals(emptyMap(), fakeGraphProcessor.repeatingRequest?.extras)

            frameBuffer2.close()
        }

    companion object {
        private val CAPTURE_REQUEST_KEY = CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
        private val TEST_NULLABLE_KEY = CaptureRequest.BLACK_LEVEL_LOCK
    }
}
