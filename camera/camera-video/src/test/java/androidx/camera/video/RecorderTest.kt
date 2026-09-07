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

package androidx.camera.video

import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class RecorderTest {

    private val fakeProcessor1 =
        object : PassthroughAudioProcessor() {
            override fun onAudioBuffer(audioBuffer: ByteBuffer) {}
        }

    private val fakeProcessor2 =
        object : PassthroughAudioProcessor() {
            override fun onAudioBuffer(audioBuffer: ByteBuffer) {}
        }

    @Test
    fun defaultRecorder_hasEmptyAudioProcessors() {
        val recorder = Recorder.Builder().build()
        assertThat(recorder.audioProcessors).isEmpty()
    }

    @Test
    fun setAudioProcessors_setsProcessorsOnRecorder() {
        val processors = listOf(fakeProcessor1, fakeProcessor2)
        val recorder = Recorder.Builder().setAudioProcessors(processors).build()

        assertThat(recorder.audioProcessors)
            .containsExactly(fakeProcessor1, fakeProcessor2)
            .inOrder()
    }

    @Test
    fun setAudioProcessors_defensiveCopy() {
        val mutableList = mutableListOf<AudioProcessor>(fakeProcessor1)
        val recorder = Recorder.Builder().setAudioProcessors(mutableList).build()

        mutableList.add(fakeProcessor2)
        assertThat(recorder.audioProcessors).containsExactly(fakeProcessor1)
    }

    @Test
    fun setAudioProcessors_nullElementThrowsNullPointerException() {
        assertThrows(NullPointerException::class.java) {
            val listWithNull = listOf(fakeProcessor1, null)
            @Suppress("UNCHECKED_CAST")
            Recorder.Builder().setAudioProcessors(listWithNull as List<AudioProcessor>)
        }
    }
}
