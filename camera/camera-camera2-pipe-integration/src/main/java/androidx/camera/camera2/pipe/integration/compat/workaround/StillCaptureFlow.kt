/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.StillCaptureFlashStopRepeatingQuirk

/** Returns whether or not repeating should be stopped before submitting capture request. */
public fun List<Request>.shouldStopRepeatingBeforeCapture(): Boolean {
    DeviceQuirks[StillCaptureFlashStopRepeatingQuirk::class.java] ?: return false

    var isStillCapture = false
    var isFlashEnabled = false

    forEach { request ->
        if (request.template?.value == CameraDevice.TEMPLATE_STILL_CAPTURE) {
            isStillCapture = true
        }

        isFlashEnabled =
            when (request[CaptureRequest.CONTROL_AE_MODE]) {
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH,
                CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH -> true
                else -> isFlashEnabled
            }
    }

    return isStillCapture && isFlashEnabled
}
