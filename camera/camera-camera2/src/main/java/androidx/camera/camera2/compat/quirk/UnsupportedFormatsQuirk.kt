/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.core.impl.Quirk

/**
 * Quirk needed on devices where certain image formats are advertised but are broken or cause
 * crashes when used.
 *
 * QuirkSummary
 * - Bug Id: 532824830
 * - Description: Disables support for specific image formats. On OPPO A5 2020, the camera HAL
 *   advertises RAW_SENSOR format support on the back camera but captures are broken due to
 *   mismatched resolutions, so RAW_SENSOR is disabled for the back camera.
 * - Device(s): OPPO A5 2020 (CPH1931 / OP4B79L1)
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class UnsupportedFormatsQuirk : Quirk {

    /** Returns the list of image formats that are unsupported on the current camera. */
    public fun getUnsupportedFormats(cameraMetadata: CameraMetadata?): List<Int> {
        val unsupportedFormats = mutableListOf<Int>()
        if (isOppoA5() && isBackCamera(cameraMetadata)) {
            unsupportedFormats.add(ImageFormat.RAW_SENSOR)
        }
        return unsupportedFormats
    }

    public companion object {
        public fun isEnabled(): Boolean {
            return isOppoA5()
        }

        private fun isOppoA5(): Boolean {
            return "OPPO".equals(Build.MANUFACTURER, ignoreCase = true) &&
                ("CPH1931".equals(Build.MODEL, ignoreCase = true) ||
                    "OP4B79L1".equals(Build.DEVICE, ignoreCase = true))
        }

        private fun isBackCamera(cameraMetadata: CameraMetadata?): Boolean {
            if (cameraMetadata == null) {
                // If metadata is null, assume it is back camera defensively on OPPO A5.
                return true
            }
            val lensFacing = cameraMetadata.get(CameraCharacteristics.LENS_FACING)
            return lensFacing == CameraCharacteristics.LENS_FACING_BACK
        }
    }
}
