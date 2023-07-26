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

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
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
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CamcorderProfileResolutionQuirkTest {

    @Test
    fun loadByHardwareLevel() {
        var cameraMetadata =
            createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata))
            .isFalse()

        cameraMetadata =
            createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata))
            .isFalse()

        cameraMetadata =
            createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata))
            .isFalse()

        cameraMetadata =
            createCameraMetaData(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        assertThat(CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)).isTrue()
    }

    @Test
    fun canGetCorrectSupportedSizes() {
        val cameraMetadata =
            createCameraMetaData(
                supportedSizes = arrayOf(
                    EncoderProfilesUtil.RESOLUTION_2160P,
                    EncoderProfilesUtil.RESOLUTION_1080P
                )
            )
        val quirk = CamcorderProfileResolutionQuirk(
            StreamConfigurationMapCompat(
                cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!,
                OutputSizesCorrector(
                    cameraMetadata,
                    cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!
                )
            )
        )

        assertThat(quirk.supportedResolutions[0])
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_2160P)
        assertThat(quirk.supportedResolutions[1])
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_1080P)
    }

    private fun createCameraMetaData(
        hardwareLevel: Int = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        supportedSizes: Array<Size> = emptyArray()
    ): androidx.camera.camera2.pipe.CameraMetadata {
        val mockMap = Mockito.mock(StreamConfigurationMap::class.java)
        // Before Android 23, use {@link SurfaceTexture} will finally mapped to 0x22 in
        // StreamConfigurationMap to retrieve the output sizes information.
        Mockito.`when`(mockMap.getOutputSizes(ArgumentMatchers.any<Class<SurfaceTexture>>()))
            .thenReturn(supportedSizes)
        Mockito.`when`(mockMap.getOutputSizes(ArgumentMatchers.anyInt())).thenReturn(supportedSizes)

        return FakeCameraMetadata(
            characteristics = mapOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
                CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to mockMap
            )
        )
    }
}
