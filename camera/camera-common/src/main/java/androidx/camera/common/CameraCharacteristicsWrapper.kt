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

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.common.compat.Api33Compat
import androidx.camera.common.compat.Api34Compat

/**
 * Wrapper interface providing compatibility-focused access to [CameraCharacteristics].
 *
 * Use this interface to query camera capabilities, physical properties, and supported
 * configurations. It abstracts OS version differences and caches expensive-to-retrieve properties
 * to ensure efficient access across all API levels.
 *
 * `CameraCharacteristicsWrapper` implements [Metadata], allowing it to support type-safe retrieval
 * of custom library-defined metadata keys via [Metadata.Key] in addition to native
 * [CameraCharacteristics.Key] keys.
 *
 * It also implements [UnsafeWrapper], which allows unwrapping to the underlying native
 * [CameraCharacteristics] using [UnsafeWrapper.unwrapAs] when platform-specific APIs are required.
 * Note that bypassing the wrapper by unwrapping avoids compatibility fixes and caching.
 *
 * **Note:** This interface is not stable for inheritance. Implementations should not be created
 * directly by clients. For testing, use the fakes in `androidx.camera.common.testing` package (such
 * as `FakeCameraCharacteristics`).
 *
 * @sample androidx.camera.common.samples.wrapCameraCharacteristicsSample
 * @sample androidx.camera.common.samples.accessCameraCharacteristicsPropertiesSample
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
     * A [Set] of all [CameraCharacteristics.Key]s supported by this camera device.
     *
     * This property is equivalent to calling
     * [android.hardware.camera2.CameraCharacteristics.getKeys] on the underlying camera
     * characteristics, but may be cached for efficiency.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getKeys
     */
    public val keys: Set<CameraCharacteristics.Key<*>>

    /**
     * A [Set] of all [CaptureRequest.Key]s supported by this camera device for [CaptureRequest]s.
     *
     * This property is equivalent to calling
     * [CameraCharacteristics.getAvailableCaptureRequestKeys][CameraCharacteristics.getAvailableCaptureRequestKeys],
     * but may be cached for efficiency.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableCaptureRequestKeys
     */
    public val captureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * A [Set] of all [CaptureResult.Key]s supported by this camera device for [CaptureResult]s.
     *
     * This property is equivalent to calling
     * [CameraCharacteristics.getAvailableCaptureResultKeys][CameraCharacteristics.getAvailableCaptureResultKeys],
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
     * A [Set] of [CameraCharacteristics.Key]s whose values are capture session specific.
     *
     * On API levels prior to 35 (Android 15), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableSessionCharacteristicsKeys
     */
    public val sessionKeys: Set<CameraCharacteristics.Key<*>>

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
     * A [Set] of [CameraCharacteristics.Key]s that require camera clients to obtain the
     * `Manifest.permission.CAMERA` permission.
     *
     * On API levels prior to 29 (Android Q), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getKeysNeedingPermission
     */
    public val restrictedKeys: Set<CameraCharacteristics.Key<*>>

    /**
     * A [Set] of physical camera IDs that this logical camera device is made up of.
     *
     * On API levels prior to 28 (Android P), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getPhysicalCameraIds
     */
    public val physicalCameraIds: Set<CameraId>

    /**
     * Indicates whether the wrapper queried camera characteristics without camera permission.
     *
     * When `true`, the application did not hold the [android.Manifest.permission.CAMERA] permission
     * when this instance was created. In this restricted mode, querying keys listed in
     * [restrictedKeys] may return `null` or empty values.
     */
    public val isRestricted: Boolean

    /**
     * A [Set] of [CameraCharacteristics.Key]s whose values can change dynamically based on the
     * physical state of the device.
     *
     * Do not cache values retrieved using these keys. The values can change at runtime, for example
     * when a foldable device changes its posture.
     *
     * At present, only the
     * [SENSOR_ORIENTATION](https://developer.android.com/reference/kotlin/android/hardware/camera2/CameraCharacteristics#sensor_orientation)
     * key on foldable devices can change based on the posture of the device, but may be expanded in
     * the future if other camera properties change dynamically based on the posture or other
     * physical state of the device.
     *
     * The primary purpose of this property is to indicate which keys should not be cached.
     */
    public val dynamicKeys: Set<CameraCharacteristics.Key<*>>

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
         * Prefer using the [CameraCharacteristicsWrappers.streamConfigurationMap] property to
         * access this value.
         */
        @JvmField
        public val STREAM_CONFIGURATION_MAP: Metadata.Key<StreamConfigurationMapWrapper> =
            Metadata.Key("androidx.camera.common.StreamConfigurationMap")

        /** Key for retrieving the [ColorSpaceProfilesWrapper] supported by the camera device. */
        @JvmField
        public val AVAILABLE_COLOR_SPACE_PROFILES: Metadata.Key<ColorSpaceProfilesWrapper> =
            Metadata.Key("androidx.camera.common.availableColorSpaceProfiles")

        /** Key for retrieving the [DynamicRangeProfilesWrapper] supported by the camera device. */
        @JvmField
        public val AVAILABLE_DYNAMIC_RANGE_PROFILES: Metadata.Key<DynamicRangeProfilesWrapper> =
            Metadata.Key("androidx.camera.common.availableDynamicRangeProfiles")
    }
}

/** Custom metadata keys for [CameraCharacteristicsWrapper]. */
public object CameraCharacteristicsWrappers {
    /**
     * Extension property to query [StreamConfigurationMapWrapper] directly.
     *
     * This is the preferred way to access the [StreamConfigurationMapWrapper] in Kotlin. It will
     * fallback to creating a wrapper from the standard
     * [CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP] if the compatibility key is not
     * populated.
     */
    @get:JvmStatic
    public val CameraCharacteristicsMetadata.streamConfigurationMap: StreamConfigurationMapWrapper?
        get() {
            val compatibilityMap = this[CameraCharacteristicsWrapper.Keys.STREAM_CONFIGURATION_MAP]
            if (compatibilityMap != null) {
                return compatibilityMap
            }
            val camera2Map = this[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            if (camera2Map != null) {
                val cameraId =
                    (this as? CameraCharacteristicsWrapper)?.cameraId ?: CameraId("unknown")
                return AndroidStreamConfigurationMap(camera2Map, cameraId)
            }
            return null
        }

    /** Returns the [ColorSpaceProfilesWrapper] supported by the camera device. */
    @JvmStatic
    public val CameraCharacteristicsMetadata.availableColorSpaceProfiles: ColorSpaceProfilesWrapper
        get() =
            this[CameraCharacteristicsWrapper.Keys.AVAILABLE_COLOR_SPACE_PROFILES]
                ?: if (Build.VERSION.SDK_INT >= 34) {
                    Api34Compat.getColorSpaceProfiles(this) ?: UnsupportedColorSpaceProfiles
                } else {
                    UnsupportedColorSpaceProfiles
                }

    /**
     * Returns the [DynamicRangeProfilesWrapper] supported by the camera device.
     *
     * @see android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES
     */
    @JvmStatic
    public val CameraCharacteristicsMetadata.availableDynamicRangeProfiles:
        DynamicRangeProfilesWrapper
        get() =
            this[CameraCharacteristicsWrapper.Keys.AVAILABLE_DYNAMIC_RANGE_PROFILES]
                ?: if (Build.VERSION.SDK_INT >= 33) {
                    Api33Compat.getDynamicRangeProfiles(this) ?: UnsupportedDynamicRangeProfiles
                } else {
                    UnsupportedDynamicRangeProfiles
                }

    /**
     * Loads [CameraCharacteristicsWrapper] for the given [cameraId] using [Context].
     *
     * This method will perform an instantaneous check to determine if the camera permission is
     * granted, setting [CameraCharacteristicsWrapper.isRestricted] accordingly.
     */
    @JvmStatic
    @JvmName("loadFrom")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmOverloads
    public fun loadFrom(
        context: Context,
        cameraId: CameraId,
        dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    ): CameraCharacteristicsWrapper {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val hasPermission =
            context.checkPermission(
                android.Manifest.permission.CAMERA,
                android.os.Process.myPid(),
                android.os.Process.myUid(),
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return loadFrom(cameraManager, cameraId, !hasPermission, dynamicKeys)
    }

    /** Loads [CameraCharacteristicsWrapper] for the given [cameraId] using [CameraManager]. */
    @JvmStatic
    @JvmName("loadFrom")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmOverloads
    public fun loadFrom(
        cameraManager: CameraManager,
        cameraId: CameraId,
        isRestricted: Boolean = false,
        dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    ): CameraCharacteristicsWrapper {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId.value)
        return AndroidCameraCharacteristics(cameraId, characteristics, isRestricted, dynamicKeys)
    }

    /** Wraps a native [CameraCharacteristics] into a [CameraCharacteristicsWrapper]. */
    @JvmStatic
    @JvmName("wrap")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmOverloads
    public fun wrap(
        cameraId: CameraId,
        characteristics: CameraCharacteristics,
        isRestricted: Boolean = false,
        dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    ): CameraCharacteristicsWrapper {
        return AndroidCameraCharacteristics(cameraId, characteristics, isRestricted, dynamicKeys)
    }
}
