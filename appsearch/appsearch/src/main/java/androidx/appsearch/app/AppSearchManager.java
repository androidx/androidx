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
import androidx.collection.ArraySet;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.StatusProto;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // Never call Executor.shutdownNow(), it will cancel the futures it's returned. And since
    // execute() won't return anything, we will hang forever waiting for the execution.
    private final ExecutorService mQueryExecutor = Executors.newCachedThreadPool();
    private final ExecutorService mMutateExecutor = Executors.newFixedThreadPool(1);

    /**
     * Encapsulates a request to update the schema of an {@link AppSearchManager} database.
     *
     * @see AppSearchManager#setSchema
     */
    public static final class SetSchemaRequest {
        final Set<AppSearchSchema> mSchemas;
        final boolean mForceOverride;

        SetSchemaRequest(Set<AppSearchSchema> schemas, boolean forceOverride) {
            mSchemas = schemas;
            mForceOverride = forceOverride;
        }

        /** Builder for {@link SetSchemaRequest} objects. */
        public static final class Builder {
            private final Set<AppSearchSchema> mSchemas = new ArraySet<>();
            private boolean mForceOverride = false;
            private boolean mBuilt = false;

            /** Adds one or more types to the schema. */
            @NonNull
            public Builder addSchema(@NonNull AppSearchSchema... schemas) {
                return addSchema(Arrays.asList(schemas));
            }

            /** Adds one or more types to the schema. */
            @NonNull
            public Builder addSchema(@NonNull Collection<AppSearchSchema> schemas) {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkNotNull(schemas);
                mSchemas.addAll(schemas);
                return this;
            }

            /**
             * Adds one or more types to the schema.
             *
             * @param dataClasses non-inner classes annotated with
             *     {@link androidx.appsearch.annotation.AppSearchDocument}.
             */
            @NonNull
            public Builder addDataClass(@NonNull Class<?>... dataClasses)
                    throws AppSearchException {
                return addDataClass(Arrays.asList(dataClasses));
            }

            /**
             * Adds one or more types to the schema.
             *
             * @param dataClasses non-inner classes annotated with
             *     {@link androidx.appsearch.annotation.AppSearchDocument}.
             */
            @NonNull
            public Builder addDataClass(@NonNull Collection<Class<?>> dataClasses)
                    throws AppSearchException {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkNotNull(dataClasses);
                List<AppSearchSchema> schemas = new ArrayList<>(dataClasses.size());
                DataClassFactoryRegistry registry = DataClassFactoryRegistry.getInstance();
                for (Class<?> dataClass : dataClasses) {
                    DataClassFactory<?> factory = registry.getOrCreateFactory(dataClass);
                    schemas.add(factory.getSchema());
                }
                return addSchema(schemas);
            }

            /**
             * Configures the {@link SetSchemaRequest} to delete any existing documents that don't
             * follow the new schema.
             *
             * <p>By default, this is {@code false} and schema incompatibility causes the
             * {@link #setSchema} call to fail.
             *
             * @see #setSchema
             */
            @NonNull
            public Builder setForceOverride(boolean forceOverride) {
                mForceOverride = forceOverride;
                return this;
            }

            /** Builds a new {@link SetSchemaRequest}. */
            @NonNull
            public SetSchemaRequest build() {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                mBuilt = true;
                return new SetSchemaRequest(mSchemas, mForceOverride);
            }
        }
    }

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
     * <p>Supplying a schema with such changes will, by default, result in this call returning an
     * {@link AppSearchResult} with a code of {@link AppSearchResult#RESULT_INVALID_SCHEMA} and an
     * error message describing the incompatibility. In this case the previously set schema will
     * remain active.
     *
     * <p>If you need to make non-backwards-compatible changes as described above, you can set the
     * {@link SetSchemaRequest.Builder#setForceOverride} method to {@code true}. In this case,
     * instead of returning an {@link AppSearchResult} with the
     * {@link AppSearchResult#RESULT_INVALID_SCHEMA} error code, all documents which are not
     * compatible with the new schema will be deleted and the incompatible schema will be applied.
     *
     * <p>It is a no-op to set the same schema as has been previously set; this is handled
     * efficiently.
     *
     * @param request The schema update request.
     * @return a ListenableFuture representing the pending result of performing this operation.
     */
    // TODO(b/143789408): Linkify #putDocuments after that API is made public
    @NonNull
    public ListenableFuture<AppSearchResult<Void>> setSchema(@NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(request);

        // Prepare the merged schema for transmission.
        return execute(mMutateExecutor, () -> {
            SchemaProto.Builder schemaProtoBuilder = SchemaProto.newBuilder();
            for (AppSearchSchema schema : request.mSchemas) {
                schemaProtoBuilder.addTypes(schema.getProto());
            }
            try {
                mAppSearchImpl.setSchema(schemaProtoBuilder.build(), request.mForceOverride);
                return AppSearchResult.newSuccessfulResult(/*value=*/ null);
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        });
    }

    /**
     * Encapsulates a request to index a document into an {@link AppSearchManager} database.
     *
     * @see AppSearchManager#putDocuments
     */
    public static final class PutDocumentsRequest {
        final List<GenericDocument> mDocuments;

        PutDocumentsRequest(List<GenericDocument> documents) {
            mDocuments = documents;
        }

        /** Builder for {@link PutDocumentsRequest} objects. */
        public static final class Builder {
            private final List<GenericDocument> mDocuments = new ArrayList<>();
            private boolean mBuilt = false;

            /** Adds one or more documents to the request. */
            @NonNull
            public Builder addGenericDocument(@NonNull GenericDocument... documents) {
                return addGenericDocument(Arrays.asList(documents));
            }

            /** Adds one or more documents to the request. */
            @NonNull
            public Builder addGenericDocument(@NonNull Collection<GenericDocument> documents) {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkNotNull(documents);
                mDocuments.addAll(documents);
                return this;
            }

            /**
             * Adds one or more documents to the request.
             *
             * @param dataClasses non-inner classes annotated with
             *     {@link androidx.appsearch.annotation.AppSearchDocument}.
             */
            @NonNull
            public Builder addDataClass(@NonNull Object... dataClasses) throws AppSearchException {
                return addDataClass(Arrays.asList(dataClasses));
            }

            /**
             * Adds one or more documents to the request.
             *
             * @param dataClasses non-inner classes annotated with
             *     {@link androidx.appsearch.annotation.AppSearchDocument}.
             */
            @NonNull
            public Builder addDataClass(@NonNull Collection<Object> dataClasses)
                    throws AppSearchException {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkNotNull(dataClasses);
                List<GenericDocument> genericDocuments = new ArrayList<>(dataClasses.size());
                for (Object dataClass : dataClasses) {
                    GenericDocument genericDocument = toGenericDocument(dataClass);
                    genericDocuments.add(genericDocument);
                }
                return addGenericDocument(genericDocuments);
            }

            @NonNull
            private static <T> GenericDocument toGenericDocument(@NonNull T dataClass)
                    throws AppSearchException {
                DataClassFactoryRegistry registry = DataClassFactoryRegistry.getInstance();
                DataClassFactory<T> factory = registry.getOrCreateFactory(dataClass);
                return factory.toGenericDocument(dataClass);
            }

            /** Builds a new {@link PutDocumentsRequest}. */
            @NonNull
            public PutDocumentsRequest build() {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                mBuilt = true;
                return new PutDocumentsRequest(mDocuments);
            }
        }
    }

    /**
     * Index {@link GenericDocument}s into AppSearch.
     *
     * <p>Each {@link GenericDocument}'s {@code schemaType} field must be set to the name of a
     * schema type previously registered via the {@link #setSchema} method.
     *
     * @param request {@link PutDocumentsRequest} containing documents to be indexed
     * @return A {@link ListenableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@code Void}&gt;&gt;. Where mapping the document URIs to {@link Void} if they were
     *     successfully indexed, or a {@link Throwable} describing the failure if they could not
     *     be indexed.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> putDocuments(
            @NonNull PutDocumentsRequest request) {
        // TODO(b/146386470): Transmit these documents as a RemoteStream instead of sending them in
        // one big list.
        return execute(mMutateExecutor, () -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < request.mDocuments.size(); i++) {
                GenericDocument document = request.mDocuments.get(i);
                try {
                    mAppSearchImpl.putDocument(document.getProto());
                    resultBuilder.setSuccess(document.getUri(), /*result=*/ null);
                } catch (Throwable t) {
                    resultBuilder.setResult(document.getUri(), throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        });
    }

    /**
     * Retrieves {@link GenericDocument}s by URI.
     *
     * @param namespace The namespace these documents reside in.
     * @param uris URIs of the documents to look up.
     * @return A {@link ListenableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link GenericDocument}&gt;&gt;.
     *     If the call fails to start, {@link ListenableFuture} will be completed exceptionally.
     *     Otherwise, {@link ListenableFuture} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link GenericDocument}&gt;
     *     mapping the document URIs to
     *     {@link GenericDocument} values if they were successfully retrieved, a {@code null}
     *     failure if they were not found, or a {@link Throwable} failure describing the problem
     *     if an error occurred.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getDocuments(
            @NonNull String namespace,
            @NonNull List<String> uris) {
        // TODO(b/146386470): Transmit the result documents as a RemoteStream instead of sending
        //     them in one big list.
        return execute(mQueryExecutor, () -> {
            AppSearchBatchResult.Builder<String, GenericDocument> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < uris.size(); i++) {
                String uri = uris.get(i);
                try {
                    DocumentProto documentProto = mAppSearchImpl.getDocument(namespace, uri);
                    if (documentProto == null) {
                        resultBuilder.setFailure(
                                uri, AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/ null);
                    } else {
                        try {
                            GenericDocument document = new GenericDocument(documentProto);
                            resultBuilder.setSuccess(uri, document);
                        } catch (Throwable t) {
                            // These documents went through validation, so how could this fail?
                            // We must have done something wrong.
                            resultBuilder.setFailure(
                                    uri, AppSearchResult.RESULT_INTERNAL_ERROR, t.getMessage());
                        }
                    }
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        });
    }

    /**
     * Retrieves {@link GenericDocument}s by URI in default namespace.
     *
     * @param uris URIs of the documents to look up.
     * @return A {@link ListenableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link GenericDocument}&gt;&gt;.
     *     If the call fails to start, {@link ListenableFuture} will be completed exceptionally.
     *     Otherwise, {@link ListenableFuture} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link GenericDocument}&gt;
     *     mapping the document URIs to
     *     {@link GenericDocument} values if they were successfully retrieved, a {@code null}
     *     failure if they were not found, or a {@link Throwable} failure describing the problem
     *     if an error occurred.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getDocuments(
            @NonNull List<String> uris) {
        return getDocuments(GenericDocument.DEFAULT_NAMESPACE, uris);
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
     * @return  A {@link ListenableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link SearchResults}&gt;&gt;.
     *     If the call fails to start, {@link ListenableFuture} will be completed exceptionally.
     *     Otherwise, {@link ListenableFuture} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link SearchResults}&gt;
     *     where the keys are document URIs, and the values are serialized Document protos.
     */
    @NonNull
    public ListenableFuture<AppSearchResult<SearchResults>> query(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        // TODO(b/146386470): Transmit the result documents as a RemoteStream instead of sending
        //     them in one big list.
        return execute(mQueryExecutor, () -> {
            try {
                SearchSpecProto searchSpecProto = searchSpec.getSearchSpecProto();
                searchSpecProto = searchSpecProto.toBuilder().setQuery(queryExpression).build();
                SearchResultProto searchResultProto = mAppSearchImpl.query(searchSpecProto,
                        searchSpec.getResultSpecProto(), searchSpec.getScoringSpecProto());
                // TODO(sidchhabra): Translate SearchResultProto errors into error codes. This
                //  might better be done in AppSearchImpl by throwing an AppSearchException.
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
        });
    }

    /**
     * Deletes {@link GenericDocument}s by URI.
     *
     * <p>You should not call this method directly; instead, use the {@code AppSearch#delete()} API
     * provided by JetPack.
     *
     * @param namespace The namespace these documents reside in.
     * @param uris URIs of the documents to delete
     * @return A {@link ListenableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link Void}&gt;&gt;. If the call fails to start, {@link ListenableFuture} will be
     *     completed exceptionally.Otherwise, {@link ListenableFuture} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are schema types. If a schema type doesn't exist, it will be reported as a
     *     failure where the {@code throwable} is {@code null}.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> delete(@NonNull String namespace,
            @NonNull List<String> uris) {
        return execute(mMutateExecutor, () -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < uris.size(); i++) {
                String uri = uris.get(i);
                try {
                    if (!mAppSearchImpl.delete(namespace, uri)) {
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
        });
    }

    /**
     * Deletes {@link GenericDocument}s by URI in default namespace.
     *
     * <p>You should not call this method directly; instead, use the {@code AppSearch#delete()} API
     * provided by JetPack.
     *
     * @param uris URIs of the documents to delete
     * @return A {@link ListenableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link Void}&gt;&gt;. If the call fails to start, {@link ListenableFuture} will be
     *     completed exceptionally.Otherwise, {@link ListenableFuture} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are schema types. If a schema type doesn't exist, it will be reported as a
     *     failure where the {@code throwable} is {@code null}.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> delete(@NonNull List<String> uris) {
        return delete(GenericDocument.DEFAULT_NAMESPACE, uris);
    }

    //TODO(b/153118598): Implement deleteByNamespace after we port to real icing.

    /**
     * Deletes {@link GenericDocument}s by schema type.
     *
     * @param schemaTypes Schema types whose documents to delete.
     * @return A {@link ListenableFuture}&lt;{@link AppSearchBatchResult}&lt;{@link String},
     *     {@link Void}&gt;&gt;.
     *     If the call fails to start, {@link ListenableFuture} will be completed exceptionally.
     *     Otherwise, {@link ListenableFuture} will be completed with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are schema types. If a schema type doesn't exist, it will be reported as a
     *     failure where the {@code throwable} is {@code null}.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> deleteByTypes(
            @NonNull List<String> schemaTypes) {
        return execute(mMutateExecutor, () -> {
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
        });
    }

    /**
     * Deletes all documents owned by the calling app.
     *
     * @return A {@link ListenableFuture}&lt;{@link AppSearchResult}&lt;{@link Void}&gt;&gt;.
     *     Will be completed with the result of the call.
     */
    @NonNull
    public <ValueType> ListenableFuture<AppSearchResult<ValueType>> deleteAll() {
        return execute(mMutateExecutor, () -> {
            try {
                mAppSearchImpl.deleteAll();
                return AppSearchResult.newSuccessfulResult(null);
            } catch (Throwable t) {
                return throwableToFailedResult(t);
            }
        });
    }

    /** Executes the callable task on the given executor. */
    private <T> ListenableFuture<T> execute(ExecutorService executor, Callable<T> callable) {
        ResolvableFuture<T> future = ResolvableFuture.create();
        executor.execute(() -> {
            if (!future.isCancelled()) {
                try {
                    future.set(callable.call());
                } catch (Throwable t) {
                    future.setException(t);
                }
            }
        });
        return future;
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
