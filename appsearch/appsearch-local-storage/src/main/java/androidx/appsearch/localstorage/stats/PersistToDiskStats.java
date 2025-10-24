/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.localstorage.stats;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.BaseStats;

import com.google.android.icing.proto.PersistType;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Stats for persist-to-disk operations in AppSearch.
 *
 * This class captures various latency metrics and status codes related to the
 * process of persisting data from AppSearch's in-memory state to disk.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PersistToDiskStats extends BaseStats {
    private final @Nullable String mPackageName;
    private final int mTriggerCallType;
    private final @AppSearchResult.ResultCode int mStatusCode;
    private final int mTotalLatencyMillis;
    private final PersistType.@NonNull Code mPersistType;
    private final int mNativeLatencyMillis;
    private final int mBlobStorePersistLatencyMillis;
    private final int mDocumentStoreTotalPersistLatencyMillis;
    private final int mDocumentStoreComponentsPersistLatencyMillis;
    private final int mDocumentStoreChecksumUpdateLatencyMillis;
    private final int mDocumentLogChecksumUpdateLatencyMillis;
    private final int mDocumentLogDataSyncLatencyMillis;
    private final int mSchemaStorePersistLatencyMillis;
    private final int mIndexPersistLatencyMillis;
    private final int mIntegerIndexPersistLatencyMillis;
    private final int mQualifiedIdJoinIndexPersistLatencyMillis;
    private final int mEmbeddingIndexPersistLatencyMillis;

    PersistToDiskStats(@NonNull Builder builder) {
        super(builder);
        mPackageName = builder.mPackageName;
        mTriggerCallType = builder.mTriggerCallType;
        mStatusCode = builder.mStatusCode;
        mTotalLatencyMillis = builder.mTotalLatencyMillis;
        mPersistType = builder.mPersistType;
        mNativeLatencyMillis = builder.mNativeLatencyMillis;
        mBlobStorePersistLatencyMillis = builder.mBlobStorePersistLatencyMillis;
        mDocumentStoreTotalPersistLatencyMillis = builder.mDocumentStoreTotalPersistLatencyMillis;
        mDocumentStoreComponentsPersistLatencyMillis =
                builder.mDocumentStoreComponentsPersistLatencyMillis;
        mDocumentStoreChecksumUpdateLatencyMillis =
                builder.mDocumentStoreChecksumUpdateLatencyMillis;
        mDocumentLogChecksumUpdateLatencyMillis = builder.mDocumentLogChecksumUpdateLatencyMillis;
        mDocumentLogDataSyncLatencyMillis = builder.mDocumentLogDataSyncLatencyMillis;
        mSchemaStorePersistLatencyMillis = builder.mSchemaStorePersistLatencyMillis;
        mIndexPersistLatencyMillis = builder.mIndexPersistLatencyMillis;
        mIntegerIndexPersistLatencyMillis = builder.mIntegerIndexPersistLatencyMillis;
        mQualifiedIdJoinIndexPersistLatencyMillis =
                builder.mQualifiedIdJoinIndexPersistLatencyMillis;
        mEmbeddingIndexPersistLatencyMillis = builder.mEmbeddingIndexPersistLatencyMillis;
    }

    /** Returns the package name associated with this persist operation. */
    public @Nullable String getPackageName() {
        return mPackageName;
    }

    /** Returns the call type that trigger this persist to disk call. */
    public @CallType int getTriggerCallType() {
        return mTriggerCallType;
    }

    /** Returns the {@link AppSearchResult.ResultCode} of the persist operation. */
    public @AppSearchResult.ResultCode int getStatusCode() {
        return mStatusCode;
    }

    /** Returns the total latency of the persist operation in milliseconds. */
    public int getTotalLatencyMillis() {
        return mTotalLatencyMillis;
    }

    /** Returns the type of persist operation. */
    public PersistType.@NonNull Code getPersistType() {
        return mPersistType;
    }

    /**
     * Returns the latency of the native persist operation (excluding Java overhead) in
     * milliseconds.
     */
    public int getNativeLatencyMillis() {
        return mNativeLatencyMillis;
    }

    /** Returns the latency of persisting the blob store in milliseconds. */
    public int getBlobStorePersistLatencyMillis() {
        return mBlobStorePersistLatencyMillis;
    }

    /** Returns the total latency of persisting the document store in milliseconds. */
    public int getDocumentStoreTotalPersistLatencyMillis() {
        return mDocumentStoreTotalPersistLatencyMillis;
    }

    /**
     * Returns the latency of persisting individual components of the document store in
     * milliseconds.
     */
    public int getDocumentStoreComponentsPersistLatencyMillis() {
        return mDocumentStoreComponentsPersistLatencyMillis;
    }

    /** Returns the latency of updating the document store checksum in milliseconds. */
    public int getDocumentStoreChecksumUpdateLatencyMillis() {
        return mDocumentStoreChecksumUpdateLatencyMillis;
    }

    /** Returns the latency of updating the document log checksum in milliseconds. */
    public int getDocumentLogChecksumUpdateLatencyMillis() {
        return mDocumentLogChecksumUpdateLatencyMillis;
    }

    /** Returns the latency of syncing document log data to disk in milliseconds. */
    public int getDocumentLogDataSyncLatencyMillis() {
        return mDocumentLogDataSyncLatencyMillis;
    }

    /** Returns the latency of persisting the schema store in milliseconds. */
    public int getSchemaStorePersistLatencyMillis() {
        return mSchemaStorePersistLatencyMillis;
    }

    /** Returns the total latency of persisting all indexes in milliseconds. */
    public int getIndexPersistLatencyMillis() {
        return mIndexPersistLatencyMillis;
    }

    /** Returns the latency of persisting the integer index in milliseconds. */
    public int getIntegerIndexPersistLatencyMillis() {
        return mIntegerIndexPersistLatencyMillis;
    }

    /** Returns the latency of persisting the qualified ID join index in milliseconds. */
    public int getQualifiedIdJoinIndexPersistLatencyMillis() {
        return mQualifiedIdJoinIndexPersistLatencyMillis;
    }

    /** Returns the latency of persisting the embedding index in milliseconds. */
    public int getEmbeddingIndexPersistLatencyMillis() {
        return mEmbeddingIndexPersistLatencyMillis;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "PersistToDiskStats {\n"
                        + "  packageName=%s,\n"
                        + "  triggerCallType=%d,\n"
                        + "  statusCode=%d,\n"
                        + "  totalLatencyMillis=%d,\n"
                        + "  persistType=%s,\n"
                        + "  nativeLatencyMillis=%d,\n"
                        + "  blobStorePersistLatencyMillis=%d,\n"
                        + "  documentStoreTotalPersistLatencyMillis=%d,\n"
                        + "  documentStoreComponentsPersistLatencyMillis=%d,\n"
                        + "  documentStoreChecksumUpdateLatencyMillis=%d,\n"
                        + "  documentLogChecksumUpdateLatencyMillis=%d,\n"
                        + "  documentLogDataSyncLatencyMillis=%d,\n"
                        + "  schemaStorePersistLatencyMillis=%d,\n"
                        + "  indexPersistLatencyMillis=%d,\n"
                        + "  integerIndexPersistLatencyMillis=%d,\n"
                        + "  qualifiedIdJoinIndexPersistLatencyMillis=%d,\n"
                        + "  embeddingIndexPersistLatencyMillis=%d,\n"
                        // Include BaseStats fields
                        + super.toString()
                        + "}",
                mPackageName,
                mTriggerCallType,
                mStatusCode,
                mTotalLatencyMillis,
                mPersistType.name(),
                mNativeLatencyMillis,
                mBlobStorePersistLatencyMillis,
                mDocumentStoreTotalPersistLatencyMillis,
                mDocumentStoreComponentsPersistLatencyMillis,
                mDocumentStoreChecksumUpdateLatencyMillis,
                mDocumentLogChecksumUpdateLatencyMillis,
                mDocumentLogDataSyncLatencyMillis,
                mSchemaStorePersistLatencyMillis,
                mIndexPersistLatencyMillis,
                mIntegerIndexPersistLatencyMillis,
                mQualifiedIdJoinIndexPersistLatencyMillis,
                mEmbeddingIndexPersistLatencyMillis);
    }

    /** Builder for {@link PersistToDiskStats}. */
    public static class Builder extends BaseStats.Builder<PersistToDiskStats.Builder> {
        private final @Nullable String mPackageName;
        private final @CallType int mTriggerCallType;
        private @AppSearchResult.ResultCode int mStatusCode;
        private int mTotalLatencyMillis;
        private PersistType.@NonNull Code mPersistType;
        private int mNativeLatencyMillis;
        private int mBlobStorePersistLatencyMillis;
        private int mDocumentStoreTotalPersistLatencyMillis;
        private int mDocumentStoreComponentsPersistLatencyMillis;
        private int mDocumentStoreChecksumUpdateLatencyMillis;
        private int mDocumentLogChecksumUpdateLatencyMillis;
        private int mDocumentLogDataSyncLatencyMillis;
        private int mSchemaStorePersistLatencyMillis;
        private int mIndexPersistLatencyMillis;
        private int mIntegerIndexPersistLatencyMillis;
        private int mQualifiedIdJoinIndexPersistLatencyMillis;
        private int mEmbeddingIndexPersistLatencyMillis;

        /**
         * Constructor for the {@link Builder}.
         *
         * @param packageName The package name associated with the persist operation.
         *                    Can be {@code null}.
         * @param triggerCallType The type of call that triggered the persist operation.
         */
        public Builder(@Nullable String packageName, @CallType int triggerCallType) {
            mPackageName = packageName;
            mTriggerCallType = triggerCallType;
        }

        /** Sets the status code of the persist operation. */
        @CanIgnoreReturnValue
        public @NonNull Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets total latency of the persist operation in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mTotalLatencyMillis = totalLatencyMillis;
            return this;
        }

        /** Sets the type of persist operation. */
        @CanIgnoreReturnValue
        public @NonNull Builder setPersistType(PersistType.@NonNull Code persistType) {
            mPersistType = persistType;
            return this;
        }

        /**
         * Sets the latency of the native persist operation (excluding Java overhead) in
         * milliseconds.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeLatencyMillis(int latencyMillis) {
            mNativeLatencyMillis = latencyMillis;
            return this;
        }

        /** Sets the latency of persisting the blob store in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeBlobStorePersistLatencyMillis(
                int blobStorePersistLatencyMillis) {
            mBlobStorePersistLatencyMillis = blobStorePersistLatencyMillis;
            return this;
        }

        /** Sets the total latency of persisting the document store in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentStoreTotalPersistLatencyMillis(
                int documentStoreTotalPersistLatencyMillis) {
            mDocumentStoreTotalPersistLatencyMillis = documentStoreTotalPersistLatencyMillis;
            return this;
        }

        /**
         * Sets the latency of persisting individual components of the document store in
         * milliseconds.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentStoreComponentsPersistLatencyMillis(
                int documentStoreComponentsPersistLatencyMillis) {
            mDocumentStoreComponentsPersistLatencyMillis =
                    documentStoreComponentsPersistLatencyMillis;
            return this;
        }

        /** Sets the latency of updating the document store checksum in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentStoreChecksumUpdateLatencyMillis(
                int documentStoreChecksumUpdateLatencyMillis) {
            mDocumentStoreChecksumUpdateLatencyMillis = documentStoreChecksumUpdateLatencyMillis;
            return this;
        }

        /** Sets the latency of updating the document log checksum in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentLogChecksumUpdateLatencyMillis(
                int documentLogChecksumUpdateLatencyMillis) {
            mDocumentLogChecksumUpdateLatencyMillis = documentLogChecksumUpdateLatencyMillis;
            return this;
        }

        /** Sets the latency of syncing document log data to disk in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentLogDataSyncLatencyMillis(
                int documentLogDataSyncLatencyMillis) {
            mDocumentLogDataSyncLatencyMillis = documentLogDataSyncLatencyMillis;
            return this;
        }

        /** Sets the latency of persisting the schema store in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeSchemaStorePersistLatencyMillis(
                int schemaStorePersistLatencyMillis) {
            mSchemaStorePersistLatencyMillis = schemaStorePersistLatencyMillis;
            return this;
        }

        /** Sets the total latency of persisting all indexes in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeIndexPersistLatencyMillis(
                int indexPersistLatencyMillis) {
            mIndexPersistLatencyMillis = indexPersistLatencyMillis;
            return this;
        }

        /** Sets the latency of persisting the integer index in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeIntegerIndexPersistLatencyMillis(
                int integerIndexPersistLatencyMillis) {
            mIntegerIndexPersistLatencyMillis = integerIndexPersistLatencyMillis;
            return this;
        }

        /** Sets the latency of persisting the qualified ID join index in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeQualifiedIdJoinIndexPersistLatencyMillis(
                int qualifiedIdJoinIndexPersistLatencyMillis) {
            mQualifiedIdJoinIndexPersistLatencyMillis = qualifiedIdJoinIndexPersistLatencyMillis;
            return this;
        }

        /** Sets the latency of persisting the embedding index in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeEmbeddingIndexPersistLatencyMillis(
                int embeddingIndexPersistLatencyMillis) {
            mEmbeddingIndexPersistLatencyMillis = embeddingIndexPersistLatencyMillis;
            return this;
        }

        /** Builds the {@link androidx.appsearch.localstorage.stats.PersistToDiskStats} instance.*/
        @Override
        @NonNull
        public PersistToDiskStats build() {
            return new PersistToDiskStats(this);
        }
    }

}
