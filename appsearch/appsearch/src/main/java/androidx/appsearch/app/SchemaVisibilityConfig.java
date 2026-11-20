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

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.PackageIdentifierParcel;
import androidx.appsearch.safeparcel.PackageIdentifierWithTagsParcel;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.VisibilityConfigCreator;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
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
// TODO(b/384721898): Switching to JSpecify annotations changes APIs once synced to platform.
//  Do not switch unless you've checked that no APIs are affected.
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class SchemaVisibilityConfig extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final @NonNull Parcelable.Creator<SchemaVisibilityConfig> CREATOR =
            new VisibilityConfigCreator();

    @Field(id = 1)
    final @NonNull List<PackageIdentifierParcel> mAllowedPackages;

    @Field(id = 2)
    final @NonNull List<VisibilityPermissionConfig> mRequiredPermissions;

    @Field(id = 3)
    final @Nullable PackageIdentifierParcel mPubliclyVisibleTargetPackage;

    @Field(id = 4)
    final @NonNull List<PackageIdentifierWithTagsParcel> mAllowedPackagesWithTags;

    private @Nullable Integer mHashCode;
    private @Nullable List<PackageIdentifier> mAllowedPackagesCached;
    private @Nullable Set<Set<Integer>> mRequiredPermissionsCached;
    private @Nullable Map<PackageIdentifier, Set<String>> mAllowedPackagesWithTagsCached;

    @Constructor
    SchemaVisibilityConfig(
            @Param(id = 1) @NonNull List<PackageIdentifierParcel> allowedPackages,
            @Param(id = 2) @NonNull List<VisibilityPermissionConfig> requiredPermissions,
            @Param(id = 3) @Nullable PackageIdentifierParcel publiclyVisibleTargetPackage,
            @Param(id = 4)
            @Nullable List<PackageIdentifierWithTagsParcel> allowedPackagesWithTags) {
        mAllowedPackages = Objects.requireNonNull(allowedPackages);
        mRequiredPermissions = Objects.requireNonNull(requiredPermissions);
        mPubliclyVisibleTargetPackage = publiclyVisibleTargetPackage;
        // This field is absent when the config was parceled by a version of AppSearch that
        // predates it.
        mAllowedPackagesWithTags =
                allowedPackagesWithTags != null ? allowedPackagesWithTags : Collections.emptyList();
    }

     /** Returns a list of {@link PackageIdentifier}s of packages that can access this schema. */
    public @NonNull List<PackageIdentifier> getAllowedPackages() {
        if (mAllowedPackagesCached == null) {
            mAllowedPackagesCached = new ArrayList<>(mAllowedPackages.size());
            for (int i = 0; i < mAllowedPackages.size(); i++) {
                mAllowedPackagesCached.add(new PackageIdentifier(mAllowedPackages.get(i)));
            }
        }
        return mAllowedPackagesCached;
    }

    /**
     * Returns an array of Integers representing Android Permissions that the caller must hold to
     * access the schema this {@link SchemaVisibilityConfig} represents.
     * @see SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility(String, Set)
     */
    public @NonNull Set<Set<Integer>> getRequiredPermissions() {
        if (mRequiredPermissionsCached == null) {
            mRequiredPermissionsCached = new ArraySet<>(mRequiredPermissions.size());
            for (int i = 0; i < mRequiredPermissions.size(); i++) {
                VisibilityPermissionConfig permissionConfig = mRequiredPermissions.get(i);
                Set<Integer> requiredPermissions = permissionConfig.getAllRequiredPermissions();
                if (mRequiredPermissionsCached != null && requiredPermissions != null) {
                    mRequiredPermissionsCached.add(requiredPermissions);
                }
            }
        }
        // Added for nullness checker as it is @Nullable, we initialize it above if it is null.
        return Objects.requireNonNull(mRequiredPermissionsCached);
    }

    /**
     * Returns the {@link PackageIdentifier} of the package that will be used as the target package
     * in a call to {@link android.content.pm.PackageManager#canPackageQuery} to determine which
     * packages can access this publicly visible schema. Returns null if the schema is not publicly
     * visible.
     */
    public @Nullable PackageIdentifier getPubliclyVisibleTargetPackage() {
        if (mPubliclyVisibleTargetPackage == null) {
            return null;
        }
        return new PackageIdentifier(mPubliclyVisibleTargetPackage);
    }

    /**
     * Returns a map of {@link PackageIdentifier}s to the attribution tags that a caller from that
     * package must present to access this schema.
     *
     * <p>A caller from one of these packages matches if the attribution tag of the
     * {@link android.content.Context} it used to create its AppSearch session is one of the tags
     * for that package.
     *
     * @see Builder#setAllowedPackageWithTags
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    @ExperimentalAppSearchApi
    public @NonNull Map<PackageIdentifier, Set<String>> getAllowedPackagesWithTags() {
        if (mAllowedPackagesWithTagsCached == null) {
            ArrayMap<PackageIdentifier, Set<String>> allowedPackagesWithTags =
                    new ArrayMap<>(mAllowedPackagesWithTags.size());
            // The builder keeps a single entry for each package.
            for (int i = 0; i < mAllowedPackagesWithTags.size(); i++) {
                PackageIdentifierWithTagsParcel parcel = mAllowedPackagesWithTags.get(i);
                allowedPackagesWithTags.put(
                        new PackageIdentifier(parcel.getPackageIdentifierParcel()),
                        Collections.unmodifiableSet(new ArraySet<>(parcel.getAttributionTags())));
            }
            mAllowedPackagesWithTagsCached = Collections.unmodifiableMap(allowedPackagesWithTags);
        }
        // Added for nullness checker as it is @Nullable, we initialize it above if it is null.
        return Objects.requireNonNull(mAllowedPackagesWithTagsCached);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        VisibilityConfigCreator.writeToParcel(this, dest, flags);
    }

    @Override
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof SchemaVisibilityConfig)) {
            return false;
        }
        SchemaVisibilityConfig that = (SchemaVisibilityConfig) o;
        return Objects.equals(mAllowedPackages, that.mAllowedPackages)
                && Objects.equals(mRequiredPermissions, that.mRequiredPermissions)
                && Objects.equals(
                        mPubliclyVisibleTargetPackage, that.mPubliclyVisibleTargetPackage)
                && getAllowedPackagesWithTags().equals(that.getAllowedPackagesWithTags());
    }

    @Override
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = Objects.hash(
                    mAllowedPackages,
                    mRequiredPermissions,
                    mPubliclyVisibleTargetPackage,
                    getAllowedPackagesWithTags());
        }
        return mHashCode;
    }

    /**
     * Returns the name of a package whose visibility settings conflict, i.e. which is both
     * visible as a whole and restricted by attribution tag, or {@code null} if there is none.
     *
     * <p>A package is visible as a whole when it is one of {@code visibleToPackages}, or is an
     * allowed package of any of {@code visibleToConfigs}.
     *
     * @param visibleToPackages the packages which are visible as a whole other than through
     *                          {@code visibleToConfigs}, if any.
     * @param visibleToConfigs  the configs of a single schema type or blob namespace.
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    static @Nullable String findPackageWithConflictingVisibilitySettings(
            @Nullable Set<PackageIdentifier> visibleToPackages,
            @NonNull Set<SchemaVisibilityConfig> visibleToConfigs) {
        // A package may be identified by different certificates, so compare the names.
        Set<String> packageNames = new ArraySet<>();
        Set<String> packageWithTagsNames = new ArraySet<>();
        if (visibleToPackages != null) {
            for (PackageIdentifier visibleToPackage : visibleToPackages) {
                packageNames.add(visibleToPackage.getPackageName());
            }
        }
        for (SchemaVisibilityConfig visibleToConfig : visibleToConfigs) {
            List<PackageIdentifier> allowedPackages = visibleToConfig.getAllowedPackages();
            for (int i = 0; i < allowedPackages.size(); i++) {
                packageNames.add(allowedPackages.get(i).getPackageName());
            }
            for (PackageIdentifier packageWithTags :
                    visibleToConfig.getAllowedPackagesWithTags().keySet()) {
                packageWithTagsNames.add(packageWithTags.getPackageName());
            }
        }
        for (String packageWithTagsName : packageWithTagsNames) {
            if (packageNames.contains(packageWithTagsName)) {
                return packageWithTagsName;
            }
        }
        return null;
    }

    /** The builder class of {@link SchemaVisibilityConfig}. */
    @FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
    public static final class Builder {
        private List<PackageIdentifierParcel> mAllowedPackages = new ArrayList<>();
        private List<VisibilityPermissionConfig> mRequiredPermissions = new ArrayList<>();
        private @Nullable PackageIdentifierParcel mPubliclyVisibleTargetPackage;
        private final ArrayMap<PackageIdentifier, Set<String>> mAllowedPackagesWithTags =
                new ArrayMap<>();
        private boolean mBuilt;

        /** Creates a {@link Builder} for a {@link SchemaVisibilityConfig}. */
        public Builder() {}

        /**
         * Creates a {@link Builder} copying the values from an existing
         * {@link SchemaVisibilityConfig}.
         */
        @HideInPlatform
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @OptIn(markerClass = ExperimentalAppSearchApi.class)
        public Builder(@NonNull SchemaVisibilityConfig schemaVisibilityConfig) {
            Objects.requireNonNull(schemaVisibilityConfig);
            mAllowedPackages = new ArrayList<>(schemaVisibilityConfig.mAllowedPackages);
            mRequiredPermissions = new ArrayList<>(schemaVisibilityConfig.mRequiredPermissions);
            mPubliclyVisibleTargetPackage = schemaVisibilityConfig.mPubliclyVisibleTargetPackage;
            for (Map.Entry<PackageIdentifier, Set<String>> entry :
                    schemaVisibilityConfig.getAllowedPackagesWithTags().entrySet()) {
                mAllowedPackagesWithTags.put(entry.getKey(), new ArraySet<>(entry.getValue()));
            }
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @CanIgnoreReturnValue
        public @NonNull Builder addAllowedPackage(@NonNull PackageIdentifier packageIdentifier) {
            Objects.requireNonNull(packageIdentifier);
            resetIfBuilt();
            mAllowedPackages.add(packageIdentifier.getPackageIdentifierParcel());
            return this;
        }

        /** Clears the list of packages which have access to this schema. */
        @CanIgnoreReturnValue
        public @NonNull Builder clearAllowedPackages() {
            resetIfBuilt();
            mAllowedPackages.clear();
            return this;
        }

        /**
         * Sets a package which has access to this schema, restricted to callers from that package
         * that present one of the given attribution tags.
         *
         * <p>A caller presents the attribution tag of the {@link android.content.Context} that
         * it uses to create its AppSearch session. This allows an app that bundles several
         * independent modules to share a schema with only some of them, rather than with the
         * whole package as {@link #addAllowedPackage} does.
         *
         * <p>You can call this method for several packages. Calling it again for the same
         * {@link PackageIdentifier} replaces the attribution tags that were set for that package.
         * Use {@link #clearAllowedPackageWithTags} to remove a single package.
         *
         * <p>A package is either visible as a whole or restricted by attribution tag, never
         * both:
         * <ul>
         *   <li>A single {@link SchemaVisibilityConfig} cannot have both packages set with this
         *       method and packages added with {@link #addAllowedPackage}. {@link #build}
         *       throws if it does.
         *   <li>Across the {@link SchemaVisibilityConfig}s of one schema type or blob namespace,
         *       a package cannot be set with this method in one of them and be visible as a
         *       whole elsewhere, whether through {@link #addAllowedPackage} in another one or
         *       through {@link SetSchemaRequest.Builder#setSchemaTypeVisibilityForPackage}.
         *       {@link SetSchemaRequest.Builder#build} and
         *       {@link SetBlobVisibilityRequest.Builder#build} throw if it is.
         * </ul>
         *
         * <p>A caller meets the package requirement of this {@link SchemaVisibilityConfig} if it
         * is from ANY package set with this method and presents one of the attribution tags
         * for that package. The package requirement is in an "AND" relationship with the other
         * requirements of this {@link SchemaVisibilityConfig}: a caller must also meet the
         * requirements set with {@link #addRequiredPermissions} and
         * {@link #setPubliclyVisibleTargetPackage}, if any.
         *
         * <p class="caution"><b>Attribution tags are not a security boundary.</b> AppSearch
         * verifies the package name and signing certificate of the {@link PackageIdentifier}, but
         * an attribution tag is chosen by the calling application and is not verified. Any code
         * running in the package can present any tag. This restriction is intended to prevent
         * accidental access between modules of the same package, not to defend against code in
         * that package which deliberately presents another module's tag.
         *
         * <p>The packages and their attribution tags are available from
         * {@link #getAllowedPackagesWithTags()}.
         *
         * @param packageIdentifier the package which has access to this schema.
         * @param attributionTags   the attribution tags, one of which a caller from the package
         *                          must present. Must not be empty.
         * @throws IllegalArgumentException if {@code attributionTags} is empty.
         */
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
        @ExperimentalAppSearchApi
        public @NonNull Builder setAllowedPackageWithTags(
                @NonNull PackageIdentifier packageIdentifier,
                @NonNull Set<String> attributionTags) {
            Objects.requireNonNull(packageIdentifier);
            Objects.requireNonNull(attributionTags);
            Preconditions.checkArgument(!attributionTags.isEmpty(),
                    "The set of attribution tags cannot be empty");
            for (String attributionTag : attributionTags) {
                Objects.requireNonNull(attributionTag);
            }
            // Replaces the attribution tags which were set for this package, if any.
            mAllowedPackagesWithTags.put(packageIdentifier, new ArraySet<>(attributionTags));
            return this;
        }

        /**
         * Clears the given package, and its attribution tags, set by
         * {@link #setAllowedPackageWithTags}.
         *
         * <p>This does nothing if the package was not set.
         *
         * @param packageIdentifier the package to clear.
         */
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
        @ExperimentalAppSearchApi
        public @NonNull Builder clearAllowedPackageWithTags(
                @NonNull PackageIdentifier packageIdentifier) {
            Objects.requireNonNull(packageIdentifier);
            mAllowedPackagesWithTags.remove(packageIdentifier);
            return this;
        }

        /**
         * Clears all the packages, and their attribution tags, set by
         * {@link #setAllowedPackageWithTags}.
         */
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
        @ExperimentalAppSearchApi
        public @NonNull Builder clearAllowedPackagesWithTags() {
            mAllowedPackagesWithTags.clear();
            return this;
        }

        /**
         * Adds a set of required Android {@link android.Manifest.permission} combination a
         * package needs to hold to access the schema this {@link SchemaVisibilityConfig}
         * represents.
         *
         * <p> If the querier holds ALL of the required permissions in this combination, they will
         * have access to read {@link GenericDocument} objects of the given schema type.
         *
         * <p> You can call this method repeatedly to add multiple permission combinations, and the
         * querier will have access if they holds ANY of the combinations.
         *
         * <p>Merged Set available from {@link #getRequiredPermissions()}.
         *
         * @see SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility for
         * supported Permissions.
         */
        @SuppressWarnings("RequiresPermission")  // No permission required to call this method
        @CanIgnoreReturnValue
        public @NonNull Builder addRequiredPermissions(@NonNull Set<Integer> visibleToPermissions) {
            Objects.requireNonNull(visibleToPermissions);
            if (AppSearchEnvironmentFactory.getEnvironmentInstance().getEnvironment()
                    != AppSearchEnvironment.FRAMEWORK_ENVIRONMENT) {
                Preconditions.checkArgument(!visibleToPermissions.isEmpty(),
                        "The set of required permissions cannot be empty");
            }
            resetIfBuilt();
            mRequiredPermissions.add(new VisibilityPermissionConfig(visibleToPermissions));
            return this;
        }

        /**
         * Clears all required permissions combinations set to this {@link SchemaVisibilityConfig}.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder clearRequiredPermissions() {
            resetIfBuilt();
            mRequiredPermissions.clear();
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
        @CanIgnoreReturnValue
        public @NonNull Builder setPubliclyVisibleTargetPackage(
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
                mAllowedPackages = new ArrayList<>(mAllowedPackages);
                mRequiredPermissions = new ArrayList<>(mRequiredPermissions);
                mBuilt = false;
            }
        }

        /**
         * Build a {@link SchemaVisibilityConfig}
         *
         * @throws IllegalArgumentException if this builder has both packages which have access
         *                                  to this schema as a whole and packages which have
         *                                  access only with specific attribution tags.
         */
        public @NonNull SchemaVisibilityConfig build() {
            // A package is either allowed as a whole or restricted by attribution tag. Keeping
            // the two in separate configs means a config never has to define how they combine.
            Preconditions.checkArgument(
                    mAllowedPackages.isEmpty() || mAllowedPackagesWithTags.isEmpty(),
                    "A SchemaVisibilityConfig cannot have both allowed packages and allowed "
                            + "packages with attribution tags");
            mBuilt = true;
            // The built config gets its own copy of the attribution tags, so the builder can keep
            // being modified without affecting it.
            List<PackageIdentifierWithTagsParcel> allowedPackagesWithTags =
                    new ArrayList<>(mAllowedPackagesWithTags.size());
            for (int i = 0; i < mAllowedPackagesWithTags.size(); i++) {
                allowedPackagesWithTags.add(new PackageIdentifierWithTagsParcel(
                        mAllowedPackagesWithTags.keyAt(i).getPackageIdentifierParcel(),
                        new ArrayList<>(mAllowedPackagesWithTags.valueAt(i))));
            }
            return new SchemaVisibilityConfig(
                    mAllowedPackages,
                    mRequiredPermissions,
                    mPubliclyVisibleTargetPackage,
                    allowedPackagesWithTags);
        }
    }
}
