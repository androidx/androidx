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
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.checker.initialization.qual.UnknownInitialization;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.SchemaTypeConfigProto;

import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Caches and manages schema information for AppSearch.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaCache {
    /**
     * A map that contains schema types and SchemaTypeConfigProtos for all package-database
     * prefixes. It maps each package-database prefix to an inner-map. The inner-map maps each
     * prefixed schema type to its respective SchemaTypeConfigProto.
     */
    private final Map<String, Map<String, SchemaTypeConfigProto>> mSchemaMap = new ArrayMap<>();

    /**
     * A map that contains schema types and all children schema types for all package-database
     * prefixes. It maps each package-database prefix to an inner-map. The inner-map maps each
     * prefixed schema type to its respective list of children prefixed schema types.
     */
    private final Map<String, Map<String, List<String>>> mSchemaParentToChildrenMap =
            new ArrayMap<>();

    /**
     * A map that contains schema types and all parent schema types for all package-database
     * prefixes. It maps each package-database prefix to an inner-map. The inner-map maps each
     * prefixed schema type to its respective list of unprefixed parent schema types including
     * transitive parents. It's guaranteed that child types always appear before parent types in
     * the list.
     */
    private final Map<String, Map<String, List<String>>>
            mSchemaChildToTransitiveUnprefixedParentsMap = new ArrayMap<>();

    /**
     * An in-memory cache of property paths, keyed by prefixed schema type, that point to
     * {@link androidx.appsearch.app.AppSearchAccount} property paths.
     *
     * <p>This map is used for runtime validation of documents. It is rebuilt from schema
     * information during {@code getSchema} calls.
     */
    private final Map<String, Set<String>> mPrefixedAccountPropertyPaths = new ArrayMap<>();

    public SchemaCache() {
    }

    @VisibleForTesting
    public SchemaCache(@NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap)
            throws AppSearchException {
        mSchemaMap.putAll(Preconditions.checkNotNull(schemaMap));
        rebuildCache();
    }

    /**
     * Returns the schema map for the given prefix.
     */
    public @NonNull Map<String, SchemaTypeConfigProto> getSchemaMapForPrefix(
            @NonNull String prefix) {
        Preconditions.checkNotNull(prefix);

        Map<String, SchemaTypeConfigProto> schemaMap = mSchemaMap.get(prefix);
        if (schemaMap == null) {
            return Collections.emptyMap();
        }
        return schemaMap;
    }

    /**
     * Returns a set of all prefixes stored in the cache.
     */
    public @NonNull Set<String> getAllPrefixes() {
        return Collections.unmodifiableSet(mSchemaMap.keySet());
    }

    /**
     * Returns all prefixed schema types stored in the cache.
     *
     * <p>This method is inefficient to call repeatedly.
     */
    public @NonNull List<String> getAllPrefixedSchemaTypes() {
        List<String> cachedPrefixedSchemaTypes = new ArrayList<>();
        for (Map<String, SchemaTypeConfigProto> value : mSchemaMap.values()) {
            cachedPrefixedSchemaTypes.addAll(value.keySet());
        }
        return cachedPrefixedSchemaTypes;
    }

    /**
     * Returns the schema types for the given set of prefixed schema types with their
     * descendants, based on the schema parent-to-children map held in the cache.
     */
    public @NonNull Set<String> getSchemaTypesWithDescendants(@NonNull String prefix,
            @NonNull Set<String> prefixedSchemaTypes) {
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(prefixedSchemaTypes);
        Map<String, List<String>> parentToChildrenMap = mSchemaParentToChildrenMap.get(prefix);
        if (parentToChildrenMap == null) {
            parentToChildrenMap = Collections.emptyMap();
        }

        // Perform a BFS search on the inheritance graph started by the set of prefixedSchemaTypes.
        Set<String> visited = new ArraySet<>();
        Queue<String> prefixedSchemaQueue = new ArrayDeque<>(prefixedSchemaTypes);
        while (!prefixedSchemaQueue.isEmpty()) {
            String currentPrefixedSchema = Objects.requireNonNull(prefixedSchemaQueue.poll());
            if (visited.contains(currentPrefixedSchema)) {
                continue;
            }
            visited.add(currentPrefixedSchema);
            List<String> children = parentToChildrenMap.get(currentPrefixedSchema);
            if (children == null) {
                continue;
            }
            prefixedSchemaQueue.addAll(children);
        }

        return visited;
    }

    /**
     * Returns the unprefixed parent schema types, including transitive parents, for the given
     * prefixed schema type, based on the schema child-to-parents map held in the cache. It's
     * guaranteed that child types always appear before parent types in the list.
     */
    public @NonNull List<String> getTransitiveUnprefixedParentSchemaTypes(@NonNull String prefix,
            @NonNull String prefixedSchemaType) throws AppSearchException {
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(prefixedSchemaType);

        // If the flag is on, retrieve the parent types from the cache as it is available.
        // Otherwise, recalculate the parent types.
        if (Flags.enableSearchResultParentTypes()) {
            Map<String, List<String>> unprefixedChildToParentsMap =
                    mSchemaChildToTransitiveUnprefixedParentsMap.get(prefix);
            if (unprefixedChildToParentsMap == null) {
                return Collections.emptyList();
            }
            List<String> parents = unprefixedChildToParentsMap.get(prefixedSchemaType);
            return parents == null ? Collections.emptyList() : parents;
        } else {
            return calculateTransitiveUnprefixedParentSchemaTypes(prefixedSchemaType,
                    getSchemaMapForPrefix(prefix));
        }
    }

    /**  Add all the prefixed schema types with account {@link PropertyPath}s . */
    public @NonNull Map<String, Set<String>> getPrefixedAccountPropertyPaths() {
        return mPrefixedAccountPropertyPaths;
    }

    /**
     * Rebuilds the schema parent-to-children and child-to-parents maps for the given prefix,
     * based on the current schema map.
     *
     * <p>The schema parent-to-children and child-to-parents maps must be updated when
     * {@link #addToSchemaMap} or {@link #removeFromSchemaMap} has been called. Otherwise, the
     * results from {@link #getSchemaTypesWithDescendants} and
     * {@link #getTransitiveUnprefixedParentSchemaTypes} would be stale.
     */
    public void rebuildCacheForPrefix(
            @UnknownInitialization SchemaCache this, @NonNull String prefix)
            throws AppSearchException {
        Preconditions.checkNotNull(prefix);

        Objects.requireNonNull(mSchemaParentToChildrenMap).remove(prefix);
        Objects.requireNonNull(mSchemaChildToTransitiveUnprefixedParentsMap).remove(prefix);
        Map<String, SchemaTypeConfigProto> prefixedSchemaMap =
            Objects.requireNonNull(mSchemaMap).get(prefix);
        if (prefixedSchemaMap == null) {
            return;
        }

        // Build the parent-to-children map for the current prefix.
        Map<String, List<String>> parentToChildrenMap = new ArrayMap<>();
        for (SchemaTypeConfigProto childSchemaConfig : prefixedSchemaMap.values()) {
            for (int i = 0; i < childSchemaConfig.getParentTypesCount(); i++) {
                String parent = childSchemaConfig.getParentTypes(i);
                List<String> children = parentToChildrenMap.get(parent);
                if (children == null) {
                    children = new ArrayList<>();
                    parentToChildrenMap.put(parent, children);
                }
                children.add(childSchemaConfig.getSchemaType());
            }
        }
        // Record the map for the current prefix.
        if (!parentToChildrenMap.isEmpty()) {
            Objects.requireNonNull(mSchemaParentToChildrenMap).put(prefix, parentToChildrenMap);
        }

        // If the flag is on, build the child-to-parent maps as caches. Otherwise, this
        // information will have to be recalculated when needed.
        if (Flags.enableSearchResultParentTypes()) {
            // Build the child-to-parents maps for the current prefix.
            Map<String, List<String>> childToTransitiveUnprefixedParentsMap = new ArrayMap<>();
            for (SchemaTypeConfigProto childSchemaConfig : prefixedSchemaMap.values()) {
                if (childSchemaConfig.getParentTypesCount() > 0) {
                    childToTransitiveUnprefixedParentsMap.put(
                            childSchemaConfig.getSchemaType(),
                            calculateTransitiveUnprefixedParentSchemaTypes(
                                    childSchemaConfig.getSchemaType(),
                                    prefixedSchemaMap));
                }
            }
            // Record the map for the current prefix.
            if (!childToTransitiveUnprefixedParentsMap.isEmpty()) {
                mSchemaChildToTransitiveUnprefixedParentsMap.put(prefix,
                        childToTransitiveUnprefixedParentsMap);
            }
        }
    }

    /**
     * Rebuilds the schema parent-to-children and child-to-parents maps based on the current
     * schema map.
     *
     * <p>The schema parent-to-children and child-to-parents maps must be updated when
     * {@link #addToSchemaMap} or {@link #removeFromSchemaMap} has been called. Otherwise, the
     * results from {@link #getSchemaTypesWithDescendants} and
     * {@link #getTransitiveUnprefixedParentSchemaTypes} would be stale.
     */
    public void rebuildCache(
            @UnknownInitialization SchemaCache this) throws AppSearchException {
        Objects.requireNonNull(mSchemaParentToChildrenMap).clear();
        Objects.requireNonNull(mSchemaChildToTransitiveUnprefixedParentsMap).clear();
        for (String prefix : Objects.requireNonNull(mSchemaMap).keySet()) {
            rebuildCacheForPrefix(prefix);
        }
    }

    /**
     * Adds a schema to the schema map.
     *
     * <p>Note that this method will invalidate the schema parent-to-children and
     * child-to-parents maps in the cache, and either {@link #rebuildCache} or
     * {@link #rebuildCacheForPrefix} is required to be called to update the cache.
     */
    public void addToSchemaMap(@NonNull String prefix,
            @NonNull SchemaTypeConfigProto schemaTypeConfigProto) {
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(schemaTypeConfigProto);

        Map<String, SchemaTypeConfigProto> schemaTypeMap = mSchemaMap.get(prefix);
        if (schemaTypeMap == null) {
            schemaTypeMap = new ArrayMap<>();
            mSchemaMap.put(prefix, schemaTypeMap);
        }
        schemaTypeMap.put(schemaTypeConfigProto.getSchemaType(), schemaTypeConfigProto);
    }

    /**  Add the account {@link PropertyPath} for the given prefixed schema type. */
    public void addToSchemasWipeoutAccountPropertyPaths(@NonNull String prefixedSchemaType,
            @NonNull Set<String> accountPropertyPaths) {
        mPrefixedAccountPropertyPaths.put(prefixedSchemaType, accountPropertyPaths);
    }

    /**
     * Removes a schema from the schema map.
     *
     * <p>Note that this method will invalidate the schema parent-to-children and
     * child-to-parents maps in the cache, and either {@link #rebuildCache} or
     * {@link #rebuildCacheForPrefix} is required to be called to update the cache.
     */
    public void removeFromSchemaMap(@NonNull String prefix, @NonNull String schemaType) {
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(schemaType);

        Map<String, SchemaTypeConfigProto> schemaTypeMap = mSchemaMap.get(prefix);
        if (schemaTypeMap != null) {
            schemaTypeMap.remove(schemaType);
        }
    }

    /**
     * Removes the entry of the given prefix from the schema map, the schema parent-to-children
     * map and the child-to-parents map, and returns the set of removed prefixed schema type.
     */
    public @NonNull Set<String> removePrefix(@NonNull String prefix) {
        Preconditions.checkNotNull(prefix);

        Map<String, SchemaTypeConfigProto> removedSchemas =
                Preconditions.checkNotNull(mSchemaMap.remove(prefix));
        mSchemaParentToChildrenMap.remove(prefix);
        mSchemaChildToTransitiveUnprefixedParentsMap.remove(prefix);
        return removedSchemas.keySet();
    }

    /**
     * Clears all data in the cache.
     */
    public void clear() {
        mSchemaMap.clear();
        mSchemaParentToChildrenMap.clear();
        mSchemaChildToTransitiveUnprefixedParentsMap.clear();
    }

    /**
     * Get the list of unprefixed transitive parent type names of {@code prefixedSchemaType}.
     *
     * <p>It's guaranteed that child types always appear before parent types in the list.
     */
    private @NonNull List<String> calculateTransitiveUnprefixedParentSchemaTypes(
            @NonNull String prefixedSchemaType,
            @NonNull Map<String, SchemaTypeConfigProto> prefixedSchemaMap)
            throws AppSearchException {
        // Please note that neither DFS nor BFS order is guaranteed to always put child types
        // before parent types (due to the diamond problem), so a topological sorting algorithm
        // is required.
        Map<String, Integer> inDegreeMap = new ArrayMap<>();
        collectParentTypeInDegrees(prefixedSchemaType, prefixedSchemaMap,
                /* visited= */new ArraySet<>(), inDegreeMap);

        List<String> result = new ArrayList<>();
        Queue<String> queue = new ArrayDeque<>();
        // prefixedSchemaType is the only type that has zero in-degree at this point.
        queue.add(prefixedSchemaType);
        while (!queue.isEmpty()) {
            SchemaTypeConfigProto currentSchema = Preconditions.checkNotNull(
                    prefixedSchemaMap.get(queue.poll()));
            for (int i = 0; i < currentSchema.getParentTypesCount(); ++i) {
                String prefixedParentType = currentSchema.getParentTypes(i);
                int parentInDegree =
                        Preconditions.checkNotNull(inDegreeMap.get(prefixedParentType)) - 1;
                inDegreeMap.put(prefixedParentType, parentInDegree);
                if (parentInDegree == 0) {
                    result.add(PrefixUtil.removePrefix(prefixedParentType));
                    queue.add(prefixedParentType);
                }
            }
        }
        return result;
    }

    private void collectParentTypeInDegrees(
            @NonNull String prefixedSchemaType,
            @NonNull Map<String, SchemaTypeConfigProto> schemaTypeMap,
            @NonNull Set<String> visited, @NonNull Map<String, Integer> inDegreeMap) {
        if (visited.contains(prefixedSchemaType)) {
            return;
        }
        visited.add(prefixedSchemaType);
        SchemaTypeConfigProto schema =
                Preconditions.checkNotNull(schemaTypeMap.get(prefixedSchemaType));
        for (int i = 0; i < schema.getParentTypesCount(); ++i) {
            String prefixedParentType = schema.getParentTypes(i);
            Integer parentInDegree = inDegreeMap.get(prefixedParentType);
            if (parentInDegree == null) {
                parentInDegree = 0;
            }
            inDegreeMap.put(prefixedParentType, parentInDegree + 1);
            collectParentTypeInDegrees(prefixedParentType, schemaTypeMap, visited, inDegreeMap);
        }
    }
}
