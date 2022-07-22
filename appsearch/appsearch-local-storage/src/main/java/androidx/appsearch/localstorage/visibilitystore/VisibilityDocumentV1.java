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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PackageIdentifier;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.Set;

/**
 * Holds the visibility settings in version 1 that apply to a schema type.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class VisibilityDocumentV1 extends GenericDocument {
    /**
     * The Schema type for documents that hold AppSearch's metadata, e.g. visibility settings.
     */
    static final String SCHEMA_TYPE = "VisibilityType";
    /** Namespace of documents that contain visibility settings */
    static final String NAMESPACE = "";

    /**
     * Property that holds the list of platform-hidden schemas, as part of the visibility settings.
     */
    private static final String NOT_DISPLAYED_BY_SYSTEM_PROPERTY = "notPlatformSurfaceable";

    /** Property that holds the package name that can access a schema. */
    private static final String PACKAGE_NAME_PROPERTY = "packageName";

    /** Property that holds the SHA 256 certificate of the app that can access a schema. */
    private static final String SHA_256_CERT_PROPERTY = "sha256Cert";

    /** Property that holds the role can access a schema. */
    private static final String ROLE_PROPERTY = "role";

    /** Property that holds the required permissions to access the schema. */
    private static final String PERMISSION_PROPERTY = "permission";

    /**
     * Schema for the VisibilityStore's documents.
     */
    static final AppSearchSchema
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
            .addProperty(new AppSearchSchema.LongPropertyConfig.Builder(ROLE_PROPERTY)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .build())
            .addProperty(new AppSearchSchema.LongPropertyConfig.Builder(PERMISSION_PROPERTY)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .build())
            .build();

    VisibilityDocumentV1(@NonNull GenericDocument genericDocument) {
        super(genericDocument);
    }

    /** Returns whether this schema is visible to the system. */
    boolean isNotDisplayedBySystem() {
        return getPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY);
    }

    /**
     * Returns a package name array which could access this schema. Use {@link #getSha256Certs()}
     * to get package's sha 256 certs. The same index of package names array and sha256Certs array
     * represents same package.
     */
    @NonNull
    String[] getPackageNames() {
        return getPropertyStringArray(PACKAGE_NAME_PROPERTY);
    }

    /**
     * Returns a package sha256Certs array which could access this schema. Use
     * {@link #getPackageNames()} to get package's name. The same index of package names array
     * and sha256Certs array represents same package.
     */
    @NonNull
    byte[][] getSha256Certs() {
        return getPropertyBytesArray(SHA_256_CERT_PROPERTY);
    }

    /**
     * Returns an array of Android Roles that have access to the schema this
     * {@link VisibilityDocumentV1} represents.
     */
    @Nullable
    Set<Integer> getVisibleToRoles() {
        return toInts(getPropertyLongArray(ROLE_PROPERTY));
    }

    /**
     * Returns an array of Android Permissions that caller mush hold to access the schema
     * this {@link VisibilityDocumentV1} represents.
     */
    @Nullable
    Set<Integer> getVisibleToPermissions() {
        return toInts(getPropertyLongArray(PERMISSION_PROPERTY));
    }

    /** Builder for {@link VisibilityDocumentV1}. */
    static class Builder extends GenericDocument.Builder<Builder> {
        private final Set<PackageIdentifier> mPackageIdentifiers = new ArraySet<>();

        /**
         * Creates a {@link Builder} for a {@link VisibilityDocumentV1}.
         *
         * @param id The SchemaType of the {@link AppSearchSchema} that this
         *           {@link VisibilityDocumentV1} represents. The package and database prefix will
         *           be added in server side. We are using prefixed schema type to be the final
         *           id of this {@link VisibilityDocumentV1}.
         */
        Builder(@NonNull String id) {
            super(NAMESPACE, id, SCHEMA_TYPE);
        }

        /** Sets whether this schema has opted out of platform surfacing. */
        @NonNull
        Builder setNotDisplayedBySystem(boolean notDisplayedBySystem) {
            return setPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY,
                    notDisplayedBySystem);
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @NonNull
        Builder addVisibleToPackages(@NonNull Set<PackageIdentifier> packageIdentifiers) {
            Preconditions.checkNotNull(packageIdentifiers);
            mPackageIdentifiers.addAll(packageIdentifiers);
            return this;
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @NonNull
        Builder addVisibleToPackage(@NonNull PackageIdentifier packageIdentifier) {
            Preconditions.checkNotNull(packageIdentifier);
            mPackageIdentifiers.add(packageIdentifier);
            return this;
        }

        /** Add a set of Android role that has access to the schema this
         * {@link VisibilityDocumentV1} represents. */
        @NonNull
        Builder setVisibleToRoles(@NonNull Set<Integer> visibleToRoles) {
            Preconditions.checkNotNull(visibleToRoles);
            setPropertyLong(ROLE_PROPERTY, toLongs(visibleToRoles));
            return this;
        }

        /** Add a set of Android role that has access to the schema this
         * {@link VisibilityDocumentV1} represents. */
        @NonNull
        Builder setVisibleToPermissions(@NonNull Set<Integer> visibleToPermissions) {
            Preconditions.checkNotNull(visibleToPermissions);
            setPropertyLong(PERMISSION_PROPERTY, toLongs(visibleToPermissions));
            return this;
        }

        /** Build a {@link VisibilityDocumentV1} */
        @Override
        @NonNull
        public VisibilityDocumentV1 build() {
            String[] packageNames = new String[mPackageIdentifiers.size()];
            byte[][] sha256Certs = new byte[mPackageIdentifiers.size()][32];
            int i = 0;
            for (PackageIdentifier packageIdentifier : mPackageIdentifiers) {
                packageNames[i] = packageIdentifier.getPackageName();
                sha256Certs[i] = packageIdentifier.getSha256Certificate();
                ++i;
            }
            setPropertyString(PACKAGE_NAME_PROPERTY, packageNames);
            setPropertyBytes(SHA_256_CERT_PROPERTY, sha256Certs);
            return new VisibilityDocumentV1(super.build());
        }
    }

    @NonNull
    static long[] toLongs(@NonNull Set<Integer> properties) {
        long[] outputs = new long[properties.size()];
        int i = 0;
        for (int property : properties) {
            outputs[i++] = property;
        }
        return outputs;
    }

    @Nullable
    private static Set<Integer> toInts(@Nullable long[] properties) {
        if (properties == null) {
            return null;
        }
        Set<Integer> outputs = new ArraySet<>(properties.length);
        for (long property : properties) {
            outputs.add((int) property);
        }
        return outputs;
    }
}
