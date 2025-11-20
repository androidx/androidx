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
 * A quirk to denote the captured image is underexposed with ultra-wide camera even when the camera
 * uses flash ON/AUTO mode.
 *
 * QuirkSummary
 * - Bug Id: 444590340
 * - Description: While the flash is in ON/AUTO mode and the camera fires the flash in a dark
 *   environment, the captured photos are underexposed if ultra-wide camera is used.
 * - Device(s): Samsung S24 (sm-s921b and variants)
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class UltraWideFlashCaptureUnderexposureQuirk : UseTorchAsFlashQuirk {
    public companion object {
        // List of devices with the issue. See b/228800282.
        public val BUILD_MODEL_PREFIXES: List<String> =
            listOf(
                "sm-s921" // Samsung Galaxy S24
            )

        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            return BUILD_MODEL_PREFIXES.any { Build.MODEL.lowercase().startsWith(it) } &&
                cameraMetadata[LENS_FACING] == LENS_FACING_BACK
        }
    }
}
