/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Caches and manages namespace information for AppSearch.
 *
 * This class is NOT thread safety.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NamespaceCache {

    // This map contains namespaces for all package-database prefixes. All values in the map are
    // prefixed with the package-database prefix.
    private final Map<String, Set<String>> mDocumentNamespaceMap = new ArrayMap<>();

    // This map contains blob namespaces for all package-database prefixes. All values in the map
    // are prefixed with the package-database prefix.
    private final Map<String, Set<String>> mBlobNamespaceMap = new ArrayMap<>();

    public NamespaceCache() {}

    @VisibleForTesting
    public NamespaceCache(@NonNull Map<String, Set<String>> documentNamespaceMap) {
        mDocumentNamespaceMap.putAll(documentNamespaceMap);
    }

    /** Gets all prefixed document namespaces of the given set of packages. */
    public @NonNull Set<String> getAllPrefixedDocumentNamespaceForPackages(
            @NonNull Set<String> packageNames) {
        Set<String> wantedPrefixedNamespaces = new ArraySet<>();

        // Accumulate all the namespaces we're interested in.
        for (Map.Entry<String, Set<String>> entry : mDocumentNamespaceMap.entrySet()) {
            if (packageNames.contains(PrefixUtil.getPackageName(entry.getKey()))) {
                wantedPrefixedNamespaces.addAll(entry.getValue());
            }
        }
        return wantedPrefixedNamespaces;
    }

    /** Gets all prefixed document blob namespaces of the given set of packages. */
    public @NonNull Set<String> getAllPrefixedBlobNamespaceForPackages(
            @NonNull Set<String> packageNames) {
        Set<String> wantedPrefixedNamespaces = new ArraySet<>();

        // Accumulate all the namespaces we're interested in.
        for (Map.Entry<String, Set<String>> entry : mBlobNamespaceMap.entrySet()) {
            if (packageNames.contains(PrefixUtil.getPackageName(entry.getKey()))) {
                wantedPrefixedNamespaces.addAll(entry.getValue());
            }
        }
        return wantedPrefixedNamespaces;
    }

    /**  Gets prefixed document namespaces of the given prefix. */
    public @Nullable Set<String> getPrefixedDocumentNamespaces(@NonNull String prefix) {
        return mDocumentNamespaceMap.get(prefix);
    }

    /**  Gets prefixed blob namespaces of the given prefix. */
    public @Nullable Set<String> getPrefixedBlobNamespaces(@NonNull String prefix) {
        return mBlobNamespaceMap.get(prefix);
    }

    /**  Gets all prefixes that contains documents in AppSearch.  */
    public @NonNull Set<String> getAllDocumentPrefixes() {
        return mDocumentNamespaceMap.keySet();
    }


    /**  Gets all prefixed blob namespaces in AppSearch.  */
    public @NonNull List<String> getAllPrefixedBlobNamespaces() {
        List<String> prefixedBlobNamespaces = new ArrayList<>();
        for (Set<String> value : mBlobNamespaceMap.values()) {
            prefixedBlobNamespaces.addAll(value);
        }
        return prefixedBlobNamespaces;
    }

    /**  Removes prefixed document namespaces under the given prefix.  */
    public @Nullable Set<String> removeDocumentNamespaces(@NonNull String prefix) {
        return mDocumentNamespaceMap.remove(prefix);
    }

    /**  Add the given prefixed namespace to document namespace map. */
    public void addToDocumentNamespaceMap(@NonNull String prefix,
            @NonNull String prefixedNamespace) {
        addToMap(mDocumentNamespaceMap, prefix, prefixedNamespace);
    }

    /**  Add the given prefixed namespace to blob namespace map. */
    public void addToBlobNamespaceMap(@NonNull String prefix, @NonNull String prefixedNamespace) {
        addToMap(mBlobNamespaceMap, prefix, prefixedNamespace);
    }

    /**
     * Clears all data in the cache.
     */
    public void clear() {
        mDocumentNamespaceMap.clear();
        mBlobNamespaceMap.clear();
    }

    private static void addToMap(Map<String, Set<String>> map, String prefix,
            String prefixedValue) {
        Set<String> values = map.get(prefix);
        if (values == null) {
            values = new ArraySet<>();
            map.put(prefix, values);
        }
        values.add(prefixedValue);
    }
}
