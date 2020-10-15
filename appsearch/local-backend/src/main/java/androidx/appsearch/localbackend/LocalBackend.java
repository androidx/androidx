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

import static androidx.appsearch.app.AppSearchResult.newFailedResult;
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
import androidx.appsearch.exceptions.AppSearchException;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
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
 *
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

    private static final String ICING_DIR = "icing";

    // Package-level visibility to allow SearchResultsImpl to access it without a synthetic
    // accessor.
    volatile AppSearchImpl mAppSearchImpl;


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
        Preconditions.checkNotNull(context);

        ResolvableFuture<AppSearchResult<LocalBackend>> future = ResolvableFuture.create();
        mExecutorService.execute(() -> {
            if (!future.isCancelled()) {

                File icingDir = new File(context.getFilesDir(), ICING_DIR);
                try {
                    mAppSearchImpl = AppSearchImpl.create(icingDir);
                } catch (Throwable t) {
                    future.set(throwableToFailedResult(t));
                }

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

        return execute(() -> {
            try {
                mAppSearchImpl.setSchema(
                        databaseName, request.getSchemas(), request.isForceOverride());
                return AppSearchResult.newSuccessfulResult(/*value=*/ null);
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        });
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> putDocuments(
            @NonNull String databaseName, @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);

        return execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < request.getDocuments().size(); i++) {
                GenericDocument document = request.getDocuments().get(i);
                try {
                    mAppSearchImpl.putDocument(databaseName, document);
                    resultBuilder.setSuccess(document.getUri(), /*result=*/ null);
                } catch (Throwable t) {
                    resultBuilder.setResult(document.getUri(), throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        });
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByUri(
            @NonNull String databaseName, @NonNull GetByUriRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);

        return execute(() -> {
            AppSearchBatchResult.Builder<String, GenericDocument> resultBuilder =
                    new AppSearchBatchResult.Builder<>();

            for (String uri : request.getUris()) {
                try {
                    GenericDocument document =
                            mAppSearchImpl.getDocument(databaseName, request.getNamespace(), uri);
                    resultBuilder.setSuccess(uri, document);
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        });
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
        return new SearchResultsImpl(databaseName, queryExpression, searchSpec);
    }

    @Override
    @NonNull
    public BackendSearchResults globalQuery(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        return new SearchResultsImpl(/*databaseName=*/ null, queryExpression, searchSpec);
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByUri(
            @NonNull String databaseName,
            @NonNull RemoveByUriRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);

        return execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (String uri : request.getUris()) {
                try {
                    mAppSearchImpl.remove(databaseName, request.getNamespace(), uri);
                    resultBuilder.setSuccess(uri, /*result=*/null);
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        });
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchResult<Void>> removeByQuery(@NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);

        return execute(() -> {
            try {
                mAppSearchImpl.removeByQuery(databaseName, queryExpression, searchSpec);
                return AppSearchResult.newSuccessfulResult(null);
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        });
    }

    @VisibleForTesting
    @Override
    @NonNull
    public ListenableFuture<AppSearchResult<Void>> resetAllDatabases() {
        return execute(() -> {
            try {
                mAppSearchImpl.reset();
                return AppSearchResult.newSuccessfulResult(null);
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        });
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

    @NonNull
    <ValueType> AppSearchResult<ValueType> throwableToFailedResult(
            @NonNull Throwable t) {
        if (t instanceof AppSearchException) {
            return ((AppSearchException) t).toAppSearchResult();
        }

        @AppSearchResult.ResultCode int resultCode;
        if (t instanceof IllegalStateException) {
            resultCode = AppSearchResult.RESULT_INTERNAL_ERROR;
        } else if (t instanceof IllegalArgumentException) {
            resultCode = AppSearchResult.RESULT_INVALID_ARGUMENT;
        } else if (t instanceof IOException) {
            resultCode = AppSearchResult.RESULT_IO_ERROR;
        } else {
            resultCode = AppSearchResult.RESULT_UNKNOWN_ERROR;
        }
        return newFailedResult(resultCode, t.toString());
    }

    private class SearchResultsImpl implements BackendSearchResults {
        private long mNextPageToken;
        private final String mDatabaseName;
        private final String mQueryExpression;
        private final SearchSpec mSearchSpec;
        private boolean mIsFirstLoad = true;

        SearchResultsImpl(
                String databaseName,
                @NonNull String queryExpression,
                @NonNull SearchSpec searchSpec) {
            Preconditions.checkNotNull(queryExpression);
            Preconditions.checkNotNull(searchSpec);
            mDatabaseName = databaseName;
            mQueryExpression = queryExpression;
            mSearchSpec = searchSpec;
        }

        @Override
        @NonNull
        public ListenableFuture<AppSearchResult<List<SearchResults.Result>>> getNextPage() {
            return execute(() -> {
                try {
                    AppSearchImpl.SearchResultPage searchResultPage;
                    if (mIsFirstLoad) {
                        mIsFirstLoad = false;
                        if (mDatabaseName == null) {
                            // Global query, there's no one database to check.
                            searchResultPage = mAppSearchImpl.globalQuery(
                                    mQueryExpression, mSearchSpec);
                        } else {
                            // Normal local query, pass in specified database.
                            searchResultPage = mAppSearchImpl.query(
                                    mDatabaseName, mQueryExpression, mSearchSpec);
                        }
                    } else {
                        searchResultPage = mAppSearchImpl.getNextPage(mNextPageToken);
                    }
                    mNextPageToken = searchResultPage.getNextPageToken();
                    return AppSearchResult.newSuccessfulResult(
                            searchResultPage.getResults());
                } catch (Throwable t) {
                    return throwableToFailedResult(t);
                }
            });
        }

        @Override
        @SuppressWarnings("FutureReturnValueIgnored")
        public void close() {
            // No future is needed here since the method is void.
            execute(() -> {
                mAppSearchImpl.invalidateNextPageToken(mNextPageToken);
                return null;
            });
        }
    }
}
