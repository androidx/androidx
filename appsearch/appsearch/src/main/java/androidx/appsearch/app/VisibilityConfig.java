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

package androidx.appsearch.app;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.VisibilityConfigCreator;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A class to hold a all necessary Visibility information corresponding to the same schema. This
 * pattern allows for easier association of these documents.
 *
 * <p> This does not correspond to any schema, the properties held in this class are kept in two
 * separate schemas, VisibilityConfig and PublicAclOverlay.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SafeParcelable.Class(creator = "VisibilityConfigCreator")
public final class VisibilityConfig extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<VisibilityConfig> CREATOR =
            new VisibilityConfigCreator();

    /**
     * The Schema type for documents that hold AppSearch's metadata, such as visibility settings.
     */
    public static final String VISIBILITY_DOCUMENT_SCHEMA_TYPE = "VisibilityType";
    /** Namespace of documents that contain visibility settings */
    public static final String VISIBILITY_DOCUMENT_NAMESPACE = "";

    /**
     * The Schema type for the visibility overlay documents, that allow for additional visibility
     * settings.
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

        VisibilityPermissionConfig[] requiredPermissions = null;
        GenericDocument[] permissionDocs =
                visibilityDocument.getPropertyDocumentArray(PERMISSION_PROPERTY);
        if (permissionDocs != null) {
            requiredPermissions = new VisibilityPermissionConfig[permissionDocs.length];
            for (int i = 0; i < permissionDocs.length; ++i) {
                requiredPermissions[i] = new VisibilityPermissionConfig.Builder(
                        permissionDocs[i]).build();
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
        return new VisibilityConfig(schemaType, isNotDisplayedBySystem, visibleToPackageNames,
                visibleToPackageShaCerts, requiredPermissions, publiclyVisibleTargetPackage,
                publiclyVisibleTargetPackageSha);
    }

    /** Build the List of {@link VisibilityConfig}s from visibility settings. */
    @NonNull
    public static List<VisibilityConfig> toVisibilityConfigs(
            @NonNull SetSchemaRequest setSchemaRequest) {
        Set<AppSearchSchema> searchSchemas = setSchemaRequest.getSchemas();
        Map<String, PackageIdentifier> publiclyVisibleSchemas =
                setSchemaRequest.getPubliclyVisibleSchemas();
        Set<String> schemasNotDisplayedBySystem = setSchemaRequest.getSchemasNotDisplayedBySystem();
        Map<String, Set<PackageIdentifier>> schemasVisibleToPackages =
                setSchemaRequest.getSchemasVisibleToPackages();
        Map<String, Set<Set<Integer>>> schemasVisibleToPermissions =
                setSchemaRequest.getRequiredPermissionsForSchemaTypeVisibility();
        List<VisibilityConfig> visibilityConfigs = new ArrayList<>(searchSchemas.size());

        for (AppSearchSchema searchSchema : searchSchemas) {
            String schemaType = searchSchema.getSchemaType();
            Builder visibilityConfigBuilder = new Builder(/*schemaType=*/ schemaType);

            visibilityConfigBuilder.setNotDisplayedBySystem(
                    schemasNotDisplayedBySystem.contains(schemaType));

            Set<PackageIdentifier> visibleToPackages = schemasVisibleToPackages.get(schemaType);
            if (visibleToPackages != null) {
                visibilityConfigBuilder.addVisibleToPackages(visibleToPackages);
            }

            Set<Set<Integer>> visibleToPermissions = schemasVisibleToPermissions.get(schemaType);
            if (visibleToPermissions != null) {
                visibilityConfigBuilder.setVisibleToPermissions(visibleToPermissions);
            }

            PackageIdentifier targetPackage = publiclyVisibleSchemas.get(schemaType);
            if (targetPackage != null) {
                visibilityConfigBuilder.setPubliclyVisibleTargetPackage(targetPackage);
            }
            visibilityConfigs.add(visibilityConfigBuilder.build());
        }
        return visibilityConfigs;
    }

    @NonNull
    @Field(id = 1, getter = "getSchemaType")
    private final String mSchemaType;

    @Field(id = 2, getter = "isNotDisplayedBySystem")
    private final boolean mIsNotDisplayedBySystem;

    @NonNull
    @Field(id = 3)
    final List<String> mVisibleToPackageNames;

    @NonNull
    @Field(id = 4)
    final byte[][] mVisibleToPackageSha256Certs;

    @Nullable
    @Field(id = 5)
    final VisibilityPermissionConfig[] mRequiredPermissions;

    @Nullable
    @Field(id = 6)
    final String mPubliclyVisibleTargetPackage;

    @Nullable
    @Field(id = 7)
    final byte[] mPubliclyVisibleTargetPackageSha256Cert;

    @Nullable
    private Integer mHashCode;

    @Constructor
    VisibilityConfig(
            @Param(id = 1) @NonNull String schemaType,
            @Param(id = 2) boolean isNotDisplayedBySystem,
            @Param(id = 3) @NonNull List<String> visibleToPackageNames,
            @Param(id = 4) @NonNull byte[][] visibleToPackageSha256Certs,
            @Param(id = 5) @Nullable VisibilityPermissionConfig[] permissionsRequired,
            @Param(id = 6) @Nullable String publiclyVisibleTargetPackage,
            @Param(id = 7) @Nullable byte[] publiclyVisibleTargetPackageSha256Cert) {
        mSchemaType = Objects.requireNonNull(schemaType);
        mIsNotDisplayedBySystem = isNotDisplayedBySystem;
        mVisibleToPackageNames = Objects.requireNonNull(visibleToPackageNames);
        mVisibleToPackageSha256Certs = Objects.requireNonNull(visibleToPackageSha256Certs);
        mRequiredPermissions = permissionsRequired;
        mPubliclyVisibleTargetPackage = publiclyVisibleTargetPackage;
        mPubliclyVisibleTargetPackageSha256Cert = publiclyVisibleTargetPackageSha256Cert;
    }

    /**
     * Gets the schemaType for this VisibilityConfig.
     *
     * <p>This is being used as the document id when we convert a {@link VisibilityConfig} to a
     * {@link GenericDocument}.
     */
    @NonNull
    public String getSchemaType() {
        return mSchemaType;
    }

    /** Returns whether this schema is visible to the system. */
    public boolean isNotDisplayedBySystem() {
        return mIsNotDisplayedBySystem;
    }

     /** Returns a list of {@link PackageIdentifier}s of packages that can access this schema. */
    @NonNull
    public List<PackageIdentifier> getVisibleToPackages() {
        List<PackageIdentifier> visibleToPackage = new ArrayList<>(mVisibleToPackageNames.size());
        for (int i = 0; i < mVisibleToPackageNames.size(); i++) {
            visibleToPackage.add(new PackageIdentifier(mVisibleToPackageNames.get(i),
                    mVisibleToPackageSha256Certs[i]));
        }
        return visibleToPackage;
    }

    /**
     * Returns an array of Integers representing Android Permissions as defined in
     * {@link SetSchemaRequest.AppSearchSupportedPermission} that the caller must hold to access the
     * schema this {@link VisibilityConfig} represents.
     */
    @NonNull
    public Set<Set<Integer>> getVisibleToPermissions() {
        if (mRequiredPermissions == null) {
            return Collections.emptySet();
        }
        Set<Set<Integer>> visibleToPermissions = new ArraySet<>(mRequiredPermissions.length);
        for (VisibilityPermissionConfig permissionConfig : mRequiredPermissions) {
            Set<Integer> requiredPermissions = permissionConfig.getAllRequiredPermissions();
            if (requiredPermissions != null) {
                visibleToPermissions.add(requiredPermissions);
            }
        }
        return visibleToPermissions;
    }

    /**
     * Returns the {@link PackageIdentifier} of the package that will be used as the target package
     * in a call to {@link android.content.pm.PackageManager#canPackageQuery} to determine which
     * packages can access this publicly visible schema. Returns null if the schema is not publicly
     * visible.
     */
    @Nullable
    public PackageIdentifier getPubliclyVisibleTargetPackage() {
        if (mPubliclyVisibleTargetPackage == null
                || mPubliclyVisibleTargetPackageSha256Cert == null) {
            return null;
        }
        return new PackageIdentifier(mPubliclyVisibleTargetPackage,
                mPubliclyVisibleTargetPackageSha256Cert);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        VisibilityConfigCreator.writeToParcel(this, dest, flags);
    }

    /**
     * Returns the {@link GenericDocument} for the visibility schema.
     *
     * <p> The name of the schema which this VisibilityConfig describes will be used as the id.
     */
    @NonNull
    public GenericDocument createVisibilityDocument() {
        GenericDocument.Builder<?> builder = new GenericDocument.Builder<>(
                VISIBILITY_DOCUMENT_NAMESPACE, mSchemaType, VISIBILITY_DOCUMENT_SCHEMA_TYPE);
        builder.setPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY, mIsNotDisplayedBySystem);
        builder.setPropertyString(VISIBLE_TO_PACKAGE_IDENTIFIER_PROPERTY,
                mVisibleToPackageNames.toArray(new String[0]));
        builder.setPropertyBytes(VISIBLE_TO_PACKAGE_SHA_256_CERT_PROPERTY,
                mVisibleToPackageSha256Certs);

        // Generate an array of GenericDocument for VisibilityPermissionConfig.
        if (mRequiredPermissions != null) {
            GenericDocument[] permissionGenericDocs =
                    new GenericDocument[mRequiredPermissions.length];
            for (int i = 0; i < mRequiredPermissions.length; i++) {
                VisibilityPermissionConfig permissionDocument = mRequiredPermissions[i];
                permissionGenericDocs[i] = permissionDocument.toGenericDocument();
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
    public GenericDocument createPublicAclOverlay() {
        if (mPubliclyVisibleTargetPackage == null) {
            return null;
        }
        GenericDocument.Builder<?> builder = new GenericDocument.Builder<>(
                PUBLIC_ACL_OVERLAY_NAMESPACE, mSchemaType, PUBLIC_ACL_OVERLAY_SCHEMA_TYPE);

        builder.setPropertyString(PUBLICLY_VISIBLE_TARGET_PACKAGE, mPubliclyVisibleTargetPackage);
        builder.setPropertyBytes(PUBLICLY_VISIBLE_TARGET_PACKAGE_SHA_256_CERT,
                mPubliclyVisibleTargetPackageSha256Cert);

        // The creationTimestamp doesn't matter for Visibility documents.
        // But to make tests pass, we set it 0 so two GenericDocuments generated from
        // the same VisibilityConfig can be same.
        builder.setCreationTimestampMillis(0L);

        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof VisibilityConfig)) return false;
        VisibilityConfig that = (VisibilityConfig) o;
        return Objects.equals(mSchemaType, that.mSchemaType)
                && mIsNotDisplayedBySystem == that.mIsNotDisplayedBySystem
                && Objects.equals(mVisibleToPackageNames, that.mVisibleToPackageNames)
                && Arrays.deepEquals(mVisibleToPackageSha256Certs,
                that.mVisibleToPackageSha256Certs)
                && Objects.deepEquals(mRequiredPermissions, that.mRequiredPermissions)
                && Objects.equals(mPubliclyVisibleTargetPackage, that.mPubliclyVisibleTargetPackage)
                && Arrays.equals(mPubliclyVisibleTargetPackageSha256Cert,
                that.mPubliclyVisibleTargetPackageSha256Cert);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = Objects.hash(
                    mSchemaType,
                    mIsNotDisplayedBySystem,
                    mVisibleToPackageNames,
                    Arrays.deepHashCode(mVisibleToPackageSha256Certs),
                    Arrays.hashCode(mRequiredPermissions),
                    mPubliclyVisibleTargetPackage,
                    Arrays.hashCode(mPubliclyVisibleTargetPackageSha256Cert));
        }
        return mHashCode;
    }

    public static final class Builder {
        private String mSchemaType;
        private boolean mIsNotDisplayedBySystem;
        private List<String> mVisibleToPackageNames = new ArrayList<>();
        private List<byte[]> mVisibleToPackageSha256Certs = new ArrayList<>();
        private VisibilityPermissionConfig[] mRequiredPermissions;
        private String mPubliclyVisibleTargetPackage;
        private byte[] mPubliclyVisibleTargetPackageSha256Cert;

        /**
         * Creates a {@link Builder} for a {@link VisibilityConfig}.
         *
         * @param schemaType The SchemaType of the {@link AppSearchSchema} that this {@link
         *     VisibilityConfig} represents. The package and database prefix will be added in
         *     server side. We are using prefixed schema type to be the final id of this {@link
         *     VisibilityConfig}. This will be used as as an AppSearch id.
         * @see GenericDocument#getId
         */
        public Builder(@NonNull String schemaType) {
            mSchemaType = Objects.requireNonNull(schemaType);
        }

        /** Creates a {@link Builder} from an existing {@link VisibilityConfig} */
        public Builder(@NonNull VisibilityConfig visibilityConfig) {
            Objects.requireNonNull(visibilityConfig);

            mSchemaType = visibilityConfig.mSchemaType;
            mIsNotDisplayedBySystem = visibilityConfig.mIsNotDisplayedBySystem;
            mRequiredPermissions = visibilityConfig.mRequiredPermissions;
            mVisibleToPackageNames = visibilityConfig.mVisibleToPackageNames;
            mVisibleToPackageSha256Certs =
                    Arrays.asList(visibilityConfig.mVisibleToPackageSha256Certs);
            mPubliclyVisibleTargetPackage = visibilityConfig.mPubliclyVisibleTargetPackage;
            mPubliclyVisibleTargetPackageSha256Cert =
                    visibilityConfig.mPubliclyVisibleTargetPackageSha256Cert;
        }

        /** Sets schemaType, which will be as the id when converting to {@link GenericDocument}. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setSchemaType(@NonNull String schemaType) {
            mSchemaType = Objects.requireNonNull(schemaType);
            return this;
        }

        /** Sets whether this schema has opted out of platform surfacing. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setNotDisplayedBySystem(boolean notDisplayedBySystem) {
            mIsNotDisplayedBySystem = notDisplayedBySystem;
            return this;
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addVisibleToPackages(@NonNull Set<PackageIdentifier> packageIdentifiers) {
            Objects.requireNonNull(packageIdentifiers);
            for (PackageIdentifier packageIdentifier: packageIdentifiers) {
                addVisibleToPackage(packageIdentifier);
            }
            return this;
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addVisibleToPackage(@NonNull PackageIdentifier packageIdentifier) {
            Objects.requireNonNull(packageIdentifier);
            mVisibleToPackageNames.add(packageIdentifier.getPackageName());
            mVisibleToPackageSha256Certs.add(packageIdentifier.getSha256Certificate());
            return this;
        }

        /** Clears the list of packages which have access to this schema. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder clearVisibleToPackages() {
            mVisibleToPackageNames = new ArrayList<>();
            mVisibleToPackageSha256Certs = new ArrayList<>();
            return this;
        }

        /**
         * Sets required permission sets for a package needs to hold to the schema this {@link
         * VisibilityConfig} represents.
         *
         * <p>The querier could have access if they holds ALL required permissions of ANY of the
         * individual value sets.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setVisibleToPermissions(@NonNull Set<Set<Integer>> visibleToPermissions) {
            Objects.requireNonNull(visibleToPermissions);
            mRequiredPermissions =
                    new VisibilityPermissionConfig[visibleToPermissions.size()];
            int i = 0;
            for (Set<Integer> allRequiredPermissions : visibleToPermissions) {
                mRequiredPermissions[i++] =
                        new VisibilityPermissionConfig.Builder(allRequiredPermissions).build();
            }
            return this;
        }

        /**
         * Specify that this schema should be publicly available, to the same packages that have
         * visibility to the package passed as a parameter. This visibility is determined by the
         * result of {@link android.content.pm.PackageManager#canPackageQuery}.
         *
         * <p> It is possible for the packageIdentifier parameter to be different from the
         * package performing the indexing. This might happen in the case of an on-device indexer
         * processing information about various packages. The visibility will be the same
         * regardless of which package indexes the document, as the visibility is based on the
         * packageIdentifier parameter.
         *
         * @param packageIdentifier the {@link PackageIdentifier} of the package that will be used
         *                          as the target package in a call to {@link
         *                          android.content.pm.PackageManager#canPackageQuery} to determine
         *                          which packages can access this publicly visible schema.
         */
        @NonNull
        public Builder setPubliclyVisibleTargetPackage(
                @NonNull PackageIdentifier packageIdentifier) {
            Objects.requireNonNull(packageIdentifier);

            mPubliclyVisibleTargetPackage = packageIdentifier.getPackageName();
            mPubliclyVisibleTargetPackageSha256Cert = packageIdentifier.getSha256Certificate();
            return this;
        }

        /** Build a {@link VisibilityConfig} */
        @NonNull
        public VisibilityConfig build() {
            return new VisibilityConfig(mSchemaType, mIsNotDisplayedBySystem,
                    mVisibleToPackageNames,
                    mVisibleToPackageSha256Certs.toArray(new byte[0][]),
                    mRequiredPermissions,
                    mPubliclyVisibleTargetPackage, mPubliclyVisibleTargetPackageSha256Cert);
        }
    }
}
