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
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.core.impl.Quirk

/**
 * Quirk about still image (non-repeating) capture quickly succeeding a repeating request leading to
 * failures.
 *
 * QuirkSummary
 * - Bug Id: 356792665
 * - Description: On some legacy devices from Samsung J1 Mini, this can lead to an invalid parameter
 *   in the repeating request resulting in a variety of failures. Waiting for the repeating request
 *   start to be completed before image capture submission can workaround such issues.
 * - Device(s): All Samsung legacy devices
 */
@SuppressLint("CameraXQuirksClassDetector") // TODO(b/270421716): enable when kotlin is supported.
public class QuickSuccessiveImageCaptureFailsRepeatingRequestQuirk : Quirk {
    public companion object {
        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean =
            isSamsungDevice() && cameraMetadata.isHardwareLevelLegacy
    }
}
