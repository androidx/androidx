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

package androidx.camera.video

import androidx.camera.core.AspectRatio
import androidx.camera.core.AspectRatio.RATIO_DEFAULT
import androidx.camera.video.MediaConstants.MIME_TYPE_UNSPECIFIED
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class VideoSpecTest {
    @Test
    fun newBuilder_containsCorrectDefaults() {
        val videoSpec = VideoSpec.builder().build()

        assertThat(videoSpec.qualitySelector).isEqualTo(VideoSpec.QUALITY_SELECTOR_UNSPECIFIED)
        assertThat(videoSpec.bitrate).isEqualTo(VideoSpec.BITRATE_UNSPECIFIED)
        assertThat(videoSpec.encodeFrameRate).isEqualTo(VideoSpec.ENCODE_FRAME_RATE_UNSPECIFIED)
        assertThat(videoSpec.aspectRatio).isEqualTo(RATIO_DEFAULT)
        assertThat(videoSpec.mimeType).isEqualTo(MIME_TYPE_UNSPECIFIED)
    }

    @Test
    fun builder_setsCorrectValues() {
        val qualitySelector = QualitySelector.from(Quality.HD)
        val bitrate = 10_000_000
        val frameRate = 30
        val aspectRatio = AspectRatio.RATIO_16_9
        val mimeType = "video/avc"

        val videoSpec =
            VideoSpec.builder()
                .setQualitySelector(qualitySelector)
                .setBitrate(bitrate)
                .setEncodeFrameRate(frameRate)
                .setAspectRatio(aspectRatio)
                .setMimeType(mimeType)
                .build()

        assertThat(videoSpec.qualitySelector).isEqualTo(qualitySelector)
        assertThat(videoSpec.bitrate).isEqualTo(bitrate)
        assertThat(videoSpec.encodeFrameRate).isEqualTo(frameRate)
        assertThat(videoSpec.aspectRatio).isEqualTo(aspectRatio)
        assertThat(videoSpec.mimeType).isEqualTo(mimeType)
    }

    @Test
    fun builderAndConstructor_createEquivalentInstances() {
        val qualitySelector = QualitySelector.from(Quality.SD)
        val frameRate = 24
        val bitrate = 5_000_000
        val aspectRatio = AspectRatio.RATIO_4_3
        val mimeType = "video/hevc"

        val fromBuilder =
            VideoSpec.builder()
                .setQualitySelector(qualitySelector)
                .setEncodeFrameRate(frameRate)
                .setBitrate(bitrate)
                .setAspectRatio(aspectRatio)
                .setMimeType(mimeType)
                .build()

        val fromConstructor = VideoSpec(qualitySelector, frameRate, bitrate, aspectRatio, mimeType)

        assertThat(fromBuilder).isEqualTo(fromConstructor)
        assertThat(fromBuilder.hashCode()).isEqualTo(fromConstructor.hashCode())
    }
}
