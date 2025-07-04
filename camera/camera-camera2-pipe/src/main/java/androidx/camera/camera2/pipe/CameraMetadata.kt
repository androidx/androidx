/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.compat.Api33Compat
import androidx.camera.camera2.pipe.compat.Api34Compat
import androidx.camera.camera2.pipe.compat.Api35Compat
import androidx.camera.camera2.pipe.compat.Camera2ColorSpaceProfiles

/**
 * [CameraMetadata] is a compatibility wrapper around [CameraCharacteristics].
 *
 * Applications should, in most situations, prefer using this interface to using the unwrapping and
 * using the underlying [CameraCharacteristics] object directly. Implementation(s) of this interface
 * provide compatibility guarantees and performance improvements over using [CameraCharacteristics]
 * directly. This allows code to get reasonable behavior for all properties across all OS levels and
 * makes behavior that depends on [CameraMetadata] easier to test and reason about.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraMetadata : Metadata, UnsafeWrapper {
    public operator fun <T> get(key: CameraCharacteristics.Key<T>): T?

    public fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T

    public val camera: CameraId
    public val isRedacted: Boolean

    public val keys: Set<CameraCharacteristics.Key<*>>
    public val requestKeys: Set<CaptureRequest.Key<*>>
    public val resultKeys: Set<CaptureResult.Key<*>>
    public val sessionKeys: Set<CaptureRequest.Key<*>>

    public val physicalCameraIds: Set<CameraId>
    public val physicalRequestKeys: Set<CaptureRequest.Key<*>>
    public val supportedExtensions: Set<Int>

    public suspend fun getPhysicalMetadata(cameraId: CameraId): CameraMetadata

    public fun awaitPhysicalMetadata(cameraId: CameraId): CameraMetadata

    public suspend fun getExtensionMetadata(extension: Int): CameraExtensionMetadata

    public fun awaitExtensionMetadata(extension: Int): CameraExtensionMetadata

    public companion object {
        /**
         * Extension properties for querying the available capabilities of a camera device across
         * all API levels.
         */
        public var EMPTY_INT_ARRAY: IntArray = IntArray(0)

        public const val CAPABILITIES_MANUAL_SENSOR: Int = 1
        public const val CAPABILITIES_MANUAL_POST_PROCESSING: Int = 2
        public const val CAPABILITIES_RAW: Int = 3
        public const val CAPABILITIES_PRIVATE_REPROCESSING: Int = 4
        public const val CAPABILITIES_READ_SENSOR_SETTINGS: Int = 5
        public const val CAPABILITIES_BURST_CAPTURE: Int = 6
        public const val CAPABILITIES_YUV_REPROCESSING: Int = 7
        public const val CAPABILITIES_DEPTH_OUTPUT: Int = 8
        public const val CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO: Int = 9
        public const val CAPABILITIES_MOTION_TRACKING: Int = 10
        public const val CAPABILITIES_LOGICAL_MULTI_CAMERA: Int = 11
        public const val CAPABILITIES_MONOCHROME: Int = 12
        public const val CAPABILITIES_SECURE_IMAGE_DATA: Int = 13
        public const val CAPABILITIES_SYSTEM_CAMERA: Int = 14
        public const val CAPABILITIES_OFFLINE_REPROCESSING: Int = 15
        public const val CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR: Int = 16
        public const val CAPABILITIES_REMOSAIC_REPROCESSING: Int = 17
        public const val CAPABILITIES_DYNAMIC_RANGE_TEN_BIT: Int = 18
        public const val CAPABILITIES_STREAM_USE_CASE: Int = 19
        public const val CAPABILITIES_COLOR_SPACE_PROFILES: Int = 20

        public val CameraMetadata.availableCapabilities: IntArray
            @JvmStatic
            get() = this[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES] ?: EMPTY_INT_ARRAY

        public val CameraMetadata.availableVideoStabilizationModes: IntArray
            @JvmStatic
            get() =
                this[CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES]
                    ?: EMPTY_INT_ARRAY

        public val CameraMetadata.isHardwareLevelExternal: Boolean
            @JvmStatic
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL

        public val CameraMetadata.isHardwareLevelLegacy: Boolean
            @JvmStatic
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

        public val CameraMetadata.isHardwareLevelLimited: Boolean
            @JvmStatic
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED

        public val CameraMetadata.isHardwareLevelFull: Boolean
            @JvmStatic
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_FULL

        public val CameraMetadata.isHardwareLevel3: Boolean
            @JvmStatic
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_3

        public val CameraMetadata.supportsManualSensor: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_MANUAL_SENSOR)

        public val CameraMetadata.supportsManualPostProcessing: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_MANUAL_POST_PROCESSING)

        public val CameraMetadata.supportsRaw: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_RAW)

        public val CameraMetadata.supportsPrivateReprocessing: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_PRIVATE_REPROCESSING)

        public val CameraMetadata.supportsSensorSettings: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_READ_SENSOR_SETTINGS)

        public val CameraMetadata.supportsBurstCapture: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_BURST_CAPTURE)

        public val CameraMetadata.supportsYuvReprocessing: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_YUV_REPROCESSING)

        public val CameraMetadata.supportsDepthOutput: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_DEPTH_OUTPUT)

        public val CameraMetadata.supportsHighSpeedVideo: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)

        public val CameraMetadata.supportsMotionTracking: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_MOTION_TRACKING)

        public val CameraMetadata.supportsLogicalMultiCamera: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_LOGICAL_MULTI_CAMERA)

        public val CameraMetadata.supportsMonochrome: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_MONOCHROME)

        public val CameraMetadata.supportsSecureImageData: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_SECURE_IMAGE_DATA)

        public val CameraMetadata.supportsSystemCamera: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_SYSTEM_CAMERA)

        public val CameraMetadata.supportsOfflineReprocessing: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_OFFLINE_REPROCESSING)

        public val CameraMetadata.supportsUltraHighResolutionSensor: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR)

        public val CameraMetadata.supportsRemosaicProcessing: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_REMOSAIC_REPROCESSING)

        public val CameraMetadata.supportsDynamicRangeTenBit: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_DYNAMIC_RANGE_TEN_BIT)

        public val CameraMetadata.supportsStreamUseCase: Boolean
            @JvmStatic get() = this.availableCapabilities.contains(CAPABILITIES_STREAM_USE_CASE)

        public val CameraMetadata.supportsColorSpaceProfiles: Boolean
            @JvmStatic
            get() = this.availableCapabilities.contains(CAPABILITIES_COLOR_SPACE_PROFILES)

        public val CameraMetadata.availableColorSpaceProfiles: CameraColorSpaceProfiles?
            @JvmStatic
            get() =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    this[CameraCharacteristics.REQUEST_AVAILABLE_COLOR_SPACE_PROFILES]?.let {
                        Camera2ColorSpaceProfiles(it)
                    }
                } else {
                    null
                }

        public val CameraMetadata.supportsAutoFocusTrigger: Boolean
            @JvmStatic
            get() {
                val minFocusDistance = this[CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE]
                if (minFocusDistance != null) {
                    return minFocusDistance > 0
                }
                val availableAfModes =
                    this[CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES] ?: return false
                return availableAfModes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO) ||
                    availableAfModes.contains(CaptureRequest.CONTROL_AF_MODE_MACRO) ||
                    availableAfModes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) ||
                    availableAfModes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            }

        /**
         * Returns `true` if overriding zoom settings is supported on the device, otherwise `false`.
         */
        public val CameraMetadata.supportsZoomOverride: Boolean
            @JvmStatic
            get() =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    Api34Compat.isZoomOverrideSupported(this)

        /**
         * Returns `true` if configuring torch strength is supported on the device, otherwise
         * `false`.
         */
        public val CameraMetadata.supportsTorchStrength: Boolean
            @JvmStatic
            get() =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                    Api35Compat.isTorchStrengthSupported(this)

        /**
         * Returns the maximum torch strength level supported by the device.
         *
         * The torch strength is applied only when [CaptureRequest.CONTROL_AE_MODE] is set to
         * [CaptureRequest.CONTROL_AE_MODE_ON] and [CaptureRequest.FLASH_MODE] is set to
         * [CaptureRequest.FLASH_MODE_TORCH].
         *
         * Framework returns `1` when configuring torch strength is not supported on the device.
         * This method also returns `1` when the API level doesn't met to align the behavior.
         */
        public val CameraMetadata.maxTorchStrengthLevel: Int
            @JvmStatic
            get() =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                    Api35Compat.getMaxTorchStrengthLevel(this)
                else 1

        /**
         * Returns the default torch strength level.
         *
         * The torch strength is applied only when [CaptureRequest.CONTROL_AE_MODE] is set to
         * [CaptureRequest.CONTROL_AE_MODE_ON] and [CaptureRequest.FLASH_MODE] is set to
         * [CaptureRequest.FLASH_MODE_TORCH].
         *
         * Framework returns `1` when configuring torch strength is not supported on the device.
         * This method also returns `1` when the API level doesn't met to align the behavior.
         */
        public val CameraMetadata.defaultTorchStrengthLevel: Int
            @JvmStatic
            get() =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                    Api35Compat.getDefaultTorchStrengthLevel(this)
                else 1

        public val CameraMetadata.supportsLowLightBoost: Boolean
            @JvmStatic
            get() {
                val availableAeModes =
                    this[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES] ?: return false
                return availableAeModes.contains(
                    AeMode.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
                )
            }

        public val CameraMetadata.supportsPreviewStabilization: Boolean
            @JvmStatic
            get() {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Api33Compat.supportsPreviewStabilization(this)
                } else {
                    false
                }
            }
    }
}
