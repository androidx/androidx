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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata

/**
 * Camera gets stuck when taking pictures with flash ON or AUTO in dark environment.
 *
 * QuirkSummary
 * - Bug Id: 193336562, 194046401
 * - Description: Camera HAL get stuck when taking pictures with flash ON or AUTO in dark
 *   environment.
 * - Device(s): SM-N9200 and all Samsung Galaxy Note 5 devices, SM-J510FN
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CameraNoResponseWhenEnablingFlashQuirk : UseTorchAsFlashQuirk {

    public companion object {
        public val AFFECTED_MODELS: List<String> =
            listOf(
                // Enables on all Samsung Galaxy Note 5 devices.
                "SM-N9200",
                "SM-N9208",
                "SAMSUNG-SM-N920A",
                "SM-N920C",
                "SM-N920F",
                "SM-N920G",
                "SM-N920I",
                "SM-N920K",
                "SM-N920L",
                "SM-N920P",
                "SM-N920R4",
                "SM-N920R6",
                "SM-N920R7",
                "SM-N920S",
                "SM-N920T",
                "SM-N920V",
                "SM-N920W8",
                "SM-N920X",
                "SM-J510FN" // Galaxy J5
            )

        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            return AFFECTED_MODELS.contains(Build.MODEL.uppercase()) &&
                cameraMetadata[CameraCharacteristics.LENS_FACING] == LENS_FACING_BACK
        }
    }
}
