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

package androidx.camera.camera2.pipe.graph

import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestProcessor
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.formatForLogs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The [GraphProcessor] is responsible for queuing and submitting requests to a single
 * [RequestProcessor] instance, and for maintaining state across one or more [RequestProcessor]
 * instances.
 */
internal interface GraphProcessor : GraphListener {
    fun submit(request: Request)
    fun submit(requests: List<Request>)
    suspend fun <T : Any> submit(parameters: Map<T, Any?>): Boolean

    fun startRepeating(request: Request)
    fun stopRepeating()

    /**
     * Indicates that internal parameters may have changed, and that the repeating request should
     * be updated as soon as possible.
     */
    fun invalidate()

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
internal class GraphProcessorImpl @Inject constructor(
    private val threads: Threads,
    private val cameraGraphConfig: CameraGraph.Config,
    private val graphState3A: GraphState3A,
    @ForCameraGraph private val graphScope: CoroutineScope,
    @ForCameraGraph private val graphListeners: List<@JvmSuppressWildcards Request.Listener>
) : GraphProcessor {
    private val lock = Any()

    @GuardedBy("lock")
    private val submitQueue: MutableList<List<Request>> = ArrayList()

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

    override fun onGraphStarted(requestProcessor: RequestProcessor) {
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

    override fun onGraphStopped(requestProcessor: RequestProcessor) {
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

    override fun onGraphModified(requestProcessor: RequestProcessor) {
        synchronized(lock) {
            if (closed) {
                return
            }
            if (requestProcessor != _requestProcessor) {
                return
            }
        }
        resubmit()
    }

    override fun startRepeating(request: Request) {
        synchronized(lock) {
            if (closed) return
            nextRepeatingRequest = request
            debug { "startRepeating with ${request.formatForLogs()}" }
        }

        graphScope.launch {
            tryStartRepeating()
        }
    }

    override fun stopRepeating() {
        val processor: RequestProcessor?

        synchronized(lock) {
            processor = _requestProcessor
            nextRepeatingRequest = null
            currentRepeatingRequest = null
        }

        graphScope.launch {
            Debug.traceStart { "$this#stopRepeating" }
            // Start with requests that have already been submitted
            if (processor != null) {
                synchronized(processor) {
                    processor.stopRepeating()
                }
            }
            Debug.traceStop()
        }
    }

    override fun submit(request: Request) {
        submit(listOf(request))
    }

    override fun submit(requests: List<Request>) {
        synchronized(lock) {
            if (closed) {
                graphScope.launch(threads.defaultDispatcher) {
                    abortBurst(requests)
                }
                return
            }
            submitQueue.add(requests)
        }

        graphScope.launch(threads.defaultDispatcher) {
            submitLoop()
        }
    }

    /**
     * Submit a request to the camera using only the current repeating request.
     */
    override suspend fun <T : Any> submit(parameters: Map<T, Any?>): Boolean =
        withContext(threads.defaultDispatcher) {
            val processor: RequestProcessor?
            val request: Request?
            val requiredParameters: MutableMap<Any, Any?> = mutableMapOf()

            synchronized(lock) {
                if (closed) return@withContext false
                processor = _requestProcessor
                request = currentRepeatingRequest

                requiredParameters.putAll(parameters.toMutableMap())
                graphState3A.writeTo(requiredParameters)
                requiredParameters.putAll(cameraGraphConfig.requiredParameters)
            }

            return@withContext when {
                processor == null || request == null -> false
                else -> processor.submit(
                    request,
                    defaultParameters = cameraGraphConfig.defaultParameters,
                    requiredParameters = requiredParameters,
                    defaultListeners = graphListeners
                )
            }
        }

    override fun invalidate() {
        // Invalidate is only used for updates to internal state (listeners, parameters, etc) and
        // should not (currently) attempt to resubmit the normal request queue.
        graphScope.launch(threads.defaultDispatcher) {
            tryStartRepeating()
        }
    }

    override fun abort() {
        val processor: RequestProcessor?
        val requests: List<List<Request>>

        synchronized(lock) {
            processor = _requestProcessor
            requests = submitQueue.toList()
            submitQueue.clear()
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
        graphScope.launch(threads.defaultDispatcher) {
            tryStartRepeating()
            submitLoop()
        }
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

    private fun tryStartRepeating() {
        val processor: RequestProcessor?
        val request: Request?

        synchronized(lock) {
            if (closed) return

            processor = _requestProcessor
            request = nextRepeatingRequest ?: currentRepeatingRequest

            // TODO: It might be a good idea to turn the "nextRepeatingRequest" into a queue to
            //  help with cases where we want to start the camera early. Example: If we have a
            //  stream configuration where the viewfinder is deferred, but we have an ImageReader
            //  that is _not_ deferred, it may be possible to submit the repeating request.
            //  However, a request with *both* streams would be rejected because not all streams
            //  are ready.
            //  Example:
            //   - Request(listOf(viewfinderStream, otherStream)) // Fails (no viewfinder surface)
            //   - Request(listOf(otherStream)) // works
            //  If (as an app developer) we wanted to make sure the camera starts before the
            //  viewfinder is ready, we would likely want to do something like:
            //   - startRepeating(listOf(otherStream))
            //   - startRepeating(listOf(viewfinderStream, otherStream))
            //  The way this is implemented at the moment, the "nextRepeatingRequest" would be set
            //  to the second call to startRepeating, which would not work. Since the first call got
            //  discarded, we would be unable to start the camera before the viewfinder was
            //  available.
        }

        if (processor != null && request != null) {

            Debug.traceStart { "$this#startRepeating" }
            synchronized(processor) {
                val requiredParameters = mutableMapOf<Any, Any?>()
                graphState3A.writeTo(requiredParameters)
                requiredParameters.putAll(cameraGraphConfig.requiredParameters)

                if (processor.startRepeating(
                        request,
                        defaultParameters = cameraGraphConfig.defaultParameters,
                        requiredParameters = requiredParameters,
                        defaultListeners = graphListeners
                    )
                ) {
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
            val nullableBurst = submitQueue.firstOrNull()
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
                submitted = synchronized(processor) {
                    val requiredParameters = mutableMapOf<Any, Any?>()
                    graphState3A.writeTo(requiredParameters)
                    requiredParameters.putAll(cameraGraphConfig.requiredParameters)

                    if (burst.size == 1) {
                        processor.submit(
                            burst[0],
                            defaultParameters = cameraGraphConfig.defaultParameters,
                            requiredParameters = requiredParameters,
                            defaultListeners = graphListeners
                        )
                    } else {
                        processor.submit(
                            burst,
                            defaultParameters = cameraGraphConfig.defaultParameters,
                            requiredParameters = requiredParameters,
                            defaultListeners = graphListeners
                        )
                    }
                }
            } finally {
                Debug.traceStop()
                synchronized(lock) {
                    if (submitted) {
                        check(submitQueue.removeAt(0) === burst)

                        val nullableBurst = submitQueue.firstOrNull()
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
