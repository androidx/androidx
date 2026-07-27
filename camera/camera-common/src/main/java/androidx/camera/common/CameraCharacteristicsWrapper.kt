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

import android.hardware.camera2.CameraCharacteristics as PlatformCameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult

/**
 * Wrapper interface providing compatibility-focused access to [PlatformCameraCharacteristics].
 *
 * Use this interface to query camera capabilities, physical properties, and supported
 * configurations. It abstracts OS version differences and caches expensive-to-retrieve properties
 * to ensure efficient access across all API levels.
 *
 * `CameraCharacteristicsWrapper` implements [Metadata], allowing it to support type-safe retrieval
 * of custom library-defined metadata keys via [Metadata.Key] in addition to native
 * [CameraCharacteristics.Key][PlatformCameraCharacteristics.Key] keys.
 *
 * It also implements [UnsafeWrapper], which allows unwrapping to the underlying native
 * [CameraCharacteristics][PlatformCameraCharacteristics] using [UnsafeWrapper.unwrapAs] when
 * platform-specific APIs are required. Note that bypassing the wrapper by unwrapping avoids
 * compatibility fixes and caching.
 *
 * **Note:** This interface is not stable for inheritance. Implementations should not be created
 * directly by clients. For testing, use the fakes in `androidx.camera.common.testing` package (such
 * as `FakeCameraCharacteristics`).
 *
 * ### Example
 *
 * Querying native and custom keys:
 * ```kotlin
 * val characteristics: CameraCharacteristicsWrapper = ...
 *
 * // Query a native CameraCharacteristics key:
 * val lensFacing: Int? = characteristics[CameraCharacteristics.LENS_FACING]
 *
 * // Query a custom Metadata key:
 * val customValue: String? = characteristics[MY_CUSTOM_METADATA_KEY]
 * ```
 *
 * Unwrapping the native object (unsafe):
 * ```kotlin
 * val nativeCharacteristics: CameraCharacteristics? = characteristics.unwrapAs()
 * ```
 */
public interface CameraCharacteristicsWrapper : CameraCharacteristicsMetadata {
    /**
     * The [CameraId] identifying this camera device.
     *
     * This corresponds to the camera ID used to open the camera with
     * [android.hardware.camera2.CameraManager.openCamera].
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    public val cameraId: CameraId

    /**
     * A [Set] of all [PlatformCameraCharacteristics.Key]s supported by this camera device.
     *
     * This property is equivalent to calling
     * [android.hardware.camera2.CameraCharacteristics.getKeys] on the underlying camera
     * characteristics, but may be cached for efficiency.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getKeys
     */
    public val keys: Set<PlatformCameraCharacteristics.Key<*>>

    /**
     * A [Set] of all [CaptureRequest.Key]s supported by this camera device for [CaptureRequest]s.
     *
     * This property is equivalent to calling
     * [CameraCharacteristics.getAvailableCaptureRequestKeys][PlatformCameraCharacteristics.getAvailableCaptureRequestKeys],
     * but may be cached for efficiency.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableCaptureRequestKeys
     */
    public val captureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * A [Set] of all [CaptureResult.Key]s supported by this camera device for [CaptureResult]s.
     *
     * This property is equivalent to calling
     * [CameraCharacteristics.getAvailableCaptureResultKeys][PlatformCameraCharacteristics.getAvailableCaptureResultKeys],
     * but may be cached for efficiency.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableCaptureResultKeys
     */
    public val captureResultKeys: Set<CaptureResult.Key<*>>

    /**
     * A [Set] of physical [CaptureRequest.Key]s that can be overridden for physical devices backing
     * a logical multi-camera.
     *
     * On API levels prior to 28 (Android P), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailablePhysicalCameraRequestKeys
     */
    public val physicalCaptureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * A [Set] of [CameraCharacteristics.Key][PlatformCameraCharacteristics.Key]s whose values are
     * capture session specific.
     *
     * On API levels prior to 35 (Android 15), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableSessionCharacteristicsKeys
     */
    public val sessionKeys: Set<PlatformCameraCharacteristics.Key<*>>

    /**
     * A [Set] of [CaptureRequest.Key]s that the camera device can pass as part of the capture
     * session initialization.
     *
     * On API levels prior to 28 (Android P), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableSessionKeys
     */
    public val sessionCaptureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * A [Set] of [CameraCharacteristics.Key][PlatformCameraCharacteristics.Key]s that require
     * camera clients to obtain the `Manifest.permission.CAMERA` permission.
     *
     * On API levels prior to 29 (Android Q), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getKeysNeedingPermission
     */
    public val restrictedKeys: Set<PlatformCameraCharacteristics.Key<*>>

    /**
     * A [Set] of physical camera IDs that this logical camera device is made up of.
     *
     * On API levels prior to 28 (Android P), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getPhysicalCameraIds
     */
    public val physicalCameraIds: Set<CameraId>

    public object Keys {
        /**
         * Key for [StreamConfigurationMapWrapper].
         *
         * Use this key to query the compatibility wrapper for
         * [android.hardware.camera2.params.StreamConfigurationMap].
         *
         * ### Example
         *
         * ```kotlin
         * val mapWrapper = cameraCharacteristics[CameraCharacteristicsWrapper.Keys.STREAM_CONFIGURATION_MAP]
         * ```
         *
         * Prefer using the [CameraCharacteristics.streamConfigurationMap] property to access this
         * value.
         */
        @JvmField
        public val STREAM_CONFIGURATION_MAP: Metadata.Key<StreamConfigurationMapWrapper> =
            Metadata.Key("androidx.camera.common.StreamConfigurationMap")
    }
}

public object CameraCharacteristics {
    /**
     * Extension property to query [StreamConfigurationMapWrapper] directly.
     *
     * This is the preferred way to access the [StreamConfigurationMapWrapper] in Kotlin. It will
     * fallback to creating a wrapper from the standard
     * [PlatformCameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP] if the compatibility key is
     * not populated.
     */
    @get:JvmStatic
    public val CameraCharacteristicsMetadata.streamConfigurationMap: StreamConfigurationMapWrapper?
        get() {
            val compatibilityMap = this[CameraCharacteristicsWrapper.Keys.STREAM_CONFIGURATION_MAP]
            if (compatibilityMap != null) {
                return compatibilityMap
            }
            val camera2Map = this[PlatformCameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            if (camera2Map != null) {
                val cameraId =
                    (this as? CameraCharacteristicsWrapper)?.cameraId ?: CameraId("unknown")
                return AndroidStreamConfigurationMap(camera2Map, cameraId)
            }
            return null
        }
}
