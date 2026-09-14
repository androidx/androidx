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

package androidx.camera.video.internal.config

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat
import android.util.Range
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.Timebase
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.testing.impl.EncoderProfilesUtil
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class VideoEncoderConfigDefaultResolverTest {

    companion object {
        private const val DEFAULT_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val UNSUPPORTED_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_SCRAMBLED
        private val TIMEBASE = Timebase.UPTIME
        private const val FRAME_RATE_30_INT = 30
        private const val FRAME_RATE_45_INT = 45
        private val FRAME_RATE_30 = Range(30, 30)
        private val DEFAULT_VIDEO_SPEC = VideoSpec.builder().build()
    }

    @Test
    fun defaultVideoSpecProducesValidSettings_forDifferentSurfaceSizes() {
        val surfaceSizeCif = EncoderProfilesUtil.RESOLUTION_CIF
        val surfaceSize720p = EncoderProfilesUtil.RESOLUTION_720P
        val surfaceSize1080p = EncoderProfilesUtil.RESOLUTION_1080P

        val expectedCaptureFrameRateRange = Range(FRAME_RATE_30_INT, FRAME_RATE_30_INT)

        val configSupplierCif =
            VideoEncoderConfigDefaultResolver(
                DEFAULT_MIME_TYPE,
                TIMEBASE,
                DEFAULT_VIDEO_SPEC,
                surfaceSizeCif,
                DynamicRange.SDR,
                expectedCaptureFrameRateRange,
            )
        val configSupplier720p =
            VideoEncoderConfigDefaultResolver(
                DEFAULT_MIME_TYPE,
                TIMEBASE,
                DEFAULT_VIDEO_SPEC,
                surfaceSize720p,
                DynamicRange.SDR,
                expectedCaptureFrameRateRange,
            )
        val configSupplier1080p =
            VideoEncoderConfigDefaultResolver(
                DEFAULT_MIME_TYPE,
                TIMEBASE,
                DEFAULT_VIDEO_SPEC,
                surfaceSize1080p,
                DynamicRange.SDR,
                expectedCaptureFrameRateRange,
            )

        val configCif = configSupplierCif.get()
        assertThat(configCif.mimeType).isEqualTo(DEFAULT_MIME_TYPE)
        assertThat(configCif.bitrate).isGreaterThan(0)
        assertThat(configCif.resolution).isEqualTo(surfaceSizeCif)
        assertThat(configCif.captureFrameRate).isEqualTo(FRAME_RATE_30_INT)
        assertThat(configCif.encodeFrameRate).isEqualTo(FRAME_RATE_30_INT)

        val config720p = configSupplier720p.get()
        assertThat(config720p.mimeType).isEqualTo(DEFAULT_MIME_TYPE)
        assertThat(config720p.bitrate).isGreaterThan(0)
        assertThat(config720p.resolution).isEqualTo(surfaceSize720p)
        assertThat(config720p.captureFrameRate).isEqualTo(FRAME_RATE_30_INT)
        assertThat(config720p.encodeFrameRate).isEqualTo(FRAME_RATE_30_INT)

        val config1080p = configSupplier1080p.get()
        assertThat(config1080p.mimeType).isEqualTo(DEFAULT_MIME_TYPE)
        assertThat(config1080p.bitrate).isGreaterThan(0)
        assertThat(config1080p.resolution).isEqualTo(surfaceSize1080p)
        assertThat(config1080p.captureFrameRate).isEqualTo(FRAME_RATE_30_INT)
        assertThat(config1080p.encodeFrameRate).isEqualTo(FRAME_RATE_30_INT)
    }

    @Test
    fun frameRateIsDefault_whenNoExpectedRangeProvided() {
        val size = EncoderProfilesUtil.RESOLUTION_1080P

        assertThat(
                VideoEncoderConfigDefaultResolver(
                        DEFAULT_MIME_TYPE,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        size,
                        DynamicRange.SDR,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
                    )
                    .get()
                    .encodeFrameRate
            )
            .isEqualTo(VideoConfigUtil.VIDEO_FRAME_RATE_FIXED_DEFAULT)
    }

    @Test
    fun frameRateIsChosenFromUpperOfExpectedRange_whenProvided() {
        val size = EncoderProfilesUtil.RESOLUTION_1080P

        val expectedCaptureFrameRateRange = Range(FRAME_RATE_30_INT, FRAME_RATE_45_INT)

        // Expected frame rate range takes precedence over VideoSpec
        assertThat(
                VideoEncoderConfigDefaultResolver(
                        DEFAULT_MIME_TYPE,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        size,
                        DynamicRange.SDR,
                        expectedCaptureFrameRateRange,
                    )
                    .get()
                    .encodeFrameRate
            )
            .isEqualTo(expectedCaptureFrameRateRange.upper)
    }

    @Test
    fun avcMimeType_producesNoProfile_forHdrDynamicRange() {
        testMimeAndDynamicRangeResolveToProfile(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            DynamicRange.HLG_10_BIT, // AVC does not support HLG10
            EncoderProfilesProxy.CODEC_PROFILE_NONE,
        )
    }

    @Test
    fun unsupportedDynamicRange_producesNoProfile() {
        testMimeAndDynamicRangeResolveToProfile(
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            DynamicRange.DOLBY_VISION_10_BIT, // Dolby vision not supported by HEVC
            EncoderProfilesProxy.CODEC_PROFILE_NONE,
        )
    }

    @Test
    fun unsupportedMime_producesNoProfile() {
        testMimeAndDynamicRangeResolveToProfile(
            UNSUPPORTED_MIME_TYPE,
            DynamicRange.HLG_10_BIT,
            EncoderProfilesProxy.CODEC_PROFILE_NONE,
        )
    }

    @Test
    fun codecProfileIsChosenFromMimeAndDynamicRange_hevc() {
        val dynamicRangeToExpectedProfiles =
            mapOf(
                DynamicRange.SDR to EncoderProfilesProxy.CODEC_PROFILE_NONE,
                DynamicRange.HLG_10_BIT to CodecProfileLevel.HEVCProfileMain10,
                DynamicRange.HDR10_10_BIT to CodecProfileLevel.HEVCProfileMain10HDR10,
                DynamicRange.HDR10_PLUS_10_BIT to CodecProfileLevel.HEVCProfileMain10HDR10Plus,
            )

        for (entry in dynamicRangeToExpectedProfiles) {
            testMimeAndDynamicRangeResolveToProfile(
                MediaFormat.MIMETYPE_VIDEO_HEVC,
                entry.key,
                entry.value,
            )
        }
    }

    @Test
    fun codecProfileIsChosenFromMimeAndDynamicRange_av1() {
        val dynamicRangeToExpectedProfiles =
            mapOf(
                DynamicRange.SDR to CodecProfileLevel.AV1ProfileMain8,
                DynamicRange.HLG_10_BIT to CodecProfileLevel.AV1ProfileMain10,
                DynamicRange.HDR10_10_BIT to CodecProfileLevel.AV1ProfileMain10HDR10,
                DynamicRange.HDR10_PLUS_10_BIT to CodecProfileLevel.AV1ProfileMain10HDR10Plus,
            )

        for (entry in dynamicRangeToExpectedProfiles) {
            testMimeAndDynamicRangeResolveToProfile(
                MediaFormat.MIMETYPE_VIDEO_AV1,
                entry.key,
                entry.value,
            )
        }
    }

    @Test
    fun codecProfileIsChosenFromMimeAndDynamicRange_vp9() {
        val dynamicRangeToExpectedProfiles =
            mapOf(
                DynamicRange.SDR to CodecProfileLevel.VP9Profile0,
                DynamicRange.HLG_10_BIT to CodecProfileLevel.VP9Profile2,
                DynamicRange.HDR10_10_BIT to CodecProfileLevel.VP9Profile2HDR,
                DynamicRange.HDR10_PLUS_10_BIT to CodecProfileLevel.VP9Profile2HDR10Plus,
            )

        for (entry in dynamicRangeToExpectedProfiles) {
            testMimeAndDynamicRangeResolveToProfile(
                MediaFormat.MIMETYPE_VIDEO_VP9,
                entry.key,
                entry.value,
            )
        }
    }

    @Test
    fun codecProfileIsChosenFromMimeAndDynamicRange_dolbyVision() {
        val dynamicRangeToExpectedProfiles =
            mapOf(
                DynamicRange.DOLBY_VISION_10_BIT to CodecProfileLevel.DolbyVisionProfileDvheSt,
                DynamicRange.DOLBY_VISION_8_BIT to CodecProfileLevel.DolbyVisionProfileDvavSe,
            )

        for (entry in dynamicRangeToExpectedProfiles) {
            testMimeAndDynamicRangeResolveToProfile(
                MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION,
                entry.key,
                entry.value,
            )
        }
    }

    @Test
    fun dataSpaceIsUnspecified_forUnsupportedMime() {
        testMimeAndDynamicRangeResolvesToDataSpace(
            UNSUPPORTED_MIME_TYPE,
            DynamicRange.HLG_10_BIT,
            VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED,
        )
    }

    @Test
    fun dataSpaceIsChosenFromDynamicRange_hevc() {
        val dynamicRangeToExpectedDataSpaces =
            mapOf(
                // For backward compatibility, SDR maps to UNSPECIFIED
                DynamicRange.SDR to VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED,
                DynamicRange.HLG_10_BIT to VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_HLG,
                DynamicRange.HDR10_10_BIT to VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_PQ,
                DynamicRange.HDR10_PLUS_10_BIT to
                    VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_PQ,
            )

        for (entry in dynamicRangeToExpectedDataSpaces) {
            testMimeAndDynamicRangeResolvesToDataSpace(
                MediaFormat.MIMETYPE_VIDEO_HEVC,
                entry.key,
                entry.value,
            )
        }
    }

    @Test
    fun dataSpaceIsChosenFromDynamicRange_av1() {
        val dynamicRangeToExpectedDataSpaces =
            mapOf(
                // For backward compatibility, SDR maps to UNSPECIFIED
                DynamicRange.SDR to VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED,
                DynamicRange.HLG_10_BIT to VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_HLG,
                DynamicRange.HDR10_10_BIT to VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_PQ,
                DynamicRange.HDR10_PLUS_10_BIT to
                    VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_PQ,
            )

        for (entry in dynamicRangeToExpectedDataSpaces) {
            testMimeAndDynamicRangeResolvesToDataSpace(
                MediaFormat.MIMETYPE_VIDEO_AV1,
                entry.key,
                entry.value,
            )
        }
    }

    @Test
    fun dataSpaceIsChosenFromDynamicRange_vp9() {
        val dynamicRangeToExpectedDataSpaces =
            mapOf(
                // For backward compatibility, SDR maps to UNSPECIFIED
                DynamicRange.SDR to VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED,
                DynamicRange.HLG_10_BIT to VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_HLG,
                DynamicRange.HDR10_10_BIT to VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_PQ,
                DynamicRange.HDR10_PLUS_10_BIT to
                    VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_PQ,
            )

        for (entry in dynamicRangeToExpectedDataSpaces) {
            testMimeAndDynamicRangeResolvesToDataSpace(
                MediaFormat.MIMETYPE_VIDEO_VP9,
                entry.key,
                entry.value,
            )
        }
    }

    @Test
    fun dataSpaceIsChosenFromDynamicRange_dolbyVision() {
        val dynamicRangeToExpectedDataSpaces =
            mapOf(
                DynamicRange.DOLBY_VISION_10_BIT to
                    VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_HLG,
                DynamicRange.DOLBY_VISION_8_BIT to VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT709,
            )

        for (entry in dynamicRangeToExpectedDataSpaces) {
            testMimeAndDynamicRangeResolvesToDataSpace(
                MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION,
                entry.key,
                entry.value,
            )
        }
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_vp8() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_VP8,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_720P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(4_000_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_vp8_scaledAt1080p() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_VP8,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_1080P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(9_000_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_vp9() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_VP9,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_720P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(4_000_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_av1() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_AV1,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_720P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(8_000_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_av1_scaledAt1080p() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_AV1,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_1080P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(18_000_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_legacyH263() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_H263,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_720P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(4_000_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_legacyH263_scaledAt480p() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_H263,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_480P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(1_500_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_legacyMpeg4() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_MPEG4,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_720P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(4_000_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_legacyMpeg4_scaledAt480p() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_MPEG4,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_480P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(1_500_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_avc() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_720P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(4_000_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_avc_scaledAt1080p() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_1080P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(9_000_000)
    }

    @Test
    fun bitrateIsScaledBasedOnCodec_hevc() {
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC,
                    inputTimebase = TIMEBASE,
                    videoSpec = DEFAULT_VIDEO_SPEC,
                    surfaceSize = RESOLUTION_720P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(4_000_000)
    }

    @Test
    fun explicitBitrate_isRespected() {
        val videoSpec = VideoSpec.builder().setBitrate(5_000_000).build()
        val config =
            VideoEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_VP8,
                    inputTimebase = TIMEBASE,
                    videoSpec = videoSpec,
                    surfaceSize = RESOLUTION_720P,
                    dynamicRange = DynamicRange.SDR,
                    expectedFrameRateRange = FRAME_RATE_30,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(5_000_000)
    }

    private fun testMimeAndDynamicRangeResolveToProfile(
        mime: String,
        dynamicRange: DynamicRange,
        expectedProfile: Int,
    ) {
        // Expected frame rate range takes precedence over VideoSpec
        assertThat(
                VideoEncoderConfigDefaultResolver(
                        mime,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        EncoderProfilesUtil.RESOLUTION_1080P,
                        dynamicRange,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
                    )
                    .get()
                    .profile
            )
            .isEqualTo(expectedProfile)
    }

    private fun testMimeAndDynamicRangeResolvesToDataSpace(
        mime: String,
        dynamicRange: DynamicRange,
        expectedDataSpace: VideoEncoderDataSpace,
    ) {
        assertThat(
                VideoEncoderConfigDefaultResolver(
                        mime,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        EncoderProfilesUtil.RESOLUTION_1080P,
                        dynamicRange,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
                    )
                    .get()
                    .dataSpace
            )
            .isEqualTo(expectedDataSpace)
    }
}
