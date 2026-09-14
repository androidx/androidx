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

import android.media.MediaFormat
import android.util.Range
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.Timebase
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.video.VideoSpec
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
        private val TIMEBASE = Timebase.UPTIME
        private val FRAME_RATE_30 = Range(30, 30)
        private val DEFAULT_VIDEO_SPEC = VideoSpec.builder().build()
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
}
