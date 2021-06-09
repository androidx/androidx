/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.debugview;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.debugview.view.AppSearchDebugActivity;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.core.os.BuildCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Manages interactions with AppSearch.
 *
 * <p>Instances of {@link DebugAppSearchManager} are created by calling {@link #create}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DebugAppSearchManager implements Closeable {
    private static final int PAGE_SIZE = 100;

    private final Context mContext;
    private final ExecutorService mExecutor;
    private final SettableFuture<AppSearchSession> mAppSearchSessionFuture =
            SettableFuture.create();

    private DebugAppSearchManager(@NonNull Context context, @NonNull ExecutorService executor) {
        mContext = Preconditions.checkNotNull(context);
        mExecutor = Preconditions.checkNotNull(executor);
    }

    /**
     * Factory for creating a {@link DebugAppSearchManager} instance.
     *
     * <p>This factory creates an {@link AppSearchSession} instance with the provided
     * database name.
     *
     * @param context      application context.
     * @param executor     executor to run AppSearch operations on.
     * @param databaseName name of the database to open AppSearch debugging for.
     * @param storageType  constant of the storage type to start session for.
     * @throws AppSearchException if the storage type is invalid, or a R- device selects platform
     *                            storage as the storage type for debugging.
     */
    @NonNull
    public static ListenableFuture<DebugAppSearchManager> create(
            @NonNull Context context,
            @NonNull ExecutorService executor, @NonNull String databaseName,
            @AppSearchDebugActivity.StorageType int storageType) throws AppSearchException {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(databaseName);

        DebugAppSearchManager debugAppSearchManager =
                new DebugAppSearchManager(context, executor);

        ListenableFuture<DebugAppSearchManager> debugAppSearchManagerListenableFuture;

        switch (storageType) {
            case AppSearchDebugActivity.STORAGE_TYPE_LOCAL:
                debugAppSearchManagerListenableFuture =
                        Futures.transform(
                                debugAppSearchManager.initializeLocalStorage(databaseName),
                                unused -> debugAppSearchManager, executor);
                break;
            case AppSearchDebugActivity.STORAGE_TYPE_PLATFORM:
                if (BuildCompat.isAtLeastS()) {
                    debugAppSearchManagerListenableFuture =
                            Futures.transform(
                                    debugAppSearchManager.initializePlatformStorage(databaseName),
                                    unused -> debugAppSearchManager, executor);
                } else {
                    throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                            "Platform Storage debugging only valid for S+ devices.");
                }
                break;
            default:
                throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                        "Invalid storage type specified. Verify that the "
                                + "storage type that has been passed in the intent is valid.");
        }
        return debugAppSearchManagerListenableFuture;
    }

    /**
     * Searches for all documents in the AppSearch database.
     *
     * <p>Each {@link GenericDocument} object is truncated of its properties by adding
     * projection to the request.
     *
     * @return the {@link SearchResults} instance for exploring pages of results. Call
     * {@link #getNextPage} to retrieve the {@link GenericDocument} objects for each page.
     */
    @NonNull
    public ListenableFuture<SearchResults> getAllDocumentsSearchResults() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultCountPerPage(PAGE_SIZE)
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addProjection(SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD, Collections.emptyList())
                .build();
        String retrieveAllQueryString = "";

        return Futures.transform(mAppSearchSessionFuture,
                session -> session.search(retrieveAllQueryString, searchSpec), mExecutor);
    }


    /**
     * Converts the next page from the provided {@link SearchResults} instance to a list of
     * {@link GenericDocument} objects.
     *
     * @param results results to get next page for, and convert to a list of
     *                {@link GenericDocument} objects.
     */
    @NonNull
    public ListenableFuture<List<GenericDocument>> getNextPage(@NonNull SearchResults results) {
        Preconditions.checkNotNull(results);

        return Futures.transform(results.getNextPage(),
                DebugAppSearchManager::convertResultsToGenericDocuments, mExecutor);
    }

    /**
     * Gets a document from the AppSearch database by namespace and ID.
     */
    @NonNull
    public ListenableFuture<GenericDocument> getDocument(@NonNull String namespace,
            @NonNull String id) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(namespace);
        GetByDocumentIdRequest request =
                new GetByDocumentIdRequest.Builder(namespace).addIds(id).build();

        return Futures.transformAsync(mAppSearchSessionFuture,
                session -> Futures.transform(session.getByDocumentId(request),
                        response -> response.getSuccesses().get(id), mExecutor), mExecutor);
    }

    /**
     * Gets the schema of the AppSearch database.
     */
    @NonNull
    public ListenableFuture<GetSchemaResponse> getSchema() {
        return Futures.transformAsync(mAppSearchSessionFuture,
                session -> session.getSchema(), mExecutor);
    }

    /**
     * Closes the AppSearch session.
     */
    @Override
    public void close() {
        Futures.whenAllSucceed(mAppSearchSessionFuture).call(() -> {
            Futures.getDone(mAppSearchSessionFuture).close();
            return null;
        }, mExecutor);
    }

    @NonNull
    private ListenableFuture<AppSearchSession> initializeLocalStorage(
            @NonNull String databaseName) {
        mAppSearchSessionFuture.setFuture(LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(mContext, databaseName)
                        .build())
        );
        return mAppSearchSessionFuture;
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.S)
    private ListenableFuture<AppSearchSession> initializePlatformStorage(
            @NonNull String databaseName) {
        mAppSearchSessionFuture.setFuture(PlatformStorage.createSearchSession(
                new PlatformStorage.SearchContext.Builder(mContext, databaseName)
                        .build())
        );
        return mAppSearchSessionFuture;
    }

    private static List<GenericDocument> convertResultsToGenericDocuments(
            List<SearchResult> results) {
        List<GenericDocument> docs = new ArrayList<>(results.size());

        for (SearchResult result : results) {
            docs.add(result.getGenericDocument());
        }

        return docs;
    }
}
