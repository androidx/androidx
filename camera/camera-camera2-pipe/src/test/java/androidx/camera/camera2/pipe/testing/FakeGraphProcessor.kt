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

import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.GraphProcessor
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor
import androidx.camera.camera2.pipe.graph.GraphState3A
import androidx.camera.camera2.pipe.putAllMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Fake implementation of a [GraphProcessor] for tests. */
internal class FakeGraphProcessor(
    val graphState3A: GraphState3A = GraphState3A(),
    val defaultParameters: Map<*, Any?> = emptyMap<Any, Any?>(),
    val defaultListeners: List<Request.Listener> = emptyList()
) : GraphProcessor, GraphListener {
    var active = true
        private set
    var closed = false
        private set
    var repeatingRequest: Request? = null
        private set
    val requestQueue: List<List<Request>>
        get() = _requestQueue

    private val _requestQueue = mutableListOf<List<Request>>()
    private var processor: GraphRequestProcessor? = null

    private val _graphState = MutableStateFlow<GraphState>(GraphStateStopped)

    override val graphState: StateFlow<GraphState>
        get() = _graphState

    override fun startRepeating(request: Request) {
        repeatingRequest = request
    }

    override fun stopRepeating() {
        repeatingRequest = null
    }

    override fun submit(request: Request) {
        submit(listOf(request))
    }

    override fun submit(requests: List<Request>) {
        _requestQueue.add(requests)
    }

    override suspend fun trySubmit(parameters: Map<*, Any?>): Boolean {
        if (closed) {
            return false
        }
        val currProcessor = processor
        val currRepeatingRequest = repeatingRequest
        val requiredParameters = mutableMapOf<Any, Any?>()
        requiredParameters.putAllMetadata(parameters)
        graphState3A.writeTo(requiredParameters)

        return when {
            currProcessor == null || currRepeatingRequest == null -> false
            else ->
                currProcessor.submit(
                    isRepeating = false,
                    requests = listOf(currRepeatingRequest),
                    defaultParameters = defaultParameters,
                    requiredParameters = requiredParameters,
                    listeners = defaultListeners
                )
        }
    }

    override fun abort() {
        _requestQueue.clear()
        // TODO: Invoke abort on the listeners in the queue.
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        active = false
        _requestQueue.clear()
    }

    override fun onGraphStarting() {
        _graphState.value = GraphStateStarting
    }

    override fun onGraphStarted(requestProcessor: GraphRequestProcessor) {
        _graphState.value = GraphStateStarted
        val old = processor
        processor = requestProcessor
        old?.close()
    }

    override fun onGraphStopping() {
        _graphState.value = GraphStateStopping
    }

    override fun onGraphStopped(requestProcessor: GraphRequestProcessor) {
        _graphState.value = GraphStateStopped
        val old = processor
        if (requestProcessor === old) {
            processor = null
            old.close()
        }
    }

    override fun onGraphModified(requestProcessor: GraphRequestProcessor) {
        invalidate()
    }

    override fun onGraphError(graphStateError: GraphStateError) {
        _graphState.update { graphState ->
            if (graphState is GraphStateStopping || graphState is GraphStateStopped) {
                GraphStateStopped
            } else {
                graphStateError
            }
        }
    }

    override fun invalidate() {
        if (closed) {
            return
        }

        val currProcessor = processor
        val currRepeatingRequest = repeatingRequest
        val requiredParameters = graphState3A.readState()

        if (currProcessor == null || currRepeatingRequest == null) {
            return
        }

        currProcessor.submit(
            isRepeating = true,
            requests = listOf(currRepeatingRequest),
            defaultParameters = defaultParameters,
            requiredParameters = requiredParameters,
            listeners = defaultListeners
        )
    }
}
