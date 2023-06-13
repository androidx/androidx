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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.core.util.Preconditions;

/** The response class of {@code AppSearchSession#getStorageInfo}. */
public class StorageInfo {

    private static final String SIZE_BYTES_FIELD = "sizeBytes";
    private static final String ALIVE_DOCUMENTS_COUNT = "aliveDocumentsCount";
    private static final String ALIVE_NAMESPACES_COUNT = "aliveNamespacesCount";

    private final Bundle mBundle;

    StorageInfo(@NonNull Bundle bundle) {
        mBundle = Preconditions.checkNotNull(bundle);
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     * @exportToFramework:hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Bundle getBundle() {
        return mBundle;
    }

    /** Returns the estimated size of the session's database in bytes. */
    public long getSizeBytes() {
        return mBundle.getLong(SIZE_BYTES_FIELD);
    }

    /**
     * Returns the number of alive documents in the current session.
     *
     * <p>Alive documents are documents that haven't been deleted and haven't exceeded the ttl as
     * set in {@link GenericDocument.Builder#setTtlMillis}.
     */
    public int getAliveDocumentsCount() {
        return mBundle.getInt(ALIVE_DOCUMENTS_COUNT);
    }

    /**
     * Returns the number of namespaces that have at least one alive document in the current
     * session's database.
     *
     * <p>Alive documents are documents that haven't been deleted and haven't exceeded the ttl as
     * set in {@link GenericDocument.Builder#setTtlMillis}.
     */
    public int getAliveNamespacesCount() {
        return mBundle.getInt(ALIVE_NAMESPACES_COUNT);
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
            Bundle bundle = new Bundle();
            bundle.putLong(SIZE_BYTES_FIELD, mSizeBytes);
            bundle.putInt(ALIVE_DOCUMENTS_COUNT, mAliveDocumentsCount);
            bundle.putInt(ALIVE_NAMESPACES_COUNT, mAliveNamespacesCount);
            return new StorageInfo(bundle);
        }
    }
}
