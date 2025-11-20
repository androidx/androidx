/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.app;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.StorageInfoCreator;

/** The response class of {@code AppSearchSession#getStorageInfo}. */
@SafeParcelable.Class(creator = "StorageInfoCreator")
// TODO(b/384721898): Switching to JSpecify annotations changes APIs once synced to platform.
//  Do not switch unless you've checked that no APIs are affected.
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class StorageInfo extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<StorageInfo> CREATOR = new StorageInfoCreator();

    @Field(id = 1, getter = "getSizeBytes")
    private long mSizeBytes;

    @Field(id = 2, getter = "getAliveDocumentsCount")
    private int mAliveDocumentsCount;

    @Field(id = 3, getter = "getAliveNamespacesCount")
    private int mAliveNamespacesCount;

    @Field(id = 4, getter = "getBlobsSizeBytes")
    private long mBlobsSizeBytes;

    @Field(id = 5, getter = "getBlobsCount")
    private int mBlobsCount;

    @Constructor
    StorageInfo(
            @Param(id = 1) long sizeBytes,
            @Param(id = 2) int aliveDocumentsCount,
            @Param(id = 3) int aliveNamespacesCount,
            @Param(id = 4) long blobsSizeBytes,
            @Param(id = 5) int blobsCount) {
        mSizeBytes = sizeBytes;
        mAliveDocumentsCount = aliveDocumentsCount;
        mAliveNamespacesCount = aliveNamespacesCount;
        mBlobsSizeBytes = blobsSizeBytes;
        mBlobsCount = blobsCount;
    }

    /** Returns the estimated size of the session's database in bytes. */
    public long getSizeBytes() {
        return mSizeBytes;
    }

    /**
     * Returns the number of alive documents in the current session.
     *
     * <p>Alive documents are documents that haven't been deleted and haven't exceeded the ttl as
     * set in {@link GenericDocument.Builder#setTtlMillis}.
     */
    public int getAliveDocumentsCount() {
        return mAliveDocumentsCount;
    }

    /**
     * Returns the number of namespaces that have at least one alive document in the current
     * session's database.
     *
     * <p>Alive documents are documents that haven't been deleted and haven't exceeded the ttl as
     * set in {@link GenericDocument.Builder#setTtlMillis}.
     */
    public int getAliveNamespacesCount() {
        return mAliveNamespacesCount;
    }

    /**
     * Returns the total size of all blobs in the session's database in bytes.
     *
     * <p>Blobs are binary large objects associated with the documents in the database. Pending
     * blobs that haven't been committed and orphan blobs that haven't been cleared will be counted
     * along with alive blobs.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
    public long getBlobsSizeBytes() {
        return mBlobsSizeBytes;
    }

    /**
     * Returns the total number of blobs in the session's database.
     *
     * <p>Blobs are binary large objects associated with the documents in the database. Pending
     * blobs that haven't been committed and orphan blobs that haven't been cleared will be counted
     * with alive blobs as well.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
    public int getBlobsCount() {
        return mBlobsCount;
    }

    /** Builder for {@link StorageInfo} objects. */
    public static final class Builder {
        private long mSizeBytes;
        private int mAliveDocumentsCount;
        private int mAliveNamespacesCount;
        private long mBlobsSizeBytes;
        private int mBlobsCount;

        /** Sets the size in bytes. */
        @CanIgnoreReturnValue
        public @NonNull StorageInfo.Builder setSizeBytes(long sizeBytes) {
            mSizeBytes = sizeBytes;
            return this;
        }

        /** Sets the number of alive documents. */
        @CanIgnoreReturnValue
        public @NonNull StorageInfo.Builder setAliveDocumentsCount(int aliveDocumentsCount) {
            mAliveDocumentsCount = aliveDocumentsCount;
            return this;
        }

        /** Sets the number of alive namespaces. */
        @CanIgnoreReturnValue
        public @NonNull StorageInfo.Builder setAliveNamespacesCount(int aliveNamespacesCount) {
            mAliveNamespacesCount = aliveNamespacesCount;
            return this;
        }

        /** Sets the size of stored blobs in bytes. */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
        public @NonNull StorageInfo.Builder setBlobsSizeBytes(long blobsSizeBytes) {
            mBlobsSizeBytes = blobsSizeBytes;
            return this;
        }

        /** Sets the number of stored blobs. */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
        public @NonNull StorageInfo.Builder setBlobsCount(int blobsCount) {
            mBlobsCount = blobsCount;
            return this;
        }

        /** Builds a {@link StorageInfo} object. */
        public @NonNull StorageInfo build() {
            return new StorageInfo(mSizeBytes, mAliveDocumentsCount, mAliveNamespacesCount,
                    mBlobsSizeBytes, mBlobsCount);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        StorageInfoCreator.writeToParcel(this, dest, flags);
    }
}
