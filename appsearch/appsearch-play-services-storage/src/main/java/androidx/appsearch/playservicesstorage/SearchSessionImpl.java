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
import androidx.core.util.Preconditions;

import com.google.android.gms.appsearch.AppSearchClient;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Set;

/**
 * An implementation of {@link AppSearchSession} which proxies to a Google Play Service's
 * {@link AppSearchClient}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SearchSessionImpl implements AppSearchSession {
    private final AppSearchClient mPlayServicesClient;
    private final Features mFeatures;

    SearchSessionImpl(
            @NonNull AppSearchClient playServicesClient,
            @NonNull Features features) {
        mPlayServicesClient = Preconditions.checkNotNull(playServicesClient);
        mFeatures = Preconditions.checkNotNull(features);
    }

    @NonNull
    @Override
    public ListenableFuture<SetSchemaResponse> setSchemaAsync(@NonNull SetSchemaRequest request) {
        // TODO(b/274986359): Implement setSchemaAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "setSchemaAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<GetSchemaResponse> getSchemaAsync() {
        // TODO(b/274986359): Implement getSchemaAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "getSchemaAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<Set<String>> getNamespacesAsync() {
        // TODO(b/274986359): Implement getNamespacesAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "getNamespacesAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, Void>> putAsync(
            @NonNull PutDocumentsRequest request) {
        // TODO(b/274986359): Implement putAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "putAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull GetByDocumentIdRequest request) {
        // TODO(b/274986359): Implement getByDocumentIdAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "getByDocumentIdAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public SearchResults search(@NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        // TODO(b/274986359): Implement search for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "search is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<List<SearchSuggestionResult>> searchSuggestionAsync(
            @NonNull String suggestionQueryExpression,
            @NonNull SearchSuggestionSpec searchSuggestionSpec) {
        // TODO(b/274986359): Implement searchSuggestionAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "searchSuggestionAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<Void> reportUsageAsync(@NonNull ReportUsageRequest request) {
        // TODO(b/274986359): Implement reportUsageAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "reportUsageAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeAsync(
            @NonNull RemoveByDocumentIdRequest request) {
        // TODO(b/274986359): Implement removeAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "removeAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<Void> removeAsync(@NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        // TODO(b/274986359): Implement removeAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "removeAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<StorageInfo> getStorageInfoAsync() {
        // TODO(b/274986359): Implement getStorageInfoAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "getStorageInfoAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<Void> requestFlushAsync() {
        // TODO(b/274986359): Implement getSchemaAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "requestFlushAsync is not supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public Features getFeatures() {
        return mFeatures;
    }

    @Override
    public void close() {
        mPlayServicesClient.close();
    }
}
