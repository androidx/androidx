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

package androidx.camera.camera2.pipe.testing

import android.util.Range
import android.util.Size
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.FakeCameraStreamConfigurationMap.InputTableEntry
import androidx.camera.camera2.pipe.testing.FakeCameraStreamConfigurationMap.OutputTableEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricCameraPipeTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeCameraStreamConfigurationMapTest {
    private val streamConfigurationMap =
        FakeCameraStreamConfigurationMap(
            outputTable =
                listOf(
                    OutputTableEntry(StreamFormat.PRIVATE, Size(640, 480)),
                    OutputTableEntry(StreamFormat.PRIVATE, Size(1920, 1080)), // Different order
                    OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 1080), // Duplicate size, different params
                        highSpeedFpsRange = Range(60, 240),
                    ),
                    OutputTableEntry(StreamFormat.YUV_420_888, Size(640, 480)),
                    OutputTableEntry(StreamFormat.YUV_420_888, Size(1920, 1080)),
                    OutputTableEntry(StreamFormat.YUV_420_888, Size(3840, 2160)),
                    OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(7680, 4320),
                        isHighRes = true,
                        minDuration = 100,
                        stallDuration = 50,
                    ),
                    OutputTableEntry(StreamFormat.JPEG, Size(640, 480)),
                    OutputTableEntry(StreamFormat.JPEG, Size(1920, 1080)),
                    OutputTableEntry(StreamFormat.JPEG, Size(3840, 2160)),
                    OutputTableEntry(StreamFormat.JPEG, Size(7680, 4320), isHighRes = true),
                    OutputTableEntry(
                        StreamFormat.RAW10,
                        Size(4000, 3000),
                        minDuration = 33,
                        stallDuration = 100,
                    ),
                ),
            inputTable =
                listOf(
                    InputTableEntry(StreamFormat.YUV_420_888, Size(640, 480)),
                    InputTableEntry(StreamFormat.YUV_420_888, Size(1920, 1080)),
                    InputTableEntry(StreamFormat.YUV_420_888, Size(3840, 2160)),
                    InputTableEntry(StreamFormat.PRIVATE, Size(640, 480)),
                ),
            outputFormatsForInputFormats =
                mapOf(
                    StreamFormat.YUV_420_888 to listOf(StreamFormat.JPEG, StreamFormat.PRIVATE),
                    StreamFormat.PRIVATE to listOf(StreamFormat.PRIVATE),
                ),
        )

    @Test
    fun getOutputFormats_returnsDistinctFormats() {
        val formats = streamConfigurationMap.getOutputFormats()
        assertThat(formats)
            .containsExactly(
                StreamFormat.PRIVATE,
                StreamFormat.YUV_420_888,
                StreamFormat.JPEG,
                StreamFormat.RAW10,
            )
    }

    @Test
    fun getValidOutputFormatsForInput_existingInputFormat() {
        val formats = streamConfigurationMap.getValidOutputFormatsForInput(StreamFormat.YUV_420_888)
        assertThat(formats).containsExactly(StreamFormat.JPEG, StreamFormat.PRIVATE)
    }

    @Test
    fun getValidOutputFormatsForInput_nonExistingInputFormat() {
        val formats = streamConfigurationMap.getValidOutputFormatsForInput(StreamFormat.RAW10)
        assertThat(formats).isEmpty()
    }

    @Test
    fun getInputFormats_returnsDistinctFormats() {
        val formats = streamConfigurationMap.getInputFormats()
        assertThat(formats).containsExactly(StreamFormat.YUV_420_888, StreamFormat.PRIVATE)
    }

    @Test
    fun getInputSizes_existingFormat() {
        val sizes = streamConfigurationMap.getInputSizes(StreamFormat.YUV_420_888)
        assertThat(sizes).containsExactly(Size(640, 480), Size(1920, 1080), Size(3840, 2160))
    }

    @Test
    fun getInputSizes_nonExistingFormat() {
        val sizes = streamConfigurationMap.getInputSizes(StreamFormat.JPEG)
        assertThat(sizes).isEmpty()
    }

    @Test
    fun isOutputSupportedFor_format_supported() {
        assertThat(streamConfigurationMap.isOutputSupportedFor(StreamFormat.PRIVATE)).isTrue()
        assertThat(streamConfigurationMap.isOutputSupportedFor(StreamFormat.YUV_420_888)).isTrue()
        assertThat(streamConfigurationMap.isOutputSupportedFor(StreamFormat.JPEG)).isTrue()
        assertThat(streamConfigurationMap.isOutputSupportedFor(StreamFormat.RAW10)).isTrue()
    }

    @Test
    fun isOutputSupportedFor_format_unsupported() {
        assertThat(streamConfigurationMap.isOutputSupportedFor(StreamFormat.RAW12)).isFalse()
    }

    @Test
    fun getOutputSizes_format_returnsNonHighResSizes() {
        val privateSizes = streamConfigurationMap.getOutputSizes(StreamFormat.PRIVATE)
        assertThat(privateSizes).containsExactly(Size(640, 480), Size(1920, 1080))

        val yuvSizes = streamConfigurationMap.getOutputSizes(StreamFormat.YUV_420_888)
        assertThat(yuvSizes).containsExactly(Size(640, 480), Size(1920, 1080), Size(3840, 2160))

        val jpegSizes = streamConfigurationMap.getOutputSizes(StreamFormat.JPEG)
        assertThat(jpegSizes).containsExactly(Size(640, 480), Size(1920, 1080), Size(3840, 2160))
    }

    @Test
    fun getOutputSizes_format_noStandardSizes() {
        // Example: Add a format with only high-res sizes
        val mapWithOnlyHighRes =
            FakeCameraStreamConfigurationMap(
                outputTable =
                    listOf(OutputTableEntry(StreamFormat.RAW10, Size(1, 1), isHighRes = true))
            )
        assertThat(mapWithOnlyHighRes.getOutputSizes(StreamFormat.RAW10)).isEmpty()
    }

    @Test
    fun getHighSpeedVideoSizes_returnsCorrectSizes() {
        val sizes = streamConfigurationMap.getHighSpeedVideoSizes()
        assertThat(sizes).containsExactly(Size(1920, 1080))
    }

    @Test
    fun getHighSpeedVideoFpsRangesFor_existingSize() {
        val ranges = streamConfigurationMap.getHighSpeedVideoFpsRangesFor(Size(1920, 1080))
        assertThat(ranges).containsExactly(Range(60, 240))
    }

    @Test
    fun getHighSpeedVideoFpsRangesFor_nonExistingSize() {
        val ranges = streamConfigurationMap.getHighSpeedVideoFpsRangesFor(Size(640, 480))
        assertThat(ranges).isEmpty()
    }

    @Test
    fun getHighSpeedVideoFpsRanges_returnsAllDistinctRanges() {
        val ranges = streamConfigurationMap.getHighSpeedVideoFpsRanges()
        assertThat(ranges).containsExactly(Range(60, 240))
    }

    @Test
    fun getHighSpeedVideoSizesFor_existingRange() {
        val sizes = streamConfigurationMap.getHighSpeedVideoSizesFor(Range(60, 240))
        assertThat(sizes).containsExactly(Size(1920, 1080))
    }

    @Test
    fun getHighSpeedVideoSizesFor_nonExistingRange() {
        val sizes = streamConfigurationMap.getHighSpeedVideoSizesFor(Range(30, 30))
        assertThat(sizes).isEmpty()
    }

    @Test
    fun getHighResolutionOutputSizes_returnsCorrectSizes() {
        val yuvHighRes =
            streamConfigurationMap.getHighResolutionOutputSizes(StreamFormat.YUV_420_888)
        assertThat(yuvHighRes).containsExactly(Size(7680, 4320))

        val jpegHighRes = streamConfigurationMap.getHighResolutionOutputSizes(StreamFormat.JPEG)
        assertThat(jpegHighRes).containsExactly(Size(7680, 4320))

        val privateHighRes =
            streamConfigurationMap.getHighResolutionOutputSizes(StreamFormat.PRIVATE)
        assertThat(privateHighRes).isEmpty()
    }

    @Test
    fun getOutputMinFrameDuration_existing() {
        val duration =
            streamConfigurationMap.getOutputMinFrameDuration(StreamFormat.RAW10, Size(4000, 3000))
        assertThat(duration).isEqualTo(33)
    }

    @Test(expected = NoSuchElementException::class)
    fun getOutputMinFrameDuration_nonExistingSize_throws() {
        streamConfigurationMap.getOutputMinFrameDuration(StreamFormat.RAW10, Size(1, 1))
    }

    @Test(expected = NoSuchElementException::class)
    fun getOutputMinFrameDuration_highResSize_throws() {
        streamConfigurationMap.getOutputMinFrameDuration(StreamFormat.YUV_420_888, Size(7680, 4320))
    }

    @Test
    fun getOutputStallDuration_existing() {
        val duration =
            streamConfigurationMap.getOutputStallDuration(StreamFormat.RAW10, Size(4000, 3000))
        assertThat(duration).isEqualTo(100)
    }

    @Test(expected = NoSuchElementException::class)
    fun getOutputStallDuration_nonExistingFormat_throws() {
        streamConfigurationMap.getOutputStallDuration(StreamFormat.RAW12, Size(4000, 3000))
    }

    @Test(expected = NoSuchElementException::class)
    fun getOutputStallDuration_highResSize_throws() {
        streamConfigurationMap.getOutputStallDuration(StreamFormat.YUV_420_888, Size(7680, 4320))
    }
}
