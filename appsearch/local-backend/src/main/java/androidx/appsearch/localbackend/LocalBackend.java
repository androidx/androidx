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

package androidx.appsearch.localbackend;

import static androidx.appsearch.app.AppSearchResult.newSuccessfulResult;

import android.content.Context;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.app.AppSearchBackend;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByUriRequest;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByUriRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An implementation of {@link androidx.appsearch.app.AppSearchBackend} which stores data locally
 * in the app's storage space using a bundled version of the search native library.
 *
 * <p>Queries are executed multi-threaded, but a single thread is used for mutate requests (put,
 * delete, etc..).
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LocalBackend implements AppSearchBackend {
    private static volatile ListenableFuture<AppSearchResult<LocalBackend>> sInstance;

    // Never call Executor.shutdownNow(), it will cancel the futures it's returned. And since
    // execute() won't return anything, we will hang forever waiting for the execution.
    // AppSearch multi-thread execution is guarded by Read & Write Lock in AppSearchImpl, all
    // mutate requests will need to gain write lock and query requests need to gain read lock.
    private final ExecutorService mExecutorService = Executors.newCachedThreadPool();

    private volatile LocalBackendSyncImpl mSyncImpl;

    /**
     * Returns an instance of {@link LocalBackend}.
     *
     * <p>If no instance exists, one will be created using the provided {@code context}.
     */
    @AnyThread
    @NonNull
    public static ListenableFuture<AppSearchResult<LocalBackend>> getInstance(
            @NonNull Context context) {
        Preconditions.checkNotNull(context);
        if (sInstance == null) {
            synchronized (LocalBackend.class) {
                if (sInstance == null) {
                    sInstance = new LocalBackend().initialize(context);
                }
            }
        }
        return sInstance;
    }

    private LocalBackend() {}

    // NOTE: No instance of this class should be created or returned except via initialize().
    // Once the ListenableFuture returned here is populated, the class is ready to use.
    @GuardedBy("LocalBackend.class")
    private ListenableFuture<AppSearchResult<LocalBackend>> initialize(@NonNull Context context) {
        ResolvableFuture<AppSearchResult<LocalBackend>> future = ResolvableFuture.create();
        mExecutorService.execute(() -> {
            if (!future.isCancelled()) {
                AppSearchResult<LocalBackendSyncImpl> implCreateResult =
                        LocalBackendSyncImpl.create(context);
                if (!implCreateResult.isSuccess()) {
                    future.set(
                            AppSearchResult.newFailedResult(
                                    implCreateResult.getResultCode(),
                                    implCreateResult.getErrorMessage()));
                    return;
                }
                mSyncImpl = implCreateResult.getResultValue();
                future.set(newSuccessfulResult(this));
            }
        });
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchResult<Void>> setSchema(
            @NonNull String databaseName, @NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        return execute(() -> mSyncImpl.setSchema(databaseName, request));
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> putDocuments(
            @NonNull String databaseName, @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        return execute(() -> mSyncImpl.putDocuments(databaseName, request));
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByUri(
            @NonNull String databaseName, @NonNull GetByUriRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        return execute(() -> mSyncImpl.getByUri(databaseName, request));
    }

    @Override
    @NonNull
    public BackendSearchResults query(
            @NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        return new SearchResultsImpl(mSyncImpl.query(databaseName, queryExpression, searchSpec));
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByUri(
            @NonNull String databaseName,
            @NonNull RemoveByUriRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        return execute(() -> mSyncImpl.removeByUri(databaseName, request));
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByType(
            @NonNull String databaseName, @NonNull List<String> schemaTypes) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(schemaTypes);
        return execute(() -> mSyncImpl.removeByType(databaseName, schemaTypes));
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByNamespace(
            @NonNull String databaseName, @NonNull List<String> namespaces) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(namespaces);
        return execute(() -> mSyncImpl.removeByNamespace(databaseName, namespaces));
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchResult<Void>> removeAll(@NonNull String databaseName) {
        Preconditions.checkNotNull(databaseName);
        return execute(() -> mSyncImpl.removeAll(databaseName));
    }

    @VisibleForTesting
    @Override
    @NonNull
    public ListenableFuture<AppSearchResult<Void>> resetAllDatabases() {
        return execute(() -> mSyncImpl.resetAllDatabases());
    }

    /** Executes the callable task and set result to ListenableFuture. */
    <T> ListenableFuture<T> execute(Callable<T> callable) {
        ResolvableFuture<T> future = ResolvableFuture.create();
        mExecutorService.execute(() -> {
            if (!future.isCancelled()) {
                try {
                    future.set(callable.call());
                } catch (Throwable t) {
                    future.setException(t);
                }
            }
        });
        return future;
    }

    private class SearchResultsImpl implements BackendSearchResults {
        private final LocalBackendSyncImpl.SearchResultsImpl mSyncResults;

        SearchResultsImpl(@NonNull LocalBackendSyncImpl.SearchResultsImpl syncResults) {
            mSyncResults = Preconditions.checkNotNull(syncResults);
        }

        @Override
        @NonNull
        public ListenableFuture<AppSearchResult<List<SearchResults.Result>>> getNextPage() {
            return execute(mSyncResults::getNextPage);
        }

        @Override
        @SuppressWarnings("FutureReturnValueIgnored")
        public void close() {
            // Close the SearchResult in the backend thread. No future is needed here since the
            // method is void.
            execute(() -> {
                mSyncResults.close();
                return null;
            });
        }
    }
}
