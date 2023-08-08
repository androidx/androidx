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
import androidx.collection.ArraySet;

import com.google.android.icing.proto.SchemaTypeConfigProto;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for working with {@link SearchSpecToProtoConverter} and
 * {@link SearchSuggestionSpecToProtoConverter}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SearchSpecToProtoConverterUtil {
    private SearchSpecToProtoConverterUtil() {}

    /**
     * Add prefix to the given namespace filters that user want to search over and find the
     * intersection set with those prefixed namespace candidates that are stored in AppSearch.
     *
     * @param prefixes              Set of database prefix which the caller want to access.
     * @param namespaceMap          The cached Map of {@code <Prefix, Set<PrefixedNamespace>>}
     *                              stores all prefixed namespace filters which are stored in
     *                              AppSearch.
     * @param inputNamespaceFilters The set contains all desired but un-prefixed namespace filters
     *                              of user. If the inputNamespaceFilters is empty, all existing
     *                              prefixedCandidates will be added to the prefixedTargetFilters.
     */
    static Set<String> generateTargetNamespaceFilters(
            @NonNull Set<String> prefixes,
            @NonNull Map<String, Set<String>> namespaceMap,
            @NonNull List<String> inputNamespaceFilters) {
        // Convert namespace filters to prefixed namespace filters
        Set<String> targetPrefixedNamespaceFilters = new ArraySet<>();
        for (String prefix : prefixes) {
            // Step1: find all prefixed namespace candidates that are stored in AppSearch.
            Set<String> prefixedNamespaceCandidates = namespaceMap.get(prefix);
            if (prefixedNamespaceCandidates == null) {
                // This is should never happen. All prefixes should be verified before reach
                // here.
                continue;
            }
            // Step2: get the intersection of user searching filters and those candidates which are
            // stored in AppSearch.
            addIntersectedFilters(prefix, prefixedNamespaceCandidates,
                    inputNamespaceFilters, targetPrefixedNamespaceFilters);
        }
        return targetPrefixedNamespaceFilters;
    }

    /**
     * Add prefix to the given schema filters that user want to search over and find the
     * intersection set with those prefixed schema candidates that are stored in AppSearch.
     *
     * @param prefixes              Set of database prefix which the caller want to access.
     * @param schemaMap             The cached Map of
     *                              {@code <Prefix, Map<PrefixedSchemaType, schemaProto>>}
     *                              stores all prefixed schema filters which are stored in
     *                              AppSearch.
     * @param inputSchemaFilters    The set contains all desired but un-prefixed namespace filters
     *                              of user. If the inputSchemaFilters is empty, all existing
     *                              prefixedCandidates will be added to the prefixedTargetFilters.
     */
    static Set<String> generateTargetSchemaFilters(
            @NonNull Set<String> prefixes,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap,
            @NonNull List<String> inputSchemaFilters) {
        Set<String> targetPrefixedSchemaFilters = new ArraySet<>();
        // Append prefix to input schema filters and get the intersection of existing schema filter.
        for (String prefix : prefixes) {
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
            addIntersectedFilters(prefix, prefixedSchemaCandidates, inputSchemaFilters,
                    targetPrefixedSchemaFilters);
        }
        return targetPrefixedSchemaFilters;
    }

    /**
     * Find the intersection set of candidates existing in AppSearch and user specified filters.
     *
     * @param prefix                   The package and database's identifier.
     * @param prefixedCandidates       The set contains all prefixed candidates which are existing
     *                                 in a database.
     * @param inputFilters             The set contains all desired but un-prefixed filters of user.
     *                                 If the inputFilters is empty, all prefixedCandidates will be
     *                                 added to the prefixedTargetFilters.
     * @param prefixedTargetFilters    The output set contains all desired prefixed filters which
     *                                 are existing in the database.
     */
    private static void addIntersectedFilters(
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
