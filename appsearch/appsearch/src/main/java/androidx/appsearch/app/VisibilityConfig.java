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
import androidx.appsearch.safeparcel.PackageIdentifierParcel;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.VisibilityConfigCreator;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.List;
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

    @NonNull
    @Field(id = 1)
    final List<PackageIdentifierParcel> mVisibleToPackages;

    @NonNull
    @Field(id = 2)
    final List<VisibilityPermissionConfig> mVisibleToPermissions;

    @Nullable
    @Field(id = 3)
    final PackageIdentifierParcel mPubliclyVisibleTargetPackage;

    @Nullable private Integer mHashCode;
    @Nullable private List<PackageIdentifier> mVisibleToPackagesCached;
    @Nullable private Set<Set<Integer>> mVisibleToPermissionsCached;

    @Constructor
    VisibilityConfig(
            @Param(id = 1) @NonNull List<PackageIdentifierParcel> visibleToPackages,
            @Param(id = 2) @NonNull List<VisibilityPermissionConfig> visibleToPermissions,
            @Param(id = 3) @Nullable PackageIdentifierParcel publiclyVisibleTargetPackage) {
        mVisibleToPackages = Objects.requireNonNull(visibleToPackages);
        mVisibleToPermissions = Objects.requireNonNull(visibleToPermissions);
        mPubliclyVisibleTargetPackage = publiclyVisibleTargetPackage;
    }

     /** Returns a list of {@link PackageIdentifier}s of packages that can access this schema. */
    @NonNull
    public List<PackageIdentifier> getVisibleToPackages() {
        if (mVisibleToPackagesCached == null) {
            mVisibleToPackagesCached = new ArrayList<>(mVisibleToPackages.size());
            for (int i = 0; i < mVisibleToPackages.size(); i++) {
                mVisibleToPackagesCached.add(new PackageIdentifier(mVisibleToPackages.get(i)));
            }
        }
        return mVisibleToPackagesCached;
    }

    /**
     * Returns an array of Integers representing Android Permissions as defined in
     * {@link SetSchemaRequest.AppSearchSupportedPermission} that the caller must hold to access the
     * schema this {@link VisibilityConfig} represents.
     */
    @NonNull
    public Set<Set<Integer>> getVisibleToPermissions() {
        if (mVisibleToPermissionsCached == null) {
            mVisibleToPermissionsCached = new ArraySet<>(mVisibleToPermissions.size());
            for (int i = 0; i < mVisibleToPermissions.size(); i++) {
                VisibilityPermissionConfig permissionConfig = mVisibleToPermissions.get(i);
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
        if (mPubliclyVisibleTargetPackage == null) {
            return null;
        }
        return new PackageIdentifier(mPubliclyVisibleTargetPackage);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        VisibilityConfigCreator.writeToParcel(this, dest, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VisibilityConfig)) return false;
        VisibilityConfig that = (VisibilityConfig) o;
        return Objects.equals(mVisibleToPackages, that.mVisibleToPackages)
                && Objects.equals(mVisibleToPermissions, that.mVisibleToPermissions)
                && Objects.equals(
                        mPubliclyVisibleTargetPackage, that.mPubliclyVisibleTargetPackage);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = Objects.hash(
                    mVisibleToPackages,
                    mVisibleToPermissions,
                    mPubliclyVisibleTargetPackage);
        }
        return mHashCode;
    }

    /** The builder class of {@link VisibilityConfig}. */
    @FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
    public static final class Builder {
        private List<PackageIdentifierParcel> mVisibleToPackages = new ArrayList<>();
        private List<VisibilityPermissionConfig> mVisibleToPermissions = new ArrayList<>();
        private PackageIdentifierParcel mPubliclyVisibleTargetPackage;
        private boolean mBuilt;

        /** Creates a {@link Builder} for a {@link VisibilityConfig}. */
        public Builder() {}

        /**
         * Creates a {@link Builder} copying the values from an existing {@link VisibilityConfig}.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Builder(@NonNull VisibilityConfig visibilityConfig) {
            Objects.requireNonNull(visibilityConfig);
            mVisibleToPackages = new ArrayList<>(visibilityConfig.mVisibleToPackages);
            mVisibleToPermissions = new ArrayList<>(visibilityConfig.mVisibleToPermissions);
            mPubliclyVisibleTargetPackage = visibilityConfig.mPubliclyVisibleTargetPackage;
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addVisibleToPackage(@NonNull PackageIdentifier packageIdentifier) {
            Objects.requireNonNull(packageIdentifier);
            resetIfBuilt();
            mVisibleToPackages.add(packageIdentifier.getPackageIdentifierParcel());
            return this;
        }

        /** Clears the list of packages which have access to this schema. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearVisibleToPackages() {
            resetIfBuilt();
            mVisibleToPackages.clear();
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
            mVisibleToPermissions.add(new VisibilityPermissionConfig(visibleToPermissions));
            return this;
        }

        /** Clears all required permissions combinations set to this {@link VisibilityConfig}.  */
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearVisibleToPermissions() {
            resetIfBuilt();
            mVisibleToPermissions.clear();
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
            if (packageIdentifier == null) {
                mPubliclyVisibleTargetPackage = null;
            } else {
                mPubliclyVisibleTargetPackage = packageIdentifier.getPackageIdentifierParcel();
            }
            return this;
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mVisibleToPackages = new ArrayList<>(mVisibleToPackages);
                mVisibleToPermissions = new ArrayList<>(mVisibleToPermissions);
                mBuilt = false;
            }
        }

        /** Build a {@link VisibilityConfig} */
        @NonNull
        public VisibilityConfig build() {
            mBuilt = true;
            return new VisibilityConfig(
                    mVisibleToPackages,
                    mVisibleToPermissions,
                    mPubliclyVisibleTargetPackage);
        }
    }
}
