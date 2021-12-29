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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds the visibility settings that apply to a schema type.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VisibilityDocument extends GenericDocument {
    /**
     * The Schema type for documents that hold AppSearch's metadata, e.g. visibility settings.
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

    /** Property that holds the role can access a schema. */
    private static final String ROLE_PROPERTY = "role";

    // The initial schema version, one VisibilityDocument contains all visibility information for
    // whole package.
    public static final int SCHEMA_VERSION_DOC_PER_PACKAGE = 0;

    // One VisibilityDocument contains visibility information for a single schema.
    public static final int SCHEMA_VERSION_DOC_PER_SCHEMA = 1;

    public static final int SCHEMA_VERSION_LATEST = SCHEMA_VERSION_DOC_PER_SCHEMA;

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
            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(ROLE_PROPERTY)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .build())
            .build();

    public VisibilityDocument(@NonNull GenericDocument genericDocument) {
        super(genericDocument);
    }

    public VisibilityDocument(@NonNull Bundle bundle) {
        super(bundle);
    }

    /** Returns whether this schema is visible to the system. */
    public boolean isNotDisplayedBySystem() {
        return getPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY);
    }

    /**
     * Returns a package name array which could access this schema. Use {@link #getSha256Certs()}
     * to get package's sha 256 certs. The same index of package names array and sha256Certs array
     * represents same package.
     */
    @NonNull
    public String[] getPackageNames() {
        return getPropertyStringArray(PACKAGE_NAME_PROPERTY);
    }

    /**
     * Returns a package sha256Certs array which could access this schema. Use
     * {@link #getPackageNames()} to get package's name. The same index of package names array
     * and sha256Certs array represents same package.
     */
    @NonNull
    public byte[][] getSha256Certs() {
        return getPropertyBytesArray(SHA_256_CERT_PROPERTY);
    }

    /**
     * Returns an array of Android Roles that have access to the schema this
     * {@link VisibilityDocument} represents.
     */
    @Nullable
    public String[] getVisibleToRoles() {
        return getPropertyStringArray(ROLE_PROPERTY);
    }

    /** Builder for {@link VisibilityDocument}. */
    public static class Builder extends GenericDocument.Builder<Builder> {
        private final Set<PackageIdentifier> mPackageIdentifiers = new ArraySet<>();

        /**
         * Creates a {@link Builder} for a {@link VisibilityDocument}.
         *
         * @param id The SchemaType of the {@link AppSearchSchema} that this
         *           {@link VisibilityDocument} represents. The package and database prefix will be
         *           added in server side. We are using prefixed schema type to be the final id of
         *           this {@link VisibilityDocument}.
         */
        public Builder(@NonNull String id) {
            super(NAMESPACE, id, SCHEMA_TYPE);
        }

        /** Sets whether this schema has opted out of platform surfacing. */
        @NonNull
        public Builder setNotDisplayedBySystem(boolean notDisplayedBySystem) {
            return setPropertyBoolean(NOT_DISPLAYED_BY_SYSTEM_PROPERTY,
                    notDisplayedBySystem);
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @NonNull
        public Builder addVisibleToPackages(@NonNull Set<PackageIdentifier> packageIdentifiers) {
            Preconditions.checkNotNull(packageIdentifiers);
            mPackageIdentifiers.addAll(packageIdentifiers);
            return this;
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @NonNull
        public Builder addVisibleToPackage(@NonNull PackageIdentifier packageIdentifier) {
            Preconditions.checkNotNull(packageIdentifier);
            mPackageIdentifiers.add(packageIdentifier);
            return this;
        }

        /** Add a set of Android role that has access to the schema this
         * {@link VisibilityDocument} represents. */
        @NonNull
        public Builder setVisibleToRoles(@NonNull String... visibleToRoles) {
            Preconditions.checkNotNull(visibleToRoles);
            setPropertyString(ROLE_PROPERTY, visibleToRoles);
            return this;
        }

        /** Build a {@link VisibilityDocument} */
        @Override
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
            setPropertyString(PACKAGE_NAME_PROPERTY, packageNames);
            setPropertyBytes(SHA_256_CERT_PROPERTY, sha256Certs);
            return new VisibilityDocument(super.build());
        }
    }


    /**  Build the List of {@link VisibilityDocument} from visibility settings. */
    @NonNull
    public static List<VisibilityDocument> toVisibilityDocuments(
            @NonNull SetSchemaRequest setSchemaRequest) {
        Set<AppSearchSchema> searchSchemas = setSchemaRequest.getSchemas();
        Set<String> schemasNotDisplayedBySystem = setSchemaRequest.getSchemasNotDisplayedBySystem();
        Map<String, Set<PackageIdentifier>> schemasVisibleToPackages =
                setSchemaRequest.getSchemasVisibleToPackages();
        Map<String, Set<String>> schemasVisibleToRoles =
                setSchemaRequest.getAllowedRolesForSchemaTypeVisibility();

        List<VisibilityDocument> visibilityDocuments = new ArrayList<>(searchSchemas.size());

        for (AppSearchSchema searchSchema : searchSchemas) {
            String schemaType = searchSchema.getSchemaType();
            VisibilityDocument.Builder documentBuilder =
                    new VisibilityDocument.Builder(/*id=*/searchSchema.getSchemaType());
            documentBuilder.setNotDisplayedBySystem(
                    schemasNotDisplayedBySystem.contains(schemaType));

            if (schemasVisibleToPackages.containsKey(schemaType)) {
                documentBuilder.addVisibleToPackages(schemasVisibleToPackages.get(schemaType));
            }

            if (schemasVisibleToRoles.containsKey(schemaType)) {
                documentBuilder.setVisibleToRoles(
                        schemasVisibleToRoles.get(schemaType).toArray(new String[0]));
            }
            visibilityDocuments.add(documentBuilder.build());
        }
        return visibilityDocuments;
    }
}
