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

package androidx.media2.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.upstream.BaseDataSource;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DataSpec;

import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An ExoPayer {@link DataSource} for reading from a file descriptor.
 *
 * @hide
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(LIBRARY_GROUP)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ class FileDescriptorDataSource extends BaseDataSource {

    /**
     * Returns a factory for {@link FileDescriptorDataSource}s.
     *
     * @param fileDescriptor The file descriptor to read from.
     * @param offset The start offset in the file descriptor.
     * @param length The length of the range to read from the file descriptor.
     * @param lock An object to synchronize on when using the file descriptor.
     * @return A factory for data sources that read from the file descriptor.
     */
    static DataSource.Factory getFactory(
            final FileDescriptor fileDescriptor,
            final long offset,
            final long length,
            final Object lock) {
        return new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return new FileDescriptorDataSource(fileDescriptor, offset, length, lock);
            }
        };
    }

    // TODO(b/80232248): Move into core ExoPlayer library and delete this class.

    private final FileDescriptor mFileDescriptor;
    private final long mOffset;
    private final long mLength;
    private final Object mLock;

    private @Nullable Uri mUri;
    private @Nullable InputStream mInputStream;
    private long mBytesRemaining;
    private boolean mOpened;
    private long mPosition;

    FileDescriptorDataSource(
            FileDescriptor fileDescriptor, long offset, long length, Object lock) {
        super(/* isNetwork= */ false);
        mFileDescriptor = fileDescriptor;
        mOffset = offset;
        mLength = length;
        mLock = lock;
    }

    @Override
    public long open(DataSpec dataSpec) {
        mUri = dataSpec.uri;
        transferInitializing(dataSpec);
        mInputStream = new FileInputStream(mFileDescriptor);
        if (dataSpec.length != C.LENGTH_UNSET) {
            mBytesRemaining = dataSpec.length;
        } else if (mLength != C.LENGTH_UNSET) {
            mBytesRemaining = mLength - dataSpec.position;
        } else {
            mBytesRemaining = C.LENGTH_UNSET;
        }
        mPosition = mOffset + dataSpec.position;
        mOpened = true;
        transferStarted(dataSpec);
        return mBytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) {
            return 0;
        } else if (mBytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }
        int bytesToRead = mBytesRemaining == C.LENGTH_UNSET
                ? readLength : (int) Math.min(mBytesRemaining, readLength);
        int bytesRead;
        synchronized (mLock) {
            // The file descriptor position is shared across all users, so seek before reading.
            FileDescriptorUtil.seek(mFileDescriptor, mPosition);
            bytesRead = Preconditions.checkNotNull(mInputStream).read(buffer, offset, bytesToRead);
            if (bytesRead == -1) {
                if (mBytesRemaining != C.LENGTH_UNSET) {
                    throw new EOFException();
                }
                return C.RESULT_END_OF_INPUT;
            }
            mPosition += bytesRead;
        }
        if (mBytesRemaining != C.LENGTH_UNSET) {
            mBytesRemaining -= bytesRead;
        }
        bytesTransferred(bytesRead);
        return bytesRead;
    }

    @Override
    public Uri getUri() {
        return Preconditions.checkNotNull(mUri);
    }

    @Override
    public void close() throws IOException {
        mUri = null;
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
        } finally {
            mInputStream = null;
            if (mOpened) {
                mOpened = false;
                transferEnded();
            }
        }
    }

}
