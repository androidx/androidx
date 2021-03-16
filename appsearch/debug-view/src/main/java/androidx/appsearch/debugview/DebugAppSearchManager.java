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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.localstorage.LocalStorage;
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
     */
    @NonNull
    public static ListenableFuture<DebugAppSearchManager> create(
            @NonNull Context context,
            @NonNull ExecutorService executor, @NonNull String databaseName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(databaseName);

        DebugAppSearchManager debugAppSearchManager =
                new DebugAppSearchManager(context, executor);

        return Futures.transform(debugAppSearchManager.initialize(databaseName),
                unused -> debugAppSearchManager, executor);
    }

    /**
     * Searches for all documents in the AppSearch database.
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
    private ListenableFuture<AppSearchSession> initialize(@NonNull String databaseName) {
        mAppSearchSessionFuture.setFuture(LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(mContext, databaseName)
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
