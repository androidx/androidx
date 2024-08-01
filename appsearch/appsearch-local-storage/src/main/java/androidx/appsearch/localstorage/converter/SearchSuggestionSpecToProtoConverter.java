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

package androidx.appsearch.localstorage.converter;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.localstorage.SchemaCache;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.NamespaceDocumentUriGroup;
import com.google.android.icing.proto.SuggestionScoringSpecProto;
import com.google.android.icing.proto.SuggestionSpecProto;
import com.google.android.icing.proto.TermMatchType;
import com.google.android.icing.proto.TypePropertyMask;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates a {@link SearchSuggestionSpec} into icing search protos.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SearchSuggestionSpecToProtoConverter {
    private final String mSuggestionQueryExpression;
    private final SearchSuggestionSpec mSearchSuggestionSpec;
    /**
     * The client specific packages and databases to search for. For local storage, this always
     * contains a single prefix.
     */
    private final Set<String> mPrefixes;
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
     * Creates a {@link SearchSuggestionSpecToProtoConverter} for given
     * {@link SearchSuggestionSpec}.
     *
     * @param suggestionQueryExpression The non-empty query expression used to be completed.
     * @param searchSuggestionSpec      The spec we need to convert from.
     * @param prefixes                  Set of database prefix which the caller want to access.
     * @param namespaceMap              The cached Map of {@code <Prefix, Set<PrefixedNamespace>>}
     *                                  stores all prefixed namespace filters which are stored in
     *                                  AppSearch.
     */
    public SearchSuggestionSpecToProtoConverter(
            @NonNull String suggestionQueryExpression,
            @NonNull SearchSuggestionSpec searchSuggestionSpec,
            @NonNull Set<String> prefixes,
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull SchemaCache schemaCache) {
        mSuggestionQueryExpression = Preconditions.checkNotNull(suggestionQueryExpression);
        mSearchSuggestionSpec = Preconditions.checkNotNull(searchSuggestionSpec);
        mPrefixes = Preconditions.checkNotNull(prefixes);
        Preconditions.checkNotNull(namespaceMap);
        mTargetPrefixedNamespaceFilters =
                SearchSpecToProtoConverterUtil.generateTargetNamespaceFilters(
                        prefixes, namespaceMap, searchSuggestionSpec.getFilterNamespaces());
        mTargetPrefixedSchemaFilters =
                SearchSpecToProtoConverterUtil.generateTargetSchemaFilters(
                        prefixes, schemaCache, searchSuggestionSpec.getFilterSchemas());
    }

    /**
     * Returns whether this search's target filters are empty. If any target filter is empty, we
     * should skip send request to Icing.
     */
    public boolean hasNothingToSearch() {
        return mTargetPrefixedNamespaceFilters.isEmpty() || mTargetPrefixedSchemaFilters.isEmpty();
    }

    /**
     * Extracts {@link SuggestionSpecProto} information from a {@link SearchSuggestionSpec}.
     *
     */
    @NonNull
    public SuggestionSpecProto toSearchSuggestionSpecProto() {
        // Set query suggestion prefix to the SearchSuggestionProto and override schema and
        // namespace filter by targetPrefixedFilters which contains all existing and also
        // accessible to the caller filters.
        SuggestionSpecProto.Builder protoBuilder = SuggestionSpecProto.newBuilder()
                .setPrefix(mSuggestionQueryExpression)
                .addAllNamespaceFilters(mTargetPrefixedNamespaceFilters)
                .addAllSchemaTypeFilters(mTargetPrefixedSchemaFilters)
                .setNumToReturn(mSearchSuggestionSpec.getMaximumResultCount())
                .addAllQueryParameterStrings(mSearchSuggestionSpec.getSearchStringParameters());

        // Convert type property filter map into type property mask proto.
        for (Map.Entry<String, List<String>> entry :
                mSearchSuggestionSpec.getFilterProperties().entrySet()) {
            for (String prefix : mPrefixes) {
                String prefixedSchemaType = prefix + entry.getKey();
                if (mTargetPrefixedSchemaFilters.contains(prefixedSchemaType)) {
                    protoBuilder.addTypePropertyFilters(TypePropertyMask.newBuilder()
                            .setSchemaType(prefixedSchemaType)
                            .addAllPaths(entry.getValue())
                            .build());
                }
            }
        }

        // Convert the document ids filters
        for (Map.Entry<String, List<String>> entry :
                mSearchSuggestionSpec.getFilterDocumentIds().entrySet()) {
            for (String prefix : mPrefixes) {
                String prefixedNamespace = prefix + entry.getKey();
                if (mTargetPrefixedNamespaceFilters.contains(prefixedNamespace)) {
                    protoBuilder.addDocumentUriFilters(NamespaceDocumentUriGroup.newBuilder()
                            .setNamespace(prefixedNamespace)
                            .addAllDocumentUris(entry.getValue())
                            .build());
                }
            }
        }

        // TODO(b/227356108) expose setTermMatch in SearchSuggestionSpec.
        protoBuilder.setScoringSpec(SuggestionScoringSpecProto.newBuilder()
                .setScoringMatchType(TermMatchType.Code.EXACT_ONLY)
                .setRankBy(toProtoRankingStrategy(mSearchSuggestionSpec.getRankingStrategy()))
                .build());

        return protoBuilder.build();
    }

    private static SuggestionScoringSpecProto.SuggestionRankingStrategy.Code toProtoRankingStrategy(
            @SearchSuggestionSpec.SuggestionRankingStrategy int rankingStrategyCode) {
        switch (rankingStrategyCode) {
            case SearchSuggestionSpec.SUGGESTION_RANKING_STRATEGY_NONE:
                return SuggestionScoringSpecProto.SuggestionRankingStrategy.Code.NONE;
            case SearchSuggestionSpec.SUGGESTION_RANKING_STRATEGY_DOCUMENT_COUNT:
                return SuggestionScoringSpecProto.SuggestionRankingStrategy.Code.DOCUMENT_COUNT;
            case SearchSuggestionSpec.SUGGESTION_RANKING_STRATEGY_TERM_FREQUENCY:
                return SuggestionScoringSpecProto.SuggestionRankingStrategy.Code.TERM_FREQUENCY;
            default:
                throw new IllegalArgumentException("Invalid suggestion ranking strategy: "
                        + rankingStrategyCode);
        }
    }
}
