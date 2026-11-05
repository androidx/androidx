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

import androidx.camera.video.internal.audio.EMPTY_AUDIO_BUFFER
import java.nio.ByteBuffer

/**
 * Forwards audio buffers unmodified while enabling real-time audio inspection.
 *
 * Subclass this processor to inspect, visualize, or analyze recording audio without altering the
 * recorded audio track. Typical use cases:
 * * **Audio metering**: Calculate audio level or RMS amplitude.
 * * **Waveform display**: Render real-time audio waveforms.
 * * **Audio classification**: Run asynchronous ML inference (e.g. sound event detection).
 *
 * Subclasses only need to override [onAudioBuffer] to receive audio data. Buffer forwarding, format
 * passthrough, flushing, and resetting are handled automatically. To inspect stream parameters
 * (such as sample rate or channel count) or reject unsupported formats, override [onConfigure]. To
 * clear inspection state when the audio stream is interrupted or released, override [onFlush] or
 * [onReset]. To modify or filter the audio stream, implement [AudioProcessor] directly instead.
 * * **Threading**: [onAudioBuffer] is called on an internal, serialized audio thread.
 *   Implementations must not block this thread.
 *
 * @see AudioProcessor
 * @see Recorder.Builder.setAudioProcessors
 */
@ExperimentalAudioApi
public abstract class PassthroughAudioProcessor : AudioProcessor {

    private var outputBuffer: ByteBuffer = EMPTY_AUDIO_BUFFER

    @Throws(AudioProcessor.UnsupportedAudioFormatException::class)
    final override fun configure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        onConfigure(inputAudioFormat)
        return inputAudioFormat
    }

    final override fun queueInput(inputBuffer: ByteBuffer) {
        if (inputBuffer.hasRemaining()) {
            val readOnlyBuffer = inputBuffer.asReadOnlyBuffer().order(inputBuffer.order())
            onAudioBuffer(readOnlyBuffer)
            outputBuffer = inputBuffer
        } else {
            outputBuffer = EMPTY_AUDIO_BUFFER
        }
    }

    final override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_AUDIO_BUFFER
        return output
    }

    final override fun flush() {
        outputBuffer = EMPTY_AUDIO_BUFFER
        onFlush()
    }

    final override fun reset() {
        outputBuffer = EMPTY_AUDIO_BUFFER
        onReset()
    }

    /**
     * Called when the processor is configured with the format of the incoming audio stream.
     *
     * Override to record stream parameters (such as sample rate or channel count) or to reject
     * unsupported formats. The audio is always forwarded in [inputAudioFormat]. The default
     * implementation does nothing.
     *
     * @param inputAudioFormat format of the incoming audio stream
     * @throws AudioProcessor.UnsupportedAudioFormatException if [inputAudioFormat] is not supported
     * @see AudioProcessor.configure
     */
    @Throws(AudioProcessor.UnsupportedAudioFormatException::class)
    protected open fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat) {}

    /**
     * Called when the audio stream is interrupted, such as when the recording is paused or stopped.
     *
     * Override to clear inspection state. The default implementation does nothing.
     *
     * @see AudioProcessor.flush
     */
    protected open fun onFlush() {}

    /**
     * Called when the processor is reset to its unconfigured state.
     *
     * Override to release resources held for inspection. The default implementation does nothing.
     *
     * @see AudioProcessor.reset
     */
    protected open fun onReset() {}

    /**
     * Receives audio buffers synchronously as they arrive during recording.
     * * **Buffer lifecycle**: [audioBuffer] is a direct, read-only buffer in native byte order and
     *   is valid only during this call. To use or store the data after this call returns, make a
     *   copy.
     * * **Non-blocking requirement**: Implementations must not block this thread. For heavy or
     *   asynchronous computation (such as ML inference), copy the buffer data and dispatch to a
     *   background thread.
     *
     * @param audioBuffer read-only direct buffer containing incoming audio frames in native byte
     *   order
     */
    protected abstract fun onAudioBuffer(audioBuffer: ByteBuffer)
}
