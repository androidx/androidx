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

import android.hardware.camera2.CaptureRequest

/**
 * Wrapper interface representing the settings applied to a specific capture request.
 *
 * Use this interface to inspect configuration parameters sent to the camera device during a capture
 * session.
 *
 * **Note:** This interface is not stable for inheritance. Implementations should not be created
 * directly by clients. For testing, use the fakes in `androidx.camera.common.testing` package (such
 * as `FakeCaptureRequest`).
 *
 * ### Example
 *
 * ```kotlin
 * val requestMetadata: CaptureRequestWrapper = ...
 * val exposureTime = requestMetadata[CaptureRequest.SENSOR_EXPOSURE_TIME]
 * ```
 */
public interface CaptureRequestWrapper : Metadata, UnsafeWrapper {
    /**
     * Retrieves the value of the specified [CaptureRequest.Key].
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    public operator fun <T> get(key: CaptureRequest.Key<T>): T?

    /**
     * Retrieves the value of the specified [CaptureRequest.Key], or returns [default] if not found.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present.
     * @return The value of the key, or [default] if null.
     */
    public fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T {
        return get(key) ?: default
    }

    /**
     * List of all [CaptureRequest.Key]s supported by this capture request.
     *
     * @see android.hardware.camera2.CaptureRequest.getKeys
     */
    public val keys: List<CaptureRequest.Key<*>>
}
