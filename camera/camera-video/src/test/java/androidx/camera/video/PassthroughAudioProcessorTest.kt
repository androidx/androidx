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

import android.media.AudioFormat
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class PassthroughAudioProcessorTest {

    private val testAudioFormat =
        AudioProcessor.AudioFormat(
            sampleRate = 44100,
            channelCount = 2,
            encoding = AudioFormat.ENCODING_PCM_16BIT,
        )

    @Test
    fun passthroughAudioProcessor_receivesReadOnlyBufferAndPassesDataThrough() {
        var observedBytes: ByteArray? = null
        var isReadOnly = false

        val passthroughProcessor =
            object : PassthroughAudioProcessor() {
                override fun onAudioBuffer(audioBuffer: ByteBuffer) {
                    isReadOnly = audioBuffer.isReadOnly
                    val bytes = ByteArray(audioBuffer.remaining())
                    audioBuffer.get(bytes)
                    observedBytes = bytes
                }
            }

        val configuredFormat = passthroughProcessor.configure(testAudioFormat)
        assertThat(configuredFormat).isEqualTo(testAudioFormat)

        val testData = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val inputBuffer = ByteBuffer.allocateDirect(testData.size).order(ByteOrder.nativeOrder())
        inputBuffer.put(testData)
        inputBuffer.flip()

        passthroughProcessor.queueInput(inputBuffer)

        // Verify onAudioBuffer was called with read-only buffer and correct data
        assertThat(isReadOnly).isTrue()
        assertThat(observedBytes).isEqualTo(testData)

        // Verify getOutput() returns the original buffer unmodified
        val output = passthroughProcessor.getOutput()
        assertThat(output).isEqualTo(inputBuffer)

        // Subsequent getOutput() returns EMPTY_BUFFER
        assertThat(passthroughProcessor.getOutput()).isEqualTo(AudioProcessor.EMPTY_BUFFER)
    }

    @Test
    fun passthroughAudioProcessor_flushAndReset_clearsOutput() {
        val passthroughProcessor =
            object : PassthroughAudioProcessor() {
                override fun onAudioBuffer(audioBuffer: ByteBuffer) {
                    // no-op
                }
            }

        val testData = byteArrayOf(1, 2, 3, 4)
        val inputBuffer = ByteBuffer.allocateDirect(testData.size).order(ByteOrder.nativeOrder())
        inputBuffer.put(testData)
        inputBuffer.flip()

        passthroughProcessor.queueInput(inputBuffer)
        passthroughProcessor.flush()
        assertThat(passthroughProcessor.getOutput()).isEqualTo(AudioProcessor.EMPTY_BUFFER)

        inputBuffer.position(0)
        passthroughProcessor.queueInput(inputBuffer)
        passthroughProcessor.reset()
        assertThat(passthroughProcessor.getOutput()).isEqualTo(AudioProcessor.EMPTY_BUFFER)
    }
}
