/*
 * Copyright 2022 The Android Open Source Project
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

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraAccessException.CAMERA_DISABLED
import android.hardware.camera2.CameraAccessException.CAMERA_DISCONNECTED
import android.hardware.camera2.CameraAccessException.CAMERA_ERROR
import android.hardware.camera2.CameraAccessException.CAMERA_IN_USE
import android.hardware.camera2.CameraAccessException.MAX_CAMERAS_IN_USE
import android.hardware.camera2.CameraDevice.StateCallback
import androidx.annotation.RequiresApi

@JvmInline
@RequiresApi(21)
value class CameraError private constructor(val value: Int) {
    companion object {
        /**
         * Convenient placeholder for errors like CameraAccessException.CAMERA_ERROR where the cause
         * of the error will be determined later through onError().
         */
        val ERROR_UNDETERMINED = CameraError(0)

        /**
         * Indicates that the camera is in use. This can occur when:
         * - Camera is in use by another app or process.
         * - Camera is in use by a higher priority process
         */
        val ERROR_CAMERA_IN_USE = CameraError(1)

        /**
         * The system-wide limit for number of open cameras or camera resources has been reached.
         * - This can happen on devices that allow the app to open multiple camera devices. If the
         *   number of cameras has reached the system-wide limit, and the app attempts to open
         *   another one, this error can occur.
         */
        val ERROR_CAMERA_LIMIT_EXCEEDED = CameraError(2)

        /**
         * The camera is disabled and cannot be opened. This can occur when:
         * - Camera(s) are disabled due to security policy on the device.
         * - Camera(s) are disabled because the app is not considered "foreground".
         */
        val ERROR_CAMERA_DISABLED = CameraError(3)

        /**
         * The camera device has encountered a fatal error. This can occur when:
         * - A critical error took place in the camera HAL.
         * - A critical error in the driver or the underlying hardware.
         */
        val ERROR_CAMERA_DEVICE = CameraError(4)

        /**
         * The camera service (framework) has encountered a fatal error. This can occur when:
         * - There are issues within the camera service itself.
         * - Camera service received erroneous data from the camera HAL.
         * - We have Native-level crashes that brought down the camera service.
         * - There are persistent hardware issues preventing the service from running.
         */
        val ERROR_CAMERA_SERVICE = CameraError(5)

        /**
         * The camera has been disconnected for one of the following potential causes:
         * - The camera has been disconnected from the Android device.
         * - The camera ID is no longer valid.
         * - The camera has been taken for a higher-priority process.
         */
        val ERROR_CAMERA_DISCONNECTED = CameraError(6)

        /**
         * This indicates that we received IllegalArgumentException while opening the camera.
         */
        val ERROR_ILLEGAL_ARGUMENT_EXCEPTION = CameraError(7)

        /**
         * This indicates that we received SecurityException while opening the camera.
         */
        val ERROR_SECURITY_EXCEPTION = CameraError(8)

        internal fun from(throwable: Throwable) =
            when (throwable) {
                is CameraAccessException -> from(throwable)
                is IllegalArgumentException -> ERROR_ILLEGAL_ARGUMENT_EXCEPTION
                is SecurityException -> ERROR_SECURITY_EXCEPTION
                else -> {
                    throw IllegalArgumentException("Unexpected throwable: $throwable")
                }
            }

        internal fun from(exception: CameraAccessException) =
            when (exception.reason) {
                CAMERA_DISABLED -> ERROR_CAMERA_DISABLED
                CAMERA_DISCONNECTED -> ERROR_CAMERA_DISCONNECTED
                CAMERA_ERROR -> ERROR_UNDETERMINED
                CAMERA_IN_USE -> ERROR_CAMERA_IN_USE
                MAX_CAMERAS_IN_USE -> ERROR_CAMERA_LIMIT_EXCEEDED
                else -> {
                    throw IllegalArgumentException(
                        "Unexpected CameraAccessException reason:" +
                            "${exception.reason}"
                    )
                }
            }

        internal fun from(stateCallbackError: Int) =
            when (stateCallbackError) {
                StateCallback.ERROR_CAMERA_IN_USE -> ERROR_CAMERA_IN_USE
                StateCallback.ERROR_MAX_CAMERAS_IN_USE -> ERROR_CAMERA_LIMIT_EXCEEDED
                StateCallback.ERROR_CAMERA_DISABLED -> ERROR_CAMERA_DISABLED
                StateCallback.ERROR_CAMERA_DEVICE -> ERROR_CAMERA_DEVICE
                StateCallback.ERROR_CAMERA_SERVICE -> ERROR_CAMERA_SERVICE
                else -> {
                    throw IllegalArgumentException(
                        "Unexpected StateCallback error code:" +
                            "$stateCallbackError"
                    )
                }
            }
    }
}