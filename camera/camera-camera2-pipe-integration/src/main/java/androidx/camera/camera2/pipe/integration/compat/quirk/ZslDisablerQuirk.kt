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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isXiaomiDevice
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: 252818931, 261744070, 319913852
 * - Description: On certain devices, the captured image has color issue for reprocessing. We need
 *   to disable zero-shutter lag and return false for [CameraInfo.isZslSupported].
 * - Device(s): Samsung Fold4, Samsung s22, Xiaomi Mi 8
 */
@SuppressLint("CameraXQuirksClassDetector") // TODO(b/270421716): enable when kotlin is supported.
class ZslDisablerQuirk : Quirk {

    companion object {
        private val AFFECTED_SAMSUNG_MODEL = listOf("SM-F936", "SM-S901U", "SM-S908U", "SM-S908U1")

        private val AFFECTED_XIAOMI_MODEL = listOf("MI 8")

        fun load(): Boolean {
            return isAffectedSamsungDevices() || isAffectedXiaoMiDevices()
        }

        private fun isAffectedSamsungDevices(): Boolean {
            return (isSamsungDevice() && isAffectedModel(AFFECTED_SAMSUNG_MODEL))
        }

        private fun isAffectedXiaoMiDevices(): Boolean {
            return (isXiaomiDevice() && isAffectedModel(AFFECTED_XIAOMI_MODEL))
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
