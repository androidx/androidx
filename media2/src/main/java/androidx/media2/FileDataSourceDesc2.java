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

import java.io.FileDescriptor;

/**
 * Structure for data source descriptor for a file. Used by {@link MediaItem2}.
 * <p>
 * Users should use {@link Builder} to create {@link FileDataSourceDesc2}.
 *
 * @see MediaItem2
 */
public class FileDataSourceDesc2 extends DataSourceDesc2 {
    /**
     * Used when the length of file descriptor is unknown.
     *
     * @see #getFileDescriptorLength()
     */
    public static final long FD_LENGTH_UNKNOWN = LONG_MAX;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    FileDescriptor mFD;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mFDOffset = 0;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mFDLength = FD_LENGTH_UNKNOWN;

    FileDataSourceDesc2(Builder builder) {
        super(builder);
        mFD = builder.mFD;
        mFDOffset = builder.mFDOffset;
        mFDLength = builder.mFDLength;
    }

    /**
     * Return the type of data source.
     * @return the type of data source
     */
    public int getType() {
        return TYPE_FD;
    }

    /**
     * Return the FileDescriptor of this data source.
     * @return the FileDescriptor of this data source
     */
    public @NonNull FileDescriptor getFileDescriptor() {
        return mFD;
    }

    /**
     * Return the offset associated with the FileDescriptor of this data source.
     * It's meaningful only when it has been set by the {@link DataSourceDesc2.Builder}.
     * @return the offset associated with the FileDescriptor of this data source
     */
    public long getFileDescriptorOffset() {
        return mFDOffset;
    }

    /**
     * Return the content length associated with the FileDescriptor of this data source.
     * {@link #FD_LENGTH_UNKNOWN} means same as the length of source content.
     * @return the content length associated with the FileDescriptor of this data source
     */
    public long getFileDescriptorLength() {
        return mFDLength;
    }

    /**
     * This Builder class simplifies the creation of a {@link FileDataSourceDesc2} object.
     */
    public static final class Builder extends DataSourceDesc2.Builder<Builder> {

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        FileDescriptor mFD;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mFDOffset = 0;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        long mFDLength = FD_LENGTH_UNKNOWN;

        /**
         * Creates a new Builder object with a data source (FileDescriptor) to use. The
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
         * Creates a new Builder object with a data source (FileDescriptor) to use. The
         * FileDescriptor must be seekable (N.B. a LocalSocket is not seekable). It is the caller's
         * responsibility to close the file descriptor after the source has been used.
         *
         * Any negative number for offset is treated as 0.
         * Any negative number for length is treated as maximum length of the data source.
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
         * @return A new FileDataSourceDesc2 with values supplied by the Builder.
         */
        public FileDataSourceDesc2 build() {
            return new FileDataSourceDesc2(this);
        }
    }
}
