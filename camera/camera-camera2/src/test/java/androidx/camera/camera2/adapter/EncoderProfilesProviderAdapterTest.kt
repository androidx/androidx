/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.adapter

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile
import android.media.EncoderProfiles
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.compat.quirk.CamcorderProfileResolutionQuirk
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.testing.impl.EncoderProfilesUtil
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCamcorderProfile
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    sdk = [Config.ALL_SDKS],
    shadows =
        [
            EncoderProfilesProviderAdapterTest.ShadowEncoderProfilesApi31::class,
            EncoderProfilesProviderAdapterTest.ShadowVideoProfileApi31::class,
            EncoderProfilesProviderAdapterTest.ShadowCamcorderProfileApi31::class,
        ],
)
class EncoderProfilesProviderAdapterTest {

    @Before
    fun setUp() {
        ShadowCamcorderProfile.reset()
        ShadowCamcorderProfileApi31.reset()
    }

    @After
    fun tearDown() {
        ShadowCamcorderProfile.reset()
        ShadowCamcorderProfileApi31.reset()
    }

    @Test
    fun legacyCamera_validatesEveryQualityAndRemapsHighAndLow() {
        registerProfile(CamcorderProfile.QUALITY_HIGH, EncoderProfilesUtil.RESOLUTION_1080P)
        registerProfile(CamcorderProfile.QUALITY_1080P, EncoderProfilesUtil.RESOLUTION_1080P)
        registerProfile(CamcorderProfile.QUALITY_720P, EncoderProfilesUtil.RESOLUTION_720P)
        registerProfile(CamcorderProfile.QUALITY_480P, EncoderProfilesUtil.RESOLUTION_480P)
        registerProfile(CamcorderProfile.QUALITY_CIF, EncoderProfilesUtil.RESOLUTION_CIF)
        registerProfile(CamcorderProfile.QUALITY_LOW, EncoderProfilesUtil.RESOLUTION_CIF)

        val adapter =
            createAdapter(
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                supportedSizes =
                    arrayOf(
                        EncoderProfilesUtil.RESOLUTION_720P,
                        EncoderProfilesUtil.RESOLUTION_480P,
                    ),
            )

        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_1080P)).isFalse()
        assertThat(adapter.getAll(CamcorderProfile.QUALITY_1080P)).isNull()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_CIF)).isFalse()
        assertThat(adapter.getAll(CamcorderProfile.QUALITY_CIF)).isNull()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_720P)).isTrue()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_480P)).isTrue()

        assertThat(
                requireNotNull(adapter.getAll(CamcorderProfile.QUALITY_HIGH))
                    .videoProfiles[0]
                    .resolution
            )
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_720P)
        assertThat(
                requireNotNull(adapter.getAll(CamcorderProfile.QUALITY_LOW))
                    .videoProfiles[0]
                    .resolution
            )
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_480P)
    }

    @Test
    @Config(minSdk = 29)
    fun nonLegacyCamera_api29Plus_filtersUnsupportedCtsUnverifiedQualities() {
        val resolution8kUhd = Size(7680, 4320)
        registerProfile(CamcorderProfile.QUALITY_HIGH, EncoderProfilesUtil.RESOLUTION_2160P)
        registerProfile(CamcorderProfile.QUALITY_2160P, EncoderProfilesUtil.RESOLUTION_2160P)
        registerProfile(CamcorderProfile.QUALITY_8KUHD, resolution8kUhd)
        registerProfile(CamcorderProfile.QUALITY_4KDCI, EncoderProfilesUtil.RESOLUTION_4KDCI)
        registerProfile(CamcorderProfile.QUALITY_QHD, EncoderProfilesUtil.RESOLUTION_QHD)
        registerProfile(CamcorderProfile.QUALITY_2K, EncoderProfilesUtil.RESOLUTION_2K)
        registerProfile(CamcorderProfile.QUALITY_1080P, EncoderProfilesUtil.RESOLUTION_1080P)
        registerProfile(CamcorderProfile.QUALITY_VGA, EncoderProfilesUtil.RESOLUTION_VGA)
        registerProfile(CamcorderProfile.QUALITY_LOW, EncoderProfilesUtil.RESOLUTION_480P)
        registerProfile(CamcorderProfile.QUALITY_480P, EncoderProfilesUtil.RESOLUTION_480P)

        val adapter =
            createAdapter(
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                supportedSizes =
                    arrayOf(
                        EncoderProfilesUtil.RESOLUTION_2160P,
                        EncoderProfilesUtil.RESOLUTION_1080P,
                        EncoderProfilesUtil.RESOLUTION_480P,
                    ),
            )

        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_QHD)).isFalse()
        assertThat(adapter.getAll(CamcorderProfile.QUALITY_QHD)).isNull()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_2K)).isFalse()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_4KDCI)).isFalse()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_8KUHD)).isFalse()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_VGA)).isFalse()

        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_2160P)).isTrue()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_1080P)).isTrue()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_480P)).isTrue()
    }

    @Test
    @Config(minSdk = 29)
    fun nonLegacyCamera_api29Plus_keepsSupportedCtsUnverifiedQualities() {
        registerProfile(CamcorderProfile.QUALITY_QHD, EncoderProfilesUtil.RESOLUTION_QHD)

        val adapter =
            createAdapter(
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                supportedSizes =
                    arrayOf(
                        EncoderProfilesUtil.RESOLUTION_QHD,
                        EncoderProfilesUtil.RESOLUTION_1080P,
                    ),
            )

        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_QHD)).isTrue()
        assertThat(
                requireNotNull(adapter.getAll(CamcorderProfile.QUALITY_QHD))
                    .videoProfiles[0]
                    .resolution
            )
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_QHD)
    }

    @Test
    @Config(minSdk = 29)
    fun nonLegacyCamera_api29Plus_trustsCtsVerifiedQualityMissingFromSupportedSizes() {
        // Simulates devices like Samsung J7 / J7 Prime where ExcludedSupportedSizesQuirk removes
        // 1920x1080 PRIVATE output size for preview-stretch workaround, while 1080p video profile
        // remains valid per CTS RecordingTest.
        registerProfile(CamcorderProfile.QUALITY_1080P, EncoderProfilesUtil.RESOLUTION_1080P)

        val adapter =
            createAdapter(
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                supportedSizes =
                    arrayOf(
                        EncoderProfilesUtil.RESOLUTION_720P,
                        EncoderProfilesUtil.RESOLUTION_480P,
                    ),
            )

        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_1080P)).isTrue()
        assertThat(
                requireNotNull(adapter.getAll(CamcorderProfile.QUALITY_1080P))
                    .videoProfiles[0]
                    .resolution
            )
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_1080P)
    }

    @Test
    @Config(minSdk = 29)
    fun nonLegacyCamera_api29Plus_neverFiltersOrRemapsQualityHighAndLow() {
        registerProfile(CamcorderProfile.QUALITY_HIGH, EncoderProfilesUtil.RESOLUTION_1080P)
        registerProfile(CamcorderProfile.QUALITY_720P, EncoderProfilesUtil.RESOLUTION_720P)
        registerProfile(CamcorderProfile.QUALITY_LOW, EncoderProfilesUtil.RESOLUTION_CIF)

        val adapter =
            createAdapter(
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                supportedSizes = arrayOf(EncoderProfilesUtil.RESOLUTION_720P),
            )

        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_HIGH)).isTrue()
        assertThat(
                requireNotNull(adapter.getAll(CamcorderProfile.QUALITY_HIGH))
                    .videoProfiles[0]
                    .resolution
            )
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_1080P)
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_LOW)).isTrue()
        assertThat(
                requireNotNull(adapter.getAll(CamcorderProfile.QUALITY_LOW))
                    .videoProfiles[0]
                    .resolution
            )
            .isEqualTo(EncoderProfilesUtil.RESOLUTION_CIF)
    }

    @Test
    @Config(minSdk = 29)
    fun nonLegacyCamera_api29Plus_emptySupportedSizes_doesNotFilterProfiles() {
        registerProfile(CamcorderProfile.QUALITY_QHD, EncoderProfilesUtil.RESOLUTION_QHD)
        registerProfile(CamcorderProfile.QUALITY_1080P, EncoderProfilesUtil.RESOLUTION_1080P)

        val adapter =
            createAdapter(
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                supportedSizes = emptyArray(),
            )

        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_QHD)).isTrue()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_1080P)).isTrue()
    }

    @Test
    @Config(minSdk = 29)
    fun nonLegacyCamera_api29Plus_highResolutionOnlySize_isFiltered() {
        registerProfile(CamcorderProfile.QUALITY_QHD, EncoderProfilesUtil.RESOLUTION_QHD)

        val adapter =
            createAdapter(
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                supportedSizes = arrayOf(EncoderProfilesUtil.RESOLUTION_1080P),
                highResolutionSizes = arrayOf(EncoderProfilesUtil.RESOLUTION_QHD),
            )

        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_QHD)).isFalse()
        assertThat(adapter.getAll(CamcorderProfile.QUALITY_QHD)).isNull()
    }

    @Test
    @Config(maxSdk = 28)
    fun nonLegacyCamera_belowApi29_quirkNotEnabled() {
        registerProfile(CamcorderProfile.QUALITY_QHD, EncoderProfilesUtil.RESOLUTION_QHD)

        val quirks =
            createCameraQuirks(
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                supportedSizes = arrayOf(EncoderProfilesUtil.RESOLUTION_1080P),
            )
        val adapter = EncoderProfilesProviderAdapter(CAMERA_ID_STRING, quirks)

        assertThat(quirks.contains(CamcorderProfileResolutionQuirk::class.java)).isFalse()
        assertThat(adapter.hasProfile(CamcorderProfile.QUALITY_QHD)).isTrue()
    }

    private fun createAdapter(
        hardwareLevel: Int,
        supportedSizes: Array<Size>,
        highResolutionSizes: Array<Size> = emptyArray(),
    ): EncoderProfilesProviderAdapter {
        val quirks = createCameraQuirks(hardwareLevel, supportedSizes, highResolutionSizes)
        return EncoderProfilesProviderAdapter(CAMERA_ID_STRING, quirks)
    }

    private fun createCameraQuirks(
        hardwareLevel: Int,
        supportedSizes: Array<Size>,
        highResolutionSizes: Array<Size> = emptyArray(),
    ) = run {
        val mockMap = Mockito.mock(StreamConfigurationMap::class.java)
        Mockito.`when`(mockMap.getOutputSizes(ArgumentMatchers.any<Class<SurfaceTexture>>()))
            .thenReturn(supportedSizes)
        Mockito.`when`(mockMap.getOutputSizes(ArgumentMatchers.anyInt())).thenReturn(supportedSizes)
        if (Build.VERSION.SDK_INT >= 23) {
            Mockito.`when`(
                    mockMap.getHighResolutionOutputSizes(
                        ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                    )
                )
                .thenReturn(highResolutionSizes)
        }

        val cameraMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                lensFacing = CameraCharacteristics.LENS_FACING_BACK,
                characteristicsOverrides =
                    mapOf(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to mockMap,
                    ),
            )
        CameraQuirks(
                cameraMetadata,
                StreamConfigurationMapCompat(mockMap, cameraMetadata),
            )
            .quirks
    }

    private fun registerProfile(quality: Int, resolution: Size) {
        val camcorderProfile = createFakeCamcorderProfile(resolution)
        ShadowCamcorderProfile.addProfile(CAMERA_ID, quality, camcorderProfile)
        if (Build.VERSION.SDK_INT >= 31) {
            ShadowCamcorderProfileApi31.addEncoderProfiles(
                CAMERA_ID,
                quality,
                createFakeEncoderProfiles(resolution),
            )
        }
    }

    private fun createFakeCamcorderProfile(resolution: Size): CamcorderProfile {
        val profile = ReflectionHelpers.callConstructor(CamcorderProfile::class.java)
        ReflectionHelpers.setField(profile, "duration", EncoderProfilesUtil.DEFAULT_DURATION)
        ReflectionHelpers.setField(profile, "fileFormat", EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT)
        ReflectionHelpers.setField(profile, "videoCodec", MediaRecorder.VideoEncoder.H264)
        ReflectionHelpers.setField(
            profile,
            "videoBitRate",
            EncoderProfilesUtil.DEFAULT_VIDEO_BITRATE,
        )
        ReflectionHelpers.setField(
            profile,
            "videoFrameRate",
            EncoderProfilesUtil.DEFAULT_VIDEO_FRAME_RATE,
        )
        ReflectionHelpers.setField(profile, "videoFrameWidth", resolution.width)
        ReflectionHelpers.setField(profile, "videoFrameHeight", resolution.height)
        ReflectionHelpers.setField(profile, "audioCodec", MediaRecorder.AudioEncoder.AAC)
        ReflectionHelpers.setField(
            profile,
            "audioBitRate",
            EncoderProfilesUtil.DEFAULT_AUDIO_BITRATE,
        )
        ReflectionHelpers.setField(
            profile,
            "audioSampleRate",
            EncoderProfilesUtil.DEFAULT_AUDIO_SAMPLE_RATE,
        )
        ReflectionHelpers.setField(
            profile,
            "audioChannels",
            EncoderProfilesUtil.DEFAULT_AUDIO_CHANNELS,
        )
        return profile
    }

    @RequiresApi(31)
    private fun createFakeEncoderProfiles(resolution: Size): EncoderProfiles {
        val videoProfile =
            ReflectionHelpers.callConstructor(EncoderProfiles.VideoProfile::class.java)
        val shadowVideo: ShadowVideoProfileApi31 = Shadow.extract(videoProfile)
        shadowVideo.widthVar = resolution.width
        shadowVideo.heightVar = resolution.height

        val encoderProfiles = ReflectionHelpers.callConstructor(EncoderProfiles::class.java)
        val shadowProfiles: ShadowEncoderProfilesApi31 = Shadow.extract(encoderProfiles)
        shadowProfiles.videoProfilesList = listOf(videoProfile)
        return encoderProfiles
    }

    @Suppress("unused")
    @Implements(EncoderProfiles.VideoProfile::class, minSdk = 31)
    class ShadowVideoProfileApi31 {
        var codecVar = MediaRecorder.VideoEncoder.H264
        var mediaTypeVar = MediaFormat.MIMETYPE_VIDEO_AVC
        var bitrateVar = EncoderProfilesUtil.DEFAULT_VIDEO_BITRATE
        var frameRateVar = EncoderProfilesUtil.DEFAULT_VIDEO_FRAME_RATE
        var widthVar = 0
        var heightVar = 0
        var profileVar = EncoderProfilesProxy.CODEC_PROFILE_NONE
        var bitDepthVar = VideoProfileProxy.BIT_DEPTH_8
        var chromaSubsamplingVar = EncoderProfiles.VideoProfile.YUV_420
        var hdrFormatVar = EncoderProfiles.VideoProfile.HDR_NONE

        @Implementation fun getCodec() = codecVar

        @Implementation fun getMediaType() = mediaTypeVar

        @Implementation fun getBitrate() = bitrateVar

        @Implementation fun getFrameRate() = frameRateVar

        @Implementation fun getWidth() = widthVar

        @Implementation fun getHeight() = heightVar

        @Implementation fun getProfile() = profileVar

        @Implementation fun getBitDepth() = bitDepthVar

        @Implementation fun getChromaSubsampling() = chromaSubsamplingVar

        @Implementation fun getHdrFormat() = hdrFormatVar
    }

    @Suppress("unused")
    @Implements(EncoderProfiles::class, minSdk = 31)
    class ShadowEncoderProfilesApi31 {
        var durationVar = EncoderProfilesUtil.DEFAULT_DURATION
        var fileFormatVar = EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT
        var audioProfilesList = emptyList<EncoderProfiles.AudioProfile>()
        var videoProfilesList = emptyList<EncoderProfiles.VideoProfile>()

        @Implementation fun getDefaultDurationSeconds() = durationVar

        @Implementation fun getRecommendedFileFormat() = fileFormatVar

        @Implementation fun getAudioProfiles() = audioProfilesList

        @Implementation fun getVideoProfiles() = videoProfilesList
    }

    @Implements(CamcorderProfile::class)
    class ShadowCamcorderProfileApi31 : ShadowCamcorderProfile() {
        companion object {
            private val profileMap = mutableMapOf<String, EncoderProfiles>()

            @JvmStatic
            @Implementation
            fun getAll(cameraId: String, quality: Int) = profileMap["$cameraId-$quality"]

            fun addEncoderProfiles(cameraId: Int, quality: Int, profiles: EncoderProfiles) {
                profileMap["$cameraId-$quality"] = profiles
            }

            fun reset() {
                profileMap.clear()
            }
        }
    }

    private companion object {
        const val CAMERA_ID = 0
        const val CAMERA_ID_STRING = "0"
    }
}
