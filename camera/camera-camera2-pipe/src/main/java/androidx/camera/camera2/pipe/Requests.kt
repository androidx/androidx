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

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.media.ImageWrapper

/**
 * A [RequestNumber] is an artificial identifier that is created for each request that is submitted
 * to the Camera.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RequestNumber(public val value: Long)

/**
 * A [Request] is an immutable package of outputs and parameters needed to issue a [CaptureRequest]
 * to a Camera2 [CameraCaptureSession].
 *
 * [Request] objects are handled by camera2 via the [RequestProcessor] interface, and will translate
 * each [Request] object into a corresponding [CaptureRequest] object using the active
 * [CameraDevice], [CameraCaptureSession], and [CameraGraph.Config]. Requests may be queued up and
 * submitted after a delay, or reused (in the case of repeating requests) if the
 * [CameraCaptureSession] is reconfigured or recreated.
 *
 * Depending on the [CameraGraph.Config], it is possible that not all parameters that are set on the
 * [Request] will be honored when a [Request] is sent to the camera. Specifically, Camera2
 * parameters related to 3A State and any required parameters specified on the [CameraGraph.Config]
 * will override parameters specified in a [Request]
 *
 * @param streams The list of streams to submit. Each request *must* have 1 or more valid streams.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Request(
    public val streams: List<StreamId>,
    public val parameters: Map<CaptureRequest.Key<*>, Any> = emptyMap(),
    public val extras: Map<Metadata.Key<*>, Any> = emptyMap(),
    public val listeners: List<Listener> = emptyList(),
    public val template: RequestTemplate? = null,
    public val inputRequest: InputRequest? = null
) {
    public operator fun <T> get(key: CaptureRequest.Key<T>): T? = getUnchecked(key)

    public operator fun <T> get(key: Metadata.Key<T>): T? = getUnchecked(key)

    /**
     * This listener is used to observe the state and progress of a [Request] that has been issued
     * to the [CameraGraph]. Listeners will be invoked on background threads at high speed, and
     * should avoid blocking work or accessing synchronized resources if possible. [Listener]s used
     * in a repeating request may be issued multiple times within the same session, and should not
     * rely on [onRequestSequenceSubmitted] from being invoked only once.
     */
    @JvmDefaultWithCompatibility
    public interface Listener {
        /**
         * This event indicates that the camera sensor has started exposing the frame associated
         * with this Request. The timestamp will either be the beginning or end of the sensors
         * exposure time depending on the device, and may be in a different timebase from the
         * timestamps that are returned from the underlying buffers.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param timestamp the android timestamp in nanos for this exposure
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureStarted
         */
        public fun onStarted(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            timestamp: CameraTimestamp
        ) {}

        /**
         * This event indicates that the camera sensor has additional information about the frame
         * associated with this Request. This method may be invoked 0 or more times before the frame
         * receives onComplete.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param captureResult the current android capture result for this exposure
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureStarted
         */
        public fun onPartialCaptureResult(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            captureResult: FrameMetadata
        ) {}

        /**
         * This event provides clients with an estimate of the post-processing progress of a capture
         * which could take significantly more time relative to the rest of the
         * [CameraExtensionSession.capture] sequence. The callback will be triggered only by
         * extensions that return true from calls
         * [CameraExtensionCharacteristics.isCaptureProcessProgressAvailable]. If support for this
         * callback is present, then clients will be notified at least once with progress value 100.
         * The callback will be triggered only for still capture requests
         * [CameraExtensionSession.capture] and is not supported for repeating requests
         * [CameraExtensionSession.setRepeatingRequest].
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param progress the value indicating the current post-processing progress (between 0 and
         *   100 inclusive)
         * @see
         *   android.hardware.camera2.CameraExtensionSession.ExtensionCaptureCallback.onCaptureProcessProgressed
         */
        public fun onCaptureProgress(requestMetadata: RequestMetadata, progress: Int) {}

        /**
         * This event indicates that all of the metadata associated with this frame has been
         * produced. If [onPartialCaptureResult] was invoked, the values returned in the
         * totalCaptureResult map be a superset of the values produced from the
         * [onPartialCaptureResult] calls.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param totalCaptureResult the final android capture result for this exposure
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureStarted
         */
        public fun onTotalCaptureResult(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            totalCaptureResult: FrameInfo
        ) {}

        /**
         * This is an artificial event that will be invoked after onTotalCaptureResult. This may be
         * invoked several frames after onTotalCaptureResult due to incorrect HAL implementations
         * that return metadata that get shifted several frames in the future. See b/154568653 for
         * real examples of this. The actual amount of shifting and required transformations may
         * vary per device.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param result the package of metadata associated with this result.
         */
        public fun onComplete(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            result: FrameInfo
        ) {}

        /**
         * onFailed occurs when a CaptureRequest failed in some way and the frame will not receive
         * the [onTotalCaptureResult] callback.
         *
         * Surfaces may not received images if "wasImagesCaptured" is set to false.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param requestFailure the android [RequestFailure] data wrapper
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureFailed
         */
        public fun onFailed(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            requestFailure: RequestFailure
        ) {}

        /**
         * onReadoutStarted occurs when the camera device has started reading out the output image
         * for the request, at the beginning of the sensor image readout. Concretely, it is invoked
         * right after onCaptureStarted.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this capture.
         * @param timestamp the android timestamp in nanos at the start of camera data readout.
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onReadoutStarted
         */
        public fun onReadoutStarted(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            timestamp: SensorTimestamp
        ) {}

        /**
         * onBufferLost occurs when a CaptureRequest failed to create an image for a given output
         * stream. This method may be invoked multiple times per frame if multiple buffers were
         * lost. This method may not be invoked when an image is lost in some situations.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the android frame number for this exposure
         * @param stream the internal stream that will not receive a buffer for this frame.
         * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureBufferLost
         */
        public fun onBufferLost(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            stream: StreamId
        ) {}

        /**
         * This is an artificial callback that will be invoked if a specific request was pending or
         * had already been submitted to when an abort was requested. The behavior of the request is
         * undefined if this method is invoked and images or metadata may or may not be produced for
         * this request. Repeating requests will not receive onAborted. Failed reprocessing requests
         * will be aborted and removed from the queue.
         *
         * @param request information about this specific request.
         */
        public fun onAborted(request: Request) {}

        /**
         * Invoked after the CaptureRequest(s) have been created, but before the request is
         * submitted to the Camera. This method may be invoked multiple times if the request fails
         * to submit or if this is a repeating request.
         *
         * @param requestMetadata information about this specific request.
         */
        public fun onRequestSequenceCreated(requestMetadata: RequestMetadata) {}

        /**
         * Invoked after the CaptureRequest(s) has been submitted. This method may be invoked
         * multiple times if the request was submitted as a repeating request.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         */
        public fun onRequestSequenceSubmitted(requestMetadata: RequestMetadata) {}

        /**
         * Invoked by Camera2 if the request was aborted after having been submitted. This method is
         * distinct from onAborted, which is directly invoked when aborting captures.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @see
         *   android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureSequenceAborted
         */
        public fun onRequestSequenceAborted(requestMetadata: RequestMetadata) {}

        /**
         * Invoked by Camera2 if the request was completed after having been submitted. This method
         * is distinct from onCompleted which is invoked for each frame when used with a repeating
         * request.
         *
         * @param requestMetadata the data about the camera2 request that was sent to the camera.
         * @param frameNumber the final frame number of this sequence.
         * @see
         *   android.hardware.camera2.CameraCaptureSession.CaptureCallback.onCaptureSequenceCompleted
         */
        public fun onRequestSequenceCompleted(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber
        ) {}
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getUnchecked(key: Metadata.Key<T>): T? = this.extras[key] as T?

    @Suppress("UNCHECKED_CAST")
    private fun <T> getUnchecked(key: CaptureRequest.Key<T>): T? = this.parameters[key] as T?

    override fun toString(): String {
        val parametersString =
            if (parameters.isEmpty()) {
                ""
            } else {
                ", parameters=${Debug.formatParameterMap(parameters, limit = 5)}"
            }
        val extrasString =
            if (extras.isEmpty()) "" else ", extras=${Debug.formatParameterMap(extras, limit = 5)}"
        val templateString = if (template == null) "" else ", template=$template"
        // Ignore listener count, always include stream list (required), and use super.toString to
        // reference the class name.
        return "Request(streams=$streams$templateString$parametersString$extrasString)"
    }
}

/**
 * Interface wrapper for [CaptureFailure].
 *
 * This interface should be used instead of [CaptureFailure] because its package-private constructor
 * prevents directly creating an instance of it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RequestFailure : UnsafeWrapper {
    /** Metadata about the request that has failed. */
    public val requestMetadata: RequestMetadata

    /** The Camera [FrameNumber] for the request that has failed. */
    public val frameNumber: FrameNumber

    /** Indicates the reason the particular request failed, see [CaptureFailure] for details. */
    public val reason: Int

    /**
     * Indicates if images were still captured for this request. If this is true, the camera should
     * invoke [Request.Listener.onBufferLost] individually for each output that failed. If this is
     * false, these outputs will never arrive, and the individual callbacks will not be invoked.
     */
    public val wasImageCaptured: Boolean
}

/**
 * A [RequestTemplate] indicates which preset set list of parameters will be applied to a request by
 * default. These values are defined by camera2.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RequestTemplate(public val value: Int) {
    public val name: String
        get() {
            return when (value) {
                1 -> "TEMPLATE_PREVIEW"
                2 -> "TEMPLATE_STILL_CAPTURE"
                3 -> "TEMPLATE_RECORD"
                4 -> "TEMPLATE_VIDEO_SNAPSHOT"
                5 -> "TEMPLATE_ZERO_SHUTTER_LAG"
                6 -> "TEMPLATE_MANUAL"
                else -> "UNKNOWN-$value"
            }
        }
}

/**
 * The intended use for this class is to submit the input needed for a reprocessing request, the
 * [ImageWrapper] and [FrameInfo]. Both values are non-nullable because both values are needed for
 * reprocessing.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class InputRequest(val image: ImageWrapper, val frameInfo: FrameInfo)

/**
 * RequestMetadata wraps together all of the information about a specific CaptureRequest that was
 * submitted to Camera2.
 *
 * <p> This class is distinct from [Request] which is used to configure and issue a request to the
 * [CameraGraph]. This class will report the actual keys / values that were sent to camera2 (if
 * different) from the request that was used to create the Camera2 [CaptureRequest].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RequestMetadata : Metadata, UnsafeWrapper {
    public operator fun <T> get(key: CaptureRequest.Key<T>): T?

    public fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T

    /** The actual Camera2 template that was used when creating this [CaptureRequest] */
    public val template: RequestTemplate

    /**
     * A Map of StreamId(s) that were submitted with this CaptureRequest and the Surface(s) used for
     * this request. It's possible that not all of the streamId's specified in the [Request] are
     * present in the [CaptureRequest].
     */
    public val streams: Map<StreamId, Surface>

    /** Returns true if this is used in a repeating request. */
    public val repeating: Boolean

    /** The request object that was used to create this [CaptureRequest] */
    public val request: Request

    /** An internal number used to identify a specific [CaptureRequest] */
    public val requestNumber: RequestNumber
}

/**
 * This is a timestamp from the Camera, and corresponds to the nanosecond exposure time of a Frame.
 * While the value is expressed in nano-seconds, the precision may be much lower. In addition, the
 * time-base of the Camera is undefined, although it's common for it to be in either Monotonic or
 * Realtime.
 *
 * <p> Timestamp may differ from timestamps that are obtained from other parts of the Camera and
 * media systems within the same device. For example, it's common for high frequency sensors to
 * operate based on a real-time clock, while audio/visual systems commonly operate based on a
 * monotonic clock.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class CameraTimestamp(public val value: Long)

/**
 * This is a timestamp happen at start of readout for a regular request, or the timestamp at the
 * input image's start of readout for a reprocess request, in nanoseconds.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class SensorTimestamp(public val value: Long)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Request.getOrDefault(key: Metadata.Key<T>, default: T): T = this[key] ?: default

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Request.getOrDefault(key: CaptureRequest.Key<T>, default: T): T =
    this[key] ?: default

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Request.formatForLogs(): String = "Request($streams)@${Integer.toHexString(hashCode())}"

/** Utility function to help deal with the unsafe nature of the typed Key/Value pairs. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun CaptureRequest.Builder.writeParameters(parameters: Map<*, Any?>) {
    for ((key, value) in parameters) {
        writeParameter(key, value)
    }
}

/** Utility function to help deal with the unsafe nature of the typed Key/Value pairs. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun CaptureRequest.Builder.writeParameter(key: Any?, value: Any?) {
    if (key != null && key is CaptureRequest.Key<*>) {
        try {
            @Suppress("UNCHECKED_CAST") this.set(key as CaptureRequest.Key<Any>, value)
        } catch (e: IllegalArgumentException) {
            // Setting keys on CaptureRequest.Builder can fail if the key is defined on some
            // OS versions, but not on others. Log and ignore these kinds of failures.
            //
            // See b/309518353 for an example failure.
            Log.warn(e) { "Failed to set [${key.name}: $value] on CaptureRequest.Builder" }
        }
    }
}

/**
 * Utility function to put all metadata in the current map through an unchecked cast. The unchecked
 * cast is necessary since CameraGraph.Config uses Map<*, Any?> as the standard type for parameters.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun MutableMap<Any, Any?>.putAllMetadata(metadata: Map<*, Any?>) {
    @Suppress("UNCHECKED_CAST") this.putAll(metadata as Map<Any, Any?>)
}
