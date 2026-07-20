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

import android.hardware.camera2.CaptureResult
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.CameraId
import androidx.camera.common.CaptureRequestWrapper
import androidx.camera.common.CaptureResultWrapper
import androidx.camera.common.Metadata
import androidx.camera.common.getUnchecked
import java.lang.Class

/**
 * A fake implementation of [CaptureResultWrapper] for testing.
 *
 * Allows mock values to be configured for capture result parameters, custom metadata, camera ID,
 * frame number, and capture request via its constructor or companion [create] method.
 */
public class FakeCaptureResult
private constructor(
    cameraId: CameraId,
    frameNumber: CameraFrameNumber,
    override val captureRequest: CaptureRequestWrapper,
    private val resultParameters: Map<CaptureResult.Key<*>, Any?>,
    private val resultMetadata: Map<Metadata.Key<*>, Any?>,
) : CaptureResultWrapper {

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val cameraId: CameraId = cameraId

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getFrameNumber")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val frameNumber: CameraFrameNumber = frameNumber

    override val metadataKeys: Set<Metadata.Key<*>>
        get() = resultMetadata.keys

    override val keys: List<CaptureResult.Key<*>>
        get() = resultParameters.keys.toList()

    override fun <T> get(key: Metadata.Key<T>): T? = resultMetadata.getUnchecked(key)

    override fun <T> get(key: CaptureResult.Key<T>): T? = resultParameters.getUnchecked(key)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            else -> null
        }

    public companion object {
        /**
         * Creates a [FakeCaptureResult] instance for Kotlin clients.
         *
         * Allows constructor-like syntax in Kotlin: `FakeCaptureResult(...)`.
         *
         * @param cameraId The strongly typed [CameraId] associated with the result.
         * @param frameNumber The strongly typed [CameraFrameNumber] associated with the result.
         * @param captureRequest Optional wrapped [CaptureRequestWrapper] that produced this result.
         * @param resultParameters Optional map of capture result keys to their mock values.
         * @param resultMetadata Optional map of custom metadata keys to their mock values.
         * @return A configured [FakeCaptureResult] instance.
         */
        @JvmSynthetic
        @Suppress("MissingJvmstatic", "ValueClassUsageWithoutJvmName")
        public operator fun invoke(
            cameraId: CameraId,
            frameNumber: CameraFrameNumber,
            captureRequest: CaptureRequestWrapper = FakeCaptureRequest(),
            resultParameters: Map<CaptureResult.Key<*>, Any?> = emptyMap(),
            resultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
        ): FakeCaptureResult {
            return FakeCaptureResult(
                cameraId,
                frameNumber,
                captureRequest,
                resultParameters,
                resultMetadata,
            )
        }

        /**
         * Creates a [FakeCaptureResult] instance for Java compatibility.
         *
         * @param cameraId The camera ID string.
         * @param frameNumber The frame number.
         * @param captureRequest The capture request wrapper that generated this result.
         * @param resultParameters The map of capture result keys to their mock values.
         * @param resultMetadata The map of custom metadata keys to their mock values.
         * @return A configured [FakeCaptureResult] instance.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            cameraId: String,
            frameNumber: Long,
            captureRequest: CaptureRequestWrapper = FakeCaptureRequest(),
            resultParameters: Map<CaptureResult.Key<*>, Any?> = emptyMap(),
            resultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
        ): FakeCaptureResult {
            return FakeCaptureResult(
                CameraId(cameraId),
                CameraFrameNumber(frameNumber),
                captureRequest,
                resultParameters,
                resultMetadata,
            )
        }
    }
}
