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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.GetByDocumentIdRequestCreator;
import androidx.appsearch.util.BundleUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Encapsulates a request to retrieve documents by namespace and IDs from the
 * {@link AppSearchSession} database.
 *
 * @see AppSearchSession#getByDocumentIdAsync
 */
@SuppressWarnings("HiddenSuperclass")
@SafeParcelable.Class(creator = "GetByDocumentIdRequestCreator")
public final class GetByDocumentIdRequest extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @NonNull public static final Parcelable.Creator<GetByDocumentIdRequest> CREATOR =
            new GetByDocumentIdRequestCreator();
    /**
     * Schema type to be used in
     * {@link GetByDocumentIdRequest.Builder#addProjection}
     * to apply property paths to all results, excepting any types that have had their own, specific
     * property paths set.
     */
    public static final String PROJECTION_SCHEMA_TYPE_WILDCARD = "*";

    @NonNull
    @Field(id = 1, getter = "getNamespace")
    private final String mNamespace;
    @NonNull
    @Field(id = 2)
    final List<String> mIds;
    @NonNull
    @Field(id = 3)
    final Bundle mTypePropertyPaths;

    /**
     * Cache of the ids. Comes from inflating mIds at first use.
     */
    @Nullable private Set<String> mIdsCached;

    @Constructor
    GetByDocumentIdRequest(
            @Param(id = 1) @NonNull String namespace,
            @Param(id = 2) @NonNull List<String> ids,
            @Param(id = 3) @NonNull Bundle typePropertyPaths) {
        mNamespace = Objects.requireNonNull(namespace);
        mIds = Objects.requireNonNull(ids);
        mTypePropertyPaths = Objects.requireNonNull(typePropertyPaths);
    }

    /** Returns the namespace attached to the request. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the set of document IDs attached to the request. */
    @NonNull
    public Set<String> getIds() {
        if (mIdsCached == null) {
            mIdsCached = Collections.unmodifiableSet(new ArraySet<>(mIds));
        }
        return mIdsCached;
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
        Set<String> schemas = mTypePropertyPaths.keySet();
        Map<String, List<String>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            List<String> propertyPaths = mTypePropertyPaths.getStringArrayList(schema);
            if (propertyPaths != null) {
                typePropertyPathsMap.put(schema, Collections.unmodifiableList(propertyPaths));
            }
        }
        return typePropertyPathsMap;
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
    public Map<String, List<PropertyPath>> getProjectionPaths() {
        Set<String> schemas = mTypePropertyPaths.keySet();
        Map<String, List<PropertyPath>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            List<String> paths = mTypePropertyPaths.getStringArrayList(schema);
            if (paths != null) {
                int pathsSize = paths.size();
                List<PropertyPath> propertyPathList = new ArrayList<>(pathsSize);
                for (int i = 0; i < pathsSize; i++) {
                    propertyPathList.add(new PropertyPath(paths.get(i)));
                }
                typePropertyPathsMap.put(schema, Collections.unmodifiableList(propertyPathList));
            }
        }
        return typePropertyPathsMap;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        GetByDocumentIdRequestCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link GetByDocumentIdRequest} objects. */
    public static final class Builder {
        private final String mNamespace;
        private List<String> mIds = new ArrayList<>();
        private Bundle mProjectionTypePropertyPaths = new Bundle();
        private boolean mBuilt = false;

        /** Creates a {@link GetByDocumentIdRequest.Builder} instance. */
        public Builder(@NonNull String namespace) {
            mNamespace = Preconditions.checkNotNull(namespace);
        }

        /** Adds one or more document IDs to the request. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addIds(@NonNull String... ids) {
            Preconditions.checkNotNull(ids);
            resetIfBuilt();
            return addIds(Arrays.asList(ids));
        }

        /** Adds a collection of IDs to the request. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addIds(@NonNull Collection<String> ids) {
            Preconditions.checkNotNull(ids);
            resetIfBuilt();
            mIds.addAll(ids);
            return this;
        }

        /**
         * Adds property paths for the specified type to be used for projection. If property
         * paths are added for a type, then only the properties referred to will be retrieved for
         * results of that type. If a property path that is specified isn't present in a result,
         * it will be ignored for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of
         * results of that type will be retrieved.
         *
         * <p>If property path is added for the
         * {@link GetByDocumentIdRequest#PROJECTION_SCHEMA_TYPE_WILDCARD}, then those property paths
         * will apply to all results, excepting any types that have their own, specific property
         * paths set.
         *
         * @see SearchSpec.Builder#addProjectionPaths
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addProjection(
                @NonNull String schemaType, @NonNull Collection<String> propertyPaths) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(propertyPaths);
            resetIfBuilt();
            ArrayList<String> propertyPathsList = new ArrayList<>(propertyPaths.size());
            for (String propertyPath : propertyPaths) {
                Preconditions.checkNotNull(propertyPath);
                propertyPathsList.add(propertyPath);
            }
            mProjectionTypePropertyPaths.putStringArrayList(schemaType, propertyPathsList);
            return this;
        }

        /**
         * Adds property paths for the specified type to be used for projection. If property
         * paths are added for a type, then only the properties referred to will be retrieved for
         * results of that type. If a property path that is specified isn't present in a result,
         * it will be ignored for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of
         * results of that type will be retrieved.
         *
         * <p>If property path is added for the
         * {@link GetByDocumentIdRequest#PROJECTION_SCHEMA_TYPE_WILDCARD}, then those property paths
         * will apply to all results, excepting any types that have their own, specific property
         * paths set.
         *
         * @see SearchSpec.Builder#addProjectionPaths
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addProjectionPaths(
                @NonNull String schemaType, @NonNull Collection<PropertyPath> propertyPaths) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(propertyPaths);
            List<String> propertyPathsList = new ArrayList<>(propertyPaths.size());
            for (PropertyPath propertyPath : propertyPaths) {
                propertyPathsList.add(propertyPath.toString());
            }
            return addProjection(schemaType, propertyPathsList);
        }

        /** Builds a new {@link GetByDocumentIdRequest}. */
        @NonNull
        public GetByDocumentIdRequest build() {
            mBuilt = true;
            return new GetByDocumentIdRequest(mNamespace, mIds, mProjectionTypePropertyPaths);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mIds = new ArrayList<>(mIds);
                // No need to clone each propertyPathsList inside mProjectionTypePropertyPaths since
                // the builder only replaces it, never adds to it. So even if the builder is used
                // again, the previous one will remain with the object.
                mProjectionTypePropertyPaths = BundleUtil.deepCopy(mProjectionTypePropertyPaths);
                mBuilt = false;
            }
        }
    }
}
