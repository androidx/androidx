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

    private final OutputOptionsInternal mOutputOptionsInternal;

    OutputOptions(@NonNull OutputOptionsInternal outputOptionsInternal) {
        mOutputOptionsInternal = outputOptionsInternal;
    }

    /**
     * Gets the limit for the file size in bytes.
     *
     * @return the file size limit in bytes.
     */
    public long getFileSizeLimit() {
        return mOutputOptionsInternal.getFileSizeLimit();
    }

    /**
     * Returns a {@link Location} object representing the geographic location where the video was
     * recorded.
     *
     * @return The location object or {@code null} if no location was set.
     */
    @Nullable
    public Location getLocation() {
        return mOutputOptionsInternal.getLocation();
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
        }

        /**
         * Sets the limit for the file length in bytes.
         *
         * <p>If not set, defaults to {@link #FILE_SIZE_UNLIMITED}.
         */
        @NonNull
        public B setFileSizeLimit(long bytes) {
            mRootInternalBuilder.setFileSizeLimit(bytes);
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
         * [-90, 90] or the longitude of the location is not in the range [-180, 180].
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

        abstract long getFileSizeLimit();

        @Nullable
        abstract Location getLocation();

        // A base class of a @AutoValue.Builder class
        @SuppressWarnings("NullableProblems") // Nullable problem in AutoValue generated class
        abstract static class Builder<B> {

            @NonNull
            abstract B setFileSizeLimit(long fileSizeLimitBytes);

            @NonNull
            abstract B setLocation(@Nullable Location location);

            @NonNull
            abstract OutputOptionsInternal build();
        }
    }
}
