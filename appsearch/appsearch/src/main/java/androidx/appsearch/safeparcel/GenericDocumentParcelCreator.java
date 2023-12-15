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

package androidx.appsearch.safeparcel;


import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

/**
 * An implemented creator for {@link GenericDocumentParcel}.
 *
 * <p>In Jetpack, in order to serialize
 * {@link GenericDocumentParcel} for {@link androidx.appsearch.app.GenericDocument},
 * {@link PropertyParcel} needs to be a real {@link Parcelable}.
 */
// @exportToFramework:skipFile()
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GenericDocumentParcelCreator implements
        Parcelable.Creator<GenericDocumentParcel> {
    private static final String PROPERTIES_FIELD = "properties";
    private static final String SCHEMA_TYPE_FIELD = "schemaType";
    private static final String ID_FIELD = "id";
    private static final String SCORE_FIELD = "score";
    private static final String TTL_MILLIS_FIELD = "ttlMillis";
    private static final String CREATION_TIMESTAMP_MILLIS_FIELD = "creationTimestampMillis";
    private static final String NAMESPACE_FIELD = "namespace";
    private static final String PARENT_TYPES_FIELD = "parentTypes";

    /** Creates a {@link GenericDocumentParcel} from a {@link Bundle}. */
    @NonNull
    private static GenericDocumentParcel createGenericDocumentParcelFromBundle(
            @NonNull Bundle genericDocumentParcelBundle) {
        // Get namespace, id, and schema type
        String namespace = genericDocumentParcelBundle.getString(NAMESPACE_FIELD);
        String id = genericDocumentParcelBundle.getString(ID_FIELD);
        String schemaType = genericDocumentParcelBundle.getString(SCHEMA_TYPE_FIELD);

        // Those three can NOT be null.
        if (namespace == null || id == null || schemaType == null) {
            throw new IllegalArgumentException("GenericDocumentParcel bundle doesn't have "
                    + "namespace, id, or schemaType.");
        }

        GenericDocumentParcel.Builder builder = new GenericDocumentParcel.Builder(namespace,
                id, schemaType);
        List<String> parentTypes =
                genericDocumentParcelBundle.getStringArrayList(PARENT_TYPES_FIELD);
        if (parentTypes != null) {
            builder.setParentTypes(parentTypes);
        }
        builder.setScore(genericDocumentParcelBundle.getInt(SCORE_FIELD));
        builder.setCreationTimestampMillis(
                genericDocumentParcelBundle.getLong(CREATION_TIMESTAMP_MILLIS_FIELD));
        builder.setTtlMillis(genericDocumentParcelBundle.getLong(TTL_MILLIS_FIELD));

        // properties
        Bundle propertyBundle = genericDocumentParcelBundle.getBundle(PROPERTIES_FIELD);
        if (propertyBundle != null) {
            for (String propertyName : propertyBundle.keySet()) {
                // SuppressWarnings can be applied on a local variable, but not any
                // single line of code.
                @SuppressWarnings("deprecation")
                PropertyParcel propertyParcel = propertyBundle.getParcelable(propertyName);
                builder.putInPropertyMap(propertyName, propertyParcel);
            }
        }

        return builder.build();
    }

    /** Creates a {@link Bundle} from a {@link GenericDocumentParcel}. */
    @NonNull
    private static Bundle createBundleFromGenericDocumentParcel(
            @NonNull GenericDocumentParcel genericDocumentParcel) {
        Bundle genericDocumentParcelBundle = new Bundle();

        // Common fields
        genericDocumentParcelBundle.putString(NAMESPACE_FIELD,
                genericDocumentParcel.getNamespace());
        genericDocumentParcelBundle.putString(ID_FIELD, genericDocumentParcel.getId());
        genericDocumentParcelBundle.putString(SCHEMA_TYPE_FIELD,
                genericDocumentParcel.getSchemaType());
        genericDocumentParcelBundle.putStringArrayList(PARENT_TYPES_FIELD,
                (ArrayList<String>) genericDocumentParcel.getParentTypes());
        genericDocumentParcelBundle.putInt(SCORE_FIELD, genericDocumentParcel.getScore());
        genericDocumentParcelBundle.putLong(CREATION_TIMESTAMP_MILLIS_FIELD,
                genericDocumentParcel.getCreationTimestampMillis());
        genericDocumentParcelBundle.putLong(TTL_MILLIS_FIELD,
                genericDocumentParcel.getTtlMillis());

        // Properties
        Bundle properties = new Bundle();
        List<PropertyParcel> propertyParcels = genericDocumentParcel.getProperties();
        for (int i = 0; i < propertyParcels.size(); ++i) {
            PropertyParcel propertyParcel = propertyParcels.get(i);
            properties.putParcelable(propertyParcel.getPropertyName(), propertyParcel);
        }
        genericDocumentParcelBundle.putBundle(PROPERTIES_FIELD, properties);

        return genericDocumentParcelBundle;
    }

    @Nullable
    @Override
    public GenericDocumentParcel createFromParcel(Parcel in) {
        Bundle bundle = in.readBundle(getClass().getClassLoader());
        return createGenericDocumentParcelFromBundle(bundle);
    }

    @Override
    public GenericDocumentParcel[] newArray(int size) {
        return new GenericDocumentParcel[size];
    }

    /** Writes a {@link GenericDocumentParcel} to a {@link Parcel}. */
    public static void writeToParcel(@NonNull GenericDocumentParcel genericDocumentParcel,
            @NonNull android.os.Parcel parcel, int flags) {
        parcel.writeBundle(createBundleFromGenericDocumentParcel(genericDocumentParcel));
    }
}
