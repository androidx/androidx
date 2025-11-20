/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.localstorage.converter;

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.AppSearchConfig;
import androidx.appsearch.localstorage.SchemaCache;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.DocumentProtoOrBuilder;
import com.google.android.icing.proto.PropertyProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.protobuf.ByteString;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Translates a {@link GenericDocument} into a {@link DocumentProto}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GenericDocumentToProtoConverter {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    private static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    private static final byte[][] EMPTY_BYTES_ARRAY = new byte[0][0];
    private static final GenericDocument[] EMPTY_DOCUMENT_ARRAY = new GenericDocument[0];
    private static final EmbeddingVector[] EMPTY_EMBEDDING_ARRAY =
            new EmbeddingVector[0];

    private GenericDocumentToProtoConverter() {
    }

    /**
     * Converts a {@link GenericDocument} into a {@link DocumentProto}.
     */
    @SuppressWarnings("unchecked")
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static @NonNull DocumentProto toDocumentProto(@NonNull GenericDocument document) {
        Preconditions.checkNotNull(document);
        DocumentProto.Builder mProtoBuilder = DocumentProto.newBuilder();
        mProtoBuilder.setUri(document.getId())
                .setSchema(document.getSchemaType())
                .setNamespace(document.getNamespace())
                .setScore(document.getScore())
                .setTtlMs(document.getTtlMillis())
                .setCreationTimestampMs(document.getCreationTimestampMillis());
        ArrayList<String> keys = new ArrayList<>(document.getPropertyNames());
        Collections.sort(keys);
        for (int i = 0; i < keys.size(); i++) {
            String name = keys.get(i);
            PropertyProto.Builder propertyProto = PropertyProto.newBuilder().setName(name);
            Object property = document.getProperty(name);
            if (property instanceof String[]) {
                String[] stringValues = (String[]) property;
                for (int j = 0; j < stringValues.length; j++) {
                    propertyProto.addStringValues(stringValues[j]);
                }
            } else if (property instanceof long[]) {
                long[] longValues = (long[]) property;
                for (int j = 0; j < longValues.length; j++) {
                    propertyProto.addInt64Values(longValues[j]);
                }
            } else if (property instanceof double[]) {
                double[] doubleValues = (double[]) property;
                for (int j = 0; j < doubleValues.length; j++) {
                    propertyProto.addDoubleValues(doubleValues[j]);
                }
            } else if (property instanceof boolean[]) {
                boolean[] booleanValues = (boolean[]) property;
                for (int j = 0; j < booleanValues.length; j++) {
                    propertyProto.addBooleanValues(booleanValues[j]);
                }
            } else if (property instanceof byte[][]) {
                byte[][] bytesValues = (byte[][]) property;
                for (int j = 0; j < bytesValues.length; j++) {
                    propertyProto.addBytesValues(ByteString.copyFrom(bytesValues[j]));
                }
            } else if (property instanceof GenericDocument[]) {
                GenericDocument[] documentValues = (GenericDocument[]) property;
                for (int j = 0; j < documentValues.length; j++) {
                    DocumentProto proto = toDocumentProto(documentValues[j]);
                    propertyProto.addDocumentValues(proto);
                }
            } else if (property instanceof EmbeddingVector[]) {
                EmbeddingVector[] embeddingValues = (EmbeddingVector[]) property;
                for (int j = 0; j < embeddingValues.length; j++) {
                    propertyProto.addVectorValues(
                            embeddingVectorToVectorProto(embeddingValues[j]));
                }
            } else if (property instanceof AppSearchBlobHandle[]) {
                AppSearchBlobHandle[] blobHandleValues = (AppSearchBlobHandle[]) property;
                for (int j = 0; j < blobHandleValues.length; j++) {
                    propertyProto.addBlobHandleValues(
                            BlobHandleToProtoConverter.toBlobHandleProto(blobHandleValues[j]));
                }
            } else if (property == null) {
                throw new IllegalStateException(
                        String.format("Property \"%s\" doesn't have any value!", name));
            } else {
                throw new IllegalStateException(
                        String.format("Property \"%s\" has unsupported value type %s", name,
                                property.getClass().toString()));
            }
            mProtoBuilder.addProperties(propertyProto);
        }
        return mProtoBuilder.build();
    }

    /**
     * Converts a {@link DocumentProto} into a {@link GenericDocument}.
     *
     * <p>In the case that the {@link DocumentProto} object proto has no values set, the
     * converter searches for the matching property name in the {@link SchemaTypeConfigProto}
     * object for the document, and infers the correct default value to set for the empty
     * property based on the data type of the property defined by the schema type.
     *
     * @param proto         the document to convert to a {@link GenericDocument} instance. The
     *                      document proto should have its package + database prefix stripped
     *                      from its fields.
     * @param prefix        the package + database prefix used searching the {@code schemaTypeMap}.
     * @param schemaCache   The SchemaCache instance held in AppSearch.
     */
    @SuppressWarnings("deprecation")
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static @NonNull GenericDocument toGenericDocument(@NonNull DocumentProtoOrBuilder proto,
            @NonNull String prefix,
            @NonNull SchemaCache schemaCache,
            @NonNull AppSearchConfig config) throws AppSearchException {
        Preconditions.checkNotNull(proto);
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(schemaCache);
        Preconditions.checkNotNull(config);
        Map<String, SchemaTypeConfigProto> schemaTypeMap =
                schemaCache.getSchemaMapForPrefix(prefix);

        GenericDocument.Builder<?> documentBuilder =
                new GenericDocument.Builder<>(proto.getNamespace(), proto.getUri(),
                        proto.getSchema())
                        .setScore(proto.getScore())
                        .setTtlMillis(proto.getTtlMs())
                        .setCreationTimestampMillis(proto.getCreationTimestampMs());
        String prefixedSchemaType = prefix + proto.getSchema();
        if (config.shouldRetrieveParentInfo() && !Flags.enableSearchResultParentTypes()) {
            List<String> parentSchemaTypes =
                    schemaCache.getTransitiveUnprefixedParentSchemaTypes(
                            prefix, prefixedSchemaType);
            if (!parentSchemaTypes.isEmpty()) {
                if (config.shouldStoreParentInfoAsSyntheticProperty()) {
                    documentBuilder.setPropertyString(
                            GenericDocument.PARENT_TYPES_SYNTHETIC_PROPERTY,
                            parentSchemaTypes.toArray(new String[0]));
                } else {
                    documentBuilder.setParentTypes(parentSchemaTypes);
                }
            }
        }

        for (int i = 0; i < proto.getPropertiesCount(); i++) {
            PropertyProto property = proto.getProperties(i);
            String name = property.getName();
            if (property.getStringValuesCount() > 0) {
                String[] values = new String[property.getStringValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getStringValues(j);
                }
                documentBuilder.setPropertyString(name, values);
            } else if (property.getInt64ValuesCount() > 0) {
                long[] values = new long[property.getInt64ValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getInt64Values(j);
                }
                documentBuilder.setPropertyLong(name, values);
            } else if (property.getDoubleValuesCount() > 0) {
                double[] values = new double[property.getDoubleValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getDoubleValues(j);
                }
                documentBuilder.setPropertyDouble(name, values);
            } else if (property.getBooleanValuesCount() > 0) {
                boolean[] values = new boolean[property.getBooleanValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getBooleanValues(j);
                }
                documentBuilder.setPropertyBoolean(name, values);
            } else if (property.getBytesValuesCount() > 0) {
                byte[][] values = new byte[property.getBytesValuesCount()][];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getBytesValues(j).toByteArray();
                }
                documentBuilder.setPropertyBytes(name, values);
            } else if (property.getDocumentValuesCount() > 0) {
                GenericDocument[] values = new GenericDocument[property.getDocumentValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = toGenericDocument(property.getDocumentValues(j), prefix,
                            schemaCache, config);
                }
                documentBuilder.setPropertyDocument(name, values);
            } else if (property.getVectorValuesCount() > 0) {
                EmbeddingVector[] values =
                        new EmbeddingVector[property.getVectorValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = vectorProtoToEmbeddingVector(property.getVectorValues(j));
                }
                documentBuilder.setPropertyEmbedding(name, values);
            } else if (property.getBlobHandleValuesCount() > 0) {
                AppSearchBlobHandle[] values =
                        new AppSearchBlobHandle[property.getBlobHandleValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = BlobHandleToProtoConverter.toAppSearchBlobHandle(
                            property.getBlobHandleValues(j));
                }
                documentBuilder.setPropertyBlobHandle(name, values);
            } else {
                // TODO(b/184966497): Optimize by caching PropertyConfigProto
                SchemaTypeConfigProto schema =
                        Preconditions.checkNotNull(schemaTypeMap.get(prefixedSchemaType));
                setEmptyProperty(name, documentBuilder, schema);
            }
        }
        return documentBuilder.build();
    }

    /**
     * Converts a {@link PropertyProto.VectorProto} into an {@link EmbeddingVector}.
     */
    public static @NonNull EmbeddingVector vectorProtoToEmbeddingVector(
            PropertyProto.@NonNull VectorProto vectorProto) {
        Preconditions.checkNotNull(vectorProto);

        float[] values = new float[vectorProto.getValuesCount()];
        for (int i = 0; i < vectorProto.getValuesCount(); i++) {
            values[i] = vectorProto.getValues(i);
        }
        return new EmbeddingVector(values, vectorProto.getModelSignature());
    }

    /**
     * Converts an {@link EmbeddingVector} into a {@link PropertyProto.VectorProto}.
     */
    public static PropertyProto.@NonNull VectorProto embeddingVectorToVectorProto(
            @NonNull EmbeddingVector embedding) {
        Preconditions.checkNotNull(embedding);

        PropertyProto.VectorProto.Builder builder = PropertyProto.VectorProto.newBuilder();
        for (int i = 0; i < embedding.getValues().length; i++) {
            builder.addValues(embedding.getValues()[i]);
        }
        return builder.setModelSignature(embedding.getModelSignature()).build();
    }

    private static void setEmptyProperty(@NonNull String propertyName,
            GenericDocument.@NonNull Builder<?> documentBuilder,
            @NonNull SchemaTypeConfigProto schema) {
        @AppSearchSchema.PropertyConfig.DataType int dataType = 0;
        for (int i = 0; i < schema.getPropertiesCount(); ++i) {
            if (propertyName.equals(schema.getProperties(i).getPropertyName())) {
                dataType = schema.getProperties(i).getDataType().getNumber();
                break;
            }
        }

        switch (dataType) {
            case AppSearchSchema.PropertyConfig.DATA_TYPE_STRING:
                documentBuilder.setPropertyString(propertyName, EMPTY_STRING_ARRAY);
                break;
            case AppSearchSchema.PropertyConfig.DATA_TYPE_LONG:
                documentBuilder.setPropertyLong(propertyName, EMPTY_LONG_ARRAY);
                break;
            case AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE:
                documentBuilder.setPropertyDouble(propertyName, EMPTY_DOUBLE_ARRAY);
                break;
            case AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN:
                documentBuilder.setPropertyBoolean(propertyName, EMPTY_BOOLEAN_ARRAY);
                break;
            case AppSearchSchema.PropertyConfig.DATA_TYPE_BYTES:
                documentBuilder.setPropertyBytes(propertyName, EMPTY_BYTES_ARRAY);
                break;
            case AppSearchSchema.PropertyConfig.DATA_TYPE_DOCUMENT:
                documentBuilder.setPropertyDocument(propertyName, EMPTY_DOCUMENT_ARRAY);
                break;
            case AppSearchSchema.PropertyConfig.DATA_TYPE_EMBEDDING:
                documentBuilder.setPropertyEmbedding(propertyName, EMPTY_EMBEDDING_ARRAY);
                break;
            default:
                throw new IllegalStateException("Unknown type of value: " + propertyName);
        }
    }
}
