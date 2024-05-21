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

package androidx.appsearch.playservicesstorage.converter;

import androidx.annotation.NonNull;
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
 * Translates between Gms and Jetpack versions of {@link GetSchemaResponse}.

 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GetSchemaResponseToGmsConverter {
    private GetSchemaResponseToGmsConverter() {
    }

    /**
     * Translates a Gms {@link com.google.android.gms.appsearch.GetSchemaResponse}
     * into a jetpack
     * {@link GetSchemaResponse}.
     */
    @NonNull
    public static GetSchemaResponse toJetpackGetSchemaResponse(
            @NonNull com.google.android.gms.appsearch.GetSchemaResponse
                    gmsResponse) {
        Preconditions.checkNotNull(gmsResponse);
        GetSchemaResponse.Builder jetpackBuilder;
        jetpackBuilder = new GetSchemaResponse.Builder();
        for (com.google.android.gms.appsearch.AppSearchSchema gmsSchema :
                gmsResponse.getSchemas()) {
            jetpackBuilder.addSchema(SchemaToGmsConverter.toJetpackSchema(
                    gmsSchema));
        }
        jetpackBuilder.setVersion(gmsResponse.getVersion());
        // Convert schemas not displayed by system
        for (String schemaTypeNotDisplayedBySystem :
                gmsResponse.getSchemaTypesNotDisplayedBySystem()) {
            jetpackBuilder.addSchemaTypeNotDisplayedBySystem(schemaTypeNotDisplayedBySystem);
        }
        // Convert schemas visible to packages
        convertSchemasVisibleToPackages(gmsResponse, jetpackBuilder);
        // Convert schemas visible to permissions
        for (Map.Entry<String, Set<Set<Integer>>> entry :
                gmsResponse.getRequiredPermissionsForSchemaTypeVisibility()
                        .entrySet()) {
            jetpackBuilder.setRequiredPermissionsForSchemaTypeVisibility(entry.getKey(),
                    entry.getValue());
        }
        // Convert publicly visible schemas
        Map<String, PackageIdentifier> publiclyVisibleSchemas =
                getPubliclyVisibleSchemas(gmsResponse);
        if (!publiclyVisibleSchemas.isEmpty()) {
            for (Map.Entry<String, PackageIdentifier> entry :
                    publiclyVisibleSchemas.entrySet()) {
                jetpackBuilder.setPubliclyVisibleSchema(entry.getKey(), entry.getValue());
            }
        }

        // Convert schemas visible to configs
        Map<String, Set<SchemaVisibilityConfig>> schemasVisibleToConfigs =
                getSchemasVisibleToConfigs(gmsResponse);
        if (!schemasVisibleToConfigs.isEmpty()) {
            for (Map.Entry<String, Set<SchemaVisibilityConfig>> entry :
                    schemasVisibleToConfigs.entrySet()) {
                jetpackBuilder.setSchemaTypeVisibleToConfigs(entry.getKey(), entry.getValue());
            }
        }
        return jetpackBuilder.build();
    }

    private static Map<String, PackageIdentifier> getPubliclyVisibleSchemas(
            com.google.android.gms.appsearch.GetSchemaResponse gmsResponse) {
        Map<String, com.google.android.gms.appsearch.PackageIdentifier>
                gmsPubliclyVisibleSchemas = gmsResponse.getPubliclyVisibleSchemas();
        if (gmsPubliclyVisibleSchemas.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, PackageIdentifier> jetpackPubliclyVisibleSchemas =
                new ArrayMap<>(gmsPubliclyVisibleSchemas.size());
        for (Map.Entry<String, com.google.android.gms.appsearch.PackageIdentifier> entry :
                gmsPubliclyVisibleSchemas.entrySet()) {
            jetpackPubliclyVisibleSchemas.put(
                    entry.getKey(),
                    new PackageIdentifier(
                            entry.getValue().getPackageName(),
                            entry.getValue().getSha256Certificate()));
        }
        return jetpackPubliclyVisibleSchemas;
    }

    private static Map<String, Set<SchemaVisibilityConfig>> getSchemasVisibleToConfigs(
            com.google.android.gms.appsearch.GetSchemaResponse gmsResponse) {
        Map<String, Set<com.google.android.gms.appsearch.SchemaVisibilityConfig>>
                gmsSchemasVisibleToConfigs =
                gmsResponse.getSchemaTypesVisibleToConfigs();
        if (gmsSchemasVisibleToConfigs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Set<SchemaVisibilityConfig>> jetpackSchemasVisibleToConfigs =
                new ArrayMap<>(gmsSchemasVisibleToConfigs.size());
        for (Map.Entry<String, Set<com.google.android.gms.appsearch.SchemaVisibilityConfig>> entry :
                gmsSchemasVisibleToConfigs.entrySet()) {
            Set<SchemaVisibilityConfig> jetpackConfigPerType =
                    new ArraySet<>(entry.getValue().size());
            for (com.google.android.gms.appsearch.SchemaVisibilityConfig gmsConfigPerType :
                    entry.getValue()) {
                SchemaVisibilityConfig jetpackConfig =
                        toJetpackSchemaVisibilityConfig(gmsConfigPerType);
                jetpackConfigPerType.add(jetpackConfig);
            }
            jetpackSchemasVisibleToConfigs.put(entry.getKey(), jetpackConfigPerType);
        }
        return jetpackSchemasVisibleToConfigs;
    }

    /**
     * Adds package visibilities in a Gms
     * {@link com.google.android.gms.appsearch.GetSchemaResponse} into
     * the given jetpack {@link GetSchemaResponse}.
     */
    private static void convertSchemasVisibleToPackages(
            @NonNull com.google.android.gms.appsearch.GetSchemaResponse gmsResponse,
            @NonNull GetSchemaResponse.Builder jetpackBuilder) {
        Map<String, Set<com.google.android.gms.appsearch.PackageIdentifier>>
                schemaTypesVisibleToPackages =
                gmsResponse.getSchemaTypesVisibleToPackages();
        if (schemaTypesVisibleToPackages != null) {
            for (Map.Entry<String, Set<com.google.android.gms.appsearch.PackageIdentifier>> entry
                    : schemaTypesVisibleToPackages.entrySet()) {
                Set<PackageIdentifier> jetpackPackageIdentifiers =
                        new ArraySet<>(entry.getValue().size());
                for (com.google.android.gms.appsearch.PackageIdentifier frameworkPackageIdentifier
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

    /**
     * Translates a platform {@link com.google.android.gms.appsearch.SchemaVisibilityConfig} into
     * a jetpack
     * {@link SchemaVisibilityConfig}.
     */
    @NonNull
    private static SchemaVisibilityConfig toJetpackSchemaVisibilityConfig(
            @NonNull com.google.android.gms.appsearch.SchemaVisibilityConfig platformConfig) {
        Preconditions.checkNotNull(platformConfig);
        SchemaVisibilityConfig.Builder jetpackBuilder = new SchemaVisibilityConfig.Builder();

        // Translate allowedPackages
        List<com.google.android.gms.appsearch.PackageIdentifier> allowedPackages =
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
        com.google.android.gms.appsearch.PackageIdentifier publiclyVisibleTargetPackage =
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
