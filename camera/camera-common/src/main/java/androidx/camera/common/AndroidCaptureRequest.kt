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
import androidx.annotation.RestrictTo
import androidx.camera.common.compat.Api28Compat
import java.lang.Class

/**
 * An implementation of [CaptureRequestWrapper] that wraps a native Android [CaptureRequest].
 *
 * This class provides access to the settings applied to a specific capture request, wrapping a
 * native [CaptureRequest] and optionally associating custom metadata with it.
 *
 * @see CaptureRequestWrapper
 * @see Metadata
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AndroidCaptureRequest
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    private val captureRequest: CaptureRequest,
    private val metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
) : CaptureRequestWrapper {

    /**
     * Retrieves the value of the specified native [CaptureRequest.Key] from the wrapped
     * [CaptureRequest].
     *
     * @param key The native capture request key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: CaptureRequest.Key<T>): T? {
        return captureRequest[key]
    }

    /**
     * List of all [CaptureRequest.Key]s supported by this capture request.
     *
     * On API levels prior to 28 (Android P), this property always returns an empty list, as
     * retrieving the list of keys from a [CaptureRequest] is not supported by the platform.
     *
     * @see android.hardware.camera2.CaptureRequest.getKeys
     */
    override val keys: List<CaptureRequest.Key<*>>
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Compat.getKeys(captureRequest)
            } else {
                emptyList()
            }

    /**
     * Retrieves the value associated with the specified custom [Metadata.Key].
     *
     * @param key The custom metadata key to query.
     * @return The value associated with the key, or `null` if not found.
     */
    override fun <T> get(key: Metadata.Key<T>): T? = metadata.getUnchecked(key)

    /** Set of all custom [Metadata.Key]s available in this request. */
    override val metadataKeys: Set<Metadata.Key<*>>
        get() = metadata.keys

    /**
     * Attempts to unwrap this object into the specified type.
     *
     * This method supports unwrapping to:
     * - [AndroidCaptureRequest] (returns `this`)
     * - [CaptureRequest] (returns the wrapped native capture request)
     *
     * For any other type, it returns `null`.
     *
     * @param type The class of the type to unwrap to.
     * @return The unwrapped object, or `null` if the type is not supported.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type.isInstance(captureRequest) -> captureRequest as T
            else -> null
        }
}
