/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Test;

import java.util.List;

public class AppSearchSchemaInternalTest {
    // TODO(b/384947619): move delete propagation tests back to AppSearchSchemaCtsTest once the API
    //   is ready.
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testStringPropertyConfigDefaultValues_withDeletePropagationType() {
        StringPropertyConfig builder = new StringPropertyConfig.Builder("test").build();
        assertThat(builder.getIndexingType()).isEqualTo(StringPropertyConfig.INDEXING_TYPE_NONE);
        assertThat(builder.getTokenizerType()).isEqualTo(StringPropertyConfig.TOKENIZER_TYPE_NONE);
        assertThat(builder.getCardinality()).isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(builder.getJoinableValueType())
                .isEqualTo(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE);
        assertThat(builder.getDeletePropagationType())
                .isEqualTo(StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testPropertyConfig_withDeletePropagationType() {
        AppSearchSchema schema =
                new AppSearchSchema.Builder("Test")
                        .addProperty(
                                new StringPropertyConfig.Builder("qualifiedId1")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .setDeletePropagationType(
                                                StringPropertyConfig
                                                        .DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("qualifiedId2")
                                        .setCardinality(
                                                PropertyConfig.CARDINALITY_REQUIRED)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .setDeletePropagationType(
                                                StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE)
                                        .build())
                        .build();

        assertThat(schema.getSchemaType()).isEqualTo("Test");
        List<PropertyConfig> properties = schema.getProperties();
        assertThat(properties).hasSize(2);

        assertThat(properties.get(0).getName()).isEqualTo("qualifiedId1");
        assertThat(properties.get(0).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(((StringPropertyConfig) properties.get(0)).getJoinableValueType())
                .isEqualTo(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);
        assertThat(((StringPropertyConfig) properties.get(0)).getDeletePropagationType())
                .isEqualTo(StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM);

        assertThat(properties.get(1).getName()).isEqualTo("qualifiedId2");
        assertThat(properties.get(1).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(((StringPropertyConfig) properties.get(1)).getJoinableValueType())
                .isEqualTo(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);
        assertThat(((StringPropertyConfig) properties.get(1)).getDeletePropagationType())
                .isEqualTo(StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testSetDeletePropagationTypeWithoutJoinableValueTypeQualifiedId_throwsException() {
        // Setting delete propagation type PROPAGATE_FROM with joinable value type other than
        // QUALIFIED_ID should fail.
        final StringPropertyConfig.Builder builder =
                new StringPropertyConfig.Builder("qualifiedId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setDeletePropagationType(
                                StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM);
        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> builder.build());
        assertThat(e).hasMessageThat().contains(
                "Cannot set delete propagation without setting JOINABLE_VALUE_TYPE_QUALIFIED_ID.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testAppSearchSchema_toString_withDeletePropagationType() {
        AppSearchSchema schema =
                new AppSearchSchema.Builder("testSchema")
                        .addProperty(
                                new StringPropertyConfig.Builder("qualifiedId1")
                                        .setDescription("first qualifiedId")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .setDeletePropagationType(
                                                StringPropertyConfig
                                                        .DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("qualifiedId2")
                                        .setDescription("second qualifiedId")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .setDeletePropagationType(
                                                StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE)
                                        .build())
                        .build();

        String schemaString = schema.toString();

        String expectedString =
                "{\n"
                        + "  schemaType: \"testSchema\",\n"
                        + "  properties: [\n"
                        + "    {\n"
                        + "      name: \"qualifiedId1\",\n"
                        + "      description: \"first qualifiedId\",\n"
                        + "      indexingType: INDEXING_TYPE_NONE,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_NONE,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_QUALIFIED_ID,\n"
                        + "      deletePropagationType: DELETE_PROPAGATION_TYPE_PROPAGATE_FROM,\n"
                        + "      cardinality: CARDINALITY_REQUIRED,\n"
                        + "      dataType: DATA_TYPE_STRING,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"qualifiedId2\",\n"
                        + "      description: \"second qualifiedId\",\n"
                        + "      indexingType: INDEXING_TYPE_NONE,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_NONE,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_QUALIFIED_ID,\n"
                        + "      deletePropagationType: DELETE_PROPAGATION_TYPE_NONE,\n"
                        + "      cardinality: CARDINALITY_OPTIONAL,\n"
                        + "      dataType: DATA_TYPE_STRING,\n"
                        + "    }\n"
                        + "  ]\n"
                        + "}";

        String[] lines = expectedString.split("\n");
        for (String line : lines) {
            assertThat(schemaString).contains(line);
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testStringPropertyConfig_setDeletePropagationType() {
        assertThrows(IllegalArgumentException.class, () ->
                new StringPropertyConfig.Builder("qualifiedId").setDeletePropagationType(5)
                        .build());
        assertThrows(IllegalArgumentException.class, () ->
                new StringPropertyConfig.Builder("qualifiedId").setDeletePropagationType(2)
                        .build());
        assertThrows(IllegalArgumentException.class, () ->
                new StringPropertyConfig.Builder("qualifiedId").setDeletePropagationType(-1)
                        .build());
    }
}
