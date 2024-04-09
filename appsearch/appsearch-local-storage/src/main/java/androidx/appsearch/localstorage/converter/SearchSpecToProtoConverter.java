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

package androidx.appsearch.localstorage.converter;

import static androidx.appsearch.localstorage.util.PrefixUtil.createPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.getPackageName;
import static androidx.appsearch.localstorage.util.PrefixUtil.getPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.removePrefix;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.IcingOptionsConfig;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.localstorage.visibilitystore.VisibilityUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.JoinSpecProto;
import com.google.android.icing.proto.PropertyWeight;
import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.TermMatchType;
import com.google.android.icing.proto.TypePropertyMask;
import com.google.android.icing.proto.TypePropertyWeights;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates a {@link SearchSpec} into icing search protos.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SearchSpecToProtoConverter {
    private static final String TAG = "AppSearchSearchSpecConv";
    private final String mQueryExpression;
    private final SearchSpec mSearchSpec;
    /**
     * The union of allowed prefixes for the top-level SearchSpec and any nested SearchSpecs.
     */
    private final Set<String> mAllAllowedPrefixes;
    /**
     * The intersection of mAllAllowedPrefixes and prefixes requested in the SearchSpec currently
     * being handled.
     */
    private final Set<String> mCurrentSearchSpecPrefixFilters;
    /**
     * The intersected prefixed namespaces that are existing in AppSearch and also accessible to the
     * client.
     */
    private final Set<String> mTargetPrefixedNamespaceFilters;
    /**
     * The intersected prefixed schema types that are existing in AppSearch and also accessible to
     * the client.
     */
    private final Set<String> mTargetPrefixedSchemaFilters;

    /**
     * The cached Map of {@code <Prefix, Set<PrefixedNamespace>>} stores all prefixed namespace
     * filters which are stored in AppSearch. This is a field so that we can generate nested protos.
     */
    private final Map<String, Set<String>> mNamespaceMap;

    /**
     *The cached Map of {@code <Prefix, Map<PrefixedSchemaType, schemaProto>>} stores all
     * prefixed schema filters which are stored inAppSearch. This is a field so that we can
     * generated nested protos.
     */
    private final Map<String, Map<String, SchemaTypeConfigProto>> mSchemaMap;

    /**
     * Optional config flags in {@link SearchSpecProto}.
     */
    private final IcingOptionsConfig mIcingOptionsConfig;

    /**
     * The nested converter, which contains SearchSpec, ResultSpec, and ScoringSpec information
     * about the nested query. This will remain null if there is no nested {@link JoinSpec}.
     */
    @Nullable
    private SearchSpecToProtoConverter mNestedConverter = null;

    /**
     * Creates a {@link SearchSpecToProtoConverter} for given {@link SearchSpec}.
     *
     * @param queryExpression                Query String to search.
     * @param searchSpec    The spec we need to convert from.
     * @param allAllowedPrefixes Superset of database prefixes which the {@link SearchSpec} and all
     *                           nested SearchSpecs are allowed to access. An empty set means no
     *                           database prefixes are allowed, so nothing will be searched.
     * @param namespaceMap  The cached Map of {@code <Prefix, Set<PrefixedNamespace>>} stores
     *                      all prefixed namespace filters which are stored in AppSearch.
     * @param schemaMap     The cached Map of {@code <Prefix, Map<PrefixedSchemaType, schemaProto>>}
     *                      stores all prefixed schema filters which are stored inAppSearch.
     */
    public SearchSpecToProtoConverter(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @NonNull Set<String> allAllowedPrefixes,
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap,
            @NonNull IcingOptionsConfig icingOptionsConfig) {
        mQueryExpression = Preconditions.checkNotNull(queryExpression);
        mSearchSpec = Preconditions.checkNotNull(searchSpec);
        mAllAllowedPrefixes = Preconditions.checkNotNull(allAllowedPrefixes);
        mNamespaceMap = Preconditions.checkNotNull(namespaceMap);
        mSchemaMap = Preconditions.checkNotNull(schemaMap);
        mIcingOptionsConfig = Preconditions.checkNotNull(icingOptionsConfig);

        // This field holds the prefix filters for the SearchSpec currently being handled, which
        // could be an outer or inner SearchSpec. If this constructor is called from outside of
        // SearchSpecToProtoConverter, it will be handling an outer SearchSpec. If this SearchSpec
        // contains a JoinSpec, the nested SearchSpec will be handled in the creation of
        // mNestedConverter. This is useful as the two SearchSpecs could have different package
        // filters.
        List<String> packageFilters = searchSpec.getFilterPackageNames();
        if (packageFilters.isEmpty()) {
            mCurrentSearchSpecPrefixFilters = mAllAllowedPrefixes;
        } else {
            mCurrentSearchSpecPrefixFilters = new ArraySet<>();
            for (String prefix : mAllAllowedPrefixes) {
                String packageName = getPackageName(prefix);
                if (packageFilters.contains(packageName)) {
                    // This performs an intersection of allowedPrefixes with prefixes requested
                    // in the SearchSpec currently being handled. The same operation is done
                    // on the nested SearchSpecs when mNestedConverter is created.
                    mCurrentSearchSpecPrefixFilters.add(prefix);
                }
            }
        }

        mTargetPrefixedNamespaceFilters =
                SearchSpecToProtoConverterUtil.generateTargetNamespaceFilters(
                        mCurrentSearchSpecPrefixFilters, namespaceMap,
                        searchSpec.getFilterNamespaces());

        // If the target namespace filter is empty, the user has nothing to search for. We can skip
        // generate the target schema filter.
        if (!mTargetPrefixedNamespaceFilters.isEmpty()) {
            mTargetPrefixedSchemaFilters =
                    SearchSpecToProtoConverterUtil.generateTargetSchemaFilters(
                            mCurrentSearchSpecPrefixFilters, schemaMap,
                            searchSpec.getFilterSchemas());
        } else {
            mTargetPrefixedSchemaFilters = new ArraySet<>();
        }

        JoinSpec joinSpec = searchSpec.getJoinSpec();
        if (joinSpec == null) {
            return;
        }

        mNestedConverter = new SearchSpecToProtoConverter(
                joinSpec.getNestedQuery(),
                joinSpec.getNestedSearchSpec(),
                mAllAllowedPrefixes,
                namespaceMap,
                schemaMap,
                mIcingOptionsConfig);
    }

    /**
     * @return whether this search's target filters are empty. If any target filter is empty, we
     * should skip send request to Icing.
     *
     * <p> The nestedConverter is not checked as {@link SearchResult}s from the nested query have
     * to be joined to a {@link SearchResult} from the parent query. If the parent query has
     * nothing to search, then so does the child query.
     */
    public boolean hasNothingToSearch() {
        return mTargetPrefixedNamespaceFilters.isEmpty() || mTargetPrefixedSchemaFilters.isEmpty();
    }

    /**
     * For each target schema, we will check visibility store is that accessible to the caller. And
     * remove this schemas if it is not allowed for caller to query.
     *
     * @param callerAccess      Visibility access info of the calling app
     * @param visibilityStore   The {@link VisibilityStore} that store all visibility
     *                          information.
     * @param visibilityChecker Optional visibility checker to check whether the caller
     *                          could access target schemas. Pass {@code null} will
     *                          reject access for all documents which doesn't belong
     *                          to the calling package.
     */
    public void removeInaccessibleSchemaFilter(
            @NonNull CallerAccess callerAccess,
            @Nullable VisibilityStore visibilityStore,
            @Nullable VisibilityChecker visibilityChecker) {
        removeInaccessibleSchemaFilterCached(callerAccess, visibilityStore,
                /*inaccessibleSchemaPrefixes=*/new ArraySet<>(),
                /*accessibleSchemaPrefixes=*/new ArraySet<>(), visibilityChecker);
    }

    /**
     * For each target schema, we will check visibility store is that accessible to the caller. And
     * remove this schemas if it is not allowed for caller to query. This private version accepts
     * two additional parameters to minimize the amount of calls to
     * {@link VisibilityUtil#isSchemaSearchableByCaller}.
     *
     * @param callerAccess      Visibility access info of the calling app
     * @param visibilityStore   The {@link VisibilityStore} that store all visibility
     *                          information.
     * @param visibilityChecker Optional visibility checker to check whether the caller
     *                          could access target schemas. Pass {@code null} will
     *                          reject access for all documents which doesn't belong
     *                          to the calling package.
     * @param inaccessibleSchemaPrefixes A set of schemas that are known to be inaccessible. This
     *                                  is helpful for reducing duplicate calls to
     *                                  {@link VisibilityUtil}.
     * @param accessibleSchemaPrefixes A set of schemas that are known to be accessible. This is
     *                                 helpful for reducing duplicate calls to
     *                                 {@link VisibilityUtil}.
     */
    private void removeInaccessibleSchemaFilterCached(
            @NonNull CallerAccess callerAccess,
            @Nullable VisibilityStore visibilityStore,
            @NonNull Set<String> inaccessibleSchemaPrefixes,
            @NonNull Set<String> accessibleSchemaPrefixes,
            @Nullable VisibilityChecker visibilityChecker) {
        Iterator<String> targetPrefixedSchemaFilterIterator =
                mTargetPrefixedSchemaFilters.iterator();
        while (targetPrefixedSchemaFilterIterator.hasNext()) {
            String targetPrefixedSchemaFilter = targetPrefixedSchemaFilterIterator.next();
            String packageName = getPackageName(targetPrefixedSchemaFilter);

            if (accessibleSchemaPrefixes.contains(targetPrefixedSchemaFilter)) {
                continue;
            } else if (inaccessibleSchemaPrefixes.contains(targetPrefixedSchemaFilter)) {
                targetPrefixedSchemaFilterIterator.remove();
            } else if (!VisibilityUtil.isSchemaSearchableByCaller(
                    callerAccess,
                    packageName,
                    targetPrefixedSchemaFilter,
                    visibilityStore,
                    visibilityChecker)) {
                targetPrefixedSchemaFilterIterator.remove();
                inaccessibleSchemaPrefixes.add(targetPrefixedSchemaFilter);
            } else {
                accessibleSchemaPrefixes.add(targetPrefixedSchemaFilter);
            }
        }

        if (mNestedConverter != null) {
            mNestedConverter.removeInaccessibleSchemaFilterCached(
                    callerAccess, visibilityStore, inaccessibleSchemaPrefixes,
                    accessibleSchemaPrefixes, visibilityChecker);
        }
    }


    /** Extracts {@link SearchSpecProto} information from a {@link SearchSpec}. */
    @NonNull
    public SearchSpecProto toSearchSpecProto() {
        // set query to SearchSpecProto and override schema and namespace filter by
        // targetPrefixedFilters which contains all existing and also accessible to the caller
        // filters.
        SearchSpecProto.Builder protoBuilder = SearchSpecProto.newBuilder()
                .setQuery(mQueryExpression)
                .addAllNamespaceFilters(mTargetPrefixedNamespaceFilters)
                .addAllSchemaTypeFilters(mTargetPrefixedSchemaFilters)
                .setUseReadOnlySearch(mIcingOptionsConfig.getUseReadOnlySearch());

        // Convert type property filter map into type property mask proto.
        for (Map.Entry<String, List<String>> entry :
                mSearchSpec.getFilterProperties().entrySet()) {
            if (entry.getKey().equals(SearchSpec.SCHEMA_TYPE_WILDCARD)) {
                protoBuilder.addTypePropertyFilters(TypePropertyMask.newBuilder()
                        .setSchemaType(SearchSpec.SCHEMA_TYPE_WILDCARD)
                        .addAllPaths(entry.getValue())
                        .build());
            } else {
                for (String prefix : mCurrentSearchSpecPrefixFilters) {
                    String prefixedSchemaType = prefix + entry.getKey();
                    if (mTargetPrefixedSchemaFilters.contains(prefixedSchemaType)) {
                        protoBuilder.addTypePropertyFilters(TypePropertyMask.newBuilder()
                                .setSchemaType(prefixedSchemaType)
                                .addAllPaths(entry.getValue())
                                .build());
                    }
                }
            }
        }

        @SearchSpec.TermMatch int termMatchCode = mSearchSpec.getTermMatch();
        TermMatchType.Code termMatchCodeProto = TermMatchType.Code.forNumber(termMatchCode);
        if (termMatchCodeProto == null || termMatchCodeProto.equals(TermMatchType.Code.UNKNOWN)) {
            throw new IllegalArgumentException("Invalid term match type: " + termMatchCode);
        }
        protoBuilder.setTermMatchType(termMatchCodeProto);

        if (mNestedConverter != null && !mNestedConverter.hasNothingToSearch()) {
            JoinSpecProto.NestedSpecProto nestedSpec =
                    JoinSpecProto.NestedSpecProto.newBuilder()
                            .setResultSpec(mNestedConverter.toResultSpecProto(
                                    mNamespaceMap, mSchemaMap))
                            .setScoringSpec(mNestedConverter.toScoringSpecProto())
                            .setSearchSpec(mNestedConverter.toSearchSpecProto())
                            .build();

            // This cannot be null, otherwise mNestedConverter would be null as well.
            JoinSpec joinSpec = mSearchSpec.getJoinSpec();
            JoinSpecProto.Builder joinSpecProtoBuilder =
                    JoinSpecProto.newBuilder()
                            .setNestedSpec(nestedSpec)
                            .setParentPropertyExpression(JoinSpec.QUALIFIED_ID)
                            .setChildPropertyExpression(joinSpec.getChildPropertyExpression())
                            .setAggregationScoringStrategy(
                                    toAggregationScoringStrategy(
                                            joinSpec.getAggregationScoringStrategy()));

            protoBuilder.setJoinSpec(joinSpecProtoBuilder);
        }

        // TODO(b/208654892) Remove this field once EXPERIMENTAL_ICING_ADVANCED_QUERY is fully
        //  supported.
        boolean turnOnIcingAdvancedQuery =
                mSearchSpec.isNumericSearchEnabled() || mSearchSpec.isVerbatimSearchEnabled()
                        || mSearchSpec.isListFilterQueryLanguageEnabled();
        if (turnOnIcingAdvancedQuery) {
            protoBuilder.setSearchType(
                    SearchSpecProto.SearchType.Code.EXPERIMENTAL_ICING_ADVANCED_QUERY);
        }

        // Set enabled features
        protoBuilder.addAllEnabledFeatures(mSearchSpec.getEnabledFeatures());

        return protoBuilder.build();
    }

    /**
     * Helper to convert to JoinSpecProto.AggregationScore.
     *
     * <p> {@link JoinSpec#AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL} will be treated as
     * undefined, which is the default behavior.
     *
     * @param aggregationScoringStrategy the scoring strategy to convert.
     */
    @NonNull
    public static JoinSpecProto.AggregationScoringStrategy.Code toAggregationScoringStrategy(
            @JoinSpec.AggregationScoringStrategy int aggregationScoringStrategy) {
        switch (aggregationScoringStrategy) {
            case JoinSpec.AGGREGATION_SCORING_AVG_RANKING_SIGNAL:
                return JoinSpecProto.AggregationScoringStrategy.Code.AVG;
            case JoinSpec.AGGREGATION_SCORING_MIN_RANKING_SIGNAL:
                return JoinSpecProto.AggregationScoringStrategy.Code.MIN;
            case JoinSpec.AGGREGATION_SCORING_MAX_RANKING_SIGNAL:
                return JoinSpecProto.AggregationScoringStrategy.Code.MAX;
            case JoinSpec.AGGREGATION_SCORING_SUM_RANKING_SIGNAL:
                return JoinSpecProto.AggregationScoringStrategy.Code.SUM;
            case JoinSpec.AGGREGATION_SCORING_RESULT_COUNT:
                return JoinSpecProto.AggregationScoringStrategy.Code.COUNT;
            default:
                return JoinSpecProto.AggregationScoringStrategy.Code.NONE;
        }
    }

    /**
     * Extracts {@link ResultSpecProto} information from a {@link SearchSpec}.
     *
     * @param namespaceMap    The cached Map of {@code <Prefix, Set<PrefixedNamespace>>} stores
     *                        all existing prefixed namespace.
     * @param schemaMap       The cached Map of {@code <Prefix, Map<PrefixedSchemaType,
     *                        schemaProto>>} stores all prefixed schema filters which are stored
     *                        inAppSearch.
     */
    @NonNull
    public ResultSpecProto toResultSpecProto(
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap) {
        ResultSpecProto.Builder resultSpecBuilder = ResultSpecProto.newBuilder()
                .setNumPerPage(mSearchSpec.getResultCountPerPage())
                .setSnippetSpec(
                        ResultSpecProto.SnippetSpecProto.newBuilder()
                                .setNumToSnippet(mSearchSpec.getSnippetCount())
                                .setNumMatchesPerProperty(mSearchSpec.getSnippetCountPerProperty())
                                .setMaxWindowUtf32Length(mSearchSpec.getMaxSnippetSize()))
                .setNumTotalBytesPerPageThreshold(mIcingOptionsConfig.getMaxPageBytesLimit());
        JoinSpec joinSpec = mSearchSpec.getJoinSpec();
        if (joinSpec != null) {
            resultSpecBuilder.setMaxJoinedChildrenPerParentToReturn(
                    joinSpec.getMaxJoinedResultCount());
        }

        // Add result groupings for the available prefixes
        int groupingType = mSearchSpec.getResultGroupingTypeFlags();
        ResultSpecProto.ResultGroupingType resultGroupingType =
                ResultSpecProto.ResultGroupingType.NONE;
        switch (groupingType) {
            case SearchSpec.GROUPING_TYPE_PER_PACKAGE :
                addPerPackageResultGroupings(mCurrentSearchSpecPrefixFilters,
                        mSearchSpec.getResultGroupingLimit(), namespaceMap, resultSpecBuilder);
                resultGroupingType = ResultSpecProto.ResultGroupingType.NAMESPACE;
                break;
            case SearchSpec.GROUPING_TYPE_PER_NAMESPACE:
                addPerNamespaceResultGroupings(mCurrentSearchSpecPrefixFilters,
                        mSearchSpec.getResultGroupingLimit(), namespaceMap, resultSpecBuilder);
                resultGroupingType = ResultSpecProto.ResultGroupingType.NAMESPACE;
                break;
            case SearchSpec.GROUPING_TYPE_PER_SCHEMA:
                addPerSchemaResultGrouping(mCurrentSearchSpecPrefixFilters,
                        mSearchSpec.getResultGroupingLimit(), schemaMap, resultSpecBuilder);
                resultGroupingType = ResultSpecProto.ResultGroupingType.SCHEMA_TYPE;
                break;
            case SearchSpec.GROUPING_TYPE_PER_PACKAGE | SearchSpec.GROUPING_TYPE_PER_NAMESPACE:
                addPerPackagePerNamespaceResultGroupings(mCurrentSearchSpecPrefixFilters,
                        mSearchSpec.getResultGroupingLimit(),
                        namespaceMap, resultSpecBuilder);
                resultGroupingType = ResultSpecProto.ResultGroupingType.NAMESPACE;
                break;
            case SearchSpec.GROUPING_TYPE_PER_PACKAGE | SearchSpec.GROUPING_TYPE_PER_SCHEMA:
                addPerPackagePerSchemaResultGroupings(mCurrentSearchSpecPrefixFilters,
                        mSearchSpec.getResultGroupingLimit(),
                        schemaMap, resultSpecBuilder);
                resultGroupingType = ResultSpecProto.ResultGroupingType.SCHEMA_TYPE;
                break;
            case SearchSpec.GROUPING_TYPE_PER_NAMESPACE | SearchSpec.GROUPING_TYPE_PER_SCHEMA:
                addPerNamespaceAndSchemaResultGrouping(mCurrentSearchSpecPrefixFilters,
                        mSearchSpec.getResultGroupingLimit(),
                        namespaceMap, schemaMap, resultSpecBuilder);
                resultGroupingType = ResultSpecProto.ResultGroupingType.NAMESPACE_AND_SCHEMA_TYPE;
                break;
            case SearchSpec.GROUPING_TYPE_PER_PACKAGE | SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                | SearchSpec.GROUPING_TYPE_PER_SCHEMA:
                addPerPackagePerNamespacePerSchemaResultGrouping(mCurrentSearchSpecPrefixFilters,
                        mSearchSpec.getResultGroupingLimit(),
                        namespaceMap, schemaMap, resultSpecBuilder);
                resultGroupingType = ResultSpecProto.ResultGroupingType.NAMESPACE_AND_SCHEMA_TYPE;
                break;
            default:
                break;
        }
        resultSpecBuilder.setResultGroupType(resultGroupingType);

        List<TypePropertyMask.Builder> typePropertyMaskBuilders =
                TypePropertyPathToProtoConverter
                        .toTypePropertyMaskBuilderList(mSearchSpec.getProjections());
        // Rewrite filters to include a database prefix.
        for (int i = 0; i < typePropertyMaskBuilders.size(); i++) {
            String unprefixedType = typePropertyMaskBuilders.get(i).getSchemaType();
            if (unprefixedType.equals(SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD)) {
                resultSpecBuilder.addTypePropertyMasks(typePropertyMaskBuilders.get(i).build());
            } else {
                // Qualify the given schema types
                for (String prefix : mCurrentSearchSpecPrefixFilters) {
                    String prefixedType = prefix + unprefixedType;
                    if (mTargetPrefixedSchemaFilters.contains(prefixedType)) {
                        resultSpecBuilder.addTypePropertyMasks(typePropertyMaskBuilders.get(i)
                                .setSchemaType(prefixedType).build());
                    }
                }
            }
        }

        return resultSpecBuilder.build();
    }

    /** Extracts {@link ScoringSpecProto} information from a {@link SearchSpec}. */
    @NonNull
    public ScoringSpecProto toScoringSpecProto() {
        ScoringSpecProto.Builder protoBuilder = ScoringSpecProto.newBuilder();

        @SearchSpec.Order int orderCode = mSearchSpec.getOrder();
        ScoringSpecProto.Order.Code orderCodeProto =
                ScoringSpecProto.Order.Code.forNumber(orderCode);
        if (orderCodeProto == null) {
            throw new IllegalArgumentException("Invalid result ranking order: " + orderCode);
        }
        protoBuilder.setOrderBy(orderCodeProto).setRankBy(
                toProtoRankingStrategy(mSearchSpec.getRankingStrategy()));

        addTypePropertyWeights(mSearchSpec.getPropertyWeights(), protoBuilder);

        protoBuilder.setAdvancedScoringExpression(mSearchSpec.getAdvancedRankingExpression());

        return protoBuilder.build();
    }

    private static ScoringSpecProto.RankingStrategy.Code toProtoRankingStrategy(
            @SearchSpec.RankingStrategy int rankingStrategyCode) {
        switch (rankingStrategyCode) {
            case SearchSpec.RANKING_STRATEGY_NONE:
                return ScoringSpecProto.RankingStrategy.Code.NONE;
            case SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE:
                return ScoringSpecProto.RankingStrategy.Code.DOCUMENT_SCORE;
            case SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP:
                return ScoringSpecProto.RankingStrategy.Code.CREATION_TIMESTAMP;
            case SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE:
                return ScoringSpecProto.RankingStrategy.Code.RELEVANCE_SCORE;
            case SearchSpec.RANKING_STRATEGY_USAGE_COUNT:
                return ScoringSpecProto.RankingStrategy.Code.USAGE_TYPE1_COUNT;
            case SearchSpec.RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP:
                return ScoringSpecProto.RankingStrategy.Code.USAGE_TYPE1_LAST_USED_TIMESTAMP;
            case SearchSpec.RANKING_STRATEGY_SYSTEM_USAGE_COUNT:
                return ScoringSpecProto.RankingStrategy.Code.USAGE_TYPE2_COUNT;
            case SearchSpec.RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP:
                return ScoringSpecProto.RankingStrategy.Code.USAGE_TYPE2_LAST_USED_TIMESTAMP;
            case SearchSpec.RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION:
                return ScoringSpecProto.RankingStrategy.Code.ADVANCED_SCORING_EXPRESSION;
            case SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE:
                return ScoringSpecProto.RankingStrategy.Code.JOIN_AGGREGATE_SCORE;
            default:
                throw new IllegalArgumentException("Invalid result ranking strategy: "
                        + rankingStrategyCode);
        }
    }

    /**
     * Returns a Map of namespace to prefixedNamespaces. This is NOT necessarily the
     * same as the list of namespaces. If a namespace exists under different packages and/or
     * different databases, they should still be grouped together.
     *
     * @param prefixes          Prefixes that we should prepend to all our filters.
     * @param namespaceMap      The namespace map contains all prefixed existing namespaces.
     */
    private static Map<String, List<String>> getNamespaceToPrefixedNamespaces(
            @NonNull Set<String> prefixes,
            @NonNull Map<String, Set<String>> namespaceMap) {
        Map<String, List<String>> namespaceToPrefixedNamespaces = new ArrayMap<>();
        for (String prefix : prefixes) {
            Set<String> prefixedNamespaces = namespaceMap.get(prefix);
            if (prefixedNamespaces == null) {
                continue;
            }
            for (String prefixedNamespace : prefixedNamespaces) {
                String namespace;
                try {
                    namespace = removePrefix(prefixedNamespace);
                } catch (AppSearchException e) {
                    // This should never happen. Skip this namespace if it does.
                    Log.e(TAG, "Prefixed namespace " + prefixedNamespace + " is malformed.");
                    continue;
                }
                List<String> groupedPrefixedNamespaces =
                        namespaceToPrefixedNamespaces.get(namespace);
                if (groupedPrefixedNamespaces == null) {
                    groupedPrefixedNamespaces = new ArrayList<>();
                    namespaceToPrefixedNamespaces.put(namespace, groupedPrefixedNamespaces);
                }
                groupedPrefixedNamespaces.add(prefixedNamespace);
            }
        }
        return namespaceToPrefixedNamespaces;
    }

    /**
     * Returns a map for package+namespace to prefixedNamespaces. This is NOT necessarily the
     * same as the list of namespaces. If one package has multiple databases, each with the same
     * namespace, then those should be grouped together.
     *
     * @param prefixes          Prefixes that we should prepend to all our filters.
     * @param namespaceMap      The namespace map contains all prefixed existing namespaces.
     */
    private static Map<String, List<String>> getPackageAndNamespaceToPrefixedNamespaces(
            @NonNull Set<String> prefixes,
            @NonNull Map<String, Set<String>> namespaceMap) {
        Map<String, List<String>> packageAndNamespaceToNamespaces = new ArrayMap<>();
        for (String prefix : prefixes) {
            Set<String> prefixedNamespaces = namespaceMap.get(prefix);
            if (prefixedNamespaces == null) {
                continue;
            }
            String packageName = getPackageName(prefix);
            // Create a new prefix without the database name. This will allow us to group namespaces
            // that have the same name and package but a different database name together.
            String emptyDatabasePrefix = createPrefix(packageName, /*databaseName*/"");
            for (String prefixedNamespace : prefixedNamespaces) {
                String namespace;
                try {
                    namespace = removePrefix(prefixedNamespace);
                } catch (AppSearchException e) {
                    // This should never happen. Skip this namespace if it does.
                    Log.e(TAG, "Prefixed namespace " + prefixedNamespace + " is malformed.");
                    continue;
                }
                String emptyDatabasePrefixedNamespace = emptyDatabasePrefix + namespace;
                List<String> namespaceList =
                        packageAndNamespaceToNamespaces.get(emptyDatabasePrefixedNamespace);
                if (namespaceList == null) {
                    namespaceList = new ArrayList<>();
                    packageAndNamespaceToNamespaces.put(emptyDatabasePrefixedNamespace,
                            namespaceList);
                }
                namespaceList.add(prefixedNamespace);
            }
        }
        return packageAndNamespaceToNamespaces;
    }

    /**
     * Returns a map of schema to prefixedSchemas. This is NOT necessarily the
     * same as the list of schemas. If a schema exists under different packages and/or
     * different databases, they should still be grouped together.
     *
     * @param prefixes      Prefixes that we should prepend to all our filters.
     * @param schemaMap     The schema map contains all prefixed existing schema types.
     */
    private static Map<String, List<String>> getSchemaToPrefixedSchemas(
            @NonNull Set<String> prefixes,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap) {
        Map<String, List<String>> schemaToPrefixedSchemas = new ArrayMap<>();
        for (String prefix : prefixes) {
            Map<String, SchemaTypeConfigProto> prefixedSchemas = schemaMap.get(prefix);
            if (prefixedSchemas == null) {
                continue;
            }
            for (String prefixedSchema : prefixedSchemas.keySet()) {
                String schema;
                try {
                    schema = removePrefix(prefixedSchema);
                } catch (AppSearchException e) {
                    // This should never happen. Skip this schema if it does.
                    Log.e(TAG, "Prefixed schema " + prefixedSchema + " is malformed.");
                    continue;
                }
                List<String> groupedPrefixedSchemas =
                        schemaToPrefixedSchemas.get(schema);
                if (groupedPrefixedSchemas == null) {
                    groupedPrefixedSchemas = new ArrayList<>();
                    schemaToPrefixedSchemas.put(schema, groupedPrefixedSchemas);
                }
                groupedPrefixedSchemas.add(prefixedSchema);
            }
        }
        return schemaToPrefixedSchemas;
    }

    /**
     * Returns a map for package+schema to prefixedSchemas. This is NOT necessarily the
     * same as the list of schemas. If one package has multiple databases, each with the same
     * schema, then those should be grouped together.
     *
     * @param prefixes      Prefixes that we should prepend to all our filters.
     * @param schemaMap     The schema map contains all prefixed existing schema types.
     */
    private static Map<String, List<String>> getPackageAndSchemaToPrefixedSchemas(
            @NonNull Set<String> prefixes,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap) {
        Map<String, List<String>> packageAndSchemaToSchemas = new ArrayMap<>();
        for (String prefix : prefixes) {
            Map<String, SchemaTypeConfigProto> prefixedSchemas = schemaMap.get(prefix);
            if (prefixedSchemas == null) {
                continue;
            }
            String packageName = getPackageName(prefix);
            // Create a new prefix without the database name. This will allow us to group schemas
            // that have the same name and package but a different database name together.
            String emptyDatabasePrefix = createPrefix(packageName, /*database*/"");
            for (String prefixedSchema : prefixedSchemas.keySet()) {
                String schema;
                try {
                    schema = removePrefix(prefixedSchema);
                } catch (AppSearchException e) {
                    // This should never happen. Skip this schema if it does.
                    Log.e(TAG, "Prefixed schema " + prefixedSchema + " is malformed.");
                    continue;
                }
                String emptyDatabasePrefixedSchema = emptyDatabasePrefix + schema;
                List<String> schemaList =
                        packageAndSchemaToSchemas.get(emptyDatabasePrefixedSchema);
                if (schemaList == null) {
                    schemaList = new ArrayList<>();
                    packageAndSchemaToSchemas.put(emptyDatabasePrefixedSchema, schemaList);
                }
                schemaList.add(prefixedSchema);
            }
        }
        return packageAndSchemaToSchemas;
    }

    /**
     * Adds result groupings for each namespace in each package being queried for.
     *
     * @param prefixes          Prefixes that we should prepend to all our filters
     * @param maxNumResults     The maximum number of results for each grouping to support.
     * @param namespaceMap      The namespace map contains all prefixed existing namespaces.
     * @param resultSpecBuilder ResultSpecs as specified by client
     */
    private static void addPerPackagePerNamespaceResultGroupings(
            @NonNull Set<String> prefixes,
            int maxNumResults,
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull ResultSpecProto.Builder resultSpecBuilder) {
        Map<String, List<String>> packageAndNamespaceToNamespaces =
                getPackageAndNamespaceToPrefixedNamespaces(prefixes, namespaceMap);

        for (List<String> prefixedNamespaces : packageAndNamespaceToNamespaces.values()) {
            List<ResultSpecProto.ResultGrouping.Entry> entries =
                    new ArrayList<>(prefixedNamespaces.size());
            for (int i = 0; i < prefixedNamespaces.size(); i++) {
                entries.add(
                        ResultSpecProto.ResultGrouping.Entry.newBuilder()
                            .setNamespace(prefixedNamespaces.get(i)).build());
            }
            resultSpecBuilder.addResultGroupings(
                    ResultSpecProto.ResultGrouping.newBuilder()
                            .addAllEntryGroupings(entries).setMaxResults(maxNumResults));
        }
    }

    /**
     * Adds result groupings for each schema type in each package being queried for.
     *
     * @param prefixes          Prefixes that we should prepend to all our filters.
     * @param maxNumResults     The maximum number of results for each grouping to support.
     * @param schemaMap         The schema map contains all prefixed existing schema types.
     * @param resultSpecBuilder ResultSpecs as a specified by client.
     */
    private static void addPerPackagePerSchemaResultGroupings(
            @NonNull Set<String> prefixes,
            int maxNumResults,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap,
            @NonNull ResultSpecProto.Builder resultSpecBuilder) {
        Map<String, List<String>> packageAndSchemaToSchemas =
                getPackageAndSchemaToPrefixedSchemas(prefixes, schemaMap);

        for (List<String> prefixedSchemas : packageAndSchemaToSchemas.values()) {
            List<ResultSpecProto.ResultGrouping.Entry> entries =
                    new ArrayList<>(prefixedSchemas.size());
            for (int i = 0; i < prefixedSchemas.size(); i++) {
                entries.add(
                        ResultSpecProto.ResultGrouping.Entry.newBuilder()
                            .setSchema(prefixedSchemas.get(i)).build());
            }
            resultSpecBuilder.addResultGroupings(
                    ResultSpecProto.ResultGrouping.newBuilder()
                            .addAllEntryGroupings(entries).setMaxResults(maxNumResults));
        }
    }

    /**
     * Adds result groupings for each namespace and schema type being queried for.
     *
     * @param prefixes          Prefixes that we should prepend to all our filters.
     * @param maxNumResults     The maximum number of results for each grouping to support.
     * @param namespaceMap      The namespace map contains all prefixed existing namespaces.
     * @param schemaMap         The schema map contains all prefixed existing schema types.
     * @param resultSpecBuilder ResultSpec as specified by client.
     */
    private static void addPerPackagePerNamespacePerSchemaResultGrouping(
            @NonNull Set<String> prefixes,
            int maxNumResults,
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap,
            @NonNull ResultSpecProto.Builder resultSpecBuilder) {
        Map<String, List<String>> packageAndNamespaceToNamespaces =
                getPackageAndNamespaceToPrefixedNamespaces(prefixes, namespaceMap);
        Map<String, List<String>> packageAndSchemaToSchemas =
                getPackageAndSchemaToPrefixedSchemas(prefixes, schemaMap);

        for (List<String> prefixedNamespaces : packageAndNamespaceToNamespaces.values()) {
            for (List<String> prefixedSchemas : packageAndSchemaToSchemas.values()) {
                List<ResultSpecProto.ResultGrouping.Entry> entries =
                        new ArrayList<>(prefixedNamespaces.size() * prefixedSchemas.size());
                // Iterate through all namespaces.
                for (int i = 0; i < prefixedNamespaces.size(); i++) {
                    String namespacePackage = getPackageName(prefixedNamespaces.get(i));
                    // Iterate through all schemas.
                    for (int j = 0; j < prefixedSchemas.size(); j++) {
                        String schemaPackage = getPackageName(prefixedSchemas.get(j));
                        if (namespacePackage.equals(schemaPackage)) {
                            entries.add(
                                    ResultSpecProto.ResultGrouping.Entry.newBuilder()
                                        .setNamespace(prefixedNamespaces.get(i))
                                        .setSchema(prefixedSchemas.get(j))
                                        .build());
                        }
                    }
                }
                if (entries.size() > 0) {
                    resultSpecBuilder.addResultGroupings(
                            ResultSpecProto.ResultGrouping.newBuilder()
                                .addAllEntryGroupings(entries).setMaxResults(maxNumResults));
                }
            }
        }
    }

    /**
     * Adds result groupings for each package being queried for.
     *
     * @param prefixes          Prefixes that we should prepend to all our filters
     * @param maxNumResults     The maximum number of results for each grouping to support.
     * @param namespaceMap      The namespace map contains all prefixed existing namespaces.
     * @param resultSpecBuilder ResultSpecs as specified by client
     */
    private static void addPerPackageResultGroupings(
            @NonNull Set<String> prefixes,
            int maxNumResults,
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull ResultSpecProto.Builder resultSpecBuilder) {
        // Build up a map of package to namespaces.
        Map<String, List<String>> packageToNamespacesMap = new ArrayMap<>();
        for (String prefix : prefixes) {
            Set<String> prefixedNamespaces = namespaceMap.get(prefix);
            if (prefixedNamespaces == null) {
                continue;
            }
            String packageName = getPackageName(prefix);
            List<String> packageNamespaceList = packageToNamespacesMap.get(packageName);
            if (packageNamespaceList == null) {
                packageNamespaceList = new ArrayList<>();
                packageToNamespacesMap.put(packageName, packageNamespaceList);
            }
            packageNamespaceList.addAll(prefixedNamespaces);
        }

        for (List<String> prefixedNamespaces : packageToNamespacesMap.values()) {
            List<ResultSpecProto.ResultGrouping.Entry> entries =
                    new ArrayList<>(prefixedNamespaces.size());
            for (String namespace : prefixedNamespaces) {
                entries.add(
                        ResultSpecProto.ResultGrouping.Entry.newBuilder()
                                .setNamespace(namespace).build());
            }
            resultSpecBuilder.addResultGroupings(
                    ResultSpecProto.ResultGrouping.newBuilder()
                            .addAllEntryGroupings(entries).setMaxResults(maxNumResults));
        }
    }

    /**
     * Adds result groupings for each namespace being queried for.
     *
     * @param prefixes          Prefixes that we should prepend to all our filters
     * @param maxNumResults     The maximum number of results for each grouping to support.
     * @param namespaceMap      The namespace map contains all prefixed existing namespaces.
     * @param resultSpecBuilder ResultSpecs as specified by client
     */
    private static void addPerNamespaceResultGroupings(
            @NonNull Set<String> prefixes,
            int maxNumResults,
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull ResultSpecProto.Builder resultSpecBuilder) {
        Map<String, List<String>> namespaceToPrefixedNamespaces =
                getNamespaceToPrefixedNamespaces(prefixes, namespaceMap);

        for (List<String> prefixedNamespaces : namespaceToPrefixedNamespaces.values()) {
            List<ResultSpecProto.ResultGrouping.Entry> entries =
                    new ArrayList<>(prefixedNamespaces.size());
            for (int i = 0; i < prefixedNamespaces.size(); i++) {
                entries.add(
                        ResultSpecProto.ResultGrouping.Entry.newBuilder()
                            .setNamespace(prefixedNamespaces.get(i)).build());
            }
            resultSpecBuilder.addResultGroupings(
                    ResultSpecProto.ResultGrouping.newBuilder()
                            .addAllEntryGroupings(entries).setMaxResults(maxNumResults));
        }
    }

    /**
     * Adds result groupings for each schema type being queried for.
     *
     * @param prefixes          Prefixes that we should prepend to all our filters.
     * @param maxNumResults     The maximum number of results for each grouping to support.
     * @param schemaMap         The schema map contains all prefixed existing schema types.
     * @param resultSpecBuilder ResultSpec as specified by client.
     */
    private static void addPerSchemaResultGrouping(
            @NonNull Set<String> prefixes,
            int maxNumResults,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap,
            @NonNull ResultSpecProto.Builder resultSpecBuilder) {
        Map<String, List<String>> schemaToPrefixedSchemas =
                getSchemaToPrefixedSchemas(prefixes, schemaMap);

        for (List<String> prefixedSchemas : schemaToPrefixedSchemas.values()) {
            List<ResultSpecProto.ResultGrouping.Entry> entries =
                    new ArrayList<>(prefixedSchemas.size());
            for (int i = 0; i < prefixedSchemas.size(); i++) {
                entries.add(
                        ResultSpecProto.ResultGrouping.Entry.newBuilder()
                            .setSchema(prefixedSchemas.get(i)).build());
            }
            resultSpecBuilder.addResultGroupings(
                    ResultSpecProto.ResultGrouping.newBuilder()
                            .addAllEntryGroupings(entries).setMaxResults(maxNumResults));
        }
    }

    /**
     * Adds result groupings for each namespace and schema type being queried for.
     *
     * @param prefixes          Prefixes that we should prepend to all our filters.
     * @param maxNumResults     The maximum number of results for each grouping to support.
     * @param namespaceMap      The namespace map contains all prefixed existing namespaces.
     * @param schemaMap         The schema map contains all prefixed existing schema types.
     * @param resultSpecBuilder ResultSpec as specified by client.
     */
    private static void addPerNamespaceAndSchemaResultGrouping(
            @NonNull Set<String> prefixes,
            int maxNumResults,
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap,
            @NonNull ResultSpecProto.Builder resultSpecBuilder) {
        Map<String, List<String>> namespaceToPrefixedNamespaces =
                getNamespaceToPrefixedNamespaces(prefixes, namespaceMap);
        Map<String, List<String>> schemaToPrefixedSchemas =
                getSchemaToPrefixedSchemas(prefixes, schemaMap);

        for (List<String> prefixedNamespaces : namespaceToPrefixedNamespaces.values()) {
            for (List<String> prefixedSchemas : schemaToPrefixedSchemas.values()) {
                List<ResultSpecProto.ResultGrouping.Entry> entries =
                        new ArrayList<>(prefixedNamespaces.size() * prefixedSchemas.size());
                // Iterate through all namespaces.
                for (int i = 0; i < prefixedNamespaces.size(); i++) {
                    // Iterate through all schemas.
                    for (int j = 0; j < prefixedSchemas.size(); j++) {
                        try {
                            if (getPrefix(prefixedNamespaces.get(i))
                                    .equals(getPrefix(prefixedSchemas.get(j)))) {
                                entries.add(
                                                ResultSpecProto.ResultGrouping.Entry.newBuilder()
                                                .setNamespace(prefixedNamespaces.get(i))
                                                .setSchema(prefixedSchemas.get(j))
                                                .build());
                            }
                        } catch (AppSearchException e) {
                            // This should never happen. Skip this schema if it does.
                            Log.e(TAG, "Prefixed string " + prefixedNamespaces.get(i) + " or "
                                    + prefixedSchemas.get(j) + " is malformed.");
                            continue;
                        }
                    }
                }
                if (entries.size() > 0) {
                    resultSpecBuilder.addResultGroupings(
                            ResultSpecProto.ResultGrouping.newBuilder()
                                .addAllEntryGroupings(entries).setMaxResults(maxNumResults));
                }
            }
        }
    }

    /**
     * Adds {@link TypePropertyWeights} to {@link ScoringSpecProto}.
     *
     * <p>{@link TypePropertyWeights} are added to the {@link ScoringSpecProto} with database and
     * package prefixing added to the schema type.
     *
     * @param typePropertyWeightsMap a map from unprefixed schema type to an inner-map of property
     *                               paths to weight.
     * @param scoringSpecBuilder     scoring spec to add weights to.
     */
    private void addTypePropertyWeights(
            @NonNull Map<String, Map<String, Double>> typePropertyWeightsMap,
            @NonNull ScoringSpecProto.Builder scoringSpecBuilder) {
        Preconditions.checkNotNull(scoringSpecBuilder);
        Preconditions.checkNotNull(typePropertyWeightsMap);

        for (Map.Entry<String, Map<String, Double>> typePropertyWeight :
                typePropertyWeightsMap.entrySet()) {
            for (String prefix : mCurrentSearchSpecPrefixFilters) {
                String prefixedSchemaType = prefix + typePropertyWeight.getKey();
                if (mTargetPrefixedSchemaFilters.contains(prefixedSchemaType)) {
                    TypePropertyWeights.Builder typePropertyWeightsBuilder =
                            TypePropertyWeights.newBuilder().setSchemaType(prefixedSchemaType);

                    for (Map.Entry<String, Double> propertyWeight :
                            typePropertyWeight.getValue().entrySet()) {
                        typePropertyWeightsBuilder.addPropertyWeights(
                                PropertyWeight.newBuilder().setPath(
                                        propertyWeight.getKey()).setWeight(
                                        propertyWeight.getValue()));
                    }

                    scoringSpecBuilder.addTypePropertyWeights(typePropertyWeightsBuilder);
                }
            }
        }
    }
}
