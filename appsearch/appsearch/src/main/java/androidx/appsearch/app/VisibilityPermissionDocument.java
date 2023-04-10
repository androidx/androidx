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

package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;

import java.util.Set;

/**
 * The nested document that holds all required permissions for a caller need to hold to access the
 * schema which the outer {@link VisibilityDocument} represents.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VisibilityPermissionDocument extends GenericDocument {

    /**
     * The Schema type for documents that hold AppSearch's metadata, e.g. visibility settings.
     */
    public static final String SCHEMA_TYPE = "VisibilityPermissionType";

    /** Property that holds the required permissions to access the schema. */
    private static final String ALL_REQUIRED_PERMISSIONS_PROPERTY = "allRequiredPermissions";

    /**
     * Schema for the VisibilityStore's documents.
     *
     * <p>NOTE: If you update this, also update {@link VisibilityDocument#SCHEMA_VERSION_LATEST}.
     */
    public static final AppSearchSchema
            SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
            .addProperty(new AppSearchSchema.LongPropertyConfig
                    .Builder(ALL_REQUIRED_PERMISSIONS_PROPERTY)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .build())
            .build();

    VisibilityPermissionDocument(@NonNull GenericDocument genericDocument) {
        super(genericDocument);
    }

    /**
     * Returns an array of Android Permissions that caller mush hold to access the schema
     * that the outer {@link VisibilityDocument} represents.
     */
    @Nullable
    public Set<Integer> getAllRequiredPermissions() {
        return toInts(getPropertyLongArray(ALL_REQUIRED_PERMISSIONS_PROPERTY));
    }

    /** Builder for {@link VisibilityPermissionDocument}. */
    public static class Builder extends GenericDocument.Builder<Builder> {

        /**
         * Creates a {@link VisibilityDocument.Builder} for a {@link VisibilityDocument}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id, SCHEMA_TYPE);
        }

        /** Sets whether this schema has opted out of platform surfacing. */
        @NonNull
        public Builder setVisibleToAllRequiredPermissions(
                @NonNull Set<Integer> allRequiredPermissions) {
            setPropertyLong(ALL_REQUIRED_PERMISSIONS_PROPERTY, toLongs(allRequiredPermissions));
            return this;
        }

        /** Build a {@link VisibilityPermissionDocument} */
        @Override
        @NonNull
        public VisibilityPermissionDocument build() {
            return new VisibilityPermissionDocument(super.build());
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
