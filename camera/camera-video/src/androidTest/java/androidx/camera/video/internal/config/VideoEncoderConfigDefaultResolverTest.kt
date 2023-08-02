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

import android.util.Range
import androidx.camera.testing.CamcorderProfileUtil
import androidx.camera.video.VideoSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class VideoEncoderConfigDefaultResolverTest {

    companion object {
        const val MIME_TYPE = "video/avc"

        const val FRAME_RATE_30 = 30
        const val FRAME_RATE_45 = 45
        const val FRAME_RATE_60 = 60
    }

    private val defaultVideoSpec = VideoSpec.builder().build()

    @Test
    fun defaultVideoSpecProducesValidSettings_forDifferentSurfaceSizes() {
        val surfaceSizeCif = CamcorderProfileUtil.RESOLUTION_CIF
        val surfaceSize720p = CamcorderProfileUtil.RESOLUTION_720P
        val surfaceSize1080p = CamcorderProfileUtil.RESOLUTION_1080P

        val expectedFrameRateRange = Range(FRAME_RATE_30, FRAME_RATE_30)

        val configSupplierCif =
            VideoEncoderConfigDefaultResolver(
                MIME_TYPE,
                defaultVideoSpec,
                surfaceSizeCif,
                expectedFrameRateRange
            )
        val configSupplier720p =
            VideoEncoderConfigDefaultResolver(
                MIME_TYPE,
                defaultVideoSpec,
                surfaceSize720p,
                expectedFrameRateRange
            )
        val configSupplier1080p =
            VideoEncoderConfigDefaultResolver(
                MIME_TYPE,
                defaultVideoSpec,
                surfaceSize1080p,
                expectedFrameRateRange
            )

        val configCif = configSupplierCif.get()
        assertThat(configCif.mimeType).isEqualTo(MIME_TYPE)
        assertThat(configCif.bitrate).isGreaterThan(0)
        assertThat(configCif.resolution).isEqualTo(surfaceSizeCif)
        assertThat(configCif.frameRate).isEqualTo(FRAME_RATE_30)

        val config720p = configSupplier720p.get()
        assertThat(config720p.mimeType).isEqualTo(MIME_TYPE)
        assertThat(config720p.bitrate).isGreaterThan(0)
        assertThat(config720p.resolution).isEqualTo(surfaceSize720p)
        assertThat(config720p.frameRate).isEqualTo(FRAME_RATE_30)

        val config1080p = configSupplier1080p.get()
        assertThat(config1080p.mimeType).isEqualTo(MIME_TYPE)
        assertThat(config1080p.bitrate).isGreaterThan(0)
        assertThat(config1080p.resolution).isEqualTo(surfaceSize1080p)
        assertThat(config1080p.frameRate).isEqualTo(FRAME_RATE_30)
    }

    @Test
    fun bitrateRangeInVideoSpecClampsBitrate() {
        val surfaceSize720p = CamcorderProfileUtil.RESOLUTION_720P

        // Get default bit rate for this size
        val defaultConfig =
            VideoEncoderConfigDefaultResolver(
                MIME_TYPE,
                defaultVideoSpec,
                surfaceSize720p,
                /*expectedFrameRateRange=*/null
            ).get()
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
                MIME_TYPE,
                higherVideoSpec,
                surfaceSize720p,
                /*expectedFrameRateRange=*/null
            ).get().bitrate
        ).isEqualTo(higherBitrate)

        assertThat(
            VideoEncoderConfigDefaultResolver(
                MIME_TYPE,
                lowerVideoSpec,
                surfaceSize720p,
                /*expectedFrameRateRange=*/null
            ).get().bitrate
        ).isEqualTo(lowerBitrate)
    }

    @Test
    fun frameRateIsChosenFromVideoSpec_whenNoExpectedRangeProvided() {
        // Give a VideoSpec with a frame rate higher than 30
        val videoSpec =
            VideoSpec.builder().setFrameRate(Range(FRAME_RATE_60, FRAME_RATE_60)).build()
        val size = CamcorderProfileUtil.RESOLUTION_1080P

        assertThat(
            VideoEncoderConfigDefaultResolver(
                MIME_TYPE,
                videoSpec,
                size,
                /*expectedFrameRateRange=*/null
            ).get().frameRate
        ).isEqualTo(
            FRAME_RATE_60
        )
    }

    @Test
    fun frameRateIsChosenFromExpectedRange_whenNoOverlapWithVideoSpec() {
        // Give a VideoSpec with a frame rate higher than 30
        val videoSpec =
            VideoSpec.builder().setFrameRate(Range(FRAME_RATE_60, FRAME_RATE_60)).build()
        val size = CamcorderProfileUtil.RESOLUTION_1080P

        val expectedFrameRateRange = Range(FRAME_RATE_30, FRAME_RATE_30)

        // Expected frame rate range takes precedence over VideoSpec
        assertThat(
            VideoEncoderConfigDefaultResolver(
                MIME_TYPE,
                videoSpec,
                size,
                expectedFrameRateRange
            ).get().frameRate
        ).isEqualTo(
            FRAME_RATE_30
        )
    }

    @Test
    fun frameRateIsChosenFromOverlapOfExpectedRangeAndVideoSpec() {
        // Give a VideoSpec with a frame rate higher than 30
        val videoSpec =
            VideoSpec.builder().setFrameRate(Range(FRAME_RATE_30, FRAME_RATE_45)).build()
        val size = CamcorderProfileUtil.RESOLUTION_1080P

        val expectedFrameRateRange = Range(FRAME_RATE_30, FRAME_RATE_60)

        val intersection = expectedFrameRateRange.intersect(videoSpec.frameRate)

        // Expected frame rate range takes precedence over VideoSpec
        assertThat(
            VideoEncoderConfigDefaultResolver(
                MIME_TYPE,
                videoSpec,
                size,
                expectedFrameRateRange
            ).get().frameRate
        ).isIn(com.google.common.collect.Range.closed(intersection.lower, intersection.upper))
    }
}