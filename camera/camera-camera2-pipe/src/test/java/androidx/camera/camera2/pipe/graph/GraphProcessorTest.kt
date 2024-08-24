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

package androidx.camera.camera2.pipe.graph

import android.graphics.SurfaceTexture
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AE_LOCK
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isCapture
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isClose
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isRejected
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isRepeating
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requests
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requiredParameters
import androidx.camera.camera2.pipe.testing.FakeGraphConfigs
import androidx.camera.camera2.pipe.testing.FakeRequestListener
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class GraphProcessorTest {
    private val testScope = TestScope()
    private val fakeThreads = FakeThreads.fromTestScope(testScope)

    private val globalListener = FakeRequestListener()
    private val graphState3A = GraphState3A()
    private val graphListener3A = Listener3A()
    private val streamId = StreamId(0)
    private val surfaceMap = mapOf(streamId to Surface(SurfaceTexture(1)))

    private val csp1 = FakeCaptureSequenceProcessor().also { it.surfaceMap = surfaceMap }
    private val csp2 = FakeCaptureSequenceProcessor().also { it.surfaceMap = surfaceMap }

    private val grp1 = GraphRequestProcessor.from(csp1)
    private val grp2 = GraphRequestProcessor.from(csp2)

    private val requestListener1 = FakeRequestListener()
    private val request1 = Request(listOf(StreamId(0)), listeners = listOf(requestListener1))

    private val requestListener2 = FakeRequestListener()
    private val request2 = Request(listOf(StreamId(0)), listeners = listOf(requestListener2))

    private val graphProcessor =
        GraphProcessorImpl(
            fakeThreads,
            CameraGraphId.nextId(),
            FakeGraphConfigs.graphConfig,
            graphState3A,
            graphListener3A,
            arrayListOf(globalListener)
        )

    @After
    fun teardown() {
        surfaceMap[streamId]?.release()
    }

    @Test
    fun graphProcessorSubmitsRequests() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.submit(request1)
            advanceUntilIdle()

            // Make sure the requests get submitted to the request processor
            assertThat(csp1.events.size).isEqualTo(1)

            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requiredParameters)
                .containsEntry(CaptureRequest.JPEG_THUMBNAIL_QUALITY, 42)
        }

    @Test
    fun graphProcessorSubmitsRequestsToMostRecentProcessor() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.onGraphStarted(grp2)
            graphProcessor.submit(request1)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isCapture).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
        }

    @Test
    fun graphProcessorSubmitsQueuedRequests() =
        testScope.runTest {
            graphProcessor.submit(request1)
            graphProcessor.submit(request2)

            // Request1 and 2 should be queued and will be submitted even when the request
            // processor is set after the requests are submitted.
            graphProcessor.onGraphStarted(grp1)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
        }

    @Test
    fun graphProcessorSubmitsBurstsOfRequestsTogetherWithExtras() =
        testScope.runTest {
            graphProcessor.submit(listOf(request1, request2))
            graphProcessor.onGraphStarted(grp1)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1, request2).inOrder()
        }

    @Test
    fun graphProcessorDoesNotForgetRejectedRequests() =
        testScope.runTest {
            csp1.rejectSubmit = true
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.submit(request1)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRejected).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            graphProcessor.submit(request2)
            advanceUntilIdle()
            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[1].isRejected).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1) // Re-attempt #1

            graphProcessor.onGraphStarted(grp2)
            advanceUntilIdle()

            // Assert that after a new request processor is set, it receives the queued up requests.
            assertThat(csp2.events.size).isEqualTo(2)
            assertThat(csp2.events[0].isCapture).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
            assertThat(csp2.events[1].isCapture).isTrue()
            assertThat(csp2.events[1].requests).containsExactly(request2).inOrder()
        }

    @Test
    fun graphProcessorContinuesSubmittingRequestsWhenFirstRequestIsRejected() =
        testScope.runTest {

            // Note: setting the requestProcessor, and calling submit() can both trigger a call
            // to submit a request.
            csp1.rejectSubmit = true
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.submit(request1)
            advanceUntilIdle()

            // Check to make sure that submit is called at least once, and that request1 is rejected
            // from the request processor.
            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRejected).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            // Stop rejecting requests
            csp1.rejectSubmit = false

            graphProcessor.submit(request2)
            advanceUntilIdle()

            // Assert that immediately after we get a successfully submitted request, the
            //  next request is also submitted.
            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
        }

    @Test
    fun graphProcessorSetsRepeatingRequest() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            graphProcessor.repeatingRequest = request2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request2)
            assertThat(csp1.events[0].requiredParameters)
                .containsEntry(CaptureRequest.JPEG_THUMBNAIL_QUALITY, 42)
        }

    @Test
    fun graphProcessorDoesNotForgetRejectedRepeatingRequests() =
        testScope.runTest {
            csp1.rejectSubmit = true
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRejected).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            graphProcessor.repeatingRequest = request2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[1].isRejected).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)

            csp1.rejectSubmit = false
            graphProcessor.invalidate()
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[2].isRepeating).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
        }

    @Test
    fun graphProcessorTracksRepeatingRequest() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            graphProcessor.onGraphStarted(grp2)
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isRepeating).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
        }

    @Test
    fun graphProcessorTracksRejectedRepeatingRequests() =
        testScope.runTest {
            csp1.rejectSubmit = true
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRejected).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            graphProcessor.onGraphStarted(grp2)
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isRepeating).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
        }

    @Test
    fun graphProcessorSubmitsRepeatingRequestAndQueuedRequests() =
        testScope.runTest {
            graphProcessor.repeatingRequest = request1
            graphProcessor.submit(request2)
            advanceUntilIdle()

            graphProcessor.onGraphStarted(grp1)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
        }

    @Test
    fun graphProcessorAbortsQueuedRequests() =
        testScope.runTest {
            graphProcessor.repeatingRequest = request1
            graphProcessor.submit(request2)

            // Abort queued and in-flight requests.
            graphProcessor.abort()
            graphProcessor.onGraphStarted(grp1)

            val abortEvent1 = requestListener2.onAbortedFlow.first()
            val globalAbortEvent = globalListener.onAbortedFlow.first()

            assertThat(abortEvent1.request).isSameInstanceAs(request2)
            assertThat(globalAbortEvent.request).isSameInstanceAs(request2)

            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
        }

    @Test
    fun closingGraphProcessorAbortsSubsequentRequests() =
        testScope.runTest {
            graphProcessor.close()
            advanceUntilIdle()

            // Abort queued and in-flight requests.
            // graphProcessor.onGraphStarted(graphRequestProcessor1)
            graphProcessor.repeatingRequest = request1
            graphProcessor.submit(request2)

            val abortEvent1 =
                withTimeoutOrNull(timeMillis = 50L) { requestListener1.onAbortedFlow.firstOrNull() }
            val abortEvent2 = requestListener2.onAbortedFlow.first()
            assertThat(abortEvent1).isNull()
            assertThat(abortEvent2.request).isSameInstanceAs(request2)
        }

    @Test
    fun graphProcessorResubmitsParametersAfterGraphStarts() =
        testScope.runTest {
            // Submit a repeating request first to make sure we have one in progress.
            graphProcessor.repeatingRequest = request1
            graphProcessor.submit(mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AE_LOCK to false))
            graphProcessor.onGraphStarted(grp1)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].requiredParameters).containsEntry(CONTROL_AE_LOCK, false)
        }

    @Test
    fun graphProcessorSubmitsLatestParametersWhenSubmittedTwiceBeforeGraphStarts() =
        testScope.runTest {

            // Submit a repeating request first to make sure we have one in progress.
            graphProcessor.repeatingRequest = request1
            graphProcessor.submit(mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AE_LOCK to false))
            graphProcessor.submit(mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AE_LOCK to true))
            advanceUntilIdle()

            graphProcessor.onGraphStarted(grp1)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].requiredParameters).containsEntry(CONTROL_AE_LOCK, false)

            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request1)
            assertThat(csp1.events[2].requiredParameters).containsEntry(CONTROL_AE_LOCK, true)
        }

    @Test
    fun trySubmitShouldReturnFalseWhenNoRepeatingRequestIsQueued() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            advanceUntilIdle()

            assertThrows<IllegalStateException> {
                graphProcessor.submit(mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AE_LOCK to true))
            }
        }

    @Test
    fun graphProcessorChangesGraphStateOnError() =
        testScope.runTest {
            assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)

            graphProcessor.onGraphStarted(grp1)
            graphProcessor.onGraphError(
                GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)
            )
            assertThat(graphProcessor.graphState.value).isInstanceOf(GraphStateError::class.java)
        }

    @Test
    fun graphProcessorDropsStaleErrors() =
        testScope.runTest {
            assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)

            graphProcessor.onGraphError(
                GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)
            )
            assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)

            graphProcessor.onGraphStarting()
            graphProcessor.onGraphStarted(grp1)

            // GraphProcessor should drop errors while the camera graph is stopping.
            graphProcessor.onGraphStopping()
            graphProcessor.onGraphError(
                GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)
            )
            assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)

            // GraphProcessor should also drop errors while the camera graph is stopped.
            graphProcessor.onGraphStopped(grp1)
            graphProcessor.onGraphError(
                GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)
            )
            assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)
        }
}
