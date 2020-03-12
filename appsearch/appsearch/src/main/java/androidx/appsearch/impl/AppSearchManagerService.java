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
package androidx.appsearch.impl;

import android.content.Context;
import android.os.Binder;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.StatusProto;

import java.io.IOException;
import java.util.List;

/**
 * TODO(b/142567528): add comments when implement this class
 * @hide
 */
// TODO(b/149787478): Merge this class into AppSearch/AppSearchImpl
// TODO(b/149787478): Remove all binder stuff.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppSearchManagerService {
    private final AppSearchImpl mAppSearchImpl;

    public AppSearchManagerService(@NonNull Context context) {
        mAppSearchImpl = new AppSearchImpl(context);
    }

    /**
     * Sets the schema.
     *
     * @param schemaBytes Serialized SchemaProto.
     * @param forceOverride Whether to apply the new schema even if it is incompatible. All
     *     incompatible documents will be deleted.
     * @param callback {@link ResolvableFuture}&lt;{@link AppSearchResult}&lt;{@link Void}&gt&gt;.
     *     The results of the call.
     */
    public void setSchema(
            @NonNull byte[] schemaBytes,
            boolean forceOverride,
            @NonNull ResolvableFuture<AppSearchResult<Void>> callback) {
        Preconditions.checkNotNull(schemaBytes);
        Preconditions.checkNotNull(callback);
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            SchemaProto schema = SchemaProto.parseFrom(schemaBytes);
            mAppSearchImpl.setSchema(callingUid, schema, forceOverride);
            callback.set(AppSearchResult.newSuccessfulResult(/*value=*/ null));
        } catch (Throwable t) {
            callback.set(throwableToFailedResult(t));
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /**
     * Inserts documents into the index.
     *
     * @param documentsBytes {@link List}&lt;byte[]&gt; of serialized DocumentProtos.
     * @param callback
     *     {@link ResolvableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link Void}&gt;&gt;.
     *     If the call fails to start, {@code callback} will be completed exceptionally. Otherwise,
     *     {@code callback} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are document URIs, and the values are {@code null}.
     */
    public void putDocuments(
            @NonNull List<byte[]> documentsBytes,
            @NonNull ResolvableFuture<AppSearchBatchResult<String, Void>> callback) {
        Preconditions.checkNotNull(documentsBytes);
        Preconditions.checkNotNull(callback);
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < documentsBytes.size(); i++) {
                byte[] documentBytes = (byte[]) documentsBytes.get(i);
                DocumentProto document = DocumentProto.parseFrom(documentBytes);
                try {
                    mAppSearchImpl.putDocument(callingUid, document);
                    resultBuilder.setSuccess(document.getUri(), /*result=*/ null);
                } catch (Throwable t) {
                    resultBuilder.setResult(document.getUri(), throwableToFailedResult(t));
                }
            }
            callback.set(resultBuilder.build());
        } catch (Throwable t) {
            callback.setException(t);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /**
     * Retrieves documents from the index.
     *
     * @param uris The URIs of the documents to retrieve
     * @param callback
     *     {@link ResolvableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link byte[]}&gt;&gt;.
     *     If the call fails to start, {@code callback} will be completed exceptionally. Otherwise,
     *     {@code callback} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link byte[]}&gt;
     *     where the keys are document URIs, and the values are serialized Document protos.
     */
    public void getDocuments(
            @NonNull List<String> uris,
            @NonNull ResolvableFuture<AppSearchBatchResult<String, byte[]>> callback) {
        Preconditions.checkNotNull(uris);
        Preconditions.checkNotNull(callback);
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            AppSearchBatchResult.Builder<String, byte[]> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < uris.size(); i++) {
                String uri = uris.get(i);
                try {
                    DocumentProto document = mAppSearchImpl.getDocument(callingUid, uri);
                    if (document == null) {
                        resultBuilder.setFailure(
                                uri, AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/ null);
                    } else {
                        resultBuilder.setSuccess(uri, document.toByteArray());
                    }
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            callback.set(resultBuilder.build());
        } catch (Throwable t) {
            callback.setException(t);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /**
     * Searches a document based on a given specifications.
     *
     * @param searchSpecBytes Serialized SearchSpecProto.
     * @param resultSpecBytes Serialized SearchResultsProto.
     * @param scoringSpecBytes Serialized ScoringSpecProto.
     * @param callback {@link ResolvableFuture}&lt;{@link AppSearchResult}&lt;{@link byte[]}&gt;&gt;
     *     Will be completed with a serialized {@link SearchResultsProto}.
     */
    // TODO(sidchhabra): Do this in a threadpool.
    public void query(
            @NonNull byte[] searchSpecBytes,
            @NonNull byte[] resultSpecBytes,
            @NonNull byte[] scoringSpecBytes,
            @NonNull ResolvableFuture<AppSearchResult<byte[]>> callback) {
        Preconditions.checkNotNull(searchSpecBytes);
        Preconditions.checkNotNull(resultSpecBytes);
        Preconditions.checkNotNull(scoringSpecBytes);
        Preconditions.checkNotNull(callback);
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            SearchSpecProto searchSpecProto = SearchSpecProto.parseFrom(searchSpecBytes);
            ResultSpecProto resultSpecProto = ResultSpecProto.parseFrom(resultSpecBytes);
            ScoringSpecProto scoringSpecProto = ScoringSpecProto.parseFrom(scoringSpecBytes);
            SearchResultProto searchResultProto = mAppSearchImpl.query(callingUid,
                    searchSpecProto, resultSpecProto, scoringSpecProto);
            // TODO(sidchhabra): Translate SearchResultProto errors into error codes. This might
            //     better be done in AppSearchImpl by throwing an AppSearchException.
            if (searchResultProto.getStatus().getCode() != StatusProto.Code.OK) {
                callback.set(
                        AppSearchResult.newFailedResult(
                                AppSearchResult.RESULT_INTERNAL_ERROR,
                                searchResultProto.getStatus().getMessage()));
            } else {
                callback.set(
                        AppSearchResult.newSuccessfulResult(searchResultProto.toByteArray()));
            }
        } catch (Throwable t) {
            callback.set(throwableToFailedResult(t));
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /**
     * Deletes documents by URI.
     *
     * @param uris The URIs of the documents to delete
     * @param callback
     *     {@link ResolvableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link Void}&gt;&gt;.
     *     If the call fails to start, {@code callback} will be completed exceptionally. Otherwise,
     *     {@code callback} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are document URIs. If a document doesn't exist, it will be reported as a
     *     failure where the {@code throwable} is {@code null}.
     */
    public void delete(@NonNull List<String> uris,
            @NonNull ResolvableFuture<AppSearchBatchResult<String, Void>> callback) {
        Preconditions.checkNotNull(uris);
        Preconditions.checkNotNull(callback);
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < uris.size(); i++) {
                String uri = uris.get(i);
                try {
                    if (!mAppSearchImpl.delete(callingUid, uri)) {
                        resultBuilder.setFailure(
                                uri, AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/ null);
                    } else {
                        resultBuilder.setSuccess(uri, /*result=*/null);
                    }
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            callback.set(resultBuilder.build());
        } catch (Throwable t) {
            callback.setException(t);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /**
     * Deletes documents by schema type.
     *
     * @param schemaTypes The schema types of the documents to delete
     * @param callback
     *     {@link ResolvableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link Void}&gt;&gt;.
     *     If the call fails to start, {@code callback} will be completed exceptionally. Otherwise,
     *     {@code callback} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are schema types. If a schema type doesn't exist, it will be reported as a
     *     failure where the {@code throwable} is {@code null}.
     */
    public void deleteByTypes(
            @NonNull List<String> schemaTypes,
            @NonNull ResolvableFuture<AppSearchBatchResult<String, Void>> callback) {
        Preconditions.checkNotNull(schemaTypes);
        Preconditions.checkNotNull(callback);
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < schemaTypes.size(); i++) {
                String schemaType = schemaTypes.get(i);
                try {
                    if (!mAppSearchImpl.deleteByType(callingUid, schemaType)) {
                        resultBuilder.setFailure(
                                schemaType,
                                AppSearchResult.RESULT_NOT_FOUND,
                                /*errorMessage=*/ null);
                    } else {
                        resultBuilder.setSuccess(schemaType, /*result=*/null);
                    }
                } catch (Throwable t) {
                    resultBuilder.setResult(schemaType, throwableToFailedResult(t));
                }
            }
            callback.set(resultBuilder.build());
        } catch (Throwable t) {
            callback.setException(t);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /**
     * Deletes all documents belonging to the calling app.
     *
     * @param callback {@link ResolvableFuture}&lt;{@link AppSearchResult}&lt;{@link Void}&gt;&gt;.
     *     Will be completed with the result of the call.
     */
    public <ValueType> void deleteAll(@NonNull
            ResolvableFuture<AppSearchResult<ValueType>> callback) {
        Preconditions.checkNotNull(callback);
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            mAppSearchImpl.deleteAll(callingUid);
            callback.set(AppSearchResult.newSuccessfulResult(null));
        } catch (Throwable t) {
            callback.set(throwableToFailedResult(t));
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private <ValueType> AppSearchResult<ValueType> throwableToFailedResult(
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
        return AppSearchResult.newFailedResult(resultCode, t.getMessage());
    }
}
