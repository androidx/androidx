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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.view.Surface
import androidx.annotation.RequiresApi

/**
 * A map-like interface used to describe or interact with metadata from CameraPipe and Camera2.
 *
 * These interfaces are designed to wrap native camera2 metadata objects in a way that allows
 * additional values to be passed back internally computed values, state, or control values.
 *
 * These interfaces are read-only.
 */
public interface Metadata {
    public operator fun <T> get(key: Key<T>): T?
    public fun <T> getOrDefault(key: Key<T>, default: T): T

    /**
     * Metadata keys provide values or controls that are provided or computed by CameraPipe.
     */
    public class Key<T> private constructor(private val name: String) {
        public companion object {
            @JvmStatic
            internal val keys: MutableSet<String> = HashSet()

            /**
             * This will create a new Key instance, and will check to see that the key has not been
             * previously created somewhere else.
             */
            public fun <T> create(name: String): Key<T> {
                synchronized(keys) {
                    check(keys.add(name)) { "$name is already defined!" }
                }
                return Key(name)
            }
        }

        override fun toString(): String {
            return name
        }
    }
}

/**
 * RequestMetadata wraps together all of the information about a specific CaptureRequest that was
 * submitted to Camera2.
 *
 * <p> This class is distinct from [Request] which is used to configure and issue a request to the
 * [CameraGraph]. This class will report the actual keys / values that were sent to camera2 (if
 * different) from the request that was used to create the Camera2 [CaptureRequest].
 */
public interface RequestMetadata : Metadata, UnsafeWrapper<CaptureRequest> {
    public operator fun <T> get(key: CaptureRequest.Key<T>): T?
    public fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T

    /** The actual Camera2 template that was used when creating this [CaptureRequest] */
    public val template: RequestTemplate

    /**
     * A Map of StreamId(s) that were submitted with this CaptureRequest and the Surface(s) used
     * for this request. It's possible that not all of the streamId's specified in the [Request]
     * are present in the [CaptureRequest].
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
 * [FrameInfo] is a wrapper around [TotalCaptureResult].
 */
public interface FrameInfo : UnsafeWrapper<TotalCaptureResult> {
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

/**
 * [FrameMetadata] is a wrapper around [CaptureResult].
 */
public interface FrameMetadata : Metadata, UnsafeWrapper<CaptureResult> {
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
public data class MetadataTransform(
    /**
     * This defines the number of historical [TotalCaptureResult] objects this transform is
     * allowed to look at. Setting this value to > 0 increases the number of [TotalCaptureResult]
     * the [CameraGraph] will hold on to.
     */
    val past: Int = 0,

    /**
     * This defines the number of future [TotalCaptureResult] objects this transform is allowed to
     * look at. Setting this value to > 0 will cause [Request.Listener.onComplete] to be delayed
     * by the number of frames specified here.
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

/**
 * A [RequestTemplate] indicates which preset set list of parameters will be applied to a request by
 * default. These values are defined by camera2.
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
public inline class RequestTemplate(public val value: Int)

/**
 * A [RequestNumber] is an artificial identifier that is created for each request that is submitted
 * to the Camera.
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
public inline class RequestNumber(public val value: Long)

/**
 * A [FrameNumber] is the identifier that represents a specific exposure by the Camera. FrameNumbers
 * increase within a specific CameraCaptureSession, and are not created until the HAL begins
 * processing a request.
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
public inline class FrameNumber(public val value: Long)

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
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
public inline class CameraTimestamp(public val value: Long)

/**
 * Utility function to help deal with the unsafe nature of the typed Key/Value pairs.
 */
public fun CaptureRequest.Builder.writeParameters(parameters: Map<*, Any?>) {
    for ((key, value) in parameters) {
        writeParameter(key, value)
    }
}

/**
 * Utility function to help deal with the unsafe nature of the typed Key/Value pairs.
 */
public fun CaptureRequest.Builder.writeParameter(key: Any?, value: Any?) {
    if (key != null && key is CaptureRequest.Key<*>) {
        @Suppress("UNCHECKED_CAST")
        this.set(key as CaptureRequest.Key<Any>, value)
    }
}
