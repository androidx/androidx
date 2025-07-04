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
import androidx.appsearch.app.RemoveByDocumentIdRequest;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.stats.BaseStats;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class holds detailed stats for
 * {@link androidx.appsearch.app.AppSearchSession#removeAsync(RemoveByDocumentIdRequest)} and
 * {@link androidx.appsearch.app.AppSearchSession#removeAsync(String, SearchSpec)}
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RemoveStats extends BaseStats {
    /** Types of stats available for remove API. */
    @IntDef(value = {
            // It needs to be sync with DeleteType.Code in
            // external/icing/proto/icing/proto/logging.proto#DeleteStatsProto
            UNKNOWN,
            SINGLE,
            QUERY,
            NAMESPACE,
            SCHEMA_TYPE,

    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeleteType {
    }

    /** Default. Should never be used. */
    public static final int UNKNOWN = 0;
    /** Delete by namespace + id. */
    public static final int SINGLE = 1;
    /** Delete by query. */
    public static final int QUERY = 2;
    /** Delete by namespace. */
    public static final int NAMESPACE = 3;
    /** Delete by schema type. */
    public static final int SCHEMA_TYPE = 4;

    private final @NonNull String mPackageName;
    private final @NonNull String mDatabase;
    /**
     * The status code returned by {@link AppSearchResult#getResultCode()} for the call or
     * internal state.
     */
    @AppSearchResult.ResultCode
    private final int mStatusCode;
    private final int mTotalLatencyMillis;
    private final int mNativeLatencyMillis;
    @DeleteType
    private final int mNativeDeleteType;
    private final int mNativeNumDocumentsDeleted;
    private final int mQueryLength;
    private final int mNumTerms;
    private final int mNumNamespacesFiltered;
    private final int mNumSchemaTypesFiltered;
    private final int mParseQueryLatencyMillis;
    private final int mDocumentRemovalLatencyMillis;


    RemoveStats(@NonNull Builder builder) {
        super(builder);
        mPackageName = builder.mPackageName;
        mDatabase = builder.mDatabase;
        mStatusCode = builder.mStatusCode;
        mTotalLatencyMillis = builder.mTotalLatencyMillis;
        mNativeLatencyMillis = builder.mNativeLatencyMillis;
        mNativeDeleteType = builder.mNativeDeleteType;
        mNativeNumDocumentsDeleted = builder.mNativeNumDocumentsDeleted;
        mQueryLength = builder.mQueryLength;
        mNumTerms = builder.mNumTerms;
        mNumNamespacesFiltered = builder.mNumNamespacesFiltered;
        mNumSchemaTypesFiltered = builder.mNumSchemaTypesFiltered;
        mParseQueryLatencyMillis = builder.mParseQueryLatencyMillis;
        mDocumentRemovalLatencyMillis = builder.mDocumentRemovalLatencyMillis;
    }

    /** Returns calling package name. */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /** Returns calling database name. */
    public @NonNull String getDatabase() {
        return mDatabase;
    }

    /** Returns status code for this remove. */
    @AppSearchResult.ResultCode
    public int getStatusCode() {
        return mStatusCode;
    }

    /** Returns total latency of this remove in millis. */
    public int getTotalLatencyMillis() {
        return mTotalLatencyMillis;
    }

    /** Returns how much time in millis spent in the native code. */
    public int getNativeLatencyMillis() {
        return mNativeLatencyMillis;
    }

    /** Returns what type of delete for this remove call. */
    @DeleteType
    public int getDeleteType() {
        return mNativeDeleteType;
    }

    /** Returns how many documents get deleted in this call. */
    public int getDeletedDocumentCount() {
        return mNativeNumDocumentsDeleted;
    }

    /** The UTF-8 length of the query string. */
    public int getQueryLength() {
        return mQueryLength;
    }

    /** Number of terms in the query string. */
    public int getNumTerms() {
        return mNumTerms;
    }

    /** Number of namespaces filtered. */
    public int getNumNamespacesFiltered() {
        return mNumNamespacesFiltered;
    }

    /** Number of schema types filtered.. */
    public int getNumSchemaTypesFiltered() {
        return mNumSchemaTypesFiltered;
    }

    /**
     *  Returns the time used to parse the query, including 2 parts: tokenizing and transforming
     *  tokens into an iterator tree.
     */
    public int getParseQueryLatencyMillis() {
        return mParseQueryLatencyMillis;
    }

    /** Returns the time used to delete each document */
    public int getDocumentRemovalLatencyMillis() {
        return mDocumentRemovalLatencyMillis;
    }

    /** Builder for {@link RemoveStats}. */
    public static class Builder extends BaseStats.Builder<RemoveStats.Builder> {
        final @NonNull String mPackageName;
        final @NonNull String mDatabase;
        @AppSearchResult.ResultCode
        int mStatusCode;
        int mTotalLatencyMillis;
        int mNativeLatencyMillis;
        @DeleteType
        int mNativeDeleteType;
        int mNativeNumDocumentsDeleted;
        int mQueryLength;
        int mNumTerms;
        int mNumNamespacesFiltered;
        int mNumSchemaTypesFiltered;
        int mParseQueryLatencyMillis;
        int mDocumentRemovalLatencyMillis;

        /** Constructor for the {@link Builder}. */
        public Builder(@NonNull String packageName, @NonNull String database) {
            mPackageName = Preconditions.checkNotNull(packageName);
            mDatabase = Preconditions.checkNotNull(database);
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

        /** Sets native latency in millis. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeLatencyMillis(int nativeLatencyMillis) {
            mNativeLatencyMillis = nativeLatencyMillis;
            return this;
        }

        /** Sets delete type for this call. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDeleteType(@DeleteType int nativeDeleteType) {
            mNativeDeleteType = nativeDeleteType;
            return this;
        }

        /** Sets how many documents get deleted for this call. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDeletedDocumentCount(int nativeNumDocumentsDeleted) {
            mNativeNumDocumentsDeleted = nativeNumDocumentsDeleted;
            return this;
        }

        /** Sets the UTF-8 length of the query string. */
        @CanIgnoreReturnValue
        public @NonNull Builder setQueryLength(int queryLength) {
            mQueryLength = queryLength;
            return this;
        }

        /** Sets number of terms in the query string. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNumTerms(int numTerms) {
            mNumTerms = numTerms;
            return this;
        }

        /** Sets number of namespaces filtered. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNumNamespacesFiltered(int numNamespacesFiltered) {
            mNumNamespacesFiltered = numNamespacesFiltered;
            return this;
        }

        /** Sets number of schema types filtered. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNumSchemaTypesFiltered(int numSchemaTypesFiltered) {
            mNumSchemaTypesFiltered = numSchemaTypesFiltered;
            return this;
        }

        /**
         * Sets time used to parse the query, including 2 parts: tokenizing and transforming tokens
         * into an iterator tree.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setParseQueryLatencyMillis(int parseQueryLatencyMillis) {
            mParseQueryLatencyMillis = parseQueryLatencyMillis;
            return this;
        }

        /** Sets Time used to delete each document. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDocumentRemovalLatencyMillis(int documentRemovalLatencyMillis) {
            mDocumentRemovalLatencyMillis = documentRemovalLatencyMillis;
            return this;
        }

        /** Creates a {@link RemoveStats}. */
        @Override
        public @NonNull RemoveStats build() {
            return new RemoveStats(/* builder= */ this);
        }
    }
}
