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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelize;

import java.io.IOException;

/**
 * Structure for media item for a file.
 * <p>
 * Users should use {@link Builder} to create {@link FileMediaItem}.
 * <p>
 * You cannot directly send this object across the process through {@link ParcelUtils}. See
 * {@link MediaItem} for detail.
 *
 * @see MediaItem
 */
@VersionedParcelize(isCustom = true)
public class FileMediaItem extends MediaItem {
    private static final String TAG = "FileMediaItem";
    /**
     * Used when the length of file descriptor is unknown.
     *
     * @see #getFileDescriptorLength()
     */
    public static final long FD_LENGTH_UNKNOWN = LONG_MAX;

    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ParcelFileDescriptor mPFD;
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mFDOffset = 0;
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mFDLength = FD_LENGTH_UNKNOWN;
    @NonParcelField
    Integer mRefCount = new Integer(0);
    @NonParcelField
    boolean mClosed;

    /**
     * Used for VersionedParcelable
     */
    FileMediaItem() {
        // no-op
    }

    FileMediaItem(Builder builder) {
        super(builder);
        mPFD = builder.mPFD;
        mFDOffset = builder.mFDOffset;
        mFDLength = builder.mFDLength;
    }

    /**
     * Returns the ParcelFileDescriptor of this media item.
     * @return the ParcelFileDescriptor of this media item
     */
    @NonNull
    public ParcelFileDescriptor getParcelFileDescriptor() {
        return mPFD;
    }

    /**
     * Returns the offset associated with the ParcelFileDescriptor of this media item.
     * It's meaningful only when it has been set by the {@link MediaItem.Builder}.
     * @return the offset associated with the ParcelFileDescriptor of this media item
     */
    public long getFileDescriptorOffset() {
        return mFDOffset;
    }

    /**
     * Returns the content length associated with the ParcelFileDescriptor of this media item.
     * {@link #FD_LENGTH_UNKNOWN} means same as the length of source content.
     * @return the content length associated with the ParcelFileDescriptor of this media item
     */
    public long getFileDescriptorLength() {
        return mFDLength;
    }

    /**
     * Increases reference count for underlying ParcelFileDescriptor.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void increaseRefCount() {
        synchronized (mRefCount) {
            if (mClosed) {
                Log.w(TAG, "ParcelFileDescriptorClient is already closed.");
                return;
            }
            mRefCount++;
        }
    }

    /**
     * Increases reference count for underlying ParcelFileDescriptor. The ParcelFileDescriptor will
     * be closed when the count becomes zero.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void decreaseRefCount() {
        synchronized (mRefCount) {
            if (mClosed) {
                Log.w(TAG, "ParcelFileDescriptorClient is already closed.");
                return;
            }
            if (--mRefCount <= 0) {
                try {
                    if (mPFD != null) {
                        mPFD.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close the ParcelFileDescriptor " + mPFD, e);
                } finally {
                    mClosed = true;
                }
            }
        }
    }

    /**
     * @return whether the underlying {@link ParcelFileDescriptor} is closed or not.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isClosed() {
        synchronized (mRefCount) {
            return mClosed;
        }
    }

    /**
     * Close the {@link ParcelFileDescriptor} of this {@link FileMediaItem}.
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(LIBRARY_GROUP)
    public void close() throws IOException {
        synchronized (mRefCount) {
            if (mPFD != null) {
                mPFD.close();
            }
            mClosed = true;
        }
    }

    /**
     * This Builder class simplifies the creation of a {@link FileMediaItem} object.
     */
    public static final class Builder extends MediaItem.Builder {

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        ParcelFileDescriptor mPFD;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mFDOffset = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mFDLength = FD_LENGTH_UNKNOWN;

        /**
         * Creates a new Builder object with a media item (ParcelFileDescriptor) to use. The
         * ParcelFileDescriptor must be seekable (N.B. a LocalSocket is not seekable).
         * <p>
         * If {@link FileMediaItem} is passed to {@link androidx.media2.player.MediaPlayer},
         * {@link androidx.media2.player.MediaPlayer} will
         * close the ParcelFileDescriptor.
         *
         * @param pfd the ParcelFileDescriptor for the file you want to play
         */
        public Builder(@NonNull ParcelFileDescriptor pfd) {
            Preconditions.checkNotNull(pfd);
            mPFD = pfd;
            mFDOffset = 0;
            mFDLength = FD_LENGTH_UNKNOWN;
        }

        /**
         * Sets the start offset of the file where the data to be played in bytes.
         * Any negative number for offset is treated as 0.
         *
         * @param offset the start offset of the file where the data to be played in bytes
         * @return this instance for chaining
         */
        @NonNull
        public Builder setFileDescriptorOffset(long offset) {
            if (offset < 0) {
                offset = 0;
            }
            mFDOffset = offset;
            return this;
        }

        /**
         * Sets the length of the data to be played in bytes.
         * Any negative number for length is treated as maximum length of the media item.
         *
         * @param length the length of the data to be played in bytes
         * @return this instance for chaining
         */
        @NonNull
        public Builder setFileDescriptorLength(long length) {
            if (length < 0) {
                length = FD_LENGTH_UNKNOWN;
            }
            mFDLength = length;
            return this;
        }

        // Override just to change return type.
        @Override
        @NonNull
        public Builder setMetadata(@Nullable MediaMetadata metadata) {
            return (Builder) super.setMetadata(metadata);
        }

        // Override just to change return type.
        @Override
        @NonNull
        public Builder setStartPosition(long position) {
            return (Builder) super.setStartPosition(position);
        }

        // Override just to change return type.
        @Override
        @NonNull
        public Builder setEndPosition(long position) {
            return (Builder) super.setEndPosition(position);
        }

        /**
         * @return A new FileMediaItem with values supplied by the Builder.
         */
        @Override
        @NonNull
        public FileMediaItem build() {
            return new FileMediaItem(this);
        }
    }
}
