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

import android.location.Location;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

/**
 * Options for configuring output destination for generating a recording.
 *
 * <p>A {@link PendingRecording} can be generated with {@link Recorder#prepareRecording} for
 * different types of output destination, such as {@link FileOutputOptions},
 * {@link FileDescriptorOutputOptions} and {@link MediaStoreOutputOptions}.
 *
 * @see FileOutputOptions
 * @see FileDescriptorOutputOptions
 * @see MediaStoreOutputOptions
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class OutputOptions {

    /** Represents an unbound file size. */
    public static final int FILE_SIZE_UNLIMITED = 0;

    /** Represents an unlimited duration. */
    public static final int DURATION_UNLIMITED = 0;

    private final OutputOptionsInternal mOutputOptionsInternal;

    OutputOptions(@NonNull OutputOptionsInternal outputOptionsInternal) {
        mOutputOptionsInternal = outputOptionsInternal;
    }

    /**
     * Gets the limit for the file size in bytes.
     *
     * @return the file size limit in bytes or {@link #FILE_SIZE_UNLIMITED} if it's unlimited.
     */
    @IntRange(from = 0)
    public long getFileSizeLimit() {
        return mOutputOptionsInternal.getFileSizeLimit();
    }

    /**
     * Returns a {@link Location} object representing the geographic location where the video was
     * recorded.
     *
     * @return the location object or {@code null} if no location was set.
     */
    @Nullable
    public Location getLocation() {
        return mOutputOptionsInternal.getLocation();
    }

    /**
     * Gets the limit for the video duration in milliseconds.
     *
     * @return the video duration limit in milliseconds or {@link #DURATION_UNLIMITED} if it's
     * unlimited.
     */
    @IntRange(from = 0)
    public long getDurationLimitMillis() {
        return mOutputOptionsInternal.getDurationLimitMillis();
    }

    /**
     * The builder of the {@link OutputOptions}.
     */
    @SuppressWarnings("unchecked") // Cast to type B
    abstract static class Builder<T extends OutputOptions, B> {

        final OutputOptionsInternal.Builder<?> mRootInternalBuilder;

        Builder(@NonNull OutputOptionsInternal.Builder<?> builder) {
            mRootInternalBuilder = builder;
            // Apply default value
            mRootInternalBuilder.setFileSizeLimit(FILE_SIZE_UNLIMITED);
            mRootInternalBuilder.setDurationLimitMillis(DURATION_UNLIMITED);
        }

        /**
         * Sets the limit for the file length in bytes.
         *
         * <p>When used with {@link Recorder} to generate recording, if the specified file size
         * limit is reached while the recording is being recorded, the recording will be
         * finalized with {@link VideoRecordEvent.Finalize#ERROR_FILE_SIZE_LIMIT_REACHED}.
         *
         * <p>If not set or set with zero, the file size will be {@linkplain #FILE_SIZE_UNLIMITED
         * unlimited}. If set with a negative value, an {@link IllegalArgumentException} will be
         * thrown.
         *
         * @param fileSizeLimitBytes the file size limit in bytes.
         * @return this Builder.
         * @throws IllegalArgumentException if the specified file size limit is negative.
         */
        @NonNull
        public B setFileSizeLimit(@IntRange(from = 0) long fileSizeLimitBytes) {
            Preconditions.checkArgument(fileSizeLimitBytes >= 0, "The specified file size limit "
                    + "can't be negative.");
            mRootInternalBuilder.setFileSizeLimit(fileSizeLimitBytes);
            return (B) this;
        }

        /**
         * Sets the limit for the video duration in milliseconds.
         *
         * <p>When used to generate recording with {@link Recorder}, if the specified duration
         * limit is reached while the recording is being recorded, the recording will be
         * finalized with {@link VideoRecordEvent.Finalize#ERROR_DURATION_LIMIT_REACHED}.
         *
         * <p>If not set or set with zero, the duration will be {@linkplain #DURATION_UNLIMITED
         * unlimited}. If set with a negative value, an {@link IllegalArgumentException} will be
         * thrown.
         *
         * @param durationLimitMillis the video duration limit in milliseconds.
         * @return this Builder.
         * @throws IllegalArgumentException if the specified duration limit is negative.
         */
        @NonNull
        public B setDurationLimitMillis(@IntRange(from = 0) long durationLimitMillis) {
            Preconditions.checkArgument(durationLimitMillis >= 0, "The specified duration limit "
                    + "can't be negative.");
            mRootInternalBuilder.setDurationLimitMillis(durationLimitMillis);
            return (B) this;
        }

        /**
         * Sets a {@link Location} object representing a geographic location where the video was
         * recorded.
         *
         * <p>When use with {@link Recorder}, the geographic location is stored in udta box if the
         * output format is MP4, and is ignored for other formats. The geographic location is
         * stored according to ISO-6709 standard.
         *
         * <p>If {@code null}, no location information will be saved with the video. Default
         * value is {@code null}.
         *
         * @throws IllegalArgumentException if the latitude of the location is not in the range
         * {@code [-90, 90]} or the longitude of the location is not in the range {@code [-180,
         * 180]}.
         */
        @NonNull
        public B setLocation(@Nullable Location location) {
            if (location != null) {
                Preconditions.checkArgument(
                        location.getLatitude() >= -90 && location.getLatitude() <= 90,
                        "Latitude must be in the range [-90, 90]");
                Preconditions.checkArgument(
                        location.getLongitude() >= -180 && location.getLongitude() <= 180,
                        "Longitude must be in the range [-180, 180]");
            }
            mRootInternalBuilder.setLocation(location);
            return (B) this;
        }

        /**
         * Builds the {@link OutputOptions} instance.
         */
        @NonNull
        abstract T build();
    }

    // A base class of a @AutoValue class
    abstract static class OutputOptionsInternal {

        @IntRange(from = 0)
        abstract long getFileSizeLimit();

        @IntRange(from = 0)
        abstract long getDurationLimitMillis();

        @Nullable
        abstract Location getLocation();

        // A base class of a @AutoValue.Builder class
        @SuppressWarnings("NullableProblems") // Nullable problem in AutoValue generated class
        abstract static class Builder<B> {

            @NonNull
            abstract B setFileSizeLimit(@IntRange(from = 0) long fileSizeLimitBytes);

            @NonNull
            abstract B setDurationLimitMillis(@IntRange(from = 0) long durationLimitMillis);

            @NonNull
            abstract B setLocation(@Nullable Location location);

            @NonNull
            abstract OutputOptionsInternal build();
        }
    }
}
