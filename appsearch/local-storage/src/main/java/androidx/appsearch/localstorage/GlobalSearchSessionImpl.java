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
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.core.util.Preconditions;

import java.util.concurrent.ExecutorService;

/**
 * An implementation of {@link AppSearchSession} which stores data locally
 * in the app's storage space using a bundled version of the search native library.
 *
 * <p>Queries are executed multi-threaded, but a single thread is used for mutate requests (put,
 * delete, etc..).
 */
class GlobalSearchSessionImpl implements GlobalSearchSession {
    private final AppSearchImpl mAppSearchImpl;
    private final ExecutorService mExecutorService;

    GlobalSearchSessionImpl(
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull ExecutorService executorService) {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mExecutorService = Preconditions.checkNotNull(executorService);
    }

    @NonNull
    @Override
    public SearchResults query(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        return new SearchResultsImpl(
                mAppSearchImpl,
                mExecutorService,
                /*databaseName=*/ null,
                queryExpression,
                searchSpec);
    }
}
