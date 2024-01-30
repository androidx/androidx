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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.AspectRatio;

import com.google.auto.value.AutoValue;

import java.util.Arrays;

/**
 * Video specification that is options to config video encoding.
 */
@SuppressWarnings("NullableProblems") // Nullable problem in AutoValue generated class
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(Scope.LIBRARY)
@AutoValue
public abstract class VideoSpec {

    /**
     * Frame rate range representing no preference for frame rate.
     *
     * <p>Using this value with {@link Builder#setFrameRate(Range)} informs the video frame producer
     * it should choose any appropriate frame rate given the device and codec constraints.
     */
    @NonNull
    public static final Range<Integer> FRAME_RATE_RANGE_AUTO = new Range<>(0,
            Integer.MAX_VALUE);

    /**
     * Bitrate range representing no preference for bitrate.
     *
     * <p>Using this value with {@link Builder#setBitrate(Range)} informs the video frame producer
     * it should choose any appropriate bitrate given the device and codec constraints.
     */
    @NonNull
    public static final Range<Integer> BITRATE_RANGE_AUTO = new Range<>(0,
            Integer.MAX_VALUE);

    /**
     * Quality selector representing no preference for quality.
     *
     * <p>Using this value with {@link Builder#setQualitySelector(QualitySelector)} allows the
     * video frame producer to choose video quality based on its current state.
     */
    @NonNull
    public static final QualitySelector QUALITY_SELECTOR_AUTO =
            QualitySelector.fromOrderedList(Arrays.asList(Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.higherQualityOrLowerThan(Quality.FHD));

    // Restrict constructor to same package
    VideoSpec() {
    }

    /** Returns a build for this config. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_VideoSpec.Builder()
                .setQualitySelector(QUALITY_SELECTOR_AUTO)
                .setFrameRate(FRAME_RATE_RANGE_AUTO)
                .setBitrate(BITRATE_RANGE_AUTO)
                .setAspectRatio(AspectRatio.RATIO_DEFAULT);
    }

    /** Gets the {@link QualitySelector}. */
    @NonNull
    public abstract QualitySelector getQualitySelector();

    /** Gets the frame rate. */
    @NonNull
    public abstract Range<Integer> getFrameRate();

    /** Gets the bitrate. */
    @NonNull
    public abstract Range<Integer> getBitrate();

    /** Gets the aspect ratio. */
    @AspectRatio.Ratio
    abstract int getAspectRatio();

    /**
     * Returns a {@link Builder} instance with the same property values as this instance.
     */
    @NonNull
    public abstract Builder toBuilder();

    /**
     * The builder of the {@link VideoSpec}.
     */
    @RestrictTo(Scope.LIBRARY)
    @SuppressWarnings("StaticFinalBuilder")
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /**
         * Sets the {@link QualitySelector}.
         *
         * <p>Video encoding parameters such as frame rate and bitrate will often be automatically
         * determined according to quality. If video parameters are not set directly (such as
         * through {@link #setFrameRate(Range)}, the device will choose values calibrated for the
         * quality on that device.
         *
         * <p>If not set, defaults to {@link #QUALITY_SELECTOR_AUTO}.
         */
        @NonNull
        public abstract Builder setQualitySelector(@NonNull QualitySelector qualitySelector);

        /**
         * Sets the frame rate.
         *
         * <p>If not set, defaults to {@link #FRAME_RATE_RANGE_AUTO}.
         */
        @NonNull
        public abstract Builder setFrameRate(@NonNull Range<Integer> frameRate);

        /**
         * Sets the bitrate.
         *
         * <p>If not set, defaults to {@link #BITRATE_RANGE_AUTO}.
         */
        @NonNull
        public abstract Builder setBitrate(@NonNull Range<Integer> bitrate);

        /**
         * Sets the aspect ratio.
         *
         * <p>Available values for aspect ratio are {@link AspectRatio#RATIO_16_9},
         * {@link AspectRatio#RATIO_4_3} and {@link AspectRatio#RATIO_DEFAULT}.
         *
         * <p>If not set, defaults to {@link AspectRatio#RATIO_DEFAULT}.
         */
        @NonNull
        abstract Builder setAspectRatio(@AspectRatio.Ratio int aspectRatio);

        /** Builds the VideoSpec instance. */
        @NonNull
        public abstract VideoSpec build();
    }
}
