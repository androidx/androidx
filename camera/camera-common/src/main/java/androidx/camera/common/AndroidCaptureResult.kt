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
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.camera.common.compat.Api28Compat
import java.lang.Class

/**
 * An implementation of [CaptureResultWrapper] that wraps a native [CaptureResult] object.
 *
 * This wrapper provides compatibility-focused access to the underlying camera capture results,
 * allowing query of camera states such as 3A, exposure, and lens state. It also supports additional
 * custom metadata key-value pairs.
 *
 * @property captureRequest The wrapped [CaptureRequestWrapper] that initiated this capture.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AndroidCaptureResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    private val captureResult: CaptureResult,
    cameraId: CameraId,
    override val captureRequest: CaptureRequestWrapper,
    private val metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
) : CaptureResultWrapper {

    /** The [CameraId] of the camera that produced this result. */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val cameraId: CameraId = cameraId

    /** The [CameraFrameNumber] associated with this result. */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getFrameNumber")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val frameNumber: CameraFrameNumber
        get() = CameraFrameNumber(captureResult.frameNumber)

    /**
     * Retrieves the value of the specified [CaptureResult.Key] from the wrapped [CaptureResult].
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    override fun <T : Any> get(key: CaptureResult.Key<T>): T? {
        return captureResult[key]
    }

    /**
     * List of all [CaptureResult.Key]s supported by this capture result.
     *
     * On Android P (API 28) and above, this returns keys queryable from the underlying
     * [CaptureResult]. On older versions, it returns an empty list.
     */
    override val keys: List<CaptureResult.Key<*>>
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Compat.getKeys(captureResult)
            } else {
                emptyList()
            }

    /**
     * Retrieves the value associated with the specified custom [Metadata.Key].
     *
     * @param key The key to query.
     * @return The value associated with the key, or `null` if not found.
     */
    override fun <T : Any> get(key: Metadata.Key<T>): T? = metadata.getUnchecked(key)

    /** Set of all custom [Metadata.Key]s available in this result. */
    override val metadataKeys: Set<Metadata.Key<*>>
        get() = metadata.keys

    /**
     * Unwraps this object to the specified type.
     *
     * This implementation can unwrap to [AndroidCaptureResult] itself or the underlying native
     * [CaptureResult].
     *
     * @param type The [Class] representing the target type.
     * @return The unwrapped object matching [type], or `null` if unwrapping is not supported.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type.isInstance(captureResult) -> captureResult as T
            else -> null
        }
}
