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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.CaptureRequest
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.RequestProcessor
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

/**
 * Fake implementation of a [RequestProcessor] that passes events to a [Channel].
 *
 * This allows kotlin tests to check sequences of interactions that dispatch in the background
 * without blocking between events.
 */
public class FakeRequestProcessor(
    private val streamToSurfaceMap: Map<StreamId, Surface> = emptyMap(),
    private val defaultTemplate: RequestTemplate = RequestTemplate(1)
) : RequestProcessor {
    private val lock = Any()
    private val eventChannel = Channel<Event>(Channel.UNLIMITED)
    private val requestCounter = atomic(0L)

    @GuardedBy("lock")
    private var pendingSequence: CompletableDeferred<RequestSequence>? = null

    @GuardedBy("lock")
    private val requestSequenceQueue: MutableList<RequestSequence> = mutableListOf()

    @GuardedBy("lock")
    private var repeatingRequestSequence: RequestSequence? = null

    @GuardedBy("lock")
    private var _rejectRequests = false

    var rejectRequests: Boolean
        get() = synchronized(lock) {
            _rejectRequests
        }
        set(value) {
            synchronized(lock) {
                _rejectRequests = value
            }
        }

    override fun submit(
        request: Request,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        defaultListeners: List<Request.Listener>
    ): Boolean = submit(listOf(request), defaultParameters, requiredParameters, defaultListeners)

    override fun submit(
        requests: List<Request>,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        defaultListeners: List<Request.Listener>
    ): Boolean {
        val requestSequence =
            createRequestSequence(
                repeating = false,
                requests,
                defaultParameters,
                requiredParameters,
                defaultListeners
            )
        if (rejectRequests) {
            check(
                eventChannel
                    .trySend(Event(requestSequence = requestSequence, rejected = true))
                    .isSuccess
            )
            return false
        }

        val signal = synchronized(lock) {
            requestSequenceQueue.add(requestSequence)
            pendingSequence?.also {
                pendingSequence = null
            }
        }
        requestSequence.invokeOnSequenceCreated()
        requestSequence.invokeOnSequenceSubmitted()
        signal?.complete(requestSequence)

        check(
            eventChannel
                .trySend(Event(requestSequence = requestSequence, submit = true))
                .isSuccess
        )

        return true
    }

    override fun startRepeating(
        request: Request,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        defaultListeners: List<Request.Listener>
    ): Boolean {
        val requestSequence =
            createRequestSequence(
                repeating = true,
                listOf(request),
                defaultParameters,
                requiredParameters,
                defaultListeners
            )
        if (rejectRequests) {
            check(
                eventChannel
                    .trySend(Event(requestSequence = requestSequence, rejected = true))
                    .isSuccess
            )
            return false
        }

        val signal = synchronized(lock) {
            repeatingRequestSequence = requestSequence
            pendingSequence?.also {
                pendingSequence = null
            }
        }
        requestSequence.invokeOnSequenceCreated()
        requestSequence.invokeOnSequenceSubmitted()
        signal?.complete(requestSequence)

        check(
            eventChannel
                .trySend(Event(requestSequence = requestSequence, startRepeating = true))
                .isSuccess
        )
        return true
    }

    override fun abortCaptures() {
        val requestSequencesToAbort: List<RequestSequence>
        synchronized(lock) {
            requestSequencesToAbort = requestSequenceQueue.toList()
            requestSequenceQueue.clear()
        }
        for (sequence in requestSequencesToAbort) {
            sequence.invokeOnSequenceAborted()
        }
        check(eventChannel.trySend(Event(abort = true)).isSuccess)
    }

    override fun stopRepeating() {
        val requestSequence = synchronized(lock) {
            repeatingRequestSequence.also {
                repeatingRequestSequence = null
            }
        }
        requestSequence?.invokeOnSequenceAborted()
        check(eventChannel.trySend(Event(stop = true)).isSuccess)
    }

    override fun close() {
        synchronized(lock) {
            rejectRequests = true
        }
        check(eventChannel.trySend(Event(close = true)).isSuccess)
    }

    /**
     * Get the next event from queue with an option to specify a timeout for tests.
     */
    suspend fun nextEvent(timeMillis: Long = 500): Event = withTimeout(timeMillis) {
        eventChannel.receive()
    }

    suspend fun nextRequestSequence(): RequestSequence {

        while (true) {
            val pending: Deferred<RequestSequence>
            synchronized(lock) {
                var sequence = requestSequenceQueue.removeFirstOrNull()
                if (sequence == null) {
                    sequence = repeatingRequestSequence
                }
                if (sequence != null) {
                    return sequence
                }

                if (pendingSequence == null) {
                    pendingSequence = CompletableDeferred()
                }
                pending = pendingSequence!!
            }

            pending.await()
        }
    }

    private fun createRequestSequence(
        repeating: Boolean,
        requests: List<Request>,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        defaultListeners: List<Request.Listener>,
    ): RequestSequence {
        val requestInfoMap = mutableMapOf<Request, RequestMetadata>()
        val requestListenerMap = mutableMapOf<Request, List<Request.Listener>>()
        for (request in requests) {
            val captureParameters = mutableMapOf<CaptureRequest.Key<*>, Any?>()
            val metadataParameters = mutableMapOf<Metadata.Key<*>, Any?>()
            for ((k, v) in defaultParameters) {
                if (k != null) {
                    if (k is CaptureRequest.Key<*>) {
                        captureParameters[k] = v
                    } else if (k is Metadata.Key<*>) {
                        metadataParameters[k] = v
                    }
                }
            }
            for ((k, v) in request.parameters) {
                captureParameters[k] = v
            }
            for ((k, v) in requiredParameters) {
                if (k != null) {
                    if (k is CaptureRequest.Key<*>) {
                        captureParameters[k] = v
                    } else if (k is Metadata.Key<*>) {
                        metadataParameters[k] = v
                    }
                }
            }
            val listeners = mutableListOf<Request.Listener>()
            listeners.addAll(defaultListeners)
            listeners.addAll(request.listeners)

            val requestNumber = RequestNumber(requestCounter.incrementAndGet())

            val requestMetadata = FakeRequestMetadata(
                request = request,
                requestParameters = captureParameters,
                metadata = metadataParameters,
                template = request.template ?: defaultTemplate,
                streams = streamToSurfaceMap,
                repeating = repeating,
                requestNumber = requestNumber
            )
            requestInfoMap[request] = requestMetadata
            requestListenerMap[request] = listeners
        }

        // Copy maps / lists for tests.
        return RequestSequence(
            requests = requests.toList(),
            defaultParameters = defaultParameters.toMap(),
            requiredParameters = requiredParameters.toMap(),
            defaultListeners = defaultListeners.toList(),
            requestMetadata = requestInfoMap,
            requestListeners = requestListenerMap
        )
    }

    fun reset() {
        synchronized(lock) {
            requestSequenceQueue.clear()
            repeatingRequestSequence = null
            _rejectRequests = false
        }
    }

    data class RequestSequence(
        val requests: List<Request>,
        val defaultParameters: Map<*, Any?>,
        val requiredParameters: Map<*, Any?>,
        val defaultListeners: List<Request.Listener>,
        val requestMetadata: Map<Request, RequestMetadata>,
        val requestListeners: Map<Request, List<Request.Listener>>
    ) {
        fun invokeOnSequenceCreated() {
            for (request in requests) {
                for (listener in requestListeners[request]!!) {
                    listener.onRequestSequenceCreated(requestMetadata[request]!!)
                }
            }
        }

        fun invokeOnSequenceSubmitted() {
            for (request in requests) {
                for (listener in requestListeners[request]!!) {
                    listener.onRequestSequenceSubmitted(requestMetadata[request]!!)
                }
            }
        }

        fun invokeOnSequenceAborted() {
            for (request in requests) {
                for (listener in requestListeners[request]!!) {
                    listener.onRequestSequenceAborted(requestMetadata[request]!!)
                }
            }
        }

        fun invokeOnSequenceCompleted(frameNumber: FrameNumber) {
            for (request in requests) {
                for (listener in requestListeners[request]!!) {
                    listener.onRequestSequenceCompleted(requestMetadata[request]!!, frameNumber)
                }
            }
        }
    }

    /**
     * TODO: It's probably better to model this as a sealed class.
     */
    data class Event(
        val requestSequence: RequestSequence? = null,
        val rejected: Boolean = false,
        val abort: Boolean = false,
        val close: Boolean = false,
        val stop: Boolean = false,
        val submit: Boolean = false,
        val startRepeating: Boolean = false
    )
}

suspend fun FakeRequestProcessor.awaitEvent(
    request: Request? = null,
    filter: (event: FakeRequestProcessor.Event) -> Boolean
): FakeRequestProcessor.Event {

    var event: FakeRequestProcessor.Event
    var loopCount = 0
    while (loopCount < 10) {
        loopCount++
        event = this.nextEvent()

        if (request != null) {
            val contains = event.requestSequence?.requests?.contains(request) ?: false
            if (filter(event) && contains) {
                return event
            }
        } else if (filter(event)) {
            return event
        }
    }

    throw IllegalStateException("Failed to observe a submit event containing $request")
}