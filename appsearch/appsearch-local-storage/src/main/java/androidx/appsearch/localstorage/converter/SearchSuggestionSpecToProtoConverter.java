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
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.SuggestionSpecProto;
import com.google.android.icing.proto.TermMatchType;

import java.util.Map;
import java.util.Set;

/**
 * Translates a {@link SearchSuggestionSpec} into icing search protos.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SearchSuggestionSpecToProtoConverter {
    private final String mSuggestionQueryExpression;
    private final SearchSuggestionSpec mSearchSuggestionSpec;
    /**
     * The intersected prefixed namespaces that are existing in AppSearch and also accessible to the
     * client.
     */
    private final Set<String> mTargetPrefixedNamespaceFilters;

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
            @NonNull Map<String, Set<String>> namespaceMap) {
        mSuggestionQueryExpression = Preconditions.checkNotNull(suggestionQueryExpression);
        mSearchSuggestionSpec = Preconditions.checkNotNull(searchSuggestionSpec);
        Preconditions.checkNotNull(prefixes);
        Preconditions.checkNotNull(namespaceMap);
        mTargetPrefixedNamespaceFilters =
                SearchSpecToProtoConverterUtil.generateTargetNamespaceFilters(
                        prefixes, namespaceMap, searchSuggestionSpec.getFilterNamespaces());
    }

    /**
     * @return whether this search's target filters are empty. If any target filter is empty, we
     * should skip send request to Icing.
     */
    public boolean hasNothingToSearch() {
        return mTargetPrefixedNamespaceFilters.isEmpty();
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
                .setNumToReturn(mSearchSuggestionSpec.getMaximumResultCount());

        // TODO(b/227356108) expose setTermMatch in SearchSuggestionSpec.
        protoBuilder.setScoringSpec(SuggestionSpecProto.SuggestionScoringSpecProto.newBuilder()
                .setScoringMatchType(TermMatchType.Code.EXACT_ONLY)
                .build());

        return protoBuilder.build();
    }
}
