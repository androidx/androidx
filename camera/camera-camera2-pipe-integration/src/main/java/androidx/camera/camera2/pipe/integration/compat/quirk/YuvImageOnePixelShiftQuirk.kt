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
import androidx.annotation.RequiresApi
import androidx.camera.core.internal.compat.quirk.OnePixelShiftQuirk

/**
 * QuirkSummary
 * - Bug Id: 184229033
 * - Description: On certain devices, one pixel shifted when the HAL layer converts RGB data to
 *                YUV data. It leads to the leftmost column degradation when converting YUV to
 *                RGB in applications.
 * - Device(s): Motorola MotoG3, Samsung SM-G532F/SM-J700F
 */
@SuppressLint("CameraXQuirksClassDetector") // TODO(b/270421716): enable when kotlin is supported.
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class YuvImageOnePixelShiftQuirk : OnePixelShiftQuirk {
    companion object {
        fun isEnabled() = isMotorolaMotoG3() || isSamsungSMG532F() || isSamsungSMGJ700F()

        private fun isMotorolaMotoG3() =
            "motorola".equals(Build.BRAND, ignoreCase = true) &&
                "MotoG3".equals(Build.MODEL, ignoreCase = true)

        private fun isSamsungSMG532F() =
            "samsung".equals(Build.BRAND, ignoreCase = true) &&
                "SM-G532F".equals(Build.MODEL, ignoreCase = true)

        private fun isSamsungSMGJ700F() =
            "samsung".equals(Build.BRAND, ignoreCase = true) &&
                "SM-J700F".equals(Build.MODEL, ignoreCase = true)
    }
}
