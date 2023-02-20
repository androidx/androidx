/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.video.internal.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Settings required to configure the audio source.
 */
@SuppressWarnings("NullableProblems") // Problem from AutoValue generated class.
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class AudioSettings {

    // Common sample rate options to choose from in descending order.
    public static final List<Integer> COMMON_SAMPLE_RATES = Collections.unmodifiableList(
            Arrays.asList(48000, 44100, 22050, 11025, 8000, 4800));

    /** Creates a builder for these settings. */
    @SuppressLint("Range") // Need to initialize as invalid values
    @NonNull
    public static Builder builder() {
        return new AutoValue_AudioSettings.Builder()
                .setAudioSource(-1)
                .setSampleRate(-1)
                .setChannelCount(-1)
                .setAudioFormat(-1);
    }

    /** Creates a {@link Builder} initialized with the same settings as this instance. */
    @NonNull
    public abstract Builder toBuilder();

    /**
     * Gets the device audio source.
     *
     * @see android.media.MediaRecorder.AudioSource#MIC
     * @see android.media.MediaRecorder.AudioSource#CAMCORDER
     */
    public abstract int getAudioSource();

    /**
     * Gets the audio sample rate.
     */
    @IntRange(from = 1)
    public abstract int getSampleRate();

    /**
     * Gets the channel count.
     */
    @IntRange(from = 1)
    public abstract int getChannelCount();

    /**
     * Sets the audio format.
     *
     * @see AudioFormat#ENCODING_PCM_16BIT
     */
    public abstract int getAudioFormat();

    // Should not be instantiated directly
    AudioSettings() {
    }

    /** Gets the size in bytes per frame. */
    public int getBytesPerFrame() {
        return AudioUtils.getBytesPerFrame(getAudioFormat(), getChannelCount());
    }

    /**
     * A Builder for {@link AudioSettings}.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Sets the device audio source.
         *
         * @see android.media.MediaRecorder.AudioSource#MIC
         * @see android.media.MediaRecorder.AudioSource#CAMCORDER
         */
        @NonNull
        public abstract Builder setAudioSource(int audioSource);

        /**
         * Sets the audio sample rate in Hertz.
         */
        @NonNull
        public abstract Builder setSampleRate(@IntRange(from = 1) int sampleRate);

        /**
         * Sets the channel count.
         */
        @NonNull
        public abstract Builder setChannelCount(@IntRange(from = 1) int channelCount);

        /**
         * Sets the audio format.
         *
         * @see AudioFormat#ENCODING_PCM_16BIT
         */
        @NonNull
        public abstract Builder setAudioFormat(int audioFormat);

        abstract AudioSettings autoBuild(); // Actual build method. Not public.

        /**
         * Returns the built config after performing settings validation.
         *
         * @throws IllegalArgumentException if a setting is missing or invalid.
         */
        @NonNull
        public final AudioSettings build() {
            AudioSettings settings = autoBuild();
            String missingOrInvalid = "";
            if (settings.getAudioSource() == -1) {
                missingOrInvalid += " audioSource";
            }
            if (settings.getSampleRate() <= 0) {
                missingOrInvalid += " sampleRate";
            }
            if (settings.getChannelCount() <= 0) {
                missingOrInvalid += " channelCount";
            }
            if (settings.getAudioFormat() == -1) {
                missingOrInvalid += " audioFormat";
            }

            if (!missingOrInvalid.isEmpty()) {
                throw new IllegalArgumentException("Required settings missing or "
                        + "non-positive:" + missingOrInvalid);
            }

            return settings;
        }

        // Should not be instantiated directly
        Builder() {
        }
    }
}
