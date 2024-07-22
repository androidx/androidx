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
@SuppressWarnings("HiddenSuperclass")
public final class StorageInfo extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @NonNull
    public static final Parcelable.Creator<StorageInfo> CREATOR = new StorageInfoCreator();

    @Field(id = 1, getter = "getSizeBytes")
    private long mSizeBytes;

    @Field(id = 2, getter = "getAliveDocumentsCount")
    private int mAliveDocumentsCount;

    @Field(id = 3, getter = "getAliveNamespacesCount")
    private int mAliveNamespacesCount;

    @Constructor
    StorageInfo(
            @Param(id = 1) long sizeBytes,
            @Param(id = 2) int aliveDocumentsCount,
            @Param(id = 3) int aliveNamespacesCount) {
        mSizeBytes = sizeBytes;
        mAliveDocumentsCount = aliveDocumentsCount;
        mAliveNamespacesCount = aliveNamespacesCount;
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

    /** Builder for {@link StorageInfo} objects. */
    public static final class Builder {
        private long mSizeBytes;
        private int mAliveDocumentsCount;
        private int mAliveNamespacesCount;

        /** Sets the size in bytes. */
        @CanIgnoreReturnValue
        @NonNull
        public StorageInfo.Builder setSizeBytes(long sizeBytes) {
            mSizeBytes = sizeBytes;
            return this;
        }

        /** Sets the number of alive documents. */
        @CanIgnoreReturnValue
        @NonNull
        public StorageInfo.Builder setAliveDocumentsCount(int aliveDocumentsCount) {
            mAliveDocumentsCount = aliveDocumentsCount;
            return this;
        }

        /** Sets the number of alive namespaces. */
        @CanIgnoreReturnValue
        @NonNull
        public StorageInfo.Builder setAliveNamespacesCount(int aliveNamespacesCount) {
            mAliveNamespacesCount = aliveNamespacesCount;
            return this;
        }

        /** Builds a {@link StorageInfo} object. */
        @NonNull
        public StorageInfo build() {
            return new StorageInfo(mSizeBytes, mAliveDocumentsCount, mAliveNamespacesCount);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        StorageInfoCreator.writeToParcel(this, dest, flags);
    }
}
