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
 * Wrapper interface providing compatibility-focused access to [CaptureResult].
 *
 * Use this interface to query the results of a single image capture, including sensor exposure,
 * lens status, and 3A states.
 *
 * **Note:** This interface is not stable for inheritance. Implementations should not be created
 * directly by clients. For testing, use the fakes in `androidx.camera.common.testing` package (such
 * as `FakeCaptureResult`).
 *
 * ### Example
 *
 * ```kotlin
 * val resultMetadata: CaptureResultWrapper = ...
 * val lensState = resultMetadata[CaptureResult.LENS_STATE]
 * ```
 */
public interface CaptureResultWrapper : Metadata, UnsafeWrapper {
    /** The [CameraId] of the camera that produced this result. */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    public val cameraId: CameraId

    /** The [CaptureRequestWrapper] that generated this capture result. */
    public val captureRequest: CaptureRequestWrapper

    /** The [CameraFrameNumber] associated with this result. */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getFrameNumber")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    public val frameNumber: CameraFrameNumber

    /**
     * Retrieves the value of the specified [CaptureResult.Key].
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    public operator fun <T> get(key: CaptureResult.Key<T>): T?

    /**
     * Retrieves the value of the specified [CaptureResult.Key], or returns [default] if not found.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present.
     * @return The value of the key, or [default] if null.
     */
    public fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T {
        return get(key) ?: default
    }

    /**
     * List of all [CaptureResult.Key]s supported by this capture result.
     *
     * @see android.hardware.camera2.CaptureResult.getKeys
     */
    public val keys: List<CaptureResult.Key<*>>
}
