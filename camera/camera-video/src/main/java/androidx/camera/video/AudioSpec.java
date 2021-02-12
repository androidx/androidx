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

package androidx.camera.video;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

/**
 * Audio specification that is options to config audio encoding.
 */
@AutoValue
public abstract class AudioSpec {

    // Restrict constructor to same package
    AudioSpec() {
    }

    /** Returns a build for this config. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_AudioSpec.Builder();
    }

    /** Gets the bitrate. */
    public abstract int getBitrate();

    /** Gets the sample bitrate. */
    public abstract int getSampleRate();

    /** Gets the channel count. */
    public abstract int getChannelCount();

    /** The builder of the {@link AudioSpec}. */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /** Sets the bitrate. */
        @NonNull
        public abstract Builder setBitrate(int bitrate);

        /** Sets the sample rate. */
        @NonNull
        public abstract Builder setSampleRate(int sampleRate);

        /** Sets the channel count. */
        @NonNull
        public abstract Builder setChannelCount(int channelCount);

        /** Builds the AudioSpec instance. */
        @NonNull
        public abstract AudioSpec build();
    }
}
