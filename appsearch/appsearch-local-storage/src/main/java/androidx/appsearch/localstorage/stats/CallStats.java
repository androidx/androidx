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

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.BaseStats;
import androidx.collection.ArraySet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;

/**
 * A class for setting basic information to log for all function calls.
 *
 * <p>This class can set which stats to log for both batch and non-batch
 * {@link androidx.appsearch.app.AppSearchSession} calls.
 *
 * <p>Some function calls may have their own detailed stats class like {@link PutDocumentStats}.
 * However, {@link CallStats} can still be used along with the detailed stats class for easy
 * aggregation/analysis with other function calls.
 *
 * <!--@exportToFramework:hide-->
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CallStats extends BaseStats {

    private final @Nullable String mPackageName;
    private final @Nullable String mDatabase;
    /**
     * The status code returned by {@link AppSearchResult#getResultCode()} for the call or
     * internal state.
     */
    @AppSearchResult.ResultCode
    private final int mStatusCode;
    private final int mTotalLatencyMillis;

    @CallType
    private final int mCallType;
    private final int mEstimatedBinderLatencyMillis;
    private final int mNumOperationsSucceeded;
    private final int mNumOperationsFailed;
    private final long mCallReceivedTimestampMillis;
    private final int mGetUserInstanceLatencyMillis;
    private final int mPvmBinderLatencyMillis;
    // The request payload object size in byte.
    private final long mRequestPayloadSize;
    // The response payload object size in byte.
    private final long mResponsePayloadSize;
    @CallType
    int mLastCallTypeHoldExecutor;
    int mExecutorAcquisitionLatencyMillis;
    int mOnExecutorLatencyMillis;

    CallStats(@NonNull Builder builder) {
        super(builder);
        mPackageName = builder.mPackageName;
        mDatabase = builder.mDatabase;
        mStatusCode = builder.mStatusCode;
        mTotalLatencyMillis = builder.mTotalLatencyMillis;
        mCallType = builder.mCallType;
        mEstimatedBinderLatencyMillis = builder.mEstimatedBinderLatencyMillis;
        mNumOperationsSucceeded = builder.mNumOperationsSucceeded;
        mNumOperationsFailed = builder.mNumOperationsFailed;
        mCallReceivedTimestampMillis = builder.mCallReceivedTimestampMillis;
        mLastCallTypeHoldExecutor = builder.mLastCallTypeHoldExecutor;
        mExecutorAcquisitionLatencyMillis = builder.mExecutorAcquisitionLatencyMillis;
        mOnExecutorLatencyMillis = builder.mOnExecutorLatencyMillis;
        mGetUserInstanceLatencyMillis = builder.mGetUserInstanceLatencyMillis;
        mPvmBinderLatencyMillis = builder.mPvmBinderLatencyMillis;
        mRequestPayloadSize = builder.mRequestPayloadSize;
        mResponsePayloadSize = builder.mResponsePayloadSize;
    }

    /** Returns calling package name. */
    public @Nullable String getPackageName() {
        return mPackageName;
    }

    /** Returns calling database name. */
    public @Nullable String getDatabase() {
        return mDatabase;
    }

    /** Returns status code for this api call. */
    @AppSearchResult.ResultCode
    public int getStatusCode() {
        return mStatusCode;
    }

    /** Returns total latency of this api call in millis. */
    public int getTotalLatencyMillis() {
        return mTotalLatencyMillis;
    }

    /** Returns type of the call. */
    @CallType
    public int getCallType() {
        return mCallType;
    }

    /** Returns estimated binder latency, in milliseconds */
    public int getEstimatedBinderLatencyMillis() {
        return mEstimatedBinderLatencyMillis;
    }

    /**
     * Returns number of operations succeeded.
     *
     * <p>For example, for
     * {@link androidx.appsearch.app.AppSearchSession#putAsync}, it is the total number of
     * individual successful put operations. In this case, how many documents are successfully
     * indexed.
     *
     * <p>For non-batch calls such as
     * {@link androidx.appsearch.app.AppSearchSession#setSchemaAsync}, the sum of
     * {@link CallStats#getNumOperationsSucceeded()} and
     * {@link CallStats#getNumOperationsFailed()} is always 1 since there is only one
     * operation.
     */
    public int getNumOperationsSucceeded() {
        return mNumOperationsSucceeded;
    }

    /**
     * Returns number of operations failed.
     *
     * <p>For example, for
     * {@link androidx.appsearch.app.AppSearchSession#putAsync}, it is the total number of
     * individual failed put operations. In this case, how many documents are failed to be indexed.
     *
     * <p>For non-batch calls such as
     * {@link androidx.appsearch.app.AppSearchSession#setSchemaAsync}, the sum of
     * {@link CallStats#getNumOperationsSucceeded()} and
     * {@link CallStats#getNumOperationsFailed()} is always 1 since there is only one
     * operation.
     */
    public int getNumOperationsFailed() {
        return mNumOperationsFailed;
    }

    /** Returns the wall-clock timestamp in milliseconds when the API call was received. */
    public long getCallReceivedTimestampMillis() {
        return mCallReceivedTimestampMillis;
    }

    /** Gets the last call type that hold the executor */
    public int getLastCallTypeHoldExecutor() {
        return mLastCallTypeHoldExecutor;
    }

    /** Gets total latency for creating or waiting the user executor. */
    public int getExecutorAcquisitionLatencyMillis() {
        return mExecutorAcquisitionLatencyMillis;
    }

    /** Gets total latency while the task is running on the user executor. */
    public int getOnExecutorLatencyMillis() {
        return mOnExecutorLatencyMillis;
    }

    /** Gets the latency that AppSearch service get the user instance. */
    public int getGetUserInstanceLatencyMillis() {
        return mGetUserInstanceLatencyMillis;
    }

    /** Gets the latency that AppSearch pass request to Pvm via binder. */
    public int getPvmBinderLatencyMillis() {
        return mPvmBinderLatencyMillis;
    }

    /** Gets the payload size of the given request object. */
    public long getRequestPayloadSize() {
        return mRequestPayloadSize;
    }

    /** Gets the payload size of the returned response object. */
    public long getResponsePayloadSize() {
        return mResponsePayloadSize;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "CallStats {\n"
                        + "  packageName=%s,\n"
                        + "  database=%s,\n"
                        + "  statusCode=%d,\n"
                        + "  totalLatencyMillis=%d,\n"
                        + "  callType=%d,\n"
                        + "  estimatedBinderLatencyMillis=%d,\n"
                        + "  numOperationsSucceeded=%d,\n"
                        + "  numOperationsFailed=%d,\n"
                        + "  callReceivedTimestampMillis=%d,\n"
                        + "  lastCallTypeHoldExecutor=%d,\n"
                        + "  executorAcquisitionLatencyMillis=%d,\n"
                        + "  onExecutorLatencyMillis=%d,\n"
                        + "  getUserInstanceLatencyMillis=%d,\n"
                        + "  pvmBinderLatencyMillis=%d,\n"
                        + "  requestPayloadSize=%d,\n"
                        + "  responsePayloadSize=%d,\n"
                        // Include BaseStats fields
                        + super.toString()
                        + "}",
                mPackageName,
                mDatabase,
                mStatusCode,
                mTotalLatencyMillis,
                mCallType,
                mEstimatedBinderLatencyMillis,
                mNumOperationsSucceeded,
                mNumOperationsFailed,
                mCallReceivedTimestampMillis,
                mLastCallTypeHoldExecutor,
                mExecutorAcquisitionLatencyMillis,
                mOnExecutorLatencyMillis,
                mGetUserInstanceLatencyMillis,
                mPvmBinderLatencyMillis,
                mRequestPayloadSize,
                mResponsePayloadSize);
    }

    /** Builder for {@link CallStats}. */
    public static class Builder extends BaseStats.Builder<CallStats.Builder> {
        @Nullable String mPackageName;
        @Nullable String mDatabase;
        @AppSearchResult.ResultCode
        int mStatusCode;
        int mTotalLatencyMillis;
        @CallType
        int mCallType;
        int mEstimatedBinderLatencyMillis;
        int mNumOperationsSucceeded;
        int mNumOperationsFailed;
        long mCallReceivedTimestampMillis;
        @CallType
        int mLastCallTypeHoldExecutor;
        int mExecutorAcquisitionLatencyMillis;
        int mOnExecutorLatencyMillis;
        int mGetUserInstanceLatencyMillis;
        int mPvmBinderLatencyMillis;
        long mRequestPayloadSize;
        long mResponsePayloadSize;

        /** Sets the PackageName used by the session. */
        @CanIgnoreReturnValue
        public @NonNull Builder setPackageName(@Nullable String packageName) {
            mPackageName = packageName;
            return this;
        }

        /** Sets the database used by the session. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDatabase(@Nullable String database) {
            mDatabase = database;
            return this;
        }

        /** Sets the status code. */
        @CanIgnoreReturnValue
        public @NonNull Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets total latency in millis. */
        @CanIgnoreReturnValue
        public @NonNull Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mTotalLatencyMillis = totalLatencyMillis;
            return this;
        }

        /** Sets type of the call. */
        @CanIgnoreReturnValue
        public @NonNull Builder setCallType(@CallType int callType) {
            mCallType = callType;
            return this;
        }

        /** Sets estimated binder latency, in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setEstimatedBinderLatencyMillis(int estimatedBinderLatencyMillis) {
            mEstimatedBinderLatencyMillis = estimatedBinderLatencyMillis;
            return this;
        }

        /**
         * Sets number of operations succeeded.
         *
         * <p>For example, for
         * {@link androidx.appsearch.app.AppSearchSession#putAsync}, it is the total number of
         * individual successful put operations. In this case, how many documents are
         * successfully indexed.
         *
         * <p>For non-batch calls such as
         * {@link androidx.appsearch.app.AppSearchSession#setSchemaAsync}, the sum of
         * {@link CallStats#getNumOperationsSucceeded()} and
         * {@link CallStats#getNumOperationsFailed()} is always 1 since there is only one
         * operation.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNumOperationsSucceeded(int numOperationsSucceeded) {
            mNumOperationsSucceeded = numOperationsSucceeded;
            return this;
        }

        /**
         * Sets number of operations failed.
         *
         * <p>For example, for {@link androidx.appsearch.app.AppSearchSession#putAsync}, it is the
         * total number of individual failed put operations. In this case, how many documents
         * are failed to be indexed.
         *
         * <p>For non-batch calls such as
         * {@link androidx.appsearch.app.AppSearchSession#setSchemaAsync}, the sum of
         * {@link CallStats#getNumOperationsSucceeded()} and
         * {@link CallStats#getNumOperationsFailed()} is always 1 since there is only one
         * operation.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNumOperationsFailed(int numOperationsFailed) {
            mNumOperationsFailed = numOperationsFailed;
            return this;
        }

        /** Sets the wall-clock timestamp in milliseconds when the API call was received. */
        @CanIgnoreReturnValue
        public @NonNull Builder setCallReceivedTimestampMillis(long callReceivedTimestampMillis) {
            mCallReceivedTimestampMillis = callReceivedTimestampMillis;
            return this;
        }

        /** Sets total latency for creating or waiting the user executor. */
        @CanIgnoreReturnValue
        public @NonNull Builder setLastCallTypeHoldExecutor(@CallType int callType) {
            mLastCallTypeHoldExecutor = callType;
            return this;
        }

        /** Sets the last call type that hold the executor */
        @CanIgnoreReturnValue
        public @NonNull Builder setExecutorAcquisitionLatencyMillis(
                int executorAcquisitionLatencyMillis) {
            mExecutorAcquisitionLatencyMillis = executorAcquisitionLatencyMillis;
            return this;
        }

        /** Sets total latency while the task is running on the user executor. */
        @CanIgnoreReturnValue
        public @NonNull Builder setOnExecutorLatencyMillis(int onExecutorLatencyMillis) {
            mOnExecutorLatencyMillis = onExecutorLatencyMillis;
            return this;
        }

        /** Sets the latency that AppSearch service get the user instance. */
        @CanIgnoreReturnValue
        public @NonNull Builder setGetUserInstanceLatency(int getUserInstanceLatencyMillis) {
            mGetUserInstanceLatencyMillis = getUserInstanceLatencyMillis;
            return this;
        }

        /** Sets the latency that AppSearch pass request to Pvm via binder. */
        @CanIgnoreReturnValue
        public @NonNull Builder setPvmBinderLatency(int pvmBinderLatencyMillis) {
            mPvmBinderLatencyMillis = pvmBinderLatencyMillis;
            return this;
        }

        /** Sets the payload size of the given request object. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRequestPayloadSize(int requestPayloadSize) {
            mRequestPayloadSize = requestPayloadSize;
            return this;
        }

        /** Sets the payload size of the returned response object. */
        @CanIgnoreReturnValue
        public @NonNull Builder setResponsePayloadSize(int responsePayloadSize) {
            mResponsePayloadSize = responsePayloadSize;
            return this;
        }

        /** Creates {@link CallStats} object from {@link Builder} instance. */
        @Override
        public @NonNull CallStats build() {
            return new CallStats(/* builder= */ this);
        }
    }

    /**
     * Returns the {@link CallStats.CallType} represented by the given AppSearchManager API name. If
     * an unknown name is provided, {@link BaseStats#CALL_TYPE_UNKNOWN} is returned.
     */
    @CallType
    public static int getApiCallTypeFromName(@NonNull String name) {
        switch (name) {
            case CALL_TYPE_STRING_INITIALIZE:
                return CALL_TYPE_INITIALIZE;
            case CALL_TYPE_STRING_SET_SCHEMA:
                return CALL_TYPE_SET_SCHEMA;
            case CALL_TYPE_STRING_PUT_DOCUMENTS:
                return CALL_TYPE_PUT_DOCUMENTS;
            case CALL_TYPE_STRING_GET_DOCUMENTS:
                return CALL_TYPE_GET_DOCUMENTS;
            case CALL_TYPE_STRING_REMOVE_DOCUMENTS_BY_ID:
                return CALL_TYPE_REMOVE_DOCUMENTS_BY_ID;
            case CALL_TYPE_STRING_SEARCH:
                return CALL_TYPE_SEARCH;
            case CALL_TYPE_STRING_FLUSH:
                return CALL_TYPE_FLUSH;
            case CALL_TYPE_STRING_GLOBAL_SEARCH:
                return CALL_TYPE_GLOBAL_SEARCH;
            case CALL_TYPE_STRING_REMOVE_DOCUMENTS_BY_SEARCH:
                return CALL_TYPE_REMOVE_DOCUMENTS_BY_SEARCH;
            case CALL_TYPE_STRING_GLOBAL_GET_DOCUMENT_BY_ID:
                return CALL_TYPE_GLOBAL_GET_DOCUMENT_BY_ID;
            case CALL_TYPE_STRING_GLOBAL_GET_SCHEMA:
                return CALL_TYPE_GLOBAL_GET_SCHEMA;
            case CALL_TYPE_STRING_GET_SCHEMA:
                return CALL_TYPE_GET_SCHEMA;
            case CALL_TYPE_STRING_GET_NAMESPACES:
                return CALL_TYPE_GET_NAMESPACES;
            case CALL_TYPE_STRING_GET_NEXT_PAGE:
                return CALL_TYPE_GET_NEXT_PAGE;
            case CALL_TYPE_STRING_INVALIDATE_NEXT_PAGE_TOKEN:
                return CALL_TYPE_INVALIDATE_NEXT_PAGE_TOKEN;
            case CALL_TYPE_STRING_WRITE_SEARCH_RESULTS_TO_FILE:
                return CALL_TYPE_WRITE_SEARCH_RESULTS_TO_FILE;
            case CALL_TYPE_STRING_PUT_DOCUMENTS_FROM_FILE:
                return CALL_TYPE_PUT_DOCUMENTS_FROM_FILE;
            case CALL_TYPE_STRING_SEARCH_SUGGESTION:
                return CALL_TYPE_SEARCH_SUGGESTION;
            case CALL_TYPE_STRING_REPORT_SYSTEM_USAGE:
                return CALL_TYPE_REPORT_SYSTEM_USAGE;
            case CALL_TYPE_STRING_REPORT_USAGE:
                return CALL_TYPE_REPORT_USAGE;
            case CALL_TYPE_STRING_GET_STORAGE_INFO:
                return CALL_TYPE_GET_STORAGE_INFO;
            case CALL_TYPE_STRING_REGISTER_OBSERVER_CALLBACK:
                return CALL_TYPE_REGISTER_OBSERVER_CALLBACK;
            case CALL_TYPE_STRING_UNREGISTER_OBSERVER_CALLBACK:
                return CALL_TYPE_UNREGISTER_OBSERVER_CALLBACK;
            case CALL_TYPE_STRING_GLOBAL_GET_NEXT_PAGE:
                return CALL_TYPE_GLOBAL_GET_NEXT_PAGE;
            case CALL_TYPE_STRING_EXECUTE_APP_FUNCTION:
                return CALL_TYPE_EXECUTE_APP_FUNCTION;
            case CALL_TYPE_STRING_OPEN_WRITE_BLOB:
                return CALL_TYPE_OPEN_WRITE_BLOB;
            case CALL_TYPE_STRING_COMMIT_BLOB:
                return CALL_TYPE_COMMIT_BLOB;
            case CALL_TYPE_STRING_OPEN_READ_BLOB:
                return CALL_TYPE_OPEN_READ_BLOB;
            case CALL_TYPE_STRING_GLOBAL_OPEN_READ_BLOB:
                return CALL_TYPE_GLOBAL_OPEN_READ_BLOB;
            case CALL_TYPE_STRING_REMOVE_BLOB:
                return CALL_TYPE_REMOVE_BLOB;
            case CALL_TYPE_STRING_SET_BLOB_VISIBILITY:
                return CALL_TYPE_SET_BLOB_VISIBILITY;
            case INTERNAL_CALL_TYPE_STRING_APP_OPEN_EVENT_INDEXER:
                return INTERNAL_CALL_TYPE_APP_OPEN_EVENT_INDEXER;
            case INTERNAL_CALL_TYPE_STRING_ISOLATED_STORAGE_DATA_MIGRATION:
                return INTERNAL_CALL_TYPE_ISOLATED_STORAGE_DATA_MIGRATION;
            case INTERNAL_CALL_TYPE_STRING_PRUNE_PACKAGE_DATA:
                return INTERNAL_CALL_TYPE_PRUNE_PACKAGE_DATA;
            case INTERNAL_CALL_TYPE_STRING_CLOSE:
                return INTERNAL_CALL_TYPE_CLOSE;
            case INTERNAL_CALL_TYPE_STRING_PERSIST_TO_DISK_JOB:
                return INTERNAL_CALL_TYPE_PERSIST_TO_DISK_JOB;
            case INTERNAL_CALL_TYPE_STRING_ON_USER_UNLOCKING:
                return INTERNAL_CALL_TYPE_ON_USER_UNLOCKING;
            case INTERNAL_CALL_TYPE_STRING_HANDLE_PACKAGE_REMOVED:
                return INTERNAL_CALL_TYPE_HANDLE_PACKAGE_REMOVED;
            case INTERNAL_CALL_TYPE_STRING_SCHEDULED_FLUSH:
                return INTERNAL_CALL_TYPE_SCHEDULED_FLUSH;
            case INTERNAL_CALL_TYPE_STRING_MANUALLY_SCHEDULE_FLUSH:
                return CALL_TYPE_MANUALLY_SCHEDULE_FLUSH;
            default:
                return CALL_TYPE_UNKNOWN;
        }
    }

    /**
     * Returns the set of all {@link CallStats.CallType} that map to an AppSearchManager API.
     */
    @VisibleForTesting
    public static @NonNull Set<Integer> getAllApiCallTypes() {
        return new ArraySet<>(Arrays.asList(
                CALL_TYPE_INITIALIZE,
                CALL_TYPE_SET_SCHEMA,
                CALL_TYPE_PUT_DOCUMENTS,
                CALL_TYPE_GET_DOCUMENTS,
                CALL_TYPE_REMOVE_DOCUMENTS_BY_ID,
                CALL_TYPE_SEARCH,
                CALL_TYPE_FLUSH,
                CALL_TYPE_GLOBAL_SEARCH,
                CALL_TYPE_REMOVE_DOCUMENTS_BY_SEARCH,
                CALL_TYPE_GLOBAL_GET_DOCUMENT_BY_ID,
                CALL_TYPE_GLOBAL_GET_SCHEMA,
                CALL_TYPE_GET_SCHEMA,
                CALL_TYPE_GET_NAMESPACES,
                CALL_TYPE_GET_NEXT_PAGE,
                CALL_TYPE_INVALIDATE_NEXT_PAGE_TOKEN,
                CALL_TYPE_WRITE_SEARCH_RESULTS_TO_FILE,
                CALL_TYPE_PUT_DOCUMENTS_FROM_FILE,
                CALL_TYPE_SEARCH_SUGGESTION,
                CALL_TYPE_REPORT_SYSTEM_USAGE,
                CALL_TYPE_REPORT_USAGE,
                CALL_TYPE_GET_STORAGE_INFO,
                CALL_TYPE_REGISTER_OBSERVER_CALLBACK,
                CALL_TYPE_UNREGISTER_OBSERVER_CALLBACK,
                CALL_TYPE_GLOBAL_GET_NEXT_PAGE,
                CALL_TYPE_EXECUTE_APP_FUNCTION,
                CALL_TYPE_OPEN_WRITE_BLOB,
                CALL_TYPE_COMMIT_BLOB,
                CALL_TYPE_OPEN_READ_BLOB,
                CALL_TYPE_GLOBAL_OPEN_READ_BLOB,
                CALL_TYPE_REMOVE_BLOB,
                CALL_TYPE_SET_BLOB_VISIBILITY,
                INTERNAL_CALL_TYPE_APP_OPEN_EVENT_INDEXER,
                INTERNAL_CALL_TYPE_ISOLATED_STORAGE_DATA_MIGRATION,
                INTERNAL_CALL_TYPE_PRUNE_PACKAGE_DATA,
                INTERNAL_CALL_TYPE_CLOSE,
                INTERNAL_CALL_TYPE_PERSIST_TO_DISK_JOB));
    }
}
