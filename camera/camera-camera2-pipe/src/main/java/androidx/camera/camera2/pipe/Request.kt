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

package androidx.camera.camera2.pipe

import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest

/**
 * An immutable package of settings and outputs needed to capture a single image from the camera
 * device. The exact set of keys and surfaces used in the CaptureRequest builder may be different
 * from what is specified in a request depending on how the request was submitted and on the
 * state of the camera.
 */
data class Request(
    val streams: List<StreamId>,
    val requestParameters: Map<CaptureRequest.Key<*>, Any> = emptyMap(),
    val extraRequestParameters: Map<Metadata.Key<*>, Any> = emptyMap(),
    val listeners: List<Listener> = emptyList(),
    val template: RequestTemplate? = null
) {

    /**
     * This listener is used to observe the state and progress of requests that are submitted to the
     * [CameraGraph] and can be attached to individual requests.
     */
    interface Listener {
        /**
         * This event indicates that the camera sensor has started exposing the frame associated
         * with this Request. The timestamp will either be the beginning or end of the sensors
         * exposure time depending on the device, and may be in a different timebase from the
         * timestamps that are returned from the underlying buffers.
         *
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureStarted
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param timestamp the android timestamp in nanos for this exposure
         */
        fun onStarted(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            timestamp: CameraTimestamp
        ) {
        }

        /**
         * This event indicates that the camera sensor has additional information about the frame
         * associated with this Request. This method may be invoked 0 or more times before the frame
         * receives onComplete.
         *
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureStarted
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param captureResult the current android capture result for this exposure
         */
        fun onPartialCaptureResult(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            captureResult: FrameMetadata
        ) {
        }

        /**
         * This event indicates that all of the metadata associated with this frame has been
         * produced. If [onPartialCaptureResult] was invoked, the values returned in
         * the totalCaptureResult map be a superset of the values produced from the
         * [onPartialCaptureResult] calls.
         *
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureStarted
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param totalCaptureResult the final android capture result for this exposure
         */
        fun onTotalCaptureResult(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            totalCaptureResult: FrameInfo
        ) {
        }

        /**
         * This is an artificial event that will be invoked after onTotalCaptureResult. This may
         * be invoked several frames after onTotalCaptureResult due to incorrect HAL implementations
         * that return metadata that get shifted several frames in the future. See b/154568653
         * for real examples of this. The actual amount of shifting and required transformations
         * may vary per device.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param result the package of metadata associated with this result.
         */
        fun onComplete(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            result: FrameInfo
        ) {
        }

        /**
         * onFailed occurs when a CaptureRequest failed in some way and the frame will not receive
         * the [onTotalCaptureResult] callback.
         *
         * Surfaces may not received images if "wasImagesCaptured" is set to false.
         *
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureFailed
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param captureFailure the android [CaptureFailure] data
         */
        fun onFailed(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            captureFailure: CaptureFailure
        ) {
        }

        /**
         * onBufferLost occurs when a CaptureRequest failed to create an image for a given output
         * stream. This method may be invoked multiple times per frame if multiple buffers were
         * lost. This method may not be invoked when an image is lost in some situations.
         *
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureBufferLost
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param stream the internal stream that will not receive a buffer for this frame.
         */
        fun onBufferLost(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            stream: StreamId
        ) {
        }

        /**
         * This is an artificial callback that will be invoked if a specific request was pending or
         * had already been submitted to when an abort was requested. The behavior of the request is
         * undefined if this method is invoked and images or metadata may or may not be produced
         * for this request. Repeating requests will not receive onAborted.
         *
         * @param request information about this specific request.
         */
        fun onAborted(request: Request) {
        }

        /**
         * Invoked after the CaptureRequest(s) have been created, but before the request is
         * submitted to the Camera. This method may be invoked multiple times if the request
         * fails to submit or if this is a repeating request.
         *
         * @param requestMetadata information about this specific request.
         */
        fun onRequestSequenceCreated(requestMetadata: RequestMetadata) {
        }

        /**
         * Invoked after the CaptureRequest(s) has been submitted. This method may be invoked
         * multiple times if the request was submitted as a repeating request.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         */
        fun onRequestSequenceSubmitted(requestMetadata: RequestMetadata) {
        }

        /**
         * Invoked by Camera2 if the request was aborted after having been submitted. This method
         * is distinct from onAborted, which is directly invoked when aborting captures.
         *
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureSequenceAborted
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         */
        fun onRequestSequenceAborted(requestMetadata: RequestMetadata) {
        }

        /**
         * Invoked by Camera2 if the request was completed after having been submitted. This method
         * is distinct from onCompleted which is invoked for each frame when used with a repeating
         * request.
         *
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureSequenceCompleted
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the final frame number of this sequence.
         */
        fun onRequestSequenceCompleted(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber
        ) {
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> Request.getUnchecked(key: Metadata.Key<T>): T? =
    this.extraRequestParameters[key] as T?

operator fun <T> Request.get(key: Metadata.Key<T>): T? = getUnchecked(key)
fun <T> Request.getOrDefault(key: Metadata.Key<T>, default: T): T = getUnchecked(key) ?: default

@Suppress("UNCHECKED_CAST")
private fun <T> Request.getUnchecked(key: CaptureRequest.Key<T>): T? =
    this.requestParameters[key] as T?

operator fun <T> Request.get(key: CaptureRequest.Key<T>): T? = getUnchecked(key)
fun <T> Request.getOrDefault(key: CaptureRequest.Key<T>, default: T): T =
    getUnchecked(key) ?: default

fun Request.formatForLogs(): String = "Request($streams)@${Integer.toHexString(hashCode())}"
