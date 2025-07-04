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

package androidx.camera.video.internal.encoder;

import android.media.MediaFormat;

import androidx.camera.core.impl.Timebase;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/** {@inheritDoc} */
@AutoValue
public abstract class AudioEncoderConfig implements EncoderConfig {

    // Restrict constructor to same package
    AudioEncoderConfig() {
    }

    /** Returns a build for this config. */
    public static @NonNull Builder builder() {
        return new AutoValue_AudioEncoderConfig.Builder()
                .setProfile(EncoderConfig.CODEC_PROFILE_NONE);
    }

    /** {@inheritDoc} */
    @Override
    public abstract @NonNull String getMimeType();

    /**
     * {@inheritDoc}
     *
     * <p>Default is {@link EncoderConfig#CODEC_PROFILE_NONE}.
     */
    @Override
    public abstract int getProfile();

    @Override
    public abstract @NonNull Timebase getInputTimebase();

    /** Gets the bitrate. */
    public abstract int getBitrate();

    /** Gets the capture sample rate. */
    public abstract int getCaptureSampleRate();

    /** Gets the encode sample rate. */
    public abstract int getEncodeSampleRate();

    /** Gets the channel count. */
    public abstract int getChannelCount();

    /** {@inheritDoc} */
    @Override
    public @NonNull MediaFormat toMediaFormat() {
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(getMimeType(),
                getEncodeSampleRate(), getChannelCount());
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, getBitrate());
        if (getProfile() != CODEC_PROFILE_NONE) {
            if (getMimeType().equals(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, getProfile());
            } else {
                mediaFormat.setInteger(MediaFormat.KEY_PROFILE, getProfile());
            }
        }

        return mediaFormat;
    }

    /** The builder of the config. */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /**
         * Sets the mime type.
         *
         * <p>If the mime type is set to AAC, i.e. {@link MediaFormat#MIMETYPE_AUDIO_AAC}, then a
         * profile must be set with {@link #setProfile(int)}. If a profile isn't set or is set to
         * {@link EncoderConfig#CODEC_PROFILE_NONE}, an IllegalArgumentException will be thrown by
         * {@link #build()}.
         */
        public abstract @NonNull Builder setMimeType(@NonNull String mimeType);

        /** Sets (optional) profile for the mime type specified by {@link #setMimeType(String)}. */
        public abstract @NonNull Builder setProfile(int profile);

        /** Sets the source timebase. */
        public abstract @NonNull Builder setInputTimebase(@NonNull Timebase timebase);

        /** Sets the bitrate. */
        public abstract @NonNull Builder setBitrate(int bitrate);

        /** Sets the capture sample rate. */
        public abstract @NonNull Builder setCaptureSampleRate(int sampleRate);

        /** Sets the encode sample rate. */
        public abstract @NonNull Builder setEncodeSampleRate(int sampleRate);

        /** Sets the channel count. */
        public abstract @NonNull Builder setChannelCount(int channelCount);

        abstract @NonNull AudioEncoderConfig autoBuild();

        /** Builds the config instance. */
        public @NonNull AudioEncoderConfig build() {
            AudioEncoderConfig config = autoBuild();
            if (Objects.equals(config.getMimeType(), MediaFormat.MIMETYPE_AUDIO_AAC)
                    && config.getProfile() == CODEC_PROFILE_NONE) {
                throw new IllegalArgumentException("Encoder mime set to AAC, but no AAC profile "
                        + "was provided.");
            }

            return config;
        }
    }
}
