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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.media2.MediaMetadata2.FLAG_BROWSABLE;
import static androidx.media2.MediaMetadata2.FLAG_PLAYABLE;

import android.os.ParcelUuid;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * Structure for data source descriptor. Used by {@link MediaItem2}.
 *
 * @see MediaItem2
 */
// TODO(jaewan): Rename this to the MediaItem2 after removing current MediaItem2
// TODO(jaewan): Make it versioned parcelable
public abstract class DataSourceDesc2 {

    // intentionally less than long.MAX_VALUE.
    // Declare this first to avoid 'illegal forward reference'.
    static final long LONG_MAX = 0x7ffffffffffffffL;

    /**
     * Used when a position is unknown.
     *
     * @see #getEndPosition()
     */
    public static final long POSITION_UNKNOWN = LONG_MAX;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = { FLAG_BROWSABLE, FLAG_PLAYABLE })
    public @interface Flags { }

    // TODO(jaewan): Add @ParcelField
    int mFlags;
    ParcelUuid mParcelUuid;
    MediaMetadata2 mMetadata;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    String mMediaId;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mStartPositionMs = 0;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mEndPositionMs = POSITION_UNKNOWN;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mDurationMs = MediaPlayerConnector.UNKNOWN_TIME;

    /**
     * Used by {@link Builder} and its subclasses
     */
    // Note: Needs to be protected when we want to allow 3rd party player to define customized
    //       MediaItem2.
    DataSourceDesc2(Builder builder) {
        this(builder.mUuid, builder.mMediaId, builder.mFlags, builder.mMetadata,
                builder.mStartPositionMs, builder.mEndPositionMs, builder.mDurationMs);
    }

    DataSourceDesc2(@Nullable UUID uuid, @Nullable String mediaId, Integer flags,
            @Nullable MediaMetadata2 metadata, long startPositionMs, long endPositionMs,
            long durationMs) {
        if (startPositionMs > endPositionMs) {
            throw new IllegalStateException("Illegal start/end position: "
                    + startPositionMs + " : " + endPositionMs);
        }
        if (mDurationMs != MediaPlayerConnector.UNKNOWN_TIME && endPositionMs != POSITION_UNKNOWN
                && endPositionMs > durationMs) {
            throw new IllegalStateException("endPositionMs shouldn't be greater than durationMs: "
                    + " endPositionMs=" + endPositionMs + ", durationMs=" + durationMs);
        }

        mParcelUuid = new ParcelUuid((uuid != null) ? uuid : UUID.randomUUID());
        if (mediaId != null || durationMs > 0 || flags != null) {
            MediaMetadata2.Builder builder = metadata != null ?
                    new MediaMetadata2.Builder(metadata) : new MediaMetadata2.Builder();
            if (mediaId != null) {
                builder.putString(MediaMetadata2.METADATA_KEY_MEDIA_ID, mediaId);
            }
            if (durationMs > 0) {
                builder.putLong(MediaMetadata2.METADATA_KEY_DURATION, durationMs);
            }
            if (flags != null) {
                builder.putLong(MediaMetadata2.METADATA_KEY_FLAGS, flags);
            }
            metadata = builder.build();
        }
        if (metadata != null) {
            mediaId = metadata.getString(MediaMetadata2.METADATA_KEY_MEDIA_ID);
            durationMs = metadata.getLong(MediaMetadata2.METADATA_KEY_DURATION);
            flags = (int) metadata.getLong(MediaMetadata2.METADATA_KEY_FLAGS);
        }
        mMediaId = mediaId;
        mFlags = flags != null ? flags : 0;
        mMetadata = metadata;
        mStartPositionMs = startPositionMs;
        mEndPositionMs = endPositionMs;
        mDurationMs = durationMs;
    }

    /**
     * Gets the flags of the item.
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public @Flags int getFlags() {
        return mFlags;
    }

    /**
     * Returns whether this item is browsable.
     * @see MediaMetadata2#FLAG_BROWSABLE
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public boolean isBrowsable() {
        return (mFlags & FLAG_BROWSABLE) != 0;
    }

    /**
     * Returns whether this item is playable.
     * @see MediaMetadata2#FLAG_PLAYABLE
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public boolean isPlayable() {
        return (mFlags & FLAG_PLAYABLE) != 0;
    }

    /**
     * Sets a metadata. If the metadata is not {@code null}, its id should be matched with this
     * instance's media id.
     *
     * @param metadata metadata to update
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public void setMetadata(@Nullable MediaMetadata2 metadata) {
        if (metadata != null && !TextUtils.equals(mMediaId, metadata.getMediaId())) {
            throw new IllegalArgumentException("metadata's id should be matched with the mediaId");
        }
        mMetadata = metadata;
        if (metadata != null) {
            mDurationMs = metadata.getLong(MediaMetadata2.METADATA_KEY_DURATION);
        }
    }

    /**
     * Returns the metadata of the media.
     *
     * @return metadata from the session
     * @hide
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public @Nullable MediaMetadata2 getMetadata() {
        return mMetadata;
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
     * @hide
     * @return
     */
    // TODO(jaewan): Unhide this
    @RestrictTo(LIBRARY)
    public long getDuration() {
        return mDurationMs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataSourceDesc2{");
        sb.append("mMediaId=").append(mMediaId);
        sb.append(", mFlags=").append(mFlags);
        sb.append(", mMetadata=").append(mMetadata);
        sb.append(", mStartPositionMs=").append(mStartPositionMs);
        sb.append(", mEndPositionMs=").append(mEndPositionMs);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return mParcelUuid.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof DataSourceDesc2)) {
            return false;
        }
        DataSourceDesc2 other = (DataSourceDesc2) obj;
        return mParcelUuid.equals(other.mParcelUuid);
    }

    UUID getUuid() {
        return mParcelUuid.getUuid();
    }

    /**
     * Builder class for {@link DataSourceDesc2} objects.
     *
     * @param <T> The Builder of the derived classe.
     */
    // TODO(jaewan): remove abstract
    public abstract static class Builder<T extends Builder> {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @Flags Integer mFlags;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        MediaMetadata2 mMetadata;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        UUID mUuid;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        String mMediaId;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mStartPositionMs = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mEndPositionMs = POSITION_UNKNOWN;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mDurationMs = MediaPlayerConnector.UNKNOWN_TIME;

        /**
         * Constructs a new Builder with the defaults.
         */
        Builder() {
            this(null);
        }

        /**
         * @param metadata
         * @hide
         */
        // Note: Setting the metadata is only available in the constructor, because setMediaId(),
        //       setFlags(), and setDuration() change the metadata contents. If we allow so,
        //       syntax becomes unclear when setMediaId(id) and then setMetadata(null) is called.
        // TODO(jaewan): Unhide
        @RestrictTo(LIBRARY)
        public Builder(@Nullable MediaMetadata2 metadata) {
            mMetadata = metadata;
        }

        /**
         * Sets the media Id of this data source.
         *
         * @param mediaId the media Id of this data source
         * @return the same Builder instance.
         */
        public @NonNull T setMediaId(@Nullable String mediaId) {
            mMediaId = mediaId;
            return (T) this;
        }

        /**
         * Sets the flags whether it can be playable and/or browable (i.e. has children media
         * contents).
         *
         * @param flags flags
         * @return the same Builder instance
         * @hide
         */
        // TODO(jaewan): Unhide this
        @RestrictTo(LIBRARY)
        public @NonNull T setFlags(@Flags int flags) {
            mFlags = flags;
            return (T) this;
        }

        /**
         * Sets the start position in milliseconds at which the playback will start.
         * Any negative number is treated as 0.
         *
         * @param position the start position in milliseconds at which the playback will start
         * @return the same Builder instance.
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

        /**
         * Sets the duration of the playback in milliseconds.
         * Any negative number is considered as unknown duration.
         *
         * @param durationMs
         * @return the same Builder instance.
         * @hide
         */
        @RestrictTo(LIBRARY)
        public @NonNull T setDuration(long durationMs) {
            if (durationMs < 0) {
                durationMs = MediaPlayerConnector.UNKNOWN_TIME;
            }
            mDurationMs = durationMs;
            return (T) this;
        }

        @NonNull T setUuid(UUID uuid) {
            mUuid = uuid;
            return (T) this;
        }

        /**
         * @hide
         */
        // TODO(unhide this)
        @RestrictTo(LIBRARY)
        public DataSourceDesc2 build() {
            return new DataSourceDesc2(this) { };
        }
    }
}
