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

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

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
@WorkerThread
public interface AppSearchBackend {
    /** Returns {@code true} if this backend has been successfully initialized. */
    @AnyThread
    boolean isInitialized();

    /**
     * Initializes this backend, or returns a successful {@link AppSearchResult} if it has already
     * been initialized.
     */
    @NonNull
    AppSearchResult<Void> initialize();

    /**
     * Sets the schema being used by documents provided to the {@link #putDocuments} method.
     *
     * @see AppSearchManager#setSchema
     */
    @NonNull
    AppSearchResult<Void> setSchema(
            @NonNull String databaseName, @NonNull AppSearchManager.SetSchemaRequest request);

    /**
     * Indexes documents into AppSearch.
     *
     * @see AppSearchManager#putDocuments
     */
    @NonNull
    AppSearchBatchResult<String, Void> putDocuments(
            @NonNull String databaseName, @NonNull AppSearchManager.PutDocumentsRequest request);

    /**
     * Retrieves {@link GenericDocument}s by URI.
     *
     * @see AppSearchManager#getDocuments
     */
    @NonNull
    AppSearchBatchResult<String, GenericDocument> getDocuments(
            @NonNull String databaseName, @NonNull AppSearchManager.GetDocumentsRequest request);

    /**
     * Searches a document based on a given query string.
     * <p> This method is lightweight. The heavy work will be done in
     * {@link BackendSearchResults#getNextPage()}.
     * @see AppSearchManager#query
     */
    @NonNull
    BackendSearchResults query(
            @NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec);

    /**
     * Removes {@link GenericDocument}s from the index by URI.
     *
     * @see AppSearchManager#removeDocuments
     */
    @NonNull
    AppSearchBatchResult<String, Void> removeDocuments(
            @NonNull String databaseName, @NonNull AppSearchManager.RemoveDocumentsRequest request);

    /**
     * Removes {@link GenericDocument}s from the index by schema type.
     *
     * @see AppSearchManager#removeByType
     */
    @NonNull
    AppSearchBatchResult<String, Void> removeByType(
            @NonNull String databaseName, @NonNull List<String> schemaTypes);

    /**
     * Removes {@link GenericDocument}s from the index by namespace.
     *
     * @see AppSearchManager#removeByNamespace
     */
    @NonNull
    AppSearchBatchResult<String, Void> removeByNamespace(
            @NonNull String databaseName, @NonNull List<String> namespaces);

    /**
     * Removes all documents owned by this database.
     *
     * @see AppSearchManager#removeAll
     */
    @NonNull
    AppSearchResult<Void> removeAll(@NonNull String databaseName);

    /** Clears all documents, schemas and all other information owned by this app. */
    @VisibleForTesting
    @NonNull
    AppSearchResult<Void> resetAllDatabases();

    /**
     * Abstracts a returned search results object, where the pagination of the results can be
     * implemented.
     */
    @WorkerThread
    interface BackendSearchResults extends Closeable {

        /**
         * Fetches the next page of results of a previously executed query. Results can be empty if
         * next-page token is invalid or all pages have been returned.
         */
        @NonNull
        AppSearchResult<List<SearchResults.Result>> getNextPage();
    }
}
