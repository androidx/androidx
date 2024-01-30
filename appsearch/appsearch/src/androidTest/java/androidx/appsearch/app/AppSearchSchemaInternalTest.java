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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.testutil.AppSearchEmail;

import org.junit.Test;

import java.util.List;

/** Tests for private APIs of {@link AppSearchSchema}. */
public class AppSearchSchemaInternalTest {
    // TODO(b/291122592): move to CTS once the APIs it uses are public
    @Test
    public void testParentTypes() {
        AppSearchSchema schema =
                new AppSearchSchema.Builder("EmailMessage")
                        .addParentType("Email")
                        .addParentType("Message")
                        .build();
        assertThat(schema.getParentTypes()).containsExactly("Email", "Message");
    }

    // TODO(b/291122592): move to CTS once the APIs it uses are public
    @Test
    public void testDuplicateParentTypes() {
        AppSearchSchema schema =
                new AppSearchSchema.Builder("EmailMessage")
                        .addParentType("Email")
                        .addParentType("Message")
                        .addParentType("Email")
                        .build();
        assertThat(schema.getParentTypes()).containsExactly("Email", "Message");
    }

    // TODO(b/291122592): move to CTS once the APIs it uses are public
    @Test
    public void testDocumentPropertyConfig_indexableNestedPropertyStrings() {
        AppSearchSchema.DocumentPropertyConfig documentPropertyConfig =
                new AppSearchSchema.DocumentPropertyConfig.Builder("property", "Schema")
                        .addIndexableNestedProperties("prop1", "prop2", "prop1.prop2")
                        .build();
        assertThat(documentPropertyConfig.getIndexableNestedProperties())
                .containsExactly("prop1", "prop2", "prop1.prop2");
    }

    // TODO(b/291122592): move to CTS once the APIs it uses are public
    @Test
    public void testDocumentPropertyConfig_indexableNestedPropertyPropertyPaths() {
        AppSearchSchema.DocumentPropertyConfig documentPropertyConfig =
                new AppSearchSchema.DocumentPropertyConfig.Builder("property", "Schema")
                        .addIndexableNestedPropertyPaths(
                                new PropertyPath("prop1"), new PropertyPath("prop1.prop2"))
                        .build();
        assertThat(documentPropertyConfig.getIndexableNestedProperties())
                .containsExactly("prop1", "prop1.prop2");
    }

    // TODO(b/291122592): move to CTS once the APIs it uses are public
    @Test
    public void testDocumentPropertyConfig_indexableNestedPropertyProperty_duplicatePaths() {
        AppSearchSchema.DocumentPropertyConfig documentPropertyConfig =
                new AppSearchSchema.DocumentPropertyConfig.Builder("property", "Schema")
                        .addIndexableNestedPropertyPaths(
                                new PropertyPath("prop1"), new PropertyPath("prop1.prop2"))
                        .addIndexableNestedProperties("prop1")
                        .build();
        assertThat(documentPropertyConfig.getIndexableNestedProperties())
                .containsExactly("prop1", "prop1.prop2");
    }

    // TODO(b/291122592): move to CTS once the APIs it uses are public
    @Test
    public void testDocumentPropertyConfig_reusingBuilderDoesNotAffectPreviouslyBuiltConfigs() {
        AppSearchSchema.DocumentPropertyConfig.Builder builder =
                new AppSearchSchema.DocumentPropertyConfig.Builder("property", "Schema")
                        .addIndexableNestedProperties("prop1");
        AppSearchSchema.DocumentPropertyConfig config1 = builder.build();
        assertThat(config1.getIndexableNestedProperties()).containsExactly("prop1");

        builder.addIndexableNestedProperties("prop2");
        AppSearchSchema.DocumentPropertyConfig config2 = builder.build();
        assertThat(config2.getIndexableNestedProperties()).containsExactly("prop1", "prop2");
        assertThat(config1.getIndexableNestedProperties()).containsExactly("prop1");

        builder.addIndexableNestedPropertyPaths(new PropertyPath("prop3"));
        AppSearchSchema.DocumentPropertyConfig config3 = builder.build();
        assertThat(config3.getIndexableNestedProperties())
                .containsExactly("prop1", "prop2", "prop3");
        assertThat(config2.getIndexableNestedProperties()).containsExactly("prop1", "prop2");
        assertThat(config1.getIndexableNestedProperties()).containsExactly("prop1");
    }

    // TODO(b/291122592): move to CTS once the APIs it uses are public
    @Test
    public void testPropertyConfig() {
        AppSearchSchema schema =
                new AppSearchSchema.Builder("Test")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("string")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                AppSearchSchema.StringPropertyConfig
                                                        .INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                AppSearchSchema.StringPropertyConfig
                                                        .TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.LongPropertyConfig.Builder("long")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                AppSearchSchema.LongPropertyConfig
                                                        .INDEXING_TYPE_NONE)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.LongPropertyConfig.Builder("indexableLong")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                AppSearchSchema.LongPropertyConfig
                                                        .INDEXING_TYPE_RANGE)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DoublePropertyConfig.Builder("double")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.BooleanPropertyConfig.Builder("boolean")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.BytesPropertyConfig.Builder("bytes")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                                "document1", AppSearchEmail.SCHEMA_TYPE)
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                                "document2", AppSearchEmail.SCHEMA_TYPE)
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties("path1", "path2", "path3")
                                        .build())
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("qualifiedId1")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setJoinableValueType(
                                                AppSearchSchema.StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("qualifiedId2")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .setJoinableValueType(
                                                AppSearchSchema.StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .build())
                        .build();

        assertThat(schema.getSchemaType()).isEqualTo("Test");
        List<AppSearchSchema.PropertyConfig> properties = schema.getProperties();
        assertThat(properties).hasSize(10);

        assertThat(properties.get(0).getName()).isEqualTo("string");
        assertThat(properties.get(0).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(((AppSearchSchema.StringPropertyConfig) properties.get(0)).getIndexingType())
                .isEqualTo(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThat(((AppSearchSchema.StringPropertyConfig) properties.get(0)).getTokenizerType())
                .isEqualTo(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN);

        assertThat(properties.get(1).getName()).isEqualTo("long");
        assertThat(properties.get(1).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(((AppSearchSchema.LongPropertyConfig) properties.get(1)).getIndexingType())
                .isEqualTo(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE);

        assertThat(properties.get(2).getName()).isEqualTo("indexableLong");
        assertThat(properties.get(2).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(((AppSearchSchema.LongPropertyConfig) properties.get(2)).getIndexingType())
                .isEqualTo(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE);

        assertThat(properties.get(3).getName()).isEqualTo("double");
        assertThat(properties.get(3).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED);
        assertThat(properties.get(3)).isInstanceOf(AppSearchSchema.DoublePropertyConfig.class);

        assertThat(properties.get(4).getName()).isEqualTo("boolean");
        assertThat(properties.get(4).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(properties.get(4)).isInstanceOf(AppSearchSchema.BooleanPropertyConfig.class);

        assertThat(properties.get(5).getName()).isEqualTo("bytes");
        assertThat(properties.get(5).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(properties.get(5)).isInstanceOf(AppSearchSchema.BytesPropertyConfig.class);

        assertThat(properties.get(6).getName()).isEqualTo("document1");
        assertThat(properties.get(6).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED);
        assertThat(((AppSearchSchema.DocumentPropertyConfig) properties.get(6)).getSchemaType())
                .isEqualTo(AppSearchEmail.SCHEMA_TYPE);
        assertThat(
                        ((AppSearchSchema.DocumentPropertyConfig) properties.get(6))
                                .shouldIndexNestedProperties())
                .isEqualTo(true);

        assertThat(properties.get(7).getName()).isEqualTo("document2");
        assertThat(properties.get(7).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED);
        assertThat(((AppSearchSchema.DocumentPropertyConfig) properties.get(7)).getSchemaType())
                .isEqualTo(AppSearchEmail.SCHEMA_TYPE);
        assertThat(
                        ((AppSearchSchema.DocumentPropertyConfig) properties.get(7))
                                .shouldIndexNestedProperties())
                .isEqualTo(false);
        assertThat(
                        ((AppSearchSchema.DocumentPropertyConfig) properties.get(7))
                                .getIndexableNestedProperties())
                .containsExactly("path1", "path2", "path3");

        assertThat(properties.get(8).getName()).isEqualTo("qualifiedId1");
        assertThat(properties.get(8).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(
                        ((AppSearchSchema.StringPropertyConfig) properties.get(8))
                                .getJoinableValueType())
                .isEqualTo(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);

        assertThat(properties.get(9).getName()).isEqualTo("qualifiedId2");
        assertThat(properties.get(9).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(
                        ((AppSearchSchema.StringPropertyConfig) properties.get(9))
                                .getJoinableValueType())
                .isEqualTo(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);
    }
}
