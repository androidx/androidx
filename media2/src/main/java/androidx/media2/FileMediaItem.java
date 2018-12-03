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

import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelize;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
    Set<Object> mClientMap = new HashSet<Object>();
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
    public @NonNull ParcelFileDescriptor getParcelFileDescriptor() {
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
     * Adds a client in the client list so that ParcelFileDescriptor can be closed when the list
     * becomes empty. No-op if ParcelFileDescriptor is already closed.
     * @param client an object instance for identifying the client.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void addParcelFileDescriptorClient(Object client) {
        synchronized (mClientMap) {
            if (mClosed) {
                Log.w(TAG, "ParcelFileDescriptorClient is already closed.");
                return;
            }
            mClientMap.add(client);
        }
    }

    /**
     * Removes a client from the client list. The ParcelFileDescriptor will be closed when the list
     * becomes empty.
     * @param client an object instance for identifying the client.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void removeParcelFileDescriptorClient(Object client) {
        synchronized (mClientMap) {
            if (mClientMap.remove(client) && mClientMap.isEmpty() && !mClosed) {
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
     */
    boolean isClosed() {
        synchronized (mClientMap) {
            return mClosed;
        }
    }

    /**
     * Close the {@link ParcelFileDescriptor} of this {@link FileMediaItem}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void close() throws IOException {
        synchronized (mClientMap) {
            if (mPFD != null) {
                mPFD.close();
            }
            mClosed = true;
        }
    }

    /**
     * This Builder class simplifies the creation of a {@link FileMediaItem} object.
     */
    public static final class Builder extends BuilderBase<Builder> {

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
         * If {@link FileMediaItem} is passed to {@link MediaPlayer}, {@link MediaPlayer} will
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
         * Creates a new Builder object with a media item (ParcelFileDescriptor) to use. The
         * ParcelFileDescriptor must be seekable (N.B. a LocalSocket is not seekable).
         * <p>
         * If {@link FileMediaItem} is passed to {@link MediaPlayer}, {@link MediaPlayer} will
         * close the ParcelFileDescriptor.
         * <p>
         * Any negative number for offset is treated as 0.
         * Any negative number for length is treated as maximum length of the media item.
         *
         * @param pfd the ParcelFileDescriptor for the file you want to play
         * @param offset the offset into the file where the data to be played starts, in bytes
         * @param length the length in bytes of the data to be played
         */
        public Builder(@NonNull ParcelFileDescriptor pfd, long offset, long length) {
            Preconditions.checkNotNull(pfd);
            if (offset < 0) {
                offset = 0;
            }
            if (length < 0) {
                length = FD_LENGTH_UNKNOWN;
            }
            mPFD = pfd;
            mFDOffset = offset;
            mFDLength = length;
        }

        /**
         * @return A new FileMediaItem with values supplied by the Builder.
         */
        @Override
        public @NonNull FileMediaItem build() {
            return new FileMediaItem(this);
        }
    }
}
