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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.camera2.compat.quirk.Device.isMotorolaDevice
import androidx.camera.camera2.compat.quirk.Device.isSamsungDevice
import androidx.camera.camera2.compat.quirk.Device.isXiaomiDevice
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: 252818931, 261744070, 319913852, 361328838
 * - Description: On certain devices, the captured image has color or zoom freezing issue for
 *   reprocessing. We need to disable zero-shutter lag and return false for
 *   [androidx.camera.core.CameraInfo.isZslSupported].
 * - Device(s): Samsung Fold4, Samsung S6/S22/A05 series, Xiaomi Mi 8, Redmi Note 11, Redmi Note 12,
 *   Motorola Razr+ 2024, Honor 90
 */
@SuppressLint("CameraXQuirksClassDetector")
// TODO(b/270421716): enable when kotlin is supported.
public class ZslDisablerQuirk : Quirk {

    public companion object {
        private val AFFECTED_SAMSUNG_MODEL =
            listOf(
                "SM-F936",
                "SM-S901U",
                "SM-S908U",
                "SM-S908U1",
                "SM-F721",
                "SM-S928U1",
                "SM-G920",
                "SM-A057",
            )

        private val AFFECTED_XIAOMI_MODEL = listOf("MI 8", "2201117", "23028RA60")

        private val AFFECTED_MOTOROLA_MODEL = listOf("MOTOROLA RAZR PLUS 2024")

        private val AFFECTED_HONOR_MODEL = listOf("REA-NX9")

        public fun load(): Boolean {
            return isAffectedSamsungDevices() ||
                isAffectedXiaoMiDevices() ||
                isAffectedMotorolaDevices() ||
                isAffectedHonorDevices()
        }

        private fun isAffectedSamsungDevices(): Boolean {
            return (isSamsungDevice() && isAffectedModel(AFFECTED_SAMSUNG_MODEL))
        }

        private fun isAffectedXiaoMiDevices(): Boolean {
            return ((isXiaomiDevice() || Device.isRedmiDevice() || Device.isPocoDevice()) &&
                isAffectedModel(AFFECTED_XIAOMI_MODEL))
        }

        private fun isAffectedMotorolaDevices(): Boolean {
            return (isMotorolaDevice() && isAffectedModel(AFFECTED_MOTOROLA_MODEL))
        }

        private fun isAffectedHonorDevices(): Boolean {
            return isAffectedModel(AFFECTED_HONOR_MODEL)
        }

        private fun isAffectedModel(modelList: List<String>): Boolean {
            for (model in modelList) {
                if (Build.MODEL.uppercase().startsWith(model)) {
                    return true
                }
            }
            return false
        }
    }
}
