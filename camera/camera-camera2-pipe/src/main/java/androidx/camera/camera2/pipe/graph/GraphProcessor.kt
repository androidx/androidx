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

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.compat.Camera2Quirks
import androidx.camera.camera2.pipe.compat.CameraPipeKeys
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.info
import androidx.camera.camera2.pipe.core.Threads
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * The [GraphProcessor] is responsible for queuing and then submitting them to a
 * [CaptureSequenceProcessor] when it becomes available. This enables interactions to be queued up
 * and submitted before the camera is available.
 */
internal interface GraphProcessor {
    val graphState: StateFlow<GraphState>

    /**
     * The currently configured repeating request. Setting this value to null will attempt to call
     * stopRepeating on the Camera.
     */
    var repeatingRequest: Request?

    fun submit(request: Request): Boolean

    fun submit(requests: List<Request>): Boolean

    /**
     * This tries to submit a list of parameters based on the current repeating request. If the
     * CameraGraph hasn't been started but a valid repeating request has already been set this
     * method will enqueue the submission based on the repeating request.
     *
     * This behavior is required if users call 3A methods immediately after start. For example:
     * ```
     * cameraGraph.start()
     * cameraGraph.acquireSession().use {
     *     it.startRepeating(request)
     *     it.lock3A(...)
     * }
     * ```
     *
     * Under this scenario, developers should reasonably expect things to work, and therefore the
     * implementation handles this on a best-effort basis for the developer. Please read b/263211462
     * for more context.
     *
     * This method will throw a checked exception if no repeating request has been configured.
     */
    fun trigger(parameters: Map<*, Any?>): Boolean

    /** Update [androidx.camera.camera2.pipe.Parameters] changes to current repeating request. */
    fun updateGraphParameters(parameters: Map<*, Any?>)

    /** Update [androidx.camera.camera2.pipe.Parameters] changes to current repeating request. */
    fun update3AParameters(parameters: Map<*, Any?>)

    /**
     * Indicates that internal state may have changed, and that the repeating request may need to be
     * re-issued.
     */
    fun invalidate()

    /**
     * Abort all submitted requests that have not yet been submitted, as well as asking the
     * [CaptureSequenceProcessor] to abort any submitted requests, which may or may not succeed.
     */
    fun abort()

    /**
     * Closing the [GraphProcessor] will abort all queued requests. Any requests submitted after the
     * [GraphProcessor] is closed will immediately be aborted.
     */
    fun close()
}

/** The graph processor handles *cross-session* state, such as the most recent repeating request. */
@CameraGraphScope
internal class GraphProcessorImpl
@Inject
constructor(
    threads: Threads,
    private val cameraGraphId: CameraGraphId,
    private val cameraGraphConfig: CameraGraph.Config,
    graphListener3A: Listener3A,
    @ForCameraGraph graphListeners: List<@JvmSuppressWildcards Request.Listener>,
) : GraphProcessor, GraphListener {
    private val graphLoop: GraphLoop

    init {
        val defaultParameters = cameraGraphConfig.defaultParameters
        val requiredParameters = cameraGraphConfig.requiredParameters
        val ignore3AState =
            (defaultParameters[CameraPipeKeys.ignore3ARequiredParameters] == true) ||
                (requiredParameters[CameraPipeKeys.ignore3ARequiredParameters] == true)

        if (ignore3AState) {
            info {
                "${CameraPipeKeys.ignore3ARequiredParameters} is set to true, " +
                    "ignoring GraphState3A parameters."
            }
        }

        val requestsUntilActive =
            Camera2Quirks.getRepeatingRequestFrameCountForCapture(cameraGraphConfig.flags)

        val captureLimiter =
            if (requestsUntilActive != 0) {
                CaptureLimiter(requestsUntilActive.toLong())
            } else {
                null
            }

        graphLoop =
            GraphLoop(
                cameraGraphId = cameraGraphId,
                defaultParameters = defaultParameters,
                requiredParameters = requiredParameters,
                graphListeners = graphListeners + listOfNotNull(captureLimiter),
                listeners = listOfNotNull(graphListener3A, captureLimiter),
                shutdownScope = threads.cameraPipeScope,
                dispatcher = threads.lightweightDispatcher,
            )

        captureLimiter?.graphLoop = graphLoop
    }

    private val _graphState = MutableStateFlow<GraphState>(GraphStateStopped)
    override val graphState: StateFlow<GraphState>
        get() = _graphState

    override var repeatingRequest: Request?
        get() = graphLoop.repeatingRequest
        set(value) {
            graphLoop.repeatingRequest = value
        }

    override fun onGraphStarting() {
        debug { "$this onGraphStarting" }
        _graphState.value = GraphStateStarting
    }

    override fun onGraphStarted(requestProcessor: GraphRequestProcessor) {
        debug { "$this onGraphStarted" }
        _graphState.value = GraphStateStarted
        graphLoop.requestProcessor = requestProcessor
    }

    override fun onGraphStopping() {
        debug { "$this onGraphStopping" }
        _graphState.value = GraphStateStopping
    }

    override fun onGraphStopped(requestProcessor: GraphRequestProcessor?) {
        debug { "$this onGraphStopped" }
        _graphState.value = GraphStateStopped
        graphLoop.requestProcessor = null
    }

    override fun onGraphModified(requestProcessor: GraphRequestProcessor) {
        debug { "$this onGraphModified" }
        graphLoop.invalidate()
    }

    override fun onGraphError(graphStateError: GraphStateError) {
        debug { "$this onGraphError($graphStateError)" }
        _graphState.update { graphState ->
            if (graphState is GraphStateStopping || graphState is GraphStateStopped) {
                GraphStateStopped
            } else {
                graphStateError
            }
        }
    }

    override fun submit(request: Request): Boolean = submit(listOf(request))

    override fun submit(requests: List<Request>): Boolean {
        val reprocessingRequest = requests.firstOrNull { it.inputRequest != null }
        if (reprocessingRequest != null) {
            checkNotNull(cameraGraphConfig.input) {
                "Cannot submit $reprocessingRequest with input request " +
                    "${reprocessingRequest.inputRequest} to $this because CameraGraph was not " +
                    "configured to support reprocessing"
            }
        }

        return graphLoop.submit(requests)
    }

    /**
     * Submit a one time request to the camera using the most recent repeating request.
     *
     * If a repeating request is not currently set, this method will return false and fail.
     */
    override fun trigger(parameters: Map<*, Any?>): Boolean = graphLoop.trigger(parameters)

    override fun updateGraphParameters(parameters: Map<*, Any?>) {
        graphLoop.graphParameters = parameters
    }

    override fun update3AParameters(parameters: Map<*, Any?>) {
        graphLoop.graph3AParameters = parameters
    }

    override fun invalidate() {
        graphLoop.invalidate()
    }

    override fun abort() {
        graphLoop.abort()
    }

    override fun close() {
        graphLoop.close()
    }

    override fun toString(): String = "GraphProcessor(cameraGraph: $cameraGraphId)"
}
