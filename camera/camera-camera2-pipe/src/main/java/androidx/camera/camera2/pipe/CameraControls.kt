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
import androidx.camera.camera2.pipe.Result3A.Status

// Public controls and enums used to interact with a CameraGraph.

/**
 * An enum to match the CameraMetadata.CONTROL_AF_MODE_* constants.
 */
public enum class AfMode(public val value: Int) {
    OFF(CameraMetadata.CONTROL_AF_MODE_OFF),
    AUTO(CameraMetadata.CONTROL_AF_MODE_AUTO),
    MACRO(CameraMetadata.CONTROL_AF_MODE_MACRO),
    CONTINUOUS_VIDEO(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO),
    CONTINUOUS_PICTURE(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE),
    EDOF(CameraMetadata.CONTROL_AF_MODE_EDOF);

    public companion object {
        @JvmStatic
        public fun fromIntOrNull(value: Int): AfMode? = values().firstOrNull { it.value == value }
    }
}

/**
 * An enum to match the CameraMetadata.CONTROL_AE_MODE_* constants.
 */
public enum class AeMode(public val value: Int) {
    OFF(CameraMetadata.CONTROL_AE_MODE_OFF),
    ON(CameraMetadata.CONTROL_AE_MODE_ON),
    ON_AUTO_FLASH(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH),
    ON_ALWAYS_FLASH(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH),
    ON_AUTO_FLASH_REDEYE(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);

    public companion object {
        @JvmStatic
        public fun fromIntOrNull(value: Int): AeMode? = values().firstOrNull { it.value == value }
    }
}

/**
 * An enum to match the CameraMetadata.CONTROL_AWB_MODE_* constants.
 */
public enum class AwbMode(public val value: Int) {
    AUTO(CameraMetadata.CONTROL_AWB_MODE_AUTO),
    CLOUDY_DAYLIGHT(CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    DAYLIGHT(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT),
    INCANDESCENT(CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT(CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT);

    public companion object {
        @JvmStatic
        public fun fromIntOrNull(value: Int): AwbMode? = values().firstOrNull { it.value == value }
    }
}

/**
 * An enum to match the CameraMetadata.FLASH_MODE_* constants.
 */
public enum class FlashMode(public val value: Int) {
    OFF(CameraMetadata.FLASH_MODE_OFF),
    SINGLE(CameraMetadata.FLASH_MODE_SINGLE),
    TORCH(CameraMetadata.FLASH_MODE_TORCH);

    public companion object {
        @JvmStatic
        public fun fromIntOrNull(value: Int): FlashMode? = values().firstOrNull {
            it.value == value
        }
    }
}

/**
 * Enum to turn the torch on/off.
 *
 * <https://developer.android.com/reference/android/hardware/camera2/CameraMetadata
 * #CONTROL_AE_MODE_OFF
 * https://developer.android.com/reference/android/hardware/camera2/CameraMetadata
 * #CONTROL_AE_MODE_ON
 */
public enum class TorchState {
    ON,
    OFF
}

/**
 * Requirement to consider prior to locking auto-exposure, auto-focus and auto-whitebalance.
 */
public enum class Lock3ABehavior {
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
    IMMEDIATE,

    /**
     * Lock 3A values after their current scan is finished. If there is no active ongoing scan then
     * the values will be locked to the current values.
     */
    AFTER_CURRENT_SCAN,

    /**
     * Initiate a new scan, and then lock the values once the scan is done.
     */
    AFTER_NEW_SCAN,
}

/**
 * Return type for a 3A method.
 *
 * @param status [Status] of the 3A operation at the time of return.
 * @param frameMetadata [FrameMetadata] of the latest frame at which the method succeeded or was
 * aborted. The metadata reflects CaptureResult or TotalCaptureResult for that frame. It can so
 * happen that the [CaptureResult] itself has all the key-value pairs needed to determine the
 * completion of the method, in that case this frameMetadata may not contain all the kay value pairs
 * associated with the final result i.e [TotalCaptureResult] of this frame.
 */
public data class Result3A(val status: Status, val frameMetadata: FrameMetadata? = null) {
    /**
     * Enum to know the status of 3A operation in case the method returns before the desired
     * operation is complete. The reason could be that the operation was talking a lot longer and an
     * enforced frame or time limit was reached, submitting the desired request to camera failed etc.
     */
    public enum class Status {
        OK,
        FRAME_LIMIT_REACHED,
        TIME_LIMIT_REACHED,
        SUBMIT_FAILED
    }
}
