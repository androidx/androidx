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

import android.util.Log;

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.DocumentIndexingConfig;
import com.google.android.icing.proto.EmbeddingIndexingConfig;
import com.google.android.icing.proto.IntegerIndexingConfig;
import com.google.android.icing.proto.JoinableConfig;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.SchemaTypeConfigProtoOrBuilder;
import com.google.android.icing.proto.StringIndexingConfig;
import com.google.android.icing.proto.TermMatchType;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Translates an {@link AppSearchSchema} into a {@link SchemaTypeConfigProto}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SchemaToProtoConverter {
    private static final String TAG = "AppSearchSchemaToProtoC";

    private SchemaToProtoConverter() {}

    /**
     * Converts an {@link androidx.appsearch.app.AppSearchSchema} into a
     * {@link SchemaTypeConfigProto}.
     */
    // TODO(b/284356266): Consider handling addition of schema name prefixes in this function.
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static @NonNull SchemaTypeConfigProto toSchemaTypeConfigProto(
            @NonNull AppSearchSchema schema, @Nullable Set<String> accountPropertyPaths,
            int version) {
        Preconditions.checkNotNull(schema);
        SchemaTypeConfigProto.Builder protoBuilder = SchemaTypeConfigProto.newBuilder()
                .setSchemaType(schema.getSchemaType())
                .setDescription(schema.getDescription())
                .setVersion(version);
        List<AppSearchSchema.PropertyConfig> properties = schema.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            PropertyConfigProto propertyProto = toPropertyConfigProto(properties.get(i));
            protoBuilder.addProperties(propertyProto);
        }
        protoBuilder.addAllParentTypes(schema.getParentTypes());
        if (accountPropertyPaths != null) {
            protoBuilder.addAllAccountProperties(accountPropertyPaths);
        }
        return protoBuilder.build();
    }

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static @NonNull PropertyConfigProto toPropertyConfigProto(
            AppSearchSchema.@NonNull PropertyConfig property) {
        Preconditions.checkNotNull(property);
        PropertyConfigProto.Builder builder = PropertyConfigProto.newBuilder()
                .setPropertyName(property.getName())
                .setDescription(property.getDescription());

        // Set dataType
        @AppSearchSchema.PropertyConfig.DataType int dataType = property.getDataType();
        PropertyConfigProto.DataType.Code dataTypeProto =
                PropertyConfigProto.DataType.Code.forNumber(dataType);
        if (dataTypeProto == null) {
            throw new IllegalArgumentException("Invalid dataType: " + dataType);
        }
        builder.setDataType(dataTypeProto);

        // Set cardinality
        @AppSearchSchema.PropertyConfig.Cardinality int cardinality = property.getCardinality();
        PropertyConfigProto.Cardinality.Code cardinalityProto =
                PropertyConfigProto.Cardinality.Code.forNumber(cardinality);
        if (cardinalityProto == null) {
            throw new IllegalArgumentException("Invalid cardinality: " + dataType);
        }
        builder.setCardinality(cardinalityProto);

        if (property instanceof AppSearchSchema.StringPropertyConfig) {
            AppSearchSchema.StringPropertyConfig stringProperty =
                    (AppSearchSchema.StringPropertyConfig) property;
            // No need to check against delete propagation type vs joinable value type here, because
            // the builder has already enforced the restriction.

            // Set JoinableConfig only if it is joinable (i.e. joinableValueType is not NONE).
            if (stringProperty.getJoinableValueType()
                    != AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE) {
                JoinableConfig joinableConfig = JoinableConfig.newBuilder()
                        .setValueType(
                                convertJoinableValueTypeToProto(
                                        stringProperty.getJoinableValueType()))
                        .setDeletePropagationType(
                                convertDeletePropagationTypeToProto(
                                        stringProperty.getDeletePropagationType()))
                        .build();
                builder.setJoinableConfig(joinableConfig);
            }
            StringIndexingConfig stringIndexingConfig = StringIndexingConfig.newBuilder()
                    .setTermMatchType(convertTermMatchTypeToProto(stringProperty.getIndexingType()))
                    .setTokenizerType(
                            convertTokenizerTypeToProto(stringProperty.getTokenizerType()))
                    .build();
            builder.setStringIndexingConfig(stringIndexingConfig);
        } else if (property instanceof AppSearchSchema.DocumentPropertyConfig) {
            AppSearchSchema.DocumentPropertyConfig documentProperty =
                    (AppSearchSchema.DocumentPropertyConfig) property;
            builder
                    .setSchemaType(documentProperty.getSchemaType())
                    .setDocumentIndexingConfig(
                            DocumentIndexingConfig.newBuilder()
                                    .setIndexNestedProperties(
                                            documentProperty.shouldIndexNestedProperties())
                                    .addAllIndexableNestedPropertiesList(
                                            documentProperty.getIndexableNestedProperties()));
        } else if (property instanceof AppSearchSchema.LongPropertyConfig) {
            AppSearchSchema.LongPropertyConfig longProperty =
                    (AppSearchSchema.LongPropertyConfig) property;
            // Set integer indexing config only if it is indexable (i.e. not INDEXING_TYPE_NONE).
            if (longProperty.getIndexingType()
                    != AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE) {
                IntegerIndexingConfig integerIndexingConfig = IntegerIndexingConfig.newBuilder()
                        .setNumericMatchType(
                                convertNumericMatchTypeToProto(longProperty.getIndexingType()))
                        .build();
                builder.setIntegerIndexingConfig(integerIndexingConfig);
            }
            builder.setScorableType(toScorableTypeCode(longProperty.isScoringEnabled()));
        } else if (property instanceof AppSearchSchema.EmbeddingPropertyConfig) {
            AppSearchSchema.EmbeddingPropertyConfig embeddingProperty =
                    (AppSearchSchema.EmbeddingPropertyConfig) property;
            // Set embedding indexing config only if it is indexable (i.e. not INDEXING_TYPE_NONE).
            // Non-indexable embedding property only requires to builder.setDataType, without the
            // need to set an EmbeddingIndexingConfig.
            if (embeddingProperty.getIndexingType()
                    != AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE) {
                EmbeddingIndexingConfig embeddingIndexingConfig =
                        EmbeddingIndexingConfig.newBuilder()
                                .setEmbeddingIndexingType(
                                        convertEmbeddingIndexingTypeToProto(
                                                embeddingProperty.getIndexingType()))
                                .setQuantizationType(
                                        convertEmbeddingQuantizationTypeToProto(
                                                embeddingProperty.getQuantizationType()))
                                .build();
                builder.setEmbeddingIndexingConfig(embeddingIndexingConfig);
            }
        } else if (property instanceof AppSearchSchema.DoublePropertyConfig) {
            AppSearchSchema.DoublePropertyConfig doubleProperty =
                    (AppSearchSchema.DoublePropertyConfig) property;
            builder.setScorableType(toScorableTypeCode(doubleProperty.isScoringEnabled()));
        } else if (property instanceof AppSearchSchema.BooleanPropertyConfig) {
            AppSearchSchema.BooleanPropertyConfig booleanProperty =
                    (AppSearchSchema.BooleanPropertyConfig) property;
            builder.setScorableType(toScorableTypeCode(booleanProperty.isScoringEnabled()));
        }
        return builder.build();
    }

    /**
     * Converts a {@link SchemaTypeConfigProto} into an
     * {@link androidx.appsearch.app.AppSearchSchema}.
     */
    // TODO(b/284356266): Consider handling removal of schema name prefixes in this function.
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static @NonNull AppSearchSchema toAppSearchSchema(
            @NonNull SchemaTypeConfigProtoOrBuilder proto) {
        Preconditions.checkNotNull(proto);
        AppSearchSchema.Builder builder =
                new AppSearchSchema.Builder(proto.getSchemaType());
        builder.setDescription(proto.getDescription());
        List<PropertyConfigProto> properties = proto.getPropertiesList();
        for (int i = 0; i < properties.size(); i++) {
            AppSearchSchema.PropertyConfig propertyConfig = toPropertyConfig(properties.get(i));
            builder.addProperty(propertyConfig);
        }
        List<String> parentTypes = proto.getParentTypesList();
        for (int i = 0; i < parentTypes.size(); i++) {
            builder.addParentType(parentTypes.get(i));
        }
        return builder.build();
    }

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static AppSearchSchema.@NonNull PropertyConfig toPropertyConfig(
            @NonNull PropertyConfigProto proto) {
        Preconditions.checkNotNull(proto);
        switch (proto.getDataType()) {
            case STRING:
                return toStringPropertyConfig(proto);
            case INT64:
                return toLongPropertyConfig(proto);
            case DOUBLE:
                return new AppSearchSchema.DoublePropertyConfig.Builder(proto.getPropertyName())
                        .setDescription(proto.getDescription())
                        .setCardinality(proto.getCardinality().getNumber())
                        .setScoringEnabled(
                                proto.getScorableType() ==
                                        PropertyConfigProto.ScorableType.Code.ENABLED)
                        .build();
            case BOOLEAN:
                return new AppSearchSchema.BooleanPropertyConfig.Builder(proto.getPropertyName())
                        .setDescription(proto.getDescription())
                        .setCardinality(proto.getCardinality().getNumber())
                        .setScoringEnabled(
                                proto.getScorableType() ==
                                        PropertyConfigProto.ScorableType.Code.ENABLED)
                        .build();
            case BYTES:
                return new AppSearchSchema.BytesPropertyConfig.Builder(proto.getPropertyName())
                        .setDescription(proto.getDescription())
                        .setCardinality(proto.getCardinality().getNumber())
                        .build();
            case DOCUMENT:
                return toDocumentPropertyConfig(proto);
            case VECTOR:
                return toEmbeddingPropertyConfig(proto);
            case BLOB_HANDLE:
                return new AppSearchSchema.BlobHandlePropertyConfig.Builder(proto.getPropertyName())
                        .setDescription(proto.getDescription())
                        .setCardinality(proto.getCardinality().getNumber())
                        .build();
            default:
                throw new IllegalArgumentException(
                        "Invalid dataType code: " + proto.getDataType().getNumber());
        }
    }

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static AppSearchSchema.@NonNull StringPropertyConfig toStringPropertyConfig(
            @NonNull PropertyConfigProto proto) {
        AppSearchSchema.StringPropertyConfig.Builder builder =
                new AppSearchSchema.StringPropertyConfig.Builder(proto.getPropertyName())
                        .setDescription(proto.getDescription())
                        .setCardinality(proto.getCardinality().getNumber())
                        .setJoinableValueType(
                                convertJoinableValueTypeFromProto(
                                        proto.getJoinableConfig().getValueType()))
                        .setDeletePropagationType(
                                convertDeletePropagationTypeFromProto(
                                        proto.getJoinableConfig().getDeletePropagationType()))
                        .setTokenizerType(
                                proto.getStringIndexingConfig().getTokenizerType().getNumber());

        // Set indexingType
        TermMatchType.Code termMatchTypeProto = proto.getStringIndexingConfig().getTermMatchType();
        builder.setIndexingType(convertTermMatchTypeFromProto(termMatchTypeProto));

        return builder.build();
    }

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static AppSearchSchema.@NonNull DocumentPropertyConfig toDocumentPropertyConfig(
            @NonNull PropertyConfigProto proto) {
        AppSearchSchema.DocumentPropertyConfig.Builder builder =
                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                proto.getPropertyName(), proto.getSchemaType())
                        .setDescription(proto.getDescription())
                        .setCardinality(proto.getCardinality().getNumber())
                        .setShouldIndexNestedProperties(
                                proto.getDocumentIndexingConfig().getIndexNestedProperties());
        builder.addIndexableNestedProperties(
                proto.getDocumentIndexingConfig().getIndexableNestedPropertiesListList());
        return builder.build();
    }

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static AppSearchSchema.@NonNull LongPropertyConfig toLongPropertyConfig(
            @NonNull PropertyConfigProto proto) {
        AppSearchSchema.LongPropertyConfig.Builder builder =
                new AppSearchSchema.LongPropertyConfig.Builder(proto.getPropertyName())
                        .setDescription(proto.getDescription())
                        .setCardinality(proto.getCardinality().getNumber())
                        .setScoringEnabled(
                                proto.getScorableType() ==
                                        PropertyConfigProto.ScorableType.Code.ENABLED);
        // Set indexingType
        IntegerIndexingConfig.NumericMatchType.Code numericMatchTypeProto =
                proto.getIntegerIndexingConfig().getNumericMatchType();
        builder.setIndexingType(convertNumericMatchTypeFromProto(numericMatchTypeProto));

        return builder.build();
    }

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static AppSearchSchema.@NonNull EmbeddingPropertyConfig toEmbeddingPropertyConfig(
            @NonNull PropertyConfigProto proto) {
        AppSearchSchema.EmbeddingPropertyConfig.Builder builder =
                new AppSearchSchema.EmbeddingPropertyConfig.Builder(proto.getPropertyName())
                        .setDescription(proto.getDescription())
                        .setCardinality(proto.getCardinality().getNumber());

        // Set indexingType
        EmbeddingIndexingConfig.EmbeddingIndexingType.Code embeddingIndexingType =
                proto.getEmbeddingIndexingConfig().getEmbeddingIndexingType();
        builder.setIndexingType(convertEmbeddingIndexingTypeFromProto(embeddingIndexingType));

        // Set quantizationType
        if (embeddingIndexingType != EmbeddingIndexingConfig.EmbeddingIndexingType.Code.UNKNOWN) {
            EmbeddingIndexingConfig.QuantizationType.Code embeddingQuantizationType =
                    proto.getEmbeddingIndexingConfig().getQuantizationType();
            builder.setQuantizationType(
                    convertEmbeddingQuantizationTypeTypeFromProto(embeddingQuantizationType));
        }

        return builder.build();
    }

    private static JoinableConfig.ValueType.@NonNull Code convertJoinableValueTypeToProto(
            @AppSearchSchema.StringPropertyConfig.JoinableValueType int joinableValueType) {
        switch (joinableValueType) {
            case AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE:
                return JoinableConfig.ValueType.Code.NONE;
            case AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID:
                return JoinableConfig.ValueType.Code.QUALIFIED_ID;
            default:
                throw new IllegalArgumentException(
                        "Invalid joinableValueType: " + joinableValueType);
        }
    }

    @AppSearchSchema.StringPropertyConfig.JoinableValueType
    private static int convertJoinableValueTypeFromProto(
            JoinableConfig.ValueType.@NonNull Code joinableValueType) {
        switch (joinableValueType) {
            case NONE:
                return AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE;
            case QUALIFIED_ID:
                return AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID;
        }
        // Avoid crashing in the 'read' path; we should try to interpret the document to the
        // extent possible.
        Log.w(TAG, "Invalid joinableValueType: " + joinableValueType.getNumber());
        return AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE;
    }

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static JoinableConfig.DeletePropagationType.@NonNull Code
            convertDeletePropagationTypeToProto(
                    @AppSearchSchema.StringPropertyConfig.DeletePropagationType
                    int deletePropagationType) {
        switch (deletePropagationType) {
            case AppSearchSchema.StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE:
                return JoinableConfig.DeletePropagationType.Code.NONE;
            case AppSearchSchema.StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM:
                return JoinableConfig.DeletePropagationType.Code.PROPAGATE_FROM;
            default:
                throw new IllegalArgumentException(
                        "Invalid deletePropagationType: " + deletePropagationType);
        }
    }

    @AppSearchSchema.StringPropertyConfig.DeletePropagationType
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static int convertDeletePropagationTypeFromProto(
            JoinableConfig.DeletePropagationType.@NonNull Code deletePropagationType) {
        switch (deletePropagationType) {
            case NONE:
                return AppSearchSchema.StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE;
            case PROPAGATE_FROM:
                return AppSearchSchema.StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM;
        }
        // Avoid crashing in the 'read' path; we should try to interpret the schema to the
        // extent possible.
        Log.w(TAG, "Invalid deletePropagationType: " + deletePropagationType.getNumber());
        return AppSearchSchema.StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE;
    }

    private static TermMatchType.@NonNull Code convertTermMatchTypeToProto(
            @AppSearchSchema.StringPropertyConfig.IndexingType int indexingType) {
        switch (indexingType) {
            case AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE:
                return TermMatchType.Code.UNKNOWN;
            case AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS:
                return TermMatchType.Code.EXACT_ONLY;
            case AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES:
                return TermMatchType.Code.PREFIX;
            default:
                throw new IllegalArgumentException("Invalid indexingType: " + indexingType);
        }
    }

    @AppSearchSchema.StringPropertyConfig.IndexingType
    private static int convertTermMatchTypeFromProto(TermMatchType.@NonNull Code termMatchType) {
        switch (termMatchType) {
            case UNKNOWN:
                return AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE;
            case EXACT_ONLY:
                return AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS;
            case PREFIX:
                return AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES;
            default:
                // Avoid crashing in the 'read' path; we should try to interpret the document to the
                // extent possible.
                Log.w(TAG, "Invalid indexingType: " + termMatchType.getNumber());
                return AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE;
        }
    }

    private static StringIndexingConfig.TokenizerType.@NonNull Code convertTokenizerTypeToProto(
            @AppSearchSchema.StringPropertyConfig.TokenizerType int tokenizerType) {
        StringIndexingConfig.TokenizerType.Code tokenizerTypeProto =
                StringIndexingConfig.TokenizerType.Code.forNumber(tokenizerType);
        if (tokenizerTypeProto == null) {
            throw new IllegalArgumentException("Invalid tokenizerType: " + tokenizerType);
        }
        return tokenizerTypeProto;
    }

    private static IntegerIndexingConfig.NumericMatchType.@NonNull Code
            convertNumericMatchTypeToProto(
                    @AppSearchSchema.LongPropertyConfig.IndexingType int indexingType) {
        switch (indexingType) {
            case AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE:
                return IntegerIndexingConfig.NumericMatchType.Code.UNKNOWN;
            case AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE:
                return IntegerIndexingConfig.NumericMatchType.Code.RANGE;
            default:
                throw new IllegalArgumentException("Invalid indexingType: " + indexingType);
        }
    }

    @AppSearchSchema.LongPropertyConfig.IndexingType
    private static int convertNumericMatchTypeFromProto(
            IntegerIndexingConfig.NumericMatchType.@NonNull Code numericMatchType) {
        switch (numericMatchType) {
            case UNKNOWN:
                return AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE;
            case RANGE:
                return AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE;
        }
        // Avoid crashing in the 'read' path; we should try to interpret the document to the
        // extent possible.
        Log.w(TAG, "Invalid indexingType: " + numericMatchType.getNumber());
        return AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE;
    }

    private static EmbeddingIndexingConfig.EmbeddingIndexingType.@NonNull Code
            convertEmbeddingIndexingTypeToProto(
            @AppSearchSchema.EmbeddingPropertyConfig.IndexingType int indexingType) {
        switch (indexingType) {
            case AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE:
                return EmbeddingIndexingConfig.EmbeddingIndexingType.Code.UNKNOWN;
            case AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY:
                return EmbeddingIndexingConfig.EmbeddingIndexingType.Code.LINEAR_SEARCH;
            default:
                throw new IllegalArgumentException("Invalid indexingType: " + indexingType);
        }
    }

    @AppSearchSchema.EmbeddingPropertyConfig.IndexingType
    private static int convertEmbeddingIndexingTypeFromProto(
            EmbeddingIndexingConfig.EmbeddingIndexingType.@NonNull Code indexingType) {
        switch (indexingType) {
            case UNKNOWN:
                return AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE;
            case LINEAR_SEARCH:
                return AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY;
        }
        // Avoid crashing in the 'read' path; we should try to interpret the document to the
        // extent possible.
        Log.w(TAG, "Invalid indexingType: " + indexingType.getNumber());
        return AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE;
    }

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static EmbeddingIndexingConfig.QuantizationType.@NonNull Code
            convertEmbeddingQuantizationTypeToProto(
            @AppSearchSchema.EmbeddingPropertyConfig.QuantizationType int quantizationType) {
        switch (quantizationType) {
            case AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE:
                return EmbeddingIndexingConfig.QuantizationType.Code.NONE;
            case AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_8_BIT:
                return EmbeddingIndexingConfig.QuantizationType.Code.QUANTIZE_8_BIT;
            default:
                throw new IllegalArgumentException("Invalid quantizationType: " + quantizationType);
        }
    }

    @AppSearchSchema.EmbeddingPropertyConfig.QuantizationType
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static int convertEmbeddingQuantizationTypeTypeFromProto(
            EmbeddingIndexingConfig.QuantizationType.@NonNull Code quantizationType) {
        switch (quantizationType) {
            case NONE:
                return AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE;
            case QUANTIZE_8_BIT:
                return AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_8_BIT;
            default:
                // Avoid crashing in the 'read' path; we should try to interpret the document to the
                // extent possible.
                Log.w(TAG, "Invalid quantizationType: " + quantizationType.getNumber());
                return AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE;
        }
    }

    private static PropertyConfigProto.ScorableType.Code toScorableTypeCode(
            boolean isScoringEnabled) {
        if (isScoringEnabled) {
            return PropertyConfigProto.ScorableType.Code.ENABLED;
        } else {
            return PropertyConfigProto.ScorableType.Code.DISABLED;
        }
    }
}
