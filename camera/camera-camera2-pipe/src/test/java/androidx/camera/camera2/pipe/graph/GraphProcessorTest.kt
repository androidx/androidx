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
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.awaitEvent
import androidx.camera.camera2.pipe.testing.FakeGraphConfigs
import androidx.camera.camera2.pipe.testing.FakeRequestListener
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
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
    private val globalListener = FakeRequestListener()
    private val graphState3A = GraphState3A()
    private val streamId = StreamId(0)
    private val surfaceMap = mapOf(streamId to Surface(SurfaceTexture(1)))

    private val fakeProcessor1 = FakeCaptureSequenceProcessor().also { it.surfaceMap = surfaceMap }
    private val fakeProcessor2 = FakeCaptureSequenceProcessor().also { it.surfaceMap = surfaceMap }

    private val graphRequestProcessor1 = GraphRequestProcessor.from(fakeProcessor1)
    private val graphRequestProcessor2 = GraphRequestProcessor.from(fakeProcessor2)

    private val requestListener1 = FakeRequestListener()
    private val request1 = Request(listOf(StreamId(0)), listeners = listOf(requestListener1))

    private val requestListener2 = FakeRequestListener()
    private val request2 = Request(listOf(StreamId(0)), listeners = listOf(requestListener2))

    @After
    fun teardown() {
        surfaceMap[streamId]?.release()
    }

    @Test
    fun graphProcessorSubmitsRequests() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )
        graphProcessor.onGraphStarted(graphRequestProcessor1)
        graphProcessor.submit(request1)
        advanceUntilIdle()

        // Make sure the requests get submitted to the request processor
        val event = fakeProcessor1.nextEvent()
        assertThat(event.requestSequence!!.captureRequestList).containsExactly(request1)
        assertThat(event.requestSequence!!.requiredParameters)
            .containsEntry(CaptureRequest.JPEG_THUMBNAIL_QUALITY, 42)
    }

    @Test
    fun graphProcessorSubmitsRequestsToMostRecentProcessor() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        graphProcessor.onGraphStarted(graphRequestProcessor1)
        graphProcessor.onGraphStarted(graphRequestProcessor2)
        graphProcessor.submit(request1)

        val event1 = fakeProcessor1.nextEvent()
        assertThat(event1.close).isTrue()

        val event2 = fakeProcessor2.nextEvent()
        assertThat(event2.submit).isTrue()
        assertThat(event2.requestSequence!!.captureRequestList).containsExactly(request1)
    }

    @Test
    fun graphProcessorSubmitsQueuedRequests() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        graphProcessor.submit(request1)
        graphProcessor.submit(request2)

        // Request1 and 2 should be queued and will be submitted even when the request
        // processor is set after the requests are submitted.
        graphProcessor.onGraphStarted(graphRequestProcessor1)

        val event1 = fakeProcessor1.awaitEvent(request = request1) { it.submit }
        assertThat(event1.requestSequence!!.captureRequestList).hasSize(1)
        assertThat(event1.requestSequence!!.captureRequestList).contains(request1)

        val event2 = fakeProcessor1.nextEvent()
        assertThat(event2.requestSequence!!.captureRequestList).hasSize(1)
        assertThat(event2.requestSequence!!.captureRequestList).contains(request2)
    }

    @Test
    fun graphProcessorSubmitsBurstsOfRequestsTogetherWithExtras() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        graphProcessor.submit(listOf(request1, request2))
        graphProcessor.onGraphStarted(graphRequestProcessor1)
        val event = fakeProcessor1.awaitEvent(request = request1) { it.submit }
        assertThat(event.requestSequence!!.captureRequestList).hasSize(2)
        assertThat(event.requestSequence!!.captureRequestList).contains(request1)
        assertThat(event.requestSequence!!.captureRequestList).contains(request2)
    }

    @Test
    fun graphProcessorDoesNotForgetRejectedRequests() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        fakeProcessor1.rejectRequests = true
        graphProcessor.onGraphStarted(graphRequestProcessor1)

        graphProcessor.submit(request1)
        val event1 = fakeProcessor1.nextEvent()
        assertThat(event1.rejected).isTrue()
        assertThat(event1.requestSequence!!.captureRequestList[0]).isSameInstanceAs(request1)

        graphProcessor.submit(request2)
        val event2 = fakeProcessor1.nextEvent()
        assertThat(event2.rejected).isTrue()
        assertThat(event2.requestSequence!!.captureRequestList[0]).isSameInstanceAs(request1)

        graphProcessor.onGraphStarted(graphRequestProcessor2)
        assertThat(fakeProcessor2.nextEvent().requestSequence!!.captureRequestList[0])
            .isSameInstanceAs(request1)
        assertThat(fakeProcessor2.nextEvent().requestSequence!!.captureRequestList[0])
            .isSameInstanceAs(request2)
    }

    @Test
    fun graphProcessorContinuesSubmittingRequestsWhenFirstRequestIsRejected() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        // Note: setting the requestProcessor, and calling submit() can both trigger a call
        // to submit a request.
        fakeProcessor1.rejectRequests = true
        graphProcessor.onGraphStarted(graphRequestProcessor1)
        graphProcessor.submit(request1)

        // Check to make sure that submit is called at least once, and that request1 is rejected
        // from the request processor.
        fakeProcessor1.awaitEvent(request = request1) { it.rejected }

        // Stop rejecting requests
        fakeProcessor1.rejectRequests = false

        graphProcessor.submit(request2)
        // Cycle events until we get a submitted event with request1
        val event2 = fakeProcessor1.awaitEvent(request = request1) { it.submit }
        assertThat(event2.rejected).isFalse()

        // Assert that immediately after we get a successfully submitted request, the
        //  next request is also submitted.
        val event3 = fakeProcessor1.nextEvent()
        assertThat(event3.requestSequence!!.captureRequestList).contains(request2)
        assertThat(event3.submit).isTrue()
        assertThat(event3.rejected).isFalse()
    }

    @Test
    fun graphProcessorSetsRepeatingRequest() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        graphProcessor.onGraphStarted(graphRequestProcessor1)
        graphProcessor.startRepeating(request1)
        graphProcessor.startRepeating(request2)
        advanceUntilIdle()

        val event =
            fakeProcessor1.awaitEvent(request = request2) {
                it.submit && it.requestSequence?.repeating == true
            }
        assertThat(event.requestSequence!!.requiredParameters)
            .containsEntry(CaptureRequest.JPEG_THUMBNAIL_QUALITY, 42)
    }

    @Test
    fun graphProcessorDoesNotForgetRejectedRepeatingRequests() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        fakeProcessor1.rejectRequests = true
        graphProcessor.onGraphStarted(graphRequestProcessor1)

        graphProcessor.startRepeating(request1)
        val event1 = fakeProcessor1.nextEvent()
        assertThat(event1.rejected).isTrue()
        assertThat(event1.requestSequence!!.captureRequestList[0]).isSameInstanceAs(request1)

        graphProcessor.startRepeating(request2)
        val event2 = fakeProcessor1.nextEvent()
        assertThat(event2.rejected).isTrue()
        fakeProcessor1.awaitEvent(request = request2) {
            !it.submit && it.requestSequence?.repeating == true
        }

        fakeProcessor1.rejectRequests = false
        graphProcessor.onGraphStarted(graphRequestProcessor1)

        fakeProcessor1.awaitEvent(request = request2) {
            it.submit && it.requestSequence?.repeating == true
        }
    }

    @Test
    fun graphProcessorTracksRepeatingRequest() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        graphProcessor.onGraphStarted(graphRequestProcessor1)
        graphProcessor.startRepeating(request1)
        advanceUntilIdle()

        fakeProcessor1.awaitEvent(request = request1) {
            it.submit && it.requestSequence?.repeating == true
        }

        graphProcessor.onGraphStarted(graphRequestProcessor2)
        advanceUntilIdle()

        fakeProcessor2.awaitEvent(request = request1) {
            it.submit && it.requestSequence?.repeating == true
        }
    }

    @Test
    fun graphProcessorTracksRejectedRepeatingRequests() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        fakeProcessor1.rejectRequests = true
        graphProcessor.onGraphStarted(graphRequestProcessor1)
        graphProcessor.startRepeating(request1)
        fakeProcessor1.awaitEvent(request = request1) { it.rejected }

        graphProcessor.onGraphStarted(graphRequestProcessor2)
        fakeProcessor2.awaitEvent(request = request1) {
            it.submit && it.requestSequence?.repeating == true
        }
    }

    @Test
    fun graphProcessorSubmitsRepeatingRequestAndQueuedRequests() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        graphProcessor.startRepeating(request1)
        graphProcessor.submit(request2)
        delay(50)

        graphProcessor.onGraphStarted(graphRequestProcessor1)

        var hasRequest1Event = false
        var hasRequest2Event = false

        // Loop until we see at least one repeating request, and one submit event.
        launch {
            while (!hasRequest1Event && !hasRequest2Event) {
                val event = fakeProcessor1.nextEvent()
                hasRequest1Event =
                    hasRequest1Event ||
                        event.requestSequence?.captureRequestList?.contains(request1) ?: false
                hasRequest2Event =
                    hasRequest2Event ||
                        event.requestSequence?.captureRequestList?.contains(request2) ?: false
            }
        }
            .join()
    }

    @Test
    fun graphProcessorAbortsQueuedRequests() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        graphProcessor.startRepeating(request1)
        graphProcessor.submit(request2)

        // Abort queued and in-flight requests.
        graphProcessor.abort()
        graphProcessor.onGraphStarted(graphRequestProcessor1)

        val abortEvent1 =
            withTimeoutOrNull(timeMillis = 50L) { requestListener1.onAbortedFlow.firstOrNull() }
        val abortEvent2 = requestListener2.onAbortedFlow.first()
        val globalAbortEvent = globalListener.onAbortedFlow.first()

        assertThat(abortEvent1).isNull()
        assertThat(abortEvent2.request).isSameInstanceAs(request2)
        assertThat(globalAbortEvent.request).isSameInstanceAs(request2)

        val nextSequence = fakeProcessor1.nextRequestSequence()
        assertThat(nextSequence.captureRequestList.first()).isSameInstanceAs(request1)
        assertThat(nextSequence.requestMetadata[request1]!!.repeating).isTrue()
    }

    @Test
    fun closingGraphProcessorAbortsSubsequentRequests() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )
        graphProcessor.close()

        // Abort queued and in-flight requests.
        graphProcessor.onGraphStarted(graphRequestProcessor1)
        graphProcessor.startRepeating(request1)
        graphProcessor.submit(request2)

        val abortEvent1 =
            withTimeoutOrNull(timeMillis = 50L) { requestListener1.onAbortedFlow.firstOrNull() }
        val abortEvent2 = requestListener2.onAbortedFlow.first()
        assertThat(abortEvent1).isNull()
        assertThat(abortEvent2.request).isSameInstanceAs(request2)

        assertThat(fakeProcessor1.nextEvent().close).isTrue()
    }

    @Test
    fun graphProcessorResubmitsParametersAfterGraphStarts() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        val result = async {
            graphProcessor.trySubmit(mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AE_LOCK to false))
        }
        advanceUntilIdle()

        graphProcessor.onGraphStarted(graphRequestProcessor1)
        graphProcessor.startRepeating(request1)
        advanceUntilIdle()

        assertThat(result.await()).isTrue()
    }

    @Test
    fun graphProcessorSubmitsLatestParametersWhenSubmittedTwiceBeforeGraphStarts() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

        val result1 = async {
            graphProcessor.trySubmit(mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AE_LOCK to false))
        }
        advanceUntilIdle()
        val result2 = async {
            graphProcessor.trySubmit(mapOf<CaptureRequest.Key<*>, Any>(CONTROL_AE_LOCK to true))
        }
        advanceUntilIdle()

        graphProcessor.onGraphStarted(graphRequestProcessor1)
        advanceUntilIdle()

        graphProcessor.startRepeating(request1)
        advanceUntilIdle()

        val event1 = fakeProcessor1.nextEvent()
        assertThat(event1.requestSequence?.repeating).isTrue()
        val event2 = fakeProcessor1.nextEvent()
        assertThat(event2.requestSequence?.repeating).isFalse()
        assertThat(
            event2.requestSequence?.requestMetadata?.get(request1)?.get(CONTROL_AE_LOCK)
        ).isTrue()

        assertThat(result1.await()).isFalse()
        assertThat(result2.await()).isTrue()
    }

    @Test
    fun graphProcessorChangesGraphStateOnError() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )
        assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)

        graphProcessor.onGraphStarted(graphRequestProcessor1)
        graphProcessor.onGraphError(
            GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)
        )
        assertThat(graphProcessor.graphState.value).isInstanceOf(GraphStateError::class.java)
    }

    @Test
    fun graphProcessorDropsStaleErrors() = runTest {
        val graphProcessor =
            GraphProcessorImpl(
                FakeThreads.fromTestScope(this),
                FakeGraphConfigs.graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )
        assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)

        graphProcessor.onGraphError(
            GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)
        )
        assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)

        graphProcessor.onGraphStarting()
        graphProcessor.onGraphStarted(graphRequestProcessor1)

        // GraphProcessor should drop errors while the camera graph is stopping.
        graphProcessor.onGraphStopping()
        graphProcessor.onGraphError(
            GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)
        )
        assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)

        // GraphProcessor should also drop errors while the camera graph is stopped.
        graphProcessor.onGraphStopped(graphRequestProcessor1)
        graphProcessor.onGraphError(
            GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)
        )
        assertThat(graphProcessor.graphState.value).isEqualTo(GraphStateStopped)
    }
}
