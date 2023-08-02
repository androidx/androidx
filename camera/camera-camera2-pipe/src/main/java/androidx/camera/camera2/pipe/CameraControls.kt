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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.Result3A.Status

// Public controls and enums used to interact with a CameraGraph.

/** An enum to match the CameraMetadata.CONTROL_AF_MODE_* constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
value class AfMode(val value: Int) {
    companion object {
        val OFF = AfMode(CameraMetadata.CONTROL_AF_MODE_OFF)
        val AUTO = AfMode(CameraMetadata.CONTROL_AF_MODE_AUTO)
        val MACRO = AfMode(CameraMetadata.CONTROL_AF_MODE_MACRO)
        val CONTINUOUS_VIDEO = AfMode(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        val CONTINUOUS_PICTURE = AfMode(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        val EDOF = AfMode(CameraMetadata.CONTROL_AF_MODE_EDOF)

        val values = listOf(OFF, AUTO, MACRO, CONTINUOUS_VIDEO, CONTINUOUS_PICTURE, EDOF)

        @JvmStatic
        fun fromIntOrNull(value: Int): AfMode? = values.firstOrNull { it.value == value }
    }
}

/** An enum to match the CameraMetadata.CONTROL_AE_MODE_* constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
value class AeMode(val value: Int) {
    companion object {
        val OFF = AeMode(CameraMetadata.CONTROL_AE_MODE_OFF)
        val ON = AeMode(CameraMetadata.CONTROL_AE_MODE_ON)
        val ON_ALWAYS_FLASH = AeMode(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        val ON_AUTO_FLASH = AeMode(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH)
        val ON_AUTO_FLASH_REDEYE = AeMode(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE)
        val ON_EXTERNAL_FLASH = AeMode(CameraMetadata.CONTROL_AE_MODE_ON_EXTERNAL_FLASH)

        val values =
            listOf(OFF, ON, ON_AUTO_FLASH, ON_ALWAYS_FLASH, ON_AUTO_FLASH_REDEYE, ON_EXTERNAL_FLASH)

        @JvmStatic
        fun fromIntOrNull(value: Int): AeMode? = values.firstOrNull { it.value == value }
    }
}

/** An enum to match the CameraMetadata.CONTROL_AWB_MODE_* constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
value class AwbMode(val value: Int) {
    companion object {
        val OFF = AwbMode(CameraMetadata.CONTROL_AWB_MODE_OFF)
        val AUTO = AwbMode(CameraMetadata.CONTROL_AWB_MODE_AUTO)
        val CLOUDY_DAYLIGHT = AwbMode(CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT)
        val DAYLIGHT = AwbMode(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT)
        val INCANDESCENT = AwbMode(CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT)
        val FLUORESCENT = AwbMode(CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT)
        val SHADE = AwbMode(CameraMetadata.CONTROL_AWB_MODE_SHADE)
        val TWILIGHT = AwbMode(CameraMetadata.CONTROL_AWB_MODE_TWILIGHT)

        val values =
            listOf(OFF, AUTO, CLOUDY_DAYLIGHT, DAYLIGHT, INCANDESCENT, FLUORESCENT, SHADE, TWILIGHT)

        @JvmStatic
        fun fromIntOrNull(value: Int): AwbMode? = values.firstOrNull { it.value == value }
    }
}

/** An enum to match the CameraMetadata.FLASH_MODE_* constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
value class FlashMode(val value: Int) {
    companion object {
        val OFF = FlashMode(CameraMetadata.FLASH_MODE_OFF)
        val SINGLE = FlashMode(CameraMetadata.FLASH_MODE_SINGLE)
        val TORCH = FlashMode(CameraMetadata.FLASH_MODE_TORCH)

        private val values = listOf(OFF, SINGLE, TORCH)

        @JvmStatic
        fun fromIntOrNull(value: Int): FlashMode? = values.firstOrNull { it.value == value }
    }
}

/**
 * Enum to turn the torch on/off.
 *
 * <https://developer.android.com/reference/android/hardware/camera2/CameraMetadata
 *
 * #CONTROL_AE_MODE_OFF
 * https://developer.android.com/reference/android/hardware/camera2/CameraMetadata
 *
 * #CONTROL_AE_MODE_ON
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TorchState private constructor() {
    companion object {
        val ON = TorchState()
        val OFF = TorchState()
    }
}

/** Requirement to consider prior to locking auto-exposure, auto-focus and auto-whitebalance. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
value class Lock3ABehavior private constructor(val value: Int) {
    companion object {
        /**
         * This requirement means that we want to lock the values for 3A immediately.
         *
         * For AE/AWB this is achieved by asking the camera device to lock them immediately by setting
         * [android.hardware.camera2.CaptureRequest.CONTROL_AE_LOCK],
         * [android.hardware.camera2.CaptureRequest.CONTROL_AWB_LOCK] to true right away.
         *
         * For AF we immediately ask the camera device to trigger AF by setting the
         * [android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER] to
         * [android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER_START].
         */
        val IMMEDIATE = Lock3ABehavior(1)

        /**
         * Lock 3A values after their current scan is finished. If there is no active ongoing scan then
         * the values will be locked to the current values.
         */
        val AFTER_CURRENT_SCAN = Lock3ABehavior(2)

        /** Initiate a new scan, and then lock the values once the scan is done. */
        val AFTER_NEW_SCAN = Lock3ABehavior(3)
    }
}

/**
 * Return type for a 3A method.
 *
 * @param status [Status] of the 3A operation at the time of return.
 * @param frameMetadata [FrameMetadata] of the latest frame at which the method succeeded or was
 *   aborted. The metadata reflects CaptureResult or TotalCaptureResult for that frame. It can so
 *   happen that the [CaptureResult] itself has all the key-value pairs needed to determine the
 *   completion of the method, in that case this frameMetadata may not contain all the kay value
 *   pairs associated with the final result i.e. [TotalCaptureResult] of this frame.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class Result3A(val status: Status, val frameMetadata: FrameMetadata? = null) {
    /**
     * Enum to know the status of 3A operation in case the method returns before the desired
     * operation is complete. The reason could be that the operation was talking a lot longer and an
     * enforced frame or time limit was reached, submitting the desired request to camera failed
     * etc.
     */
    @JvmInline
    value class Status private constructor(val value: Int) {
        companion object {
            val OK = Status(0)
            val FRAME_LIMIT_REACHED = Status(1)
            val TIME_LIMIT_REACHED = Status(2)
            val SUBMIT_CANCELLED = Status(3)
            val SUBMIT_FAILED = Status(4)
        }
    }
}
