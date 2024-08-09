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
package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isGoogleDevice
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/123897971
 * - Description: Quirk required to turn on/off HDR+ on Pixel devices by enabling/disabling
 *   zero-shutter-lag (ZSL) mode on the capture request, depending on the image capture use case's
 *   capture mode, i.e. prioritizing image capture latency over quality, or vice versa. This means
 *   that when the capture mode is [ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY], HDR+ is turned off
 *   by disabling ZSL, and when it is [ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY], HDR+ is turned
 *   on by enabling ZSL.
 * - Device(s): Pixel 2, Pixel 2 XL, Pixel 3, Pixel 3 XL
 *
 * TODO: enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class ImageCapturePixelHDRPlusQuirk : Quirk {
    public companion object {
        private val BUILD_MODELS = listOf("Pixel 2", "Pixel 2 XL", "Pixel 3", "Pixel 3 XL")

        public fun isEnabled(): Boolean {
            return BUILD_MODELS.contains(Build.MODEL) &&
                isGoogleDevice() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        }
    }
}
