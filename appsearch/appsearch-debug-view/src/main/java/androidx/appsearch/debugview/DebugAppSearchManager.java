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
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.debugview.view.AppSearchDebugActivity;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.appsearch.platformstorage.PlatformStorage;
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
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DebugAppSearchManager implements Closeable {
    private static final int PAGE_SIZE = 100;

    private final Context mContext;
    private final ExecutorService mExecutor;
    private final SettableFuture<AppSearchSession> mAppSearchSessionFuture =
            SettableFuture.create();
    private final SettableFuture<GlobalSearchSession> mGlobalSearchSessionFuture =
            SettableFuture.create();
    private final @AppSearchDebugActivity.SearchType int mSearchType;
    private final @Nullable String mTargetPackageName;
    private final String mTargetDatabase;

    private DebugAppSearchManager(@NonNull Context context, @NonNull ExecutorService executor,
                                  @AppSearchDebugActivity.SearchType int searchType,
                                  @Nullable String targetPackageName,
                                  @NonNull String targetDatabaseName) {
        mContext = Preconditions.checkNotNull(context);
        mExecutor = Preconditions.checkNotNull(executor);
        mSearchType = searchType;
        mTargetPackageName = targetPackageName;
        mTargetDatabase = Preconditions.checkNotNull(targetDatabaseName);
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
    public static ListenableFuture<DebugAppSearchManager> createAsync(
            @NonNull Context context,
            @NonNull ExecutorService executor, @NonNull String databaseName,
            @Nullable String targetPackageName,
            @AppSearchDebugActivity.StorageType int storageType,
            @AppSearchDebugActivity.SearchType int searchType) throws AppSearchException {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(databaseName);
        if (searchType == AppSearchDebugActivity.SEARCH_TYPE_GLOBAL) {
            Preconditions.checkArgument(
                    storageType == AppSearchDebugActivity.STORAGE_TYPE_PLATFORM);
            Preconditions.checkNotNull(targetPackageName);
        }

        DebugAppSearchManager debugAppSearchManager =
                new DebugAppSearchManager(context, executor, searchType,
                                          targetPackageName, databaseName);

        ListenableFuture<DebugAppSearchManager> debugAppSearchManagerListenableFuture;

        if (searchType == AppSearchDebugActivity.SEARCH_TYPE_GLOBAL) {
            if (Build.VERSION.SDK_INT < 31) {
                throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                        "Platform Storage debugging only valid for S+ devices.");
            }
            debugAppSearchManagerListenableFuture =
                    Futures.transform(
                            debugAppSearchManager.initializeGlobalSearchSessionAsync(),
                            unused -> debugAppSearchManager, executor);
        } else {
            switch (storageType) {
                case AppSearchDebugActivity.STORAGE_TYPE_LOCAL:
                    debugAppSearchManagerListenableFuture =
                            Futures.transform(
                                    debugAppSearchManager.initializeLocalStorageAsync(databaseName),
                                    unused -> debugAppSearchManager, executor);
                    break;
                case AppSearchDebugActivity.STORAGE_TYPE_PLATFORM:
                    if (Build.VERSION.SDK_INT < 31) {
                        throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                                "Platform Storage debugging only valid for S+ devices.");
                    }
                    debugAppSearchManagerListenableFuture = Futures.transform(
                            debugAppSearchManager.initializePlatformStorageAsync(databaseName),
                            unused -> debugAppSearchManager, executor);
                    break;
                default:
                    throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                            "Invalid storage type specified. Verify that the "
                                    + "storage type that has been passed in the intent is valid.");
            }
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
    public ListenableFuture<SearchResults> getAllDocumentsSearchResultsAsync() {
        SearchSpec.Builder searchSpecBuilder = new SearchSpec.Builder()
                .setResultCountPerPage(PAGE_SIZE)
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addProjection(SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD, Collections.emptyList());
        String retrieveAllQueryString = "";

        if (mSearchType == AppSearchDebugActivity.SEARCH_TYPE_GLOBAL) {
            searchSpecBuilder.addFilterPackageNames(mTargetPackageName);
            return Futures.transform(mGlobalSearchSessionFuture,
                    session -> session.search(retrieveAllQueryString, searchSpecBuilder.build()),
                    mExecutor);
        } else {
            return Futures.transform(mAppSearchSessionFuture,
                    session -> session.search(retrieveAllQueryString, searchSpecBuilder.build()),
                    mExecutor);
        }
    }


    /**
     * Converts the next page from the provided {@link SearchResults} instance to a list of
     * {@link GenericDocument} objects.
     *
     * @param results results to get next page for, and convert to a list of
     *                {@link GenericDocument} objects.
     */
    @NonNull
    public ListenableFuture<List<GenericDocument>> getNextPageAsync(
            @NonNull SearchResults results) {
        Preconditions.checkNotNull(results);

        return Futures.transform(results.getNextPageAsync(),
                DebugAppSearchManager::convertResultsToGenericDocuments, mExecutor);
    }

    /**
     * Gets a document from the AppSearch database by namespace and ID.
     */
    @NonNull
    public ListenableFuture<GenericDocument> getDocumentAsync(@NonNull String namespace,
            @NonNull String id) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(namespace);
        GetByDocumentIdRequest request =
                new GetByDocumentIdRequest.Builder(namespace).addIds(id).build();

        if (mSearchType == AppSearchDebugActivity.SEARCH_TYPE_GLOBAL) {
            return Futures.transformAsync(mGlobalSearchSessionFuture,
                    session -> {
                        if (!session.getFeatures().isFeatureSupported(
                                Features.GLOBAL_SEARCH_SESSION_GET_BY_ID)) {
                            return Futures.immediateFailedFuture(
                                    new UnsupportedOperationException());
                        }
                        return Futures.transform(
                                session.getByDocumentIdAsync(mTargetPackageName,
                                                             mTargetDatabase, request),
                                response -> response.getSuccesses().get(id), mExecutor);
                    }, mExecutor);
        } else {
            return Futures.transformAsync(mAppSearchSessionFuture,
                    session -> Futures.transform(session.getByDocumentIdAsync(request),
                            response -> response.getSuccesses().get(id), mExecutor), mExecutor);
        }
    }

    /**
     * Gets the schema of the AppSearch database.
     */
    @NonNull
    public ListenableFuture<GetSchemaResponse> getSchemaAsync() {
        if (mSearchType == AppSearchDebugActivity.SEARCH_TYPE_GLOBAL) {
            return Futures.transformAsync(mGlobalSearchSessionFuture,
                    session -> {
                        if (!session.getFeatures().isFeatureSupported(
                                Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA)) {
                            return Futures.immediateFailedFuture(
                                    new UnsupportedOperationException());
                        }
                        return session.getSchemaAsync(mTargetPackageName, mTargetDatabase);
                    }, mExecutor);
        } else {
            return Futures.transformAsync(mAppSearchSessionFuture,
                    session -> session.getSchemaAsync(), mExecutor);
        }
    }

    /**
     * Closes the AppSearch session.
     */
    @Override
    public void close() {
        if (mSearchType == AppSearchDebugActivity.SEARCH_TYPE_GLOBAL) {
            Futures.whenAllSucceed(mGlobalSearchSessionFuture).call(() -> {
                Futures.getDone(mGlobalSearchSessionFuture).close();
                return null;
            }, mExecutor);
        } else {
            Futures.whenAllSucceed(mAppSearchSessionFuture).call(() -> {
                Futures.getDone(mAppSearchSessionFuture).close();
                return null;
            }, mExecutor);
        }
    }

    @NonNull
    private ListenableFuture<AppSearchSession> initializeLocalStorageAsync(
            @NonNull String databaseName) {
        mAppSearchSessionFuture.setFuture(LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(mContext, databaseName)
                        .build())
        );
        return mAppSearchSessionFuture;
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.S)
    private ListenableFuture<AppSearchSession> initializePlatformStorageAsync(
            @NonNull String databaseName) {
        mAppSearchSessionFuture.setFuture(PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(mContext, databaseName)
                        .build())
        );
        return mAppSearchSessionFuture;
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.S)
    private ListenableFuture<GlobalSearchSession> initializeGlobalSearchSessionAsync() {
        mGlobalSearchSessionFuture.setFuture(PlatformStorage.createGlobalSearchSessionAsync(
                new PlatformStorage.GlobalSearchContext.Builder(mContext).build())
        );
        return mGlobalSearchSessionFuture;
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
