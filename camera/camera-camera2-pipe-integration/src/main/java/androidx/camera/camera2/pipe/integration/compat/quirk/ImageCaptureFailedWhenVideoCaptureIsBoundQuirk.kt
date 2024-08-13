/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isBluDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isItelDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isMotorolaDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isPositivoDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isVivoDevice
import androidx.camera.core.internal.compat.quirk.SurfaceProcessingQuirk

/**
 * QuirkSummary
 * - Bug Id: b/239369953, b/331754902, b/338869048, b/339555742, b/336925549
 * - Description: When taking image with VideoCapture is bound, the capture result is returned but
 *   the resulting image can not be obtained. On Pixel 4XL API29, taking image with VideoCapture UHD
 *   is bound, camera HAL returns error. Pixel 4XL starts from API29 and API30+ work fine. On Moto
 *   E13, taking picture will time out after recording is started, even if the recording is stopped.
 *   On Samsung Tab A8, apps can't take pictures successfully when ImageCapture selects 1920x1080
 *   under Preview + VideoCapture + ImageCapture UseCase combination.
 * - Device(s): BLU Studio X10, Itel w6004, Twist 2 Pro, and Vivo 1805, Pixel 4XL API29, Moto E13,
 *   Samsung Tab A8
 */
@SuppressLint("CameraXQuirksClassDetector")
public class ImageCaptureFailedWhenVideoCaptureIsBoundQuirk :
    CaptureIntentPreviewQuirk, SurfaceProcessingQuirk {

    public companion object {
        public fun isEnabled(): Boolean {
            return isBluStudioX10 ||
                isItelW6004 ||
                isVivo1805 ||
                isPositivoTwist2Pro ||
                isPixel4XLApi29 ||
                isMotoE13 ||
                isSamsungTabA8
        }

        private val isBluStudioX10: Boolean
            get() = isBluDevice() && "studio x10".equals(Build.MODEL, ignoreCase = true)

        private val isItelW6004: Boolean
            get() = isItelDevice() && "itel w6004".equals(Build.MODEL, ignoreCase = true)

        private val isVivo1805: Boolean
            get() = isVivoDevice() && "vivo 1805".equals(Build.MODEL, ignoreCase = true)

        private val isPositivoTwist2Pro: Boolean
            get() = isPositivoDevice() && "twist 2 pro".equals(Build.MODEL, ignoreCase = true)

        private val isPixel4XLApi29: Boolean
            get() =
                "pixel 4 xl".equals(Build.MODEL, ignoreCase = true) && Build.VERSION.SDK_INT == 29

        private val isMotoE13: Boolean
            get() = isMotorolaDevice() && "moto e13".equals(Build.MODEL, ignoreCase = true)

        private val isSamsungTabA8: Boolean
            get() =
                isSamsungDevice() &&
                    ("gta8".equals(Build.DEVICE, ignoreCase = true) ||
                        "gta8wifi".equals(Build.DEVICE, ignoreCase = true))
    }

    override fun workaroundByCaptureIntentPreview(): Boolean {
        return isBluStudioX10 || isItelW6004 || isVivo1805 || isPositivoTwist2Pro
    }

    override fun workaroundBySurfaceProcessing(): Boolean {
        return isBluStudioX10 ||
            isItelW6004 ||
            isVivo1805 ||
            isPositivoTwist2Pro ||
            isPixel4XLApi29 ||
            isMotoE13 ||
            isSamsungTabA8
    }
}
