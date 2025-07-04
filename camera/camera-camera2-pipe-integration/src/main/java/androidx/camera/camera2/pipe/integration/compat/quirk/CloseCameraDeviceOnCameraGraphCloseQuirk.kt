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
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isXiaomiDevice
import androidx.camera.core.impl.Quirk

/**
 * Quirk needed on devices where not closing the camera device can lead to undesirable behaviors,
 * such as switching to a new session without closing the camera device may cause native camera HAL
 * crashes, or the app getting "frozen" while CameraPipe awaits on a 1s cooldown to finally close
 * the camera device.
 *
 * QuirkSummary
 * - Bug Id: 282871038, 369300443, 425588561, 426104225
 * - Description: Instructs CameraPipe to close the camera device before creating a new capture
 *   session to avoid undesirable behaviors
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CloseCameraDeviceOnCameraGraphCloseQuirk : Quirk {

    public fun shouldCloseCameraDevice(isExtensions: Boolean): Boolean =
        if (
            isXiaomiProblematicDevice || (isSamsungProblematicDevice && !isSamsungExynos7870Device)
        ) {
            // Xiaomi 14 Ultra and Samsung API 31 ~ 34 devices need to apply the quirk only when
            // Extensions is enabled.
            isExtensions
        } else {
            true
        }

    public companion object {
        @JvmStatic
        public fun isEnabled(): Boolean {
            if (isSamsungExynos7870Device) {
                // On Exynos7870 platforms, when their 3A pipeline times out, recreating a capture
                // session has a high chance of triggering use-after-free crashes. Closing the
                // camera device helps reduce the likelihood of this happening.
                return true
            } else if (
                Build.VERSION.SDK_INT in Build.VERSION_CODES.R..Build.VERSION_CODES.TIRAMISU &&
                    (Device.isOppoDevice() || Device.isOnePlusDevice() || Device.isRealmeDevice())
            ) {
                // On Oppo-family devices from Android 11 to Android 13, a process called
                // OplusHansManager actively "freezes" app processes, which means we cannot delay
                // closing the camera device for any amount of time.
                return true
            } else if (isXiaomiProblematicDevice) {
                // When Extensions is enabled, switching modes might cause the black screen issue.
                // Applying this quirk when Extensions is enabled will fix it.
                return true
            } else if (isSamsungProblematicDevice) {
                // When Extensions is enabled, there might be some timing issue to cause the
                // BindUnbindUseCasesStressTest to run fail easily. Applying this quirk will fix it.
                return true
            }
            return false
        }

        private val isSamsungExynos7870Device: Boolean = Build.HARDWARE == "samsungexynos7870"

        // Xiaomi 14 Ultra and Xiaomi 14 to apply the quirk when Extensions is enabled.
        private val isXiaomiProblematicDevice: Boolean =
            isXiaomiDevice() && arrayOf("aurora", "houji").contains(Build.DEVICE.lowercase())

        // Samsung API 31 ~ 34 devices to apply the quirk when Extensions is enabled.
        private val isSamsungProblematicDevice: Boolean =
            isSamsungDevice() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
}
