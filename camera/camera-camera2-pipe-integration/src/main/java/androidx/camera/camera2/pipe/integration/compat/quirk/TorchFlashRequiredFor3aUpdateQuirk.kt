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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.impl.isExternalFlashAeModeSupported
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: 294870640
 * - Description: Quirk denoting the devices where [CaptureRequest.FLASH_MODE_TORCH] has to be set
 *   for 3A states to be updated with good values (in some cases, AWB scanning is not triggered at
 *   all). This results in problems like color tint or bad exposure in captured image during
 *   captures where lighting condition changes (e.g. screen flash capture). This maybe required even
 *   if a flash unit is not available (e.g. with front camera) and
 *   [CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER] has been requested. If
 *   [CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH] is supported, it can be used instead and
 *   thus setting `FLASH_MODE_TORCH` won't be required.
 * - Device(s): Pixel 6A, 6 PRO, 7, 7A, 7 PRO, 8, 8 PRO.
 */
@SuppressLint("CameraXQuirksClassDetector")
// TODO: b/270421716 - enable when kotlin is supported.
public class TorchFlashRequiredFor3aUpdateQuirk(private val cameraMetadata: CameraMetadata) :
    Quirk {
    /**
     * Returns whether [CaptureRequest.FLASH_MODE_TORCH] is required to be set.
     *
     * This will check if the [CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH] is supported, which
     * is more recommended than using a quirk like using `FLASH_MODE_TORCH`.
     */
    public fun isFlashModeTorchRequired(): Boolean =
        !cameraMetadata.isExternalFlashAeModeSupported()

    public companion object {
        private val AFFECTED_PIXEL_MODELS: List<String> =
            mutableListOf(
                "PIXEL 6A",
                "PIXEL 6 PRO",
                "PIXEL 7",
                "PIXEL 7A",
                "PIXEL 7 PRO",
                "PIXEL 8",
                "PIXEL 8 PRO"
            )

        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean =
            isAffectedModel(cameraMetadata)

        private fun isAffectedModel(cameraMetadata: CameraMetadata) =
            isAffectedPixelModel() && cameraMetadata.isFrontCamera

        private fun isAffectedPixelModel(): Boolean {
            AFFECTED_PIXEL_MODELS.forEach { model ->
                if (Build.MODEL.uppercase() == model) {
                    return true
                }
            }
            return false
        }

        private val CameraMetadata.isFrontCamera: Boolean
            get() = this[CameraCharacteristics.LENS_FACING] == LENS_FACING_FRONT
    }
}
