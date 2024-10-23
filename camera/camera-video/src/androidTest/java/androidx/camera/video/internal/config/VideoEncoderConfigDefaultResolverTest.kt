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
import android.os.Build
import android.util.Range
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.Timebase
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.EncoderProfilesUtil
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class VideoEncoderConfigDefaultResolverTest {

    companion object {
        const val DEFAULT_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        const val UNSUPPORTED_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_SCRAMBLED
        val TIMEBASE = Timebase.UPTIME
        const val FRAME_RATE_30 = 30
        const val FRAME_RATE_45 = 45
        val DEFAULT_VIDEO_SPEC: VideoSpec by lazy { VideoSpec.builder().build() }
    }

    @Test
    fun defaultVideoSpecProducesValidSettings_forDifferentSurfaceSizes() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val surfaceSizeCif = EncoderProfilesUtil.RESOLUTION_CIF
        val surfaceSize720p = EncoderProfilesUtil.RESOLUTION_720P
        val surfaceSize1080p = EncoderProfilesUtil.RESOLUTION_1080P

        val expectedFrameRateRange = Range(FRAME_RATE_30, FRAME_RATE_30)

        val configSupplierCif =
            VideoEncoderConfigDefaultResolver(
                DEFAULT_MIME_TYPE,
                TIMEBASE,
                DEFAULT_VIDEO_SPEC,
                surfaceSizeCif,
                DynamicRange.SDR,
                expectedFrameRateRange
            )
        val configSupplier720p =
            VideoEncoderConfigDefaultResolver(
                DEFAULT_MIME_TYPE,
                TIMEBASE,
                DEFAULT_VIDEO_SPEC,
                surfaceSize720p,
                DynamicRange.SDR,
                expectedFrameRateRange
            )
        val configSupplier1080p =
            VideoEncoderConfigDefaultResolver(
                DEFAULT_MIME_TYPE,
                TIMEBASE,
                DEFAULT_VIDEO_SPEC,
                surfaceSize1080p,
                DynamicRange.SDR,
                expectedFrameRateRange
            )

        val configCif = configSupplierCif.get()
        assertThat(configCif.mimeType).isEqualTo(DEFAULT_MIME_TYPE)
        assertThat(configCif.bitrate).isGreaterThan(0)
        assertThat(configCif.resolution).isEqualTo(surfaceSizeCif)
        assertThat(configCif.frameRate).isEqualTo(FRAME_RATE_30)

        val config720p = configSupplier720p.get()
        assertThat(config720p.mimeType).isEqualTo(DEFAULT_MIME_TYPE)
        assertThat(config720p.bitrate).isGreaterThan(0)
        assertThat(config720p.resolution).isEqualTo(surfaceSize720p)
        assertThat(config720p.frameRate).isEqualTo(FRAME_RATE_30)

        val config1080p = configSupplier1080p.get()
        assertThat(config1080p.mimeType).isEqualTo(DEFAULT_MIME_TYPE)
        assertThat(config1080p.bitrate).isGreaterThan(0)
        assertThat(config1080p.resolution).isEqualTo(surfaceSize1080p)
        assertThat(config1080p.frameRate).isEqualTo(FRAME_RATE_30)
    }

    @Test
    fun bitrateRangeInVideoSpecClampsBitrate() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val surfaceSize720p = EncoderProfilesUtil.RESOLUTION_720P

        // Get default bit rate for this size
        val defaultConfig =
            VideoEncoderConfigDefaultResolver(
                    DEFAULT_MIME_TYPE,
                    TIMEBASE,
                    DEFAULT_VIDEO_SPEC,
                    surfaceSize720p,
                    DynamicRange.SDR,
                    SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
                )
                .get()
        val defaultBitrate = defaultConfig.bitrate

        // Create video spec with limit 20% higher than default.
        val higherBitrate = (defaultBitrate * 1.2).toInt()
        val higherVideoSpec =
            VideoSpec.builder().setBitrate(Range(higherBitrate, Int.MAX_VALUE)).build()

        // Create video spec with limit 20% lower than default.
        val lowerBitrate = (defaultBitrate * 0.8).toInt()
        val lowerVideoSpec = VideoSpec.builder().setBitrate(Range(0, lowerBitrate)).build()

        assertThat(
                VideoEncoderConfigDefaultResolver(
                        DEFAULT_MIME_TYPE,
                        TIMEBASE,
                        higherVideoSpec,
                        surfaceSize720p,
                        DynamicRange.SDR,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
                    )
                    .get()
                    .bitrate
            )
            .isEqualTo(higherBitrate)

        assertThat(
                VideoEncoderConfigDefaultResolver(
                        DEFAULT_MIME_TYPE,
                        TIMEBASE,
                        lowerVideoSpec,
                        surfaceSize720p,
                        DynamicRange.SDR,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
                    )
                    .get()
                    .bitrate
            )
            .isEqualTo(lowerBitrate)
    }

    @Test
    fun frameRateIsDefault_whenNoExpectedRangeProvided() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val size = EncoderProfilesUtil.RESOLUTION_1080P

        assertThat(
                VideoEncoderConfigDefaultResolver(
                        DEFAULT_MIME_TYPE,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        size,
                        DynamicRange.SDR,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
                    )
                    .get()
                    .frameRate
            )
            .isEqualTo(VideoEncoderConfigDefaultResolver.VIDEO_FRAME_RATE_FIXED_DEFAULT)
    }

    @Test
    fun frameRateIsChosenFromUpperOfExpectedRange_whenProvided() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val size = EncoderProfilesUtil.RESOLUTION_1080P

        val expectedFrameRateRange = Range(FRAME_RATE_30, FRAME_RATE_45)

        // Expected frame rate range takes precedence over VideoSpec
        assertThat(
                VideoEncoderConfigDefaultResolver(
                        DEFAULT_MIME_TYPE,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        size,
                        DynamicRange.SDR,
                        expectedFrameRateRange
                    )
                    .get()
                    .frameRate
            )
            .isEqualTo(FRAME_RATE_45)
    }

    @Test
    fun avcMimeType_producesNoProfile_forHdrDynamicRange() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        testMimeAndDynamicRangeResolveToProfile(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            DynamicRange.HLG_10_BIT, // AVC does not support HLG10
            EncoderProfilesProxy.CODEC_PROFILE_NONE
        )
    }

    @Test
    fun unsupportedDynamicRange_producesNoProfile() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        testMimeAndDynamicRangeResolveToProfile(
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            DynamicRange.DOLBY_VISION_10_BIT, // Dolby vision not supported by HEVC
            EncoderProfilesProxy.CODEC_PROFILE_NONE
        )
    }

    @Test
    fun unsupportedMime_producesNoProfile() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        testMimeAndDynamicRangeResolveToProfile(
            UNSUPPORTED_MIME_TYPE,
            DynamicRange.HLG_10_BIT,
            EncoderProfilesProxy.CODEC_PROFILE_NONE
        )
    }

    @Test
    fun codecProfileIsChosenFromMimeAndDynamicRange_hevc() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val dynamicRangeToExpectedProfiles =
            mapOf(
                DynamicRange.SDR to CodecProfileLevel.HEVCProfileMain,
                DynamicRange.HLG_10_BIT to CodecProfileLevel.HEVCProfileMain10,
                DynamicRange.HDR10_10_BIT to CodecProfileLevel.HEVCProfileMain10HDR10,
                DynamicRange.HDR10_PLUS_10_BIT to CodecProfileLevel.HEVCProfileMain10HDR10Plus
            )

        for (entry in dynamicRangeToExpectedProfiles) {
            testMimeAndDynamicRangeResolveToProfile(
                MediaFormat.MIMETYPE_VIDEO_HEVC,
                entry.key,
                entry.value
            )
        }
    }

    @Test
    fun codecProfileIsChosenFromMimeAndDynamicRange_av1() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val dynamicRangeToExpectedProfiles =
            mapOf(
                DynamicRange.SDR to CodecProfileLevel.AV1ProfileMain8,
                DynamicRange.HLG_10_BIT to CodecProfileLevel.AV1ProfileMain10,
                DynamicRange.HDR10_10_BIT to CodecProfileLevel.AV1ProfileMain10HDR10,
                DynamicRange.HDR10_PLUS_10_BIT to CodecProfileLevel.AV1ProfileMain10HDR10Plus
            )

        for (entry in dynamicRangeToExpectedProfiles) {
            testMimeAndDynamicRangeResolveToProfile(
                MediaFormat.MIMETYPE_VIDEO_HEVC,
                entry.key,
                entry.value
            )
        }
    }

    @Test
    fun codecProfileIsChosenFromMimeAndDynamicRange_vp9() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val dynamicRangeToExpectedProfiles =
            mapOf(
                DynamicRange.SDR to CodecProfileLevel.VP9Profile0,
                DynamicRange.HLG_10_BIT to CodecProfileLevel.VP9Profile2,
                DynamicRange.HDR10_10_BIT to CodecProfileLevel.VP9Profile2HDR,
                DynamicRange.HDR10_PLUS_10_BIT to CodecProfileLevel.VP9Profile2HDR10Plus
            )

        for (entry in dynamicRangeToExpectedProfiles) {
            testMimeAndDynamicRangeResolveToProfile(
                MediaFormat.MIMETYPE_VIDEO_VP9,
                entry.key,
                entry.value
            )
        }
    }

    @Test
    fun codecProfileIsChosenFromMimeAndDynamicRange_dolbyVision() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val dynamicRangeToExpectedProfiles =
            mapOf(
                DynamicRange.DOLBY_VISION_10_BIT to CodecProfileLevel.DolbyVisionProfileDvheSt,
                DynamicRange.DOLBY_VISION_8_BIT to CodecProfileLevel.DolbyVisionProfileDvavSe,
            )

        for (entry in dynamicRangeToExpectedProfiles) {
            testMimeAndDynamicRangeResolveToProfile(
                MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION,
                entry.key,
                entry.value
            )
        }
    }

    @Test
    fun dataSpaceIsUnspecified_forUnsupportedMime() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        testMimeAndDynamicRangeResolvesToDataSpace(
            UNSUPPORTED_MIME_TYPE,
            DynamicRange.HLG_10_BIT,
            VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED
        )
    }

    @Test
    fun dataSpaceIsChosenFromDynamicRange_hevc() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
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
                entry.value
            )
        }
    }

    @Test
    fun dataSpaceIsChosenFromDynamicRange_av1() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
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
                entry.value
            )
        }
    }

    @Test
    fun dataSpaceIsChosenFromDynamicRange_vp9() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
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
                entry.value
            )
        }
    }

    @Test
    fun dataSpaceIsChosenFromDynamicRange_dolbyVision() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
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
                entry.value
            )
        }
    }

    private fun testMimeAndDynamicRangeResolveToProfile(
        mime: String,
        dynamicRange: DynamicRange,
        expectedProfile: Int
    ) {
        // Expected frame rate range takes precedence over VideoSpec
        assertThat(
                VideoEncoderConfigDefaultResolver(
                        mime,
                        TIMEBASE,
                        DEFAULT_VIDEO_SPEC,
                        EncoderProfilesUtil.RESOLUTION_1080P,
                        dynamicRange,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
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
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
                    )
                    .get()
                    .dataSpace
            )
            .isEqualTo(expectedDataSpace)
    }
}
