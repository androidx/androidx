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

package androidx.appsearch.platformstorage.converter;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
import java.util.function.Function;

/**
 * Translates between Platform and Jetpack versions of {@link SetSchemaRequest}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class SetSchemaRequestToPlatformConverter {
    private SetSchemaRequestToPlatformConverter() {}

    /**
     * Translates a jetpack {@link SetSchemaRequest} into a platform
     * {@link android.app.appsearch.SetSchemaRequest}.
     */
    @NonNull
    public static android.app.appsearch.SetSchemaRequest toPlatformSetSchemaRequest(
            @NonNull SetSchemaRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        android.app.appsearch.SetSchemaRequest.Builder platformBuilder =
                new android.app.appsearch.SetSchemaRequest.Builder();
        for (AppSearchSchema jetpackSchema : jetpackRequest.getSchemas()) {
            platformBuilder.addSchemas(SchemaToPlatformConverter.toPlatformSchema(jetpackSchema));
        }
        for (String schemaNotDisplayedBySystem : jetpackRequest.getSchemasNotDisplayedBySystem()) {
            platformBuilder.setSchemaTypeDisplayedBySystem(
                    schemaNotDisplayedBySystem, /*displayed=*/ false);
        }
        for (Map.Entry<String, Set<PackageIdentifier>> jetpackSchemaVisibleToPackage :
                jetpackRequest.getSchemasVisibleToPackagesInternal().entrySet()) {
            for (PackageIdentifier jetpackPackageIdentifier :
                    jetpackSchemaVisibleToPackage.getValue()) {
                platformBuilder.setSchemaTypeVisibilityForPackage(
                        jetpackSchemaVisibleToPackage.getKey(),
                        /*visible=*/ true,
                        new android.app.appsearch.PackageIdentifier(
                                jetpackPackageIdentifier.getPackageName(),
                                jetpackPackageIdentifier.getSha256Certificate()));
            }
        }
        if (!jetpackRequest.getRequiredPermissionsForSchemaTypeVisibility().isEmpty()) {
            // TODO(b/181908338) set getRequiredPermissionsForSchemaTypeVisibilities when we support
            //  it in framework.
            throw new UnsupportedOperationException(
                    "Set required permissions for schema type visibility are not supported with "
                            + "this backend/Android API level combination.");
        }
        for (Map.Entry<String, Migrator> entry : jetpackRequest.getMigrators().entrySet()) {
            Migrator jetpackMigrator = entry.getValue();
            android.app.appsearch.Migrator platformMigrator = new android.app.appsearch.Migrator() {
                @Override
                public boolean shouldMigrate(int currentVersion, int finalVersion) {
                    return jetpackMigrator.shouldMigrate(currentVersion, finalVersion);
                }

                @NonNull
                @Override
                public android.app.appsearch.GenericDocument onUpgrade(
                        int currentVersion,
                        int finalVersion,
                        @NonNull android.app.appsearch.GenericDocument inPlatformDocument) {
                    GenericDocument inJetpackDocument =
                            GenericDocumentToPlatformConverter.toJetpackGenericDocument(
                                    inPlatformDocument);
                    GenericDocument outJetpackDocument = jetpackMigrator.onUpgrade(
                            currentVersion, finalVersion, inJetpackDocument);
                    if (inJetpackDocument.equals(outJetpackDocument)) {
                        return inPlatformDocument; // Same object; no conversion occurred.
                    }
                    return GenericDocumentToPlatformConverter.toPlatformGenericDocument(
                            outJetpackDocument);
                }

                @NonNull
                @Override
                public android.app.appsearch.GenericDocument onDowngrade(
                        int currentVersion,
                        int finalVersion,
                        @NonNull android.app.appsearch.GenericDocument inPlatformDocument) {
                    GenericDocument inJetpackDocument =
                            GenericDocumentToPlatformConverter.toJetpackGenericDocument(
                                    inPlatformDocument);
                    GenericDocument outJetpackDocument = jetpackMigrator.onDowngrade(
                            currentVersion, finalVersion, inJetpackDocument);
                    if (inJetpackDocument.equals(outJetpackDocument)) {
                        return inPlatformDocument; // Same object; no conversion occurred.
                    }
                    return GenericDocumentToPlatformConverter.toPlatformGenericDocument(
                            outJetpackDocument);
                }
            };
            platformBuilder.setMigrator(entry.getKey(), platformMigrator);
        }
        return platformBuilder
                .setForceOverride(jetpackRequest.isForceOverride())
                .setVersion(jetpackRequest.getVersion())
                .build();
    }

    /**
     * Translates a platform {@link android.app.appsearch.SetSchemaResponse} into a jetpack
     * {@link SetSchemaResponse}.
     */
    @NonNull
    public static SetSchemaResponse toJetpackSetSchemaResponse(
            @NonNull android.app.appsearch.SetSchemaResponse platformResponse) {
        Preconditions.checkNotNull(platformResponse);
        SetSchemaResponse.Builder jetpackBuilder = new SetSchemaResponse.Builder()
                .addDeletedTypes(platformResponse.getDeletedTypes())
                .addIncompatibleTypes(platformResponse.getIncompatibleTypes())
                .addMigratedTypes(platformResponse.getMigratedTypes());
        for (android.app.appsearch.SetSchemaResponse.MigrationFailure migrationFailure :
                platformResponse.getMigrationFailures()) {
            jetpackBuilder.addMigrationFailure(new SetSchemaResponse.MigrationFailure(
                    migrationFailure.getNamespace(),
                    migrationFailure.getDocumentId(),
                    migrationFailure.getSchemaType(),
                    AppSearchResultToPlatformConverter.platformAppSearchResultToJetpack(
                            migrationFailure.getAppSearchResult(), Function.identity()))
            );
        }
        return jetpackBuilder.build();
    }
}
