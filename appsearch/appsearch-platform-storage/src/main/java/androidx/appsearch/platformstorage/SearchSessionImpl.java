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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByDocumentIdRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.platformstorage.converter.AppSearchResultToPlatformConverter;
import androidx.appsearch.platformstorage.converter.GenericDocumentToPlatformConverter;
import androidx.appsearch.platformstorage.converter.GetSchemaResponseToPlatformConverter;
import androidx.appsearch.platformstorage.converter.RequestToPlatformConverter;
import androidx.appsearch.platformstorage.converter.ResponseToPlatformConverter;
import androidx.appsearch.platformstorage.converter.SearchSpecToPlatformConverter;
import androidx.appsearch.platformstorage.converter.SetSchemaRequestToPlatformConverter;
import androidx.appsearch.platformstorage.util.BatchResultCallbackAdapter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.os.BuildCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * An implementation of {@link AppSearchSession} which proxies to a platform
 * {@link android.app.appsearch.AppSearchSession}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
class SearchSessionImpl implements AppSearchSession {
    private final android.app.appsearch.AppSearchSession mPlatformSession;
    private final Executor mExecutor;
    private final Features mFeatures;

    SearchSessionImpl(
            @NonNull android.app.appsearch.AppSearchSession platformSession,
            @NonNull Executor executor,
            @NonNull Features features) {
        mPlatformSession = Preconditions.checkNotNull(platformSession);
        mExecutor = Preconditions.checkNotNull(executor);
        mFeatures = Preconditions.checkNotNull(features);
    }

    @Override
    @NonNull
    @BuildCompat.PrereleaseSdkCheck
    public ListenableFuture<SetSchemaResponse> setSchemaAsync(@NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<SetSchemaResponse> future = ResolvableFuture.create();
        mPlatformSession.setSchema(
                SetSchemaRequestToPlatformConverter.toPlatformSetSchemaRequest(request),
                mExecutor,
                mExecutor,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                        result,
                        future,
                        SetSchemaRequestToPlatformConverter::toJetpackSetSchemaResponse));
        return future;
    }

    @Override
    @NonNull
    @BuildCompat.PrereleaseSdkCheck
    public ListenableFuture<GetSchemaResponse> getSchemaAsync() {
        ResolvableFuture<GetSchemaResponse> future = ResolvableFuture.create();
        mPlatformSession.getSchema(
                mExecutor,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                        result,
                        future,
                        GetSchemaResponseToPlatformConverter::toJetpackGetSchemaResponse));
        return future;
    }

    @NonNull
    @Override
    public ListenableFuture<Set<String>> getNamespacesAsync() {
        ResolvableFuture<Set<String>> future = ResolvableFuture.create();
        mPlatformSession.getNamespaces(
                mExecutor,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                        result, future));
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> putAsync(
            @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<AppSearchBatchResult<String, Void>> future = ResolvableFuture.create();
        mPlatformSession.put(
                RequestToPlatformConverter.toPlatformPutDocumentsRequest(request),
                mExecutor,
                BatchResultCallbackAdapter.forSameValueType(future));
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull GetByDocumentIdRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<AppSearchBatchResult<String, GenericDocument>> future =
                ResolvableFuture.create();
        mPlatformSession.getByDocumentId(
                RequestToPlatformConverter.toPlatformGetByDocumentIdRequest(request),
                mExecutor,
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
        return new SearchResultsImpl(platformSearchResults, searchSpec, mExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<List<SearchSuggestionResult>> searchSuggestionAsync(
            @NonNull String suggestionQueryExpression, @NonNull SearchSuggestionSpec searchSpec) {
        // TODO(b/227356108) Implement this after we export to framework.
        throw new UnsupportedOperationException(
                "Search Suggestion is not supported on this AppSearch implementation.");
    }

    @Override
    @NonNull
    public ListenableFuture<Void> reportUsageAsync(@NonNull ReportUsageRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<Void> future = ResolvableFuture.create();
        mPlatformSession.reportUsage(
                RequestToPlatformConverter.toPlatformReportUsageRequest(request),
                mExecutor,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                        result, future));
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeAsync(
            @NonNull RemoveByDocumentIdRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<AppSearchBatchResult<String, Void>> future = ResolvableFuture.create();
        mPlatformSession.remove(
                RequestToPlatformConverter.toPlatformRemoveByDocumentIdRequest(request),
                mExecutor,
                BatchResultCallbackAdapter.forSameValueType(future));
        return future;
    }

    @Override
    @NonNull
    @BuildCompat.PrereleaseSdkCheck
    public ListenableFuture<Void> removeAsync(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        ResolvableFuture<Void> future = ResolvableFuture.create();

        if (!BuildCompat.isAtLeastT() && !searchSpec.getFilterNamespaces().isEmpty()) {
            // This is a patch for b/197361770, framework-appsearch in Android S will
            // disable the given namespace filter if it is not empty and none of given namespaces
            // exist.
            // And that will result in Icing remove documents under all namespaces if it matches
            // query express and schema filter.
            mPlatformSession.getNamespaces(
                    mExecutor,
                    namespaceResult -> {
                        if (!namespaceResult.isSuccess()) {
                            future.setException(
                                    new AppSearchException(
                                            namespaceResult.getResultCode(),
                                            namespaceResult.getErrorMessage()));
                            return;
                        }

                        try {
                            Set<String> existingNamespaces = namespaceResult.getResultValue();
                            List<String> filterNamespaces = searchSpec.getFilterNamespaces();
                            for (int i = 0; i < filterNamespaces.size(); i++) {
                                if (existingNamespaces.contains(filterNamespaces.get(i))) {
                                    // There is a given namespace exist in AppSearch, we are fine.
                                    mPlatformSession.remove(
                                            queryExpression,
                                            SearchSpecToPlatformConverter
                                                    .toPlatformSearchSpec(searchSpec),
                                            mExecutor,
                                            removeResult ->
                                                    AppSearchResultToPlatformConverter
                                                            .platformAppSearchResultToFuture(
                                                                    removeResult, future));
                                    return;
                                }
                            }
                            // None of the namespace in the given namespace filter exists. Return
                            // early.
                            future.set(null);
                        } catch (Throwable t) {
                            future.setException(t);
                        }
                    });
        } else {
            // Handle normally for Android T and above.
            mPlatformSession.remove(
                    queryExpression,
                    SearchSpecToPlatformConverter.toPlatformSearchSpec(searchSpec),
                    mExecutor,
                    removeResult -> AppSearchResultToPlatformConverter
                            .platformAppSearchResultToFuture(removeResult, future));
        }
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<StorageInfo> getStorageInfoAsync() {
        ResolvableFuture<StorageInfo> future = ResolvableFuture.create();
        mPlatformSession.getStorageInfo(
                mExecutor,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                        result, future, ResponseToPlatformConverter::toJetpackStorageInfo)
        );
        return future;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> requestFlushAsync() {
        ResolvableFuture<Void> future = ResolvableFuture.create();
        // The data in platform will be flushed by scheduled task. This api won't do anything extra
        // flush.
        future.set(null);
        return future;
    }

    @NonNull
    @Override
    public Features getFeatures() {
        return mFeatures;
    }

    @Override
    public void close() {
        mPlatformSession.close();
    }
}
