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
package androidx.appsearch.app;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.Set;

/**
 * Provides a connection to a single AppSearch database.
 *
 * <p>An {@link AppSearchSession} instance provides access to database operations such as setting
 * a schema, adding documents, and searching.
 *
 * <p>Instances of this interface are usually obtained from a storage implementation, e.g.
 * {@code LocalStorage.createSearchSessionAsync()} or
 * {@code PlatformStorage.createSearchSessionAsync()}.
 *
 * <p>All implementations of this interface must be thread safe.
 *
 * @see GlobalSearchSession
 */
public interface AppSearchSession extends Closeable {

    /**
     * Sets the schema that represents the organizational structure of data within the AppSearch
     * database.
     *
     * <p>Upon creating an {@link AppSearchSession}, {@link #setSchemaAsync} should be called. If
     * the schema needs to be updated, or it has not been previously set, then the provided schema
     * will be saved and persisted to disk. Otherwise, {@link #setSchemaAsync} is handled
     * efficiently as a no-op call.
     *
     * @param  request the schema to set or update the AppSearch database to.
     * @return a {@link ListenableFuture} which resolves to a {@link SetSchemaResponse} object.
     */
    @NonNull
    ListenableFuture<SetSchemaResponse> setSchemaAsync(@NonNull SetSchemaRequest request);

    /**
     * @deprecated use {@link #setSchemaAsync}
     * @param  request the schema to set or update the AppSearch database to.
     * @return a {@link ListenableFuture} which resolves to a {@link SetSchemaResponse} object.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<SetSchemaResponse> setSchema(
            @NonNull SetSchemaRequest request) {
        return setSchemaAsync(request);
    }

    /**
     * Retrieves the schema most recently successfully provided to {@link #setSchemaAsync}.
     *
     * @return The pending {@link GetSchemaResponse} of performing this operation.
     */
    // This call hits disk; async API prevents us from treating these calls as properties.
    @SuppressLint("KotlinPropertyAccess")
    @NonNull
    ListenableFuture<GetSchemaResponse> getSchemaAsync();

    /**
     * @deprecated use {@link #getSchemaAsync}
     *
     * @return The pending {@link GetSchemaResponse} of performing this operation.
     */
    // This call hits disk; async API prevents us from treating these calls as properties.
    @SuppressLint("KotlinPropertyAccess")
    @NonNull
    @Deprecated
    default ListenableFuture<GetSchemaResponse> getSchema() {
        return getSchemaAsync();
    }

    /**
     * Retrieves the set of all namespaces in the current database with at least one document.
     *
     * @return The pending result of performing this operation. */
    @NonNull
    ListenableFuture<Set<String>> getNamespacesAsync();

    /**
     * @deprecated use {@link #getNamespacesAsync()}
     *
     * @return The pending result of performing this operation. */
    @NonNull
    @Deprecated
    default ListenableFuture<Set<String>> getNamespaces() {
        return getNamespacesAsync();
    }

    /**
     * Indexes documents into the {@link AppSearchSession} database.
     *
     * <p>Each {@link GenericDocument} object must have a {@code schemaType} field set to an
     * {@link AppSearchSchema} type that has been previously registered by calling the
     * {@link #setSchemaAsync} method.
     *
     * @param request containing documents to be indexed.
     * @return a {@link ListenableFuture} which resolves to an {@link AppSearchBatchResult}.
     * The keys of the returned {@link AppSearchBatchResult} are the IDs of the input documents.
     * The values are either {@code null} if the corresponding document was successfully indexed,
     * or a failed {@link AppSearchResult} otherwise.
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, Void>> putAsync(
            @NonNull PutDocumentsRequest request);

    /**
     * @deprecated use {@link #putAsync}
     *
     * @param request containing documents to be indexed.
     * @return a {@link ListenableFuture} which resolves to an {@link AppSearchBatchResult}.
     * The keys of the returned {@link AppSearchBatchResult} are the IDs of the input documents.
     * The values are either {@code null} if the corresponding document was successfully indexed,
     * or a failed {@link AppSearchResult} otherwise.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<AppSearchBatchResult<String, Void>> put(
            @NonNull PutDocumentsRequest request) {
        return putAsync(request);
    }

    /**
     * Gets {@link GenericDocument} objects by document IDs in a namespace from the
     * {@link AppSearchSession} database.
     *
     * @param request a request containing a namespace and IDs to get documents for.
     * @return A {@link ListenableFuture} which resolves to an {@link AppSearchBatchResult}.
     * The keys of the {@link AppSearchBatchResult} represent the input document IDs from the
     * {@link GetByDocumentIdRequest} object. The values are either the corresponding
     * {@link GenericDocument} object for the ID on success, or an {@link AppSearchResult}
     * object on failure. For example, if an ID is not found, the value for that ID will be set
     * to an {@link AppSearchResult} object with result code:
     * {@link AppSearchResult#RESULT_NOT_FOUND}.
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull GetByDocumentIdRequest request);

    /**
     * @deprecated use {@link #getByDocumentIdAsync}
     *
     * @param request a request containing a namespace and IDs to get documents for.
     * @return A {@link ListenableFuture} which resolves to an {@link AppSearchBatchResult}.
     * The keys of the {@link AppSearchBatchResult} represent the input document IDs from the
     * {@link GetByDocumentIdRequest} object. The values are either the corresponding
     * {@link GenericDocument} object for the ID on success, or an {@link AppSearchResult}
     * object on failure. For example, if an ID is not found, the value for that ID will be set
     * to an {@link AppSearchResult} object with result code:
     * {@link AppSearchResult#RESULT_NOT_FOUND}.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentId(
            @NonNull GetByDocumentIdRequest request) {
        return getByDocumentIdAsync(request);
    }

    /**
     * Retrieves documents from the open {@link AppSearchSession} that match a given query string
     * and type of search provided.
     *
     * <p>Query strings can be empty, contain one term with no operators, or contain multiple
     * terms and operators.
     *
     * <p>For query strings that are empty, all documents that match the {@link SearchSpec} will be
     * returned.
     *
     * <p>For query strings with a single term and no operators, documents that match the
     * provided query string and {@link SearchSpec} will be returned.
     *
     * <p>The following operators are supported:
     *
     * <ul>
     *     <li>AND (implicit)
     *     <p>AND is an operator that matches documents that contain <i>all</i>
     *     provided terms.
     *     <p><b>NOTE:</b> A space between terms is treated as an "AND" operator. Explicitly
     *     including "AND" in a query string will treat "AND" as a term, returning documents that
     *     also contain "AND".
     *     <p>Example: "apple AND banana" matches documents that contain the
     *     terms "apple", "and", "banana".
     *     <p>Example: "apple banana" matches documents that contain both "apple" and
     *     "banana".
     *     <p>Example: "apple banana cherry" matches documents that contain "apple", "banana", and
     *     "cherry".
     *
     *     <li>OR
     *     <p>OR is an operator that matches documents that contain <i>any</i> provided term.
     *     <p>Example: "apple OR banana" matches documents that contain either "apple" or "banana".
     *     <p>Example: "apple OR banana OR cherry" matches documents that contain any of
     *     "apple", "banana", or "cherry".
     *
     *     <li>Exclusion (-)
     *     <p>Exclusion (-) is an operator that matches documents that <i>do not</i> contain the
     *     provided term.
     *     <p>Example: "-apple" matches documents that do not contain "apple".
     *
     *     <li>Grouped Terms
     *     <p>For queries that require multiple operators and terms, terms can be grouped into
     *     subqueries. Subqueries are contained within an open "(" and close ")" parenthesis.
     *     <p>Example: "(donut OR bagel) (coffee OR tea)" matches documents that contain
     *     either "donut" or "bagel" and either "coffee" or "tea".
     *
     *     <li>Property Restricts
     *     <p>For queries that require a term to match a specific {@link AppSearchSchema}
     *     property of a document, a ":" must be included between the property name and the term.
     *     <p>Example: "subject:important" matches documents that contain the term "important" in
     *     the "subject" property.
     * </ul>
     *
     * <p>Additional search specifications, such as filtering by {@link AppSearchSchema} type or
     * adding projection, can be set by calling the corresponding {@link SearchSpec.Builder} setter.
     *
     * <p>This method is lightweight. The heavy work will be done in
     * {@link SearchResults#getNextPageAsync}.
     *
     * @param queryExpression query string to search.
     * @param searchSpec      spec for setting document filters, adding projection, setting term
     *                        match type, etc.
     * @return a {@link SearchResults} object for retrieved matched documents.
     */
    @NonNull
    SearchResults search(@NonNull String queryExpression, @NonNull SearchSpec searchSpec);

    /**
     * Reports usage of a particular document by namespace and ID.
     *
     * <p>A usage report represents an event in which a user interacted with or viewed a document.
     *
     * <p>For each call to {@link #reportUsageAsync}, AppSearch updates usage count and usage
     * recency * metrics for that particular document. These metrics are used for ordering
     * {@link #search} results by the {@link SearchSpec#RANKING_STRATEGY_USAGE_COUNT} and
     * {@link SearchSpec#RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP} ranking strategies.
     *
     * <p>Reporting usage of a document is optional.
     *
     * @param request The usage reporting request.
     * @return The pending result of performing this operation which resolves to {@code null} on
     *     success.
     */
    @NonNull
    ListenableFuture<Void> reportUsageAsync(@NonNull ReportUsageRequest request);

    /**
     * @deprecated use {@link #reportUsageAsync}
     *
     * @param request The usage reporting request.
     * @return The pending result of performing this operation which resolves to {@code null} on
     *     success.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<Void> reportUsage(@NonNull ReportUsageRequest request) {
        return reportUsageAsync(request);
    }

    /**
     * Removes {@link GenericDocument} objects by document IDs in a namespace from the
     * {@link AppSearchSession} database.
     *
     * <p>Removed documents will no longer be surfaced by {@link #search} or
     * {@link #getByDocumentIdAsync}
     * calls.
     *
     * <p>Once the database crosses the document count or byte usage threshold, removed documents
     * will be deleted from disk.
     *
     * @param request {@link RemoveByDocumentIdRequest} with IDs in a namespace to remove from the
     *                index.
     * @return a {@link ListenableFuture} which resolves to an {@link AppSearchBatchResult}.
     * The keys of the {@link AppSearchBatchResult} represent the input IDs from the
     * {@link RemoveByDocumentIdRequest} object. The values are either {@code null} on success,
     * or a failed {@link AppSearchResult} otherwise. IDs that are not found will return a failed
     * {@link AppSearchResult} with a result code of {@link AppSearchResult#RESULT_NOT_FOUND}.
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, Void>> removeAsync(
            @NonNull RemoveByDocumentIdRequest request);

    /**
     * @deprecated use {@link #removeAsync}
     *
     * @param request {@link RemoveByDocumentIdRequest} with IDs in a namespace to remove from the
     *                index.
     * @return a {@link ListenableFuture} which resolves to an {@link AppSearchBatchResult}.
     * The keys of the {@link AppSearchBatchResult} represent the input IDs from the
     * {@link RemoveByDocumentIdRequest} object. The values are either {@code null} on success,
     * or a failed {@link AppSearchResult} otherwise. IDs that are not found will return a failed
     * {@link AppSearchResult} with a result code of {@link AppSearchResult#RESULT_NOT_FOUND}.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<AppSearchBatchResult<String, Void>> remove(
            @NonNull RemoveByDocumentIdRequest request) {
        return removeAsync(request);
    }

    /**
     * Removes {@link GenericDocument}s from the index by Query. Documents will be removed if they
     * match the {@code queryExpression} in given namespaces and schemaTypes which is set via
     * {@link SearchSpec.Builder#addFilterNamespaces} and
     * {@link SearchSpec.Builder#addFilterSchemas}.
     *
     * <p> An empty {@code queryExpression} matches all documents.
     *
     * <p> An empty set of namespaces or schemaTypes matches all namespaces or schemaTypes in
     * the current database.
     *
     * @param queryExpression Query String to search.
     * @param searchSpec      Spec containing schemaTypes, namespaces and query expression
     *                        indicates how document will be removed. All specific about how to
     *                        scoring, ordering, snippeting and resulting will be ignored.
     * @return The pending result of performing this operation.
     */
    @NonNull
    ListenableFuture<Void> removeAsync(@NonNull String queryExpression,
            @NonNull SearchSpec searchSpec);

    /**
     * @deprecated use {@link #removeAsync}
     *
     * @param queryExpression Query String to search.
     * @param searchSpec      Spec containing schemaTypes, namespaces and query expression
     *                        indicates how document will be removed. All specific about how to
     *                        scoring, ordering, snippeting and resulting will be ignored.
     * @return The pending result of performing this operation.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<Void> remove(@NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        return removeAsync(queryExpression, searchSpec);
    }

    /**
     * Gets the storage info for this {@link AppSearchSession} database.
     *
     * <p>This may take time proportional to the number of documents and may be inefficient to
     * call repeatedly.
     *
     * @return a {@link ListenableFuture} which resolves to a {@link StorageInfo} object.
     */
    @NonNull
    ListenableFuture<StorageInfo> getStorageInfoAsync();

    /**
     * @deprecated use {@link #getStorageInfoAsync()}
     *
     * @return a {@link ListenableFuture} which resolves to a {@link StorageInfo} object.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<StorageInfo> getStorageInfo() {
        return getStorageInfoAsync();
    }

    /**
     * Flush all schema and document updates, additions, and deletes to disk if possible.
     *
     * <p>The request is not guaranteed to be handled and may be ignored by some implementations of
     * AppSearchSession.
     *
     * @return The pending result of performing this operation.
     * {@link androidx.appsearch.exceptions.AppSearchException} with
     * {@link AppSearchResult#RESULT_INTERNAL_ERROR} will be set to the future if we hit error when
     * save to disk.
     */
    @NonNull
    ListenableFuture<Void> requestFlushAsync();

    /**
     * @deprecated use {@link #requestFlushAsync()}
     *
     * @return The pending result of performing this operation.
     * {@link androidx.appsearch.exceptions.AppSearchException} with
     * {@link AppSearchResult#RESULT_INTERNAL_ERROR} will be set to the future if we hit error when
     * save to disk.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<Void> requestFlush() {
        return requestFlushAsync();
    }

    /**
     * Returns the {@link Features} to check for the availability of certain features
     * for this session.
     */
    @NonNull
    Features getFeatures();

    /**
     * Closes the {@link AppSearchSession} to persist all schema and document updates, additions,
     * and deletes to disk.
     */
    @Override
    void close();
}
