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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile
import android.os.Build
import android.util.Size
import androidx.camera.camera2.adapter.EncoderProfilesProviderAdapter
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.Quirk

/**
 * Validates [CamcorderProfile] video resolutions in [EncoderProfilesProviderAdapter] against
 * camera-supported output sizes.
 *
 * QuirkSummary
 * - Bug Id: 180819729, 583414580
 * - Description: When using the Camera2 API in `LEGACY` mode (i.e. when
 *   [CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] is set to
 *   [CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY]), [CamcorderProfile.hasProfile]
 *   may return `true` for unsupported resolutions. To ensure a given resolution is supported in
 *   `LEGACY` mode, the configuration given in
 *   [CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP] must contain the resolution in the
 *   output sizes returned by [StreamConfigurationMap.getOutputSizes]. On non-`LEGACY` cameras
 *   running API 29+, CTS `RecordingTest` only validates the 9 classic qualities
 *   ([CamcorderProfile.QUALITY_HIGH], [CamcorderProfile.QUALITY_2160P],
 *   [CamcorderProfile.QUALITY_1080P], [CamcorderProfile.QUALITY_720P],
 *   [CamcorderProfile.QUALITY_480P], [CamcorderProfile.QUALITY_CIF],
 *   [CamcorderProfile.QUALITY_QCIF], [CamcorderProfile.QUALITY_QVGA],
 *   [CamcorderProfile.QUALITY_LOW]) against supported video sizes, leaving newer qualities
 *   ([CamcorderProfile.QUALITY_VGA], [CamcorderProfile.QUALITY_4KDCI],
 *   [CamcorderProfile.QUALITY_QHD], [CamcorderProfile.QUALITY_2K],
 *   [CamcorderProfile.QUALITY_8KUHD]) unvalidated. Some non-`LEGACY` devices (such as Xiaomi Redmi
 *   Note 8 at [CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3]) advertise unsupported
 *   qualities like [CamcorderProfile.QUALITY_QHD] (`2560x1440`) that fail camera stream
 *   configuration. On non-`LEGACY` cameras, only those CTS-unverified qualities are checked against
 *   normal `PRIVATE` output sizes.
 * - Device(s): All legacy devices, and API 29+ non-legacy devices advertising CTS-unverified
 *   qualities (e.g. Xiaomi Redmi Note 8)
 *
 * TODO: enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CamcorderProfileResolutionQuirk(
    private val streamConfigurationMapCompat: StreamConfigurationMapCompat,
    private val isLegacy: Boolean,
) : Quirk {

    // Lazily queries normal PRIVATE output sizes so non-LEGACY cameras with only CTS-verified
    // qualities never query StreamConfigurationMap. Excludes getHighResolutionOutputSizes() because
    // burst-only sizes (< 20fps) cannot sustain standard CamcorderProfile recording frame rates.
    private val supportedResolution: List<Size> by lazy {
        val sizes =
            streamConfigurationMapCompat.getOutputSizes(
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
            )

        val result: List<Size> = sizes?.asList() ?: emptyList()
        Camera2Logger.debug { "supportedResolutions = $result" }
        result
    }

    /** Returns the supported video resolutions. */
    public fun getSupportedResolutions(): List<Size> {
        return supportedResolution.toList()
    }

    /** Returns whether the profile of [quality] must be validated against supported sizes. */
    public fun requiresValidation(quality: Int): Boolean =
        isLegacy || quality in CTS_UNVERIFIED_QUALITIES

    /**
     * Checks whether [resolution] is valid for [quality] against normal `PRIVATE` output sizes.
     *
     * Non-`LEGACY` cameras trust qualities covered by CTS `RecordingTest` without querying
     * [StreamConfigurationMapCompat] (avoiding false rejections when quirks such as
     * [ExcludedSupportedSizesQuirk] remove a size like `1920x1080` for preview workarounds while
     * [CamcorderProfile.QUALITY_1080P] video remains valid), and skip validation when the supported
     * size list is empty.
     */
    public fun isProfileResolutionValid(quality: Int, resolution: Size): Boolean {
        if (!requiresValidation(quality)) {
            return true
        }
        val resolutions = supportedResolution
        if (!isLegacy && resolutions.isEmpty()) {
            // Fail open on non-LEGACY cameras when PRIVATE sizes are empty/missing so valid
            // profiles are not wiped out; LEGACY cameras preserve historical fail-closed behavior.
            return true
        }
        return resolutions.contains(resolution)
    }

    public companion object {
        // Qualities omitted from CTS RecordingTest#mCamcorderProfileList on non-LEGACY cameras.
        // Do not validate CTS-covered qualities here: quirks like ExcludedSupportedSizesQuirk may
        // remove PRIVATE preview sizes (e.g. 1080p) on devices whose video profile still works.
        private val CTS_UNVERIFIED_QUALITIES =
            setOf(
                CamcorderProfile.QUALITY_VGA,
                CamcorderProfile.QUALITY_4KDCI,
                CamcorderProfile.QUALITY_QHD,
                CamcorderProfile.QUALITY_2K,
                CamcorderProfile.QUALITY_8KUHD,
            )

        // Gates at API 29 (not 30): although QUALITY_VGA/4KDCI/QHD/2K became public SDK constants
        // in API 30 (and QUALITY_8KUHD in API 31), they are compile-time int constants that Android
        // 10 (API 29) framework profiles can already advertise.
        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean =
            cameraMetadata.isHardwareLevelLegacy || Build.VERSION.SDK_INT >= 29
    }
}
