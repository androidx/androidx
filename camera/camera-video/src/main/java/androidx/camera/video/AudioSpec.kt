/*
 * Copyright 2020 The Android Open Source Project
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
import android.media.MediaRecorder
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.camera.video.MediaConstants.MIME_TYPE_UNSPECIFIED
import java.util.Objects

/** Audio specification that is options to config audio source and encoding. */
@RestrictTo(Scope.LIBRARY)
public class AudioSpec
@JvmOverloads
constructor(
    public val bitrate: Int = BITRATE_UNSPECIFIED,
    @get:SourceFormat public val sourceFormat: Int = SOURCE_FORMAT_UNSPECIFIED,
    @get:Source public val source: Int = SOURCE_UNSPECIFIED,
    public val sampleRate: Int = SAMPLE_RATE_UNSPECIFIED,
    @get:ChannelCount public val channelCount: Int = CHANNEL_COUNT_UNSPECIFIED,
    public val mimeType: String = MIME_TYPE_UNSPECIFIED,
) {
    /** Returns a [Builder] instance with the same property values as this instance. */
    public fun toBuilder(): Builder {
        return Builder()
            .setSampleRate(sampleRate)
            .setBitrate(bitrate)
            .setChannelCount(channelCount)
            .setSource(source)
            .setSourceFormat(sourceFormat)
            .setMimeType(mimeType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioSpec) return false
        return sourceFormat == other.sourceFormat &&
            source == other.source &&
            channelCount == other.channelCount &&
            bitrate == other.bitrate &&
            sampleRate == other.sampleRate &&
            mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        return Objects.hash(bitrate, sourceFormat, source, sampleRate, channelCount)
    }

    override fun toString(): String {
        return "AudioSpec{" +
            "bitrate=$bitrate, " +
            "sourceFormat=$sourceFormat, " +
            "source=$source, " +
            "sampleRate=$sampleRate, " +
            "channelCount=$channelCount, " +
            "mimeType=$mimeType" +
            '}'
    }

    /** The builder of the [AudioSpec]. */
    @RestrictTo(Scope.LIBRARY)
    public class Builder {
        private var bitrate: Int = BITRATE_UNSPECIFIED
        private var sourceFormat: Int = SOURCE_FORMAT_UNSPECIFIED
        private var source: Int = SOURCE_UNSPECIFIED
        private var sampleRate: Int = SAMPLE_RATE_UNSPECIFIED
        private var channelCount: Int = CHANNEL_COUNT_UNSPECIFIED
        private var mimeType: String = MIME_TYPE_UNSPECIFIED

        /**
         * Sets the desired bitrate to be used by the encoder.
         *
         * If not set, defaults to [BITRATE_UNSPECIFIED].
         */
        public fun setBitrate(bitrate: Int): Builder {
            this.bitrate = bitrate
            return this
        }

        /**
         * Sets the audio source format.
         *
         * Available values for source format are [SOURCE_FORMAT_UNSPECIFIED] and
         * [SOURCE_FORMAT_PCM_16BIT].
         *
         * If not set, defaults to [SOURCE_FORMAT_UNSPECIFIED].
         */
        public fun setSourceFormat(@SourceFormat audioFormat: Int): Builder {
            this.sourceFormat = audioFormat
            return this
        }

        /**
         * Sets the audio source.
         *
         * Available values for source are [SOURCE_UNSPECIFIED] and [SOURCE_CAMCORDER].
         *
         * If not set, defaults to [SOURCE_UNSPECIFIED].
         */
        public fun setSource(@Source source: Int): Builder = apply { this.source = source }

        /**
         * Sets the desired sample rate to be used by the encoder.
         *
         * If not set, defaults to [SAMPLE_RATE_UNSPECIFIED].
         */
        public fun setSampleRate(sampleRate: Int): Builder = apply { this.sampleRate = sampleRate }

        /**
         * Sets the desired number of audio channels.
         *
         * If not set, defaults to [CHANNEL_COUNT_UNSPECIFIED]. Other common channel counts include
         * [CHANNEL_COUNT_MONO] or [CHANNEL_COUNT_STEREO].
         *
         * Setting to [CHANNEL_COUNT_NONE] is equivalent to requesting that no audio should be
         * present.
         */
        public fun setChannelCount(@ChannelCount channelCount: Int): Builder = apply {
            this.channelCount = channelCount
        }

        /**
         * Sets the desired MIME type to be used by the encoder.
         *
         * If not set, defaults to [MIME_TYPE_UNSPECIFIED].
         */
        public fun setMimeType(mimeType: String): Builder = apply { this.mimeType = mimeType }

        /** Builds the AudioSpec instance. */
        public fun build(): AudioSpec {
            return AudioSpec(bitrate, sourceFormat, source, sampleRate, channelCount, mimeType)
        }
    }

    @RestrictTo(Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(SOURCE_FORMAT_UNSPECIFIED, SOURCE_FORMAT_PCM_16BIT)
    public annotation class SourceFormat

    @RestrictTo(Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        open = true,
        value =
            [
                CHANNEL_COUNT_UNSPECIFIED,
                CHANNEL_COUNT_NONE,
                CHANNEL_COUNT_MONO,
                CHANNEL_COUNT_STEREO,
            ],
    )
    public annotation class ChannelCount

    @RestrictTo(Scope.LIBRARY)
    @IntDef(
        SOURCE_UNSPECIFIED,
        SOURCE_CAMCORDER,
        SOURCE_DEFAULT,
        SOURCE_MIC,
        SOURCE_UNPROCESSED,
        SOURCE_VOICE_COMMUNICATION,
        SOURCE_VOICE_RECOGNITION,
        SOURCE_VOICE_PERFORMANCE,
    )
    @Retention(AnnotationRetention.SOURCE)
    public annotation class Source

    public companion object {
        /** The audio source format representing no preference for audio source format. */
        public const val SOURCE_FORMAT_UNSPECIFIED: Int = -1

        /**
         * The PCM 16 bit per sample audio source format. Guaranteed to be supported by all devices.
         */
        public const val SOURCE_FORMAT_PCM_16BIT: Int = AudioFormat.ENCODING_PCM_16BIT

        /** Allows the audio source to choose the appropriate number of channels. */
        public const val CHANNEL_COUNT_UNSPECIFIED: Int = -1

        /** A channel count which is equivalent to no audio. */
        public const val CHANNEL_COUNT_NONE: Int = 0

        /** A channel count corresponding to a single audio channel. */
        public const val CHANNEL_COUNT_MONO: Int = 1

        /** A channel count corresponding to two audio channels. */
        public const val CHANNEL_COUNT_STEREO: Int = 2

        /** The audio source representing no preference for audio source. */
        public const val SOURCE_UNSPECIFIED: Int = -1

        /**
         * Microphone audio source tuned for video recording, with the same orientation as the
         * camera if available.
         *
         * @see MediaRecorder.AudioSource.CAMCORDER
         */
        public const val SOURCE_CAMCORDER: Int = MediaRecorder.AudioSource.CAMCORDER

        /**
         * Default audio source.
         *
         * @see MediaRecorder.AudioSource.DEFAULT
         */
        public const val SOURCE_DEFAULT: Int = MediaRecorder.AudioSource.DEFAULT

        /**
         * Microphone audio source.
         *
         * @see MediaRecorder.AudioSource.MIC
         */
        public const val SOURCE_MIC: Int = MediaRecorder.AudioSource.MIC

        /**
         * Microphone audio source tuned for unprocessed (raw) sound if available, behaves like
         * [SOURCE_DEFAULT] otherwise.
         *
         * @see MediaRecorder.AudioSource.UNPROCESSED
         */
        @RequiresApi(24)
        public const val SOURCE_UNPROCESSED: Int = MediaRecorder.AudioSource.UNPROCESSED

        /**
         * Microphone audio source tuned for voice communications such as VoIP.
         *
         * @see MediaRecorder.AudioSource.VOICE_COMMUNICATION
         */
        public const val SOURCE_VOICE_COMMUNICATION: Int =
            MediaRecorder.AudioSource.VOICE_COMMUNICATION

        /**
         * Microphone audio source tuned for voice recognition.
         *
         * @see MediaRecorder.AudioSource.VOICE_RECOGNITION
         */
        public const val SOURCE_VOICE_RECOGNITION: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION

        /**
         * Source for capturing audio meant to be processed in real time and played back for live
         * performance.
         *
         * @see MediaRecorder.AudioSource.VOICE_PERFORMANCE
         */
        @RequiresApi(29)
        public const val SOURCE_VOICE_PERFORMANCE: Int = MediaRecorder.AudioSource.VOICE_PERFORMANCE

        /**
         * No preference for bitrate.
         *
         * Using this value with [AudioSpec.Builder.setBitrate] informs the device it should choose
         * any appropriate bitrate given the device and codec constraints.
         */
        public const val BITRATE_UNSPECIFIED: Int = 0

        /**
         * No preference for sample rate.
         *
         * Using this value with [AudioSpec.Builder.setSampleRate] informs the device it should
         * choose any appropriate sample rate given the device and codec constraints.
         */
        public const val SAMPLE_RATE_UNSPECIFIED: Int = 0

        /** An [AudioSpec] representing the default audio configuration. */
        public val DEFAULT: AudioSpec = builder().build()

        /** Returns a build for this config. */
        @RestrictTo(Scope.LIBRARY)
        @JvmStatic
        public fun builder(): Builder {
            return Builder()
        }
    }
}
