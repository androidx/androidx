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
package androidx.appsearch.localstorage;

import static androidx.appsearch.app.AppSearchResult.throwableToFailedResult;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByUriRequest;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByUriRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * An implementation of {@link AppSearchSession} which stores data locally
 * in the app's storage space using a bundled version of the search native library.
 *
 * <p>Queries are executed multi-threaded, but a single thread is used for mutate requests (put,
 * delete, etc..).
 */
class SearchSessionImpl implements AppSearchSession {
    private final AppSearchImpl mAppSearchImpl;
    private final ExecutorService mExecutorService;
    private final String mDatabaseName;

    SearchSessionImpl(
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull ExecutorService executorService,
            @NonNull String databaseName) {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mExecutorService = Preconditions.checkNotNull(executorService);
        mDatabaseName = Preconditions.checkNotNull(databaseName);
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchResult<Void>> setSchema(@NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(request);
        return execute(() -> {
            try {
                mAppSearchImpl.setSchema(
                        mDatabaseName, request.getSchemas(),
                        request.getSchemasNotPlatformSurfaceable(), request.isForceOverride());
                return AppSearchResult.newSuccessfulResult(/*value=*/ null);
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        });
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchResult<Set<AppSearchSchema>>> getSchema() {
        return execute(() -> {
            try {
                return AppSearchResult.newSuccessfulResult(mAppSearchImpl.getSchema(mDatabaseName));
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        });
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> putDocuments(
            @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(request);
        return execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < request.getDocuments().size(); i++) {
                GenericDocument document = request.getDocuments().get(i);
                try {
                    mAppSearchImpl.putDocument(mDatabaseName, document);
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
            @NonNull GetByUriRequest request) {
        Preconditions.checkNotNull(request);
        return execute(() -> {
            AppSearchBatchResult.Builder<String, GenericDocument> resultBuilder =
                    new AppSearchBatchResult.Builder<>();

            for (String uri : request.getUris()) {
                try {
                    GenericDocument document =
                            mAppSearchImpl.getDocument(mDatabaseName, request.getNamespace(), uri);
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
    public SearchResults query(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        return new SearchResultsImpl(
                mAppSearchImpl,
                mExecutorService,
                mDatabaseName,
                queryExpression,
                searchSpec);
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByUri(
            @NonNull RemoveByUriRequest request) {
        Preconditions.checkNotNull(request);
        return execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (String uri : request.getUris()) {
                try {
                    mAppSearchImpl.remove(mDatabaseName, request.getNamespace(), uri);
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
    public ListenableFuture<AppSearchResult<Void>> removeByQuery(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        return execute(() -> {
            try {
                mAppSearchImpl.removeByQuery(mDatabaseName, queryExpression, searchSpec);
                return AppSearchResult.newSuccessfulResult(null);
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        });
    }

    private <T> ListenableFuture<T> execute(Callable<T> callable) {
        return FutureUtil.execute(mExecutorService, callable);
    }
}
