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
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class GenericDocumentToPlatformConverter {
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
                        jetpackDocument.getNamespace(),
                        jetpackDocument.getId(),
                        jetpackDocument.getSchemaType());
        platformBuilder
                .setScore(jetpackDocument.getScore())
                .setTtlMillis(jetpackDocument.getTtlMillis())
                .setCreationTimestampMillis(jetpackDocument.getCreationTimestampMillis());
        for (String propertyName : jetpackDocument.getPropertyNames()) {
            Object property = jetpackDocument.getProperty(propertyName);
            if (property instanceof String[]) {
                platformBuilder.setPropertyString(propertyName, (String[]) property);
            } else if (property instanceof long[]) {
                platformBuilder.setPropertyLong(propertyName, (long[]) property);
            } else if (property instanceof double[]) {
                platformBuilder.setPropertyDouble(propertyName, (double[]) property);
            } else if (property instanceof boolean[]) {
                platformBuilder.setPropertyBoolean(propertyName, (boolean[]) property);
            } else if (property instanceof byte[][]) {
                byte[][] byteValues = (byte[][]) property;
                // This is a patch for b/204677124, framework-appsearch in Android S and S_V2 will
                // crash if the user put a document with empty byte[][] or document[].
                if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.S
                        || Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2)
                        && byteValues.length == 0) {
                    continue;
                }
                platformBuilder.setPropertyBytes(propertyName, byteValues);
            } else if (property instanceof GenericDocument[]) {
                GenericDocument[] documentValues = (GenericDocument[]) property;
                // This is a patch for b/204677124, framework-appsearch in Android S and S_V2 will
                // crash if the user put a document with empty byte[][] or document[].
                if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.S
                        || Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2)
                        && documentValues.length == 0) {
                    continue;
                }
                android.app.appsearch.GenericDocument[] platformSubDocuments =
                        new android.app.appsearch.GenericDocument[documentValues.length];
                for (int j = 0; j < documentValues.length; j++) {
                    platformSubDocuments[j] = toPlatformGenericDocument(documentValues[j]);
                }
                platformBuilder.setPropertyDocument(propertyName, platformSubDocuments);
            } else {
                throw new IllegalStateException(
                        String.format("Property \"%s\" has unsupported value type %s", propertyName,
                                property.getClass().toString()));
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
                        platformDocument.getNamespace(),
                        platformDocument.getId(),
                        platformDocument.getSchemaType());
        jetpackBuilder
                .setScore(platformDocument.getScore())
                .setTtlMillis(platformDocument.getTtlMillis())
                .setCreationTimestampMillis(platformDocument.getCreationTimestampMillis());
        for (String propertyName : platformDocument.getPropertyNames()) {
            Object property = platformDocument.getProperty(propertyName);
            if (property instanceof String[]) {
                jetpackBuilder.setPropertyString(propertyName, (String[]) property);
            } else if (property instanceof long[]) {
                jetpackBuilder.setPropertyLong(propertyName, (long[]) property);
            } else if (property instanceof double[]) {
                jetpackBuilder.setPropertyDouble(propertyName, (double[]) property);
            } else if (property instanceof boolean[]) {
                jetpackBuilder.setPropertyBoolean(propertyName, (boolean[]) property);
            } else if (property instanceof byte[][]) {
                jetpackBuilder.setPropertyBytes(propertyName, (byte[][]) property);
            } else if (property instanceof android.app.appsearch.GenericDocument[]) {
                android.app.appsearch.GenericDocument[] documentValues =
                        (android.app.appsearch.GenericDocument[]) property;
                GenericDocument[] jetpackSubDocuments = new GenericDocument[documentValues.length];
                for (int j = 0; j < documentValues.length; j++) {
                    jetpackSubDocuments[j] = toJetpackGenericDocument(documentValues[j]);
                }
                jetpackBuilder.setPropertyDocument(propertyName, jetpackSubDocuments);
            } else {
                throw new IllegalStateException(
                        String.format("Property \"%s\" has unsupported value type %s", propertyName,
                                property.getClass().toString()));
            }
        }
        return jetpackBuilder.build();
    }

    private GenericDocumentToPlatformConverter() {}
}
