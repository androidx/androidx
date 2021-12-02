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

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;

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
     * {@link SearchResults#getNextPage}.
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
     * <p>See {@link AppSearchSession#reportUsage} for a general description of document usage, as
     * well as an API that can be used by the app itself.
     *
     * <p>Usage reported via this method is accounted separately from usage reported via
     * {@link AppSearchSession#reportUsage} and may be accessed using the constants
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
    ListenableFuture<Void> reportSystemUsage(@NonNull ReportSystemUsageRequest request);

    /**
     * Returns the {@link Capabilities} to check for the availability of certain features
     * for this session.
     */
    @NonNull Capabilities getCapabilities();

    /** Closes the {@link GlobalSearchSession}. */
    @Override
    void close();
}
