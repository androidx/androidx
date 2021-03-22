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
package androidx.appsearch.platformstorage;

import android.os.Build;
import android.util.ArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByUriRequest;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByUriRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.platformstorage.converter.AppSearchResultToPlatformConverter;
import androidx.appsearch.platformstorage.converter.GenericDocumentToPlatformConverter;
import androidx.appsearch.platformstorage.converter.RequestToPlatformConverter;
import androidx.appsearch.platformstorage.converter.SchemaToPlatformConverter;
import androidx.appsearch.platformstorage.converter.SearchSpecToPlatformConverter;
import androidx.appsearch.platformstorage.util.BatchResultCallbackAdapter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * An implementation of {@link AppSearchSession} which proxies to a platform
 * {@link android.app.appsearch.AppSearchSession}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
class SearchSessionImpl implements AppSearchSession {
    private final android.app.appsearch.AppSearchSession mPlatformSession;
    private final ExecutorService mExecutorService;

    SearchSessionImpl(
            @NonNull android.app.appsearch.AppSearchSession platformSession,
            @NonNull ExecutorService executorService) {
        mPlatformSession = Preconditions.checkNotNull(platformSession);
        mExecutorService = Preconditions.checkNotNull(executorService);
    }

    @Override
    @NonNull
    public ListenableFuture<SetSchemaResponse> setSchema(@NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<SetSchemaResponse> future = ResolvableFuture.create();
        mPlatformSession.setSchema(
                RequestToPlatformConverter.toPlatformSetSchemaRequest(request),
                mExecutorService,
                result -> {
                    if (result.isSuccess()) {
                        SetSchemaResponse jetpackResponse =
                                RequestToPlatformConverter.toJetpackSetSchemaResponse(
                                        result.getResultValue());
                        future.set(jetpackResponse);
                    } else {
                        handleFailedPlatformResult(result, future);
                    }
                });
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<Set<AppSearchSchema>> getSchema() {
        ResolvableFuture<Set<AppSearchSchema>> future = ResolvableFuture.create();
        mPlatformSession.getSchema(
                mExecutorService,
                result -> {
                    if (result.isSuccess()) {
                        Set<android.app.appsearch.AppSearchSchema> platformSchemas =
                                result.getResultValue();
                        Set<AppSearchSchema> jetpackSchemas =
                                new ArraySet<>(platformSchemas.size());
                        for (android.app.appsearch.AppSearchSchema platformSchema :
                                platformSchemas) {
                            jetpackSchemas.add(
                                    SchemaToPlatformConverter.toJetpackSchema(platformSchema));
                        }
                        future.set(jetpackSchemas);
                    } else {
                        handleFailedPlatformResult(result, future);
                    }
                });
        return future;
    }

    @NonNull
    @Override
    public ListenableFuture<Set<String>> getNamespaces() {
        // TODO(b/183042276): Implement this once getNamespaces() is exposed in the platform SDK
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> put(
            @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<AppSearchBatchResult<String, Void>> future = ResolvableFuture.create();
        mPlatformSession.put(
                RequestToPlatformConverter.toPlatformPutDocumentsRequest(request),
                mExecutorService,
                BatchResultCallbackAdapter.forSameValueType(future));
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByUri(
            @NonNull GetByUriRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<AppSearchBatchResult<String, GenericDocument>> future =
                ResolvableFuture.create();
        mPlatformSession.getByUri(
                RequestToPlatformConverter.toPlatformGetByUriRequest(request),
                mExecutorService,
                new BatchResultCallbackAdapter<>(
                        future, GenericDocumentToPlatformConverter::toJetpackGenericDocument));
        return future;
    }

    @Override
    @NonNull
    public SearchResults search(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        android.app.appsearch.SearchResults platformSearchResults =
                mPlatformSession.search(
                        queryExpression,
                        SearchSpecToPlatformConverter.toPlatformSearchSpec(searchSpec));
        return new SearchResultsImpl(platformSearchResults, mExecutorService);
    }

    @Override
    @NonNull
    public ListenableFuture<Void> reportUsage(@NonNull ReportUsageRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<Void> future = ResolvableFuture.create();
        mPlatformSession.reportUsage(
                RequestToPlatformConverter.toPlatformReportUsageRequest(request),
                mExecutorService,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                        result, future));
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> remove(
            @NonNull RemoveByUriRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<AppSearchBatchResult<String, Void>> future = ResolvableFuture.create();
        mPlatformSession.remove(
                RequestToPlatformConverter.toPlatformRemoveByUriRequest(request),
                mExecutorService,
                BatchResultCallbackAdapter.forSameValueType(future));
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<Void> remove(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        ResolvableFuture<Void> future = ResolvableFuture.create();
        mPlatformSession.remove(
                queryExpression,
                SearchSpecToPlatformConverter.toPlatformSearchSpec(searchSpec),
                mExecutorService,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                        result, future));
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<StorageInfo> getStorageInfo() {
        ResolvableFuture<StorageInfo> future = ResolvableFuture.create();
        // TODO(b/182909475): Implement this if we decide to expose an API on platform.
        future.set(new StorageInfo.Builder().build());
        return future;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> maybeFlush() {
        ResolvableFuture<Void> future = ResolvableFuture.create();
        // The data in platform will be flushed by scheduled task. This api won't do anything extra
        // flush.
        future.set(null);
        return future;
    }

    @Override
    public void close() {
        mPlatformSession.close();
    }

    private void handleFailedPlatformResult(
            @NonNull android.app.appsearch.AppSearchResult<?> platformResult,
            @NonNull ResolvableFuture<?> future) {
        future.setException(
                new AppSearchException(
                        platformResult.getResultCode(), platformResult.getErrorMessage()));
    }
}
