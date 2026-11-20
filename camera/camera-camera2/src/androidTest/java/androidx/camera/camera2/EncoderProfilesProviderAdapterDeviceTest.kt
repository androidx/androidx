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

package androidx.camera.camera2

import android.hardware.camera2.CameraCharacteristics
import android.media.CamcorderProfile
import android.media.EncoderProfiles.VideoProfile.HDR_NONE
import android.media.EncoderProfiles.VideoProfile.YUV_420
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.adapter.EncoderProfilesProviderAdapter
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.compat.quirk.CamcorderProfileResolutionQuirk
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.quirk.DeviceQuirks
import androidx.camera.camera2.compat.quirk.InvalidVideoProfilesQuirk
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.common.CameraId
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_8
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.QuirkSettingsHolder
import androidx.camera.core.impl.Quirks
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
class EncoderProfilesProviderAdapterDeviceTest(private val quality: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Array<Array<Int>> =
            arrayOf(
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
                arrayOf(CamcorderProfile.QUALITY_2K),
                arrayOf(CamcorderProfile.QUALITY_8KUHD),
            )

        private val CTS_UNVERIFIED_QUALITIES =
            setOf(
                CamcorderProfile.QUALITY_VGA,
                CamcorderProfile.QUALITY_4KDCI,
                CamcorderProfile.QUALITY_QHD,
                CamcorderProfile.QUALITY_2K,
                CamcorderProfile.QUALITY_8KUHD,
            )
    }

    private lateinit var encoderProfilesProvider: EncoderProfilesProviderAdapter
    private var cameraId = ""
    private var intCameraId = -1
    private lateinit var cameraMetadata: CameraMetadata
    private lateinit var streamConfigurationMapCompat: StreamConfigurationMapCompat
    private lateinit var cameraQuirks: Quirks

    @get:Rule val useCamera = CameraUtil.grantCameraPermissionAndPreTestAndPostTest()

    @get:Rule val labTestRule = LabTestRule()

    @Before
    fun setup() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        cameraId =
            requireNotNull(CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_BACK))
        intCameraId = cameraId.toInt()
        setUptEncoderProfileProvider()
    }

    private fun setUptEncoderProfileProvider() {
        val cameraPipe = CameraPipe(CameraPipe.Config(ApplicationProvider.getApplicationContext()))
        cameraMetadata =
            requireNotNull(cameraPipe.cameras().awaitCameraMetadata(CameraId(cameraId)))
        val streamConfigurationMap =
            cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
        streamConfigurationMapCompat =
            StreamConfigurationMapCompat(streamConfigurationMap, cameraMetadata)
        cameraQuirks = CameraQuirks(cameraMetadata, streamConfigurationMapCompat).quirks
        encoderProfilesProvider = EncoderProfilesProviderAdapter(cameraId, cameraQuirks)
    }

    @Test
    fun notHasProfile_getReturnNull() {
        assumeTrue(!CamcorderProfile.hasProfile(intCameraId, quality))

        assertThat(encoderProfilesProvider.getAll(quality)).isNull()
    }

    @Suppress("DEPRECATION")
    @Test
    fun hasSameContentAsCamcorderProfile() {
        assumeValidCamcorderProfile(quality)

        val profile = CamcorderProfile.get(intCameraId, quality)
        val encoderProfiles = encoderProfilesProvider.getAll(quality)!!
        val videoProfile = encoderProfiles.videoProfiles[0]
        val audioProfile = encoderProfiles.audioProfiles[0]

        assertThat(encoderProfiles.defaultDurationSeconds).isEqualTo(profile.duration)
        assertThat(encoderProfiles.recommendedFileFormat).isEqualTo(profile.fileFormat)
        assertThat(videoProfile.codec).isEqualTo(profile.videoCodec)
        assertThat(videoProfile.bitrate).isEqualTo(profile.videoBitRate)
        assertThat(videoProfile.frameRate).isEqualTo(profile.videoFrameRate)
        assertThat(videoProfile.width).isEqualTo(profile.videoFrameWidth)
        assertThat(videoProfile.height).isEqualTo(profile.videoFrameHeight)
        assertThat(audioProfile.codec).isEqualTo(profile.audioCodec)
        assertThat(audioProfile.bitrate).isEqualTo(profile.audioBitRate)
        assertThat(audioProfile.sampleRate).isEqualTo(profile.audioSampleRate)
        assertThat(audioProfile.channels).isEqualTo(profile.audioChannels)
    }

    @SdkSuppress(minSdkVersion = 31, maxSdkVersion = 32)
    @Test
    fun api31Api32_hasSameContentAsEncoderProfiles() {
        assumeValidEncoderProfiles(quality)

        val profiles = CamcorderProfile.getAll(cameraId, quality)!!
        val video = profiles.videoProfiles[0]
        val audio = profiles.audioProfiles[0]
        val profilesProxy = encoderProfilesProvider.getAll(quality)
        val videoProxy = profilesProxy!!.videoProfiles[0]
        val audioProxy = profilesProxy.audioProfiles[0]

        // Don't check video/audio profile, see cts/CamcorderProfileTest.java
        assertThat(profilesProxy.defaultDurationSeconds).isEqualTo(profiles.defaultDurationSeconds)
        assertThat(profilesProxy.recommendedFileFormat).isEqualTo(profiles.recommendedFileFormat)
        assertThat(videoProxy.codec).isEqualTo(video.codec)
        assertThat(videoProxy.mediaType).isEqualTo(video.mediaType)
        assertThat(videoProxy.bitrate).isEqualTo(video.bitrate)
        assertThat(videoProxy.frameRate).isEqualTo(video.frameRate)
        assertThat(videoProxy.width).isEqualTo(video.width)
        assertThat(videoProxy.height).isEqualTo(video.height)
        assertThat(videoProxy.bitDepth).isEqualTo(BIT_DEPTH_8)
        assertThat(videoProxy.chromaSubsampling).isEqualTo(YUV_420)
        assertThat(videoProxy.hdrFormat).isEqualTo(HDR_NONE)
        assertThat(audioProxy.codec).isEqualTo(audio.codec)
        assertThat(audioProxy.mediaType).isEqualTo(audio.mediaType)
        assertThat(audioProxy.bitrate).isEqualTo(audio.bitrate)
        assertThat(audioProxy.sampleRate).isEqualTo(audio.sampleRate)
        assertThat(audioProxy.channels).isEqualTo(audio.channels)
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    fun afterApi33_hasSameContentAsEncoderProfiles() {
        assumeValidEncoderProfiles(quality)

        val profiles = CamcorderProfile.getAll(cameraId, quality)!!
        val video = profiles.videoProfiles[0]
        val audio = profiles.audioProfiles[0]
        val profilesProxy = encoderProfilesProvider.getAll(quality)
        val videoProxy = profilesProxy!!.videoProfiles[0]
        val audioProxy = profilesProxy.audioProfiles[0]

        // Don't check video/audio profile, see cts/CamcorderProfileTest.java
        assertThat(profilesProxy.defaultDurationSeconds).isEqualTo(profiles.defaultDurationSeconds)
        assertThat(profilesProxy.recommendedFileFormat).isEqualTo(profiles.recommendedFileFormat)
        assertThat(videoProxy.codec).isEqualTo(video.codec)
        assertThat(videoProxy.mediaType).isEqualTo(video.mediaType)
        assertThat(videoProxy.bitrate).isEqualTo(video.bitrate)
        assertThat(videoProxy.frameRate).isEqualTo(video.frameRate)
        assertThat(videoProxy.width).isEqualTo(video.width)
        assertThat(videoProxy.height).isEqualTo(video.height)
        assertThat(videoProxy.bitDepth).isEqualTo(video.bitDepth)
        assertThat(videoProxy.chromaSubsampling).isEqualTo(video.chromaSubsampling)
        assertThat(videoProxy.hdrFormat).isEqualTo(video.hdrFormat)
        assertThat(audioProxy.codec).isEqualTo(audio.codec)
        assertThat(audioProxy.mediaType).isEqualTo(audio.mediaType)
        assertThat(audioProxy.bitrate).isEqualTo(audio.bitrate)
        assertThat(audioProxy.sampleRate).isEqualTo(audio.sampleRate)
        assertThat(audioProxy.channels).isEqualTo(audio.channels)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun qualityHighAndLowIsNotNull() {
        assumeTrue(
            quality == CamcorderProfile.QUALITY_HIGH || quality == CamcorderProfile.QUALITY_LOW
        )

        assertThat(encoderProfilesProvider.getAll(quality)).isNotNull()
    }

    @Test
    fun invalidResolutionInQuirk_returnsNull() {
        assumeTrue(CamcorderProfile.hasProfile(intCameraId, quality))
        assumeTrue(
            quality != CamcorderProfile.QUALITY_HIGH && quality != CamcorderProfile.QUALITY_LOW
        )
        val profileSize = getPlatformProfileSize(quality)
        assumeTrue(profileSize != null)
        assumeTrue(!isExpectedValidByResolutionQuirk(quality, requireNotNull(profileSize)))

        assertThat(encoderProfilesProvider.hasProfile(quality)).isFalse()
        assertThat(encoderProfilesProvider.getAll(quality)).isNull()
    }

    @Suppress("DEPRECATION")
    private fun getPlatformProfileSize(quality: Int): Size? {
        if (
            Build.VERSION.SDK_INT >= 31 &&
                DeviceQuirks[InvalidVideoProfilesQuirk::class.java] == null
        ) {
            val profiles = CamcorderProfile.getAll(cameraId, quality) ?: return null
            val videoProfiles = profiles.videoProfiles
            if (videoProfiles.isEmpty()) {
                return null
            }
            val video = videoProfiles[0]
            if (video != null) {
                return Size(video.width, video.height)
            }
        }
        val profile = CamcorderProfile.get(intCameraId, quality) ?: return null
        return Size(profile.videoFrameWidth, profile.videoFrameHeight)
    }

    @Suppress("DEPRECATION")
    private fun assumeValidCamcorderProfile(quality: Int) {
        assumeTrue(CamcorderProfile.hasProfile(intCameraId, quality))
        val profile = CamcorderProfile.get(intCameraId, quality)
        assumeSizeValidForCamcorderProfileResolutionQuirk(
            quality,
            Size(profile.videoFrameWidth, profile.videoFrameHeight),
        )
    }

    @RequiresApi(31)
    private fun assumeValidEncoderProfiles(quality: Int) {
        assumeTrue(CamcorderProfile.hasProfile(intCameraId, quality))
        val profiles = requireNotNull(CamcorderProfile.getAll(cameraId, quality))
        val video = profiles.videoProfiles[0]
        assumeTrue(video != null)
        assumeSizeValidForCamcorderProfileResolutionQuirk(quality, Size(video.width, video.height))
    }

    private fun assumeSizeValidForCamcorderProfileResolutionQuirk(
        quality: Int,
        profileSize: Size,
    ) {
        if (!isExpectedValidByResolutionQuirk(quality, profileSize)) {
            val supportedSizes = getCameraSupportedPrivateSizes()
            assumeTrue(
                "The profile size $profileSize for quality $quality is not in camera supported " +
                    "resolutions $supportedSizes, which is an invalid profile.",
                false,
            )
        }
    }

    private fun isExpectedValidByResolutionQuirk(quality: Int, profileSize: Size): Boolean {
        val isLegacy = cameraMetadata.isHardwareLevelLegacy
        val shouldEnableQuirk =
            QuirkSettingsHolder.instance()
                .get()
                .shouldEnableQuirk(
                    CamcorderProfileResolutionQuirk::class.java,
                    isLegacy || Build.VERSION.SDK_INT >= 29,
                )
        if (!shouldEnableQuirk) {
            return true
        }
        if (!isLegacy && quality !in CTS_UNVERIFIED_QUALITIES) {
            return true
        }
        val supportedSizes = getCameraSupportedPrivateSizes()
        if (!isLegacy && supportedSizes.isEmpty()) {
            return true
        }
        return profileSize in supportedSizes
    }

    private fun getCameraSupportedPrivateSizes(): List<Size> =
        streamConfigurationMapCompat
            .getOutputSizes(ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE)
            ?.asList()
            .orEmpty()
}
