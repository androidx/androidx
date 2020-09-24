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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.List;

/**
 * Abstracts a storage system where {@link androidx.appsearch.annotation.AppSearchDocument}s can be
 * placed and queried.
 *
 * All implementations of this interface must be thread safe.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppSearchBackend {
    /**
     * Sets the schema being used by documents provided to the {@link #putDocuments} method.
     *
     * @see AppSearchManager#setSchema
     */
    @NonNull
    ListenableFuture<AppSearchResult<Void>> setSchema(
            @NonNull String databaseName, @NonNull SetSchemaRequest request);

    /**
     * Indexes documents into AppSearch.
     *
     * @see AppSearchManager#putDocuments
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, Void>> putDocuments(
            @NonNull String databaseName, @NonNull PutDocumentsRequest request);

    /**
     * Retrieves {@link GenericDocument}s by URI.
     *
     * @see AppSearchManager#getByUri
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByUri(
            @NonNull String databaseName, @NonNull GetByUriRequest request);

    /**
     * Searches {@link GenericDocument}s based on a given query string.
     * <p> This method is lightweight. The heavy work will be done in
     * {@link AppSearchBackend.BackendSearchResults#getNextPage}.
     * @see AppSearchManager#query
     */
    @NonNull
    AppSearchBackend.BackendSearchResults query(
            @NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec);

    /**
     * Removes {@link GenericDocument}s from the index by URI.
     *
     * @see AppSearchManager#removeByUri
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, Void>> removeByUri(
            @NonNull String databaseName, @NonNull RemoveByUriRequest request);

    /**
     * Removes {@link GenericDocument}s from the index by query.
     *
     * @see AppSearchManager#removeByQuery
     */
    @NonNull
    ListenableFuture<AppSearchResult<Void>> removeByQuery(
            @NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec);

    /** Clears all {@link GenericDocument}s, {@link AppSearchSchema}s and all other information
     * owned by this app. */
    @VisibleForTesting
    @NonNull
    ListenableFuture<AppSearchResult<Void>> resetAllDatabases();

    /**
     * SearchResults are a returned object from a query API.
     *
     * <p>Each {@link SearchResults.Result} contains a document and may contain other fields like
     * snippets based on request.
     *
     * <p>Should close this object after finish fetching results.
     *
     * <p>This class is not thread safe.
     */
    interface BackendSearchResults extends Closeable {
        /**
         * Gets a whole page of {@link SearchResults.Result}s.
         *
         * <p>Re-call this method to get next page of {@link SearchResults.Result}, until it returns
         * an empty list.
         *
         * <p>The page size is set by {@link SearchSpec.Builder#setNumPerPage}.
         *
         * @return The pending result of performing this operation.
         */
        @NonNull
        ListenableFuture<AppSearchResult<List<SearchResults.Result>>> getNextPage();

        @Override
        void close();
    }
}
