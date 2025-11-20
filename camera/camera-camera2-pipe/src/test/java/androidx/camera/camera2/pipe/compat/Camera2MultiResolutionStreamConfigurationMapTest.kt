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

import android.graphics.ImageFormat
import android.hardware.camera2.params.MultiResolutionStreamConfigurationMap
import android.hardware.camera2.params.MultiResolutionStreamInfo
import android.os.Build
import androidx.camera.camera2.pipe.StreamFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [Camera2MultiResolutionStreamConfigurationMap] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.S)
class Camera2MultiResolutionStreamConfigurationMapTest {
    @get:Rule val mocks: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mockedConfigMap: MultiResolutionStreamConfigurationMap

    @InjectMocks
    private lateinit var camera2MultiResolutionStreamConfigurationMap:
        Camera2MultiResolutionStreamConfigurationMap

    @Test
    fun getOutputFormats_returnsConvertedFormats() {
        val frameworkFormats = intArrayOf(ImageFormat.JPEG, ImageFormat.YUV_420_888)
        whenever(mockedConfigMap.outputFormats) doReturn frameworkFormats
        val expectedFormats = listOf(StreamFormat.JPEG, StreamFormat.YUV_420_888)

        val result = camera2MultiResolutionStreamConfigurationMap.getOutputFormats()

        verify(mockedConfigMap, times(1)).outputFormats
        assertThat(result).containsExactlyElementsIn(expectedFormats)
    }

    @Test
    fun getInputFormats_returnsConvertedFormats() {
        val frameworkFormats = intArrayOf(ImageFormat.PRIVATE, ImageFormat.RAW_SENSOR)
        whenever(mockedConfigMap.inputFormats) doReturn frameworkFormats
        val expectedFormats = listOf(StreamFormat.PRIVATE, StreamFormat.RAW_SENSOR)

        val result = camera2MultiResolutionStreamConfigurationMap.getInputFormats()

        verify(mockedConfigMap, times(1)).inputFormats
        assertThat(result).containsExactlyElementsIn(expectedFormats)
    }

    @Test
    fun getOutputInfo_returnsCollectionFromParams() {
        val format = StreamFormat.JPEG
        val streamInfo1 = MultiResolutionStreamInfo(1920, 1080, "0")
        val streamInfo2 = MultiResolutionStreamInfo(1280, 720, "0")
        val expectedInfo: Collection<MultiResolutionStreamInfo> = listOf(streamInfo1, streamInfo2)
        whenever(mockedConfigMap.getOutputInfo(format.value)) doReturn expectedInfo

        val result = camera2MultiResolutionStreamConfigurationMap.getOutputInfo(format)

        verify(mockedConfigMap, times(1)).getOutputInfo(format.value)
        assertThat(result).containsExactlyElementsIn(expectedInfo)
    }

    @Test
    fun getInputInfo_returnsCollectionFromParams() {
        val format = StreamFormat.PRIVATE
        val streamInfo1 = MultiResolutionStreamInfo(4032, 3024, "1")
        val expectedInfo: Collection<MultiResolutionStreamInfo> = listOf(streamInfo1)
        whenever(mockedConfigMap.getInputInfo(format.value)) doReturn expectedInfo

        val result = camera2MultiResolutionStreamConfigurationMap.getInputInfo(format)

        verify(mockedConfigMap, times(1)).getInputInfo(format.value)
        assertThat(result).containsExactlyElementsIn(expectedInfo)
    }

    @Test
    fun unwrapAs_supportedType_returnsWrappedParams() {
        val unwrapped =
            camera2MultiResolutionStreamConfigurationMap.unwrapAs(
                MultiResolutionStreamConfigurationMap::class
            )

        assertThat(unwrapped).isNotNull()
        assertThat(unwrapped).isSameInstanceAs(mockedConfigMap)
    }

    @Test
    fun unwrapAs_selfType_returnsSelf() {
        val unwrapped =
            camera2MultiResolutionStreamConfigurationMap.unwrapAs(
                Camera2MultiResolutionStreamConfigurationMap::class
            )

        assertThat(unwrapped).isNotNull()
        assertThat(unwrapped).isSameInstanceAs(camera2MultiResolutionStreamConfigurationMap)
    }

    @Test
    fun unwrapAs_unsupportedType_returnsNull() {
        val unwrapped = camera2MultiResolutionStreamConfigurationMap.unwrapAs(String::class)

        assertThat(unwrapped).isNull()
    }
}
