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
import java.util.Collections;
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
     * The Schema type for the Android V visibility setting overlay documents, that allow for
     * additional visibility settings.
     */
    public static final String ANDROID_V_OVERLAY_SCHEMA_TYPE = "AndroidVOverlayType";
    /** Namespace of documents that contain Android V visibility setting overlay documents */
    public static final String ANDROID_V_OVERLAY_NAMESPACE = "androidVOverlay";

    /**
     * The schema type that combine properties from {@link #VISIBILITY_DOCUMENT_SCHEMA} and
     * public acl.
     *
     * <p> This will be used in {@link #ANDROID_V_OVERLAY_SCHEMA_TYPE}
     */
    public static final String VISIBLE_TO_CONFIG_SCHEMA_TYPE = "VisibleToConfigType";
    /**
     * Property that holds the {@link #VISIBLE_TO_CONFIG_SCHEMA_TYPE} property, as part of
     * the {@link #ANDROID_V_OVERLAY_SCHEMA_TYPE}.
     */
    public static final String VISIBLE_TO_CONFIG_PROPERTY = "visibleToConfigProperty";

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

    // The version number of schema saved in Android V overlay database.
    public static final int ANDROID_V_OVERLAY_SCHEMA_VERSION = 0;

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

    /**  Schema for the VisibilityStore's Android V visibility setting overlay. */
    public static final AppSearchSchema ANDROID_V_OVERLAY_SCHEMA =
            new AppSearchSchema.Builder(ANDROID_V_OVERLAY_SCHEMA_TYPE)
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PUBLICLY_VISIBLE_TARGET_PACKAGE)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder(
                            PUBLICLY_VISIBLE_TARGET_PACKAGE_SHA_256_CERT)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                            VISIBLE_TO_CONFIG_PROPERTY,
                            VISIBLE_TO_CONFIG_SCHEMA_TYPE)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .build())
                    .build();

    /**
     * Schema for the visible to config.
     *
     * <p> This is document property of {@link #ANDROID_V_OVERLAY_SCHEMA}
     */
    public static final AppSearchSchema VISIBLE_TO_CONFIG_SCHEMA =
            new AppSearchSchema.Builder(VISIBLE_TO_CONFIG_SCHEMA_TYPE)
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
     * @param visibilityDocument       a {@link GenericDocument} holding visibility properties
     *                                 in {@link #VISIBILITY_DOCUMENT_SCHEMA}
     * @param androidVOverlayDocument  a {@link GenericDocument} holding visibility properties
     *                                 in {@link #ANDROID_V_OVERLAY_SCHEMA}
     */
    @NonNull
    public static VisibilityConfig createVisibilityConfig(
            @NonNull GenericDocument visibilityDocument,
            @Nullable GenericDocument androidVOverlayDocument) {
        Objects.requireNonNull(visibilityDocument);

        String schemaType = visibilityDocument.getId();
        boolean isNotDisplayedBySystem = visibilityDocument.getPropertyBoolean(
                NOT_DISPLAYED_BY_SYSTEM_PROPERTY);
        List<String> visibleToPackageNames = Arrays.asList(visibilityDocument
                .getPropertyStringArray(VISIBLE_TO_PACKAGE_IDENTIFIER_PROPERTY));
        byte[][] visibleToPackageShaCerts = visibilityDocument
                .getPropertyBytesArray(VISIBLE_TO_PACKAGE_SHA_256_CERT_PROPERTY);
        List<VisibilityPermissionConfig> visibilityPermissionConfigs =
                readVisibleToPermissionFromDocument(visibilityDocument);

        String publiclyVisibleTargetPackage = null;
        byte[] publiclyVisibleTargetPackageSha = null;
        List<VisibilityConfig> visibleToConfigs = new ArrayList<>();
        if (androidVOverlayDocument != null) {
            publiclyVisibleTargetPackage =
                    androidVOverlayDocument.getPropertyString(PUBLICLY_VISIBLE_TARGET_PACKAGE);
            publiclyVisibleTargetPackageSha = androidVOverlayDocument.getPropertyBytes(
                    PUBLICLY_VISIBLE_TARGET_PACKAGE_SHA_256_CERT);

            GenericDocument[] visibleToConfigDocuments = androidVOverlayDocument
                    .getPropertyDocumentArray(VISIBLE_TO_CONFIG_PROPERTY);
            for (int i = 0; i < visibleToConfigDocuments.length; i++) {
                visibleToConfigs.add(convertVisibleToConfigDocumentToVisibilityConfig(
                        visibleToConfigDocuments[i]));
            }
        }

        return new VisibilityConfig(
                schemaType,
                isNotDisplayedBySystem,
                visibleToPackageNames,
                visibleToPackageShaCerts,
                visibilityPermissionConfigs,
                publiclyVisibleTargetPackage,
                publiclyVisibleTargetPackageSha,
                visibleToConfigs);
    }

    /**
     * Converts a generic document of {@link #VISIBLE_TO_CONFIG_SCHEMA_TYPE} to a
     *  {@link VisibilityConfig}.
     */
    private static VisibilityConfig convertVisibleToConfigDocumentToVisibilityConfig(
            @NonNull GenericDocument visibleToConfigDocument) {
        if (!visibleToConfigDocument.getSchemaType().equals(VISIBLE_TO_CONFIG_SCHEMA_TYPE)) {
            throw new IllegalArgumentException(
                    "The schema of the given document isn't visibleToConfig. :"
                    + visibleToConfigDocument.getSchemaType());
        }
        boolean isNotDisplayedBySystem = visibleToConfigDocument.getPropertyBoolean(
                NOT_DISPLAYED_BY_SYSTEM_PROPERTY);
        List<String> visibleToPackageNames = Arrays.asList(visibleToConfigDocument
                .getPropertyStringArray(VISIBLE_TO_PACKAGE_IDENTIFIER_PROPERTY));
        byte[][] visibleToPackageShaCerts = visibleToConfigDocument
                .getPropertyBytesArray(VISIBLE_TO_PACKAGE_SHA_256_CERT_PROPERTY);
        List<VisibilityPermissionConfig> visibilityPermissionConfigs =
                readVisibleToPermissionFromDocument(visibleToConfigDocument);
        String publiclyVisibleTargetPackage = visibleToConfigDocument
                .getPropertyString(PUBLICLY_VISIBLE_TARGET_PACKAGE);
        byte[] publiclyVisibleTargetPackageSha = visibleToConfigDocument.getPropertyBytes(
                PUBLICLY_VISIBLE_TARGET_PACKAGE_SHA_256_CERT);
        return new VisibilityConfig(
                /*schemaType=*/ null, // we don't need schemaType in nest nested visibleToConfig
                isNotDisplayedBySystem,
                visibleToPackageNames,
                visibleToPackageShaCerts,
                visibilityPermissionConfigs,
                publiclyVisibleTargetPackage,
                publiclyVisibleTargetPackageSha,
                /*visibleToConfigs=*/ Collections.emptyList());
    }

    /**
     * Returns the {@link GenericDocument} for the visibility schema.
     *
     * <p> The name of the schema which this VisibilityConfig describes will be used as the id.
     */
    @NonNull
    public static GenericDocument createVisibilityDocument(
            @NonNull VisibilityConfig config) {
        // We are using schemaType to be the document Id when store to Icing.
        String documentId = config.getSchemaType();
        if (documentId == null) {
            // This is the nested VisibilityConfig, we could skip to set the document id.
            documentId = "";
        }
        GenericDocument.Builder<?> builder = new GenericDocument.Builder<>(
                VISIBILITY_DOCUMENT_NAMESPACE,
                documentId,
                VISIBILITY_DOCUMENT_SCHEMA_TYPE);
        builder.setPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY,
                config.isNotDisplayedBySystem());
        setVisibleToPackageProperty(builder, config);
        setVisibleToPermissionProperty(builder, config);

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
    public static GenericDocument createAndroidVOverlay(
            @NonNull VisibilityConfig visibilityConfig) {
        PackageIdentifier publiclyVisibleTargetPackage =
                visibilityConfig.getPubliclyVisibleTargetPackage();
        Set<VisibilityConfig> visibleToConfigs = visibilityConfig.getVisibleToConfigs();
        if (publiclyVisibleTargetPackage == null && visibleToConfigs.isEmpty()) {
            // This config doesn't contains any Android V overlay settings
            return null;
        }

        GenericDocument.Builder<?> androidVOverlaybuilder = new GenericDocument.Builder<>(
                ANDROID_V_OVERLAY_NAMESPACE,
                visibilityConfig.getSchemaType(),
                ANDROID_V_OVERLAY_SCHEMA_TYPE);
        setPublicAclProperty(androidVOverlaybuilder, visibilityConfig);

        GenericDocument[] visibleToConfigDocs = new GenericDocument[visibleToConfigs.size()];
        int i = 0;
        for (VisibilityConfig visibleToConfig : visibleToConfigs) {
            GenericDocument.Builder<?> visibleToConfigBuilder =
                    new GenericDocument.Builder<>(/*namespace=*/"", /*id=*/ "",
                            VISIBLE_TO_CONFIG_SCHEMA_TYPE);

            visibleToConfigBuilder.setPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY,
                    visibleToConfig.isNotDisplayedBySystem());
            setVisibleToPackageProperty(visibleToConfigBuilder, visibleToConfig);
            setVisibleToPermissionProperty(visibleToConfigBuilder, visibleToConfig);
            setPublicAclProperty(visibleToConfigBuilder, visibleToConfig);

            // The creationTimestamp doesn't matter for Visibility documents.
            // But to make tests pass, we set it 0 so two GenericDocuments generated from
            // the same VisibilityConfig can be same.
            visibleToConfigBuilder.setCreationTimestampMillis(0L);

            visibleToConfigDocs[i++] = visibleToConfigBuilder.build();
        }
        androidVOverlaybuilder.setPropertyDocument(VISIBLE_TO_CONFIG_PROPERTY, visibleToConfigDocs);

        // The creationTimestamp doesn't matter for Visibility documents.
        // But to make tests pass, we set it 0 so two GenericDocuments generated from
        // the same VisibilityConfig can be same.
        androidVOverlaybuilder.setCreationTimestampMillis(0L);

        return androidVOverlaybuilder.build();
    }

    /**
     * Sets the visible to packages property to the given builder.
     * @param builder  The {@link GenericDocument.Builder} of
     * {@link #VISIBILITY_DOCUMENT_SCHEMA_TYPE} or {@link #VISIBLE_TO_CONFIG_SCHEMA_TYPE}
     */
    private static void setVisibleToPackageProperty(@NonNull GenericDocument.Builder<?> builder,
            @NonNull VisibilityConfig config) {
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
    }

    /**
     * Sets the visible to permission property to the given builder.
     * @param builder  The {@link GenericDocument.Builder} of
     * {@link #VISIBILITY_DOCUMENT_SCHEMA_TYPE} or {@link #VISIBLE_TO_CONFIG_SCHEMA_TYPE}
     */
    private static void setVisibleToPermissionProperty(@NonNull GenericDocument.Builder<?> builder,
            @NonNull VisibilityConfig config) {
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
    }

    /**
     * Sets the public acl property to the given builder.
     * @param builder  The {@link GenericDocument.Builder} of
     * {@link #VISIBILITY_DOCUMENT_SCHEMA_TYPE} or {@link #VISIBLE_TO_CONFIG_SCHEMA_TYPE}
     */
    private static void setPublicAclProperty(@NonNull GenericDocument.Builder<?> builder,
            @NonNull VisibilityConfig config) {
        PackageIdentifier publiclyVisibleTargetPackage = config.getPubliclyVisibleTargetPackage();
        if (publiclyVisibleTargetPackage == null) {
            // This config doesn't contains public acl.
            return;
        }
        builder.setPropertyString(PUBLICLY_VISIBLE_TARGET_PACKAGE,
                publiclyVisibleTargetPackage.getPackageName());
        builder.setPropertyBytes(PUBLICLY_VISIBLE_TARGET_PACKAGE_SHA_256_CERT,
                publiclyVisibleTargetPackage.getSha256Certificate());
    }

    /**
     * Reads the {@link VisibilityConfig} from the {@link GenericDocument}.
     *
     * @param visibilityDocument  The {@link GenericDocument} to of
     * {@link #VISIBILITY_DOCUMENT_SCHEMA_TYPE} or {@link #VISIBLE_TO_CONFIG_SCHEMA_TYPE}
     */
    @NonNull
    private static List<VisibilityPermissionConfig> readVisibleToPermissionFromDocument(
            @NonNull GenericDocument visibilityDocument) {
        String schemaType = visibilityDocument.getSchemaType();
        if (!schemaType.equals(VISIBILITY_DOCUMENT_SCHEMA_TYPE)
                && !schemaType.equals(VISIBLE_TO_CONFIG_SCHEMA_TYPE)) {
            throw new IllegalArgumentException(
                    "The given document doesn't contains permission document property: "
                            + schemaType);
        }
        List<VisibilityPermissionConfig> visibilityPermissionConfigs = new ArrayList<>();
        GenericDocument[] permissionDocs =
                visibilityDocument.getPropertyDocumentArray(PERMISSION_PROPERTY);
        if (permissionDocs != null) {
            for (int i = 0; i < permissionDocs.length; ++i) {
                visibilityPermissionConfigs.add(
                        new VisibilityPermissionConfig.Builder(permissionDocs[i]).build());
            }
        }
        return visibilityPermissionConfigs;
    }
}
