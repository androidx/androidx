/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.playservicesstorage;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.playservicesstorage.converter.SearchResultToGmsConverter;
import androidx.appsearch.playservicesstorage.util.AppSearchTaskFutures;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * GooglePlayService's implementation of {@link SearchResults} which proxies to the
 * GooglePlayService's {@link com.google.android.gms.appsearch.SearchResults}.

 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SearchResultsImpl implements SearchResults {
    private final com.google.android.gms.appsearch.SearchResults mGmsResults;
    private final Executor mExecutor;

    SearchResultsImpl(
            @NonNull com.google.android.gms.appsearch.SearchResults gmsResults,
            @NonNull Executor executor) {
        mGmsResults = Preconditions.checkNotNull(gmsResults);
        mExecutor = Preconditions.checkNotNull(executor);
    }

    @Override
    @NonNull
    public ListenableFuture<List<SearchResult>> getNextPageAsync() {
        return AppSearchTaskFutures.toListenableFuture(
                mGmsResults.getNextPage(),
                SearchResultToGmsConverter::toJetpackSearchResultList,
                mExecutor
        );
    }

    @Override
    public void close() {
        mGmsResults.close();
    }
}
