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

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.impl.RequestProcessor
import androidx.camera.camera2.pipe.impl.TokenLock
import androidx.camera.camera2.pipe.impl.TokenLockImpl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

/**
 * Fake implementation of a [RequestProcessor] for tests.
 */
class FakeRequestProcessor : RequestProcessor {
    private val eventChannel = Channel<Event>(Channel.UNLIMITED)

    val requestQueue: MutableList<FakeRequest> = mutableListOf()
    var repeatingRequest: FakeRequest? = null

    var rejectRequests = false
    var abortInvoked = false
        private set
    var disconnectInvoked = false
        private set
    var stopInvoked = false
        private set

    private val tokenLock = TokenLockImpl(1)
    private var token: TokenLock.Token? = null

    data class FakeRequest(
        val burst: List<Request>,
        val extraRequestParameters: Map<CaptureRequest.Key<*>, Any> = emptyMap(),
        val requireStreams: Boolean = false
    )

    override fun submit(
        request: Request,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean {
        val fakeRequest =
            FakeRequest(listOf(request), extraRequestParameters, requireSurfacesForAllStreams)

        if (rejectRequests || disconnectInvoked || stopInvoked) {
            eventChannel.offer(Event(request = fakeRequest, rejected = true))
            return false
        }

        requestQueue.add(fakeRequest)
        eventChannel.offer(Event(request = fakeRequest, submit = true))

        return true
    }

    override fun submit(
        requests: List<Request>,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean {
        val fakeRequest =
            FakeRequest(requests, extraRequestParameters, requireSurfacesForAllStreams)
        if (rejectRequests || disconnectInvoked || stopInvoked) {
            eventChannel.offer(Event(request = fakeRequest, rejected = true))
            return false
        }

        requestQueue.add(fakeRequest)
        eventChannel.offer(Event(request = fakeRequest, submit = true))

        return true
    }

    override fun setRepeating(
        request: Request,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean {
        val fakeRequest =
            FakeRequest(listOf(request), extraRequestParameters, requireSurfacesForAllStreams)
        if (rejectRequests || disconnectInvoked || stopInvoked) {
            eventChannel.offer(Event(request = fakeRequest, rejected = true))
            return false
        }

        repeatingRequest = fakeRequest
        eventChannel.offer(Event(request = fakeRequest, setRepeating = true))
        return true
    }

    override fun abort() {
        abortInvoked = true
        eventChannel.offer(Event(abort = true))
    }

    override fun disconnect() {
        disconnectInvoked = true
        eventChannel.offer(Event(disconnect = true))
    }

    override fun stop() {
        stopInvoked = true
        eventChannel.offer(Event(stop = true))
    }

    /**
     * Get the next event from queue with an option to specify a timeout for tests.
     */
    suspend fun nextEvent(timeMillis: Long = 25): Event = withTimeout(timeMillis) {
        eventChannel.receive()
    }
}

data class Event(
    val request: FakeRequestProcessor.FakeRequest? = null,
    val rejected: Boolean = false,
    val abort: Boolean = false,
    val disconnect: Boolean = false,
    val stop: Boolean = false,
    val submit: Boolean = false,
    val setRepeating: Boolean = false
)