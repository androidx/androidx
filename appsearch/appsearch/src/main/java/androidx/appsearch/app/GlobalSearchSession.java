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
import androidx.annotation.RequiresFeature;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.observer.AppSearchObserverCallback;
import androidx.appsearch.observer.ObserverSpec;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.concurrent.Executor;

/**
 * Provides a connection to all AppSearch databases the querying application has been
 * granted access to.
 *
 * <p>All implementations of this interface must be thread safe.
 *
 * @see AppSearchSession
 */
public interface GlobalSearchSession extends Closeable {
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
     * Reports that a particular document has been used from a system surface.
     *
     * <p>See {@link AppSearchSession#reportUsageAsync} for a general description of document usage,
     * as well as an API that can be used by the app itself.
     *
     * <p>Usage reported via this method is accounted separately from usage reported via
     * {@link AppSearchSession#reportUsageAsync} and may be accessed using the constants
     * {@link SearchSpec#RANKING_STRATEGY_SYSTEM_USAGE_COUNT} and
     * {@link SearchSpec#RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP}.
     *
     * @return The pending result of performing this operation which resolves to {@code null} on
     *     success. The pending result will be completed with an
     *     {@link androidx.appsearch.exceptions.AppSearchException} with a code of
     *     {@link AppSearchResult#RESULT_SECURITY_ERROR} if this API is invoked by an app which
     *     is not part of the system.
     */
    @NonNull
    ListenableFuture<Void> reportSystemUsageAsync(@NonNull ReportSystemUsageRequest request);

    /**
     * @deprecated use {@link #reportSystemUsageAsync}
     *
     * @return The pending result of performing this operation which resolves to {@code null} on
     *     success. The pending result will be completed with an
     *     {@link androidx.appsearch.exceptions.AppSearchException} with a code of
     *     {@link AppSearchResult#RESULT_SECURITY_ERROR} if this API is invoked by an app which
     *     is not part of the system.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<Void> reportSystemUsage(@NonNull ReportSystemUsageRequest request) {
        return reportSystemUsageAsync(request);
    }

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
    // @exportToFramework:startStrip()
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA)
    // @exportToFramework:endStrip()
    ListenableFuture<GetSchemaResponse> getSchemaAsync(@NonNull String packageName,
            @NonNull String databaseName);

    /**
     * @deprecated use {@link #getSchemaAsync}.
     *
     * @param packageName the package that owns the requested {@link AppSearchSchema} instances.
     * @param databaseName the database that owns the requested {@link AppSearchSchema} instances.
     * @return The pending {@link GetSchemaResponse} containing the schemas that the caller has
     * access to or an empty GetSchemaResponse if the request package and database does not
     * exist, has not set a schema or contains no schemas that are accessible to the caller.
     */
    @SuppressLint("KotlinPropertyAccess")
    @NonNull
    // @exportToFramework:startStrip()
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA)
    // @exportToFramework:endStrip()
    @Deprecated
    default ListenableFuture<GetSchemaResponse> getSchema(@NonNull String packageName,
            @NonNull String databaseName) {
        return getSchemaAsync(packageName, databaseName);
    }

    /**
     * Returns the {@link Features} to check for the availability of certain features
     * for this session.
     */
    @NonNull
    Features getFeatures();

    /**
     * Adds an {@link AppSearchObserverCallback} to monitor changes within the
     * databases owned by {@code targetPackageName} if they match the given
     * {@link androidx.appsearch.observer.ObserverSpec}.
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
     * {@link Features#GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER} before calling this method.
     *
     * @param targetPackageName Package whose changes to monitor
     * @param spec            Specification of what types of changes to listen for
     * @param executor        Executor on which to call the {@code observer} callback methods.
     * @param observer        Callback to trigger when a schema or document changes
     * @throws AppSearchException            if an error occurs trying to register the observer
     * @throws UnsupportedOperationException if this feature is not available on this
     *                                       AppSearch implementation.
     */
    // @exportToFramework:startStrip()
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER)
    // @exportToFramework:endStrip()
    void addObserver(
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull AppSearchObserverCallback observer) throws AppSearchException;

    /**
     * Removes previously registered {@link AppSearchObserverCallback} instances from the system.
     *
     * <p>All instances of {@link AppSearchObserverCallback} which are registered to observe
     * {@code targetPackageName} and compare equal to the provided callback using
     * {@link AppSearchObserverCallback#equals} will be removed.
     *
     * <p>If no matching observers have been registered, this method has no effect. If multiple
     * matching observers have been registered, all will be removed.
     *
     * <p>This feature may not be available in all implementations. Check
     * {@link Features#GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER} before calling this method.
     *
     * @param targetPackageName Package which the observers to be removed are listening to.
     * @param observer          Callback to unregister.
     * @throws UnsupportedOperationException if this feature is not available on this
     *                                       AppSearch implementation.
     */
    // @exportToFramework:startStrip()
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER)
    // @exportToFramework:endStrip()
    void removeObserver(
            @NonNull String targetPackageName, @NonNull AppSearchObserverCallback observer);

    /** Closes the {@link GlobalSearchSession}. */
    @Override
    void close();
}
