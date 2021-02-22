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

import android.util.Range;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Audio specification that is options to config audio encoding.
 */
@AutoValue
public abstract class AudioSpec {

    /**
     * Allows the audio source to choose the appropriate number of channels.
     */
    public static final int CHANNEL_COUNT_AUTO = -1;
    /**
     * A channel count which is equivalent to no audio.
     */
    public static final int CHANNEL_COUNT_NONE = 0;
    /**
     * A channel count corresponding to a single audio channel.
     */
    public static final int CHANNEL_COUNT_MONO = 1;
    /**
     * A channel count corresponding to two audio channels.
     */
    public static final int CHANNEL_COUNT_STEREO = 2;

    /** @hide */
    @IntDef(open = true,
            value = {CHANNEL_COUNT_AUTO, CHANNEL_COUNT_NONE, CHANNEL_COUNT_MONO,
                    CHANNEL_COUNT_STEREO})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface ChannelCount {
    }

    /**
     * Bitrate range representing no preference for bitrate.
     *
     * <p>Using this value with {@link AudioSpec.Builder#setBitrate(Range)} informs the device it
     * should choose any appropriate bitrate given the device and codec constraints.
     */
    public static final Range<Integer> BITRATE_RANGE_AUTO = new Range<>(0,
            Integer.MAX_VALUE);

    /**
     * Sample rate range representing no preference for sample rate.
     *
     * <p>Using this value with {@link AudioSpec.Builder#setSampleRate(Range)} informs the device it
     * should choose any appropriate sample rate given the device and codec constraints.
     */
    public static final Range<Integer> SAMPLE_RATE_RANGE_AUTO = new Range<>(0,
            Integer.MAX_VALUE);

    // Restrict constructor to same package
    AudioSpec() {
    }

    /** Returns a build for this config. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_AudioSpec.Builder()
                .setChannelCount(CHANNEL_COUNT_AUTO)
                .setBitrate(BITRATE_RANGE_AUTO)
                .setSampleRate(SAMPLE_RATE_RANGE_AUTO);
    }

    /** Gets the bitrate. */
    @NonNull
    public abstract Range<Integer> getBitrate();

    /** Gets the sample bitrate. */
    @NonNull
    public abstract Range<Integer> getSampleRate();

    /** Gets the channel count. */
    @ChannelCount
    public abstract int getChannelCount();

    /**
     * Returns a {@link AudioSpec.Builder} instance with the same property values as this instance.
     */
    @NonNull
    public abstract AudioSpec.Builder toBuilder();

    /** The builder of the {@link AudioSpec}. */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /**
         * Sets the desired range of bitrates to be used by the encoder.
         *
         * <p>If not set, defaults to {@link #BITRATE_RANGE_AUTO}.
         */
        @NonNull
        public abstract Builder setBitrate(@NonNull Range<Integer> bitrate);

        /**
         * Sets the desired range of sample rates to be used by the encoder.
         *
         * <p>If not set, defaults to {@link #SAMPLE_RATE_RANGE_AUTO}.
         */
        @NonNull
        public abstract Builder setSampleRate(@NonNull Range<Integer> sampleRate);

        /**
         * Sets the desired number of audio channels.
         *
         * <p>If not set, defaults to {@link #CHANNEL_COUNT_AUTO}. Other common channel counts
         * include {@link #CHANNEL_COUNT_MONO} or {@link #CHANNEL_COUNT_STEREO}.
         *
         * <p>Setting to {@link #CHANNEL_COUNT_NONE} is equivalent to requesting that no audio
         * should be present.
         */
        @NonNull
        public abstract Builder setChannelCount(@ChannelCount int channelCount);

        /** Builds the AudioSpec instance. */
        @NonNull
        public abstract AudioSpec build();
    }

    /**
     * An audio specification that corresponds to no audio.
     *
     * <p>This is equivalent to creating an {@link AudioSpec} with channel count set to
     * {@link #CHANNEL_COUNT_NONE}.
     */
    public static final AudioSpec NO_AUDIO = builder().setChannelCount(CHANNEL_COUNT_NONE).build();
}
