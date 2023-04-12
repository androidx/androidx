/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.playservicesstorage;

import androidx.annotation.NonNull;
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
import androidx.appsearch.playservicesstorage.converter.AppSearchResultToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.GenericDocumentToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.GetSchemaResponseToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.RequestToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.ResponseToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.SearchSpecToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.SetSchemaRequestToGmsConverter;
import androidx.appsearch.playservicesstorage.util.AppSearchTaskFutures;
import androidx.core.util.Preconditions;

import com.google.android.gms.appsearch.AppSearchClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

/**
 * An implementation of {@link AppSearchSession} which proxies to a Google Play Service's
 * {@link AppSearchClient}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SearchSessionImpl implements AppSearchSession, Closeable {
    private final AppSearchClient mGmsClient;
    private final Features mFeatures;
    private final String mDatabaseName;


    SearchSessionImpl(
            @NonNull AppSearchClient gmsClient,
            @NonNull Features features,
            @NonNull String databaseName) {
        mGmsClient = Preconditions.checkNotNull(gmsClient);
        mFeatures = Preconditions.checkNotNull(features);
        mDatabaseName = Preconditions.checkNotNull(databaseName);
    }

    @NonNull
    @Override
    public ListenableFuture<SetSchemaResponse> setSchemaAsync(@NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(request);
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.setSchema(
                        SetSchemaRequestToGmsConverter.toGmsSetSchemaRequest(
                                request),
                        mDatabaseName),
                SetSchemaRequestToGmsConverter::toJetpackSetSchemaResponse);
    }

    @NonNull
    @Override
    public ListenableFuture<GetSchemaResponse> getSchemaAsync() {
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.getSchema(mDatabaseName),
                GetSchemaResponseToGmsConverter::toJetpackGetSchemaResponse);
    }

    @NonNull
    @Override
    public ListenableFuture<Set<String>> getNamespacesAsync() {
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.getNamespaces(mDatabaseName),
                /* valueMapper= */ i -> i);
    }

    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, Void>> putAsync(
            @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(request);
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.put(
                        RequestToGmsConverter.toGmsPutDocumentsRequest(request),
                        mDatabaseName),
                result -> AppSearchResultToGmsConverter.gmsAppSearchBatchResultToJetpack(
                        result, /* valueMapper= */ i -> i));
    }

    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull GetByDocumentIdRequest request) {
        Preconditions.checkNotNull(request);
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.getByDocumentId(
                        RequestToGmsConverter.toGmsGetByDocumentIdRequest(request),
                        mDatabaseName),
                result -> AppSearchResultToGmsConverter.gmsAppSearchBatchResultToJetpack(
                                result, GenericDocumentToGmsConverter::toJetpackGenericDocument));
    }

    @NonNull
    @Override
    public SearchResults search(@NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        com.google.android.gms.appsearch.SearchResults gmsSearchResults =
                mGmsClient.search(
                        queryExpression,
                        SearchSpecToGmsConverter.toGmsSearchSpec(searchSpec),
                        mDatabaseName);
        return new SearchResultsImpl(gmsSearchResults, searchSpec);
    }

    @NonNull
    @Override
    public ListenableFuture<List<SearchSuggestionResult>> searchSuggestionAsync(
            @NonNull String suggestionQueryExpression,
            @NonNull SearchSuggestionSpec searchSuggestionSpec) {
        // TODO(b/274986359): Implement searchSuggestionAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "Search Suggestion is not yet supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<Void> reportUsageAsync(@NonNull ReportUsageRequest request) {
        Preconditions.checkNotNull(request);
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.reportUsage(
                        RequestToGmsConverter.toGmsReportUsageRequest(request),
                        mDatabaseName),
                /* valueMapper= */ i -> i);
    }

    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeAsync(
            @NonNull RemoveByDocumentIdRequest request) {
        Preconditions.checkNotNull(request);
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.remove(
                        RequestToGmsConverter
                                .toGmsRemoveByDocumentIdRequest(request),
                        mDatabaseName),
                result -> AppSearchResultToGmsConverter.gmsAppSearchBatchResultToJetpack(
                        result, /* valueMapper= */ i -> i));
    }

    @NonNull
    @Override
    public ListenableFuture<Void> removeAsync(@NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        return AppSearchTaskFutures.toListenableFuture(mGmsClient.remove(
                        queryExpression,
                        SearchSpecToGmsConverter.toGmsSearchSpec(searchSpec),
                        mDatabaseName),
                /* valueMapper= */ i -> i);
    }

    @NonNull
    @Override
    public ListenableFuture<StorageInfo> getStorageInfoAsync() {
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.getStorageInfo(mDatabaseName),
                ResponseToGmsConverter::toJetpackStorageInfo);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> requestFlushAsync() {
        Task<Void> flushTask = Tasks.forResult(null);
        return AppSearchTaskFutures.toListenableFuture(flushTask, /* valueMapper= */ i-> i);
    }

    @NonNull
    @Override
    public Features getFeatures() {
        return mFeatures;
    }

    @Override
    public void close() {
        mGmsClient.close();
    }
}
