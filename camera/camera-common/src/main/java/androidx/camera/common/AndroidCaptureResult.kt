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
import androidx.camera.common.compat.Api28Compat
import java.lang.Class

/** [CaptureResultWrapper] implementation that wraps a [CaptureResult] object. */
public final class AndroidCaptureResult
private constructor(
    private val captureResult: CaptureResult,
    cameraId: CameraId,
    override val captureRequest: CaptureRequestWrapper,
    private val metadata: Map<Metadata.Key<*>, Any?>,
) : CaptureResultWrapper {

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val cameraId: CameraId = cameraId

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getFrameNumber")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val frameNumber: CameraFrameNumber
        get() = CameraFrameNumber(captureResult.frameNumber)

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: CaptureResult.Key<T>): T? {
        return captureResult[key]
    }

    override val keys: List<CaptureResult.Key<*>>
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Compat.getKeys(captureResult)
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
            type.isInstance(captureResult) -> captureResult as T
            else -> null
        }

    public companion object {
        /**
         * Creates an [AndroidCaptureResult] instance for Kotlin clients.
         *
         * Allows constructor-like syntax in Kotlin: `AndroidCaptureResult(...)`.
         *
         * @param captureResult The native [CaptureResult] to wrap.
         * @param cameraId The strongly typed [CameraId] associated with the result.
         * @param captureRequest The wrapped [CaptureRequestWrapper] that produced this result.
         * @param metadata Optional map of custom metadata key-value properties.
         * @return A configured [AndroidCaptureResult] instance.
         */
        @JvmSynthetic
        @Suppress("MissingJvmstatic", "ValueClassUsageWithoutJvmName")
        public operator fun invoke(
            captureResult: CaptureResult,
            cameraId: CameraId,
            captureRequest: CaptureRequestWrapper,
            metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
        ): AndroidCaptureResult =
            AndroidCaptureResult(captureResult, cameraId, captureRequest, metadata)

        /**
         * Creates an [AndroidCaptureResult] instance for Java compatibility.
         *
         * @param captureResult The native [CaptureResult] to wrap.
         * @param cameraId The camera identifier string.
         * @param captureRequest The wrapped [CaptureRequestWrapper] that produced this result.
         * @param metadata Optional map of custom metadata key-value properties.
         * @return A configured [AndroidCaptureResult] instance.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            captureResult: CaptureResult,
            cameraId: String,
            captureRequest: CaptureRequestWrapper,
            metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
        ): AndroidCaptureResult =
            AndroidCaptureResult(captureResult, CameraId(cameraId), captureRequest, metadata)
    }
}
