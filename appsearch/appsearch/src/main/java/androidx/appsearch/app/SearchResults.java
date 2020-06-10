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

package androidx.appsearch.app;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * SearchResults are a returned object from a query API. It contains multiple pages of
 * {@link Result}.
 * <p>Each {@link Result} contains a document and may contain other fields like snippets based on
 * request.
 * <p>Should close this object after finish fetching results.
 * <p>This class is not thread safe.
 */
public final class SearchResults implements Closeable {

    private static final String TAG = "AppSearch-SearchResults";
    private final ExecutorService mExecutorService;
    private final AppSearchBackend.BackendSearchResults mBackendSearchResults;

    /** @hide */
    public SearchResults(@NonNull ExecutorService executorService,
            @NonNull AppSearchBackend.BackendSearchResults backendSearchResults)  {
        mExecutorService = executorService;
        mBackendSearchResults = backendSearchResults;
    }

    /**
     * Gets a whole page of {@link Result}.
     * <p>Re-called this method to get next page of {@link Result}, until it return an empty list.
     * <p>The page size is set by {@link SearchSpec.Builder#setNumPerPage(int)}.
     * @return The pending result of performing this operation.
     */
    @NonNull
    public ListenableFuture<AppSearchResult<List<Result>>> getNextPage() {
        ResolvableFuture<AppSearchResult<List<Result>>> future = ResolvableFuture.create();
        mExecutorService.execute(() -> {
            if (!future.isCancelled()) {
                try {
                    future.set(mBackendSearchResults.getNextPage());
                } catch (Throwable t) {
                    future.setException(t);
                }
            }
        });
        return future;
    }

    @Override
    public void close() {
        // Close the SearchResult in the backend thread. No future is needed here since the
        // method is void.
        mExecutorService.execute(() -> {
            try {
                mBackendSearchResults.close();
            } catch (IOException e) {
                Log.w(TAG, "Fail to close the SearchResults.", e);
            }
        });
    }

    /**
     * This class represents the result obtained from the query. It will contain the document which
     * which matched the specified query string and specifications.
     */
    public static final class Result {

        @NonNull
        private GenericDocument mDocument;

        /**
         * Contains a list of Snippets that matched the request. Only populated when requested in
         * both {@link SearchSpec.Builder#setSnippetCount(int)}
         * and {@link SearchSpec.Builder#setSnippetCountPerProperty(int)}.
         *
         * @see #getMatches()
         */
        @Nullable
        private final List<MatchInfo> mMatches;

        Result(@NonNull GenericDocument document, @Nullable List<MatchInfo> matches) {
            mDocument = document;
            mMatches = matches;
        }

        /**
         * Contains the matching {@link GenericDocument}.
         * @return Document object which matched the query.
         */
        @NonNull
        public GenericDocument getDocument() {
            return mDocument;
        }

        /**
         * Contains a list of Snippets that matched the request. Only populated when requested in
         * both {@link SearchSpec.Builder#setSnippetCount(int)}
         * and {@link SearchSpec.Builder#setSnippetCountPerProperty(int)}.
         *
         * @return  List of matches based on {@link SearchSpec}, if snippeting is disabled and this
         * method is called it will return {@code null}. Users can also restrict snippet population
         * using {@link SearchSpec.Builder#setSnippetCount} and
         * {@link SearchSpec.Builder#setSnippetCountPerProperty(int)}, for all results after that
         * value this method will return {@code null}.
         */
        @Nullable
        public List<MatchInfo> getMatches() {
            return mMatches;
        }
    }
}
