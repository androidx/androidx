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
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata

/**
 * A quirk to denote the camera never fire the flash while taking picture with flash ON/AUTO mode.
 *
 * QuirkSummary
 * - Bug Id: 228800360
 * - Description: The flash doesn't fire while taking picture with flash ON/AUTO mode.
 * - Device(s): Itel w6004, Samsung Galaxy J7 (sm-j700f, sm-j710f) front camera
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class ImageCaptureFlashNotFireQuirk : UseTorchAsFlashQuirk {

    public companion object {
        // List of devices with the issue. See b/228800360.
        private val BUILD_MODELS =
            listOf(
                "itel w6004" // Itel W6004
            )
        private val BUILD_MODELS_FRONT_CAMERA =
            listOf(
                "sm-j700f", // Samsung Galaxy J7
                "sm-j710f", // Samsung Galaxy J7
            )

        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            val isFrontCameraAffected =
                BUILD_MODELS_FRONT_CAMERA.contains(Build.MODEL.lowercase()) &&
                    cameraMetadata[LENS_FACING] == LENS_FACING_FRONT
            val isAffected = BUILD_MODELS.contains(Build.MODEL.lowercase())
            return isFrontCameraAffected || isAffected
        }
    }
}
