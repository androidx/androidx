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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * SearchResults are a returned object from a query API. It contains multiple pages of
 * {@link Result}.
 *
 * <p>Each {@link Result} contains a document and may contain other fields like snippets based on
 * request.
 *
 * <p>Should close this object after finish fetching results.
 *
 * <p>This class is not thread safe.
 */
public final class SearchResults implements Closeable {
    private final AppSearchBackend.BackendSearchResults mBackendSearchResults;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    SearchResults(AppSearchBackend.BackendSearchResults backendSearchResults) {
        mBackendSearchResults = backendSearchResults;
    }

    /**
     * Gets a whole page of {@link Result}s.
     *
     * <p>Re-call this method to get next page of {@link Result}s, until it returns an empty
     * list.
     *
     * <p>The page size is set by {@link SearchSpec.Builder#setNumPerPage}.
     *
     * @return The pending result of performing this operation.
     */
    @NonNull
    public ListenableFuture<AppSearchResult<List<Result>>> getNextPage() {
        return mBackendSearchResults.getNextPage();
    }

    @Override
    public void close() {
        mBackendSearchResults.close();
    }

    /**
     * This class represents the result obtained from the query. It will contain the document which
     * which matched the specified query string and specifications.
     */
    public static final class Result {
        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final String DOCUMENT_FIELD = "document";

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final String MATCHES_FIELD = "matches";

        @NonNull
        private final Bundle mBundle;

        @NonNull
        private final Bundle mDocumentBundle;

        @Nullable
        private GenericDocument mDocument;

        @Nullable
        private final List<Bundle> mMatchBundles;

        /**
         * Contains a list of Snippets that matched the request. Only populated when requested in
         * both {@link SearchSpec.Builder#setSnippetCount(int)}
         * and {@link SearchSpec.Builder#setSnippetCountPerProperty(int)}.
         *
         * @see #getMatches()
         */
        @Nullable
        private List<MatchInfo> mMatches;

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Result(@NonNull Bundle bundle) {
            mBundle = Preconditions.checkNotNull(bundle);
            mDocumentBundle = Preconditions.checkNotNull(bundle.getBundle(DOCUMENT_FIELD));
            mMatchBundles = bundle.getParcelableArrayList(MATCHES_FIELD);
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public Bundle getBundle() {
            return mBundle;
        }

        /**
         * Contains the matching {@link GenericDocument}.
         * @return Document object which matched the query.
         */
        @NonNull
        public GenericDocument getDocument() {
            if (mDocument == null) {
                mDocument = new GenericDocument(mDocumentBundle);
            }
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
            if (mMatchBundles != null && mMatches == null) {
                mMatches = new ArrayList<>(mMatchBundles.size());
                for (int i = 0; i < mMatchBundles.size(); i++) {
                    MatchInfo matchInfo = new MatchInfo(getDocument(), mMatchBundles.get(i));
                    mMatches.add(matchInfo);
                }
            }
            return mMatches;
        }
    }
}
