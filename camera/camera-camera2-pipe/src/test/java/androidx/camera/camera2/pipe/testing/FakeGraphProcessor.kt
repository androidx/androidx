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
import androidx.camera.camera2.pipe.impl.GraphProcessor
import androidx.camera.camera2.pipe.impl.RequestProcessor

/**
 * Fake implementation of a [GraphProcessor] for tests.
 */
class FakeGraphProcessor : GraphProcessor {
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

    override fun setRepeating(request: Request) {
        repeatingRequest = request
    }

    override fun submit(request: Request) {
        submit(listOf(request))
    }

    override fun submit(requests: List<Request>) {
        _requestQueue.add(requests)
    }

    override suspend fun submit(parameters: Map<CaptureRequest.Key<*>, Any>): Boolean {
        if (closed) {
            return false
        }
        val currProcessor = processor
        val currRepeatingRequest = repeatingRequest
        return when {
            currProcessor == null || currRepeatingRequest == null -> false
            else -> currProcessor.submit(
                currRepeatingRequest,
                parameters,
                requireSurfacesForAllStreams = false
            )
        }
    }

    override fun abort() {
        _requestQueue.clear()
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        active = false
        _requestQueue.clear()
    }

    override fun attach(requestProcessor: RequestProcessor) {
        val old = processor
        processor = requestProcessor
        old?.close()
    }

    override fun detach(requestProcessor: RequestProcessor) {
        val old = processor
        if (requestProcessor === old) {
            processor = null
            old.close()
        }
    }

    override fun invalidate() {
    }
}