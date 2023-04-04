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
import androidx.core.util.Preconditions;

import com.google.android.gms.appsearch.GlobalSearchClient;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * An implementation of {@link GlobalSearchSession} which proxies to a Google Play Service's
 * {@link GlobalSearchClient}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GlobalSearchSessionImpl implements GlobalSearchSession {

    private final GlobalSearchClient mPlayServicesClient;
    private final Features mFeatures;

    GlobalSearchSessionImpl(
            @NonNull GlobalSearchClient playServicesClient,
            @NonNull Features features) {
        mPlayServicesClient = Preconditions.checkNotNull(playServicesClient);
        mFeatures = Preconditions.checkNotNull(features);
    }
    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull String packageName, @NonNull String databaseName,
            @NonNull GetByDocumentIdRequest request) {
        // TODO(b/274986359): Implement getByDocumentIdAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "getByDocumentIdAsync is not yet supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public SearchResults search(@NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        // TODO(b/274986359): Implement search for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "search is not yet supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<Void> reportSystemUsageAsync(
            @NonNull ReportSystemUsageRequest request) {
        // TODO(b/274986359): Implement reportSystemUsageAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "reportSystemUsageAsync is not yet supported on this AppSearch implementation.");
    }

    @NonNull
    @Override
    public ListenableFuture<GetSchemaResponse> getSchemaAsync(@NonNull String packageName,
            @NonNull String databaseName) {
        // TODO(b/274986359): Implement getSchemaAsync for PlayServicesStorage.
        throw new UnsupportedOperationException(
                "getSchemaAsync is not yet supported on this AppSearch implementation.");
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
        mPlayServicesClient.close();
    }
}
