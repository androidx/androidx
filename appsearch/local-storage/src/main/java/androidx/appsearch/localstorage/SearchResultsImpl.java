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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutorService;

class SearchResultsImpl implements SearchResults {
    private final AppSearchImpl mAppSearchImpl;

    private final ExecutorService mExecutorService;

    @Nullable
    private final String mDatabaseName;

    private final String mQueryExpression;

    private final SearchSpec mSearchSpec;

    private long mNextPageToken;

    private boolean mIsFirstLoad = true;

    SearchResultsImpl(
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull ExecutorService executorService,
            @Nullable String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mExecutorService = Preconditions.checkNotNull(executorService);
        mDatabaseName = databaseName;
        mQueryExpression = Preconditions.checkNotNull(queryExpression);
        mSearchSpec = Preconditions.checkNotNull(searchSpec);
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchResult<List<SearchResult>>> getNextPage() {
        return FutureUtil.execute(mExecutorService, () -> {
            try {
                SearchResultPage searchResultPage;
                if (mIsFirstLoad) {
                    mIsFirstLoad = false;
                    if (mDatabaseName == null) {
                        // Global query, there's no one database to check.
                        searchResultPage = mAppSearchImpl.globalQuery(
                                mQueryExpression, mSearchSpec);
                    } else {
                        // Normal local query, pass in specified database.
                        searchResultPage = mAppSearchImpl.query(
                                mDatabaseName, mQueryExpression, mSearchSpec);
                    }
                } else {
                    searchResultPage = mAppSearchImpl.getNextPage(mNextPageToken);
                }
                mNextPageToken = searchResultPage.getNextPageToken();
                return AppSearchResult.newSuccessfulResult(
                        searchResultPage.getResults());
            } catch (Throwable t) {
                return AppSearchResult.throwableToFailedResult(t);
            }
        });
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void close() {
        // No future is needed here since the method is void.
        FutureUtil.execute(mExecutorService, () -> {
            mAppSearchImpl.invalidateNextPageToken(mNextPageToken);
            return null;
        });
    }
}
