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

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

/** {@inheritDoc} */
@AutoValue
public abstract class AudioEncoderConfig implements EncoderConfig {

    // Restrict constructor to same package
    AudioEncoderConfig() {
    }

    /** Returns a build for this config. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_AudioEncoderConfig.Builder();
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public abstract String getMimeType();

    /** Gets the bitrate. */
    public abstract int getBitrate();

    /** Gets the sample bitrate. */
    public abstract int getSampleRate();

    /** Gets the channel count. */
    public abstract int getChannelCount();

    /** {@inheritDoc} */
    @NonNull
    @Override
    public MediaFormat toMediaFormat() {
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(getMimeType(), getSampleRate(),
                getChannelCount());
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, getBitrate());
        if (getMimeType().equals(MediaFormat.MIMETYPE_AUDIO_AAC)) {
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        }
        return mediaFormat;
    }

    /** The builder of the config. */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /** Sets the mime type. */
        @NonNull
        public abstract Builder setMimeType(@NonNull String mimeType);

        /** Sets the bitrate. */
        @NonNull
        public abstract Builder setBitrate(int bitrate);

        /** Sets the sample rate. */
        @NonNull
        public abstract Builder setSampleRate(int sampleRate);

        /** Sets the channel count. */
        @NonNull
        public abstract Builder setChannelCount(int channelCount);

        /** Builds the config instance. */
        @NonNull
        public abstract AudioEncoderConfig build();
    }
}
