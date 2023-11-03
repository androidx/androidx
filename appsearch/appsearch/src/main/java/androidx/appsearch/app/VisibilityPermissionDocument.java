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

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.VisibilityPermissionDocumentCreator;
import androidx.collection.ArraySet;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * The nested document that holds all required permissions for a caller need to hold to access the
 * schema which the outer {@link VisibilityDocument} represents.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SafeParcelable.Class(creator = "VisibilityPermissionDocumentCreator")
public final class VisibilityPermissionDocument extends AbstractSafeParcelable {
    @NonNull
    public static final VisibilityPermissionDocumentCreator CREATOR =
            new VisibilityPermissionDocumentCreator();

    /**
     * The Schema type for documents that hold AppSearch's metadata, such as visibility settings.
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

    @NonNull
    @Field(id = 1, getter = "getId")
    private final String mId;

    @NonNull
    @Field(id = 2, getter = "getNamespace")
    private final String mNamespace;

    @Nullable
    @Field(id = 3, getter = "getAllRequiredPermissionsInts")
    // SafeParcelable doesn't support Set<Integer>, so we have to convert it to int[].
    private final int[] mAllRequiredPermissions;

    @Nullable
    // We still need to convert this class to a GenericDocument until we completely treat it
    // differently in AppSearchImpl.
    // TODO(b/298118943) Remove this once internally we don't use GenericDocument to store
    //  visibility information.
    private GenericDocument mGenericDocument;

    @Nullable
    private Integer mHashCode;

    @Constructor
    VisibilityPermissionDocument(
            @Param(id = 1) @NonNull String id,
            @Param(id = 2) @NonNull String namespace,
            @Param(id = 3) @Nullable int[] allRequiredPermissions) {
        mId = Objects.requireNonNull(id);
        mNamespace = Objects.requireNonNull(namespace);
        mAllRequiredPermissions = allRequiredPermissions;
    }

    /**
     * Gets the id for this {@link VisibilityPermissionDocument}.
     *
     * <p>This is being used as the document id when we convert a
     * {@link VisibilityPermissionDocument} to a {@link GenericDocument}.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Gets the namespace for this {@link VisibilityPermissionDocument}.
     *
     * <p>This is being used as the namespace when we convert a
     * {@link VisibilityPermissionDocument} to a {@link GenericDocument}.
     */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Gets the required Android Permissions in an int array. */
    @Nullable
    int[] getAllRequiredPermissionsInts() {
        return mAllRequiredPermissions;
    }

    /**
     * Returns an array of Android Permissions that caller mush hold to access the schema that the
     * outer {@link VisibilityDocument} represents.
     */
    @Nullable
    public Set<Integer> getAllRequiredPermissions() {
        return toIntegerSet(mAllRequiredPermissions);
    }

    @NonNull
    private static int[] toInts(@NonNull Set<Integer> properties) {
        int[] outputs = new int[properties.size()];
        int i = 0;
        for (int property : properties) {
            outputs[i++] = property;
        }
        return outputs;
    }

    @Nullable
    private static Set<Integer> toIntegerSet(@Nullable int[] properties) {
        if (properties == null) {
            return null;
        }
        Set<Integer> outputs = new ArraySet<>(properties.length);
        for (int property : properties) {
            outputs.add(property);
        }
        return outputs;
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
                    mNamespace, mId, SCHEMA_TYPE);

            if (mAllRequiredPermissions != null) {
                // GenericDocument only supports long, so int[] needs to be converted to
                // long[] here.
                long[] longs = new long[mAllRequiredPermissions.length];
                for (int i = 0; i < mAllRequiredPermissions.length; ++i) {
                    longs[i] = mAllRequiredPermissions[i];
                }
                builder.setPropertyLong(ALL_REQUIRED_PERMISSIONS_PROPERTY, longs);
            }

            mGenericDocument = builder.build();
        }
        return mGenericDocument;
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = Objects.hash(mId, mNamespace, Arrays.hashCode(mAllRequiredPermissions));
        }
        return mHashCode;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VisibilityPermissionDocument)) {
            return false;
        }
        VisibilityPermissionDocument otherVisibilityPermissionDocument =
                (VisibilityPermissionDocument) other;
        return mId.equals(otherVisibilityPermissionDocument.mId)
                && mNamespace.equals(otherVisibilityPermissionDocument.mNamespace)
                && Arrays.equals(
                mAllRequiredPermissions,
                otherVisibilityPermissionDocument.mAllRequiredPermissions);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        VisibilityPermissionDocumentCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link VisibilityPermissionDocument}. */
    public static final class Builder {
        private String mId;
        private String mNamespace;
        private int[] mAllRequiredPermissions;

        /**
         * Constructs a {@link VisibilityPermissionDocument} from a {@link GenericDocument}.
         *
         * <p>This constructor is still needed until we don't treat Visibility related documents as
         * {@link GenericDocument}s internally.
         */
        public Builder(@NonNull GenericDocument genericDocument) {
            Objects.requireNonNull(genericDocument);
            mId = genericDocument.getId();
            mNamespace = genericDocument.getNamespace();
            // GenericDocument only supports long[], so we need to convert it back to int[].
            long[] longs = genericDocument.getPropertyLongArray(
                    ALL_REQUIRED_PERMISSIONS_PROPERTY);
            if (longs != null) {
                mAllRequiredPermissions = new int[longs.length];
                for (int i = 0; i < longs.length; ++i) {
                    mAllRequiredPermissions[i] = (int) longs[i];
                }
            }
        }

        /** Creates a {@link VisibilityDocument.Builder} for a {@link VisibilityDocument}. */
        public Builder(@NonNull String namespace, @NonNull String id) {
            mNamespace = Objects.requireNonNull(namespace);
            mId = Objects.requireNonNull(id);
        }

        /**
         * Sets a set of Android Permissions that caller mush hold to access the schema that the
         * outer {@link VisibilityDocument} represents.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setVisibleToAllRequiredPermissions(
                @NonNull Set<Integer> allRequiredPermissions) {
            mAllRequiredPermissions = toInts(Objects.requireNonNull(allRequiredPermissions));
            return this;
        }

        /** Builds a {@link VisibilityPermissionDocument} */
        @NonNull
        public VisibilityPermissionDocument build() {
            return new VisibilityPermissionDocument(mId,
                    mNamespace,
                    mAllRequiredPermissions);
        }
    }
}
