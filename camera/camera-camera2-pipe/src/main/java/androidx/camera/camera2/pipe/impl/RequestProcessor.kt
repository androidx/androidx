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

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.util.ArrayMap
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.wrapper.CameraCaptureSessionWrapper
import androidx.camera.camera2.pipe.wrapper.ObjectUnavailableException
import androidx.camera.camera2.pipe.writeParameters
import kotlinx.atomicfu.atomic
import java.util.Collections.singletonList
import java.util.Collections.singletonMap
import javax.inject.Inject

/**
 * An instance of a RequestProcessor exists for the duration of a CameraCaptureSession and must be
 * created for each new CameraCaptureSession. It is responsible for low level interactions with the
 * CameraCaptureSession and for shimming the interfaces and callbacks to make them easier to work
 * with. Unlike the CameraCaptureSessionProxy interface the RequestProcessor has more liberty to
 * change the standard Camera2 API contract to make it easier to work with.
 *
 * There are some important design considerations:
 * - Instances class is not thread safe, although the companion object has some counters that are
 *   global and *are* thread safe.
 * - Special care is taken to reduce the number objects and wrappers that are created, and to reduce
 *   the number of loops and overhead in wrapper objects.
 * - Callbacks are expected to be invoked at very high frequency.
 * - One RequestProcessor instance per CameraCaptureSession
 */
interface RequestProcessor {

    /**
     * Submit a single [Request] with an optional set of extra parameters.
     *
     * @param request the request to submit to the camera.
     * @param extraRequestParameters extra parameters to apply to the request.
     * @param requireSurfacesForAllStreams if this flag is defined then this method will only submit
     *   the request if all streamIds can be mapped to valid surfaces. At least one surface is
     *   always required. This is useful if (for example) someone needs to quickly submit a
     *   request with a specific trigger or mode key but does not care about modifying the list of
     *   current surfaces.
     * @return false if this request failed to be submitted. If this method returns false, none of
     *   the callbacks on the Request(s) will be invoked.
     */
    fun submit(
        request: Request,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean

    /**
     * Submit a list of [Request]s with an optional set of extra parameters.
     *
     * @param requests the requests to submit to the camera.
     * @param extraRequestParameters extra parameters to apply to the request.
     * @param requireSurfacesForAllStreams if this flag is defined then this method will only submit
     *   the request if all streamIds can be mapped to valid surfaces. At least one surface is
     *   always required. This is useful if (for example) someone needs to quickly submit a
     *   request with a specific trigger or mode key but does not care about modifying the list of
     *   current surfaces.
     * @return false if this request failed to be submitted. If this method returns false, none of
     *   the callbacks on the Request(s) will be invoked.
     */
    fun submit(
        requests: List<Request>,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean

    /**
     * Set the repeating [Request] with an optional set of extra parameters.
     *
     * The current repeating request may not be executed at all, or it may be executed multiple
     * times. The repeating request is used as the base request for all 3A interactions which may
     * cause the request to be used to generate multiple [CaptureRequest]s to the camera.
     *
     * @param request the requests to set as the repeating request.
     * @param extraRequestParameters extra parameters to apply to the request.
     * @param requireSurfacesForAllStreams if this flag is defined then this method will only submit
     *   the request if all streamIds can be mapped to valid surfaces. At least one surface is
     *   always required. This is useful if (for example) someone needs to quickly submit a
     *   request with a specific trigger or mode key but does not care about modifying the list of
     *   current surfaces.
     * @return false if this request failed to be submitted. If this method returns false, none of
     *   the callbacks on the Request(s) will be invoked.
     */
    fun setRepeating(
        request: Request,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean

    /**
     * Abort requests that have been submitted but not completed.
     */
    fun abortCaptures()

    /**
     * Stops the current repeating request.
     */
    fun stopRepeating()

    /**
     * Puts the RequestProcessor into a closed state where it will reject all incoming requests.
     */
    fun close()

    interface Factory {
        fun create(
            session: CameraCaptureSessionWrapper,
            surfaceMap: Map<StreamId, Surface>
        ): RequestProcessor
    }
}

class StandardRequestProcessorFactory @Inject constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    @ForCameraGraph private val graphListeners: ArrayList<Request.Listener>,
    private val graphState3A: GraphState3A
) : RequestProcessor.Factory {
    override fun create(
        session: CameraCaptureSessionWrapper,
        surfaceMap: Map<StreamId, Surface>
    ): RequestProcessor =
        StandardRequestProcessor(
            session,
            threads,
            graphConfig,
            surfaceMap,
            graphListeners,
            graphState3A
        )
}

internal val requestProcessorDebugIds = atomic(0)
internal val requestSequenceDebugIds = atomic(0L)
internal val requestTags = atomic(0L)
internal fun nextRequestTag(): RequestNumber = RequestNumber(requestTags.incrementAndGet())

/**
 * This class is designed to synchronously handle interactions with the Camera CaptureSession.
 */
class StandardRequestProcessor(
    private val session: CameraCaptureSessionWrapper,
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val surfaceMap: Map<StreamId, Surface>,
    private val graphListeners: List<Request.Listener>,
    private val graphState3A: GraphState3A
) : RequestProcessor {

    @GuardedBy("inFlightRequests")
    private val inFlightRequests = mutableListOf<CaptureSequence>()
    private val debugId = requestProcessorDebugIds.incrementAndGet()
    private val closed = atomic(false)

    override fun submit(
        request: Request,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean {
        return configureAndCapture(
            singletonList(request),
            extraRequestParameters,
            requireSurfacesForAllStreams,
            isRepeating = false
        )
    }

    override fun submit(
        requests: List<Request>,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean {
        return configureAndCapture(
            requests,
            extraRequestParameters,
            requireSurfacesForAllStreams,
            isRepeating = false
        )
    }

    override fun setRepeating(
        request: Request,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean {
        return configureAndCapture(
            singletonList(request),
            extraRequestParameters,
            requireSurfacesForAllStreams,
            isRepeating = true
        )
    }

    override fun abortCaptures() {
        val requestsToAbort = synchronized(inFlightRequests) {
            val copy = inFlightRequests.toList()
            inFlightRequests.clear()
            copy
        }
        for (sequence in requestsToAbort) {
            sequence.invokeOnAborted()
        }
        session.abortCaptures()
    }

    override fun stopRepeating() {
        session.stopRepeating()
    }

    override fun close() {
        closed.compareAndSet(expect = false, update = true)
    }

    private fun configureAndCapture(
        requests: List<Request>,
        extras: Map<CaptureRequest.Key<*>, Any>,
        requireStreams: Boolean,
        isRepeating: Boolean
    ): Boolean {
        // Reject incoming requests if this instance has been stopped or closed.
        if (closed.value) {
            return false
        }

        val requestMap = ArrayMap<RequestNumber, RequestInfo>(requests.size)
        val captureRequests = ArrayList<CaptureRequest>(requests.size)

        val surfaceToStreamMap = ArrayMap<Surface, StreamId>()
        val streamToSurfaceMap = ArrayMap<StreamId, Surface>()

        for (request in requests) {
            val requestTemplate = request.template ?: graphConfig.template

            Log.debug { "Building CaptureRequest for $request" }

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
                } else if (requireStreams) {
                    Log.info { "  Failed to bind surface to $stream" }
                    // If requireStreams is set we are required to map every stream to a valid
                    // Surface object for this request. If this condition is violated, then we
                    // return false because we cannot submit these request(s) until there is a valid
                    // StreamId -> Surface mapping for all streams.
                    return false
                }
            }

            // If there are no surfaces on a particular request, camera2 will now allow us to
            // submit it.
            if (!hasSurface) {
                return false
            }

            // Create the request builder. There is a risk this will throw an exception or return null
            // if the CameraDevice has been closed or disconnected. If this fails, indicate that the
            // request was not submitted.
            val requestBuilder: CaptureRequest.Builder
            try {
                requestBuilder = session.device.createCaptureRequest(requestTemplate)
            } catch (exception: ObjectUnavailableException) {
                return false
            }

            // Apply the output surfaces to the requestBuilder
            hasSurface = false
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

            // Apply the parameters to the requestBuilder
            requestBuilder.writeParameters(request.requestParameters)

            // Apply the 3A parameters first. This gives the users of camerapipe the ability to
            // still override the 3A parameters for complicated use cases.
            //
            // TODO(sushilnath@): Implement one of the two options. (1) Apply the 3A parameters
            // from internal 3A state machine at last and provide a flag in the Request object to
            // specify when the clients want to explicitly override some of the 3A parameters
            // directly. Add code to handle the flag. (2) Let clients override the 3A parameters
            // freely and when that happens intercept those parameters from the request and keep the
            // internal 3A state machine in sync.
            graphState3A.writeTo(requestBuilder)

            // Write extra parameters to the request. These parameters will overwite parameters
            // defined in the Request (if they overlap)
            requestBuilder.writeParameters(extras)

            // The tag must be set for every request. We use it to lookup listeners for the
            // individual requests so that each request can specify individual listeners.
            val requestTag = nextRequestTag()
            requestBuilder.setTag(requestTag)

            // Create the camera2 captureRequest and add it to our list of requests.
            val captureRequest = requestBuilder.build()
            captureRequests.add(captureRequest)

            @Suppress("SyntheticAccessor")
            requestMap[requestTag] = RequestInfo(
                captureRequest,
                emptyMap(),
                streamToSurfaceMap,
                requestTemplate,
                isRepeating,
                request,
                requestTag
            )
        }

        // Create the captureSequence listener
        @Suppress("SyntheticAccessor")
        val captureSequence = CaptureSequence(
            graphListeners,
            if (requests.size == 1) {
                singletonMap(requestMap.keyAt(0), requestMap.valueAt(0))
            } else {
                requestMap
            },
            captureRequests,
            surfaceToStreamMap,
            inFlightRequests,
            session.device.cameraId
        )

        // Non-repeating requests must always be aware of abort calls.
        if (!isRepeating) {
            synchronized(inFlightRequests) {
                inFlightRequests.add(captureSequence)
            }
        }

        var captured = false
        return try {

            Log.debug { "Submitting $captureSequence" }
            capture(captureRequests, captureSequence, isRepeating)
            captured = true
            Log.debug { "Submitted $captureSequence" }
            true
        } catch (closedException: ObjectUnavailableException) {
            false
        } catch (accessException: CameraAccessException) {
            false
        } finally {
            // If ANY unhandled exception occurs, don't throw, but make sure we remove it from the
            // list of in-flight requests.
            if (!captured && !isRepeating) {
                captureSequence.invokeOnAborted()
            }
        }
    }

    private fun capture(
        captureRequests: List<CaptureRequest>,
        captureSequence: CaptureSequence,
        isRepeating: Boolean
    ) {
        captureSequence.invokeOnRequestSequenceCreated()

        // NOTE: This is a funny synchronization call. The purpose is to avoid a rare but possible
        // situation where calling capture causes one of the callback methods to be invoked before
        // sequenceNumber has been set on the callback. Both this call and the synchronized
        // behavior on the CaptureSequence listener have been designed to minimize the number of
        // synchronized calls.
        synchronized(lock = captureSequence) {
            // TODO: Update these calls to use executors on newer versions of the OS
            val sequenceNumber: Int = if (captureRequests.size == 1) {
                if (isRepeating) {
                    session.setRepeatingRequest(
                        captureRequests[0],
                        captureSequence,
                        threads.camera2Handler
                    )
                } else {
                    session.capture(captureRequests[0], captureSequence, threads.camera2Handler)
                }
            } else {
                if (isRepeating) {
                    session.setRepeatingBurst(
                        captureRequests,
                        captureSequence,
                        threads.camera2Handler
                    )
                } else {
                    session.captureBurst(captureRequests, captureSequence, threads.camera2Handler)
                }
            }
            captureSequence.sequenceNumber = sequenceNumber
        }

        // Invoke callbacks without holding a lock.
        captureSequence.invokeOnRequestSequenceSubmitted()
    }

    override fun toString(): String {
        return "RequestProcessor-$debugId"
    }
}

/**
 * This class packages together information about a request that was submitted to the camera.
 */
@Suppress("SyntheticAccessor") // Using an inline class generates a synthetic constructor
internal class RequestInfo(
    private val captureRequest: CaptureRequest,
    private val extraRequestParameters: Map<Metadata.Key<*>, Any?>,
    override val streams: Map<StreamId, Surface>,
    override val template: RequestTemplate,
    override val repeating: Boolean,
    override val request: Request,
    override val requestNumber: RequestNumber
) : RequestMetadata {
    override fun <T> get(key: CaptureRequest.Key<T>): T? = captureRequest[key]
    override fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T =
        get(key) ?: default

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Metadata.Key<T>): T? = extraRequestParameters[key] as T?

    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = get(key) ?: default

    override fun unwrap(): CaptureRequest = captureRequest
}

/**
 * This class responds to events from a set of one or more requests. It uses the tag field on
 * a CaptureRequest object to lookup and invoke per-request listeners so that a listener can be
 * defined on a specific request within a burst.
 */
internal class CaptureSequence(
    private val internalListeners: List<Request.Listener>,
    private val requests: Map<RequestNumber, RequestInfo>,
    private val captureRequests: List<CaptureRequest>,
    private val surfaceMap: Map<Surface, StreamId>,
    private val inFlightRequests: MutableList<CaptureSequence>,
    private val camera: CameraId
) : CameraCaptureSession.CaptureCallback() {
    private val debugId = requestSequenceDebugIds.incrementAndGet()

    @Volatile
    private var _sequenceNumber: Int? = null
    var sequenceNumber: Int
        get() {
            if (_sequenceNumber == null) {
                // If the sequence id has not been submitted, it means the call to capture or
                // setRepeating has not yet returned. The callback methods should never be synchronously
                // invoked, so the only case this should happen is if a second thread attempted to
                // invoke one of the callbacks before the initial call completed. By locking against the
                // captureSequence object here and in the capture call, we can block the callback thread
                // until the sequenceId is available.
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

    override fun onCaptureStarted(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        captureTimestamp: Long,
        captureFrameNumber: Long
    ) {
        val requestNumber = readRequestNumber(captureRequest)
        val timestamp = CameraTimestamp(captureTimestamp)
        val frameNumber = FrameNumber(captureFrameNumber)

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequest(requestNumber)

        invokeOnRequest(request) {
            it.onStarted(
                request,
                frameNumber,
                timestamp
            )
        }
    }

    override fun onCaptureProgressed(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        partialCaptureResult: CaptureResult
    ) {
        val requestNumber = readRequestNumber(captureRequest)
        val frameNumber = FrameNumber(partialCaptureResult.frameNumber)
        val frameMetadata = AndroidFrameMetadata(partialCaptureResult, camera)

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequest(requestNumber)

        invokeOnRequest(request) {
            it.onPartialCaptureResult(
                request,
                frameNumber,
                frameMetadata
            )
        }
    }

    override fun onCaptureCompleted(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        captureResult: TotalCaptureResult
    ) {
        // Remove this request from the set of requests that are currently tracked.
        synchronized(inFlightRequests) {
            inFlightRequests.remove(this)
        }

        val requestNumber = readRequestNumber(captureRequest)
        val frameNumber = FrameNumber(captureResult.frameNumber)

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequest(requestNumber)

        val frameInfo = AndroidFrameInfo(
            captureResult,
            camera,
            request
        )

        invokeOnRequest(request) {
            it.onTotalCaptureResult(
                request,
                frameNumber,
                frameInfo
            )
        }
    }

    override fun onCaptureFailed(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        captureFailure: CaptureFailure
    ) {
        // Remove this request from the set of requests that are currently tracked.
        synchronized(inFlightRequests) {
            inFlightRequests.remove(this)
        }

        val requestNumber = readRequestNumber(captureRequest)
        val frameNumber = FrameNumber(captureFailure.frameNumber)

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequest(requestNumber)

        invokeOnRequest(request) {
            it.onFailed(
                request,
                frameNumber,
                captureFailure
            )
        }
    }

    override fun onCaptureBufferLost(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        surface: Surface,
        frameId: Long
    ) {
        val requestNumber = readRequestNumber(captureRequest)
        val frameNumber = FrameNumber(frameId)
        val streamId = checkNotNull(surfaceMap[surface]) {
            "Unable to find the streamId for $surface on frame $frameNumber"
        }

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequest(requestNumber)

        invokeOnRequest(request) {
            it.onBufferLost(
                request,
                frameNumber,
                streamId
            )
        }
    }

    /**
     * Custom implementation that informs all listeners that the request had not completed when
     * abort was called.
     */
    fun invokeOnAborted() {
        invokeOnRequests { request, _, listener ->
            listener.onAborted(request.request)
        }
    }

    fun invokeOnRequestSequenceCreated() {
        invokeOnRequests { request, _, listener ->
            listener.onRequestSequenceCreated(request)
        }
    }

    fun invokeOnRequestSequenceSubmitted() {
        invokeOnRequests { request, _, listener ->
            listener.onRequestSequenceSubmitted(request)
        }
    }

    override fun onCaptureSequenceCompleted(
        captureSession: CameraCaptureSession,
        captureSequenceId: Int,
        captureFrameNumber: Long
    ) {
        check(sequenceNumber == captureSequenceId) {
            "Complete was invoked on $sequenceNumber, but the sequence was not fully submitted!"
        }
        synchronized(inFlightRequests) {
            inFlightRequests.remove(this)
        }

        val frameNumber = FrameNumber(captureFrameNumber)
        invokeOnRequests { request, _, listener ->
            listener.onRequestSequenceCompleted(request, frameNumber)
        }
    }

    override fun onCaptureSequenceAborted(
        captureSession: CameraCaptureSession,
        captureSequenceId: Int
    ) {
        check(sequenceNumber == captureSequenceId) {
            "Abort was invoked on $sequenceNumber, but the sequence was not fully submitted!"
        }

        // Remove this request from the set of requests that are currently tracked.
        synchronized(inFlightRequests) {
            inFlightRequests.remove(this)
        }

        invokeOnRequests { request, _, listener ->
            listener.onRequestSequenceAborted(request)
        }
    }

    private fun readRequestNumber(request: CaptureRequest): RequestNumber =
        checkNotNull(request.tag as RequestNumber)

    private fun readRequest(requestNumber: RequestNumber): RequestInfo {
        return checkNotNull(requests[requestNumber]) {
            "Unable to find the request for $requestNumber!"
        }
    }

    private inline fun invokeOnRequests(
        crossinline fn: (RequestMetadata, Int, Request.Listener) -> Any
    ) {

        // Always invoke the internal listener first on all of the internal listeners for the
        // entire sequence before invoking the listeners specified in the specific requests
        for (i in captureRequests.indices) {
            val requestNumber = readRequestNumber(captureRequests[i])
            val request = checkNotNull(requests[requestNumber])

            for (listenerIndex in internalListeners.indices) {
                fn(request, i, internalListeners[listenerIndex])
            }
        }

        for (i in captureRequests.indices) {
            val requestNumber = readRequestNumber(captureRequests[i])
            val request = checkNotNull(requests[requestNumber])

            for (listenerIndex in request.request.listeners.indices) {
                fn(request, i, request.request.listeners[listenerIndex])
            }
        }
    }

    private inline fun invokeOnRequest(
        request: RequestInfo,
        crossinline fn: (Request.Listener) -> Any
    ) {
        // Always invoke the internal listener first so that internal sate can be updated before
        // other listeners ask for it.
        for (i in internalListeners.indices) {
            fn(internalListeners[i])
        }

        // Invoke the listeners that were defined on this request.
        for (i in request.request.listeners.indices) {
            fn(request.request.listeners[i])
        }
    }

    override fun toString(): String = "CaptureSequence-$debugId"
}
