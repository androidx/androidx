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

package androidx.appsearch.localstorage.stats;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.BaseStats;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class holds detailed stats for initialization
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class InitializeStats extends BaseStats {
    /**
     * The cause of IcingSearchEngine recovering from a previous bad state during initialization.
     */
    @IntDef(value = {
            // It needs to be sync with RecoveryCause in
            // external/icing/proto/icing/proto/logging.proto#InitializeStatsProto
            RECOVERY_CAUSE_NONE,
            RECOVERY_CAUSE_DATA_LOSS,
            RECOVERY_CAUSE_INCONSISTENT_WITH_GROUND_TRUTH,
            RECOVERY_CAUSE_SCHEMA_CHANGES_OUT_OF_SYNC,
            RECOVERY_CAUSE_IO_ERROR,
            RECOVERY_CAUSE_LEGACY_DOCUMENT_LOG_FORMAT,
            RECOVERY_CAUSE_VERSION_CHANGED,
            RECOVERY_CAUSE_DEPENDENCIES_CHANGED,
            RECOVERY_CAUSE_FEATURE_FLAG_CHANGED,
            RECOVERY_CAUSE_UNKNOWN_OUT_OF_SYNC,
            RECOVERY_CAUSE_OPTIMIZE_OUT_OF_SYNC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecoveryCause {
    }

    // No recovery happened.
    public static final int RECOVERY_CAUSE_NONE = 0;
    // Data loss in ground truth.
    public static final int RECOVERY_CAUSE_DATA_LOSS = 1;
    // Data in index is inconsistent with ground truth.
    public static final int RECOVERY_CAUSE_INCONSISTENT_WITH_GROUND_TRUTH = 2;
    // Changes were made to the schema, but the marker file remains in the
    // filesystem indicating that changes possibly were not fully applied to the
    // document store and the index - requiring a recovery.
    public static final int RECOVERY_CAUSE_SCHEMA_CHANGES_OUT_OF_SYNC = 3;
    // Random I/O errors.
    public static final int RECOVERY_CAUSE_IO_ERROR = 4;
    // The document log is using legacy format.
    public static final int RECOVERY_CAUSE_LEGACY_DOCUMENT_LOG_FORMAT = 5;
    // The current code version is different from existing data version.
    public static final int RECOVERY_CAUSE_VERSION_CHANGED = 6;
    // Any dependencies have changed.
    public static final int RECOVERY_CAUSE_DEPENDENCIES_CHANGED = 7;
    // Change detected in Icing's feature flags since last initialization that
    // requires recovery.
    public static final int RECOVERY_CAUSE_FEATURE_FLAG_CHANGED = 8;
    // Changes were made by an incomplete complex operation, which caused marker
    // file to remain in the filesystem - requiring a recovery.
    //
    // Note: Icing is unable to interpret the information from the marker file
    // due to some reasons, so the OUT_OF_SYNC reason is UNKNOWN.
    public static final int RECOVERY_CAUSE_UNKNOWN_OUT_OF_SYNC = 9;
    // Changes were made by optimize, but the marker file remains in the
    // filesystem indicating that optimize possibly was not fully applied to the
    // document store and the index - requiring a recovery.
    public static final int RECOVERY_CAUSE_OPTIMIZE_OUT_OF_SYNC = 10;

    /**
     * Status regarding how much data is lost during the initialization.
     */
    @IntDef(value = {
            // It needs to be sync with DocumentStoreDataStatus in
            // external/icing/proto/icing/proto/logging.proto#InitializeStatsProto

            DOCUMENT_STORE_DATA_STATUS_NO_DATA_LOSS,
            DOCUMENT_STORE_DATA_STATUS_PARTIAL_LOSS,
            DOCUMENT_STORE_DATA_STATUS_COMPLETE_LOSS,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DocumentStoreDataStatus {
    }

    // Document store is successfully initialized or fully recovered.
    public static final int DOCUMENT_STORE_DATA_STATUS_NO_DATA_LOSS = 0;
    // Ground truth data is partially lost.
    public static final int DOCUMENT_STORE_DATA_STATUS_PARTIAL_LOSS = 1;
    // Ground truth data is completely lost.
    public static final int DOCUMENT_STORE_DATA_STATUS_COMPLETE_LOSS = 2;

    @AppSearchResult.ResultCode
    private final int mStatusCode;
    private final int mTotalLatencyMillis;
    /** Whether the initialize() detects deSync. */
    private final boolean mHasDeSync;
    /** Time used to read and process the schema and namespaces. */
    private final int mPrepareSchemaAndNamespacesLatencyMillis;
    /** Time used to read and process the visibility store. */
    private final int mPrepareVisibilityStoreLatencyMillis;
    /** Overall time used for the native function call. */
    private final int mNativeLatencyMillis;
    @RecoveryCause
    private final int mNativeDocumentStoreRecoveryCause;
    @RecoveryCause
    private final int mNativeIndexRestorationCause;
    @RecoveryCause
    private final int mNativeSchemaStoreRecoveryCause;
    /** Time used to recover the document store. */
    private final int mNativeDocumentStoreRecoveryLatencyMillis;
    /** Time used to restore the index. */
    private final int mNativeIndexRestorationLatencyMillis;
    /** Time used to recover the schema store. */
    private final int mNativeSchemaStoreRecoveryLatencyMillis;
    /** Status regarding how much data is lost during the initialization. */
    private final int mNativeDocumentStoreDataStatus;
    /**
     * Returns number of documents currently in document store. Those may include alive, deleted,
     * and expired documents.
     */
    private final int mNativeNumDocuments;
    /** Returns number of schema types currently in the schema store. */
    private final int mNativeNumSchemaTypes;
    /**
     * Number of consecutive initialization failures that immediately preceded this
     * initialization.
     */
    int mNativeNumPreviousInitFailures;
    /** Restoration cause of integer index.*/
    @RecoveryCause
    int mNativeIntegerIndexRestorationCause;
    /** Restoration cause of qualified id join index.*/
    @RecoveryCause
    int mNativeQualifiedIdJoinIndexRestorationCause;
    /** Restoration cause of embedding index.*/
    @RecoveryCause
    int mNativeEmbeddingIndexRestorationCause;
    /** ICU data initialization status code*/
    @AppSearchResult.ResultCode
    int mNativeInitializeIcuDataStatusCode;
    /** Number of documents that failed to be reindexed during index restoration.*/
    int mNativeNumFailedReindexedDocuments;
    private final boolean mHasReset;
    /** If we had to reset, contains the status code of the reset operation. */
    @AppSearchResult.ResultCode
    private final int mResetStatusCode;

    /** Returns the status of the initialization. */
    @AppSearchResult.ResultCode
    public int getStatusCode() {
        return mStatusCode;
    }

    /** Returns the total latency in milliseconds for the initialization. */
    public int getTotalLatencyMillis() {
        return mTotalLatencyMillis;
    }

    /**
     * Returns whether the initialize() detects deSync.
     *
     * <p>If there is a deSync, it means AppSearch and IcingSearchEngine have an inconsistent view
     * of what data should exist.
     */
    public boolean hasDeSync() {
        return mHasDeSync;
    }

    /** Returns time used to read and process the schema and namespaces. */
    public int getPrepareSchemaAndNamespacesLatencyMillis() {
        return mPrepareSchemaAndNamespacesLatencyMillis;
    }

    /** Returns time used to read and process the visibility file. */
    public int getPrepareVisibilityStoreLatencyMillis() {
        return mPrepareVisibilityStoreLatencyMillis;
    }

    /** Returns overall time used for the native function call. */
    public int getNativeLatencyMillis() {
        return mNativeLatencyMillis;
    }

    /** Returns recovery cause for document store. */
    @RecoveryCause
    public int getNativeDocumentStoreRecoveryCause() {
        return mNativeDocumentStoreRecoveryCause;
    }

    /** Returns restoration cause for index store. */
    @RecoveryCause
    public int getNativeIndexRestorationCause() {
        return mNativeIndexRestorationCause;
    }

    /** Returns recovery cause for schema store.  */
    @RecoveryCause
    public int getNativeSchemaStoreRecoveryCause() {
        return mNativeSchemaStoreRecoveryCause;
    }

    /** Returns time used to recover the document store. */
    public int getNativeDocumentStoreRecoveryLatencyMillis() {
        return mNativeDocumentStoreRecoveryLatencyMillis;
    }

    /** Returns time used to restore the index. */
    public int getNativeIndexRestorationLatencyMillis() {
        return mNativeIndexRestorationLatencyMillis;
    }

    /** Returns time used to recover the schema store. */
    public int getNativeSchemaStoreRecoveryLatencyMillis() {
        return mNativeSchemaStoreRecoveryLatencyMillis;
    }

    /** Returns status about how much data is lost during the initialization. */
    @DocumentStoreDataStatus
    public int getNativeDocumentStoreDataStatus() {
        return mNativeDocumentStoreDataStatus;
    }

    /**
     * Returns number of documents currently in document store. Those may include alive, deleted,
     * and expired documents.
     */
    public int getNativeDocumentCount() {
        return mNativeNumDocuments;
    }

    /** Returns number of schema types currently in the schema store. */
    public int getNativeSchemaTypeCount() {
        return mNativeNumSchemaTypes;
    }

    /**
     * Returns number of consecutive initialization failures that immediately preceded this
     * initialization.
     */
    public int getNativeNumPreviousInitFailures() {
        return mNativeNumPreviousInitFailures;
    }

    /** Returns restoration cause for Integer index.    */
    @RecoveryCause
    public int getNativeIntegerIndexRestorationCause() {
        return mNativeIntegerIndexRestorationCause;
    }

    /**  Returns restoration cause for qualified id join index.  */
    @RecoveryCause
    public int getNativeQualifiedIdJoinIndexRestorationCause() {
        return mNativeQualifiedIdJoinIndexRestorationCause;
    }

    /**  Returns restoration cause for embedding index.  */
    @RecoveryCause
    public int getNativeEmbeddingIndexRestorationCause() {
        return mNativeEmbeddingIndexRestorationCause;
    }

    /**
     * Returns the status of ICU data initialization.
     *
     * <p>If no value has been set, the default value is {@link AppSearchResult#RESULT_OK}.
     */
    @AppSearchResult.ResultCode
    public int getNativeInitializeIcuDataStatusCode() {
        return mNativeInitializeIcuDataStatusCode;
    }

    /** Returns number of documents that failed to be reindexed during index restoration. */
    public int getNativeNumFailedReindexedDocuments() {
        return mNativeNumFailedReindexedDocuments;
    }

    /** Returns whether we had to reset the index, losing all data, as part of initialization. */
    public boolean hasReset() {
        return mHasReset;
    }

    /**
     * Returns the status of the reset, if one was performed according to {@link #hasReset}.
     *
     * <p>If no value has been set, the default value is {@link AppSearchResult#RESULT_OK}.
     */
    @AppSearchResult.ResultCode
    public int getResetStatusCode() {
        return mResetStatusCode;
    }

    InitializeStats(@NonNull Builder builder) {
        super(builder);
        mStatusCode = builder.mStatusCode;
        mTotalLatencyMillis = builder.mTotalLatencyMillis;
        mHasDeSync = builder.mHasDeSync;
        mPrepareSchemaAndNamespacesLatencyMillis = builder.mPrepareSchemaAndNamespacesLatencyMillis;
        mPrepareVisibilityStoreLatencyMillis = builder.mPrepareVisibilityStoreLatencyMillis;
        mNativeLatencyMillis = builder.mNativeLatencyMillis;
        mNativeDocumentStoreRecoveryCause = builder.mNativeDocumentStoreRecoveryCause;
        mNativeIndexRestorationCause = builder.mNativeIndexRestorationCause;
        mNativeSchemaStoreRecoveryCause = builder.mNativeSchemaStoreRecoveryCause;
        mNativeDocumentStoreRecoveryLatencyMillis =
                builder.mNativeDocumentStoreRecoveryLatencyMillis;
        mNativeIndexRestorationLatencyMillis = builder.mNativeIndexRestorationLatencyMillis;
        mNativeSchemaStoreRecoveryLatencyMillis = builder.mNativeSchemaStoreRecoveryLatencyMillis;
        mNativeDocumentStoreDataStatus = builder.mNativeDocumentStoreDataStatus;
        mNativeNumDocuments = builder.mNativeNumDocuments;
        mNativeNumSchemaTypes = builder.mNativeNumSchemaTypes;
        mNativeNumPreviousInitFailures = builder.mNativeNumPreviousInitFailures;
        mNativeIntegerIndexRestorationCause = builder.mNativeIntegerIndexRestorationCause;
        mNativeQualifiedIdJoinIndexRestorationCause =
                builder.mNativeQualifiedIdJoinIndexRestorationCause;
        mNativeEmbeddingIndexRestorationCause = builder.mNativeEmbeddingIndexRestorationCause;
        mNativeInitializeIcuDataStatusCode = builder.mNativeInitializeIcuDataStatusCode;
        mNativeNumFailedReindexedDocuments = builder.mNativeNumFailedReindexedDocuments;
        mHasReset = builder.mHasReset;
        mResetStatusCode = builder.mResetStatusCode;
    }

    /** Builder for {@link InitializeStats}. */
    public static class Builder extends BaseStats.Builder<InitializeStats.Builder> {
        @AppSearchResult.ResultCode
        int mStatusCode;

        int mTotalLatencyMillis;
        boolean mHasDeSync;
        int mPrepareSchemaAndNamespacesLatencyMillis;
        int mPrepareVisibilityStoreLatencyMillis;
        int mNativeLatencyMillis;
        @RecoveryCause
        int mNativeDocumentStoreRecoveryCause;
        @RecoveryCause
        int mNativeIndexRestorationCause;
        @RecoveryCause
        int mNativeSchemaStoreRecoveryCause;
        int mNativeDocumentStoreRecoveryLatencyMillis;
        int mNativeIndexRestorationLatencyMillis;
        int mNativeSchemaStoreRecoveryLatencyMillis;
        @DocumentStoreDataStatus
        int mNativeDocumentStoreDataStatus;
        int mNativeNumDocuments;
        int mNativeNumSchemaTypes;
        int mNativeNumPreviousInitFailures;
        @RecoveryCause
        int mNativeIntegerIndexRestorationCause;
        @RecoveryCause
        int mNativeQualifiedIdJoinIndexRestorationCause;
        @RecoveryCause
        int mNativeEmbeddingIndexRestorationCause;
        int mNativeInitializeIcuDataStatusCode;
        int mNativeNumFailedReindexedDocuments;
        boolean mHasReset;
        @AppSearchResult.ResultCode
        int mResetStatusCode;

        /** Sets the status of the initialization. */
        @CanIgnoreReturnValue
        public @NonNull Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets the total latency of the initialization in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mTotalLatencyMillis = totalLatencyMillis;
            return this;
        }

        /**
         * Sets whether the initialize() detects deSync.
         *
         * <p>If there is a deSync, it means AppSearch and IcingSearchEngine have an inconsistent
         * view of what data should exist.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setHasDeSync(boolean hasDeSync) {
            mHasDeSync = hasDeSync;
            return this;
        }

        /** Sets time used to read and process the schema and namespaces. */
        @CanIgnoreReturnValue
        public @NonNull Builder setPrepareSchemaAndNamespacesLatencyMillis(
                int prepareSchemaAndNamespacesLatencyMillis) {
            mPrepareSchemaAndNamespacesLatencyMillis = prepareSchemaAndNamespacesLatencyMillis;
            return this;
        }

        /** Sets time used to read and process the visibility file. */
        @CanIgnoreReturnValue
        public @NonNull Builder setPrepareVisibilityStoreLatencyMillis(
                int prepareVisibilityStoreLatencyMillis) {
            mPrepareVisibilityStoreLatencyMillis = prepareVisibilityStoreLatencyMillis;
            return this;
        }

        /** Sets overall time used for the native function call. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeLatencyMillis(int nativeLatencyMillis) {
            mNativeLatencyMillis = nativeLatencyMillis;
            return this;
        }

        /** Sets recovery cause for document store.  */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentStoreRecoveryCause(
                @RecoveryCause int nativeDocumentStoreRecoveryCause) {
            mNativeDocumentStoreRecoveryCause = nativeDocumentStoreRecoveryCause;
            return this;
        }

        /**  Sets restoration cause for index store.  */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeIndexRestorationCause(
                @RecoveryCause int nativeIndexRestorationCause) {
            mNativeIndexRestorationCause = nativeIndexRestorationCause;
            return this;
        }

        /**  Sets recovery cause for schema store. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeSchemaStoreRecoveryCause(
                @RecoveryCause int nativeSchemaStoreRecoveryCause) {
            mNativeSchemaStoreRecoveryCause = nativeSchemaStoreRecoveryCause;
            return this;
        }

        /** Sets time used to recover the document store. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentStoreRecoveryLatencyMillis(
                int nativeDocumentStoreRecoveryLatencyMillis) {
            mNativeDocumentStoreRecoveryLatencyMillis = nativeDocumentStoreRecoveryLatencyMillis;
            return this;
        }

        /** Sets time used to restore the index. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeIndexRestorationLatencyMillis(
                int nativeIndexRestorationLatencyMillis) {
            mNativeIndexRestorationLatencyMillis = nativeIndexRestorationLatencyMillis;
            return this;
        }

        /** Sets time used to recover the schema store. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeSchemaStoreRecoveryLatencyMillis(
                int nativeSchemaStoreRecoveryLatencyMillis) {
            mNativeSchemaStoreRecoveryLatencyMillis = nativeSchemaStoreRecoveryLatencyMillis;
            return this;
        }

        /**
         * Sets Native Document Store Data status.
         * status is defined in external/icing/proto/icing/proto/logging.proto
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentStoreDataStatus(
                @DocumentStoreDataStatus int nativeDocumentStoreDataStatus) {
            mNativeDocumentStoreDataStatus = nativeDocumentStoreDataStatus;
            return this;
        }

        /**
         * Sets number of documents currently in document store. Those may include alive, deleted,
         * and expired documents.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentCount(int nativeNumDocuments) {
            mNativeNumDocuments = nativeNumDocuments;
            return this;
        }

        /** Sets number of schema types currently in the schema store. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeSchemaTypeCount(int nativeNumSchemaTypes) {
            mNativeNumSchemaTypes = nativeNumSchemaTypes;
            return this;
        }

        /**
         * Sets number of consecutive initialization failures that immediately preceded this
         * initialization.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeNumPreviousInitFailures(
                int nativeNumPreviousInitFailures) {
            mNativeNumPreviousInitFailures = nativeNumPreviousInitFailures;
            return this;
        }

        /** Sets restoration cause for integer store.  */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeIntegerIndexRestorationCause(
                @RecoveryCause int nativeIntegerIndexRestorationCause) {
            mNativeIntegerIndexRestorationCause = nativeIntegerIndexRestorationCause;
            return this;
        }

        /** Sets restoration cause for qualified id join index. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeQualifiedIdJoinIndexRestorationCause(
                @RecoveryCause int nativeQualifiedIdJoinIndexRestorationCause) {
            mNativeQualifiedIdJoinIndexRestorationCause =
                    nativeQualifiedIdJoinIndexRestorationCause;
            return this;
        }

        /** Sets restoration cause for embedding index. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeEmbeddingIndexRestorationCause(
                @RecoveryCause int nativeEmbeddingIndexRestorationCause) {
            mNativeEmbeddingIndexRestorationCause = nativeEmbeddingIndexRestorationCause;
            return this;
        }

        /** Sets the status of the initialize Icu data. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeInitializeIcuDataStatusCode(
                @AppSearchResult.ResultCode int nativeInitializeIcuDataStatusCode) {
            mNativeInitializeIcuDataStatusCode = nativeInitializeIcuDataStatusCode;
            return this;
        }

        /**  Sets number of documents that failed to be reindexed during index restoration.   */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeNumFailedReindexedDocuments(
                int nativeNumFailedReindexedDocuments) {
            mNativeNumFailedReindexedDocuments = nativeNumFailedReindexedDocuments;
            return this;
        }

        /** Sets whether we had to reset the index, losing all data, as part of initialization. */
        @CanIgnoreReturnValue
        public @NonNull Builder setHasReset(boolean hasReset) {
            mHasReset = hasReset;
            return this;
        }

        /** Sets the status of the reset, if one was performed according to {@link #setHasReset}. */
        @CanIgnoreReturnValue
        public @NonNull Builder setResetStatusCode(
                @AppSearchResult.ResultCode int resetStatusCode) {
            mResetStatusCode = resetStatusCode;
            return this;
        }

        /**
         * Constructs a new {@link InitializeStats} from the contents of this
         * {@link InitializeStats.Builder}
         */
        @Override
        public @NonNull InitializeStats build() {
            return new InitializeStats(/* builder= */ this);
        }
    }
}
