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
package androidx.appsearch.platformstorage;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.platformstorage.converter.SearchResultToPlatformConverter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Platform implementation of {@link SearchResults} which proxies to the platform's
 * {@link android.app.appsearch.SearchResults}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
class SearchResultsImpl implements SearchResults {
    private final android.app.appsearch.SearchResults mPlatformResults;
    private final Executor mExecutor;

    SearchResultsImpl(
            @NonNull android.app.appsearch.SearchResults platformResults,
            @NonNull Executor executor) {
        mPlatformResults = Preconditions.checkNotNull(platformResults);
        mExecutor = Preconditions.checkNotNull(executor);
    }

    @Override
    @NonNull
    public ListenableFuture<List<SearchResult>> getNextPage() {
        ResolvableFuture<List<SearchResult>> future = ResolvableFuture.create();
        mPlatformResults.getNextPage(mExecutor, result -> {
            if (result.isSuccess()) {
                List<android.app.appsearch.SearchResult> frameworkResults = result.getResultValue();
                List<SearchResult> jetpackResults = new ArrayList<>(frameworkResults.size());
                for (int i = 0; i < frameworkResults.size(); i++) {
                    SearchResult jetpackResult =
                            SearchResultToPlatformConverter.toJetpackSearchResult(
                                    frameworkResults.get(i));
                    jetpackResults.add(jetpackResult);
                }
                future.set(jetpackResults);
            } else {
                future.setException(
                        new AppSearchException(result.getResultCode(), result.getErrorMessage()));
            }
        });
        return future;
    }

    @Override
    public void close() {
        mPlatformResults.close();
    }
}
