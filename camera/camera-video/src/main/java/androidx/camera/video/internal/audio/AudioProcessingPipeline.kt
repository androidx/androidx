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
import androidx.camera.core.Logger
import androidx.camera.video.AudioProcessor
import androidx.camera.video.AudioProcessor.Companion.EMPTY_BUFFER
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Pipeline for chaining, managing intermediate buffers, and draining multiple [AudioProcessor]
 * instances.
 *
 * Intermediate buffers are retained between adjacent stages to support backpressure and draining of
 * multi-output processors.
 */
public class AudioProcessingPipeline(audioProcessors: List<AudioProcessor>) {

    private val audioProcessors: List<AudioProcessor> =
        if (audioProcessors.isEmpty()) emptyList() else audioProcessors.toList()

    private val outputBuffers: Array<ByteBuffer> =
        if (audioProcessors.isEmpty()) emptyArray()
        else Array(audioProcessors.size) { EMPTY_BUFFER }

    /** Checks whether the pipeline has active processors. */
    public val isOperational: Boolean
        get() = audioProcessors.isNotEmpty()

    /** Configures all audio processors in sequence and returns the final output [AudioSettings]. */
    @Throws(AudioSourceAccessException::class)
    public fun configure(inputSettings: AudioSettings): AudioSettings {
        if (!isOperational) {
            return inputSettings
        }

        val inputFormat =
            AudioProcessor.AudioFormat(
                inputSettings.captureSampleRate,
                inputSettings.channelCount,
                inputSettings.audioFormat,
            )

        var currentFormat = inputFormat
        for (processor in audioProcessors) {
            try {
                currentFormat = processor.configure(currentFormat)
            } catch (t: Throwable) {
                throw AudioSourceAccessException(
                    "Failed to configure AudioProcessor: ${t.message}",
                    t,
                )
            }
        }
        val outputFormat = currentFormat

        if (outputFormat.encoding != AudioFormat.ENCODING_PCM_16BIT) {
            throw AudioSourceAccessException(
                "The terminal AudioProcessor must output AudioFormat.ENCODING_PCM_16BIT, " +
                    "got: ${outputFormat.encoding}"
            )
        }

        val newCaptureRate = outputFormat.sampleRate
        val newEncodeRate =
            if (inputSettings.captureSampleRate == inputSettings.encodeSampleRate) {
                newCaptureRate
            } else {
                maxOf(
                    1,
                    (newCaptureRate.toFloat() * inputSettings.encodeSampleRate /
                            inputSettings.captureSampleRate)
                        .roundToInt(),
                )
            }

        return inputSettings
            .toBuilder()
            .setCaptureSampleRate(newCaptureRate)
            .setEncodeSampleRate(newEncodeRate)
            .setChannelCount(outputFormat.channelCount)
            .setAudioFormat(outputFormat.encoding)
            .build()
    }

    /** Queues audio data from [inputBuffer] for processing through the pipeline. */
    public fun queueInput(inputBuffer: ByteBuffer) {
        if (!isOperational) {
            return
        }
        processData(inputBuffer)
    }

    /**
     * Returns a [ByteBuffer] containing processed output data from the pipeline.
     *
     * Returns [EMPTY_BUFFER] if no output is available.
     */
    public val output: ByteBuffer
        get() {
            if (!isOperational) {
                return EMPTY_BUFFER
            }
            val finalIndex = finalOutputBufferIndex
            val outputBuffer = outputBuffers[finalIndex]
            if (outputBuffer.hasRemaining()) {
                return outputBuffer
            }

            processData(EMPTY_BUFFER)
            return outputBuffers[finalIndex]
        }

    private fun processData(inputBuffer: ByteBuffer) {
        val finalIndex = finalOutputBufferIndex
        var progressMade = true
        while (progressMade) {
            progressMade = false
            for (index in 0..finalIndex) {
                if (outputBuffers[index].hasRemaining()) {
                    // Processor at this index has output that has not been consumed.
                    // Do not queue input.
                    continue
                }

                val audioProcessor = audioProcessors[index]
                val input =
                    if (index > 0) {
                        outputBuffers[index - 1]
                    } else if (inputBuffer.hasRemaining()) {
                        inputBuffer
                    } else {
                        EMPTY_BUFFER
                    }

                val inputBytes = input.remaining().toLong()
                audioProcessor.queueInput(input)
                outputBuffers[index] = audioProcessor.getOutput()

                progressMade =
                    progressMade or
                        ((inputBytes - input.remaining() > 0) ||
                            outputBuffers[index].hasRemaining())
            }
        }
    }

    /** Flushes all audio processors and clears intermediate output buffers. */
    public fun flush() {
        for (i in outputBuffers.indices) {
            outputBuffers[i] = EMPTY_BUFFER
        }
        for (processor in audioProcessors) {
            try {
                processor.flush()
            } catch (t: Throwable) {
                Logger.w(TAG, "Error flushing AudioProcessor", t)
            }
        }
    }

    /** Resets all audio processors and clears intermediate output buffers. */
    public fun reset() {
        for (i in outputBuffers.indices) {
            outputBuffers[i] = EMPTY_BUFFER
        }
        for (processor in audioProcessors) {
            try {
                processor.reset()
            } catch (t: Throwable) {
                Logger.w(TAG, "Error resetting AudioProcessor", t)
            }
        }
    }

    private val finalOutputBufferIndex: Int
        get() = outputBuffers.size - 1

    private companion object {
        private const val TAG = "AudioProcessingPipeline"
    }
}
