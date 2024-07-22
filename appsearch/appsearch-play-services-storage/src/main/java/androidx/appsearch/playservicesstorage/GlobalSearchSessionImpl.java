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
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.app.ReportSystemUsageRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.observer.ObserverCallback;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.playservicesstorage.converter.AppSearchResultToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.GenericDocumentToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.GetSchemaResponseToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.RequestToGmsConverter;
import androidx.appsearch.playservicesstorage.converter.SearchSpecToGmsConverter;
import androidx.appsearch.playservicesstorage.util.AppSearchTaskFutures;
import androidx.core.util.Preconditions;

import com.google.android.gms.appsearch.GlobalSearchClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * An implementation of {@link GlobalSearchSession} which proxies to a Google Play Service's
 * {@link GlobalSearchClient}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GlobalSearchSessionImpl implements GlobalSearchSession {

    private final GlobalSearchClient mGmsClient;
    private final Features mFeatures;
    private final Executor mExecutor;

    private boolean mIsClosed = false;

    GlobalSearchSessionImpl(
            @NonNull GlobalSearchClient gmsClient,
            @NonNull Features features,
            @NonNull Executor executor) {
        mGmsClient = Preconditions.checkNotNull(gmsClient);
        mFeatures = Preconditions.checkNotNull(features);
        mExecutor = Preconditions.checkNotNull(executor);
    }
    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull String packageName, @NonNull String databaseName,
            @NonNull GetByDocumentIdRequest request) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.getByDocumentId(packageName, databaseName,
                        RequestToGmsConverter.toGmsGetByDocumentIdRequest(request)),
                result -> AppSearchResultToGmsConverter.gmsAppSearchBatchResultToJetpack(
                        result, GenericDocumentToGmsConverter::toJetpackGenericDocument),
                mExecutor);
    }

    @NonNull
    @Override
    public SearchResults search(@NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        com.google.android.gms.appsearch.SearchResults searchResults =
                mGmsClient.search(queryExpression,
                        SearchSpecToGmsConverter.toGmsSearchSpec(searchSpec));
        return new SearchResultsImpl(searchResults, mExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> reportSystemUsageAsync(
            @NonNull ReportSystemUsageRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        Task<Void> flushTask = Tasks.forResult(null);
        return AppSearchTaskFutures.toListenableFuture(flushTask, /* valueMapper= */ i-> i,
                mExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<GetSchemaResponse> getSchemaAsync(@NonNull String packageName,
            @NonNull String databaseName) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return AppSearchTaskFutures.toListenableFuture(
                mGmsClient.getSchema(packageName, databaseName),
                GetSchemaResponseToGmsConverter::toJetpackGetSchemaResponse, mExecutor);
    }

    @NonNull
    @Override
    public Features getFeatures() {
        return mFeatures;
    }

    @Override
    public void registerObserverCallback(@NonNull String targetPackageName,
            @NonNull ObserverSpec spec, @NonNull Executor executor,
            @NonNull ObserverCallback observer) throws AppSearchException {
        // TODO(b/274986359): Implement registerObserverCallback for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "registerObserverCallback is not yet supported on this AppSearch implementation.");
    }

    @Override
    public void unregisterObserverCallback(@NonNull String targetPackageName,
            @NonNull ObserverCallback observer) throws AppSearchException {
        // TODO(b/274986359): Implement unregisterObserverCallback for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "unregisterObserverCallback is not yet supported on this "
                        + "AppSearch implementation.");
    }

    @Override
    public void close() {
        mIsClosed = true;
    }
}
