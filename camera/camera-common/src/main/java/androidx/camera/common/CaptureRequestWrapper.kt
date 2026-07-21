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
 * A wrapper interface representing the settings applied to a specific camera capture request.
 *
 * This interface provides a unified way to inspect the configuration parameters sent to the camera
 * device during a capture session. It supports querying:
 * 1. Native Camera2 capture request keys via [CaptureRequest.Key] (using [get]).
 * 2. Extension or custom metadata keys via [Metadata.Key] (using [Metadata.get], inherited from
 *    [Metadata]).
 *
 * Additionally, if access to the underlying native Android [CaptureRequest] is required, this
 * interface can be unwrapped using [unwrapAs] (inherited from [UnsafeWrapper]).
 *
 * ### Example Usage
 *
 * #### Querying a native Camera2 CaptureRequest key:
 * ```kotlin
 * val requestWrapper: CaptureRequestWrapper = ...
 * val exposureTime: Long? = requestWrapper[CaptureRequest.SENSOR_EXPOSURE_TIME]
 * ```
 *
 * #### Querying a custom metadata key:
 * ```kotlin
 * val requestWrapper: CaptureRequestWrapper = ...
 * val customExtensionKey = Metadata.Key<Int>("androidx.camera.custom_extension")
 * val customValue: Int? = requestWrapper[customExtensionKey]
 * ```
 *
 * #### Unwrapping the underlying native CaptureRequest:
 * ```kotlin
 * val requestWrapper: CaptureRequestWrapper = ...
 * val nativeRequest: CaptureRequest? = requestWrapper.unwrapAs<CaptureRequest>()
 * ```
 *
 * **Note:** This interface is not stable for inheritance. Implementations should not be created
 * directly by clients. For testing, use the fakes in the `androidx.camera.common.testing` package
 * (such as `FakeCaptureRequest`).
 *
 * @see Metadata
 * @see UnsafeWrapper
 */
public interface CaptureRequestWrapper : Metadata, UnsafeWrapper {
    /**
     * Retrieves the value of the specified [CaptureRequest.Key].
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     * @see android.hardware.camera2.CaptureRequest.get
     */
    public operator fun <T> get(key: CaptureRequest.Key<T>): T?

    /**
     * Retrieves the value of the specified [CaptureRequest.Key], or returns [default] if the key is
     * not present or its value is `null`.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present or is `null`.
     * @return The value of the key, or [default] if the key is not present or its value is `null`.
     */
    public fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T {
        return get(key) ?: default
    }

    /**
     * The list of all [CaptureRequest.Key]s that are set or supported by this capture request.
     *
     * @see android.hardware.camera2.CaptureRequest.getKeys
     */
    public val keys: List<CaptureRequest.Key<*>>
}
