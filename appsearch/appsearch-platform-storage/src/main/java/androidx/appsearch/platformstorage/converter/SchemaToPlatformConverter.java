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

import static android.app.appsearch.AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE;
import static android.app.appsearch.AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN;
import static android.app.appsearch.AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_RFC822;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresExtension;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.core.os.BuildCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;

/**
 * Translates a jetpack {@link AppSearchSchema} into a platform
 * {@link android.app.appsearch.AppSearchSchema}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class SchemaToPlatformConverter {
    private SchemaToPlatformConverter() {
    }

    /**
     * Translates a jetpack {@link AppSearchSchema} into a platform
     * {@link android.app.appsearch.AppSearchSchema}.
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static android.app.appsearch.@NonNull AppSearchSchema toPlatformSchema(
            @NonNull AppSearchSchema jetpackSchema) {
        Preconditions.checkNotNull(jetpackSchema);
        android.app.appsearch.AppSearchSchema.Builder platformBuilder =
                new android.app.appsearch.AppSearchSchema.Builder(jetpackSchema.getSchemaType());
        if (!jetpackSchema.getDescription().isEmpty()) {
            // TODO(b/326987971): Remove this once description becomes available.
            throw new UnsupportedOperationException(Features.SCHEMA_SET_DESCRIPTION
                    + " is not available on this AppSearch implementation.");
        }
        if (!jetpackSchema.getParentTypes().isEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                throw new UnsupportedOperationException(Features.SCHEMA_ADD_PARENT_TYPE
                        + " is not available on this AppSearch implementation.");
            }
            List<String> parentTypes = jetpackSchema.getParentTypes();
            for (int i = 0; i < parentTypes.size(); i++) {
                ApiHelperForV.addParentType(platformBuilder, parentTypes.get(i));
            }
        }
        List<AppSearchSchema.PropertyConfig> properties = jetpackSchema.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            android.app.appsearch.AppSearchSchema.PropertyConfig platformProperty =
                    toPlatformProperty(properties.get(i));
            platformBuilder.addProperty(platformProperty);
        }
        return platformBuilder.build();
    }

    /**
     * Translates a platform {@link android.app.appsearch.AppSearchSchema} to a jetpack
     * {@link AppSearchSchema}.
     */
    public static @NonNull AppSearchSchema toJetpackSchema(
            android.app.appsearch.@NonNull AppSearchSchema platformSchema) {
        Preconditions.checkNotNull(platformSchema);
        AppSearchSchema.Builder jetpackBuilder =
                new AppSearchSchema.Builder(platformSchema.getSchemaType());
        List<android.app.appsearch.AppSearchSchema.PropertyConfig> properties =
                platformSchema.getProperties();
        // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
        // available in platform.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            List<String> parentTypes = ApiHelperForV.getParentTypes(platformSchema);
            for (int i = 0; i < parentTypes.size(); i++) {
                jetpackBuilder.addParentType(parentTypes.get(i));
            }
        }
        for (int i = 0; i < properties.size(); i++) {
            AppSearchSchema.PropertyConfig jetpackProperty = toJetpackProperty(properties.get(i));
            jetpackBuilder.addProperty(jetpackProperty);
        }
        return jetpackBuilder.build();
    }

    // Most stringProperty.get calls cause WrongConstant lint errors because the methods are not
    // defined as returning the same constants as the corresponding setter expects, but they do
    @SuppressLint("WrongConstant")
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static android.app.appsearch.AppSearchSchema.@NonNull PropertyConfig toPlatformProperty(
            AppSearchSchema.@NonNull PropertyConfig jetpackProperty) {
        Preconditions.checkNotNull(jetpackProperty);
        if (!jetpackProperty.getDescription().isEmpty()) {
            // TODO(b/326987971): Remove this once description becomes available.
            throw new UnsupportedOperationException(Features.SCHEMA_SET_DESCRIPTION
                    + " is not available on this AppSearch implementation.");
        }
        if (jetpackProperty instanceof AppSearchSchema.StringPropertyConfig) {
            AppSearchSchema.StringPropertyConfig stringProperty =
                    (AppSearchSchema.StringPropertyConfig) jetpackProperty;
            android.app.appsearch.AppSearchSchema.StringPropertyConfig.Builder platformBuilder =
                    new android.app.appsearch.AppSearchSchema.StringPropertyConfig.Builder(
                            stringProperty.getName())
                            .setCardinality(stringProperty.getCardinality())
                            .setIndexingType(stringProperty.getIndexingType())
                            .setTokenizerType(stringProperty.getTokenizerType());

            @AppSearchSchema.StringPropertyConfig.TokenizerType int maxSupportedTokenizerType =
                    BuildCompat.T_EXTENSION_INT >= AppSearchVersionUtil.TExtensionVersions.U_BASE ?
                            TOKENIZER_TYPE_RFC822 : TOKENIZER_TYPE_PLAIN;
            Preconditions.checkArgumentInRange(stringProperty.getTokenizerType(),
                    TOKENIZER_TYPE_NONE, maxSupportedTokenizerType, "tokenizerType");

            // Check joinable value type.
            if (stringProperty.getJoinableValueType()
                    == AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID) {
                if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.U_BASE) {
                    throw new UnsupportedOperationException(
                            "StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID is not supported"
                                    + " on this AppSearch implementation.");
                }
                ApiHelperForSdkExtensionUBase.setJoinableValueType(platformBuilder,
                        stringProperty.getJoinableValueType());
            }

            // Check delete propagation type.
            if (stringProperty.getDeletePropagationType()
                    != AppSearchSchema.StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE) {
                // TODO(b/376913014): add isAtLeastC check to allow
                //  DELETE_PROPAGATION_TYPE_PROPAGATE_FROM after Android C.
                throw new UnsupportedOperationException(
                        "StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM is not"
                                + " supported on this AppSearch implementation.");
            }
            return platformBuilder.build();
        } else if (jetpackProperty instanceof AppSearchSchema.LongPropertyConfig) {
            AppSearchSchema.LongPropertyConfig longProperty =
                    (AppSearchSchema.LongPropertyConfig) jetpackProperty;
            android.app.appsearch.AppSearchSchema.LongPropertyConfig.Builder longPropertyBuilder =
                    new android.app.appsearch.AppSearchSchema.LongPropertyConfig.Builder(
                            jetpackProperty.getName())
                            .setCardinality(jetpackProperty.getCardinality());
            if (longProperty.getIndexingType()
                    == AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE) {
                if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.U_BASE) {
                    throw new UnsupportedOperationException(
                            "LongProperty.INDEXING_TYPE_RANGE is not supported on this AppSearch "
                                    + "implementation.");
                }
                ApiHelperForSdkExtensionUBase.setIndexingType(
                        longPropertyBuilder, longProperty.getIndexingType());
            }
            if (longProperty.isScoringEnabled()) {
                if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                    throw new UnsupportedOperationException(
                            Features.SCHEMA_SCORABLE_PROPERTY_CONFIG
                                    + " is not available on this AppSearch implementation.");
                }
                ApiHelperForSdkExtensionBBase.setScoringEnabled(
                        longPropertyBuilder, longProperty.isScoringEnabled());
            }
            return longPropertyBuilder.build();
        } else if (jetpackProperty instanceof AppSearchSchema.DoublePropertyConfig) {
            AppSearchSchema.DoublePropertyConfig doubleProperty =
                    (AppSearchSchema.DoublePropertyConfig) jetpackProperty;
            android.app.appsearch.AppSearchSchema.DoublePropertyConfig.Builder
                    doublePropertyBuilder =
                    new android.app.appsearch.AppSearchSchema.DoublePropertyConfig.Builder(
                            jetpackProperty.getName())
                            .setCardinality(jetpackProperty.getCardinality());
            if (doubleProperty.isScoringEnabled()) {
                if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                    throw new UnsupportedOperationException(
                            Features.SCHEMA_SCORABLE_PROPERTY_CONFIG
                                    + " is not available on this AppSearch implementation.");
                }
                ApiHelperForSdkExtensionBBase.setScoringEnabled(
                        doublePropertyBuilder, doubleProperty.isScoringEnabled());
            }
            return doublePropertyBuilder.build();
        } else if (jetpackProperty instanceof AppSearchSchema.BooleanPropertyConfig) {
            AppSearchSchema.BooleanPropertyConfig booleanProperty =
                    (AppSearchSchema.BooleanPropertyConfig) jetpackProperty;
            android.app.appsearch.AppSearchSchema.BooleanPropertyConfig.Builder
                    booleanPropertyBuilder =
                    new android.app.appsearch.AppSearchSchema.BooleanPropertyConfig.Builder(
                            jetpackProperty.getName())
                            .setCardinality(jetpackProperty.getCardinality());
            if (booleanProperty.isScoringEnabled()) {
                if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                    throw new UnsupportedOperationException(
                            Features.SCHEMA_SCORABLE_PROPERTY_CONFIG
                                    + " is not available on this AppSearch implementation.");
                }
                ApiHelperForSdkExtensionBBase.setScoringEnabled(
                        booleanPropertyBuilder, booleanProperty.isScoringEnabled());
            }
            return booleanPropertyBuilder.build();
        } else if (jetpackProperty instanceof AppSearchSchema.BytesPropertyConfig) {
            return new android.app.appsearch.AppSearchSchema.BytesPropertyConfig.Builder(
                    jetpackProperty.getName())
                    .setCardinality(jetpackProperty.getCardinality())
                    .build();
        } else if (jetpackProperty instanceof AppSearchSchema.DocumentPropertyConfig) {
            AppSearchSchema.DocumentPropertyConfig documentProperty =
                    (AppSearchSchema.DocumentPropertyConfig) jetpackProperty;
            android.app.appsearch.AppSearchSchema.DocumentPropertyConfig.Builder platformBuilder =
                    new android.app.appsearch.AppSearchSchema.DocumentPropertyConfig.Builder(
                            documentProperty.getName(), documentProperty.getSchemaType())
                            .setCardinality(documentProperty.getCardinality())
                            .setShouldIndexNestedProperties(
                                    documentProperty.shouldIndexNestedProperties());
            if (!documentProperty.getIndexableNestedProperties().isEmpty()) {
                if (BuildCompat.T_EXTENSION_INT
                        < AppSearchVersionUtil.TExtensionVersions.M2023_11) {
                    throw new UnsupportedOperationException(
                            "DocumentPropertyConfig.addIndexableNestedProperties is not supported "
                                    + "on this AppSearch implementation.");
                }
                ApiHelperForSdkExtensionM202311.addIndexableNestedProperties(platformBuilder,
                        documentProperty.getIndexableNestedProperties());
            }
            return platformBuilder.build();
        } else if (jetpackProperty instanceof AppSearchSchema.EmbeddingPropertyConfig) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
                throw new UnsupportedOperationException(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG
                        + " is not available on this AppSearch implementation.");
            }
            AppSearchSchema.EmbeddingPropertyConfig embeddingProperty =
                    (AppSearchSchema.EmbeddingPropertyConfig) jetpackProperty;
            return ApiHelperForB.createPlatformEmbeddingPropertyConfig(embeddingProperty);
        } else if (jetpackProperty instanceof AppSearchSchema.BlobHandlePropertyConfig) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
                throw new UnsupportedOperationException(Features.SCHEMA_BLOB_HANDLE
                        + " is not available on this AppSearch implementation.");
            }
            AppSearchSchema.BlobHandlePropertyConfig blobHandleProperty =
                    (AppSearchSchema.BlobHandlePropertyConfig) jetpackProperty;
            return ApiHelperForB.createPlatformBlobHandlePropertyConfig(blobHandleProperty);
        } else {
            throw new IllegalArgumentException(
                    "Invalid dataType: " + jetpackProperty.getDataType());
        }
    }

    // Most stringProperty.get calls cause WrongConstant lint errors because the methods are not
    // defined as returning the same constants as the corresponding setter expects, but they do
    @SuppressLint({"WrongConstant", "NewApi"}) // EmbeddingPropertyConfig incorrectly flagged
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static AppSearchSchema.@NonNull PropertyConfig toJetpackProperty(
            android.app.appsearch.AppSearchSchema.@NonNull PropertyConfig platformProperty) {
        Preconditions.checkNotNull(platformProperty);
        if (platformProperty
                instanceof android.app.appsearch.AppSearchSchema.StringPropertyConfig) {
            android.app.appsearch.AppSearchSchema.StringPropertyConfig stringProperty =
                    (android.app.appsearch.AppSearchSchema.StringPropertyConfig) platformProperty;
            AppSearchSchema.StringPropertyConfig.Builder jetpackBuilder =
                    new AppSearchSchema.StringPropertyConfig.Builder(stringProperty.getName())
                            .setCardinality(stringProperty.getCardinality())
                            .setIndexingType(stringProperty.getIndexingType())
                            .setTokenizerType(stringProperty.getTokenizerType());
            if (BuildCompat.T_EXTENSION_INT >= AppSearchVersionUtil.TExtensionVersions.U_BASE) {
                jetpackBuilder.setJoinableValueType(
                        ApiHelperForSdkExtensionUBase.getJoinableValueType(stringProperty));
            }
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            // available in platform.
            return jetpackBuilder.build();
        } else if (platformProperty
                instanceof android.app.appsearch.AppSearchSchema.LongPropertyConfig) {
            android.app.appsearch.AppSearchSchema.LongPropertyConfig longProperty =
                    (android.app.appsearch.AppSearchSchema.LongPropertyConfig) platformProperty;
            AppSearchSchema.LongPropertyConfig.Builder jetpackBuilder =
                    new AppSearchSchema.LongPropertyConfig.Builder(longProperty.getName())
                            .setCardinality(longProperty.getCardinality());
            if (BuildCompat.T_EXTENSION_INT >= AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                jetpackBuilder.setScoringEnabled(
                        ApiHelperForSdkExtensionBBase.getScoringEnabled(longProperty));
            }
            if (BuildCompat.T_EXTENSION_INT >= AppSearchVersionUtil.TExtensionVersions.U_BASE) {
                jetpackBuilder.setIndexingType(
                        ApiHelperForSdkExtensionUBase.getIndexingType(longProperty));
            }
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            // available in platform.
            return jetpackBuilder.build();
        } else if (platformProperty
                instanceof android.app.appsearch.AppSearchSchema.DoublePropertyConfig) {
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            // available in platform.
            android.app.appsearch.AppSearchSchema.DoublePropertyConfig doubleProperty =
                    (android.app.appsearch.AppSearchSchema.DoublePropertyConfig) platformProperty;
            AppSearchSchema.DoublePropertyConfig.Builder jetpackBuilder =
                    new AppSearchSchema.DoublePropertyConfig.Builder(platformProperty.getName())
                            .setCardinality(platformProperty.getCardinality());
            if (BuildCompat.T_EXTENSION_INT >= AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                jetpackBuilder.setScoringEnabled(
                        ApiHelperForSdkExtensionBBase.getScoringEnabled(doubleProperty));
            }

            return jetpackBuilder.build();
        } else if (platformProperty
                instanceof android.app.appsearch.AppSearchSchema.BooleanPropertyConfig) {
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            // available in platform.
            android.app.appsearch.AppSearchSchema.BooleanPropertyConfig booleanProperty =
                    (android.app.appsearch.AppSearchSchema.BooleanPropertyConfig) platformProperty;
            AppSearchSchema.BooleanPropertyConfig.Builder jetpackBuilder =
                    new AppSearchSchema.BooleanPropertyConfig.Builder(platformProperty.getName())
                            .setCardinality(platformProperty.getCardinality());
            if (BuildCompat.T_EXTENSION_INT >= AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                jetpackBuilder.setScoringEnabled(
                        ApiHelperForSdkExtensionBBase.getScoringEnabled(booleanProperty));
            }
            return jetpackBuilder.build();
        } else if (platformProperty
                instanceof android.app.appsearch.AppSearchSchema.BytesPropertyConfig) {
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            // available in platform.
            return new AppSearchSchema.BytesPropertyConfig.Builder(platformProperty.getName())
                    .setCardinality(platformProperty.getCardinality())
                    .build();
        } else if (platformProperty
                instanceof android.app.appsearch.AppSearchSchema.DocumentPropertyConfig) {
            android.app.appsearch.AppSearchSchema.DocumentPropertyConfig documentProperty =
                    (android.app.appsearch.AppSearchSchema.DocumentPropertyConfig) platformProperty;
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            // available in platform.
            AppSearchSchema.DocumentPropertyConfig.Builder jetpackBuilder =
                    new AppSearchSchema.DocumentPropertyConfig.Builder(
                            documentProperty.getName(),
                            documentProperty.getSchemaType())
                            .setCardinality(documentProperty.getCardinality())
                            .setShouldIndexNestedProperties(
                                    documentProperty.shouldIndexNestedProperties());
            if (BuildCompat.T_EXTENSION_INT >= AppSearchVersionUtil.TExtensionVersions.V_BASE) {
                List<String> indexableNestedProperties = ApiHelperForSdkExtensionVBase
                        .getIndexableNestedProperties(documentProperty);
                jetpackBuilder.addIndexableNestedProperties(indexableNestedProperties);
            }
            return jetpackBuilder.build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && platformProperty
                instanceof android.app.appsearch.AppSearchSchema.EmbeddingPropertyConfig) {
            android.app.appsearch.AppSearchSchema.EmbeddingPropertyConfig embeddingProperty =
                    (android.app.appsearch.AppSearchSchema
                            .EmbeddingPropertyConfig) platformProperty;
            return ApiHelperForB.createJetpackEmbeddingPropertyConfig(embeddingProperty);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && platformProperty
                instanceof android.app.appsearch.AppSearchSchema.BlobHandlePropertyConfig) {
            android.app.appsearch.AppSearchSchema.BlobHandlePropertyConfig blobHandleProperty =
                    (android.app.appsearch.AppSearchSchema
                            .BlobHandlePropertyConfig) platformProperty;
            return ApiHelperForB.createJetpackBlobHandlePropertyConfig(blobHandleProperty);
        } else {
            throw new IllegalArgumentException(
                    "Invalid property type " + platformProperty.getClass()
                            + ": " + platformProperty);
        }
    }

    @SuppressLint("NewApi")
    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU,
            version = AppSearchVersionUtil.TExtensionVersions.U_BASE)
    private static class ApiHelperForSdkExtensionUBase {
        private ApiHelperForSdkExtensionUBase() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setJoinableValueType(
                android.app.appsearch.AppSearchSchema.StringPropertyConfig.Builder builder,
                @AppSearchSchema.StringPropertyConfig.JoinableValueType int joinableValueType) {
            builder.setJoinableValueType(joinableValueType);
        }

        // Most stringProperty.get calls cause WrongConstant lint errors because the methods are not
        // defined as returning the same constants as the corresponding setter expects, but they do
        @SuppressLint("WrongConstant")
        @DoNotInline
        @AppSearchSchema.StringPropertyConfig.JoinableValueType
        static int getJoinableValueType(
                android.app.appsearch.AppSearchSchema.StringPropertyConfig stringPropertyConfig) {
            return stringPropertyConfig.getJoinableValueType();
        }

        @DoNotInline
        static void setIndexingType(
                android.app.appsearch.AppSearchSchema.LongPropertyConfig.Builder builder,
                @AppSearchSchema.LongPropertyConfig.IndexingType int longIndexingType) {
            builder.setIndexingType(longIndexingType);
        }

        // Most LongProperty.get calls cause WrongConstant lint errors because the methods are not
        // defined as returning the same constants as the corresponding setter expects, but they do
        @SuppressLint("WrongConstant")
        @DoNotInline
        @AppSearchSchema.LongPropertyConfig.IndexingType
        static int getIndexingType(
                android.app.appsearch.AppSearchSchema.LongPropertyConfig longPropertyConfig) {
            return longPropertyConfig.getIndexingType();
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU,
            version = AppSearchVersionUtil.TExtensionVersions.M2023_11)
    private static class ApiHelperForSdkExtensionM202311 {
        private ApiHelperForSdkExtensionM202311() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void addIndexableNestedProperties(
                android.app.appsearch.AppSearchSchema.DocumentPropertyConfig.Builder
                        platformBuilder,
                @NonNull Collection<String> indexableNestedProperties) {
            platformBuilder.addIndexableNestedProperties(indexableNestedProperties);
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU,
            version = AppSearchVersionUtil.TExtensionVersions.V_BASE)
    private static class ApiHelperForSdkExtensionVBase {
        private ApiHelperForSdkExtensionVBase() {
            // This class is not instantiable.
        }

        @DoNotInline
        static List<String> getIndexableNestedProperties(
                android.app.appsearch.AppSearchSchema.DocumentPropertyConfig
                        platformDocumentProperty) {
            return platformDocumentProperty.getIndexableNestedProperties();
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU,
            version = AppSearchVersionUtil.TExtensionVersions.B_BASE)
    private static class ApiHelperForSdkExtensionBBase {
        private ApiHelperForSdkExtensionBBase() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setScoringEnabled(
                android.app.appsearch.AppSearchSchema.LongPropertyConfig.Builder platformBuilder,
                boolean isScoringEnabled) {
            platformBuilder.setScoringEnabled(isScoringEnabled);
        }

        @DoNotInline
        static void setScoringEnabled(
                android.app.appsearch.AppSearchSchema.DoublePropertyConfig.Builder platformBuilder,
                boolean isScoringEnabled) {
            platformBuilder.setScoringEnabled(isScoringEnabled);
        }

        @DoNotInline
        static void setScoringEnabled(
                android.app.appsearch.AppSearchSchema.BooleanPropertyConfig.Builder platformBuilder,
                boolean isScoringEnabled) {
            platformBuilder.setScoringEnabled(isScoringEnabled);
        }

        @DoNotInline
        static boolean getScoringEnabled(
                android.app.appsearch.AppSearchSchema.LongPropertyConfig platformProperty) {
            return platformProperty.isScoringEnabled();
        }

        @DoNotInline
        static boolean getScoringEnabled(
                android.app.appsearch.AppSearchSchema.DoublePropertyConfig platformProperty) {
            return platformProperty.isScoringEnabled();
        }

        @DoNotInline
        static boolean getScoringEnabled(
                android.app.appsearch.AppSearchSchema.BooleanPropertyConfig platformProperty) {
            return platformProperty.isScoringEnabled();
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private static class ApiHelperForV {
        private ApiHelperForV() {
        }

        @DoNotInline
        @SuppressLint("NewApi")
        static void addParentType(
                android.app.appsearch.AppSearchSchema.Builder platformBuilder,
                @NonNull String parentSchemaType) {
            platformBuilder.addParentType(parentSchemaType);
        }

        @DoNotInline
        static List<String> getParentTypes(android.app.appsearch.AppSearchSchema platformSchema) {
            return platformSchema.getParentTypes();
        }
    }

    @RequiresApi(36)
    @SuppressLint("NewApi") // EmbeddingPropertyConfig incorrectly flagged as 34-ext16
    private static class ApiHelperForB {
        private ApiHelperForB() {
        }

        @DoNotInline
        @SuppressLint("WrongConstant")
        @OptIn(markerClass = ExperimentalAppSearchApi.class)
        static android.app.appsearch.AppSearchSchema.PropertyConfig
                createPlatformEmbeddingPropertyConfig(
                AppSearchSchema.@NonNull EmbeddingPropertyConfig jetpackEmbeddingProperty) {
            return new android.app.appsearch.AppSearchSchema.EmbeddingPropertyConfig.Builder(
                    jetpackEmbeddingProperty.getName())
                    .setCardinality(jetpackEmbeddingProperty.getCardinality())
                    .setIndexingType(jetpackEmbeddingProperty.getIndexingType())
                    .setQuantizationType(jetpackEmbeddingProperty.getQuantizationType())
                    .build();
        }

        @DoNotInline
        @SuppressLint("WrongConstant")
        @OptIn(markerClass = ExperimentalAppSearchApi.class)
        static AppSearchSchema.EmbeddingPropertyConfig createJetpackEmbeddingPropertyConfig(
                android.app.appsearch.AppSearchSchema.@NonNull EmbeddingPropertyConfig
                        platformEmbeddingProperty) {
            return new AppSearchSchema.EmbeddingPropertyConfig.Builder(
                    platformEmbeddingProperty.getName())
                    .setCardinality(platformEmbeddingProperty.getCardinality())
                    .setIndexingType(platformEmbeddingProperty.getIndexingType())
                    .setQuantizationType(platformEmbeddingProperty.getQuantizationType())
                    .build();
        }

        @DoNotInline
        @SuppressLint("WrongConstant")
        static android.app.appsearch.AppSearchSchema.PropertyConfig
                    createPlatformBlobHandlePropertyConfig(
                AppSearchSchema.@NonNull BlobHandlePropertyConfig platformBlobHandleProperty) {
            // TODO(b/326987971) : convert description when SetSchemaDescription is ready in
            //  service-appsearch fall through.
            return new android.app.appsearch.AppSearchSchema.BlobHandlePropertyConfig.Builder(
                    platformBlobHandleProperty.getName())
                    .setCardinality(platformBlobHandleProperty.getCardinality())
                    .build();
        }

        @DoNotInline
        @SuppressLint("WrongConstant")
        static AppSearchSchema.BlobHandlePropertyConfig createJetpackBlobHandlePropertyConfig(
                android.app.appsearch.AppSearchSchema.@NonNull BlobHandlePropertyConfig
                        platformBlobHandleProperty) {
            // TODO(b/326987971) : convert description when SetSchemaDescription is ready in
            //  service-appsearch fall through.
            return new AppSearchSchema.BlobHandlePropertyConfig.Builder(
                    platformBlobHandleProperty.getName())
                    .setCardinality(platformBlobHandleProperty.getCardinality())
                    .build();
        }
    }
}
