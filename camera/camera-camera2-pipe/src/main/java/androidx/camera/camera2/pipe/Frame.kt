/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.FrameReference.Companion.acquire
import androidx.camera.camera2.pipe.media.OutputImage

/**
 * A [Frame] is a container for all of the data and outputs that are sent to, and produced from, a
 * single request issued to the camera.
 *
 * A frame represents a single "exposure" and/or moment in time. Since many modern cameras operate
 * multiple individual sub-cameras together as a larger "logical" camera, this means that the
 * outputs produced by the frame may contain outputs from more than one individual sub camera.
 *
 * Frames allow a developer to reason about the outputs from the camera without having to do all of
 * the timestamp correlation and internal error handling. In the simple case, a frame will have the
 * original request, the fully resolved [RequestMetadata] (which includes any modifications due to
 * required parameters, 3A state, etc), a unique FrameId, the camera provided timestamp, and
 * accessors for getting images when they are available. Frames are created as soon as the camera
 * indicates an exposure has started, and output images may not be immediately available.
 *
 * Since a Frame holds onto expensive objects (Images) it is very important to make sure each frame
 * is ALWAYS closed as soon as it is no longer needed. Cameras can easily operate at 30-60 frames
 * per second.
 *
 * Implementations of this interface are thread safe.
 *
 * **Warning**: All [AutoCloseable] resources, including the [Frame] itself, must be closed or it
 * will result in resource leaks and/or camera stalls!
 *
 * Example:
 * ```
 * /** Process and save the jpeg output from a Frame */
 * suspend fun processAndSaveFrame(frame: Frame): Boolean {
 *     var jpegImage: OutputImage? = null
 *     var frameMetadata: FrameMetadata? = null
 *
 *     frame.use {
 *         jpegImage = frame[jpegStreamId].await()
 *         frameInfo = frame.frameInfo.await()?.metadata
 *     } // `frame` is closed here. jpegImage is not.
 *
 *
 *     if (jpegImage == null || frameMetadata == null) {
 *         jpegImage?.close() // Always close the image.
 *         return false
 *     }
 *
 *     // save is responsible for closing jpegImage
 *     return save(jpegImage, frameMetadata)
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Frame : FrameReference, AutoCloseable {
    /**
     * Return the [FrameInfo], if available or suspend until the FrameInfo has been resolved.
     *
     * Returns null if the frameInfo could not be produced for any reason, or if the frame is
     * closed. If frameInfo is not available, [frameInfoStatus] can be used to understand why this
     * metadata is not available.
     */
    public suspend fun awaitFrameInfo(): FrameInfo?

    /**
     * Return the [FrameInfo], if available, for this Frame. This method does not block and will
     * return null if the Frame has been closed, or if the [FrameInfo] has not yet been produced.
     */
    public fun getFrameInfo(): FrameInfo?

    /**
     * Return the [OutputImage] for this [streamId], if available or suspend until the output for
     * this stream has been resolved.
     *
     * Returns null if the image could not be produced for any reason, or if this frame is closed.
     * If an image is not available, [imageStatus] can be used to understand the reason this image
     * was not produced by the camera. Each call produces a unique [OutputImage] that *must* be
     * closed to avoid memory leaks.
     */
    public suspend fun awaitImage(streamId: StreamId): OutputImage?

    /**
     * Return the [OutputImage] for this [streamId], if available.
     *
     * Returns null if the image could not be produced for any reason, or if this frame is closed.
     * If an image is not available, [imageStatus] can be used to understand the reason this image
     * was not produced by the camera. Each call produces a unique [OutputImage] that *must* be
     * closed to avoid memory leaks.
     */
    public fun getImage(streamId: StreamId): OutputImage?

    /**
     * Listener for non-coroutine based applications that may need to be notified when the state of
     * this [Frame] changes.
     */
    public fun addListener(listener: Listener)

    /** Listener for events about an [Frame] */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Listener {
        /**
         * Invoked after an [Frame] has been created and has started.
         *
         * @param frameNumber is the camera-provided identifier for this Frame.
         * @param frameTimestamp is the primary camera-provided timestamp for this Frame.
         */
        public fun onFrameStarted(frameNumber: FrameNumber, frameTimestamp: CameraTimestamp)

        /** Invoked after [FrameInfo] is available, or has failed to be produced. */
        public fun onFrameInfoAvailable()

        /** Invoked after the output for a given [StreamId] has been produced. */
        public fun onImageAvailable(streamId: StreamId)

        /**
         * Invoked after *all* outputs for this [Frame] have been produced. This method will be
         * invoked after [onImageAvailable] has been invoked for all relevant streams, and will be
         * invoked immediately after [onFrameStarted] for frames that do not produce outputs.
         */
        public fun onImagesAvailable()

        /** Invoked after the [FrameInfo] and all outputs have been completed for this [Frame]. */
        public fun onFrameComplete()
    }

    public companion object {
        public val Frame.request: Request
            get() = this.requestMetadata.request

        public val FrameReference.isFrameInfoAvailable: Boolean
            get() = this.frameInfoStatus == OutputStatus.AVAILABLE

        public fun FrameReference.isImageAvailable(streamId: StreamId): Boolean =
            this.imageStatus(streamId) == OutputStatus.AVAILABLE
    }
}

/** A [FrameId] a unique identifier that represents the order a [Frame] was produced in. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class FrameId(public val value: Long)

/** Represents the status of an output from the camera with enum-like values. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class OutputStatus internal constructor(public val value: Int) {
    public companion object {
        /** Output is not yet available. */
        public val PENDING: OutputStatus = OutputStatus(0)

        /** Output has arrived and is available. */
        public val AVAILABLE: OutputStatus = OutputStatus(1)

        /**
         * Output has been resolved, and is not available for some reason that is not due to Camera
         * operation, error, or other internal behavior. For example, if the object holding an
         * output is closed, the method to get the output may return [UNAVAILABLE].
         */
        public val UNAVAILABLE: OutputStatus = OutputStatus(2)

        /** Output is not available because the Camera reported an error for this output. */
        public val ERROR_OUTPUT_FAILED: OutputStatus = OutputStatus(10)

        /** Output is not available because it was intentionally aborted, or arrived after close. */
        public val ERROR_OUTPUT_ABORTED: OutputStatus = OutputStatus(11)

        /**
         * Output is not available because it was unexpectedly dropped or failed to arrive from the
         * camera without some other kind of explicit error.
         */
        public val ERROR_OUTPUT_MISSING: OutputStatus = OutputStatus(12)

        /**
         * Output is not available because it was intentionally dropped due to rate limiting. This
         * can happen when the configured output capacity has been exceeded. While this can happen
         * under normal usage, it can also indicate that some bit of code is not correctly closing
         * frames and/or images.
         */
        public val ERROR_OUTPUT_DROPPED: OutputStatus = OutputStatus(13)
    }
}

/**
 * A FrameCapture represents a [Request] that has been sent to the Camera, but that has not yet
 * started. This object serves as a placeholder until the Camera begins exposing the frame, at which
 * point all interactions should happen on the provided [Frame].
 *
 * Closing this FrameCapture will *not* cancel or abort the [Request].
 *
 * **Warning**: This object *must* must be closed or it will result in resource leaks and/or camera
 * stalls!
 *
 * Example:
 * ```
 * /** Capture, process, and save a jpeg from the camera.  */
 * suspend fun captureFrame(cameraGraphSession: CameraGraph.Session): Boolean {
 *     // Issue the request to the camera and return a deferred capture.
 *     val frameCapture = cameraGraphSession.capture(
 *         Request(
 *             streams = listOf(viewfinderStream, jpegStream)
 *         )
 *     )
 *
 *     // Wait for the frame to start and then pass it to `processAndSaveFrame`
 *     return frameCapture.use {
 *         val frame = frameCapture.awaitFrame() // suspend
 *         frameCapture.close() // close the frameCapture early since we have the Frame
 *         if (frame != null) {
 *             processAndSaveFrame(frame) // responsible for closing frame
 *         } else {
 *             false // capture failed
 *         }
 *     } // .use causes frameCapture to close, even if there is an exception
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface FrameCapture : AutoCloseable {
    /** The [Request] that was used to issue this [FrameCapture]. */
    public val request: Request

    /** Get the status of the pending [Frame]. */
    public val status: OutputStatus

    /**
     * Get or suspend until the [Frame] that will be produced by the camera for this [request] is
     * available, failed, or aborted, or until this object is closed.
     *
     * Invoking this multiple times will produce distinct Frame instances that will need to be
     * individually closed.
     */
    public suspend fun awaitFrame(): Frame?

    /**
     * Get the [Frame] that will was produced by the camera for this [request] or null if the
     * request failed, was aborted, or if this [FrameCapture] was closed.
     *
     * Invoking this multiple times will produce distinct Frame instances that will need to be
     * individually closed.
     */
    public fun getFrame(): Frame?

    /** Adds a [Frame.Listener] that will be invoked for each of the subsequent [Frame] events. */
    public fun addListener(listener: Frame.Listener)
}

/**
 * A FrameReference is a weak reference to a [Frame]. It will not prevent the underlying frame from
 * being closed or released unless the frame is acquired via [acquire] or [tryAcquire].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface FrameReference {
    /**
     * Metadata about the request that produced this [Frame].
     *
     * [RequestMetadata] includes any modifications to the original request that were made due to
     * 3A, Zoom, default and required parameters defined, and more.
     */
    public val requestMetadata: RequestMetadata

    /**
     * The unique, sequential identifier defined by CameraPipe for this Frame. This identifier is
     * incremented each time a new exposure starts from the Camera.
     */
    public val frameId: FrameId

    /** The original camera provided [FrameNumber] from this [Frame] */
    public val frameNumber: FrameNumber

    /** The original camera provided [CameraTimestamp] from this [Frame] */
    public val frameTimestamp: CameraTimestamp

    /** Get the current [OutputStatus] for the FrameInfo of this Frame. */
    public val frameInfoStatus: OutputStatus

    /** Get the current [OutputStatus] of the output for a given [streamId]. */
    public fun imageStatus(streamId: StreamId): OutputStatus

    /**
     * [StreamId]'s that can be used to access [OutputImage]s from this [Frame] via [Frame.getImage]
     *
     * **This may be different from the list of streams defined in the original [Request]!** since
     * this list will only include streams that were internally created and managed by CameraPipe.
     */
    public val imageStreams: Set<StreamId>

    /**
     * Acquire a reference to a [Frame] that can be independently managed or closed. A filter can be
     * provided to limit which outputs are available.
     */
    public fun tryAcquire(streamFilter: Set<StreamId>? = null): Frame?

    public companion object {
        /**
         * Acquire a [Frame] from a [FrameReference]. The outputs can be limited by specifying a
         * filter to restrict which outputs are acquired.
         */
        public fun FrameReference.acquire(streamFilter: Set<StreamId>? = null): Frame {
            return checkNotNull(tryAcquire(streamFilter)) {
                "Failed to acquire a strong reference to $this!"
            }
        }
    }
}
