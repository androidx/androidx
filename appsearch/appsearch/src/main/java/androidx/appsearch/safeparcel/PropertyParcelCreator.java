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
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.EmbeddingVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An implemented creator for {@link PropertyParcel}.
 *
 * <p>In Jetpack, in order to serialize
 * {@link GenericDocumentParcel} for {@link androidx.appsearch.app.GenericDocument},
 * {@link PropertyParcel} needs to be a real {@link Parcelable}.
 */
// @exportToFramework:skipFile()
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PropertyParcelCreator implements Parcelable.Creator<PropertyParcel> {
    private static final String PROPERTY_NAME_FIELD = "propertyName";
    private static final String STRING_ARRAY_FIELD = "stringArray";
    private static final String LONG_ARRAY_FIELD = "longArray";
    private static final String DOUBLE_ARRAY_FIELD = "doubleArray";
    private static final String BOOLEAN_ARRAY_FIELD = "booleanArray";
    // 1d
    private static final String BYTE_ARRAY_FIELD = "byteArray";
    // 2d
    private static final String BYTES_ARRAY_FIELD = "bytesArray";
    private static final String DOC_ARRAY_FIELD = "docArray";
    private static final String EMBEDDING_VALUE_FIELD = "embeddingValue";
    private static final String EMBEDDING_MODEL_SIGNATURE_FIELD = "embeddingModelSignature";
    private static final String EMBEDDING_ARRAY_FIELD = "embeddingArray";

    public PropertyParcelCreator() {
    }

    /** Creates a {@link PropertyParcel} from a {@link Bundle}. */
    @SuppressWarnings({"unchecked"})
    @NonNull
    private static PropertyParcel createPropertyParcelFromBundle(
            @NonNull Bundle propertyParcelBundle) {
        Objects.requireNonNull(propertyParcelBundle);
        String propertyName = propertyParcelBundle.getString(PROPERTY_NAME_FIELD);

        Objects.requireNonNull(propertyName);
        PropertyParcel.Builder builder = new PropertyParcel.Builder(propertyName);

        // Get the values out of the bundle.
        String[] stringValues = propertyParcelBundle.getStringArray(STRING_ARRAY_FIELD);
        long[] longValues = propertyParcelBundle.getLongArray(LONG_ARRAY_FIELD);
        double[] doubleValues = propertyParcelBundle.getDoubleArray(DOUBLE_ARRAY_FIELD);
        boolean[] booleanValues = propertyParcelBundle.getBooleanArray(BOOLEAN_ARRAY_FIELD);

        List<Bundle> bytesArray;
        // SuppressWarnings can be applied on a local variable, but not any single line of
        // code.
        @SuppressWarnings("deprecation")
        List<Bundle> tmpList = propertyParcelBundle.getParcelableArrayList(BYTES_ARRAY_FIELD);
        bytesArray = tmpList;

        Parcelable[] docValues;
        // SuppressWarnings can be applied on a local variable, but not any single line of
        // code.
        @SuppressWarnings("deprecation")
        Parcelable[] tmpParcel = propertyParcelBundle.getParcelableArray(DOC_ARRAY_FIELD);
        docValues = tmpParcel;

        // SuppressWarnings can be applied on a local variable, but not any single line of
        // code.
        @SuppressWarnings("deprecation")
        List<Bundle> embeddingArray = propertyParcelBundle.getParcelableArrayList(
                EMBEDDING_ARRAY_FIELD);

        // Only one of those values will be set.
        boolean valueSet = false;
        if (stringValues != null) {
            builder.setStringValues(stringValues);
            valueSet = true;
        } else if (longValues != null) {
            builder.setLongValues(longValues);
            valueSet = true;
        } else if (doubleValues != null) {
            builder.setDoubleValues(doubleValues);
            valueSet = true;
        } else if (booleanValues != null) {
            builder.setBooleanValues(booleanValues);
            valueSet = true;
        } else if (bytesArray != null) {
            byte[][] bytes = new byte[bytesArray.size()][];
            for (int i = 0; i < bytesArray.size(); i++) {
                Bundle byteArray = bytesArray.get(i);
                if (byteArray == null) {
                    continue;
                }
                byte[] innerBytes = byteArray.getByteArray(BYTE_ARRAY_FIELD);
                if (innerBytes == null) {
                    continue;
                }
                bytes[i] = innerBytes;
            }
            builder.setBytesValues(bytes);
            valueSet = true;
        } else if (docValues != null) {
            GenericDocumentParcel[] documentParcels =
                    new GenericDocumentParcel[docValues.length];
            System.arraycopy(docValues, 0, documentParcels, 0, docValues.length);
            builder.setDocumentValues(documentParcels);
            valueSet = true;
        } else if (embeddingArray != null) {
            EmbeddingVector[] embeddings = new EmbeddingVector[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                Bundle embeddingBundle = embeddingArray.get(i);
                if (embeddingBundle == null) {
                    continue;
                }
                float[] values = embeddingBundle.getFloatArray(EMBEDDING_VALUE_FIELD);
                String modelSignature = embeddingBundle.getString(EMBEDDING_MODEL_SIGNATURE_FIELD);
                if (values == null || modelSignature == null) {
                    continue;
                }
                embeddings[i] = new EmbeddingVector(values, modelSignature);
            }
            builder.setEmbeddingValues(embeddings);
            valueSet = true;
        }

        if (!valueSet) {
            throw new IllegalArgumentException("property bundle passed in doesn't have any "
                    + "value set.");
        }

        return builder.build();
    }

    /** Creates a {@link Bundle} from a {@link PropertyParcel}. */
    @NonNull
    private static Bundle createBundleFromPropertyParcel(
            @NonNull PropertyParcel propertyParcel) {
        Objects.requireNonNull(propertyParcel);
        Bundle propertyParcelBundle = new Bundle();
        propertyParcelBundle.putString(PROPERTY_NAME_FIELD, propertyParcel.getPropertyName());

        // Check and set the properties
        String[] stringValues = propertyParcel.getStringValues();
        long[] longValues = propertyParcel.getLongValues();
        double[] doubleValues = propertyParcel.getDoubleValues();
        boolean[] booleanValues = propertyParcel.getBooleanValues();
        byte[][] bytesArray = propertyParcel.getBytesValues();
        GenericDocumentParcel[] docArray = propertyParcel.getDocumentValues();
        EmbeddingVector[] embeddingArray = propertyParcel.getEmbeddingValues();

        if (stringValues != null) {
            propertyParcelBundle.putStringArray(STRING_ARRAY_FIELD, stringValues);
        } else if (longValues != null) {
            propertyParcelBundle.putLongArray(LONG_ARRAY_FIELD, longValues);
        } else if (doubleValues != null) {
            propertyParcelBundle.putDoubleArray(DOUBLE_ARRAY_FIELD, doubleValues);
        } else if (booleanValues != null) {
            propertyParcelBundle.putBooleanArray(BOOLEAN_ARRAY_FIELD, booleanValues);
        } else if (bytesArray != null) {
            ArrayList<Bundle> bundles = new ArrayList<>(bytesArray.length);
            for (int i = 0; i < bytesArray.length; i++) {
                Bundle byteArray = new Bundle();
                byteArray.putByteArray(BYTE_ARRAY_FIELD, bytesArray[i]);
                bundles.add(byteArray);
            }
            propertyParcelBundle.putParcelableArrayList(BYTES_ARRAY_FIELD, bundles);
        } else if (docArray != null) {
            propertyParcelBundle.putParcelableArray(DOC_ARRAY_FIELD, docArray);
        } else if (embeddingArray != null) {
            ArrayList<Bundle> bundles = new ArrayList<>(embeddingArray.length);
            for (int i = 0; i < embeddingArray.length; i++) {
                Bundle embedding = new Bundle();
                embedding.putFloatArray(EMBEDDING_VALUE_FIELD, embeddingArray[i].getValues());
                embedding.putString(EMBEDDING_MODEL_SIGNATURE_FIELD,
                        embeddingArray[i].getModelSignature());
                bundles.add(embedding);
            }
            propertyParcelBundle.putParcelableArrayList(EMBEDDING_ARRAY_FIELD, bundles);
        }

        return propertyParcelBundle;
    }

    @NonNull
    @Override
    public PropertyParcel createFromParcel(Parcel in) {
        Bundle bundle = in.readBundle(getClass().getClassLoader());
        return createPropertyParcelFromBundle(bundle);
    }

    @Override
    public PropertyParcel[] newArray(int size) {
        return new PropertyParcel[size];
    }

    /** Writes a {@link PropertyParcel} to a {@link Parcel}. */
    public static void writeToParcel(@NonNull PropertyParcel propertyParcel,
            @NonNull android.os.Parcel parcel, int flags) {
        parcel.writeBundle(createBundleFromPropertyParcel(propertyParcel));
    }
}
