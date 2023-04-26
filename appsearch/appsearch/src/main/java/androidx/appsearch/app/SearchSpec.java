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

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.exceptions.AppSearchException;
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
 * This class represents the specification logic for AppSearch. It can be used to set the type of
 * search, like prefix or exact only or apply filters to search for a specific schema type only etc.
 */
public final class SearchSpec {
    /**
     * Schema type to be used in {@link SearchSpec.Builder#addProjection} to apply
     * property paths to all results, excepting any types that have had their own, specific
     * property paths set.
     */
    public static final String PROJECTION_SCHEMA_TYPE_WILDCARD = "*";

    static final String TERM_MATCH_TYPE_FIELD = "termMatchType";
    static final String SCHEMA_FIELD = "schema";
    static final String NAMESPACE_FIELD = "namespace";
    static final String PACKAGE_NAME_FIELD = "packageName";
    static final String NUM_PER_PAGE_FIELD = "numPerPage";
    static final String RANKING_STRATEGY_FIELD = "rankingStrategy";
    static final String ORDER_FIELD = "order";
    static final String SNIPPET_COUNT_FIELD = "snippetCount";
    static final String SNIPPET_COUNT_PER_PROPERTY_FIELD = "snippetCountPerProperty";
    static final String MAX_SNIPPET_FIELD = "maxSnippet";
    static final String PROJECTION_TYPE_PROPERTY_PATHS_FIELD = "projectionTypeFieldMasks";
    static final String RESULT_GROUPING_TYPE_FLAGS = "resultGroupingTypeFlags";
    static final String RESULT_GROUPING_LIMIT = "resultGroupingLimit";
    static final String TYPE_PROPERTY_WEIGHTS_FIELD = "typePropertyWeightsField";
    static final String JOIN_SPEC = "joinSpec";
    static final String ADVANCED_RANKING_EXPRESSION = "advancedRankingExpression";
    static final String ENABLED_FEATURES_FIELD = "enabledFeatures";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final int DEFAULT_NUM_PER_PAGE = 10;

    // TODO(b/170371356): In framework, we may want these limits to be flag controlled.
    //  If that happens, the @IntRange() directives in this class may have to change.
    private static final int MAX_NUM_PER_PAGE = 10_000;
    private static final int MAX_SNIPPET_COUNT = 10_000;
    private static final int MAX_SNIPPET_PER_PROPERTY_COUNT = 10_000;
    private static final int MAX_SNIPPET_SIZE_LIMIT = 10_000;

    /**
     * Term Match Type for the query.
     *
     * @hide
     */
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link com.google.android.icing.proto.SearchSpecProto.termMatchType}
    @IntDef(value = {
            TERM_MATCH_EXACT_ONLY,
            TERM_MATCH_PREFIX
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TermMatch {
    }

    /**
     * Query terms will only match exact tokens in the index.
     * <p>Ex. A query term "foo" will only match indexed token "foo", and not "foot" or "football".
     */
    public static final int TERM_MATCH_EXACT_ONLY = 1;
    /**
     * Query terms will match indexed tokens when the query term is a prefix of the token.
     * <p>Ex. A query term "foo" will match indexed tokens like "foo", "foot", and "football".
     */
    public static final int TERM_MATCH_PREFIX = 2;

    /**
     * Ranking Strategy for query result.
     *
     * @hide
     */
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link ScoringSpecProto.RankingStrategy.Code}
    @IntDef(value = {
            RANKING_STRATEGY_NONE,
            RANKING_STRATEGY_DOCUMENT_SCORE,
            RANKING_STRATEGY_CREATION_TIMESTAMP,
            RANKING_STRATEGY_RELEVANCE_SCORE,
            RANKING_STRATEGY_USAGE_COUNT,
            RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP,
            RANKING_STRATEGY_SYSTEM_USAGE_COUNT,
            RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP,
            RANKING_STRATEGY_JOIN_AGGREGATE_SCORE,
            RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RankingStrategy {
    }

    /** No Ranking, results are returned in arbitrary order. */
    public static final int RANKING_STRATEGY_NONE = 0;
    /** Ranked by app-provided document scores. */
    public static final int RANKING_STRATEGY_DOCUMENT_SCORE = 1;
    /** Ranked by document creation timestamps. */
    public static final int RANKING_STRATEGY_CREATION_TIMESTAMP = 2;
    /** Ranked by document relevance score. */
    public static final int RANKING_STRATEGY_RELEVANCE_SCORE = 3;
    /** Ranked by number of usages, as reported by the app. */
    public static final int RANKING_STRATEGY_USAGE_COUNT = 4;
    /** Ranked by timestamp of last usage, as reported by the app. */
    public static final int RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP = 5;
    /** Ranked by number of usages from a system UI surface. */
    public static final int RANKING_STRATEGY_SYSTEM_USAGE_COUNT = 6;
    /** Ranked by timestamp of last usage from a system UI surface. */
    public static final int RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP = 7;
    /**
     * Ranked by the aggregated ranking signal of the joined documents.
     *
     * <p> Which aggregation strategy is used to determine a ranking signal is specified in the
     * {@link JoinSpec} set by {@link Builder#setJoinSpec}. This ranking strategy may not be used
     * if no {@link JoinSpec} is provided.
     *
     * @see Builder#build
     */
    public static final int RANKING_STRATEGY_JOIN_AGGREGATE_SCORE = 8;
    /** Ranked by the advanced ranking expression provided. */
    public static final int RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION = 9;

    /**
     * Order for query result.
     *
     * @hide
     */
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link ScoringSpecProto.Order.Code}
    @IntDef(value = {
            ORDER_DESCENDING,
            ORDER_ASCENDING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Order {
    }

    /** Search results will be returned in a descending order. */
    public static final int ORDER_DESCENDING = 0;
    /** Search results will be returned in an ascending order. */
    public static final int ORDER_ASCENDING = 1;

    /**
     * Grouping type for result limits.
     *
     * @hide
     */
    @IntDef(flag = true, value = {
            GROUPING_TYPE_PER_PACKAGE,
            GROUPING_TYPE_PER_NAMESPACE,
            GROUPING_TYPE_PER_SCHEMA
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    public @interface GroupingType {
    }
    /**
     * Results should be grouped together by package for the purpose of enforcing a limit on the
     * number of results returned per package.
     */
    public static final int GROUPING_TYPE_PER_PACKAGE = 1 << 0;
    /**
     * Results should be grouped together by namespace for the purpose of enforcing a limit on the
     * number of results returned per namespace.
     */
    public static final int GROUPING_TYPE_PER_NAMESPACE = 1 << 1;
    /**
     * Results should be grouped together by schema type for the purpose of enforcing a limit on the
     * number of results returned per schema type.
     */
    // @exportToFramework:startStrip()
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA)
    // @exportToFramework:endStrip()
    public static final int GROUPING_TYPE_PER_SCHEMA = 1 << 2;

    private final Bundle mBundle;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SearchSpec(@NonNull Bundle bundle) {
        Preconditions.checkNotNull(bundle);
        mBundle = bundle;
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Bundle getBundle() {
        return mBundle;
    }

    /** Returns how the query terms should match terms in the index. */
    @TermMatch
    public int getTermMatch() {
        return mBundle.getInt(TERM_MATCH_TYPE_FIELD, -1);
    }

    /**
     * Returns the list of schema types to search for.
     *
     * <p>If empty, the query will search over all schema types.
     */
    @NonNull
    public List<String> getFilterSchemas() {
        List<String> schemas = mBundle.getStringArrayList(SCHEMA_FIELD);
        if (schemas == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(schemas);
    }

    /**
     * Returns the list of namespaces to search over.
     *
     * <p>If empty, the query will search over all namespaces.
     */
    @NonNull
    public List<String> getFilterNamespaces() {
        List<String> namespaces = mBundle.getStringArrayList(NAMESPACE_FIELD);
        if (namespaces == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(namespaces);
    }

    /**
     * Returns the list of package name filters to search over.
     *
     * <p>If empty, the query will search over all packages that the caller has access to. If
     * package names are specified which caller doesn't have access to, then those package names
     * will be ignored.
     */
    @NonNull
    public List<String> getFilterPackageNames() {
        List<String> packageNames = mBundle.getStringArrayList(PACKAGE_NAME_FIELD);
        if (packageNames == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(packageNames);
    }

    /** Returns the number of results per page in the result set. */
    public int getResultCountPerPage() {
        return mBundle.getInt(NUM_PER_PAGE_FIELD, DEFAULT_NUM_PER_PAGE);
    }

    /** Returns the ranking strategy. */
    @RankingStrategy
    public int getRankingStrategy() {
        return mBundle.getInt(RANKING_STRATEGY_FIELD);
    }

    /** Returns the order of returned search results (descending or ascending). */
    @Order
    public int getOrder() {
        return mBundle.getInt(ORDER_FIELD);
    }

    /** Returns how many documents to generate snippets for. */
    public int getSnippetCount() {
        return mBundle.getInt(SNIPPET_COUNT_FIELD);
    }

    /**
     * Returns how many matches for each property of a matching document to generate snippets for.
     */
    public int getSnippetCountPerProperty() {
        return mBundle.getInt(SNIPPET_COUNT_PER_PROPERTY_FIELD);
    }

    /** Returns the maximum size of a snippet in characters. */
    public int getMaxSnippetSize() {
        return mBundle.getInt(MAX_SNIPPET_FIELD);
    }

    /**
     * Returns a map from schema type to property paths to be used for projection.
     *
     * <p>If the map is empty, then all properties will be retrieved for all results.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned
     * by this function, rather than calling it multiple times.
     *
     * @return A mapping of schema types to lists of projection strings.
     */
    @NonNull
    public Map<String, List<String>> getProjections() {
        Bundle typePropertyPathsBundle = Preconditions.checkNotNull(
                mBundle.getBundle(PROJECTION_TYPE_PROPERTY_PATHS_FIELD));
        Set<String> schemas = typePropertyPathsBundle.keySet();
        Map<String, List<String>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            typePropertyPathsMap.put(schema, Preconditions.checkNotNull(
                    typePropertyPathsBundle.getStringArrayList(schema)));
        }
        return typePropertyPathsMap;
    }

    /**
     * Returns a map from schema type to property paths to be used for projection.
     *
     * <p>If the map is empty, then all properties will be retrieved for all results.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned
     * by this function, rather than calling it multiple times.
     *
     * @return A mapping of schema types to lists of projection {@link PropertyPath} objects.
     */
    @NonNull
    public Map<String, List<PropertyPath>> getProjectionPaths() {
        Bundle typePropertyPathsBundle = mBundle.getBundle(PROJECTION_TYPE_PROPERTY_PATHS_FIELD);
        Set<String> schemas = typePropertyPathsBundle.keySet();
        Map<String, List<PropertyPath>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            ArrayList<String> propertyPathList = typePropertyPathsBundle.getStringArrayList(schema);
            List<PropertyPath> copy = new ArrayList<>(propertyPathList.size());
            for (String p: propertyPathList) {
                copy.add(new PropertyPath(p));
            }
            typePropertyPathsMap.put(schema, copy);
        }
        return typePropertyPathsMap;
    }

    /**
     * Returns properties weights to be used for scoring.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the {@link Map} returned
     * by this function, rather than calling it multiple times.
     *
     * @return a {@link Map} of schema type to an inner-map of property paths of the schema type to
     * the weight to set for that property.
     */
    @NonNull
    public Map<String, Map<String, Double>> getPropertyWeights() {
        Bundle typePropertyWeightsBundle = mBundle.getBundle(TYPE_PROPERTY_WEIGHTS_FIELD);
        Set<String> schemaTypes = typePropertyWeightsBundle.keySet();
        Map<String, Map<String, Double>> typePropertyWeightsMap = new ArrayMap<>(
                schemaTypes.size());
        for (String schemaType : schemaTypes) {
            Bundle propertyPathBundle = typePropertyWeightsBundle.getBundle(schemaType);
            Set<String> propertyPaths = propertyPathBundle.keySet();
            Map<String, Double> propertyPathWeights = new ArrayMap<>(propertyPaths.size());
            for (String propertyPath : propertyPaths) {
                propertyPathWeights.put(propertyPath, propertyPathBundle.getDouble(propertyPath));
            }
            typePropertyWeightsMap.put(schemaType, propertyPathWeights);
        }
        return typePropertyWeightsMap;
    }

    /**
     * Returns properties weights to be used for scoring.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the {@link Map} returned
     * by this function, rather than calling it multiple times.
     *
     * @return a {@link Map} of schema type to an inner-map of property paths of the schema type to
     * the weight to set for that property.
     */
    @NonNull
    public Map<String, Map<PropertyPath, Double>> getPropertyWeightPaths() {
        Bundle typePropertyWeightsBundle = mBundle.getBundle(TYPE_PROPERTY_WEIGHTS_FIELD);
        Set<String> schemaTypes = typePropertyWeightsBundle.keySet();
        Map<String, Map<PropertyPath, Double>> typePropertyWeightsMap = new ArrayMap<>(
                schemaTypes.size());
        for (String schemaType : schemaTypes) {
            Bundle propertyPathBundle = typePropertyWeightsBundle.getBundle(schemaType);
            Set<String> propertyPaths = propertyPathBundle.keySet();
            Map<PropertyPath, Double> propertyPathWeights = new ArrayMap<>(propertyPaths.size());
            for (String propertyPath : propertyPaths) {
                propertyPathWeights.put(new PropertyPath(propertyPath),
                        propertyPathBundle.getDouble(propertyPath));
            }
            typePropertyWeightsMap.put(schemaType, propertyPathWeights);
        }
        return typePropertyWeightsMap;
    }

    /**
     * Get the type of grouping limit to apply, or 0 if {@link Builder#setResultGrouping} was not
     * called.
     */
    @GroupingType
    public int getResultGroupingTypeFlags() {
        return mBundle.getInt(RESULT_GROUPING_TYPE_FLAGS);
    }

    /**
     * Get the maximum number of results to return for each group.
     *
     * @return the maximum number of results to return for each group or Integer.MAX_VALUE if
     * {@link Builder#setResultGrouping(int, int)} was not called.
     */
    public int getResultGroupingLimit() {
        return mBundle.getInt(RESULT_GROUPING_LIMIT, Integer.MAX_VALUE);
    }

    /**
     * Returns specification on which documents need to be joined.
     */
    @Nullable
    public JoinSpec getJoinSpec() {
        Bundle joinSpec = mBundle.getBundle(JOIN_SPEC);
        if (joinSpec == null) {
            return null;
        }
        return new JoinSpec(joinSpec);
    }

    /**
     * Get the advanced ranking expression, or "" if {@link Builder#setRankingStrategy(String)}
     * was not called.
     */
    @NonNull
    public String getAdvancedRankingExpression() {
        return mBundle.getString(ADVANCED_RANKING_EXPRESSION, "");
    }

    /**
     * Returns whether the {@link Features#NUMERIC_SEARCH} feature is enabled.
     */
    public boolean isNumericSearchEnabled() {
        return getEnabledFeatures().contains(FeatureConstants.NUMERIC_SEARCH);
    }

    /**
     * Returns whether the {@link Features#VERBATIM_SEARCH} feature is enabled.
     */
    public boolean isVerbatimSearchEnabled() {
        return getEnabledFeatures().contains(FeatureConstants.VERBATIM_SEARCH);
    }

    /**
     * Returns whether the {@link Features#LIST_FILTER_QUERY_LANGUAGE} feature is enabled.
     */
    public boolean isListFilterQueryLanguageEnabled() {
        return getEnabledFeatures().contains(FeatureConstants.LIST_FILTER_QUERY_LANGUAGE);
    }

    /**
     * Get the list of enabled features that the caller is intending to use in this search call.
     *
     * @return the set of {@link Features} enabled in this {@link SearchSpec} Entry.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public List<String> getEnabledFeatures() {
        return mBundle.getStringArrayList(ENABLED_FEATURES_FIELD);
    }

    /** Builder for {@link SearchSpec objects}. */
    public static final class Builder {
        private ArrayList<String> mSchemas = new ArrayList<>();
        private ArrayList<String> mNamespaces = new ArrayList<>();
        private ArrayList<String> mPackageNames = new ArrayList<>();
        private ArraySet<String> mEnabledFeatures = new ArraySet<>();
        private Bundle mProjectionTypePropertyMasks = new Bundle();
        private Bundle mTypePropertyWeights = new Bundle();

        private int mResultCountPerPage = DEFAULT_NUM_PER_PAGE;
        @TermMatch private int mTermMatchType = TERM_MATCH_PREFIX;
        private int mSnippetCount = 0;
        private int mSnippetCountPerProperty = MAX_SNIPPET_PER_PROPERTY_COUNT;
        private int mMaxSnippetSize = 0;
        @RankingStrategy private int mRankingStrategy = RANKING_STRATEGY_NONE;
        @Order private int mOrder = ORDER_DESCENDING;
        @GroupingType private int mGroupingTypeFlags = 0;
        private int mGroupingLimit = 0;
        private JoinSpec mJoinSpec;
        private String mAdvancedRankingExpression = "";
        private boolean mBuilt = false;

        /**
         * Indicates how the query terms should match {@code TermMatchCode} in the index.
         *
         * <p>If this method is not called, the default term match type is
         * {@link SearchSpec#TERM_MATCH_PREFIX}.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTermMatch(@TermMatch int termMatchType) {
            Preconditions.checkArgumentInRange(termMatchType, TERM_MATCH_EXACT_ONLY,
                    TERM_MATCH_PREFIX, "Term match type");
            resetIfBuilt();
            mTermMatchType = termMatchType;
            return this;
        }

        /**
         * Adds a Schema type filter to {@link SearchSpec} Entry. Only search for documents that
         * have the specified schema types.
         *
         * <p>If unset, the query will search over all schema types.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterSchemas(@NonNull String... schemas) {
            Preconditions.checkNotNull(schemas);
            resetIfBuilt();
            return addFilterSchemas(Arrays.asList(schemas));
        }

        /**
         * Adds a Schema type filter to {@link SearchSpec} Entry. Only search for documents that
         * have the specified schema types.
         *
         * <p>If unset, the query will search over all schema types.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterSchemas(@NonNull Collection<String> schemas) {
            Preconditions.checkNotNull(schemas);
            resetIfBuilt();
            mSchemas.addAll(schemas);
            return this;
        }

// @exportToFramework:startStrip()

        /**
         * Adds the Schema names of given document classes to the Schema type filter of
         * {@link SearchSpec} Entry. Only search for documents that have the specified schema types.
         *
         * <p>If unset, the query will search over all schema types.
         *
         * <p>Merged list available from {@link #getFilterSchemas()}.
         *
         * @param documentClasses classes annotated with {@link Document}.
         */
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addFilterDocumentClasses(
                @NonNull Collection<? extends Class<?>> documentClasses) throws AppSearchException {
            Preconditions.checkNotNull(documentClasses);
            resetIfBuilt();
            List<String> schemas = new ArrayList<>(documentClasses.size());
            DocumentClassFactoryRegistry registry = DocumentClassFactoryRegistry.getInstance();
            for (Class<?> documentClass : documentClasses) {
                DocumentClassFactory<?> factory = registry.getOrCreateFactory(documentClass);
                schemas.add(factory.getSchemaName());
            }
            addFilterSchemas(schemas);
            return this;
        }
// @exportToFramework:endStrip()

// @exportToFramework:startStrip()

        /**
         * Adds the Schema names of given document classes to the Schema type filter of
         * {@link SearchSpec} Entry. Only search for documents that have the specified schema types.
         *
         * <p>If unset, the query will search over all schema types.
         *
         * <p>Merged list available from {@link #getFilterSchemas()}.
         *
         * @param documentClasses classes annotated with {@link Document}.
         */
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addFilterDocumentClasses(@NonNull Class<?>... documentClasses)
                throws AppSearchException {
            Preconditions.checkNotNull(documentClasses);
            resetIfBuilt();
            return addFilterDocumentClasses(Arrays.asList(documentClasses));
        }
// @exportToFramework:endStrip()

        /**
         * Adds a namespace filter to {@link SearchSpec} Entry. Only search for documents that
         * have the specified namespaces.
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
         * Adds a namespace filter to {@link SearchSpec} Entry. Only search for documents that
         * have the specified namespaces.
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
         * Adds a package name filter to {@link SearchSpec} Entry. Only search for documents that
         * were indexed from the specified packages.
         *
         * <p>If unset, the query will search over all packages that the caller has access to.
         * If package names are specified which caller doesn't have access to, then those package
         * names will be ignored.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterPackageNames(@NonNull String... packageNames) {
            Preconditions.checkNotNull(packageNames);
            resetIfBuilt();
            return addFilterPackageNames(Arrays.asList(packageNames));
        }

        /**
         * Adds a package name filter to {@link SearchSpec} Entry. Only search for documents that
         * were indexed from the specified packages.
         *
         * <p>If unset, the query will search over all packages that the caller has access to.
         * If package names are specified which caller doesn't have access to, then those package
         * names will be ignored.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterPackageNames(@NonNull Collection<String> packageNames) {
            Preconditions.checkNotNull(packageNames);
            resetIfBuilt();
            mPackageNames.addAll(packageNames);
            return this;
        }

        /**
         * Sets the number of results per page in the returned object.
         *
         * <p>The default number of results per page is 10.
         */
        @CanIgnoreReturnValue
        @NonNull
        public SearchSpec.Builder setResultCountPerPage(
                @IntRange(from = 0, to = MAX_NUM_PER_PAGE) int resultCountPerPage) {
            Preconditions.checkArgumentInRange(
                    resultCountPerPage, 0, MAX_NUM_PER_PAGE, "resultCountPerPage");
            resetIfBuilt();
            mResultCountPerPage = resultCountPerPage;
            return this;
        }

        /** Sets ranking strategy for AppSearch results. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRankingStrategy(@RankingStrategy int rankingStrategy) {
            Preconditions.checkArgumentInRange(rankingStrategy, RANKING_STRATEGY_NONE,
                    RANKING_STRATEGY_JOIN_AGGREGATE_SCORE, "Result ranking strategy");
            resetIfBuilt();
            mRankingStrategy = rankingStrategy;
            mAdvancedRankingExpression = "";
            return this;
        }

        /**
         * Enables advanced ranking to score based on {@code advancedRankingExpression}.
         *
         * <p>This method will set RankingStrategy to
         * {@link #RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION}.
         *
         * <p>The ranking expression is a mathematical expression that will be evaluated to a
         * floating-point number of double type representing the score of each document.
         *
         * <p>Numeric literals, arithmetic operators, mathematical functions, and document-based
         * functions are supported to build expressions.
         *
         * <p>The following are supported arithmetic operators:
         * <ul>
         *     <li>Addition(+)
         *     <li>Subtraction(-)
         *     <li>Multiplication(*)
         *     <li>Floating Point Division(/)
         * </ul>
         *
         * <p>Operator precedences are compliant with the Java Language, and parentheses are
         * supported. For example, "2.2 + (3 - 4) / 2" evaluates to 1.7.
         *
         * <p>The following are supported basic mathematical functions:
         * <ul>
         *     <li>log(x) - the natural log of x
         *     <li>log(x, y) - the log of y with base x
         *     <li>pow(x, y) - x to the power of y
         *     <li>sqrt(x)
         *     <li>abs(x)
         *     <li>sin(x), cos(x), tan(x)
         *     <li>Example: "max(abs(-100), 10) + pow(2, 10)" will be evaluated to 1124
         * </ul>
         *
         * <p>The following variadic mathematical functions are supported, with n > 0. They also
         * accept list value parameters. For example, if V is a value of list type, we can call
         * sum(V) to get the sum of all the values in V. List literals are not supported, so a
         * value of list type can only be constructed as a return value of some particular
         * document-based functions.
         * <ul>
         *     <li>max(v1, v2, ..., vn) or max(V)
         *     <li>min(v1, v2, ..., vn) or min(V)
         *     <li>len(v1, v2, ..., vn) or len(V)
         *     <li>sum(v1, v2, ..., vn) or sum(V)
         *     <li>avg(v1, v2, ..., vn) or avg(V)
         * </ul>
         *
         * <p>Document-based functions must be called via "this", which represents the current
         * document being scored. The following are supported document-based functions:
         * <ul>
         *     <li>this.documentScore()
         *     <p>Get the app-provided document score of the current document. This is the same
         *     score that is returned for {@link #RANKING_STRATEGY_DOCUMENT_SCORE}.
         *     <li>this.creationTimestamp()
         *     <p>Get the creation timestamp of the current document. This is the same score that
         *     is returned for {@link #RANKING_STRATEGY_CREATION_TIMESTAMP}.
         *     <li>this.relevanceScore()
         *     <p>Get the BM25F relevance score of the current document in relation to the query
         *     string. This is the same score that is returned for
         *     {@link #RANKING_STRATEGY_RELEVANCE_SCORE}.
         *     <li>this.usageCount(type) and this.usageLastUsedTimestamp(type)
         *     <p>Get the number of usages or the timestamp of last usage by type for the current
         *     document, where type must be evaluated to an integer from 1 to 2. Type 1 refers to
         *     usages reported by {@link AppSearchSession#reportUsageAsync}, and type 2 refers to
         *     usages reported by {@link GlobalSearchSession#reportSystemUsageAsync}.
         *     <li>this.childrenScores()
         *     <p>Returns a list of children document scores. Currently, a document can only be a
         *     child of another document in the context of joins. If this function is called
         *     without the Join API enabled, a type error will be raised.
         *     <li>this.propertyWeights()
         *     <p>Returns a list of the normalized weights of the matched properties for the
         *     current document being scored. Property weights come from what's specified in
         *     {@link SearchSpec}. After normalizing, each provided weight will be divided by the
         *     maximum weight, so that each of them will be <= 1.
         * </ul>
         *
         * <p>Some errors may occur when using advanced ranking.
         *
         * <p>Syntax Error: the expression violates the syntax of the advanced ranking language.
         * Below are some examples.
         * <ul>
         *     <li>"1 + " - missing operand
         *     <li>"2 * (1 + 2))" - unbalanced parenthesis
         *     <li>"2 ^ 3" - unknown operator
         * </ul>
         *
         * <p>Type Error: the expression fails a static type check. Below are some examples.
         * <ul>
         *     <li>"sin(2, 3)" - wrong number of arguments for the sin function
         *     <li>"this.childrenScores() + 1" - cannot add a list with a number
         *     <li>"this.propertyWeights()" - the final type of the overall expression cannot be
         *     a list, which can be fixed by "max(this.propertyWeights())"
         *     <li>"abs(this.propertyWeights())" - the abs function does not support list type
         *     arguments
         *     <li>"print(2)" - unknown function
         * </ul>
         *
         * <p>Evaluation Error: an error occurred while evaluating the value of the expression.
         * Below are some examples.
         * <ul>
         *     <li>"1 / 0", "log(0)", "1 + sqrt(-1)" - getting a non-finite value in the middle
         *     of evaluation
         *     <li>"this.usageCount(1 + 0.5)" - expect the argument to be an integer. Note that
         *     this is not a type error and "this.usageCount(1.5 + 1/2)" can succeed without any
         *     issues
         *     <li>"this.documentScore()" - in case of an IO error, this will be an evaluation error
         * </ul>
         *
         * <p>Syntax errors and type errors will fail the entire search and will cause
         * {@link SearchResults#getNextPageAsync} to throw an {@link AppSearchException} with the
         * result code of {@link AppSearchResult#RESULT_INVALID_ARGUMENT}.
         * <p>Evaluation errors will result in the offending documents receiving the default score.
         * For {@link #ORDER_DESCENDING}, the default score will be 0, for
         * {@link #ORDER_ASCENDING} the default score will be infinity.
         *
         * @param advancedRankingExpression a non-empty string representing the ranking expression.
         */
        @CanIgnoreReturnValue
        @NonNull
        // @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION)
        // @exportToFramework:endStrip()
        public Builder setRankingStrategy(@NonNull String advancedRankingExpression) {
            Preconditions.checkStringNotEmpty(advancedRankingExpression);
            resetIfBuilt();
            mRankingStrategy = RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION;
            mAdvancedRankingExpression = advancedRankingExpression;
            return this;
        }

        /**
         * Indicates the order of returned search results, the default is
         * {@link #ORDER_DESCENDING}, meaning that results with higher scores come first.
         *
         * <p>This order field will be ignored if RankingStrategy = {@code RANKING_STRATEGY_NONE}.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setOrder(@Order int order) {
            Preconditions.checkArgumentInRange(order, ORDER_DESCENDING, ORDER_ASCENDING,
                    "Result ranking order");
            resetIfBuilt();
            mOrder = order;
            return this;
        }

        /**
         * Only the first {@code snippetCount} documents based on the ranking strategy
         * will have snippet information provided.
         *
         * <p>The list returned from {@link SearchResult#getMatchInfos} will contain at most this
         * many entries.
         *
         * <p>If set to 0 (default), snippeting is disabled and the list returned from
         * {@link SearchResult#getMatchInfos} will be empty.
         */
        @CanIgnoreReturnValue
        @NonNull
        public SearchSpec.Builder setSnippetCount(
                @IntRange(from = 0, to = MAX_SNIPPET_COUNT) int snippetCount) {
            Preconditions.checkArgumentInRange(snippetCount, 0, MAX_SNIPPET_COUNT, "snippetCount");
            resetIfBuilt();
            mSnippetCount = snippetCount;
            return this;
        }

        /**
         * Sets {@code snippetCountPerProperty}. Only the first {@code snippetCountPerProperty}
         * snippets for each property of each {@link GenericDocument} will contain snippet
         * information.
         *
         * <p>If set to 0, snippeting is disabled and the list
         * returned from {@link SearchResult#getMatchInfos} will be empty.
         *
         * <p>The default behavior is to snippet all matches a property contains, up to the maximum
         * value of 10,000.
         */
        @CanIgnoreReturnValue
        @NonNull
        public SearchSpec.Builder setSnippetCountPerProperty(
                @IntRange(from = 0, to = MAX_SNIPPET_PER_PROPERTY_COUNT)
                int snippetCountPerProperty) {
            Preconditions.checkArgumentInRange(snippetCountPerProperty,
                    0, MAX_SNIPPET_PER_PROPERTY_COUNT, "snippetCountPerProperty");
            resetIfBuilt();
            mSnippetCountPerProperty = snippetCountPerProperty;
            return this;
        }

        /**
         * Sets {@code maxSnippetSize}, the maximum snippet size. Snippet windows start at
         * {@code maxSnippetSize/2} bytes before the middle of the matching token and end at
         * {@code maxSnippetSize/2} bytes after the middle of the matching token. It respects
         * token boundaries, therefore the returned window may be smaller than requested.
         *
         * <p> Setting {@code maxSnippetSize} to 0 will disable windowing and an empty string will
         * be returned. If matches enabled is also set to false, then snippeting is disabled.
         *
         * <p>Ex. {@code maxSnippetSize} = 16. "foo bar baz bat rat" with a query of "baz" will
         * return a window of "bar baz bat" which is only 11 bytes long.
         */
        @CanIgnoreReturnValue
        @NonNull
        public SearchSpec.Builder setMaxSnippetSize(
                @IntRange(from = 0, to = MAX_SNIPPET_SIZE_LIMIT) int maxSnippetSize) {
            Preconditions.checkArgumentInRange(
                    maxSnippetSize, 0, MAX_SNIPPET_SIZE_LIMIT, "maxSnippetSize");
            resetIfBuilt();
            mMaxSnippetSize = maxSnippetSize;
            return this;
        }

        /**
         * Adds property paths for the specified type to be used for projection. If property
         * paths are added for a type, then only the properties referred to will be retrieved for
         * results of that type. If a property path that is specified isn't present in a result,
         * it will be ignored for that result. Property paths cannot be null.
         *
         * @see #addProjectionPaths
         *
         * @param schema a string corresponding to the schema to add projections to.
         * @param propertyPaths the projections to add.
         */
        @CanIgnoreReturnValue
        @NonNull
        public SearchSpec.Builder addProjection(
                @NonNull String schema, @NonNull Collection<String> propertyPaths) {
            Preconditions.checkNotNull(schema);
            Preconditions.checkNotNull(propertyPaths);
            resetIfBuilt();
            ArrayList<String> propertyPathsArrayList = new ArrayList<>(propertyPaths.size());
            for (String propertyPath : propertyPaths) {
                Preconditions.checkNotNull(propertyPath);
                propertyPathsArrayList.add(propertyPath);
            }
            mProjectionTypePropertyMasks.putStringArrayList(schema, propertyPathsArrayList);
            return this;
        }

        /**
         * Adds property paths for the specified type to be used for projection. If property
         * paths are added for a type, then only the properties referred to will be retrieved for
         * results of that type. If a property path that is specified isn't present in a result,
         * it will be ignored for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of
         * results of that type will be retrieved.
         *
         * <p>If property path is added for the
         * {@link SearchSpec#PROJECTION_SCHEMA_TYPE_WILDCARD}, then those property paths will
         * apply to all results, excepting any types that have their own, specific property paths
         * set.
         *
         * <p>Suppose the following document is in the index.
         * <pre>{@code
         * Email: Document {
         *   sender: Document {
         *     name: "Mr. Person"
         *     email: "mrperson123@google.com"
         *   }
         *   recipients: [
         *     Document {
         *       name: "John Doe"
         *       email: "johndoe123@google.com"
         *     }
         *     Document {
         *       name: "Jane Doe"
         *       email: "janedoe123@google.com"
         *     }
         *   ]
         *   subject: "IMPORTANT"
         *   body: "Limited time offer!"
         * }
         * }</pre>
         *
         * <p>Then, suppose that a query for "important" is issued with the following projection
         * type property paths:
         * <pre>{@code
         * {schema: "Email", ["subject", "sender.name", "recipients.name"]}
         * }</pre>
         *
         * <p>The above document will be returned as:
         * <pre>{@code
         * Email: Document {
         *   sender: Document {
         *     name: "Mr. Body"
         *   }
         *   recipients: [
         *     Document {
         *       name: "John Doe"
         *     }
         *     Document {
         *       name: "Jane Doe"
         *     }
         *   ]
         *   subject: "IMPORTANT"
         * }
         * }</pre>
         *
         * @param schema a string corresponding to the schema to add projections to.
         * @param propertyPaths the projections to add.
         */
        @CanIgnoreReturnValue
        @NonNull
        public SearchSpec.Builder addProjectionPaths(
                @NonNull String schema, @NonNull Collection<PropertyPath> propertyPaths) {
            Preconditions.checkNotNull(schema);
            Preconditions.checkNotNull(propertyPaths);
            ArrayList<String> propertyPathsArrayList = new ArrayList<>(propertyPaths.size());
            for (PropertyPath propertyPath : propertyPaths) {
                propertyPathsArrayList.add(propertyPath.toString());
            }
            return addProjection(schema, propertyPathsArrayList);
        }

// @exportToFramework:startStrip()
        /**
         * Adds property paths for the Document class to be used for projection. If property
         * paths are added for a document class, then only the properties referred to will be
         * retrieved for results of that type. If a property path that is specified isn't present
         * in a result, it will be ignored for that result. Property paths cannot be null.
         *
         * @see #addProjection
         *
         * @param documentClass a class, annotated with @Document, corresponding to the schema to
         *                      add projections to.
         * @param propertyPaths the projections to add.
         */
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")  // Projections available from getProjections
        @NonNull
        public SearchSpec.Builder addProjectionsForDocumentClass(
                @NonNull Class<?> documentClass, @NonNull Collection<String> propertyPaths)
                throws AppSearchException {
            Preconditions.checkNotNull(documentClass);
            resetIfBuilt();
            DocumentClassFactoryRegistry registry = DocumentClassFactoryRegistry.getInstance();
            DocumentClassFactory<?> factory = registry.getOrCreateFactory(documentClass);
            return addProjection(factory.getSchemaName(), propertyPaths);
        }

        /**
         * Adds property paths for the specified Document class to be used for projection.
         * @see #addProjectionPaths
         *
         * @param documentClass a class, annotated with @Document, corresponding to the schema to
         *                      add projections to.
         * @param propertyPaths the projections to add.
         */
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")  // Projections available from getProjections
        @NonNull
        public SearchSpec.Builder addProjectionPathsForDocumentClass(
                @NonNull Class<?> documentClass, @NonNull Collection<PropertyPath> propertyPaths)
                throws AppSearchException {
            Preconditions.checkNotNull(documentClass);
            resetIfBuilt();
            ArrayList<String> propertyPathsArrayList = new ArrayList<>(propertyPaths.size());
            for (PropertyPath propertyPath : propertyPaths) {
                propertyPathsArrayList.add(propertyPath.toString());
            }
            return addProjectionsForDocumentClass(documentClass, propertyPathsArrayList);
        }
// @exportToFramework:endStrip()

        /**
         * Sets the maximum number of results to return for each group, where groups are defined
         * by grouping type.
         *
         * <p>Calling this method will override any previous calls. So calling
         * setResultGrouping(GROUPING_TYPE_PER_PACKAGE, 7) and then calling
         * setResultGrouping(GROUPING_TYPE_PER_PACKAGE, 2) will result in only the latter, a
         * limit of two results per package, being applied. Or calling setResultGrouping
         * (GROUPING_TYPE_PER_PACKAGE, 1) and then calling setResultGrouping
         * (GROUPING_TYPE_PER_PACKAGE | GROUPING_PER_NAMESPACE, 5) will result in five results
         * per package per namespace.
         *
         * @param groupingTypeFlags One or more combination of grouping types.
         * @param limit             Number of results to return per {@code groupingTypeFlags}.
         * @throws IllegalArgumentException if groupingTypeFlags is zero.
         */
        // Individual parameters available from getResultGroupingTypeFlags and
        // getResultGroupingLimit
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setResultGrouping(@GroupingType int groupingTypeFlags, int limit) {
            Preconditions.checkState(
                    groupingTypeFlags != 0, "Result grouping type cannot be zero.");
            resetIfBuilt();
            mGroupingTypeFlags = groupingTypeFlags;
            mGroupingLimit = limit;
            return this;
        }

        /**
         * Sets property weights by schema type and property path.
         *
         * <p>Property weights are used to promote and demote query term matches within a
         * {@link GenericDocument} property when applying scoring.
         *
         * <p>Property weights must be positive values (greater than 0). A property's weight is
         * multiplied with that property's scoring contribution. This means weights set between 0.0
         * and 1.0 demote scoring contributions by a term match within the property. Weights set
         * above 1.0 promote scoring contributions by a term match within the property.
         *
         * <p>Properties that exist in the {@link AppSearchSchema}, but do not have a weight
         * explicitly set will be given a default weight of 1.0.
         *
         * <p>Weights set for property paths that do not exist in the {@link AppSearchSchema} will
         * be discarded and not affect scoring.
         *
         * <p><b>NOTE:</b> Property weights only affect scoring for query-dependent scoring
         * strategies, such as {@link #RANKING_STRATEGY_RELEVANCE_SCORE}.
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         * <!--@exportToFramework:else()-->
         *
         * @param schemaType          the schema type to set property weights for.
         * @param propertyPathWeights a {@link Map} of property paths of the schema type to the
         *                            weight to set for that property.
         * @throws IllegalArgumentException if a weight is equal to or less than 0.0.
         */
        // @exportToFramework:startStrip()
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_PROPERTY_WEIGHTS)
        // @exportToFramework:endStrip()
        @NonNull
        public SearchSpec.Builder setPropertyWeights(@NonNull String schemaType,
                @NonNull Map<String, Double> propertyPathWeights) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(propertyPathWeights);

            Bundle propertyPathBundle = new Bundle();
            for (Map.Entry<String, Double> propertyPathWeightEntry :
                    propertyPathWeights.entrySet()) {
                String propertyPath = Preconditions.checkNotNull(propertyPathWeightEntry.getKey());
                Double weight = Preconditions.checkNotNull(propertyPathWeightEntry.getValue());
                if (weight <= 0.0) {
                    throw new IllegalArgumentException("Cannot set non-positive property weight "
                            + "value " + weight + " for property path: " + propertyPath);
                }
                propertyPathBundle.putDouble(propertyPath, weight);
            }
            mTypePropertyWeights.putBundle(schemaType, propertyPathBundle);
            return this;
        }

        /**
         * Specifies which documents to join with, and how to join.
         *
         * <p> If the ranking strategy is {@link #RANKING_STRATEGY_JOIN_AGGREGATE_SCORE}, and the
         * JoinSpec is null, {@link #build} will throw an {@link AppSearchException}.
         *
         * @param joinSpec a specification on how to perform the Join operation.
         */
        // @exportToFramework:startStrip()
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.JOIN_SPEC_AND_QUALIFIED_ID)
        // @exportToFramework:endStrip()
        @NonNull
        public Builder setJoinSpec(@NonNull JoinSpec joinSpec) {
            resetIfBuilt();
            mJoinSpec = Preconditions.checkNotNull(joinSpec);
            return this;
        }

        /**
         * Sets property weights by schema type and property path.
         *
         * <p>Property weights are used to promote and demote query term matches within a
         * {@link GenericDocument} property when applying scoring.
         *
         * <p>Property weights must be positive values (greater than 0). A property's weight is
         * multiplied with that property's scoring contribution. This means weights set between 0.0
         * and 1.0 demote scoring contributions by a term match within the property. Weights set
         * above 1.0 promote scoring contributions by a term match within the property.
         *
         * <p>Properties that exist in the {@link AppSearchSchema}, but do not have a weight
         * explicitly set will be given a default weight of 1.0.
         *
         * <p>Weights set for property paths that do not exist in the {@link AppSearchSchema} will
         * be discarded and not affect scoring.
         *
         * <p><b>NOTE:</b> Property weights only affect scoring for query-dependent scoring
         * strategies, such as {@link #RANKING_STRATEGY_RELEVANCE_SCORE}.
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         * <!--@exportToFramework:else()-->
         *
         * @param schemaType          the schema type to set property weights for.
         * @param propertyPathWeights a {@link Map} of property paths of the schema type to the
         *                            weight to set for that property.
         * @throws IllegalArgumentException if a weight is equal to or less than 0.0.
         */
        // @exportToFramework:startStrip()
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_PROPERTY_WEIGHTS)
        // @exportToFramework:endStrip()
        @NonNull
        public SearchSpec.Builder setPropertyWeightPaths(@NonNull String schemaType,
                @NonNull Map<PropertyPath, Double> propertyPathWeights) {
            Preconditions.checkNotNull(propertyPathWeights);

            Map<String, Double> propertyWeights = new ArrayMap<>(propertyPathWeights.size());
            for (Map.Entry<PropertyPath, Double> propertyPathWeightEntry :
                    propertyPathWeights.entrySet()) {
                PropertyPath propertyPath =
                        Preconditions.checkNotNull(propertyPathWeightEntry.getKey());
                propertyWeights.put(propertyPath.toString(), propertyPathWeightEntry.getValue());
            }
            return setPropertyWeights(schemaType, propertyWeights);
        }

// @exportToFramework:startStrip()

        /**
         * Sets property weights by schema type and property path.
         *
         * <p>Property weights are used to promote and demote query term matches within a
         * {@link GenericDocument} property when applying scoring.
         *
         * <p>Property weights must be positive values (greater than 0). A property's weight is
         * multiplied with that property's scoring contribution. This means weights set between 0.0
         * and 1.0 demote scoring contributions by a term match within the property. Weights set
         * above 1.0 promote scoring contributions by a term match within the property.
         *
         * <p>Properties that exist in the {@link AppSearchSchema}, but do not have a weight
         * explicitly set will be given a default weight of 1.0.
         *
         * <p>Weights set for property paths that do not exist in the {@link AppSearchSchema} will
         * be discarded and not affect scoring.
         *
         * <p><b>NOTE:</b> Property weights only affect scoring for query-dependent scoring
         * strategies, such as {@link #RANKING_STRATEGY_RELEVANCE_SCORE}.
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         * <!--@exportToFramework:else()-->
         *
         * @param documentClass a class, annotated with @Document, corresponding to the schema to
         *                      set property weights for.
         * @param propertyPathWeights a {@link Map} of property paths of the schema type to the
         *                            weight to set for that property.
         * @throws AppSearchException if no factory for this document class could be found on the
         *                            classpath
         * @throws IllegalArgumentException if a weight is equal to or less than 0.0.
         */
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_PROPERTY_WEIGHTS)
        @NonNull
        public SearchSpec.Builder setPropertyWeightsForDocumentClass(
                @NonNull Class<?> documentClass,
                @NonNull Map<String, Double> propertyPathWeights) throws AppSearchException {
            Preconditions.checkNotNull(documentClass);
            DocumentClassFactoryRegistry registry = DocumentClassFactoryRegistry.getInstance();
            DocumentClassFactory<?> factory = registry.getOrCreateFactory(documentClass);
            return setPropertyWeights(factory.getSchemaName(), propertyPathWeights);
        }

        /**
         * Sets property weights by schema type and property path.
         *
         * <p>Property weights are used to promote and demote query term matches within a
         * {@link GenericDocument} property when applying scoring.
         *
         * <p>Property weights must be positive values (greater than 0). A property's weight is
         * multiplied with that property's scoring contribution. This means weights set between 0.0
         * and 1.0 demote scoring contributions by a term match within the property. Weights set
         * above 1.0 promote scoring contributions by a term match within the property.
         *
         * <p>Properties that exist in the {@link AppSearchSchema}, but do not have a weight
         * explicitly set will be given a default weight of 1.0.
         *
         * <p>Weights set for property paths that do not exist in the {@link AppSearchSchema} will
         * be discarded and not affect scoring.
         *
         * <p><b>NOTE:</b> Property weights only affect scoring for query-dependent scoring
         * strategies, such as {@link #RANKING_STRATEGY_RELEVANCE_SCORE}.
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         * <!--@exportToFramework:else()-->
         *
         * @param documentClass a class, annotated with @Document, corresponding to the schema to
         *                      set property weights for.
         * @param propertyPathWeights a {@link Map} of property paths of the schema type to the
         *                            weight to set for that property.
         * @throws AppSearchException if no factory for this document class could be found on the
         *                            classpath
         * @throws IllegalArgumentException if a weight is equal to or less than 0.0.
         */
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_SPEC_PROPERTY_WEIGHTS)
        @NonNull
        public SearchSpec.Builder setPropertyWeightPathsForDocumentClass(
                @NonNull Class<?> documentClass,
                @NonNull Map<PropertyPath, Double> propertyPathWeights) throws AppSearchException {
            Preconditions.checkNotNull(documentClass);
            DocumentClassFactoryRegistry registry = DocumentClassFactoryRegistry.getInstance();
            DocumentClassFactory<?> factory = registry.getOrCreateFactory(documentClass);
            return setPropertyWeightPaths(factory.getSchemaName(), propertyPathWeights);
        }
// @exportToFramework:endStrip()

        /**
         * Sets the {@link Features#NUMERIC_SEARCH} feature as enabled/disabled according to the
         * enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it.
         *
         * <p>If disabled, disallows use of
         * {@link AppSearchSchema.LongPropertyConfig#INDEXING_TYPE_RANGE} and all other numeric
         * querying features.
         */
        // @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.NUMERIC_SEARCH)
        // @exportToFramework:endStrip()
        @NonNull
        public Builder setNumericSearchEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.NUMERIC_SEARCH, enabled);
            return this;
        }

        /**
         * Sets the {@link Features#VERBATIM_SEARCH} feature as enabled/disabled according to the
         * enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it
         *
         * <p>If disabled, disallows use of
         * {@link AppSearchSchema.StringPropertyConfig#TOKENIZER_TYPE_VERBATIM} and all other
         * verbatim search features within the query language that allows clients to search
         * using the verbatim string operator.
         *
         * <p>Ex. The verbatim string operator '"foo/bar" OR baz' will ensure that 'foo/bar' is
         * treated as a single 'verbatim' token.
         */
        // @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.VERBATIM_SEARCH)
        // @exportToFramework:endStrip()
        @NonNull
        public Builder setVerbatimSearchEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.VERBATIM_SEARCH, enabled);
            return this;
        }

        /**
         * Sets the {@link Features#LIST_FILTER_QUERY_LANGUAGE} feature as enabled/disabled
         * according to the enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it.
         *
         * This feature covers the expansion of the query language to conform to the definition
         * of the list filters language (https://aip.dev/160). This includes:
         * <ul>
         * <li>addition of explicit 'AND' and 'NOT' operators</li>
         * <li>property restricts are allowed with grouping (ex. "prop:(a OR b)")</li>
         * <li>addition of custom functions to control matching</li>
         * </ul>
         *
         * <p>The newly added custom functions covered by this feature are:
         * <ul>
         * <li>createList(String...)</li>
         * <li>termSearch(String, List<String>)</li>
         * </ul>
         *
         * <p>createList takes a variable number of strings and returns a list of strings.
         * It is for use with termSearch.
         *
         * <p>termSearch takes a query string that will be parsed according to the supported
         * query language and an optional list of strings that specify the properties to be
         * restricted to. This exists as a convenience for multiple property restricts. So,
         * for example, the query "(subject:foo OR body:foo) (subject:bar OR body:bar)"
         * could be rewritten as "termSearch(\"foo bar\", createList(\"subject\", \"bar\"))"
         */
        // @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.LIST_FILTER_QUERY_LANGUAGE)
        // @exportToFramework:endStrip()
        @NonNull
        public Builder setListFilterQueryLanguageEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.LIST_FILTER_QUERY_LANGUAGE, enabled);
            return this;
        }

        /**
         * Constructs a new {@link SearchSpec} from the contents of this builder.
         *
         * @throws IllegalArgumentException if property weights are provided with a
         *                                  ranking strategy that isn't
         *                                  RANKING_STRATEGY_RELEVANCE_SCORE.
         * @throws IllegalStateException if the ranking strategy is
         * {@link #RANKING_STRATEGY_JOIN_AGGREGATE_SCORE} and {@link #setJoinSpec} has never been
         * called.
         * @throws IllegalStateException if the aggregation scoring strategy has been set in
         * {@link JoinSpec#getAggregationScoringStrategy()} but the ranking strategy is not
         * {@link #RANKING_STRATEGY_JOIN_AGGREGATE_SCORE}.
         *
         */
        @NonNull
        public SearchSpec build() {
            Bundle bundle = new Bundle();
            if (mJoinSpec != null) {
                if (mRankingStrategy != RANKING_STRATEGY_JOIN_AGGREGATE_SCORE
                        && mJoinSpec.getAggregationScoringStrategy()
                        != JoinSpec.AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL) {
                    throw new IllegalStateException("Aggregate scoring strategy has been set in "
                            + "the nested JoinSpec, but ranking strategy is not "
                            + "RANKING_STRATEGY_JOIN_AGGREGATE_SCORE");
                }
                bundle.putBundle(JOIN_SPEC, mJoinSpec.getBundle());
            } else if (mRankingStrategy == RANKING_STRATEGY_JOIN_AGGREGATE_SCORE) {
                throw new IllegalStateException("Attempting to rank based on joined documents, but "
                        + "no JoinSpec provided");
            }
            bundle.putStringArrayList(SCHEMA_FIELD, mSchemas);
            bundle.putStringArrayList(NAMESPACE_FIELD, mNamespaces);
            bundle.putStringArrayList(PACKAGE_NAME_FIELD, mPackageNames);
            bundle.putStringArrayList(ENABLED_FEATURES_FIELD, new ArrayList<>(mEnabledFeatures));
            bundle.putBundle(PROJECTION_TYPE_PROPERTY_PATHS_FIELD, mProjectionTypePropertyMasks);
            bundle.putInt(NUM_PER_PAGE_FIELD, mResultCountPerPage);
            bundle.putInt(TERM_MATCH_TYPE_FIELD, mTermMatchType);
            bundle.putInt(SNIPPET_COUNT_FIELD, mSnippetCount);
            bundle.putInt(SNIPPET_COUNT_PER_PROPERTY_FIELD, mSnippetCountPerProperty);
            bundle.putInt(MAX_SNIPPET_FIELD, mMaxSnippetSize);
            bundle.putInt(RANKING_STRATEGY_FIELD, mRankingStrategy);
            bundle.putInt(ORDER_FIELD, mOrder);
            bundle.putInt(RESULT_GROUPING_TYPE_FLAGS, mGroupingTypeFlags);
            bundle.putInt(RESULT_GROUPING_LIMIT, mGroupingLimit);
            if (!mTypePropertyWeights.isEmpty()
                    && RANKING_STRATEGY_RELEVANCE_SCORE != mRankingStrategy
                    && RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION != mRankingStrategy) {
                throw new IllegalArgumentException("Property weights are only compatible with the "
                        + "RANKING_STRATEGY_RELEVANCE_SCORE and "
                        + "RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION ranking strategies.");
            }
            bundle.putBundle(TYPE_PROPERTY_WEIGHTS_FIELD, mTypePropertyWeights);
            bundle.putString(ADVANCED_RANKING_EXPRESSION, mAdvancedRankingExpression);
            mBuilt = true;
            return new SearchSpec(bundle);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mSchemas = new ArrayList<>(mSchemas);
                mNamespaces = new ArrayList<>(mNamespaces);
                mPackageNames = new ArrayList<>(mPackageNames);
                mProjectionTypePropertyMasks = BundleUtil.deepCopy(mProjectionTypePropertyMasks);
                mTypePropertyWeights = BundleUtil.deepCopy(mTypePropertyWeights);
                mBuilt = false;
            }
        }

        private void modifyEnabledFeature(@NonNull String feature, boolean enabled) {
            resetIfBuilt();
            if (enabled) {
                mEnabledFeatures.add(feature);
            } else {
                mEnabledFeatures.remove(feature);
            }
        }
    }
}
