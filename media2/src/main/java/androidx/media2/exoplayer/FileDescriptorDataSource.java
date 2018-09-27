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
import android.system.Os;
import android.system.OsConstants;

import androidx.annotation.GuardedBy;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * An ExoPayer {@link DataSource} for reading from a file descriptor.
 *
 * @hide
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(LIBRARY_GROUP)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ class FileDescriptorDataSource extends BaseDataSource {

    // TODO(b/80232248): Move into core ExoPlayer library and delete this class.

    /**
     * {@link OsConstants} was added in API 21 and initializes its fields lazily, so we directly
     * specify the constant for the {@code lseek} {@code whence} argument for earlier API versions.
     */
    private static final int SEEK_SET = 0;

    // Before API 21 we access the hidden Posix.lseek API using reflection.
    private static final Object sPosixLockV14 = new Object();
    @GuardedBy("sPosixLockV14")
    private static @Nullable Object sPosixObjectV14;
    @GuardedBy("sPosixLockV14")
    private static @Nullable Method sLseekMethodV14;

    /**
     * Returns a factory for {@link FileDescriptorDataSource}s.
     *
     * @param fileDescriptor The file descriptor to read from.
     * @param offset The start offset in the file descriptor.
     * @param length The length of the range to read from the file descriptor.
     * @return A factory for data sources that read from the file descriptor.
     */
    static DataSource.Factory getFactory(
            final FileDescriptor fileDescriptor, final long offset, final long length) {
        return new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return new FileDescriptorDataSource(fileDescriptor, offset, length);
            }
        };
    }

    private final FileDescriptor mFileDescriptor;
    private final long mOffset;
    private final long mLength;

    private @Nullable Uri mUri;
    private @Nullable InputStream mInputStream;
    private long mBytesRemaining;
    private boolean mOpened;

    public FileDescriptorDataSource(FileDescriptor fileDescriptor, long offset, long length) {
        super(/* isNetwork= */ false);
        mFileDescriptor = fileDescriptor;
        mOffset = offset;
        mLength = length;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        mUri = dataSpec.uri;
        transferInitializing(dataSpec);
        seekFileDescriptor(mFileDescriptor, mOffset + dataSpec.position);
        mInputStream = new FileInputStream(mFileDescriptor);
        if (dataSpec.length != C.LENGTH_UNSET) {
            mBytesRemaining = dataSpec.length;
        } else if (mLength != C.LENGTH_UNSET) {
            mBytesRemaining = mLength - dataSpec.position;
        } else {
            mBytesRemaining = C.LENGTH_UNSET;
        }
        mOpened = true;
        transferStarted(dataSpec);
        return mBytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength)
            throws IOException {
        if (readLength == 0) {
            return 0;
        } else if (mBytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }
        int bytesToRead = mBytesRemaining == C.LENGTH_UNSET
                ? readLength : (int) Math.min(mBytesRemaining, readLength);
        int bytesRead = Preconditions.checkNotNull(mInputStream).read(buffer, offset, bytesToRead);
        if (bytesRead == -1) {
            if (mBytesRemaining != C.LENGTH_UNSET) {
                throw new EOFException();
            }
            return C.RESULT_END_OF_INPUT;
        }
        if (mBytesRemaining != C.LENGTH_UNSET) {
            mBytesRemaining -= bytesRead;
        }
        bytesTransferred(bytesRead);
        return bytesRead;
    }

    @Override
    public Uri getUri() {
        return mUri;
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

    private static void seekFileDescriptor(FileDescriptor fileDescriptor, long position)
            throws IOException {
        if (Build.VERSION.SDK_INT >= 21) {
            seekFileDescriptorV21(fileDescriptor, position);
        } else {
            seekFileDescriptorV14(fileDescriptor, position);
        }
    }

    @TargetApi(21)
    private static void seekFileDescriptorV21(FileDescriptor fileDescriptor, long position)
            throws IOException {
        try {
            Os.lseek(fileDescriptor, position, /* whence= */ OsConstants.SEEK_SET);
        } catch (Exception e) {
            throw new IOException("Failed to seek the file descriptor", e);
        }
    }

    @SuppressLint("PrivateApi")
    private static void seekFileDescriptorV14(FileDescriptor fileDescriptor, long position)
            throws IOException {
        try {
            Method method;
            Object object;
            synchronized (sPosixLockV14) {
                if (sPosixObjectV14 == null) {
                    Class<?> posixClass = Class.forName("libcore.io.Posix");
                    Constructor<?> constructor = posixClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    sLseekMethodV14 = posixClass.getMethod(
                            "lseek", FileDescriptor.class, Long.TYPE, Integer.TYPE);
                    sPosixObjectV14 = constructor.newInstance();
                }
                object = sPosixObjectV14;
                method = sLseekMethodV14;
            }
            method.invoke(object, fileDescriptor, position, /* whence= */ SEEK_SET);
        } catch (Exception e) {
            throw new IOException("Failed to seek the file descriptor", e);
        }
    }
}
