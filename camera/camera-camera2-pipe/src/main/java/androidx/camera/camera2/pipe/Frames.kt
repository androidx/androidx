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

import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.annotation.RestrictTo

/**
 * A [FrameNumber] is the identifier that represents a specific exposure by the Camera. FrameNumbers
 * increase within a specific CameraCaptureSession, and are not created until the HAL begins
 * processing a request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class FrameNumber(public val value: Long)

/** [FrameInfo] is a wrapper around [TotalCaptureResult]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface FrameInfo : UnsafeWrapper {
    public val metadata: FrameMetadata

    /**
     * If this [FrameInfo] was produced from a logical camera there will be metadata associated with
     * the physical streams that were sent to the camera.
     */
    public operator fun get(camera: CameraId): FrameMetadata?

    public val camera: CameraId
    public val frameNumber: FrameNumber
    public val requestMetadata: RequestMetadata
}

/** [FrameMetadata] is a wrapper around [CaptureResult]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface FrameMetadata : Metadata, UnsafeWrapper {
    public operator fun <T> get(key: CaptureResult.Key<T>): T?

    public fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T

    public val camera: CameraId
    public val frameNumber: FrameNumber

    /**
     * Extra metadata will override values defined by the wrapped CaptureResult object. This is
     * exposed separately to allow other systems to know what is altered relative to Camera2.
     */
    public val extraMetadata: Map<*, Any?>
}

/**
 * This defines a metadata transform that will be applied to the data produced by
 * [Request.Listener.onTotalCaptureResult]. The returned map will override the values returned by
 * TotalCaptureResult. Setting the offset and window size will cause the
 * [Request.Listener.onComplete] method to be delayed so that the transform can be run on future
 * metadata.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class MetadataTransform(
    /**
     * This defines the number of historical [TotalCaptureResult] objects this transform is allowed
     * to look at. Setting this value to > 0 increases the number of [TotalCaptureResult] the
     * [CameraGraph] will hold on to.
     */
    val past: Int = 0,

    /**
     * This defines the number of future [TotalCaptureResult] objects this transform is allowed to
     * look at. Setting this value to > 0 will cause [Request.Listener.onComplete] to be delayed by
     * the number of frames specified here.
     */
    val future: Int = 0,

    /**
     * This transform function will be invoked at high speed, and may be invoked multiple times if
     * correcting physical camera results.
     *
     * the returned values should be limited to values that will override the default values that
     * are set on the TotalCaptureResult for this frame.
     */
    val transformFn: TransformFn = object : TransformFn {}
) {
    init {
        check(past >= 0)
        check(future >= 0)
    }

    public interface TransformFn {
        public fun computeOverridesFor(
            result: FrameInfo,
            camera: CameraId,
            related: List<FrameInfo?>
        ): Map<*, Any?> = emptyMap<Any, Any?>()
    }
}
