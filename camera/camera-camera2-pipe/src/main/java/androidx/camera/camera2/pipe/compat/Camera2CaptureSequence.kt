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
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.view.Surface
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.StreamId

/**
 * This class responds to events from a set of one or more requests. It uses the tag field on
 * a [CaptureRequest] object to lookup and invoke per-request listeners so that a listener can be
 * defined on a specific request within a burst.
 */
internal class Camera2CaptureSequence(
    private val internalListeners: List<Request.Listener>,
    private val requests: Map<RequestNumber, RequestInfo>,
    private val captureRequests: List<CaptureRequest>,
    private val surfaceMap: Map<Surface, StreamId>,
    private val inFlightRequests: MutableList<Camera2CaptureSequence>,
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
        // Always invoke the internal listener first so that internal state can be updated before
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