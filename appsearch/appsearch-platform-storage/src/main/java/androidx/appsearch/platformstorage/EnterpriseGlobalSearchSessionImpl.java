/*
 * Copyright 2024 The Android Open Source Project
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

import static androidx.appsearch.app.AppSearchResult.RESULT_NOT_FOUND;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.EnterpriseGlobalSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.platformstorage.converter.AppSearchResultToPlatformConverter;
import androidx.appsearch.platformstorage.converter.GenericDocumentToPlatformConverter;
import androidx.appsearch.platformstorage.converter.GetSchemaResponseToPlatformConverter;
import androidx.appsearch.platformstorage.converter.RequestToPlatformConverter;
import androidx.appsearch.platformstorage.converter.SearchSpecToPlatformConverter;
import androidx.appsearch.platformstorage.util.BatchResultCallbackAdapter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * An implementation of {@link EnterpriseGlobalSearchSession} which proxies to a
 * platform {@link android.app.appsearch.EnterpriseGlobalSearchSession}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class EnterpriseGlobalSearchSessionImpl implements EnterpriseGlobalSearchSession {
    private final android.app.appsearch.EnterpriseGlobalSearchSession mPlatformSession;
    private final Executor mExecutor;
    private final Features mFeatures;

    EnterpriseGlobalSearchSessionImpl(
            @NonNull android.app.appsearch.EnterpriseGlobalSearchSession platformSession,
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
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        ResolvableFuture<AppSearchBatchResult<String, GenericDocument>> future =
                ResolvableFuture.create();
        mPlatformSession.getByDocumentId(packageName, databaseName,
                RequestToPlatformConverter.toPlatformGetByDocumentIdRequest(request), mExecutor,
                new BatchResultCallbackAdapter<>(future,
                        GenericDocumentToPlatformConverter::toJetpackGenericDocument,
                        result -> {
                            // Due to b/349805579, when the enterprise user is missing, the batch
                            // result returned may be empty when it should instead contain NOT_FOUND
                            // errors. When this happens, we populate the batch result with
                            // NOT_FOUND errors before returning it to the caller.
                            if (!request.getIds().isEmpty() && result.getAll().isEmpty()) {
                                AppSearchBatchResult.Builder<String, GenericDocument>
                                        resultBuilder =
                                        new AppSearchBatchResult.Builder<>();
                                String namespace = request.getNamespace();
                                for (String id : request.getIds()) {
                                    resultBuilder.setFailure(id, RESULT_NOT_FOUND,
                                            "Document (" + namespace + ", " + id + ") not found.");
                                }
                                result = resultBuilder.build();
                            }
                            return result;
                        }));
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
    public ListenableFuture<GetSchemaResponse> getSchemaAsync(@NonNull String packageName,
            @NonNull String databaseName) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        ResolvableFuture<GetSchemaResponse> future = ResolvableFuture.create();
        mPlatformSession.getSchema(packageName, databaseName, mExecutor,
                result -> AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(result,
                        future, GetSchemaResponseToPlatformConverter::toJetpackGetSchemaResponse));
        return future;
    }

    @NonNull
    @Override
    public Features getFeatures() {
        return mFeatures;
    }
}
