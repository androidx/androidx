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
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.exceptions.AppSearchException;

import com.google.android.icing.IcingSearchEngine;
import com.google.android.icing.proto.DeleteByNamespaceResultProto;
import com.google.android.icing.proto.DeleteBySchemaTypeResultProto;
import com.google.android.icing.proto.DeleteResultProto;
import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.GetOptimizeInfoResultProto;
import com.google.android.icing.proto.GetResultProto;
import com.google.android.icing.proto.GetSchemaResultProto;
import com.google.android.icing.proto.IcingSearchEngineOptions;
import com.google.android.icing.proto.InitializeResultProto;
import com.google.android.icing.proto.OptimizeResultProto;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.PropertyProto;
import com.google.android.icing.proto.PutResultProto;
import com.google.android.icing.proto.ResetResultProto;
import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.SetSchemaResultProto;
import com.google.android.icing.proto.StatusProto;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Manages interaction with the native IcingSearchEngine and other components to implement AppSearch
 * functionality.
 *
 * <p>Callers should use {@link #getInstance(Context)} to retrieve the singleton instance and call
 * {@link #initialize()} before using the class.
 *
 * <p>The singleton instance of {@link AppSearchImpl} supports all instances of
 * {@link androidx.appsearch.app.AppSearchManager} with different database name. All logically
 * isolated schemas and documents will be physically saved together in IcingSearchEngine.
 * The way to isolated those schemas and documents for different database:
 * <ul>
 *      <li>Rewrite SchemaType in SchemaProto by adding database name prefix and save into
 *          SchemaTypes set in {@link #setSchema(String, SchemaProto, boolean)}.
 *      <li>Rewrite namespace and SchemaType in DocumentProto by adding database name prefix and
 *          save to namespaces set in {@link #putDocument(String, DocumentProto)}.
 *      <li>Remove database name prefix when retrieve documents in
 *          {@link #getDocument(String, String, String)}, and
 *          {@link #query(String, SearchSpecProto, ResultSpecProto, ScoringSpecProto)}.
 *      <li>Rewrite filters in {@link SearchSpecProto} to have all namespaces and schema types of
 *          the queried database when user using empty filters in
 *          {@link #query(String, SearchSpecProto, ResultSpecProto, ScoringSpecProto)}.
 * </ul>
 *
 * <p>Methods in this class belong to two groups, the query group and the mutate group.
 * <ul>
 *     <li>All methods are going to modify global parameters and data in Icing should be executed
 *     under WRITE lock to keep thread safety.
 *     <li>All methods are going to access global parameters or query data from Icing
 *     should be executed under READ lock to improve query performance.
 * </ul>
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppSearchImpl {
    private static volatile AppSearchImpl sInstance;
    private static final String TAG = "AppSearchImpl";
    private static final String SHARED_PREFERENCES_NAME = "androidx.appsearch";
    private static final String NAMESPACE_SET_NAME = "namespace-set";
    private static final String SCHEMA_TYPE_SET_NAME = "schema-type-set";
    private static final String ICING_DIR = "/icing";
    @VisibleForTesting
    static final int OPTIMIZE_THRESHOLD_DOC_COUNT = 1000;
    @VisibleForTesting
    static final int OPTIMIZE_THRESHOLD_BYTES = 1_000_000; // 1MB
    @VisibleForTesting
    static final int CHECK_OPTIMIZE_INTERVAL = 100;
    // TODO(b/158350212) Remove SharedPreferences once getAllNamespace() is ready in Icing lib.
    // SharedPreferences is discouraged to be used in go/sharedpreferences.
    private final SharedPreferences mSharedPreferences;
    private final ReentrantReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();
    private final CountDownLatch mInitCompleteLatch = new CountDownLatch(1);
    private volatile IcingSearchEngine mIcingSearchEngine;
    private volatile boolean mInitialized = false;

    /**
     * The counter to check when to call {@link #checkForOptimize(boolean)}. The interval is
     * {@link #CHECK_OPTIMIZE_INTERVAL}.
     */
    private int mOptimizeIntervalCount = 0;

    /** Gets the singleton instance of {@link AppSearchImpl} */
    @NonNull
    public static AppSearchImpl getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (AppSearchImpl.class) {
                if (sInstance == null) {
                    sInstance = new AppSearchImpl(context);
                }
            }
        }
        return sInstance;
    }

    private AppSearchImpl(@NonNull Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

    /**
     * Initializes the underlying IcingSearchEngine.
     *
     * <p>This method belongs to mutate group.
     *
     * @throws IOException        on error opening directory.
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public void initialize(@NonNull Context context) throws IOException, AppSearchException {
        if (isInitialized()) {
            return;
        }
        boolean isReset = false;
        mReadWriteLock.writeLock().lock();
        try {
        // We synchronize here because we don't want to call IcingSearchEngine.initialize() more
        // than once. It's unnecessary and can be a costly operation.
            if (isInitialized()) {
                return;
            }

            IcingSearchEngineOptions options = IcingSearchEngineOptions.newBuilder().setBaseDir(
                    context.getFilesDir().getCanonicalPath() + ICING_DIR).build();
            mIcingSearchEngine = new IcingSearchEngine(options);

            InitializeResultProto initializeResultProto = mIcingSearchEngine.initialize();
            try {
                checkSuccess(initializeResultProto.getStatus());
            } catch (AppSearchException e) {
                // Some error. Reset and see if it fixes it.
                reset();
                isReset = true;
            }
            mInitialized = true;
            mInitCompleteLatch.countDown();
            if (!isReset) {
                checkForOptimize(/* force= */ true);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /** Checks if the internal state of {@link AppSearchImpl} has been initialized. */
    public boolean isInitialized() {
        return mInitialized;
    }

    /**
     * Updates the AppSearch schema for this app.
     *
     * <p>This method belongs to mutate group.
     *
     * @param databaseName  The name of the database where this schema lives.
     * @param origSchema    The schema to set for this app.
     * @param forceOverride Whether to force-apply the schema even if it is incompatible. Documents
     *                      which do not comply with the new schema will be deleted.
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws InterruptedException if the current thread was interrupted during execution.
     */
    public void setSchema(@NonNull String databaseName, @NonNull SchemaProto origSchema,
            boolean forceOverride) throws AppSearchException, InterruptedException {
        checkInitialized();

        GetSchemaResultProto getSchemaResultProto = mIcingSearchEngine.getSchema();
        SchemaProto.Builder existingSchemaBuilder = getSchemaResultProto.getSchema().toBuilder();

        // Combine the existing schema (which may have types from other databases) with this
        // database's new schema. Modifies the existingSchemaBuilder.
        rewriteSchema(existingSchemaBuilder, getDatabasePrefix(databaseName), origSchema);

        SetSchemaResultProto setSchemaResultProto;
        mReadWriteLock.writeLock().lock();
        try {
            setSchemaResultProto = mIcingSearchEngine.setSchema(existingSchemaBuilder.build(),
                    forceOverride);
            for (SchemaTypeConfigProto typeConfig : origSchema.getTypesList()) {
                addToSharedSet(databaseName, SCHEMA_TYPE_SET_NAME, typeConfig.getSchemaType());
            }
            if (setSchemaResultProto.getDeletedSchemaTypesCount() > 0
                    || (setSchemaResultProto.getIncompatibleSchemaTypesCount() > 0
                    && forceOverride)) {
                // Any existing schemas which is not in origSchema will be deleted, and all
                // documents of these types were also deleted. And so well if we force override
                // incompatible schemas.
                checkForOptimize(/* force= */true);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
        checkSuccess(setSchemaResultProto.getStatus());
    }

    /**
     * Adds a document to the AppSearch index.
     *
     * <p>This method belongs to mutate group.
     *
     * @param databaseName The databaseName this document resides in.
     * @param document     The document to index.
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws InterruptedException if the current thread was interrupted during execution.
     */
    public void putDocument(@NonNull String databaseName, @NonNull DocumentProto document)
            throws AppSearchException, InterruptedException {
        checkInitialized();

        DocumentProto.Builder documentBuilder = document.toBuilder();
        rewriteDocumentTypes(getDatabasePrefix(databaseName), documentBuilder, /*add=*/ true);

        PutResultProto putResultProto;
        mReadWriteLock.writeLock().lock();
        try {
            putResultProto = mIcingSearchEngine.put(documentBuilder.build());
            addToSharedSet(databaseName, NAMESPACE_SET_NAME, document.getNamespace());
            // The existing documents with same URI will be deleted, so there maybe some resources
            // could be released after optimize().
            checkForOptimize(/* force= */false);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
        checkSuccess(putResultProto.getStatus());
    }

    /**
     * Retrieves a document from the AppSearch index by URI.
     *
     * <p>This method belongs to query group.
     *
     * @param databaseName The databaseName this document resides in.
     * @param namespace    The namespace this document resides in.
     * @param uri          The URI of the document to get.
     * @return The Document contents, or {@code null} if no such URI exists in the system.
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws InterruptedException if the current thread was interrupted during execution.
     */
    @Nullable
    public DocumentProto getDocument(@NonNull String databaseName, @NonNull String namespace,
            @NonNull String uri) throws AppSearchException, InterruptedException {
        checkInitialized();
        GetResultProto getResultProto;
        mReadWriteLock.readLock().lock();
        try {
            getResultProto = mIcingSearchEngine.get(
                    getDatabasePrefix(databaseName) + namespace, uri);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
        checkSuccess(getResultProto.getStatus());

        DocumentProto.Builder documentBuilder = getResultProto.getDocument().toBuilder();
        rewriteDocumentTypes(getDatabasePrefix(databaseName), documentBuilder, /*add=*/ false);
        return documentBuilder.build();
    }

    /**
     * Executes a query against the AppSearch index and returns results.
     *
     * <p>This method belongs to query group.
     *
     * @param databaseName The databaseName this query for.
     * @param searchSpec   Defines what and how to search
     * @param resultSpec   Defines what results to show
     * @param scoringSpec  Defines how to order results
     * @return The results of performing this search  The proto might have no {@code results} if no
     * documents matched the query.
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws InterruptedException if the current thread was interrupted during execution.
     */
    // TODO(b/161838267) support getNextPage for query, under the read lock.
    @NonNull
    public SearchResultProto query(
            @NonNull String databaseName,
            @NonNull SearchSpecProto searchSpec,
            @NonNull ResultSpecProto resultSpec,
            @NonNull ScoringSpecProto scoringSpec) throws AppSearchException, InterruptedException {
        checkInitialized();

        SearchSpecProto.Builder searchSpecBuilder = searchSpec.toBuilder();
        SearchResultProto searchResultProto;
        mReadWriteLock.readLock().lock();
        try {
            // Rewrite any schema types specified in the searchSpec, or add schema types to limit
            // the search to this database instance.
            if (searchSpecBuilder.getSchemaTypeFiltersCount() > 0) {
                for (int i = 0; i < searchSpecBuilder.getSchemaTypeFiltersCount(); i++) {
                    String qualifiedType = getDatabasePrefix(databaseName)
                            + searchSpecBuilder.getSchemaTypeFilters(i);
                    searchSpecBuilder.setSchemaTypeFilters(i, qualifiedType);
                }
            } else {
                Set<String> schemaTypeSet = getSharedSet(databaseName, SCHEMA_TYPE_SET_NAME);
                for (String schemaType : schemaTypeSet) {
                    String qualifiedType = getDatabasePrefix(databaseName) + schemaType;
                    searchSpecBuilder.addSchemaTypeFilters(qualifiedType);
                }
            }

            // Rewrite any namespaces specified in the searchSpec, or add namespaces to limit the
            // search to this database instance.
            if (searchSpecBuilder.getNamespaceFiltersCount() > 0) {
                for (int i = 0; i < searchSpecBuilder.getNamespaceFiltersCount(); i++) {
                    String qualifiedNamespace = getDatabasePrefix(databaseName)
                            + searchSpecBuilder.getNamespaceFilters(i);
                    searchSpecBuilder.setNamespaceFilters(i, qualifiedNamespace);
                }
            } else {
                Set<String> namespaceSet = getSharedSet(databaseName, NAMESPACE_SET_NAME);
                for (String namespace : namespaceSet) {
                    String qualifiedNamespace = getDatabasePrefix(databaseName) + namespace;
                    searchSpecBuilder.addNamespaceFilters(qualifiedNamespace);
                }
            }
            searchResultProto = mIcingSearchEngine.search(
                    searchSpecBuilder.build(), scoringSpec, resultSpec);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
        if (searchResultProto.getResultsCount() == 0) {
            return searchResultProto;
        }

        // Remove the rewritten schema types from any result documents.
        SearchResultProto.Builder searchResultsBuilder = searchResultProto.toBuilder();
        for (int i = 0; i < searchResultsBuilder.getResultsCount(); i++) {
            if (searchResultProto.getResults(i).hasDocument()) {
                SearchResultProto.ResultProto.Builder resultBuilder =
                        searchResultsBuilder.getResults(i).toBuilder();
                DocumentProto.Builder documentBuilder = resultBuilder.getDocument().toBuilder();
                rewriteDocumentTypes(
                        getDatabasePrefix(databaseName), documentBuilder, /*add=*/false);
                resultBuilder.setDocument(documentBuilder);
                searchResultsBuilder.setResults(i, resultBuilder);
            }
        }
        return searchResultsBuilder.build();
    }

    /**
     * Removes the given document by URI.
     *
     * <p>This method belongs to mutate group.
     *
     * @param databaseName The databaseName the document is in.
     * @param namespace    Namespace of the document to remove.
     * @param uri          URI of the document to remove.
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws InterruptedException if the current thread was interrupted during execution.
     */
    public void remove(@NonNull String databaseName, @NonNull String namespace,
            @NonNull String uri) throws AppSearchException, InterruptedException {
        checkInitialized();

        String qualifiedNamespace = getDatabasePrefix(databaseName) + namespace;
        DeleteResultProto deleteResultProto;
        mReadWriteLock.writeLock().lock();
        try {
            deleteResultProto = mIcingSearchEngine.delete(qualifiedNamespace, uri);
            checkForOptimize(/* force= */false);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
        checkSuccess(deleteResultProto.getStatus());
    }

    /**
     * Removes all documents having the given {@code schemaType} in given database.
     *
     * <p>This method belongs to mutate group.
     *
     * @param databaseName The databaseName that contains documents of schemaType.
     * @param schemaType   The schemaType of documents to remove.
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws InterruptedException if the current thread was interrupted during execution.
     */
    public void removeByType(@NonNull String databaseName, @NonNull String schemaType)
            throws AppSearchException, InterruptedException {
        checkInitialized();

        String qualifiedType = getDatabasePrefix(databaseName) + schemaType;
        DeleteBySchemaTypeResultProto deleteBySchemaTypeResultProto;
        mReadWriteLock.writeLock().lock();
        try {
            deleteBySchemaTypeResultProto = mIcingSearchEngine.deleteBySchemaType(qualifiedType);
            checkForOptimize(/* force= */true);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
        checkSuccess(deleteBySchemaTypeResultProto.getStatus());
    }

    /**
     * Removes all documents having the given {@code namespace} in given database.
     *
     * <p>This method belongs to mutate group.
     *
     * @param databaseName The databaseName that contains documents of namespace.
     * @param namespace    The namespace of documents to remove.
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws InterruptedException if the current thread was interrupted during execution.
     */
    public void removeByNamespace(@NonNull String databaseName, @NonNull String namespace)
            throws AppSearchException, InterruptedException {
        checkInitialized();

        String qualifiedNamespace = getDatabasePrefix(databaseName) + namespace;
        DeleteByNamespaceResultProto deleteByNamespaceResultProto;
        mReadWriteLock.writeLock().lock();
        try {
            deleteByNamespaceResultProto = mIcingSearchEngine.deleteByNamespace(qualifiedNamespace);
            checkForOptimize(/* force= */true);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
        checkSuccess(deleteByNamespaceResultProto.getStatus());
    }

    /**
     * Removes all documents in given database.
     *
     * <p>This method belongs to mutate group.
     *
     * @param databaseName The databaseName to remove all documents from.
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws InterruptedException if the current thread was interrupted during execution.
     */
    public void removeAll(@NonNull String databaseName)
            throws AppSearchException, InterruptedException {
        checkInitialized();

        mReadWriteLock.writeLock().lock();
        try {
            for (String namespace : getSharedSet(databaseName, NAMESPACE_SET_NAME)) {
                DeleteByNamespaceResultProto deleteByNamespaceResultProto =
                        mIcingSearchEngine.deleteByNamespace(
                                getDatabasePrefix(databaseName) + namespace);
                checkSuccess(deleteByNamespaceResultProto.getStatus());
            }
            checkForOptimize(/* force= */true);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Clears documents and schema across all databaseNames.
     *
     * <p>This method belongs to mutate group.
     *
     * @throws AppSearchException on IcingSearchEngine error.
     */
    @VisibleForTesting
    public void reset() throws AppSearchException {
        // Clear data from IcingSearchEngine.

        ResetResultProto resetResultProto;
        mReadWriteLock.writeLock().lock();
        try {
            resetResultProto = mIcingSearchEngine.reset();
            mOptimizeIntervalCount = 0;

            // Clear data from SharedPreferences.
            mSharedPreferences.edit().clear().commit();
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
        checkSuccess(resetResultProto.getStatus());
    }

    /**
     * Rewrites all types mentioned in the given {@code newSchema} to prepend {@code prefix}.
     * Rewritten types will be added to the {@code existingSchema}.
     *
     * @param existingSchema A schema that may contain existing types from across all database
     *                       instances. Will be mutated to contain the properly rewritten schema
     *                       types from {@code newSchema}.
     * @param prefix         Prefix to add to schema type names in {@code newSchema}.
     * @param newSchema      Schema with types to add to the {@code existingSchema}.
     */
    @VisibleForTesting
    void rewriteSchema(
            @NonNull SchemaProto.Builder existingSchema, @NonNull String prefix,
            @NonNull SchemaProto newSchema) {
        HashMap<String, SchemaTypeConfigProto> newTypesToProto = new HashMap<>();

        // Rewrite the schema type to include the typePrefix.
        for (int typeIdx = 0; typeIdx < newSchema.getTypesCount(); typeIdx++) {
            SchemaTypeConfigProto.Builder typeConfigBuilder =
                    newSchema.getTypes(typeIdx).toBuilder();

            // Rewrite SchemaProto.types.schema_type
            String newSchemaType = prefix + typeConfigBuilder.getSchemaType();
            typeConfigBuilder.setSchemaType(newSchemaType);

            // Rewrite SchemaProto.types.properties.schema_type
            for (int propertyIdx = 0;
                    propertyIdx < typeConfigBuilder.getPropertiesCount();
                    propertyIdx++) {
                PropertyConfigProto.Builder propertyConfigBuilder =
                        typeConfigBuilder.getProperties(propertyIdx).toBuilder();
                if (!propertyConfigBuilder.getSchemaType().isEmpty()) {
                    String newPropertySchemaType =
                            prefix + propertyConfigBuilder.getSchemaType();
                    propertyConfigBuilder.setSchemaType(newPropertySchemaType);
                    typeConfigBuilder.setProperties(propertyIdx, propertyConfigBuilder);
                }
            }

            newTypesToProto.put(newSchemaType, typeConfigBuilder.build());
        }

        // Update the existing schema with new types.
        // TODO(b/162093169): This prevents us from ever removing types from the schema.
        for (int i = 0; i < existingSchema.getTypesCount(); i++) {
            String schemaType = existingSchema.getTypes(i).getSchemaType();
            SchemaTypeConfigProto newProto = newTypesToProto.remove(schemaType);
            if (newProto != null) {
                // Replacement
                existingSchema.setTypes(i, newProto);
            }
        }
        // We've been removing existing types from newTypesToProto, so everything that remains is
        // new.
        existingSchema.addAllTypes(newTypesToProto.values());
    }

    /**
     * Rewrites all types and namespaces mentioned anywhere in {@code documentBuilder} to prepend
     * or remove {@code prefix}.
     *
     * @param prefix          The prefix to add or remove
     * @param documentBuilder The document to mutate
     * @param add             Whether to add prefix to the types and namespaces. If {@code false},
     *                        prefix will be removed.
     * @throws IllegalStateException If {@code add=false} and the document has a type or namespace
     *                               that doesn't start with {@code prefix}.
     */
    @VisibleForTesting
    void rewriteDocumentTypes(
            @NonNull String prefix,
            @NonNull DocumentProto.Builder documentBuilder,
            boolean add) {
        // Rewrite the type name to include/remove the prefix.
        String newSchema;
        if (add) {
            newSchema = prefix + documentBuilder.getSchema();
        } else {
            newSchema = removePrefix(prefix, "schemaType", documentBuilder.getSchema());
        }
        documentBuilder.setSchema(newSchema);

        // Rewrite the namespace to include/remove the prefix.
        if (add) {
            documentBuilder.setNamespace(prefix + documentBuilder.getNamespace());
        } else {
            documentBuilder.setNamespace(
                    removePrefix(prefix, "namespace", documentBuilder.getNamespace()));
        }

        // Recurse into derived documents
        for (int propertyIdx = 0;
                propertyIdx < documentBuilder.getPropertiesCount();
                propertyIdx++) {
            int documentCount = documentBuilder.getProperties(propertyIdx).getDocumentValuesCount();
            if (documentCount > 0) {
                PropertyProto.Builder propertyBuilder =
                        documentBuilder.getProperties(propertyIdx).toBuilder();
                for (int documentIdx = 0; documentIdx < documentCount; documentIdx++) {
                    DocumentProto.Builder derivedDocumentBuilder =
                            propertyBuilder.getDocumentValues(documentIdx).toBuilder();
                    rewriteDocumentTypes(prefix, derivedDocumentBuilder, add);
                    propertyBuilder.setDocumentValues(documentIdx, derivedDocumentBuilder);
                }
                documentBuilder.setProperties(propertyIdx, propertyBuilder);
            }
        }
    }

    @NonNull
    private String getDatabasePrefix(String databaseName) {
        return databaseName + "/";
    }

    @NonNull
    private static String removePrefix(@NonNull String prefix, @NonNull String inputType,
            @NonNull String input) {
        if (!input.startsWith(prefix)) {
            throw new IllegalStateException(
                    "Unexpected " + inputType + " \"" + input
                            + "\" does not start with \"" + prefix + "\"");
        }
        return input.substring(prefix.length());
    }

    private void addToSharedSet(String databaseName, String setName, String value) {
        String fullSetName = getDatabasePrefix(databaseName) + setName;
        Set<String> sharedSet = mSharedPreferences.getStringSet(fullSetName,
                new HashSet<>());
        if (!sharedSet.contains(value)) {
            HashSet<String> newSet = new HashSet<>(sharedSet);
            newSet.add(value);
            mSharedPreferences.edit().putStringSet(fullSetName, newSet).commit();
        }
    }

    private Set<String> getSharedSet(String databaseName, String setType) {
        return mSharedPreferences.getStringSet(getDatabasePrefix(databaseName) + setType,
                Collections.emptySet());
    }

    /**
     * Ensure the instance is intialized.
     *
     * @throws AppSearchException if not.
     * @throws InterruptedException if the current thread was interrupted during execution.
     */
    private void checkInitialized() throws AppSearchException, InterruptedException {
        mInitCompleteLatch.await();
        if (!isInitialized()) {
            throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                    "Accessing an uninitialized AppSearchImpl");
        }
    }

    /**
     * Checks the given status code and throws an {@link AppSearchException} if code is an error.
     *
     * @throws AppSearchException on error codes.
     */
    private void checkSuccess(StatusProto statusProto) throws AppSearchException {
        if (statusProto.getCode() == StatusProto.Code.OK) {
            // Everything's good
            return;
        }

        if (statusProto.getCode() == StatusProto.Code.WARNING_DATA_LOSS) {
            // TODO: May want to propagate WARNING_DATA_LOSS up to AppSearchManager so they can
            //  choose to log the error or potentially pass it on to clients.
            Log.w(TAG, "Encountered WARNING_DATA_LOSS: " + statusProto.getMessage());
            return;
        }

        throw statusProtoToAppSearchException(statusProto);
    }

    /**
     * Checks whether {@link IcingSearchEngine#optimize()} should be called to release resources.
     *
     * <p>This method should be only called in mutate methods and get the write lock to keep thread
     * safety.
     * <p>{@link IcingSearchEngine#optimize()} should be called only if
     * {@link GetOptimizeInfoResultProto} shows there is enough resources could be released.
     * <p>{@link IcingSearchEngine#getOptimizeInfo()} should be called once per
     * {@link #CHECK_OPTIMIZE_INTERVAL} of remove executions.
     *
     * @param force whether we should directly call {@link IcingSearchEngine#getOptimizeInfo()}.
     */
    @GuardedBy("mReadWriteLock")
    private void checkForOptimize(boolean force) throws AppSearchException {
        ++mOptimizeIntervalCount;
        if (force || mOptimizeIntervalCount >= CHECK_OPTIMIZE_INTERVAL) {
            mOptimizeIntervalCount = 0;
            GetOptimizeInfoResultProto optimizeInfo = getOptimizeInfoResult();
            checkSuccess(optimizeInfo.getStatus());
            // Second threshold, decide when to call optimize().
            if (optimizeInfo.getOptimizableDocs() >= OPTIMIZE_THRESHOLD_DOC_COUNT
                    || optimizeInfo.getEstimatedOptimizableBytes()
                    >= OPTIMIZE_THRESHOLD_BYTES) {
                // TODO(b/155939114): call optimize in the same thread will slow down api calls
                //  significantly. Move this call to background.
                OptimizeResultProto optimizeResultProto = mIcingSearchEngine.optimize();
                checkSuccess(optimizeResultProto.getStatus());
            }
            // TODO(b/147699081): Return OptimizeResultProto & log lost data detail once we add
            //  a field to indicate lost_schema and lost_documents in OptimizeResultProto.
            //  go/icing-library-apis.
        }
    }

    @VisibleForTesting
    GetOptimizeInfoResultProto getOptimizeInfoResult() {
        return mIcingSearchEngine.getOptimizeInfo();
    }

    /**
     * Converts an erroneous status code to an AppSearchException. Callers should ensure that
     * the status code is not OK or WARNING_DATA_LOSS.
     *
     * @param statusProto StatusProto with error code and message to translate into
     *                    AppSearchException.
     * @return AppSearchException with the parallel error code.
     */
    private AppSearchException statusProtoToAppSearchException(StatusProto statusProto) {
        switch (statusProto.getCode()) {
            case INVALID_ARGUMENT:
                return new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                        statusProto.getMessage());
            case NOT_FOUND:
                return new AppSearchException(AppSearchResult.RESULT_NOT_FOUND,
                        statusProto.getMessage());
            case FAILED_PRECONDITION:
                // Fallthrough
            case ABORTED:
                // Fallthrough
            case INTERNAL:
                return new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        statusProto.getMessage());
            case OUT_OF_SPACE:
                return new AppSearchException(AppSearchResult.RESULT_OUT_OF_SPACE,
                        statusProto.getMessage());
            default:
                // Some unknown/unsupported error
                return new AppSearchException(AppSearchResult.RESULT_UNKNOWN_ERROR,
                        "Unknown IcingSearchEngine status code: " + statusProto.getCode());
        }
    }
}
