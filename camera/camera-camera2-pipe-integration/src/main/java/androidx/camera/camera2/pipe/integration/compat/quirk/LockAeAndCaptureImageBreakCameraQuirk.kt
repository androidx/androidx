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
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.compat.workaround.Lock3ABehaviorWhenCaptureImage
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/360106037
 * - Description: Quirk indicating that locking AE (Auto Exposure) and taking pictures can lead to
 *   an abnormal camera service state on Pixel 3 back camera. Although the picture is successfully
 *   taken, the camera service becomes unresponsive without any error callbacks. Reopening the
 *   camera can restore its functionality.
 * - Device(s): Pixel 3.
 *
 * @see Lock3ABehaviorWhenCaptureImage
 */
@SuppressLint("CameraXQuirksClassDetector")
public class LockAeAndCaptureImageBreakCameraQuirk : Quirk {

    public companion object {
        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            return isPixel3 && cameraMetadata[LENS_FACING] == LENS_FACING_BACK
        }

        private val isPixel3: Boolean
            get() = "Pixel 3".equals(Build.MODEL, ignoreCase = true)
    }
}
