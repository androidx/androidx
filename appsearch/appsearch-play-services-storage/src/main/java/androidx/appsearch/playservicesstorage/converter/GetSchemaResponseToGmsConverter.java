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
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

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
        return jetpackBuilder.build();
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
}
