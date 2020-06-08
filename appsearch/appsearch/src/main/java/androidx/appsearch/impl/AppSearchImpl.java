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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArraySet;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.PropertyProto;
import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages interaction with {@link FakeIcing} and other components to implement AppSearch
 * functionality.
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
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppSearchImpl {
    private static volatile AppSearchImpl sInstance;
    private static final String SHARED_PREFERENCES_NAME = "androidx.appsearch";
    private static final String NAMESPACE_SET_NAME = "namespace-set";
    private static final String SCHEMA_TYPE_SET_NAME = "schema-type-set";
    // TODO(b/158350212) Remove SharedPreferences once getAllNamespace() is ready in Icing lib.
    // SharedPreferences is discouraged to be used in go/sharedpreferences.
    private final SharedPreferences mSharedPreferences;
    private final FakeIcing mFakeIcing = new FakeIcing();
    private AppSearchImpl(@NonNull Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

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

    /**
     * Updates the AppSearch schema for this app.
     *
     * @param databaseName The name of the database where this schema lives.
     * @param origSchema The schema to set for this app.
     * @param forceOverride Whether to force-apply the schema even if it is incompatible. Documents
     *     which do not comply with the new schema will be deleted.
     */
    public void setSchema(@NonNull String databaseName, @NonNull SchemaProto origSchema,
            boolean forceOverride) {
        SchemaProto.Builder schemaBuilder = origSchema.toBuilder();
        rewriteSchemaTypes(getDatabasePrefix(databaseName), schemaBuilder);
        for (SchemaTypeConfigProto typeConfig : origSchema.getTypesList()) {
            addToSharedSet(databaseName, SCHEMA_TYPE_SET_NAME, typeConfig.getSchemaType());
        }
        // TODO(b/145635424): Apply the schema to Icing and report results
    }

    /**
     * Adds a document to the AppSearch index.
     *
     * @param databaseName The databaseName this document resides in.
     * @param document The document to index.
     */
    public void putDocument(@NonNull String databaseName, @NonNull DocumentProto document) {
        addToSharedSet(databaseName, NAMESPACE_SET_NAME, document.getNamespace());
        DocumentProto.Builder documentBuilder = document.toBuilder();
        rewriteDocumentTypes(getDatabasePrefix(databaseName), documentBuilder, /*add=*/ true);
        mFakeIcing.put(documentBuilder.build());
    }

    /**
     * Retrieves a document from the AppSearch index by URI.
     *
     * @param databaseName The databaseName this document resides in.
     * @param namespace The namespace this document resides in.
     * @param uri The URI of the document to get.
     * @return The Document contents, or {@code null} if no such URI exists in the system.
     */
    @Nullable
    public DocumentProto getDocument(@NonNull String databaseName, @NonNull String namespace,
            @NonNull String uri) {
        DocumentProto documentProto = mFakeIcing.get(
                getDatabasePrefix(databaseName) + namespace, uri);
        if (documentProto == null) {
            return null;
        }
        DocumentProto.Builder documentBuilder = documentProto.toBuilder();
        rewriteDocumentTypes(getDatabasePrefix(databaseName), documentBuilder, /*add=*/ false);
        return documentBuilder.build();
    }
    /**
     * Executes a query against the AppSearch index and returns results.
     *
     * @param databaseName The databaseName this query for.
     * @param searchSpec Defines what and how to search
     * @param resultSpec Defines what results to show
     * @param scoringSpec Defines how to order results
     * @return The results of performing this search  The proto might have no {@code results} if no
     *     documents matched the query.
     */
    @NonNull
    public SearchResultProto query(
            @NonNull String databaseName,
            @NonNull SearchSpecProto searchSpec,
            @NonNull ResultSpecProto resultSpec,
            @NonNull ScoringSpecProto scoringSpec) {
        SearchResultProto searchResults = mFakeIcing.query(searchSpec.getQuery());
        if (searchResults.getResultsCount() == 0) {
            return searchResults;
        }
        Set<String> qualifiedTypeSearchFilters;
        Set<String> qualifiedNamespaceSearchFilters;
        if (searchSpec.getSchemaTypeFiltersCount() > 0) {
            qualifiedTypeSearchFilters = new ArraySet<>(searchSpec.getSchemaTypeFiltersCount());
            for (String schema : searchSpec.getSchemaTypeFiltersList()) {
                String qualifiedSchema = getDatabasePrefix(databaseName) + schema;
                qualifiedTypeSearchFilters.add(qualifiedSchema);
            }
        } else {
            Set<String> schemaTypeSet = getSharedSet(databaseName, SCHEMA_TYPE_SET_NAME);
            qualifiedTypeSearchFilters = new ArraySet<>(schemaTypeSet.size());
            for (String schemaType : schemaTypeSet) {
                qualifiedTypeSearchFilters.add(getDatabasePrefix(databaseName) + schemaType);
            }
        }
        if (searchSpec.getNamespaceFiltersCount() > 0) {
            qualifiedNamespaceSearchFilters = new ArraySet<>(searchSpec.getNamespaceFiltersCount());
            for (String namespace : searchSpec.getNamespaceFiltersList()) {
                String qualifiedNamespace = getDatabasePrefix(databaseName) + namespace;
                qualifiedNamespaceSearchFilters.add(qualifiedNamespace);
            }
        } else {
            Set<String> namespaceSet = getSharedSet(databaseName, NAMESPACE_SET_NAME);
            qualifiedNamespaceSearchFilters = new ArraySet<>(namespaceSet.size());
            for (String namespace : namespaceSet) {
                qualifiedNamespaceSearchFilters.add(getDatabasePrefix(databaseName) + namespace);
            }
        }

        SearchResultProto.Builder searchResultsBuilder = searchResults.toBuilder();
        for (int i = 0; i < searchResultsBuilder.getResultsCount(); i++) {
            if (searchResults.getResults(i).hasDocument()) {
                SearchResultProto.ResultProto.Builder resultBuilder =
                        searchResultsBuilder.getResults(i).toBuilder();
                DocumentProto.Builder documentBuilder = resultBuilder.getDocument().toBuilder();
                // TODO(b/145631811): Since FakeIcing doesn't currently handle type names, we
                //  perform a post-filter to make sure we don't return documents we shouldn't. This
                //  should be removed once the real Icing Lib is implemented.
                if (isNotInFilter(qualifiedTypeSearchFilters, documentBuilder.getSchema())
                        || isNotInFilter(qualifiedNamespaceSearchFilters,
                        documentBuilder.getNamespace())) {
                    searchResultsBuilder.removeResults(i);
                    i--;
                    continue;
                }
                rewriteDocumentTypes(
                        getDatabasePrefix(databaseName), documentBuilder, /*add=*/false);
                resultBuilder.setDocument(documentBuilder);
                searchResultsBuilder.setResults(i, resultBuilder);
            }
        }
        return searchResultsBuilder.build();
    }

    /** Deletes the given document by URI */
    public boolean delete(@NonNull String databaseName, @NonNull String namespace,
            @NonNull String uri) {
        String qualifiedNamespace = getDatabasePrefix(databaseName) + namespace;
        DocumentProto document = mFakeIcing.get(qualifiedNamespace, uri);
        if (document == null) {
            return false;
        }
        return mFakeIcing.delete(qualifiedNamespace, uri);
    }

    /** Deletes all documents having the given {@code schemaType}. */
    public boolean deleteByType(@NonNull String databaseName, @NonNull String schemaType) {
        String qualifiedType = getDatabasePrefix(databaseName) + schemaType;
        return mFakeIcing.deleteByType(qualifiedType);
    }

    /**  Deletes all documents owned by the calling app. */
    public void deleteAll() {
        mFakeIcing.deleteAll();
    }

    /**
     * Rewrites all types mentioned in the given {@code schemaBuilder} to prepend
     * {@code typePrefix}.
     *
     * @param typePrefix The prefix to add
     * @param schemaBuilder The schema to mutate
     */
    @VisibleForTesting
    void rewriteSchemaTypes(
            @NonNull String typePrefix, @NonNull SchemaProto.Builder schemaBuilder) {
        for (int typeIdx = 0; typeIdx < schemaBuilder.getTypesCount(); typeIdx++) {
            SchemaTypeConfigProto.Builder typeConfigBuilder =
                    schemaBuilder.getTypes(typeIdx).toBuilder();

            // Rewrite SchemaProto.types.schema_type
            String newSchemaType = typePrefix + typeConfigBuilder.getSchemaType();
            typeConfigBuilder.setSchemaType(newSchemaType);

            // Rewrite SchemaProto.types.properties.schema_type
            for (int propertyIdx = 0;
                    propertyIdx < typeConfigBuilder.getPropertiesCount();
                    propertyIdx++) {
                PropertyConfigProto.Builder propertyConfigBuilder =
                        typeConfigBuilder.getProperties(propertyIdx).toBuilder();
                if (!propertyConfigBuilder.getSchemaType().isEmpty()) {
                    String newPropertySchemaType =
                            typePrefix + propertyConfigBuilder.getSchemaType();
                    propertyConfigBuilder.setSchemaType(newPropertySchemaType);
                    typeConfigBuilder.setProperties(propertyIdx, propertyConfigBuilder);
                }
            }

            schemaBuilder.setTypes(typeIdx, typeConfigBuilder);
        }
    }

    /**
     * Rewrites all types mentioned anywhere in {@code documentBuilder} to prepend or remove
     * {@code typePrefix}.
     *
     * @param typePrefix The prefix to add or remove
     * @param documentBuilder The document to mutate
     * @param add Whether to add typePrefix to the types. If {@code false}, typePrefix will be
     *     removed from the types.
     * @throws IllegalArgumentException If {@code add=false} and the document has a type that
     *     doesn't start with {@code typePrefix}.
     */
    @VisibleForTesting
    void rewriteDocumentTypes(
            @NonNull String typePrefix,
            @NonNull DocumentProto.Builder documentBuilder,
            boolean add) {
        // Rewrite the type name to include/remove the app's prefix
        String newSchema;
        if (add) {
            newSchema = typePrefix + documentBuilder.getSchema();
        } else {
            newSchema = removePrefix(typePrefix, documentBuilder.getSchema());
        }
        documentBuilder.setSchema(newSchema);

        if (add) {
            documentBuilder.setNamespace(typePrefix + documentBuilder.getNamespace());
        } else if (!documentBuilder.getNamespace().startsWith(typePrefix)) {
            throw new IllegalStateException(
                    "Unexpected namespace \"" + documentBuilder.getNamespace()
                            + "\" (expected \"" + typePrefix + "\")");
        } else {
            documentBuilder.setNamespace(removePrefix(typePrefix, documentBuilder.getNamespace()));
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
                    rewriteDocumentTypes(typePrefix, derivedDocumentBuilder, add);
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
    private static String removePrefix(@NonNull String prefix, @NonNull String input) {
        if (!input.startsWith(prefix)) {
            throw new IllegalArgumentException(
                    "Input \"" + input + "\" does not start with \"" + prefix + "\"");
        }
        return input.substring(prefix.length());
    }

    private static boolean isNotInFilter(Set<String> filter, String candidate) {
        return filter != null && !filter.contains(candidate);
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
}
