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
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.BaseStats;
import androidx.collection.ArraySet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
    /** Call types. */
    @IntDef(value = {
            CALL_TYPE_UNKNOWN,
            CALL_TYPE_INITIALIZE,
            CALL_TYPE_SET_SCHEMA,
            CALL_TYPE_PUT_DOCUMENTS,
            CALL_TYPE_GET_DOCUMENTS,
            CALL_TYPE_REMOVE_DOCUMENTS_BY_ID,
            CALL_TYPE_PUT_DOCUMENT,
            CALL_TYPE_GET_DOCUMENT,
            CALL_TYPE_REMOVE_DOCUMENT_BY_ID,
            CALL_TYPE_SEARCH,
            CALL_TYPE_OPTIMIZE,
            CALL_TYPE_FLUSH,
            CALL_TYPE_GLOBAL_SEARCH,
            CALL_TYPE_REMOVE_DOCUMENTS_BY_SEARCH,
            CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH,
            CALL_TYPE_GLOBAL_GET_DOCUMENT_BY_ID,
            CALL_TYPE_SCHEMA_MIGRATION,
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
            CALL_TYPE_SET_BLOB_VISIBILITY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CallType {
    }

    public static final int CALL_TYPE_UNKNOWN = 0;
    public static final int CALL_TYPE_INITIALIZE = 1;
    public static final int CALL_TYPE_SET_SCHEMA = 2;
    public static final int CALL_TYPE_PUT_DOCUMENTS = 3;
    public static final int CALL_TYPE_GET_DOCUMENTS = 4;
    public static final int CALL_TYPE_REMOVE_DOCUMENTS_BY_ID = 5;
    public static final int CALL_TYPE_PUT_DOCUMENT = 6;
    public static final int CALL_TYPE_GET_DOCUMENT = 7;
    public static final int CALL_TYPE_REMOVE_DOCUMENT_BY_ID = 8;
    public static final int CALL_TYPE_SEARCH = 9;
    public static final int CALL_TYPE_OPTIMIZE = 10;
    public static final int CALL_TYPE_FLUSH = 11;
    public static final int CALL_TYPE_GLOBAL_SEARCH = 12;
    public static final int CALL_TYPE_REMOVE_DOCUMENTS_BY_SEARCH = 13;
    public static final int CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH = 14;
    public static final int CALL_TYPE_GLOBAL_GET_DOCUMENT_BY_ID = 15;
    public static final int CALL_TYPE_SCHEMA_MIGRATION = 16;
    public static final int CALL_TYPE_GLOBAL_GET_SCHEMA = 17;
    public static final int CALL_TYPE_GET_SCHEMA = 18;
    public static final int CALL_TYPE_GET_NAMESPACES = 19;
    public static final int CALL_TYPE_GET_NEXT_PAGE = 20;
    public static final int CALL_TYPE_INVALIDATE_NEXT_PAGE_TOKEN = 21;
    public static final int CALL_TYPE_WRITE_SEARCH_RESULTS_TO_FILE = 22;
    public static final int CALL_TYPE_PUT_DOCUMENTS_FROM_FILE = 23;
    public static final int CALL_TYPE_SEARCH_SUGGESTION = 24;
    public static final int CALL_TYPE_REPORT_SYSTEM_USAGE = 25;
    public static final int CALL_TYPE_REPORT_USAGE = 26;
    public static final int CALL_TYPE_GET_STORAGE_INFO = 27;
    public static final int CALL_TYPE_REGISTER_OBSERVER_CALLBACK = 28;
    public static final int CALL_TYPE_UNREGISTER_OBSERVER_CALLBACK = 29;
    public static final int CALL_TYPE_GLOBAL_GET_NEXT_PAGE = 30;
    public static final int CALL_TYPE_EXECUTE_APP_FUNCTION = 31;
    public static final int CALL_TYPE_OPEN_WRITE_BLOB = 32;
    public static final int CALL_TYPE_COMMIT_BLOB = 33;
    public static final int CALL_TYPE_OPEN_READ_BLOB = 34;
    public static final int CALL_TYPE_GLOBAL_OPEN_READ_BLOB = 35;
    public static final int CALL_TYPE_REMOVE_BLOB = 36;
    public static final int CALL_TYPE_SET_BLOB_VISIBILITY = 37;
    // Most call types are for AppSearchManager APIs. This call type is for internal calls, such
    // as from indexers.
    public static final int INTERNAL_CALL_TYPE_APP_OPEN_EVENT_INDEXER = 38;


    // These strings are for the subset of call types that correspond to an AppSearchManager API
    private static final String CALL_TYPE_STRING_INITIALIZE = "initialize";
    private static final String CALL_TYPE_STRING_SET_SCHEMA = "localSetSchema";
    private static final String CALL_TYPE_STRING_PUT_DOCUMENTS = "localPutDocuments";
    private static final String CALL_TYPE_STRING_GET_DOCUMENTS = "localGetDocuments";
    private static final String CALL_TYPE_STRING_REMOVE_DOCUMENTS_BY_ID = "localRemoveByDocumentId";
    private static final String CALL_TYPE_STRING_SEARCH = "localSearch";
    private static final String CALL_TYPE_STRING_FLUSH = "flush";
    private static final String CALL_TYPE_STRING_GLOBAL_SEARCH = "globalSearch";
    private static final String CALL_TYPE_STRING_REMOVE_DOCUMENTS_BY_SEARCH = "localRemoveBySearch";
    private static final String CALL_TYPE_STRING_GLOBAL_GET_DOCUMENT_BY_ID = "globalGetDocuments";
    private static final String CALL_TYPE_STRING_GLOBAL_GET_SCHEMA = "globalGetSchema";
    private static final String CALL_TYPE_STRING_GET_SCHEMA = "localGetSchema";
    private static final String CALL_TYPE_STRING_GET_NAMESPACES = "localGetNamespaces";
    private static final String CALL_TYPE_STRING_GET_NEXT_PAGE = "localGetNextPage";
    private static final String CALL_TYPE_STRING_INVALIDATE_NEXT_PAGE_TOKEN =
            "invalidateNextPageToken";
    private static final String CALL_TYPE_STRING_WRITE_SEARCH_RESULTS_TO_FILE =
            "localWriteSearchResultsToFile";
    private static final String CALL_TYPE_STRING_PUT_DOCUMENTS_FROM_FILE =
            "localPutDocumentsFromFile";
    private static final String CALL_TYPE_STRING_SEARCH_SUGGESTION = "localSearchSuggestion";
    private static final String CALL_TYPE_STRING_REPORT_SYSTEM_USAGE = "globalReportUsage";
    private static final String CALL_TYPE_STRING_REPORT_USAGE = "localReportUsage";
    private static final String CALL_TYPE_STRING_GET_STORAGE_INFO = "localGetStorageInfo";
    private static final String CALL_TYPE_STRING_REGISTER_OBSERVER_CALLBACK =
            "globalRegisterObserverCallback";
    private static final String CALL_TYPE_STRING_UNREGISTER_OBSERVER_CALLBACK =
            "globalUnregisterObserverCallback";
    private static final String CALL_TYPE_STRING_GLOBAL_GET_NEXT_PAGE = "globalGetNextPage";
    private static final String CALL_TYPE_STRING_EXECUTE_APP_FUNCTION = "executeAppFunction";
    private static final String CALL_TYPE_STRING_OPEN_WRITE_BLOB = "openWriteBlob";
    private static final String CALL_TYPE_STRING_COMMIT_BLOB = "commitBlob";
    private static final String CALL_TYPE_STRING_OPEN_READ_BLOB = "openReadBlob";
    private static final String CALL_TYPE_STRING_GLOBAL_OPEN_READ_BLOB = "globalOpenReadBlob";
    private static final String CALL_TYPE_STRING_REMOVE_BLOB = "removeBlob";
    private static final String CALL_TYPE_STRING_SET_BLOB_VISIBILITY = "setBlobVisibility";

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

        /** Creates {@link CallStats} object from {@link Builder} instance. */
        @Override
        public @NonNull CallStats build() {
            return new CallStats(/* builder= */ this);
        }
    }

    /**
     * Returns the {@link CallStats.CallType} represented by the given AppSearchManager API name. If
     * an unknown name is provided, {@link CallStats.CallType#CALL_TYPE_UNKNOWN} is returned.
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
                CALL_TYPE_SET_BLOB_VISIBILITY));
    }
}
