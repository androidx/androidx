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
import androidx.camera.core.impl.Quirk

/**
 * Quirk caused by a device bug that occurs on certain devices, like the Samsung A3 devices. It
 * causes the a crash after taking a picture with a
 * [android.hardware.camera2.CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH] auto-exposure
 * mode.
 *
 * QuirkSummary
 * - Bug Id: 157535165, 161730578, 194046401
 * - Description: It will cause a crash when taking pictures with flash AUTO mode.
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CrashWhenTakingPhotoWithAutoFlashAEModeQuirk : Quirk {

    public companion object {

        private val AFFECTED_MODELS =
            listOf(
                // Enables on all Galaxy A3 devices.
                "SM-A3000",
                "SM-A3009",
                "SM-A300F",
                "SM-A300FU",
                "SM-A300G",
                "SM-A300H",
                "SM-A300M",
                "SM-A300X",
                "SM-A300XU",
                "SM-A300XZ",
                "SM-A300Y",
                "SM-A300YZ",
                "SM-J510FN", // Galaxy J5
                "5059X" // TCT Alcatel 1X
            )

        @JvmStatic
        public fun isEnabled(): Boolean {
            return AFFECTED_MODELS.contains(Build.MODEL.uppercase())
        }
    }
}
