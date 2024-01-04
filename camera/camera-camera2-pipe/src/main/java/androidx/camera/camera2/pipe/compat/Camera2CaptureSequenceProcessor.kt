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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.util.ArrayMap
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CaptureSequence
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.writeParameters
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.runBlocking

internal interface Camera2CaptureSequenceProcessorFactory {
    fun create(
        session: CameraCaptureSessionWrapper,
        surfaceMap: Map<StreamId, Surface>
    ): CaptureSequenceProcessor<*, *>
}

internal class StandardCamera2CaptureSequenceProcessorFactory
@Inject
constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val streamGraph: StreamGraphImpl,
    private val quirks: Camera2Quirks,
) : Camera2CaptureSequenceProcessorFactory {
    @Suppress("UNCHECKED_CAST")
    override fun create(
        session: CameraCaptureSessionWrapper,
        surfaceMap: Map<StreamId, Surface>
    ): CaptureSequenceProcessor<*, CaptureSequence<Any>> {
        return Camera2CaptureSequenceProcessor(
            session,
            threads,
            graphConfig.defaultTemplate,
            surfaceMap,
            streamGraph,
            quirks.shouldWaitForRepeatingRequest(graphConfig)
        )
            as CaptureSequenceProcessor<Any, CaptureSequence<Any>>
    }
}

internal val captureSequenceProcessorDebugIds = atomic(0)
internal val captureSequenceDebugIds = atomic(0L)
internal val requestTags = atomic(0L)

internal fun nextRequestTag(): RequestNumber = RequestNumber(requestTags.incrementAndGet())

private const val REQUIRE_SURFACE_FOR_ALL_STREAMS = false

/**
 * This class is designed to synchronously handle interactions with a [CameraCaptureSessionWrapper].
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class Camera2CaptureSequenceProcessor(
    private val session: CameraCaptureSessionWrapper,
    private val threads: Threads,
    private val template: RequestTemplate,
    private val surfaceMap: Map<StreamId, Surface>,
    private val streamGraph: StreamGraph,
    private val shouldWaitForRepeatingRequest: Boolean = false,
) : CaptureSequenceProcessor<CaptureRequest, Camera2CaptureSequence> {
    private val debugId = captureSequenceProcessorDebugIds.incrementAndGet()
    private val lock = Any()

    @GuardedBy("lock")
    private var closed = false

    @GuardedBy("lock")
    private var lastSingleRepeatingRequestSequence: Camera2CaptureSequence? = null

    override fun build(
        isRepeating: Boolean,
        requests: List<Request>,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        listeners: List<Request.Listener>,
        sequenceListener: CaptureSequence.CaptureSequenceListener
    ): Camera2CaptureSequence? {

        val requestMap = ArrayMap<RequestNumber, Camera2RequestMetadata>(requests.size)
        val requestList = ArrayList<Camera2RequestMetadata>(requests.size)

        val captureRequests = ArrayList<CaptureRequest>(requests.size)

        val surfaceToStreamMap = ArrayMap<Surface, StreamId>()
        val streamToSurfaceMap = ArrayMap<StreamId, Surface>()

        if (!validateRequestList(requests, session)) {
            return null
        }

        if (!buildSurfaceMaps(requests, surfaceToStreamMap, streamToSurfaceMap)) {
            return null
        }

        for (request in requests) {

            Log.debug { "Building CaptureRequest for $request" }

            val requestTemplate = request.template ?: template

            // Create the request builder. There is a risk this will throw an exception or return
            // null
            // if the CameraDevice has been closed or disconnected. If this fails, indicate that the
            // request was not submitted.
            val requestBuilder = session.device.createCaptureRequest(requestTemplate)
            if (requestBuilder == null) {
                Log.info { "  Failed to create a CaptureRequest.Builder from $requestTemplate!" }
                return null
            }

            // Apply the output surfaces to the requestBuilder
            var hasSurface = false
            for (stream in request.streams) {
                val surface = streamToSurfaceMap[stream]
                if (surface != null) {
                    requestBuilder.addTarget(surface)
                    hasSurface = true
                }
            }

            // Soundness check to make sure we add at least one surface. This should be guaranteed
            // because we are supposed to exit early and return false if we cannot map at least one
            // surface per request.
            check(hasSurface)

            // Apply default parameters to the builder first.
            requestBuilder.writeParameters(defaultParameters)

            // Apply request parameters to the builder.
            requestBuilder.writeParameters(request.parameters)

            // Finally, write required parameters to the request builder. This will override any
            // value that has ben previously set.
            //
            // TODO(sushilnath@): Implement one of the two options. (1) Apply the 3A parameters
            // from internal 3A state machine at last and provide a flag in the Request object to
            // specify when the clients want to explicitly override some of the 3A parameters
            // directly. Add code to handle the flag. (2) Let clients override the 3A parameters
            // freely and when that happens intercept those parameters from the request and keep the
            // internal 3A state machine in sync.
            requestBuilder.writeParameters(requiredParameters)

            // The tag must be set for every request. We use it to lookup listeners for the
            // individual requests so that each request can specify individual listeners.
            val requestTag = nextRequestTag()
            requestBuilder.setTag(requestTag)

            // Create the camera2 captureRequest and add it to our list of requests.
            val captureRequest = requestBuilder.build()

            // Create high speed capture requests if session is a high speed session
            if (session is CameraConstrainedHighSpeedCaptureSessionWrapper) {
                val highSpeedRequestList = session.createHighSpeedRequestList(captureRequest)

                // Check if video stream use case is present
                val containsVideoStream =
                    request.streams.any {
                        streamGraph.outputs.any {
                            it.streamUseCase == OutputStream.StreamUseCase.VIDEO_RECORD
                        }
                    }

                // If preview stream is present with no recording stream, then only submit the first
                // request from the list of high speed requests. Preview only high speed requests
                // drain at 30 frames/sec instead of 120 or 240 frames/sec. When parameters change
                // (e.g. zoom crop rectangle), the same request is repeated for 4/30 frames at
                // the same value instead of smoothly changing across each frame.
                if (!containsVideoStream) {
                    captureRequests.add(highSpeedRequestList[0])
                    // If recording video with or without preview stream, then add all requests to
                    // list
                } else {
                    captureRequests.addAll(highSpeedRequestList)
                }

                val metadata =
                    Camera2RequestMetadata(
                        session,
                        highSpeedRequestList[0],
                        defaultParameters,
                        requiredParameters,
                        streamToSurfaceMap,
                        requestTemplate,
                        isRepeating,
                        request,
                        requestTag
                    )
                requestMap[requestTag] = metadata
                requestList.add(metadata)
            } else {
                captureRequests.add(captureRequest)

                val metadata =
                    Camera2RequestMetadata(
                        session,
                        captureRequest,
                        defaultParameters,
                        requiredParameters,
                        streamToSurfaceMap,
                        requestTemplate,
                        isRepeating,
                        request,
                        requestTag
                    )
                requestMap[requestTag] = metadata
                requestList.add(metadata)
            }
        }

        // Create the captureSequence listener
        return Camera2CaptureSequence(
            session.device.cameraId,
            isRepeating,
            captureRequests,
            requestList,
            listeners,
            sequenceListener,
            requestMap,
            surfaceToStreamMap
        )
    }

    override fun submit(captureSequence: Camera2CaptureSequence): Int? = synchronized(lock) {
        if (closed) {
            Log.warn { "Capture sequence processor closed. $captureSequence won't be submitted" }
            return null
        }
        val captureCallback = captureSequence as CameraCaptureSession.CaptureCallback
        // TODO: Update these calls to use executors on newer versions of the OS
        return if (captureSequence.captureRequestList.size == 1 &&
            session !is CameraConstrainedHighSpeedCaptureSessionWrapper
        ) {
            if (captureSequence.repeating) {
                if (shouldWaitForRepeatingRequest) {
                    lastSingleRepeatingRequestSequence = captureSequence
                }
                session.setRepeatingRequest(captureSequence.captureRequestList[0], captureCallback)
            } else {
                session.capture(captureSequence.captureRequestList[0], captureSequence)
            }
        } else {
            if (captureSequence.repeating) {
                session.setRepeatingBurst(captureSequence.captureRequestList, captureSequence)
            } else {
                session.captureBurst(captureSequence.captureRequestList, captureSequence)
            }
        }
    }

    override fun abortCaptures(): Unit = synchronized(lock) {
        Log.debug { "$this#abortCaptures" }
        session.abortCaptures()
    }

    override fun stopRepeating(): Unit = synchronized(lock) {
        Log.debug { "$this#stopRepeating" }
        session.stopRepeating()
    }

    override fun close() = synchronized(lock) {
        // Close should not shut down
        Debug.trace("$this#close") {
            if (shouldWaitForRepeatingRequest) {
                lastSingleRepeatingRequestSequence?.let {
                    Log.debug { "Waiting for the last repeating request sequence $it" }
                    runBlocking { it.awaitStarted() }
                }
            }
            closed = true
        }
    }

    override fun toString(): String {
        return "Camera2RequestProcessor-$debugId"
    }

    private fun validateRequestList(
        requests: List<Request>,
        session: CameraCaptureSessionWrapper
    ): Boolean {
        check(requests.isNotEmpty()) {
            "build(...) should never be called with an empty request list!"
        }

        // Create high speed capture requests if session is a high speed session
        if (session is CameraConstrainedHighSpeedCaptureSessionWrapper) {

            var containsPreviewStream: Boolean? = null
            var containsVideoStream: Boolean? = null

            for (request in requests) {

                val prevContainsPreviewStream = containsPreviewStream
                val prevContainsVideoStream = containsVideoStream

                // Check if preview stream use case is present
                containsPreviewStream =
                    request.streams.any {
                        streamGraph.outputs.any {
                            it.streamUseCase == OutputStream.StreamUseCase.PREVIEW
                        }
                    }

                // Check if all high speed requests have the same preview use case
                if (prevContainsPreviewStream != null) {
                    if (prevContainsPreviewStream != containsPreviewStream) {
                        Log.error {
                            "The previous high speed request and the current high speed request " +
                                "do not have the same preview stream use case. Previous request " +
                                "contains preview stream use case: $prevContainsPreviewStream. " +
                                "Current request contains preview stream use " +
                                "case: $containsPreviewStream."
                        }
                    }
                }

                // Check if video stream use case is present
                containsVideoStream =
                    request.streams.any {
                        streamGraph.outputs.any {
                            it.streamUseCase == OutputStream.StreamUseCase.VIDEO_RECORD
                        }
                    }

                // Check if all high speed requests have the same video use case
                if (prevContainsVideoStream != null) {
                    if (prevContainsVideoStream != containsVideoStream) {
                        Log.error {
                            "The previous high speed request and the current high speed request " +
                                "do not have the same video stream use case. Previous request " +
                                "contains video stream use case: $prevContainsPreviewStream. " +
                                "Current request contains video stream use case" +
                                ": $containsPreviewStream."
                        }
                    }
                }

                if (!containsPreviewStream && !containsVideoStream) {
                    Log.error {
                        "Preview and/or Video stream use cases must be " +
                            "present for high speed sessions."
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun buildSurfaceMaps(
        requests: List<Request>,
        surfaceToStreamMap: MutableMap<Surface, StreamId>,
        streamToSurfaceMap: MutableMap<StreamId, Surface>
    ): Boolean {
        check(requests.isNotEmpty()) {
            "build(...) should never be called with an empty request list!"
        }

        for (request in requests) {

            // Check to see if there is at least one valid surface for each stream.
            var hasSurface = false
            for (stream in request.streams) {
                if (streamToSurfaceMap.contains(stream)) {
                    hasSurface = true
                    continue
                }

                val surface = surfaceMap[stream]
                if (surface != null) {
                    Log.debug { "  Binding $stream to $surface" }

                    // TODO(codelogic) There should be a more efficient way to do these lookups than
                    // having two maps.
                    surfaceToStreamMap[surface] = stream
                    streamToSurfaceMap[stream] = surface
                    hasSurface = true
                } else if (REQUIRE_SURFACE_FOR_ALL_STREAMS) {
                    Log.info { "  Failed to bind surface to $stream" }

                    // If requireStreams is set we are required to map every stream to a valid
                    // Surface object for this request. If this condition is violated, then we
                    // return false because we cannot submit these request(s) until there is a valid
                    // StreamId -> Surface mapping for all streams.
                    return false
                }
            }

            // If there are no surfaces on a particular request, camera2 will not allow us to
            // submit it.
            if (!hasSurface) {
                Log.info { "  Failed to bind any surfaces for $request!" }
                return false
            }

            // Soundness check to make sure we add at least one surface. This should be guaranteed
            // because we are supposed to exit early and return false if we cannot map at least one
            // surface per request.
            check(hasSurface)
        }
        return true
    }
}

/** This class packages together information about a request that was submitted to the camera. */
@RequiresApi(21)
internal class Camera2RequestMetadata(
    private val cameraCaptureSessionWrapper: CameraCaptureSessionWrapper,
    private val captureRequest: CaptureRequest,
    private val defaultParameters: Map<*, Any?>,
    private val requiredParameters: Map<*, Any?>,
    override val streams: Map<StreamId, Surface>,
    override val template: RequestTemplate,
    override val repeating: Boolean,
    override val request: Request,
    override val requestNumber: RequestNumber
) : RequestMetadata {
    override fun <T> get(key: CaptureRequest.Key<T>): T? = captureRequest[key]
    override fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T = get(key) ?: default

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Metadata.Key<T>): T? =
        when {
            requiredParameters.containsKey(key) -> {
                requiredParameters[key] as T?
            }

            request.extras.containsKey(key) -> {
                request.extras[key] as T?
            }

            else -> {
                defaultParameters[key] as T?
            }
        }

    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = get(key) ?: default

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CaptureRequest::class -> captureRequest as T
            CameraCaptureSession::class ->
                cameraCaptureSessionWrapper.unwrapAs(CameraCaptureSession::class) as? T

            else -> null
        }
}
