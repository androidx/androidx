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
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.hardware.camera2.CaptureRequest
import android.util.Size
import androidx.camera.camera2.pipe.CameraBackendFactory
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
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
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
class FrameGraphImplTest {
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
    // TODO: b/412695326 - Refactor FrameGraphImplTest to use CameraGraphSimulator

    private val frameGraphBuffers = FrameGraphBuffers(cameraGraph, testScope)
    private val frameGraph: FrameGraph =
        FrameGraphImpl(cameraGraph, frameDistributor, frameGraphBuffers, testScope.backgroundScope)
    private val streamId1: StreamId = StreamId(1)
    private val streamId2: StreamId = StreamId(2)

    @Test
    fun startFrameGraph_CameraGraphStarts() =
        testScope.runTest {
            assertThat(cameraGraph.graphState.value).isEqualTo(GraphStateStopped)
            frameGraph.start()
            assertThat(cameraGraph.graphState.value).isEqualTo(GraphStateStarting)
        }

    @Test
    fun stopFrameGraph_CameraGraphStops() =
        testScope.runTest {
            frameGraph.start()
            assertThat(cameraGraph.graphState.value).isEqualTo(GraphStateStarting)

            frameGraph.stop()

            assertThat(cameraGraph.graphState.value).isEqualTo(GraphStateStopping)
        }

    @Test
    fun captureWithSingleStream_graphProcessorRepeatingRequestUpdates() =
        testScope.runTest {
            frameGraph.start()

            frameGraph.captureWith(setOf(streamId1))
            advanceUntilIdle()

            assertEquals(listOf(streamId1), fakeGraphProcessor.repeatingRequest?.streams)
        }

    @Test
    fun captureWithMultipleStreams_graphProcessorRepeatingRequestUpdates() =
        testScope.runTest {
            frameGraph.start()

            frameGraph.captureWith(setOf(streamId1, streamId2))
            advanceUntilIdle()

            assertEquals(listOf(streamId1, streamId2), fakeGraphProcessor.repeatingRequest?.streams)
        }

    @Test
    fun captureWithMultipleStreamsAndParameters_graphProcessorRepeatingRequestUpdates() =
        testScope.runTest {
            frameGraph.start()

            frameGraph.captureWith(
                setOf(streamId1, streamId2),
                mapOf(CAPTURE_REQUEST_KEY to 2, TEST_NULLABLE_KEY to null, TEST_KEY to 5),
            )
            advanceUntilIdle()

            val parameters: Map<CaptureRequest.Key<*>, Any> = mapOf(CAPTURE_REQUEST_KEY to 2)
            val extras: Map<Metadata.Key<*>, Any> = mapOf(TEST_KEY to 5)
            assertEquals(listOf(streamId1, streamId2), fakeGraphProcessor.repeatingRequest?.streams)
            assertEquals(parameters, fakeGraphProcessor.repeatingRequest?.parameters)
            assertEquals(extras, fakeGraphProcessor.repeatingRequest?.extras)
        }

    @Test
    fun captureWithConflictingParameters_throwException() =
        testScope.runTest {
            frameGraph.start()
            frameGraph.captureWith(setOf(streamId1, streamId2), mapOf(CAPTURE_REQUEST_KEY to 2))

            assertThrows<IllegalStateException> {
                frameGraph.captureWith(setOf(streamId1, streamId2), mapOf(CAPTURE_REQUEST_KEY to 3))
            }
        }

    @Test
    fun detachAllStream_stopRepeating() =
        testScope.runTest {
            frameGraph.start()
            val buffer =
                frameGraph.captureWith(setOf(streamId1, streamId2), mapOf(CAPTURE_REQUEST_KEY to 2))
            advanceUntilIdle()

            buffer.close()
            advanceUntilIdle()

            assertNull(fakeGraphProcessor.repeatingRequest)
        }

    @Test
    fun testAcquireSession() =
        testScope.runTest {
            val session = frameGraph.acquireSession()
            assertThat(session).isNotNull()
        }

    @Test
    fun testAcquireSessionOrNull() =
        testScope.runTest {
            val session = frameGraph.acquireSessionOrNull()
            assertThat(session).isNotNull()
        }

    @Test
    fun testAcquireSessionOrNullAfterAcquireSession() =
        testScope.runTest {
            val session = frameGraph.acquireSession()
            assertThat(session).isNotNull()

            // Since a session is already active, an attempt to acquire another session will fail.
            val session1 = frameGraph.acquireSessionOrNull()
            assertThat(session1).isNull()

            // Closing an active session should allow a new session instance to be created.
            session.close()
            advanceUntilIdle()

            val session2 = frameGraph.acquireSessionOrNull()
            assertThat(session2).isNotNull()
        }

    @Test
    fun testUseSessionClosesAndDoesNotBlock() =
        testScope.runTest {
            val events = mutableListOf<Int>()
            frameGraph.useSession { events += 1 }
            frameGraph.useSession { events += 2 }

            assertThat(events).containsExactly(1, 2).inOrder()
        }

    @Test
    fun testUseSessionInClosesAndDoesNotBlock() =
        testScope.runTest {
            val events = mutableListOf<Int>()
            val scope = CoroutineScope(Job())
            val job1 = frameGraph.useSessionIn(this) { events += 1 }
            val job2 = frameGraph.useSessionIn(scope) { events += 2 }
            job1.await()
            job2.await()

            assertThat(events.size).isEqualTo(2)
        }

    @Test
    fun useSession_invalidatesSessionAfterClosure_revertsCaptureStreams() =
        testScope.runTest {
            val initialStreams = listOf(streamId1)
            val repeatingRequestStreams = listOf(streamId2)

            frameGraph.start()
            frameGraph.captureWith(initialStreams.toSet())
            advanceUntilIdle()
            frameGraph.useSession {
                it.startRepeating(Request(streams = repeatingRequestStreams))
                assertThat(fakeGraphProcessor.repeatingRequest?.streams)
                    .isEqualTo(repeatingRequestStreams)
            }
            advanceUntilIdle()

            assertThat(fakeGraphProcessor.repeatingRequest?.streams).isEqualTo(initialStreams)
        }

    @Test
    fun useSession_invalidatesSessionAfterClosure_revertsParameters() =
        testScope.runTest {
            val repeatingRequestParameters =
                mapOf<CaptureRequest.Key<*>, Any>(CaptureRequest.SCALER_CROP_REGION to Rect())

            frameGraph.start()
            frameGraph.captureWith(streamIds = setOf(streamId1))
            advanceUntilIdle()
            frameGraph.useSession {
                it.startRepeating(
                    Request(streams = listOf(streamId1), parameters = repeatingRequestParameters)
                )
                assertThat(fakeGraphProcessor.repeatingRequest?.parameters)
                    .isEqualTo(repeatingRequestParameters)
            }
            advanceUntilIdle()

            assertThat(fakeGraphProcessor.repeatingRequest?.parameters)
                .isEqualTo(emptyMap<CaptureRequest.Key<*>, Any>())
        }

    @Test
    fun useSession_invalidatesSessionAfterClosure_revertsBothStreamsAndParameters() =
        testScope.runTest {
            val initialStreams = listOf(streamId1)
            val initialParameters = emptyMap<CaptureRequest.Key<*>, Any>()
            val repeatingRequestStreams = listOf(streamId2)
            val repeatingRequestParameters =
                mapOf<CaptureRequest.Key<*>, Any>(CaptureRequest.SCALER_CROP_REGION to Rect())

            frameGraph.start()
            frameGraph.captureWith(initialStreams.toSet(), initialParameters.toMap())
            advanceUntilIdle()
            frameGraph.useSession {
                it.startRepeating(
                    Request(
                        streams = repeatingRequestStreams,
                        parameters = repeatingRequestParameters,
                    )
                )
                assertThat(fakeGraphProcessor.repeatingRequest?.parameters)
                    .isEqualTo(repeatingRequestParameters)
                assertThat(fakeGraphProcessor.repeatingRequest?.streams)
                    .isEqualTo(repeatingRequestStreams)
            }
            advanceUntilIdle()

            assertThat(fakeGraphProcessor.repeatingRequest?.parameters).isEqualTo(initialParameters)
            assertThat(fakeGraphProcessor.repeatingRequest?.streams).isEqualTo(initialStreams)
        }

    companion object {
        private val CAPTURE_REQUEST_KEY = CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
        private val TEST_NULLABLE_KEY = CaptureRequest.BLACK_LEVEL_LOCK
    }
}
