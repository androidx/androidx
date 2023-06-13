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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.JoinableValueType;
import androidx.appsearch.app.SearchSpec;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class holds detailed stats for
 * {@link androidx.appsearch.app.AppSearchSession#search(String, SearchSpec)}
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SearchStats {
    @IntDef(value = {
            // Searches apps' own documents.
            VISIBILITY_SCOPE_LOCAL,
            // Searches the global documents. Including platform surfaceable and 3p-access.
            VISIBILITY_SCOPE_GLOBAL,
            VISIBILITY_SCOPE_UNKNOWN,
            // TODO(b/173532925) Add THIRD_PARTY_ACCESS once we can distinguish platform
            //  surfaceable from 3p access(right both of them are categorized as
            //  VISIBILITY_SCOPE_GLOBAL)
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface VisibilityScope {
    }

    // Searches apps' own documents.
    public static final int VISIBILITY_SCOPE_LOCAL = 1;
    // Searches the global documents. Including platform surfaceable and 3p-access.
    public static final int VISIBILITY_SCOPE_GLOBAL = 2;
    public static final int VISIBILITY_SCOPE_UNKNOWN = 3;

    // TODO(b/173532925): Add a field searchType to indicate where the search is used(normal
    //  query vs in removeByQuery vs during migration)

    @NonNull
    private final String mPackageName;
    @Nullable
    private final String mDatabase;
    /**
     * The status code returned by {@link AppSearchResult#getResultCode()} for the call or
     * internal state.
     */
    @AppSearchResult.ResultCode
    private final int mStatusCode;
    private final int mTotalLatencyMillis;
    /** Time used to rewrite the search spec. */
    private final int mRewriteSearchSpecLatencyMillis;
    /** Time used to rewrite the search results. */
    private final int mRewriteSearchResultLatencyMillis;
    /** Time passed while waiting to acquire the lock during Java function calls. */
    private final int mJavaLockAcquisitionLatencyMillis;
    /**
     * Time spent on ACL checking. This is the time spent filtering namespaces based on package
     * permissions and Android permission access.
     */
    private final int mAclCheckLatencyMillis;
    /** Defines the scope the query is searching over. */
    @VisibilityScope
    private final int mVisibilityScope;
    /** Overall time used for the native function call. */
    private final int mNativeLatencyMillis;
    /** Number of terms in the query string. */
    private final int mNativeNumTerms;
    /** Length of the query string. */
    private final int mNativeQueryLength;
    /** Number of namespaces filtered. */
    private final int mNativeNumNamespacesFiltered;
    /** Number of schema types filtered. */
    private final int mNativeNumSchemaTypesFiltered;
    /** The requested number of results in one page. */
    private final int mNativeRequestedPageSize;
    /** The actual number of results returned in the current page. */
    private final int mNativeNumResultsReturnedCurrentPage;
    /**
     * Whether the function call is querying the first page. If it's
     * not, Icing will fetch the results from cache so that some steps
     * may be skipped.
     */
    private final boolean mNativeIsFirstPage;
    /**
     * Time used to parse the query, including 2 parts: tokenizing and
     * transforming tokens into an iterator tree.
     */
    private final int mNativeParseQueryLatencyMillis;
    /** Strategy of scoring and ranking. */
    @SearchSpec.RankingStrategy
    private final int mNativeRankingStrategy;
    /** Number of documents scored. */
    private final int mNativeNumDocumentsScored;
    /** Time used to score the raw results. */
    private final int mNativeScoringLatencyMillis;
    /** Time used to rank the scored results. */
    private final int mNativeRankingLatencyMillis;
    /**
     * Time used to fetch the document protos. Note that it includes the
     * time to snippet if {@link SearchStats#mNativeNumResultsWithSnippets} is greater than 0.
     */
    private final int mNativeDocumentRetrievingLatencyMillis;
    /** How many snippets are calculated. */
    private final int mNativeNumResultsWithSnippets;
    /** Time passed while waiting to acquire the lock during native function calls. */
    private final int mNativeLockAcquisitionLatencyMillis;
    /** Time used to send data across the JNI boundary from java to native side. */
    private final int mJavaToNativeJniLatencyMillis;
    /** Time used to send data across the JNI boundary from native to java side. */
    private final int mNativeToJavaJniLatencyMillis;
    /** The type of join performed. Zero if no join is performed */
    @JoinableValueType private final int mJoinType;
    /** The total number of joined documents in the current page. */
    private final int mNativeNumJoinedResultsCurrentPage;
    /** Time taken to join documents together. */
    private final int mNativeJoinLatencyMillis;

    SearchStats(@NonNull Builder builder) {
        Preconditions.checkNotNull(builder);
        mPackageName = builder.mPackageName;
        mDatabase = builder.mDatabase;
        mStatusCode = builder.mStatusCode;
        mTotalLatencyMillis = builder.mTotalLatencyMillis;
        mRewriteSearchSpecLatencyMillis = builder.mRewriteSearchSpecLatencyMillis;
        mRewriteSearchResultLatencyMillis = builder.mRewriteSearchResultLatencyMillis;
        mJavaLockAcquisitionLatencyMillis = builder.mJavaLockAcquisitionLatencyMillis;
        mAclCheckLatencyMillis = builder.mAclCheckLatencyMillis;
        mVisibilityScope = builder.mVisibilityScope;
        mNativeLatencyMillis = builder.mNativeLatencyMillis;
        mNativeNumTerms = builder.mNativeNumTerms;
        mNativeQueryLength = builder.mNativeQueryLength;
        mNativeNumNamespacesFiltered = builder.mNativeNumNamespacesFiltered;
        mNativeNumSchemaTypesFiltered = builder.mNativeNumSchemaTypesFiltered;
        mNativeRequestedPageSize = builder.mNativeRequestedPageSize;
        mNativeNumResultsReturnedCurrentPage = builder.mNativeNumResultsReturnedCurrentPage;
        mNativeIsFirstPage = builder.mNativeIsFirstPage;
        mNativeParseQueryLatencyMillis = builder.mNativeParseQueryLatencyMillis;
        mNativeRankingStrategy = builder.mNativeRankingStrategy;
        mNativeNumDocumentsScored = builder.mNativeNumDocumentsScored;
        mNativeScoringLatencyMillis = builder.mNativeScoringLatencyMillis;
        mNativeRankingLatencyMillis = builder.mNativeRankingLatencyMillis;
        mNativeNumResultsWithSnippets = builder.mNativeNumResultsWithSnippets;
        mNativeDocumentRetrievingLatencyMillis = builder.mNativeDocumentRetrievingLatencyMillis;
        mNativeLockAcquisitionLatencyMillis = builder.mNativeLockAcquisitionLatencyMillis;
        mJavaToNativeJniLatencyMillis = builder.mJavaToNativeJniLatencyMillis;
        mNativeToJavaJniLatencyMillis = builder.mNativeToJavaJniLatencyMillis;
        mJoinType = builder.mJoinType;
        mNativeNumJoinedResultsCurrentPage = builder.mNativeNumJoinedResultsCurrentPage;
        mNativeJoinLatencyMillis = builder.mNativeJoinLatencyMillis;
    }

    /** Returns the package name of the session. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns the database name of the session.
     *
     * @return database name used by the session. {@code null} if and only if it is a
     * global search(visibilityScope is {@link SearchStats#VISIBILITY_SCOPE_GLOBAL}).
     */
    @Nullable
    public String getDatabase() {
        return mDatabase;
    }

    /** Returns status of the search. */
    @AppSearchResult.ResultCode
    public int getStatusCode() {
        return mStatusCode;
    }

    /** Returns the total latency of the search. */
    public int getTotalLatencyMillis() {
        return mTotalLatencyMillis;
    }

    /** Returns how much time spent on rewriting the {@link SearchSpec}. */
    public int getRewriteSearchSpecLatencyMillis() {
        return mRewriteSearchSpecLatencyMillis;
    }

    /** Returns how much time spent on rewriting the {@link androidx.appsearch.app.SearchResult}. */
    public int getRewriteSearchResultLatencyMillis() {
        return mRewriteSearchResultLatencyMillis;
    }

    /** Returns time passed while waiting to acquire the lock during Java function calls */
    public int getJavaLockAcquisitionLatencyMillis() {
        return mJavaLockAcquisitionLatencyMillis;
    }

    /**
     * Returns time spent on ACL checking, which is the time spent filtering namespaces based on
     * package permissions and Android permission access.
     */
    public int getAclCheckLatencyMillis() {
        return mAclCheckLatencyMillis;
    }

    /** Returns the visibility scope of the search. */
    @VisibilityScope
    public int getVisibilityScope() {
        return mVisibilityScope;
    }

    /** Returns how much time spent on the native calls. */
    public int getNativeLatencyMillis() {
        return mNativeLatencyMillis;
    }

    /** Returns number of terms in the search string. */
    public int getTermCount() {
        return mNativeNumTerms;
    }

    /** Returns the length of the search string. */
    public int getQueryLength() {
        return mNativeQueryLength;
    }

    /** Returns number of namespaces filtered. */
    public int getFilteredNamespaceCount() {
        return mNativeNumNamespacesFiltered;
    }

    /** Returns number of schema types filtered. */
    public int getFilteredSchemaTypeCount() {
        return mNativeNumSchemaTypesFiltered;
    }

    /** Returns the requested number of results in one page. */
    public int getRequestedPageSize() {
        return mNativeRequestedPageSize;
    }

    /** Returns the actual number of results returned in the current page. */
    public int getCurrentPageReturnedResultCount() {
        return mNativeNumResultsReturnedCurrentPage;
    }

    // TODO(b/185184738) Make it an integer to show how many pages having been returned.
    /** Returns whether the function call is querying the first page. */
    public boolean isFirstPage() {
        return mNativeIsFirstPage;
    }

    /**
     * Returns time used to parse the query, including 2 parts: tokenizing and transforming
     * tokens into an iterator tree.
     */
    public int getParseQueryLatencyMillis() {
        return mNativeParseQueryLatencyMillis;
    }

    /** Returns strategy of scoring and ranking. */
    @SearchSpec.RankingStrategy
    public int getRankingStrategy() {
        return mNativeRankingStrategy;
    }

    /** Returns number of documents scored. */
    public int getScoredDocumentCount() {
        return mNativeNumDocumentsScored;
    }

    /** Returns time used to score the raw results. */
    public int getScoringLatencyMillis() {
        return mNativeScoringLatencyMillis;
    }

    /** Returns time used to rank the scored results. */
    public int getRankingLatencyMillis() {
        return mNativeRankingLatencyMillis;
    }

    /**
     * Returns time used to fetch the document protos. Note that it includes the
     * time to snippet if {@link SearchStats#mNativeNumResultsWithSnippets} is not zero.
     */
    public int getDocumentRetrievingLatencyMillis() {
        return mNativeDocumentRetrievingLatencyMillis;
    }

    /** Returns the number of the results in the page returned were snippeted. */
    public int getResultWithSnippetsCount() {
        return mNativeNumResultsWithSnippets;
    }

    /** Returns time passed while waiting to acquire the lock during native function calls. */
    public int getNativeLockAcquisitionLatencyMillis() {
        return mNativeLockAcquisitionLatencyMillis;
    }

    /** Returns time used to send data across the JNI boundary from java to native side. */
    public int getJavaToNativeJniLatencyMillis() {
        return mJavaToNativeJniLatencyMillis;
    }

    /** Returns time used to send data across the JNI boundary from native to java side. */
    public int getNativeToJavaJniLatencyMillis() {
        return mNativeToJavaJniLatencyMillis;
    }

    /** Returns the type of join performed. Blank if no join is performed */
    public @JoinableValueType int getJoinType() {
        return mJoinType;
    }

    /** Returns the total number of joined documents in the current page. */
    public int getNumJoinedResultsCurrentPage() {
        return mNativeNumJoinedResultsCurrentPage;
    }

    /** Returns the time taken to join documents together. */
    public int getJoinLatencyMillis() {
        return mNativeJoinLatencyMillis;
    }

    /** Builder for {@link SearchStats} */
    public static class Builder {
        @NonNull
        final String mPackageName;
        @Nullable
        String mDatabase;
        @AppSearchResult.ResultCode
        int mStatusCode;
        int mTotalLatencyMillis;
        int mRewriteSearchSpecLatencyMillis;
        int mRewriteSearchResultLatencyMillis;
        int mJavaLockAcquisitionLatencyMillis;
        int mAclCheckLatencyMillis;
        int mVisibilityScope;
        int mNativeLatencyMillis;
        int mNativeNumTerms;
        int mNativeQueryLength;
        int mNativeNumNamespacesFiltered;
        int mNativeNumSchemaTypesFiltered;
        int mNativeRequestedPageSize;
        int mNativeNumResultsReturnedCurrentPage;
        boolean mNativeIsFirstPage;
        int mNativeParseQueryLatencyMillis;
        int mNativeRankingStrategy;
        int mNativeNumDocumentsScored;
        int mNativeScoringLatencyMillis;
        int mNativeRankingLatencyMillis;
        int mNativeNumResultsWithSnippets;
        int mNativeDocumentRetrievingLatencyMillis;
        int mNativeLockAcquisitionLatencyMillis;
        int mJavaToNativeJniLatencyMillis;
        int mNativeToJavaJniLatencyMillis;
        @JoinableValueType int mJoinType;
        int mNativeNumJoinedResultsCurrentPage;
        int mNativeJoinLatencyMillis;

        /**
         * Constructor
         *
         * @param visibilityScope scope for the corresponding search.
         * @param packageName     name of the calling package.
         */
        public Builder(@VisibilityScope int visibilityScope, @NonNull String packageName) {
            mVisibilityScope = visibilityScope;
            mPackageName = Preconditions.checkNotNull(packageName);
        }

        /** Sets the database used by the session. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setDatabase(@Nullable String database) {
            mDatabase = database;
            return this;
        }

        /** Sets the status of the search. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets total latency for the search. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mTotalLatencyMillis = totalLatencyMillis;
            return this;
        }

        /** Sets time used to rewrite the search spec. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRewriteSearchSpecLatencyMillis(int rewriteSearchSpecLatencyMillis) {
            mRewriteSearchSpecLatencyMillis = rewriteSearchSpecLatencyMillis;
            return this;
        }

        /** Sets time used to rewrite the search results. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRewriteSearchResultLatencyMillis(int rewriteSearchResultLatencyMillis) {
            mRewriteSearchResultLatencyMillis = rewriteSearchResultLatencyMillis;
            return this;
        }

        /** Sets time passed while waiting to acquire the lock during Java function calls. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setJavaLockAcquisitionLatencyMillis(int javaLockAcquisitionLatencyMillis) {
            mJavaLockAcquisitionLatencyMillis = javaLockAcquisitionLatencyMillis;
            return this;
        }

        /**
         * Sets time spent on ACL checking, which is the time spent filtering namespaces based on
         * package permissions and Android permission access.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setAclCheckLatencyMillis(int aclCheckLatencyMillis) {
            mAclCheckLatencyMillis = aclCheckLatencyMillis;
            return this;
        }

        /** Sets overall time used for the native function calls. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setNativeLatencyMillis(int nativeLatencyMillis) {
            mNativeLatencyMillis = nativeLatencyMillis;
            return this;
        }

        /** Sets number of terms in the search string. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTermCount(int termCount) {
            mNativeNumTerms = termCount;
            return this;
        }

        /** Sets length of the search string. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setQueryLength(int queryLength) {
            mNativeQueryLength = queryLength;
            return this;
        }

        /** Sets number of namespaces filtered. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setFilteredNamespaceCount(int filteredNamespaceCount) {
            mNativeNumNamespacesFiltered = filteredNamespaceCount;
            return this;
        }

        /** Sets number of schema types filtered. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setFilteredSchemaTypeCount(int filteredSchemaTypeCount) {
            mNativeNumSchemaTypesFiltered = filteredSchemaTypeCount;
            return this;
        }

        /** Sets the requested number of results in one page. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRequestedPageSize(int requestedPageSize) {
            mNativeRequestedPageSize = requestedPageSize;
            return this;
        }

        /** Sets the actual number of results returned in the current page. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setCurrentPageReturnedResultCount(
                int currentPageReturnedResultCount) {
            mNativeNumResultsReturnedCurrentPage = currentPageReturnedResultCount;
            return this;
        }

        /**
         * Sets whether the function call is querying the first page. If it's
         * not, Icing will fetch the results from cache so that some steps
         * may be skipped.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setIsFirstPage(boolean nativeIsFirstPage) {
            mNativeIsFirstPage = nativeIsFirstPage;
            return this;
        }

        /**
         * Sets time used to parse the query, including 2 parts: tokenizing and
         * transforming tokens into an iterator tree.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setParseQueryLatencyMillis(int parseQueryLatencyMillis) {
            mNativeParseQueryLatencyMillis = parseQueryLatencyMillis;
            return this;
        }

        /** Sets strategy of scoring and ranking. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRankingStrategy(
                @SearchSpec.RankingStrategy int rankingStrategy) {
            mNativeRankingStrategy = rankingStrategy;
            return this;
        }

        /** Sets number of documents scored. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setScoredDocumentCount(int scoredDocumentCount) {
            mNativeNumDocumentsScored = scoredDocumentCount;
            return this;
        }

        /** Sets time used to score the raw results. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setScoringLatencyMillis(int scoringLatencyMillis) {
            mNativeScoringLatencyMillis = scoringLatencyMillis;
            return this;
        }

        /** Sets time used to rank the scored results. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRankingLatencyMillis(int rankingLatencyMillis) {
            mNativeRankingLatencyMillis = rankingLatencyMillis;
            return this;
        }

        /** Sets time used to fetch the document protos. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setDocumentRetrievingLatencyMillis(
                int documentRetrievingLatencyMillis) {
            mNativeDocumentRetrievingLatencyMillis = documentRetrievingLatencyMillis;
            return this;
        }

        /** Sets how many snippets are calculated. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setResultWithSnippetsCount(int resultWithSnippetsCount) {
            mNativeNumResultsWithSnippets = resultWithSnippetsCount;
            return this;
        }

        /** Sets time passed while waiting to acquire the lock during native function calls. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setNativeLockAcquisitionLatencyMillis(
                int nativeLockAcquisitionLatencyMillis) {
            mNativeLockAcquisitionLatencyMillis = nativeLockAcquisitionLatencyMillis;
            return this;
        }

        /** Sets time used to send data across the JNI boundary from java to native side. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setJavaToNativeJniLatencyMillis(int javaToNativeJniLatencyMillis) {
            mJavaToNativeJniLatencyMillis = javaToNativeJniLatencyMillis;
            return this;
        }

        /** Sets time used to send data across the JNI boundary from native to java side. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setNativeToJavaJniLatencyMillis(int nativeToJavaJniLatencyMillis) {
            mNativeToJavaJniLatencyMillis = nativeToJavaJniLatencyMillis;
            return this;
        }

        /** Sets whether or not this is a join query */
        @NonNull
        public Builder setJoinType(@JoinableValueType int joinType) {
            mJoinType = joinType;
            return this;
        }

        /** Set the total number of joined documents in a page. */
        @NonNull
        public Builder setNativeNumJoinedResultsCurrentPage(int nativeNumJoinedResultsCurrentPage) {
            mNativeNumJoinedResultsCurrentPage = nativeNumJoinedResultsCurrentPage;
            return this;
        }

        /** Sets time it takes to join documents together in icing. */
        @NonNull
        public Builder setNativeJoinLatencyMillis(int nativeJoinLatencyMillis) {
            mNativeJoinLatencyMillis = nativeJoinLatencyMillis;
            return this;
        }

        /**
         * Constructs a new {@link SearchStats} from the contents of this
         * {@link SearchStats.Builder}.
         */
        @NonNull
        public SearchStats build() {
            if (mDatabase == null) {
                Preconditions.checkState(mVisibilityScope != SearchStats.VISIBILITY_SCOPE_LOCAL,
                        "database can not be null if visibilityScope is local.");
            }

            return new SearchStats(/* builder= */ this);
        }
    }
}
