/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.platformstorage.converter;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.core.util.Preconditions;

/**
 * Translates between Platform and Jetpack versions of {@link GenericDocument}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class GenericDocumentToPlatformConverter {
    private GenericDocumentToPlatformConverter() {}

    /**
     * Translates a jetpack {@link androidx.appsearch.app.GenericDocument} into a platform
     * {@link android.app.appsearch.GenericDocument}.
     */
    @NonNull
    public static android.app.appsearch.GenericDocument toPlatformGenericDocument(
            @NonNull GenericDocument jetpackDocument) {
        Preconditions.checkNotNull(jetpackDocument);
        android.app.appsearch.GenericDocument.Builder<
                android.app.appsearch.GenericDocument.Builder<?>> platformBuilder =
                new android.app.appsearch.GenericDocument.Builder<>(
                        jetpackDocument.getUri(), jetpackDocument.getSchemaType());
        platformBuilder
                .setNamespace(jetpackDocument.getNamespace())
                .setScore(jetpackDocument.getScore())
                .setTtlMillis(jetpackDocument.getTtlMillis())
                .setCreationTimestampMillis(jetpackDocument.getCreationTimestampMillis());
        for (String propertyName : jetpackDocument.getPropertyNames()) {
            // TODO(b/174614009): This generates log spam from failed casts. Switch to getProperty
            // API once it is submitted.
            String[] stringValues = jetpackDocument.getPropertyStringArray(propertyName);
            long[] longValues = jetpackDocument.getPropertyLongArray(propertyName);
            double[] doubleValues = jetpackDocument.getPropertyDoubleArray(propertyName);
            boolean[] booleanValues = jetpackDocument.getPropertyBooleanArray(propertyName);
            byte[][] bytesValues = jetpackDocument.getPropertyBytesArray(propertyName);
            GenericDocument[] documentValues =
                    jetpackDocument.getPropertyDocumentArray(propertyName);
            if (stringValues != null) {
                platformBuilder.setPropertyString(propertyName, stringValues);
            } else if (longValues != null) {
                platformBuilder.setPropertyLong(propertyName, longValues);
            } else if (doubleValues != null) {
                platformBuilder.setPropertyDouble(propertyName, doubleValues);
            } else if (booleanValues != null) {
                platformBuilder.setPropertyBoolean(propertyName, booleanValues);
            } else if (bytesValues != null) {
                platformBuilder.setPropertyBytes(propertyName, bytesValues);
            } else if (documentValues != null) {
                android.app.appsearch.GenericDocument[] platformSubDocuments =
                        new android.app.appsearch.GenericDocument[documentValues.length];
                for (int j = 0; j < documentValues.length; j++) {
                    platformSubDocuments[j] = toPlatformGenericDocument(documentValues[j]);
                }
                platformBuilder.setPropertyDocument(propertyName, platformSubDocuments);
            } else {
                throw new IllegalStateException(
                        "Property \"" + propertyName + "\" has unsupported value type");
            }
        }
        return platformBuilder.build();
    }

    /**
     * Translates a platform {@link android.app.appsearch.GenericDocument} into a jetpack
     * {@link androidx.appsearch.app.GenericDocument}.
     */
    @NonNull
    public static GenericDocument toJetpackGenericDocument(
            @NonNull android.app.appsearch.GenericDocument platformDocument) {
        Preconditions.checkNotNull(platformDocument);
        GenericDocument.Builder<GenericDocument.Builder<?>> jetpackBuilder =
                new GenericDocument.Builder<>(
                        platformDocument.getUri(), platformDocument.getSchemaType());
        jetpackBuilder
                .setNamespace(platformDocument.getNamespace())
                .setScore(platformDocument.getScore())
                .setTtlMillis(platformDocument.getTtlMillis())
                .setCreationTimestampMillis(platformDocument.getCreationTimestampMillis());
        for (String propertyName : platformDocument.getPropertyNames()) {
            // TODO(b/174614009): This generates log spam from failed casts. Switch to getProperty
            // API once it is submitted.
            String[] stringValues = platformDocument.getPropertyStringArray(propertyName);
            long[] longValues = platformDocument.getPropertyLongArray(propertyName);
            double[] doubleValues = platformDocument.getPropertyDoubleArray(propertyName);
            boolean[] booleanValues = platformDocument.getPropertyBooleanArray(propertyName);
            byte[][] bytesValues = platformDocument.getPropertyBytesArray(propertyName);
            android.app.appsearch.GenericDocument[] documentValues =
                    platformDocument.getPropertyDocumentArray(propertyName);
            if (stringValues != null) {
                jetpackBuilder.setPropertyString(propertyName, stringValues);
            } else if (longValues != null) {
                jetpackBuilder.setPropertyLong(propertyName, longValues);
            } else if (doubleValues != null) {
                jetpackBuilder.setPropertyDouble(propertyName, doubleValues);
            } else if (booleanValues != null) {
                jetpackBuilder.setPropertyBoolean(propertyName, booleanValues);
            } else if (bytesValues != null) {
                jetpackBuilder.setPropertyBytes(propertyName, bytesValues);
            } else if (documentValues != null) {
                GenericDocument[] jetpackSubDocuments = new GenericDocument[documentValues.length];
                for (int j = 0; j < documentValues.length; j++) {
                    jetpackSubDocuments[j] = toJetpackGenericDocument(documentValues[j]);
                }
                jetpackBuilder.setPropertyDocument(propertyName, jetpackSubDocuments);
            } else {
                throw new IllegalStateException(
                        "Property \"" + propertyName + "\" has unsupported value type");
            }
        }
        return jetpackBuilder.build();
    }
}
