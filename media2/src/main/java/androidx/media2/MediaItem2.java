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

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Pair;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * A class with information on a single media item with the metadata information. Here are use
 * cases.
 * <ul>
 * <li>Specify media items to {@link SessionPlayer2} for playback.
 * <li>Share media items across the processes.
 * </ul>
 * <p>
 * Subclasses of the session player may only accept certain subclasses of the media items. Check
 * the player documentation that you're interested in.
 * <p>
 * When it's shared across the processes, we cannot guarantee that they contain the right values
 * because media items are application dependent especially for the metadata.
 * <p>
 * When an object of the {@link MediaItem2}'s subclass is sent across the process between
 * {@link MediaSession2}/{@link MediaController2} or
 * {@link androidx.media2.MediaLibraryService2.MediaLibrarySession}/{@link MediaBrowser2}, the
 * object will sent as if it's {@link MediaItem2}. The recipient cannot get the object with the
 * subclasses' type. This will sanitize process specific information (e.g.
 * {@link java.io.FileDescriptor}, {@link android.content.Context}, etc).
 * <p>
 * This object isn't a thread safe.
 */
@VersionedParcelize(isCustom = true)
public class MediaItem2 extends CustomVersionedParcelable {
    // intentionally less than long.MAX_VALUE.
    // Declare this first to avoid 'illegal forward reference'.
    static final long LONG_MAX = 0x7ffffffffffffffL;

    /**
     * Used when a position is unknown.
     *
     * @see #getEndPosition()
     */
    public static final long POSITION_UNKNOWN = LONG_MAX;

    private static final String KEY_METADATA = "android.media.mediaitem2.metadata";

    @ParcelField(1)
    MediaMetadata2 mMetadata;
    @ParcelField(2)
    long mStartPositionMs = 0;
    @ParcelField(3)
    long mEndPositionMs = POSITION_UNKNOWN;

    @NonParcelField
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    @NonParcelField
    private final List<Pair<OnMetadataChangedListener, Executor>> mListeners = new ArrayList<>();

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
        this(builder.mMetadata, builder.mStartPositionMs, builder.mEndPositionMs);
    }

    MediaItem2(MediaItem2 item) {
        this(item.mMetadata, item.mStartPositionMs, item.mEndPositionMs);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem2(@Nullable MediaMetadata2 metadata, long startPositionMs, long endPositionMs) {
        if (startPositionMs > endPositionMs) {
            throw new IllegalStateException("Illegal start/end position: "
                    + startPositionMs + " : " + endPositionMs);
        }
        if (metadata != null && metadata.containsKey(MediaMetadata2.METADATA_KEY_DURATION)) {
            long durationMs = metadata.getLong(MediaMetadata2.METADATA_KEY_DURATION);
            if (durationMs != SessionPlayer2.UNKNOWN_TIME && endPositionMs != POSITION_UNKNOWN
                    && endPositionMs > durationMs) {
                throw new IllegalStateException("endPositionMs shouldn't be greater than"
                        + " duration in the metdata, endPositionMs=" + endPositionMs
                        + ", durationMs=" + durationMs);
            }
        }
        mMetadata = metadata;
        mStartPositionMs = startPositionMs;
        mEndPositionMs = endPositionMs;
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
        if (mMetadata != null) {
            bundle.putBundle(KEY_METADATA, mMetadata.toBundle());
        }
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
        final Bundle metadataBundle = bundle.getBundle(KEY_METADATA);
        final MediaMetadata2 metadata = metadataBundle != null
                ? MediaMetadata2.fromBundle(metadataBundle) : null;
        return new MediaItem2(metadata, 0, 0);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{mMetadata=").append(mMetadata);
        sb.append(", mStartPositionMs=").append(mStartPositionMs);
        sb.append(", mEndPositionMs=").append(mEndPositionMs);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Sets metadata. If the metadata is not {@code null}, its id should be matched with this
     * instance's media id.
     *
     * @param metadata metadata to update
     * @see MediaMetadata2#METADATA_KEY_MEDIA_ID
     */
    public void setMetadata(@Nullable MediaMetadata2 metadata) {
        if (metadata != null && !TextUtils.equals(getMediaId(), metadata.getMediaId())) {
            throw new IllegalArgumentException("metadata's id should be matched with the mediaId");
        }
        mMetadata = metadata;
        List<Pair<OnMetadataChangedListener, Executor>> listeners = new ArrayList<>();
        synchronized (mLock) {
            listeners.addAll(mListeners);
        }

        for (Pair<OnMetadataChangedListener, Executor> pair : listeners) {
            final OnMetadataChangedListener listener = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onMetadataChanged(MediaItem2.this);
                }
            });
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
     * @hide
     */
    // TODO: Remove
    @RestrictTo(LIBRARY)
    public @Nullable String getMediaId() {
        return mMetadata != null ? mMetadata.getString(MediaMetadata2.METADATA_KEY_MEDIA_ID) : null;
    }

    void addOnMetadataChangedListener(Executor executor, OnMetadataChangedListener listener) {
        synchronized (mLock) {
            for (Pair<OnMetadataChangedListener, Executor> pair : mListeners) {
                if (pair.first == listener) {
                    return;
                }
            }
            mListeners.add(new Pair<>(listener, executor));
        }
    }

    void removeOnMetadataChangedListener(OnMetadataChangedListener listener) {
        synchronized (mLock) {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                if (mListeners.get(i).first == listener) {
                    mListeners.remove(i);
                    return;
                }
            }
        }
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
        MediaMetadata2 mMetadata;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mStartPositionMs = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mEndPositionMs = POSITION_UNKNOWN;

        /**
         * Set the metadata of this instance. {@code null} for unset.
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
        /**
         * Default constructor
         */
        public Builder() {
            super();
        }
    }

    interface OnMetadataChangedListener {
        void onMetadataChanged(MediaItem2 item);
    }

    /**
     * @hide
     * @param isStream
     */
    @RestrictTo(LIBRARY)
    @Override
    public void onPreParceling(boolean isStream) {
        if (getClass() != MediaItem2.class) {
            throw new RuntimeException("MediaItem2's subclasses shouldn't be parcelized."
                    + " Use instead");
        }
        super.onPreParceling(isStream);
    }
}
