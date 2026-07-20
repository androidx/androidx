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
import android.os.Build
import androidx.camera.common.compat.Api28Compat
import java.lang.Class

/** [CaptureRequestWrapper] implementation that wraps a [CaptureRequest] object. */
public final class AndroidCaptureRequest
private constructor(
    private val captureRequest: CaptureRequest,
    private val metadata: Map<Metadata.Key<*>, Any?>,
) : CaptureRequestWrapper {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: CaptureRequest.Key<T>): T? {
        return captureRequest[key]
    }

    override val keys: List<CaptureRequest.Key<*>>
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Compat.getKeys(captureRequest)
            } else {
                emptyList()
            }

    override fun <T> get(key: Metadata.Key<T>): T? = metadata.getUnchecked(key)

    override val metadataKeys: Set<Metadata.Key<*>>
        get() = metadata.keys

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type.isInstance(captureRequest) -> captureRequest as T
            else -> null
        }

    public companion object {
        /**
         * Creates an [AndroidCaptureRequest] instance for Kotlin clients.
         *
         * Allows constructor-like syntax in Kotlin: `AndroidCaptureRequest(...)`.
         *
         * @param captureRequest The native [CaptureRequest] to wrap.
         * @param metadata Optional map of custom metadata key-value properties.
         * @return A configured [AndroidCaptureRequest] instance.
         */
        @JvmSynthetic
        @Suppress("MissingJvmstatic")
        public operator fun invoke(
            captureRequest: CaptureRequest,
            metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
        ): AndroidCaptureRequest = AndroidCaptureRequest(captureRequest, metadata)

        /**
         * Creates an [AndroidCaptureRequest] instance for Java compatibility.
         *
         * @param captureRequest The native [CaptureRequest] to wrap.
         * @param metadata Optional map of custom metadata key-value properties.
         * @return A configured [AndroidCaptureRequest] instance.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            captureRequest: CaptureRequest,
            metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
        ): AndroidCaptureRequest = AndroidCaptureRequest(captureRequest, metadata)
    }
}
