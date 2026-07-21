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

package androidx.camera.common.testing

import android.hardware.camera2.CaptureRequest
import androidx.camera.common.CaptureRequestWrapper
import androidx.camera.common.Metadata
import androidx.camera.common.getUnchecked
import java.lang.Class

/**
 * A fake implementation of [CaptureRequestWrapper] designed for unit testing.
 *
 * This class allows tests to mock camera settings by configuring mock values for both native
 * [CaptureRequest.Key]s and custom [Metadata.Key]s.
 *
 * ### Kotlin Example
 *
 * ```kotlin
 * val fakeRequest = FakeCaptureRequest(
 *     requestParameters = mapOf(CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON),
 *     requestMetadata = mapOf(customMetadataKey to "custom_value")
 * )
 *
 * // Querying values
 * val aeMode = fakeRequest[CaptureRequest.CONTROL_AE_MODE] // Returns CONTROL_AE_MODE_ON
 * ```
 *
 * ### Java Example
 *
 * ```java
 * Map<CaptureRequest.Key<?>, Object> parameters = new HashMap<>();
 * parameters.put(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
 *
 * FakeCaptureRequest fakeRequest = FakeCaptureRequest.create(parameters, Collections.emptyMap());
 * ```
 *
 * ### Behavior of [unwrapAs]
 * Because `FakeCaptureRequest` does not wrap a real, native [CaptureRequest] object, calling
 * `unwrapAs(CaptureRequest::class.java)` will return `null`. It only supports unwrapping to
 * [FakeCaptureRequest] itself or its supertypes.
 */
public class FakeCaptureRequest
private constructor(
    private val requestParameters: Map<CaptureRequest.Key<*>, Any?>,
    private val requestMetadata: Map<Metadata.Key<*>, Any?>,
) : CaptureRequestWrapper {

    /** The set of custom [Metadata.Key]s that have configured mock values in this fake request. */
    override val metadataKeys: Set<Metadata.Key<*>>
        get() = requestMetadata.keys

    /** The list of [CaptureRequest.Key]s that have configured mock values in this fake request. */
    override val keys: List<CaptureRequest.Key<*>>
        get() = requestParameters.keys.toList()

    /**
     * Retrieves the mock value configured for the specified [Metadata.Key].
     *
     * @param key The custom metadata key to query.
     * @return The configured mock value, or `null` if no value was configured for the key.
     */
    override fun <T> get(key: Metadata.Key<T>): T? = requestMetadata.getUnchecked(key)

    /**
     * Retrieves the mock value configured for the specified [CaptureRequest.Key].
     *
     * @param key The native capture request key to query.
     * @return The configured mock value, or `null` if no value was configured for the key.
     */
    override fun <T> get(key: CaptureRequest.Key<T>): T? = requestParameters.getUnchecked(key)

    /**
     * Unwraps this object to the specified type.
     *
     * Since this is a fake implementation and does not wrap a native Android [CaptureRequest]
     * object, this method will return `null` if [type] is [CaptureRequest]. It only returns `this`
     * (cast to [T]) if [type] is compatible with [FakeCaptureRequest].
     *
     * @param type The class representing the target type.
     * @return This instance if compatible with [type], otherwise `null`.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            else -> null
        }

    public companion object {
        /**
         * Creates a [FakeCaptureRequest] instance.
         *
         * This allows constructor-like syntax in Kotlin: `FakeCaptureRequest(...)`.
         *
         * @param requestParameters The map of capture request keys to their mock values. Defaults
         *   to an empty map.
         * @param requestMetadata The map of custom metadata keys to their mock values. Defaults to
         *   an empty map.
         * @return A configured [FakeCaptureRequest] instance.
         */
        @JvmSynthetic
        @Suppress("MissingJvmstatic")
        public operator fun invoke(
            requestParameters: Map<CaptureRequest.Key<*>, Any?> = emptyMap(),
            requestMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
        ): FakeCaptureRequest {
            return FakeCaptureRequest(requestParameters, requestMetadata)
        }

        /**
         * Factory method to create a [FakeCaptureRequest] instance for Java interoperability.
         *
         * @param requestParameters The map of capture request keys to their mock values. Defaults
         *   to an empty map.
         * @param requestMetadata The map of custom metadata keys to their mock values. Defaults to
         *   an empty map.
         * @return A configured [FakeCaptureRequest] instance.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            requestParameters: Map<CaptureRequest.Key<*>, Any?> = emptyMap(),
            requestMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
        ): FakeCaptureRequest {
            return FakeCaptureRequest(requestParameters, requestMetadata)
        }
    }
}
