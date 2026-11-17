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

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.VisibilityPermissionConfig;
import androidx.appsearch.flags.Flags;
import androidx.collection.ArraySet;

import com.google.android.appsearch.proto.AndroidVOverlayProto;
import com.google.android.appsearch.proto.PackageIdentifierProto;
import com.google.android.appsearch.proto.VisibilityConfigProto;
import com.google.android.appsearch.proto.VisibleToPackageWithTagsProto;
import com.google.android.appsearch.proto.VisibleToPermissionProto;
import com.google.android.icing.protobuf.ByteString;
import com.google.android.icing.protobuf.InvalidProtocolBufferException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utilities for working with {@link VisibilityChecker} and {@link VisibilityStore}.
 */
@HideInPlatform
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
     * The name of a placeholder package which is stored, in a visible to config, as a package
     * that has access whenever the config has packages that are restricted by attribution tag.
     *
     * <p>A version of AppSearch that predates packages restricted by attribution tag ignores them
     * when it reads a visible to config. Without the placeholder, such a version would be left
     * with only the other requirements of the config, e.g. its permissions, and would grant
     * access to every caller that meets them. No package can have this name, so with the
     * placeholder such a version grants access to no one through the config instead.
     *
     * <p>The placeholder only exists in storage: it is removed when a visible to config is read,
     * so a config that is returned to a caller never has it.
     *
     * <p>A version of AppSearch that predates packages restricted by attribution tag does return
     * the placeholder to callers, as a package. A caller can set it back, which stores a config
     * with the placeholder but without the packages it was stored for. Such a config is skipped
     * when it is read if the placeholder is its only package: no caller can match the config as
     * it is stored, and without the placeholder its other requirements would be on their own.
     *
     * <p>Only this placeholder is removed. A placeholder that a later version of AppSearch
     * stores for another setting is not known to this version, which reads it as a package that
     * no caller can match.
     */
    @VisibleForTesting
    static final String PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME = "VS#PackagesWithTags";

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
    @OptIn(markerClass = androidx.appsearch.app.ExperimentalAppSearchApi.class)
    public static @NonNull InternalVisibilityConfig createInternalVisibilityConfig(
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
            builder.setWriterUid(androidVOverlayProto.getWriterUid());
            List<VisibilityConfigProto> visibleToConfigProtoList =
                    androidVOverlayProto.getVisibleToConfigsList();
            for (int i = 0; i < visibleToConfigProtoList.size(); i++) {
                VisibilityConfigProto visibleToConfigProto = visibleToConfigProtoList.get(i);
                if (Flags.enableSchemaVisibleToPackageWithTags()) {
                    VisibilityConfigProto storedVisibleToConfigProto = visibleToConfigProto;
                    visibleToConfigProto =
                            removePackagesWithTagsPlaceholder(storedVisibleToConfigProto);
                    if (hasPackagesWithAndWithoutAttributionTags(visibleToConfigProto)
                            || hasNoPackageAttributionTags(visibleToConfigProto)
                            || hasOnlyPackagesWithTagsPlaceholder(storedVisibleToConfigProto)) {
                        // None of these configs can be created through the API. A config cannot
                        // have both packages and packages that are restricted by attribution tag.
                        // No caller can match a package that has no attribution tags, so such a
                        // package is ignored when the config is converted; if that would leave
                        // the config without any package that is restricted by attribution tag,
                        // its other requirements (e.g. permissions) would be on their own and
                        // grant access to more callers. The same goes for a config whose only
                        // package is the placeholder, see
                        // PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME. Skipping the whole config
                        // can only narrow access.
                        continue;
                    }
                }
                builder.addVisibleToConfig(convertVisibilityConfigFromProto(visibleToConfigProto));
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
    private static @NonNull SchemaVisibilityConfig createVisibilityConfig(
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

    /**
     * Convert {@link VisibilityConfigProto} into {@link SchemaVisibilityConfig}.
     *
     * <p>The returned config never grants access to more callers than the proto does. When the
     * packages of the proto that are restricted by attribution tag cannot be converted, the
     * returned config is not left with only the other requirements of the proto: it either
     * keeps the placeholder package, which no caller can match, or has no requirement at all.
     */
    @OptIn(markerClass = androidx.appsearch.app.ExperimentalAppSearchApi.class)
    public static @NonNull SchemaVisibilityConfig convertVisibilityConfigFromProto(
            @NonNull VisibilityConfigProto proto) {
        if (Flags.enableSchemaVisibleToPackageWithTags()) {
            if (hasPackageAttributionTags(proto)) {
                // The placeholder stands for the packages that are restricted by attribution tag,
                // see PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME. It is only removed here when
                // one of them can be converted. Otherwise it stays as a package that no caller
                // can match, so that the other requirements of the proto are not left on their
                // own.
                proto = removePackagesWithTagsPlaceholder(proto);
            } else if (hasNoPackageAttributionTags(proto)
                    && proto.getVisibleToPackagesCount() == 0) {
                // No caller can match a package that has no attribution tags, and nothing else in
                // the proto says which package has access. A config without any requirement
                // grants access to no one, whereas the other requirements of the proto on their
                // own would grant access to more callers.
                return new SchemaVisibilityConfig.Builder().build();
            }
        }
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

        if (Flags.enableSchemaVisibleToPackageWithTags()) {
            // A config cannot have both packages and packages that are restricted by attribution
            // tag, so the latter are ignored when there are any of the former. Ignoring them can
            // only narrow access.
            List<VisibleToPackageWithTagsProto> visibleToPackageWithTagsProtoList =
                    proto.getVisibleToPackagesCount() == 0
                            ? proto.getVisibleToPackagesWithTagsList() : Collections.emptyList();
            for (int i = 0; i < visibleToPackageWithTagsProtoList.size(); i++) {
                VisibleToPackageWithTagsProto visibleToPackageWithTags =
                        visibleToPackageWithTagsProtoList.get(i);
                if (visibleToPackageWithTags.getAttributionTagsCount() == 0) {
                    // No caller can match a package that has no attribution tags, so this entry
                    // grants access to no one.
                    continue;
                }
                builder.setAllowedPackageWithTags(
                        convertPackageIdentifierFromProto(
                                visibleToPackageWithTags.getPackageIdentifier()),
                        new ArraySet<>(visibleToPackageWithTags.getAttributionTagsList()));
            }
        }

        return builder.build();
    }

    /**  Convert {@link SchemaVisibilityConfig} into {@link VisibilityConfigProto}.     */
    @OptIn(markerClass = androidx.appsearch.app.ExperimentalAppSearchApi.class)
    public static @NonNull VisibilityConfigProto convertSchemaVisibilityConfigToProto(
            @NonNull SchemaVisibilityConfig schemaVisibilityConfig) {
        VisibilityConfigProto.Builder builder = VisibilityConfigProto.newBuilder();

        List<PackageIdentifier> visibleToPackages = schemaVisibilityConfig.getAllowedPackages();
        for (int i = 0; i < visibleToPackages.size(); i++) {
            PackageIdentifier visibleToPackage = visibleToPackages.get(i);
            builder.addVisibleToPackages(convertPackageIdentifierToProto(visibleToPackage));
        }
        // Intentionally not gated by Flags.enableSchemaVisibleToPackageWithTags(): dropping
        // packages restricted by attribution tag while keeping other requirements (e.g.
        // permissions) would grant access to more callers. When the flag is off, the read path
        // keeps the placeholder and ignores the tags, so no caller gets access.
        if (!schemaVisibilityConfig.getAllowedPackagesWithTags().isEmpty()) {
            // See PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME.
            builder.addVisibleToPackages(
                    PackageIdentifierProto.newBuilder()
                            .setPackageName(PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME)
                            .setPackageSha256Cert(ByteString.copyFrom(new byte[32])));
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

        addPackagesWithTagsToProto(builder, schemaVisibilityConfig);

        return builder.build();
    }

    /**
     * Returns the {@link GenericDocument} for the visibility schema.
     *
     * @param config the configuration to populate into the document
     */
    public static @NonNull GenericDocument createVisibilityDocument(
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
    @OptIn(markerClass = androidx.appsearch.app.ExperimentalAppSearchApi.class)
    public static @Nullable GenericDocument createAndroidVOverlay(
            @NonNull InternalVisibilityConfig internalVisibilityConfig) {
        if (!doesConfigContainAndroidVOverlay(internalVisibilityConfig)) {
            // This config doesn't contain any Android V overlay settings
            return null;
        }
        PackageIdentifier publiclyVisibleTargetPackage =
                internalVisibilityConfig.getVisibilityConfig().getPubliclyVisibleTargetPackage();
        Set<SchemaVisibilityConfig> visibleToConfigs =
                internalVisibilityConfig.getVisibleToConfigs();

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
        if (internalVisibilityConfig.getWriterUid() != InternalVisibilityConfig.INVALID_UID) {
            androidVOverlayProtoBuilder.setWriterUid(internalVisibilityConfig.getWriterUid());
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

    /**
     * Whether the given {@link InternalVisibilityConfig} contains Android V overlay settings.
     *
     * <p>Android V overlay {@link #ANDROID_V_OVERLAY_SCHEMA} contains public acl, visible to
     * config, and writer UID.
     */
    @OptIn(markerClass = androidx.appsearch.app.ExperimentalAppSearchApi.class)
    public static boolean doesConfigContainAndroidVOverlay(
            @Nullable InternalVisibilityConfig config) {
        return config != null
                && (config.getVisibilityConfig().getPubliclyVisibleTargetPackage() != null
                || !config.getVisibleToConfigs().isEmpty()
                || config.getWriterUid() != InternalVisibilityConfig.INVALID_UID);
    }

    private static @NonNull PackageIdentifierProto convertPackageIdentifierToProto(
            @NonNull PackageIdentifier packageIdentifier) {
        // Always populate Tag 2 (primary certificate) for backward compatibility with
        // older readers and single-cert packages.
        PackageIdentifierProto.Builder builder =
                PackageIdentifierProto.newBuilder()
                        .setPackageName(packageIdentifier.getPackageName())
                        .setPackageSha256Cert(
                                ByteString.copyFrom(packageIdentifier.getSha256Certificate()));
        if (Flags.enableAppsIndexerPwaMultiCert()) {
            List<byte[]> certs = packageIdentifier.getMultiSignerSha256Certificates();
            if (!certs.isEmpty()) {
                for (int i = 0; i < certs.size(); i++) {
                    builder.addPackageSha256Certs(ByteString.copyFrom(certs.get(i)));
                }
            }
        }
        return builder.build();
    }

    private static @NonNull PackageIdentifier convertPackageIdentifierFromProto(
            @NonNull PackageIdentifierProto packageIdentifierProto) {
        // Prefer Tag 3 (repeated certs) when present for multi-signer packages;
        // otherwise fall back to Tag 2 for single-cert and legacy documents.
        if (Flags.enableAppsIndexerPwaMultiCert()
                && packageIdentifierProto.getPackageSha256CertsCount() > 0) {
            List<ByteString> certsList = packageIdentifierProto.getPackageSha256CertsList();
            List<byte[]> certs = new ArrayList<>(certsList.size());
            for (int i = 0; i < certsList.size(); i++) {
                certs.add(certsList.get(i).toByteArray());
            }
            return new PackageIdentifier(packageIdentifierProto.getPackageName(), certs);
        }
        return new PackageIdentifier(
                packageIdentifierProto.getPackageName(),
                packageIdentifierProto.getPackageSha256Cert().toByteArray());
    }

    /**
     * Adds the packages of the given config that are restricted by attribution tag, and their
     * tags, to the given proto.
     */
    @OptIn(markerClass = androidx.appsearch.app.ExperimentalAppSearchApi.class)
    private static void addPackagesWithTagsToProto(
            VisibilityConfigProto.@NonNull Builder builder,
            @NonNull SchemaVisibilityConfig schemaVisibilityConfig) {
        for (Map.Entry<PackageIdentifier, Set<String>> entry :
                schemaVisibilityConfig.getAllowedPackagesWithTags().entrySet()) {
            builder.addVisibleToPackagesWithTags(
                    VisibleToPackageWithTagsProto.newBuilder()
                            .setPackageIdentifier(convertPackageIdentifierToProto(entry.getKey()))
                            .addAllAttributionTags(entry.getValue()));
        }
    }

    /**
     * Returns the given proto without the placeholder package that is stored with packages that
     * are restricted by attribution tag, see
     * {@link #PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME}.
     *
     * <p>The placeholder is removed whether or not the proto has packages that are restricted by
     * attribution tag. The caller is responsible for not using a proto that grants access to
     * more callers without the placeholder, see {@link #hasOnlyPackagesWithTagsPlaceholder}.
     */
    private static @NonNull VisibilityConfigProto removePackagesWithTagsPlaceholder(
            @NonNull VisibilityConfigProto proto) {
        boolean hasPlaceholder = false;
        for (int i = 0; i < proto.getVisibleToPackagesCount(); i++) {
            if (isPackagesWithTagsPlaceholder(proto.getVisibleToPackages(i))) {
                hasPlaceholder = true;
                break;
            }
        }
        if (!hasPlaceholder) {
            return proto;
        }
        VisibilityConfigProto.Builder builder = proto.toBuilder().clearVisibleToPackages();
        for (int i = 0; i < proto.getVisibleToPackagesCount(); i++) {
            PackageIdentifierProto visibleToPackage = proto.getVisibleToPackages(i);
            if (!isPackagesWithTagsPlaceholder(visibleToPackage)) {
                builder.addVisibleToPackages(visibleToPackage);
            }
        }
        return builder.build();
    }

    /** Returns whether the given package is the placeholder package. */
    private static boolean isPackagesWithTagsPlaceholder(
            @NonNull PackageIdentifierProto packageIdentifierProto) {
        return PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME.equals(
                packageIdentifierProto.getPackageName());
    }

    /**
     * Returns whether the placeholder package is the only package of the given proto, and the
     * proto has no packages that are restricted by attribution tag, see
     * {@link #PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME}.
     *
     * <p>Such a proto grants access to no one as it is stored, and would grant access to every
     * caller that meets its other requirements without the placeholder.
     */
    private static boolean hasOnlyPackagesWithTagsPlaceholder(
            @NonNull VisibilityConfigProto proto) {
        if (proto.getVisibleToPackagesCount() == 0
                || proto.getVisibleToPackagesWithTagsCount() > 0) {
            return false;
        }
        for (int i = 0; i < proto.getVisibleToPackagesCount(); i++) {
            if (!isPackagesWithTagsPlaceholder(proto.getVisibleToPackages(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the given proto has both packages and packages that are restricted by
     * attribution tag.
     */
    private static boolean hasPackagesWithAndWithoutAttributionTags(
            @NonNull VisibilityConfigProto proto) {
        return proto.getVisibleToPackagesCount() > 0
                && proto.getVisibleToPackagesWithTagsCount() > 0;
    }

    /**
     * Returns whether the given proto has a package that is restricted by attribution tag, and
     * has at least one attribution tag.
     */
    private static boolean hasPackageAttributionTags(@NonNull VisibilityConfigProto proto) {
        for (int i = 0; i < proto.getVisibleToPackagesWithTagsCount(); i++) {
            if (proto.getVisibleToPackagesWithTags(i).getAttributionTagsCount() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given proto has packages that are restricted by attribution tag, but
     * none of them has an attribution tag.
     */
    private static boolean hasNoPackageAttributionTags(@NonNull VisibilityConfigProto proto) {
        return proto.getVisibleToPackagesWithTagsCount() > 0 && !hasPackageAttributionTags(proto);
    }

    private VisibilityToDocumentConverter() {}
}
