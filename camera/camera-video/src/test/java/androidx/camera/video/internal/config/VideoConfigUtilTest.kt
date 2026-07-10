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
import android.media.MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION
import android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
import android.media.MediaFormat.MIMETYPE_VIDEO_VP9
import android.media.MediaRecorder.VideoEncoder.DOLBY_VISION
import android.media.MediaRecorder.VideoEncoder.HEVC
import android.media.MediaRecorder.VideoEncoder.VP9
import android.util.Range
import android.util.Size
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_8
import androidx.camera.testing.impl.EncoderProfilesUtil
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
