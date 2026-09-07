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

import androidx.annotation.RestrictTo
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
 * (such as sample rate or channel count), override [configure], record the incoming
 * [AudioProcessor.AudioFormat], and return `super.configure(inputAudioFormat)`. To modify or filter
 * the audio stream, implement [AudioProcessor] directly instead.
 * * **Threading**: [onAudioBuffer] is called on an internal, serialized audio thread.
 *   Implementations must not block this thread.
 *
 * @see AudioProcessor
 * @see Recorder.Builder.setAudioProcessors
 */
// TODO: b/305067133 - Make this public in next alpha
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class PassthroughAudioProcessor : AudioProcessor {

    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    override fun configure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat = inputAudioFormat

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (inputBuffer.hasRemaining()) {
            val readOnlyBuffer = inputBuffer.asReadOnlyBuffer().order(inputBuffer.order())
            onAudioBuffer(readOnlyBuffer)
            outputBuffer = inputBuffer
        } else {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
        }
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
    public abstract fun onAudioBuffer(audioBuffer: ByteBuffer)
}
