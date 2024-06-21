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
@SuppressWarnings("HiddenSuperclass")
public final class GetSchemaResponse extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @NonNull
    public static final Parcelable.Creator<GetSchemaResponse> CREATOR =
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
    @Nullable
    final List<InternalVisibilityConfig> mVisibilityConfigs;

    /**
     * This set contains all schemas most recently successfully provided to
     * {@link AppSearchSession#setSchemaAsync}. We do lazy fetch, the object will be created when
     * you first time fetch it.
     */
    @Nullable
    private Set<AppSearchSchema> mSchemasCached;

    /**
     * This Set contains all schemas that are not displayed by the system. All values in the set are
     * prefixed with the package-database prefix. We do lazy fetch, the object will be created
     * when you first time fetch it.
     */
    @Nullable
    private Set<String> mSchemasNotDisplayedBySystemCached;

    /**
     * This map contains all schemas and {@link PackageIdentifier} that has access to the schema.
     * All keys in the map are prefixed with the package-database prefix. We do lazy fetch, the
     * object will be created when you first time fetch it.
     */
    @Nullable
    private Map<String, Set<PackageIdentifier>> mSchemasVisibleToPackagesCached;

    /**
     * This map contains all schemas and Android Permissions combinations that are required to
     * access the schema. All keys in the map are prefixed with the package-database prefix. We
     * do lazy fetch, the object will be created when you first time fetch it.
     * The Map is constructed in ANY-ALL cases. The querier could read the {@link GenericDocument}
     * objects under the {@code schemaType} if they hold ALL required permissions of ANY
     * combinations.
     * @see SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility(String, Set)
     */
    @Nullable
    private Map<String, Set<Set<Integer>>> mSchemasVisibleToPermissionsCached;

    /**
     * This map contains all publicly visible schemas and the {@link PackageIdentifier} specifying
     * the package that the schemas are from.
     */
    @Nullable
    private Map<String, PackageIdentifier> mPubliclyVisibleSchemasCached;

    /**
     * This map contains all {@link SchemaVisibilityConfig}s that has access to the schema.
     * All keys in the map are prefixed with the package-database prefix. We do lazy fetch, the
     * object will be created when you first time fetch it.
     */
    @Nullable
    private Map<String, Set<SchemaVisibilityConfig>> mSchemasVisibleToConfigsCached;

    @Constructor
    GetSchemaResponse(
            @Param(id = 1) int version,
            @Param(id = 2) @NonNull List<AppSearchSchema> schemas,
            @Param(id = 3) @Nullable List<InternalVisibilityConfig> visibilityConfigs) {
        mVersion = version;
        mSchemas = Preconditions.checkNotNull(schemas);
        mVisibilityConfigs = visibilityConfigs;
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
    @NonNull
    public Set<AppSearchSchema> getSchemas() {
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
    @NonNull
    public Set<String> getSchemaTypesNotDisplayedBySystem() {
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
    @NonNull
    public Map<String, Set<PackageIdentifier>> getSchemaTypesVisibleToPackages() {
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
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.ADD_PERMISSIONS_AND_GET_VISIBILITY)
    @NonNull
    public Map<String, Set<Set<Integer>>> getRequiredPermissionsForSchemaTypeVisibility() {
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
    @NonNull
    public Map<String, PackageIdentifier> getPubliclyVisibleSchemas() {
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
    @NonNull
    public Map<String, Set<SchemaVisibilityConfig>> getSchemaTypesVisibleToConfigs() {
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

    @NonNull
    private List<InternalVisibilityConfig> getVisibilityConfigsOrThrow() {
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
        @Nullable
        private Map<String, InternalVisibilityConfig.Builder> mVisibilityConfigBuilders;
        private boolean mBuilt = false;

        /** Creates a new {@link Builder} */
        public Builder() {
            setVisibilitySettingSupported(true);
        }

        /** Creates a new {@link Builder} from the given {@link GetSchemaResponse}. */
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
        }

        /**
         * Sets the database overall schema version.
         *
         * <p>Default version is 0
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setVersion(@IntRange(from = 0) int version) {
            Preconditions.checkArgument(version >= 0, "Version must be a non-negative number.");
            resetIfBuilt();
            mVersion = version;
            return this;
        }

        /** Adds one {@link AppSearchSchema} to the schema list. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addSchema(@NonNull AppSearchSchema schema) {
            Preconditions.checkNotNull(schema);
            resetIfBuilt();
            mSchemas.add(schema);
            return this;
        }

        /** Clears all {@link AppSearchSchema}s from the list of schemas. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearSchemas() {
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
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addSchemaTypeNotDisplayedBySystem(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            InternalVisibilityConfig.Builder visibilityConfigBuilder =
                    getOrCreateVisibilityConfigBuilder(schemaType);
            visibilityConfigBuilder.setNotDisplayedBySystem(true);
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
        @NonNull
        public Builder setSchemaTypeVisibleToPackages(
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
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        // @SetSchemaRequest is an IntDef annotation applied to Set<Set<Integer>>.
        @SuppressWarnings("SupportAnnotationUsage")
        @NonNull
        public Builder setRequiredPermissionsForSchemaTypeVisibility(
                @NonNull String schemaType,
                @SetSchemaRequest.AppSearchSupportedPermission @NonNull
                        Set<Set<Integer>> visibleToPermissionSets) {
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
         * Specify that the schema should be publicly available, to packages which already have
         * visibility to {@code packageIdentifier}.
         *
         * @param schemaType the schema to make publicly accessible.
         * @param packageIdentifier the package from which the document schema is from.
         * @see SetSchemaRequest.Builder#setPubliclyVisibleSchema
         */
        // Merged list available from getPubliclyVisibleSchemas
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @FlaggedApi(Flags.FLAG_ENABLE_SET_PUBLICLY_VISIBLE_SCHEMA)
        @NonNull
        public Builder setPubliclyVisibleSchema(
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
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
        @NonNull
        public Builder setSchemaTypeVisibleToConfigs(@NonNull String schemaType,
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
         * Clears the visibility settings configured through
         * {@link Builder#addSchemaTypeNotDisplayedBySystem},
         * {@link Builder#setSchemaTypeVisibleToPackages},
         * {@link Builder#setRequiredPermissionsForSchemaTypeVisibility},
         * {@link Builder#setPubliclyVisibleSchema(String, PackageIdentifier)}, and
         * {@link Builder#setSchemaTypeVisibleToConfigs} for the given {@code schemaType}.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearSchemaTypeVisibilityConfig(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            if (mVisibilityConfigBuilders != null) {
                mVisibilityConfigBuilders.remove(schemaType);
            }
            return this;
        }

        /**
         * Clears the visibility settings configured through
         * {@link Builder#addSchemaTypeNotDisplayedBySystem},
         * {@link Builder#setSchemaTypeVisibleToPackages},
         * {@link Builder#setRequiredPermissionsForSchemaTypeVisibility},
         * {@link Builder#setPubliclyVisibleSchema(String, PackageIdentifier)}, and
         * {@link Builder#setSchemaTypeVisibleToConfigs} for all schema types.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearSchemaTypeVisibilityConfigs() {
            resetIfBuilt();
            if (mVisibilityConfigBuilders != null) {
                mVisibilityConfigBuilders.clear();
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
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setVisibilitySettingSupported(boolean visibilitySettingSupported) {
            if (visibilitySettingSupported) {
                mVisibilityConfigBuilders = new ArrayMap<>();
            } else {
                mVisibilityConfigBuilders = null;
            }
            return this;
        }

        /** Builds a {@link GetSchemaResponse} object. */
        @NonNull
        public GetSchemaResponse build() {
            List<InternalVisibilityConfig> visibilityConfigs = null;
            if (mVisibilityConfigBuilders != null) {
                visibilityConfigs = new ArrayList<>();
                for (InternalVisibilityConfig.Builder builder :
                        mVisibilityConfigBuilders.values()) {
                    visibilityConfigs.add(builder.build());
                }
            }
            mBuilt = true;
            return new GetSchemaResponse(mVersion, mSchemas, visibilityConfigs);
        }

        @NonNull
        private InternalVisibilityConfig.Builder getOrCreateVisibilityConfigBuilder(
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

        private void resetIfBuilt() {
            if (mBuilt) {
                // No need to copy mVisibilityConfigBuilders -- it gets copied during build().
                mSchemas = new ArrayList<>(mSchemas);
                mBuilt = false;
            }
        }
    }
}
