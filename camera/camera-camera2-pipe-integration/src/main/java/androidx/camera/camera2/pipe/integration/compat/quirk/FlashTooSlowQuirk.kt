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
package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata

/**
 * Quirks that denotes the device has a slow flash sequence that could result in blurred pictures.
 *
 * QuirkSummary
 * - Bug Id: 211474332, 286190938, 280221967, 296814664, 296816175
 * - Description: When capturing still photos in auto flash mode, it needs more than 1 second to
 *   flash or capture actual photo after flash, and therefore it easily results in blurred or dark
 *   or overexposed pictures.
 * - Device(s): Pixel 3a / Pixel 3a XL, all models of Pixel 4 and 5, SM-A320, Moto G20, Itel A48,
 *   Realme C11 2021
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class FlashTooSlowQuirk : UseTorchAsFlashQuirk {

    public companion object {
        private val AFFECTED_MODEL_PREFIXES =
            listOf(
                "PIXEL 3A",
                "PIXEL 3A XL",
                "PIXEL 4", // includes Pixel 4 XL, 4A, and 4A (5g) too
                "PIXEL 5", // includes Pixel 5A too
                "SM-A320",
                "MOTO G(20)",
                "ITEL L6006", // Itel A48
                "RMX3231" // Realme C11 2021
            )

        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            return isAffectedModel() && cameraMetadata[LENS_FACING] == LENS_FACING_BACK
        }

        private fun isAffectedModel(): Boolean {
            for (modelPrefix in AFFECTED_MODEL_PREFIXES) {
                if (Build.MODEL.uppercase().startsWith(modelPrefix)) {
                    return true
                }
            }
            return false
        }
    }
}
