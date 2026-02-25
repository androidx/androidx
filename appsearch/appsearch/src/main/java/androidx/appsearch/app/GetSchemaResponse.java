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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.GetSchemaResponseCreator;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The response class of {@link AppSearchSession#getSchemaAsync} */
@SafeParcelable.Class(creator = "GetSchemaResponseCreator")
// TODO(b/384721898): Switching to JSpecify annotations changes APIs once synced to platform.
//  Do not switch unless you've checked that no APIs are affected.
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class GetSchemaResponse extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<GetSchemaResponse> CREATOR =
            new GetSchemaResponseCreator();

    @Field(id = 1, getter = "getVersion")
    private final int mVersion;

    @Field(id = 2)
    final List<AppSearchSchema> mSchemas;

    /**
     * List of VisibilityConfigs for the current schema. May be {@code null} if retrieving the
     * visibility settings is not possible on the current backend.
     */
    @Field(id = 3)
    final @Nullable List<InternalVisibilityConfig> mVisibilityConfigs;

    /**
     * This Bundle contains a mapping from schema types to an ArrayList of strings, where each
     * string is a property path indicating that the property is an {@link AppSearchAccount}.
     */
    @Field(id = 4)
    final @Nullable Bundle mSchemasWipeoutAccountPropertyPathsBundle;

    /**
     * This set contains all schemas most recently successfully provided to
     * {@link AppSearchSession#setSchemaAsync}. We do lazy fetch, the object will be created when
     * you first time fetch it.
     */
    private @Nullable Set<AppSearchSchema> mSchemasCached;

    /**
     * This Set contains all schemas that are not displayed by the system. All values in the set are
     * prefixed with the package-database prefix. We do lazy fetch, the object will be created
     * when you first time fetch it.
     */
    private @Nullable Set<String> mSchemasNotDisplayedBySystemCached;

    /**
     * This map contains all schemas and {@link PackageIdentifier} that has access to the schema.
     * All keys in the map are prefixed with the package-database prefix. We do lazy fetch, the
     * object will be created when you first time fetch it.
     */
    private @Nullable Map<String, Set<PackageIdentifier>> mSchemasVisibleToPackagesCached;

    /**
     * This map contains all schemas and Android Permissions combinations that are required to
     * access the schema. All keys in the map are prefixed with the package-database prefix. We
     * do lazy fetch, the object will be created when you first time fetch it.
     * The Map is constructed in ANY-ALL cases. The querier could read the {@link GenericDocument}
     * objects under the {@code schemaType} if they hold ALL required permissions of ANY
     * combinations.
     * @see SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility(String, Set)
     */
    private @Nullable Map<String, Set<Set<Integer>>> mSchemasVisibleToPermissionsCached;

    /**
     * This map contains all publicly visible schemas and the {@link PackageIdentifier} specifying
     * the package that the schemas are from.
     */
    private @Nullable Map<String, PackageIdentifier> mPubliclyVisibleSchemasCached;

    /**
     * This map contains all {@link SchemaVisibilityConfig}s that has access to the schema.
     * All keys in the map are prefixed with the package-database prefix. We do lazy fetch, the
     * object will be created when you first time fetch it.
     */
    private @Nullable Map<String, Set<SchemaVisibilityConfig>> mSchemasVisibleToConfigsCached;

    /**
     * This map contains a mapping from schema types to an set of strings, where each
     * string is a property path indicating that the property is an {@link AppSearchAccount}.
     */
    private @Nullable Map<String, Set<PropertyPath>> mSchemasWipeoutAccountPropertyPathsCached;

    @Constructor
    GetSchemaResponse(
            @Param(id = 1) int version,
            @Param(id = 2) @NonNull List<AppSearchSchema> schemas,
            @Param(id = 3) @Nullable List<InternalVisibilityConfig> visibilityConfigs,
            @Param(id = 4) @Nullable Bundle schemasWipeoutAccountPropertyPathsBundle) {
        mVersion = version;
        mSchemas = Preconditions.checkNotNull(schemas);
        mVisibilityConfigs = visibilityConfigs;
        mSchemasWipeoutAccountPropertyPathsBundle = schemasWipeoutAccountPropertyPathsBundle;
    }

    /**
     * Returns the overall database schema version.
     *
     * <p>If the database is empty, 0 will be returned.
     */
    @IntRange(from = 0)
    public int getVersion() {
        return mVersion;
    }

    /**
     * Return the schemas most recently successfully provided to
     * {@link AppSearchSession#setSchemaAsync}.
     */
    public @NonNull Set<AppSearchSchema> getSchemas() {
        if (mSchemasCached == null) {
            mSchemasCached = Collections.unmodifiableSet(new ArraySet<>(mSchemas));
        }
        return mSchemasCached;
    }

    /**
     * Returns all the schema types that are opted out of being displayed and visible on any
     * system UI surface.
     * <!--@exportToFramework:ifJetpack()-->
     * @throws UnsupportedOperationException if {@link Builder#setVisibilitySettingSupported} was
     * called with false.
     * <!--@exportToFramework:else()-->
     */
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.ADD_PERMISSIONS_AND_GET_VISIBILITY)
    public @NonNull Set<String> getSchemaTypesNotDisplayedBySystem() {
        List<InternalVisibilityConfig> visibilityConfigs = getVisibilityConfigsOrThrow();
        if (mSchemasNotDisplayedBySystemCached == null) {
            Set<String> copy = new ArraySet<>();
            for (int i = 0; i < visibilityConfigs.size(); i++) {
                if (visibilityConfigs.get(i).isNotDisplayedBySystem()) {
                    copy.add(visibilityConfigs.get(i).getSchemaType());
                }
            }
            mSchemasNotDisplayedBySystemCached = Collections.unmodifiableSet(copy);
        }
        return mSchemasNotDisplayedBySystemCached;
    }

    /**
     * Returns a mapping of schema types to the set of packages that have access
     * to that schema type.
     * <!--@exportToFramework:ifJetpack()-->
     * @throws UnsupportedOperationException if {@link Builder#setVisibilitySettingSupported} was
     * called with false.
     * <!--@exportToFramework:else()-->
     */
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.ADD_PERMISSIONS_AND_GET_VISIBILITY)
    public @NonNull Map<String, Set<PackageIdentifier>> getSchemaTypesVisibleToPackages() {
        List<InternalVisibilityConfig> visibilityConfigs = getVisibilityConfigsOrThrow();
        if (mSchemasVisibleToPackagesCached == null) {
            Map<String, Set<PackageIdentifier>> copy = new ArrayMap<>();
            for (int i = 0; i < visibilityConfigs.size(); i++) {
                InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(i);
                List<PackageIdentifier> visibleToPackages =
                        visibilityConfig.getVisibilityConfig().getAllowedPackages();
                if (!visibleToPackages.isEmpty()) {
                    copy.put(
                            visibilityConfig.getSchemaType(),
                            Collections.unmodifiableSet(new ArraySet<>(visibleToPackages)));
                }
            }
            mSchemasVisibleToPackagesCached = Collections.unmodifiableMap(copy);
        }
        return mSchemasVisibleToPackagesCached;
    }

    /**
     * Returns a mapping of schema types to the set of {@link android.Manifest.permission}
     * combination sets that querier must hold to access that schema type.
     *
     * <p> The querier could read the {@link GenericDocument} objects under the {@code schemaType}
     * if they holds ALL required permissions of ANY of the individual value sets.
     *
     * <p>For example, if the Map contains {@code {% verbatim %}{{permissionA, PermissionB},
     * { PermissionC, PermissionD}, {PermissionE}}{% endverbatim %}}.
     * <ul>
     *     <li>A querier holding both PermissionA and PermissionB has access.</li>
     *     <li>A querier holding both PermissionC and PermissionD has access.</li>
     *     <li>A querier holding only PermissionE has access.</li>
     *     <li>A querier holding both PermissionA and PermissionE has access.</li>
     *     <li>A querier holding only PermissionA doesn't have access.</li>
     *     <li>A querier holding only PermissionA and PermissionC doesn't have access.</li>
     * </ul>
     *
     * @return The map contains schema type and all combinations of required permission for querier
     *         to access it. The supported Permission are {@link SetSchemaRequest#READ_SMS},
     *         {@link SetSchemaRequest#READ_CALENDAR}, {@link SetSchemaRequest#READ_CONTACTS},
     *         {@link SetSchemaRequest#READ_EXTERNAL_STORAGE},
     *         {@link SetSchemaRequest#READ_HOME_APP_SEARCH_DATA} and
     *         {@link SetSchemaRequest#READ_ASSISTANT_APP_SEARCH_DATA}.
     * <!--@exportToFramework:ifJetpack()-->
     * @throws UnsupportedOperationException if {@link Builder#setVisibilitySettingSupported} was
     * called with false.
     * <!--@exportToFramework:else()-->
     */
    // TODO(b/237388235): add enterprise permissions to javadocs after they're unhidden
    // Annotation is here to suppress lint error. Lint error is erroneous since the method does not
    // require the caller to hold any permission for the method to function.
    @SuppressLint("RequiresPermission")
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.ADD_PERMISSIONS_AND_GET_VISIBILITY)
    public @NonNull Map<String, Set<Set<Integer>>> getRequiredPermissionsForSchemaTypeVisibility() {
        List<InternalVisibilityConfig> visibilityConfigs = getVisibilityConfigsOrThrow();
        if (mSchemasVisibleToPermissionsCached == null) {
            Map<String, Set<Set<Integer>>> copy = new ArrayMap<>();
            for (int i = 0; i < visibilityConfigs.size(); i++) {
                InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(i);
                Set<Set<Integer>> visibleToPermissions =
                        visibilityConfig.getVisibilityConfig().getRequiredPermissions();
                if (!visibleToPermissions.isEmpty()) {
                    copy.put(
                            visibilityConfig.getSchemaType(),
                            Collections.unmodifiableSet(visibleToPermissions));
                }
            }
            mSchemasVisibleToPermissionsCached = Collections.unmodifiableMap(copy);
        }
        return mSchemasVisibleToPermissionsCached;
    }

    /**
     * Returns a mapping of publicly visible schemas to the {@link PackageIdentifier} specifying
     * the package the schemas are from.
     *
     * <p> If no schemas have been set as publicly visible, an empty set will be returned.
     * <!--@exportToFramework:ifJetpack()-->
     * @throws UnsupportedOperationException if {@link Builder#setVisibilitySettingSupported} was
     * called with false.
     * <!--@exportToFramework:else()-->
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SET_PUBLICLY_VISIBLE_SCHEMA)
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.ADD_PERMISSIONS_AND_GET_VISIBILITY)
    public @NonNull Map<String, PackageIdentifier> getPubliclyVisibleSchemas() {
        List<InternalVisibilityConfig> visibilityConfigs = getVisibilityConfigsOrThrow();
        if (mPubliclyVisibleSchemasCached == null) {
            Map<String, PackageIdentifier> copy = new ArrayMap<>();
            for (int i = 0; i < visibilityConfigs.size(); i++) {
                InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(i);
                PackageIdentifier publiclyVisibleTargetPackage =
                        visibilityConfig.getVisibilityConfig().getPubliclyVisibleTargetPackage();
                if (publiclyVisibleTargetPackage != null) {
                    copy.put(visibilityConfig.getSchemaType(), publiclyVisibleTargetPackage);
                }
            }
            mPubliclyVisibleSchemasCached = Collections.unmodifiableMap(copy);
        }
        return mPubliclyVisibleSchemasCached;
    }

    /**
     * Returns a mapping of schema types to the set of {@link SchemaVisibilityConfig} that have
     * access to that schema type.
     *
     * @see SetSchemaRequest.Builder#addSchemaTypeVisibleToConfig
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.ADD_PERMISSIONS_AND_GET_VISIBILITY)
    public @NonNull Map<String, Set<SchemaVisibilityConfig>> getSchemaTypesVisibleToConfigs() {
        List<InternalVisibilityConfig> visibilityConfigs = getVisibilityConfigsOrThrow();
        if (mSchemasVisibleToConfigsCached == null) {
            Map<String, Set<SchemaVisibilityConfig>> copy = new ArrayMap<>();
            for (int i = 0; i < visibilityConfigs.size(); i++) {
                InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(i);
                Set<SchemaVisibilityConfig> nestedVisibilityConfigs =
                        visibilityConfig.getVisibleToConfigs();
                if (!nestedVisibilityConfigs.isEmpty()) {
                    copy.put(visibilityConfig.getSchemaType(),
                            Collections.unmodifiableSet(nestedVisibilityConfigs));
                }
            }
            mSchemasVisibleToConfigsCached = Collections.unmodifiableMap(copy);
        }
        return mSchemasVisibleToConfigsCached;
    }

    /**
     * Returns a map containing all schema types that have been configured for account wipeout,
     * mapped to the specific property paths within those schemas that hold the account identifiers.
     *
     * <p>This method performs a deep copy to ensure the returned map and its contained sets are
     * immutable from external modifications.
     *
     * @return A map where keys are schema type names, and values are sets of {@link PropertyPath}
     * strings configured for account wipeout for that schema type.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMAS_WIPEOUT_ACCOUNT_PROPERTY_PATHS)
    @ExperimentalAppSearchApi
    public @NonNull Map<String, Set<PropertyPath>> getSchemasWipeoutAccountPropertyPaths() {
        if (mSchemasWipeoutAccountPropertyPathsCached == null) {
            if (mSchemasWipeoutAccountPropertyPathsBundle == null) {
                mSchemasWipeoutAccountPropertyPathsCached = new ArrayMap<>();
            } else {
                Map<String, Set<PropertyPath>> copy = new ArrayMap<>();
                for (String key : mSchemasWipeoutAccountPropertyPathsBundle.keySet()) {
                    List<String> propertyPathsList = mSchemasWipeoutAccountPropertyPathsBundle
                            .getStringArrayList(key);
                    Set<PropertyPath> propertyPathsSet = new ArraySet<>(propertyPathsList.size());
                    for (int i = 0; i < propertyPathsList.size(); i++) {
                        propertyPathsSet.add(new PropertyPath(propertyPathsList.get(i)));
                    }
                    copy.put(key, propertyPathsSet);
                }
                mSchemasWipeoutAccountPropertyPathsCached = Collections.unmodifiableMap(copy);
            }
        }
        return mSchemasWipeoutAccountPropertyPathsCached;
    }

    private @NonNull List<InternalVisibilityConfig> getVisibilityConfigsOrThrow() {
        List<InternalVisibilityConfig> visibilityConfigs = mVisibilityConfigs;
        if (visibilityConfigs == null) {
            throw new UnsupportedOperationException("Get visibility setting is not supported with "
                    + "this backend/Android API level combination.");
        }
        return visibilityConfigs;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        GetSchemaResponseCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link GetSchemaResponse} objects. */
    public static final class Builder {
        private int mVersion = 0;
        private ArrayList<AppSearchSchema> mSchemas = new ArrayList<>();
        /**
         * Creates the object when we actually set them. If we never set visibility settings, we
         * should throw {@link UnsupportedOperationException} in the visibility getters.
         */
        private @Nullable Map<String, InternalVisibilityConfig.Builder> mVisibilityConfigBuilders;

        private @Nullable Map<String, Set<String>> mSchemasWipeoutAccountPropertyPaths;
        private boolean mBuilt = false;

        /** Creates a new {@link Builder} */
        public Builder() {
            setVisibilitySettingSupported(true);
        }

        /** Creates a new {@link Builder} from the given {@link GetSchemaResponse}. */
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        public Builder(@NonNull GetSchemaResponse getSchemaResponse) {
            setVisibilitySettingSupported(true);
            mVersion = getSchemaResponse.mVersion;
            mSchemas.addAll(getSchemaResponse.mSchemas);
            if (getSchemaResponse.mVisibilityConfigs != null) {
                int count = getSchemaResponse.mVisibilityConfigs.size();
                for (int i = 0; i < count; i++) {
                    InternalVisibilityConfig config = getSchemaResponse.mVisibilityConfigs.get(i);
                    mVisibilityConfigBuilders.put(config.getSchemaType(),
                            new InternalVisibilityConfig.Builder(config));
                }
            }
            if (getSchemaResponse.mSchemasWipeoutAccountPropertyPathsBundle != null) {
                Bundle otherBundle = getSchemaResponse.mSchemasWipeoutAccountPropertyPathsBundle;
                mSchemasWipeoutAccountPropertyPaths = new ArrayMap<>(otherBundle.size());
                for (String key : otherBundle.keySet()) {
                    mSchemasWipeoutAccountPropertyPaths.put(key,
                            new ArraySet<>(otherBundle.getStringArrayList(key)));
                }
            }
        }

        /**
         * Sets the database overall schema version.
         *
         * <p>Default version is 0
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setVersion(@IntRange(from = 0) int version) {
            Preconditions.checkArgument(version >= 0, "Version must be a non-negative number.");
            resetIfBuilt();
            mVersion = version;
            return this;
        }

        /** Adds one {@link AppSearchSchema} to the schema list. */
        @CanIgnoreReturnValue
        public @NonNull Builder addSchema(@NonNull AppSearchSchema schema) {
            Preconditions.checkNotNull(schema);
            resetIfBuilt();
            mSchemas.add(schema);
            return this;
        }

        /** Clears all {@link AppSearchSchema}s from the list of schemas. */
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearSchemas() {
            resetIfBuilt();
            mSchemas.clear();
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
        @CanIgnoreReturnValue
        public @NonNull Builder addSchemaTypeNotDisplayedBySystem(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getOrCreateVisibilityConfigBuilder(schemaType);
            visibilityConfigBuilder.setNotDisplayedBySystem(true);
            return this;
        }

        /**
         * Clears the visibility setting for the given schema type that prevents the schema from
         * being displayed and visible on any system UI surface.
         *
         * @see Builder#addSchemaTypeNotDisplayedBySystem
         */
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearSchemaTypeNotDisplayedBySystem(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getVisibilityConfigBuilder(schemaType);
            if (visibilityConfigBuilder != null) {
                visibilityConfigBuilder.setNotDisplayedBySystem(false);
            }
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
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setSchemaTypeVisibleToPackages(
                @NonNull String schemaType,
                @NonNull Set<PackageIdentifier> packageIdentifiers) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(packageIdentifiers);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getOrCreateVisibilityConfigBuilder(schemaType);
            for (PackageIdentifier packageIdentifier : packageIdentifiers) {
                visibilityConfigBuilder.addVisibleToPackage(packageIdentifier);
            }
            return this;
        }

        /**
         * Clears the set of packages that can read the given schema type.
         *
         * @see Builder#setSchemaTypeVisibleToPackages
         */
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearSchemaTypeVisibleToPackages(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getVisibilityConfigBuilder(schemaType);
            if (visibilityConfigBuilder != null) {
                visibilityConfigBuilder.clearVisibleToPackages();
            }
            return this;
        }

        /**
         * Sets a set of required {@link android.Manifest.permission} combinations to the given
         * schema type.
         *
         * <p> The querier could read the {@link GenericDocument} objects under the
         * {@code schemaType} if they holds ALL required permissions of ANY of the individual value
         * sets.
         *
         * <p>For example, if the Map contains {@code {% verbatim %}{{permissionA, PermissionB},
         * {PermissionC, PermissionD}, {PermissionE}}{% endverbatim %}}.
         * <ul>
         *     <li>A querier holds both PermissionA and PermissionB has access.</li>
         *     <li>A querier holds both PermissionC and PermissionD has access.</li>
         *     <li>A querier holds only PermissionE has access.</li>
         *     <li>A querier holds both PermissionA and PermissionE has access.</li>
         *     <li>A querier holds only PermissionA doesn't have access.</li>
         *     <li>A querier holds both PermissionA and PermissionC doesn't have access.</li>
         * </ul>
         *
         * @param schemaType              The schema type to set visibility on.
         * @param visibleToPermissionSets The Sets of Android permissions that will be required to
         *                                access the given schema.
         * @see android.Manifest.permission#READ_SMS
         * @see android.Manifest.permission#READ_CALENDAR
         * @see android.Manifest.permission#READ_CONTACTS
         * @see android.Manifest.permission#READ_EXTERNAL_STORAGE
         * @see android.Manifest.permission#READ_HOME_APP_SEARCH_DATA
         * @see android.Manifest.permission#READ_ASSISTANT_APP_SEARCH_DATA
         */
        // TODO(b/237388235): add enterprise permissions to javadocs after they're unhidden
        // Getter getRequiredPermissionsForSchemaTypeVisibility returns a map for all schemaTypes.
        // To use this API doesn't require permissions.
        @SuppressLint({"MissingGetterMatchingBuilder", "RequiresPermission"})
        @CanIgnoreReturnValue
        // @SetSchemaRequest is an IntDef annotation applied to Set<Set<Integer>>.
        @SuppressWarnings("SupportAnnotationUsage")
        public @NonNull Builder setRequiredPermissionsForSchemaTypeVisibility(
                @NonNull String schemaType,
                @SetSchemaRequest.AppSearchSupportedPermission
                @NonNull Set<Set<Integer>> visibleToPermissionSets) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(visibleToPermissionSets);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getOrCreateVisibilityConfigBuilder(schemaType);
            for (Set<Integer> visibleToPermissions : visibleToPermissionSets) {
                visibilityConfigBuilder.addVisibleToPermissions(visibleToPermissions);
            }
            return this;
        }

        /**
         * Clears the set of required {@link android.Manifest.permission} combinations to read the
         * given schema type.
         *
         * @see Builder#setRequiredPermissionsForSchemaTypeVisibility
         */
        // To use this API doesn't require permissions.
        @ExperimentalAppSearchApi
        @SuppressLint("RequiresPermission")
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearRequiredPermissionsForSchemaTypeVisibility(
                @NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getVisibilityConfigBuilder(schemaType);
            if (visibilityConfigBuilder != null) {
                visibilityConfigBuilder.clearVisibleToPermissions();
            }
            return this;
        }

        /**
         * Specify that the schema should be publicly available, to packages which already have
         * visibility to {@code packageIdentifier}.
         *
         * @param schemaType the schema to make publicly accessible.
         * @param packageIdentifier the package from which the document schema is from.
         * @see SetSchemaRequest.Builder#setPubliclyVisibleSchema
         */
        // Merged list available from getPubliclyVisibleSchemas
        @SuppressLint("MissingGetterMatchingBuilder")
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SET_PUBLICLY_VISIBLE_SCHEMA)
        public @NonNull Builder setPubliclyVisibleSchema(
                @NonNull String schemaType, @NonNull PackageIdentifier packageIdentifier) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(packageIdentifier);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getOrCreateVisibilityConfigBuilder(schemaType);
            visibilityConfigBuilder.setPubliclyVisibleTargetPackage(packageIdentifier);
            return this;
        }

        /**
         * Clears the visibility setting that specifies that the given schema type should be
         * publicly available to packages which already have visibility to a specified package.
         *
         * @see Builder#setPubliclyVisibleSchema
         */
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearPubliclyVisibleSchema(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getVisibilityConfigBuilder(schemaType);
            if (visibilityConfigBuilder != null) {
                visibilityConfigBuilder.setPubliclyVisibleTargetPackage(null);
            }
            return this;
        }

        /**
         * Sets the documents from the provided {@code schemaType} can be read by the caller if they
         * match the ALL visibility requirements set in {@link SchemaVisibilityConfig}.
         *
         * <p> The requirements in a {@link SchemaVisibilityConfig} is "AND" relationship. A
         * caller must match ALL requirements to access the schema. For example, a caller must hold
         * required permissions AND it is a specified package.
         *
         * <p> The querier could have access if they match ALL requirements in ANY of the given
         * {@link SchemaVisibilityConfig}s
         *
         * <p>For example, if the Set contains {@code {% verbatim %}{{PackageA and Permission1},
         * {PackageB and Permission2}}{% endverbatim %}}.
         * <ul>
         *     <li>A querier from packageA could read if they holds Permission1.</li>
         *     <li>A querier from packageA could NOT read if they only holds Permission2 instead of
         *     Permission1.</li>
         *     <li>A querier from packageB could read if they holds Permission2.</li>
         *     <li>A querier from packageC could never read.</li>
         *     <li>A querier holds both PermissionA and PermissionE has access.</li>
         * </ul>
         *
         * @param schemaType         The schema type to set visibility on.
         * @param visibleToConfigs   The {@link SchemaVisibilityConfig}s hold all requirements that
         *                           a call must to match to access the schema.
         */
        // Merged map available from getSchemasVisibleToConfigs
        @SuppressLint("MissingGetterMatchingBuilder")
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
        public @NonNull Builder setSchemaTypeVisibleToConfigs(@NonNull String schemaType,
                @NonNull Set<SchemaVisibilityConfig> visibleToConfigs) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(visibleToConfigs);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getOrCreateVisibilityConfigBuilder(schemaType);
            for (SchemaVisibilityConfig visibleToConfig : visibleToConfigs) {
                visibilityConfigBuilder.addVisibleToConfig(visibleToConfig);
            }
            return this;
        }

        /**
         * Clears the {@link SchemaVisibilityConfig}s for the given schema type which allow
         * visibility to the schema if the caller matches ALL visibility requirements of ANY
         * {@link SchemaVisibilityConfig}.
         *
         * @see Builder#setSchemaTypeVisibleToConfigs
         */
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearSchemaTypeVisibleToConfigs(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getVisibilityConfigBuilder(schemaType);
            if (visibilityConfigBuilder != null) {
                visibilityConfigBuilder.clearVisibleToConfig();
            }
            return this;
        }

        /**
         * Method to set visibility setting. If this is called with false,
         * {@link #getRequiredPermissionsForSchemaTypeVisibility()},
         * {@link #getSchemaTypesNotDisplayedBySystem()}}, and
         * {@link #getSchemaTypesVisibleToPackages()} calls will throw an
         * {@link UnsupportedOperationException}. If called with true, visibility information for
         * all schemas will be cleared.
         *
         * @param visibilitySettingSupported whether supported
         * {@link Features#ADD_PERMISSIONS_AND_GET_VISIBILITY} by this
         *                                      backend/Android API level.
         * @exportToFramework:hide
         */
         // Visibility setting is determined by SDK version, so it won't be needed in framework
        @SuppressLint("MissingGetterMatchingBuilder")
        @CanIgnoreReturnValue
        public @NonNull Builder setVisibilitySettingSupported(boolean visibilitySettingSupported) {
            if (visibilitySettingSupported) {
                mVisibilityConfigBuilders = new ArrayMap<>();
            } else {
                mVisibilityConfigBuilders = null;
            }
            return this;
        }

        /**
         * Sets the account {@link PropertyPath} for the given {@code schemaType}.
         *
         * <p>These property paths are used to identify data that belongs to an account,
         * which can then be wiped when an account is removed from the system.
         *
         * <p>If called multiple times for the same {@code schemaType}, the new paths will be
         * added to the existing ones. To replace the paths, first call
         * {@link #clearSchemaTypeWipeoutAccountPropertyPaths(String)}.
         *
         * @param schemaType The name of the schema type to which these property paths belong.
         * @param accountPropertyPaths A Set of {@link PropertyPath} that point to accounts.
         * @see SetSchemaRequest.Builder#setSchemaTypeWipeoutAccountPropertyPaths
         */
        // Merged map available from getSchemaTypesWipeoutAccountPropertyPaths
        @SuppressLint("MissingGetterMatchingBuilder")
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMAS_WIPEOUT_ACCOUNT_PROPERTY_PATHS)
        @ExperimentalAppSearchApi
        public @NonNull Builder setSchemaTypeWipeoutAccountPropertyPaths(
                @NonNull String schemaType,
                @NonNull Set<PropertyPath> accountPropertyPaths) {
            Preconditions.checkNotNull(schemaType);
            Preconditions.checkNotNull(accountPropertyPaths);
            resetIfBuilt();
            if (mSchemasWipeoutAccountPropertyPaths == null) {
                mSchemasWipeoutAccountPropertyPaths = new ArrayMap<>();
            }
            List<String> propertyPathsList = new ArrayList<>(accountPropertyPaths.size());
            for (PropertyPath propertyPath : accountPropertyPaths) {
                propertyPathsList.add(propertyPath.toString());
            }

            Set<String> accountProperties = mSchemasWipeoutAccountPropertyPaths.get(schemaType);
            if (accountProperties == null) {
                accountProperties = new ArraySet<>();
                mSchemasWipeoutAccountPropertyPaths.put(schemaType, accountProperties);
            }
            accountProperties.addAll(propertyPathsList);
            return this;
        }

        /**
         * Clears all account {@link PropertyPath} for the given {@code schemaType}.
         *
         * @param schemaType The name of the schema type for which to clear account property paths.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMAS_WIPEOUT_ACCOUNT_PROPERTY_PATHS)
        @ExperimentalAppSearchApi
        public @NonNull Builder clearSchemaTypeWipeoutAccountPropertyPaths(
                @NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            if (mSchemasWipeoutAccountPropertyPaths == null) {
                mSchemasWipeoutAccountPropertyPaths = new ArrayMap<>();
            }
            mSchemasWipeoutAccountPropertyPaths.remove(schemaType);
            return this;
        }

        /** Builds a {@link GetSchemaResponse} object. */
        public @NonNull GetSchemaResponse build() {
            List<InternalVisibilityConfig> visibilityConfigs = null;
            if (mVisibilityConfigBuilders != null) {
                visibilityConfigs = new ArrayList<>();
                for (InternalVisibilityConfig.Builder builder :
                        mVisibilityConfigBuilders.values()) {
                    visibilityConfigs.add(builder.build());
                }
            }
            Bundle schemasWipeoutAccountPropertyPathsBundle = null;
            if (mSchemasWipeoutAccountPropertyPaths != null) {
                schemasWipeoutAccountPropertyPathsBundle = new Bundle();
                for (Map.Entry<String, Set<String>> entry :
                        mSchemasWipeoutAccountPropertyPaths.entrySet()) {
                    schemasWipeoutAccountPropertyPathsBundle.putStringArrayList(entry.getKey(),
                            new ArrayList<>(entry.getValue()));
                }
            }
            mBuilt = true;
            return new GetSchemaResponse(mVersion, mSchemas, visibilityConfigs,
                    schemasWipeoutAccountPropertyPathsBundle);
        }

        private @NonNull InternalVisibilityConfig.Builder getOrCreateVisibilityConfigBuilder(
                @NonNull String schemaType) {
            if (mVisibilityConfigBuilders == null) {
                throw new IllegalStateException("GetSchemaResponse is not configured with"
                        + "visibility setting support");
            }
            InternalVisibilityConfig.Builder builder = mVisibilityConfigBuilders.get(schemaType);
            if (builder == null) {
                builder = new InternalVisibilityConfig.Builder(schemaType);
                mVisibilityConfigBuilders.put(schemaType, builder);
            }
            return builder;
        }

        private @Nullable InternalVisibilityConfig.Builder getVisibilityConfigBuilder(
                @NonNull String schemaType) {
            if (mVisibilityConfigBuilders == null) {
                throw new IllegalStateException("GetSchemaResponse is not configured with"
                        + "visibility setting support");
            }
            return mVisibilityConfigBuilders.get(schemaType);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                // No need to copy mVisibilityConfigBuilders -- it gets copied during build().
                mSchemas = new ArrayList<>(mSchemas);
                if (mSchemasWipeoutAccountPropertyPaths != null) {
                    Map<String, Set<String>> schemasWipeoutAccountPropertyPaths = new ArrayMap<>();
                    schemasWipeoutAccountPropertyPaths.putAll(mSchemasWipeoutAccountPropertyPaths);
                    mSchemasWipeoutAccountPropertyPaths = schemasWipeoutAccountPropertyPaths;
                }
                mBuilt = false;
            }
        }
    }
}
