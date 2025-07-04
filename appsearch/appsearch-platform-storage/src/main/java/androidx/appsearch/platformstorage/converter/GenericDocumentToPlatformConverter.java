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

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * Translates between Platform and Jetpack versions of {@link GenericDocument}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class GenericDocumentToPlatformConverter {
    /**
     * Translates a jetpack {@link androidx.appsearch.app.GenericDocument} into a platform
     * {@link android.app.appsearch.GenericDocument}.
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static android.app.appsearch.@NonNull GenericDocument toPlatformGenericDocument(
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
            } else if (property instanceof EmbeddingVector[]) {
                if (!AppSearchVersionUtil.isAtLeastB()) {
                    throw new UnsupportedOperationException(
                            Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG
                                    + " is not available on this AppSearch implementation.");
                }
                EmbeddingVector[] embeddingVectors = (EmbeddingVector[]) property;
                ApiHelperForB.setPlatformPropertyEmbedding(platformBuilder, propertyName,
                        embeddingVectors);
            } else if (property instanceof AppSearchBlobHandle[]) {
                // TODO(b/273591938): Remove this once blob APIs are available.
                throw new UnsupportedOperationException(Features.BLOB_STORAGE
                        + " is not available on this AppSearch implementation.");
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
    @SuppressWarnings("deprecation")
    public static @NonNull GenericDocument toJetpackGenericDocument(
            android.app.appsearch.@NonNull GenericDocument platformDocument) {
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
            if (propertyName.equals(GenericDocument.PARENT_TYPES_SYNTHETIC_PROPERTY)) {
                if (!(property instanceof String[])) {
                    throw new IllegalStateException(
                            String.format("Parents list must be of String[] type, but got %s",
                                    property.getClass().toString()));
                }
                jetpackBuilder.setParentTypes(Arrays.asList((String[]) property));
                continue;
            }
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
            } else if (AppSearchVersionUtil.isAtLeastB()
                    && property instanceof android.app.appsearch.EmbeddingVector[]) {
                android.app.appsearch.EmbeddingVector[] embeddingVectors =
                        (android.app.appsearch.EmbeddingVector[]) property;
                ApiHelperForB.setJetpackPropertyEmbedding(jetpackBuilder, propertyName,
                        embeddingVectors);
            } else {
                throw new IllegalStateException(
                        String.format("Property \"%s\" has unsupported value type %s", propertyName,
                                property.getClass().toString()));
            }
        }
        return jetpackBuilder.build();
    }

    private GenericDocumentToPlatformConverter() {}

    @RequiresApi(36)
    private static class ApiHelperForB {
        private ApiHelperForB() {
        }

        @SuppressLint("NewApi") // EmbeddingVector is incorrectly flagged as needing 34-ext16
        @DoNotInline
        static void setPlatformPropertyEmbedding(
                android.app.appsearch.GenericDocument.@NonNull Builder<
                        android.app.appsearch.GenericDocument.Builder<?>> platformBuilder,
                @NonNull String propertyName,
                EmbeddingVector @NonNull [] jetpackEmbeddingVectors) {
            android.app.appsearch.EmbeddingVector[] platformEmbeddingVectors =
                    new android.app.appsearch.EmbeddingVector[jetpackEmbeddingVectors.length];
            for (int i = 0; i < jetpackEmbeddingVectors.length; i++) {
                platformEmbeddingVectors[i] = new android.app.appsearch.EmbeddingVector(
                        jetpackEmbeddingVectors[i].getValues(),
                        jetpackEmbeddingVectors[i].getModelSignature());
            }
            platformBuilder.setPropertyEmbedding(propertyName, platformEmbeddingVectors);
        }

        @SuppressLint("NewApi") // getValues() is incorrectly flagged as needing 34-ext16
        @DoNotInline
        static void setJetpackPropertyEmbedding(
                GenericDocument.@NonNull Builder<GenericDocument.Builder<?>> jetpackBuilder,
                @NonNull String propertyName,
                android.app.appsearch.EmbeddingVector @NonNull [] platformEmbeddingVectors) {
            EmbeddingVector[] jetpackEmbeddingVectors =
                    new EmbeddingVector[platformEmbeddingVectors.length];
            for (int i = 0; i < platformEmbeddingVectors.length; i++) {
                jetpackEmbeddingVectors[i] = new EmbeddingVector(
                        platformEmbeddingVectors[i].getValues(),
                        platformEmbeddingVectors[i].getModelSignature());
            }
            jetpackBuilder.setPropertyEmbedding(propertyName, jetpackEmbeddingVectors);
        }
    }
}
