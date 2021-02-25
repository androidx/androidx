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

import java.io.Closeable;

/**
 * This class provides global access to the centralized AppSearch index maintained by the system.
 *
 * <p>Apps can retrieve indexed documents through the {@link #search} API.
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

    /** Closes the {@link GlobalSearchSession}. */
    @Override
    void close();
}
