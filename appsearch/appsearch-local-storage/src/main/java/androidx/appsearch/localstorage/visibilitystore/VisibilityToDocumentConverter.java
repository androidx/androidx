/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.VisibilityConfig;
import androidx.appsearch.app.VisibilityPermissionConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Utilities for working with {@link VisibilityChecker} and {@link VisibilityStore}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VisibilityToDocumentConverter {
    private VisibilityToDocumentConverter() {}

    /**
     * The Schema type for documents that hold AppSearch's metadata, such as visibility settings.
     */
    public static final String VISIBILITY_DOCUMENT_SCHEMA_TYPE = "VisibilityType";
    /** Namespace of documents that contain visibility settings */
    public static final String VISIBILITY_DOCUMENT_NAMESPACE = "";

    /**
     * The Schema type for the public acl visibility overlay documents, that allow for additional
     * visibility settings.
     */
    public static final String PUBLIC_ACL_OVERLAY_SCHEMA_TYPE = "PublicAclOverlayType";
    /** Namespace of documents that contain public acl visibility settings */
    public static final String PUBLIC_ACL_OVERLAY_NAMESPACE = "overlay";

    /**
     * Property that holds the list of platform-hidden schemas, as part of the visibility settings.
     */
    private static final String NOT_DISPLAYED_BY_SYSTEM_PROPERTY = "notPlatformSurfaceable";

    /** Property that holds the package name that can access a schema. */
    private static final String VISIBLE_TO_PACKAGE_IDENTIFIER_PROPERTY = "packageName";

    /** Property that holds the SHA 256 certificate of the app that can access a schema. */
    private static final String VISIBLE_TO_PACKAGE_SHA_256_CERT_PROPERTY = "sha256Cert";

    /** Property that holds the required permissions to access the schema. */
    private static final String PERMISSION_PROPERTY = "permission";

    /**
     * Property that holds the package to use as a target package for public visibility. Empty if
     * the schema is not publicly visible.
     */
    private static final String PUBLICLY_VISIBLE_TARGET_PACKAGE = "publiclyVisibleTargetPackage";

    /** Property that holds the package sha of the target package for public visibility. */
    private static final String PUBLICLY_VISIBLE_TARGET_PACKAGE_SHA_256_CERT =
            "publiclyVisibleTargetPackageSha256Cert";

    // The initial schema version, one VisibilityConfig contains all visibility information for
    // whole package.
    public static final int SCHEMA_VERSION_DOC_PER_PACKAGE = 0;

    // One VisibilityConfig contains visibility information for a single schema.
    public static final int SCHEMA_VERSION_DOC_PER_SCHEMA = 1;

    // One VisibilityConfig contains visibility information for a single schema. The permission
    // visibility information is stored in a document property VisibilityPermissionConfig of the
    // outer doc.
    public static final int SCHEMA_VERSION_NESTED_PERMISSION_SCHEMA = 2;

    public static final int SCHEMA_VERSION_LATEST = SCHEMA_VERSION_NESTED_PERMISSION_SCHEMA;

    /**
     * Schema for the VisibilityStore's documents.
     *
     * <p>NOTE: If you update this, also update {@link #SCHEMA_VERSION_LATEST}.
     */
    public static final AppSearchSchema VISIBILITY_DOCUMENT_SCHEMA =
            new AppSearchSchema.Builder(VISIBILITY_DOCUMENT_SCHEMA_TYPE)
                    .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                            NOT_DISPLAYED_BY_SYSTEM_PROPERTY)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            VISIBLE_TO_PACKAGE_IDENTIFIER_PROPERTY)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .build())
                    .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder(
                            VISIBLE_TO_PACKAGE_SHA_256_CERT_PROPERTY)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .build())
                    .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                            PERMISSION_PROPERTY, VisibilityPermissionConfig.SCHEMA_TYPE)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .build())
                    .build();

    /**
     * Schema for the VisibilityStore's public acl overlays.
     *
     * <p>NOTE: If you need to add an additional visibility property, add another overlay type.
     */
    public static final AppSearchSchema PUBLIC_ACL_OVERLAY_SCHEMA =
            new AppSearchSchema.Builder(PUBLIC_ACL_OVERLAY_SCHEMA_TYPE)
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PUBLICLY_VISIBLE_TARGET_PACKAGE)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder(
                            PUBLICLY_VISIBLE_TARGET_PACKAGE_SHA_256_CERT)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    .build();

    /**
     * Constructs a {@link VisibilityConfig} from two {@link GenericDocument}s.
     *
     * <p>This constructor is still needed until we don't treat Visibility related documents as
     * {@link GenericDocument}s internally.
     *
     * @param visibilityDocument a {@link GenericDocument} holding all visibility properties
     *                           other than publiclyVisibleTargetPackage.
     * @param publicAclDocument a {@link GenericDocument} holding the
     *                          publiclyVisibleTargetPackage visibility property
     */
    @NonNull
    public static VisibilityConfig createVisibilityConfig(
            @NonNull GenericDocument visibilityDocument,
            @Nullable GenericDocument publicAclDocument) {
        Objects.requireNonNull(visibilityDocument);

        String schemaType = visibilityDocument.getId();
        boolean isNotDisplayedBySystem = visibilityDocument.getPropertyBoolean(
                NOT_DISPLAYED_BY_SYSTEM_PROPERTY);

        List<String> visibleToPackageNames = Arrays.asList(visibilityDocument
                .getPropertyStringArray(VISIBLE_TO_PACKAGE_IDENTIFIER_PROPERTY));
        byte[][] visibleToPackageShaCerts = visibilityDocument
                .getPropertyBytesArray(VISIBLE_TO_PACKAGE_SHA_256_CERT_PROPERTY);

        List<VisibilityPermissionConfig> visibilityPermissionConfigs = new ArrayList<>();
        GenericDocument[] permissionDocs =
                visibilityDocument.getPropertyDocumentArray(PERMISSION_PROPERTY);
        if (permissionDocs != null) {
            for (int i = 0; i < permissionDocs.length; ++i) {
                visibilityPermissionConfigs.add(
                        new VisibilityPermissionConfig.Builder(permissionDocs[i]).build());
            }
        }

        String publiclyVisibleTargetPackage = null;
        byte[] publiclyVisibleTargetPackageSha = null;
        if (publicAclDocument != null) {
            publiclyVisibleTargetPackage =
                    publicAclDocument.getPropertyString(PUBLICLY_VISIBLE_TARGET_PACKAGE);
            publiclyVisibleTargetPackageSha = publicAclDocument.getPropertyBytes(
                    PUBLICLY_VISIBLE_TARGET_PACKAGE_SHA_256_CERT);
        }

        return new VisibilityConfig(
                schemaType,
                isNotDisplayedBySystem,
                visibleToPackageNames,
                visibleToPackageShaCerts,
                visibilityPermissionConfigs,
                publiclyVisibleTargetPackage,
                publiclyVisibleTargetPackageSha);
    }

    /**
     * Returns the {@link GenericDocument} for the visibility schema.
     *
     * <p> The name of the schema which this VisibilityConfig describes will be used as the id.
     */
    @NonNull
    public static GenericDocument createVisibilityDocument(
            @NonNull VisibilityConfig config) {
        GenericDocument.Builder<?> builder = new GenericDocument.Builder<>(
                VISIBILITY_DOCUMENT_NAMESPACE,
                config.getSchemaType(),
                VISIBILITY_DOCUMENT_SCHEMA_TYPE);
        builder.setPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY,
                config.isNotDisplayedBySystem());
        List<PackageIdentifier> visibleToPackages = config.getVisibleToPackages();
        String[] visibleToPackageNames = new String[visibleToPackages.size()];
        byte[][] visibleToPackageSha256Certs = new byte[visibleToPackages.size()][32];
        for (int i = 0; i < visibleToPackages.size(); i++) {
            visibleToPackageNames[i] = visibleToPackages.get(i).getPackageName();
            visibleToPackageSha256Certs[i] = visibleToPackages.get(i).getSha256Certificate();
        }
        builder.setPropertyString(VISIBLE_TO_PACKAGE_IDENTIFIER_PROPERTY, visibleToPackageNames);
        builder.setPropertyBytes(VISIBLE_TO_PACKAGE_SHA_256_CERT_PROPERTY,
                visibleToPackageSha256Certs);

        // Generate an array of GenericDocument for VisibilityPermissionConfig.
        Set<Set<Integer>> visibleToPermissions = config.getVisibleToPermissions();
        if (!visibleToPermissions.isEmpty()) {
            GenericDocument[] permissionGenericDocs =
                    new GenericDocument[visibleToPermissions.size()];
            int i = 0;
            for (Set<Integer> allRequiredPermissions : visibleToPermissions) {
                VisibilityPermissionConfig permissionDocument = new VisibilityPermissionConfig
                        .Builder(allRequiredPermissions).build();
                permissionGenericDocs[i++] = permissionDocument.toGenericDocument();
            }
            builder.setPropertyDocument(PERMISSION_PROPERTY, permissionGenericDocs);
        }

        // The creationTimestamp doesn't matter for Visibility documents.
        // But to make tests pass, we set it 0 so two GenericDocuments generated from
        // the same VisibilityConfig can be same.
        builder.setCreationTimestampMillis(0L);

        return builder.build();
    }

    /**
     * Returns the {@link GenericDocument} for the public acl overlay schema if it is provided,
     * null otherwise.
     */
    @Nullable
    public static GenericDocument createPublicAclOverlay(
            @NonNull VisibilityConfig config) {
        PackageIdentifier publiclyVisibleTargetPackage = config.getPubliclyVisibleTargetPackage();
        if (publiclyVisibleTargetPackage == null) {
            return null;
        }
        GenericDocument.Builder<?> builder = new GenericDocument.Builder<>(
                PUBLIC_ACL_OVERLAY_NAMESPACE,
                config.getSchemaType(),
                PUBLIC_ACL_OVERLAY_SCHEMA_TYPE);

        builder.setPropertyString(PUBLICLY_VISIBLE_TARGET_PACKAGE,
                publiclyVisibleTargetPackage.getPackageName());
        builder.setPropertyBytes(PUBLICLY_VISIBLE_TARGET_PACKAGE_SHA_256_CERT,
                publiclyVisibleTargetPackage.getSha256Certificate());

        // The creationTimestamp doesn't matter for Visibility documents.
        // But to make tests pass, we set it 0 so two GenericDocuments generated from
        // the same VisibilityConfig can be same.
        builder.setCreationTimestampMillis(0L);

        return builder.build();
    }


}
