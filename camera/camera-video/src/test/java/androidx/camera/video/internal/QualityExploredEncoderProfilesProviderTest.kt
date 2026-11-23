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

package androidx.camera.video.internal

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.CamcorderProfile.QUALITY_HIGH
import android.media.CamcorderProfile.QUALITY_LOW
import android.media.CamcorderProfile.QUALITY_QHD
import android.media.EncoderProfiles.VideoProfile.HDR_HDR10
import android.media.EncoderProfiles.VideoProfile.HDR_HLG
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION
import android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
import android.media.MediaRecorder.OutputFormat.MPEG_4
import android.media.MediaRecorder.OutputFormat.THREE_GPP
import android.media.MediaRecorder.OutputFormat.WEBM
import android.media.MediaRecorder.VideoEncoder.DOLBY_VISION
import android.media.MediaRecorder.VideoEncoder.H263
import android.media.MediaRecorder.VideoEncoder.MPEG_4_SP
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10
import androidx.camera.testing.impl.EncoderProfilesUtil.DEFAULT_DURATION
import androidx.camera.testing.impl.EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_QHD
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeAudioProfileProxy
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeVideoProfileProxy
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.Quality
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.QHD
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import androidx.camera.video.internal.utils.DynamicRangeUtil.isHdrSettingsMatched
import androidx.camera.video.internal.utils.EncoderProfilesUtil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class QualityExploredEncoderProfilesProviderTest {

    private val allQualities = Quality.getSortedQualities()
    private val providerSupportOnlySdrFhd =
        FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_HIGH, PROFILES_1080P)
            .add(QUALITY_1080P, PROFILES_1080P)
            .add(QUALITY_LOW, PROFILES_1080P)
            .build()
    private val defaultCameraResolutions =
        mutableSetOf(RESOLUTION_2160P, RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P)
    private val unlimitedVideoEncoderInfo = FakeVideoEncoderInfo()

    @Test
    fun canExploreQualities_byTargetQualities() {
        // Arrange: SD is not a target quality.
        val targetQualities = setOf(UHD, FHD, HD)

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                providerSupportOnlySdrFhd,
                targetQualities,
                setOf(SDR),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert.
        verifyQualitiesAreSupported(provider, SDR, QUALITY_2160P, QUALITY_1080P, QUALITY_720P)
        verifyQualitiesAreNotSupported(provider, SDR, QUALITY_480P)
    }

    @Test
    fun canNotExploreQualities_whenCannotFindEncoderInfo() {
        // Arrange: SD is not a target quality.
        val targetQualities = setOf(UHD, FHD, HD)

        // Act: EncoderFinder always return null
        val provider =
            QualityExploredEncoderProfilesProvider(
                providerSupportOnlySdrFhd,
                targetQualities,
                setOf(SDR),
                defaultCameraResolutions,
            ) {
                null
            }

        // Assert.
        verifyQualitiesAreNotSupported(provider, SDR, QUALITY_2160P, QUALITY_720P)
    }

    @Test
    fun createDefaultProfiles_whenNoBaseProfile() {
        // Arrange: a provider doesn't have any supported qualities.
        val emptyProvider = FakeEncoderProfilesProvider.Builder().build()

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                emptyProvider,
                allQualities,
                setOf(SDR),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert: all qualities supported by the camera are created from default values.
        verifyQualitiesAreSupported(
            provider,
            SDR,
            QUALITY_2160P,
            QUALITY_1080P,
            QUALITY_720P,
            QUALITY_480P,
        )
        val profiles = provider.getAll(QUALITY_1080P)!!
        assertThat(profiles.defaultDurationSeconds)
            .isEqualTo(EncoderProfilesUtil.DEFAULT_DURATION_SECONDS)
        assertThat(profiles.recommendedFileFormat)
            .isEqualTo(EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT)
        assertThat(profiles.audioProfiles.single().mediaType).isEqualTo(MIMETYPE_AUDIO_AAC)
        val videoProfile = profiles.videoProfiles.single()
        assertThat(videoProfile.mediaType).isEqualTo(MIMETYPE_VIDEO_AVC)
        assertThat(videoProfile.resolution).isEqualTo(RESOLUTION_1080P)
        // 4 Mbps at 720p scaled to 1080p.
        assertThat(videoProfile.bitrate).isEqualTo(9_000_000)
    }

    @Test
    fun createDefaultProfiles_qhd() {
        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                FakeEncoderProfilesProvider.Builder().build(),
                setOf(QHD),
                setOf(SDR),
                setOf(RESOLUTION_QHD),
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert: 4 Mbps at 720p scaled to 1440p.
        val videoProfile = provider.getAll(QUALITY_QHD)!!.videoProfiles.single()
        assertThat(videoProfile.resolution).isEqualTo(RESOLUTION_QHD)
        assertThat(videoProfile.bitrate).isEqualTo(16_000_000)
    }

    @Test
    fun createDefaultProfiles_usesNextTypicalSize_whenCameraNotSupportFirstTypicalSize() {
        // Arrange: camera supports 640x480 but not 720x480, the first typical size of SD.
        val resolutionVga = Size(640, 480)

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                FakeEncoderProfilesProvider.Builder().build(),
                setOf(SD),
                setOf(SDR),
                setOf(resolutionVga),
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert.
        assertThat(provider.getAll(QUALITY_480P)!!.videoProfiles.single().resolution)
            .isEqualTo(resolutionVga)
    }

    @Test
    fun canNotCreateDefaultProfiles_whenEncoderNotSupportSize() {
        // Arrange: encoder supports up to 1080p.
        val videoEncoderInfo =
            FakeVideoEncoderInfo(
                supportedWidths = Range.create(1, RESOLUTION_1080P.width),
                supportedHeights = Range.create(1, RESOLUTION_1080P.height),
            )

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                FakeEncoderProfilesProvider.Builder().build(),
                setOf(UHD, FHD),
                setOf(SDR),
                defaultCameraResolutions,
            ) {
                videoEncoderInfo
            }

        // Assert.
        assertThat(provider.hasProfile(QUALITY_2160P)).isFalse()
        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
    }

    @Test
    fun createDefaultProfiles_bitrateIsClamped() {
        // Arrange: a provider doesn't have any supported qualities.
        val emptyProvider = FakeEncoderProfilesProvider.Builder().build()
        val maxBitrate = 1000
        val videoEncoderInfo =
            FakeVideoEncoderInfo(supportedBitrateRange = Range.create(0, maxBitrate))

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                emptyProvider,
                setOf(FHD),
                setOf(SDR),
                defaultCameraResolutions,
            ) {
                videoEncoderInfo
            }

        // Assert.
        assertThat(provider.getAll(QUALITY_1080P)!!.videoProfiles.single().bitrate)
            .isEqualTo(maxBitrate)
    }

    @Test
    fun canNotExploreQuality_whenCameraNotSupport() {
        // Arrange: camera doesn't support HD(720P).
        val cameraResolutions = defaultCameraResolutions.apply { remove(RESOLUTION_720P) }

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                providerSupportOnlySdrFhd,
                allQualities,
                setOf(SDR),
                cameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert.
        verifyQualitiesAreSupported(provider, SDR, QUALITY_2160P, QUALITY_1080P, QUALITY_480P)
        verifyQualitiesAreNotSupported(provider, SDR, QUALITY_720P)
    }

    @Test
    fun canNotExploreQuality_whenCodecNotSupport() {
        // Arrange: codec does not support UHD(2160P).
        val videoEncoderInfo =
            FakeVideoEncoderInfo(
                supportedWidths = Range.create(0, RESOLUTION_1080P.width),
                supportedHeights = Range.create(0, RESOLUTION_1080P.height),
            )

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                providerSupportOnlySdrFhd,
                allQualities,
                setOf(SDR),
                defaultCameraResolutions,
            ) {
                videoEncoderInfo
            }

        // Assert.
        verifyQualitiesAreSupported(provider, SDR, QUALITY_1080P, QUALITY_720P, QUALITY_480P)
        verifyQualitiesAreNotSupported(provider, SDR, QUALITY_2160P)
    }

    @Test
    fun deriveHdrProfileFromSdr_whenNoProfileOfSameDynamicRange() {
        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                providerSupportOnlySdrFhd,
                allQualities,
                setOf(SDR, HLG_10_BIT),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert: HLG10 profiles are derived from the SDR profiles.
        verifyQualitiesAreSupported(
            provider,
            HLG_10_BIT,
            QUALITY_2160P,
            QUALITY_1080P,
            QUALITY_720P,
            QUALITY_480P,
        )
        val videoProfiles = provider.getAll(QUALITY_1080P)!!.videoProfiles
        val sdrProfile = videoProfiles.single { isHdrSettingsMatched(it, SDR) }
        val hlgProfile = videoProfiles.single { isHdrSettingsMatched(it, HLG_10_BIT) }
        assertThat(hlgProfile.mediaType).isEqualTo(MIMETYPE_VIDEO_HEVC)
        assertThat(hlgProfile.resolution).isEqualTo(sdrProfile.resolution)
        assertThat(hlgProfile.bitrate).isEqualTo(sdrProfile.bitrate * 10 / 8)
    }

    @Test
    fun canNotExploreHdrProfile_whenEncoderIsSoftware() {
        // Act: a software encoder doesn't support 10-bit HDR.
        val provider =
            QualityExploredEncoderProfilesProvider(
                providerSupportOnlySdrFhd,
                allQualities,
                setOf(SDR, HLG_10_BIT),
                defaultCameraResolutions,
            ) {
                FakeVideoEncoderInfo(isHardwareAccelerated = false)
            }

        // Assert.
        verifyAllQualitiesAreNotSupported(provider, HLG_10_BIT)
        verifyQualitiesAreSupported(
            provider,
            SDR,
            QUALITY_2160P,
            QUALITY_1080P,
            QUALITY_720P,
            QUALITY_480P,
        )
    }

    @Test
    fun preferProfileOfSameDynamicRange_overSdrProfile() {
        // Arrange: FHD has an SDR profile and UHD has an HLG10 profile.
        val profilesUhdHlg =
            createEncoderProfiles(
                createFakeVideoProfileProxy(
                    RESOLUTION_2160P,
                    videoMediaType = MIMETYPE_VIDEO_HEVC,
                    bitrate = 40_000_000,
                    videoBitDepth = BIT_DEPTH_10,
                    videoHdrFormat = HDR_HLG,
                )
            )
        val profilesFhdSdr =
            createEncoderProfiles(createFakeVideoProfileProxy(RESOLUTION_1080P, bitrate = 1_000))
        val baseProvider =
            FakeEncoderProfilesProvider.Builder()
                .add(QUALITY_2160P, profilesUhdHlg)
                .add(QUALITY_1080P, profilesFhdSdr)
                .build()

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                baseProvider,
                setOf(FHD),
                setOf(SDR, HLG_10_BIT),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert: FHD HLG10 is scaled from UHD HLG10 instead of derived from FHD SDR.
        val hlgProfile =
            provider.getAll(QUALITY_1080P)!!.videoProfiles.single {
                isHdrSettingsMatched(it, HLG_10_BIT)
            }
        assertThat(hlgProfile.resolution).isEqualTo(RESOLUTION_1080P)
        assertThat(hlgProfile.bitrate).isEqualTo(10_000_000)
    }

    @Test
    fun createDefaultHdrProfile_whenNoProfileToDeriveFrom() {
        // Arrange: a provider doesn't have any supported qualities.
        val emptyProvider = FakeEncoderProfilesProvider.Builder().build()

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                emptyProvider,
                setOf(FHD),
                setOf(HLG_10_BIT),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert.
        val videoProfile = provider.getAll(QUALITY_1080P)!!.videoProfiles.single()
        assertThat(isHdrSettingsMatched(videoProfile, HLG_10_BIT)).isTrue()
        assertThat(videoProfile.mediaType).isEqualTo(MIMETYPE_VIDEO_HEVC)
        assertThat(videoProfile.profile).isEqualTo(CodecProfileLevel.HEVCProfileMain10)
        assertThat(videoProfile.resolution).isEqualTo(RESOLUTION_1080P)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun deriveDolbyVisionProfileFromSdr() {
        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                providerSupportOnlySdrFhd,
                setOf(FHD),
                setOf(SDR, DOLBY_VISION_10_BIT),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert.
        val dvProfile =
            provider.getAll(QUALITY_1080P)!!.videoProfiles.single {
                isHdrSettingsMatched(it, DOLBY_VISION_10_BIT)
            }
        assertThat(dvProfile.codec).isEqualTo(DOLBY_VISION)
        assertThat(dvProfile.mediaType).isEqualTo(MIMETYPE_VIDEO_DOLBY_VISION)
        assertThat(dvProfile.profile).isEqualTo(CodecProfileLevel.DolbyVisionProfileDvheSt)
    }

    @Test
    fun nonTargetQuality_returnsBaseProfiles() {
        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                providerSupportOnlySdrFhd,
                allQualities,
                setOf(SDR, HLG_10_BIT),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert.
        assertThat(provider.getAll(QUALITY_HIGH))
            .isSameInstanceAs(providerSupportOnlySdrFhd.getAll(QUALITY_HIGH))
    }

    @Test
    fun allTargetDynamicRangesExist_returnsBaseProfiles() {
        // Arrange: FHD has both SDR and HLG10 profiles.
        val profilesFhd =
            createEncoderProfiles(
                createFakeVideoProfileProxy(RESOLUTION_1080P),
                createFakeVideoProfileProxy(
                    RESOLUTION_1080P,
                    videoMediaType = MIMETYPE_VIDEO_HEVC,
                    videoBitDepth = BIT_DEPTH_10,
                    videoHdrFormat = HDR_HLG,
                ),
            )
        val baseProvider =
            FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, profilesFhd).build()

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                baseProvider,
                allQualities,
                setOf(SDR, HLG_10_BIT),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert.
        assertThat(provider.getAll(QUALITY_1080P)).isSameInstanceAs(profilesFhd)
    }

    @Test
    fun canExploreQuality_byMultipleTargetDynamicRanges() {
        // Arrange: create FHD SDR VideoProfile.
        val videoProfileFhdSdr = createFakeVideoProfileProxy(RESOLUTION_1080P)
        // Arrange: create HD HDR10 VideoProfile.
        val videoProfileHdHdr10 =
            createFakeVideoProfileProxy(
                RESOLUTION_720P,
                videoCodec = H263,
                videoMediaType = MIMETYPE_VIDEO_HEVC,
                videoBitDepth = BIT_DEPTH_10,
                videoHdrFormat = HDR_HDR10,
            )
        // Arrange: create SD HLG10 VideoProfile.
        val videoProfileSdHlg10 =
            createFakeVideoProfileProxy(
                RESOLUTION_480P,
                videoCodec = MPEG_4_SP,
                videoMediaType = MIMETYPE_VIDEO_HEVC,
                videoBitDepth = BIT_DEPTH_10,
                videoHdrFormat = HDR_HLG,
            )
        // Arrange: create FHD AudioProfile
        val audioProfileFhd = createFakeAudioProfileProxy()
        // Arrange: create FHD EncoderProfiles.
        val profilesFhd =
            EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
                30,
                THREE_GPP,
                listOf(audioProfileFhd),
                listOf(videoProfileFhdSdr),
            )
        // Arrange: create HD AudioProfile
        val audioProfileHd = createFakeAudioProfileProxy()
        // Arrange: create HD EncoderProfiles.
        val profilesHd =
            EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
                20,
                WEBM,
                listOf(audioProfileHd),
                listOf(videoProfileHdHdr10),
            )
        // Arrange: create SD AudioProfile
        val audioProfileSd = createFakeAudioProfileProxy()
        // Arrange: create SD EncoderProfiles.
        val profilesSd =
            EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
                10,
                MPEG_4,
                listOf(audioProfileSd),
                listOf(videoProfileSdHlg10),
            )
        // Arrange: create EncoderProfileProvider with above EncoderProfiles.
        val baseProvider =
            FakeEncoderProfilesProvider.Builder()
                .add(QUALITY_HIGH, profilesFhd)
                .add(QUALITY_1080P, profilesFhd)
                .add(QUALITY_720P, profilesHd)
                .add(QUALITY_480P, profilesSd)
                .add(QUALITY_LOW, profilesSd)
                .build()

        // Act: explore HLG_10_BIT and HDR10_10_BIT
        val provider =
            QualityExploredEncoderProfilesProvider(
                baseProvider,
                allQualities,
                setOf(HLG_10_BIT, HDR10_10_BIT),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert: all qualities of HLG10 and HDR10 should be explored.
        verifyAllQualitiesAreSupported(provider, HLG_10_BIT)
        verifyAllQualitiesAreSupported(provider, HDR10_10_BIT)
        // Assert: ensure SDR FHD is still supported and other qualities are not explored.
        verifyQualitiesAreSupported(provider, SDR, QUALITY_1080P)
        verifyQualitiesAreNotSupported(provider, SDR, QUALITY_2160P, QUALITY_720P, QUALITY_480P)
        // Assert: UHD profile should derive from the size closest highest profile (HD HDR10).
        val profile = provider.getAll(QUALITY_2160P)!!
        assertThat(profile.defaultDurationSeconds).isEqualTo(20)
        assertThat(profile.recommendedFileFormat).isEqualTo(WEBM)
        assertThat(profile.audioProfiles.single()).isSameInstanceAs(audioProfileHd)
        // Assert: UHD HLG10 video profile should derive from SD HLG10.
        assertThat(profile.videoProfiles.single { isHdrSettingsMatched(it, HLG_10_BIT) }.codec)
            .isEqualTo(MPEG_4_SP)
        // Assert: UHD HDR10 video profile should derive from HD HDR10.
        assertThat(profile.videoProfiles.single { isHdrSettingsMatched(it, HDR10_10_BIT) }.codec)
            .isEqualTo(H263)
    }

    @Test
    fun exploreByMultipleTargetDynamicRanges_noDuplicateProfileAdded() {
        // Arrange: create FHD SDR VideoProfile.
        val videoProfileFhdSdr = createFakeVideoProfileProxy(RESOLUTION_1080P)
        // Arrange: create HD HDR10 VideoProfile.
        val videoProfileHdHdr10 =
            createFakeVideoProfileProxy(
                RESOLUTION_720P,
                videoMediaType = MIMETYPE_VIDEO_HEVC,
                videoBitDepth = BIT_DEPTH_10,
                videoHdrFormat = HDR_HDR10,
            )
        // Arrange: create FHD audio profile.
        val audioProfileFhd = createFakeAudioProfileProxy()
        // Arrange: create FHD EncoderProfiles.
        val profilesFhd =
            EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
                30,
                THREE_GPP,
                listOf(audioProfileFhd),
                listOf(videoProfileFhdSdr),
            )
        // Arrange: create HD audio profile.
        val audioProfileHd = createFakeAudioProfileProxy()
        // Arrange: create HD EncoderProfiles.
        val profilesHd =
            EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
                20,
                WEBM,
                listOf(audioProfileHd),
                listOf(videoProfileHdHdr10),
            )
        // Arrange: create EncoderProfileProvider with above EncoderProfiles.
        val baseProvider =
            FakeEncoderProfilesProvider.Builder()
                .add(QUALITY_HIGH, profilesFhd)
                .add(QUALITY_1080P, profilesFhd)
                .add(QUALITY_720P, profilesHd)
                .add(QUALITY_LOW, profilesHd)
                .build()

        // Act: explore FHD by SDR and HDR10_10_BIT
        val provider =
            QualityExploredEncoderProfilesProvider(
                baseProvider,
                setOf(FHD),
                setOf(SDR, HDR10_10_BIT),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert: FHD HDR10 is explored and no duplicate video profile to be added.
        val encoderProfiles = provider.getAll(QUALITY_1080P)!!
        val videoProfiles = encoderProfiles.videoProfiles
        assertThat(videoProfiles.size).isEqualTo(2)
        assertThat(isHdrSettingsMatched(videoProfiles[0], SDR)).isTrue()
        assertThat(isHdrSettingsMatched(videoProfiles[1], HDR10_10_BIT)).isTrue()
        // Assert: No duplicate audio profile to be added.
        val audioProfiles = encoderProfiles.audioProfiles
        assertThat(audioProfiles.size).isEqualTo(1)
        // Assert: audio profile is from FHD profile.
        assertThat(audioProfiles[0]).isSameInstanceAs(audioProfileFhd)
    }

    @Test
    fun bitrateIsScaled() {
        // Arrange: create a FHD videoProfile with a specific bitrate.
        val baseBitrate = 1000
        val profilesFhd =
            EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
                DEFAULT_DURATION,
                DEFAULT_OUTPUT_FORMAT,
                listOf(createFakeAudioProfileProxy()),
                listOf(createFakeVideoProfileProxy(RESOLUTION_1080P, bitrate = baseBitrate)),
            )
        val baseProvider =
            FakeEncoderProfilesProvider.Builder()
                .add(QUALITY_HIGH, profilesFhd)
                .add(QUALITY_1080P, profilesFhd)
                .add(QUALITY_LOW, profilesFhd)
                .build()

        // Act: explore UHD.
        val provider =
            QualityExploredEncoderProfilesProvider(
                baseProvider,
                setOf(UHD),
                setOf(SDR),
                defaultCameraResolutions,
            ) {
                unlimitedVideoEncoderInfo
            }

        // Assert.
        verifyQualitiesAreSupported(provider, SDR, QUALITY_2160P)
        val profiles = provider.getAll(QUALITY_2160P)!!
        // Expected bitrate = base bitrate * width rational * height rational
        assertThat(profiles.videoProfiles.single().bitrate.toDouble())
            .isWithin(0.001)
            .of(baseBitrate * 3840.0 / 1920.0 * 2160.0 / 1080.0)
    }

    @Test
    fun bitrateIsClamped() {
        // Arrange.
        val bitrateUpperBound = 10
        val videoEncoderInfo =
            FakeVideoEncoderInfo(supportedBitrateRange = Range.create(1, bitrateUpperBound))

        // Act.
        val provider =
            QualityExploredEncoderProfilesProvider(
                providerSupportOnlySdrFhd,
                setOf(UHD),
                setOf(SDR),
                defaultCameraResolutions,
            ) {
                videoEncoderInfo
            }

        // Assert.
        verifyQualitiesAreSupported(provider, SDR, QUALITY_2160P)
        val profiles = provider.getAll(QUALITY_2160P)!!
        assertThat(profiles.videoProfiles.single().bitrate).isEqualTo(bitrateUpperBound)
    }

    private fun verifyAllQualitiesAreSupported(
        provider: EncoderProfilesProvider,
        dynamicRange: DynamicRange,
    ) =
        verifyQualitiesAreSupported(
            provider,
            dynamicRange,
            QUALITY_2160P,
            QUALITY_1080P,
            QUALITY_720P,
            QUALITY_480P,
        )

    private fun verifyQualitiesAreSupported(
        provider: EncoderProfilesProvider,
        dynamicRange: DynamicRange,
        vararg qualities: Int,
    ) {
        for (quality in qualities) {
            assertWithMessage("Verify supported for $quality and $dynamicRange")
                .that(provider.hasMatchedDynamicRangeProfile(quality, dynamicRange))
                .isTrue()
        }
    }

    private fun verifyAllQualitiesAreNotSupported(
        provider: EncoderProfilesProvider,
        dynamicRange: DynamicRange,
    ) =
        verifyQualitiesAreNotSupported(
            provider,
            dynamicRange,
            QUALITY_2160P,
            QUALITY_1080P,
            QUALITY_720P,
            QUALITY_480P,
        )

    private fun verifyQualitiesAreNotSupported(
        provider: EncoderProfilesProvider,
        dynamicRange: DynamicRange,
        vararg qualities: Int,
    ) {
        for (quality in qualities) {
            assertWithMessage("Verify not supported for $quality and $dynamicRange")
                .that(provider.hasMatchedDynamicRangeProfile(quality, dynamicRange))
                .isFalse()
        }
    }

    private fun EncoderProfilesProvider.getMatchedDynamicRangeProfileCount(
        quality: Int,
        dynamicRange: DynamicRange,
    ): Int =
        getAll(quality)?.videoProfiles?.count { videoProfile ->
            isHdrSettingsMatched(videoProfile, dynamicRange)
        } ?: 0

    private fun EncoderProfilesProvider.hasMatchedDynamicRangeProfile(
        quality: Int,
        dynamicRange: DynamicRange,
    ): Boolean = getMatchedDynamicRangeProfileCount(quality, dynamicRange) > 0

    private fun createEncoderProfiles(
        vararg videoProfiles: EncoderProfilesProxy.VideoProfileProxy
    ): EncoderProfilesProxy =
        EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
            DEFAULT_DURATION,
            DEFAULT_OUTPUT_FORMAT,
            listOf(createFakeAudioProfileProxy()),
            videoProfiles.toList(),
        )
}
