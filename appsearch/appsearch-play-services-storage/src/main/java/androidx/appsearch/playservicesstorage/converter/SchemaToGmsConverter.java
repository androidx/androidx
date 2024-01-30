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
import androidx.appsearch.app.AppSearchSchema;
import androidx.core.util.Preconditions;

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
    @NonNull
    public static com.google.android.gms.appsearch.AppSearchSchema toGmsSchema(
            @NonNull AppSearchSchema jetpackSchema) {
        Preconditions.checkNotNull(jetpackSchema);
        com.google.android.gms.appsearch.AppSearchSchema.Builder gmsBuilder =
                new com.google.android.gms.appsearch.AppSearchSchema
                        .Builder(jetpackSchema.getSchemaType());
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
    @NonNull
    public static AppSearchSchema toJetpackSchema(
            @NonNull com.google.android.gms.appsearch.AppSearchSchema gmsSchema) {
        Preconditions.checkNotNull(gmsSchema);
        AppSearchSchema.Builder jetpackBuilder =
                new AppSearchSchema.Builder(gmsSchema.getSchemaType());
        List<com.google.android.gms.appsearch.AppSearchSchema.PropertyConfig> properties =
                gmsSchema.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            AppSearchSchema.PropertyConfig jetpackProperty = toJetpackProperty(properties.get(i));
            jetpackBuilder.addProperty(jetpackProperty);
        }
        return jetpackBuilder.build();
    }

    @NonNull
    private static com.google.android.gms.appsearch.AppSearchSchema.PropertyConfig toGmsProperty(
            @NonNull AppSearchSchema.PropertyConfig jetpackProperty) {
        Preconditions.checkNotNull(jetpackProperty);
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
            if (stringProperty.getDeletionPropagation()) {
                // TODO(b/268521214): Update once deletion propagation is available.
                throw new UnsupportedOperationException("Setting deletion propagation is not "
                        + "supported on this AppSearch implementation.");
            }

            if (stringProperty.getJoinableValueType()
                    == AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID) {
                //TODO(b/274986359) Add GMSCore feature check for Joins once available.
                throw new UnsupportedOperationException(
                        "StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID is not supported"
                                + " on this AppSearch implementation.");
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
                //TODO(b/274986359) Add GMSCore feature check for Indexing Range once available.
                throw new UnsupportedOperationException(
                        "LongProperty.INDEXING_TYPE_RANGE is not supported on this AppSearch "
                                + "implementation.");
            }
            return longPropertyBuilder.build();
        } else if (jetpackProperty instanceof AppSearchSchema.DoublePropertyConfig) {
            return new com.google.android.gms.appsearch.AppSearchSchema.DoublePropertyConfig
                    .Builder(
                    jetpackProperty.getName())
                    .setCardinality(jetpackProperty.getCardinality())
                    .build();
        } else if (jetpackProperty instanceof AppSearchSchema.BooleanPropertyConfig) {
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
                    .Builder(
                    documentProperty.getName(), documentProperty.getSchemaType())
                    .setCardinality(documentProperty.getCardinality())
                    .setShouldIndexNestedProperties(documentProperty.shouldIndexNestedProperties())
                    .build();
        } else {
            throw new IllegalArgumentException(
                    "Invalid dataType: " + jetpackProperty.getDataType());
        }
    }

    @NonNull
    private static AppSearchSchema.PropertyConfig toJetpackProperty(
            @NonNull com.google.android.gms.appsearch.AppSearchSchema.PropertyConfig
                    gmsProperty) {
        Preconditions.checkNotNull(gmsProperty);
        if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.StringPropertyConfig) {
            com.google.android.gms.appsearch.AppSearchSchema.StringPropertyConfig stringProperty =
                    (com.google.android.gms.appsearch.AppSearchSchema.StringPropertyConfig)
                            gmsProperty;
            return new AppSearchSchema.StringPropertyConfig.Builder(stringProperty.getName())
                    .setCardinality(stringProperty.getCardinality())
                    .setIndexingType(stringProperty.getIndexingType())
                    .setTokenizerType(stringProperty.getTokenizerType())
                    .build();
        } else if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.LongPropertyConfig) {
            return new AppSearchSchema.LongPropertyConfig.Builder(
                    gmsProperty.getName())
                    .setCardinality(gmsProperty.getCardinality())
                    .build();
        } else if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.DoublePropertyConfig) {
            return new AppSearchSchema.DoublePropertyConfig.Builder(
                    gmsProperty.getName())
                    .setCardinality(gmsProperty.getCardinality())
                    .build();
        } else if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.BooleanPropertyConfig) {
            return new AppSearchSchema.BooleanPropertyConfig.Builder(
                    gmsProperty.getName())
                    .setCardinality(gmsProperty.getCardinality())
                    .build();
        } else if (gmsProperty
                instanceof com.google.android.gms.appsearch.AppSearchSchema.BytesPropertyConfig) {
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
            return new AppSearchSchema.DocumentPropertyConfig.Builder(
                    documentProperty.getName(),
                    documentProperty.getSchemaType())
                    .setCardinality(documentProperty.getCardinality())
                    .setShouldIndexNestedProperties(documentProperty.shouldIndexNestedProperties())
                    .build();
        } else {
            throw new IllegalArgumentException(
                    "Invalid property type " + gmsProperty.getClass()
                            + ": " + gmsProperty);
        }
    }
}
