/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.camera2.compat.quirk.Device.isSamsungDevice
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/582678455
 * - Description: Quirk denotes that the camera HAL process crashes when the session has an HLG10
 *   output stream of UHD (3840x2160) or larger and video stabilization
 *   (CONTROL_VIDEO_STABILIZATION_MODE ON) is enabled, e.g. Preview + UHD VideoCapture with HLG10
 *   and video stabilization. The device's feature combination query reports such a session as
 *   supported, but right after the session is configured the HAL crashes with a SIGSEGV in
 *   libscaler_sw.unifunc.so (resizeAreaFast on 10-bit data), so neither the preview nor the video
 *   stream receives any frames. The same streams work with video stabilization at FHD.
 * - Device(s): Samsung Galaxy S26 series. Verified on Galaxy S26 (SM-S942U1) and Galaxy S26+
 *   (SM-S947U1), back and front cameras. Galaxy Z Fold8 (SM-F971U1), which has the same chipset, is
 *   not affected.
 *
 * @see androidx.camera.camera2.compat.workaround.Hlg10UhdVideoStabilizationValidator
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CameraCrashWhenHlg10UhdWithVideoStabilizationQuirk : Quirk {

    public companion object {
        public fun isEnabled(): Boolean = isSamsungDevice() && isS26Series()

        private fun isS26Series(): Boolean =
            when (Build.DEVICE.lowercase()) {
                "m1q", // S26 (SM-S942)
                "m2q", // S26+ (SM-S947)
                "m3q" -> true // S26 Ultra (SM-S948)
                else -> false
            }
    }
}
