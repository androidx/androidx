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

package androidx.camera.video.internal.config

import android.media.EncoderProfiles
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Range
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.Timebase
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace
import androidx.camera.video.internal.utils.DynamicRangeUtil
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class VideoEncoderConfigVideoProfileResolverTest {

    companion object {
        private const val FRAME_RATE_30 = 30
        private const val FRAME_RATE_45 = 45
        private val TIMEBASE = Timebase.UPTIME
        private val DEFAULT_VIDEO_SPEC = VideoSpec.builder().build()
        private val TEST_DYNAMIC_RANGES =
            setOf(
                DynamicRange.SDR,
                DynamicRange.HLG_10_BIT,
                DynamicRange.HDR10_10_BIT,
                DynamicRange.HDR10_PLUS_10_BIT,
            )
    }

    private fun getFakeVideoProfile(dynamicRange: DynamicRange): VideoProfileProxy {
        val mimeType =
            if (dynamicRange == DynamicRange.SDR) {
                MediaFormat.MIMETYPE_VIDEO_AVC
            } else {
                MediaFormat.MIMETYPE_VIDEO_HEVC
            }
        val codec =
            if (dynamicRange == DynamicRange.SDR) {
                MediaRecorder.VideoEncoder.H264
            } else {
                MediaRecorder.VideoEncoder.HEVC
            }
        val profile =
            DynamicRangeUtil.dynamicRangeToCodecProfileLevelForMime(mimeType, dynamicRange)
        val bitDepth = dynamicRange.bitDepth
        val hdrFormat =
            DynamicRangeUtil.dynamicRangeToVideoProfileHdrFormats(dynamicRange).firstOrNull()
                ?: EncoderProfiles.VideoProfile.HDR_NONE

        return VideoProfileProxy.create(
            codec,
            mimeType,
            10_000_000,
            FRAME_RATE_30,
            RESOLUTION_720P.width,
            RESOLUTION_720P.height,
            profile,
            bitDepth,
            EncoderProfiles.VideoProfile.YUV_420,
            hdrFormat,
        )
    }

    @Test
    fun defaultVideoSpecProducesValidSettings_forSurfaceSizeEquivalentToQuality() {
        TEST_DYNAMIC_RANGES.forEach { dynamicRange ->
            val profile = getFakeVideoProfile(dynamicRange)

            val config =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        profile.resolution,
                        profile,
                        dynamicRange,
                        Range(profile.frameRate, profile.frameRate),
                    )
                    .get()

            assertThat(config.mimeType).isEqualTo(profile.mediaType)
            assertThat(config.bitrate).isEqualTo(profile.bitrate)
            assertThat(config.resolution).isEqualTo(profile.resolution)
            assertThat(config.captureFrameRate).isEqualTo(profile.frameRate)
            assertThat(config.encodeFrameRate).isEqualTo(profile.frameRate)
        }
    }

    @Test
    fun bitrateIncreasesOrDecreasesWithIncreaseOrDecreaseInSurfaceSize() {
        TEST_DYNAMIC_RANGES.forEach { dynamicRange ->
            val profile = getFakeVideoProfile(dynamicRange)
            val surfaceSize = profile.resolution
            val profileFrameRate = Range(profile.frameRate, profile.frameRate)

            val defaultBitrate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        profileFrameRate,
                    )
                    .get()
                    .bitrate

            val increasedSurfaceSize = Size(surfaceSize.width + 100, surfaceSize.height + 100)
            val decreasedSurfaceSize = Size(surfaceSize.width - 100, surfaceSize.height - 100)

            assertThat(
                    VideoEncoderConfigVideoProfileResolver(
                            profile.mediaType,
                            TIMEBASE,
                            DEFAULT_VIDEO_SPEC,
                            increasedSurfaceSize,
                            profile,
                            dynamicRange,
                            profileFrameRate,
                        )
                        .get()
                        .bitrate
                )
                .isGreaterThan(defaultBitrate)

            assertThat(
                    VideoEncoderConfigVideoProfileResolver(
                            profile.mediaType,
                            TIMEBASE,
                            DEFAULT_VIDEO_SPEC,
                            decreasedSurfaceSize,
                            profile,
                            dynamicRange,
                            profileFrameRate,
                        )
                        .get()
                        .bitrate
                )
                .isLessThan(defaultBitrate)
        }
    }

    @Test
    fun frameRateIsDefault_whenNoExpectedRangeProvided() {
        TEST_DYNAMIC_RANGES.forEach { dynamicRange ->
            val profile = getFakeVideoProfile(dynamicRange)
            val surfaceSize = profile.resolution

            assertThat(
                    VideoEncoderConfigVideoProfileResolver(
                            profile.mediaType,
                            TIMEBASE,
                            DEFAULT_VIDEO_SPEC,
                            surfaceSize,
                            profile,
                            dynamicRange,
                            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
                        )
                        .get()
                        .encodeFrameRate
                )
                .isEqualTo(VideoConfigUtil.VIDEO_FRAME_RATE_FIXED_DEFAULT)
        }
    }

    @Test
    fun frameRateIsChosenFromUpperOfExpectedRange_whenProvided() {
        TEST_DYNAMIC_RANGES.forEach { dynamicRange ->
            val profile = getFakeVideoProfile(dynamicRange)
            val surfaceSize = profile.resolution

            val expectedCaptureFrameRateRange = Range(FRAME_RATE_30, FRAME_RATE_45)

            val resolvedFrameRate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        expectedCaptureFrameRateRange,
                    )
                    .get()
                    .encodeFrameRate

            assertThat(resolvedFrameRate).isEqualTo(expectedCaptureFrameRateRange.upper)
        }
    }

    @Test
    fun bitrateScalesWithFrameRateOperatingRange() {
        TEST_DYNAMIC_RANGES.forEach { dynamicRange ->
            val profile = getFakeVideoProfile(dynamicRange)
            val surfaceSize = profile.resolution

            // Construct a range which is constant and half the profile FPS
            val operatingFrameRate = profile.frameRate / 2
            val operatingRange = Range(operatingFrameRate, operatingFrameRate)

            val resolvedBitrate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        operatingRange,
                    )
                    .get()
                    .bitrate

            assertThat(resolvedBitrate)
                .isEqualTo(
                    (profile.bitrate * (operatingFrameRate.toDouble() / profile.frameRate)).toInt()
                )
        }
    }

    @Test
    fun codecProfileLevel_isResolvedFromVideoProfile() {
        TEST_DYNAMIC_RANGES.forEach { dynamicRange ->
            val videoProfile = getFakeVideoProfile(dynamicRange)
            val surfaceSize = videoProfile.resolution

            val resolvedProfile =
                VideoEncoderConfigVideoProfileResolver(
                        videoProfile.mediaType,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        surfaceSize,
                        videoProfile,
                        dynamicRange,
                        Range(videoProfile.frameRate, videoProfile.frameRate),
                    )
                    .get()
                    .profile

            assertThat(resolvedProfile).isEqualTo(videoProfile.profile)
        }
    }

    @Test
    fun supportedHdrDynamicRanges_mapToSpecifiedVideoEncoderDataSpace() {
        TEST_DYNAMIC_RANGES.forEach { dynamicRange ->
            val videoProfile = getFakeVideoProfile(dynamicRange)
            val surfaceSize = videoProfile.resolution

            val resolvedDataSpace =
                VideoEncoderConfigVideoProfileResolver(
                        videoProfile.mediaType,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        surfaceSize,
                        videoProfile,
                        dynamicRange,
                        Range(videoProfile.frameRate, videoProfile.frameRate),
                    )
                    .get()
                    .dataSpace

            // SDR should always map to UNSPECIFIED, while others should not
            if (dynamicRange == DynamicRange.SDR) {
                assertThat(resolvedDataSpace)
                    .isEqualTo(VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED)
            } else {
                assertThat(resolvedDataSpace)
                    .isNotEqualTo(VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED)
            }
        }
    }
}
