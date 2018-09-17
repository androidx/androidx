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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * A class with information on a single media item with the metadata information.
 * Media item are application dependent so we cannot guarantee that they contain the right values.
 * <p>
 * When it's sent to a controller or browser, it's anonymized and data descriptor wouldn't be sent.
 * <p>
 * This object isn't a thread safe.
 */
@VersionedParcelize
public class MediaItem2 implements VersionedParcelable {
    // intentionally less than long.MAX_VALUE.
    // Declare this first to avoid 'illegal forward reference'.
    static final long LONG_MAX = 0x7ffffffffffffffL;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = { FLAG_BROWSABLE, FLAG_PLAYABLE })
    public @interface Flags { }

    /**
     * Flag: Indicates that the item has children of its own.
     */
    public static final int FLAG_BROWSABLE = 1 << 0;

    /**
     * Flag: Indicates that the item is playable.
     * <p>
     * The id of this item may be passed to
     * {@link MediaController2#playFromMediaId(String, Bundle)}
     */
    public static final int FLAG_PLAYABLE = 1 << 1;

    /**
     * Used when a position is unknown.
     *
     * @see #getEndPosition()
     */
    public static final long POSITION_UNKNOWN = LONG_MAX;

    private static final String KEY_ID = "android.media.mediaitem2.id";
    private static final String KEY_FLAGS = "android.media.mediaitem2.flags";
    private static final String KEY_METADATA = "android.media.mediaitem2.metadata";
    private static final String KEY_UUID = "android.media.mediaitem2.uuid";

    @ParcelField(1)
    String mMediaId;
    @ParcelField(2)
    int mFlags;
    @ParcelField(3)
    ParcelUuid mParcelUuid;
    @ParcelField(4)
    MediaMetadata2 mMetadata;
    @ParcelField(5)
    long mStartPositionMs = 0;
    @ParcelField(6)
    long mEndPositionMs = POSITION_UNKNOWN;
    @ParcelField(7)
    long mDurationMs = SessionPlayer2.UNKNOWN_TIME;

    /**
     * Used for VersionedParcelable
     */
    MediaItem2() {
    }

    /**
     * Used by {@link MediaItem2.Builder} and its subclasses
     */
    // Note: Needs to be protected when we want to allow 3rd party player to define customized
    //       MediaItem2.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem2(BuilderBase builder) {
        this(builder.mUuid, builder.mMediaId, builder.mFlags, builder.mMetadata,
                builder.mStartPositionMs, builder.mEndPositionMs, builder.mDurationMs);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem2(@Nullable UUID uuid, @Nullable String mediaId, Integer flags,
            @Nullable MediaMetadata2 metadata, long startPositionMs, long endPositionMs,
            long durationMs) {
        if (startPositionMs > endPositionMs) {
            throw new IllegalStateException("Illegal start/end position: "
                    + startPositionMs + " : " + endPositionMs);
        }
        if (durationMs != SessionPlayer2.UNKNOWN_TIME && endPositionMs != POSITION_UNKNOWN
                && endPositionMs > durationMs) {
            throw new IllegalStateException("endPositionMs shouldn't be greater than durationMs: "
                    + " endPositionMs=" + endPositionMs + ", durationMs=" + durationMs);
        }

        mParcelUuid = new ParcelUuid((uuid != null) ? uuid : UUID.randomUUID());
        if (mediaId != null || durationMs > 0 || flags != null) {
            MediaMetadata2.Builder builder = metadata != null
                    ? new MediaMetadata2.Builder(metadata) : new MediaMetadata2.Builder();
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
     * Return this object as a bundle to share between processes.
     *
     * @return a new bundle instance
     * @hide
     */
    // TODO(jaewan): Remove
    public @NonNull Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ID, mMediaId);
        bundle.putInt(KEY_FLAGS, mFlags);
        if (mMetadata != null) {
            bundle.putBundle(KEY_METADATA, mMetadata.toBundle());
        }
        bundle.putParcelable(KEY_UUID, mParcelUuid);
        return bundle;
    }

    /**
     * Create a MediaItem2 from the {@link Bundle}.
     *
     * @param bundle The bundle which was published by {@link MediaItem2#toBundle()}.
     * @return The newly created MediaItem2. Can be {@code null} for {@code null} bundle.
     * @hide
     */
    // TODO(jaewan): Remove
    public static @Nullable MediaItem2 fromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        final ParcelUuid parcelUuid = bundle.getParcelable(KEY_UUID);
        return fromBundle(bundle, parcelUuid);
    }

    /**
     * Create a MediaItem2 from the {@link Bundle} with the specified {@link UUID} string.
     * <p>
     * {@link UUID} string can be null if it want to generate new one.
     *
     * @param bundle The bundle which was published by {@link MediaItem2#toBundle()}.
     * @param parcelUuid A {@link ParcelUuid} string to override. Can be {@link null} for override.
     * @return The newly created MediaItem2
     */
    static MediaItem2 fromBundle(@NonNull Bundle bundle, @Nullable ParcelUuid parcelUuid) {
        if (bundle == null) {
            return null;
        }
        final UUID uuid = (parcelUuid != null) ? parcelUuid.getUuid() : null;
        final String id = bundle.getString(KEY_ID);
        final Bundle metadataBundle = bundle.getBundle(KEY_METADATA);
        final MediaMetadata2 metadata = metadataBundle != null
                ? MediaMetadata2.fromBundle(metadataBundle) : null;
        final int flags = bundle.getInt(KEY_FLAGS);
        return new MediaItem2(uuid, id, flags, metadata, 0, 0, 0);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MediaItem2{");
        sb.append("mMediaId=").append(mMediaId);
        sb.append(", mFlags=").append(mFlags);
        sb.append(", mMetadata=").append(mMetadata);
        sb.append(", mStartPositionMs=").append(mStartPositionMs);
        sb.append(", mEndPositionMs=").append(mEndPositionMs);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Gets the flags of the item.
     */
    public @Flags int getFlags() {
        return mFlags;
    }

    /**
     * Checks whether this item is browsable.
     * @see #FLAG_BROWSABLE
     */
    public boolean isBrowsable() {
        return (mFlags & FLAG_BROWSABLE) != 0;
    }

    /**
     * Checks whether this item is playable.
     * @see #FLAG_PLAYABLE
     */
    public boolean isPlayable() {
        return (mFlags & FLAG_PLAYABLE) != 0;
    }

    /**
     * Sets a metadata. If the metadata is not {@code null}, its id should be matched with this
     * instance's media id.
     *
     * @param metadata metadata to update
     */
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
     * Gets the metadata of the media.
     *
     * @return metadata from the session
     */
    public @Nullable MediaMetadata2 getMetadata() {
        return mMetadata;
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
     * Gets the media id for this item. If it's not {@code null}, it's a persistent unique key
     * for the underlying media content.
     *
     * @return media Id from the session
     */
    public @Nullable String getMediaId() {
        return mMediaId;
    }

    @Override
    public int hashCode() {
        return mParcelUuid.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof MediaItem2)) {
            return false;
        }
        MediaItem2 other = (MediaItem2) obj;
        return mParcelUuid.equals(other.mParcelUuid);
    }

    UUID getUuid() {
        return mParcelUuid.getUuid();
    }

    /**
     * Base builder for {@link MediaItem2} and its subclass.
     *
     * @param <T> builder class
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static class BuilderBase<T extends BuilderBase> {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @Flags int mFlags;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        String mMediaId;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        MediaMetadata2 mMetadata;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        UUID mUuid;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mStartPositionMs = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mEndPositionMs = POSITION_UNKNOWN;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mDurationMs = SessionPlayer2.UNKNOWN_TIME;

        /**
         * Constructs a new {@link T}.
         *
         * @param flags flags whether it's playable and/or browsable.
         * @see #FLAG_BROWSABLE
         * @see #FLAG_PLAYABLE
         */
        public BuilderBase(@Flags int flags) {
            mFlags = flags;
        }

        /**
         * Set the media id of this instance. {@code null} for unset.
         * <p>
         * If used, this should be a persistent unique key for the underlying content so session
         * and controller can uniquely identify a media content.
         * <p>
         * If the metadata is set with the {@link #setMetadata(MediaMetadata2)} and it has
         * media id, id from {@link #setMediaId(String)} will be ignored and metadata's id will be
         * used instead.
         *
         * @param mediaId media id
         * @return this instance for chaining
         */
        public @NonNull T setMediaId(@Nullable String mediaId) {
            mMediaId = mediaId;
            return (T) this;
        }

        /**
         * Set the metadata of this instance. {@code null} for unset.
         * <p>
         * If the metadata is set with the {@link #setMetadata(MediaMetadata2)} and it has
         * media id, id from {@link #setMediaId(String)} will be ignored and metadata's id will be
         * used instead.
         *
         * @param metadata metadata
         * @return this instance for chaining
         */
        public @NonNull T setMetadata(@Nullable MediaMetadata2 metadata) {
            mMetadata = metadata;
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
         * Any negative number is treated as maximum length of the media item.
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

        T setUuid(UUID uuid) {
            mUuid = uuid;
            return (T) this;
        }

        /**
         * Build {@link MediaItem2}.
         *
         * @return a new {@link MediaItem2}.
         */
        public @NonNull MediaItem2 build() {
            return new MediaItem2(this);
        }
    }

    /**
     * Builder for {@link MediaItem2}.
     */
    public static class Builder extends BuilderBase<BuilderBase> {
        public Builder(@Flags int flags) {
            super(flags);
        }
    }
}
