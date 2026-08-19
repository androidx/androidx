/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common

import android.hardware.camera2.CaptureResult
import androidx.annotation.RestrictTo

/**
 * A wrapper interface providing compatibility-focused access to [CaptureResult] and custom
 * metadata.
 *
 * Use this interface to query the results of a single image capture, including sensor exposure,
 * lens status, and 3A states.
 *
 * Since this interface extends [Metadata], it supports querying both standard [CaptureResult.Key]s
 * and custom [Metadata.Key]s.
 *
 * To access the underlying native [CaptureResult] (if available), use the [unwrapAs] extension
 * function:
 * ```kotlin
 * val nativeResult = resultMetadata.unwrapAs<CaptureResult>()
 * ```
 *
 * **Note:** This interface is not stable for inheritance. Implementations should not be created
 * directly by clients. For testing, use the fakes in the `androidx.camera.common.testing` package,
 * such as `FakeCaptureResult`.
 *
 * ### Example
 *
 * ```kotlin
 * val resultMetadata: CaptureResultWrapper = ...
 *
 * // Querying standard CaptureResult keys
 * val lensState = resultMetadata[CaptureResult.LENS_STATE]
 *
 * // Querying custom Metadata keys
 * val customValue = resultMetadata[myCustomMetadataKey]
 * ```
 *
 * @see CaptureResultMetadata
 */
public interface CaptureResultWrapper : CaptureResultMetadata {
    /** The [CameraId] of the camera that produced this result. */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    public val cameraId: CameraId

    /**
     * The [CaptureRequestWrapper] that generated this capture result.
     *
     * This represents the request settings that were active when this result was produced.
     */
    public val captureRequest: CaptureRequestWrapper

    /**
     * The [CameraFrameNumber] associated with this result.
     *
     * The frame number is a unique, monotonically increasing identifier for the frame.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getFrameNumber")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    public val frameNumber: CameraFrameNumber

    /**
     * List of all [CaptureResult.Key]s supported by this capture result.
     *
     * The list of all [CaptureResult.Key]s that are supported and can be queried from this result.
     *
     * @see android.hardware.camera2.CaptureResult.getKeys
     */
    public val keys: List<CaptureResult.Key<*>>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public object Keys {
        /**
         * Key for [LensShadingMapWrapper].
         *
         * Use this key to query the compatibility wrapper for
         * [android.hardware.camera2.params.LensShadingMap].
         *
         * ### Example
         *
         * ```kotlin
         * val mapWrapper = captureResult[CaptureResultWrapper.Keys.LENS_SHADING_MAP]
         * ```
         *
         * Prefer using the [CaptureResultWrappers.lensShadingMap] property to access this value.
         */
        @JvmField
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val LENS_SHADING_MAP: Metadata.Key<LensShadingMapWrapper> =
            Metadata.Key("androidx.camera.common.LensShadingMap")
    }
}

/** Helper utilities for constructing and wrapping [CaptureResultWrapper] instances. */
public object CaptureResultWrappers {
    /**
     * Wraps a native [CaptureResult] into a [CaptureResultWrapper].
     *
     * @param captureResult the native [CaptureResult] to wrap.
     * @param cameraId the [CameraId] of the camera device that produced the result.
     * @param captureRequest the [CaptureRequestWrapper] representing the request that produced this
     *   result.
     * @param metadata optional map of custom [Metadata.Key] values to attach to the wrapper.
     * @return a [CaptureResultWrapper] wrapping the given [captureResult] and metadata.
     */
    @JvmStatic
    @JvmName("wrap")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmOverloads
    public fun wrap(
        captureResult: CaptureResult,
        cameraId: CameraId,
        captureRequest: CaptureRequestWrapper,
        metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    ): CaptureResultWrapper {
        return AndroidCaptureResult(captureResult, cameraId, captureRequest, metadata)
    }

    /**
     * Extension property to query [LensShadingMapWrapper] directly.
     *
     * This is the preferred way to access the [LensShadingMapWrapper] in Kotlin. It will fallback
     * to creating a wrapper from the standard
     * [CaptureResult.STATISTICS_LENS_SHADING_CORRECTION_MAP] if the compatibility key is not
     * populated, and will return null if the STATISTICS_LENS_SHADING_CORRECTION_MAP is not
     * available.
     */
    @get:JvmStatic
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val CaptureResultMetadata.lensShadingMap: LensShadingMapWrapper?
        get() {
            val compatibilityMap = this[CaptureResultWrapper.Keys.LENS_SHADING_MAP]
            if (compatibilityMap != null) {
                return compatibilityMap
            }
            val camera2Map = this[CaptureResult.STATISTICS_LENS_SHADING_CORRECTION_MAP]
            if (camera2Map != null) {
                return AndroidLensShadingMap(camera2Map)
            }
            return null
        }
}
