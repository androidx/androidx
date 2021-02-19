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

package androidx.camera.video;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * MediaSpec communicates the encoding type and encoder-specific options for both the
 * video and audio inputs to the VideoOutput.
 */
@AutoValue
public abstract class MediaSpec {

    /** An output format that will be determined by the implementation of {@link VideoOutput}. */
    public static final int OUTPUT_FORMAT_AUTO = -1;
    /** MPEG4 media file format. */
    public static final int OUTPUT_FORMAT_MPEG_4 = 0;
    /** VP8, VP9 media file format */
    public static final int OUTPUT_FORMAT_WEBM = 1;

    /** @hide */
    @IntDef({OUTPUT_FORMAT_AUTO, OUTPUT_FORMAT_MPEG_4, OUTPUT_FORMAT_WEBM})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface OutputFormat {
    }

    // Doesn't allow inheritance outside of package
    MediaSpec() {
    }

    /**
     * Returns a VideoSpec instance.
     */
    @NonNull
    public abstract VideoSpec getVideoSpec();

    /**
     * Returns an AudioSpec instance.
     */
    @NonNull
    public abstract AudioSpec getAudioSpec();


    /**
     * Returns the output file format.
     *
     * @return the file format
     */
    @OutputFormat
    public abstract int getOutputFormat();

    /** Creates a {@link Builder}. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_MediaSpec.Builder()
                .setOutputFormat(OUTPUT_FORMAT_AUTO)
                .setAudioSpec(AudioSpec.builder().build())
                .setVideoSpec(VideoSpec.builder().build());
    }

    /**
     * The builder for {@link MediaSpec}.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        Builder() {
        }

        /**
         * Sets audio related configuration.
         *
         * <p>If not set, contains the default implementation of {@link AudioSpec} returned by
         * {@link AudioSpec.Builder#build()}.
         *
         * <p>The provided specification will override all audio related properties of
         * this media specification. If only some properties need to be modified, use
         * {@link #configureAudio(Consumer)}.
         *
         * <p>To request disabling audio, this should be set to {@link AudioSpec#NO_AUDIO}.
         */
        @NonNull
        public abstract Builder setAudioSpec(@NonNull AudioSpec audioSpec);

        /**
         * Configures the {@link AudioSpec} of this media specification with the given block.
         *
         * <p>The provided {@link AudioSpec.Builder} is pre-populated with the current state of the
         * @param configBlock A consumer which provides the {@link AudioSpec.Builder} which will
         *                    configure the {@link AudioSpec} of this media specification.
         */
        @NonNull
        public Builder configureAudio(@NonNull Consumer<AudioSpec.Builder> configBlock) {
            AudioSpec.Builder audioSpecBuilder = getAudioSpec().toBuilder();
            configBlock.accept(audioSpecBuilder);
            setAudioSpec(audioSpecBuilder.build());
            return this;
        }

        /**
         * Gets the existing AudioSpec.
         */
        // This is not meant to be a public API. Only should be used internally within Builder.
        @SuppressLint("KotlinPropertyAccess")
        abstract AudioSpec getAudioSpec();

        /**
         * Sets video related configuration.
         *
         * <p>If not set, contains the default implementation of {@link VideoSpec} returned by
         * {@link VideoSpec.Builder#build()}.
         *
         * <p>The provided specification will override all video related properties of this media
         * specification. If only some properties need to be modified, use
         * {@link #configureVideo(Consumer)}.
         */
        @NonNull
        public abstract Builder setVideoSpec(@NonNull VideoSpec videoSpec);

        /**
         * Configures the {@link AudioSpec} of this media specification with the given block.
         * @param configBlock A consumer which provides the {@link AudioSpec.Builder} which will
         *                    configure the {@link AudioSpec} of this media specification.
         */
        @NonNull
        public Builder configureVideo(@NonNull Consumer<VideoSpec.Builder> configBlock) {
            VideoSpec.Builder videoSpecBuilder = getVideoSpec().toBuilder();
            configBlock.accept(videoSpecBuilder);
            setVideoSpec(videoSpecBuilder.build());
            return this;
        }

        /**
         * Gets the existing VideoSpec.
         */
        // This is not meant to be a public API. Only should be used internally within Builder.
        @SuppressLint("KotlinPropertyAccess")
        abstract VideoSpec getVideoSpec();

        /**
         * Sets the video recording output format.
         *
         * <p>If not set, the default is {@link #OUTPUT_FORMAT_AUTO}.
         *
         * @param videoFormat The requested video format. Possible values are
         * {@link MediaSpec#OUTPUT_FORMAT_AUTO}, {@link MediaSpec#OUTPUT_FORMAT_MPEG_4} or
         * {@link MediaSpec#OUTPUT_FORMAT_WEBM}.
         */
        @NonNull
        public abstract Builder setOutputFormat(@OutputFormat int videoFormat);

        /** Build the {@link MediaSpec} from this builder. */
        @NonNull
        public abstract MediaSpec build();
    }
}
