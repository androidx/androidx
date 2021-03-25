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
import androidx.appsearch.app.GetByUriRequest;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByUriRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates between Platform and Jetpack versions of requests.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class RequestToPlatformConverter {
    private RequestToPlatformConverter() {}

    /**
     * Translates a jetpack {@link androidx.appsearch.app.SetSchemaRequest} into a platform
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
        platformBuilder.setForceOverride(jetpackRequest.isForceOverride());
        return platformBuilder.build();
    }

    /**
     * Translates a platform {@link android.app.appsearch.SetSchemaResponse} into a jetpack
     * {@link androidx.appsearch.app.SetSchemaResponse}.
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
            jetpackBuilder.addMigrationFailure(new SetSchemaResponse.MigrationFailure.Builder()
                    .setSchemaType(migrationFailure.getSchemaType())
                    .setNamespace(migrationFailure.getNamespace())
                    .setUri(migrationFailure.getUri())
                    .setAppSearchResult(
                            AppSearchResultToPlatformConverter.platformAppSearchResultToJetpack(
                                    migrationFailure.getAppSearchResult())).build());
        }
        return jetpackBuilder.build();
    }

    /**
     * Translates a jetpack {@link PutDocumentsRequest} into a platform
     * {@link android.app.appsearch.PutDocumentsRequest}.
     */
    @NonNull
    public static android.app.appsearch.PutDocumentsRequest toPlatformPutDocumentsRequest(
            @NonNull PutDocumentsRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        android.app.appsearch.PutDocumentsRequest.Builder platformBuilder =
                new android.app.appsearch.PutDocumentsRequest.Builder();
        for (GenericDocument jetpackDocument : jetpackRequest.getGenericDocuments()) {
            platformBuilder.addGenericDocuments(
                    GenericDocumentToPlatformConverter.toPlatformGenericDocument(jetpackDocument));
        }
        return platformBuilder.build();
    }

    /**
     * Translates a jetpack {@link GetByUriRequest} into a platform
     * {@link android.app.appsearch.GetByUriRequest}.
     */
    @NonNull
    public static android.app.appsearch.GetByUriRequest toPlatformGetByUriRequest(
            @NonNull GetByUriRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        android.app.appsearch.GetByUriRequest.Builder platformBuilder =
                new android.app.appsearch.GetByUriRequest.Builder(jetpackRequest.getNamespace())
                        .addUris(jetpackRequest.getUris());
        for (Map.Entry<String, List<String>> projection :
                jetpackRequest.getProjectionsInternal().entrySet()) {
            platformBuilder.addProjection(projection.getKey(), projection.getValue());
        }
        return platformBuilder.build();
    }

    /**
     * Translates a jetpack {@link RemoveByUriRequest} into a platform
     * {@link android.app.appsearch.RemoveByUriRequest}.
     */
    @NonNull
    public static android.app.appsearch.RemoveByUriRequest toPlatformRemoveByUriRequest(
            @NonNull RemoveByUriRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        return new android.app.appsearch.RemoveByUriRequest.Builder(jetpackRequest.getNamespace())
                .addUris(jetpackRequest.getUris())
                .build();
    }

    /**
     * Translates a jetpack {@link androidx.appsearch.app.ReportUsageRequest} into a platform
     * {@link android.app.appsearch.ReportUsageRequest}.
     */
    @NonNull
    public static android.app.appsearch.ReportUsageRequest toPlatformReportUsageRequest(
            @NonNull ReportUsageRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        return new android.app.appsearch.ReportUsageRequest.Builder(jetpackRequest.getNamespace())
                .setUri(jetpackRequest.getUri())
                .setUsageTimeMillis(jetpackRequest.getUsageTimeMillis())
                .build();
    }
}
