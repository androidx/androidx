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
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.CameraPipeSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeMetadata.Companion.TEST_KEY
import androidx.camera.camera2.pipe.testing.FrameGraphSimulator
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertEquals
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
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FrameGraphImplTest {
    private val testScope = TestScope()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val metadata =
        FakeCameraMetadata(
            mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        )
    private val streamConfig1 = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)
    private val streamConfig2 = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)
    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(streamConfig1, streamConfig2))
    private val cameraPipeSimulator =
        CameraPipeSimulator.create(testScope, context, listOf(metadata))
    private val frameGraph: FrameGraphSimulator =
        cameraPipeSimulator.createFrameGraph(FrameGraph.Config(graphConfig))

    private fun initialize(scope: TestScope) {
        frameGraph.start()
        frameGraph.simulateCameraStarted()
        frameGraph.initializeSurfaces()
        scope.advanceUntilIdle()
    }

    @Test
    fun startFrameGraph_CameraGraphStarts() =
        testScope.runTest {
            assertEquals(GraphStateStopped, frameGraph.graphState.value)
            frameGraph.start()
            assertEquals(GraphStateStarting, frameGraph.graphState.value)
        }

    @Test
    fun stopFrameGraph_CameraGraphStops() =
        testScope.runTest {
            frameGraph.start()
            assertEquals(GraphStateStarting, frameGraph.graphState.value)

            frameGraph.stop()

            assertEquals(GraphStateStopping, frameGraph.graphState.value)
        }

    @Test
    fun captureWithSingleStream_repeatingRequestUpdates() =
        testScope.runTest {
            initialize(this)
            val stream = frameGraph.streams[streamConfig1]!!.id

            frameGraph.captureWith(setOf(stream))
            advanceUntilIdle()

            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            assertEquals(listOf(stream), frame.request.streams)
        }

    @Test
    fun captureWithMultipleStreams_repeatingRequestUpdates() =
        testScope.runTest {
            initialize(this)
            val stream1 = frameGraph.streams[streamConfig1]!!.id
            val stream2 = frameGraph.streams[streamConfig2]!!.id

            frameGraph.captureWith(setOf(stream1, stream2))
            advanceUntilIdle()

            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            assertEquals(listOf(stream1, stream2), frame.request.streams)
        }

    @Test
    fun captureWithMultipleStreamsAndParameters_repeatingRequestUpdates() =
        testScope.runTest {
            initialize(this)
            val stream1 = frameGraph.streams[streamConfig1]!!.id
            val stream2 = frameGraph.streams[streamConfig2]!!.id

            frameGraph.captureWith(
                setOf(stream1, stream2),
                mapOf(CAPTURE_REQUEST_KEY to 2, TEST_NULLABLE_KEY to null, TEST_KEY to 5),
            )
            advanceUntilIdle()

            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            assertEquals(listOf(stream1, stream2), frame.request.streams)
            val parameters: Map<CaptureRequest.Key<*>, Any> = mapOf(CAPTURE_REQUEST_KEY to 2)
            assertEquals(parameters, frame.request.parameters)
            val extras: Map<Metadata.Key<*>, Any> = mapOf(TEST_KEY to 5)
            assertEquals(extras, frame.request.extras)
        }

    @Test
    fun captureWithConflictingParameters_throwException() =
        testScope.runTest {
            initialize(this)
            val stream1 = frameGraph.streams[streamConfig1]!!.id
            val stream2 = frameGraph.streams[streamConfig2]!!.id
            frameGraph.captureWith(setOf(stream1, stream2), mapOf(CAPTURE_REQUEST_KEY to 2))

            assertThrows<IllegalStateException> {
                frameGraph.captureWith(setOf(stream1, stream2), mapOf(CAPTURE_REQUEST_KEY to 3))
            }
        }

    @Test
    fun detachAllStream_stopRepeating() =
        testScope.runTest {
            initialize(this)
            val stream1 = frameGraph.streams[streamConfig1]!!.id
            val stream2 = frameGraph.streams[streamConfig2]!!.id

            val buffer =
                frameGraph.captureWith(setOf(stream1, stream2), mapOf(CAPTURE_REQUEST_KEY to 2))
            advanceUntilIdle()
            assertEquals(listOf(stream1, stream2), frameGraph.simulateNextFrame().request.streams)

            buffer.close()
            advanceUntilIdle()
            assertThrows<IllegalStateException> { frameGraph.simulateNextFrame() }
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
            initialize(this)
            val events = mutableListOf<Int>()
            frameGraph.useSession { events += 1 }
            frameGraph.useSession { events += 2 }

            assertThat(events).containsExactly(1, 2).inOrder()
        }

    @Test
    fun testUseSessionInClosesAndDoesNotBlock() =
        testScope.runTest {
            initialize(this)
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
            initialize(this)
            val stream1 = frameGraph.streams[streamConfig1]!!.id
            val stream2 = frameGraph.streams[streamConfig2]!!.id
            val initialStreams = listOf(stream1)
            val repeatingRequestStreams = listOf(stream2)

            frameGraph.captureWith(initialStreams.toSet())
            advanceUntilIdle()

            assertEquals(initialStreams, frameGraph.simulateNextFrame().request.streams)
            frameGraph.useSession {
                it.startRepeating(Request(streams = repeatingRequestStreams))
                advanceUntilIdle()
                assertEquals(
                    repeatingRequestStreams,
                    frameGraph.simulateNextFrame().request.streams,
                )
            }
            advanceUntilIdle()

            assertEquals(initialStreams, frameGraph.simulateNextFrame().request.streams)
        }

    @Test
    fun useSession_invalidatesSessionAfterClosure_revertsParameters() =
        testScope.runTest {
            initialize(this)
            val stream = frameGraph.streams[streamConfig1]!!.id
            val repeatingRequestParameters =
                mapOf<CaptureRequest.Key<*>, Any>(CaptureRequest.SCALER_CROP_REGION to Rect())

            frameGraph.start()
            frameGraph.captureWith(streamIds = setOf(stream))
            advanceUntilIdle()
            assertEquals(emptyMap(), frameGraph.simulateNextFrame().request.parameters)

            frameGraph.useSession {
                it.startRepeating(
                    Request(streams = listOf(stream), parameters = repeatingRequestParameters)
                )
                advanceUntilIdle()
                assertEquals(
                    repeatingRequestParameters,
                    frameGraph.simulateNextFrame().request.parameters,
                )
            }
            advanceUntilIdle()

            assertEquals(emptyMap(), frameGraph.simulateNextFrame().request.parameters)
        }

    @Test
    fun useSession_invalidatesSessionAfterClosure_revertsBothStreamsAndParameters() =
        testScope.runTest {
            initialize(this)
            val stream1 = frameGraph.streams[streamConfig1]!!.id
            val stream2 = frameGraph.streams[streamConfig2]!!.id
            val initialStreams = listOf(stream1)
            val repeatingRequestStreams = listOf(stream2)
            val initialParameters = emptyMap<CaptureRequest.Key<*>, Any>()
            val repeatingRequestParameters =
                mapOf<CaptureRequest.Key<*>, Any>(CaptureRequest.SCALER_CROP_REGION to Rect())

            frameGraph.captureWith(initialStreams.toSet(), initialParameters.toMap())
            advanceUntilIdle()
            assertEquals(initialStreams, frameGraph.simulateNextFrame().request.streams)
            assertEquals(initialParameters, frameGraph.simulateNextFrame().request.parameters)
            frameGraph.useSession {
                it.startRepeating(
                    Request(
                        streams = repeatingRequestStreams,
                        parameters = repeatingRequestParameters,
                    )
                )
                advanceUntilIdle()
                assertEquals(
                    repeatingRequestStreams,
                    frameGraph.simulateNextFrame().request.streams,
                )
                assertEquals(
                    repeatingRequestParameters,
                    frameGraph.simulateNextFrame().request.parameters,
                )
            }
            advanceUntilIdle()

            assertEquals(initialStreams, frameGraph.simulateNextFrame().request.streams)
            assertEquals(initialParameters, frameGraph.simulateNextFrame().request.parameters)
        }

    @Test
    fun useSession_invalidatesSessionAfterClosure_restores3A() =
        testScope.runTest {
            initialize(this)
            val stream1 = frameGraph.streams[streamConfig1]!!.id
            val initialStreams = listOf(stream1)
            val initialParameters = emptyMap<CaptureRequest.Key<*>, Any>()
            val frameGraph3AParameters =
                mapOf<CaptureRequest.Key<*>, Any>(CaptureRequest.CONTROL_AE_LOCK to true)

            frameGraph.captureWith(initialStreams.toSet(), initialParameters.toMap())
            advanceUntilIdle()

            frameGraph.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)
            advanceUntilIdle()
            frameGraph.simulateNextFrame()
            assertEquals(
                frameGraph3AParameters,
                frameGraph.simulateNextFrame().requestSequence.requiredParameters,
            )

            frameGraph.useSession {
                it.unlock3A(ae = true)
                advanceUntilIdle()
                assertEquals(
                    mapOf<CaptureRequest.Key<*>, Any>(CaptureRequest.CONTROL_AE_LOCK to false),
                    frameGraph.simulateNextFrame().requestSequence.requiredParameters,
                )
            }
            advanceUntilIdle()

            assertEquals(
                frameGraph3AParameters,
                frameGraph.simulateNextFrame().requestSequence.requiredParameters,
            )
        }

    @Test
    fun useSession_invalidatesSessionAfterClosure_revertsStreamsAndParametersAnd3A() =
        testScope.runTest {
            initialize(this)
            val stream1 = frameGraph.streams[streamConfig1]!!.id
            val stream2 = frameGraph.streams[streamConfig2]!!.id
            val initialStreams = listOf(stream1)
            val repeatingRequestStreams = listOf(stream2)
            val initialParameters = emptyMap<CaptureRequest.Key<*>, Any>()
            val repeatingRequestParameters =
                mapOf<CaptureRequest.Key<*>, Any>(CaptureRequest.SCALER_CROP_REGION to Rect())
            val frameGraph3AParameters =
                mapOf<CaptureRequest.Key<*>, Any>(
                    CaptureRequest.CONTROL_AF_MODE to AfMode.AUTO.value
                )

            frameGraph.captureWith(initialStreams.toSet(), initialParameters.toMap())
            advanceUntilIdle()
            assertEquals(initialStreams, frameGraph.simulateNextFrame().request.streams)
            assertEquals(initialParameters, frameGraph.simulateNextFrame().request.parameters)

            frameGraph.update3A(afMode = AfMode.AUTO)
            advanceUntilIdle()
            assertEquals(
                frameGraph3AParameters,
                frameGraph.simulateNextFrame().requestSequence.requiredParameters,
            )

            frameGraph.useSession {
                it.startRepeating(
                    Request(
                        streams = repeatingRequestStreams,
                        parameters = repeatingRequestParameters,
                    )
                )
                advanceUntilIdle()
                assertEquals(
                    repeatingRequestStreams,
                    frameGraph.simulateNextFrame().request.streams,
                )
                assertEquals(
                    repeatingRequestParameters,
                    frameGraph.simulateNextFrame().request.parameters,
                )

                it.update3A(afMode = AfMode.MACRO)
                advanceUntilIdle()
                assertEquals(
                    mapOf<CaptureRequest.Key<*>, Any>(
                        CaptureRequest.CONTROL_AF_MODE to AfMode.MACRO.value
                    ),
                    frameGraph.simulateNextFrame().requestSequence.requiredParameters,
                )
            }
            advanceUntilIdle()

            // Simulate a few requests to invalidate the FrameBuffer(s) when session closes.
            frameGraph.simulateNextFrame()

            assertEquals(initialStreams, frameGraph.simulateNextFrame().request.streams)
            assertEquals(initialParameters, frameGraph.simulateNextFrame().request.parameters)
            assertEquals(
                frameGraph3AParameters,
                frameGraph.simulateNextFrame().requestSequence.requiredParameters,
            )
        }

    @Test
    fun capture_returnsPendingFrameCaptureImmediately() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id

            val frameCapture = frameGraph.capture(Request(streams = listOf(streamId)))

            assertThat(frameCapture).isNotNull()
            assertThat(frameCapture.status).isEqualTo(OutputStatus.PENDING)
        }

    @Test
    fun capture_waitsForActiveSessionLock() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id

            val session = frameGraph.acquireSession()

            val frameCapture = frameGraph.capture(Request(streams = listOf(streamId)))
            advanceUntilIdle()

            assertThat(frameCapture.status).isEqualTo(OutputStatus.PENDING)

            session.close()
            advanceUntilIdle()

            val frame = frameGraph.simulateNextFrame()
            assertEquals(listOf(streamId), frame.request.streams)
            assertThat(frameCapture.status).isEqualTo(OutputStatus.AVAILABLE)
        }

    @Test
    fun captureBurst_linksAllCaptures() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id
            val requests =
                listOf(Request(streams = listOf(streamId)), Request(streams = listOf(streamId)))

            val session = frameGraph.acquireSession()
            val frameCaptures = frameGraph.capture(requests)

            assertThat(frameCaptures).hasSize(2)
            frameCaptures.forEach { assertThat(it.status).isEqualTo(OutputStatus.PENDING) }

            session.close()
            advanceUntilIdle()

            frameGraph.simulateNextFrame()
            frameGraph.simulateNextFrame()

            frameCaptures.forEach { assertThat(it.status).isEqualTo(OutputStatus.AVAILABLE) }
        }

    @Test
    fun capture_whenClosed_isAborted() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id

            frameGraph.close()
            advanceUntilIdle()

            val frameCapture = frameGraph.capture(Request(streams = listOf(streamId)))
            advanceUntilIdle()

            assertThat(frameCapture.status).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        }

    @Test
    fun capture_handlesCancellationDuringLockContention() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id

            val session = frameGraph.acquireSession()
            val frameCapture = frameGraph.capture(Request(streams = listOf(streamId)))

            frameGraph.close()
            advanceUntilIdle()

            assertThat(frameCapture.status).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)

            session.close()
            advanceUntilIdle()
        }

    @Test
    fun capture_statusReportsCorrectTerminalState() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id

            val session = frameGraph.acquireSession()
            val captureToAbort = frameGraph.capture(Request(streams = listOf(streamId)))

            frameGraph.close()
            advanceUntilIdle()

            assertThat(captureToAbort.status).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
            session.close()
        }

    @Test
    fun capture_awaitFrameReturnsNullIfAborted() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id

            val session = frameGraph.acquireSession()
            val frameCapture = frameGraph.capture(Request(streams = listOf(streamId)))

            frameGraph.close()
            advanceUntilIdle()

            val result = frameCapture.awaitFrame()
            assertThat(result).isNull()

            session.close()
        }

    @Test
    fun capture_orderingIsPreservedUnderLockContention() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id

            val requestA = Request(streams = listOf(streamId))
            val requestB = Request(streams = listOf(streamId))

            val session = frameGraph.acquireSession()

            val frameGraphCapture = frameGraph.capture(requestA)
            advanceUntilIdle()

            val sessionCapture = session.capture(requestB)
            advanceUntilIdle()

            assertThat(frameGraphCapture.status).isEqualTo(OutputStatus.PENDING)

            session.close()
            advanceUntilIdle()

            val firstSimulatedFrame = frameGraph.simulateNextFrame()
            val secondSimulatedFrame = frameGraph.simulateNextFrame()

            val frameB = sessionCapture.getFrame()
            assertThat(frameB).isNotNull()
            assertThat(sessionCapture.status).isEqualTo(OutputStatus.AVAILABLE)
            assertEquals(firstSimulatedFrame.frameNumber, frameB!!.frameNumber)

            val frameA = frameGraphCapture.getFrame()
            assertThat(frameA).isNotNull()
            assertThat(frameGraphCapture.status).isEqualTo(OutputStatus.AVAILABLE)
            assertEquals(secondSimulatedFrame.frameNumber, frameA!!.frameNumber)

            assertThat(frameB.frameNumber.value).isLessThan(frameA.frameNumber.value)
        }

    @Test
    fun capture_identicalRequestOrderingUnderLockContention() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id

            val sharedRequest = Request(streams = listOf(streamId))

            val session = frameGraph.acquireSession()

            val frameGraphCapture = frameGraph.capture(sharedRequest)
            advanceUntilIdle()

            val sessionCapture = session.capture(sharedRequest)
            advanceUntilIdle()

            assertThat(frameGraphCapture.status).isEqualTo(OutputStatus.PENDING)

            session.close()
            advanceUntilIdle()

            val firstSimulatedFrame = frameGraph.simulateNextFrame()
            val secondSimulatedFrame = frameGraph.simulateNextFrame()

            val frameFromSession = sessionCapture.getFrame()
            assertThat(frameFromSession).isNotNull()
            assertThat(sessionCapture.status).isEqualTo(OutputStatus.AVAILABLE)
            assertEquals(firstSimulatedFrame.frameNumber, frameFromSession!!.frameNumber)

            val frameFromFG = frameGraphCapture.getFrame()
            assertThat(frameFromFG).isNotNull()
            assertThat(frameGraphCapture.status).isEqualTo(OutputStatus.AVAILABLE)
            assertEquals(secondSimulatedFrame.frameNumber, frameFromFG!!.frameNumber)

            assertThat(frameFromSession.frameNumber.value).isLessThan(frameFromFG.frameNumber.value)
        }

    @Test
    fun capture_multipleIndividualCalls_areBatchedUnderContention() =
        testScope.runTest {
            initialize(this)
            val streamId = frameGraph.streams[streamConfig1]!!.id

            val session = frameGraph.acquireSession()

            val capture1 = frameGraph.capture(Request(streams = listOf(streamId)))
            val capture2 = frameGraph.capture(Request(streams = listOf(streamId)))
            val capture3 = frameGraph.capture(Request(streams = listOf(streamId)))

            advanceUntilIdle()

            assertThat(capture1.status).isEqualTo(OutputStatus.PENDING)
            assertThat(capture2.status).isEqualTo(OutputStatus.PENDING)
            assertThat(capture3.status).isEqualTo(OutputStatus.PENDING)

            session.close()
            advanceUntilIdle()

            frameGraph.simulateNextFrame()
            frameGraph.simulateNextFrame()
            frameGraph.simulateNextFrame()

            assertThat(capture1.status).isEqualTo(OutputStatus.AVAILABLE)
            assertThat(capture2.status).isEqualTo(OutputStatus.AVAILABLE)
            assertThat(capture3.status).isEqualTo(OutputStatus.AVAILABLE)
        }

    companion object {
        private val CAPTURE_REQUEST_KEY = CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
        private val TEST_NULLABLE_KEY = CaptureRequest.BLACK_LEVEL_LOCK
    }
}
