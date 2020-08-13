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
import androidx.camera.camera2.pipe.impl.Log.warn
import kotlinx.coroutines.CoroutineDispatcher
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
    var requestProcessor: RequestProcessor?

    /**
     * This method puts the [GraphProcessor] into a started state. Starting the [GraphProcessor]
     * will cause it to attempt to submit all requests to the current [RequestProcessor] instance,
     * and any subsequent requests will be immediately submitted to the current [RequestProcessor].
     */
    fun start()

    /**
     * This method puts the [GraphProcessor] into a stopped state and clears the current
     * [RequestProcessor] instance. While the graph processor is stopped, all requests are
     * buffered.
     */
    fun stop()

    fun setRepeating(request: Request)
    fun submit(request: Request)
    fun submit(requests: List<Request>)

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
}

/**
 * The graph processor handles *cross-session* state, such as the most recent repeating request.
 */
@CameraGraphScope
class GraphProcessorImpl @Inject constructor(
    @ForCameraGraph private val graphScope: CoroutineScope,
    @ForCameraGraph private val graphDispatcher: CoroutineDispatcher,
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

    @GuardedBy("lock")
    private var active = false

    override var requestProcessor: RequestProcessor?
        get() = synchronized(lock) {
            _requestProcessor
        }
        set(value) {
            val processorToClose: RequestProcessor?
            val processorToDisconnect: RequestProcessor?
            synchronized(lock) {
                processorToDisconnect = _requestProcessor
                if (closed) {
                    processorToClose = value
                } else {
                    processorToClose = null
                    _requestProcessor = value
                }
            }

            if (value === processorToDisconnect) {
                warn { "RequestProcessor was set more than once." }
                return
            }

            // Setting the request processor to null will disconnect the old processor.
            if (processorToDisconnect != null) {
                synchronized(processorToDisconnect) {
                    processorToDisconnect.disconnect()
                }
            }

            if (processorToClose != null) {
                synchronized(processorToClose) {
                    processorToClose.stop()
                }
                return
            }

            if (value != null) {
                graphScope.launch {
                    trySetRepeating()
                    submitLoop()
                }
            }
        }

    override fun start() {
        synchronized(lock) {
            active = true
        }

        Log.debug { "Starting GraphProcessor" }
        // TODO: Start the camera and configure the capture session.
    }

    /**
     * This method puts the [GraphProcessorImpl] into a stopped state. While the graph processor is
     * in this state, all requests are buffered in the RequestQueue.
     */
    override fun stop() {
        val processor = synchronized(lock) {
            active = false
            _requestProcessor.also { _requestProcessor = null }
        }

        Log.debug { "Stopping GraphProcessor" }

        if (processor == null) {
            return
        }

        // There are about ~3 main ways a Camera2 CameraCaptureSession can be shut down and closed,
        // and the behavior will be different depending on the circumstances.
        //
        // A session can be replaced by another session by simply calling createCaptureSession on
        // the CameraDevice. Internally this will reconfigure the camera capture session, and there
        // are optimizations present in the CameraFramework and Camera HAL that can optimize how
        // fast the new session is created and started. The most obvious example of this is
        // replacing a surface with a new one after recording a video, which can effectively cause
        // the new session to be created and replaced without dropping a frame.
        //
        // Second, a session can be _stopped_ by calling stopRepeating and/or abortCaptures. This
        // keeps the session alive but may abort pending requests. In some cases it's faster to
        // switch sessions if these methods are invoked before creating a new session on the
        // device because requests that are in-flight can be explicitly aborted.
        //
        // Finally, a session may be closed as a result of the underlying CameraDevice being closed
        // or disconnected. This can happen if a higher priority process steals the camera, or
        // during switches from one camera to another.

        graphScope.launch {
            processor.stop()
        }
    }

    override fun setRepeating(request: Request) {
        synchronized(lock) {
            if (closed) return
            nextRepeatingRequest = request
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
    suspend fun submit(parameters: Map<CaptureRequest.Key<*>, Any>): Boolean =
        withContext(graphDispatcher) {
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
            // Start with requests that have already been submitted
            if (processor != null) {
                synchronized(processor) {
                    processor.abort()
                }
            }

            // Then abort requests that have not been submitted
            for (burst in requests) {
                abortBurst(burst)
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
        }

        abort()
        stop()
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
            if (closed || !active) return

            processor = _requestProcessor
            request = nextRepeatingRequest ?: currentRepeatingRequest
        }

        if (processor != null && request != null) {
            val extras: Map<CaptureRequest.Key<*>, Any> = read3AState()

            synchronized(processor) {
                if (processor.setRepeating(request, extras, requireSurfacesForAllStreams = true)) {

                    // ONLY update the current repeating request if the update succeeds
                    synchronized(lock) {
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
    }

    private fun submitLoop() {
        var burst: List<Request>
        var processor: RequestProcessor

        synchronized(lock) {
            if (closed || !active) return

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
                        // If we did not submit, and we are also not dirty, then exit the loop
                        submitting = false
                        return
                    } else {
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
