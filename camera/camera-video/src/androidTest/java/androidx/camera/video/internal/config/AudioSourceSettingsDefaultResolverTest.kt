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

import android.media.AudioFormat
import android.media.MediaRecorder
import android.util.Range
import androidx.camera.video.AudioSpec
import androidx.camera.video.internal.AudioSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class AudioSourceSettingsDefaultResolverTest {

    @Test
    fun sampleRateRangeResolvesToSupportedSampleRate() {
        val audioSpecs = listOf(
            AudioSpec.builder().build(),
            AudioSpec.builder().setSampleRate(Range(0, 1000)).build(),
            AudioSpec.builder().setSampleRate(Range(1000, 10000)).build(),
            AudioSpec.builder().setSampleRate(Range(10000, 100000)).build()
        )

        audioSpecs.forEach {
            val audioSettings = AudioSourceSettingsDefaultResolver(it).get()
            assertThat(
                AudioSource.isSettingsSupported(
                    audioSettings.sampleRate,
                    audioSettings.channelCount,
                    audioSettings.audioFormat
                )
            )
        }
    }

    @Test
    fun audioSpecDefaultProducesValidSourceEnum() {
        val audioSpec = AudioSpec.builder().build()
        val resolvedAudioSourceEnum =
            AudioSourceSettingsDefaultResolver(audioSpec).get().audioSource

        assertThat(resolvedAudioSourceEnum).isAnyOf(
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.MIC
        )
    }

    @Test
    fun audioSpecDefaultProducesValidSourceFormat() {
        val audioSpec = AudioSpec.builder().build()
        val resolvedAudioSourceFormat =
            AudioSourceSettingsDefaultResolver(audioSpec).get().audioFormat

        assertThat(resolvedAudioSourceFormat).isNotEqualTo(AudioFormat.ENCODING_INVALID)
    }
}