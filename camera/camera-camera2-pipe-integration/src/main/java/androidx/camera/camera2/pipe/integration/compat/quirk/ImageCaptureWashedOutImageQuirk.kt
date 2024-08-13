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
 * QuirkSummary
 * - Bug Id: 176399765, 181966663
 * - Description: Quirk that prevents from getting washed out image while taking picture with flash
 *   ON/AUTO mode.
 * - Device(s): Galaxy S7, Galaxy S7+
 *
 * @see UseTorchAsFlashQuirk
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class ImageCaptureWashedOutImageQuirk : UseTorchAsFlashQuirk {

    public companion object {
        public val BUILD_MODELS: List<String> =
            listOf(
                // List of devices with the issue. See b/181966663.
                "SM-G9300", // Galaxy S7
                "SM-G930R",
                "SM-G930A",
                "SM-G930V",
                "SM-G930T",
                "SM-G930U",
                "SM-G930P", // Galaxy S7+
                "SM-SC02H",
                "SM-SCV33",
                "SM-G9350",
                "SM-G935R",
                "SM-G935A",
                "SM-G935V",
                "SM-G935T",
                "SM-G935U",
                "SM-G935P"
            )

        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            return BUILD_MODELS.contains(Build.MODEL.uppercase()) &&
                cameraMetadata[LENS_FACING] == LENS_FACING_BACK
        }
    }
}
