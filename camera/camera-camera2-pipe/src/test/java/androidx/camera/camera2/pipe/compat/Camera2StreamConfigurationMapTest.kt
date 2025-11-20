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

package androidx.camera.camera2.pipe.compat

import Camera2StreamConfigurationMap
import android.graphics.ImageFormat
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Build
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.StreamFormat
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.MethodRule
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [Camera2StreamConfigurationMap] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Camera2StreamConfigurationMapTest {
    @Rule @JvmField val mocks: MethodRule = MockitoJUnit.rule()

    @Mock private lateinit var mockStreamConfigurationMap: StreamConfigurationMap

    @InjectMocks private lateinit var camera2StreamConfigurationMap: Camera2StreamConfigurationMap

    @Test
    fun getOutputFormats() {
        val rawFormats = intArrayOf(ImageFormat.JPEG, ImageFormat.YUV_420_888)
        val expectedFormats =
            listOf(StreamFormat(ImageFormat.JPEG), StreamFormat(ImageFormat.YUV_420_888))
        whenever(mockStreamConfigurationMap.outputFormats) doReturn rawFormats

        val actualFormats = camera2StreamConfigurationMap.getOutputFormats()

        verify(mockStreamConfigurationMap, times(1)).outputFormats
        assertThat(actualFormats).containsExactlyElementsIn(expectedFormats)
    }

    @Test
    fun getValidOutputFormatsForInput() {
        val inputFormat = StreamFormat(ImageFormat.RAW_SENSOR)
        val rawFormats = intArrayOf(ImageFormat.JPEG, ImageFormat.YUV_420_888)
        val expectedFormats =
            listOf(StreamFormat(ImageFormat.JPEG), StreamFormat(ImageFormat.YUV_420_888))
        whenever(
            mockStreamConfigurationMap.getValidOutputFormatsForInput(inputFormat.value)
        ) doReturn rawFormats

        val actualFormats = camera2StreamConfigurationMap.getValidOutputFormatsForInput(inputFormat)

        verify(mockStreamConfigurationMap, times(1))
            .getValidOutputFormatsForInput(inputFormat.value)
        assertThat(actualFormats).containsExactlyElementsIn(expectedFormats)
    }

    @Test
    fun getInputFormats() {
        val rawFormats = intArrayOf(ImageFormat.RAW_SENSOR, ImageFormat.PRIVATE)
        val expectedFormats =
            listOf(StreamFormat(ImageFormat.RAW_SENSOR), StreamFormat(ImageFormat.PRIVATE))
        whenever(mockStreamConfigurationMap.inputFormats) doReturn rawFormats

        val actualFormats = camera2StreamConfigurationMap.getInputFormats()

        verify(mockStreamConfigurationMap, times(1)).inputFormats
        assertThat(actualFormats).containsExactlyElementsIn(expectedFormats)
    }

    @Test
    fun getInputSizes() {
        val format = StreamFormat(ImageFormat.YUV_420_888)
        val expectedSizes = arrayOf(Size(1920, 1080), Size(1280, 720))
        whenever(mockStreamConfigurationMap.getInputSizes(format.value)) doReturn expectedSizes

        val actualSizes = camera2StreamConfigurationMap.getInputSizes(format)

        verify(mockStreamConfigurationMap, times(1)).getInputSizes(format.value)
        assertThat(actualSizes).containsExactlyElementsIn(expectedSizes)
    }

    @Test
    fun isOutputSupportedFor_format() {
        val format = StreamFormat(ImageFormat.JPEG)
        whenever(mockStreamConfigurationMap.isOutputSupportedFor(format.value)) doReturn true

        val isSupported = camera2StreamConfigurationMap.isOutputSupportedFor(format)

        verify(mockStreamConfigurationMap, times(1)).isOutputSupportedFor(format.value)
        assertThat(isSupported).isTrue()
    }

    @Test
    fun isOutputSupportedFor_class() {
        val klass = ImageReader::class.java

        val isSupported = camera2StreamConfigurationMap.isOutputSupportedFor(klass)

        assertThat(isSupported).isTrue()
    }

    @Test
    fun isOutputSupportedFor_surface() {
        val mockSurface: Surface = mock()
        whenever(mockStreamConfigurationMap.isOutputSupportedFor(mockSurface)) doReturn true

        val isSupported = camera2StreamConfigurationMap.isOutputSupportedFor(mockSurface)

        verify(mockStreamConfigurationMap, times(1)).isOutputSupportedFor(mockSurface)
        assertThat(isSupported).isTrue()
    }

    @Test
    fun getOutputSizes_class() {
        val klass = String::class.java
        val expectedSizes = arrayOf(Size(1920, 1080), Size(1280, 720))
        whenever(mockStreamConfigurationMap.getOutputSizes(klass)) doReturn expectedSizes

        val actualSizes = camera2StreamConfigurationMap.getOutputSizes(klass)

        verify(mockStreamConfigurationMap, times(1)).getOutputSizes(klass)
        assertThat(actualSizes).containsExactlyElementsIn(expectedSizes)
    }

    @Test
    fun getOutputSizes_format() {
        val format = StreamFormat(ImageFormat.JPEG)
        val expectedSizes = arrayOf(Size(1920, 1080), Size(1280, 720))
        whenever(mockStreamConfigurationMap.getOutputSizes(format.value)) doReturn expectedSizes

        val actualSizes = camera2StreamConfigurationMap.getOutputSizes(format)

        verify(mockStreamConfigurationMap, times(1)).getOutputSizes(format.value)
        assertThat(actualSizes).containsExactlyElementsIn(expectedSizes)
    }

    @Test
    fun getHighSpeedVideoSizes() {
        val expectedSizes = arrayOf(Size(1920, 1080), Size(1280, 720))
        whenever(mockStreamConfigurationMap.highSpeedVideoSizes) doReturn expectedSizes

        val actualSizes = camera2StreamConfigurationMap.getHighSpeedVideoSizes()

        verify(mockStreamConfigurationMap, times(1)).highSpeedVideoSizes
        assertThat(actualSizes).containsExactlyElementsIn(expectedSizes)
    }

    @Test
    fun getHighSpeedVideoFpsRangesFor() {
        val size = Size(1920, 1080)
        val expectedRanges = arrayOf(Range(30, 30), Range(60, 60))
        whenever(mockStreamConfigurationMap.getHighSpeedVideoFpsRangesFor(size)) doReturn
            expectedRanges

        val actualRanges = camera2StreamConfigurationMap.getHighSpeedVideoFpsRangesFor(size)

        verify(mockStreamConfigurationMap, times(1)).getHighSpeedVideoFpsRangesFor(size)
        assertThat(actualRanges).containsExactlyElementsIn(expectedRanges)
    }

    @Test
    fun getHighSpeedVideoFpsRanges() {
        val expectedRanges = arrayOf(Range(30, 30), Range(60, 60))
        whenever(mockStreamConfigurationMap.highSpeedVideoFpsRanges) doReturn expectedRanges

        val actualRanges = camera2StreamConfigurationMap.getHighSpeedVideoFpsRanges()

        verify(mockStreamConfigurationMap, times(1)).highSpeedVideoFpsRanges
        assertThat(actualRanges).containsExactlyElementsIn(expectedRanges)
    }

    @Test
    fun getHighSpeedVideoSizesFor() {
        val fpsRange = Range(30, 30)
        val expectedSizes = arrayOf(Size(1920, 1080))
        whenever(mockStreamConfigurationMap.getHighSpeedVideoSizesFor(fpsRange)) doReturn
            expectedSizes

        val actualSizes = camera2StreamConfigurationMap.getHighSpeedVideoSizesFor(fpsRange)

        verify(mockStreamConfigurationMap, times(1)).getHighSpeedVideoSizesFor(fpsRange)
        assertThat(actualSizes).containsExactlyElementsIn(expectedSizes)
    }

    @Test
    fun getHighResolutionOutputSizes() {
        val format = StreamFormat(ImageFormat.JPEG)
        val expectedSizes = arrayOf(Size(4000, 3000), Size(8000, 6000))
        whenever(mockStreamConfigurationMap.getHighResolutionOutputSizes(format.value)) doReturn
            expectedSizes

        val actualSizes = camera2StreamConfigurationMap.getHighResolutionOutputSizes(format)

        verify(mockStreamConfigurationMap, times(1)).getHighResolutionOutputSizes(format.value)
        assertThat(actualSizes).containsExactlyElementsIn(expectedSizes)
    }

    @Test
    fun getOutputMinFrameDuration() {
        val format = StreamFormat(ImageFormat.JPEG)
        val size = Size(1920, 1080)
        val expectedDuration: Long = 33333333
        whenever(mockStreamConfigurationMap.getOutputMinFrameDuration(format.value, size)) doReturn
            expectedDuration

        val actualDuration = camera2StreamConfigurationMap.getOutputMinFrameDuration(format, size)

        verify(mockStreamConfigurationMap, times(1)).getOutputMinFrameDuration(format.value, size)
        assertThat(actualDuration).isEqualTo(expectedDuration)
    }

    @Test
    fun getOutputMinFrameDuration_class() {
        val klass = String::class.java
        val size = Size(1920, 1080)
        val expectedDuration: Long = 33333333
        whenever(mockStreamConfigurationMap.getOutputMinFrameDuration(klass, size)) doReturn
            expectedDuration

        val actualDuration = camera2StreamConfigurationMap.getOutputMinFrameDuration(klass, size)

        verify(mockStreamConfigurationMap, times(1)).getOutputMinFrameDuration(klass, size)
        assertThat(actualDuration).isEqualTo(expectedDuration)
    }

    @Test
    fun getOutputStallDuration() {
        val format = StreamFormat(ImageFormat.JPEG)
        val size = Size(1920, 1080)
        val expectedDuration: Long = 100000000
        whenever(mockStreamConfigurationMap.getOutputStallDuration(format.value, size)) doReturn
            expectedDuration

        val actualDuration = camera2StreamConfigurationMap.getOutputStallDuration(format, size)

        verify(mockStreamConfigurationMap, times(1)).getOutputStallDuration(format.value, size)
        assertThat(actualDuration).isEqualTo(expectedDuration)
    }

    @Test
    fun getOutputStallDuration_class() {
        val klass = String::class.java
        val size = Size(1920, 1080)
        val expectedDuration: Long = 100000000
        whenever(mockStreamConfigurationMap.getOutputStallDuration(klass, size)) doReturn
            expectedDuration

        val actualDuration = camera2StreamConfigurationMap.getOutputStallDuration(klass, size)

        verify(mockStreamConfigurationMap, times(1)).getOutputStallDuration(klass, size)
        assertThat(actualDuration).isEqualTo(expectedDuration)
    }

    @Test
    fun unwrapAs_supportedType() {
        val unwrapped = camera2StreamConfigurationMap.unwrapAs(StreamConfigurationMap::class)

        assertThat(unwrapped).isNotNull()
        assertThat(unwrapped).isSameInstanceAs(mockStreamConfigurationMap)
    }

    @Test
    fun unwrapAs_unsupportedType_shouldReturnNull() {
        val unwrapped = camera2StreamConfigurationMap.unwrapAs(KClass::class)

        assertThat(unwrapped).isNull()
    }

    @Test
    fun toString_test() {
        val expected = "StreamConfigurationMap(, , , , )"
        whenever(mockStreamConfigurationMap.toString()) doReturn expected

        val actual = camera2StreamConfigurationMap.toString()
        assertThat(actual).isEqualTo(expected)
    }
}
