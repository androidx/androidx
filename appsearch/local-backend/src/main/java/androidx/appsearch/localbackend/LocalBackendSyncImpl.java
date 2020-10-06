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

package androidx.appsearch.localbackend;

import static androidx.appsearch.app.AppSearchResult.newFailedResult;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByUriRequest;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByUriRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localbackend.converter.GenericDocumentToProtoConverter;
import androidx.appsearch.localbackend.converter.SchemaToProtoConverter;
import androidx.appsearch.localbackend.converter.SearchResultToProtoConverter;
import androidx.appsearch.localbackend.converter.SearchSpecToProtoConverter;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Synchronous implementation of AppSearch functionality, with data stored locally in the app's
 * storage space using a bundled version of the search native library.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@WorkerThread
class LocalBackendSyncImpl {
    private static final String ICING_DIR = "icing";

    final AppSearchImpl mAppSearchImpl;

    public static AppSearchResult<LocalBackendSyncImpl> create(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        File icingDir = new File(context.getFilesDir(), ICING_DIR);
        AppSearchImpl appSearchImpl;
        try {
            appSearchImpl = AppSearchImpl.create(icingDir);
        } catch (Throwable t) {
            return throwableToFailedResult(t);
        }
        return AppSearchResult.newSuccessfulResult(new LocalBackendSyncImpl(appSearchImpl));
    }

    private LocalBackendSyncImpl(@NonNull AppSearchImpl appSearchImpl) {
        mAppSearchImpl = appSearchImpl;
    }

    @NonNull
    public AppSearchResult<Void> setSchema(
            @NonNull String databaseName, @NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        SchemaProto.Builder schemaProtoBuilder = SchemaProto.newBuilder();
        for (AppSearchSchema schema : request.getSchemas()) {
            SchemaTypeConfigProto schemaTypeProto = SchemaToProtoConverter.convert(schema);
            schemaProtoBuilder.addTypes(schemaTypeProto);
        }
        try {
            mAppSearchImpl.setSchema(
                    databaseName, schemaProtoBuilder.build(), request.isForceOverride());
            return AppSearchResult.newSuccessfulResult(/*value=*/ null);
        } catch (Throwable t) {
            return throwableToFailedResult(t);
        }
    }

    @NonNull
    public AppSearchBatchResult<String, Void> putDocuments(
            @NonNull String databaseName, @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        AppSearchBatchResult.Builder<String, Void> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        for (int i = 0; i < request.getDocuments().size(); i++) {
            GenericDocument document = request.getDocuments().get(i);
            try {
                DocumentProto documentProto = GenericDocumentToProtoConverter.convert(document);
                mAppSearchImpl.putDocument(databaseName, documentProto);
                resultBuilder.setSuccess(document.getUri(), /*result=*/ null);
            } catch (Throwable t) {
                resultBuilder.setResult(document.getUri(), throwableToFailedResult(t));
            }
        }
        return resultBuilder.build();
    }

    @NonNull
    public AppSearchBatchResult<String, GenericDocument> getByUri(
            @NonNull String databaseName, @NonNull GetByUriRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        AppSearchBatchResult.Builder<String, GenericDocument> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        for (String uri : request.getUris()) {
            try {
                DocumentProto documentProto =
                        mAppSearchImpl.getDocument(databaseName, request.getNamespace(), uri);
                try {
                    GenericDocument document =
                            GenericDocumentToProtoConverter.convert(documentProto);
                    resultBuilder.setSuccess(uri, document);
                } catch (Throwable t) {
                    // These documents went through validation, so how could this fail?
                    // We must have done something wrong.
                    resultBuilder.setFailure(
                            uri, AppSearchResult.RESULT_INTERNAL_ERROR, t.getMessage());
                }
            } catch (Throwable t) {
                resultBuilder.setResult(uri, throwableToFailedResult(t));
            }
        }
        return resultBuilder.build();
    }

    @NonNull
    public SearchResultsImpl query(
            @NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        return new SearchResultsImpl(databaseName, queryExpression, searchSpec);
    }

    @NonNull
    public AppSearchBatchResult<String, Void> removeByUri(
            @NonNull String databaseName,
            @NonNull RemoveByUriRequest request) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        AppSearchBatchResult.Builder<String, Void> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        for (String uri : request.getUris()) {
            try {
                mAppSearchImpl.remove(databaseName, request.getNamespace(), uri);
                resultBuilder.setSuccess(uri, /*result= */null);
            } catch (Throwable t) {
                resultBuilder.setResult(uri, throwableToFailedResult(t));
            }
        }
        return resultBuilder.build();
    }

    @NonNull
    public AppSearchBatchResult<String, Void> removeByType(
            @NonNull String databaseName, @NonNull List<String> schemaTypes) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(schemaTypes);
        AppSearchBatchResult.Builder<String, Void> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        for (int i = 0; i < schemaTypes.size(); i++) {
            String schemaType = schemaTypes.get(i);
            try {
                mAppSearchImpl.removeByType(databaseName, schemaType);
                resultBuilder.setSuccess(schemaType, /*result=*/ null);
            } catch (Throwable t) {
                resultBuilder.setResult(schemaType, throwableToFailedResult(t));
            }
        }
        return resultBuilder.build();
    }

    @NonNull
    public AppSearchBatchResult<String, Void> removeByNamespace(
            @NonNull String databaseName, @NonNull List<String> namespaces) {
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(namespaces);
        AppSearchBatchResult.Builder<String, Void> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        for (int i = 0; i < namespaces.size(); i++) {
            String namespace = namespaces.get(i);
            try {
                mAppSearchImpl.removeByNamespace(databaseName, namespace);
                resultBuilder.setSuccess(namespace, /*result=*/ null);
            } catch (Throwable t) {
                resultBuilder.setResult(namespace, throwableToFailedResult(t));
            }
        }
        return resultBuilder.build();
    }

    @NonNull
    public AppSearchResult<Void> removeAll(@NonNull String databaseName) {
        Preconditions.checkNotNull(databaseName);
        try {
            mAppSearchImpl.removeAll(databaseName);
            return AppSearchResult.newSuccessfulResult(null);
        } catch (Throwable t) {
            return throwableToFailedResult(t);
        }
    }

    @VisibleForTesting
    @NonNull
    public AppSearchResult<Void> resetAllDatabases() {
        try {
            mAppSearchImpl.reset();
            return AppSearchResult.newSuccessfulResult(null);
        } catch (Throwable t) {
            return throwableToFailedResult(t);
        }
    }

    @NonNull
    static <ValueType> AppSearchResult<ValueType> throwableToFailedResult(
            @NonNull Throwable t) {
        if (t instanceof AppSearchException) {
            return ((AppSearchException) t).toAppSearchResult();
        }

        @AppSearchResult.ResultCode int resultCode;
        if (t instanceof IllegalStateException) {
            resultCode = AppSearchResult.RESULT_INTERNAL_ERROR;
        } else if (t instanceof IllegalArgumentException) {
            resultCode = AppSearchResult.RESULT_INVALID_ARGUMENT;
        } else if (t instanceof IOException) {
            resultCode = AppSearchResult.RESULT_IO_ERROR;
        } else {
            resultCode = AppSearchResult.RESULT_UNKNOWN_ERROR;
        }
        return newFailedResult(resultCode, t.toString());
    }

    /**
     * Presents the search results in the app's locally storage space using a bundled version of the
     * search native library.
     */
    class SearchResultsImpl implements Closeable {
        private long mNextPageToken;
        private final String mDatabaseName;
        private final SearchSpec mSearchSpec;
        private final String mQueryExpression;
        private boolean mIsFirstLoad = true;

        SearchResultsImpl(@NonNull String databaseName,
                @NonNull String queryExpression,
                @NonNull SearchSpec searchSpec)  {
            Preconditions.checkNotNull(databaseName);
            Preconditions.checkNotNull(queryExpression);
            Preconditions.checkNotNull(searchSpec);
            mDatabaseName = databaseName;
            mQueryExpression = queryExpression;
            mSearchSpec = searchSpec;
        }

        @NonNull
        public AppSearchResult<List<SearchResults.Result>> getNextPage() {
            try {
                if (mIsFirstLoad) {
                    mIsFirstLoad = false;
                    SearchSpecProto searchSpecProto =
                            SearchSpecToProtoConverter.toSearchSpecProto(mSearchSpec);
                    searchSpecProto = searchSpecProto.toBuilder()
                            .setQuery(mQueryExpression).build();
                    SearchResultProto searchResultProto = mAppSearchImpl.query(
                            mDatabaseName,
                            searchSpecProto,
                            SearchSpecToProtoConverter.toResultSpecProto(mSearchSpec),
                            SearchSpecToProtoConverter.toScoringSpecProto(mSearchSpec));
                    mNextPageToken = searchResultProto.getNextPageToken();
                    return AppSearchResult.newSuccessfulResult(
                            SearchResultToProtoConverter.convert(searchResultProto));
                } else {
                    SearchResultProto searchResultProto = mAppSearchImpl.getNextPage(mDatabaseName,
                            mNextPageToken);
                    mNextPageToken = searchResultProto.getNextPageToken();
                    return AppSearchResult.newSuccessfulResult(
                            SearchResultToProtoConverter.convert(searchResultProto));
                }
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        }

        @Override
        public void close() {
            mAppSearchImpl.invalidateNextPageToken(mNextPageToken);
        }
    }
}
