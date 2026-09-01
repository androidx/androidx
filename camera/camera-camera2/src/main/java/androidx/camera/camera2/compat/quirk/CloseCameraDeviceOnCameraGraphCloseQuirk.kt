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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.camera2.compat.quirk.Device.isSamsungDevice
import androidx.camera.camera2.compat.quirk.Device.isSonyDevice
import androidx.camera.camera2.compat.quirk.Device.isXiaomiDevice
import androidx.camera.core.impl.Quirk

/**
 * Quirk needed on devices where not closing the camera device can lead to undesirable behaviors,
 * such as switching to a new session without closing the camera device may cause native camera HAL
 * crashes, or the app getting "frozen" while CameraPipe awaits on a 1s cooldown to finally close
 * the camera device.
 *
 * QuirkSummary
 * - Bug Id: 282871038, 369300443, 425588561, 426104225, 369291594, 385881561
 * - Description: Instructs CameraPipe to close the camera device before creating a new capture
 *   session to avoid undesirable behaviors
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CloseCameraDeviceOnCameraGraphCloseQuirk : Quirk {

    public fun shouldCloseCameraDevice(isExtensions: Boolean): Boolean =
        if (
            isXiaomiProblematicDevice ||
                (isSamsungProblematicDevice && !isSamsungUnconditionalDevice)
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
            if (isSamsungUnconditionalDevice) {
                // On Exynos7570, Exynos7870, and Snapdragon 845 Galaxy S9 platforms, recreating
                // a capture session without closing the camera device can trigger native HAL
                // crashes or pipeline stalls. Closing the camera device resolves this issue.
                return true
            } else if (
                Build.VERSION.SDK_INT in Build.VERSION_CODES.R..Build.VERSION_CODES.TIRAMISU &&
                    (Device.isOppoDevice() || Device.isOnePlusDevice() || Device.isRealmeDevice())
            ) {
                // On Oppo-family devices from Android 11 to Android 13, a process called
                // OplusHansManager actively "freezes" app processes, which means we cannot delay
                // closing the camera device for any amount of time.
                return true
            } else if (Device.isVivoDevice()) {
                // On Vivo devices, buggy custom modifications were added during CameraDevice.close
                // such that it may trigger NPE if the camera device is closed too late.
                return true
            } else if (isXiaomiProblematicDevice) {
                // When Extensions is enabled, switching modes might cause the black screen issue.
                // Applying this quirk when Extensions is enabled will fix it.
                return true
            } else if (isSamsungProblematicDevice) {
                // When Extensions is enabled, there might be some timing issue to cause the
                // BindUnbindUseCasesStressTest to run fail easily. Applying this quirk will fix it.
                return true
            } else if (isSonyProblematicDevice) {
                return true
            }
            return false
        }

        private val isSamsungUnconditionalDevice: Boolean
            get() =
                isSamsungExynos7570Device ||
                    isSamsungExynos7870Device ||
                    isSamsungGalaxyS9SnapdragonDevice

        private val isSamsungExynos7570Device: Boolean
            get() =
                isSamsungDevice() &&
                    (Build.HARDWARE.contains("7570", ignoreCase = true) ||
                        Build.BOARD.contains("7570", ignoreCase = true))

        private val isSamsungExynos7870Device: Boolean
            get() =
                isSamsungDevice() &&
                    (Build.HARDWARE.contains("7870", ignoreCase = true) ||
                        Build.BOARD.contains("7870", ignoreCase = true))

        private val isSamsungGalaxyS9SnapdragonDevice: Boolean
            get() =
                isSamsungDevice() &&
                    (listOf("starqlte", "star2qlte").any {
                        Build.DEVICE.startsWith(it, ignoreCase = true)
                    } ||
                        listOf(
                                "sm-g960u",
                                "sm-g9600",
                                "sm-g960w",
                                "sm-g965u",
                                "sm-g9650",
                                "sm-g965w",
                            )
                            .any { Build.MODEL.startsWith(it, ignoreCase = true) })

        // Xiaomi 14 Ultra and Xiaomi 14 to apply the quirk when Extensions is enabled.
        private val isXiaomiProblematicDevice: Boolean
            get() =
                isXiaomiDevice() && arrayOf("aurora", "houji").contains(Build.DEVICE.lowercase())

        private val isSonyProblematicDevice: Boolean
            get() =
                isSonyDevice() &&
                    listOf(
                            "XQ-DQ", // Sony Xperia 1 V (XQ-DQ72, XQ-DQ54 etc.), ref: b/445897456
                            "SO", // Sony Xperia 1 V (SO-51D, SOG10), ref: b/445897456
                            "A301SO", // Sony Xperia 1 V, ref: b/445897456
                        )
                        .any { Build.DEVICE.startsWith(it, ignoreCase = true) }

        // Samsung API 31 ~ 34 devices to apply the quirk when Extensions is enabled.
        private val isSamsungProblematicDevice: Boolean
            get() =
                isSamsungDevice() &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
}
