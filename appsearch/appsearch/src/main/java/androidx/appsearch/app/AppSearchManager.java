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

package androidx.appsearch.app;

import static androidx.appsearch.app.AppSearchResult.newFailedResult;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.impl.AppSearchImpl;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.StatusProto;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class provides access to the centralized AppSearch index maintained by the system.
 *
 * <p>Apps can index structured text documents with AppSearch, which can then be retrieved through
 * the query API.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(b/148046169): This class header needs a detailed example/tutorial.
// TODO(b/149787478): Rename this class to AppSearch.
public class AppSearchManager {

    private final AppSearchImpl mAppSearchImpl = new AppSearchImpl();
    private final ExecutorService mQueryExecutor = Executors.newCachedThreadPool();
    private final ExecutorService mMutateExecutor = Executors.newFixedThreadPool(1);
    /**
     * Sets the schema being used by documents provided to the #putDocuments method.
     *
     * <p>The schema provided here is compared to the stored copy of the schema previously supplied
     * to {@link #setSchema}, if any, to determine how to treat existing documents. The following
     * types of schema modifications are always safe and are made without deleting any existing
     * documents:
     * <ul>
     *     <li>Addition of new types
     *     <li>Addition of new
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_OPTIONAL OPTIONAL} or
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_REPEATED REPEATED} properties to a
     *         type
     *     <li>Changing the cardinality of a data type to be less restrictive (e.g. changing an
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_OPTIONAL OPTIONAL} property into a
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_REPEATED REPEATED} property.
     * </ul>
     *
     * <p>The following types of schema changes are not backwards-compatible:
     * <ul>
     *     <li>Removal of an existing type
     *     <li>Removal of a property from a type
     *     <li>Changing the data type ({@code boolean}, {@code long}, etc.) of an existing property
     *     <li>For properties of {@code Document} type, changing the schema type of
     *         {@code Document}s of that property
     *     <li>Changing the cardinality of a data type to be more restrictive (e.g. changing an
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_OPTIONAL OPTIONAL} property into a
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_REQUIRED REQUIRED} property).
     *     <li>Adding a
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_REQUIRED REQUIRED} property.
     * </ul>
     * <p>Supplying a schema with such changes will result in this call returning an
     * {@link AppSearchResult} with a code of {@link AppSearchResult#RESULT_INVALID_SCHEMA} and an
     * error message describing the incompatibility. In this case the previously set schema will
     * remain active.
     *
     * <p>If you need to make non-backwards-compatible changes as described above, instead use the
     * {@link #setSchema(List, boolean)} method with the {@code forceOverride} parameter set to
     * {@code true}.
     *
     * <p>It is a no-op to set the same schema as has been previously set; this is handled
     * efficiently.
     *
     * @param schemas The schema configs for the types used by the calling app.
     * @return a Future representing the pending result of performing this operation.
     */
    // TODO(b/143789408): Linkify #putDocuments after that API is made public
    @NonNull
    public Future<AppSearchResult<Void>> setSchema(@NonNull AppSearchSchema... schemas) {
        return setSchema(Arrays.asList(schemas), /*forceOverride=*/false);
    }

    /**
     * Sets the schema being used by documents provided to the #putDocuments method.
     *
     * <p>This method is similar to {@link #setSchema(AppSearchSchema...)}, except for the
     * {@code forceOverride} parameter. If a backwards-incompatible schema is specified but the
     * {@code forceOverride} parameter is set to {@code true}, instead of returning an
     * {@link AppSearchResult} with the {@link AppSearchResult#RESULT_INVALID_SCHEMA} code, all
     * documents which are not compatible with the new schema will be deleted and the incompatible
     * schema will be applied.
     *
     * @param schemas The schema configs for the types used by the calling app.
     * @param forceOverride Whether to force the new schema to be applied even if there are
     *     incompatible changes versus the previously set schema. Documents which are incompatible
     *     with the new schema will be deleted.
     * @return @return a Future representing the pending result of performing this operation.
     */
    @NonNull
    public Future<AppSearchResult<Void>> setSchema(
            @NonNull List<AppSearchSchema> schemas, boolean forceOverride) {
        // Prepare the merged schema for transmission.
        Callable<AppSearchResult<Void>> callableTask = () -> {
            SchemaProto.Builder schemaProtoBuilder = SchemaProto.newBuilder();
            for (AppSearchSchema schema : schemas) {
                schemaProtoBuilder.addTypes(schema.getProto());
            }
            try {
                mAppSearchImpl.setSchema(schemaProtoBuilder.build(), forceOverride);
                return AppSearchResult.newSuccessfulResult(/*value=*/ null);
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        };
        return mMutateExecutor.submit(callableTask);
    }

    /**
     * Index {@link GenericDocument}s into AppSearch.
     *
     * <p>You should not call this method directly; instead, use the
     * {@code AppSearch#putDocuments()} API provided by JetPack.
     *
     * <p>Each {@link GenericDocument}'s {@code schemaType} field must be set to the name of a
     * schema type previously registered via the {@link #setSchema} method.
     *
     * @param documents {@link GenericDocument}s that need to be indexed.
     * @return A {@link Future}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@code Void}&gt;&gt;. Where mapping the document URIs to {@link Void} if they were
     *     successfully indexed, or a {@link Throwable} describing the failure if they could not
     *     be indexed.
     */
    @NonNull
    public Future<AppSearchBatchResult<String, Void>> putDocuments(
            @NonNull List<GenericDocument> documents) {
        // TODO(b/146386470): Transmit these documents as a RemoteStream instead of sending them in
        // one big list.
        Callable<AppSearchBatchResult<String, Void>> callableTask = () -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < documents.size(); i++) {
                GenericDocument document = documents.get(i);
                try {
                    mAppSearchImpl.putDocument(document.getProto());
                    resultBuilder.setSuccess(document.getUri(), /*result=*/ null);
                } catch (Throwable t) {
                    resultBuilder.setResult(document.getUri(), throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        };
        return mMutateExecutor.submit(callableTask);
    }

    /**
     * Retrieves {@link GenericDocument}s by URI.
     *
     * <p>You should not call this method directly; instead, use the
     * {@code AppSearch#getDocuments()} API provided by JetPack.
     *
     * @param uris URIs of the documents to look up.
     * @return A {@link Future}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link GenericDocument}&gt;&gt;.
     *     If the call fails to start, {@link Future} will be completed exceptionally.
     *     Otherwise, {@link Future} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link GenericDocument}&gt;
     *     mapping the document URIs to
     *     {@link GenericDocument} values if they were successfully retrieved, a {@code null}
     *     failure if they were not found, or a {@link Throwable} failure describing the problem
     *     if an error occurred.
     */
    @NonNull
    public Future<AppSearchBatchResult<String, GenericDocument>> getDocuments(
            @NonNull List<String> uris) {
        // TODO(b/146386470): Transmit the result documents as a RemoteStream instead of sending
        //     them in one big list.
        Callable<AppSearchBatchResult<String, GenericDocument>> callableTask = () -> {
            AppSearchBatchResult.Builder<String, GenericDocument> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < uris.size(); i++) {
                String uri = uris.get(i);
                try {
                    DocumentProto documentProto = mAppSearchImpl.getDocument(uri);
                    if (documentProto == null) {
                        resultBuilder.setFailure(
                                uri, AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/ null);
                    } else {
                        try {
                            GenericDocument document = new GenericDocument(documentProto);
                            resultBuilder.setSuccess(uri, document);
                        } catch (Throwable t) {
                            // These documents went through validation, so how could this fail? We
                            // must have done something wrong.
                            resultBuilder.setFailure(
                                    uri, AppSearchResult.RESULT_INTERNAL_ERROR, t.getMessage());
                        }
                    }
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        };
        return mQueryExecutor.submit(callableTask);
    }

    /**
     * Searches a document based on a given query string.
     *
     * <p>You should not call this method directly; instead, use the {@code AppSearch#query()} API
     * provided by JetPack.
     *
     * <p>Currently we support following features in the raw query format:
     * <ul>
     *     <li>AND
     *     <p>AND joins (e.g. “match documents that have both the terms ‘dog’ and
     *     ‘cat’”).
     *     Example: hello world matches documents that have both ‘hello’ and ‘world’
     *     <li>OR
     *     <p>OR joins (e.g. “match documents that have either the term ‘dog’ or
     *     ‘cat’”).
     *     Example: dog OR puppy
     *     <li>Exclusion
     *     <p>Exclude a term (e.g. “match documents that do
     *     not have the term ‘dog’”).
     *     Example: -dog excludes the term ‘dog’
     *     <li>Grouping terms
     *     <p>Allow for conceptual grouping of subqueries to enable hierarchical structures (e.g.
     *     “match documents that have either ‘dog’ or ‘puppy’, and either ‘cat’ or ‘kitten’”).
     *     Example: (dog puppy) (cat kitten) two one group containing two terms.
     *     <li>Property restricts
     *     <p> Specifies which properties of a document to specifically match terms in (e.g.
     *     “match documents where the ‘subject’ property contains ‘important’”).
     *     Example: subject:important matches documents with the term ‘important’ in the
     *     ‘subject’ property
     *     <li>Schema type restricts
     *     <p>This is similar to property restricts, but allows for restricts on top-level document
     *     fields, such as schema_type. Clients should be able to limit their query to documents of
     *     a certain schema_type (e.g. “match documents that are of the ‘Email’ schema_type”).
     *     Example: { schema_type_filters: “Email”, “Video”,query: “dog” } will match documents
     *     that contain the query term ‘dog’ and are of either the ‘Email’ schema type or the
     *     ‘Video’ schema type.
     * </ul>
     *
     * <p> It is strongly recommended to use Jetpack APIs.
     *
     * @param queryExpression Query String to search.
     * @param searchSpec Spec for setting filters, raw query etc.
     * @return  A {@link Future}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link SearchResults}&gt;&gt;.
     *     If the call fails to start, {@link Future} will be completed exceptionally.
     *     Otherwise, {@link Future} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link SearchResults}&gt;
     *     where the keys are document URIs, and the values are serialized Document protos.
     */
    @NonNull
    public Future<AppSearchResult<SearchResults>> query(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        // TODO(b/146386470): Transmit the result documents as a RemoteStream instead of sending
        //     them in one big list.
        Callable<AppSearchResult<SearchResults>> callableTask = () -> {
            try {
                SearchSpecProto searchSpecProto = searchSpec.getSearchSpecProto();
                searchSpecProto = searchSpecProto.toBuilder().setQuery(queryExpression).build();
                SearchResultProto searchResultProto = mAppSearchImpl.query(searchSpecProto,
                        searchSpec.getResultSpecProto(), searchSpec.getScoringSpecProto());
                // TODO(sidchhabra): Translate SearchResultProto errors into error codes. This might
                //     better be done in AppSearchImpl by throwing an AppSearchException.
                if (searchResultProto.getStatus().getCode() != StatusProto.Code.OK) {
                    return AppSearchResult.newFailedResult(
                            AppSearchResult.RESULT_INTERNAL_ERROR,
                            searchResultProto.getStatus().getMessage());
                } else {
                    return AppSearchResult.newSuccessfulResult(
                            new SearchResults(searchResultProto));
                }
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        };
        return mQueryExecutor.submit(callableTask);
    }

    /**
     * Deletes {@link GenericDocument}s by URI.
     *
     * <p>You should not call this method directly; instead, use the {@code AppSearch#delete()} API
     * provided by JetPack.
     *
     * @param uris URIs of the documents to delete
     * @return A {@link Future}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link Void}&gt;&gt;. If the call fails to start, {@link Future} will be
     *     completed exceptionally.Otherwise, {@link Future} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are schema types. If a schema type doesn't exist, it will be reported as a
     *     failure where the {@code throwable} is {@code null}..
     */
    @NonNull
    public Future<AppSearchBatchResult<String, Void>> delete(@NonNull List<String> uris) {
        Callable<AppSearchBatchResult<String, Void>> callableTask = () -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < uris.size(); i++) {
                String uri = uris.get(i);
                try {
                    if (!mAppSearchImpl.delete(uri)) {
                        resultBuilder.setFailure(
                                uri, AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/ null);
                    } else {
                        resultBuilder.setSuccess(uri, /*result= */null);
                    }
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        };
        return mMutateExecutor.submit(callableTask);
    }

    /**
     * Deletes {@link GenericDocument}s by schema type.
     *
     * <p>You should not call this method directly; instead, use the
     * {@code AppSearch#deleteByType()} API provided by JetPack.
     *
     * @param schemaTypes Schema types whose documents to delete.
     * @return A {@link Future}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link Void}&gt;&gt;.
     *     If the call fails to start, {@link Future} will be completed exceptionally.
     *     Otherwise, {@link Future} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are schema types. If a schema type doesn't exist, it will be reported as a
     *     failure where the {@code throwable} is {@code null}.
     */
    @NonNull
    public Future<AppSearchBatchResult<String, Void>> deleteByTypes(
            @NonNull List<String> schemaTypes) {
        Callable<AppSearchBatchResult<String, Void>> callableTask = () -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < schemaTypes.size(); i++) {
                String schemaType = schemaTypes.get(i);
                try {
                    if (!mAppSearchImpl.deleteByType(schemaType)) {
                        resultBuilder.setFailure(
                                schemaType,
                                AppSearchResult.RESULT_NOT_FOUND,
                                /*errorMessage=*/ null);
                    } else {
                        resultBuilder.setSuccess(schemaType, /*result=*/ null);
                    }
                } catch (Throwable t) {
                    resultBuilder.setResult(schemaType, throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        };
        return mMutateExecutor.submit(callableTask);
    }

    /**
     * Deletes all documents owned by the calling app.
     *
     * @return A {@link Future}&lt;{@link AppSearchResult}&lt;{@link Void}&gt;&gt;.
     *     Will be completed with the result of the call.
     */
    @NonNull
    public <ValueType> Future<AppSearchResult<ValueType>> deleteAll() {
        Callable<AppSearchResult<ValueType>> callableTask = () -> {
            try {
                mAppSearchImpl.deleteAll();
                return AppSearchResult.newSuccessfulResult(null);
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        };
        return mMutateExecutor.submit(callableTask);
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
        return newFailedResult(resultCode, t.getMessage());
    }
}
