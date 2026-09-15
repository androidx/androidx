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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class MediaSpecTest {

    @Test
    fun newBuilder_containsCorrectDefaults() {
        val mediaSpec = MediaSpec.builder().build()

        val defaultAudioSpec = AudioSpec.builder().build()
        val defaultVideoSpec = VideoSpec.builder().build()
        assertThat(mediaSpec.audioSpec).isEqualTo(defaultAudioSpec)
        assertThat(mediaSpec.videoSpec).isEqualTo(defaultVideoSpec)
        assertThat(mediaSpec.outputFormat).isEqualTo(MediaSpec.OUTPUT_FORMAT_UNSPECIFIED)
    }

    @Test
    fun canConfigureVideo_fromMediaSpecBuilder() {
        val testFrameRate = 30
        val mediaSpec =
            MediaSpec.builder().configureVideo { it.setEncodeFrameRate(testFrameRate) }.build()

        assertThat(mediaSpec.videoSpec.encodeFrameRate).isEqualTo(testFrameRate)
    }

    @Test
    fun canConfigureAudio_fromMediaSpecBuilder() {
        val mediaSpec =
            MediaSpec.builder()
                .configureAudio { it.setChannelCount(AudioSpec.CHANNEL_COUNT_STEREO) }
                .build()

        assertThat(mediaSpec.audioSpec.channelCount).isEqualTo(AudioSpec.CHANNEL_COUNT_STEREO)
    }
}
