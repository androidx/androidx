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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.ArrayMap
import android.view.Surface
import androidx.annotation.GuardedBy
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
import androidx.camera.camera2.pipe.core.Log.MonitoredLogMessages.REPEATING_REQUEST_STARTED_TIMEOUT
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.media.AndroidImageWriter
import androidx.camera.camera2.pipe.media.ImageWriterWrapper
import androidx.camera.camera2.pipe.writeParameters
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

internal interface Camera2CaptureSequenceProcessorFactory {
    fun create(
        session: CameraCaptureSessionWrapper,
        surfaceMap: Map<StreamId, Surface>,
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
        surfaceMap: Map<StreamId, Surface>,
    ): CaptureSequenceProcessor<*, CaptureSequence<Any>> {
        return Camera2CaptureSequenceProcessor(
            session,
            threads,
            graphConfig.defaultTemplate,
            surfaceMap,
            streamGraph,
            quirks.shouldWaitForRepeatingRequestStartOnDisconnect(graphConfig),
        )
            as CaptureSequenceProcessor<Any, CaptureSequence<Any>>
    }
}

internal val captureSequenceProcessorDebugIds = atomic(0)
internal val captureSequenceDebugIds = atomic(0L)
internal val requestTags = atomic(0L)

internal fun nextRequestNumber(): RequestNumber = RequestNumber(requestTags.incrementAndGet())

private const val REQUIRE_SURFACE_FOR_ALL_STREAMS = false

/**
 * This class is designed to synchronously handle interactions with a [CameraCaptureSessionWrapper].
 */
internal class Camera2CaptureSequenceProcessor(
    private val session: CameraCaptureSessionWrapper,
    private val threads: Threads,
    private val template: RequestTemplate,
    private val surfaceMap: Map<StreamId, Surface>,
    private val streamGraph: StreamGraph,
    private val awaitRepeatingRequestOnDisconnect: Boolean = false,
) : CaptureSequenceProcessor<CaptureRequest, Camera2CaptureSequence> {
    private val debugId = captureSequenceProcessorDebugIds.incrementAndGet()
    private val lock = Any()

    @GuardedBy("lock") private var disconnected = false

    @GuardedBy("lock")
    private var lastSingleRepeatingRequestSequence: Camera2CaptureSequence? = null

    override fun build(
        isRepeating: Boolean,
        requests: List<Request>,
        defaultParameters: Map<*, Any?>,
        graphParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        sequenceListener: CaptureSequence.CaptureSequenceListener,
        listeners: List<Request.Listener>,
    ): Camera2CaptureSequence? {
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
            val requestBuilder = buildCaptureRequestBuilder(request, requestTemplate) ?: return null

            val tag =
                requiredParameters[CameraPipeKeys.camera2CaptureRequestTag]
                    ?: defaultParameters[CameraPipeKeys.camera2CaptureRequestTag]
            requestBuilder.setTag(tag)

            // Apply the output surfaces to the requestBuilder
            var hasSurface = false
            for (i in request.streams.indices) {
                val surface = streamToSurfaceMap[request.streams[i]]
                if (surface != null) {
                    requestBuilder.addTarget(surface)
                    hasSurface = true
                }
            }

            // Soundness check to make sure we add at least one surface. This should be guaranteed
            // because we are supposed to exit early and return false if we cannot map at least one
            // surface per request.
            check(hasSurface)

            if (request.inputRequest != null) {
                checkNotNull(imageWriter) {
                    "Failed to create ImageWriter for capture session: $session"
                }
                val image = request.inputRequest.image
                synchronized(lock) {
                    if (disconnected) {
                        Log.warn { "$this disconnected. $image can't be queued to $imageWriter" }
                        return null
                    }
                }
                Log.debug { "Queuing image $image for reprocessing to ImageWriter $imageWriter" }
                // TODO(b/321603591): Queue image closer to when capture request is submitted
                if (!imageWriter.queueInputImage(image)) {
                    Log.debug {
                        "Failed to queue image $image for reprocessing to ImageWriter $imageWriter"
                    }
                    return null
                }

                // Apply request parameters to the builder.
                requestBuilder.writeParameters(request.parameters)
            } else {
                // Apply default parameters to the builder first.
                requestBuilder.writeParameters(defaultParameters)

                // Apply CameraGraph parameters to the builder.
                requestBuilder.writeParameters(graphParameters)

                // Apply request parameters to the builder.
                requestBuilder.writeParameters(request.parameters)

                // Finally, write required parameters to the request builder. This will override any
                // value that has ben previously set.
                //
                // TODO(sushilnath@): Implement one of the two options
                //  (1) Apply the 3A parameters from internal 3A state machine at last and provide
                //      a flag in the Request object to specify when the clients want to explicitly
                //      override some of the 3A parameters directly. Add code to handle the flag.
                //  (2) Let clients override the 3A parameters freely and when that happens
                //      intercept those parameters from the request and keep the internal 3A state
                //      machine in sync.
                requestBuilder.writeParameters(requiredParameters)
            }
            val requestNumber = nextRequestNumber()

            // Create the camera2 captureRequest and add it to our list of requests.
            val captureRequest = requestBuilder.build()

            // Create high speed capture requests if session is a high speed session
            if (session is CameraConstrainedHighSpeedCaptureSessionWrapper) {
                val highSpeedRequestList =
                    session.createHighSpeedRequestList(captureRequest) ?: return null

                // Check if video stream use case or hint is present
                val containsVideoStream =
                    request.streams.any {
                        streamGraph.outputs.any {
                            it.streamUseCase == OutputStream.StreamUseCase.VIDEO_RECORD ||
                                it.streamUseHint == OutputStream.StreamUseHint.VIDEO_RECORD
                        }
                    }

                // If preview stream is present with no recording stream, then only submit the first
                // request from the list of high speed requests. Preview only high speed requests
                // drain at 30 frames/sec instead of 120 or 240 frames/sec. When parameters change
                // (e.g. zoom crop rectangle), the same request is repeated for 4/30 frames at
                // the same value instead of smoothly changing across each frame.
                if (!containsVideoStream) {
                    val metadata =
                        Camera2RequestMetadata(
                            session,
                            highSpeedRequestList[0],
                            defaultParameters,
                            graphParameters,
                            requiredParameters,
                            streamToSurfaceMap,
                            requestTemplate,
                            isRepeating,
                            request,
                            requestNumber,
                        )
                    captureRequests.add(highSpeedRequestList[0])
                    requestList.add(metadata)
                } else {
                    // If the recording stream is present, add all captureRequests from
                    // createHighSpeedRequestList to the list of captureRequests.
                    for (i in highSpeedRequestList.indices) {
                        val metadata =
                            Camera2RequestMetadata(
                                session,
                                highSpeedRequestList[i],
                                defaultParameters,
                                graphParameters,
                                requiredParameters,
                                streamToSurfaceMap,
                                requestTemplate,
                                isRepeating,
                                request,
                                requestNumber,
                            )

                        captureRequests.add(highSpeedRequestList[i])
                        requestList.add(metadata)
                    }
                }
            } else {
                val metadata =
                    Camera2RequestMetadata(
                        session,
                        captureRequest,
                        defaultParameters,
                        graphParameters,
                        requiredParameters,
                        streamToSurfaceMap,
                        requestTemplate,
                        isRepeating,
                        request,
                        requestNumber,
                    )
                captureRequests.add(captureRequest)
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
            surfaceToStreamMap,
        )
    }

    override fun submit(captureSequence: Camera2CaptureSequence): Int? =
        synchronized(lock) {
            if (disconnected) {
                Log.warn { "$this disconnected. $captureSequence won't be submitted" }
                return null
            }
            val captureCallback = captureSequence as CameraCaptureSession.CaptureCallback
            // TODO: Update these calls to use executors on newer versions of the OS
            return if (
                captureSequence.captureRequestList.size == 1 &&
                    session !is CameraConstrainedHighSpeedCaptureSessionWrapper
            ) {
                if (captureSequence.repeating) {
                    if (awaitRepeatingRequestOnDisconnect) {
                        lastSingleRepeatingRequestSequence = captureSequence
                    }
                    session.setRepeatingRequest(
                        captureSequence.captureRequestList[0],
                        captureCallback,
                    )
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

    override fun abortCaptures(): Unit =
        synchronized(lock) {
            Log.debug { "$this#abortCaptures" }
            session.abortCaptures()
        }

    override fun stopRepeating(): Unit =
        synchronized(lock) {
            Log.debug { "$this#stopRepeating" }
            session.stopRepeating()
        }

    override suspend fun shutdown() {
        disconnect()
    }

    internal fun disconnect() {
        // Shutdown is responsible for releasing resources that are no longer in use.
        Debug.trace("$this#disconnect") {
            val captureSequence: Camera2CaptureSequence? =
                synchronized(lock) {
                    if (!disconnected) {
                        disconnected = true
                        imageWriter?.close()
                        session.inputSurface?.release()
                        lastSingleRepeatingRequestSequence
                    } else {
                        null
                    }
                }
            // Wait for the last submitted repeating request sequence to start, if one was set.
            if (awaitRepeatingRequestOnDisconnect && captureSequence != null) {
                awaitRepeatingRequestStarted(captureSequence)
            }
        }
    }

    override fun toString(): String {
        return "Camera2CaptureSequenceProcessor-$debugId"
    }

    private fun awaitRepeatingRequestStarted(captureSequence: Camera2CaptureSequence) {
        Log.debug { "Waiting for the last repeating request sequence: $captureSequence" }
        // On certain devices, the submitted repeating request sequence may not give
        // us onCaptureStarted() or onCaptureSequenceAborted() [1]. Hence we wrap
        // the wait under a timeout to prevent us from waiting forever.
        //
        // [1] b/307588161 - [ANR] at
        // androidx.camera.camera2.pipe.compat.Camera2CaptureSequenceProcessor.close
        threads.runBlockingCheckedOrNull(WAIT_FOR_REPEATING_TIMEOUT_MS) {
            captureSequence.awaitStarted()
        }
            ?: Log.error {
                "$this#close: $REPEATING_REQUEST_STARTED_TIMEOUT" +
                    ", lastSingleRepeatingRequestSequence = $captureSequence"
            }
    }

    /**
     * The [ImageWriterWrapper] is created once per capture session when the capture session is
     * created, assuming it's a reprocessing session.
     */
    private val imageWriter =
        if (streamGraph.inputs.isNotEmpty()) {
            val inputStream = streamGraph.inputs.first()
            val sessionInputSurface = session.inputSurface
            checkNotNull(sessionInputSurface) {
                "inputSurface is required to create instance of imageWriter."
            }
            val androidImageWriter =
                try {
                    AndroidImageWriter.create(
                        sessionInputSurface,
                        inputStream.id,
                        inputStream.maxImages,
                        inputStream.format,
                        threads.camera2Handler,
                    )
                } catch (e: RuntimeException) {
                    Log.warn(e) { "Failed to create ImageWriter for session $session" }
                    null
                }
            if (androidImageWriter != null) {
                Log.debug { "Created ImageWriter $androidImageWriter for session $session" }
            }
            androidImageWriter
        } else {
            null
        }

    private fun validateRequestList(
        requests: List<Request>,
        session: CameraCaptureSessionWrapper,
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
                            it.streamUseCase == OutputStream.StreamUseCase.PREVIEW ||
                                it.streamUseHint == OutputStream.StreamUseHint.DEFAULT ||
                                it.streamUseHint == null
                        }
                    }

                // Check if all high speed requests have preview use case or hint
                if (prevContainsPreviewStream != null) {
                    if (prevContainsPreviewStream != containsPreviewStream) {
                        Log.error {
                            "The previous high speed request and the current high speed request " +
                                "must both have a preview stream use case or hint. " +
                                "Previous request contains preview stream use case or hint: " +
                                "$prevContainsPreviewStream. Current request contains " +
                                "preview stream use case or hint: $containsPreviewStream."
                        }
                    }
                }

                // Check if video stream use case is present
                containsVideoStream =
                    request.streams.any {
                        streamGraph.outputs.any {
                            it.streamUseCase == OutputStream.StreamUseCase.VIDEO_RECORD ||
                                it.streamUseHint == OutputStream.StreamUseHint.VIDEO_RECORD
                        }
                    }

                // Check if all high speed requests have the same video use case
                if (prevContainsVideoStream != null) {
                    if (prevContainsVideoStream != containsVideoStream) {
                        Log.error {
                            "The previous high speed request and the current high speed request " +
                                "do not have the same video stream use case. Previous request " +
                                "contains video stream use case: $prevContainsVideoStream. " +
                                "Current request contains video stream use case" +
                                ": $containsVideoStream."
                        }
                    }
                }

                // Streams must be preview and/or video for high speed sessions
                val allStreamsValidForHighSpeedOperatingMode =
                    this.streamGraph.outputs.all { it.isValidForHighSpeedOperatingMode() }

                if (!allStreamsValidForHighSpeedOperatingMode) {
                    Log.error {
                        "HIGH_SPEED CameraGraph must only contain Preview and/or Video " +
                            "streams. Configured outputs are ${streamGraph.outputs}"
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
        streamToSurfaceMap: MutableMap<StreamId, Surface>,
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
                    // TODO(codelogic) There should be a more efficient way to do these lookups than
                    // having two maps.
                    surfaceToStreamMap[surface] = stream
                    streamToSurfaceMap[stream] = surface
                    hasSurface = true
                } else if (REQUIRE_SURFACE_FOR_ALL_STREAMS) {
                    Log.info { "  Failed to bind surface for $stream" }

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

    /**
     * Create a reprocessing request builder if the request is a reprocessing request. Otherwise,
     * create a regular request builder. There is a risk this will throw an exception or return null
     * if the CameraDevice has been closed or disconnected. If this fails, indicate that the request
     * was not submitted.
     */
    private fun buildCaptureRequestBuilder(
        request: Request,
        requestTemplate: RequestTemplate,
    ): CaptureRequest.Builder? {
        val requestBuilder =
            if (request.inputRequest != null) {
                val totalCaptureResult =
                    request.inputRequest.frameInfo.unwrapAs(TotalCaptureResult::class)
                checkNotNull(totalCaptureResult) {
                    "Failed to unwrap FrameInfo ${request.inputRequest.frameInfo} as " +
                        "TotalCaptureResult"
                }
                session.device.createReprocessCaptureRequest(totalCaptureResult)
            } else {
                session.device.createCaptureRequest(requestTemplate)
            }

        if (requestBuilder == null) {
            if (request.inputRequest != null) {
                Log.info {
                    "Failed to create a ReprocessingCaptureRequest.Builder " +
                        "from ${request.inputRequest.frameInfo}!"
                }
            } else {
                Log.info { "Failed to create a CaptureRequest.Builder " + "from $requestTemplate!" }
            }
            return null
        }
        return requestBuilder
    }

    companion object {
        private const val WAIT_FOR_REPEATING_TIMEOUT_MS = 2_000L // 2s
    }
}

/** This class packages together information about a request that was submitted to the camera. */
internal class Camera2RequestMetadata(
    private val cameraCaptureSessionWrapper: CameraCaptureSessionWrapper,
    private val captureRequest: CaptureRequest,
    private val defaultParameters: Map<*, Any?>,
    private val graphParameters: Map<*, Any?>,
    private val requiredParameters: Map<*, Any?>,
    override val streams: Map<StreamId, Surface>,
    override val template: RequestTemplate,
    override val repeating: Boolean,
    override val request: Request,
    override val requestNumber: RequestNumber,
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
            graphParameters.containsKey(key) -> {
                graphParameters[key] as T?
            }
            else -> {
                defaultParameters[key] as T?
            }
        }

    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = get(key) ?: default

    @Suppress("UNCHECKED_CAST", "NewApi")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CaptureRequest::class -> captureRequest as T
            CameraCaptureSession::class ->
                cameraCaptureSessionWrapper.unwrapAs(CameraCaptureSession::class) as? T
            CameraExtensionSession::class -> {
                check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                cameraCaptureSessionWrapper.unwrapAs(CameraExtensionSession::class) as? T
            }
            else -> null
        }
}
