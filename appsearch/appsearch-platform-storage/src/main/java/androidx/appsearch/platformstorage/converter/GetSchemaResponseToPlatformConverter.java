/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.platformstorage.converter;

import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates between Platform and Jetpack versions of {@link GetSchemaResponse}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class GetSchemaResponseToPlatformConverter {
    private GetSchemaResponseToPlatformConverter() {}

    /**
     * Translates a platform {@link android.app.appsearch.GetSchemaResponse} into a jetpack
     * {@link GetSchemaResponse}.
     */
    @NonNull
    public static GetSchemaResponse toJetpackGetSchemaResponse(
            @NonNull android.app.appsearch.GetSchemaResponse platformResponse) {
        Preconditions.checkNotNull(platformResponse);
        GetSchemaResponse.Builder jetpackBuilder = new GetSchemaResponse.Builder();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Android API level in S-v2 and lower won't have visibility information.
            jetpackBuilder.setVisibilitySettingSupported(false);
        }
        for (android.app.appsearch.AppSearchSchema platformSchema : platformResponse.getSchemas()) {
            jetpackBuilder.addSchema(SchemaToPlatformConverter.toJetpackSchema(platformSchema));
        }
        jetpackBuilder.setVersion(platformResponse.getVersion());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Convert schemas not displayed by system
            for (String schemaTypeNotDisplayedBySystem :
                    ApiHelperForT.getSchemaTypesNotDisplayedBySystem(platformResponse)) {
                jetpackBuilder.addSchemaTypeNotDisplayedBySystem(schemaTypeNotDisplayedBySystem);
            }
            // Convert schemas visible to packages
            convertSchemasVisibleToPackages(platformResponse, jetpackBuilder);
            // Convert schemas visible to permissions
            for (Map.Entry<String, Set<Set<Integer>>> entry :
                    ApiHelperForT.getRequiredPermissionsForSchemaTypeVisibility(platformResponse)
                            .entrySet()) {
                jetpackBuilder.setRequiredPermissionsForSchemaTypeVisibility(entry.getKey(),
                        entry.getValue());
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Convert publicly visible schemas
            Map<String, PackageIdentifier> publiclyVisibleSchemas =
                    ApiHelperForV.getPubliclyVisibleSchemas(platformResponse);
            if (!publiclyVisibleSchemas.isEmpty()) {
                for (Map.Entry<String, PackageIdentifier> entry :
                        publiclyVisibleSchemas.entrySet()) {
                    jetpackBuilder.setPubliclyVisibleSchema(entry.getKey(), entry.getValue());
                }
            }

            // Convert schemas visible to configs
            Map<String, Set<SchemaVisibilityConfig>> schemasVisibleToConfigs =
                    ApiHelperForV.getSchemasVisibleToConfigs(platformResponse);
            if (!schemasVisibleToConfigs.isEmpty()) {
                for (Map.Entry<String, Set<SchemaVisibilityConfig>> entry :
                        schemasVisibleToConfigs.entrySet()) {
                    jetpackBuilder.setSchemaTypeVisibleToConfigs(entry.getKey(), entry.getValue());
                }
            }
        }

        return jetpackBuilder.build();
    }

    /**
     * Adds package visibilities in a platform {@link android.app.appsearch.GetSchemaResponse} into
     * the given jetpack {@link GetSchemaResponse}.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static void convertSchemasVisibleToPackages(
            @NonNull android.app.appsearch.GetSchemaResponse platformResponse,
            @NonNull GetSchemaResponse.Builder jetpackBuilder) {
        // TODO(b/205749173): If there were no packages, getSchemaTypesVisibleToPackages
        //  incorrectly returns {@code null} in some prerelease versions of Android T. Remove
        //  this workaround after the issue is fixed in T.
        Map<String, Set<android.app.appsearch.PackageIdentifier>> schemaTypesVisibleToPackages =
                ApiHelperForT.getSchemaTypesVisibleToPackage(platformResponse);
        if (schemaTypesVisibleToPackages != null) {
            for (Map.Entry<String, Set<android.app.appsearch.PackageIdentifier>> entry
                    : schemaTypesVisibleToPackages.entrySet()) {
                Set<PackageIdentifier> jetpackPackageIdentifiers =
                        new ArraySet<>(entry.getValue().size());
                for (android.app.appsearch.PackageIdentifier frameworkPackageIdentifier
                        : entry.getValue()) {
                    jetpackPackageIdentifiers.add(new PackageIdentifier(
                            frameworkPackageIdentifier.getPackageName(),
                            frameworkPackageIdentifier.getSha256Certificate()));
                }
                jetpackBuilder.setSchemaTypeVisibleToPackages(
                        entry.getKey(), jetpackPackageIdentifiers);
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static class ApiHelperForT {
        private ApiHelperForT() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Set<String> getSchemaTypesNotDisplayedBySystem(
                android.app.appsearch.GetSchemaResponse platformResponse) {
            return platformResponse.getSchemaTypesNotDisplayedBySystem();
        }

        @DoNotInline
        static Map<String, Set<android.app.appsearch.PackageIdentifier>>
                getSchemaTypesVisibleToPackage(
                    android.app.appsearch.GetSchemaResponse platformResponse) {
            return platformResponse.getSchemaTypesVisibleToPackages();
        }

        @DoNotInline
        static Map<String, Set<Set<Integer>>> getRequiredPermissionsForSchemaTypeVisibility(
                android.app.appsearch.GetSchemaResponse platformResponse) {
            return platformResponse.getRequiredPermissionsForSchemaTypeVisibility();
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private static class ApiHelperForV {
        private ApiHelperForV() {}

        @DoNotInline
        @NonNull
        static Map<String, PackageIdentifier> getPubliclyVisibleSchemas(
                android.app.appsearch.GetSchemaResponse platformResponse) {
            Map<String, android.app.appsearch.PackageIdentifier> platformPubliclyVisibleSchemas =
                    platformResponse.getPubliclyVisibleSchemas();
            if (platformPubliclyVisibleSchemas.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, PackageIdentifier> jetpackPubliclyVisibleSchemas =
                    new ArrayMap<>(platformPubliclyVisibleSchemas.size());
            for (Map.Entry<String, android.app.appsearch.PackageIdentifier> entry :
                    platformPubliclyVisibleSchemas.entrySet()) {
                jetpackPubliclyVisibleSchemas.put(
                        entry.getKey(),
                        new PackageIdentifier(
                                entry.getValue().getPackageName(),
                                entry.getValue().getSha256Certificate()));
            }
            return jetpackPubliclyVisibleSchemas;
        }

        @DoNotInline
        @NonNull
        static Map<String, Set<SchemaVisibilityConfig>> getSchemasVisibleToConfigs(
                android.app.appsearch.GetSchemaResponse platformResponse) {
            Map<String, Set<android.app.appsearch.SchemaVisibilityConfig>>
                    platformSchemasVisibleToConfigs =
                    platformResponse.getSchemaTypesVisibleToConfigs();
            if (platformSchemasVisibleToConfigs.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Set<SchemaVisibilityConfig>> jetpackSchemasVisibleToConfigs =
                    new ArrayMap<>(platformSchemasVisibleToConfigs.size());
            for (Map.Entry<String, Set<android.app.appsearch.SchemaVisibilityConfig>> entry :
                    platformSchemasVisibleToConfigs.entrySet()) {
                Set<SchemaVisibilityConfig> jetpackConfigPerType =
                        new ArraySet<>(entry.getValue().size());
                for (android.app.appsearch.SchemaVisibilityConfig platformConfigPerType :
                        entry.getValue()) {
                    SchemaVisibilityConfig jetpackConfig =
                            toJetpackSchemaVisibilityConfig(platformConfigPerType);
                    jetpackConfigPerType.add(jetpackConfig);
                }
                jetpackSchemasVisibleToConfigs.put(entry.getKey(), jetpackConfigPerType);
            }
            return jetpackSchemasVisibleToConfigs;
        }

        /**
         * Translates a platform {@link android.app.appsearch.SchemaVisibilityConfig} into a jetpack
         * {@link SchemaVisibilityConfig}.
         */
        @NonNull
        private static SchemaVisibilityConfig toJetpackSchemaVisibilityConfig(
                @NonNull android.app.appsearch.SchemaVisibilityConfig platformConfig) {
            Preconditions.checkNotNull(platformConfig);
            SchemaVisibilityConfig.Builder jetpackBuilder = new SchemaVisibilityConfig.Builder();

            // Translate allowedPackages
            List<android.app.appsearch.PackageIdentifier> allowedPackages =
                    platformConfig.getAllowedPackages();
            for (int i = 0; i < allowedPackages.size(); i++) {
                jetpackBuilder.addAllowedPackage(new PackageIdentifier(
                        allowedPackages.get(i).getPackageName(),
                        allowedPackages.get(i).getSha256Certificate()));
            }

            // Translate requiredPermissions
            for (Set<Integer> requiredPermissions : platformConfig.getRequiredPermissions()) {
                jetpackBuilder.addRequiredPermissions(requiredPermissions);
            }

            // Translate publiclyVisibleTargetPackage
            android.app.appsearch.PackageIdentifier publiclyVisibleTargetPackage =
                    platformConfig.getPubliclyVisibleTargetPackage();
            if (publiclyVisibleTargetPackage != null) {
                jetpackBuilder.setPubliclyVisibleTargetPackage(
                        new PackageIdentifier(
                                publiclyVisibleTargetPackage.getPackageName(),
                                publiclyVisibleTargetPackage.getSha256Certificate()));
            }

            return jetpackBuilder.build();
        }
    }
}
