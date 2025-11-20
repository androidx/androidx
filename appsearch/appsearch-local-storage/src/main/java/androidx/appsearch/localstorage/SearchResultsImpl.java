/*
 * Copyright 2020 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.localstorage;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.localstorage.stats.QueryStats;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

class SearchResultsImpl implements SearchResults {
    private final AppSearchImpl mAppSearchImpl;

    private final Executor mExecutor;

    /* The package name of the current app which is using the local backend. */
    private final String mPackageName;

    /** A CallerAccess object describing local-only access of the current app. */
    private final CallerAccess mSelfCallerAccess;

    /* The database name to search over. If null, this will search over all database names. */
    private final @Nullable String mDatabaseName;

    private final String mQueryExpression;

    private final SearchSpec mSearchSpec;

    private long mNextPageToken;

    private boolean mIsFirstLoad = true;

    private boolean mIsClosed = false;

    private final @Nullable AppSearchLogger mLogger;

    // Visibility Scope(local vs global) for 1st query, so it can be used for the visibility
    // scope for getNextPage().
    @QueryStats.VisibilityScope
    private int mVisibilityScope = QueryStats.VISIBILITY_SCOPE_UNKNOWN;

    SearchResultsImpl(
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull Executor executor,
            @NonNull String packageName,
            @Nullable String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @Nullable AppSearchLogger logger) {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mExecutor = Preconditions.checkNotNull(executor);
        mPackageName = Preconditions.checkNotNull(packageName);
        mSelfCallerAccess = new CallerAccess(/*callingPackageName=*/mPackageName);
        mDatabaseName = databaseName;
        mQueryExpression = Preconditions.checkNotNull(queryExpression);
        mSearchSpec = Preconditions.checkNotNull(searchSpec);
        mLogger = logger;
    }

    @Override
    public @NonNull ListenableFuture<List<SearchResult>> getNextPageAsync() {
        Preconditions.checkState(!mIsClosed, "SearchResults has already been closed");
        return FutureUtil.execute(mExecutor, () -> {
            SearchResultPage searchResultPage;
            if (mIsFirstLoad) {
                mIsFirstLoad = false;
                if (mDatabaseName == null) {
                    mVisibilityScope = QueryStats.VISIBILITY_SCOPE_GLOBAL;
                    // Global queries aren't restricted to a single database
                    searchResultPage = mAppSearchImpl.globalQuery(
                            mQueryExpression, mSearchSpec, mSelfCallerAccess, mLogger,
                            /*callStatsBuilder=*/null);
                } else {
                    mVisibilityScope = QueryStats.VISIBILITY_SCOPE_LOCAL;
                    // Normal local query, pass in specified database.
                    searchResultPage = mAppSearchImpl.query(
                            mPackageName, mDatabaseName, mQueryExpression, mSearchSpec, mLogger,
                            /*callStatsBuilder=*/null);
                }
            } else {
                if (mNextPageToken == SearchResultPage.EMPTY_PAGE_TOKEN) {
                    // Return an empty list directly if the next page token is empty token. This
                    // will save a binder call.
                    return Collections.emptyList();
                }

                QueryStats.Builder sStatsBuilder = null;
                if (mLogger != null) {
                    sStatsBuilder =
                            new QueryStats.Builder(mVisibilityScope, mPackageName);
                    if (mDatabaseName != null) {
                        sStatsBuilder.setDatabase(mDatabaseName);
                    }
                }
                searchResultPage = mAppSearchImpl.getNextPage(mPackageName, mNextPageToken,
                        sStatsBuilder,
                        /*callStatsBuilder=*/null);
                if (mLogger != null && sStatsBuilder != null) {
                    if (mSearchSpec.getJoinSpec() != null
                            && !mSearchSpec.getJoinSpec().getChildPropertyExpression().isEmpty()) {
                        sStatsBuilder.setJoinType(AppSearchSchema.StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID);
                    }
                    mLogger.logStats(sStatsBuilder.build());
                }
            }
            mNextPageToken = searchResultPage.getNextPageToken();
            return searchResultPage.getResults();
        });
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void close() {
        // Checking the future result is not needed here since this is a cleanup step which is not
        // critical to the correct functioning of the system; also, the return value is void.
        if (!mIsClosed) {
            if (mNextPageToken == SearchResultPage.EMPTY_PAGE_TOKEN) {
                // Save a binder call for invalidateNextPageToken if the next page token is empty
                // token.
                mIsClosed = true;
            } else {
                FutureUtil.execute(mExecutor, () -> {
                    mAppSearchImpl.invalidateNextPageToken(mPackageName, mNextPageToken,
                            /*callStatsBuilder=*/null);
                    mIsClosed = true;
                    return null;
                });
            }
        }
    }
}
