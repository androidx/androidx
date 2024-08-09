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
import androidx.annotation.RestrictTo

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

        public val CameraMetadata.availableCapabilities: IntArray
            get() = this[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES] ?: EMPTY_INT_ARRAY

        public val CameraMetadata.isHardwareLevelExternal: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL

        public val CameraMetadata.isHardwareLevelLegacy: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

        public val CameraMetadata.isHardwareLevelLimited: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED

        public val CameraMetadata.isHardwareLevelFull: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_FULL

        public val CameraMetadata.isHardwareLevel3: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_3

        public val CameraMetadata.supportsManualSensor: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_MANUAL_SENSOR)

        public val CameraMetadata.supportsManualPostProcessing: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_MANUAL_POST_PROCESSING)

        public val CameraMetadata.supportsRaw: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_RAW)

        public val CameraMetadata.supportsPrivateReprocessing: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_PRIVATE_REPROCESSING)

        public val CameraMetadata.supportsSensorSettings: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_READ_SENSOR_SETTINGS)

        public val CameraMetadata.supportsBurstCapture: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_BURST_CAPTURE)

        public val CameraMetadata.supportsYuvReprocessing: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_YUV_REPROCESSING)

        public val CameraMetadata.supportsDepthOutput: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_DEPTH_OUTPUT)

        public val CameraMetadata.supportsHighSpeedVideo: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)

        public val CameraMetadata.supportsMotionTracking: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_MOTION_TRACKING)

        public val CameraMetadata.supportsLogicalMultiCamera: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_LOGICAL_MULTI_CAMERA)

        public val CameraMetadata.supportsMonochrome: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_MONOCHROME)

        public val CameraMetadata.supportsSecureImageData: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_SECURE_IMAGE_DATA)

        public val CameraMetadata.supportsSystemCamera: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_SYSTEM_CAMERA)

        public val CameraMetadata.supportsOfflineReprocessing: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_OFFLINE_REPROCESSING)

        public val CameraMetadata.supportsAutoFocusTrigger: Boolean
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
    }
}
