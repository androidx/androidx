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
import androidx.appsearch.safeparcel.stub.StubCreators.VisibilityConfigCreator;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A class to hold a all necessary Visibility information corresponding to the same schema. This
 * pattern allows for easier association of these documents.
 *
 * <p> This does not correspond to any schema, the properties held in this class are kept in two
 * separate schemas, VisibilityConfig and PublicAclOverlay.
 */
@FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
@SafeParcelable.Class(creator = "VisibilityConfigCreator")
public final class VisibilityConfig extends AbstractSafeParcelable {
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final Parcelable.Creator<VisibilityConfig> CREATOR =
            new VisibilityConfigCreator();

    /**
     * Build the List of {@link VisibilityConfig}s from visibility settings.
     *
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static List<VisibilityConfig> toVisibilityConfigs(
            @NonNull SetSchemaRequest setSchemaRequest) {
        Set<AppSearchSchema> searchSchemas = setSchemaRequest.getSchemas();
        Map<String, PackageIdentifier> publiclyVisibleSchemas =
                setSchemaRequest.getPubliclyVisibleSchemas();
        Set<String> schemasNotDisplayedBySystem = setSchemaRequest.getSchemasNotDisplayedBySystem();
        Map<String, Set<PackageIdentifier>> schemasVisibleToPackages =
                setSchemaRequest.getSchemasVisibleToPackages();
        Map<String, Set<Set<Integer>>> schemasVisibleToPermissions =
                setSchemaRequest.getRequiredPermissionsForSchemaTypeVisibility();
        Map<String, Set<VisibilityConfig>> schemasVisibleToConfigs =
                setSchemaRequest.getSchemasVisibleToConfigs();
        List<VisibilityConfig> visibilityConfigs = new ArrayList<>(searchSchemas.size());

        for (AppSearchSchema searchSchema : searchSchemas) {
            String schemaType = searchSchema.getSchemaType();
            Builder visibilityConfigBuilder = new Builder(/*schemaType=*/ schemaType);

            visibilityConfigBuilder.setNotDisplayedBySystem(
                    schemasNotDisplayedBySystem.contains(schemaType));

            Set<PackageIdentifier> visibleToPackages = schemasVisibleToPackages.get(schemaType);
            if (visibleToPackages != null) {
                for (PackageIdentifier packageIdentifier : visibleToPackages) {
                    visibilityConfigBuilder.addVisibleToPackage(packageIdentifier);
                }
            }

            Set<Set<Integer>> visibleToPermissionSets = schemasVisibleToPermissions.get(schemaType);
            if (visibleToPermissionSets != null) {
                for (Set<Integer> visibleToPermissions : visibleToPermissionSets) {
                    visibilityConfigBuilder.addVisibleToPermissions(visibleToPermissions);
                }
            }

            PackageIdentifier targetPackage = publiclyVisibleSchemas.get(schemaType);
            if (targetPackage != null) {
                visibilityConfigBuilder.setPubliclyVisibleTargetPackage(targetPackage);
            }

            Set<VisibilityConfig> visibleToConfigs = schemasVisibleToConfigs.get(schemaType);
            if (visibleToConfigs != null) {
                for (VisibilityConfig visibilityConfig : visibleToConfigs) {
                    visibilityConfigBuilder.addVisibleToConfig(visibilityConfig);
                }
            }
            visibilityConfigs.add(visibilityConfigBuilder.build());
        }
        return visibilityConfigs;
    }

    @Nullable
    @Field(id = 1, getter = "getSchemaType")
    private final String mSchemaType;

    @Field(id = 2, getter = "isNotDisplayedBySystem")
    private final boolean mIsNotDisplayedBySystem;

    @NonNull
    @Field(id = 3)
    final List<String> mVisibleToPackageNames;

    @NonNull
    @Field(id = 4)
    final byte[][] mVisibleToPackageSha256Certs;

    @NonNull
    @Field(id = 5)
    final List<VisibilityPermissionConfig> mVisibilityPermissionConfigs;

    @Nullable
    @Field(id = 6)
    final String mPubliclyVisibleTargetPackage;

    @Nullable
    @Field(id = 7)
    final byte[] mPubliclyVisibleTargetPackageSha256Cert;

    @NonNull
    @Field(id = 8)
    final List<VisibilityConfig> mVisibleToConfigs;

    @Nullable
    private Integer mHashCode;
    private Set<Set<Integer>> mVisibleToPermissionsCached;
    private List<PackageIdentifier> mVisibleToPackageIdentifiersCached;

    /**
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Constructor
    public VisibilityConfig(
            @Param(id = 1) @Nullable String schemaType,
            @Param(id = 2) boolean isNotDisplayedBySystem,
            @Param(id = 3) @NonNull List<String> visibleToPackageNames,
            @Param(id = 4) @NonNull byte[][] visibleToPackageSha256Certs,
            @Param(id = 5) @NonNull List<VisibilityPermissionConfig> visibilityPermissionConfigs,
            @Param(id = 6) @Nullable String publiclyVisibleTargetPackage,
            @Param(id = 7) @Nullable byte[] publiclyVisibleTargetPackageSha256Cert,
            @Param(id = 8) @NonNull List<VisibilityConfig> visibleToConfigs) {
        mSchemaType = schemaType;
        mIsNotDisplayedBySystem = isNotDisplayedBySystem;
        mVisibleToPackageNames = Objects.requireNonNull(visibleToPackageNames);
        mVisibleToPackageSha256Certs = Objects.requireNonNull(visibleToPackageSha256Certs);
        mVisibilityPermissionConfigs = visibilityPermissionConfigs;
        mPubliclyVisibleTargetPackage = publiclyVisibleTargetPackage;
        mPubliclyVisibleTargetPackageSha256Cert = publiclyVisibleTargetPackageSha256Cert;
        mVisibleToConfigs = visibleToConfigs;
    }

    /**
     * Gets the schemaType for this VisibilityConfig.
     *
     * <p>This is being used as the document id when we convert a {@link VisibilityConfig} to a
     * {@link GenericDocument}.
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    public String getSchemaType() {
        return mSchemaType;
    }

    /** Returns whether this schema is visible to the system. */
    public boolean isNotDisplayedBySystem() {
        return mIsNotDisplayedBySystem;
    }

     /** Returns a list of {@link PackageIdentifier}s of packages that can access this schema. */
    @NonNull
    public List<PackageIdentifier> getVisibleToPackages() {
        if (mVisibleToPackageIdentifiersCached == null) {
            mVisibleToPackageIdentifiersCached = new ArrayList<>(mVisibleToPackageNames.size());
            for (int i = 0; i < mVisibleToPackageNames.size(); i++) {
                mVisibleToPackageIdentifiersCached.add(
                        new PackageIdentifier(mVisibleToPackageNames.get(i),
                                mVisibleToPackageSha256Certs[i]));
            }
        }
        return mVisibleToPackageIdentifiersCached;
    }

    /**
     * Returns an array of Integers representing Android Permissions as defined in
     * {@link SetSchemaRequest.AppSearchSupportedPermission} that the caller must hold to access the
     * schema this {@link VisibilityConfig} represents.
     */
    @NonNull
    public Set<Set<Integer>> getVisibleToPermissions() {
        if (mVisibilityPermissionConfigs == null) {
            return Collections.emptySet();
        }
        if (mVisibleToPermissionsCached == null) {
            mVisibleToPermissionsCached = new ArraySet<>(mVisibilityPermissionConfigs.size());
            for (VisibilityPermissionConfig permissionConfig : mVisibilityPermissionConfigs) {
                Set<Integer> requiredPermissions = permissionConfig.getAllRequiredPermissions();
                if (requiredPermissions != null) {
                    mVisibleToPermissionsCached.add(requiredPermissions);
                }
            }
        }
        return mVisibleToPermissionsCached;
    }

    /**
     * Returns the {@link PackageIdentifier} of the package that will be used as the target package
     * in a call to {@link android.content.pm.PackageManager#canPackageQuery} to determine which
     * packages can access this publicly visible schema. Returns null if the schema is not publicly
     * visible.
     */
    @Nullable
    public PackageIdentifier getPubliclyVisibleTargetPackage() {
        if (mPubliclyVisibleTargetPackage == null
                || mPubliclyVisibleTargetPackageSha256Cert == null) {
            return null;
        }
        return new PackageIdentifier(mPubliclyVisibleTargetPackage,
                mPubliclyVisibleTargetPackageSha256Cert);
    }

    /**
     * Returns required {@link VisibilityConfig} sets for a caller need to match to access the
     * schema this {@link VisibilityConfig} represents.
     *
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Set<VisibilityConfig> getVisibleToConfigs() {
        return new ArraySet<>(mVisibleToConfigs);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        VisibilityConfigCreator.writeToParcel(this, dest, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof VisibilityConfig)) return false;
        VisibilityConfig that = (VisibilityConfig) o;
        return Objects.equals(mSchemaType, that.mSchemaType)
                && mIsNotDisplayedBySystem == that.mIsNotDisplayedBySystem
                && Objects.equals(mVisibleToPackageNames, that.mVisibleToPackageNames)
                && Arrays.deepEquals(mVisibleToPackageSha256Certs,
                that.mVisibleToPackageSha256Certs)
                && Objects.equals(
                mVisibilityPermissionConfigs, that.mVisibilityPermissionConfigs)
                && Objects.equals(mPubliclyVisibleTargetPackage, that.mPubliclyVisibleTargetPackage)
                && Arrays.equals(mPubliclyVisibleTargetPackageSha256Cert,
                that.mPubliclyVisibleTargetPackageSha256Cert)
                && Objects.equals(mVisibleToConfigs, that.mVisibleToConfigs);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = Objects.hash(
                    mSchemaType,
                    mIsNotDisplayedBySystem,
                    mVisibleToPackageNames,
                    Arrays.deepHashCode(mVisibleToPackageSha256Certs),
                    mVisibilityPermissionConfigs,
                    mPubliclyVisibleTargetPackage,
                    Arrays.hashCode(mPubliclyVisibleTargetPackageSha256Cert),
                    mVisibleToConfigs);
        }
        return mHashCode;
    }

    /** The builder class of {@link VisibilityConfig}. */
    @FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
    public static final class Builder {
        private String mSchemaType;
        private boolean mIsNotDisplayedBySystem;
        private List<String> mVisibleToPackageNames = new ArrayList<>();
        private List<byte[]> mVisibleToPackageSha256Certs = new ArrayList<>();
        private List<VisibilityPermissionConfig> mVisibilityPermissionConfigs = new ArrayList<>();
        private String mPubliclyVisibleTargetPackage;
        private byte[] mPubliclyVisibleTargetPackageSha256Cert;
        private List<VisibilityConfig> mVisibleToConfigs = new ArrayList<>();
        private boolean mBuilt;

        /** Creates a {@link Builder} for a {@link VisibilityConfig}. */
        public Builder() {}

        /**
         * Creates a {@link Builder} for a {@link VisibilityConfig}.
         *
         * @param schemaType The SchemaType of the {@link AppSearchSchema} that this {@link
         *     VisibilityConfig} represents. The package and database prefix will be added in
         *     server side. We are using prefixed schema type to be the final id of this {@link
         *     VisibilityConfig}. This will be used as as an AppSearch id.
         * @see GenericDocument#getId
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Builder(@NonNull String schemaType) {
            mSchemaType = Objects.requireNonNull(schemaType);
        }

        /**
         * Creates a {@link Builder} from an existing {@link VisibilityConfig}
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Builder(@NonNull VisibilityConfig visibilityConfig) {
            Objects.requireNonNull(visibilityConfig);

            mSchemaType = visibilityConfig.mSchemaType;
            mIsNotDisplayedBySystem = visibilityConfig.mIsNotDisplayedBySystem;
            mVisibilityPermissionConfigs = visibilityConfig.mVisibilityPermissionConfigs;
            mVisibleToPackageNames = visibilityConfig.mVisibleToPackageNames;
            mVisibleToPackageSha256Certs =
                    Arrays.asList(visibilityConfig.mVisibleToPackageSha256Certs);
            mPubliclyVisibleTargetPackage = visibilityConfig.mPubliclyVisibleTargetPackage;
            mPubliclyVisibleTargetPackageSha256Cert =
                    visibilityConfig.mPubliclyVisibleTargetPackageSha256Cert;
            mVisibleToConfigs = visibilityConfig.mVisibleToConfigs;
        }

        /**
         *  Sets schemaType, which will be as the id when converting to {@link GenericDocument}.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @CanIgnoreReturnValue
        @NonNull
        public Builder setSchemaType(@NonNull String schemaType) {
            resetIfBuilt();
            mSchemaType = Objects.requireNonNull(schemaType);
            return this;
        }

        /** Sets whether this schema has opted out of platform surfacing. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setNotDisplayedBySystem(boolean notDisplayedBySystem) {
            resetIfBuilt();
            mIsNotDisplayedBySystem = notDisplayedBySystem;
            return this;
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addVisibleToPackage(@NonNull PackageIdentifier packageIdentifier) {
            Objects.requireNonNull(packageIdentifier);
            resetIfBuilt();
            mVisibleToPackageNames.add(packageIdentifier.getPackageName());
            mVisibleToPackageSha256Certs.add(packageIdentifier.getSha256Certificate());
            return this;
        }

        /** Clears the list of packages which have access to this schema. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearVisibleToPackages() {
            mVisibleToPackageNames = new ArrayList<>();
            mVisibleToPackageSha256Certs = new ArrayList<>();
            resetIfBuilt();
            return this;
        }

        /**
         * Adds a set of required Android {@link android.Manifest.permission} combination a
         * package needs to hold to access the schema this {@link VisibilityConfig} represents.
         *
         * <p> If the querier holds ALL of the required permissions in this combination, they will
         * have access to read {@link GenericDocument} objects of the given schema type.
         *
         * <p> You can call this method repeatedly to add multiple permission combinations, and the
         * querier will have access if they holds ANY of the combinations.
         *
         * <p>Merged Set available from {@link #getVisibleToPermissions()}.
         *
         * @see SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility for
         * supported Permissions.
         */
        @SuppressWarnings("RequiresPermission")  // No permission required to call this method
        @CanIgnoreReturnValue
        @NonNull
        public Builder addVisibleToPermissions(@NonNull Set<Integer> visibleToPermissions) {
            Objects.requireNonNull(visibleToPermissions);
            resetIfBuilt();
            mVisibilityPermissionConfigs.add(
                    new VisibilityPermissionConfig.Builder(visibleToPermissions).build());
            return this;
        }

        /**  Clears all required permissions combinations set to this {@link VisibilityConfig}.  */
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearVisibleToPermissions() {
            resetIfBuilt();
            mVisibilityPermissionConfigs.clear();
            return this;
        }

        /**
         * Specify that this schema should be publicly available, to the same packages that have
         * visibility to the package passed as a parameter. This visibility is determined by the
         * result of {@link android.content.pm.PackageManager#canPackageQuery}.
         *
         * <p> It is possible for the packageIdentifier parameter to be different from the
         * package performing the indexing. This might happen in the case of an on-device indexer
         * processing information about various packages. The visibility will be the same
         * regardless of which package indexes the document, as the visibility is based on the
         * packageIdentifier parameter.
         *
         * <p> Calling this with packageIdentifier set to null is valid, and will remove public
         * visibility for the schema.
         *
         * @param packageIdentifier the {@link PackageIdentifier} of the package that will be used
         *                          as the target package in a call to {@link
         *                          android.content.pm.PackageManager#canPackageQuery} to determine
         *                          which packages can access this publicly visible schema.
         */
        @NonNull
        public Builder setPubliclyVisibleTargetPackage(
                @Nullable PackageIdentifier packageIdentifier) {
            resetIfBuilt();
            if (packageIdentifier != null) {
                mPubliclyVisibleTargetPackage = packageIdentifier.getPackageName();
                mPubliclyVisibleTargetPackageSha256Cert = packageIdentifier.getSha256Certificate();
            } else {
                mPubliclyVisibleTargetPackage = null;
                mPubliclyVisibleTargetPackageSha256Cert = null;
            }
            return this;
        }

        /**
         * Add the {@link VisibilityConfig} for a caller need to match to access the schema this
         * {@link VisibilityConfig} represents.
         *
         * <p> You can call this method repeatedly to add multiple {@link VisibilityConfig}, and the
         * querier will have access if they match ANY of the {@link VisibilityConfig}.
         *
         * @param visibilityConfig      The {@link VisibilityConfig} hold all requirements that
         *                              a call must to match to access the schema.
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public Builder addVisibleToConfig(@NonNull VisibilityConfig visibilityConfig) {
            Objects.requireNonNull(visibilityConfig);
            resetIfBuilt();
            mVisibleToConfigs.add(visibilityConfig);
            return this;
        }

        /**
         *  Clears the set of {@link VisibilityConfig} which have access to this schema.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public Builder clearVisibleToConfig() {
            resetIfBuilt();
            mVisibleToConfigs.clear();
            return this;
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mVisibleToPackageNames = new ArrayList<>(mVisibleToPackageNames);
                mVisibleToPackageSha256Certs = new ArrayList<>(mVisibleToPackageSha256Certs);
                mVisibilityPermissionConfigs = new ArrayList<>(mVisibilityPermissionConfigs);
                mVisibleToConfigs = new ArrayList<>(mVisibleToConfigs);
                mBuilt = false;
            }
        }

        /** Build a {@link VisibilityConfig} */
        @NonNull
        public VisibilityConfig build() {
            mBuilt = true;
            return new VisibilityConfig(
                    mSchemaType,
                    mIsNotDisplayedBySystem,
                    mVisibleToPackageNames,
                    mVisibleToPackageSha256Certs.toArray(new byte[0][]),
                    mVisibilityPermissionConfigs,
                    mPubliclyVisibleTargetPackage,
                    mPubliclyVisibleTargetPackageSha256Cert,
                    mVisibleToConfigs);
        }
    }
}
