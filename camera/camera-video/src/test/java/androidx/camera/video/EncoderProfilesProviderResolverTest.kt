/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.video

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_HIGH
import android.media.EncoderProfiles.VideoProfile
import android.media.MediaFormat
import android.os.Build
import android.util.Range
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.Quality.QUALITY_SOURCE_HIGH_SPEED
import androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class EncoderProfilesProviderResolverTest {

    private val videoEncoderInfoFinder = VideoEncoderInfo.Finder { FakeVideoEncoderInfo() }

    @Test
    fun resolve_highSpeed_returnsProviderIfSupported() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                isHighSpeedSupported = true
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder()
                        .add(QUALITY_HIGH_SPEED_HIGH, PROFILES_1080P)
                        .build()
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_HIGH_SPEED,
                videoEncoderInfoFinder,
            )

        assertThat(provider).isSameInstanceAs(cameraInfo.encoderProfilesProvider)
    }

    @Test
    fun resolve_highSpeed_returnsEmptyIfNotSupported() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                isHighSpeedSupported = false
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder()
                        .add(QUALITY_HIGH_SPEED_HIGH, PROFILES_1080P)
                        .build()
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_HIGH_SPEED,
                videoEncoderInfoFinder,
            )

        assertThat(provider).isSameInstanceAs(EncoderProfilesProvider.EMPTY)
    }

    @Test
    fun resolve_noSupportedQuality_createsDefaultProfiles() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider = EncoderProfilesProvider.EMPTY
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P),
                )
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        // Assert: 1080p is added.
        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
    }

    @Test
    fun resolve_codecCapabilitiesSource_usesQualityExploredProvider() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P, RESOLUTION_2160P),
                )
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        // Assert: 2160P is explored.
        assertThat(provider.hasProfile(QUALITY_2160P)).isTrue()
    }

    @Test
    fun resolve_hlg10Supported_addsHlg10Profile() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                supportedDynamicRanges = setOf(DynamicRange.HLG_10_BIT)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(
                provider.getAll(QUALITY_1080P)!!.videoProfiles.any {
                    it.hdrFormat == VideoProfile.HDR_HLG
                }
            )
            .isTrue()
    }

    @Test
    fun resolve_hlg10Smpte209450Supported_addsHlg10Profile() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                supportedDynamicRanges = setOf(DynamicRange.HLG_10_BIT_SMPTE_2094_50)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(
                provider.getAll(QUALITY_1080P)!!.videoProfiles.any {
                    it.hdrFormat == VideoProfile.HDR_HLG
                }
            )
            .isTrue()
    }

    @Test
    fun resolve_hlg10Supported_softwareEncoder_doesNotAddHdrProfile() {
        val softwareFinder = VideoEncoderInfo.Finder {
            FakeVideoEncoderInfo(isHardwareAccelerated = false)
        }
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                supportedDynamicRanges = setOf(DynamicRange.HLG_10_BIT)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                softwareFinder,
            )

        assertThat(
                requireNotNull(provider.getAll(QUALITY_1080P)).videoProfiles.any {
                    it.hdrFormat == VideoProfile.HDR_HLG
                }
            )
            .isFalse()
    }

    @Test
    fun resolve_noSupportedQuality_camcorderProfileSource_limitsToFhd() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider = EncoderProfilesProvider.EMPTY
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_2160P, RESOLUTION_1080P),
                )
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        assertThat(provider.hasProfile(QUALITY_2160P)).isFalse()
    }

    @Test
    fun resolve_noSupportedQuality_codecCapabilitiesSource_addsAllSupportedQualities() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider = EncoderProfilesProvider.EMPTY
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_2160P, RESOLUTION_1080P),
                )
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        assertThat(provider.hasProfile(QUALITY_2160P)).isTrue()
    }

    @Test
    fun resolve_camcorderProfileSource_doesNotAddQuality() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P, RESOLUTION_2160P),
                )
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(provider.hasProfile(QUALITY_2160P)).isFalse()
    }

    @Test
    fun resolve_hlg10Supported_doesNotAddHdrProfileToQualityWithoutVideoProfile() {
        val profilesWithoutVideo =
            EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
                PROFILES_1080P.defaultDurationSeconds,
                PROFILES_1080P.recommendedFileFormat,
                PROFILES_1080P.audioProfiles,
                emptyList(),
            )
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder()
                        .add(QUALITY_1080P, PROFILES_1080P)
                        .add(QUALITY_2160P, profilesWithoutVideo)
                        .build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P, RESOLUTION_2160P),
                )
                supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG_10_BIT)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(provider.getAll(QUALITY_2160P)?.videoProfiles.orEmpty()).isEmpty()
    }

    @Test
    fun resolve_hlg10Supported_doesNotAddQuality() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P, RESOLUTION_2160P),
                )
                supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG_10_BIT)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        // Assert: HLG10 is added to FHD, but no UHD profile is created for HLG10.
        assertThat(provider.getAll(QUALITY_1080P)!!.videoProfiles.map { it.hdrFormat })
            .contains(VideoProfile.HDR_HLG)
        assertThat(provider.hasProfile(QUALITY_2160P)).isFalse()
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun resolve_codecCapabilitiesSource_addsCameraSupportedHdrProfiles() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P, RESOLUTION_2160P),
                )
                supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.DOLBY_VISION_10_BIT)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        // Assert: Dolby Vision is added since it is supported by the camera, but HDR10 is not.
        for (quality in listOf(QUALITY_1080P, QUALITY_2160P)) {
            val hdrFormats = provider.getAll(quality)!!.videoProfiles.map { it.hdrFormat }
            assertThat(hdrFormats).contains(VideoProfile.HDR_DOLBY_VISION)
            assertThat(hdrFormats).doesNotContain(VideoProfile.HDR_HDR10)
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun resolve_camcorderProfileSource_doesNotAddCameraSupportedHdrProfiles() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.DOLBY_VISION_10_BIT)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(provider.getAll(QUALITY_1080P)!!.videoProfiles.map { it.hdrFormat })
            .doesNotContain(VideoProfile.HDR_DOLBY_VISION)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun resolve_codecCapabilitiesSource_addsHdr10Profile_whenCameraSupportsHdr10Smpte209450() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P),
                )
                supportedDynamicRanges =
                    setOf(DynamicRange.SDR, DynamicRange.HDR10_10_BIT_SMPTE_2094_50)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(provider.getAll(QUALITY_1080P)!!.videoProfiles.map { it.hdrFormat })
            .contains(VideoProfile.HDR_HDR10)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun resolve_codecCapabilitiesSource_exploresHlg10_whenSdrCanNotBeExplored() {
        // Arrange: AVC supports up to 1080p, while HEVC supports up to 2160p.
        val finder = VideoEncoderInfo.Finder { mimeType ->
            if (mimeType == MediaFormat.MIMETYPE_VIDEO_AVC) {
                FakeVideoEncoderInfo(
                    supportedWidths = Range.create(1, RESOLUTION_1080P.width),
                    supportedHeights = Range.create(1, RESOLUTION_1080P.height),
                )
            } else {
                FakeVideoEncoderInfo()
            }
        }
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P, RESOLUTION_2160P),
                )
                supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG_10_BIT)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                QUALITY_SOURCE_REGULAR,
                finder,
            )

        // Assert: UHD HLG10 is explored with HEVC even though UHD SDR (AVC) is not supported.
        val hdrFormats = provider.getAll(QUALITY_2160P)!!.videoProfiles.map { it.hdrFormat }
        assertThat(hdrFormats).containsExactly(VideoProfile.HDR_HLG)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun resolve_codecCapabilitiesSource_addsAllCameraSupportedHdrProfiles() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P),
                )
                supportedDynamicRanges =
                    setOf(
                        DynamicRange.SDR,
                        DynamicRange.HLG_10_BIT,
                        DynamicRange.HDR10_10_BIT,
                        DynamicRange.HDR10_PLUS_10_BIT,
                        DynamicRange.DOLBY_VISION_10_BIT,
                    )
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(provider.getAll(QUALITY_1080P)!!.videoProfiles.map { it.hdrFormat })
            .containsExactly(
                VideoProfile.HDR_NONE,
                VideoProfile.HDR_HLG,
                VideoProfile.HDR_HDR10,
                VideoProfile.HDR_HDR10PLUS,
                VideoProfile.HDR_DOLBY_VISION,
            )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun resolve_codecCapabilitiesSource_doesNotAddHdrProfiles_whenNoHardwareHdrEncoder() {
        // Arrange: HEVC encoder is software and there is no Dolby Vision encoder.
        val finder = VideoEncoderInfo.Finder { mimeType ->
            when (mimeType) {
                MediaFormat.MIMETYPE_VIDEO_HEVC ->
                    FakeVideoEncoderInfo(isHardwareAccelerated = false)
                MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION -> null
                else -> FakeVideoEncoderInfo()
            }
        }
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P, RESOLUTION_2160P),
                )
                supportedDynamicRanges =
                    setOf(
                        DynamicRange.SDR,
                        DynamicRange.HLG_10_BIT,
                        DynamicRange.HDR10_10_BIT,
                        DynamicRange.HDR10_PLUS_10_BIT,
                        DynamicRange.DOLBY_VISION_10_BIT,
                    )
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                QUALITY_SOURCE_REGULAR,
                finder,
            )

        // Assert: only SDR is supported, and UHD SDR is still explored.
        for (quality in listOf(QUALITY_1080P, QUALITY_2160P)) {
            assertThat(provider.getAll(quality)!!.videoProfiles.map { it.hdrFormat })
                .containsExactly(VideoProfile.HDR_NONE)
        }
    }
}
