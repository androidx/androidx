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
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.compat.Camera2Quirks
import androidx.camera.camera2.pipe.compat.CameraPipeKeys
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.info
import androidx.camera.camera2.pipe.core.Threads
import java.util.concurrent.CountDownLatch
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
    fun submit(parameters: Map<*, Any?>): Boolean

    /**
     * Indicates that internal parameters may have changed, and that the repeating request should be
     * updated as soon as possible.
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
    graphState3A: GraphState3A,
    graphListener3A: Listener3A,
    @ForCameraGraph graphListeners: List<@JvmSuppressWildcards Request.Listener>
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

        val captureLimiter =
            if (Camera2Quirks.shouldWaitForRepeatingBeforeCapture()) {
                CaptureLimiter(10)
            } else {
                null
            }

        graphLoop =
            GraphLoop(
                cameraGraphId = cameraGraphId,
                defaultParameters = defaultParameters,
                requiredParameters = requiredParameters,
                graphListeners = graphListeners + listOfNotNull(captureLimiter),
                graphState3A = if (ignore3AState) null else graphState3A,
                listeners = listOfNotNull(graphListener3A, captureLimiter),
                shutdownScope = threads.globalScope,
                dispatcher = threads.lightweightDispatcher
            )

        captureLimiter?.graphLoop = graphLoop
    }

    // On some devices, we need to wait for 10 frames to complete before we can guarantee the
    // success of single capture requests. This is a quirk identified as part of b/287020251 and
    // reported in b/289284907.
    private var repeatingRequestsCompleted = CountDownLatch(10)

    // Graph listener added to repeating requests in order to handle the aforementioned quirk.
    private val graphProcessorRepeatingListeners =
        if (!Camera2Quirks.shouldWaitForRepeatingBeforeCapture()) {
            graphListeners
        } else {
            graphListeners +
                object : Request.Listener {
                    override fun onComplete(
                        requestMetadata: RequestMetadata,
                        frameNumber: FrameNumber,
                        result: FrameInfo
                    ) {
                        repeatingRequestsCompleted.countDown()
                    }
                }
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
     * Submit a request to the camera using only the current repeating request. If we don't have the
     * current repeating request, and there are no repeating requests queued, this will return
     * false. Otherwise, the method tries to submit the provided [parameters] and suspends until it
     * finishes.
     */
    override fun submit(parameters: Map<*, Any?>): Boolean = graphLoop.submit(parameters)

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
