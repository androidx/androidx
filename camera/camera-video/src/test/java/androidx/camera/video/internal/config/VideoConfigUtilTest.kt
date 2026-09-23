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

package androidx.camera.video.internal.config

import android.media.EncoderProfiles.VideoProfile.HDR_DOLBY_VISION
import android.media.EncoderProfiles.VideoProfile.HDR_HDR10
import android.media.EncoderProfiles.VideoProfile.HDR_HDR10PLUS
import android.media.EncoderProfiles.VideoProfile.HDR_HLG
import android.media.MediaFormat.MIMETYPE_VIDEO_APV
import android.media.MediaFormat.MIMETYPE_VIDEO_AV1
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION
import android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
import android.media.MediaFormat.MIMETYPE_VIDEO_VP8
import android.media.MediaFormat.MIMETYPE_VIDEO_VP9
import android.media.MediaRecorder.VideoEncoder.DOLBY_VISION
import android.media.MediaRecorder.VideoEncoder.HEVC
import android.media.MediaRecorder.VideoEncoder.VP9
import android.util.Range
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT_SMPTE_2094_50
import androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT
import androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT_SMPTE_2094_50
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT_SMPTE_2094_50
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT_SMPTE_2094_50
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT_SMPTE_2094_50
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.DynamicRange.SDR_SMPTE_2094_50
import androidx.camera.core.SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_8
import androidx.camera.testing.impl.EncoderProfilesUtil
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.MediaConstants.MIME_TYPE_UNSPECIFIED
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy
import androidx.camera.video.internal.config.VideoConfigUtil.VIDEO_FRAME_RATE_FIXED_DEFAULT
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class VideoConfigUtilTest {

    @Test
    fun resolveFrameRates_expectedCaptureFrameRateUnspecified_videoSpecUnspecified() {
        val videoSpec = VideoSpec.builder().build()
        val expectedCaptureFrameRateRange = FRAME_RATE_RANGE_UNSPECIFIED

        val result = VideoConfigUtil.resolveFrameRates(videoSpec, expectedCaptureFrameRateRange)

        assertThat(result.captureRate).isEqualTo(VIDEO_FRAME_RATE_FIXED_DEFAULT)
        assertThat(result.encodeRate).isEqualTo(VIDEO_FRAME_RATE_FIXED_DEFAULT)
    }

    @Test
    fun resolveFrameRates_expectedCaptureFrameRateSpecified_videoSpecUnspecified() {
        val videoSpec = VideoSpec.builder().build()
        val expectedCaptureFrameRateRange = Range(24, 60)

        val result = VideoConfigUtil.resolveFrameRates(videoSpec, expectedCaptureFrameRateRange)

        assertThat(result.captureRate).isEqualTo(60)
        assertThat(result.encodeRate).isEqualTo(60)
    }

    @Test
    fun resolveFrameRates_expectedCaptureFrameRateUnspecified_videoSpecSpecified() {
        val videoSpec = VideoSpec.builder().setEncodeFrameRate(30).build()
        val expectedCaptureFrameRateRange = FRAME_RATE_RANGE_UNSPECIFIED

        val result = VideoConfigUtil.resolveFrameRates(videoSpec, expectedCaptureFrameRateRange)

        assertThat(result.captureRate).isEqualTo(VIDEO_FRAME_RATE_FIXED_DEFAULT)
        assertThat(result.encodeRate).isEqualTo(30)
    }

    @Test
    fun resolveFrameRates_expectedCaptureFrameRateSpecified_videoSpecSpecified() {
        val videoSpec = VideoSpec.builder().setEncodeFrameRate(30).build()
        val expectedCaptureFrameRateRange = Range(24, 60)

        val result = VideoConfigUtil.resolveFrameRates(videoSpec, expectedCaptureFrameRateRange)

        assertThat(result.captureRate).isEqualTo(60)
        assertThat(result.encodeRate).isEqualTo(30)
    }

    @Test
    fun getDynamicRangeDefaultMime_returnsCorrectMime() {
        // SDR
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(SDR)).isEqualTo(MIMETYPE_VIDEO_AVC)
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(SDR_SMPTE_2094_50))
            .isEqualTo(MIMETYPE_VIDEO_AVC)

        // HLG
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(HLG_10_BIT))
            .isEqualTo(MIMETYPE_VIDEO_HEVC)
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(HLG_10_BIT_SMPTE_2094_50))
            .isEqualTo(MIMETYPE_VIDEO_HEVC)

        // HDR10 & HDR10+
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(HDR10_10_BIT))
            .isEqualTo(MIMETYPE_VIDEO_HEVC)
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(HDR10_10_BIT_SMPTE_2094_50))
            .isEqualTo(MIMETYPE_VIDEO_HEVC)
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(HDR10_PLUS_10_BIT))
            .isEqualTo(MIMETYPE_VIDEO_HEVC)
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(HDR10_PLUS_10_BIT_SMPTE_2094_50))
            .isEqualTo(MIMETYPE_VIDEO_HEVC)

        // Dolby Vision
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(DOLBY_VISION_10_BIT))
            .isEqualTo(MIMETYPE_VIDEO_DOLBY_VISION)
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(DOLBY_VISION_10_BIT_SMPTE_2094_50))
            .isEqualTo(MIMETYPE_VIDEO_DOLBY_VISION)
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(DOLBY_VISION_8_BIT))
            .isEqualTo(MIMETYPE_VIDEO_DOLBY_VISION)
        assertThat(VideoConfigUtil.getDynamicRangeDefaultMime(DOLBY_VISION_8_BIT_SMPTE_2094_50))
            .isEqualTo(MIMETYPE_VIDEO_DOLBY_VISION)
    }

    @Test
    fun resolveCompatibleVideoProfile_unspecifiedMime_returnsFirstMatchingDynamicRange() {
        // Arrange: UNSPECIFIED MIME should ignore the media type and find the first compatible
        // profile
        val videoMime = MIME_TYPE_UNSPECIFIED
        val dynamicRange = HLG_10_BIT
        val profiles =
            listOf(
                VIDEO_PROFILE_DEFAULT, // SDR, 8-bit (Incompatible)
                VIDEO_PROFILE_VP9_HLG10, // HLG, 10-bit (Compatible)
                VIDEO_PROFILE_HEVC_HLG10, // HLG, 10-bit (Compatible)
            )

        // Act
        val result =
            VideoConfigUtil.resolveCompatibleVideoProfile(videoMime, dynamicRange, profiles)

        // Assert: Should return the first compatible one (VP9)
        assertThat(result).isEqualTo(VIDEO_PROFILE_VP9_HLG10)
    }

    @Test
    fun resolveCompatibleVideoProfile_specificMime_matchesBothMimeAndDynamicRange() {
        // Arrange
        val expectedProfileMap =
            mapOf(
                SDR to VIDEO_PROFILE_DEFAULT,
                HLG_10_BIT to VIDEO_PROFILE_HEVC_HLG10,
                HDR10_10_BIT to VIDEO_PROFILE_HEVC_HDR10,
                HDR10_PLUS_10_BIT to VIDEO_PROFILE_HEVC_HDR10_PLUS,
                DOLBY_VISION_10_BIT to VIDEO_PROFILE_DOLBY_VISION_10_BIT,
                DOLBY_VISION_8_BIT to VIDEO_PROFILE_DOLBY_VISION_8_BIT,
            )
        val encoderProfiles = createFakeEncoderProfiles(expectedProfileMap.values.toList())

        for ((dynamicRange, expectedVideoProfile) in expectedProfileMap) {

            // Act
            val result =
                VideoConfigUtil.resolveCompatibleVideoProfile(
                    expectedVideoProfile.mediaType,
                    dynamicRange,
                    encoderProfiles.videoProfiles,
                )

            // Assert
            assertThat(result).isEqualTo(expectedVideoProfile)
        }
    }

    @Test
    fun resolveCompatibleVideoProfile_mismatchingDynamicRange_returnsNull() {
        // Arrange: Requesting HLG 10-bit but only SDR or Dolby 8-bit are available
        val videoMime = MIME_TYPE_UNSPECIFIED
        val dynamicRange = HLG_10_BIT
        val profiles =
            listOf(
                VIDEO_PROFILE_DEFAULT, // SDR
                VIDEO_PROFILE_DOLBY_VISION_8_BIT, // Incompatible HDR
            )

        // Act
        val result =
            VideoConfigUtil.resolveCompatibleVideoProfile(videoMime, dynamicRange, profiles)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun resolveCompatibleVideoProfile_mismatchingMime_returnsNull() {
        // Arrange: Requesting VP9 but only DOLBY_VISION is available
        val videoMime = MIMETYPE_VIDEO_VP9
        val dynamicRange = DOLBY_VISION_10_BIT
        val profiles = listOf(VIDEO_PROFILE_DOLBY_VISION_10_BIT)

        // Act
        val result =
            VideoConfigUtil.resolveCompatibleVideoProfile(videoMime, dynamicRange, profiles)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun resolveCompatibleVideoProfile_mismatchingDynamicRangeBitDepth_returnsNull() {
        // Arrange: Requesting DOLBY_VISION 10-bit but only 8-bit is available
        val videoMime = MIMETYPE_VIDEO_DOLBY_VISION
        val dynamicRange = DOLBY_VISION_10_BIT
        val profiles = listOf(VIDEO_PROFILE_DOLBY_VISION_8_BIT)

        // Act
        val result =
            VideoConfigUtil.resolveCompatibleVideoProfile(videoMime, dynamicRange, profiles)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun getSupportedDynamicRanges_sdrOnlyEncoders_neverIncludes10BitHdr() {
        listOf(MIMETYPE_VIDEO_AVC, MIMETYPE_VIDEO_VP8).forEach { mime ->
            val softwareEncoderInfo =
                FakeVideoEncoderInfo(isHardwareAccelerated = false, mime = mime)
            val hardwareEncoderInfo =
                FakeVideoEncoderInfo(isHardwareAccelerated = true, mime = mime)

            val softwareRanges =
                VideoConfigUtil.getSupportedDynamicRanges(mime, softwareEncoderInfo)
            val hardwareRanges =
                VideoConfigUtil.getSupportedDynamicRanges(mime, hardwareEncoderInfo)

            assertThat(softwareRanges).contains(SDR)
            assertThat(hardwareRanges).contains(SDR)
            assertThat(softwareRanges.none { it.bitDepth == DynamicRange.BIT_DEPTH_10_BIT })
                .isTrue()
            assertThat(hardwareRanges.none { it.bitDepth == DynamicRange.BIT_DEPTH_10_BIT })
                .isTrue()
        }
    }

    @Test
    fun getSupportedDynamicRanges_hevcHardware_returnsAllSupportedRanges() {
        val hardwareEncoderInfo =
            FakeVideoEncoderInfo(isHardwareAccelerated = true, mime = MIMETYPE_VIDEO_HEVC)
        val expectedRanges = VideoConfigUtil.getDynamicRangesForMime(MIMETYPE_VIDEO_HEVC)

        val result =
            VideoConfigUtil.getSupportedDynamicRanges(MIMETYPE_VIDEO_HEVC, hardwareEncoderInfo)

        assertThat(result).isEqualTo(expectedRanges)
        assertThat(result).contains(HLG_10_BIT)
        assertThat(result).contains(HDR10_10_BIT)
        assertThat(result).contains(HDR10_PLUS_10_BIT)
    }

    @Test
    fun getSupportedDynamicRanges_hevcSoftware_filtersOut10BitRanges() {
        // All software 10-bit HDR encoders lack hardware acceleration per CDD §5.12 [C-6-2]
        val softwareEncoderInfo =
            FakeVideoEncoderInfo(isHardwareAccelerated = false, mime = MIMETYPE_VIDEO_HEVC)

        val result =
            VideoConfigUtil.getSupportedDynamicRanges(MIMETYPE_VIDEO_HEVC, softwareEncoderInfo)

        // 8-bit dynamic ranges remain supported on software encoders
        assertThat(result).contains(SDR)
        // 10-bit dynamic ranges are strictly filtered out
        assertThat(result.none { it.bitDepth == DynamicRange.BIT_DEPTH_10_BIT }).isTrue()
    }

    @Test
    fun getSupportedDynamicRanges_vp9Hardware_returnsHdr10() {
        val hardwareEncoderInfo =
            FakeVideoEncoderInfo(isHardwareAccelerated = true, mime = MIMETYPE_VIDEO_VP9)
        val expectedRanges = VideoConfigUtil.getDynamicRangesForMime(MIMETYPE_VIDEO_VP9)

        val result =
            VideoConfigUtil.getSupportedDynamicRanges(MIMETYPE_VIDEO_VP9, hardwareEncoderInfo)

        assertThat(result).isEqualTo(expectedRanges)
        assertThat(result).contains(HDR10_10_BIT)
    }

    @Test
    fun getSupportedDynamicRanges_vp9Software_filtersOutHdr10() {
        val softwareEncoderInfo =
            FakeVideoEncoderInfo(isHardwareAccelerated = false, mime = MIMETYPE_VIDEO_VP9)

        val result =
            VideoConfigUtil.getSupportedDynamicRanges(MIMETYPE_VIDEO_VP9, softwareEncoderInfo)

        assertThat(result).contains(SDR)
        assertThat(result.none { it.bitDepth == DynamicRange.BIT_DEPTH_10_BIT }).isTrue()
    }

    @Test
    @Config(minSdk = 34)
    fun getSupportedDynamicRanges_av1Hardware_includes10BitHdr() {
        val hardwareEncoderInfo =
            FakeVideoEncoderInfo(isHardwareAccelerated = true, mime = MIMETYPE_VIDEO_AV1)

        val result =
            VideoConfigUtil.getSupportedDynamicRanges(MIMETYPE_VIDEO_AV1, hardwareEncoderInfo)

        assertThat(result).contains(HLG_10_BIT)
        assertThat(result).contains(SDR)
    }

    @Test
    @Config(minSdk = 34)
    fun getSupportedDynamicRanges_av1Software_filtersOut10BitHdr() {
        val softwareEncoderInfo =
            FakeVideoEncoderInfo(isHardwareAccelerated = false, mime = MIMETYPE_VIDEO_AV1)

        val result =
            VideoConfigUtil.getSupportedDynamicRanges(MIMETYPE_VIDEO_AV1, softwareEncoderInfo)

        assertThat(result).contains(SDR)
        assertThat(result.none { it.bitDepth == DynamicRange.BIT_DEPTH_10_BIT }).isTrue()
    }

    @Test
    @Config(minSdk = 36)
    fun getSupportedDynamicRanges_apvSoftware_returnsEmpty() {
        val softwareEncoderInfo =
            FakeVideoEncoderInfo(isHardwareAccelerated = false, mime = MIMETYPE_VIDEO_APV)

        // APV has no 8-bit SDR profile, and software encoder filters out 10-bit HDR
        assertThat(
                VideoConfigUtil.getSupportedDynamicRanges(MIMETYPE_VIDEO_APV, softwareEncoderInfo)
            )
            .isEmpty()
    }

    @Test
    @Config(minSdk = 36)
    fun getSupportedDynamicRanges_apvHardware_includes10BitHdr() {
        val hardwareEncoderInfo =
            FakeVideoEncoderInfo(isHardwareAccelerated = true, mime = MIMETYPE_VIDEO_APV)

        assertThat(
                VideoConfigUtil.getSupportedDynamicRanges(MIMETYPE_VIDEO_APV, hardwareEncoderInfo)
            )
            .contains(HDR10_PLUS_10_BIT)
    }

    companion object {
        fun createFakeEncoderProfiles(videoProfileProxies: List<VideoProfileProxy>) =
            VideoValidatedEncoderProfilesProxy.create(
                EncoderProfilesUtil.DEFAULT_DURATION,
                EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT,
                emptyList(),
                videoProfileProxies,
            )

        private val DEFAULT_VIDEO_RESOLUTION = Size(1920, 1080)

        val VIDEO_PROFILE_DEFAULT =
            EncoderProfilesUtil.createFakeVideoProfileProxy(DEFAULT_VIDEO_RESOLUTION)

        val VIDEO_PROFILE_HEVC_HLG10 =
            EncoderProfilesUtil.createFakeVideoProfileProxy(
                DEFAULT_VIDEO_RESOLUTION,
                videoCodec = HEVC,
                videoMediaType = MIMETYPE_VIDEO_HEVC,
                videoHdrFormat = HDR_HLG,
                videoBitDepth = BIT_DEPTH_10,
            )

        val VIDEO_PROFILE_HEVC_HDR10 =
            EncoderProfilesUtil.createFakeVideoProfileProxy(
                DEFAULT_VIDEO_RESOLUTION,
                videoCodec = HEVC,
                videoMediaType = MIMETYPE_VIDEO_HEVC,
                videoHdrFormat = HDR_HDR10,
                videoBitDepth = BIT_DEPTH_10,
            )

        val VIDEO_PROFILE_HEVC_HDR10_PLUS =
            EncoderProfilesUtil.createFakeVideoProfileProxy(
                DEFAULT_VIDEO_RESOLUTION,
                videoCodec = HEVC,
                videoMediaType = MIMETYPE_VIDEO_HEVC,
                videoHdrFormat = HDR_HDR10PLUS,
                videoBitDepth = BIT_DEPTH_10,
            )

        val VIDEO_PROFILE_DOLBY_VISION_10_BIT =
            EncoderProfilesUtil.createFakeVideoProfileProxy(
                DEFAULT_VIDEO_RESOLUTION,
                videoCodec = DOLBY_VISION,
                videoMediaType = MIMETYPE_VIDEO_DOLBY_VISION,
                videoHdrFormat = HDR_DOLBY_VISION,
                videoBitDepth = BIT_DEPTH_10,
            )

        val VIDEO_PROFILE_DOLBY_VISION_8_BIT =
            EncoderProfilesUtil.createFakeVideoProfileProxy(
                DEFAULT_VIDEO_RESOLUTION,
                videoCodec = DOLBY_VISION,
                videoMediaType = MIMETYPE_VIDEO_DOLBY_VISION,
                videoHdrFormat = HDR_DOLBY_VISION,
                videoBitDepth = BIT_DEPTH_8,
            )

        val VIDEO_PROFILE_VP9_HLG10 =
            EncoderProfilesUtil.createFakeVideoProfileProxy(
                DEFAULT_VIDEO_RESOLUTION,
                videoCodec = VP9,
                videoMediaType = MIMETYPE_VIDEO_VP9,
                videoHdrFormat = HDR_HLG,
                videoBitDepth = BIT_DEPTH_10,
            )
    }
}
