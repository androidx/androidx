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

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Translates a jetpack {@link androidx.appsearch.app.AppSearchSchema} into a Gms
 * {@link com.google.android.gms.appsearch.AppSearchSchema}.

 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SchemaToGmsConverter {
    private SchemaToGmsConverter() {
    }

    /**
     * Translates a jetpack {@link androidx.appsearch.app.AppSearchSchema} into a Gms
     * {@link com.google.android.gms.appsearch.AppSearchSchema}.
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static com.google.android.gms.appsearch.@NonNull AppSearchSchema toGmsSchema(
            @NonNull AppSearchSchema jetpackSchema) {
        Preconditions.checkNotNull(jetpackSchema);
        com.google.android.gms.appsearch.AppSearchSchema.Builder gmsBuilder =
                new com.google.android.gms.appsearch.AppSearchSchema
                        .Builder(jetpackSchema.getSchemaType());
        if (!jetpackSchema.getDescription().isEmpty()) {
            // TODO(b/326987971): Remove this once description becomes available.
            throw new UnsupportedOperationException(Features.SCHEMA_SET_DESCRIPTION
                    + " is not available on this AppSearch implementation.");
        }
        if (!jetpackSchema.getParentTypes().isEmpty()) {
            List<String> parentTypes = jetpackSchema.getParentTypes();
            for (int i = 0; i < parentTypes.size(); i++) {
                gmsBuilder.addParentType(parentTypes.get(i));
            }
        }
        List<AppSearchSchema.PropertyConfig> properties = jetpackSchema.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            com.google.android.gms.appsearch.AppSearchSchema.PropertyConfig gmsProperty =
                    toGmsProperty(properties.get(i));
            gmsBuilder.addProperty(gmsProperty);
        }
        return gmsBuilder.build();
    }

    /**
     * Translates a Gms {@link com.google.android.gms.appsearch.AppSearchSchema}
     * to a jetpack
     * {@link androidx.appsearch.app.AppSearchSchema}.
     */
    public static @NonNull AppSearchSchema toJetpackSchema(
            com.google.android.gms.appsearch.@NonNull AppSearchSchema gmsSchema) {
        Preconditions.checkNotNull(gmsSchema);
        AppSearchSchema.Builder jetpackBuilder =
                new AppSearchSchema.Builder(gmsSchema.getSchemaType());
        // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
        //  available in gms.
        List<com.google.android.gms.appsearch.AppSearchSchema.PropertyConfig> properties =
                gmsSchema.getProperties();
        List<String> parentTypes = gmsSchema.getParentTypes();
        for (int i = 0; i < parentTypes.size(); i++) {
            jetpackBuilder.addParentType(parentTypes.get(i));
        }
        for (int i = 0; i < properties.size(); i++) {
            AppSearchSchema.PropertyConfig jetpackProperty = toJetpackProperty(properties.get(i));
            jetpackBuilder.addProperty(jetpackProperty);
        }
        return jetpackBuilder.build();
    }

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private static com.google.android.gms.appsearch.AppSearchSchema.@NonNull PropertyConfig
            toGmsProperty(AppSearchSchema.@NonNull PropertyConfig jetpackProperty) {
        Preconditions.checkNotNull(jetpackProperty);
        if (!jetpackProperty.getDescription().isEmpty()) {
            // TODO(b/326987971): Remove this once description becomes available.
            throw new UnsupportedOperationException(Features.SCHEMA_SET_DESCRIPTION
                    + " is not available on this AppSearch implementation.");
        }
        if (jetpackProperty instanceof AppSearchSchema.StringPropertyConfig) {
            AppSearchSchema.StringPropertyConfig stringProperty =
                    (AppSearchSchema.StringPropertyConfig) jetpackProperty;
            com.google.android.gms.appsearch.AppSearchSchema.StringPropertyConfig.Builder
                    gmsBuilder =
                    new com.google.android.gms.appsearch.AppSearchSchema.StringPropertyConfig
                            .Builder(stringProperty.getName())
                            .setCardinality(stringProperty.getCardinality())
                            .setIndexingType(stringProperty.getIndexingType())
                            .setTokenizerType(stringProperty.getTokenizerType());
            if (stringProperty.getJoinableValueType()
                    == AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID) {
                gmsBuilder.setJoinableValueType(stringProperty.getJoinableValueType());
            }
            if (stringProperty.getDeletePropagationType()
                    == AppSearchSchema.StringPropertyConfig
                            .DELETE_PROPAGATION_TYPE_PROPAGATE_FROM) {
                // TODO(b/376913014): remove this once delete propagation API is available.
                throw new UnsupportedOperationException(
                        "StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM is not"
                                + " supported on this AppSearch implementation.");
            }
            return gmsBuilder.build();
        } else if (jetpackProperty instanceof AppSearchSchema.LongPropertyConfig) {
            AppSearchSchema.LongPropertyConfig longProperty =
                    (AppSearchSchema.LongPropertyConfig) jetpackProperty;
            com.google.android.gms.appsearch.AppSearchSchema.LongPropertyConfig.Builder
                    longPropertyBuilder =
                    new com.google.android.gms.appsearch.AppSearchSchema.LongPropertyConfig
                            .Builder(jetpackProperty.getName())
                            .setCardinality(jetpackProperty.getCardinality());
            if (longProperty.getIndexingType()
                    == AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE) {
                longPropertyBuilder.setIndexingType(longProperty.getIndexingType());
            }
            if (longProperty.isScoringEnabled()) {
                // TODO(b/379743983): update once this feature is available.
                throw new UnsupportedOperationException(
                        Features.SCHEMA_SCORABLE_PROPERTY_CONFIG
                                + " is not available on this AppSearch implementation.");
            }
            return longPropertyBuilder.build();
        } else if (jetpackProperty instanceof AppSearchSchema.DoublePropertyConfig) {
            AppSearchSchema.DoublePropertyConfig doubleProperty =
                    (AppSearchSchema.DoublePropertyConfig) jetpackProperty;
            if (doubleProperty.isScoringEnabled()) {
                // TODO(b/379743983): update once this feature is available.
                throw new UnsupportedOperationException(
                        Features.SCHEMA_SCORABLE_PROPERTY_CONFIG
                                + " is not available on this AppSearch implementation.");
            }
            return new com.google.android.gms.appsearch.AppSearchSchema.DoublePropertyConfig
                    .Builder(
                    jetpackProperty.getName())
                    .setCardinality(jetpackProperty.getCardinality())
                    .build();
        } else if (jetpackProperty instanceof AppSearchSchema.BooleanPropertyConfig) {
            AppSearchSchema.BooleanPropertyConfig booleanProperty =
                    (AppSearchSchema.BooleanPropertyConfig) jetpackProperty;
            if (booleanProperty.isScoringEnabled()) {
                // TODO(b/379743983): update once this feature is available.
                throw new UnsupportedOperationException(
                        Features.SCHEMA_SCORABLE_PROPERTY_CONFIG
                                + " is not available on this AppSearch implementation.");
            }
            return new com.google.android.gms.appsearch.AppSearchSchema.BooleanPropertyConfig
                    .Builder(
                    jetpackProperty.getName())
                    .setCardinality(jetpackProperty.getCardinality())
                    .build();
        } else if (jetpackProperty instanceof AppSearchSchema.BytesPropertyConfig) {
            return new com.google.android.gms.appsearch.AppSearchSchema.BytesPropertyConfig
                    .Builder(
                    jetpackProperty.getName())
                    .setCardinality(jetpackProperty.getCardinality())
                    .build();
        } else if (jetpackProperty instanceof AppSearchSchema.DocumentPropertyConfig) {
            AppSearchSchema.DocumentPropertyConfig documentProperty =
                    (AppSearchSchema.DocumentPropertyConfig) jetpackProperty;
            return new com.google.android.gms.appsearch.AppSearchSchema.DocumentPropertyConfig
                    .Builder(documentProperty.getName(), documentProperty.getSchemaType())
                    .setCardinality(documentProperty.getCardinality())
                    .setShouldIndexNestedProperties(
                            documentProperty.shouldIndexNestedProperties())
                    .addIndexableNestedProperties(
                            documentProperty.getIndexableNestedProperties()).build();
        } else if (jetpackProperty instanceof AppSearchSchema.EmbeddingPropertyConfig) {
            // TODO(b/326656531): Remove this once embedding search APIs are available.
            // TODO(b/359959345): Remember to add the check for quantization when embedding has
            //  become available but quantization has not yet.
            throw new UnsupportedOperationException(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG
                    + " is not available on this AppSearch implementation.");
        } else if (jetpackProperty instanceof AppSearchSchema.BlobHandlePropertyConfig) {
            // TODO(b/273591938): Remove this once blob APIs are available.
            throw new UnsupportedOperationException(Features.SCHEMA_BLOB_HANDLE
                    + " is not available on this AppSearch implementation.");
        } else {
            throw new IllegalArgumentException(
                    "Invalid dataType: " + jetpackProperty.getDataType());
        }
    }

    private static AppSearchSchema.@NonNull PropertyConfig toJetpackProperty(
            com.google.android.gms.appsearch.AppSearchSchema.@NonNull PropertyConfig
                    gmsProperty) {
        Preconditions.checkNotNull(gmsProperty);
        if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.StringPropertyConfig) {
            com.google.android.gms.appsearch.AppSearchSchema.StringPropertyConfig stringProperty =
                    (com.google.android.gms.appsearch.AppSearchSchema.StringPropertyConfig)
                            gmsProperty;
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            //  available in gms.
            return new AppSearchSchema.StringPropertyConfig.Builder(stringProperty.getName())
                    .setCardinality(stringProperty.getCardinality())
                    .setIndexingType(stringProperty.getIndexingType())
                    .setTokenizerType(stringProperty.getTokenizerType())
                    .setJoinableValueType(stringProperty.getJoinableValueType())
                    .build();
        } else if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.LongPropertyConfig) {
            com.google.android.gms.appsearch.AppSearchSchema.LongPropertyConfig longProperty =
                    (com.google.android.gms.appsearch.AppSearchSchema.LongPropertyConfig)
                            gmsProperty;
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            //  available in gms.
            // TODO(b/379743983): call setScoringEnabled() once this feature is available.
            return new AppSearchSchema.LongPropertyConfig.Builder(
                    gmsProperty.getName())
                    .setCardinality(gmsProperty.getCardinality())
                    .setIndexingType(longProperty.getIndexingType())
                    .build();
        } else if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.DoublePropertyConfig) {
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            //  available in gms.
            // TODO(b/379743983): call setScoringEnabled() once this feature is available.
            return new AppSearchSchema.DoublePropertyConfig.Builder(
                    gmsProperty.getName())
                    .setCardinality(gmsProperty.getCardinality()).build();
        } else if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.BooleanPropertyConfig) {
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            // available in gms.
            // TODO(b/379743983): call setScoringEnabled() once this feature is available.
            return new AppSearchSchema.BooleanPropertyConfig.Builder(
                    gmsProperty.getName())
                    .setCardinality(gmsProperty.getCardinality())
                    .build();
        } else if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.BytesPropertyConfig) {
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            // available in gms.
            return new AppSearchSchema.BytesPropertyConfig.Builder(
                    gmsProperty.getName())
                    .setCardinality(gmsProperty.getCardinality())
                    .build();
        } else if (gmsProperty
                instanceof
                com.google.android.gms.appsearch.AppSearchSchema.DocumentPropertyConfig) {
            com.google.android.gms.appsearch.AppSearchSchema.DocumentPropertyConfig
                    documentProperty =
                    (com.google.android.gms.appsearch.AppSearchSchema.DocumentPropertyConfig)
                            gmsProperty;
            // TODO(b/326987971): Call jetpackBuilder.setDescription() once descriptions become
            //  available in gms.
            return new AppSearchSchema.DocumentPropertyConfig.Builder(
                    documentProperty.getName(),
                    documentProperty.getSchemaType())
                    .setCardinality(documentProperty.getCardinality())
                    .setShouldIndexNestedProperties(
                            documentProperty.shouldIndexNestedProperties())
                    .addIndexableNestedProperties(
                            documentProperty.getIndexableNestedProperties())
                    .build();
        } else {
            // TODO(b/326656531) : Add an entry for EmbeddingPropertyConfig once it becomes
            //  available in gms-appsearch.
            throw new IllegalArgumentException(
                    "Invalid property type " + gmsProperty.getClass()
                            + ": " + gmsProperty);
        }
    }
}
