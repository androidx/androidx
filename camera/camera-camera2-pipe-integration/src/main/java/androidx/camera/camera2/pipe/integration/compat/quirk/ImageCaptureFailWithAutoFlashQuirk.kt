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
import android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: 228800360
 * - Description: The image capturing may fail when the camera turns on the auto flash mode, and the
 *   devices also fail to fire the flash on the flash on mode.
 * - Device(s): Samsung Galaxy J7 (sm-j700f, sm-j710f) front camera
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class ImageCaptureFailWithAutoFlashQuirk : Quirk {

    public companion object {
        /** List of devices with the issue. See b/228800360. */
        private val BUILD_MODELS_FRONT_CAMERA =
            listOf(
                "sm-j700f", // Samsung Galaxy J7
                "sm-j710f", // Samsung Galaxy J7
            )

        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            return BUILD_MODELS_FRONT_CAMERA.contains(Build.MODEL.lowercase()) &&
                cameraMetadata[LENS_FACING] == LENS_FACING_FRONT
        }
    }
}
