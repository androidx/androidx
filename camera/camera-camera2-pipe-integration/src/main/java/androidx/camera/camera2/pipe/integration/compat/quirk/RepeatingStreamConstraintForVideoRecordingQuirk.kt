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
import android.os.Build
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isHuaweiDevice
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: 220214040
 * - Description: The video recording fails if no repeating stream is configured with appropriate
 *   settings. For the Huawei Mate 9, the camera device may be stuck if only configuring a UHD size
 *   video recording output. It requires an extra repeating stream in at least 320x240.
 * - Device(s): Huawei Mate 9
 */
@SuppressLint("CameraXQuirksClassDetector") // TODO(b/270421716): enable when kotlin is supported.
class RepeatingStreamConstraintForVideoRecordingQuirk : Quirk {
    companion object {
        fun isEnabled() = isHuaweiMate9()

        private fun isHuaweiMate9() =
            isHuaweiDevice() && "mha-l29".equals(Build.MODEL, ignoreCase = true)
    }
}
