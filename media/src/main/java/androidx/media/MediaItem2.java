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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

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
public class MediaItem2 {
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

    private static final String KEY_ID = "android.media.mediaitem2.id";
    private static final String KEY_FLAGS = "android.media.mediaitem2.flags";
    private static final String KEY_METADATA = "android.media.mediaitem2.metadata";
    private static final String KEY_UUID = "android.media.mediaitem2.uuid";

    private final String mId;
    private final int mFlags;
    private final UUID mUUID;
    private MediaMetadata2 mMetadata;
    private DataSourceDesc mDataSourceDesc;

    private MediaItem2(@NonNull String mediaId, @Nullable DataSourceDesc dsd,
            @Nullable MediaMetadata2 metadata, @Flags int flags) {
        this(mediaId, dsd, metadata, flags, null);
    }

    private MediaItem2(@NonNull String mediaId, @Nullable DataSourceDesc dsd,
            @Nullable MediaMetadata2 metadata, @Flags int flags, @Nullable UUID uuid) {
        if (mediaId == null) {
            throw new IllegalArgumentException("mediaId shouldn't be null");
        }
        if (metadata != null && !TextUtils.equals(mediaId, metadata.getMediaId())) {
            throw new IllegalArgumentException("metadata's id should be matched with the mediaid");
        }

        mId = mediaId;
        mDataSourceDesc = dsd;
        mMetadata = metadata;
        mFlags = flags;
        mUUID = (uuid == null) ? UUID.randomUUID() : uuid;
    }
    /**
     * Return this object as a bundle to share between processes.
     *
     * @return a new bundle instance
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ID, mId);
        bundle.putInt(KEY_FLAGS, mFlags);
        if (mMetadata != null) {
            bundle.putBundle(KEY_METADATA, mMetadata.toBundle());
        }
        bundle.putString(KEY_UUID, mUUID.toString());
        return bundle;
    }

    /**
     * Create a MediaItem2 from the {@link Bundle}.
     *
     * @param bundle The bundle which was published by {@link MediaItem2#toBundle()}.
     * @return The newly created MediaItem2
     */
    public static MediaItem2 fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        final String uuidString = bundle.getString(KEY_UUID);
        return fromBundle(bundle, UUID.fromString(uuidString));
    }

    /**
     * Create a MediaItem2 from the {@link Bundle} with the specified {@link UUID}.
     * If {@link UUID}
     * can be null for creating new.
     *
     * @param bundle The bundle which was published by {@link MediaItem2#toBundle()}.
     * @param uuid A {@link UUID} to override. Can be {@link null} for override.
     * @return The newly created MediaItem2
     */
    static MediaItem2 fromBundle(@NonNull Bundle bundle, @Nullable UUID uuid) {
        if (bundle == null) {
            return null;
        }
        final String id = bundle.getString(KEY_ID);
        final Bundle metadataBundle = bundle.getBundle(KEY_METADATA);
        final MediaMetadata2 metadata = metadataBundle != null
                ? MediaMetadata2.fromBundle(metadataBundle) : null;
        final int flags = bundle.getInt(KEY_FLAGS);
        return new MediaItem2(id, null, metadata, flags, uuid);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MediaItem2{");
        sb.append("mFlags=").append(mFlags);
        sb.append(", mMetadata=").append(mMetadata);
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
     * Returns whether this item is browsable.
     * @see #FLAG_BROWSABLE
     */
    public boolean isBrowsable() {
        return (mFlags & FLAG_BROWSABLE) != 0;
    }

    /**
     * Returns whether this item is playable.
     * @see #FLAG_PLAYABLE
     */
    public boolean isPlayable() {
        return (mFlags & FLAG_PLAYABLE) != 0;
    }

    /**
     * Set a metadata. If the metadata is not null, its id should be matched with this instance's
     * media id.
     *
     * @param metadata metadata to update
     */
    public void setMetadata(@Nullable MediaMetadata2 metadata) {
        if (metadata != null && !TextUtils.equals(mId, metadata.getMediaId())) {
            throw new IllegalArgumentException("metadata's id should be matched with the mediaId");
        }
        mMetadata = metadata;
    }

    /**
     * Returns the metadata of the media.
     */
    public @Nullable MediaMetadata2 getMetadata() {
        return mMetadata;
    }

    /**
     * Returns the media id for this item.
     */
    public /*@NonNull*/ String getMediaId() {
        return mId;
    }

    /**
     * Return the {@link DataSourceDesc}
     * <p>
     * Can be {@code null} if the MediaItem2 came from another process and anonymized
     *
     * @return data source descriptor
     */
    public @Nullable DataSourceDesc getDataSourceDesc() {
        return mDataSourceDesc;
    }

    @Override
    public int hashCode() {
        return mUUID.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MediaItem2)) {
            return false;
        }
        MediaItem2 other = (MediaItem2) obj;
        return mUUID.equals(other.mUUID);
    }

    /**
     * Build {@link MediaItem2}
     */
    public static final class Builder {
        private @Flags int mFlags;
        private String mMediaId;
        private MediaMetadata2 mMetadata;
        private DataSourceDesc mDataSourceDesc;

        /**
         * Constructor for {@link Builder}
         *
         * @param flags
         */
        public Builder(@Flags int flags) {
            mFlags = flags;
        }

        /**
         * Set the media id of this instance. {@code null} for unset.
         * <p>
         * Media id is used to identify a media contents between session and controller.
         * <p>
         * If the metadata is set with the {@link #setMetadata(MediaMetadata2)} and it has
         * media id, id from {@link #setMediaId(String)} will be ignored and metadata's id will be
         * used instead. If the id isn't set neither by {@link #setMediaId(String)} nor
         * {@link #setMetadata(MediaMetadata2)}, id will be automatically generated.
         *
         * @param mediaId media id
         * @return this instance for chaining
         */
        public Builder setMediaId(@Nullable String mediaId) {
            mMediaId = mediaId;
            return this;
        }

        /**
         * Set the metadata of this instance. {@code null} for unset.
         * <p>
         * If the metadata is set with the {@link #setMetadata(MediaMetadata2)} and it has
         * media id, id from {@link #setMediaId(String)} will be ignored and metadata's id will be
         * used instead. If the id isn't set neither by {@link #setMediaId(String)} nor
         * {@link #setMetadata(MediaMetadata2)}, id will be automatically generated.
         *
         * @param metadata metadata
         * @return this instance for chaining
         */
        public Builder setMetadata(@Nullable MediaMetadata2 metadata) {
            mMetadata = metadata;
            return this;
        }

        /**
         * Set the data source descriptor for this instance. {@code null} for unset.
         *
         * @param dataSourceDesc data source descriptor
         * @return this instance for chaining
         */
        public Builder setDataSourceDesc(@Nullable DataSourceDesc dataSourceDesc) {
            mDataSourceDesc = dataSourceDesc;
            return this;
        }

        /**
         * Build {@link MediaItem2}.
         *
         * @return a new {@link MediaItem2}.
         */
        public MediaItem2 build() {
            String id = (mMetadata != null)
                    ? mMetadata.getString(MediaMetadata2.METADATA_KEY_MEDIA_ID) : null;
            if (id == null) {
                id = (mMediaId != null) ? mMediaId : toString();
            }
            return new MediaItem2(id, mDataSourceDesc, mMetadata, mFlags);
        }
    }
}
