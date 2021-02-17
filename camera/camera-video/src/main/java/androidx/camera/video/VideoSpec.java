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

import android.media.CamcorderProfile;
import android.util.Range;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Video specification that is options to config video encoding.
 */
@AutoValue
public abstract class VideoSpec {

    /**
     * Frame rate range representing no preference for frame rate.
     *
     * <p>Using this value with {@link Builder#setFrameRate(Range)} informs the video frame producer
     * it should choose any appropriate frame rate given the device and codec constraints.
     */
    public static final Range<Integer> FRAME_RATE_RANGE_AUTO = new Range<>(0,
            Integer.MAX_VALUE);

    /**
     * Bitrate range representing no preference for bitrate.
     *
     * <p>Using this value with {@link Builder#setBitrate(Range)} informs the video frame producer
     * it should choose any appropriate bitrate given the device and codec constraints.
     */
    public static final Range<Integer> BITRATE_RANGE_AUTO = new Range<>(0,
            Integer.MAX_VALUE);

    /**
     * Allow the video frame producer to choose video quality based on its current state.
     */
    public static final int VIDEO_QUALITY_AUTO = -1;
    /**
     * Choose the lowest video quality supported by the video frame producer.
     */
    public static final int VIDEO_QUALITY_LOWEST = CamcorderProfile.QUALITY_LOW;
    /**
     * Choose the highest video quality supported by the video frame producer.
     */
    public static final int VIDEO_QUALITY_HIGHEST = CamcorderProfile.QUALITY_HIGH;
    /**
     * Standard Definition (SD) 480p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 720 x 480 (480p) pixels.
     */
    public static final int VIDEO_QUALITY_SD = CamcorderProfile.QUALITY_480P;
    /**
     * High Definition (HD) video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 1280 x 720 (720p) pixels.
     */
    public static final int VIDEO_QUALITY_HD = CamcorderProfile.QUALITY_720P;
    /**
     * Full High Definition (FHD) 1080p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 1920 x 1080 (1080p) pixels.
     */
    public static final int VIDEO_QUALITY_FHD = CamcorderProfile.QUALITY_1080P;
    /**
     * Ultra High Definition (UHD) 2160p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 3840 x 2160 (2160p) pixels.
     */
    public static final int VIDEO_QUALITY_UHD = CamcorderProfile.QUALITY_2160P;


    /** @hide */
    @IntDef({VIDEO_QUALITY_AUTO, VIDEO_QUALITY_LOWEST, VIDEO_QUALITY_HIGHEST, VIDEO_QUALITY_SD,
            VIDEO_QUALITY_HD, VIDEO_QUALITY_FHD, VIDEO_QUALITY_UHD})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface VideoQuality {
    }

    // Restrict constructor to same package
    VideoSpec() {
    }

    /** Returns a build for this config. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_VideoSpec.Builder()
                .setVideoQuality(VIDEO_QUALITY_AUTO)
                .setFrameRate(FRAME_RATE_RANGE_AUTO)
                .setBitrate(BITRATE_RANGE_AUTO);
    }

    /**
     * Gets the video quality.
     *
     * @return the video quality. Possible values include {@link #VIDEO_QUALITY_AUTO},
     * {@link #VIDEO_QUALITY_LOWEST}, {@link #VIDEO_QUALITY_HIGHEST}, {@link #VIDEO_QUALITY_SD},
     * {@link #VIDEO_QUALITY_HD}, {@link #VIDEO_QUALITY_FHD}, or {@link #VIDEO_QUALITY_UHD}.
     */
    @VideoQuality
    public abstract int getVideoQuality();

    /** Gets the frame rate. */
    @NonNull
    public abstract Range<Integer> getFrameRate();

    /** Gets the bitrate. */
    @NonNull
    public abstract Range<Integer> getBitrate();

    /**
     * Returns a {@link Builder} instance with the same property values as this instance.
     */
    @NonNull
    public abstract Builder toBuilder();

    /** The builder of the {@link VideoSpec}. */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /**
         * Sets the video quality.
         *
         * <p>Video encoding parameters such as frame rate and bitrate will often be automatically
         * determined according to quality. If video parameters are not set directly (such as
         * through {@link #setFrameRate(Range)}, the device will choose values calibrated for the
         * quality on that device.
         *
         * <p>If not set, defaults to {@link #VIDEO_QUALITY_AUTO}.
         *
         * @param videoQuality the video quality. Possible values include
         * {@link #VIDEO_QUALITY_AUTO}, {@link #VIDEO_QUALITY_LOWEST},
         * {@link #VIDEO_QUALITY_HIGHEST}, {@link #VIDEO_QUALITY_SD}, {@link #VIDEO_QUALITY_HD},
         * {@link #VIDEO_QUALITY_FHD}, or {@link #VIDEO_QUALITY_UHD}.
         */
        @NonNull
        public abstract Builder setVideoQuality(@VideoQuality int videoQuality);

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

        /** Builds the VideoSpec instance. */
        @NonNull
        public abstract VideoSpec build();
    }
}
