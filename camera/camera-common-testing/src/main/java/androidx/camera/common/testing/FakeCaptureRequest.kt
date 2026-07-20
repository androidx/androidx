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
 * A fake implementation of [CaptureRequestWrapper] for testing.
 *
 * Allows mock values to be configured for request parameters and custom metadata via its companion
 * [invoke] operator (Kotlin) or [create] factory method (Java).
 */
public class FakeCaptureRequest
private constructor(
    private val requestParameters: Map<CaptureRequest.Key<*>, Any?>,
    private val requestMetadata: Map<Metadata.Key<*>, Any?>,
) : CaptureRequestWrapper {

    override val metadataKeys: Set<Metadata.Key<*>>
        get() = requestMetadata.keys

    override val keys: List<CaptureRequest.Key<*>>
        get() = requestParameters.keys.toList()

    override fun <T> get(key: Metadata.Key<T>): T? = requestMetadata.getUnchecked(key)

    override fun <T> get(key: CaptureRequest.Key<T>): T? = requestParameters.getUnchecked(key)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            else -> null
        }

    public companion object {
        /**
         * Creates a [FakeCaptureRequest] instance for Kotlin clients.
         *
         * Allows constructor-like syntax in Kotlin: `FakeCaptureRequest(...)`.
         *
         * @param requestParameters The map of capture request keys to their mock values.
         * @param requestMetadata The map of custom metadata keys to their mock values.
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
         * Creates a [FakeCaptureRequest] instance for Java compatibility.
         *
         * @param requestParameters The map of capture request keys to their mock values.
         * @param requestMetadata The map of custom metadata keys to their mock values.
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
