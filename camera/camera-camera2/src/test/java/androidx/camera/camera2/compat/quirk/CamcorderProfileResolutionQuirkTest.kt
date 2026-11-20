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

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile
import android.util.Size
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.testing.impl.EncoderProfilesUtil
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class CamcorderProfileResolutionQuirkTest {

    @Test
    @Config(maxSdk = 28)
    fun loadByHardwareLevel_belowApi29_enabledOnlyForLegacy() {
        var cameraMetadata = createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)).isFalse()

        cameraMetadata = createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)).isFalse()

        cameraMetadata = createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)).isFalse()

        cameraMetadata = createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)).isTrue()
    }

    @Test
    @Config(minSdk = 29)
    fun loadByHardwareLevel_api29AndAbove_enabledForAllLevels() {
        var cameraMetadata = createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)).isTrue()

        cameraMetadata = createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)).isTrue()

        cameraMetadata = createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)).isTrue()

        cameraMetadata = createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)).isTrue()
    }

    @Test
    fun canGetCorrectSupportedSizes() {
        val cameraMetadata =
            createCameraMetaData(
                supportedSizes =
                    arrayOf(
                        EncoderProfilesUtil.RESOLUTION_2160P,
                        EncoderProfilesUtil.RESOLUTION_1080P,
                    )
            )
        val map =
            requireNotNull(cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP])
        val quirk =
            CamcorderProfileResolutionQuirk(
                StreamConfigurationMapCompat(map, cameraMetadata),
                isLegacy = true,
            )

        assertThat(quirk.getSupportedResolutions()[0])
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_2160P)
        assertThat(quirk.getSupportedResolutions()[1])
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_1080P)
    }

    @Test
    fun requiresValidation_legacyCamera_validatesAllQualities() {
        val cameraMetadata =
            createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val map =
            requireNotNull(cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP])
        val quirk =
            CamcorderProfileResolutionQuirk(
                StreamConfigurationMapCompat(map, cameraMetadata),
                isLegacy = true,
            )

        for (quality in CTS_VERIFIED_QUALITIES + CTS_UNVERIFIED_QUALITIES) {
            assertThat(quirk.requiresValidation(quality)).isTrue()
        }
    }

    @Test
    fun requiresValidation_nonLegacyCamera_validatesOnlyCtsUnverifiedQualities() {
        val cameraMetadata = createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        val map =
            requireNotNull(cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP])
        val quirk =
            CamcorderProfileResolutionQuirk(
                StreamConfigurationMapCompat(map, cameraMetadata),
                isLegacy = false,
            )

        for (quality in CTS_VERIFIED_QUALITIES) {
            assertThat(quirk.requiresValidation(quality)).isFalse()
        }
        for (quality in CTS_UNVERIFIED_QUALITIES) {
            assertThat(quirk.requiresValidation(quality)).isTrue()
        }
    }

    @Test
    fun isProfileResolutionValid_nonLegacy_skipsCtsVerifiedAndValidatesUnverified() {
        val cameraMetadata =
            createCameraMetaData(
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                supportedSizes = arrayOf(EncoderProfilesUtil.RESOLUTION_2160P),
            )
        val map =
            requireNotNull(cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP])
        val quirk =
            CamcorderProfileResolutionQuirk(
                StreamConfigurationMapCompat(map, cameraMetadata),
                isLegacy = false,
            )

        // CTS-verified qualities are trusted without querying StreamConfigurationMap.
        assertThat(
                quirk.isProfileResolutionValid(
                    CamcorderProfile.QUALITY_1080P,
                    EncoderProfilesUtil.RESOLUTION_1080P,
                )
            )
            .isTrue()
        Mockito.verifyNoInteractions(map)

        // CTS-unverified qualities are validated against normal PRIVATE output sizes.
        assertThat(
                quirk.isProfileResolutionValid(
                    CamcorderProfile.QUALITY_QHD,
                    EncoderProfilesUtil.RESOLUTION_QHD,
                )
            )
            .isFalse()
    }

    @Test
    fun isProfileResolutionValid_emptySupportedSizes_skipsOnNonLegacyOnly() {
        val emptyMetadata = createCameraMetaData(supportedSizes = emptyArray())
        val map =
            requireNotNull(emptyMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP])
        val compatMap = StreamConfigurationMapCompat(map, emptyMetadata)

        val nonLegacyQuirk = CamcorderProfileResolutionQuirk(compatMap, isLegacy = false)
        assertThat(
                nonLegacyQuirk.isProfileResolutionValid(
                    CamcorderProfile.QUALITY_QHD,
                    EncoderProfilesUtil.RESOLUTION_QHD,
                )
            )
            .isTrue()

        val legacyQuirk = CamcorderProfileResolutionQuirk(compatMap, isLegacy = true)
        assertThat(
                legacyQuirk.isProfileResolutionValid(
                    CamcorderProfile.QUALITY_1080P,
                    EncoderProfilesUtil.RESOLUTION_1080P,
                )
            )
            .isFalse()
        assertThat(
                legacyQuirk.isProfileResolutionValid(
                    CamcorderProfile.QUALITY_QHD,
                    EncoderProfilesUtil.RESOLUTION_QHD,
                )
            )
            .isFalse()
    }

    private fun createCameraMetaData(
        hardwareLevel: Int = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        supportedSizes: Array<Size> = emptyArray(),
    ): androidx.camera.camera2.pipe.CameraMetadata {
        val mockMap = Mockito.mock(StreamConfigurationMap::class.java)
        // Before Android 23, use {@link SurfaceTexture} will finally mapped to 0x22 in
        // StreamConfigurationMap to retrieve the output sizes information.
        Mockito.`when`(mockMap.getOutputSizes(ArgumentMatchers.any<Class<SurfaceTexture>>()))
            .thenReturn(supportedSizes)
        Mockito.`when`(mockMap.getOutputSizes(ArgumentMatchers.anyInt())).thenReturn(supportedSizes)

        return FakeCameraMetadata.fromTemplate(
            template = HighEndDeviceTemplate,
            lensFacing = CameraCharacteristics.LENS_FACING_BACK,
            characteristicsOverrides =
                mapOf(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to mockMap,
                ),
        )
    }

    private companion object {
        val CTS_VERIFIED_QUALITIES =
            listOf(
                CamcorderProfile.QUALITY_HIGH,
                CamcorderProfile.QUALITY_2160P,
                CamcorderProfile.QUALITY_1080P,
                CamcorderProfile.QUALITY_720P,
                CamcorderProfile.QUALITY_480P,
                CamcorderProfile.QUALITY_CIF,
                CamcorderProfile.QUALITY_QCIF,
                CamcorderProfile.QUALITY_QVGA,
                CamcorderProfile.QUALITY_LOW,
            )

        val CTS_UNVERIFIED_QUALITIES =
            listOf(
                CamcorderProfile.QUALITY_VGA,
                CamcorderProfile.QUALITY_4KDCI,
                CamcorderProfile.QUALITY_QHD,
                CamcorderProfile.QUALITY_2K,
                CamcorderProfile.QUALITY_8KUHD,
            )
    }
}
