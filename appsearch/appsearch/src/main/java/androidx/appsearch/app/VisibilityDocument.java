/*
 * Copyright (C) 2021 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.VisibilityDocumentCreator;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Holds the visibility settings that apply to a schema type.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SafeParcelable.Class(creator = "VisibilityDocumentCreator")
public final class VisibilityDocument extends AbstractSafeParcelable {
    @NonNull
    public static final VisibilityDocumentCreator CREATOR = new VisibilityDocumentCreator();

    /**
     * The Schema type for documents that hold AppSearch's metadata, such as visibility settings.
     */
    public static final String SCHEMA_TYPE = "VisibilityType";
    /** Namespace of documents that contain visibility settings */
    public static final String NAMESPACE = "";

    /**
     * Property that holds the list of platform-hidden schemas, as part of the visibility settings.
     */
    private static final String NOT_DISPLAYED_BY_SYSTEM_PROPERTY = "notPlatformSurfaceable";

    /** Property that holds the package name that can access a schema. */
    private static final String PACKAGE_NAME_PROPERTY = "packageName";

    /** Property that holds the SHA 256 certificate of the app that can access a schema. */
    private static final String SHA_256_CERT_PROPERTY = "sha256Cert";

    /** Property that holds the required permissions to access the schema. */
    private static final String PERMISSION_PROPERTY = "permission";

    // The initial schema version, one VisibilityDocument contains all visibility information for
    // whole package.
    public static final int SCHEMA_VERSION_DOC_PER_PACKAGE = 0;

    // One VisibilityDocument contains visibility information for a single schema.
    public static final int SCHEMA_VERSION_DOC_PER_SCHEMA = 1;

    // One VisibilityDocument contains visibility information for a single schema.
    public static final int SCHEMA_VERSION_NESTED_PERMISSION_SCHEMA = 2;

    public static final int SCHEMA_VERSION_LATEST = SCHEMA_VERSION_NESTED_PERMISSION_SCHEMA;

    /**
     * Schema for the VisibilityStore's documents.
     *
     * <p>NOTE: If you update this, also update {@link #SCHEMA_VERSION_LATEST}.
     */
    public static final AppSearchSchema
            SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
            .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                    NOT_DISPLAYED_BY_SYSTEM_PROPERTY)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                    .build())
            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(PACKAGE_NAME_PROPERTY)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .build())
            .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder(SHA_256_CERT_PROPERTY)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .build())
            .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(PERMISSION_PROPERTY,
                    VisibilityPermissionDocument.SCHEMA_TYPE)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .build())
            .build();

    @NonNull
    @Field(id = 1, getter = "getId")
    private String mId;

    @Field(id = 2, getter = "isNotDisplayedBySystem")
    private final boolean mIsNotDisplayedBySystem;

    @NonNull
    @Field(id = 3, getter = "getPackageNames")
    private final String[] mPackageNames;

    @NonNull
    @Field(id = 4, getter = "getSha256Certs")
    private final byte[][] mSha256Certs;

    @Nullable
    @Field(id = 5, getter = "getPermissionDocuments")
    private final VisibilityPermissionDocument[] mPermissionDocuments;

    @Nullable
    // We still need to convert this class to a GenericDocument until we completely treat it
    // differently in AppSearchImpl.
    // TODO(b/298118943) Remove this once internally we don't use GenericDocument to store
    //  visibility information.
    private GenericDocument mGenericDocument;

    @Nullable
    private Integer mHashCode;

    @Constructor
    VisibilityDocument(
            @Param(id = 1) @NonNull String id,
            @Param(id = 2) boolean isNotDisplayedBySystem,
            @Param(id = 3) @NonNull String[] packageNames,
            @Param(id = 4) @NonNull byte[][] sha256Certs,
            @Param(id = 5) @Nullable VisibilityPermissionDocument[] permissionDocuments) {
        mId = Objects.requireNonNull(id);
        mIsNotDisplayedBySystem = isNotDisplayedBySystem;
        mPackageNames = Objects.requireNonNull(packageNames);
        mSha256Certs = Objects.requireNonNull(sha256Certs);
        mPermissionDocuments = permissionDocuments;
    }

    /**
     * Gets the id for this VisibilityDocument.
     *
     * <p>This is being used as the document id when we convert a {@link VisibilityDocument}
     * to a {@link GenericDocument}.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /** Returns whether this schema is visible to the system. */
    public boolean isNotDisplayedBySystem() {
        return mIsNotDisplayedBySystem;
    }

    /**
     * Returns a package name array which could access this schema. Use {@link #getSha256Certs()} to
     * get package's sha 256 certs. The same index of package names array and sha256Certs array
     * represents same package.
     */
    @NonNull
    public String[] getPackageNames() {
        return mPackageNames;
    }

    /**
     * Returns a package sha256Certs array which could access this schema. Use {@link
     * #getPackageNames()} to get package's name. The same index of package names array and
     * sha256Certs array represents same package.
     */
    @NonNull
    public byte[][] getSha256Certs() {
        return mSha256Certs;
    }

    /** Gets a list of {@link VisibilityDocument}.
     *
     * <p>A {@link VisibilityDocument} holds all required permissions for the caller need to have
     * to access the schema this {@link VisibilityDocument} presents.
     */
    @Nullable
    VisibilityPermissionDocument[] getPermissionDocuments() {
        return mPermissionDocuments;
    }

    /**
     * Returns an array of Android Permissions that caller mush hold to access the schema this
     * {@link VisibilityDocument} represents.
     */
    @NonNull
    public Set<Set<Integer>> getVisibleToPermissions() {
        if (mPermissionDocuments == null) {
            return Collections.emptySet();
        }
        Set<Set<Integer>> visibleToPermissions = new ArraySet<>(mPermissionDocuments.length);
        for (VisibilityPermissionDocument permissionDocument : mPermissionDocuments) {
            Set<Integer> requiredPermissions = permissionDocument.getAllRequiredPermissions();
            if (requiredPermissions != null) {
                visibleToPermissions.add(requiredPermissions);
            }
        }
        return visibleToPermissions;
    }

    /** Build the List of {@link VisibilityDocument} from visibility settings. */
    @NonNull
    public static List<VisibilityDocument> toVisibilityDocuments(
            @NonNull SetSchemaRequest setSchemaRequest) {
        Set<AppSearchSchema> searchSchemas = setSchemaRequest.getSchemas();
        Set<String> schemasNotDisplayedBySystem = setSchemaRequest.getSchemasNotDisplayedBySystem();
        Map<String, Set<PackageIdentifier>> schemasVisibleToPackages =
                setSchemaRequest.getSchemasVisibleToPackages();
        Map<String, Set<Set<Integer>>> schemasVisibleToPermissions =
                setSchemaRequest.getRequiredPermissionsForSchemaTypeVisibility();
        List<VisibilityDocument> visibilityDocuments = new ArrayList<>(searchSchemas.size());
        for (AppSearchSchema searchSchema : searchSchemas) {
            String schemaType = searchSchema.getSchemaType();
            VisibilityDocument.Builder documentBuilder =
                    new VisibilityDocument.Builder(/*id=*/ searchSchema.getSchemaType());
            documentBuilder.setNotDisplayedBySystem(
                    schemasNotDisplayedBySystem.contains(schemaType));

            if (schemasVisibleToPackages.containsKey(schemaType)) {
                documentBuilder.addVisibleToPackages(schemasVisibleToPackages.get(schemaType));
            }

            if (schemasVisibleToPermissions.containsKey(schemaType)) {
                documentBuilder.setVisibleToPermissions(
                        schemasVisibleToPermissions.get(schemaType));
            }
            visibilityDocuments.add(documentBuilder.build());
        }
        return visibilityDocuments;
    }

    /**
     * Generates a {@link GenericDocument} from the current class.
     *
     * <p>This conversion is needed until we don't treat Visibility related documents as
     * {@link GenericDocument}s internally.
     */
    @NonNull
    public GenericDocument toGenericDocument() {
        if (mGenericDocument == null) {
            GenericDocument.Builder<?> builder = new GenericDocument.Builder<>(
                    NAMESPACE, mId, SCHEMA_TYPE);
            builder.setPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY, mIsNotDisplayedBySystem);
            builder.setPropertyString(PACKAGE_NAME_PROPERTY, mPackageNames);
            builder.setPropertyBytes(SHA_256_CERT_PROPERTY, mSha256Certs);

            // Generate an array of GenericDocument for VisibilityPermissionDocument.
            if (mPermissionDocuments != null) {
                GenericDocument[] permissionGenericDocs =
                        new GenericDocument[mPermissionDocuments.length];
                for (int i = 0; i < mPermissionDocuments.length; ++i) {
                    permissionGenericDocs[i] = mPermissionDocuments[i].toGenericDocument();
                }
                builder.setPropertyDocument(PERMISSION_PROPERTY, permissionGenericDocs);
            }

            // The creationTimestamp doesn't matter for Visibility documents.
            // But to make tests pass, we set it 0 so two GenericDocuments generated from
            // the same VisibilityDocument can be same.
            builder.setCreationTimestampMillis(0L);

            mGenericDocument = builder.build();
        }
        return mGenericDocument;
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = Objects.hash(
                    mId,
                    mIsNotDisplayedBySystem,
                    Arrays.hashCode(mPackageNames),
                    Arrays.deepHashCode(mSha256Certs),
                    Arrays.hashCode(mPermissionDocuments));
        }
        return mHashCode;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VisibilityDocument)) {
            return false;
        }
        VisibilityDocument otherVisibilityDocument = (VisibilityDocument) other;
        return mId.equals(otherVisibilityDocument.mId)
                && mIsNotDisplayedBySystem == otherVisibilityDocument.mIsNotDisplayedBySystem
                && Arrays.equals(
                mPackageNames, otherVisibilityDocument.mPackageNames)
                && Arrays.deepEquals(
                mSha256Certs, otherVisibilityDocument.mSha256Certs)
                && Arrays.equals(
                mPermissionDocuments, otherVisibilityDocument.mPermissionDocuments);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        VisibilityDocumentCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link VisibilityDocument}. */
    public static final class Builder {
        private final Set<PackageIdentifier> mPackageIdentifiers = new ArraySet<>();
        private String mId;
        private boolean mIsNotDisplayedBySystem;
        private VisibilityPermissionDocument[] mPermissionDocuments;

        /**
         * Creates a {@link Builder} for a {@link VisibilityDocument}.
         *
         * @param id The SchemaType of the {@link AppSearchSchema} that this {@link
         *     VisibilityDocument} represents. The package and database prefix will be added in
         *     server side. We are using prefixed schema type to be the final id of this {@link
         *     VisibilityDocument}.
         */
        public Builder(@NonNull String id) {
            mId = Objects.requireNonNull(id);
        }

        /**
         * Constructs a {@link VisibilityDocument} from a {@link GenericDocument}.
         *
         * <p>This constructor is still needed until we don't treat Visibility related documents as
         * {@link GenericDocument}s internally.
         */
        public Builder(@NonNull GenericDocument genericDocument) {
            Objects.requireNonNull(genericDocument);

            mId = genericDocument.getId();
            mIsNotDisplayedBySystem = genericDocument.getPropertyBoolean(
                    NOT_DISPLAYED_BY_SYSTEM_PROPERTY);

            String[] packageNames = genericDocument.getPropertyStringArray(PACKAGE_NAME_PROPERTY);
            byte[][] sha256Certs = genericDocument.getPropertyBytesArray(SHA_256_CERT_PROPERTY);
            for (int i = 0; i < packageNames.length; ++i) {
                mPackageIdentifiers.add(new PackageIdentifier(packageNames[i], sha256Certs[i]));
            }

            GenericDocument[] permissionDocs =
                    genericDocument.getPropertyDocumentArray(PERMISSION_PROPERTY);
            if (permissionDocs != null) {
                mPermissionDocuments = new VisibilityPermissionDocument[permissionDocs.length];
                for (int i = 0; i < permissionDocs.length; ++i) {
                    mPermissionDocuments[i] = new VisibilityPermissionDocument.Builder(
                            permissionDocs[i]).build();
                }
            }
        }

        public Builder(@NonNull VisibilityDocument visibilityDocument) {
            Objects.requireNonNull(visibilityDocument);

            mIsNotDisplayedBySystem = visibilityDocument.mIsNotDisplayedBySystem;
            mPermissionDocuments = visibilityDocument.mPermissionDocuments;
            for (int i = 0; i < visibilityDocument.mPackageNames.length; ++i) {
                mPackageIdentifiers.add(new PackageIdentifier(visibilityDocument.mPackageNames[i],
                            visibilityDocument.mSha256Certs[i]));
            }
        }

        /** Sets id. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setId(@NonNull String id) {
            mId = Objects.requireNonNull(id);
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
            mPackageIdentifiers.addAll(Objects.requireNonNull(packageIdentifiers));
            return this;
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addVisibleToPackage(@NonNull PackageIdentifier packageIdentifier) {
            mPackageIdentifiers.add(Objects.requireNonNull(packageIdentifier));
            return this;
        }

        /**
         * Sets required permission sets for a package needs to hold to the schema this {@link
         * VisibilityDocument} represents.
         *
         * <p>The querier could have access if they holds ALL required permissions of ANY of the
         * individual value sets.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setVisibleToPermissions(@NonNull Set<Set<Integer>> visibleToPermissions) {
            Objects.requireNonNull(visibleToPermissions);
            mPermissionDocuments =
                    new VisibilityPermissionDocument[visibleToPermissions.size()];
            int i = 0;
            for (Set<Integer> allRequiredPermissions : visibleToPermissions) {
                mPermissionDocuments[i++] =
                        new VisibilityPermissionDocument.Builder(
                                NAMESPACE, /*id=*/ String.valueOf(i))
                                .setVisibleToAllRequiredPermissions(allRequiredPermissions)
                                .build();
            }
            return this;
        }

        /** Build a {@link VisibilityDocument} */
        @NonNull
        public VisibilityDocument build() {
            String[] packageNames = new String[mPackageIdentifiers.size()];
            byte[][] sha256Certs = new byte[mPackageIdentifiers.size()][32];
            int i = 0;
            for (PackageIdentifier packageIdentifier : mPackageIdentifiers) {
                packageNames[i] = packageIdentifier.getPackageName();
                sha256Certs[i] = packageIdentifier.getSha256Certificate();
                ++i;
            }
            return new VisibilityDocument(mId, mIsNotDisplayedBySystem,
                    packageNames, sha256Certs, mPermissionDocuments);
        }
    }
}

