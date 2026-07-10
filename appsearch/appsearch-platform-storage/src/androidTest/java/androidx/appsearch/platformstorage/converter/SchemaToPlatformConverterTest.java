/*
 * Copyright 2026 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.Build;

import androidx.annotation.OptIn;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.appsearch.platformstorage.UnsupportedPlatformConversionAdapter;
import androidx.appsearch.testutil.TestPlatformConversionAdapter;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@OptIn(markerClass = ExperimentalAppSearchApi.class)
public class SchemaToPlatformConverterTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void testSetSchemaDescription() {
        assumeTrue(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SCHEMA_SET_DESCRIPTION));
        AppSearchSchema jetpackSchema = new AppSearchSchema.Builder("testSchema")
                .setDescription("schema description")
                .build();
        TestPlatformConversionAdapter adapter = new TestPlatformConversionAdapter();

        SchemaToPlatformConverter.toPlatformSchema(mContext, jetpackSchema, adapter);

        assertThat(adapter.getSchemaDescription()).isEqualTo("schema description");
    }

    @Test
    public void testSetPropertyDescription_stringProperty() {
        assumeTrue(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SCHEMA_SET_DESCRIPTION));
        AppSearchSchema jetpackSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("prop")
                        .setDescription("property description")
                        .build())
                .build();
        TestPlatformConversionAdapter adapter = new TestPlatformConversionAdapter();

        SchemaToPlatformConverter.toPlatformSchema(mContext, jetpackSchema, adapter);

        assertThat(adapter.getPropertyDescription()).isEqualTo("property description");
    }

    @Test
    public void testSetDeletePropagationType() {
        assumeTrue(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM));
        AppSearchSchema jetpackSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("prop")
                        .setDeletePropagationType(
                                AppSearchSchema.StringPropertyConfig
                                        .DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
                        .build())
                .build();
        TestPlatformConversionAdapter adapter = new TestPlatformConversionAdapter();

        SchemaToPlatformConverter.toPlatformSchema(mContext, jetpackSchema, adapter);

        assertThat(adapter.getDeletePropagationType()).isEqualTo(
                AppSearchSchema.StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM);
    }

    @Test
    public void testSetIndexingType_embeddingProperty() {
        assumeTrue(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SCHEMA_EMBEDDING_APPROXIMATE_NEAREST_NEIGHBOR));
        AppSearchSchema jetpackSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("prop")
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig
                                        .INDEXING_TYPE_APPROXIMATE_NEAREST_NEIGHBOR)
                        .build())
                .build();
        TestPlatformConversionAdapter adapter = new TestPlatformConversionAdapter();

        SchemaToPlatformConverter.toPlatformSchema(mContext, jetpackSchema, adapter);

        assertThat(adapter.getIndexingType()).isEqualTo(
                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_APPROXIMATE_NEAREST_NEIGHBOR);
    }

    @Test
    public void testSetSchemaDescription_throws() {
        assumeFalse(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SCHEMA_SET_DESCRIPTION));
        AppSearchSchema jetpackSchema = new AppSearchSchema.Builder("testSchema")
                .setDescription("schema description")
                .build();
        UnsupportedPlatformConversionAdapter adapter = new UnsupportedPlatformConversionAdapter();

        assertThrows(UnsupportedOperationException.class,
                () -> SchemaToPlatformConverter.toPlatformSchema(mContext, jetpackSchema, adapter));
    }

    @Test
    public void testSetDeletePropagationType_throws() {
        assumeFalse(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM));
        AppSearchSchema jetpackSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("prop")
                        .setJoinableValueType(
                                AppSearchSchema.StringPropertyConfig
                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setDeletePropagationType(
                                AppSearchSchema.StringPropertyConfig
                                        .DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
                        .build())
                .build();
        UnsupportedPlatformConversionAdapter adapter = new UnsupportedPlatformConversionAdapter();

        assertThrows(UnsupportedOperationException.class,
                () -> SchemaToPlatformConverter.toPlatformSchema(mContext, jetpackSchema, adapter));
    }

    @Test
    public void testSetIndexingType_embeddingProperty_throws() {
        assumeFalse(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SCHEMA_EMBEDDING_APPROXIMATE_NEAREST_NEIGHBOR));
        AppSearchSchema jetpackSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("prop")
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig
                                        .INDEXING_TYPE_APPROXIMATE_NEAREST_NEIGHBOR)
                        .build())
                .build();
        UnsupportedPlatformConversionAdapter adapter = new UnsupportedPlatformConversionAdapter();

        assertThrows(UnsupportedOperationException.class,
                () -> SchemaToPlatformConverter.toPlatformSchema(mContext, jetpackSchema, adapter));
    }
}
