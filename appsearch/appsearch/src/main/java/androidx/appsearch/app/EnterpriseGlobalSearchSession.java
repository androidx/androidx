/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.annotation.RequiresFeature;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Provides a connection to all enterprise (work profile) AppSearch databases the querying
 * application has been granted access to.
 *
 * <p>This session can be created from any user profile but will only properly return results when
 * created from the main profile. If the user is not the main profile or an associated work profile
 * does not exist, queries will still successfully complete but with empty results.
 *
 * <p>Schemas must be explicitly tagged enterprise and may require additional permissions to be
 * visible from an enterprise session. Retrieved documents may also have certain fields restricted
 * or modified unlike if they were retrieved directly from {@link GlobalSearchSession} on the work
 * profile.
 *
 * <p>All implementations of this interface must be thread safe.
 *
 * @see GlobalSearchSession
 */
@RequiresFeature(
        enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
        name = Features.ENTERPRISE_GLOBAL_SEARCH_SESSION)
public interface EnterpriseGlobalSearchSession {
    /**
     * Retrieves {@link GenericDocument} documents, belonging to the specified package name and
     * database name and identified by the namespace and ids in the request, from the
     * {@link EnterpriseGlobalSearchSession} database. When a call is successful, the result will be
     * returned in the successes section of the {@link AppSearchBatchResult} object in the callback.
     * If the package doesn't exist, database doesn't exist, or if the calling package doesn't have
     * access, these failures will be reflected as {@link AppSearchResult} objects with a
     * RESULT_NOT_FOUND status code in the failures section of the {@link AppSearchBatchResult}
     * object.
     *
     * @param packageName the name of the package to get from
     * @param databaseName the name of the database to get from
     * @param request a request containing a namespace and IDs of the documents to retrieve.
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull GetByDocumentIdRequest request);

    /**
     * Retrieves documents from all enterprise (work profile) AppSearch databases that the querying
     * application has access to.
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
     *
     * @param packageName the package that owns the requested {@link AppSearchSchema} instances.
     * @param databaseName the database that owns the requested {@link AppSearchSchema} instances.
     * @return The pending {@link GetSchemaResponse} containing the schemas that the caller has
     * access to or an empty GetSchemaResponse if the request package and database does not
     * exist, has not set a schema or contains no schemas that are accessible to the caller.
     */
    // This call hits disk; async API prevents us from treating these calls as properties.
    @SuppressLint("KotlinPropertyAccess")
    @NonNull
    ListenableFuture<GetSchemaResponse> getSchemaAsync(@NonNull String packageName,
            @NonNull String databaseName);

    /**
     * Returns the {@link Features} to check for the availability of certain features
     * for this session.
     */
    @NonNull
    Features getFeatures();
}
