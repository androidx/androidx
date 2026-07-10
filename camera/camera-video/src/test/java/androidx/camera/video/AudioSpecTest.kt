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
class AudioSpecTest {
    @Test
    fun newBuilder_containsCorrectDefaults() {
        val audioSpec = AudioSpec.builder().build()

        assertThat(audioSpec.source).isEqualTo(AudioSpec.SOURCE_UNSPECIFIED)
        assertThat(audioSpec.sourceFormat).isEqualTo(AudioSpec.SOURCE_FORMAT_UNSPECIFIED)
        assertThat(audioSpec.bitrate).isEqualTo(AudioSpec.BITRATE_UNSPECIFIED)
        assertThat(audioSpec.channelCount).isEqualTo(AudioSpec.CHANNEL_COUNT_UNSPECIFIED)
        assertThat(audioSpec.sampleRate).isEqualTo(AudioSpec.SAMPLE_RATE_UNSPECIFIED)
        assertThat(audioSpec.mimeType).isEqualTo(MIME_TYPE_UNSPECIFIED)
    }

    @Test
    fun builder_setsCorrectValues() {
        val bitrate = 128_000
        val sourceFormat = AudioSpec.SOURCE_FORMAT_PCM_16BIT
        val source = AudioSpec.SOURCE_CAMCORDER
        val sampleRate = 44100
        val channelCount = AudioSpec.CHANNEL_COUNT_STEREO
        val mimeType = "audio/mp4a-latm"

        val audioSpec =
            AudioSpec.builder()
                .setBitrate(bitrate)
                .setSourceFormat(sourceFormat)
                .setSource(source)
                .setSampleRate(sampleRate)
                .setChannelCount(channelCount)
                .setMimeType(mimeType)
                .build()

        assertThat(audioSpec.bitrate).isEqualTo(bitrate)
        assertThat(audioSpec.sourceFormat).isEqualTo(sourceFormat)
        assertThat(audioSpec.source).isEqualTo(source)
        assertThat(audioSpec.sampleRate).isEqualTo(sampleRate)
        assertThat(audioSpec.channelCount).isEqualTo(channelCount)
        assertThat(audioSpec.mimeType).isEqualTo(mimeType)
    }

    @Test
    fun builderAndConstructor_createEquivalentInstances() {
        val bitrate = 256_000
        val sourceFormat = AudioSpec.SOURCE_FORMAT_PCM_16BIT
        val source = AudioSpec.SOURCE_MIC
        val sampleRate = 48000
        val channelCount = AudioSpec.CHANNEL_COUNT_MONO
        val mimeType = "audio/amr-wb"

        val fromBuilder =
            AudioSpec.builder()
                .setBitrate(bitrate)
                .setSourceFormat(sourceFormat)
                .setSource(source)
                .setSampleRate(sampleRate)
                .setChannelCount(channelCount)
                .setMimeType(mimeType)
                .build()

        val fromConstructor =
            AudioSpec(bitrate, sourceFormat, source, sampleRate, channelCount, mimeType)

        assertThat(fromBuilder).isEqualTo(fromConstructor)
        assertThat(fromBuilder.hashCode()).isEqualTo(fromConstructor.hashCode())
    }
}
