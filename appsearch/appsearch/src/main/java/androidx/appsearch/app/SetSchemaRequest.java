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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.exceptions.AppSearchException;
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
 * Encapsulates a request to update the schema of an {@link AppSearchSession} database.
 *
 * @see AppSearchSession#setSchema
 */
public final class SetSchemaRequest {
    private final Set<AppSearchSchema> mSchemas;
    private final Set<String> mSchemasNotVisibleToSystemUi;
    private final Map<String, Set<PackageIdentifier>> mSchemasVisibleToPackages;
    private final Map<String, AppSearchSchema.Migrator> mMigrators;
    private final boolean mForceOverride;

    SetSchemaRequest(@NonNull Set<AppSearchSchema> schemas,
            @NonNull Set<String> schemasNotVisibleToSystemUi,
            @NonNull Map<String, Set<PackageIdentifier>> schemasVisibleToPackages,
            @NonNull Map<String, AppSearchSchema.Migrator> migrators,
            boolean forceOverride) {
        mSchemas = Preconditions.checkNotNull(schemas);
        mSchemasNotVisibleToSystemUi = Preconditions.checkNotNull(schemasNotVisibleToSystemUi);
        mSchemasVisibleToPackages = Preconditions.checkNotNull(schemasVisibleToPackages);
        mMigrators = Preconditions.checkNotNull(migrators);
        mForceOverride = forceOverride;
    }

    /** Returns the schemas that are part of this request. */
    @NonNull
    public Set<AppSearchSchema> getSchemas() {
        return Collections.unmodifiableSet(mSchemas);
    }

    /**
     * Returns the set of schema types that have opted out of being visible on system UI surfaces.
     */
    @NonNull
    public Set<String> getSchemasNotVisibleToSystemUi() {
        return Collections.unmodifiableSet(mSchemasNotVisibleToSystemUi);
    }

    /**
     * Returns a mapping of schema types to the set of packages that have access
     * to that schema type. Each package is represented by a {@link PackageIdentifier}.
     * name and byte[] certificate.
     *
     * This method is inefficient to call repeatedly.
     */
    @NonNull
    public Map<String, Set<PackageIdentifier>> getSchemasVisibleToPackages() {
        Map<String, Set<PackageIdentifier>> copy = new ArrayMap<>();
        for (String key : mSchemasVisibleToPackages.keySet()) {
            copy.put(key, new ArraySet<>(mSchemasVisibleToPackages.get(key)));
        }
        return copy;
    }

    /**
     * Returns the map of {@link androidx.appsearch.app.AppSearchSchema.Migrator}.
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Map<String, AppSearchSchema.Migrator> getMigrator() {
        return Collections.unmodifiableMap(mMigrators);
    }

    /**
     * Returns a mapping of schema types to the set of packages that have access
     * to that schema type. Each package is represented by a {@link PackageIdentifier}.
     * name and byte[] certificate.
     *
     * A more efficient version of {@link #getSchemasVisibleToPackages}, but it returns a
     * modifiable map. This is not meant to be unhidden and should only be used by internal
     * classes.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Map<String, Set<PackageIdentifier>> getSchemasVisibleToPackagesInternal() {
        return mSchemasVisibleToPackages;
    }

    /** Returns whether this request will force the schema to be overridden. */
    public boolean isForceOverride() {
        return mForceOverride;
    }

    /** Builder for {@link SetSchemaRequest} objects. */
    public static final class Builder {
        private final Set<AppSearchSchema> mSchemas = new ArraySet<>();
        private final Set<String> mSchemasNotVisibleToSystemUi = new ArraySet<>();
        private final Map<String, Set<PackageIdentifier>> mSchemasVisibleToPackages =
                new ArrayMap<>();
        private final Map<String, AppSearchSchema.Migrator> mMigrators = new ArrayMap<>();
        private boolean mForceOverride = false;
        private boolean mBuilt = false;

        /**
         * Adds one or more types to the schema.
         *
         * <p>Any documents of these types will be visible on system UI surfaces by default.
         */
        @NonNull
        public Builder addSchema(@NonNull AppSearchSchema... schemas) {
            Preconditions.checkNotNull(schemas);
            return addSchema(Arrays.asList(schemas));
        }

        /**
         * Adds one or more types to the schema.
         *
         * <p>Any documents of these types will be visible on system UI surfaces by default.
         */
        @NonNull
        public Builder addSchema(@NonNull Collection<AppSearchSchema> schemas) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(schemas);
            mSchemas.addAll(schemas);
            return this;
        }

// @exportToFramework:startStrip()
        /**
         * Adds one or more types to the schema.
         *
         * <p>Any documents of these types will be visible on system UI surfaces by default.
         *
         * @param dataClasses classes annotated with
         *                    {@link androidx.appsearch.annotation.AppSearchDocument}.
         * @throws AppSearchException if {@code androidx.appsearch.compiler.AppSearchCompiler}
         *                            has not generated a schema for the given data classes.
         */
        @SuppressLint("MissingGetterMatchingBuilder")  // Merged list available from getSchemas()
        @NonNull
        public Builder addDataClass(@NonNull Class<?>... dataClasses)
                throws AppSearchException {
            Preconditions.checkNotNull(dataClasses);
            return addDataClass(Arrays.asList(dataClasses));
        }

        /**
         * Adds one or more types to the schema.
         *
         * <p>Any documents of these types will be visible on system UI surfaces by default.
         *
         * @param dataClasses classes annotated with
         *                    {@link androidx.appsearch.annotation.AppSearchDocument}.
         * @throws AppSearchException if {@code androidx.appsearch.compiler.AppSearchCompiler}
         *                            has not generated a schema for the given data classes.
         */
        @SuppressLint("MissingGetterMatchingBuilder")  // Merged list available from getSchemas()
        @NonNull
        public Builder addDataClass(@NonNull Collection<? extends Class<?>> dataClasses)
                throws AppSearchException {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(dataClasses);
            List<AppSearchSchema> schemas = new ArrayList<>(dataClasses.size());
            DataClassFactoryRegistry registry = DataClassFactoryRegistry.getInstance();
            for (Class<?> dataClass : dataClasses) {
                DataClassFactory<?> factory = registry.getOrCreateFactory(dataClass);
                schemas.add(factory.getSchema());
            }
            return addSchema(schemas);
        }
// @exportToFramework:endStrip()

        /**
         * Sets visibility on system UI surfaces for the given {@code schemaType}.
         *
         * @param schemaType The schema type to set visibility on.
         * @param visible    Whether the {@code schemaType} will be visible or not.
         */
        // Merged list available from getSchemasNotVisibleToSystemUi
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setSchemaTypeVisibilityForSystemUi(@NonNull String schemaType,
                boolean visible) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkState(!mBuilt, "Builder has already been used");

            if (visible) {
                mSchemasNotVisibleToSystemUi.remove(schemaType);
            } else {
                mSchemasNotVisibleToSystemUi.add(schemaType);
            }
            return this;
        }

        /**
         * Sets visibility for a package for the given {@code schemaType}.
         *
         * @param schemaType        The schema type to set visibility on.
         * @param visible           Whether the {@code schemaType} will be visible or not.
         * @param packageIdentifier Represents the package that will be granted visibility.
         */
        // Merged list available from getSchemasVisibleToPackages
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setSchemaTypeVisibilityForPackage(@NonNull String schemaType,
                boolean visible, @NonNull PackageIdentifier packageIdentifier) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(packageIdentifier);
            Preconditions.checkState(!mBuilt, "Builder has already been used");

            Set<PackageIdentifier> packageIdentifiers =
                    mSchemasVisibleToPackages.get(schemaType);
            if (visible) {
                if (packageIdentifiers == null) {
                    packageIdentifiers = new ArraySet<>();
                }
                packageIdentifiers.add(packageIdentifier);
                mSchemasVisibleToPackages.put(schemaType, packageIdentifiers);
            } else {
                if (packageIdentifiers == null) {
                    // Return early since there was nothing set to begin with.
                    return this;
                }
                packageIdentifiers.remove(packageIdentifier);
                if (packageIdentifiers.isEmpty()) {
                    // Remove the entire key so that we don't have empty sets as values.
                    mSchemasVisibleToPackages.remove(schemaType);
                }
            }

            return this;
        }

        /**
         * set the {@link androidx.appsearch.app.AppSearchSchema.Migrator}.
         *
         * @param schemaType The schema type to set migrator on.
         * @param migrator   The migrator translate a document from it's old version to a new
         *                   incompatible version.
         * @hide
         */
        @NonNull
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Builder setMigrator(@NonNull String schemaType,
                @NonNull AppSearchSchema.Migrator migrator) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(migrator);
            mMigrators.put(schemaType, migrator);
            return this;
        }

// @exportToFramework:startStrip()
        /**
         * Sets visibility on system UI surfaces for the given {@code dataClass}.
         *
         * @param dataClass The schema to set visibility on.
         * @param visible   Whether the {@code schemaType} will be visible or not.
         * @return {@link SetSchemaRequest.Builder}
         * @throws AppSearchException if {@code androidx.appsearch.compiler.AppSearchCompiler}
         *                            has not generated a schema for the given data classes.
         */
        // Merged list available from getSchemasNotVisibleToSystemUi
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setDataClassVisibilityForSystemUi(@NonNull Class<?> dataClass,
                boolean visible) throws AppSearchException {
            Preconditions.checkNotNull(dataClass);

            DataClassFactoryRegistry registry = DataClassFactoryRegistry.getInstance();
            DataClassFactory<?> factory = registry.getOrCreateFactory(dataClass);
            return setSchemaTypeVisibilityForSystemUi(factory.getSchemaType(), visible);
        }

        /**
         * Sets visibility for a package for the given {@code dataClass}.
         *
         * @param dataClass         The schema to set visibility on.
         * @param visible           Whether the {@code schemaType} will be visible or not.
         * @param packageIdentifier Represents the package that will be granted visibility
         * @return {@link SetSchemaRequest.Builder}
         * @throws AppSearchException if {@code androidx.appsearch.compiler.AppSearchCompiler}
         *                            has not generated a schema for the given data classes.
         */
        // Merged list available from getSchemasVisibleToPackages
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setDataClassVisibilityForPackage(@NonNull Class<?> dataClass,
                boolean visible, @NonNull PackageIdentifier packageIdentifier)
                throws AppSearchException {
            Preconditions.checkNotNull(dataClass);

            DataClassFactoryRegistry registry = DataClassFactoryRegistry.getInstance();
            DataClassFactory<?> factory = registry.getOrCreateFactory(dataClass);
            return setSchemaTypeVisibilityForPackage(factory.getSchemaType(), visible,
                    packageIdentifier);
        }
// @exportToFramework:endStrip()

        /**
         * Configures the {@link SetSchemaRequest} to delete any existing documents that don't
         * follow the new schema.
         *
         * <p>By default, this is {@code false} and schema incompatibility causes the
         * {@link AppSearchSession#setSchema} call to fail.
         *
         * @see AppSearchSession#setSchema
         */
        @NonNull
        public Builder setForceOverride(boolean forceOverride) {
            mForceOverride = forceOverride;
            return this;
        }

        /**
         * Builds a new {@link SetSchemaRequest}.
         *
         * @throws IllegalArgumentException If schema types were referenced, but the
         *                                  corresponding {@link AppSearchSchema} was never added.
         */
        @NonNull
        public SetSchemaRequest build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBuilt = true;

            // Verify that any schema types with visibility settings refer to a real schema.
            // Create a copy because we're going to remove from the set for verification purposes.
            Set<String> referencedSchemas = new ArraySet<>(
                    mSchemasNotVisibleToSystemUi);
            referencedSchemas.addAll(mSchemasVisibleToPackages.keySet());

            for (AppSearchSchema schema : mSchemas) {
                referencedSchemas.remove(schema.getSchemaType());
            }
            if (!referencedSchemas.isEmpty()) {
                // We still have schema types that weren't seen in our mSchemas set. This means
                // there wasn't a corresponding AppSearchSchema.
                throw new IllegalArgumentException(
                        "Schema types " + referencedSchemas
                                + " referenced, but were not added.");
            }

            return new SetSchemaRequest(mSchemas, mSchemasNotVisibleToSystemUi,
                    mSchemasVisibleToPackages, mMigrators, mForceOverride);
        }
    }
}
