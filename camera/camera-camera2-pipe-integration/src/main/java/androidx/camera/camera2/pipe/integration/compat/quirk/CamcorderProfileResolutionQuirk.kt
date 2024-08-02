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
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.EncoderProfilesProviderAdapter
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.Quirk

/**
 * Quirk that should validate the video resolution of [EncoderProfilesProviderAdapter] on legacy
 * camera.
 *
 * QuirkSummary
 * - Bug Id: 180819729
 * - Description: When using the Camera 2 API in `LEGACY` mode (i.e. when
 *   [CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] is set to
 *   [CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY]),
 *   [EncoderProfilesProviderAdapter.hasProfile] may return `true` for unsupported resolutions. To
 *   ensure a given resolution is supported in LEGACY mode, the configuration given in
 *   [CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP] must contain the resolution in the
 *   supported output sizes. The recommended way to check this is with
 *   [StreamConfigurationMap.getOutputSizes] with the class of the desired recording endpoint, and
 *   check that the desired resolution is contained in the list returned.
 * - Device(s): All legacy devices
 *
 * TODO: enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
class CamcorderProfileResolutionQuirk(
    private val streamConfigurationMapCompat: StreamConfigurationMapCompat
) : Quirk {

    private val supportedResolution: List<Size> by lazy {
        val sizes =
            streamConfigurationMapCompat.getOutputSizes(
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
            )

        val result: List<Size> = sizes?.asList() ?: emptyList()
        Log.debug { "supportedResolutions = $result" }
        result
    }

    /** Returns the supported video resolutions. */
    fun getSupportedResolutions(): List<Size> {
        return supportedResolution.toList()
    }

    companion object {
        fun isEnabled(cameraMetadata: CameraMetadata) = cameraMetadata.isHardwareLevelLegacy
    }
}
