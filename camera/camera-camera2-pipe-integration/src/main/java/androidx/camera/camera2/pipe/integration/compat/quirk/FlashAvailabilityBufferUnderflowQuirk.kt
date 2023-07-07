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
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.Quirk
import java.nio.BufferUnderflowException
import java.util.Locale

/**
 * A quirk for devices that throw a [BufferUnderflowException] when querying the flash availability.
 *
 * QuirkSummary
 * - Bug Id: 216667482
 * - Description: When attempting to retrieve the
 *   [CameraCharacteristics.FLASH_INFO_AVAILABLE] characteristic, a
 *   [BufferUnderflowException] is thrown. This is an undocumented exception
 *   on the [CameraCharacteristics.get] method, so this violates the API contract.
 * - Device(s): Spreadtrum devices including LEMFO LEMP and DM20C
 *
 * TODO: enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FlashAvailabilityBufferUnderflowQuirk : Quirk {

    companion object {
        private val KNOWN_AFFECTED_MODELS = setOf(
            // Devices enumerated as DeviceInfo(Build.MANUFACTURER, Build.MODEL).
            DeviceInfo("sprd", "lemp"),
            DeviceInfo("sprd", "DM20C"),
        )

        fun isEnabled(): Boolean {
            return KNOWN_AFFECTED_MODELS.contains(
                DeviceInfo(Build.MANUFACTURER, Build.MODEL)
            )
        }
    }

    data class DeviceInfo private constructor(val manufacturer: String, val model: String) {
        companion object {
            operator fun invoke(manufacturer: String, model: String) =
                DeviceInfo(manufacturer.lowercase(Locale.US), model.lowercase(Locale.US))
        }
    }
}
