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

package androidx.camera.camera2.internal.compat.workaround

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.quirk.CamcorderProfileResolutionQuirk
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_2160P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_720P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_2160P
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CamcorderProfileResolutionValidatorTest {

    @Test
    public fun noQuirk_alwaysValid() {
        val validator = CamcorderProfileResolutionValidator(null)

        assertThat(validator.hasValidVideoResolution(PROFILE_2160P)).isTrue()
        assertThat(validator.hasValidVideoResolution(PROFILE_720P)).isTrue()
    }

    @Test
    public fun hasQuirk_shouldCheckSupportedResolutions() {
        val cameraCharacteristicsCompat = createCameraCharacteristicsCompat(
            supportedResolution = arrayOf(RESOLUTION_2160P)
        )
        val quirk = CamcorderProfileResolutionQuirk(cameraCharacteristicsCompat)
        val validator = CamcorderProfileResolutionValidator(quirk)

        assertThat(validator.hasValidVideoResolution(PROFILE_2160P)).isTrue()
        assertThat(validator.hasValidVideoResolution(PROFILE_720P)).isFalse()
    }

    @Test
    public fun nullProfile_notValid() {
        val cameraCharacteristicsCompat = createCameraCharacteristicsCompat(
            supportedResolution = arrayOf(RESOLUTION_2160P)
        )
        val quirk = CamcorderProfileResolutionQuirk(cameraCharacteristicsCompat)
        val validator = CamcorderProfileResolutionValidator(quirk)

        assertThat(validator.hasValidVideoResolution(null)).isFalse()
    }

    private fun createCameraCharacteristicsCompat(
        supportedResolution: Array<Size> = emptyArray()
    ): CameraCharacteristicsCompat {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)

        val mockMap = Mockito.mock(StreamConfigurationMap::class.java)

        // Before Android 23, use {@link SurfaceTexture} will finally mapped to 0x22 in
        // StreamConfigurationMap to retrieve the output sizes information.
        `when`(mockMap.getOutputSizes(ArgumentMatchers.any<Class<SurfaceTexture>>()))
            .thenReturn(supportedResolution)
        `when`(mockMap.getOutputSizes(ArgumentMatchers.anyInt()))
            .thenReturn(supportedResolution)

        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap)

        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics)
    }
}
