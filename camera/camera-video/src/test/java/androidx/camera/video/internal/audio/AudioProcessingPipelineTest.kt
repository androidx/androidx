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

package androidx.camera.video.internal.audio

import android.media.AudioFormat
import android.media.MediaRecorder
import androidx.camera.video.AudioProcessor
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
class AudioProcessingPipelineTest {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 2
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    private val defaultAudioSettings =
        AudioSettings.builder()
            .setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            .setCaptureSampleRate(SAMPLE_RATE)
            .setEncodeSampleRate(SAMPLE_RATE)
            .setChannelCount(CHANNEL_COUNT)
            .setAudioFormat(ENCODING)
            .build()

    @Test
    fun emptyPipeline_isNotOperational() {
        val pipeline = AudioProcessingPipeline(emptyList())

        assertThat(pipeline.isOperational).isFalse()

        val input = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder())
        input.put(ByteArray(16) { 1 })
        input.flip()

        pipeline.queueInput(input)
        assertThat(pipeline.output).isEqualTo(AudioProcessor.EMPTY_BUFFER)
    }

    @Test
    fun singleProcessor_modifiesAudioData() {
        val processor = createAudioProcessor { input ->
            val out = ByteBuffer.allocateDirect(input.remaining()).order(ByteOrder.nativeOrder())
            while (input.hasRemaining()) {
                out.put((input.get() + 1).toByte())
            }
            out.flip()
            out
        }

        val pipeline = AudioProcessingPipeline(listOf(processor))
        assertThat(pipeline.isOperational).isTrue()
        pipeline.configure(defaultAudioSettings)

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input.put(byteArrayOf(10, 20, 30, 40))
        input.flip()

        pipeline.queueInput(input)
        val output = pipeline.output

        assertThat(output.remaining()).isEqualTo(4)
        assertThat(output.get()).isEqualTo(11)
        assertThat(output.get()).isEqualTo(21)
        assertThat(output.get()).isEqualTo(31)
        assertThat(output.get()).isEqualTo(41)
    }

    @Test
    fun multiOutputProcessor_drainsUntilEmpty() {
        // A processor that splits 4 bytes of input into two 2-byte outputs
        val multiOutputProcessor =
            object : AudioProcessor {
                private val pendingOutputs = ArrayDeque<ByteBuffer>()
                private var currentOutput: ByteBuffer = AudioProcessor.EMPTY_BUFFER

                override fun configure(inputAudioFormat: AudioProcessor.AudioFormat) =
                    inputAudioFormat

                override fun queueInput(inputBuffer: ByteBuffer) {
                    if (inputBuffer.remaining() >= 4) {
                        val out1 = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
                        out1.put(inputBuffer.get())
                        out1.put(inputBuffer.get())
                        out1.flip()

                        val out2 = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
                        out2.put(inputBuffer.get())
                        out2.put(inputBuffer.get())
                        out2.flip()

                        pendingOutputs.add(out1)
                        pendingOutputs.add(out2)
                    }
                }

                override fun getOutput(): ByteBuffer {
                    if (!currentOutput.hasRemaining()) {
                        currentOutput =
                            if (pendingOutputs.isNotEmpty()) {
                                pendingOutputs.removeFirst()
                            } else {
                                AudioProcessor.EMPTY_BUFFER
                            }
                    }
                    return currentOutput
                }

                override fun flush() {
                    pendingOutputs.clear()
                    currentOutput = AudioProcessor.EMPTY_BUFFER
                }

                override fun reset() {
                    flush()
                }
            }

        val pipeline = AudioProcessingPipeline(listOf(multiOutputProcessor))
        pipeline.configure(defaultAudioSettings)

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input.put(byteArrayOf(1, 2, 3, 4))
        input.flip()

        pipeline.queueInput(input)

        // 1st getOutput() returns first 2 bytes: [1, 2]
        val output1 = pipeline.output
        assertThat(output1.remaining()).isEqualTo(2)
        assertThat(output1.get()).isEqualTo(1)
        assertThat(output1.get()).isEqualTo(2)

        // 2nd getOutput() without new queueInput drains next 2 bytes: [3, 4]
        val output2 = pipeline.output
        assertThat(output2.remaining()).isEqualTo(2)
        assertThat(output2.get()).isEqualTo(3)
        assertThat(output2.get()).isEqualTo(4)

        // 3rd getOutput() returns EMPTY_BUFFER
        val output3 = pipeline.output
        assertThat(output3).isEqualTo(AudioProcessor.EMPTY_BUFFER)
    }

    @Test
    fun chainedProcessors_propagatesThroughChain() {
        // Processor 1: adds +10 to each byte
        val p1 = createAudioProcessor { input ->
            val out = ByteBuffer.allocateDirect(input.remaining()).order(ByteOrder.nativeOrder())
            while (input.hasRemaining()) {
                out.put((input.get() + 10).toByte())
            }
            out.flip()
            out
        }

        // Processor 2: multiplies each byte by 2
        val p2 = createAudioProcessor { input ->
            val out = ByteBuffer.allocateDirect(input.remaining()).order(ByteOrder.nativeOrder())
            while (input.hasRemaining()) {
                out.put((input.get() * 2).toByte())
            }
            out.flip()
            out
        }

        val pipeline = AudioProcessingPipeline(listOf(p1, p2))
        pipeline.configure(defaultAudioSettings)

        val input = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
        input.put(byteArrayOf(1, 2))
        input.flip()

        pipeline.queueInput(input)
        val output = pipeline.output

        // (1 + 10) * 2 = 22, (2 + 10) * 2 = 24
        assertThat(output.remaining()).isEqualTo(2)
        assertThat(output.get()).isEqualTo(22)
        assertThat(output.get()).isEqualTo(24)
    }

    @Test
    fun chainedProcessors_backpressure_unconsumedOutputHeldInIntermediateBuffer() {
        // P1 produces 4 bytes [1, 2, 3, 4]
        val p1 = createAudioProcessor { input ->
            val out = ByteBuffer.allocateDirect(input.remaining()).order(ByteOrder.nativeOrder())
            while (input.hasRemaining()) {
                out.put(input.get())
            }
            out.flip()
            out
        }

        // P2 only consumes up to 2 bytes per queueInput
        val p2 =
            object : AudioProcessor {
                private var output: ByteBuffer = AudioProcessor.EMPTY_BUFFER

                override fun configure(inputAudioFormat: AudioProcessor.AudioFormat) =
                    inputAudioFormat

                override fun queueInput(inputBuffer: ByteBuffer) {
                    val bytesToRead = minOf(inputBuffer.remaining(), 2)
                    val out = ByteBuffer.allocateDirect(bytesToRead).order(ByteOrder.nativeOrder())
                    repeat(bytesToRead) { out.put(inputBuffer.get()) }
                    out.flip()
                    output = out
                }

                override fun getOutput(): ByteBuffer {
                    val out = output
                    output = AudioProcessor.EMPTY_BUFFER
                    return out
                }

                override fun flush() {
                    output = AudioProcessor.EMPTY_BUFFER
                }

                override fun reset() {
                    output = AudioProcessor.EMPTY_BUFFER
                }
            }

        val pipeline = AudioProcessingPipeline(listOf(p1, p2))
        pipeline.configure(defaultAudioSettings)

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input.put(byteArrayOf(1, 2, 3, 4))
        input.flip()

        pipeline.queueInput(input)

        // First slice: P2 consumed 2 bytes -> output is [1, 2]
        val out1 = pipeline.output
        assertThat(out1.remaining()).isEqualTo(2)
        assertThat(out1.get()).isEqualTo(1)
        assertThat(out1.get()).isEqualTo(2)

        // Second slice: P1's remaining 2 bytes were held in intermediate buffer, now consumed by P2
        val out2 = pipeline.output
        assertThat(out2.remaining()).isEqualTo(2)
        assertThat(out2.get()).isEqualTo(3)
        assertThat(out2.get()).isEqualTo(4)

        // Third slice: completely drained
        assertThat(pipeline.output).isEqualTo(AudioProcessor.EMPTY_BUFFER)
    }

    @Test
    fun flush_clearsIntermediateBuffersAndFlushesProcessors() {
        var p1Flushed = false
        val p1 =
            object : AudioProcessor {
                private var output: ByteBuffer = AudioProcessor.EMPTY_BUFFER

                override fun configure(inputAudioFormat: AudioProcessor.AudioFormat) =
                    inputAudioFormat

                override fun queueInput(inputBuffer: ByteBuffer) {
                    if (inputBuffer.hasRemaining()) {
                        output = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
                        output.put(byteArrayOf(1, 2))
                        output.flip()
                        inputBuffer.position(inputBuffer.limit())
                    }
                }

                override fun getOutput() = output

                override fun flush() {
                    p1Flushed = true
                    output = AudioProcessor.EMPTY_BUFFER
                }

                override fun reset() {
                    flush()
                }
            }

        val pipeline = AudioProcessingPipeline(listOf(p1))
        pipeline.configure(defaultAudioSettings)

        val input = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
        input.put(byteArrayOf(1, 2))
        input.flip()
        pipeline.queueInput(input)

        pipeline.flush()

        assertThat(p1Flushed).isTrue()
        assertThat(pipeline.output).isEqualTo(AudioProcessor.EMPTY_BUFFER)
    }

    @Test
    fun reset_clearsBuffersAndResetsProcessors() {
        var p1Reset = false
        val p1 =
            object : AudioProcessor {
                private var output: ByteBuffer = AudioProcessor.EMPTY_BUFFER

                override fun configure(inputAudioFormat: AudioProcessor.AudioFormat) =
                    inputAudioFormat

                override fun queueInput(inputBuffer: ByteBuffer) {
                    if (inputBuffer.hasRemaining()) {
                        output = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
                        output.put(byteArrayOf(1, 2))
                        output.flip()
                        inputBuffer.position(inputBuffer.limit())
                    }
                }

                override fun getOutput() = output

                override fun flush() {}

                override fun reset() {
                    p1Reset = true
                    output = AudioProcessor.EMPTY_BUFFER
                }
            }

        val pipeline = AudioProcessingPipeline(listOf(p1))
        pipeline.configure(defaultAudioSettings)

        val input = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
        input.put(byteArrayOf(1, 2))
        input.flip()
        pipeline.queueInput(input)

        pipeline.reset()

        assertThat(p1Reset).isTrue()
        assertThat(pipeline.output).isEqualTo(AudioProcessor.EMPTY_BUFFER)
    }

    @Test
    fun configure_terminalNotPcm16_throwsException() {
        val nonPcm16Processor =
            createAudioProcessor(
                onConfigure = {
                    AudioProcessor.AudioFormat(
                        it.sampleRate,
                        it.channelCount,
                        AudioFormat.ENCODING_PCM_FLOAT,
                    )
                }
            )

        val pipeline = AudioProcessingPipeline(listOf(nonPcm16Processor))

        assertThrows(AudioSourceAccessException::class.java) {
            pipeline.configure(defaultAudioSettings)
        }
    }

    @Test
    fun configure_intermediateProcessorFloat_terminalProcessor16Bit_succeeds() {
        val floatProcessor =
            createAudioProcessor(
                onConfigure = { inputFormat ->
                    AudioProcessor.AudioFormat(
                        inputFormat.sampleRate,
                        inputFormat.channelCount,
                        AudioFormat.ENCODING_PCM_FLOAT,
                    )
                }
            )
        val pcm16Processor =
            createAudioProcessor(
                onConfigure = { inputFormat ->
                    AudioProcessor.AudioFormat(
                        inputFormat.sampleRate,
                        inputFormat.channelCount,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                }
            )

        val pipeline = AudioProcessingPipeline(listOf(floatProcessor, pcm16Processor))
        val outputSettings = pipeline.configure(defaultAudioSettings)
        assertThat(outputSettings.audioFormat).isEqualTo(AudioFormat.ENCODING_PCM_16BIT)
    }

    @Test
    fun configure_unsupportedAudioFormatException_throwsException() {
        val failingProcessor =
            createAudioProcessor(
                onConfigure = { audioFormat ->
                    throw AudioProcessor.UnsupportedAudioFormatException(
                        "Unsupported configuration",
                        audioFormat,
                    )
                }
            )

        val pipeline = AudioProcessingPipeline(listOf(failingProcessor))
        assertThrows(AudioSourceAccessException::class.java) {
            pipeline.configure(defaultAudioSettings)
        }
    }

    private fun createAudioProcessor(
        onConfigure: (AudioProcessor.AudioFormat) -> AudioProcessor.AudioFormat = { it },
        processBlock: (ByteBuffer) -> ByteBuffer = { AudioProcessor.EMPTY_BUFFER },
    ): AudioProcessor =
        object : AudioProcessor {
            private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

            override fun configure(inputAudioFormat: AudioProcessor.AudioFormat) =
                onConfigure(inputAudioFormat)

            override fun queueInput(inputBuffer: ByteBuffer) {
                outputBuffer = processBlock(inputBuffer)
            }

            override fun getOutput(): ByteBuffer {
                val out = outputBuffer
                outputBuffer = AudioProcessor.EMPTY_BUFFER
                return out
            }

            override fun flush() {
                outputBuffer = AudioProcessor.EMPTY_BUFFER
            }

            override fun reset() {
                outputBuffer = AudioProcessor.EMPTY_BUFFER
            }
        }
}
