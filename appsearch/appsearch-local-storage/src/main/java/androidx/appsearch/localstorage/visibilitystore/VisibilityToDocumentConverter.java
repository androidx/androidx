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
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.VisibilityPermissionConfig;
import androidx.collection.ArraySet;

import com.google.android.appsearch.proto.AndroidVOverlayProto;
import com.google.android.appsearch.proto.PackageIdentifierProto;
import com.google.android.appsearch.proto.VisibilityConfigProto;
import com.google.android.appsearch.proto.VisibleToPermissionProto;
import com.google.android.icing.protobuf.ByteString;
import com.google.android.icing.protobuf.InvalidProtocolBufferException;

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

    // The initial schema version, the overlay schema contains public acl and visible to config
    // properties.
    public static final int OVERLAY_SCHEMA_VERSION_PUBLIC_ACL_VISIBLE_TO_CONFIG = 0;

    // The overlay schema only contains a proto property contains all visibility setting.
    public static final int OVERLAY_SCHEMA_VERSION_ALL_IN_PROTO = 1;

    // The version number of schema saved in Android V overlay database.
    public static final int ANDROID_V_OVERLAY_SCHEMA_VERSION_LATEST =
            OVERLAY_SCHEMA_VERSION_ALL_IN_PROTO;

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
     * The Deprecated schemas and properties that we need to remove from visibility database.
     * TODO(b/321326441) remove this method when we no longer to migrate devices in this state.
     */
    static final AppSearchSchema DEPRECATED_PUBLIC_ACL_OVERLAY_SCHEMA =
            new AppSearchSchema.Builder("PublicAclOverlayType")
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            "publiclyVisibleTargetPackage")
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder(
                            "publiclyVisibleTargetPackageSha256Cert")
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    .build();

    /**
     * Constructs a {@link InternalVisibilityConfig} from two {@link GenericDocument}s.
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
    public static InternalVisibilityConfig createInternalVisibilityConfig(
            @NonNull GenericDocument visibilityDocument,
            @Nullable GenericDocument androidVOverlayDocument) {
        Objects.requireNonNull(visibilityDocument);

        // Parse visibility proto if required
        AndroidVOverlayProto androidVOverlayProto = null;
        if (androidVOverlayDocument != null) {
            try {
                byte[] androidVOverlayProtoBytes = androidVOverlayDocument.getPropertyBytes(
                        VISIBILITY_PROTO_SERIALIZE_PROPERTY);
                if (androidVOverlayProtoBytes != null) {
                    androidVOverlayProto = AndroidVOverlayProto.parseFrom(
                            androidVOverlayProtoBytes);
                }
            } catch (InvalidProtocolBufferException e) {
                Log.e(TAG, "Get an invalid android V visibility overlay proto.", e);
            }
        }

        // Handle all visibility settings other than visibleToConfigs
        SchemaVisibilityConfig schemaVisibilityConfig = createVisibilityConfig(
                visibilityDocument, androidVOverlayProto);

        // Handle visibleToConfigs
        String schemaType = visibilityDocument.getId();
        InternalVisibilityConfig.Builder builder = new InternalVisibilityConfig.Builder(schemaType)
                .setNotDisplayedBySystem(visibilityDocument
                        .getPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY))
                .setVisibilityConfig(schemaVisibilityConfig);
        if (androidVOverlayProto != null) {
            List<VisibilityConfigProto> visibleToConfigProtoList =
                    androidVOverlayProto.getVisibleToConfigsList();
            for (int i = 0; i < visibleToConfigProtoList.size(); i++) {
                SchemaVisibilityConfig visibleToConfig =
                        convertVisibilityConfigFromProto(visibleToConfigProtoList.get(i));
                builder.addVisibleToConfig(visibleToConfig);
            }
        }

        return builder.build();
    }

    /**
     * Constructs a {@link SchemaVisibilityConfig} from a {@link GenericDocument} containing legacy
     * visibility settings, and an {@link AndroidVOverlayProto} containing extended visibility
     * settings.
     *
     * <p>This constructor is still needed until we don't treat Visibility related documents as
     * {@link GenericDocument}s internally.
     *
     * @param visibilityDocument   a {@link GenericDocument} holding all visibility properties
     *                             other than publiclyVisibleTargetPackage.
     * @param androidVOverlayProto the proto containing post-V visibility settings
     */
    @NonNull
    private static SchemaVisibilityConfig createVisibilityConfig(
            @NonNull GenericDocument visibilityDocument,
            @Nullable AndroidVOverlayProto androidVOverlayProto) {
        Objects.requireNonNull(visibilityDocument);

        // Pre-V visibility settings come from visibilityDocument
        SchemaVisibilityConfig.Builder builder = new SchemaVisibilityConfig.Builder();

        String[] visibleToPackageNames =
                visibilityDocument.getPropertyStringArray(VISIBLE_TO_PACKAGE_IDENTIFIER_PROPERTY);
        byte[][] visibleToPackageShaCerts =
                visibilityDocument.getPropertyBytesArray(VISIBLE_TO_PACKAGE_SHA_256_CERT_PROPERTY);
        if (visibleToPackageNames != null && visibleToPackageShaCerts != null) {
            for (int i = 0; i < visibleToPackageNames.length; i++) {
                builder.addAllowedPackage(
                        new PackageIdentifier(
                                visibleToPackageNames[i], visibleToPackageShaCerts[i]));
            }
        }

        GenericDocument[] visibleToPermissionDocs =
                visibilityDocument.getPropertyDocumentArray(PERMISSION_PROPERTY);
        if (visibleToPermissionDocs != null) {
            for (int i = 0; i < visibleToPermissionDocs.length; ++i) {
                long[] visibleToPermissionLongs = visibleToPermissionDocs[i].getPropertyLongArray(
                        VisibilityPermissionConfig.ALL_REQUIRED_PERMISSIONS_PROPERTY);
                if (visibleToPermissionLongs != null) {
                    Set<Integer> allRequiredPermissions =
                            new ArraySet<>(visibleToPermissionLongs.length);
                    for (int j = 0; j < visibleToPermissionLongs.length; j++) {
                        allRequiredPermissions.add((int) visibleToPermissionLongs[j]);
                    }
                    builder.addRequiredPermissions(allRequiredPermissions);
                }
            }
        }

        // Post-V visibility settings come from androidVOverlayProto
        if (androidVOverlayProto != null) {
            SchemaVisibilityConfig androidVOverlayConfig =
                    convertVisibilityConfigFromProto(
                            androidVOverlayProto.getVisibilityConfig());
            builder.setPubliclyVisibleTargetPackage(
                    androidVOverlayConfig.getPubliclyVisibleTargetPackage());
        }

        return builder.build();
    }

    /**  Convert {@link VisibilityConfigProto} into {@link SchemaVisibilityConfig}.     */
    @NonNull
    public static SchemaVisibilityConfig convertVisibilityConfigFromProto(
            @NonNull VisibilityConfigProto proto) {
        SchemaVisibilityConfig.Builder builder = new SchemaVisibilityConfig.Builder();

        List<PackageIdentifierProto> visibleToPackageProtoList = proto.getVisibleToPackagesList();
        for (int i = 0; i < visibleToPackageProtoList.size(); i++) {
            PackageIdentifierProto visibleToPackage = proto.getVisibleToPackages(i);
            builder.addAllowedPackage(convertPackageIdentifierFromProto(visibleToPackage));
        }

        List<VisibleToPermissionProto> visibleToPermissionProtoList =
                proto.getVisibleToPermissionsList();
        for (int i = 0; i < visibleToPermissionProtoList.size(); i++) {
            VisibleToPermissionProto visibleToPermissionProto = visibleToPermissionProtoList.get(i);
            Set<Integer> visibleToPermissions =
                    new ArraySet<>(visibleToPermissionProto.getPermissionsList());
            builder.addRequiredPermissions(visibleToPermissions);
        }

        if (proto.hasPubliclyVisibleTargetPackage()) {
            PackageIdentifierProto publiclyVisibleTargetPackage =
                    proto.getPubliclyVisibleTargetPackage();
            builder.setPubliclyVisibleTargetPackage(
                    convertPackageIdentifierFromProto(publiclyVisibleTargetPackage));
        }

        return builder.build();
    }

    /**  Convert {@link SchemaVisibilityConfig} into {@link VisibilityConfigProto}.     */
    @NonNull
    public static VisibilityConfigProto convertSchemaVisibilityConfigToProto(
            @NonNull SchemaVisibilityConfig schemaVisibilityConfig) {
        VisibilityConfigProto.Builder builder = VisibilityConfigProto.newBuilder();

        List<PackageIdentifier> visibleToPackages = schemaVisibilityConfig.getAllowedPackages();
        for (int i = 0; i < visibleToPackages.size(); i++) {
            PackageIdentifier visibleToPackage = visibleToPackages.get(i);
            builder.addVisibleToPackages(convertPackageIdentifierToProto(visibleToPackage));
        }

        Set<Set<Integer>> visibleToPermissions = schemaVisibilityConfig.getRequiredPermissions();
        if (!visibleToPermissions.isEmpty()) {
            for (Set<Integer> allRequiredPermissions : visibleToPermissions) {
                builder.addVisibleToPermissions(
                        VisibleToPermissionProto.newBuilder()
                                .addAllPermissions(allRequiredPermissions));
            }
        }

        PackageIdentifier publicAclPackage =
                schemaVisibilityConfig.getPubliclyVisibleTargetPackage();
        if (publicAclPackage != null) {
            builder.setPubliclyVisibleTargetPackage(
                    convertPackageIdentifierToProto(publicAclPackage));
        }

        return builder.build();
    }

    /**
     * Returns the {@link GenericDocument} for the visibility schema.
     *
     * @param config the configuration to populate into the document
     */
    @NonNull
    public static GenericDocument createVisibilityDocument(
            @NonNull InternalVisibilityConfig config) {
        GenericDocument.Builder<?> builder = new GenericDocument.Builder<>(
                VISIBILITY_DOCUMENT_NAMESPACE,
                config.getSchemaType(), // We are using the prefixedSchemaType to be the id
                VISIBILITY_DOCUMENT_SCHEMA_TYPE);
        builder.setPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY,
                config.isNotDisplayedBySystem());
        SchemaVisibilityConfig schemaVisibilityConfig = config.getVisibilityConfig();
        List<PackageIdentifier> visibleToPackages = schemaVisibilityConfig.getAllowedPackages();
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
        Set<Set<Integer>> visibleToPermissions = schemaVisibilityConfig.getRequiredPermissions();
        if (!visibleToPermissions.isEmpty()) {
            GenericDocument[] permissionGenericDocs =
                    new GenericDocument[visibleToPermissions.size()];
            int i = 0;
            for (Set<Integer> allRequiredPermissions : visibleToPermissions) {
                VisibilityPermissionConfig permissionDocument =
                        new VisibilityPermissionConfig(allRequiredPermissions);
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
     * Returns the {@link GenericDocument} for the Android V overlay schema if it is provided,
     * null otherwise.
     */
    @Nullable
    public static GenericDocument createAndroidVOverlay(
            @NonNull InternalVisibilityConfig internalVisibilityConfig) {
        PackageIdentifier publiclyVisibleTargetPackage =
                internalVisibilityConfig.getVisibilityConfig().getPubliclyVisibleTargetPackage();
        Set<SchemaVisibilityConfig> visibleToConfigs =
                internalVisibilityConfig.getVisibleToConfigs();
        if (publiclyVisibleTargetPackage == null && visibleToConfigs.isEmpty()) {
            // This config doesn't contains any Android V overlay settings
            return null;
        }

        VisibilityConfigProto.Builder visibilityConfigProtoBuilder =
                VisibilityConfigProto.newBuilder();
        // Set publicAcl
        if (publiclyVisibleTargetPackage != null) {
            visibilityConfigProtoBuilder.setPubliclyVisibleTargetPackage(
                    convertPackageIdentifierToProto(publiclyVisibleTargetPackage));
        }

        // Set visibleToConfigs
        AndroidVOverlayProto.Builder androidVOverlayProtoBuilder =
                AndroidVOverlayProto.newBuilder().setVisibilityConfig(visibilityConfigProtoBuilder);
        if (!visibleToConfigs.isEmpty()) {
            for (SchemaVisibilityConfig visibleToConfig : visibleToConfigs) {
                VisibilityConfigProto visibleToConfigProto =
                        convertSchemaVisibilityConfigToProto(visibleToConfig);
                androidVOverlayProtoBuilder.addVisibleToConfigs(visibleToConfigProto);
            }
        }

        GenericDocument.Builder<?> androidVOverlayBuilder = new GenericDocument.Builder<>(
                ANDROID_V_OVERLAY_NAMESPACE,
                internalVisibilityConfig.getSchemaType(),
                ANDROID_V_OVERLAY_SCHEMA_TYPE)
                .setPropertyBytes(
                        VISIBILITY_PROTO_SERIALIZE_PROPERTY,
                        androidVOverlayProtoBuilder.build().toByteArray());

        // The creationTimestamp doesn't matter for Visibility documents.
        // But to make tests pass, we set it 0 so two GenericDocuments generated from
        // the same VisibilityConfig can be same.
        androidVOverlayBuilder.setCreationTimestampMillis(0L);

        return androidVOverlayBuilder.build();
    }

    @NonNull
    private static PackageIdentifierProto convertPackageIdentifierToProto(
            @NonNull PackageIdentifier packageIdentifier) {
        return PackageIdentifierProto.newBuilder()
                .setPackageName(packageIdentifier.getPackageName())
                .setPackageSha256Cert(ByteString.copyFrom(packageIdentifier.getSha256Certificate()))
                .build();
    }

    @NonNull
    private static PackageIdentifier convertPackageIdentifierFromProto(
            @NonNull PackageIdentifierProto packageIdentifierProto) {
        return new PackageIdentifier(
                packageIdentifierProto.getPackageName(),
                packageIdentifierProto.getPackageSha256Cert().toByteArray());
    }

    private VisibilityToDocumentConverter() {}
}
