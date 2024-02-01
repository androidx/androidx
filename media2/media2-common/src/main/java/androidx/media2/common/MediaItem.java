/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.text.TextUtils;
import android.util.Log;

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
 *
 * <ul>
 *   <li>Specify media items to {@link SessionPlayer} for playback.
 *   <li>Share media items across the processes.
 * </ul>
 *
 * <p>Subclasses of the session player may only accept certain subclasses of the media items. Check
 * the player documentation that you're interested in.
 *
 * <p>When it's shared across the processes, we cannot guarantee that they contain the right values
 * because media items are application dependent especially for the metadata.
 *
 * <p>When an object of the {@link MediaItem}'s subclass is sent across the process between {@link
 * androidx.media2.session.MediaSession}/{@link androidx.media2.session.MediaController} or {@link
 * androidx.media2.session.MediaLibraryService.MediaLibrarySession}/ {@link
 * androidx.media2.session.MediaBrowser}, the object will sent as if it's {@link MediaItem}. The
 * recipient cannot get the object with the subclasses' type. This will sanitize process specific
 * information (e.g. {@link java.io.FileDescriptor}, {@link android.content.Context}, etc).
 *
 * <p>This object is thread safe.
 *
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
@Deprecated
@VersionedParcelize(isCustom = true)
public class MediaItem extends CustomVersionedParcelable {
    private static final String TAG = "MediaItem";

    // intentionally less than long.MAX_VALUE.
    // Declare this first to avoid 'illegal forward reference'.
    static final long LONG_MAX = 0x7ffffffffffffffL;

    /**
     * Used when a position is unknown.
     *
     * @see #getEndPosition()
     */
    public static final long POSITION_UNKNOWN = LONG_MAX;

    @NonParcelField
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    @ParcelField(1)
    MediaMetadata mMetadata;
    @ParcelField(2)
    long mStartPositionMs = 0;
    @ParcelField(3)
    long mEndPositionMs = POSITION_UNKNOWN;

    // WARNING: Adding a new ParcelField may break old library users (b/152830728)

    @GuardedBy("mLock")
    @NonParcelField
    private final List<Pair<OnMetadataChangedListener, Executor>> mListeners = new ArrayList<>();

    /**
     * Used for VersionedParcelable
     */
    MediaItem() {
    }

    /**
     * Used by {@link MediaItem.Builder} and its subclasses
     */
    // Note: Needs to be protected when we want to allow 3rd party player to define customized
    //       MediaItem.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem(Builder builder) {
        this(builder.mMetadata, builder.mStartPositionMs, builder.mEndPositionMs);
    }

    MediaItem(MediaItem item) {
        this(item.mMetadata, item.mStartPositionMs, item.mEndPositionMs);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem(@Nullable MediaMetadata metadata, long startPositionMs, long endPositionMs) {
        if (startPositionMs > endPositionMs) {
            throw new IllegalStateException("Illegal start/end position: "
                    + startPositionMs + " : " + endPositionMs);
        }
        if (metadata != null && metadata.containsKey(MediaMetadata.METADATA_KEY_DURATION)) {
            long durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            if (durationMs != SessionPlayer.UNKNOWN_TIME && endPositionMs != POSITION_UNKNOWN
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        synchronized (mLock) {
            sb.append("{Media Id=").append(getMediaId());
            sb.append(", mMetadata=").append(mMetadata);
            sb.append(", mStartPositionMs=").append(mStartPositionMs);
            sb.append(", mEndPositionMs=").append(mEndPositionMs);
            sb.append('}');
        }
        return sb.toString();
    }

    /**
     * Sets metadata. If the metadata is not {@code null}, its id should be matched with this
     * instance's media id.
     *
     * @param metadata metadata to update
     * @see MediaMetadata#METADATA_KEY_MEDIA_ID
     */
    public void setMetadata(@Nullable MediaMetadata metadata) {
        List<Pair<OnMetadataChangedListener, Executor>> listeners = new ArrayList<>();
        synchronized (mLock) {
            if (mMetadata == metadata) {
                return;
            }
            if (mMetadata != null && metadata != null
                    && !TextUtils.equals(getMediaId(), metadata.getMediaId())) {
                Log.w(TAG, "MediaItem's media ID shouldn't be changed");
                return;
            }
            mMetadata = metadata;
            listeners.addAll(mListeners);
        }

        for (Pair<OnMetadataChangedListener, Executor> pair : listeners) {
            final OnMetadataChangedListener listener = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onMetadataChanged(MediaItem.this, metadata);
                }
            });
        }
    }

    /**
     * Gets the metadata of the media.
     *
     * @return metadata from the session
     */
    @Nullable
    public MediaMetadata getMetadata() {
        synchronized (mLock) {
            return mMetadata;
        }
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
    @RestrictTo(LIBRARY_GROUP)
    @Nullable
    public String getMediaId() {
        synchronized (mLock) {
            return mMetadata != null
                    ? mMetadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) : null;
        }
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP)
    public void addOnMetadataChangedListener(
            Executor executor, OnMetadataChangedListener listener) {
        synchronized (mLock) {
            for (Pair<OnMetadataChangedListener, Executor> pair : mListeners) {
                if (pair.first == listener) {
                    return;
                }
            }
            mListeners.add(new Pair<>(listener, executor));
        }
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP)
    public void removeOnMetadataChangedListener(OnMetadataChangedListener listener) {
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
     * Builder for {@link MediaItem}.
     *
     * @deprecated androidx.media2 is deprecated. Please migrate to <a
     *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
     */
    @Deprecated
    public static class Builder {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
                MediaMetadata mMetadata;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mStartPositionMs = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mEndPositionMs = POSITION_UNKNOWN;

        /**
         * Default constructor
         */
        public Builder() {
        }

        /**
         * Set the metadata of this instance. {@code null} for unset.
         *
         * @param metadata metadata
         * @return this instance for chaining
         */
        @NonNull
        public Builder setMetadata(@Nullable MediaMetadata metadata) {
            mMetadata = metadata;
            return this;
        }

        /**
         * Sets the start position in milliseconds at which the playback will start.
         * Any negative number is treated as 0.
         *
         * @param position the start position in milliseconds at which the playback will start
         * @return this instance for chaining
         */
        @NonNull
        public Builder setStartPosition(long position) {
            if (position < 0) {
                position = 0;
            }
            mStartPositionMs = position;
            return this;
        }

        /**
         * Sets the end position in milliseconds at which the playback will end.
         * Any negative number is treated as maximum length of the media item.
         *
         * @param position the end position in milliseconds at which the playback will end
         * @return this instance for chaining
         */
        @NonNull
        public Builder setEndPosition(long position) {
            if (position < 0) {
                position = POSITION_UNKNOWN;
            }
            mEndPositionMs = position;
            return this;
        }

        /**
         * Build {@link MediaItem}.
         *
         * @return a new {@link MediaItem}.
         */
        @NonNull
        public MediaItem build() {
            return new MediaItem(this);
        }
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP)
    public interface OnMetadataChangedListener {
        /**
         * Called when a media item's metadata is changed.
         */
        void onMetadataChanged(@NonNull MediaItem item,
                @Nullable MediaMetadata metadata);
    }

    /**
     */
    @RestrictTo(LIBRARY)
    @Override
    public void onPreParceling(boolean isStream) {
        if (getClass() != MediaItem.class) {
            throw new RuntimeException("MediaItem's subclasses shouldn't be parcelized.");
        }
        super.onPreParceling(isStream);
    }
}
