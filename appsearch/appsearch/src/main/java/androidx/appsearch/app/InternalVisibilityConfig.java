/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.InternalVisibilityConfigCreator;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An expanded version of {@link VisibilityConfig} which includes fields for internal use by
 * AppSearch.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SafeParcelable.Class(creator = "InternalVisibilityConfigCreator")
public final class InternalVisibilityConfig extends AbstractSafeParcelable {
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final Parcelable.Creator<InternalVisibilityConfig> CREATOR =
            new InternalVisibilityConfigCreator();

    /**
     * Build the List of {@link InternalVisibilityConfig}s from visibility settings.
     */
    @NonNull
    public static List<InternalVisibilityConfig> toInternalVisibilityConfigs(
            @NonNull SetSchemaRequest setSchemaRequest) {
        Set<AppSearchSchema> searchSchemas = setSchemaRequest.getSchemas();
        Set<String> schemasNotDisplayedBySystem = setSchemaRequest.getSchemasNotDisplayedBySystem();
        Map<String, Set<PackageIdentifier>> schemasVisibleToPackages =
                setSchemaRequest.getSchemasVisibleToPackages();
        Map<String, Set<Set<Integer>>> schemasVisibleToPermissions =
                setSchemaRequest.getRequiredPermissionsForSchemaTypeVisibility();
        Map<String, PackageIdentifier> publiclyVisibleSchemas =
                setSchemaRequest.getPubliclyVisibleSchemas();
        Map<String, Set<VisibilityConfig>> schemasVisibleToConfigs =
                setSchemaRequest.getSchemasVisibleToConfigs();

        List<InternalVisibilityConfig> result = new ArrayList<>(searchSchemas.size());
        for (AppSearchSchema searchSchema : searchSchemas) {
            String schemaType = searchSchema.getSchemaType();
            InternalVisibilityConfig.Builder builder =
                    new InternalVisibilityConfig.Builder(schemaType)
                            .setNotDisplayedBySystem(
                                    schemasNotDisplayedBySystem.contains(schemaType));

            Set<PackageIdentifier> visibleToPackages = schemasVisibleToPackages.get(schemaType);
            if (visibleToPackages != null) {
                for (PackageIdentifier packageIdentifier : visibleToPackages) {
                    builder.addVisibleToPackage(packageIdentifier);
                }
            }

            Set<Set<Integer>> visibleToPermissionSets = schemasVisibleToPermissions.get(schemaType);
            if (visibleToPermissionSets != null) {
                for (Set<Integer> visibleToPermissions : visibleToPermissionSets) {
                    builder.addVisibleToPermissions(visibleToPermissions);
                }
            }

            PackageIdentifier publiclyVisibleTargetPackage = publiclyVisibleSchemas.get(schemaType);
            if (publiclyVisibleTargetPackage != null) {
                builder.setPubliclyVisibleTargetPackage(publiclyVisibleTargetPackage);
            }

            Set<VisibilityConfig> visibleToConfigs = schemasVisibleToConfigs.get(schemaType);
            if (visibleToConfigs != null) {
                for (VisibilityConfig visibilityConfig : visibleToConfigs) {
                    builder.addVisibleToConfig(visibilityConfig);
                }
            }

            result.add(builder.build());
        }
        return result;
    }

    @NonNull
    @Field(id = 1, getter = "getSchemaType")
    private final String mSchemaType;

    @Field(id = 2, getter = "isNotDisplayedBySystem")
    private final boolean mIsNotDisplayedBySystem;

    /** The public visibility settings available in VisibilityConfig. */
    @NonNull
    @Field(id = 3, getter = "getVisibilityConfig")
    private final VisibilityConfig mVisibilityConfig;

    /** Extended visibility settings from {@link SetSchemaRequest#getSchemasVisibleToConfigs()} */
    @NonNull
    @Field(id = 4)
    final List<VisibilityConfig> mVisibleToConfigs;

    @Constructor
    InternalVisibilityConfig(
            @Param(id = 1) @NonNull String schemaType,
            @Param(id = 2) boolean isNotDisplayedBySystem,
            @Param(id = 3) @NonNull VisibilityConfig visibilityConfig,
            @Param(id = 4) @NonNull List<VisibilityConfig> visibleToConfigs) {
        mIsNotDisplayedBySystem = isNotDisplayedBySystem;
        mSchemaType = Objects.requireNonNull(schemaType);
        mVisibilityConfig = Objects.requireNonNull(visibilityConfig);
        mVisibleToConfigs = Objects.requireNonNull(visibleToConfigs);
    }

    /**
     * Gets the schemaType for this VisibilityConfig.
     *
     * <p>This is being used as the document id when we convert a {@link InternalVisibilityConfig}
     * to a {@link GenericDocument}.
     */
    @NonNull
    public String getSchemaType() {
        return mSchemaType;
    }

    /** Returns whether this schema is visible to the system. */
    public boolean isNotDisplayedBySystem() {
        return mIsNotDisplayedBySystem;
    }

    /** Returns the visibility settings stored in the public {@link VisibilityConfig} object. */
    @NonNull
    public VisibilityConfig getVisibilityConfig() {
        return mVisibilityConfig;
    }

    /**
     * Returns required {@link VisibilityConfig} sets for a caller need to match to access the
     * schema this {@link InternalVisibilityConfig} represents.
     */
    @NonNull
    public Set<VisibilityConfig> getVisibleToConfigs() {
        return new ArraySet<>(mVisibleToConfigs);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        InternalVisibilityConfigCreator.writeToParcel(this, dest, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalVisibilityConfig)) return false;
        InternalVisibilityConfig that = (InternalVisibilityConfig) o;
        return mIsNotDisplayedBySystem == that.mIsNotDisplayedBySystem
                && Objects.equals(mSchemaType, that.mSchemaType)
                && Objects.equals(mVisibilityConfig, that.mVisibilityConfig)
                && Objects.equals(mVisibleToConfigs, that.mVisibleToConfigs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsNotDisplayedBySystem, mSchemaType, mVisibilityConfig,
                mVisibleToConfigs);
    }

    /** The builder class of {@link InternalVisibilityConfig}. */
    @FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
    public static final class Builder {
        private String mSchemaType;
        private boolean mIsNotDisplayedBySystem;
        private VisibilityConfig.Builder mVisibilityConfigBuilder;
        private List<VisibilityConfig> mVisibleToConfigs = new ArrayList<>();
        private boolean mBuilt;

        /**
         * Creates a {@link Builder} for a {@link InternalVisibilityConfig}.
         *
         * @param schemaType The SchemaType of the {@link AppSearchSchema} that this {@link
         *                   InternalVisibilityConfig} represents. The package and database prefix
         *                   will be added in server side. We are using prefixed schema type to be
         *                   the final id of this {@link InternalVisibilityConfig}. This will be
         *                   used as as an AppSearch id.
         * @see GenericDocument#getId
         */
        public Builder(@NonNull String schemaType) {
            mSchemaType = Objects.requireNonNull(schemaType);
            mVisibilityConfigBuilder = new VisibilityConfig.Builder();
        }

        /** Creates a {@link Builder} from an existing {@link InternalVisibilityConfig} */
        public Builder(@NonNull InternalVisibilityConfig internalVisibilityConfig) {
            Objects.requireNonNull(internalVisibilityConfig);
            mSchemaType = internalVisibilityConfig.mSchemaType;
            mIsNotDisplayedBySystem = internalVisibilityConfig.mIsNotDisplayedBySystem;
            mVisibilityConfigBuilder = new VisibilityConfig.Builder(
                    internalVisibilityConfig.getVisibilityConfig());
            mVisibleToConfigs = internalVisibilityConfig.mVisibleToConfigs;
        }

        /** Sets schemaType, which will be as the id when converting to {@link GenericDocument}. */
        @NonNull
        @CanIgnoreReturnValue
        public Builder setSchemaType(@NonNull String schemaType) {
            resetIfBuilt();
            mSchemaType = Objects.requireNonNull(schemaType);
            return this;
        }

        /**
         * Resets all values contained in the VisibilityConfig with the values from the given
         * VisibiltiyConfig.
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder setVisibilityConfig(@NonNull VisibilityConfig visibilityConfig) {
            resetIfBuilt();
            mVisibilityConfigBuilder = new VisibilityConfig.Builder(visibilityConfig);
            return this;
        }

        /**
         * Sets whether this schema has opted out of platform surfacing.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setNotDisplayedBySystem(boolean notDisplayedBySystem) {
            resetIfBuilt();
            mIsNotDisplayedBySystem = notDisplayedBySystem;
            return this;
        }

        /**
         * Add {@link PackageIdentifier} of packages which has access to this schema.
         *
         * @see VisibilityConfig.Builder#addVisibleToPackage
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addVisibleToPackage(@NonNull PackageIdentifier packageIdentifier) {
            resetIfBuilt();
            mVisibilityConfigBuilder.addVisibleToPackage(packageIdentifier);
            return this;
        }

        /**
         * Clears the list of packages which have access to this schema.
         *
         * @see VisibilityConfig.Builder#clearVisibleToPackages
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearVisibleToPackages() {
            resetIfBuilt();
            mVisibilityConfigBuilder.clearVisibleToPackages();
            return this;
        }

        /**
         * Adds a set of required Android {@link android.Manifest.permission} combination a package
         * needs to hold to access the schema.
         *
         * @see VisibilityConfig.Builder#addVisibleToPermissions
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addVisibleToPermissions(@NonNull Set<Integer> visibleToPermissions) {
            resetIfBuilt();
            mVisibilityConfigBuilder.addVisibleToPermissions(visibleToPermissions);
            return this;
        }

        /**
         * Clears all required permissions combinations set to this {@link VisibilityConfig}.
         *
         * @see VisibilityConfig.Builder#clearVisibleToPermissions
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearVisibleToPermissions() {
            resetIfBuilt();
            mVisibilityConfigBuilder.clearVisibleToPermissions();
            return this;
        }

        /**
         * Specify that this schema should be publicly available, to the same packages that have
         * visibility to the package passed as a parameter. This visibility is determined by the
         * result of {@link android.content.pm.PackageManager#canPackageQuery}.
         *
         * @see VisibilityConfig.Builder#setPubliclyVisibleTargetPackage
         */
        @NonNull
        public Builder setPubliclyVisibleTargetPackage(
                @Nullable PackageIdentifier packageIdentifier) {
            resetIfBuilt();
            mVisibilityConfigBuilder.setPubliclyVisibleTargetPackage(packageIdentifier);
            return this;
        }

        /**
         * Add the {@link VisibilityConfig} for a caller need to match to access the schema this
         * {@link InternalVisibilityConfig} represents.
         *
         * <p> You can call this method repeatedly to add multiple {@link VisibilityConfig}, and the
         * querier will have access if they match ANY of the {@link VisibilityConfig}.
         *
         * @param visibilityConfig The {@link VisibilityConfig} hold all requirements that a call
         *                         must match to access the schema.
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder addVisibleToConfig(@NonNull VisibilityConfig visibilityConfig) {
            Objects.requireNonNull(visibilityConfig);
            resetIfBuilt();
            mVisibleToConfigs.add(visibilityConfig);
            return this;
        }

        /** Clears the set of {@link VisibilityConfig} which have access to this schema. */
        @NonNull
        @CanIgnoreReturnValue
        public Builder clearVisibleToConfig() {
            resetIfBuilt();
            mVisibleToConfigs.clear();
            return this;
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mVisibleToConfigs = new ArrayList<>(mVisibleToConfigs);
                mBuilt = false;
            }
        }

        /** Build a {@link InternalVisibilityConfig} */
        @NonNull
        public InternalVisibilityConfig build() {
            mBuilt = true;
            return new InternalVisibilityConfig(
                    mSchemaType,
                    mIsNotDisplayedBySystem,
                    mVisibilityConfigBuilder.build(),
                    mVisibleToConfigs);
        }
    }
}
