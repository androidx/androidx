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

import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestProcessor
import androidx.camera.camera2.pipe.graph.GraphProcessor
import androidx.camera.camera2.pipe.graph.GraphState3A

/**
 * Fake implementation of a [GraphProcessor] for tests.
 */
internal class FakeGraphProcessor(
    private val graphState3A: GraphState3A = GraphState3A(),
    private val defaultParameters: Map<*, Any?> = emptyMap<Any, Any?>(),
    private val defaultListeners: List<Request.Listener> = emptyList()
) : GraphProcessor {
    var active = true
        private set
    var closed = false
        private set
    var repeatingRequest: Request? = null
        private set
    val requestQueue: List<List<Request>>
        get() = _requestQueue

    private val _requestQueue = mutableListOf<List<Request>>()
    private var processor: RequestProcessor? = null

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

    override suspend fun <T : Any> submit(parameters: Map<T, Any?>): Boolean {
        if (closed) {
            return false
        }
        val currProcessor = processor
        val currRepeatingRequest = repeatingRequest
        val requiredParameters = mutableMapOf<Any, Any?>()
        requiredParameters.putAll(parameters)
        graphState3A.writeTo(requiredParameters)

        return when {
            currProcessor == null || currRepeatingRequest == null -> false
            else -> currProcessor.submit(
                currRepeatingRequest,
                defaultParameters = defaultParameters,
                requiredParameters = requiredParameters,
                defaultListeners = defaultListeners
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

    override fun onGraphStarted(requestProcessor: RequestProcessor) {
        val old = processor
        processor = requestProcessor
        old?.close()
    }

    override fun onGraphStopped(requestProcessor: RequestProcessor) {
        val old = processor
        if (requestProcessor === old) {
            processor = null
            old.close()
        }
    }

    override fun onGraphModified(requestProcessor: RequestProcessor) {
        invalidate()
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

        currProcessor.startRepeating(
            currRepeatingRequest,
            defaultParameters = defaultParameters,
            requiredParameters = requiredParameters,
            defaultListeners = defaultListeners
        )
    }
}