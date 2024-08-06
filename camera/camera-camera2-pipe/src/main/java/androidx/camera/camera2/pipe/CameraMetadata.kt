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
interface CameraMetadata : Metadata, UnsafeWrapper {
    operator fun <T> get(key: CameraCharacteristics.Key<T>): T?

    fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T

    val camera: CameraId
    val isRedacted: Boolean

    val keys: Set<CameraCharacteristics.Key<*>>
    val requestKeys: Set<CaptureRequest.Key<*>>
    val resultKeys: Set<CaptureResult.Key<*>>
    val sessionKeys: Set<CaptureRequest.Key<*>>

    val physicalCameraIds: Set<CameraId>
    val physicalRequestKeys: Set<CaptureRequest.Key<*>>
    val supportedExtensions: Set<Int>

    suspend fun getPhysicalMetadata(cameraId: CameraId): CameraMetadata

    fun awaitPhysicalMetadata(cameraId: CameraId): CameraMetadata

    suspend fun getExtensionMetadata(extension: Int): CameraExtensionMetadata

    fun awaitExtensionMetadata(extension: Int): CameraExtensionMetadata

    companion object {
        /**
         * Extension properties for querying the available capabilities of a camera device across
         * all API levels.
         */
        var EMPTY_INT_ARRAY = IntArray(0)

        const val CAPABILITIES_MANUAL_SENSOR = 1
        const val CAPABILITIES_MANUAL_POST_PROCESSING = 2
        const val CAPABILITIES_RAW = 3
        const val CAPABILITIES_PRIVATE_REPROCESSING = 4
        const val CAPABILITIES_READ_SENSOR_SETTINGS = 5
        const val CAPABILITIES_BURST_CAPTURE = 6
        const val CAPABILITIES_YUV_REPROCESSING = 7
        const val CAPABILITIES_DEPTH_OUTPUT = 8
        const val CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO = 9
        const val CAPABILITIES_MOTION_TRACKING = 10
        const val CAPABILITIES_LOGICAL_MULTI_CAMERA = 11
        const val CAPABILITIES_MONOCHROME = 12
        const val CAPABILITIES_SECURE_IMAGE_DATA = 13
        const val CAPABILITIES_SYSTEM_CAMERA = 14
        const val CAPABILITIES_OFFLINE_REPROCESSING = 15

        val CameraMetadata.availableCapabilities: IntArray
            get() = this[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES] ?: EMPTY_INT_ARRAY

        val CameraMetadata.isHardwareLevelExternal: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL

        val CameraMetadata.isHardwareLevelLegacy: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

        val CameraMetadata.isHardwareLevelLimited: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED

        val CameraMetadata.isHardwareLevelFull: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_FULL

        val CameraMetadata.isHardwareLevel3: Boolean
            get() = this[INFO_SUPPORTED_HARDWARE_LEVEL] == INFO_SUPPORTED_HARDWARE_LEVEL_3

        val CameraMetadata.supportsManualSensor: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_MANUAL_SENSOR)

        val CameraMetadata.supportsManualPostProcessing: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_MANUAL_POST_PROCESSING)

        val CameraMetadata.supportsRaw: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_RAW)

        val CameraMetadata.supportsPrivateReprocessing: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_PRIVATE_REPROCESSING)

        val CameraMetadata.supportsSensorSettings: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_READ_SENSOR_SETTINGS)

        val CameraMetadata.supportsBurstCapture: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_BURST_CAPTURE)

        val CameraMetadata.supportsYuvReprocessing: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_YUV_REPROCESSING)

        val CameraMetadata.supportsDepthOutput: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_DEPTH_OUTPUT)

        val CameraMetadata.supportsHighSpeedVideo: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)

        val CameraMetadata.supportsMotionTracking: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_MOTION_TRACKING)

        val CameraMetadata.supportsLogicalMultiCamera: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_LOGICAL_MULTI_CAMERA)

        val CameraMetadata.supportsMonochrome: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_MONOCHROME)

        val CameraMetadata.supportsSecureImageData: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_SECURE_IMAGE_DATA)

        val CameraMetadata.supportsSystemCamera: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_SYSTEM_CAMERA)

        val CameraMetadata.supportsOfflineReprocessing: Boolean
            get() = this.availableCapabilities.contains(CAPABILITIES_OFFLINE_REPROCESSING)

        val CameraMetadata.supportsAutoFocusTrigger: Boolean
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
