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
 * @see Metadata
 * @see UnsafeWrapper
 */
public interface CaptureResultWrapper : Metadata, UnsafeWrapper {
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
     * Retrieves the value of the specified [CaptureResult.Key].
     *
     * Use this to query standard Android camera keys, such as [CaptureResult.LENS_STATE] or
     * [CaptureResult.CONTROL_AE_STATE].
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     * @see Metadata.get to query custom metadata keys.
     */
    public operator fun <T> get(key: CaptureResult.Key<T>): T?

    /**
     * Retrieves the value of the specified [CaptureResult.Key], or returns [default] if not found.
     *
     * Retrieves the value of the specified [CaptureResult.Key], or returns [default] if the key is
     * not present or its value is null.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present or its value is null.
     * @return The value of the key, or [default] if null.
     * @see Metadata.getOrDefault to query custom metadata keys with a default value.
     */
    public fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T {
        return get(key) ?: default
    }

    /**
     * List of all [CaptureResult.Key]s supported by this capture result.
     *
     * The list of all [CaptureResult.Key]s that are supported and can be queried from this result.
     *
     * @see android.hardware.camera2.CaptureResult.getKeys
     */
    public val keys: List<CaptureResult.Key<*>>
}
