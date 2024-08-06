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
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isMotorolaDevice
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary Bug Id: b/241876294, b/299075294 Description: CamcorderProfile resolutions can not
 * find a match in the output size list of [CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP].
 * Some resolutions are added back as they are supported by the camera and do not have stretching
 * issues. Device(s): Motorola Moto E5 Play.
 *
 * TODO: enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
class ExtraSupportedOutputSizeQuirk : Quirk {
    /** Returns the extra supported resolutions on the device. */
    fun getExtraSupportedResolutions(format: Int): Array<Size> {
        return if (
            (format == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE && isMotoE5Play)
        ) {
            motoE5PlayExtraSupportedResolutions
        } else {
            arrayOf()
        }
    }

    /** Returns the extra supported resolutions on the device. */
    fun <T> getExtraSupportedResolutions(klass: Class<T>): Array<Size> {
        return if (StreamConfigurationMap.isOutputSupportedFor(klass) && isMotoE5Play) {
            motoE5PlayExtraSupportedResolutions
        } else {
            arrayOf()
        }
    }

    // Both the front and the main cameras support the following resolutions.
    private val motoE5PlayExtraSupportedResolutions: Array<Size>
        get() =
            arrayOf(
                // FHD
                Size(1440, 1080),
                // HD
                Size(960, 720),
                // SD (640:480 is already included in the original list)
            )

    companion object {
        fun isEnabled(): Boolean {
            return isMotoE5Play
        }

        internal val isMotoE5Play: Boolean
            get() = isMotorolaDevice() && "moto e5 play".equals(Build.MODEL, ignoreCase = true)
    }
}
