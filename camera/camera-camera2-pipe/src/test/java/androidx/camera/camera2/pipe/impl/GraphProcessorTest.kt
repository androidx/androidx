/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.impl

import android.os.Build
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.camera.camera2.pipe.testing.Event
import androidx.camera.camera2.pipe.testing.FakeRequestListener
import androidx.camera.camera2.pipe.testing.FakeRequestProcessor
import androidx.camera.camera2.pipe.testing.FakeThreads
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(CameraPipeRobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class GraphProcessorTest {
    private val globalListener = FakeRequestListener()

    private val fakeProcessor1 = FakeRequestProcessor()
    private val fakeProcessor2 = FakeRequestProcessor()

    private val requestListener1 = FakeRequestListener()
    private val request1 = Request(listOf(StreamId(0)), listeners = listOf(requestListener1))

    private val requestListener2 = FakeRequestListener()
    private val request2 = Request(listOf(StreamId(0)), listeners = listOf(requestListener2))

    @Test
    fun graphProcessorSubmitsRequests() {
        // The graph processor uses 'launch' within the coroutine scope to invoke updates on the
        // requestProcessor instance. runBlocking forces all jobs to complete before testing the
        // state of results.
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )
            graphProcessor.attach(fakeProcessor1)
            graphProcessor.submit(request1)
        }

        // Make sure the requests get submitted to the request processor
        assertThat(fakeProcessor1.requestQueue).hasSize(1)
        assertThat(fakeProcessor1.requestQueue.first().burst).hasSize(1)
        assertThat(fakeProcessor1.requestQueue.first().burst.first()).isSameInstanceAs(request1)
    }

    @Test
    fun graphProcessorSubmitsRequestsToMostRecentProcessor() {
        // The graph processor uses 'launch' within the coroutine scope to invoke updates on the
        // requestProcessor instance. runBlocking forces all jobs to complete before testing the
        // state of results.
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.attach(fakeProcessor1)
            graphProcessor.attach(fakeProcessor2)
            graphProcessor.submit(request1)
        }

        // requestProcessor1 does not receive requests
        assertThat(fakeProcessor1.requestQueue).hasSize(0)

        // requestProcessor2 receives requests
        assertThat(fakeProcessor2.requestQueue).hasSize(1)
        assertThat(fakeProcessor2.requestQueue.first().burst).hasSize(1)
        assertThat(fakeProcessor2.requestQueue.first().burst.first()).isSameInstanceAs(request1)
    }

    @Test
    fun graphProcessorSubmitsQueuedRequests() {
        // The graph processor uses 'launch' within the coroutine scope to invoke updates on the
        // requestProcessor instance. runBlocking forces all jobs to complete before testing the
        // state of results.
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.submit(request1)
            graphProcessor.submit(request2)

            // Request1 and 2 should be queued and will be submitted even when the request
            // processor is set after the requests are submitted.
            graphProcessor.attach(fakeProcessor1)
        }

        // Make sure the requests get submitted to the request processor
        assertThat(fakeProcessor1.requestQueue).hasSize(2)
        assertThat(fakeProcessor1.requestQueue[0].burst[0]).isSameInstanceAs(request1)
        assertThat(fakeProcessor1.requestQueue[1].burst[0]).isSameInstanceAs(request2)
    }

    @Test
    fun graphProcessorSubmitsBurstsOfRequestsTogetherWithExtras() {
        // The graph processor uses 'launch' within the coroutine scope to invoke updates on the
        // requestProcessor instance. runBlocking forces all jobs to complete before testing the
        // state of results.
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.submit(listOf(request1, request2))
            graphProcessor.attach(fakeProcessor1)
        }

        assertThat(fakeProcessor1.requestQueue).hasSize(1)
        assertThat(fakeProcessor1.requestQueue[0].burst[0]).isSameInstanceAs(request1)
        assertThat(fakeProcessor1.requestQueue[0].burst[1]).isSameInstanceAs(request2)
    }

    @Test
    fun graphProcessorDoesNotForgetRejectedRequests() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            fakeProcessor1.rejectRequests = true
            graphProcessor.attach(fakeProcessor1)

            graphProcessor.submit(request1)
            assertThat(fakeProcessor1.nextEvent().rejected).isTrue()

            graphProcessor.submit(request2)
            assertThat(fakeProcessor1.nextEvent().rejected).isTrue()

            graphProcessor.attach(fakeProcessor2)
            assertThat(fakeProcessor2.nextEvent().request!!.burst[0]).isSameInstanceAs(request1)
            assertThat(fakeProcessor2.nextEvent().request!!.burst[0]).isSameInstanceAs(request2)
        }

        assertThat(fakeProcessor1.requestQueue).hasSize(0)
        assertThat(fakeProcessor2.requestQueue).hasSize(2)
        assertThat(fakeProcessor2.requestQueue[0].burst[0]).isSameInstanceAs(request1)
        assertThat(fakeProcessor2.requestQueue[1].burst[0]).isSameInstanceAs(request2)
    }

    @Test
    fun graphProcessorContinuesSubmittingRequestsWhenFirstRequestIsRejected() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            // Note: setting the requestProcessor, and calling submit() can both trigger a call
            // to submit a request.
            fakeProcessor1.rejectRequests = true
            graphProcessor.attach(fakeProcessor1)
            graphProcessor.submit(request1)

            // Check to make sure that submit is called at least once, and that request1 is rejected
            // from the request processor.
            val event1 = fakeProcessor1.nextEvent()
            assertThat(event1.request!!.burst).contains(request1)
            assertThat(event1.rejected).isTrue()

            // Stop rejecting requests
            fakeProcessor1.rejectRequests = false
            assertThat(fakeProcessor1.rejectRequests).isFalse()
            assertThat(fakeProcessor1.closeInvoked).isFalse()
            assertThat(fakeProcessor1.stopInvoked).isFalse()

            graphProcessor.submit(request2)

            // Cycle events until we get a submitted event with request1
            val event2 = awaitEvent(fakeProcessor1, request1) { it.submit }
            assertThat(event2.rejected).isFalse()

            // Assert that immediately after we get a successfully submitted request, the
            //  next request is also submitted.
            val event3 = fakeProcessor1.nextEvent()
            assertThat(event3.request!!.burst).contains(request2)
            assertThat(event3.submit).isTrue()
            assertThat(event3.rejected).isFalse()
        }
    }

    @Test
    fun graphProcessorSetsRepeatingRequest() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.attach(fakeProcessor1)
            graphProcessor.setRepeating(request1)
            graphProcessor.setRepeating(request2)
        }

        assertThat(fakeProcessor1.repeatingRequest?.burst).contains(request2)
    }

    @Test
    fun graphProcessorTracksRepeatingRequest() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.attach(fakeProcessor1)
            graphProcessor.setRepeating(request1)
            awaitEvent(fakeProcessor1, request1) { it.setRepeating }

            graphProcessor.attach(fakeProcessor2)
            awaitEvent(fakeProcessor2, request1) { it.setRepeating }
        }

        assertThat(fakeProcessor1.repeatingRequest?.burst).contains(request1)
        assertThat(fakeProcessor2.repeatingRequest?.burst).contains(request1)
    }

    @Test
    fun graphProcessorTracksRejectedRepeatingRequests() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            fakeProcessor1.rejectRequests = true
            graphProcessor.attach(fakeProcessor1)
            graphProcessor.setRepeating(request1)

            graphProcessor.attach(fakeProcessor2)
            awaitEvent(fakeProcessor2, request1) { it.setRepeating }
        }

        assertThat(fakeProcessor2.repeatingRequest?.burst).contains(request1)
    }

    @Test
    fun graphProcessorSubmitsRepeatingRequestAndQueuedRequests() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.setRepeating(request1)
            graphProcessor.submit(request2)

            graphProcessor.attach(fakeProcessor1)
        }

        assertThat(fakeProcessor1.repeatingRequest?.burst).contains(request1)
        assertThat(fakeProcessor1.requestQueue[0].burst[0]).isSameInstanceAs(request2)
    }

    @Test
    fun graphProcessorAbortsQueuedRequests() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )

            graphProcessor.setRepeating(request1)
            graphProcessor.submit(request2)

            // Abort queued and in-flight requests.
            graphProcessor.abort()
            graphProcessor.attach(fakeProcessor1)
        }

        assertThat(requestListener1.lastAbortedRequest).isNull()
        assertThat(requestListener2.lastAbortedRequest).isSameInstanceAs(request2)
        assertThat(globalListener.lastAbortedRequest).isSameInstanceAs(request2)

        assertThat(fakeProcessor1.repeatingRequest?.burst).contains(request1)
        assertThat(fakeProcessor1.requestQueue).isEmpty()
    }

    @Test
    fun closingGraphProcessorAbortsSubsequentRequests() {
        runBlocking(Dispatchers.Default) {
            val graphProcessor = GraphProcessorImpl(
                FakeThreads.forTests,
                this,
                arrayListOf(globalListener)
            )
            graphProcessor.close()

            // Abort queued and in-flight requests.
            graphProcessor.attach(fakeProcessor1)
            graphProcessor.setRepeating(request1)
            graphProcessor.submit(request2)
        }

        // The repeating request is not aborted
        assertThat(requestListener1.lastAbortedRequest).isNull()
        assertThat(requestListener2.lastAbortedRequest).isSameInstanceAs(request2)

        assertThat(fakeProcessor1.closeInvoked).isTrue()
        assertThat(fakeProcessor1.repeatingRequest).isNull()
        assertThat(fakeProcessor1.requestQueue).isEmpty()
    }

    private suspend fun awaitEvent(
        requestProcessor: FakeRequestProcessor,
        request: Request,
        filter: (event: Event) -> Boolean
    ): Event {

        var event: Event
        var loopCount = 0
        while (loopCount < 10) {
            loopCount++
            event = requestProcessor.nextEvent()
            val contains = event.request?.burst?.contains(request) ?: false
            if (filter(event) && contains) {
                return event
            }
        }

        throw IllegalStateException("Failed to observe a submit event containing $request")
    }
}