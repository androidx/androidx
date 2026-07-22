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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.util.Range
import android.util.Size
import androidx.camera.common.CameraExtension
import androidx.camera.common.CameraExtensionCharacteristicsWrapper
import androidx.camera.common.CameraId
import androidx.camera.common.ImageFormat
import androidx.camera.common.Metadata
import androidx.camera.common.UnsafeWrapper
import androidx.camera.common.getUnchecked

/**
 * A fake implementation of [CameraExtensionCharacteristicsWrapper] for testing.
 *
 * This class allows unit tests to mock and configure the characteristics and capabilities of camera
 * extension modes without relying on native Android platform classes or a physical device.
 *
 * To instantiate this class:
 * - In **Kotlin**, use the companion [invoke] operator for idiomatic builder-like
 *   creation: ```kotlin val characteristics = FakeCameraExtensionCharacteristics( cameraId =
 *   CameraId("0"), cameraExtension = CameraExtensionCharacteristics.EXTENSION_BOKEH ) ```
 * - In **Java**, use the static [create] factory method which takes a raw [String] camera
 *   ID: ```java FakeCameraExtensionCharacteristics characteristics =
 *   FakeCameraExtensionCharacteristics.create( "0", CameraExtensionCharacteristics.EXTENSION_BOKEH,
 *   // ... other parameters ); ```
 */
public class FakeCameraExtensionCharacteristics
private constructor(
    cameraId: CameraId = FakeCameraIds.default,
    @get:CameraExtension
    override val cameraExtension: Int = CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
    private val cameraCharacteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
    private val cameraMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    override val captureRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    override val captureResultKeys: Set<CaptureResult.Key<*>> = emptySet(),
    override val isRestricted: Boolean = false,
    override val restrictedKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    override val dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    override val isPostviewSupported: Boolean = false,
    override val isCaptureProgressSupported: Boolean = false,
    private val outputSizesFormat: Map<Int, Set<Size>> = emptyMap(),
    private val outputSizesClass: Map<Class<*>, Set<Size>> = emptyMap(),
    private val postviewSizes: Map<Pair<Size, Int>, Set<Size>> = emptyMap(),
    private val latencies: Map<Pair<Size, Int>, Range<Long>?> = emptyMap(),
) : CameraExtensionCharacteristicsWrapper {

    /** The ID of the camera device associated with these extension characteristics. */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val cameraId: CameraId = cameraId

    /** The supported [CameraCharacteristics.Key]s defined in the mock characteristics map. */
    override val keys: Set<CameraCharacteristics.Key<*>>
        get() = cameraCharacteristics.keys

    /** The supported [Metadata.Key]s defined in the mock metadata map. */
    override val metadataKeys: Set<Metadata.Key<*>>
        get() = cameraMetadata.keys

    /**
     * Retrieves the mock metadata value for the given key.
     *
     * @param key the metadata key.
     * @return the mock value, or null if not set.
     */
    override fun <T : Any> get(key: Metadata.Key<T>): T? = cameraMetadata.getUnchecked(key)

    /**
     * Retrieves the mock characteristics value for the given key.
     *
     * @param key the characteristics key.
     * @return the mock value, or null if not set.
     */
    override fun <T : Any> get(key: CameraCharacteristics.Key<T>): T? =
        cameraCharacteristics.getUnchecked(key)

    /**
     * Retrieves the mock characteristics value for the given key, returning the default if not set.
     *
     * @param key the characteristics key.
     * @param default the default value to return if the key is not set.
     * @return the mock value, or the default value.
     */
    override fun <T : Any> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T {
        return get(key) ?: default
    }

    /**
     * Unwraps this instance as the requested type if compatible.
     *
     * Implements [UnsafeWrapper.unwrapAs].
     *
     * @param type the class of the expected type.
     * @return this instance cast to [T] if compatible, or null.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            else -> null
        }

    /**
     * Returns the mock output sizes for the given image format.
     *
     * @param imageFormat the image format.
     * @return the set of configured output sizes.
     */
    override fun getOutputSizes(@ImageFormat imageFormat: Int): Set<Size> =
        outputSizesFormat[imageFormat] ?: emptySet()

    /**
     * Returns the mock output sizes for the given output class.
     *
     * @param klass the output class.
     * @return the set of configured output sizes.
     */
    override fun getOutputSizes(klass: Class<*>): Set<Size> = outputSizesClass[klass] ?: emptySet()

    /**
     * Returns the mock postview sizes for the given capture size and format.
     *
     * @param captureSize the capture size.
     * @param format the image format.
     * @return the set of configured postview sizes.
     */
    override fun getPostviewSizes(captureSize: Size, @ImageFormat format: Int): Set<Size> =
        postviewSizes[captureSize to format] ?: emptySet()

    /**
     * Returns the mock estimated capture latency range in milliseconds for the given capture size
     * and format.
     *
     * @param captureSize the capture size.
     * @param imageFormat the image format.
     * @return the configured latency range, or null.
     */
    override fun getEstimatedCaptureLatencyRangeMillis(
        captureSize: Size,
        @ImageFormat imageFormat: Int,
    ): Range<Long>? = latencies[captureSize to imageFormat]

    public companion object {
        /**
         * Creates a [FakeCameraExtensionCharacteristics] instance.
         *
         * This method is designed for Java interop. It takes a raw string for the camera ID and
         * supports default parameters in Java via `@JvmOverloads`.
         *
         * @param cameraId the camera ID string (defaults to [FakeCameraIds.default]).
         * @param cameraExtension the extension mode (defaults to
         *   [CameraExtensionCharacteristics.EXTENSION_AUTOMATIC]).
         * @param cameraCharacteristics the mock characteristics map.
         * @param cameraMetadata the mock metadata map.
         * @param captureRequestKeys the supported capture request keys.
         * @param captureResultKeys the supported capture result keys.
         * @param isRestricted whether the extension is restricted.
         * @param restrictedKeys the restricted characteristics keys.
         * @param dynamicKeys the dynamic characteristics keys.
         * @param isPostviewSupported whether postview is supported.
         * @param isCaptureProgressSupported whether capture progress is supported.
         * @param outputSizesFormat mock output sizes map keyed by format.
         * @param outputSizesClass mock output sizes map keyed by class.
         * @param postviewSizes mock postview sizes map keyed by capture size and format.
         * @param latencies mock latencies map keyed by capture size and format.
         * @return a configured [FakeCameraExtensionCharacteristics] instance.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            cameraId: String = FakeCameraIds.default.value,
            @CameraExtension
            cameraExtension: Int = CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
            cameraCharacteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
            cameraMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            captureRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
            captureResultKeys: Set<CaptureResult.Key<*>> = emptySet(),
            isRestricted: Boolean = false,
            restrictedKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
            dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
            isPostviewSupported: Boolean = false,
            isCaptureProgressSupported: Boolean = false,
            outputSizesFormat: Map<Int, Set<Size>> = emptyMap(),
            outputSizesClass: Map<Class<*>, Set<Size>> = emptyMap(),
            postviewSizes: Map<Pair<Size, Int>, Set<Size>> = emptyMap(),
            latencies: Map<Pair<Size, Int>, Range<Long>?> = emptyMap(),
        ): FakeCameraExtensionCharacteristics {
            return FakeCameraExtensionCharacteristics(
                cameraId = CameraId(cameraId),
                cameraExtension = cameraExtension,
                cameraCharacteristics = cameraCharacteristics,
                cameraMetadata = cameraMetadata,
                captureRequestKeys = captureRequestKeys,
                captureResultKeys = captureResultKeys,
                isRestricted = isRestricted,
                restrictedKeys = restrictedKeys,
                dynamicKeys = dynamicKeys,
                isPostviewSupported = isPostviewSupported,
                isCaptureProgressSupported = isCaptureProgressSupported,
                outputSizesFormat = outputSizesFormat,
                outputSizesClass = outputSizesClass,
                postviewSizes = postviewSizes,
                latencies = latencies,
            )
        }

        /**
         * Creates a [FakeCameraExtensionCharacteristics] instance.
         *
         * This operator is designed for Kotlin usage. It supports the type-safe [CameraId] value
         * class and default arguments.
         *
         * @param cameraId the camera ID (defaults to [FakeCameraIds.default]).
         * @param cameraExtension the extension mode (defaults to
         *   [CameraExtensionCharacteristics.EXTENSION_AUTOMATIC]).
         * @param cameraCharacteristics the mock characteristics map.
         * @param cameraMetadata the mock metadata map.
         * @param captureRequestKeys the supported capture request keys.
         * @param captureResultKeys the supported capture result keys.
         * @param isRestricted whether the extension is restricted.
         * @param restrictedKeys the restricted characteristics keys.
         * @param dynamicKeys the dynamic characteristics keys.
         * @param isPostviewSupported whether postview is supported.
         * @param isCaptureProgressSupported whether capture progress is supported.
         * @param outputSizesFormat mock output sizes map keyed by format.
         * @param outputSizesClass mock output sizes map keyed by class.
         * @param postviewSizes mock postview sizes map keyed by capture size and format.
         * @param latencies mock latencies map keyed by capture size and format.
         * @return a configured [FakeCameraExtensionCharacteristics] instance.
         */
        @JvmSynthetic
        @Suppress("MissingJvmstatic", "ValueClassUsageWithoutJvmName")
        public operator fun invoke(
            cameraId: CameraId = FakeCameraIds.default,
            @CameraExtension
            cameraExtension: Int = CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
            cameraCharacteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
            cameraMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            captureRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
            captureResultKeys: Set<CaptureResult.Key<*>> = emptySet(),
            isRestricted: Boolean = false,
            restrictedKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
            dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
            isPostviewSupported: Boolean = false,
            isCaptureProgressSupported: Boolean = false,
            outputSizesFormat: Map<Int, Set<Size>> = emptyMap(),
            outputSizesClass: Map<Class<*>, Set<Size>> = emptyMap(),
            postviewSizes: Map<Pair<Size, Int>, Set<Size>> = emptyMap(),
            latencies: Map<Pair<Size, Int>, Range<Long>?> = emptyMap(),
        ): FakeCameraExtensionCharacteristics {
            return FakeCameraExtensionCharacteristics(
                cameraId = cameraId,
                cameraExtension = cameraExtension,
                cameraCharacteristics = cameraCharacteristics,
                cameraMetadata = cameraMetadata,
                captureRequestKeys = captureRequestKeys,
                captureResultKeys = captureResultKeys,
                isRestricted = isRestricted,
                restrictedKeys = restrictedKeys,
                dynamicKeys = dynamicKeys,
                isPostviewSupported = isPostviewSupported,
                isCaptureProgressSupported = isCaptureProgressSupported,
                outputSizesFormat = outputSizesFormat,
                outputSizesClass = outputSizesClass,
                postviewSizes = postviewSizes,
                latencies = latencies,
            )
        }
    }
}
