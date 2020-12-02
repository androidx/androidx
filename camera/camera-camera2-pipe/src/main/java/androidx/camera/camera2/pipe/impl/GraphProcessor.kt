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

import android.hardware.camera2.CaptureRequest
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.formatForLogs
import androidx.camera.camera2.pipe.impl.Log.debug
import androidx.camera.camera2.pipe.impl.Log.warn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The [GraphProcessor] is responsible for queuing and submitting requests to a single
 * [RequestProcessor] instance, and for maintaining state across one or more [RequestProcessor]
 * instances.
 */
interface GraphProcessor {
    fun setRepeating(request: Request)
    fun submit(request: Request)
    fun submit(requests: List<Request>)
    suspend fun submit(parameters: Map<CaptureRequest.Key<*>, Any>): Boolean

    /**
     * Abort all submitted requests that have not yet been submitted to the [RequestProcessor] as
     * well as aborting requests on the [RequestProcessor] itself.
     */
    fun abort()

    /**
     * Closing the [GraphProcessor] will abort all queued requests. Any requests submitted after the
     * [GraphProcessor] is closed will be immediately aborted.
     */
    fun close()

    fun attach(requestProcessor: RequestProcessor)
    fun detach(requestProcessor: RequestProcessor)
    fun invalidate()
}

/**
 * The graph processor handles *cross-session* state, such as the most recent repeating request.
 */
@CameraGraphScope
class GraphProcessorImpl @Inject constructor(
    private val threads: Threads,
    @ForCameraGraph private val graphScope: CoroutineScope,
    @ForCameraGraph private val graphListeners: java.util.ArrayList<Request.Listener>
) : GraphProcessor {
    private val lock = Any()

    @GuardedBy("lock")
    private val requestQueue: MutableList<List<Request>> = ArrayList()

    @GuardedBy("lock")
    private var currentRepeatingRequest: Request? = null

    @GuardedBy("lock")
    private var nextRepeatingRequest: Request? = null

    @GuardedBy("lock")
    private var _requestProcessor: RequestProcessor? = null

    @GuardedBy("lock")
    private var submitting = false

    @GuardedBy("lock")
    private var dirty = false

    @GuardedBy("lock")
    private var closed = false

    override fun attach(requestProcessor: RequestProcessor) {
        var oldRequestProcessor: RequestProcessor? = null
        synchronized(lock) {
            if (closed) {
                requestProcessor.close()
                return
            }

            if (_requestProcessor != null && _requestProcessor !== requestProcessor) {
                oldRequestProcessor = _requestProcessor
            }
            _requestProcessor = requestProcessor
        }

        val processorToClose = oldRequestProcessor
        if (processorToClose != null) {
            synchronized(processorToClose) {
                processorToClose.close()
            }
        }

        resubmit()
    }

    override fun detach(requestProcessor: RequestProcessor) {
        var oldRequestProcessor: RequestProcessor? = null
        synchronized(lock) {
            if (closed) {
                return
            }

            if (requestProcessor === _requestProcessor) {
                oldRequestProcessor = _requestProcessor
                _requestProcessor = null
            } else {
                warn {
                    "Refusing to detach $requestProcessor. " +
                        "It is different from $_requestProcessor"
                }
            }
        }

        val processorToClose = oldRequestProcessor
        if (processorToClose != null) {
            synchronized(processorToClose) {
                processorToClose.close()
            }
        }
    }

    override fun invalidate() {
        resubmit()
    }

    override fun setRepeating(request: Request) {
        synchronized(lock) {
            if (closed) return
            nextRepeatingRequest = request
            debug { "Set repeating request to ${request.formatForLogs()}" }
        }

        graphScope.launch {
            trySetRepeating()
        }
    }

    override fun submit(request: Request) {
        submit(listOf(request))
    }

    override fun submit(requests: List<Request>) {
        synchronized(lock) {
            if (closed) {
                graphScope.launch {
                    abortBurst(requests)
                }
                return
            }
            requestQueue.add(requests)
        }

        graphScope.launch {
            submitLoop()
        }
    }

    /**
     * Submit a request to the camera using only the current repeating request.
     */
    override suspend fun submit(parameters: Map<CaptureRequest.Key<*>, Any>): Boolean =
        withContext(threads.ioDispatcher) {
            val processor: RequestProcessor?
            val request: Request?

            synchronized(lock) {
                if (closed) return@withContext false
                processor = _requestProcessor
                request = currentRepeatingRequest
            }

            return@withContext when {
                processor == null || request == null -> false
                else -> processor.submit(
                    request,
                    parameters,
                    requireSurfacesForAllStreams = false
                )
            }
        }

    override fun abort() {
        val processor: RequestProcessor?
        val requests: List<List<Request>>

        synchronized(lock) {
            processor = _requestProcessor
            requests = requestQueue.toList()
            requestQueue.clear()
        }

        graphScope.launch {
            Debug.traceStart { "$this#abort" }
            // Start with requests that have already been submitted
            if (processor != null) {
                synchronized(processor) {
                    processor.abortCaptures()
                }
            }

            // Then abort requests that have not been submitted
            for (burst in requests) {
                abortBurst(burst)
            }
            Debug.traceStop()
        }
    }

    override fun close() {
        val processor: RequestProcessor?
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            processor = _requestProcessor
            _requestProcessor = null
        }

        processor?.close()
        abort()
    }

    private fun resubmit() {
        graphScope.launch {
            trySetRepeating()
            submitLoop()
        }
    }

    private fun read3AState(): Map<CaptureRequest.Key<*>, Any> {
        // TODO: Build extras from 3A state
        return mapOf()
    }

    private fun abortBurst(requests: List<Request>) {
        for (request in requests) {
            abortRequest(request)
        }
    }

    private fun abortRequest(request: Request) {
        for (listenerIdx in graphListeners.indices) {
            graphListeners[listenerIdx].onAborted(request)
        }

        for (listenerIdx in request.listeners.indices) {
            request.listeners[listenerIdx].onAborted(request)
        }
    }

    private fun trySetRepeating() {
        val processor: RequestProcessor?
        val request: Request?

        synchronized(lock) {
            if (closed) return

            processor = _requestProcessor
            request = nextRepeatingRequest ?: currentRepeatingRequest
        }

        if (processor != null && request != null) {

            Debug.traceStart { "$this#setRepeating" }
            val extras: Map<CaptureRequest.Key<*>, Any> = read3AState()

            synchronized(processor) {
                if (processor.setRepeating(request, extras, requireSurfacesForAllStreams = true)) {
                    // ONLY update the current repeating request if the update succeeds
                    synchronized(lock) {
                        if (processor === _requestProcessor) {
                            currentRepeatingRequest = request

                            // There is a race condition where the nextRepeating request might be changed
                            // while trying to update the current repeating request. If this happens, do no
                            // overwrite the pending request.
                            if (nextRepeatingRequest == request) {
                                nextRepeatingRequest = null
                            }
                        }
                    }
                }
            }
            Debug.traceStop()
        }
    }

    private fun submitLoop() {
        var burst: List<Request>
        var processor: RequestProcessor

        synchronized(lock) {
            if (closed) return

            if (submitting) {
                dirty = true
                return
            }

            val nullableProcessor = _requestProcessor
            val nullableBurst = requestQueue.firstOrNull()
            if (nullableProcessor == null || nullableBurst == null) {
                return
            }

            processor = nullableProcessor
            burst = nullableBurst

            submitting = true
        }

        while (true) {
            var submitted = false
            Debug.traceStart { "$this#submit" }
            try {
                val extras: Map<CaptureRequest.Key<*>, Any> = read3AState()
                submitted = synchronized(processor) {
                    if (burst.size == 1) {
                        processor.submit(burst[0], extras, true)
                    } else {
                        processor.submit(burst, extras, true)
                    }
                }
            } finally {
                Debug.traceStop()
                synchronized(lock) {
                    if (submitted) {
                        check(requestQueue.removeAt(0) === burst)

                        val nullableBurst = requestQueue.firstOrNull()
                        if (nullableBurst == null) {
                            dirty = false
                            submitting = false
                            return
                        }

                        burst = nullableBurst
                    } else if (!dirty) {
                        debug { "Failed to submit $burst, and the queue is not dirty." }
                        // If we did not submit, and we are also not dirty, then exit the loop
                        submitting = false
                        return
                    } else {
                        debug {
                            "Failed to submit $burst but the request queue or processor is " +
                                "dirty. Clearing dirty flag and attempting retry."
                        }
                        dirty = false

                        // One possible situation is that the _requestProcessor was replaced or
                        // set to null. If this happens, try to update the requestProcessor we
                        // are currently using. If the current request processor is null, then
                        // we cannot submit anyways.
                        val nullableProcessor = _requestProcessor
                        if (nullableProcessor != null) {
                            processor = nullableProcessor
                        }
                    }
                }
            }
        }
    }
}
