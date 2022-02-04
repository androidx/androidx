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
import static androidx.appsearch.localstorage.util.PrefixUtil.removePrefix;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.localstorage.visibilitystore.VisibilityUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.TermMatchType;
import com.google.android.icing.proto.TypePropertyMask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates a {@link SearchSpec} into icing search protos.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SearchSpecToProtoConverter {
    private static final String TAG = "AppSearchSearchSpecConv";
    private final SearchSpec mSearchSpec;
    private final Set<String> mPrefixes;
    /** Prefixed namespaces that the client is allowed to query over */
    private final Set<String> mTargetPrefixedNamespaceFilters = new ArraySet<>();
    /** Prefixed schemas that the client is allowed to query over */
    private final Set<String> mTargetPrefixedSchemaFilters = new ArraySet<>();

    /**
     * Creates a {@link SearchSpecToProtoConverter} for given {@link SearchSpec}.
     *
     * @param searchSpec    The spec we need to convert from.
     * @param prefixes      Set of database prefix which the caller want to access.
     * @param namespaceMap  The cached Map of {@code <Prefix, Set<PrefixedNamespace>>} stores
     *                      all prefixed namespace filters which are stored in AppSearch.
     * @param schemaMap     The cached Map of {@code <Prefix, Map<PrefixedSchemaType, schemaProto>>}
     *                      stores all prefixed schema filters which are stored inAppSearch.
     */
    public SearchSpecToProtoConverter(@NonNull SearchSpec searchSpec,
            @NonNull Set<String> prefixes,
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap) {
        mSearchSpec = Preconditions.checkNotNull(searchSpec);
        mPrefixes = Preconditions.checkNotNull(prefixes);
        Preconditions.checkNotNull(namespaceMap);
        Preconditions.checkNotNull(schemaMap);
        generateTargetNamespaceFilters(namespaceMap);
        if (!mTargetPrefixedNamespaceFilters.isEmpty()) {
            // Skip generate the target schema filter if the target namespace filter is empty. We
            // have nothing to search anyway.
            generateTargetSchemaFilters(schemaMap);
        }
    }

    /**
     * Add prefix to the given namespace filters that user want to search over and find the
     * intersection set with those prefixed namespace candidates that are stored in AppSearch.
     *
     * @param namespaceMap   The cached Map of {@code <Prefix, Set<PrefixedNamespace>>} stores
     *                       all prefixed namespace filters which are stored in AppSearch.
     */
    private void generateTargetNamespaceFilters(
            @NonNull Map<String, Set<String>> namespaceMap) {
        // Convert namespace filters to prefixed namespace filters
        for (String prefix : mPrefixes) {
            // Step1: find all prefixed namespace candidates that are stored in AppSearch.
            Set<String> prefixedNamespaceCandidates = namespaceMap.get(prefix);
            if (prefixedNamespaceCandidates == null) {
                // This is should never happen. All prefixes should be verified before reach
                // here.
                continue;
            }
            // Step2: get the intersection of user searching filters and those candidates which are
            // stored in AppSearch.
            getIntersectedFilters(prefix, prefixedNamespaceCandidates,
                    mSearchSpec.getFilterNamespaces(), mTargetPrefixedNamespaceFilters);
        }
    }

    /**
     * Add prefix to the given schema filters that user want to search over and find the
     * intersection set with those prefixed schema candidates that are stored in AppSearch.
     *
     * @param schemaMap              The cached Map of
     *                               <Prefix, Map<PrefixedSchemaType, schemaProto>>
     *                               stores all prefixed schema filters which are stored in
     *                               AppSearch.
     */
    private void generateTargetSchemaFilters(
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap) {
        // Append prefix to input schema filters and get the intersection of existing schema filter.
        for (String prefix : mPrefixes) {
            // Step1: find all prefixed schema candidates that are stored in AppSearch.
            Map<String, SchemaTypeConfigProto> prefixedSchemaMap = schemaMap.get(prefix);
            if (prefixedSchemaMap == null) {
                // This is should never happen. All prefixes should be verified before reach
                // here.
                continue;
            }
            Set<String> prefixedSchemaCandidates = prefixedSchemaMap.keySet();
            // Step2: get the intersection of user searching filters and those candidates which are
            // stored in AppSearch.
            getIntersectedFilters(prefix, prefixedSchemaCandidates, mSearchSpec.getFilterSchemas(),
                    mTargetPrefixedSchemaFilters);
        }
    }

    /**
     * @return whether this search's target filters are empty. If any target filter is empty, we
     * should skip send request to Icing.
     */
    public boolean isNothingToSearch() {
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
        Iterator<String> targetPrefixedSchemaFilterIterator =
                mTargetPrefixedSchemaFilters.iterator();
        while (targetPrefixedSchemaFilterIterator.hasNext()) {
            String targetPrefixedSchemaFilter = targetPrefixedSchemaFilterIterator.next();
            String packageName = getPackageName(targetPrefixedSchemaFilter);

            if (!VisibilityUtil.isSchemaSearchableByCaller(
                    callerAccess,
                    packageName,
                    targetPrefixedSchemaFilter,
                    visibilityStore,
                    visibilityChecker)) {
                targetPrefixedSchemaFilterIterator.remove();
            }
        }
    }

    /**
     * Extracts {@link SearchSpecProto} information from a {@link SearchSpec}.
     *
     * @param queryExpression                Query String to search.
     */
    @NonNull
    public SearchSpecProto toSearchSpecProto(
            @NonNull String queryExpression) {
        Preconditions.checkNotNull(queryExpression);

        // set query to SearchSpecProto and override schema and namespace filter by
        // targetPrefixedFilters which is
        SearchSpecProto.Builder protoBuilder = SearchSpecProto.newBuilder()
                .setQuery(queryExpression)
                .addAllNamespaceFilters(mTargetPrefixedNamespaceFilters)
                .addAllSchemaTypeFilters(mTargetPrefixedSchemaFilters);

        @SearchSpec.TermMatch int termMatchCode = mSearchSpec.getTermMatch();
        TermMatchType.Code termMatchCodeProto = TermMatchType.Code.forNumber(termMatchCode);
        if (termMatchCodeProto == null || termMatchCodeProto.equals(TermMatchType.Code.UNKNOWN)) {
            throw new IllegalArgumentException("Invalid term match type: " + termMatchCode);
        }
        protoBuilder.setTermMatchType(termMatchCodeProto);

        return protoBuilder.build();
    }

    /**
     * Extracts {@link ResultSpecProto} information from a {@link SearchSpec}.
     *
     * @param namespaceMap    The cached Map of {@code <Prefix, Set<PrefixedNamespace>>} stores
     *                        all existing prefixed namespace.
     */
    @NonNull
    public ResultSpecProto toResultSpecProto(
            @NonNull Map<String, Set<String>> namespaceMap) {
        ResultSpecProto.Builder resultSpecBuilder = ResultSpecProto.newBuilder()
                .setNumPerPage(mSearchSpec.getResultCountPerPage())
                .setSnippetSpec(
                        ResultSpecProto.SnippetSpecProto.newBuilder()
                                .setNumToSnippet(mSearchSpec.getSnippetCount())
                                .setNumMatchesPerProperty(mSearchSpec.getSnippetCountPerProperty())
                                .setMaxWindowBytes(mSearchSpec.getMaxSnippetSize()));

        // Rewrites the typePropertyMasks that exist in {@code prefixes}.
        int groupingType = mSearchSpec.getResultGroupingTypeFlags();
        if ((groupingType & SearchSpec.GROUPING_TYPE_PER_PACKAGE) != 0
                && (groupingType & SearchSpec.GROUPING_TYPE_PER_NAMESPACE) != 0) {
            addPerPackagePerNamespaceResultGroupings(mPrefixes,
                    mSearchSpec.getResultGroupingLimit(),
                    namespaceMap, resultSpecBuilder);
        } else if ((groupingType & SearchSpec.GROUPING_TYPE_PER_PACKAGE) != 0) {
            addPerPackageResultGroupings(mPrefixes, mSearchSpec.getResultGroupingLimit(),
                    namespaceMap, resultSpecBuilder);
        } else if ((groupingType & SearchSpec.GROUPING_TYPE_PER_NAMESPACE) != 0) {
            addPerNamespaceResultGroupings(mPrefixes, mSearchSpec.getResultGroupingLimit(),
                    namespaceMap, resultSpecBuilder);
        }

        List<TypePropertyMask.Builder> typePropertyMaskBuilders =
                TypePropertyPathToProtoConverter
                        .toTypePropertyMaskBuilderList(mSearchSpec.getProjections());
        // Rewrite filters to include a database prefix.
        resultSpecBuilder.clearTypePropertyMasks();
        for (int i = 0; i < typePropertyMaskBuilders.size(); i++) {
            String unprefixedType = typePropertyMaskBuilders.get(i).getSchemaType();
            boolean isWildcard =
                    unprefixedType.equals(SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD);
            // Qualify the given schema types
            for (String prefix : mPrefixes) {
                String prefixedType = isWildcard ? unprefixedType : prefix + unprefixedType;
                if (isWildcard || mTargetPrefixedSchemaFilters.contains(prefixedType)) {
                    resultSpecBuilder.addTypePropertyMasks(typePropertyMaskBuilders.get(i)
                            .setSchemaType(prefixedType).build());
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
            default:
                throw new IllegalArgumentException("Invalid result ranking strategy: "
                        + rankingStrategyCode);
        }
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
        // Create a map for package+namespace to prefixedNamespaces. This is NOT necessarily the
        // same as the list of namespaces. If one package has multiple databases, each with the same
        // namespace, then those should be grouped together.
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

        for (List<String> namespaces : packageAndNamespaceToNamespaces.values()) {
            resultSpecBuilder.addResultGroupings(
                    ResultSpecProto.ResultGrouping.newBuilder()
                            .addAllNamespaces(namespaces).setMaxResults(maxNumResults));
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
            resultSpecBuilder.addResultGroupings(
                    ResultSpecProto.ResultGrouping.newBuilder()
                            .addAllNamespaces(prefixedNamespaces).setMaxResults(maxNumResults));
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
        // Create a map of namespace to prefixedNamespaces. This is NOT necessarily the
        // same as the list of namespaces. If a namespace exists under different packages and/or
        // different databases, they should still be grouped together.
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
                    namespaceToPrefixedNamespaces.put(namespace,
                            groupedPrefixedNamespaces);
                }
                groupedPrefixedNamespaces.add(prefixedNamespace);
            }
        }

        for (List<String> namespaces : namespaceToPrefixedNamespaces.values()) {
            resultSpecBuilder.addResultGroupings(
                    ResultSpecProto.ResultGrouping.newBuilder()
                            .addAllNamespaces(namespaces).setMaxResults(maxNumResults));
        }
    }

    /**
     * Find the intersection set of candidates existing in AppSearch and user specified filters.
     *
     * @param prefix                   The package and database's identifier.
     * @param prefixedCandidates       The set contains all prefixed candidates which are existing
     *                                 in a database.
     * @param inputFilters             The set contains all desired but un-prefixed filters of user.
     * @param prefixedTargetFilters    The output set contains all desired prefixed filters which
     *                                 are existing in the database.
     */
    private static void getIntersectedFilters(
            @NonNull String prefix,
            @NonNull Set<String> prefixedCandidates,
            @NonNull List<String> inputFilters,
            @NonNull Set<String> prefixedTargetFilters) {
        if (inputFilters.isEmpty()) {
            // Client didn't specify certain schemas to search over, add all candidates.
            prefixedTargetFilters.addAll(prefixedCandidates);
        } else {
            // Client specified some filters to search over, check and only add those are
            // existing in the database.
            for (int i = 0; i < inputFilters.size(); i++) {
                String prefixedTargetFilter = prefix + inputFilters.get(i);
                if (prefixedCandidates.contains(prefixedTargetFilter)) {
                    prefixedTargetFilters.add(prefixedTargetFilter);
                }
            }
        }
    }
}
