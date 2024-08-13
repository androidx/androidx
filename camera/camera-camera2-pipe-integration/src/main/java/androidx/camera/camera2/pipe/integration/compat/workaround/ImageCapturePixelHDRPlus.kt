/*
 * Copyright 2020 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ImageCapturePixelHDRPlusQuirk
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.ImageCaptureConfig

/**
 * Turns on or turns off HDR+ on Pixel devices depending on the image capture use case's capture
 * mode. When the mode is [ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY], HDR+ is turned off by
 * disabling ZSL. When the mode is [ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY], HDR+ is turned on
 * by enabling ZSL.
 *
 * @see ImageCapturePixelHDRPlusQuirk
 */
@SuppressLint("NewApi")
public fun Camera2ImplConfig.Builder.toggleHDRPlus(imageCaptureConfig: ImageCaptureConfig) {

    DeviceQuirks[ImageCapturePixelHDRPlusQuirk::class.java] ?: return
    if (!imageCaptureConfig.hasCaptureMode()) return

    when (imageCaptureConfig.captureMode) {
        // enable ZSL to make sure HDR+ is enabled
        ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY ->
            setCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, true)

        // disable ZSL to turn off HDR+
        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY ->
            setCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, false)
    }
}
