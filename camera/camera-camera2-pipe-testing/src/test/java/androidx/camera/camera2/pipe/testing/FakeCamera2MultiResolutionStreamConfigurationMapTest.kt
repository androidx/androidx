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

import android.hardware.camera2.params.MultiResolutionStreamInfo
import android.os.Build
import androidx.camera.camera2.pipe.CameraMultiResolutionStreamConfigurationMap
import androidx.camera.camera2.pipe.StreamFormat
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@Config(minSdk = Build.VERSION_CODES.S)
class FakeCamera2MultiResolutionStreamConfigurationMapTest {
    val configMap =
        FakeCameraMultiResolutionStreamConfigurationMap(
            inputFormats =
                listOf(
                    StreamFormat.JPEG,
                    StreamFormat.RAW10,
                    StreamFormat.YUV_420_888,
                    StreamFormat.JPEG,
                ),
            outputFormats =
                listOf(StreamFormat.JPEG, StreamFormat.YUV_420_888, StreamFormat.YUV_420_888),
            inputMultiResStreamFormatsByFormat =
                mapOf(
                    StreamFormat.JPEG to listOf(MultiResolutionStreamInfo(1024, 768, "1")),
                    StreamFormat.YUV_420_888 to listOf(MultiResolutionStreamInfo(1920, 1080, "1")),
                    StreamFormat.RAW10 to listOf(MultiResolutionStreamInfo(3840, 2160, "1")),
                ),
            outputMultiResStreamFormatsByFormat =
                mapOf(StreamFormat.JPEG to listOf(MultiResolutionStreamInfo(1024, 768, "1"))),
        )

    @Test
    fun getInputFormats_returnsDistinctFormats() {
        assertThat(configMap.getInputFormats())
            .containsExactly(StreamFormat.YUV_420_888, StreamFormat.JPEG, StreamFormat.RAW10)
    }

    @Test
    fun getOutputFormats_returnsDistinctFormats() {
        assertThat(configMap.getOutputFormats())
            .containsExactly(StreamFormat.YUV_420_888, StreamFormat.JPEG)
    }

    @Test
    fun getOutputInfo_returnsCorrespondingInfo() {
        assertThat(configMap.getOutputInfo(StreamFormat.JPEG))
            .isEqualTo(listOf(MultiResolutionStreamInfo(1024, 768, "1")))
        assertThat(configMap.getOutputInfo(StreamFormat.YUV_420_888)).isEmpty()
        assertThat(configMap.getOutputInfo(StreamFormat.RAW10)).isEmpty()
        assertThat(configMap.getOutputInfo(StreamFormat.PRIVATE)).isEmpty()
    }

    @Test
    fun getInputInfo_returnsCorrespondingInfo() {
        assertThat(configMap.getInputInfo(StreamFormat.JPEG))
            .isEqualTo(listOf(MultiResolutionStreamInfo(1024, 768, "1")))
        assertThat(configMap.getInputInfo(StreamFormat.YUV_420_888))
            .isEqualTo(listOf(MultiResolutionStreamInfo(1920, 1080, "1")))
        assertThat(configMap.getInputInfo(StreamFormat.RAW10))
            .isEqualTo(listOf(MultiResolutionStreamInfo(3840, 2160, "1")))
        assertThat(configMap.getInputInfo(StreamFormat.PRIVATE)).isEmpty()
    }

    @Test
    fun unwrapAs_shouldReturnItself() {
        assertThat(configMap.unwrapAs(FakeCameraMultiResolutionStreamConfigurationMap::class))
            .isEqualTo(configMap)
    }

    @Test
    fun unwrapAs_returnNull_for_unsupported_type() {
        assertThat(configMap.unwrapAs(CameraMultiResolutionStreamConfigurationMap::class)).isNull()
    }
}
