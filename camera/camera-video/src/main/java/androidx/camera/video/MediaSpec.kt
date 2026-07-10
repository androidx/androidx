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
package androidx.camera.video

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.core.util.Consumer
import java.util.Objects

/**
 * MediaSpec communicates the encoding type and encoder-specific options for both the video and
 * audio inputs.
 */
@RestrictTo(Scope.LIBRARY)
public class MediaSpec
@JvmOverloads
public constructor(
    public val videoSpec: VideoSpec = VideoSpec.DEFAULT,
    public val audioSpec: AudioSpec = AudioSpec.DEFAULT,
    @get:OutputFormat public val outputFormat: Int = OUTPUT_FORMAT_UNSPECIFIED,
) {

    /** Returns a [Builder] instance with the same property values as this instance. */
    public fun toBuilder(): Builder {
        return Builder()
            .setVideoSpec(videoSpec)
            .setAudioSpec(audioSpec)
            .setOutputFormat(outputFormat)
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaSpec) return false
        return videoSpec == other.videoSpec &&
            audioSpec == other.audioSpec &&
            outputFormat == other.outputFormat
    }

    public override fun hashCode(): Int {
        return Objects.hash(videoSpec, audioSpec, outputFormat)
    }

    public override fun toString(): String {
        return "MediaSpec{" +
            "videoSpec=$videoSpec, " +
            "audioSpec=$audioSpec, " +
            "outputFormat=$outputFormat" +
            "}"
    }

    /** The builder for [MediaSpec]. */
    @RestrictTo(Scope.LIBRARY)
    public class Builder {
        private var audioSpec: AudioSpec = AudioSpec.DEFAULT
        private var videoSpec: VideoSpec = VideoSpec.DEFAULT
        @OutputFormat private var outputFormat: Int = OUTPUT_FORMAT_UNSPECIFIED

        /** Sets the audio-related configuration. */
        public fun setAudioSpec(audioSpec: AudioSpec): Builder = apply {
            this.audioSpec = audioSpec
        }

        /** Sets the video-related configuration. */
        public fun setVideoSpec(videoSpec: VideoSpec): Builder = apply {
            this.videoSpec = videoSpec
        }

        /** Sets the video recording output format. */
        public fun setOutputFormat(@OutputFormat format: Int): Builder = apply {
            this.outputFormat = format
        }

        /** Configures the [AudioSpec] of this media specification with the given block. */
        @RestrictTo(Scope.LIBRARY)
        public fun configureAudio(configBlock: Consumer<AudioSpec.Builder>): Builder = apply {
            this.audioSpec = audioSpec.toBuilder().apply { configBlock.accept(this) }.build()
        }

        /** Configures the [VideoSpec] of this media specification with the given block. */
        @RestrictTo(Scope.LIBRARY)
        public fun configureVideo(configBlock: Consumer<VideoSpec.Builder>): Builder = apply {
            this.videoSpec = videoSpec.toBuilder().apply { configBlock.accept(this) }.build()
        }

        /** Builds the [MediaSpec] from this builder. */
        public fun build(): MediaSpec {
            return MediaSpec(videoSpec, audioSpec, outputFormat)
        }
    }

    @IntDef(OUTPUT_FORMAT_UNSPECIFIED, OUTPUT_FORMAT_MPEG_4, OUTPUT_FORMAT_WEBM)
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(Scope.LIBRARY)
    public annotation class OutputFormat

    public companion object {
        /** The output format representing no preference. */
        public const val OUTPUT_FORMAT_UNSPECIFIED: Int = -1
        /** MPEG4 media file format. */
        public const val OUTPUT_FORMAT_MPEG_4: Int = 0
        /** VP8, VP9 media file format */
        public const val OUTPUT_FORMAT_WEBM: Int = 1

        /** Creates a [Builder]. */
        @RestrictTo(Scope.LIBRARY) @JvmStatic public fun builder(): Builder = Builder()
    }
}
