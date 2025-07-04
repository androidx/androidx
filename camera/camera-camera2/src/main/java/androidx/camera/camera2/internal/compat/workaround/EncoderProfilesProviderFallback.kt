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

package androidx.camera.camera2.internal.compat.workaround

import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraMetadata.LENS_FACING_EXTERNAL
import android.media.CamcorderProfile
import android.media.CamcorderProfile.QUALITY_HIGH
import android.util.Size
import androidx.camera.camera2.internal.Camera2EncoderProfilesProvider
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.Quirks
import androidx.camera.core.internal.utils.SizeUtil.getArea

/**
 * Resolves an [EncoderProfilesProvider] for a camera, providing fallback mechanisms when a default
 * provider is unsuitable.
 *
 * This class is responsible for determining an appropriate [EncoderProfilesProvider] for a given
 * camera. It handles scenarios where the default provider may be incompatible, such as with
 * external cameras that lack [CamcorderProfile]. In such cases, it attempts to find a suitable
 * provider from other available cameras on the device.
 */
public class EncoderProfilesProviderFallback(
    /**
     * A factory method to create an [EncoderProfilesProvider] for a given camera ID and [Quirks].
     * This can be customized for testing or to provide alternative implementations.
     *
     * By default, this factory creates a [Camera2EncoderProfilesProvider].
     */
    private val providerFactory: (cameraId: String, quirks: Quirks) -> EncoderProfilesProvider =
        { cameraId, quirks ->
            Camera2EncoderProfilesProvider(cameraId, quirks)
        }
) {
    /**
     * Resolves an appropriate [EncoderProfilesProvider] for the given camera, with fallback logic
     * for external cameras.
     *
     * This function prioritizes using a default provider for the specified camera. If the camera is
     * external and the default provider is unsuitable (e.g., due to incompatible video profile
     * sizes), it attempts to find a compatible provider from other available cameras. This fallback
     * provider is then constrained by the external camera's supported sizes.
     *
     * @param cameraId The ID of the target camera.
     * @param quirks Camera quirks.
     * @param cameraManager The [CameraManagerCompat] instance used to access camera
     *   characteristics.
     * @return An appropriate [EncoderProfilesProvider] for the given camera.
     */
    public fun resolveProvider(
        cameraId: String,
        quirks: Quirks,
        cameraManager: CameraManagerCompat,
    ): EncoderProfilesProvider {
        var provider: EncoderProfilesProvider? = null
        val characteristics = cameraManager.getCameraCharacteristicsCompat(cameraId)
        val defaultProvider = providerFactory.invoke(cameraId, quirks)

        if (needFallback(characteristics, defaultProvider)) {
            // Empirically, the provider with the largest size offer the widest range of supported
            // profiles.
            provider = findProviderWithLargestSize(cameraManager)
            if (provider != null) {
                provider =
                    SizeFilteredEncoderProfilesProvider(
                        provider,
                        characteristics.getPrivateFormatSizes(),
                    )
            }
        }

        if (provider == null) {
            provider = defaultProvider
        }

        return provider
    }

    /**
     * Checks if fallback to a different camera's profiles is needed.
     *
     * Fallback is needed when the camera is an external camera and the related
     * [EncoderProfilesProvider] doesn't have any profile.
     *
     * @param characteristics The [CameraCharacteristicsCompat] of the camera.
     * @param provider The related [EncoderProfilesProvider] to check.
     * @return `true` if fallback is needed, `false` otherwise.
     */
    private fun needFallback(
        characteristics: CameraCharacteristicsCompat,
        provider: EncoderProfilesProvider,
    ): Boolean = characteristics.isExternalCamera() && !provider.hasProfile(QUALITY_HIGH)

    /**
     * Finds an [EncoderProfilesProvider] with the largest supported video profile size among all
     * available cameras.
     *
     * This method iterates through all available cameras on the device, creates an
     * [EncoderProfilesProvider] for each camera using the [providerFactory], and selects the
     * provider with the largest supported video profile size for [QUALITY_HIGH].
     *
     * @param cameraManager The [CameraManagerCompat] instance used to access camera information.
     * @return An [EncoderProfilesProvider] with the largest video profile size, or `null` if none
     *   is found.
     */
    private fun findProviderWithLargestSize(
        cameraManager: CameraManagerCompat
    ): EncoderProfilesProvider? {
        val cameraIds = runCatching { cameraManager.cameraIdList }.getOrNull() ?: return null

        var largestProvider: EncoderProfilesProvider? = null
        var largestProfileArea = 0

        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristicsCompat(cameraId)
            val quirks = CameraQuirks.get(cameraId, characteristics)
            val provider = providerFactory.invoke(cameraId, quirks)

            val profileArea =
                provider.getAll(QUALITY_HIGH)?.videoProfiles?.firstOrNull()?.let {
                    getArea(it.width, it.height)
                } ?: 0

            if (profileArea > largestProfileArea) {
                largestProfileArea = profileArea
                largestProvider = provider
            }
        }

        return largestProvider
    }

    private fun CameraCharacteristicsCompat.isExternalCamera(): Boolean =
        get(LENS_FACING) == LENS_FACING_EXTERNAL

    private fun CameraCharacteristicsCompat.getPrivateFormatSizes(): List<Size> =
        streamConfigurationMapCompat.getOutputSizes(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE)?.toList()
            ?: emptyList()
}
