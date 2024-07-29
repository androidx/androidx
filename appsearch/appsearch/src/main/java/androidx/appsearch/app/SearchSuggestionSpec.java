/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.SearchSuggestionSpecCreator;
import androidx.appsearch.util.BundleUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the specification logic for AppSearch. It can be used to set the filter
 * and settings of search a suggestions.
 *
 * @see AppSearchSession#searchSuggestionAsync
 */
@SafeParcelable.Class(creator = "SearchSuggestionSpecCreator")
@SuppressWarnings("HiddenSuperclass")
public final class SearchSuggestionSpec extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @NonNull public static final Parcelable.Creator<SearchSuggestionSpec> CREATOR =
            new SearchSuggestionSpecCreator();

    @NonNull
    @Field(id = 1, getter = "getFilterNamespaces")
    private final List<String> mFilterNamespaces;

    @NonNull
    @Field(id = 2, getter = "getFilterSchemas")
    private final List<String> mFilterSchemas;

    // Maps are not supported by SafeParcelable fields, using Bundle instead. Here the key is
    // schema type and value is a list of target property paths in that schema to search over.
    @NonNull
    @Field(id = 3)
    final Bundle mFilterProperties;

    // Maps are not supported by SafeParcelable fields, using Bundle instead. Here the key is
    // namespace and value is a list of target document ids in that namespace to search over.
    @NonNull
    @Field(id = 4)
    final Bundle mFilterDocumentIds;

    @Field(id = 5, getter = "getRankingStrategy")
    private final int mRankingStrategy;

    @Field(id = 6, getter = "getMaximumResultCount")
    private final int mMaximumResultCount;

    @NonNull
    @Field(id = 7, getter = "getSearchStringParameters")
    private final List<String> mSearchStringParameters;

    /** @exportToFramework:hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Constructor
    public SearchSuggestionSpec(
            @Param(id = 1) @NonNull List<String> filterNamespaces,
            @Param(id = 2) @NonNull List<String> filterSchemas,
            @Param(id = 3) @NonNull Bundle filterProperties,
            @Param(id = 4) @NonNull Bundle filterDocumentIds,
            @Param(id = 5) @SuggestionRankingStrategy int rankingStrategy,
            @Param(id = 6) int maximumResultCount,
            @Param(id = 7) @Nullable List<String> searchStringParameters) {
        Preconditions.checkArgument(maximumResultCount >= 1,
                "MaximumResultCount must be positive.");
        mFilterNamespaces = Preconditions.checkNotNull(filterNamespaces);
        mFilterSchemas = Preconditions.checkNotNull(filterSchemas);
        mFilterProperties = Preconditions.checkNotNull(filterProperties);
        mFilterDocumentIds = Preconditions.checkNotNull(filterDocumentIds);
        mRankingStrategy = rankingStrategy;
        mMaximumResultCount = maximumResultCount;
        mSearchStringParameters =
                (searchStringParameters != null)
                        ? Collections.unmodifiableList(searchStringParameters)
                        : Collections.emptyList();
    }

    /**
     * Ranking Strategy for {@link SearchSuggestionResult}.
     *
     * @exportToFramework:hide
     */
    @IntDef(value = {
            SUGGESTION_RANKING_STRATEGY_NONE,
            SUGGESTION_RANKING_STRATEGY_DOCUMENT_COUNT,
            SUGGESTION_RANKING_STRATEGY_TERM_FREQUENCY,
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    public @interface SuggestionRankingStrategy {
    }

    /**
     * Ranked by the document count that contains the term.
     *
     * <p>Suppose the following document is in the index.
     * <pre>Doc1 contains: term1 term2 term2 term2</pre>
     * <pre>Doc2 contains: term1</pre>
     *
     * <p>Then, suppose that a search suggestion for "t" is issued with the DOCUMENT_COUNT, the
     * returned {@link SearchSuggestionResult}s will be: term1, term2. The term1 will have higher
     * score and appear in the results first.
     */
    public static final int SUGGESTION_RANKING_STRATEGY_DOCUMENT_COUNT = 0;
    /**
     * Ranked by the term appear frequency.
     *
     * <p>Suppose the following document is in the index.
     * <pre>Doc1 contains: term1 term2 term2 term2</pre>
     * <pre>Doc2 contains: term1</pre>
     *
     * <p>Then, suppose that a search suggestion for "t" is issued with the TERM_FREQUENCY,
     * the returned {@link SearchSuggestionResult}s will be: term2, term1. The term2 will have
     * higher score and appear in the results first.
     */
    public static final int SUGGESTION_RANKING_STRATEGY_TERM_FREQUENCY = 1;

    /** No Ranking, results are returned in arbitrary order. */
    public static final int SUGGESTION_RANKING_STRATEGY_NONE = 2;

    /**
     * Returns the maximum number of wanted suggestion that will be returned in the result object.
     */
    public int getMaximumResultCount() {
        return mMaximumResultCount;
    }

    /**
     * Returns the list of namespaces to search over.
     *
     * <p>If empty, will search over all namespaces.
     */
    @NonNull
    public List<String> getFilterNamespaces() {
        if (mFilterNamespaces == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(mFilterNamespaces);
    }

    /** Returns the ranking strategy. */
    @SuggestionRankingStrategy
    public int getRankingStrategy() {
        return mRankingStrategy;
    }

    /**
     * Returns the list of schema to search the suggestion over.
     *
     * <p>If empty, will search over all schemas.
     */
    @NonNull
    public List<String> getFilterSchemas() {
        if (mFilterSchemas == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(mFilterSchemas);
    }

    /**
     * Returns the map of schema and target properties to search over.
     *
     * <p>The keys of the returned map are schema types, and the values are the target property path
     * in that schema to search over.
     *
     * <p>If {@link Builder#addFilterPropertyPaths} was never called, returns an empty map. In this
     * case AppSearch will search over all schemas and properties.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned
     * by this function, rather than calling it multiple times.
     */
    @NonNull
    @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES)
    public Map<String, List<String>> getFilterProperties() {
        Set<String> schemas = mFilterProperties.keySet();
        Map<String, List<String>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            typePropertyPathsMap.put(schema, Preconditions.checkNotNull(
                    mFilterProperties.getStringArrayList(schema)));
        }
        return typePropertyPathsMap;
    }

    /**
     * Returns the map of namespace and target document ids to search over.
     *
     * <p>The keys of the returned map are namespaces, and the values are the target document ids
     * in that namespace to search over.
     *
     * <p>If {@link Builder#addFilterDocumentIds} was never called, returns an empty map. In this
     * case AppSearch will search over all namespace and document ids.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned
     * by this function, rather than calling it multiple times.
     */
    @NonNull
    public Map<String, List<String>> getFilterDocumentIds() {
        Set<String> namespaces = mFilterDocumentIds.keySet();
        Map<String, List<String>> documentIdsMap = new ArrayMap<>(namespaces.size());
        for (String namespace : namespaces) {
            documentIdsMap.put(namespace, Preconditions.checkNotNull(
                    mFilterDocumentIds.getStringArrayList(namespace)));
        }
        return documentIdsMap;
    }

    /**
     * Returns the list of String parameters that can be referenced in the query through the
     * "getSearchStringParameter({index})" function.
     *
     * @see AppSearchSession#search
     */
    @NonNull
    @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
    public List<String> getSearchStringParameters() {
        return mSearchStringParameters;
    }

    /** Builder for {@link SearchSuggestionSpec objects}. */
    public static final class Builder {
        private ArrayList<String> mNamespaces = new ArrayList<>();
        private ArrayList<String> mSchemas = new ArrayList<>();
        private Bundle mTypePropertyFilters = new Bundle();
        private Bundle mDocumentIds = new Bundle();
        private final int mTotalResultCount;
        @SuggestionRankingStrategy private int mRankingStrategy =
                SUGGESTION_RANKING_STRATEGY_DOCUMENT_COUNT;
        private List<String> mSearchStringParameters = new ArrayList<>();
        private boolean mBuilt = false;

        /**
         * Creates an {@link SearchSuggestionSpec.Builder} object.
         *
         * @param maximumResultCount Sets the maximum number of suggestion in the returned object.
         */
        public Builder(@IntRange(from = 1) int maximumResultCount) {
            Preconditions.checkArgument(maximumResultCount >= 1,
                    "maximumResultCount must be positive.");
            mTotalResultCount = maximumResultCount;
        }

        /**
         * Adds a namespace filter to {@link SearchSuggestionSpec} Entry. Only search for
         * suggestions that has documents under the specified namespaces.
         *
         * <p>If unset, the query will search over all namespaces.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterNamespaces(@NonNull String... namespaces) {
            Preconditions.checkNotNull(namespaces);
            resetIfBuilt();
            return addFilterNamespaces(Arrays.asList(namespaces));
        }

        /**
         * Adds a namespace filter to {@link SearchSuggestionSpec} Entry. Only search for
         * suggestions that has documents under the specified namespaces.
         *
         * <p>If unset, the query will search over all namespaces.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterNamespaces(@NonNull Collection<String> namespaces) {
            Preconditions.checkNotNull(namespaces);
            resetIfBuilt();
            mNamespaces.addAll(namespaces);
            return this;
        }

        /**
         * Sets ranking strategy for suggestion results.
         *
         * <p>The default value {@link #SUGGESTION_RANKING_STRATEGY_DOCUMENT_COUNT} will be used if
         * this method is never called.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRankingStrategy(@SuggestionRankingStrategy int rankingStrategy) {
            Preconditions.checkArgumentInRange(rankingStrategy,
                    SUGGESTION_RANKING_STRATEGY_DOCUMENT_COUNT, SUGGESTION_RANKING_STRATEGY_NONE,
                    "Suggestion ranking strategy");
            resetIfBuilt();
            mRankingStrategy = rankingStrategy;
            return this;
        }

        /**
         * Adds a schema filter to {@link SearchSuggestionSpec} Entry. Only search for
         * suggestions that has documents under the specified schema.
         *
         * <p>If unset, the query will search over all schema.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterSchemas(@NonNull String... schemaTypes) {
            Preconditions.checkNotNull(schemaTypes);
            resetIfBuilt();
            return addFilterSchemas(Arrays.asList(schemaTypes));
        }

        /**
         * Adds a schema filter to {@link SearchSuggestionSpec} Entry. Only search for
         * suggestions that has documents under the specified schema.
         *
         * <p>If unset, the query will search over all schema.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterSchemas(@NonNull Collection<String> schemaTypes) {
            Preconditions.checkNotNull(schemaTypes);
            resetIfBuilt();
            mSchemas.addAll(schemaTypes);
            return this;
        }

// @exportToFramework:startStrip()

        /**
         * Adds a schema filter to {@link SearchSuggestionSpec} Entry. Only search for
         * suggestions that has documents under the specified schema.
         *
         * <p>If unset, the query will search over all schema.
         *
         * <p>Merged list available from {@link #getFilterSchemas()}.
         *
         * @param documentClasses classes annotated with {@link Document}.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterDocumentClasses(@NonNull java.lang.Class<?>... documentClasses)
                throws AppSearchException {
            Preconditions.checkNotNull(documentClasses);
            resetIfBuilt();
            return addFilterDocumentClasses(Arrays.asList(documentClasses));
        }
// @exportToFramework:endStrip()

// @exportToFramework:startStrip()

        /**
         * Adds the Schema names of given document classes to the Schema type filter of
         * {@link SearchSuggestionSpec} Entry. Only search for suggestions that has documents
         * under the specified schema.
         *
         * <p>If unset, the query will search over all schema.
         *
         * <p>Merged list available from {@link #getFilterSchemas()}.
         *
         * @param documentClasses classes annotated with {@link Document}.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterDocumentClasses(
                @NonNull Collection<? extends java.lang.Class<?>> documentClasses)
                throws AppSearchException {
            Preconditions.checkNotNull(documentClasses);
            resetIfBuilt();
            List<String> schemas = new ArrayList<>(documentClasses.size());
            DocumentClassFactoryRegistry registry = DocumentClassFactoryRegistry.getInstance();
            for (java.lang.Class<?> documentClass : documentClasses) {
                DocumentClassFactory<?> factory = registry.getOrCreateFactory(documentClass);
                schemas.add(factory.getSchemaName());
            }
            addFilterSchemas(schemas);
            return this;
        }
// @exportToFramework:endStrip()

        /**
         * Adds property paths for the specified type to the property filter of
         * {@link SearchSuggestionSpec} Entry. Only search for suggestions that has content under
         * the specified property. If property paths are added for a type, then only the
         * properties referred to will be retrieved for results of that type.
         *
         * <p> If a property path that is specified isn't present in a result, it will be ignored
         * for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of
         * results of that type will be retrieved.
         *
         * <p>Example properties: 'body', 'sender.name', 'sender.emailaddress', etc.
         *
         * @param schema the {@link AppSearchSchema} that contains the target properties
         * @param propertyPaths The String version of {@link PropertyPath}. A dot-delimited
         *                      sequence of property names indicating which property in the
         *                      document these snippets correspond to.
         */
        @CanIgnoreReturnValue
        @NonNull
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES)
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES)
        public Builder addFilterProperties(@NonNull String schema,
                @NonNull Collection<String> propertyPaths) {
            Preconditions.checkNotNull(schema);
            Preconditions.checkNotNull(propertyPaths);
            resetIfBuilt();
            ArrayList<String> propertyPathsArrayList = new ArrayList<>(propertyPaths.size());
            for (String propertyPath : propertyPaths) {
                Preconditions.checkNotNull(propertyPath);
                propertyPathsArrayList.add(propertyPath);
            }
            mTypePropertyFilters.putStringArrayList(schema, propertyPathsArrayList);
            return this;
        }

        /**
         * Adds property paths for the specified type to the property filter of
         * {@link SearchSuggestionSpec} Entry. Only search for suggestions that has content under
         * the specified property. If property paths are added for a type, then only the
         * properties referred to will be retrieved for results of that type.
         *
         * <p> If a property path that is specified isn't present in a result, it will be ignored
         * for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of
         * results of that type will be retrieved.
         *
         * @param schema the {@link AppSearchSchema} that contains the target properties
         * @param propertyPaths The {@link PropertyPath} to search suggestion over
         */
        @CanIgnoreReturnValue
        @NonNull
        // Getter method is getFilterProperties
        @SuppressLint("MissingGetterMatchingBuilder")
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES)
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES)
        public Builder addFilterPropertyPaths(@NonNull String schema,
                @NonNull Collection<PropertyPath> propertyPaths) {
            Preconditions.checkNotNull(schema);
            Preconditions.checkNotNull(propertyPaths);
            ArrayList<String> propertyPathsArrayList = new ArrayList<>(propertyPaths.size());
            for (PropertyPath propertyPath : propertyPaths) {
                propertyPathsArrayList.add(propertyPath.toString());
            }
            return addFilterProperties(schema, propertyPathsArrayList);
        }


// @exportToFramework:startStrip()
        /**
         * Adds property paths for the specified type to the property filter of
         * {@link SearchSuggestionSpec} Entry. Only search for suggestions that has content under
         * the specified property. If property paths are added for a type, then only the
         * properties referred to will be retrieved for results of that type.
         *
         * <p> If a property path that is specified isn't present in a result, it will be ignored
         * for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of
         * results of that type will be retrieved.
         *
         * @param documentClass class annotated with {@link Document}.
         * @param propertyPaths The String version of {@link PropertyPath}. A
         * {@code dot-delimited sequence of property names indicating which property in the
         * document these snippets correspond to.
         */
        @NonNull
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES)
        public Builder addFilterProperties(@NonNull java.lang.Class<?> documentClass,
                @NonNull Collection<String> propertyPaths) throws AppSearchException {
            Preconditions.checkNotNull(documentClass);
            Preconditions.checkNotNull(propertyPaths);
            resetIfBuilt();
            DocumentClassFactoryRegistry registry = DocumentClassFactoryRegistry.getInstance();
            DocumentClassFactory<?> factory = registry.getOrCreateFactory(documentClass);
            return addFilterProperties(factory.getSchemaName(), propertyPaths);
        }
// @exportToFramework:endStrip()

// @exportToFramework:startStrip()
        /**
         * Adds property paths for the specified type to the property filter of
         * {@link SearchSuggestionSpec} Entry. Only search for suggestions that has content under
         * the specified property. If property paths are added for a type, then only the
         * properties referred to will be retrieved for results of that type.
         *
         * <p> If a property path that is specified isn't present in a result, it will be ignored
         * for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of
         * results of that type will be retrieved.
         *
         * @param documentClass class annotated with {@link Document}.
         * @param propertyPaths The {@link PropertyPath} to search suggestion over
         */
        @NonNull
        // Getter method is getFilterProperties
        @SuppressLint("MissingGetterMatchingBuilder")
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES)
        public Builder addFilterPropertyPaths(@NonNull java.lang.Class<?> documentClass,
                @NonNull Collection<PropertyPath> propertyPaths) throws AppSearchException {
            Preconditions.checkNotNull(documentClass);
            Preconditions.checkNotNull(propertyPaths);
            resetIfBuilt();
            DocumentClassFactoryRegistry registry = DocumentClassFactoryRegistry.getInstance();
            DocumentClassFactory<?> factory = registry.getOrCreateFactory(documentClass);
            return addFilterPropertyPaths(factory.getSchemaName(), propertyPaths);
        }
// @exportToFramework:endStrip()

        /**
         * Adds a document ID filter to {@link SearchSuggestionSpec} Entry. Only search for
         * suggestions in the given specified documents.
         *
         * <p>If unset, the query will search over all documents.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterDocumentIds(@NonNull String namespace,
                @NonNull String... documentIds) {
            Preconditions.checkNotNull(namespace);
            Preconditions.checkNotNull(documentIds);
            resetIfBuilt();
            return addFilterDocumentIds(namespace, Arrays.asList(documentIds));
        }

        /**
         * Adds a document ID filter to {@link SearchSuggestionSpec} Entry. Only search for
         * suggestions in the given specified documents.
         *
         * <p>If unset, the query will search over all documents.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterDocumentIds(@NonNull String namespace,
                @NonNull Collection<String> documentIds) {
            Preconditions.checkNotNull(namespace);
            Preconditions.checkNotNull(documentIds);
            resetIfBuilt();
            ArrayList<String> documentIdList = new ArrayList<>(documentIds.size());
            for (String documentId : documentIds) {
                documentIdList.add(Preconditions.checkNotNull(documentId));
            }
            mDocumentIds.putStringArrayList(namespace, documentIdList);
            return this;
        }

        /**
         * Adds Strings to the list of String parameters that can be referenced in the query through
         * the "getSearchStringParameter({index})" function.
         *
         * @see AppSearchSession#search
         */
        @CanIgnoreReturnValue
        @NonNull
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
        public Builder addSearchStringParameters(@NonNull String... searchStringParameters) {
            Preconditions.checkNotNull(searchStringParameters);
            resetIfBuilt();
            return addSearchStringParameters(Arrays.asList(searchStringParameters));
        }

        /**
         * Adds Strings to the list of String parameters that can be referenced in the query through
         * the "getSearchStringParameter({index})" function.
         *
         * @see AppSearchSession#search
         */
        @CanIgnoreReturnValue
        @NonNull
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
        public Builder addSearchStringParameters(@NonNull List<String> searchStringParameters) {
            Preconditions.checkNotNull(searchStringParameters);
            resetIfBuilt();
            mSearchStringParameters.addAll(searchStringParameters);
            return this;
        }

        /** Constructs a new {@link SearchSpec} from the contents of this builder. */
        @NonNull
        public SearchSuggestionSpec build() {
            if (!mSchemas.isEmpty()) {
                Set<String> schemaFilter = new ArraySet<>(mSchemas);
                for (String schema : mTypePropertyFilters.keySet()) {
                    if (!schemaFilter.contains(schema)) {
                        throw new IllegalStateException(
                                "The schema: " + schema + " exists in the property filter but "
                                        + "doesn't exist in the schema filter.");
                    }
                }
            }
            if (!mNamespaces.isEmpty()) {
                Set<String> namespaceFilter = new ArraySet<>(mNamespaces);
                for (String namespace : mDocumentIds.keySet()) {
                    if (!namespaceFilter.contains(namespace)) {
                        throw new IllegalStateException(
                                "The namespace: " + namespace + " exists in the document id "
                                        + "filter but doesn't exist in the namespace filter.");
                    }
                }
            }
            mBuilt = true;
            return new SearchSuggestionSpec(
                    mNamespaces,
                    mSchemas,
                    mTypePropertyFilters,
                    mDocumentIds,
                    mRankingStrategy,
                    mTotalResultCount,
                    mSearchStringParameters);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mNamespaces = new ArrayList<>(mNamespaces);
                mSchemas = new ArrayList<>(mSchemas);
                mTypePropertyFilters = BundleUtil.deepCopy(mTypePropertyFilters);
                mDocumentIds = BundleUtil.deepCopy(mDocumentIds);
                mSearchStringParameters = new ArrayList<>(mSearchStringParameters);
                mBuilt = false;
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SearchSuggestionSpecCreator.writeToParcel(this, dest, flags);
    }
}
