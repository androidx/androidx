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

package androidx.camera.common

import android.graphics.ImageFormat
import android.util.Range
import android.util.Size
import androidx.camera.common.testing.FakeStreamConfigurationMap
import androidx.camera.common.testing.FakeStreamConfigurationMap.InputTableEntry
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputKey
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputValues
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
public class FakeStreamConfigurationMapTest {
    private val streamConfigurationMap =
        FakeStreamConfigurationMap(
            outputsTable =
                linkedMapOf(
                    OutputKey(ImageFormat.PRIVATE, Size(640, 480)) to OutputValues(),
                    OutputKey(ImageFormat.PRIVATE, Size(1920, 1080)) to
                        OutputValues(highSpeedFpsRanges = listOf(Range(60, 240))),
                    OutputKey(ImageFormat.YUV_420_888, Size(640, 480)) to OutputValues(),
                    OutputKey(ImageFormat.YUV_420_888, Size(1920, 1080)) to OutputValues(),
                    OutputKey(ImageFormat.YUV_420_888, Size(3840, 2160)) to OutputValues(),
                    OutputKey(ImageFormat.YUV_420_888, Size(7680, 4320)) to
                        OutputValues(
                            minDuration = 100_000_000L, // 100ms -> High-Res
                            stallDuration = 50_000_000L,
                        ),
                    OutputKey(ImageFormat.JPEG, Size(640, 480)) to OutputValues(),
                    OutputKey(ImageFormat.JPEG, Size(1920, 1080)) to OutputValues(),
                    OutputKey(ImageFormat.JPEG, Size(3840, 2160)) to OutputValues(),
                    OutputKey(ImageFormat.JPEG, Size(7680, 4320)) to
                        OutputValues(
                            minDuration = 100_000_000L // 100ms -> High-Res
                        ),
                    OutputKey(ImageFormat.RAW10, Size(4000, 3000)) to
                        OutputValues(
                            minDuration = 33_333_333L, // 33ms -> Standard
                            stallDuration = 100_000_000L,
                        ),
                ),
            inputTable =
                listOf(
                    InputTableEntry(ImageFormat.YUV_420_888, Size(640, 480)),
                    InputTableEntry(ImageFormat.YUV_420_888, Size(1920, 1080)),
                    InputTableEntry(ImageFormat.YUV_420_888, Size(3840, 2160)),
                    InputTableEntry(ImageFormat.PRIVATE, Size(640, 480)),
                ),
            outputFormatsForInputFormats =
                mapOf(
                    ImageFormat.YUV_420_888 to listOf(ImageFormat.JPEG, ImageFormat.PRIVATE),
                    ImageFormat.PRIVATE to listOf(ImageFormat.PRIVATE),
                ),
        )

    @Test
    public fun getOutputFormats_returnsDistinctFormats() {
        val formats = streamConfigurationMap.getOutputFormats()
        assertThat(formats)
            .containsExactly(
                ImageFormat.PRIVATE,
                ImageFormat.YUV_420_888,
                ImageFormat.JPEG,
                ImageFormat.RAW10,
            )
    }

    @Test
    public fun getValidOutputFormatsForInput_existingInputFormat() {
        val formats = streamConfigurationMap.getValidOutputFormatsForInput(ImageFormat.YUV_420_888)
        assertThat(formats).containsExactly(ImageFormat.JPEG, ImageFormat.PRIVATE)
    }

    @Test
    public fun getValidOutputFormatsForInput_nonExistingInputFormat() {
        val formats = streamConfigurationMap.getValidOutputFormatsForInput(ImageFormat.RAW10)
        assertThat(formats).isEmpty()
    }

    @Test
    public fun getInputFormats_returnsDistinctFormats() {
        val formats = streamConfigurationMap.getInputFormats()
        assertThat(formats).containsExactly(ImageFormat.YUV_420_888, ImageFormat.PRIVATE)
    }

    @Test
    public fun getInputSizes_existingFormat() {
        val sizes = streamConfigurationMap.getInputSizes(ImageFormat.YUV_420_888)
        assertThat(sizes).containsExactly(Size(640, 480), Size(1920, 1080), Size(3840, 2160))
    }

    @Test
    public fun getInputSizes_nonExistingFormat() {
        val sizes = streamConfigurationMap.getInputSizes(ImageFormat.JPEG)
        assertThat(sizes).isEmpty()
    }

    @Test
    public fun isOutputSupportedFor_format_supported() {
        assertThat(streamConfigurationMap.isOutputSupportedFor(ImageFormat.PRIVATE)).isTrue()
        assertThat(streamConfigurationMap.isOutputSupportedFor(ImageFormat.YUV_420_888)).isTrue()
        assertThat(streamConfigurationMap.isOutputSupportedFor(ImageFormat.JPEG)).isTrue()
        assertThat(streamConfigurationMap.isOutputSupportedFor(ImageFormat.RAW10)).isTrue()
    }

    @Test
    public fun isOutputSupportedFor_format_unsupported() {
        assertThat(streamConfigurationMap.isOutputSupportedFor(ImageFormat.RAW12)).isFalse()
    }

    @Test
    public fun getOutputSizes_format_returnsNonHighResSizes() {
        val privateSizes = streamConfigurationMap.getOutputSizes(ImageFormat.PRIVATE)
        assertThat(privateSizes).containsExactly(Size(640, 480), Size(1920, 1080))

        val yuvSizes = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)
        assertThat(yuvSizes).containsExactly(Size(640, 480), Size(1920, 1080), Size(3840, 2160))

        val jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)
        assertThat(jpegSizes).containsExactly(Size(640, 480), Size(1920, 1080), Size(3840, 2160))
    }

    @Test
    public fun getOutputSizes_format_noStandardSizes() {
        val mapWithOnlyHighRes =
            FakeStreamConfigurationMap(
                outputsTable =
                    linkedMapOf(
                        OutputKey(ImageFormat.RAW10, Size(1, 1)) to
                            OutputValues(minDuration = 60_000_000L)
                    )
            )
        assertThat(mapWithOnlyHighRes.getOutputSizes(ImageFormat.RAW10)).isEmpty()
    }

    @Test
    public fun getHighSpeedVideoSizes_returnsCorrectSizes() {
        val sizes = streamConfigurationMap.getHighSpeedVideoSizes()
        assertThat(sizes).containsExactly(Size(1920, 1080))
    }

    @Test
    public fun getHighSpeedVideoFpsRangesFor_existingSize() {
        val ranges = streamConfigurationMap.getHighSpeedVideoFpsRangesFor(Size(1920, 1080))
        assertThat(ranges).containsExactly(Range(60, 240))
    }

    @Test
    public fun getHighSpeedVideoFpsRangesFor_nonExistingSize() {
        val ranges = streamConfigurationMap.getHighSpeedVideoFpsRangesFor(Size(640, 480))
        assertThat(ranges).isEmpty()
    }

    @Test
    public fun getHighSpeedVideoFpsRanges_returnsAllDistinctRanges() {
        val ranges = streamConfigurationMap.getHighSpeedVideoFpsRanges()
        assertThat(ranges).containsExactly(Range(60, 240))
    }

    @Test
    public fun getHighSpeedVideoSizesFor_existingRange() {
        val sizes = streamConfigurationMap.getHighSpeedVideoSizesFor(Range(60, 240))
        assertThat(sizes).containsExactly(Size(1920, 1080))
    }

    @Test
    public fun getHighSpeedVideoSizesFor_nonExistingRange() {
        val sizes = streamConfigurationMap.getHighSpeedVideoSizesFor(Range(30, 30))
        assertThat(sizes).isEmpty()
    }

    @Test
    public fun getHighResolutionOutputSizes_returnsCorrectSizes() {
        val yuvHighRes =
            streamConfigurationMap.getHighResolutionOutputSizes(ImageFormat.YUV_420_888)
        assertThat(yuvHighRes).containsExactly(Size(7680, 4320))

        val jpegHighRes = streamConfigurationMap.getHighResolutionOutputSizes(ImageFormat.JPEG)
        assertThat(jpegHighRes).containsExactly(Size(7680, 4320))

        val privateHighRes =
            streamConfigurationMap.getHighResolutionOutputSizes(ImageFormat.PRIVATE)
        assertThat(privateHighRes).isEmpty()
    }

    @Test
    public fun getOutputMinFrameDuration_existing() {
        val duration =
            streamConfigurationMap.getOutputMinFrameDuration(ImageFormat.RAW10, Size(4000, 3000))
        assertThat(duration).isEqualTo(33_333_333L)
    }

    @Test(expected = NoSuchElementException::class)
    public fun getOutputMinFrameDuration_nonExistingSize_throws() {
        streamConfigurationMap.getOutputMinFrameDuration(ImageFormat.RAW10, Size(1, 1))
    }

    @Test
    public fun getOutputMinFrameDuration_highResSize_succeeds() {
        val duration =
            streamConfigurationMap.getOutputMinFrameDuration(
                ImageFormat.YUV_420_888,
                Size(7680, 4320),
            )
        assertThat(duration).isEqualTo(100_000_000L)
    }

    @Test
    public fun getOutputStallDuration_existing() {
        val duration =
            streamConfigurationMap.getOutputStallDuration(ImageFormat.RAW10, Size(4000, 3000))
        assertThat(duration).isEqualTo(100_000_000L)
    }

    @Test(expected = NoSuchElementException::class)
    public fun getOutputStallDuration_nonExistingFormat_throws() {
        streamConfigurationMap.getOutputStallDuration(ImageFormat.RAW12, Size(4000, 3000))
    }

    @Test
    public fun getOutputStallDuration_highResSize_succeeds() {
        val duration =
            streamConfigurationMap.getOutputStallDuration(ImageFormat.YUV_420_888, Size(7680, 4320))
        assertThat(duration).isEqualTo(50_000_000L)
    }

    @Test
    public fun outputLists_preserveInsertionOrder() {
        val map =
            FakeStreamConfigurationMap(
                outputsTable =
                    linkedMapOf(
                        OutputKey(ImageFormat.JPEG, Size(1920, 1080)) to OutputValues(),
                        OutputKey(ImageFormat.PRIVATE, Size(640, 480)) to OutputValues(),
                        OutputKey(ImageFormat.JPEG, Size(640, 480)) to OutputValues(),
                    )
            )
        assertThat(map.getOutputFormats())
            .containsExactly(ImageFormat.JPEG, ImageFormat.PRIVATE)
            .inOrder()
        assertThat(map.getOutputSizes(ImageFormat.JPEG))
            .containsExactly(Size(1920, 1080), Size(640, 480))
            .inOrder()
    }
}
