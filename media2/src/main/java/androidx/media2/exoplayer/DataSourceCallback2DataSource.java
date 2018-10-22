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
import androidx.media2.DataSourceCallback2;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.upstream.BaseDataSource;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DataSpec;

import java.io.EOFException;
import java.io.IOException;

/**
 * An ExoPayer {@link DataSource} for reading from a {@link DataSourceCallback2}.
 *
 * @hide
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(LIBRARY_GROUP)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
public final class DataSourceCallback2DataSource extends BaseDataSource {

    /**
     * Returns a factory for {@link DataSourceCallback2DataSource}s.
     *
     * @return A factory for data sources that read from the data source callback.
     */
    static DataSource.Factory getFactory(
            final DataSourceCallback2 dataSourceCallback2) {
        return new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return new DataSourceCallback2DataSource(dataSourceCallback2);
            }
        };
    }

    private final DataSourceCallback2 mDataSourceCallback2;

    @Nullable
    private Uri mUri;
    private long mOffset;
    private long mBytesRemaining;
    private boolean mOpened;

    public DataSourceCallback2DataSource(DataSourceCallback2 dataSourceCallback2) {
        super(/* isNetwork= */ false);
        mDataSourceCallback2 = Preconditions.checkNotNull(dataSourceCallback2);
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        mUri = dataSpec.uri;
        mOffset = dataSpec.position;
        transferInitializing(dataSpec);
        long dataSourceCallback2Size = mDataSourceCallback2.getSize();
        if (dataSpec.length != C.LENGTH_UNSET) {
            mBytesRemaining = dataSpec.length;
        } else if (dataSourceCallback2Size != -1) {
            mBytesRemaining = dataSourceCallback2Size - mOffset;
        } else {
            mBytesRemaining = C.LENGTH_UNSET;
        }
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
        int bytesRead = mDataSourceCallback2.readAt(mOffset, buffer, offset, bytesToRead);
        if (bytesRead == -1) {
            if (mBytesRemaining != C.LENGTH_UNSET) {
                throw new EOFException();
            }
            return C.RESULT_END_OF_INPUT;
        }
        mOffset += bytesRead;
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
    public void close() {
        mUri = null;
        if (mOpened) {
            mOpened = false;
            transferEnded();
        }
    }
}
