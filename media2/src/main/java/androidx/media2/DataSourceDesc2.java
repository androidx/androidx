/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Structure for data source descriptor. Used by {@link MediaItem2}.
 *
 * @see MediaItem2
 */
public abstract class DataSourceDesc2 {
    /* data source is type of MediaDataSource */
    public static final int TYPE_CALLBACK = 1;
    /* data source is type of FileDescriptor */
    public static final int TYPE_FD       = 2;
    /* data source is type of Uri */
    public static final int TYPE_URI      = 3;

    // intentionally less than long.MAX_VALUE.
    // Declare this first to avoid 'illegal forward reference'.
    static final long LONG_MAX = 0x7ffffffffffffffL;

    /**
     * Used when a position is unknown.
     *
     * @see #getEndPosition()
     */
    public static final long POSITION_UNKNOWN = LONG_MAX;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    String mMediaId;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mStartPositionMs = 0;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mEndPositionMs = POSITION_UNKNOWN;

    DataSourceDesc2(Builder builder) {
        if (builder.mStartPositionMs > builder.mEndPositionMs) {
            throw new IllegalStateException("Illegal start/end position: "
                    + builder.mStartPositionMs + " : " + builder.mEndPositionMs);
        }

        mMediaId = builder.mMediaId;
        mStartPositionMs = builder.mStartPositionMs;
        mEndPositionMs = builder.mEndPositionMs;
    }

    /**
     * Return the media Id of data source.
     * @return the media Id of data source
     */
    public @Nullable String getMediaId() {
        return mMediaId;
    }

    /**
     * Return the position in milliseconds at which the playback will start.
     * @return the position in milliseconds at which the playback will start
     */
    public long getStartPosition() {
        return mStartPositionMs;
    }

    /**
     * Return the position in milliseconds at which the playback will end.
     * {@link #POSITION_UNKNOWN} means ending at the end of source content.
     * @return the position in milliseconds at which the playback will end
     */
    public long getEndPosition() {
        return mEndPositionMs;
    }

    /**
     * Return the type of data source.
     * @return the type of data source
     */
    public abstract int getType();

    /**
     * Builder class for {@link DataSourceDesc2} objects.
     *
     * @param <T> The Builder of the derived classe.
     */
    public abstract static class Builder<T extends Builder> {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        String mMediaId;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mStartPositionMs = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mEndPositionMs = POSITION_UNKNOWN;

        /**
         * Constructs a new Builder with the defaults.
         */
        Builder() {
        }

        /**
         * Constructs a new Builder from a given {@link DataSourceDesc2} instance
         * @param dsd the {@link DataSourceDesc2} object whose data will be reused
         * in the new Builder.
         */
        Builder(@NonNull DataSourceDesc2 dsd) {
            mMediaId = dsd.mMediaId;
            mStartPositionMs = dsd.mStartPositionMs;
            mEndPositionMs = dsd.mEndPositionMs;
        }

        /**
         * Sets the media Id of this data source.
         *
         * @param mediaId the media Id of this data source
         * @return the same Builder instance.
         */
        public @NonNull T setMediaId(String mediaId) {
            mMediaId = mediaId;
            return (T) this;
        }

        /**
         * Sets the start position in milliseconds at which the playback will start.
         * Any negative number is treated as 0.
         *
         * @param position the start position in milliseconds at which the playback will start
         * @return the same Builder instance.
         *
         */
        public @NonNull T setStartPosition(long position) {
            if (position < 0) {
                position = 0;
            }
            mStartPositionMs = position;
            return (T) this;
        }

        /**
         * Sets the end position in milliseconds at which the playback will end.
         * Any negative number is treated as maximum length of the data source.
         *
         * @param position the end position in milliseconds at which the playback will end
         * @return the same Builder instance.
         */
        public @NonNull T setEndPosition(long position) {
            if (position < 0) {
                position = POSITION_UNKNOWN;
            }
            mEndPositionMs = position;
            return (T) this;
        }
    }
}
