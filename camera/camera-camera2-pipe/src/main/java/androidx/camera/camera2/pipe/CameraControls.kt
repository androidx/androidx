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

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.Result3A.Status

// Public controls and enums used to interact with a CameraGraph.

/** An enum to match the CameraMetadata.CONTROL_AF_MODE_* constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class AfMode(public val value: Int) {
    public fun isOn(): Boolean {
        return value != CameraMetadata.CONTROL_AF_MODE_OFF
    }

    public fun isContinuous(): Boolean {
        return value == CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO ||
            value == CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
    }

    public companion object {
        public val OFF: AfMode = AfMode(CameraMetadata.CONTROL_AF_MODE_OFF)
        public val AUTO: AfMode = AfMode(CameraMetadata.CONTROL_AF_MODE_AUTO)
        public val MACRO: AfMode = AfMode(CameraMetadata.CONTROL_AF_MODE_MACRO)
        public val CONTINUOUS_VIDEO: AfMode =
            AfMode(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        public val CONTINUOUS_PICTURE: AfMode =
            AfMode(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        public val EDOF: AfMode = AfMode(CameraMetadata.CONTROL_AF_MODE_EDOF)
        public val values: List<AfMode> =
            listOf(OFF, AUTO, MACRO, CONTINUOUS_VIDEO, CONTINUOUS_PICTURE, EDOF)

        @JvmStatic
        public fun fromIntOrNull(value: Int): AfMode? = values.firstOrNull { it.value == value }
    }
}

/** An enum to match the CameraMetadata.CONTROL_AE_MODE_* constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class AeMode(public val value: Int) {
    public fun isOn(): Boolean {
        return value != CameraMetadata.CONTROL_AE_MODE_OFF
    }

    public companion object {
        public val OFF: AeMode = AeMode(CameraMetadata.CONTROL_AE_MODE_OFF)
        public val ON: AeMode = AeMode(CameraMetadata.CONTROL_AE_MODE_ON)
        public val ON_ALWAYS_FLASH: AeMode = AeMode(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        public val ON_AUTO_FLASH: AeMode = AeMode(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH)
        public val ON_AUTO_FLASH_REDEYE: AeMode =
            AeMode(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE)
        public val ON_EXTERNAL_FLASH: AeMode =
            AeMode(CameraMetadata.CONTROL_AE_MODE_ON_EXTERNAL_FLASH)

        public val values: List<AeMode> =
            listOf(OFF, ON, ON_AUTO_FLASH, ON_ALWAYS_FLASH, ON_AUTO_FLASH_REDEYE, ON_EXTERNAL_FLASH)

        @JvmStatic
        public fun fromIntOrNull(value: Int): AeMode? = values.firstOrNull { it.value == value }
    }
}

/** An enum to match the CameraMetadata.CONTROL_AWB_MODE_* constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class AwbMode(public val value: Int) {
    public fun isOn(): Boolean {
        return value != CameraMetadata.CONTROL_AWB_MODE_OFF
    }

    public companion object {
        public val OFF: AwbMode = AwbMode(CameraMetadata.CONTROL_AWB_MODE_OFF)
        public val AUTO: AwbMode = AwbMode(CameraMetadata.CONTROL_AWB_MODE_AUTO)
        public val CLOUDY_DAYLIGHT: AwbMode =
            AwbMode(CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT)
        public val DAYLIGHT: AwbMode = AwbMode(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT)
        public val INCANDESCENT: AwbMode = AwbMode(CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT)
        public val FLUORESCENT: AwbMode = AwbMode(CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT)
        public val SHADE: AwbMode = AwbMode(CameraMetadata.CONTROL_AWB_MODE_SHADE)
        public val TWILIGHT: AwbMode = AwbMode(CameraMetadata.CONTROL_AWB_MODE_TWILIGHT)

        public val values: List<AwbMode> =
            listOf(OFF, AUTO, CLOUDY_DAYLIGHT, DAYLIGHT, INCANDESCENT, FLUORESCENT, SHADE, TWILIGHT)

        @JvmStatic
        public fun fromIntOrNull(value: Int): AwbMode? = values.firstOrNull { it.value == value }
    }
}

/** An enum to match the CameraMetadata.FLASH_MODE_* constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class FlashMode(public val value: Int) {
    public companion object {
        public val OFF: FlashMode = FlashMode(CameraMetadata.FLASH_MODE_OFF)
        public val SINGLE: FlashMode = FlashMode(CameraMetadata.FLASH_MODE_SINGLE)
        public val TORCH: FlashMode = FlashMode(CameraMetadata.FLASH_MODE_TORCH)

        private val values = listOf(OFF, SINGLE, TORCH)

        @JvmStatic
        public fun fromIntOrNull(value: Int): FlashMode? = values.firstOrNull { it.value == value }
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
public class TorchState private constructor() {
    public companion object {
        public val ON: TorchState = TorchState()
        public val OFF: TorchState = TorchState()
    }
}

/** Requirement to consider prior to locking auto-exposure, auto-focus and auto-whitebalance. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class Lock3ABehavior private constructor(public val value: Int) {
    public companion object {
        /**
         * This requirement means that we want to lock the values for 3A immediately.
         *
         * For AE/AWB this is achieved by asking the camera device to lock them immediately by
         * setting [android.hardware.camera2.CaptureRequest.CONTROL_AE_LOCK],
         * [android.hardware.camera2.CaptureRequest.CONTROL_AWB_LOCK] to true right away.
         *
         * For AF we immediately ask the camera device to trigger AF by setting the
         * [android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER] to
         * [android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER_START].
         */
        public val IMMEDIATE: Lock3ABehavior = Lock3ABehavior(1)

        /**
         * Lock 3A values after their current scan is finished. If there is no active ongoing scan
         * then the values will be locked to the current values.
         */
        public val AFTER_CURRENT_SCAN: Lock3ABehavior = Lock3ABehavior(2)

        /** Initiate a new scan, and then lock the values once the scan is done. */
        public val AFTER_NEW_SCAN: Lock3ABehavior = Lock3ABehavior(3)
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
public data class Result3A(val status: Status, val frameMetadata: FrameMetadata? = null) {
    /**
     * Enum to know the status of 3A operation in case the method returns before the desired
     * operation is complete. The reason could be that the operation was talking a lot longer and an
     * enforced frame or time limit was reached, submitting the desired request to camera failed
     * etc.
     */
    @JvmInline
    public value class Status private constructor(public val value: Int) {
        public companion object {
            public val OK: Status = Status(0)
            public val FRAME_LIMIT_REACHED: Status = Status(1)
            public val TIME_LIMIT_REACHED: Status = Status(2)
            public val SUBMIT_CANCELLED: Status = Status(3)
            public val SUBMIT_FAILED: Status = Status(4)
        }
    }
}
