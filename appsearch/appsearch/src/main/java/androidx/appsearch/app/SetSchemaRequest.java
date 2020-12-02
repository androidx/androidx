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
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates a request to update the schema of an {@link AppSearchSession} database.
 *
 * @see AppSearchSession#setSchema
 */
public final class SetSchemaRequest {
    private final Set<AppSearchSchema> mSchemas;
    private final Set<String> mSchemasNotPlatformSurfaceable;
    private final boolean mForceOverride;

    SetSchemaRequest(@NonNull Set<AppSearchSchema> schemas,
            @NonNull Set<String> schemasNotPlatformSurfaceable, boolean forceOverride) {
        mSchemas = Preconditions.checkNotNull(schemas);
        mSchemasNotPlatformSurfaceable = Preconditions.checkNotNull(schemasNotPlatformSurfaceable);
        mForceOverride = forceOverride;
    }

    /** Returns the schemas that are part of this request. */
    @NonNull
    public Set<AppSearchSchema> getSchemas() {
        return Collections.unmodifiableSet(mSchemas);
    }

    /**
     * Returns the set of schema types that have opted out of being visible on system UI surfaces.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Set<String> getSchemasNotPlatformSurfaceable() {
        return Collections.unmodifiableSet(mSchemasNotPlatformSurfaceable);
    }

    /** Returns whether this request will force the schema to be overridden. */
    public boolean isForceOverride() {
        return mForceOverride;
    }

    /** Builder for {@link SetSchemaRequest} objects. */
    public static final class Builder {
        private final Set<AppSearchSchema> mSchemas = new ArraySet<>();
        private final Set<String> mSchemasNotPlatformSurfaceable = new ArraySet<>();
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
        public Builder addDataClass(@NonNull Collection<Class<?>> dataClasses)
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

        /**
         * Sets visibility on system UI surfaces for schema types.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setSchemaTypeVisibilityForSystemUi(boolean visible,
                @NonNull String... schemaTypes) {
            Preconditions.checkNotNull(schemaTypes);
            return this.setSchemaTypeVisibilityForSystemUi(visible, Arrays.asList(schemaTypes));
        }

        /**
         * Sets visibility on system UI surfaces for schema types.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setSchemaTypeVisibilityForSystemUi(boolean visible,
                @NonNull Collection<String> schemaTypes) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(schemaTypes);
            if (visible) {
                mSchemasNotPlatformSurfaceable.removeAll(schemaTypes);
            } else {
                mSchemasNotPlatformSurfaceable.addAll(schemaTypes);
            }
            return this;
        }

        /**
         * Sets visibility on system UI surfaces for schema types.
         *
         * @throws AppSearchException if {@code androidx.appsearch.compiler.AppSearchCompiler}
         *                            has not generated a schema for the given data classes.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setDataClassVisibilityForSystemUi(boolean visible,
                @NonNull Class<?>... dataClasses) throws AppSearchException {
            Preconditions.checkNotNull(dataClasses);
            return setDataClassVisibilityForSystemUi(visible, Arrays.asList(dataClasses));
        }

        /**
         * Sets visibility on system UI surfaces for schema types.
         *
         * @throws AppSearchException if {@code androidx.appsearch.compiler.AppSearchCompiler}
         *                            has not generated a schema for the given data classes.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setDataClassVisibilityForSystemUi(boolean visible,
                @NonNull Collection<Class<?>> dataClasses) throws AppSearchException {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(dataClasses);
            DataClassFactoryRegistry registry = DataClassFactoryRegistry.getInstance();
            for (Class<?> dataClass : dataClasses) {
                DataClassFactory<?> factory = registry.getOrCreateFactory(dataClass);
                if (visible) {
                    mSchemasNotPlatformSurfaceable.remove(factory.getSchemaType());
                } else {
                    mSchemasNotPlatformSurfaceable.add(factory.getSchemaType());
                }
            }
            return this;
        }

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
            Set<String> schemasNotPlatformSurfaceableCopy = new ArraySet<>(
                    mSchemasNotPlatformSurfaceable);
            for (AppSearchSchema schema : mSchemas) {
                schemasNotPlatformSurfaceableCopy.remove(schema.getSchemaType());
            }
            if (!schemasNotPlatformSurfaceableCopy.isEmpty()) {
                // We still have schema types that weren't seen in our mSchemas set. This means
                // there wasn't a corresponding AppSearchSchema.
                throw new IllegalArgumentException(
                        "Schema types " + schemasNotPlatformSurfaceableCopy
                                + " referenced, but were not added.");
            }

            return new SetSchemaRequest(mSchemas, mSchemasNotPlatformSurfaceable,
                    mForceOverride);
        }
    }
}
