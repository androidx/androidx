/*
 * Copyright 2022 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CaptureRequest
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStatusMonitor
import androidx.camera.camera2.pipe.CaptureSequence
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.RequestProcessor
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

@RequiresApi(21)
class ExternalCameraController(
    private val graphConfig: CameraGraph.Config,
    private val graphListener: GraphListener,
    private val requestProcessor: RequestProcessor
) : CameraController {
    private val sequenceProcessor = ExternalCaptureSequenceProcessor(graphConfig, requestProcessor)
    private val graphProcessor: GraphRequestProcessor =
        GraphRequestProcessor.from(sequenceProcessor)
    private var started = atomic(false)

    override val cameraId: CameraId
        get() = graphConfig.camera
    override var isForeground = false

    override fun start() {
        if (started.compareAndSet(expect = false, update = true)) {
            graphListener.onGraphStarted(graphProcessor)
        }
    }

    override fun stop() {
        if (started.compareAndSet(expect = true, update = false)) {
            graphListener.onGraphStopped(graphProcessor)
        }
    }

    override fun tryRestart(cameraStatus: CameraStatusMonitor.CameraStatus) {
        // This is intentionally made a no-op for now as CameraPipe external doesn't support
        // camera status monitoring and camera controller restart.
    }

    override fun close() {
        graphProcessor.close()
    }

    override fun updateSurfaceMap(surfaceMap: Map<StreamId, Surface>) {
        sequenceProcessor.surfaceMap = surfaceMap
    }
}

@Suppress("DEPRECATION")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class ExternalCaptureSequenceProcessor(
    private val graphConfig: CameraGraph.Config,
    private val processor: RequestProcessor
) : CaptureSequenceProcessor<Request, ExternalCaptureSequenceProcessor.ExternalCaptureSequence> {
    private val internalRequestNumbers = atomic(0L)
    private val internalSequenceNumbers = atomic(0)
    private val closed = atomic(false)
    private var _surfaceMap: Map<StreamId, Surface>? = null

    var surfaceMap: Map<StreamId, Surface>?
        get() = synchronized(this) { _surfaceMap }
        set(value) = synchronized(this) { _surfaceMap = value }

    override fun build(
        isRepeating: Boolean,
        requests: List<Request>,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        listeners: List<Request.Listener>,
        sequenceListener: CaptureSequence.CaptureSequenceListener
    ): ExternalCaptureSequence? {
        if (closed.value) {
            return null
        }
        val streamToSurfaceMap = surfaceMap
        if (streamToSurfaceMap == null) {
            Log.warn { "Cannot create an ExternalCaptureSequence until Surfaces are available!" }
            return null
        }
        val metadata =
            requests.map { request ->
                val parameters = defaultParameters + request.parameters + requiredParameters

                ExternalRequestMetadata(
                    graphConfig.defaultTemplate,
                    streamToSurfaceMap,
                    parameters,
                    isRepeating,
                    request,
                    RequestNumber(internalRequestNumbers.incrementAndGet())
                )
            }

        return ExternalCaptureSequence(
            graphConfig.camera,
            isRepeating,
            requests,
            metadata,
            defaultParameters,
            requiredParameters,
            listeners,
            sequenceListener
        )
    }

    override fun submit(captureSequence: ExternalCaptureSequence): Int {
        check(!closed.value)
        check(captureSequence.captureRequestList.isNotEmpty())

        if (captureSequence.repeating) {
            check(captureSequence.captureRequestList.size == 1)
            processor.startRepeating(
                captureSequence.captureRequestList.single(),
                captureSequence.defaultParameters,
                captureSequence.requiredParameters,
                captureSequence.listeners
            )
        } else {
            if (captureSequence.captureRequestList.size == 1) {
                processor.submit(
                    captureSequence.captureRequestList.single(),
                    captureSequence.defaultParameters,
                    captureSequence.requiredParameters,
                    captureSequence.listeners
                )
            } else {
                processor.submit(
                    captureSequence.captureRequestList,
                    captureSequence.defaultParameters,
                    captureSequence.requiredParameters,
                    captureSequence.listeners
                )
            }
        }
        return internalSequenceNumbers.incrementAndGet()
    }

    override fun abortCaptures() {
        processor.abortCaptures()
    }

    override fun stopRepeating() {
        processor.stopRepeating()
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            processor.close()
        }
    }

    internal class ExternalCaptureSequence(
        override val cameraId: CameraId,
        override val repeating: Boolean,
        override val captureRequestList: List<Request>,
        override val captureMetadataList: List<RequestMetadata>,
        val defaultParameters: Map<*, Any?>,
        val requiredParameters: Map<*, Any?>,
        override val listeners: List<Request.Listener>,
        override val sequenceListener: CaptureSequence.CaptureSequenceListener,
    ) : CaptureSequence<Request> {
        @Volatile
        private var _sequenceNumber: Int? = null
        override var sequenceNumber: Int
            get() {
                if (_sequenceNumber == null) {
                    // If the sequence id has not been submitted, it means the call to capture or
                    // setRepeating has not yet returned. The callback methods should never be
                    // synchronously invoked, so the only case this should happen is if a second
                    // thread attempted to invoke one of the callbacks before the initial call
                    // completed. By locking against the captureSequence object here and in the
                    // capture call, we can block the callback thread until the sequenceId is
                    // available.
                    synchronized(this) {
                        return checkNotNull(_sequenceNumber) {
                            "SequenceNumber has not been set for $this!"
                        }
                    }
                }
                return checkNotNull(_sequenceNumber) {
                    "SequenceNumber has not been set for $this!"
                }
            }
            set(value) {
                _sequenceNumber = value
            }
    }

    @Suppress("UNCHECKED_CAST")
    internal class ExternalRequestMetadata(
        override val template: RequestTemplate,
        override val streams: Map<StreamId, Surface>,
        private val parameters: Map<*, Any?>,
        override val repeating: Boolean,
        override val request: Request,
        override val requestNumber: RequestNumber
    ) : RequestMetadata {
        override fun <T> get(key: CaptureRequest.Key<T>): T? = parameters[key] as T?
        override fun <T> get(key: Metadata.Key<T>): T? = parameters[key] as T?
        override fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T =
            get(key) ?: default

        override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = get(key) ?: default

        override fun <T : Any> unwrapAs(type: KClass<T>): T? = null
    }
}
