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
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.JoinableValueType;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.stats.BaseStats;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class holds detailed stats for
 * {@link androidx.appsearch.app.AppSearchSession#search(String, SearchSpec)}
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class QueryStats extends BaseStats {
    /** Types of Visibility scopes available for search. */
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

    /** Types of page result for search. */
    @IntDef(value = {
            PAGE_TOKEN_TYPE_NONE,
            PAGE_TOKEN_TYPE_VALID,
            PAGE_TOKEN_TYPE_NOT_FOUND,
            PAGE_TOKEN_TYPE_EMPTY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PageTokenType {
    }

    // Default. Usually used when it is the first page.
    public static final int PAGE_TOKEN_TYPE_NONE = 0;
    public static final int PAGE_TOKEN_TYPE_VALID = 1;
    // The current page token is not found in ResultStateManager. This is
    // usually caused by cache eviction.
    public static final int PAGE_TOKEN_TYPE_NOT_FOUND = 2;
    // The current page token is empty (kInvalidNextPageToken).
    public static final int PAGE_TOKEN_TYPE_EMPTY = 3;

    // Searches apps' own documents.
    public static final int VISIBILITY_SCOPE_LOCAL = 1;
    // Searches the global documents. Including platform surfaceable and 3p-access.
    public static final int VISIBILITY_SCOPE_GLOBAL = 2;
    public static final int VISIBILITY_SCOPE_UNKNOWN = 3;

    // TODO(b/173532925): Add a field searchType to indicate where the search is used(normal
    //  query vs in removeByQuery vs during migration)

    private final @NonNull String mPackageName;
    private final @Nullable String mDatabase;
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
    private final @Nullable String mSearchSourceLogTag;

    /**
     * Whether the function call is querying the first page. If it's
     * not, Icing will fetch the results from cache so that some steps
     * may be skipped.
     */
    private final boolean mNativeIsFirstPage;
    /**
     * The number of additional pages retrieved after the first page if it did not retrieve
     * enough results.
     */
    private final int mAdditionalPageCount;
    /** The requested number of results in one page. */
    private final int mNativeRequestedPageSize;
    /** The actual number of results returned in the current page. */
    private final int mNativeNumResultsReturnedCurrentPage;
    /**
     * The number of results returned in all additional pages that are retrieved if the first
     * page did not retrieve enough results.
     */
    private final int mNumResultsReturnedAdditionalPages;
    /** Overall time used for the native function call. */
    private final int mNativeLatencyMillis;
    /** Overall time used for the first native search call. */
    private final int mFirstNativeCallLatencyMillis;
    /**
     * Overall time used for retrieving additional pages if the first page did not retrieve
     * enough results.
     */
    private final int mAdditionalPageRetrievalLatencyMillis;
    /** Time used to rank the scored results. */
    private final int mNativeRankingLatencyMillis;
    /**
     * Time used to fetch the document protos. Note that it includes the
     * time to snippet if {@link QueryStats#mNativeNumResultsWithSnippets} is greater than 0.
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
    /** Time taken to join documents together. */
    private final int mNativeJoinLatencyMillis;
    /** The total number of joined documents in the current page. */
    private final int mNativeNumJoinedResultsCurrentPage;
    /** The type of join performed. Zero if no join is performed */
    @JoinableValueType private final int mJoinType;

    private final SearchStats mParentSearchStats;
    private final SearchStats mChildSearchStats;
    private final long mLiteIndexHitBufferByteSize;
    private final long mLiteIndexHitBufferUnsortedByteSize;
    // The type of the input page token.
    @PageTokenType int mPageTokenType;
    // Number of result states being force-evicted from ResultStateManager due to
    // budget limit. This doesn't include expired or invalidated states.
    int mNumResultStatesEvicted;

    QueryStats(@NonNull Builder builder) {
        super(builder);
        mPackageName = builder.mPackageName;
        mDatabase = builder.mDatabase;
        mStatusCode = builder.mStatusCode;
        mTotalLatencyMillis = builder.mTotalLatencyMillis;
        mRewriteSearchSpecLatencyMillis = builder.mRewriteSearchSpecLatencyMillis;
        mRewriteSearchResultLatencyMillis = builder.mRewriteSearchResultLatencyMillis;
        mJavaLockAcquisitionLatencyMillis = builder.mJavaLockAcquisitionLatencyMillis;
        mAclCheckLatencyMillis = builder.mAclCheckLatencyMillis;
        mVisibilityScope = builder.mVisibilityScope;
        mSearchSourceLogTag = builder.mSearchSourceLogTag;
        mNativeIsFirstPage = builder.mNativeIsFirstPage;
        mAdditionalPageCount = builder.mAdditionalPageCount;
        mNativeRequestedPageSize = builder.mNativeRequestedPageSize;
        mNativeNumResultsReturnedCurrentPage = builder.mNativeNumResultsReturnedCurrentPage;
        mNumResultsReturnedAdditionalPages = builder.mNumResultsReturnedAdditionalPages;
        mNativeLatencyMillis = builder.mNativeLatencyMillis;
        mFirstNativeCallLatencyMillis = builder.mFirstNativeCallLatencyMillis;
        mAdditionalPageRetrievalLatencyMillis = builder.mAdditionalPageRetrievalLatencyMillis;
        mNativeRankingLatencyMillis = builder.mNativeRankingLatencyMillis;
        mNativeDocumentRetrievingLatencyMillis = builder.mNativeDocumentRetrievingLatencyMillis;
        mNativeNumResultsWithSnippets = builder.mNativeNumResultsWithSnippets;
        mNativeLockAcquisitionLatencyMillis = builder.mNativeLockAcquisitionLatencyMillis;
        mJavaToNativeJniLatencyMillis = builder.mJavaToNativeJniLatencyMillis;
        mNativeToJavaJniLatencyMillis = builder.mNativeToJavaJniLatencyMillis;
        mNativeJoinLatencyMillis = builder.mNativeJoinLatencyMillis;
        mNativeNumJoinedResultsCurrentPage = builder.mNativeNumJoinedResultsCurrentPage;
        mJoinType = builder.mJoinType;
        mParentSearchStats = builder.mParentSearchStats;
        mChildSearchStats = builder.mChildSearchStats;
        mLiteIndexHitBufferByteSize = builder.mLiteIndexHitBufferByteSize;
        mLiteIndexHitBufferUnsortedByteSize = builder.mLiteIndexHitBufferUnsortedByteSize;
        mPageTokenType = builder.mPageTokenType;
        mNumResultStatesEvicted = builder.mNumResultStatesEvicted;
    }

    /** Returns the package name of the session. */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns the database name of the session.
     *
     * @return database name used by the session. {@code null} if and only if it is a
     * global search(visibilityScope is {@link QueryStats#VISIBILITY_SCOPE_GLOBAL}).
     */
    public @Nullable String getDatabase() {
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

    /**  Returns a tag to indicate the source of this search, or {code null} if never set. */
    public @Nullable String getSearchSourceLogTag() {
        return mSearchSourceLogTag;
    }

    // TODO(b/185184738) Make it an integer to show how many pages having been returned.
    /** Returns whether the function call is querying the first page. */
    public boolean isFirstPage() {
        return mNativeIsFirstPage;
    }

    /**
     * Returns the number of additional pages retrieved after the first page if it did not
     * return enough results.
     */
    public int getAdditionalPageCount() {
        return mAdditionalPageCount;
    }

    /** Returns the requested number of results in one page. */
    public int getRequestedPageSize() {
        return mNativeRequestedPageSize;
    }

    /** Returns the actual number of results returned in the current page. */
    public int getCurrentPageReturnedResultCount() {
        return mNativeNumResultsReturnedCurrentPage;
    }

    /**
     * Returns the number of results returned in all additional pages that are retrieved if the
     * first page did not retrieve enough results.
     */
    public int getAdditionalPagesReturnedResultCount() {
        return mNumResultsReturnedAdditionalPages;
    }

    /** Returns how much time spent on the native calls. */
    public int getNativeLatencyMillis() {
        return mNativeLatencyMillis;
    }

    /** Returns how much time is spent on the first native search call. */
    public int getFirstNativeCallLatencyMillis() {
        return mFirstNativeCallLatencyMillis;
    }

    /**
     * Returns how much time is spent retrieving additional pages if the first page did not
     * return enough results.
     */
    public int getAdditionalPageRetrievalLatencyMillis() {
        return mAdditionalPageRetrievalLatencyMillis;
    }

    /** Returns time used to rank the scored results. */
    public int getRankingLatencyMillis() {
        return mNativeRankingLatencyMillis;
    }

    /**
     * Returns time used to fetch the document protos. Note that it includes the
     * time to snippet if {@link QueryStats#mNativeNumResultsWithSnippets} is not zero.
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

    /** Returns the time taken to join documents together. */
    public int getJoinLatencyMillis() {
        return mNativeJoinLatencyMillis;
    }

    /** Returns the total number of joined documents in the current page. */
    public int getNumJoinedResultsCurrentPage() {
        return mNativeNumJoinedResultsCurrentPage;
    }

    /** Returns the type of join performed. Blank if no join is performed */
    public @JoinableValueType int getJoinType() {
        return mJoinType;
    }

    /**
     * Returns a search stats for parent. Only valid for first page, or {code null} if never set.
     */
    public @Nullable SearchStats getParentSearchStats() {
        return mParentSearchStats;
    }

    /**
     * Returns a search stats for child. Only valid for first page, or {code null} if never set.
     */
    public @Nullable SearchStats getChildSearchStats() {
        return mChildSearchStats;
    }

    /**  Returns the byte size of the lite index hit buffer. */
    public long getLiteIndexHitBufferByteSize() {
        return mLiteIndexHitBufferByteSize;
    }

    /**  Returns the byte size of the unsorted tail of the lite index hit buffer. */
    public long getLiteIndexHitBufferUnsortedByteSize() {
        return mLiteIndexHitBufferUnsortedByteSize;
    }

    /**  Returns the type of the input page token. */
    @PageTokenType
    public int getPageTokenType() {
        return mPageTokenType;
    }

    /**  Returns the type of the input page token. */
    public int getNumResultStatesEvicted() {
        return mNumResultStatesEvicted;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "QueryStats {\n"
                        + "package=%s, database=%s, status=%d, total_latency=%d, "
                        + "rewrite_search_spec_latency=%d,\n"
                        + "rewrite_search_result_latency=%d, java_lock_acquisition_latency=%d, "
                        + "acl_check_latency=%d, visibility_score=%d,\n"
                        + "search_source_log_tag=%s, is_first_page=%b, requested_page_size=%d, "
                        + "num_results_returned_current_page=%d,\n"
                        + "native_latency=%d, ranking_latency=%d, document_retrieving_latency=%d, "
                        + "num_results_with_snippets=%d,\n"
                        + "native_lock_acquisition_latency=%d, java_to_native_jni_latency=%d, "
                        + "native_to_java_jni_latency=%d,\n"
                        + "join_latency_ms=%d, num_joined_results_current_page=%d, join_type=%d, "
                        + "lite_index_hit_buffer_byte_size=%d,\n"
                        + "lite_index_hit_buffer_unsorted_byte_size=%d\n"
                        + "page_token_type=%d, num_result_states_evicted=%d\n"
                        + "parent_search_stats=%s,\n child_search_stats=%s}",
                mPackageName,
                mDatabase,
                mStatusCode,
                mTotalLatencyMillis,
                mRewriteSearchSpecLatencyMillis,
                mRewriteSearchResultLatencyMillis,
                mJavaLockAcquisitionLatencyMillis,
                mAclCheckLatencyMillis,
                mVisibilityScope,
                mSearchSourceLogTag,
                mNativeIsFirstPage,
                mNativeRequestedPageSize,
                mNativeNumResultsReturnedCurrentPage,
                mNativeLatencyMillis,
                mNativeRankingLatencyMillis,
                mNativeDocumentRetrievingLatencyMillis,
                mNativeNumResultsWithSnippets,
                mNativeLockAcquisitionLatencyMillis,
                mJavaToNativeJniLatencyMillis,
                mNativeToJavaJniLatencyMillis,
                mNativeJoinLatencyMillis,
                mNativeNumJoinedResultsCurrentPage,
                mJoinType,
                mLiteIndexHitBufferByteSize,
                mLiteIndexHitBufferUnsortedByteSize,
                mPageTokenType,
                mNumResultStatesEvicted,
                mParentSearchStats.toString(),
                mChildSearchStats.toString());
    }
    /** Builder for {@link QueryStats} */
    public static class Builder extends BaseStats.Builder<QueryStats.Builder> {
        final @NonNull String mPackageName;
        @Nullable String mDatabase;
        @AppSearchResult.ResultCode
        int mStatusCode;
        int mTotalLatencyMillis;
        int mRewriteSearchSpecLatencyMillis;
        int mRewriteSearchResultLatencyMillis;
        int mJavaLockAcquisitionLatencyMillis;
        int mAclCheckLatencyMillis;
        int mVisibilityScope;
        @Nullable String mSearchSourceLogTag;
        boolean mNativeIsFirstPage;
        int mAdditionalPageCount;
        int mNativeRequestedPageSize;
        int mNativeNumResultsReturnedCurrentPage;
        int mNumResultsReturnedAdditionalPages;
        int mNativeLatencyMillis;
        int mFirstNativeCallLatencyMillis;
        int mAdditionalPageRetrievalLatencyMillis;
        int mNativeRankingLatencyMillis;
        int mNativeDocumentRetrievingLatencyMillis;
        int mNativeNumResultsWithSnippets;
        int mNativeLockAcquisitionLatencyMillis;
        int mJavaToNativeJniLatencyMillis;
        int mNativeToJavaJniLatencyMillis;
        int mNativeJoinLatencyMillis;
        int mNativeNumJoinedResultsCurrentPage;
        @JoinableValueType int mJoinType;
        SearchStats mParentSearchStats;
        SearchStats mChildSearchStats;
        long mLiteIndexHitBufferByteSize;
        long mLiteIndexHitBufferUnsortedByteSize;
        @PageTokenType int mPageTokenType;
        int mNumResultStatesEvicted;

        /**
         * Constructor of {@link QueryStats}.
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
        public @NonNull Builder setDatabase(@Nullable String database) {
            mDatabase = database;
            return this;
        }

        /** Sets the status of the search. */
        @CanIgnoreReturnValue
        public @NonNull Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets total latency for the search. */
        @CanIgnoreReturnValue
        public @NonNull Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mTotalLatencyMillis = totalLatencyMillis;
            return this;
        }

        /** Sets time used to rewrite the search spec. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRewriteSearchSpecLatencyMillis(
                int rewriteSearchSpecLatencyMillis) {
            mRewriteSearchSpecLatencyMillis = rewriteSearchSpecLatencyMillis;
            return this;
        }

        /** Sets time used to rewrite the search results. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRewriteSearchResultLatencyMillis(
                int rewriteSearchResultLatencyMillis) {
            mRewriteSearchResultLatencyMillis = rewriteSearchResultLatencyMillis;
            return this;
        }

        /** Sets time passed while waiting to acquire the lock during Java function calls. */
        @CanIgnoreReturnValue
        public @NonNull Builder setJavaLockAcquisitionLatencyMillis(
                int javaLockAcquisitionLatencyMillis) {
            mJavaLockAcquisitionLatencyMillis = javaLockAcquisitionLatencyMillis;
            return this;
        }

        /**
         * Sets time spent on ACL checking, which is the time spent filtering namespaces based on
         * package permissions and Android permission access.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setAclCheckLatencyMillis(int aclCheckLatencyMillis) {
            mAclCheckLatencyMillis = aclCheckLatencyMillis;
            return this;
        }

        /** Sets a tag to indicate the source of this search. */
        @CanIgnoreReturnValue
        public @NonNull Builder setSearchSourceLogTag(@Nullable String searchSourceLogTag) {
            mSearchSourceLogTag = searchSourceLogTag;
            return this;
        }

        /**
         * Sets whether the function call is querying the first page. If it's
         * not, Icing will fetch the results from cache so that some steps
         * may be skipped.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setIsFirstPage(boolean nativeIsFirstPage) {
            mNativeIsFirstPage = nativeIsFirstPage;
            return this;
        }

        /** Sets the actual number of results returned in the current page. */
        @CanIgnoreReturnValue
        public @NonNull Builder setAdditionalPagesReturnedResultCount(
                int additionalPagesReturnedResultCount) {
            mNumResultsReturnedAdditionalPages = additionalPagesReturnedResultCount;
            return this;
        }

        /** Sets the requested number of results in one page. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRequestedPageSize(int requestedPageSize) {
            mNativeRequestedPageSize = requestedPageSize;
            return this;
        }

        /** Sets the actual number of results returned in the current page. */
        @CanIgnoreReturnValue
        public @NonNull Builder setCurrentPageReturnedResultCount(
                int currentPageReturnedResultCount) {
            mNativeNumResultsReturnedCurrentPage = currentPageReturnedResultCount;
            return this;
        }

        /** Sets the number of additional pages retrieved after the first one if it did not
         * return enough results. */
        @CanIgnoreReturnValue
        public @NonNull Builder setAdditionalPageCount(int additionalPageCount) {
            mAdditionalPageCount = additionalPageCount;
            return this;
        }

        /** Sets overall time used for the native function calls. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeLatencyMillis(int nativeLatencyMillis) {
            mNativeLatencyMillis = nativeLatencyMillis;
            return this;
        }

        /** Sets time used for the first native function call. */
        @CanIgnoreReturnValue
        public @NonNull Builder setFirstNativeCallLatency(int firstNativeCallLatencyMillis) {
            mFirstNativeCallLatencyMillis = firstNativeCallLatencyMillis;
            return this;
        }

        /**
         * Sets overall time used for retrieving additional pages if the first page did not
         * return enough results.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setAdditionalPageRetrievalLatencyMillis(
                int additionalPageRetrievalLatencyMillis) {
            mAdditionalPageRetrievalLatencyMillis = additionalPageRetrievalLatencyMillis;
            return this;
        }

        /** Sets time used to rank the scored results. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRankingLatencyMillis(int rankingLatencyMillis) {
            mNativeRankingLatencyMillis = rankingLatencyMillis;
            return this;
        }

        /** Sets time used to fetch the document protos. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDocumentRetrievingLatencyMillis(
                int documentRetrievingLatencyMillis) {
            mNativeDocumentRetrievingLatencyMillis = documentRetrievingLatencyMillis;
            return this;
        }

        /** Sets how many snippets are calculated. */
        @CanIgnoreReturnValue
        public @NonNull Builder setResultWithSnippetsCount(int resultWithSnippetsCount) {
            mNativeNumResultsWithSnippets = resultWithSnippetsCount;
            return this;
        }

        /** Sets time passed while waiting to acquire the lock during native function calls. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeLockAcquisitionLatencyMillis(
                int nativeLockAcquisitionLatencyMillis) {
            mNativeLockAcquisitionLatencyMillis = nativeLockAcquisitionLatencyMillis;
            return this;
        }

        /** Sets time used to send data across the JNI boundary from java to native side. */
        @CanIgnoreReturnValue
        public @NonNull Builder setJavaToNativeJniLatencyMillis(int javaToNativeJniLatencyMillis) {
            mJavaToNativeJniLatencyMillis = javaToNativeJniLatencyMillis;
            return this;
        }

        /** Sets time used to send data across the JNI boundary from native to java side. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeToJavaJniLatencyMillis(int nativeToJavaJniLatencyMillis) {
            mNativeToJavaJniLatencyMillis = nativeToJavaJniLatencyMillis;
            return this;
        }

        /** Sets time it takes to join documents together in icing. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeJoinLatencyMillis(int nativeJoinLatencyMillis) {
            mNativeJoinLatencyMillis = nativeJoinLatencyMillis;
            return this;
        }

        /** Set the total number of joined documents in a page. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeNumJoinedResultsCurrentPage(
                int nativeNumJoinedResultsCurrentPage) {
            mNativeNumJoinedResultsCurrentPage = nativeNumJoinedResultsCurrentPage;
            return this;
        }

        /** Sets whether or not this is a join query */
        @CanIgnoreReturnValue
        public @NonNull Builder setJoinType(@JoinableValueType int joinType) {
            mJoinType = joinType;
            return this;
        }

        /** Sets search stats for parent. Only valid for first page. */
        @CanIgnoreReturnValue
        public @NonNull Builder setParentSearchStats(@Nullable SearchStats parentSearchStats) {
            mParentSearchStats = parentSearchStats;
            return this;
        }

        /** Sets search stats for child. Only valid for first page. */
        @CanIgnoreReturnValue
        public @NonNull Builder setChildSearchStats(@Nullable SearchStats childSearchStats) {
            mChildSearchStats = childSearchStats;
            return this;
        }

        /** Sets byte size of the lite index hit buffer. */
        @CanIgnoreReturnValue
        public @NonNull Builder setLiteIndexHitBufferByteSize(long liteIndexHitBufferByteSize) {
            mLiteIndexHitBufferByteSize = liteIndexHitBufferByteSize;
            return this;
        }

        /** Sets byte size of the unsorted tail of the lite index hit buffer. */
        @CanIgnoreReturnValue
        public @NonNull Builder setLiteIndexHitBufferUnsortedByteSize(
                long liteIndexHitBufferUnsortedByteSize) {
            mLiteIndexHitBufferUnsortedByteSize = liteIndexHitBufferUnsortedByteSize;
            return this;
        }

        /** Sets the type of the input page token. */
        @CanIgnoreReturnValue
        public @NonNull Builder setPageTokenType(@PageTokenType int pageTokenType) {
            mPageTokenType = pageTokenType;
            return this;
        }

        /**
         * Sets the Number of result states being force-evicted from ResultStateManager due to
         * budget limit. This doesn't include expired or invalidated states.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNumResultStatsEvicted(int numResultStatesEvicted) {
            mNumResultStatesEvicted = numResultStatesEvicted;
            return this;
        }

        /**
         * Constructs a new {@link QueryStats} from the contents of this
         * {@link QueryStats.Builder}.
         */
        @Override
        public @NonNull QueryStats build() {
            if (mDatabase == null) {
                Preconditions.checkState(mVisibilityScope != QueryStats.VISIBILITY_SCOPE_LOCAL,
                        "database can not be null if visibilityScope is local.");
            }

            return new QueryStats(/* builder= */ this);
        }
    }
}
