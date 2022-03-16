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

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.ObserverCallback;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.observer.SchemaChangeInfo;
import androidx.appsearch.platformstorage.converter.AppSearchResultToPlatformConverter;
import androidx.appsearch.platformstorage.converter.GenericDocumentToPlatformConverter;
import androidx.appsearch.platformstorage.converter.GetSchemaResponseToPlatformConverter;
import androidx.appsearch.platformstorage.converter.ObserverSpecToPlatformConverter;
import androidx.appsearch.platformstorage.converter.RequestToPlatformConverter;
import androidx.appsearch.platformstorage.converter.SearchSpecToPlatformConverter;
import androidx.appsearch.platformstorage.util.BatchResultCallbackAdapter;
import androidx.collection.ArrayMap;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.os.BuildCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * An implementation of {@link androidx.appsearch.app.GlobalSearchSession} which proxies to a
 * platform {@link android.app.appsearch.GlobalSearchSession}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
class GlobalSearchSessionImpl implements GlobalSearchSession {
    private final android.app.appsearch.GlobalSearchSession mPlatformSession;
    private final Executor mExecutor;
    private final Features mFeatures;

    // Management of observer callbacks.
    @GuardedBy("mObserverCallbacksLocked")
    private final Map<ObserverCallback, android.app.appsearch.observer.AppSearchObserverCallback>
            mObserverCallbacksLocked = new ArrayMap<>();

    GlobalSearchSessionImpl(
            @NonNull android.app.appsearch.GlobalSearchSession platformSession,
            @NonNull Executor executor,
            @NonNull Features features) {
        mPlatformSession = Preconditions.checkNotNull(platformSession);
        mExecutor = Preconditions.checkNotNull(executor);
        mFeatures = Preconditions.checkNotNull(features);
    }

    @BuildCompat.PrereleaseSdkCheck
    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull String packageName, @NonNull String databaseName,
            @NonNull GetByDocumentIdRequest request) {
        if (!BuildCompat.isAtLeastT()) {
            throw new UnsupportedOperationException(Features.GLOBAL_SEARCH_SESSION_GET_BY_ID
                    + " is not supported on this AppSearch implementation.");
        }
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        ResolvableFuture<AppSearchBatchResult<String, GenericDocument>> future =
                ResolvableFuture.create();
        mPlatformSession.getByDocumentId(packageName, databaseName,
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
    public ListenableFuture<Void> reportSystemUsageAsync(
            @NonNull ReportSystemUsageRequest request) {
        Preconditions.checkNotNull(request);
        ResolvableFuture<Void> future = ResolvableFuture.create();
        mPlatformSession.reportSystemUsage(
                RequestToPlatformConverter.toPlatformReportSystemUsageRequest(request),
                mExecutor,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                        result, future));
        return future;
    }

    @BuildCompat.PrereleaseSdkCheck
    @NonNull
    @Override
    public ListenableFuture<GetSchemaResponse> getSchemaAsync(@NonNull String packageName,
            @NonNull String databaseName) {
        // Superclass is annotated with @RequiresFeature, so we shouldn't get here on an
        // unsupported build.
        if (!BuildCompat.isAtLeastT()) {
            throw new UnsupportedOperationException(
                    Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA
                            + " is not supported on this AppSearch implementation.");
        }
        ResolvableFuture<GetSchemaResponse> future = ResolvableFuture.create();
        mPlatformSession.getSchema(
                packageName,
                databaseName,
                mExecutor,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                        result,
                        future,
                        GetSchemaResponseToPlatformConverter::toJetpackGetSchemaResponse));
        return future;
    }

    @NonNull
    @Override
    public Features getFeatures() {
        return mFeatures;
    }

    // TODO(b/193494000): Remove these two lines once BuildCompat.isAtLeastT() is removed.
    @SuppressLint("NewApi")
    @BuildCompat.PrereleaseSdkCheck
    @Override
    public void registerObserverCallback(
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull ObserverCallback observer) throws AppSearchException {
        Preconditions.checkNotNull(targetPackageName);
        Preconditions.checkNotNull(spec);
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(observer);
        // Superclass is annotated with @RequiresFeature, so we shouldn't get here on an
        // unsupported build.
        if (!BuildCompat.isAtLeastT()) {
            throw new UnsupportedOperationException(
                    Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK
                            + " is not supported on this AppSearch implementation");
        }

        synchronized (mObserverCallbacksLocked) {
            android.app.appsearch.observer.AppSearchObserverCallback frameworkCallback =
                    mObserverCallbacksLocked.get(observer);
            if (frameworkCallback == null) {
                // No stub is associated with this package and observer, so we must create one.
                frameworkCallback = new android.app.appsearch.observer.AppSearchObserverCallback() {
                    @Override
                    public void onSchemaChanged(
                            @NonNull android.app.appsearch.observer.SchemaChangeInfo
                                    platformSchemaChangeInfo) {
                        SchemaChangeInfo jetpackSchemaChangeInfo =
                                ObserverSpecToPlatformConverter.toJetpackSchemaChangeInfo(
                                        platformSchemaChangeInfo);
                        observer.onSchemaChanged(jetpackSchemaChangeInfo);
                    }

                    @Override
                    public void onDocumentChanged(
                            @NonNull android.app.appsearch.observer.DocumentChangeInfo
                                    platformDocumentChangeInfo) {
                        DocumentChangeInfo jetpackDocumentChangeInfo =
                                ObserverSpecToPlatformConverter.toJetpackDocumentChangeInfo(
                                        platformDocumentChangeInfo);
                        observer.onDocumentChanged(jetpackDocumentChangeInfo);
                    }
                };
            }

            // Regardless of whether this stub was fresh or not, we have to register it again
            // because the user might be supplying a different spec.
            try {
                mPlatformSession.addObserver(
                        targetPackageName,
                        ObserverSpecToPlatformConverter.toPlatformObserverSpec(spec),
                        executor,
                        frameworkCallback);
            } catch (android.app.appsearch.exceptions.AppSearchException e) {
                throw new AppSearchException((int) e.getResultCode(), e.getMessage(), e.getCause());
            }

            // Now that registration has succeeded, save this stub into our in-memory cache. This
            // isn't done when errors occur because the user may not call removeObserver if
            // addObserver threw.
            mObserverCallbacksLocked.put(observer, frameworkCallback);
        }
    }

    @SuppressLint("NewApi")
    @BuildCompat.PrereleaseSdkCheck
    @Override
    public void unregisterObserverCallback(
            @NonNull String targetPackageName, @NonNull ObserverCallback observer)
            throws AppSearchException {
        Preconditions.checkNotNull(targetPackageName);
        Preconditions.checkNotNull(observer);
        // Superclass is annotated with @RequiresFeature, so we shouldn't get here on an
        // unsupported build.
        if (!BuildCompat.isAtLeastT()) {
            throw new UnsupportedOperationException(
                    Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK
                            + " is not supported on this AppSearch implementation");
        }

        android.app.appsearch.observer.AppSearchObserverCallback frameworkCallback;
        synchronized (mObserverCallbacksLocked) {
            frameworkCallback = mObserverCallbacksLocked.get(observer);
            if (frameworkCallback == null) {
                return;  // No such observer registered. Nothing to do.
            }

            try {
                mPlatformSession.removeObserver(targetPackageName, frameworkCallback);
            } catch (android.app.appsearch.exceptions.AppSearchException e) {
                throw new AppSearchException((int) e.getResultCode(), e.getMessage(), e.getCause());
            }

            // Only remove from the in-memory map once removal from the service side succeeds
            mObserverCallbacksLocked.remove(observer);
        }
    }

    @Override
    public void close() {
        mPlatformSession.close();
    }
}
