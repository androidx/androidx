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
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelize;

import java.io.FileDescriptor;

/**
 * Structure for media item for a file.
 * <p>
 * Users should use {@link Builder} to create {@link FileMediaItem2}.
 * <p>
 * You cannot directly send this object across the process through {@link ParcelUtils}. See
 * {@link MediaItem2} for detail.
 *
 * @see MediaItem2
 */
@VersionedParcelize(isCustom = true)
public class FileMediaItem2 extends MediaItem2 {
    /**
     * Used when the length of file descriptor is unknown.
     *
     * @see #getFileDescriptorLength()
     */
    public static final long FD_LENGTH_UNKNOWN = LONG_MAX;

    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    FileDescriptor mFD;
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mFDOffset = 0;
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mFDLength = FD_LENGTH_UNKNOWN;

    /**
     * Used for VersionedParcelable
     */
    FileMediaItem2() {
        // no-op
    }

    FileMediaItem2(Builder builder) {
        super(builder);
        mFD = builder.mFD;
        mFDOffset = builder.mFDOffset;
        mFDLength = builder.mFDLength;
    }

    /**
     * Return the FileDescriptor of this media item.
     * @return the FileDescriptor of this media item
     */
    public @NonNull FileDescriptor getFileDescriptor() {
        return mFD;
    }

    /**
     * Return the offset associated with the FileDescriptor of this media item.
     * It's meaningful only when it has been set by the {@link MediaItem2.Builder}.
     * @return the offset associated with the FileDescriptor of this media item
     */
    public long getFileDescriptorOffset() {
        return mFDOffset;
    }

    /**
     * Return the content length associated with the FileDescriptor of this media item.
     * {@link #FD_LENGTH_UNKNOWN} means same as the length of source content.
     * @return the content length associated with the FileDescriptor of this media item
     */
    public long getFileDescriptorLength() {
        return mFDLength;
    }

    /**
     * This Builder class simplifies the creation of a {@link FileMediaItem2} object.
     */
    public static final class Builder extends BuilderBase<Builder> {

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        FileDescriptor mFD;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mFDOffset = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mFDLength = FD_LENGTH_UNKNOWN;

        /**
         * Creates a new Builder object with a media item (FileDescriptor) to use. The
         * FileDescriptor must be seekable (N.B. a LocalSocket is not seekable). It is the caller's
         * responsibility to close the file descriptor after the source has been used.
         *
         * @param fd the FileDescriptor for the file you want to play
         */
        public Builder(@NonNull FileDescriptor fd) {
            Preconditions.checkNotNull(fd);
            mFD = fd;
            mFDOffset = 0;
            mFDLength = FD_LENGTH_UNKNOWN;
        }

        /**
         * Creates a new Builder object with a media item (FileDescriptor) to use. The
         * FileDescriptor must be seekable (N.B. a LocalSocket is not seekable). It is the caller's
         * responsibility to close the file descriptor after the source has been used.
         *
         * Any negative number for offset is treated as 0.
         * Any negative number for length is treated as maximum length of the media item.
         *
         * @param fd the FileDescriptor for the file you want to play
         * @param offset the offset into the file where the data to be played starts, in bytes
         * @param length the length in bytes of the data to be played
         */
        public Builder(@NonNull FileDescriptor fd, long offset, long length) {
            Preconditions.checkNotNull(fd);
            if (offset < 0) {
                offset = 0;
            }
            if (length < 0) {
                length = FD_LENGTH_UNKNOWN;
            }
            mFD = fd;
            mFDOffset = offset;
            mFDLength = length;
        }

        /**
         * @return A new FileMediaItem2 with values supplied by the Builder.
         */
        @Override
        public @NonNull FileMediaItem2 build() {
            return new FileMediaItem2(this);
        }
    }
}
