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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.SchemaTypeConfigProto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    public SchemaCache() {
    }

    public SchemaCache(@NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap) {
        mSchemaMap.putAll(Preconditions.checkNotNull(schemaMap));
        rebuildSchemaParentToChildrenMap();
    }

    /**
     * Returns the schema map for the given prefix.
     */
    @NonNull
    public Map<String, SchemaTypeConfigProto> getSchemaMapForPrefix(@NonNull String prefix) {
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
    @NonNull
    public Set<String> getAllPrefixes() {
        return Collections.unmodifiableSet(mSchemaMap.keySet());
    }

    /**
     * Returns all prefixed schema types stored in the cache.
     *
     * <p>This method is inefficient to call repeatedly.
     */
    @NonNull
    public List<String> getAllPrefixedSchemaTypes() {
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
    @NonNull
    public Set<String> getSchemaTypesWithDescendants(@NonNull String prefix,
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
            String currentPrefixedSchema = prefixedSchemaQueue.poll();
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
     * Rebuilds the schema parent-to-children map for the given prefix, based on the current
     * schema map.
     *
     * <p>The schema parent-to-children map is required to be updated when
     * {@link #addToSchemaMap} or {@link #removeFromSchemaMap} has been called. Otherwise, the
     * results from {@link #getSchemaTypesWithDescendants} would be stale.
     */
    public void rebuildSchemaParentToChildrenMapForPrefix(@NonNull String prefix) {
        Preconditions.checkNotNull(prefix);

        mSchemaParentToChildrenMap.remove(prefix);
        Map<String, SchemaTypeConfigProto> prefixedSchemaMap = mSchemaMap.get(prefix);
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
            mSchemaParentToChildrenMap.put(prefix, parentToChildrenMap);
        }
    }

    /**
     * Rebuilds the schema parent-to-children map based on the current schema map.
     *
     * <p>The schema parent-to-children map is required to be updated when
     * {@link #addToSchemaMap} or {@link #removeFromSchemaMap} has been called. Otherwise, the
     * results from {@link #getSchemaTypesWithDescendants} would be stale.
     */
    public void rebuildSchemaParentToChildrenMap() {
        mSchemaParentToChildrenMap.clear();
        for (String prefix : mSchemaMap.keySet()) {
            rebuildSchemaParentToChildrenMapForPrefix(prefix);
        }
    }

    /**
     * Adds a schema to the schema map.
     *
     * <p>Note that this method will invalidate the schema parent-to-children map in the cache,
     * and either {@link #rebuildSchemaParentToChildrenMap} or
     * {@link #rebuildSchemaParentToChildrenMapForPrefix} is required to be called to update the
     * cache.
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

    /**
     * Removes a schema from the schema map.
     *
     * <p>Note that this method will invalidate the schema parent-to-children map in the cache,
     * and either {@link #rebuildSchemaParentToChildrenMap} or
     * {@link #rebuildSchemaParentToChildrenMapForPrefix} is required to be called to update the
     * cache.
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
     * Removes the entry of the given prefix from both the schema map and the schema
     * parent-to-children map, and returns the set of removed prefixed schema type.
     */
    @NonNull
    public Set<String> removePrefix(@NonNull String prefix) {
        Preconditions.checkNotNull(prefix);

        Map<String, SchemaTypeConfigProto> removedSchemas =
                Preconditions.checkNotNull(mSchemaMap.remove(prefix));
        mSchemaParentToChildrenMap.remove(prefix);
        return removedSchemas.keySet();
    }

    /**
     * Clears all data in the cache.
     */
    public void clear() {
        mSchemaMap.clear();
        mSchemaParentToChildrenMap.clear();
    }
}
