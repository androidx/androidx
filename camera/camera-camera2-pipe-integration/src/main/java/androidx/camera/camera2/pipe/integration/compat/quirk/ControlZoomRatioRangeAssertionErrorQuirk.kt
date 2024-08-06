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
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isJioDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isVivoDevice
import androidx.camera.core.impl.Quirk
import java.nio.BufferUnderflowException

/**
 * A quirk for devices that throw a [BufferUnderflowException] when querying the flash availability.
 *
 * QuirkSummary
 * - Bug Id: 231701345
 * - Description: When attempting to retrieve the [CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE]
 *   characteristic, an [AssertionError] is thrown. This is an undocumented exception on the
 *   [CameraCharacteristics.get] method, so this violates the API contract.
 * - Device(s): Jio JioPhone Next, Samsung Galaxy A02s, Vivo V2039
 *
 * @see androidx.camera.camera2.pipe.integration.compat.workaround.getControlZoomRatioRangeSafely
 */
@SuppressLint("CameraXQuirksClassDetector") // TODO(b/270421716): enable when kotlin is supported.
class ControlZoomRatioRangeAssertionErrorQuirk : Quirk {
    companion object {
        fun isEnabled() = isJioPhoneNext() || isSamsungA2s() || isVivo2039()

        private fun isJioPhoneNext() =
            isJioDevice() && Build.MODEL.startsWith("LS1542QW", ignoreCase = true)

        private fun isSamsungA2s() =
            isSamsungDevice() &&
                (Build.MODEL.startsWith("SM-A025", ignoreCase = true) ||
                    Build.MODEL.equals("SM-S124DL", ignoreCase = true))

        private fun isVivo2039() =
            isVivoDevice() && Build.MODEL.equals("VIVO 2039", ignoreCase = true)
    }
}
