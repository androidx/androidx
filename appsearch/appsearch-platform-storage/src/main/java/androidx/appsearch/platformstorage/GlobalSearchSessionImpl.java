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

import android.app.appsearch.AppSearchResult;
import android.app.appsearch.BatchResultCallback;
import android.os.Build;

import androidx.annotation.DoNotInline;
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
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * An implementation of {@link GlobalSearchSession} which proxies to a
 * platform {@link android.app.appsearch.GlobalSearchSession}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
class GlobalSearchSessionImpl implements GlobalSearchSession {
    private final android.app.appsearch.GlobalSearchSession mPlatformSession;
    private final Executor mExecutor;
    private final Features mFeatures;

    // Management of observer callbacks.
    @GuardedBy("mObserverCallbacksLocked")
    private final Map<ObserverCallback, android.app.appsearch.observer.ObserverCallback>
            mObserverCallbacksLocked = new ArrayMap<>();

    GlobalSearchSessionImpl(
            @NonNull android.app.appsearch.GlobalSearchSession platformSession,
            @NonNull Executor executor,
            @NonNull Features features) {
        mPlatformSession = Preconditions.checkNotNull(platformSession);
        mExecutor = Preconditions.checkNotNull(executor);
        mFeatures = Preconditions.checkNotNull(features);
    }

    @NonNull
    @Override
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull String packageName, @NonNull String databaseName,
            @NonNull GetByDocumentIdRequest request) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            throw new UnsupportedOperationException(Features.GLOBAL_SEARCH_SESSION_GET_BY_ID
                    + " is not supported on this AppSearch implementation.");
        }
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        ResolvableFuture<AppSearchBatchResult<String, GenericDocument>> future =
                ResolvableFuture.create();
        ApiHelperForT.getByDocumentId(mPlatformSession, packageName, databaseName,
                RequestToPlatformConverter.toPlatformGetByDocumentIdRequest(request), mExecutor,
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

    @NonNull
    @Override
    public ListenableFuture<GetSchemaResponse> getSchemaAsync(@NonNull String packageName,
            @NonNull String databaseName) {
        // Superclass is annotated with @RequiresFeature, so we shouldn't get here on an
        // unsupported build.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            throw new UnsupportedOperationException(
                    Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA
                            + " is not supported on this AppSearch implementation.");
        }
        ResolvableFuture<GetSchemaResponse> future = ResolvableFuture.create();
        ApiHelperForT.getSchema(mPlatformSession, packageName, databaseName, mExecutor,
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            throw new UnsupportedOperationException(
                    Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK
                            + " is not supported on this AppSearch implementation");
        }

        synchronized (mObserverCallbacksLocked) {
            android.app.appsearch.observer.ObserverCallback frameworkCallback =
                    mObserverCallbacksLocked.get(observer);
            if (frameworkCallback == null) {
                // No stub is associated with this package and observer, so we must create one.
                frameworkCallback = new android.app.appsearch.observer.ObserverCallback() {
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
                ApiHelperForT.registerObserverCallback(mPlatformSession, targetPackageName,
                        ObserverSpecToPlatformConverter.toPlatformObserverSpec(spec), executor,
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

    @Override
    public void unregisterObserverCallback(
            @NonNull String targetPackageName, @NonNull ObserverCallback observer)
            throws AppSearchException {
        Preconditions.checkNotNull(targetPackageName);
        Preconditions.checkNotNull(observer);
        // Superclass is annotated with @RequiresFeature, so we shouldn't get here on an
        // unsupported build.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            throw new UnsupportedOperationException(
                    Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK
                            + " is not supported on this AppSearch implementation");
        }

        android.app.appsearch.observer.ObserverCallback frameworkCallback;
        synchronized (mObserverCallbacksLocked) {
            frameworkCallback = mObserverCallbacksLocked.get(observer);
            if (frameworkCallback == null) {
                return;  // No such observer registered. Nothing to do.
            }

            try {
                ApiHelperForT.unregisterObserverCallback(mPlatformSession, targetPackageName,
                        frameworkCallback);
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static class ApiHelperForT {
        private ApiHelperForT() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void getByDocumentId(android.app.appsearch.GlobalSearchSession platformSession,
                String packageName, String databaseName,
                android.app.appsearch.GetByDocumentIdRequest request, Executor executor,
                BatchResultCallback<String, android.app.appsearch.GenericDocument> callback) {
            platformSession.getByDocumentId(packageName, databaseName, request, executor, callback);
        }

        @DoNotInline
        static void getSchema(android.app.appsearch.GlobalSearchSession platformSessions,
                String packageName, String databaseName, Executor executor,
                Consumer<AppSearchResult<android.app.appsearch.GetSchemaResponse>> callback) {
            platformSessions.getSchema(packageName, databaseName, executor, callback);
        }

        @DoNotInline
        static void registerObserverCallback(
                android.app.appsearch.GlobalSearchSession platformSession, String targetPackageName,
                android.app.appsearch.observer.ObserverSpec spec, Executor executor,
                android.app.appsearch.observer.ObserverCallback observer)
                throws android.app.appsearch.exceptions.AppSearchException {
            platformSession.registerObserverCallback(targetPackageName, spec, executor, observer);
        }

        @DoNotInline
        static void unregisterObserverCallback(
                android.app.appsearch.GlobalSearchSession platformSession, String targetPackageName,
                android.app.appsearch.observer.ObserverCallback observer)
                throws android.app.appsearch.exceptions.AppSearchException {
            platformSession.unregisterObserverCallback(targetPackageName, observer);
        }
    }
}
