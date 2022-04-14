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

import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeRequestListener
import androidx.camera.camera2.pipe.testing.FakeRequestProcessor
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.awaitEvent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class GraphProcessorTest {
    private val globalListener = FakeRequestListener()

    private val graphState3A = GraphState3A()
    private val fakeProcessor1 = FakeRequestProcessor()
    private val fakeProcessor2 = FakeRequestProcessor()

    private val requestListener1 = FakeRequestListener()
    private val request1 = Request(listOf(StreamId(0)), listeners = listOf(requestListener1))

    private val requestListener2 = FakeRequestListener()
    private val request2 = Request(listOf(StreamId(0)), listeners = listOf(requestListener2))

    private val graphConfig = CameraGraph.Config(
        camera = CameraId.fromCamera2Id("CameraId-Test"),
        streams = listOf(),
        requiredParameters = mapOf(
            CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
        )
    )

    @Test
    fun graphProcessorSubmitsRequests() {
        runBlocking(Dispatchers.Default) {

            // The graph processor uses 'launch' within the coroutine scope to invoke updates on the
            // requestProcessor instance. runBlocking forces all jobs to complete before testing the
            // state of results.
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )
            graphProcessor.onGraphStarted(fakeProcessor1)
            graphProcessor.submit(request1)

            // Make sure the requests get submitted to the request processor
            val event = fakeProcessor1.nextEvent()
            assertThat(event.requestSequence!!.requests).containsExactly(request1)
            assertThat(event.requestSequence!!.requiredParameters).containsEntry(
                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }

    @Test
    fun graphProcessorSubmitsRequestsToMostRecentProcessor() {
        // The graph processor uses 'launch' within the coroutine scope to invoke updates on the
        // requestProcessor instance. runBlocking forces all jobs to complete before testing the
        // state of results.
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.onGraphStarted(fakeProcessor1)
            graphProcessor.onGraphStarted(fakeProcessor2)
            graphProcessor.submit(request1)

            val event1 = fakeProcessor1.nextEvent()
            assertThat(event1.close).isTrue()

            val event2 = fakeProcessor2.nextEvent()
            assertThat(event2.submit).isTrue()
            assertThat(event2.requestSequence!!.requests).containsExactly(request1)
        }
    }

    @Test
    fun graphProcessorSubmitsQueuedRequests() {
        // The graph processor uses 'launch' within the coroutine scope to invoke updates on the
        // requestProcessor instance. runBlocking forces all jobs to complete before testing the
        // state of results.
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.submit(request1)
            graphProcessor.submit(request2)

            // Request1 and 2 should be queued and will be submitted even when the request
            // processor is set after the requests are submitted.
            graphProcessor.onGraphStarted(fakeProcessor1)

            val event1 = fakeProcessor1.awaitEvent(request = request1) { it.submit }
            assertThat(event1.requestSequence!!.requests).hasSize(1)
            assertThat(event1.requestSequence!!.requests).contains(request1)

            val event2 = fakeProcessor1.nextEvent()
            assertThat(event2.requestSequence!!.requests).hasSize(1)
            assertThat(event2.requestSequence!!.requests).contains(request2)
        }
    }

    @Test
    fun graphProcessorSubmitsBurstsOfRequestsTogetherWithExtras() {
        // The graph processor uses 'launch' within the coroutine scope to invoke updates on the
        // requestProcessor instance. runBlocking forces all jobs to complete before testing the
        // state of results.
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.submit(listOf(request1, request2))
            graphProcessor.onGraphStarted(fakeProcessor1)
            val event = fakeProcessor1.awaitEvent(request = request1) { it.submit }
            assertThat(event.requestSequence!!.requests).hasSize(2)
            assertThat(event.requestSequence!!.requests).contains(request1)
            assertThat(event.requestSequence!!.requests).contains(request2)
        }
    }

    @Test
    fun graphProcessorDoesNotForgetRejectedRequests() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            fakeProcessor1.rejectRequests = true
            graphProcessor.onGraphStarted(fakeProcessor1)

            graphProcessor.submit(request1)
            val event1 = fakeProcessor1.nextEvent()
            assertThat(event1.rejected).isTrue()
            assertThat(event1.requestSequence!!.requests[0]).isSameInstanceAs(request1)

            graphProcessor.submit(request2)
            val event2 = fakeProcessor1.nextEvent()
            assertThat(event2.rejected).isTrue()
            assertThat(event2.requestSequence!!.requests[0]).isSameInstanceAs(request1)

            graphProcessor.onGraphStarted(fakeProcessor2)
            assertThat(fakeProcessor2.nextEvent().requestSequence!!.requests[0]).isSameInstanceAs(
                request1
            )
            assertThat(fakeProcessor2.nextEvent().requestSequence!!.requests[0]).isSameInstanceAs(
                request2
            )
        }
    }

    @Test
    fun graphProcessorContinuesSubmittingRequestsWhenFirstRequestIsRejected() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            // Note: setting the requestProcessor, and calling submit() can both trigger a call
            // to submit a request.
            fakeProcessor1.rejectRequests = true
            graphProcessor.onGraphStarted(fakeProcessor1)
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
            assertThat(event3.requestSequence!!.requests).contains(request2)
            assertThat(event3.submit).isTrue()
            assertThat(event3.rejected).isFalse()
        }
    }

    @Test
    fun graphProcessorSetsRepeatingRequest() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.onGraphStarted(fakeProcessor1)
            graphProcessor.startRepeating(request1)
            graphProcessor.startRepeating(request2)
            val event = fakeProcessor1.awaitEvent(request = request2) { it.startRepeating }
            assertThat(event.requestSequence!!.requiredParameters).containsEntry(
                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }

    @Test
    fun graphProcessorTracksRepeatingRequest() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.onGraphStarted(fakeProcessor1)
            graphProcessor.startRepeating(request1)
            fakeProcessor1.awaitEvent(request = request1) { it.startRepeating }

            graphProcessor.onGraphStarted(fakeProcessor2)
            fakeProcessor2.awaitEvent(request = request1) { it.startRepeating }
        }
    }

    @Test
    fun graphProcessorTracksRejectedRepeatingRequests() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            fakeProcessor1.rejectRequests = true
            graphProcessor.onGraphStarted(fakeProcessor1)
            graphProcessor.startRepeating(request1)
            fakeProcessor1.awaitEvent(request = request1) { it.rejected }

            graphProcessor.onGraphStarted(fakeProcessor2)
            fakeProcessor2.awaitEvent(request = request1) { it.startRepeating }
        }
    }

    @Test
    fun graphProcessorSubmitsRepeatingRequestAndQueuedRequests() {
        runTest(UnconfinedTestDispatcher()) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.startRepeating(request1)
            graphProcessor.submit(request2)
            delay(50)

            graphProcessor.onGraphStarted(fakeProcessor1)

            var hasRequest1Event = false
            var hasRequest2Event = false

            // Loop until we see at least one repeating request, and one submit event.
            launch {
                while (!hasRequest1Event && !hasRequest2Event) {
                    val event = fakeProcessor1.nextEvent()
                    hasRequest1Event = hasRequest1Event ||
                        event.requestSequence?.requests?.contains(request1) ?: false
                    hasRequest2Event = hasRequest2Event ||
                        event.requestSequence?.requests?.contains(request2) ?: false
                }
            }.join()
        }
    }

    @Test
    fun graphProcessorAbortsQueuedRequests() {
        runTest(UnconfinedTestDispatcher()) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.startRepeating(request1)
            graphProcessor.submit(request2)

            // Abort queued and in-flight requests.
            graphProcessor.abort()
            graphProcessor.onGraphStarted(fakeProcessor1)

            val abortEvent1 = withTimeoutOrNull(timeMillis = 50L) {
                requestListener1.onAbortedFlow.firstOrNull()
            }
            val abortEvent2 = requestListener2.onAbortedFlow.first()
            val globalAbortEvent = globalListener.onAbortedFlow.first()

            assertThat(abortEvent1).isNull()
            assertThat(abortEvent2.request).isSameInstanceAs(request2)
            assertThat(globalAbortEvent.request).isSameInstanceAs(request2)

            val nextSequence = fakeProcessor1.nextRequestSequence()
            assertThat(nextSequence.requests.first()).isSameInstanceAs(request1)
            assertThat(nextSequence.requestMetadata[request1]!!.repeating).isTrue()
        }
    }

    @Test
    fun closingGraphProcessorAbortsSubsequentRequests() {
        runTest(UnconfinedTestDispatcher()) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                graphConfig,
                graphState3A,
                this,
                arrayListOf(globalListener)
            )
            graphProcessor.close()

            // Abort queued and in-flight requests.
            graphProcessor.onGraphStarted(fakeProcessor1)
            graphProcessor.startRepeating(request1)
            graphProcessor.submit(request2)

            val abortEvent1 = withTimeoutOrNull(timeMillis = 50L) {
                requestListener1.onAbortedFlow.firstOrNull()
            }
            val abortEvent2 = requestListener2.onAbortedFlow.first()
            assertThat(abortEvent1).isNull()
            assertThat(abortEvent2.request).isSameInstanceAs(request2)

            assertThat(fakeProcessor1.nextEvent().close).isTrue()
        }
    }
}