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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.DocumentIndexingConfig;
import com.google.android.icing.proto.IntegerIndexingConfig;
import com.google.android.icing.proto.JoinableConfig;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.SchemaTypeConfigProtoOrBuilder;
import com.google.android.icing.proto.StringIndexingConfig;
import com.google.android.icing.proto.TermMatchType;

import java.util.List;

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
    @NonNull
    public static SchemaTypeConfigProto toSchemaTypeConfigProto(@NonNull AppSearchSchema schema,
            int version) {
        Preconditions.checkNotNull(schema);
        SchemaTypeConfigProto.Builder protoBuilder = SchemaTypeConfigProto.newBuilder()
                .setSchemaType(schema.getSchemaType())
                .setVersion(version);
        List<AppSearchSchema.PropertyConfig> properties = schema.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            PropertyConfigProto propertyProto = toPropertyConfigProto(properties.get(i));
            protoBuilder.addProperties(propertyProto);
        }
        protoBuilder.addAllParentTypes(schema.getParentTypes());
        return protoBuilder.build();
    }

    @NonNull
    private static PropertyConfigProto toPropertyConfigProto(
            @NonNull AppSearchSchema.PropertyConfig property) {
        Preconditions.checkNotNull(property);
        PropertyConfigProto.Builder builder = PropertyConfigProto.newBuilder()
                .setPropertyName(property.getName());

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

            // Set JoinableConfig only if it is joinable (i.e. joinableValueType is not NONE).
            if (stringProperty.getJoinableValueType()
                    != AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE) {
                JoinableConfig joinableConfig = JoinableConfig.newBuilder()
                        .setValueType(
                                convertJoinableValueTypeToProto(
                                        stringProperty.getJoinableValueType()))
                        // @exportToFramework:startStrip()
                        // Do not call this in framework as it will populate the proto field and
                        // fail comparison tests.
                        .setPropagateDelete(stringProperty.getDeletionPropagation())
                        // @exportToFramework:endStrip()
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
        }
        return builder.build();
    }

    /**
     * Converts a {@link SchemaTypeConfigProto} into an
     * {@link androidx.appsearch.app.AppSearchSchema}.
     */
    // TODO(b/284356266): Consider handling removal of schema name prefixes in this function.
    @NonNull
    public static AppSearchSchema toAppSearchSchema(@NonNull SchemaTypeConfigProtoOrBuilder proto) {
        Preconditions.checkNotNull(proto);
        AppSearchSchema.Builder builder =
                new AppSearchSchema.Builder(proto.getSchemaType());
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

    @NonNull
    private static AppSearchSchema.PropertyConfig toPropertyConfig(
            @NonNull PropertyConfigProto proto) {
        Preconditions.checkNotNull(proto);
        switch (proto.getDataType()) {
            case STRING:
                return toStringPropertyConfig(proto);
            case INT64:
                return toLongPropertyConfig(proto);
            case DOUBLE:
                return new AppSearchSchema.DoublePropertyConfig.Builder(proto.getPropertyName())
                        .setCardinality(proto.getCardinality().getNumber())
                        .build();
            case BOOLEAN:
                return new AppSearchSchema.BooleanPropertyConfig.Builder(proto.getPropertyName())
                        .setCardinality(proto.getCardinality().getNumber())
                        .build();
            case BYTES:
                return new AppSearchSchema.BytesPropertyConfig.Builder(proto.getPropertyName())
                        .setCardinality(proto.getCardinality().getNumber())
                        .build();
            case DOCUMENT:
                return toDocumentPropertyConfig(proto);
            default:
                throw new IllegalArgumentException("Invalid dataType: " + proto.getDataType());
        }
    }

    @NonNull
    private static AppSearchSchema.StringPropertyConfig toStringPropertyConfig(
            @NonNull PropertyConfigProto proto) {
        AppSearchSchema.StringPropertyConfig.Builder builder =
                new AppSearchSchema.StringPropertyConfig.Builder(proto.getPropertyName())
                        .setCardinality(proto.getCardinality().getNumber())
                        .setJoinableValueType(
                                convertJoinableValueTypeFromProto(
                                        proto.getJoinableConfig().getValueType()))
                        .setDeletionPropagation(proto.getJoinableConfig().getPropagateDelete())
                        .setTokenizerType(
                                proto.getStringIndexingConfig().getTokenizerType().getNumber());

        // Set indexingType
        TermMatchType.Code termMatchTypeProto = proto.getStringIndexingConfig().getTermMatchType();
        builder.setIndexingType(convertTermMatchTypeFromProto(termMatchTypeProto));

        return builder.build();
    }

    @NonNull
    private static AppSearchSchema.DocumentPropertyConfig toDocumentPropertyConfig(
            @NonNull PropertyConfigProto proto) {
        AppSearchSchema.DocumentPropertyConfig.Builder builder =
                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                proto.getPropertyName(), proto.getSchemaType())
                        .setCardinality(proto.getCardinality().getNumber())
                        .setShouldIndexNestedProperties(
                                proto.getDocumentIndexingConfig().getIndexNestedProperties());
        builder.addIndexableNestedProperties(
                proto.getDocumentIndexingConfig().getIndexableNestedPropertiesListList());
        return builder.build();
    }

    @NonNull
    private static AppSearchSchema.LongPropertyConfig toLongPropertyConfig(
            @NonNull PropertyConfigProto proto) {
        AppSearchSchema.LongPropertyConfig.Builder builder =
                new AppSearchSchema.LongPropertyConfig.Builder(proto.getPropertyName())
                        .setCardinality(proto.getCardinality().getNumber());

        // Set indexingType
        IntegerIndexingConfig.NumericMatchType.Code numericMatchTypeProto =
                proto.getIntegerIndexingConfig().getNumericMatchType();
        builder.setIndexingType(convertNumericMatchTypeFromProto(numericMatchTypeProto));

        return builder.build();
    }

    @NonNull
    private static JoinableConfig.ValueType.Code convertJoinableValueTypeToProto(
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
            @NonNull JoinableConfig.ValueType.Code joinableValueType) {
        switch (joinableValueType) {
            case NONE:
                return AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE;
            case QUALIFIED_ID:
                return AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID;
            default:
                // Avoid crashing in the 'read' path; we should try to interpret the document to the
                // extent possible.
                Log.w(TAG, "Invalid joinableValueType: " + joinableValueType.getNumber());
                return AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE;
        }
    }

    @NonNull
    private static TermMatchType.Code convertTermMatchTypeToProto(
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
    private static int convertTermMatchTypeFromProto(@NonNull TermMatchType.Code termMatchType) {
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

    @NonNull
    private static StringIndexingConfig.TokenizerType.Code convertTokenizerTypeToProto(
            @AppSearchSchema.StringPropertyConfig.TokenizerType int tokenizerType) {
        StringIndexingConfig.TokenizerType.Code tokenizerTypeProto =
                StringIndexingConfig.TokenizerType.Code.forNumber(tokenizerType);
        if (tokenizerTypeProto == null) {
            throw new IllegalArgumentException("Invalid tokenizerType: " + tokenizerType);
        }
        return tokenizerTypeProto;
    }

    @NonNull
    private static IntegerIndexingConfig.NumericMatchType.Code convertNumericMatchTypeToProto(
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
            @NonNull IntegerIndexingConfig.NumericMatchType.Code numericMatchType) {
        switch (numericMatchType) {
            case UNKNOWN:
                return AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE;
            case RANGE:
                return AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE;
            default:
                // Avoid crashing in the 'read' path; we should try to interpret the document to the
                // extent possible.
                Log.w(TAG, "Invalid indexingType: " + numericMatchType.getNumber());
                return AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE;
        }
    }
}
