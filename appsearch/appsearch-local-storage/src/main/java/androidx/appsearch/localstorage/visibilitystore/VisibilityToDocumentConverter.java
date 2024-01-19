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

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.VisibilityConfig;
import androidx.appsearch.app.VisibilityPermissionConfig;
import androidx.collection.ArraySet;

import com.google.android.appsearch.proto.AndroidVOverlayProto;
import com.google.android.appsearch.proto.PackageIdentifierProto;
import com.google.android.appsearch.proto.VisibilityConfigProto;
import com.google.android.appsearch.proto.VisibleToPermissionProto;
import com.google.android.icing.protobuf.ByteString;
import com.google.android.icing.protobuf.InvalidProtocolBufferException;

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
    private static final String TAG = "AppSearchVisibilityToDo";
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
    /** Property that holds the serialized {@link AndroidVOverlayProto}. */
    public static final String VISIBILITY_PROTO_SERIALIZE_PROPERTY =
            "visibilityProtoSerializeProperty";

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
                    .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder(
                            VISIBILITY_PROTO_SERIALIZE_PROPERTY)
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
            try {
                AndroidVOverlayProto overlayProto = AndroidVOverlayProto.parseFrom(
                        androidVOverlayDocument.getPropertyBytes(
                                VISIBILITY_PROTO_SERIALIZE_PROPERTY));

                if (overlayProto.hasVisibilityConfig()) {
                    PackageIdentifierProto publicAclPackageProto = overlayProto
                            .getVisibilityConfig().getPubliclyVisibleTargetPackage();
                    publiclyVisibleTargetPackage = publicAclPackageProto.getPackageName();
                    publiclyVisibleTargetPackageSha = publicAclPackageProto
                            .getPackageSha256Cert().toByteArray();
                }

                visibleToConfigs = convertProtoToVisibleToConfig(
                        overlayProto.getVisibleToConfigsList());
            } catch (InvalidProtocolBufferException e) {
                Log.e(TAG, "Get an invalid android V visibility overlay proto.", e);
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

    /** Convert a {@link VisibilityConfigProto} to a list of nested {@link VisibilityConfig}.  */
    private static List<VisibilityConfig> convertProtoToVisibleToConfig(
            @NonNull List<VisibilityConfigProto> protoList) {

        List<VisibilityConfig> visibleToConfigs = new ArrayList<>(protoList.size());
        for (VisibilityConfigProto proto : protoList) {
            boolean isNotDisplayedBySystem = proto.getNotPlatformSurfaceable();

            List<PackageIdentifierProto> visibleToPackageProto = proto.getVisibleToPackagesList();
            List<String> visibleToPackageNames = new ArrayList<>(visibleToPackageProto.size());
            byte[][] visibleToPackageShaCerts = new byte[visibleToPackageProto.size()][32];
            for (int i = 0; i < visibleToPackageProto.size(); i++) {
                visibleToPackageNames.add(visibleToPackageProto.get(i).getPackageName());
                visibleToPackageShaCerts[i] = visibleToPackageProto.get(i)
                        .getPackageSha256Cert().toByteArray();
            }

            List<VisibleToPermissionProto> visibleToPermissionProto =
                    proto.getVisibleToPermissionsList();
            List<VisibilityPermissionConfig> visibilityPermissionConfigs =
                    new ArrayList<>(visibleToPermissionProto.size());
            for (int i = 0; i < visibleToPermissionProto.size(); i++) {
                visibilityPermissionConfigs.add(new VisibilityPermissionConfig.Builder(
                        new ArraySet<>(
                                visibleToPermissionProto.get(i).getPermissionsList())).build());
            }
            String publiclyVisibleTargetPackage = null;
            byte[] publiclyVisibleTargetPackageSha = null;
            if (proto.hasPubliclyVisibleTargetPackage()) {
                PackageIdentifierProto publicAclPackageProto =
                        proto.getPubliclyVisibleTargetPackage();
                publiclyVisibleTargetPackage = publicAclPackageProto.getPackageName();
                publiclyVisibleTargetPackageSha =
                        publicAclPackageProto.getPackageSha256Cert().toByteArray();
            }

            visibleToConfigs.add(new VisibilityConfig(
                    /*schemaType=*/ null, // we don't need schemaType in nest nested visibleToConfig
                    isNotDisplayedBySystem,
                    visibleToPackageNames,
                    visibleToPackageShaCerts,
                    visibilityPermissionConfigs,
                    publiclyVisibleTargetPackage,
                    publiclyVisibleTargetPackageSha,
                    /*visibleToConfigs=*/ Collections.emptyList()));
        }
        return visibleToConfigs;
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
                ANDROID_V_OVERLAY_SCHEMA_TYPE)
                .setPropertyBytes(VISIBILITY_PROTO_SERIALIZE_PROPERTY,
                        convertVisibilityOverlayToProto(visibilityConfig).toByteArray());

        // The creationTimestamp doesn't matter for Visibility documents.
        // But to make tests pass, we set it 0 so two GenericDocuments generated from
        // the same VisibilityConfig can be same.
        androidVOverlaybuilder.setCreationTimestampMillis(0L);

        return androidVOverlaybuilder.build();
    }

    /** Converts a whole {@link VisibilityConfig} into {@link AndroidVOverlayProto}     */
    private static AndroidVOverlayProto convertVisibilityOverlayToProto(
            @NonNull VisibilityConfig config) {
        AndroidVOverlayProto.Builder builder = AndroidVOverlayProto.newBuilder();
        PackageIdentifier publicAclPackage = config.getPubliclyVisibleTargetPackage();
        if (publicAclPackage != null) {
            VisibilityConfigProto.Builder visibilityConfigProtoBuilder =
                    VisibilityConfigProto.newBuilder()
                            .setPubliclyVisibleTargetPackage(
                                    convertPackageIdentifierToProto(publicAclPackage));
            builder.setVisibilityConfig(visibilityConfigProtoBuilder.build());
        }
        Set<VisibilityConfig> visibleToConfigs = config.getVisibleToConfigs();
        if (!visibleToConfigs.isEmpty()) {
            builder.addAllVisibleToConfigs(convertVisibilityConfigToProto(visibleToConfigs));
        }
        return builder.build();
    }

    /**
     * Converts a set of nested {@link VisibilityConfig} into a list of
     * {@link VisibilityConfigProto}
     */
    private static List<VisibilityConfigProto> convertVisibilityConfigToProto(
            @NonNull Set<VisibilityConfig> visibleToConfigs) {
        List<VisibilityConfigProto> protoList = new ArrayList<>(visibleToConfigs.size());
        for (VisibilityConfig visibleToConfig : visibleToConfigs) {
            VisibilityConfigProto.Builder builder = VisibilityConfigProto.newBuilder()
                    .setNotPlatformSurfaceable(visibleToConfig.isNotDisplayedBySystem());

            List<PackageIdentifier> visibleToPackages = visibleToConfig.getVisibleToPackages();
            for (int i = 0; i < visibleToPackages.size(); i++) {
                builder.addVisibleToPackages(
                        convertPackageIdentifierToProto(visibleToPackages.get(i)));
            }

            Set<Set<Integer>> visibleToPermissions = visibleToConfig.getVisibleToPermissions();
            if (!visibleToPermissions.isEmpty()) {
                for (Set<Integer> allRequiredPermissions : visibleToPermissions) {
                    builder.addVisibleToPermissions(
                            VisibleToPermissionProto.newBuilder()
                                    .addAllPermissions(allRequiredPermissions));
                }
            }

            PackageIdentifier publicAclPackage = visibleToConfig.getPubliclyVisibleTargetPackage();
            if (publicAclPackage != null) {
                builder.setPubliclyVisibleTargetPackage(
                        convertPackageIdentifierToProto(publicAclPackage));
            }
            protoList.add(builder.build());
        }
        return protoList;
    }

    /** Converts a {@link PackageIdentifier} to {@link PackageIdentifierProto}     */
    private static PackageIdentifierProto convertPackageIdentifierToProto(
            @NonNull PackageIdentifier packageIdentifier) {
        return PackageIdentifierProto.newBuilder()
                .setPackageName(packageIdentifier.getPackageName())
                .setPackageSha256Cert(ByteString.copyFrom(
                        packageIdentifier.getSha256Certificate())).build();
    }

    /**
     * Sets the visible to packages property to the given builder.
     * @param builder  The {@link GenericDocument.Builder} of
     * {@link #VISIBILITY_DOCUMENT_SCHEMA_TYPE}.
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
     * {@link #VISIBILITY_DOCUMENT_SCHEMA_TYPE}.
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
     * Reads the {@link VisibilityConfig} from the {@link GenericDocument}.
     *
     * @param visibilityDocument  The {@link GenericDocument} to of
     * {@link #VISIBILITY_DOCUMENT_SCHEMA_TYPE}.
     */
    @NonNull
    private static List<VisibilityPermissionConfig> readVisibleToPermissionFromDocument(
            @NonNull GenericDocument visibilityDocument) {
        String schemaType = visibilityDocument.getSchemaType();
        if (!schemaType.equals(VISIBILITY_DOCUMENT_SCHEMA_TYPE)) {
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
