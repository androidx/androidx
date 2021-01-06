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
import androidx.appsearch.app.AppSearchSchema;
import androidx.core.util.Preconditions;

import java.util.List;

/**
 * Translates a jetpack {@link androidx.appsearch.app.AppSearchSchema} into a platform
 * {@link android.app.appsearch.AppSearchSchema}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class SchemaToPlatformConverter {
    private SchemaToPlatformConverter() {}

    /**
     * Translates a jetpack {@link androidx.appsearch.app.AppSearchSchema} into a platform
     * {@link android.app.appsearch.AppSearchSchema}.
     */
    @NonNull
    public static android.app.appsearch.AppSearchSchema toPlatformSchema(
            @NonNull AppSearchSchema jetpackSchema) {
        Preconditions.checkNotNull(jetpackSchema);
        android.app.appsearch.AppSearchSchema.Builder platformBuilder =
                new android.app.appsearch.AppSearchSchema.Builder(jetpackSchema.getSchemaType());
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
     * {@link androidx.appsearch.app.AppSearchSchema}.
     */
    @NonNull
    public static AppSearchSchema toJetpackSchema(
            @NonNull android.app.appsearch.AppSearchSchema platformSchema) {
        Preconditions.checkNotNull(platformSchema);
        AppSearchSchema.Builder jetpackBuilder =
                new AppSearchSchema.Builder(platformSchema.getSchemaType());
        List<android.app.appsearch.AppSearchSchema.PropertyConfig> properties =
                platformSchema.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            AppSearchSchema.PropertyConfig jetpackProperty = toJetpackProperty(properties.get(i));
            jetpackBuilder.addProperty(jetpackProperty);
        }
        return jetpackBuilder.build();
    }

    @NonNull
    private static android.app.appsearch.AppSearchSchema.PropertyConfig toPlatformProperty(
            @NonNull AppSearchSchema.PropertyConfig jetpackProperty) {
        Preconditions.checkNotNull(jetpackProperty);
        android.app.appsearch.AppSearchSchema.PropertyConfig.Builder builder =
                new android.app.appsearch.AppSearchSchema.PropertyConfig.Builder(
                        jetpackProperty.getName());
        builder
                .setDataType(jetpackProperty.getDataType())
                .setCardinality(jetpackProperty.getCardinality())
                .setIndexingType(jetpackProperty.getIndexingType())
                .setTokenizerType(jetpackProperty.getTokenizerType());
        String schemaType = jetpackProperty.getSchemaType();
        if (schemaType != null) {
            builder.setSchemaType(schemaType);
        }
        return builder.build();
    }

    @NonNull
    private static AppSearchSchema.PropertyConfig toJetpackProperty(
            @NonNull android.app.appsearch.AppSearchSchema.PropertyConfig platformProperty) {
        Preconditions.checkNotNull(platformProperty);
        AppSearchSchema.PropertyConfig.Builder builder =
                new AppSearchSchema.PropertyConfig.Builder(platformProperty.getName());
        builder
                .setDataType(platformProperty.getDataType())
                .setCardinality(platformProperty.getCardinality())
                .setIndexingType(platformProperty.getIndexingType())
                .setTokenizerType(platformProperty.getTokenizerType());
        String schemaType = platformProperty.getSchemaType();
        if (schemaType != null) {
            builder.setSchemaType(schemaType);
        }
        return builder.build();
    }
}
