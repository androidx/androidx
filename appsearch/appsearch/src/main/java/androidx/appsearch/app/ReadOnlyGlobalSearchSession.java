/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.observer.DocumentChangeResponse;
import androidx.appsearch.observer.ObserverCallback;
import androidx.appsearch.observer.ObserverSpec;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * Provides a connection to query over all AppSearch databases that the calling application has been
 * granted access to.
 *
 * <p>This interface is strictly for read-only operations, such as searching for documents
 * ({@link #search}), retrieving documents by ID ({@link #getByDocumentIdAsync}), and fetching
 * schemas ({@link #getSchemaAsync}). It does not include methods for writing data.
 *
 * <p>All implementations of this interface must be thread safe.
 *
 * @see AppSearchSession
 */
// TODO: b/454623046 - add remaining read apis from GlobalSearchSession when those apis are also
//  supported by EnterpriseGlobalSearchSession
public interface ReadOnlyGlobalSearchSession {
    /**
     * Retrieves {@link GenericDocument} documents, belonging to the specified package name and
     * database name and identified by the namespace and ids in the request, from the
     * {@link GlobalSearchSession} database. When a call is successful, the result will be
     * returned in the successes section of the {@link AppSearchBatchResult} object in the callback.
     * If the package doesn't exist, database doesn't exist, or if the calling package doesn't have
     * access, these failures will be reflected as {@link AppSearchResult} objects with a
     * RESULT_NOT_FOUND status code in the failures section of the {@link AppSearchBatchResult}
     * object.
     *
     * @param packageName  the name of the package to get from
     * @param databaseName the name of the database to get from
     * @param request      a request containing a namespace and IDs of the documents to retrieve.
     */
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_GET_BY_ID)
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull GetByDocumentIdRequest request);

    /**
     * Retrieves documents from all AppSearch databases that the querying application has access to.
     *
     * <p>Applications can be granted access to documents by specifying
     * {@link SetSchemaRequest.Builder#setSchemaTypeVisibilityForPackage}, or
     * {@link SetSchemaRequest.Builder#setDocumentClassVisibilityForPackage} when building a schema.
     *
     * <p>Document access can also be granted to system UIs by specifying
     * {@link SetSchemaRequest.Builder#setSchemaTypeDisplayedBySystem}, or
     * {@link SetSchemaRequest.Builder#setDocumentClassDisplayedBySystem}
     * when building a schema.
     *
     * <p>See {@link AppSearchSession#search} for a detailed explanation on
     * forming a query string.
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
     * Retrieves the collection of schemas most recently successfully provided to
     * {@link AppSearchSession#setSchemaAsync} for any types belonging to the requested package and
     * database that the caller has been granted access to.
     *
     * <p> If the requested package/database combination does not exist or the caller has not been
     * granted access to it, then an empty GetSchemaResponse will be returned.
     *
     * @param packageName  the package that owns the requested {@link AppSearchSchema} instances.
     * @param databaseName the database that owns the requested {@link AppSearchSchema} instances.
     * @return The pending {@link GetSchemaResponse} containing the schemas that the caller has
     * access to or an empty GetSchemaResponse if the request package and database does not
     * exist, has not set a schema or contains no schemas that are accessible to the caller.
     */
    // This call hits disk; async API prevents us from treating these calls as properties.
    @SuppressLint("KotlinPropertyAccess")
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA)
    @NonNull
    ListenableFuture<GetSchemaResponse> getSchemaAsync(@NonNull String packageName,
            @NonNull String databaseName);

    /**
     * Returns the {@link Features} to check for the availability of certain features
     * for this session.
     */
    @NonNull
    Features getFeatures();

    /**
     * Adds an {@link ObserverCallback} to monitor changes within the databases owned by
     * {@code targetPackageName} if they match the given
     * {@link androidx.appsearch.observer.ObserverSpec}.
     *
     * <p>The observer callback is only triggered for data that changes after it is registered. No
     * notification about existing data is sent as a result of registering an observer. To find out
     * about existing data, you must use the {@link GlobalSearchSession#search} API.
     *
     * <p>If the data owned by {@code targetPackageName} is not visible to you, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} changes its schema visibility settings.
     *
     * <p>If no package matching {@code targetPackageName} exists on the system, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} is installed and starts indexing data.
     *
     * <p>This feature may not be available in all implementations. Check
     * {@link Features#GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK} before calling this method.
     *
     * @param targetPackageName Package whose changes to monitor
     * @param spec            Specification of what types of changes to listen for
     * @param executor        Executor on which to call the {@code observer} callback methods.
     * @param observer        Callback to trigger when a schema or document changes
     * @throws AppSearchException            if an error occurs trying to register the observer
     * @throws UnsupportedOperationException if this feature is not available on this
     *                                       AppSearch implementation.
     */
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK)
    void registerObserverCallback(
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull ObserverCallback observer) throws AppSearchException;

    /**
     * Removes previously registered {@link ObserverCallback} instances from the system.
     *
     * <p>All instances of {@link ObserverCallback} which are registered to observe
     * {@code targetPackageName} and compare equal to the provided callback using the provided
     * argument's {@link ObserverCallback#equals} will be removed.
     *
     * <p>If no matching observers have been registered, this method has no effect. If multiple
     * matching observers have been registered, all will be removed.
     *
     * <p>This feature may not be available in all implementations. Check
     * {@link Features#GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK} before calling this method.
     *
     * @param targetPackageName Package which the observers to be removed are listening to.
     * @param observer          Callback to unregister.
     * @throws AppSearchException            if an error occurs trying to remove the observer, such
     *                                       as a failure to communicate with the system service
     *                                       in the platform backend. Note that no
     *                                       error will be thrown if the provided observer
     *                                       doesn't match any registered observer.
     * @throws UnsupportedOperationException if this feature is not available on this
     *                                       AppSearch implementation.
     */
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK)
    void unregisterObserverCallback(
            @NonNull String targetPackageName, @NonNull ObserverCallback observer)
            throws AppSearchException;

    /**
     * Retrieves the documents owned by {@code targetPackageName} that were added, updated or
     * removed after the point in time described by {@code paginationToken}, and that match the
     * given {@link ObserverSpec}.
     *
     * <p>Only changes to documents whose schema types are visible to the caller are returned. If
     * the data owned by {@code targetPackageName} is not visible to the caller, or no package
     * matching {@code targetPackageName} exists, an empty {@link DocumentChangeResponse} is
     * returned.
     *
     * <p>If {@link ObserverSpec#getFilterSchemas} is empty, changes to documents of all schema
     * types visible to the caller are returned. Otherwise, only changes to documents whose schema
     * type is in {@link ObserverSpec#getFilterSchemas} are returned.
     *
     * <p>A {@link androidx.appsearch.observer.DocumentChangeSet} does not convey what kind of change
     * occurred, nor the old or new values of the document. It is up to the caller to use the
     * returned document IDs to look up what has changed.
     *
     * <p>Results are paginated. To start retrieving changes, pass a
     * {@link DocumentChangeResponse.PaginationToken} constructed with the time from which changes
     * are wanted. If {@link DocumentChangeResponse#hasMoreChanges} is {@code true}, call this
     * method again with {@link DocumentChangeResponse#getNextPaginationToken} to retrieve the next
     * page of changes. Retrieving changes is idempotent, so callers should be prepared to process
     * the same change more than once.
     *
     * <p>Removed documents are only retained for a limited time, so changes for documents that
     * were removed a long time ago may be missing.
     *
     * <p>Changes to documents whose schema type no longer exists are not returned, because the
     * caller's visibility to that schema type can no longer be checked. For example, if a schema
     * type is removed, or {@code targetPackageName} is uninstalled, its documents are deleted but
     * no removals are reported for them. Callers that keep a copy of this data won't be told
     * that those documents are gone.
     *
     * <p>This feature may not be available in all implementations. Check
     * {@link Features#GLOBAL_SEARCH_SESSION_GET_DOCUMENT_CHANGES} before calling this method.
     *
     * @param targetPackageName the package whose document changes to retrieve.
     * @param spec              specification of what types of changes to retrieve.
     * @param paginationToken   the token describing from which point in time to retrieve changes.
     * @return a {@link ListenableFuture} which resolves to a {@link DocumentChangeResponse}.
     * @throws UnsupportedOperationException if this feature is not available on this
     *                                       AppSearch implementation.
     */
    // TODO(b/568469783): unhide the API once offline observers are ready.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_GET_DOCUMENT_CHANGES)
    @FlaggedApi(Flags.FLAG_ENABLE_OFFLINE_OBSERVERS)
    @ExperimentalAppSearchApi
    default @NonNull ListenableFuture<DocumentChangeResponse> getDocumentChangesAsync(
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            DocumentChangeResponse.@NonNull PaginationToken paginationToken) {
        throw new UnsupportedOperationException(
                Features.GLOBAL_SEARCH_SESSION_GET_DOCUMENT_CHANGES
                        + " is not available on this AppSearch implementation.");
    }
}
