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

package androidx.appsearch.localstorage.visibilitystore;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.stats.CallStats;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.collection.ArrayMap;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The helper class to store Visibility Document information of version 0 and handle the upgrade to
 * version 1.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VisibilityStoreMigrationHelperFromV0 {
    private VisibilityStoreMigrationHelperFromV0() {}
    /** Prefix to add to all visibility document ids. IcingSearchEngine doesn't allow empty ids. */
    private static final String DEPRECATED_ID_PREFIX = "uri:";

    /** Schema type for documents that hold AppSearch's metadata, e.g. visibility settings */
    @VisibleForTesting
    static final String DEPRECATED_VISIBILITY_SCHEMA_TYPE = "VisibilityType";

    /**
     * Property that holds the list of platform-hidden schemas, as part of the visibility settings.
     */
    @VisibleForTesting
    static final String DEPRECATED_NOT_DISPLAYED_BY_SYSTEM_PROPERTY =
            "notPlatformSurfaceable";

    /** Property that holds nested documents of package accessible schemas. */
    @VisibleForTesting
    static final String DEPRECATED_VISIBLE_TO_PACKAGES_PROPERTY = "packageAccessible";

    /**
     * Property that holds the list of platform-hidden schemas, as part of the visibility settings.
     */
    @VisibleForTesting
    static final String DEPRECATED_PACKAGE_SCHEMA_TYPE = "PackageAccessibleType";

    /** Property that holds the prefixed schema type that is accessible by some package. */
    @VisibleForTesting
    static final String DEPRECATED_ACCESSIBLE_SCHEMA_PROPERTY = "accessibleSchema";

    /** Property that holds the package name that can access a schema. */
    @VisibleForTesting
    static final String DEPRECATED_PACKAGE_NAME_PROPERTY = "packageName";

    /** Property that holds the SHA 256 certificate of the app that can access a schema. */
    @VisibleForTesting
    static final String DEPRECATED_SHA_256_CERT_PROPERTY = "sha256Cert";

//    The visibility schema of version 0.
//---------------------------------------------------------------------------------------------
//    Schema of DEPRECATED_VISIBILITY_SCHEMA_TYPE:
//    new AppSearchSchema.Builder(
//            DEPRECATED_VISIBILITY_SCHEMA_TYPE)
//            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
//                    DEPRECATED_NOT_DISPLAYED_BY_SYSTEM_PROPERTY)
//                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
//                    .build())
//            .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
//                    DEPRECATED_VISIBLE_TO_PACKAGES_PROPERTY,
//                    DEPRECATED_PACKAGE_SCHEMA_TYPE)
//                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
//                    .build())
//            .build();
//    Schema of DEPRECATED_PACKAGE_SCHEMA_TYPE:
//    new AppSearchSchema.Builder(DEPRECATED_PACKAGE_SCHEMA_TYPE)
//        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
//                 DEPRECATED_PACKAGE_NAME_PROPERTY)
//                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
//                .build())
//        .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder(
//                DEPRECATED_SHA_256_CERT_PROPERTY)
//                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
//                .build())
//        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
//                DEPRECATED_ACCESSIBLE_SCHEMA_PROPERTY)
//                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
//                .build())
//        .build();
//---------------------------------------------------------------------------------------------

    /** Returns whether the given schema type is deprecated.     */
    static boolean isDeprecatedType(@NonNull String schemaType) {
        return schemaType.equals(DEPRECATED_VISIBILITY_SCHEMA_TYPE)
                || schemaType.equals(DEPRECATED_PACKAGE_SCHEMA_TYPE);
    }

    /**
     * Adds a prefix to create a deprecated visibility document's id.
     *
     * @param packageName Package to which the visibility doc refers.
     * @param databaseName Database to which the visibility doc refers.
     * @return deprecated visibility document's id.
     */
    static @NonNull String getDeprecatedVisibilityDocumentId(
            @NonNull String packageName, @NonNull String databaseName) {
        return DEPRECATED_ID_PREFIX + PrefixUtil.createPrefix(packageName, databaseName);
    }

    /**  Reads all stored deprecated Visibility Document in version 0 from icing. */
    static List<GenericDocument> getVisibilityDocumentsInVersion0(
            @NonNull GetSchemaResponse getSchemaResponse,
            @NonNull AppSearchImpl appSearchImpl,
            CallStats.@Nullable Builder callStatsBuilder) throws AppSearchException {
        if (!hasDeprecatedType(getSchemaResponse)) {
            return new ArrayList<>();
        }
        Map<String, Set<String>> packageToDatabases = appSearchImpl.getPackageToDatabases();
        List<GenericDocument> deprecatedDocuments = new ArrayList<>(packageToDatabases.size());
        for (Map.Entry<String, Set<String>> entry : packageToDatabases.entrySet()) {
            String packageName = entry.getKey();
            if (packageName.equals(VisibilityStore.VISIBILITY_PACKAGE_NAME)) {
                continue; // Our own package. Skip.
            }
            for (String databaseName : entry.getValue()) {
                try {
                    // Note: We use the other clients' prefixed names as ids
                    deprecatedDocuments.add(appSearchImpl.getDocument(
                            VisibilityStore.VISIBILITY_PACKAGE_NAME,
                            VisibilityStore.DOCUMENT_VISIBILITY_DATABASE_NAME,
                            VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                            getDeprecatedVisibilityDocumentId(packageName, databaseName),
                            /*typePropertyPaths=*/ Collections.emptyMap(),
                            callStatsBuilder));
                } catch (AppSearchException e) {
                    if (e.getResultCode() == AppSearchResult.RESULT_NOT_FOUND) {
                        // TODO(b/172068212): This indicates some desync error. We were expecting a
                        //  document, but didn't find one. Should probably reset AppSearch instead
                        //  of ignoring it.
                        continue;
                    }
                    // Otherwise, this is some other error we should pass up.
                    throw e;
                }
            }
        }
        return deprecatedDocuments;
    }

    /**
     * Converts the given list of deprecated Visibility Documents into a Map of {@code
     * <PrefixedSchemaType, VisibilityDocument.Builder of the latest version>}.
     *
     * @param visibilityDocumentV0s          The deprecated Visibility Document we found.
     */
    static @NonNull List<VisibilityDocumentV1> toVisibilityDocumentV1(
            @NonNull List<GenericDocument> visibilityDocumentV0s) {
        Map<String, VisibilityDocumentV1.Builder> documentBuilderMap = new ArrayMap<>();

        // Set all visibility information into documentBuilderMap
        for (int i = 0; i < visibilityDocumentV0s.size(); i++) {
            GenericDocument visibilityDocumentV0 = visibilityDocumentV0s.get(i);

            // Read not displayed by system property field.
            String[] notDisplayedBySystemSchemas = visibilityDocumentV0.getPropertyStringArray(
                    DEPRECATED_NOT_DISPLAYED_BY_SYSTEM_PROPERTY);
            if (notDisplayedBySystemSchemas != null) {
                for (String notDisplayedBySystemSchema : notDisplayedBySystemSchemas) {
                    // SetSchemaRequest.Builder.build() make sure all schemas that has visibility
                    // setting must present in the requests.
                    VisibilityDocumentV1.Builder visibilityBuilder = getOrCreateBuilder(
                            documentBuilderMap, notDisplayedBySystemSchema);
                    visibilityBuilder.setNotDisplayedBySystem(true);
                }
            }

            // Read visible to packages field.
            GenericDocument[] deprecatedPackageDocuments = visibilityDocumentV0
                    .getPropertyDocumentArray(DEPRECATED_VISIBLE_TO_PACKAGES_PROPERTY);
            if (deprecatedPackageDocuments != null) {
                for (GenericDocument deprecatedPackageDocument : deprecatedPackageDocuments) {
                    String prefixedSchemaType = Preconditions.checkNotNull(
                            deprecatedPackageDocument.getPropertyString(
                                    DEPRECATED_ACCESSIBLE_SCHEMA_PROPERTY));
                    VisibilityDocumentV1.Builder visibilityBuilder =
                            getOrCreateBuilder(documentBuilderMap, prefixedSchemaType);
                    String packageName = Preconditions.checkNotNull(
                            deprecatedPackageDocument.getPropertyString(
                                    DEPRECATED_PACKAGE_NAME_PROPERTY));
                    byte[] sha256Cert = Preconditions.checkNotNull(
                            deprecatedPackageDocument.getPropertyBytes(
                                    DEPRECATED_SHA_256_CERT_PROPERTY));
                    visibilityBuilder.addVisibleToPackage(
                            new PackageIdentifier(packageName, sha256Cert));
                }
            }
        }
        List<VisibilityDocumentV1> visibilityDocumentsV1 =
                new ArrayList<>(documentBuilderMap.size());
        for (Map.Entry<String, VisibilityDocumentV1.Builder> entry :
                documentBuilderMap.entrySet()) {
            visibilityDocumentsV1.add(entry.getValue().build());
        }
        return visibilityDocumentsV1;
    }

    /**
     * Return whether the database maybe has the oldest version of deprecated schema.
     *
     * <p> Since the current version number is 0, it is possible that the database is just empty
     * and it return 0 as the default version number. So we need to check if the deprecated document
     * presents to trigger the migration.
     */
    private static boolean hasDeprecatedType(@NonNull GetSchemaResponse getSchemaResponse) {
        for (AppSearchSchema schema : getSchemaResponse.getSchemas()) {
            if (VisibilityStoreMigrationHelperFromV0
                    .isDeprecatedType(schema.getSchemaType())) {
                // Found deprecated type, we need to migrate visibility Document. And it's
                // not possible for us to find the latest visibility schema.
                return true;
            }
        }
        return false;
    }

    private static VisibilityDocumentV1.@NonNull Builder getOrCreateBuilder(
            @NonNull Map<String, VisibilityDocumentV1.Builder> documentBuilderMap,
            @NonNull String schemaType) {
        VisibilityDocumentV1.Builder builder = documentBuilderMap.get(schemaType);
        if (builder == null) {
            builder = new VisibilityDocumentV1.Builder(/*id=*/ schemaType);
            documentBuilderMap.put(schemaType, builder);
        }
        return builder;
    }
}
