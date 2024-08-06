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
import android.media.EncoderProfiles
import android.os.Build
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isRedmiDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isXiaomiDevice
import androidx.camera.core.impl.Quirk

/**
 * Quirk denoting the video profile list returns by [EncoderProfiles] is invalid.
 *
 * QuirkSummary
 * - Bug Id: 267727595, 278860860, 298951126, 298952500, 320747756
 * - Description: When using [EncoderProfiles] on some builds of Android API 33,
 *   [EncoderProfiles.getVideoProfiles] returns a list with size one, but the single value in the
 *   list is null. This is not the expected behavior, and makes [EncoderProfiles] lack of video
 *   information.
 * - Device(s): Pixel 4 and above pixel devices with TP1A or TD1A builds (API 33), Samsung devices
 *   with TP1A build (API 33), Xiaomi devices with TKQ1/TP1A build (API 33), OnePlus and Oppo
 *   devices with API 33 build.
 *
 * TODO: enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
class InvalidVideoProfilesQuirk : Quirk {

    companion object {
        private val AFFECTED_PIXEL_MODELS: List<String> =
            listOf(
                "pixel 4",
                "pixel 4a",
                "pixel 4a (5g)",
                "pixel 4 xl",
                "pixel 5",
                "pixel 5a",
                "pixel 6",
                "pixel 6a",
                "pixel 6 pro",
                "pixel 7",
                "pixel 7 pro"
            )

        private val AFFECTED_ONE_PLUS_MODELS: List<String> = listOf("cph2417", "cph2451")

        private val AFFECTED_OPPO_MODELS: List<String> =
            listOf(
                "cph2437",
                "cph2525",
                "pht110",
            )

        fun isEnabled(): Boolean {
            return isAffectedSamsungDevices() ||
                isAffectedPixelDevices() ||
                isAffectedXiaomiDevices() ||
                isAffectedOppoDevices() ||
                isAffectedOnePlusDevices()
        }

        private fun isAffectedSamsungDevices(): Boolean {
            return isSamsungDevice() && isTp1aBuild()
        }

        private fun isAffectedPixelDevices(): Boolean {
            return isAffectedPixelModel() && isAffectedPixelBuild()
        }

        private fun isAffectedXiaomiDevices(): Boolean {
            return (isRedmiDevice() or isXiaomiDevice()) && (isTkq1Build() || isTp1aBuild())
        }

        private fun isAffectedOnePlusDevices(): Boolean {
            return isAffectedOnePlusModel() && isAPI33()
        }

        private fun isAffectedOppoDevices(): Boolean {
            return isAffectedOppoModel() && isAPI33()
        }

        private fun isAffectedPixelModel(): Boolean {
            return AFFECTED_PIXEL_MODELS.contains(Build.MODEL.lowercase())
        }

        private fun isAffectedOnePlusModel(): Boolean {
            return AFFECTED_ONE_PLUS_MODELS.contains(Build.MODEL.lowercase())
        }

        private fun isAffectedOppoModel(): Boolean {
            return AFFECTED_OPPO_MODELS.contains(Build.MODEL.lowercase())
        }

        private fun isAffectedPixelBuild(): Boolean {
            return isTp1aBuild() || isTd1aBuild()
        }

        private fun isTp1aBuild(): Boolean {
            return Build.ID.startsWith("TP1A", true)
        }

        private fun isTd1aBuild(): Boolean {
            return Build.ID.startsWith("TD1A", true)
        }

        private fun isTkq1Build(): Boolean {
            return Build.ID.startsWith("TKQ1", true)
        }

        private fun isAPI33(): Boolean {
            return Build.VERSION.SDK_INT == 33
        }
    }
}
