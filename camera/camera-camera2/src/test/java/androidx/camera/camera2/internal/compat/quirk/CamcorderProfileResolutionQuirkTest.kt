/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.quirk

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_1080P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_2160P
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CamcorderProfileResolutionQuirkTest {

    @Test
    public fun loadByHardwareLevel() {
        var cameraCharacteristicsCompat =
            createCameraCharacteristicsCompat(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        assertThat(CamcorderProfileResolutionQuirk.load(cameraCharacteristicsCompat)).isFalse()

        cameraCharacteristicsCompat =
            createCameraCharacteristicsCompat(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        assertThat(CamcorderProfileResolutionQuirk.load(cameraCharacteristicsCompat)).isFalse()

        cameraCharacteristicsCompat =
            createCameraCharacteristicsCompat(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        assertThat(CamcorderProfileResolutionQuirk.load(cameraCharacteristicsCompat)).isFalse()

        cameraCharacteristicsCompat =
            createCameraCharacteristicsCompat(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        assertThat(CamcorderProfileResolutionQuirk.load(cameraCharacteristicsCompat)).isTrue()
    }

    @Test
    public fun canGetCorrectSupportedSizes() {
        val cameraCharacteristicsCompat =
            createCameraCharacteristicsCompat(
                supportedSizes = arrayOf(
                    RESOLUTION_2160P,
                    RESOLUTION_1080P
                )
            )
        val quirk = CamcorderProfileResolutionQuirk(cameraCharacteristicsCompat)

        assertThat(quirk.supportedResolutions[0]).isEqualTo(RESOLUTION_2160P)
        assertThat(quirk.supportedResolutions[1]).isEqualTo(RESOLUTION_1080P)
    }

    private fun createCameraCharacteristicsCompat(
        hardwareLevel: Int = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        supportedSizes: Array<Size> = emptyArray()
    ): CameraCharacteristicsCompat {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)

        shadowCharacteristics.set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            hardwareLevel
        )

        val mockMap = mock(StreamConfigurationMap::class.java)
        // Before Android 23, use {@link SurfaceTexture} will finally mapped to 0x22 in
        // StreamConfigurationMap to retrieve the output sizes information.
        `when`(mockMap.getOutputSizes(ArgumentMatchers.any<Class<SurfaceTexture>>()))
            .thenReturn(supportedSizes)
        `when`(mockMap.getOutputSizes(ArgumentMatchers.anyInt())).thenReturn(supportedSizes)

        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap)

        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics)
    }
}