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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByUriRequest;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByUriRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final Context mContext;
    private final String mPackageName;
    private final String mDatabaseName;
    private boolean mIsMutated = false;
    private boolean mIsClosed = false;

    SearchSessionImpl(
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull ExecutorService executorService,
            @NonNull Context context,
            @NonNull String packageName,
            @NonNull String databaseName) {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mExecutorService = Preconditions.checkNotNull(executorService);
        mContext = Preconditions.checkNotNull(context);
        mPackageName = packageName;
        mDatabaseName = Preconditions.checkNotNull(databaseName);
    }

    @Override
    @NonNull
    public ListenableFuture<Void> setSchema(@NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            // Convert the inner set into a List since Binder can't handle Set.
            Map<String, Set<PackageIdentifier>> schemasPackageAccessible =
                    request.getSchemasVisibleToPackagesInternal();
            Map<String, List<PackageIdentifier>> copySchemasPackageAccessible = new ArrayMap<>();
            for (Map.Entry<String, Set<PackageIdentifier>> entry :
                    schemasPackageAccessible.entrySet()) {
                copySchemasPackageAccessible.put(entry.getKey(),
                        new ArrayList<>(entry.getValue()));
            }

            mAppSearchImpl.setSchema(
                    mPackageName,
                    mDatabaseName,
                    new ArrayList<>(request.getSchemas()),
                    new ArrayList<>(request.getSchemasNotVisibleToSystemUi()),
                    copySchemasPackageAccessible,
                    request.isForceOverride());
            mIsMutated = true;
            return null;
        });
    }

    @Override
    @NonNull
    public ListenableFuture<Set<AppSearchSchema>> getSchema() {
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            List<AppSearchSchema> schemas = mAppSearchImpl.getSchema(mPackageName, mDatabaseName);
            return new ArraySet<>(schemas);
        });
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> putDocuments(
            @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < request.getDocuments().size(); i++) {
                GenericDocument document = request.getDocuments().get(i);
                try {
                    mAppSearchImpl.putDocument(mPackageName, mDatabaseName, document);
                    resultBuilder.setSuccess(document.getUri(), /*result=*/ null);
                } catch (Throwable t) {
                    resultBuilder.setResult(document.getUri(), throwableToFailedResult(t));
                }
            }
            mIsMutated = true;
            return resultBuilder.build();
        });
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByUri(
            @NonNull GetByUriRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            AppSearchBatchResult.Builder<String, GenericDocument> resultBuilder =
                    new AppSearchBatchResult.Builder<>();

            for (String uri : request.getUris()) {
                try {
                    GenericDocument document =
                            mAppSearchImpl.getDocument(mPackageName, mDatabaseName,
                                    request.getNamespace(), uri);
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
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return new SearchResultsImpl(
                mAppSearchImpl,
                mExecutorService,
                mContext,
                mPackageName,
                mDatabaseName,
                queryExpression,
                searchSpec);
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByUri(
            @NonNull RemoveByUriRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (String uri : request.getUris()) {
                try {
                    mAppSearchImpl.remove(mPackageName, mDatabaseName, request.getNamespace(), uri);
                    resultBuilder.setSuccess(uri, /*result=*/null);
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            mIsMutated = true;
            return resultBuilder.build();
        });
    }

    @Override
    @NonNull
    public ListenableFuture<Void> removeByQuery(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            mAppSearchImpl.removeByQuery(mPackageName, mDatabaseName, queryExpression, searchSpec);
            mIsMutated = true;
            return null;
        });
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void close() {
        if (mIsMutated && !mIsClosed) {
            // No future is needed here since the method is void.
            FutureUtil.execute(mExecutorService, () -> {
                mAppSearchImpl.persistToDisk();
                mIsClosed = true;
                return null;
            });
        }
    }

    private <T> ListenableFuture<T> execute(Callable<T> callable) {
        return FutureUtil.execute(mExecutorService, callable);
    }
}
