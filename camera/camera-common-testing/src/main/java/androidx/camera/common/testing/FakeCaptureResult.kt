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
 * Fake implementation of [CaptureResultWrapper] for unit testing.
 *
 * `FakeCaptureResult` simulates capture result metadata, camera ID, frame number, and the
 * originating capture request in unit tests without requiring a physical camera device.
 *
 * You can instantiate `FakeCaptureResult` in two ways:
 * - In **Kotlin**, call the companion [invoke] operator (`FakeCaptureResult(...)`).
 * - In **Java**, call the static [create] factory method (`FakeCaptureResult.create(...)`).
 *
 * @sample androidx.camera.common.testing.samples.fakeCaptureResultSample
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

    /**
     * Retrieves the mock value for the specified custom metadata [key].
     *
     * @return The configured mock value, or `null` if not set.
     */
    override fun <T : Any> get(key: Metadata.Key<T>): T? = resultMetadata.getUnchecked(key)

    /**
     * Retrieves the mock value for the specified [CaptureResult.Key].
     *
     * @return The configured mock value, or `null` if not set.
     */
    override fun <T : Any> get(key: CaptureResult.Key<T>): T? = resultParameters.getUnchecked(key)

    /**
     * Attempts to unwrap this fake object.
     *
     * Since this is a fake implementation, it does not wrap a native platform [CaptureResult]. This
     * method will return `null` for [CaptureResult]. It only returns `this` cast to [T] if [type]
     * is compatible with [FakeCaptureResult].
     */
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
         * This operator enables constructor-like syntax in Kotlin (`FakeCaptureResult(...)`).
         *
         * @param cameraId The strongly typed [CameraId] associated with the result.
         * @param frameNumber The strongly typed [CameraFrameNumber] associated with the result.
         * @param captureRequest Optional wrapped [CaptureRequestWrapper] that produced this result.
         *   Defaults to a default [FakeCaptureRequest].
         * @param resultParameters Optional map of capture result keys to their mock values.
         *   Defaults to an empty map.
         * @param resultMetadata Optional map of custom metadata keys to their mock values. Defaults
         *   to an empty map.
         * @return A configured [FakeCaptureResult] instance.
         * @sample androidx.camera.common.testing.samples.fakeCaptureResultSample
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
         * This method is overloaded for Java callers to allow omitting trailing parameters with
         * default values.
         *
         * @param cameraId The camera ID string.
         * @param frameNumber The frame number.
         * @param captureRequest The capture request wrapper that generated this result. Defaults to
         *   a default [FakeCaptureRequest].
         * @param resultParameters The map of capture result keys to their mock values. Defaults to
         *   an empty map.
         * @param resultMetadata The map of custom metadata keys to their mock values. Defaults to
         *   an empty map.
         * @return A configured [FakeCaptureResult] instance.
         * @sample androidx.camera.common.testing.samples.fakeCaptureResultSample
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
