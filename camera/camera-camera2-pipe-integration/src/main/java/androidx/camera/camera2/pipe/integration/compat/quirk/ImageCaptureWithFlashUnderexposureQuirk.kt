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
 * A quirk to denote even when the camera uses flash ON/AUTO mode, but the captured image is still
 * underexposed.
 *
 * QuirkSummary
 * - Bug Id: 228800282
 * - Description: While the flash is in ON/AUTO mode and the camera fires the flash in a dark
 *   environment, the captured photos are underexposed after continuously capturing 2 or more
 *   photos.
 * - Device(s): Samsung Galaxy A2 Core (sm-a260f), Samsung Galaxy J5 (sm-j530f), Samsung Galaxy J6
 *   (sm-j600g), Samsung Galaxy J7 Neo (sm-j701f), Samsung Galaxy J7 Prime (sm-g610f), Samsung
 *   Galaxy J7 (sm-j710mn)
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class ImageCaptureWithFlashUnderexposureQuirk : UseTorchAsFlashQuirk {
    public companion object {
        // List of devices with the issue. See b/228800282.
        private val BUILD_MODELS =
            listOf(
                "sm-a260f", // Samsung Galaxy A2 Core
                "sm-j530f", // Samsung Galaxy J5
                "sm-j600g", // Samsung Galaxy J6
                "sm-j701f", // Samsung Galaxy J7 Neo
                "sm-g610f", // Samsung Galaxy J7 Prime
                "sm-j710mn", // Samsung Galaxy J7
            )

        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            return BUILD_MODELS.contains(Build.MODEL.lowercase()) &&
                cameraMetadata[LENS_FACING] == LENS_FACING_BACK
        }
    }
}
