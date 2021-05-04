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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated
public final class GetByUriRequest {
    /**
     * Schema type to be used in
     * {@link GetByUriRequest.Builder#addProjection}
     * to apply property paths to all results, excepting any types that have had their own, specific
     * property paths set.
     */
    public static final String PROJECTION_SCHEMA_TYPE_WILDCARD = "*";
    private final String mNamespace;
    private final Set<String> mIds;
    private final Map<String, List<String>> mTypePropertyPathsMap;

    GetByUriRequest(@NonNull String namespace, @NonNull Set<String> ids, @NonNull Map<String,
            List<String>> typePropertyPathsMap) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mIds = Preconditions.checkNotNull(ids);
        mTypePropertyPathsMap = Preconditions.checkNotNull(typePropertyPathsMap);
    }

    /** Returns the namespace attached to the request. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the set of document IDs attached to the request. */
    @NonNull
    public Set<String> getUris() {
        return Collections.unmodifiableSet(mIds);
    }

    /**
     * Returns a map from schema type to property paths to be used for projection.
     *
     * <p>If the map is empty, then all properties will be retrieved for all results.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned
     * by this function, rather than calling it multiple times.
     */
    @NonNull
    public Map<String, List<String>> getProjections() {
        Map<String, List<String>> copy = new ArrayMap<>();
        for (Map.Entry<String, List<String>> entry : mTypePropertyPathsMap.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Returns a map from schema type to property paths to be used for projection.
     *
     * <p>If the map is empty, then all properties will be retrieved for all results.
     *
     * <p>A more efficient version of {@link #getProjections}, but it returns a modifiable map.
     * This is not meant to be unhidden and should only be used by internal classes.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Map<String, List<String>> getProjectionsInternal() {
        return mTypePropertyPathsMap;
    }

    /**
     * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated
    @NonNull
    public GetByDocumentIdRequest toGetByDocumentIdRequest() {
        GetByDocumentIdRequest.Builder builder = new GetByDocumentIdRequest.Builder(mNamespace)
                .addIds(mIds);
        for (Map.Entry<String, List<String>> projection : mTypePropertyPathsMap.entrySet()) {
            builder.addProjection(projection.getKey(), projection.getValue());
        }
        return builder.build();
    }

    /**
     * Builder for {@link GetByUriRequest} objects.
     *
     * <p>Once {@link #build} is called, the instance can no longer be used.
     */
    public static final class Builder {
        private final String mNamespace;
        private final Set<String> mIds = new ArraySet<>();
        private final Map<String, List<String>> mProjectionTypePropertyPaths = new ArrayMap<>();
        private boolean mBuilt = false;

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        public Builder(@NonNull String namespace) {
            mNamespace = Preconditions.checkNotNull(namespace);
        }

        /**
         * Adds one or more document IDs to the request.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public Builder addUris(@NonNull String... ids) {
            Preconditions.checkNotNull(ids);
            return addUris(Arrays.asList(ids));
        }

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        @NonNull
        public Builder addUris(@NonNull Collection<String> ids) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(ids);
            mIds.addAll(ids);
            return this;
        }

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        @NonNull
        public Builder addProjection(
                @NonNull String schemaType, @NonNull Collection<String> propertyPaths) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(propertyPaths);
            List<String> propertyPathsList = new ArrayList<>(propertyPaths.size());
            for (String propertyPath : propertyPaths) {
                Preconditions.checkNotNull(propertyPath);
                propertyPathsList.add(propertyPath);
            }
            mProjectionTypePropertyPaths.put(schemaType, propertyPathsList);
            return this;
        }

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        @NonNull
        public GetByUriRequest build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBuilt = true;
            return new GetByUriRequest(mNamespace, mIds, mProjectionTypePropertyPaths);
        }
    }
}
