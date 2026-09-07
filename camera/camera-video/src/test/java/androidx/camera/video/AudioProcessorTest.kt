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
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class AudioProcessorTest {

    private val testAudioFormat =
        AudioProcessor.AudioFormat(
            sampleRate = 44100,
            channelCount = 2,
            encoding = AudioFormat.ENCODING_PCM_16BIT,
        )

    @Test
    fun emptyBuffer_isDirectAndEmpty() {
        val emptyBuffer = AudioProcessor.EMPTY_BUFFER
        assertThat(emptyBuffer.isDirect).isTrue()
        assertThat(emptyBuffer.capacity()).isEqualTo(0)
        assertThat(emptyBuffer.remaining()).isEqualTo(0)
        assertThat(emptyBuffer.order()).isEqualTo(ByteOrder.nativeOrder())
    }

    @Test
    fun audioFormat_propertiesAndBytesPerFrame() {
        val format16BitStereo = AudioProcessor.AudioFormat(48000, 2)
        assertThat(format16BitStereo.sampleRate).isEqualTo(48000)
        assertThat(format16BitStereo.channelCount).isEqualTo(2)
        assertThat(format16BitStereo.encoding).isEqualTo(AudioFormat.ENCODING_PCM_16BIT)
        assertThat(format16BitStereo.bytesPerFrame).isEqualTo(4)

        val format16BitMono = AudioProcessor.AudioFormat(44100, 1)
        assertThat(format16BitMono.bytesPerFrame).isEqualTo(2)

        val format8Bit = AudioProcessor.AudioFormat(44100, 2, AudioFormat.ENCODING_PCM_8BIT)
        assertThat(format8Bit.bytesPerFrame).isEqualTo(2)

        val format24Bit =
            AudioProcessor.AudioFormat(44100, 2, AudioFormat.ENCODING_PCM_24BIT_PACKED)
        assertThat(format24Bit.bytesPerFrame).isEqualTo(6)

        val format32Bit = AudioProcessor.AudioFormat(44100, 2, AudioFormat.ENCODING_PCM_32BIT)
        assertThat(format32Bit.bytesPerFrame).isEqualTo(8)

        val formatFloat = AudioProcessor.AudioFormat(44100, 2, AudioFormat.ENCODING_PCM_FLOAT)
        assertThat(formatFloat.bytesPerFrame).isEqualTo(8)
    }

    @Test
    fun audioFormat_equalsAndHashCode() {
        val format1 = AudioProcessor.AudioFormat(48000, 2, AudioFormat.ENCODING_PCM_16BIT)
        val format2 = AudioProcessor.AudioFormat(48000, 2, AudioFormat.ENCODING_PCM_16BIT)
        val format3 = AudioProcessor.AudioFormat(44100, 2, AudioFormat.ENCODING_PCM_16BIT)

        assertThat(format1).isEqualTo(format2)
        assertThat(format1.hashCode()).isEqualTo(format2.hashCode())
        assertThat(format1).isNotEqualTo(format3)
        assertThat(format1.toString()).contains("48000")
    }

    @Test
    fun audioFormat_invalidArguments_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioProcessor.AudioFormat(sampleRate = 0, channelCount = 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AudioProcessor.AudioFormat(sampleRate = 48000, channelCount = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AudioProcessor.AudioFormat(sampleRate = 48000, channelCount = 2, encoding = 9999)
        }
    }

    @Test
    fun unsupportedAudioFormatException_storesMessageAndFormat() {
        val message = "Unsupported 96000Hz sample rate"
        val exception = AudioProcessor.UnsupportedAudioFormatException(message, testAudioFormat)

        assertThat(exception.message).isEqualTo(message)
        assertThat(exception.audioFormat).isEqualTo(testAudioFormat)

        val defaultException = AudioProcessor.UnsupportedAudioFormatException(testAudioFormat)
        assertThat(defaultException.message).contains(testAudioFormat.toString())
        assertThat(defaultException.audioFormat).isEqualTo(testAudioFormat)
    }

    @Test
    fun customAudioProcessor_processesAudioData() {
        // A processor that doubles 16-bit PCM values (gain effect)
        val gainProcessor =
            object : AudioProcessor {
                private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

                override fun configure(
                    inputAudioFormat: AudioProcessor.AudioFormat
                ): AudioProcessor.AudioFormat {
                    return inputAudioFormat
                }

                override fun queueInput(inputBuffer: ByteBuffer) {
                    val remaining = inputBuffer.remaining()
                    outputBuffer =
                        ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
                    val shortInput = inputBuffer.asShortBuffer()
                    val shortOutput = outputBuffer.asShortBuffer()
                    while (shortInput.hasRemaining()) {
                        shortOutput.put((shortInput.get() * 2).toShort())
                    }
                    outputBuffer.position(0)
                    outputBuffer.limit(remaining)
                }

                override fun getOutput(): ByteBuffer {
                    val output = outputBuffer
                    outputBuffer = AudioProcessor.EMPTY_BUFFER
                    return output
                }

                override fun flush() {
                    outputBuffer = AudioProcessor.EMPTY_BUFFER
                }

                override fun reset() {
                    outputBuffer = AudioProcessor.EMPTY_BUFFER
                }
            }

        gainProcessor.configure(testAudioFormat)

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input.asShortBuffer().put(shortArrayOf(10, -20))
        input.position(0)
        input.limit(4)

        gainProcessor.queueInput(input)
        val output = gainProcessor.getOutput()

        assertThat(output.remaining()).isEqualTo(4)
        val shortOutput = output.asShortBuffer()
        assertThat(shortOutput.get()).isEqualTo(20)
        assertThat(shortOutput.get()).isEqualTo(-40)

        // Subsequent getOutput() should return EMPTY_BUFFER
        assertThat(gainProcessor.getOutput()).isEqualTo(AudioProcessor.EMPTY_BUFFER)
    }
}
