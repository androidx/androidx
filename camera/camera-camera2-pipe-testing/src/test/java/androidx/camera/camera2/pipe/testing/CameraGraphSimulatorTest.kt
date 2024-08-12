/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraGraphSimulatorTest {
    private val testScope = TestScope()
    private val metadata =
        FakeCameraMetadata(
            characteristics =
                mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT)
        )

    private val streamConfig = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)

    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(streamConfig))

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val simulator = CameraGraphSimulator.create(testScope, context, metadata, graphConfig)

    @Test
    fun simulatorCanSimulateRepeatingFrames() =
        testScope.runTest {
            val stream = simulator.streams[streamConfig]!!
            val listener = FakeRequestListener()
            val request = Request(streams = listOf(stream.id), listeners = listOf(listener))
            simulator.acquireSession().use { it.startRepeating(request) }
            simulator.start()
            simulator.simulateCameraStarted()
            simulator.initializeSurfaces()
            advanceUntilIdle()

            val frame = simulator.simulateNextFrame()

            assertThat(frame.request).isSameInstanceAs(request)
            assertThat(frame.frameNumber.value).isGreaterThan(0)
            assertThat(frame.timestampNanos).isGreaterThan(0)

            val startEvent = listener.onStartedFlow.first()
            assertThat(startEvent.frameNumber).isNotNull()
            assertThat(startEvent.frameNumber).isEqualTo(frame.frameNumber)
            assertThat(startEvent.timestamp).isNotNull()
            assertThat(startEvent.timestamp.value).isGreaterThan(0)
            assertThat(startEvent.requestMetadata.repeating).isTrue()
            assertThat(startEvent.requestMetadata.request.streams).contains(stream.id)
            assertThat(startEvent.requestMetadata.template).isEqualTo(graphConfig.defaultTemplate)

            val totalCaptureResultEvent =
                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(timeMillis = 50) { listener.onTotalCaptureResultFlow.first() }
                }

            assertThat(totalCaptureResultEvent).isNull()

            // Launch the callbacks in a coroutine job to test the behavior of the simulator.
            val simulateCallbacks = launch {
                val resultMetadata = mutableMapOf<CaptureResult.Key<*>, Any>()
                // Simulate two partial capture results, and one total capture result.
                resultMetadata[CaptureResult.LENS_STATE] = CaptureResult.LENS_STATE_MOVING
                frame.simulatePartialCaptureResult(resultMetadata)
                delay(10)

                resultMetadata[CaptureResult.LENS_APERTURE] = 2.0f
                frame.simulatePartialCaptureResult(resultMetadata)
                delay(10)

                resultMetadata[CaptureResult.FLASH_STATE] = CaptureResult.FLASH_STATE_FIRED
                frame.simulateTotalCaptureResult(resultMetadata)
                delay(10)

                frame.simulateComplete(
                    resultMetadata,
                    extraMetadata = mapOf(CaptureResult.LENS_APERTURE to 4.0f)
                )
            }

            val partialEvent1 = listener.onPartialCaptureResultFlow.first()
            assertThat(partialEvent1.frameNumber).isEqualTo(frame.frameNumber)
            assertThat(partialEvent1.frameMetadata.camera).isEqualTo(metadata.camera)
            assertThat(partialEvent1.frameMetadata[CaptureResult.LENS_STATE]).isEqualTo(1)
            assertThat(partialEvent1.frameMetadata[CaptureResult.LENS_APERTURE]).isNull()
            assertThat(partialEvent1.frameMetadata[CaptureResult.FLASH_STATE]).isNull()

            val partialEvent2 = listener.onPartialCaptureResultFlow.drop(1).first()
            assertThat(partialEvent2.frameNumber).isEqualTo(frame.frameNumber)
            assertThat(partialEvent2.frameMetadata.camera).isEqualTo(metadata.camera)
            assertThat(partialEvent2.frameMetadata[CaptureResult.LENS_STATE]).isEqualTo(1)
            assertThat(partialEvent2.frameMetadata[CaptureResult.LENS_APERTURE]).isEqualTo(2.0f)
            assertThat(partialEvent2.frameMetadata[CaptureResult.FLASH_STATE]).isNull()

            val totalEvent = listener.onTotalCaptureResultFlow.first()
            assertThat(totalEvent.frameNumber).isEqualTo(frame.frameNumber)
            assertThat(totalEvent.frameInfo.camera).isEqualTo(metadata.camera)
            assertThat(totalEvent.frameInfo.metadata[CaptureResult.LENS_STATE]).isEqualTo(1)
            assertThat(totalEvent.frameInfo.metadata[CaptureResult.LENS_APERTURE]).isEqualTo(2.0f)
            assertThat(totalEvent.frameInfo.metadata[CaptureResult.FLASH_STATE]).isEqualTo(3)

            val completedEvent = listener.onCompleteFlow.first()
            assertThat(completedEvent.frameNumber).isEqualTo(frame.frameNumber)
            assertThat(completedEvent.frameInfo.camera).isEqualTo(metadata.camera)
            assertThat(completedEvent.frameInfo.metadata[CaptureResult.LENS_STATE]).isEqualTo(1)
            assertThat(completedEvent.frameInfo.metadata[CaptureResult.LENS_APERTURE])
                .isEqualTo(4.0f)
            assertThat(completedEvent.frameInfo.metadata[CaptureResult.FLASH_STATE]).isEqualTo(3)

            simulateCallbacks.join()
        }

    @Test
    fun simulatorAbortsRequests() =
        testScope.runTest {
            val stream = simulator.streams[streamConfig]!!
            val listener = FakeRequestListener()
            val request = Request(streams = listOf(stream.id), listeners = listOf(listener))

            simulator.acquireSession().use { it.submit(request = request) }
            simulator.close()

            val abortedEvent = listener.onAbortedFlow.first()
            assertThat(abortedEvent.request).isSameInstanceAs(request)
        }

    @Test
    fun simulatorCanIssueBufferLoss() =
        testScope.runTest {
            val stream = simulator.streams[streamConfig]!!
            val listener = FakeRequestListener()
            val request = Request(streams = listOf(stream.id), listeners = listOf(listener))

            simulator.acquireSession().use { it.submit(request = request) }

            simulator.start()
            simulator.simulateCameraStarted()
            simulator.initializeSurfaces()
            advanceUntilIdle()

            val frame = simulator.simulateNextFrame()
            assertThat(frame.request).isSameInstanceAs(request)

            frame.simulateBufferLoss(stream.id)
            val lossEvent = listener.onBufferLostFlow.first()
            assertThat(lossEvent.frameNumber).isEqualTo(frame.frameNumber)
            assertThat(lossEvent.requestMetadata.request).isSameInstanceAs(request)
            assertThat(lossEvent.streamId).isEqualTo(stream.id)
        }

    @Test
    fun simulatorCanIssueMultipleFrames() =
        testScope.runTest {
            val stream = simulator.streams[streamConfig]!!
            val listener = FakeRequestListener()
            val request = Request(streams = listOf(stream.id), listeners = listOf(listener))

            simulator.acquireSession().use { it.startRepeating(request = request) }
            simulator.start()
            simulator.simulateCameraStarted()
            simulator.initializeSurfaces()
            advanceUntilIdle()

            val frame1 = simulator.simulateNextFrame()
            val frame2 = simulator.simulateNextFrame()
            val frame3 = simulator.simulateNextFrame()

            assertThat(frame1).isNotEqualTo(frame2)
            assertThat(frame2).isNotEqualTo(frame3)
            assertThat(frame1.request).isSameInstanceAs(request)
            assertThat(frame2.request).isSameInstanceAs(request)
            assertThat(frame3.request).isSameInstanceAs(request)

            val simulateCallbacks = launch {
                val resultMetadata = mutableMapOf<CaptureResult.Key<*>, Any>()
                resultMetadata[CaptureResult.LENS_STATE] = CaptureResult.LENS_STATE_MOVING
                frame1.simulateTotalCaptureResult(resultMetadata)
                frame1.simulateComplete(resultMetadata)

                delay(15)
                frame2.simulateTotalCaptureResult(resultMetadata)
                frame2.simulateComplete(resultMetadata)

                delay(15)
                resultMetadata[CaptureResult.LENS_STATE] = CaptureResult.LENS_STATE_STATIONARY
                frame3.simulateTotalCaptureResult(resultMetadata)
                frame3.simulateComplete(resultMetadata)
            }

            val startEvents =
                withTimeout(timeMillis = 250) { listener.onStartedFlow.take(3).toList() }
            assertThat(startEvents).hasSize(3)

            val event1 = startEvents[0]
            val event2 = startEvents[1]
            val event3 = startEvents[2]

            // Frame numbers are not equal
            assertThat(event1.frameNumber).isNotEqualTo(event2.frameNumber)
            assertThat(event2.frameNumber).isNotEqualTo(event3.frameNumber)

            // Timestamps are in ascending order
            assertThat(event3.timestamp.value).isGreaterThan(event2.timestamp.value)
            assertThat(event2.timestamp.value).isGreaterThan(event1.timestamp.value)

            // Metadata references the same request.
            assertThat(event1.requestMetadata.repeating).isTrue()
            assertThat(event2.requestMetadata.repeating).isTrue()
            assertThat(event3.requestMetadata.repeating).isTrue()
            assertThat(event1.requestMetadata.request).isSameInstanceAs(request)
            assertThat(event2.requestMetadata.request).isSameInstanceAs(request)
            assertThat(event3.requestMetadata.request).isSameInstanceAs(request)

            val completeEvents =
                withTimeout(timeMillis = 250) { listener.onCompleteFlow.take(3).toList() }
            assertThat(completeEvents).hasSize(3)

            val completeEvent1 = completeEvents[0]
            val completeEvent2 = completeEvents[1]
            val completeEvent3 = completeEvents[2]

            assertThat(completeEvent1.frameNumber).isEqualTo(event1.frameNumber)
            assertThat(completeEvent2.frameNumber).isEqualTo(event2.frameNumber)
            assertThat(completeEvent3.frameNumber).isEqualTo(event3.frameNumber)

            assertThat(completeEvent1.frameInfo.metadata[CaptureResult.LENS_STATE])
                .isEqualTo(CaptureResult.LENS_STATE_MOVING)
            assertThat(completeEvent2.frameInfo.metadata[CaptureResult.LENS_STATE])
                .isEqualTo(CaptureResult.LENS_STATE_MOVING)
            assertThat(completeEvent3.frameInfo.metadata[CaptureResult.LENS_STATE])
                .isEqualTo(CaptureResult.LENS_STATE_STATIONARY)

            simulateCallbacks.join()
        }

    @Test
    fun simulatorCanSimulateGraphState() =
        testScope.runTest {
            assertThat(simulator.graphState.value).isEqualTo(GraphStateStopped)

            simulator.start()
            assertThat(simulator.graphState.value).isEqualTo(GraphStateStarting)

            simulator.simulateCameraStarted()
            assertThat(simulator.graphState.value).isEqualTo(GraphStateStarted)

            simulator.stop()
            assertThat(simulator.graphState.value).isEqualTo(GraphStateStopping)

            simulator.simulateCameraStopped()
            assertThat(simulator.graphState.value).isEqualTo(GraphStateStopped)
        }

    @Test
    fun simulatorCanSimulateGraphError() =
        testScope.runTest {
            val error = GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)

            simulator.simulateCameraError(error)
            // The CameraGraph is stopped at this point, so the errors should be ignored.
            assertThat(simulator.graphState.value).isEqualTo(GraphStateStopped)

            simulator.start()
            simulator.simulateCameraError(error)
            val graphState = simulator.graphState.value
            assertThat(graphState).isInstanceOf(GraphStateError::class.java)
            val graphStateError = graphState as GraphStateError
            assertThat(graphStateError.cameraError).isEqualTo(error.cameraError)
            assertThat(graphStateError.willAttemptRetry).isEqualTo(error.willAttemptRetry)

            simulator.simulateCameraStarted()
            assertThat(simulator.graphState.value).isEqualTo(GraphStateStarted)

            simulator.stop()
            simulator.simulateCameraStopped()
            simulator.simulateCameraError(error)
            assertThat(simulator.graphState.value).isEqualTo(GraphStateStopped)
        }
}
