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
import androidx.camera.camera2.pipe.CaptureSequence
import androidx.camera.camera2.pipe.CaptureSequences.invokeOnRequest
import androidx.camera.camera2.pipe.CaptureSequences.invokeOnRequests
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.SensorTimestamp
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Debug
import kotlinx.coroutines.CompletableDeferred

/**
 * This class responds to events from a set of one or more requests. It uses the tag field on a
 * [CaptureRequest] object to lookup and invoke per-request listeners so that a listener can be
 * defined on a specific request within a burst.
 */
internal class Camera2CaptureSequence(
    override val cameraId: CameraId,
    override val repeating: Boolean,
    override val captureRequestList: List<CaptureRequest>,
    override val captureMetadataList: List<RequestMetadata>,
    override val listeners: List<Request.Listener>,
    override val sequenceListener: CaptureSequence.CaptureSequenceListener,
    private val surfaceMap: Map<Surface, StreamId>,
) :
    Camera2CaptureCallback,
    CameraCaptureSession.CaptureCallback(),
    CaptureSequence<CaptureRequest> {
    private val debugId = captureSequenceDebugIds.incrementAndGet()
    private val hasStarted = CompletableDeferred<Unit>()

    init {
        check(captureRequestList.size == captureMetadataList.size) {
            "CaptureRequestList and CaptureMetadataList must have a 1:1 mapping."
        }
    }

    @Volatile private var _sequenceNumber: Int? = null
    override var sequenceNumber: Int
        get() {
            if (_sequenceNumber == null) {
                // If the sequence id has not been submitted, it means the call to capture or
                // setRepeating has not yet returned. The callback methods should never be
                // synchronously invoked, so the only case this should happen is if a second thread
                // attempted to invoke one of the callbacks before the initial call completed. By
                // locking against the captureSequence object here and in the capture call, we can
                // block the callback thread until the sequenceId is available.
                synchronized(this) {
                    return checkNotNull(_sequenceNumber) {
                        "SequenceNumber has not been set for $this!"
                    }
                }
            }
            return checkNotNull(_sequenceNumber) { "SequenceNumber has not been set for $this!" }
        }
        set(value) {
            _sequenceNumber = value
        }

    override fun onCaptureStarted(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        captureTimestamp: Long,
        captureFrameNumber: Long
    ) = onCaptureStarted(captureRequest, captureFrameNumber, captureTimestamp)

    override fun onCaptureStarted(
        captureRequest: CaptureRequest,
        captureFrameNumber: Long,
        captureTimestamp: Long
    ) {
        Debug.traceStart { "onCaptureStarted" }
        val timestamp = CameraTimestamp(captureTimestamp)
        val frameNumber = FrameNumber(captureFrameNumber)

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequestMetadata(captureRequest)

        hasStarted.complete(Unit)

        invokeOnRequest(request) { it.onStarted(request, frameNumber, timestamp) }
        Debug.traceStop()
    }

    override fun onCaptureProgressed(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        partialCaptureResult: CaptureResult
    ) = onCaptureProgressed(captureRequest, partialCaptureResult)

    override fun onCaptureProgressed(
        captureRequest: CaptureRequest,
        partialCaptureResult: CaptureResult
    ) {
        Debug.traceStart { "onCaptureProgressed" }
        val frameNumber = FrameNumber(partialCaptureResult.frameNumber)
        val frameMetadata = AndroidFrameMetadata(partialCaptureResult, cameraId)

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequestMetadata(captureRequest)

        invokeOnRequest(request) { it.onPartialCaptureResult(request, frameNumber, frameMetadata) }
        Debug.traceStop()
    }

    override fun onReadoutStarted(
        session: CameraCaptureSession,
        captureRequest: CaptureRequest,
        captureTimestamp: Long,
        captureFrameNumber: Long
    ) {
        Debug.traceStart { "onReadoutStarted" }
        val readoutTimestamp = SensorTimestamp(captureTimestamp)
        val frameNumber = FrameNumber(captureFrameNumber)

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequestMetadata(captureRequest)

        invokeOnRequest(request) { it.onReadoutStarted(request, frameNumber, readoutTimestamp) }
        Debug.traceStop()
    }

    override fun onCaptureCompleted(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        captureResult: TotalCaptureResult
    ) = onCaptureCompleted(captureRequest, captureResult, FrameNumber(captureResult.frameNumber))

    override fun onCaptureCompleted(
        captureRequest: CaptureRequest,
        captureResult: TotalCaptureResult,
        frameNumber: FrameNumber
    ) {
        Debug.traceStart { "onCaptureCompleted" }
        Debug.traceStart { "onCaptureSequenceComplete" }
        sequenceListener.onCaptureSequenceComplete(this)
        Debug.traceStop() // onCaptureSequenceComplete

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequestMetadata(captureRequest)

        val frameInfo = AndroidFrameInfo(captureResult, cameraId, request)

        Debug.traceStart { "onTotalCaptureResult" }
        invokeOnRequest(request) { it.onTotalCaptureResult(request, frameNumber, frameInfo) }
        Debug.traceStop() // onTotalCaptureResult

        // TODO: Implement a proper mechanism to delay the firing of onComplete(). See
        // androidx.camera.camera2.pipe.Request.Listener for context.
        Debug.traceStart { "onComplete" }
        invokeOnRequest(request) { it.onComplete(request, frameNumber, frameInfo) }
        Debug.traceStop() // onComplete
        Debug.traceStop() // onCaptureCompleted
    }

    override fun onCaptureProcessProgressed(captureRequest: CaptureRequest, progress: Int) {
        Debug.traceStart { "onCaptureProcessProgressed" }
        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequestMetadata(captureRequest)
        invokeOnRequest(request) { it.onCaptureProgress(request, progress) }
        Debug.traceStop()
    }

    override fun onCaptureFailed(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        captureFailure: CaptureFailure
    ) {
        Debug.traceStart { "onCaptureFailed" }
        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequestMetadata(captureRequest)

        val androidCaptureFailure = AndroidCaptureFailure(request, captureFailure)

        invokeCaptureFailure(
            request,
            FrameNumber(captureFailure.frameNumber),
            androidCaptureFailure
        )
        Debug.traceStop() // onCaptureFailed
    }

    private fun invokeCaptureFailure(
        request: RequestMetadata,
        frameNumber: FrameNumber,
        requestFailure: RequestFailure
    ) {
        sequenceListener.onCaptureSequenceComplete(this)
        invokeOnRequest(request) { it.onFailed(request, frameNumber, requestFailure) }
    }

    override fun onCaptureFailed(captureRequest: CaptureRequest, frameNumber: FrameNumber) {
        Debug.traceStart { "onCaptureFailed" }
        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val requestMetadata = readRequestMetadata(captureRequest)

        val extensionRequestFailure =
            ExtensionRequestFailure(
                requestMetadata,
                false,
                frameNumber,
                CaptureFailure.REASON_ERROR
            )

        invokeCaptureFailure(requestMetadata, frameNumber, extensionRequestFailure)
        Debug.traceStop() // onCaptureFailed
    }

    override fun onCaptureBufferLost(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        surface: Surface,
        frameId: Long
    ) {
        Debug.traceStart { "onCaptureBufferLost" }
        val frameNumber = FrameNumber(frameId)
        val streamId =
            checkNotNull(surfaceMap[surface]) {
                "Unable to find the streamId for $surface on frame $frameNumber"
            }

        // Load the request and throw if we are not able to find an associated request. Under
        // normal circumstances this should never happen.
        val request = readRequestMetadata(captureRequest)

        invokeOnRequest(request) { it.onBufferLost(request, frameNumber, streamId) }
        Debug.traceStop() // onCaptureBufferLost
    }

    override fun onCaptureSequenceCompleted(
        captureSession: CameraCaptureSession,
        captureSequenceId: Int,
        captureFrameNumber: Long
    ) = onCaptureSequenceCompleted(captureSequenceId, captureFrameNumber)

    override fun onCaptureSequenceCompleted(captureSequenceId: Int, captureFrameNumber: Long) {
        Debug.traceStart { "onCaptureSequenceCompleted" }
        sequenceListener.onCaptureSequenceComplete(this)

        check(sequenceNumber == captureSequenceId) {
            "onCaptureSequenceCompleted was invoked on $sequenceNumber, but expected " +
                "$captureSequenceId!"
        }

        val frameNumber = FrameNumber(captureFrameNumber)
        invokeOnRequests { request, _, listener ->
            listener.onRequestSequenceCompleted(request, frameNumber)
        }
        Debug.traceStop() // onCaptureSequenceCompleted
    }

    override fun onCaptureSequenceAborted(
        captureSession: CameraCaptureSession,
        captureSequenceId: Int
    ) = onCaptureSequenceAborted(captureSequenceId)

    override fun onCaptureSequenceAborted(captureSequenceId: Int) {
        Debug.traceStart { "onCaptureSequenceAborted" }
        sequenceListener.onCaptureSequenceComplete(this)

        check(sequenceNumber == captureSequenceId) {
            "onCaptureSequenceAborted was invoked on $sequenceNumber, but expected " +
                "$captureSequenceId!"
        }

        hasStarted.complete(Unit)
        invokeOnRequests { request, _, listener -> listener.onRequestSequenceAborted(request) }
        Debug.traceStop() // onCaptureSequenceAborted
    }

    private fun readRequestMetadata(captureRequest: CaptureRequest): RequestMetadata {
        // This loop assumes that the CaptureRequest's and RequestMetadata object in
        // captureRequestList and captureMetadataList are mapped 1:1
        for (i in captureRequestList.indices) {
            // Compare by reference. Camera2 maintains the exact CaptureRequest object in callbacks
            // since the beginning of Camera2. However, the equals method does not differentiate
            // between similar requests, and doing reference comparison avoids intermingling events.
            if (captureRequestList[i] === captureRequest) {
                return captureMetadataList[i]
            }
        }

        throw IllegalArgumentException(
            "Failed to find CaptureRequest $captureRequest in $captureRequestList"
        )
    }

    internal suspend fun awaitStarted() = hasStarted.await()

    override fun toString(): String = "Camera2CaptureSequence-$debugId"
}
