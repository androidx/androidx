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
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.core.impl.Quirk

/**
 * Quirk denoting Af Region is incorrectly flipped horizontally.
 *
 * QuirkSummary
 * - Bug Id: 210548792
 * - Description: Regions set in [android.hardware.camera2.CaptureRequest.CONTROL_AF_REGIONS] are
 *   incorrectly flipped horizontally when using front-facing cameras.
 * - Device(s): All Samsung devices.
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
class AfRegionFlipHorizontallyQuirk : Quirk {
    companion object {
        fun isEnabled(cameraMetadata: CameraMetadata) =
            isSamsungDevice() &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && // Samsung fixed it in T.
                (cameraMetadata[CameraCharacteristics.LENS_FACING] ==
                    CameraCharacteristics.LENS_FACING_FRONT)
    }
}
