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

package androidx.camera.camera2.internal

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.media.CamcorderProfile
import android.os.Build
import android.util.Size
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.testing.CameraUtil
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
@Suppress("DEPRECATION")
public class Camera2CamcorderProfileProviderTest(private val quality: Int) {
    public companion object {
        @JvmStatic
        @Parameterized.Parameters
        public fun data(): Array<Array<Int>> = arrayOf(
            arrayOf(CamcorderProfile.QUALITY_LOW),
            arrayOf(CamcorderProfile.QUALITY_HIGH),
            arrayOf(CamcorderProfile.QUALITY_QCIF),
            arrayOf(CamcorderProfile.QUALITY_CIF),
            arrayOf(CamcorderProfile.QUALITY_480P),
            arrayOf(CamcorderProfile.QUALITY_720P),
            arrayOf(CamcorderProfile.QUALITY_1080P),
            arrayOf(CamcorderProfile.QUALITY_QVGA),
            arrayOf(CamcorderProfile.QUALITY_2160P),
            arrayOf(CamcorderProfile.QUALITY_VGA),
            arrayOf(CamcorderProfile.QUALITY_4KDCI),
            arrayOf(CamcorderProfile.QUALITY_QHD),
            arrayOf(CamcorderProfile.QUALITY_2K)
        )
    }

    private lateinit var camcorderProfileProvider: Camera2CamcorderProfileProvider
    private lateinit var cameraCharacteristics: CameraCharacteristicsCompat
    private var isLegacyCamera = false
    private var intCameraId = -1

    @Before
    public fun setup() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        val cameraId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_BACK)!!
        intCameraId = cameraId.toInt()

        cameraCharacteristics = CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
            CameraUtil.getCameraCharacteristics(CameraSelector.LENS_FACING_BACK)!!
        )
        val hardwareLevel =
            cameraCharacteristics[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]

        isLegacyCamera = hardwareLevel != null && hardwareLevel == CameraCharacteristics
            .INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

        camcorderProfileProvider = Camera2CamcorderProfileProvider(cameraId, cameraCharacteristics)
    }

    @Test
    public fun nonLegacyCamera_hasProfile_returnSameResult() {
        assumeTrue(!isLegacyCamera)

        assertThat(camcorderProfileProvider.hasProfile(quality))
            .isEqualTo(CamcorderProfile.hasProfile(intCameraId, quality))
    }

    @Test
    public fun nonLegacyCamera_notHasProfile_getReturnNull() {
        assumeTrue(!isLegacyCamera)
        assumeTrue(!CamcorderProfile.hasProfile(intCameraId, quality))

        assertThat(camcorderProfileProvider.get(quality)).isNull()
    }

    @Test
    public fun nonLegacyCamera_hasProfile_getReturnSameQualityProfile() {
        assumeTrue(!isLegacyCamera)
        assumeTrue(CamcorderProfile.hasProfile(intCameraId, quality))

        val profileProxy = camcorderProfileProvider.get(quality)!!
        val profile = CamcorderProfile.get(intCameraId, quality)
        assertThat(profileProxy.quality).isEqualTo(profile.quality)
    }

    @Test
    public fun legacyCamera_notHasProfile_returnFalse() {
        assumeTrue(isLegacyCamera)
        assumeTrue(!CamcorderProfile.hasProfile(intCameraId, quality))

        assertThat(camcorderProfileProvider.hasProfile(quality)).isFalse()
    }

    @Test
    public fun legacyCamera_hasProfile_shouldCheckSupportedResolution() {
        assumeTrue(isLegacyCamera)
        assumeTrue(CamcorderProfile.hasProfile(intCameraId, quality))

        val videoSupportedResolutions = getVideoSupportedResolutions()
        val isResolutionSupported =
            videoSupportedResolutions.contains(CamcorderProfile.get(intCameraId, quality).size())

        assertThat(camcorderProfileProvider.hasProfile(quality)).isEqualTo(isResolutionSupported)
    }

    @Test
    public fun legacyCamera_notHasProfile_getReturnNull() {
        assumeTrue(isLegacyCamera)
        assumeTrue(!CamcorderProfile.hasProfile(intCameraId, quality))

        assertThat(camcorderProfileProvider.get(quality)).isNull()
    }

    @Test
    public fun legacyCamera_hasProfile_getShouldCheckSupportedResolution() {
        assumeTrue(isLegacyCamera)
        assumeTrue(CamcorderProfile.hasProfile(intCameraId, quality))

        val profile = CamcorderProfile.get(intCameraId, quality)
        val videoSupportedResolutions = getVideoSupportedResolutions()
        val isResolutionSupported = videoSupportedResolutions.contains(profile.size())

        val profileProxy = camcorderProfileProvider.get(quality)
        if (isResolutionSupported) {
            assertThat(profileProxy!!.quality).isEqualTo(profile.quality)
        } else {
            assertThat(profileProxy).isNull()
        }
    }

    private fun getVideoSupportedResolutions(): Array<Size> {
        val map = cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!

        // Before Android 23, use {@link SurfaceTexture} will finally mapped to 0x22 in
        // StreamConfigurationMap to retrieve the output sizes information.
        return if (Build.VERSION.SDK_INT < 23) {
            map.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray()
        } else {
            map.getOutputSizes(ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE)
                ?: emptyArray()
        }
    }

    private fun CamcorderProfile.size() = Size(videoFrameWidth, videoFrameHeight)
}
