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

package androidx.appsearch.playservicesstorage.converter;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.core.util.Preconditions;

/**
 * Translates between Gms and Jetpack versions of {@link GenericDocument}.

 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GenericDocumentToGmsConverter {
    private GenericDocumentToGmsConverter() {
    }

    /**
     * Translates a jetpack {@link androidx.appsearch.app.GenericDocument} into a Gms
     * {@link com.google.android.gms.appsearch.GenericDocument}.
     */
    @NonNull
    public static com.google.android.gms.appsearch.GenericDocument toGmsGenericDocument(
            @NonNull GenericDocument jetpackDocument) {
        Preconditions.checkNotNull(jetpackDocument);
        com.google.android.gms.appsearch.GenericDocument.Builder<
                com.google.android.gms.appsearch.GenericDocument.Builder<?>>
                gmsBuilder =
                new com.google.android.gms.appsearch.GenericDocument.Builder<>(
                        jetpackDocument.getNamespace(),
                        jetpackDocument.getId(),
                        jetpackDocument.getSchemaType());
        gmsBuilder
                .setScore(jetpackDocument.getScore())
                .setTtlMillis(jetpackDocument.getTtlMillis())
                .setCreationTimestampMillis(jetpackDocument.getCreationTimestampMillis());
        for (String propertyName : jetpackDocument.getPropertyNames()) {
            Object property = jetpackDocument.getProperty(propertyName);
            if (property instanceof String[]) {
                gmsBuilder.setPropertyString(propertyName, (String[]) property);
            } else if (property instanceof long[]) {
                gmsBuilder.setPropertyLong(propertyName, (long[]) property);
            } else if (property instanceof double[]) {
                gmsBuilder.setPropertyDouble(propertyName, (double[]) property);
            } else if (property instanceof boolean[]) {
                gmsBuilder.setPropertyBoolean(propertyName, (boolean[]) property);
            } else if (property instanceof byte[][]) {
                byte[][] byteValues = (byte[][]) property;
                gmsBuilder.setPropertyBytes(propertyName, byteValues);
            } else if (property instanceof GenericDocument[]) {
                GenericDocument[] documentValues = (GenericDocument[]) property;
                com.google.android.gms.appsearch.GenericDocument[] gmsSubDocuments =
                        new com.google.android.gms.appsearch.GenericDocument[documentValues.length];
                for (int j = 0; j < documentValues.length; j++) {
                    gmsSubDocuments[j] = toGmsGenericDocument(documentValues[j]);
                }
                gmsBuilder.setPropertyDocument(propertyName,
                        gmsSubDocuments);
            } else {
                throw new IllegalStateException(
                        String.format("Property \"%s\" has unsupported value type %s",
                                propertyName,
                                property.getClass().toString()));
            }
        }
        return gmsBuilder.build();
    }

    /**
     * Translates a Gms {@link com.google.android.gms.appsearch.GenericDocument}
     * into a jetpack {@link androidx.appsearch.app.GenericDocument}.
     */
    @NonNull
    public static GenericDocument toJetpackGenericDocument(
            @NonNull com.google.android.gms.appsearch.GenericDocument
                    gmsDocument) {
        Preconditions.checkNotNull(gmsDocument);
        GenericDocument.Builder<GenericDocument.Builder<?>> jetpackBuilder =
                new GenericDocument.Builder<>(
                        gmsDocument.getNamespace(),
                        gmsDocument.getId(),
                        gmsDocument.getSchemaType());
        jetpackBuilder
                .setScore(gmsDocument.getScore())
                .setTtlMillis(gmsDocument.getTtlMillis())
                .setCreationTimestampMillis(gmsDocument
                        .getCreationTimestampMillis());
        for (String propertyName : gmsDocument.getPropertyNames()) {
            Object property = gmsDocument.getProperty(propertyName);
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
            } else if (property instanceof com.google.android.gms.appsearch.GenericDocument[]) {
                com.google.android.gms.appsearch.GenericDocument[] documentValues =
                        (com.google.android.gms.appsearch.GenericDocument[]) property;
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
}
