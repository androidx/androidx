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
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.core.util.Preconditions;

import java.util.Map;
import java.util.Set;

/**
 * Translates between Gms and Jetpack versions of {@link SetSchemaRequest}.

 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SetSchemaRequestToGmsConverter {
    private SetSchemaRequestToGmsConverter() {
    }

    /**
     * Translates a jetpack {@link SetSchemaRequest} into a googleGms
     * {@link com.google.android.gms.appsearch.SetSchemaRequest}.
     */
    @NonNull
    public static com.google.android.gms.appsearch.SetSchemaRequest toGmsSetSchemaRequest(
            @NonNull SetSchemaRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        com.google.android.gms.appsearch.SetSchemaRequest.Builder gmsBuilder =
                new com.google.android.gms.appsearch.SetSchemaRequest.Builder();
        for (AppSearchSchema jetpackSchema : jetpackRequest.getSchemas()) {
            gmsBuilder.addSchemas(SchemaToGmsConverter.toGmsSchema(jetpackSchema));
        }
        for (String schemaNotDisplayedBySystem : jetpackRequest.getSchemasNotDisplayedBySystem()) {
            gmsBuilder.setSchemaTypeDisplayedBySystem(
                    schemaNotDisplayedBySystem, /*displayed=*/ false);
        }
        for (Map.Entry<String, Set<PackageIdentifier>> jetpackSchemaVisibleToPackage :
                jetpackRequest.getSchemasVisibleToPackagesInternal().entrySet()) {
            for (PackageIdentifier jetpackPackageIdentifier :
                    jetpackSchemaVisibleToPackage.getValue()) {
                gmsBuilder.setSchemaTypeVisibilityForPackage(
                        jetpackSchemaVisibleToPackage.getKey(),
                        /*visible=*/ true,
                        new com.google.android.gms.appsearch.PackageIdentifier(
                                jetpackPackageIdentifier.getPackageName(),
                                jetpackPackageIdentifier.getSha256Certificate()));
            }
        }
        if (!jetpackRequest.getRequiredPermissionsForSchemaTypeVisibility().isEmpty()) {
            for (Map.Entry<String, Set<Set<Integer>>> entry :
                    jetpackRequest.getRequiredPermissionsForSchemaTypeVisibility().entrySet()) {
                for (Set<Integer> permissionGroup : entry.getValue()) {
                    gmsBuilder.addRequiredPermissionsForSchemaTypeVisibility(
                            entry.getKey(), permissionGroup);
                }
            }
        }
        for (Map.Entry<String, Migrator> entry : jetpackRequest.getMigrators().entrySet()) {
            Migrator jetpackMigrator = entry.getValue();
            com.google.android.gms.appsearch.Migrator gmsMigrator =
                    new com.google.android.gms.appsearch.Migrator() {
                        @Override
                        public boolean shouldMigrate(int currentVersion, int finalVersion) {
                            return jetpackMigrator.shouldMigrate(currentVersion, finalVersion);
                        }

                        @NonNull
                        @Override
                        public com.google.android.gms.appsearch.GenericDocument onUpgrade(
                                int currentVersion,
                                int finalVersion,
                                @NonNull com.google.android.gms.appsearch.GenericDocument
                                        inGmsDocument) {
                            GenericDocument inJetpackDocument =
                                    GenericDocumentToGmsConverter
                                            .toJetpackGenericDocument(
                                                    inGmsDocument);
                            GenericDocument outJetpackDocument = jetpackMigrator.onUpgrade(
                                    currentVersion, finalVersion, inJetpackDocument);
                            if (inJetpackDocument.equals(outJetpackDocument)) {
                                return inGmsDocument; // Same object; no conversion occurred.
                            }
                            return GenericDocumentToGmsConverter
                                    .toGmsGenericDocument(
                                            outJetpackDocument);
                        }

                        @NonNull
                        @Override
                        public com.google.android.gms.appsearch.GenericDocument onDowngrade(
                                int currentVersion,
                                int finalVersion,
                                @NonNull com.google.android.gms.appsearch.GenericDocument
                                        inGmsDocument) {
                            GenericDocument inJetpackDocument =
                                    GenericDocumentToGmsConverter
                                            .toJetpackGenericDocument(
                                                    inGmsDocument);
                            GenericDocument outJetpackDocument = jetpackMigrator.onDowngrade(
                                    currentVersion, finalVersion, inJetpackDocument);
                            if (inJetpackDocument.equals(outJetpackDocument)) {
                                return inGmsDocument; // Same object; no conversion occurred.
                            }
                            return GenericDocumentToGmsConverter
                                    .toGmsGenericDocument(
                                            outJetpackDocument);
                        }
                    };
            gmsBuilder.setMigrator(entry.getKey(), gmsMigrator);
        }
        return gmsBuilder
                .setForceOverride(jetpackRequest.isForceOverride())
                .setVersion(jetpackRequest.getVersion())
                .build();
    }

    /**
     * Translates a gms
     * {@link com.google.android.gms.appsearch.SetSchemaResponse} into a jetpack
     * {@link SetSchemaResponse}.
     */
    @NonNull
    public static SetSchemaResponse toJetpackSetSchemaResponse(
            @NonNull com.google.android.gms.appsearch.SetSchemaResponse
                    gmsResponse) {
        Preconditions.checkNotNull(gmsResponse);
        SetSchemaResponse.Builder jetpackBuilder = new SetSchemaResponse.Builder()
                .addDeletedTypes(gmsResponse.getDeletedTypes())
                .addIncompatibleTypes(gmsResponse.getIncompatibleTypes())
                .addMigratedTypes(gmsResponse.getMigratedTypes());
        for (com.google.android.gms.appsearch.SetSchemaResponse.MigrationFailure migrationFailure :
                gmsResponse.getMigrationFailures()) {
            jetpackBuilder.addMigrationFailure(new SetSchemaResponse.MigrationFailure(
                    migrationFailure.getNamespace(),
                    migrationFailure.getDocumentId(),
                    migrationFailure.getSchemaType(),
                    AppSearchResultToGmsConverter.gmsAppSearchResultToJetpack(
                            migrationFailure.getAppSearchResult(), /* valueMapper= */ i -> i))
            );
        }
        return jetpackBuilder.build();
    }
}
