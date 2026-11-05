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

import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Processes raw PCM audio buffers in the video recording pipeline before encoding.
 *
 * Use an AudioProcessor to transform, filter, or modify audio data (such as applying gain, noise
 * suppression, or audio effects). To inspect audio without modifying it (such as for waveform
 * rendering, volume metering, or ML audio classification), extend [PassthroughAudioProcessor]
 * instead.
 * * **Execution order**: Processors run sequentially in the order provided to
 *   [Recorder.Builder.setAudioProcessors]. The output of processor `i` is fed as input to processor
 *   `i + 1`.
 * * **Threading**: [configure] is called during recording initialization on a background setup
 *   thread prior to audio streaming. Streaming methods ([queueInput], [getOutput], [flush]) and
 *   lifecycle teardown ([reset]) are called synchronously on an internal, serialized audio thread.
 *   Implementations must not block this thread (avoid file/network I/O, lock contention, or heavy
 *   compute) to prevent frame drops or recording pipeline stalls.
 * * **Memory**: All buffers are direct [ByteBuffer]s with [ByteOrder.nativeOrder]. Implementations
 *   must not modify [queueInput]'s `inputBuffer` sample data in place, and must supply processed
 *   output via [getOutput].
 * * **Terminal format**: The final processor in the chain configured on
 *   [Recorder.Builder.setAudioProcessors] must output
 *   [android.media.AudioFormat.ENCODING_PCM_16BIT].
 * * **Timing**: CameraX derives output timestamps by assuming output frames correspond, in order,
 *   to input frames (scaled by any sample rate change). Processors may delay output by buffering
 *   input, but must not add or drop frames, including extra leading frames such as priming silence
 *   from algorithmic lookahead; otherwise audio will be offset by their duration.
 * * **Error handling**: If a processor fails [configure] (such as throwing
 *   [UnsupportedAudioFormatException]) or throws an unhandled exception during streaming
 *   ([queueInput], [getOutput]), audio recording will fail with
 *   [AudioStats.AUDIO_STATE_SOURCE_ERROR] and recording will proceed without audio. The underlying
 *   exception can be inspected via [AudioStats.getErrorCause].
 *
 * ### Lifecycle
 *
 * ```
 *   [Unconfigured]
 *          │
 *          │ configure()
 *          ▼
 *    [Configured] ◄──────────────┐
 *          │                     │
 *          ├─► queueInput()      │
 *          │   getOutput()       │ flush()
 *          │                     │
 *          └─────────────────────┘
 *          │
 *          │ reset()
 *          ▼
 *   [Unconfigured]
 * ```
 *
 * ### Interoperability with Audio Pipelines
 *
 * External linear PCM audio processors can be adapted into CameraX by converting between stream
 * formats in [configure], forwarding buffers in [queueInput] and [getOutput], and delegating
 * [flush] and [reset]. Note that CameraX streams continuously without end-of-stream tokens, and the
 * final processor in the chain must output 16-bit PCM.
 *
 * @see PassthroughAudioProcessor
 * @see Recorder.Builder.setAudioProcessors
 */
@ExperimentalAudioApi
public interface AudioProcessor {

    /**
     * Defines the format of an audio stream processed by an [AudioProcessor].
     * * **Supported encodings**: The linear PCM encodings listed for [encoding].
     * * **Frame alignment**: One audio frame consists of one sample across all channels.
     *
     * @property sampleRate audio sample rate in Hertz (e.g. `44100` or `48000`)
     * @property channelCount number of audio channels (e.g. `1` for mono, `2` for stereo)
     * @property encoding linear PCM encoding, one of [android.media.AudioFormat.ENCODING_PCM_8BIT],
     *   [android.media.AudioFormat.ENCODING_PCM_16BIT],
     *   [android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED],
     *   [android.media.AudioFormat.ENCODING_PCM_32BIT], or
     *   [android.media.AudioFormat.ENCODING_PCM_FLOAT]
     * @throws IllegalArgumentException if [sampleRate] or [channelCount] is not positive, or if
     *   [encoding] is not one of the supported encodings
     */
    @ExperimentalAudioApi
    public class AudioFormat(
        @param:IntRange(from = 1) @get:IntRange(from = 1) public val sampleRate: Int,
        @param:IntRange(from = 1) @get:IntRange(from = 1) public val channelCount: Int,
        @param:Encoding @get:Encoding public val encoding: Int,
    ) {
        /**
         * Creates an audio format with [android.media.AudioFormat.ENCODING_PCM_16BIT] encoding.
         *
         * @param sampleRate audio sample rate in Hertz (e.g. `44100` or `48000`)
         * @param channelCount number of audio channels (e.g. `1` for mono, `2` for stereo)
         * @throws IllegalArgumentException if [sampleRate] or [channelCount] is not positive
         */
        public constructor(
            @IntRange(from = 1) sampleRate: Int,
            @IntRange(from = 1) channelCount: Int,
        ) : this(sampleRate, channelCount, android.media.AudioFormat.ENCODING_PCM_16BIT)

        /** The linear PCM encodings supported by [AudioFormat]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            android.media.AudioFormat.ENCODING_PCM_8BIT,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED,
            android.media.AudioFormat.ENCODING_PCM_32BIT,
            android.media.AudioFormat.ENCODING_PCM_FLOAT,
        )
        public annotation class Encoding

        init {
            require(sampleRate > 0) { "sampleRate must be greater than 0: $sampleRate" }
            require(channelCount > 0) { "channelCount must be greater than 0: $channelCount" }
        }

        /**
         * Specifies the size in bytes of a single audio frame across all channels.
         *
         * Computed as [channelCount] multiplied by the byte depth of [encoding].
         */
        public val bytesPerFrame: Int =
            when (encoding) {
                android.media.AudioFormat.ENCODING_PCM_8BIT -> channelCount
                android.media.AudioFormat.ENCODING_PCM_16BIT -> channelCount * 2
                android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> channelCount * 3
                android.media.AudioFormat.ENCODING_PCM_32BIT,
                android.media.AudioFormat.ENCODING_PCM_FLOAT -> channelCount * 4
                else -> throw IllegalArgumentException("Invalid audio encoding: $encoding")
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AudioFormat) return false
            return sampleRate == other.sampleRate &&
                channelCount == other.channelCount &&
                encoding == other.encoding
        }

        override fun hashCode(): Int {
            var result = sampleRate
            result = 31 * result + channelCount
            result = 31 * result + encoding
            return result
        }

        override fun toString(): String {
            return "AudioFormat(sampleRate=$sampleRate, channelCount=$channelCount, " +
                "encoding=$encoding)"
        }
    }

    /**
     * Indicates that an [AudioProcessor] cannot handle the given [AudioFormat].
     *
     * Thrown by [configure] when input stream parameters are unsupported. When thrown during
     * recording initialization, audio recording will fail with
     * [AudioStats.AUDIO_STATE_SOURCE_ERROR] and recording will proceed without audio. The exception
     * can be inspected via [AudioStats.getErrorCause].
     */
    @ExperimentalAudioApi
    public class UnsupportedAudioFormatException : Exception {
        /**
         * Holds the unsupported [AudioFormat] that caused this exception, or `null` if unspecified.
         */
        public val audioFormat: AudioFormat?

        /**
         * Creates an exception with a detail [message].
         *
         * @param message detail message
         */
        public constructor(message: String?) : this(message, null, null)

        /**
         * Creates an exception with a detail [message] and the unsupported [audioFormat].
         *
         * @param message detail message, or `null` to use a default message describing
         *   [audioFormat]
         * @param audioFormat unsupported audio format that caused this exception
         */
        public constructor(
            message: String?,
            audioFormat: AudioFormat?,
        ) : this(message, audioFormat, null)

        /**
         * Creates an exception with a detail [message], the unsupported [audioFormat], and the
         * underlying [cause].
         *
         * @param message detail message, or `null` to use a default message describing
         *   [audioFormat]
         * @param audioFormat unsupported audio format that caused this exception
         * @param cause underlying cause, such as an error from an adapted audio pipeline
         */
        public constructor(
            message: String?,
            audioFormat: AudioFormat?,
            cause: Throwable?,
        ) : super(message ?: audioFormat?.let { "Unsupported audio format: $it" }, cause) {
            this.audioFormat = audioFormat
        }
    }

    /**
     * Configures the processor to accept the incoming [inputAudioFormat].
     *
     * Called during recording initialization on a background setup thread before audio streaming
     * starts. Processors that alter sample rate, channel count, or encoding must return an updated
     * [AudioFormat]. Processors that do not alter the format should return [inputAudioFormat]
     * directly.
     * * **Terminal format**: If this processor is the terminal processor in the chain configured on
     *   [Recorder.Builder.setAudioProcessors], its output format encoding must be
     *   [android.media.AudioFormat.ENCODING_PCM_16BIT].
     *
     * @param inputAudioFormat incoming audio format
     * @return configured output audio format
     * @throws UnsupportedAudioFormatException if [inputAudioFormat] is not supported
     */
    @Throws(UnsupportedAudioFormatException::class)
    public fun configure(inputAudioFormat: AudioFormat): AudioFormat

    /**
     * Queues incoming audio data from [inputBuffer] for processing.
     *
     * Called synchronously on the audio streaming thread. The processor reads incoming audio data
     * from [inputBuffer] and makes processed output available via [getOutput]. Implementations must
     * consume all data between the position and limit of [inputBuffer], by processing it or
     * buffering it for later output, and advance its position to its limit. A processor that does
     * not modify audio may instead return [inputBuffer] itself from [getOutput].
     * * **Buffer ownership**: The caller guarantees [inputBuffer] remains valid and unmodified
     *   until the next call to [queueInput], [flush], or [reset]. Implementations must not retain
     *   references for asynchronous access without copying.
     * * **Non-destructive**: Implementations must not modify sample values in [inputBuffer] in
     *   place; processed samples must be written to an internal direct buffer.
     *
     * @param inputBuffer direct buffer containing incoming audio data in native byte order
     */
    public fun queueInput(inputBuffer: ByteBuffer)

    /**
     * Returns a direct buffer containing processed audio data.
     *
     * Called synchronously on the audio thread after [queueInput]. May be called repeatedly until
     * an empty buffer is returned to drain all output frames produced for the queued input. The
     * returned buffer must remain valid and unmodified until the next call to [queueInput],
     * [getOutput], [flush], or [reset].
     *
     * @return direct buffer containing processed audio data, or a buffer with no remaining data if
     *   no output is ready
     */
    public fun getOutput(): ByteBuffer

    /**
     * Discards all pending or buffered audio data and resets filter state.
     *
     * Called whenever the audio stream is interrupted, such as when the recording is paused or
     * stopped, or after an internal processing error. Audio queued after a flush is not continuous
     * with audio queued before it. The processor remains configured; [configure] will not be called
     * again unless [reset] is called.
     */
    public fun flush()

    /**
     * Resets the processor to its unconfigured state and releases held resources.
     *
     * Called on the serialized audio thread when the recording session or recorder is released.
     * After this call returns, the processor cannot process audio data until [configure] is called
     * again.
     */
    public fun reset()
}
