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

package androidx.appsearch.app;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The response class of {@link AppSearchSession#getSchema} */
public final class GetSchemaResponse {
    private static final String VERSION_FIELD = "version";
    private static final String SCHEMAS_FIELD = "schemas";
    private static final String SCHEMAS_NOT_DISPLAYED_BY_SYSTEM_FIELD =
            "schemasNotDisplayedBySystem";
    private static final String SCHEMAS_VISIBLE_TO_PACKAGES_FIELD = "schemasVisibleToPackages";
    /**
     * This Set contains all schemas that are not displayed by the system. all values in the set are
     * prefixed with the package-database prefix. We do lazy fetch, the object will be created
     * when the user first time fetch it.
     */
    @Nullable
    private Set<String> mSchemasNotDisplayedBySystem;
    // This map contains all schemas that are not displayed by the system. all keys in the map are
    // prefixed with the package-database prefix. We do lazy fetch, the object will be created
    // when the user first time fetch it.
    @Nullable
    private Map<String, Set<PackageIdentifier>> mSchemasVisibleToPackages;

    private final Bundle mBundle;

    GetSchemaResponse(@NonNull Bundle bundle) {
        mBundle = Preconditions.checkNotNull(bundle);
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Bundle getBundle() {
        return mBundle;
    }

    /**
     * Returns the overall database schema version.
     *
     * <p>If the database is empty, 0 will be returned.
     */
    @IntRange(from = 0)
    public int getVersion() {
        return mBundle.getInt(VERSION_FIELD);
    }

    /**
     * Return the schemas most recently successfully provided to
     * {@link AppSearchSession#setSchema}.
     *
     * <p>It is inefficient to call this method repeatedly.
     */
    @NonNull
    public Set<AppSearchSchema> getSchemas() {
        ArrayList<Bundle> schemaBundles = mBundle.getParcelableArrayList(SCHEMAS_FIELD);
        Set<AppSearchSchema> schemas = new ArraySet<>(schemaBundles.size());
        for (int i = 0; i < schemaBundles.size(); i++) {
            schemas.add(new AppSearchSchema(schemaBundles.get(i)));
        }
        return schemas;
    }

    /**
     * Returns all the schema types that are opted out of being displayed and visible on any
     * system UI surface.
     */
    // @exportToFramework:startStrip()
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GET_SCHEMA_RESPONSE_VISIBILITY)
    // @exportToFramework:endStrip()
    @NonNull
    public Set<String> getSchemaTypesNotDisplayedBySystem() {
        checkGetVisibilitySettingSupported();
        if (mSchemasNotDisplayedBySystem == null) {
            List<String> schemasNotDisplayedBySystemList =
                    mBundle.getStringArrayList(SCHEMAS_NOT_DISPLAYED_BY_SYSTEM_FIELD);
            mSchemasNotDisplayedBySystem =
                    Collections.unmodifiableSet(new ArraySet<>(schemasNotDisplayedBySystemList));
        }
        return mSchemasNotDisplayedBySystem;
    }

    /**
     * Returns a mapping of schema types to the set of packages that have access
     * to that schema type.
     */
    // @exportToFramework:startStrip()
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GET_SCHEMA_RESPONSE_VISIBILITY)
    // @exportToFramework:endStrip()
    @NonNull
    public Map<String, Set<PackageIdentifier>> getSchemaTypesVisibleToPackages() {
        checkGetVisibilitySettingSupported();
        if (mSchemasVisibleToPackages == null) {
            Bundle schemaVisibleToPackagesBundle =
                    mBundle.getBundle(SCHEMAS_VISIBLE_TO_PACKAGES_FIELD);
            Map<String, Set<PackageIdentifier>> copy = new ArrayMap<>();
            for (String key : schemaVisibleToPackagesBundle.keySet()) {
                List<Bundle> PackageIdentifierBundles = schemaVisibleToPackagesBundle
                        .getParcelableArrayList(key);
                Set<PackageIdentifier> packageIdentifiers =
                        new ArraySet<>(PackageIdentifierBundles.size());
                for (int i = 0; i < PackageIdentifierBundles.size(); i++) {
                    packageIdentifiers.add(new PackageIdentifier(PackageIdentifierBundles.get(i)));
                }
                copy.put(key, packageIdentifiers);
                mSchemasVisibleToPackages = Collections.unmodifiableMap(copy);
            }
        }
        return mSchemasVisibleToPackages;
    }

    private void checkGetVisibilitySettingSupported() {
        if (!mBundle.containsKey(SCHEMAS_VISIBLE_TO_PACKAGES_FIELD)) {
            throw new UnsupportedOperationException("Get visibility setting is not supported with"
                    + " this backend/Android API level combination.");
        }
    }

    /** Builder for {@link GetSchemaResponse} objects. */
    public static final class Builder {
        private int mVersion = 0;
        private ArrayList<Bundle> mSchemaBundles = new ArrayList<>();
        /**
         * Creates the object when we actually set them. If we never set visibility settings, we
         * should throw {@link UnsupportedOperationException} in the visibility getters.
         */
        @Nullable
        private ArrayList<String> mSchemasNotDisplayedBySystem;
        private Bundle mSchemasVisibleToPackages;
        private boolean mBuilt = false;

        /** Create a {@link Builder} object} */
        public Builder() {
            this(/*getVisibilitySettingSupported=*/true);
        }

        /**
         * Create a {@link Builder} object}.
         *
         * <p>This constructor should only be used in Android API below than T.
         *
         * @param getVisibilitySettingSupported whether supported
         * {@link Features#GET_SCHEMA_RESPONSE_VISIBILITY} by this backend/Android API level.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Builder(boolean getVisibilitySettingSupported) {
            if (getVisibilitySettingSupported) {
                mSchemasNotDisplayedBySystem = new ArrayList<>();
                mSchemasVisibleToPackages = new Bundle();
            }
        }

        /**
         * Sets the database overall schema version.
         *
         * <p>Default version is 0
         */
        @NonNull
        public Builder setVersion(@IntRange(from = 0) int version) {
            resetIfBuilt();
            mVersion = version;
            return this;
        }

        /**  Adds one {@link AppSearchSchema} to the schema list.  */
        @NonNull
        public Builder addSchema(@NonNull AppSearchSchema schema) {
            Preconditions.checkNotNull(schema);
            resetIfBuilt();
            mSchemaBundles.add(schema.getBundle());
            return this;
        }

        /**
         * Sets whether or not documents from the provided {@code schemaType} will be displayed
         * and visible on any system UI surface.
         *
         * @param schemaType The name of an {@link AppSearchSchema} within the same
         *                   {@link GetSchemaResponse}, which won't be displayed by system.
         */
        // Getter getSchemaTypesNotDisplayedBySystem returns plural objects.
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addSchemaTypeNotDisplayedBySystem(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            if (mSchemasNotDisplayedBySystem == null) {
                mSchemasNotDisplayedBySystem = new ArrayList<>();
            }
            mSchemasNotDisplayedBySystem.add(schemaType);
            return this;
        }

        /**
         * Sets whether or not documents from the provided {@code schemaType} can be read by the
         * specified package.
         *
         * <p>Each package is represented by a {@link PackageIdentifier}, containing a package name
         * and a byte array of type {@link android.content.pm.PackageManager#CERT_INPUT_SHA256}.
         *
         * <p>To opt into one-way data sharing with another application, the developer will need to
         * explicitly grant the other application’s package name and certificate Read access to its
         * data.
         *
         * <p>For two-way data sharing, both applications need to explicitly grant Read access to
         * one another.
         *
         * @param schemaType               The schema type to set visibility on.
         * @param packageIdentifiers       Represents the package that has access to the given
         *                                 schema type.
         */
        // Getter getSchemaTypesVisibleToPackages returns a map contains all schema types.
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setSchemaTypeVisibleToPackages(
                @NonNull String schemaType,
                @NonNull Set<PackageIdentifier> packageIdentifiers) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(packageIdentifiers);
            resetIfBuilt();
            ArrayList<Bundle> bundles = new ArrayList<>(packageIdentifiers.size());
            for (PackageIdentifier packageIdentifier : packageIdentifiers) {
                bundles.add(packageIdentifier.getBundle());
            }
            mSchemasVisibleToPackages.putParcelableArrayList(schemaType, bundles);
            return this;
        }

        /** Builds a {@link GetSchemaResponse} object. */
        @NonNull
        public GetSchemaResponse build() {
            Bundle bundle = new Bundle();
            bundle.putInt(VERSION_FIELD, mVersion);
            bundle.putParcelableArrayList(SCHEMAS_FIELD, mSchemaBundles);
            if (mSchemasNotDisplayedBySystem != null) {
                // Only save the visibility fields if it was actually set.
                bundle.putStringArrayList(SCHEMAS_NOT_DISPLAYED_BY_SYSTEM_FIELD,
                        mSchemasNotDisplayedBySystem);
                bundle.putBundle(SCHEMAS_VISIBLE_TO_PACKAGES_FIELD, mSchemasVisibleToPackages);
            }
            mBuilt = true;
            return new GetSchemaResponse(bundle);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mSchemaBundles = new ArrayList<>(mSchemaBundles);
                if (mSchemasNotDisplayedBySystem != null) {
                    // Only reset the visibility fields if it was actually set.
                    mSchemasNotDisplayedBySystem = new ArrayList<>(mSchemasNotDisplayedBySystem);
                    Bundle copyVisibleToPackages = new Bundle();
                    copyVisibleToPackages.putAll(mSchemasVisibleToPackages);
                    mSchemasVisibleToPackages = copyVisibleToPackages;
                }
                mBuilt = false;
            }
        }
    }
}
