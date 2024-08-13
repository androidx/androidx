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
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata

/**
 * QuirkSummary
 * - Bug Id: b/316560705
 * - Description: Quirk indicates the recorded video contains obvious temporal noise. The issue
 *   happens on Pixel 8 front camera and when the template type is [CameraDevice.TEMPLATE_RECORD].
 * - Device(s): Pixel 8.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class TemporalNoiseQuirk : CaptureIntentPreviewQuirk {

    public companion object {
        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            return isPixel8 && cameraMetadata[LENS_FACING] == LENS_FACING_FRONT
        }

        private val isPixel8: Boolean
            get() = "Pixel 8".equals(Build.MODEL, ignoreCase = true)
    }
}
