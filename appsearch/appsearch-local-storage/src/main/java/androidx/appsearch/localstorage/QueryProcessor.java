/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.localstorage;

import static androidx.appsearch.localstorage.util.PrefixUtil.getDatabaseName;
import static androidx.appsearch.localstorage.util.PrefixUtil.getPackageName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.collection.ArraySet;

import com.google.android.icing.proto.SchemaTypeConfigProto;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The helper class contains all static methods used in AppSearch query process.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class QueryProcessor {
    private QueryProcessor() {}

    /**
     * Add prefix to the given namespace filters that user want to search over and find the
     * intersection set with those prefixed namespace candidates that are stored in AppSearch.
     *
     * @param prefixes                   Set of database prefix which the caller want to access.
     * @param searchingNamespaceFilters  The given namespace filter that user want to search over.
     * @param namespaceMap               The cached Map of <Prefix, Set<PrefixedNamespace>> stores
     *                                   all prefixed namespace filters which are stored in
     *                                   AppSearch.
     * @return prefixed namespace that the caller want to access and existing in AppSearch.
     */
    @NonNull
    static Set<String> getPrefixedTargetNamespaceFilters(
            @NonNull Set<String> prefixes,
            @NonNull List<String> searchingNamespaceFilters,
            @NonNull Map<String, Set<String>> namespaceMap) {
        Set<String> targetPrefixedNamespaceFilters = new ArraySet<>();
        // Convert namespace filters to prefixed namespace filters
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
            getIntersectedFilters(prefix, prefixedNamespaceCandidates,
                    searchingNamespaceFilters, targetPrefixedNamespaceFilters);
        }
        return targetPrefixedNamespaceFilters;
    }

    /**
     * Add prefix to the given schema filters that user want to search over and find the
     * intersection set with those prefixed schema candidates that are stored in AppSearch.
     *
     * @param prefixes               Set of database prefix which the caller want to access.
     * @param searchingSchemaFilters The given schema filter that user want to search over.
     * @param schemaMap              The cached Map of
     *                               <Prefix, Map<PrefixedSchemaType, schemaProto>>
     *                               stores all prefixed schema filters which are stored in
     *                               AppSearch.
     * @return prefixed schema that the caller want to access and existing in AppSearch.
     */
    @NonNull
    static Set<String> getPrefixedTargetSchemaFilters(
            @NonNull Set<String> prefixes,
            @NonNull List<String> searchingSchemaFilters,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap) {
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
            getIntersectedFilters(prefix, prefixedSchemaCandidates, searchingSchemaFilters,
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
     * @param prefixedTargetFilters    The output set contains all desired filters which are
     *                                 existing in the database.
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

    /**
     * For each target schema, we will check visibility store is that accessible to the caller. And
     * remove this schemas if it is not allowed for caller to query.
     *
     * @param callerPackageName            The package name of caller
     * @param visibilityStore              The visibility store which holds all visibility settings.
     * @param callerUid                    The uid of the caller.
     * @param callerHasSystemAccess        Whether the caller has system access.
     * @param targetPrefixedSchemaFilters  The output filter set contains all desired and
     *                                     accessible for user and also existing in AppSearch.
     */
    static void removeInaccessibleSchemaFilter(
            @NonNull String callerPackageName,
            @Nullable VisibilityStore visibilityStore,
            int callerUid,
            boolean callerHasSystemAccess,
            @NonNull Set<String> targetPrefixedSchemaFilters) {
        Iterator<String> targetPrefixedSchemaFilterIterator =
                targetPrefixedSchemaFilters.iterator();
        while (targetPrefixedSchemaFilterIterator.hasNext()) {
            String targetPrefixedSchemaFilter = targetPrefixedSchemaFilterIterator.next();
            String packageName = getPackageName(targetPrefixedSchemaFilter);

            boolean allow;
            if (packageName.equals(callerPackageName)) {
                // Callers can always retrieve their own data
                allow = true;
            } else if (visibilityStore == null) {
                // If there's no visibility store, there's no extra access
                allow = false;
            } else {
                String databaseName = getDatabaseName(targetPrefixedSchemaFilter);
                allow = visibilityStore.isSchemaSearchableByCaller(packageName, databaseName,
                        targetPrefixedSchemaFilter, callerUid, callerHasSystemAccess);
            }
            if (!allow) {
                targetPrefixedSchemaFilterIterator.remove();
            }
        }
    }
}
