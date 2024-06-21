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

package androidx.appsearch.cts.app;


import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;
import androidx.appsearch.testutil.AppSearchEmail;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class AppSearchSchemaCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testInvalidEnums() {
        StringPropertyConfig.Builder builder = new StringPropertyConfig.Builder("test");
        assertThrows(IllegalArgumentException.class, () -> builder.setCardinality(99));
    }

    @Test
    public void testStringPropertyConfigDefaultValues() {
        StringPropertyConfig builder = new StringPropertyConfig.Builder("test").build();
        assertThat(builder.getIndexingType()).isEqualTo(StringPropertyConfig.INDEXING_TYPE_NONE);
        assertThat(builder.getTokenizerType()).isEqualTo(StringPropertyConfig.TOKENIZER_TYPE_NONE);
        assertThat(builder.getCardinality()).isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(builder.getJoinableValueType())
                .isEqualTo(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE);
    }

    @Test
    public void testLongPropertyConfigDefaultValues() {
        LongPropertyConfig builder = new LongPropertyConfig.Builder("test").build();
        assertThat(builder.getIndexingType()).isEqualTo(LongPropertyConfig.INDEXING_TYPE_NONE);
        assertThat(builder.getCardinality()).isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
    }

    @Test
    public void testDuplicateProperties() {
        AppSearchSchema.Builder builder = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                );
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()));
        assertThat(e).hasMessageThat().contains("Property defined more than once: subject");
    }

    @Test
    public void testEquals_identical() {
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema schema2 = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        assertThat(schema1).isEqualTo(schema2);
        assertThat(schema1.hashCode()).isEqualTo(schema2.hashCode());
    }

    @Test
    public void testEquals_differentOrder() {
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema schema2 = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build()
                ).build();
        assertThat(schema1).isEqualTo(schema2);
        assertThat(schema1.hashCode()).isEqualTo(schema2.hashCode());
    }

    @Test
    public void testEquals_failure_differentProperty() {
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema schema2 = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)  // Diff
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        assertThat(schema1).isNotEqualTo(schema2);
        assertThat(schema1.hashCode()).isNotEqualTo(schema2.hashCode());
    }

    @Test
    public void testEquals_failure_differentOrder() {
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        // Order of 'body' and 'subject' has been switched
        AppSearchSchema schema2 = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        assertThat(schema1).isNotEqualTo(schema2);
        assertThat(schema1.hashCode()).isNotEqualTo(schema2.hashCode());
    }

    @Test
    public void testParentTypes() {
        AppSearchSchema schema =
                new AppSearchSchema.Builder("EmailMessage")
                        .addParentType("Email")
                        .addParentType("Message")
                        .build();
        assertThat(schema.getParentTypes()).containsExactly("Email", "Message");
    }

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

    @Test
    public void testDocumentPropertyConfig_indexableNestedPropertyStrings() {
        AppSearchSchema.DocumentPropertyConfig documentPropertyConfig =
                new AppSearchSchema.DocumentPropertyConfig.Builder("property", "Schema")
                        .addIndexableNestedProperties("prop1", "prop2", "prop1.prop2")
                        .build();
        assertThat(documentPropertyConfig.getIndexableNestedProperties())
                .containsExactly("prop1", "prop2", "prop1.prop2");
    }

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
        List<PropertyConfig> properties = schema.getProperties();
        assertThat(properties).hasSize(10);

        assertThat(properties.get(0).getName()).isEqualTo("string");
        assertThat(properties.get(0).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(((AppSearchSchema.StringPropertyConfig) properties.get(0)).getIndexingType())
                .isEqualTo(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThat(((AppSearchSchema.StringPropertyConfig) properties.get(
                0)).getTokenizerType())
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
                .isEqualTo(
                        AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);

        assertThat(properties.get(9).getName()).isEqualTo("qualifiedId2");
        assertThat(properties.get(9).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(
                ((AppSearchSchema.StringPropertyConfig) properties.get(9))
                        .getJoinableValueType())
                .isEqualTo(
                        AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);
    }

    @Test
    public void testInvalidStringPropertyConfigsTokenizerNone() {
        // Everything should work fine with the defaults.
        final StringPropertyConfig.Builder builder =
                new StringPropertyConfig.Builder("property");
        assertThat(builder.build()).isNotNull();

        // Setting an indexing type other NONE with the default tokenizer type (NONE) should fail.
        builder.setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThrows(IllegalStateException.class, () -> builder.build());

        builder.setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES);
        assertThrows(IllegalStateException.class, () -> builder.build());

        // Explicitly setting the default should work fine.
        builder.setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE);
        assertThat(builder.build()).isNotNull();

        // Explicitly setting the default tokenizer type should result in the same behavior.
        builder.setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_NONE)
                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThrows(IllegalStateException.class, () -> builder.build());

        builder.setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES);
        assertThrows(IllegalStateException.class, () -> builder.build());

        builder.setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE);
        assertThat(builder.build()).isNotNull();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_APP_FUNCTIONS)  // setDescription
    public void testEquals_failure_differentDescription() {
        AppSearchSchema.Builder schemaBuilder =
                new AppSearchSchema.Builder("Email")
                        .setDescription("A type of electronic message")
                        .addProperty(
                                new StringPropertyConfig.Builder("subject")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build());
        AppSearchSchema schema1 = schemaBuilder.build();
        AppSearchSchema schema2 =
                schemaBuilder.setDescription("Mail, but like with an 'e'").build();
        assertThat(schema1).isNotEqualTo(schema2);
        assertThat(schema1.hashCode()).isNotEqualTo(schema2.hashCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_APP_FUNCTIONS)  // setDescription
    public void testEquals_failure_differentPropertyDescription() {
        AppSearchSchema schema1 =
                new AppSearchSchema.Builder("Email")
                        .setDescription("A type of electronic message")
                        .addProperty(
                                new StringPropertyConfig.Builder("subject")
                                        .setDescription("A summary of the contents of the email.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                .build();
        AppSearchSchema schema2 =
                new AppSearchSchema.Builder("Email")
                        .setDescription("A type of electronic message")
                        .addProperty(
                                new StringPropertyConfig.Builder("subject")
                                        .setDescription("The beginning of a message.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                .build();
        assertThat(schema1).isNotEqualTo(schema2);
        assertThat(schema1.hashCode()).isNotEqualTo(schema2.hashCode());
    }

    @Test
    public void testInvalidStringPropertyConfigsTokenizerNonNone() {
        // Setting indexing type to be NONE with tokenizer type PLAIN or VERBATIM or RFC822 should
        // fail. Regardless of whether NONE is set explicitly or just kept as default.
        final StringPropertyConfig.Builder builder1 =
                new StringPropertyConfig.Builder("property")
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN);
        final StringPropertyConfig.Builder builder2 =
                new StringPropertyConfig.Builder("property")
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM);
        final StringPropertyConfig.Builder builder3 =
                new StringPropertyConfig.Builder("property")
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_RFC822);
        assertThrows(IllegalStateException.class, () -> builder1.build());
        assertThrows(IllegalStateException.class, () -> builder2.build());
        assertThrows(IllegalStateException.class, () -> builder3.build());

        builder1.setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE);
        builder2.setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE);
        builder3.setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE);
        assertThrows(IllegalStateException.class, () -> builder1.build());
        assertThrows(IllegalStateException.class, () -> builder2.build());
        assertThrows(IllegalStateException.class, () -> builder3.build());

        // Setting indexing type to be something other than NONE with tokenizer type PLAIN should
        // be just fine.
        builder1.setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        builder2.setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        builder3.setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThat(builder1.build()).isNotNull();
        assertThat(builder2.build()).isNotNull();
        assertThat(builder3.build()).isNotNull();

        builder1.setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES);
        builder2.setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES);
        builder3.setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES);
        assertThat(builder1.build()).isNotNull();
        assertThat(builder2.build()).isNotNull();
        assertThat(builder3.build()).isNotNull();
    }

    @Test
    public void testInvalidStringPropertyConfigsJoinableValueType() {
        // Setting cardinality to be REPEATED with joinable value type QUALIFIED_ID should fail.
        final StringPropertyConfig.Builder builder =
                new StringPropertyConfig.Builder("qualifiedId")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setJoinableValueType(
                                StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);
        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> builder.build());
        assertThat(e).hasMessageThat().contains(
                "Cannot set JOINABLE_VALUE_TYPE_QUALIFIED_ID with CARDINALITY_REPEATED.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_APP_FUNCTIONS)  // setDescription
    public void testAppSearchSchema_toString() {
        AppSearchSchema schema =
                new AppSearchSchema.Builder("testSchema")
                        .setDescription("a test schema")
                        .addProperty(
                                new StringPropertyConfig.Builder("string1")
                                        .setDescription("first string")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_NONE)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("string2")
                                        .setDescription("second string")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("string3")
                                        .setDescription("third string")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("string4")
                                        .setDescription("fourth string")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("string5")
                                        .setDescription("fifth string")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_RFC822)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("qualifiedId1")
                                        .setDescription("first qualifiedId")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("qualifiedId2")
                                        .setDescription("second qualifiedId")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.LongPropertyConfig.Builder("long")
                                        .setDescription("a long")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_NONE)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.LongPropertyConfig.Builder("indexableLong")
                                        .setDescription("an indexed long")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DoublePropertyConfig.Builder("double")
                                        .setDescription("a double")
                                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.BooleanPropertyConfig.Builder("boolean")
                                        .setDescription("a boolean")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.BytesPropertyConfig.Builder("bytes")
                                        .setDescription("some bytes")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                                "document", AppSearchEmail.SCHEMA_TYPE)
                                        .setDescription("a document")
                                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                        .build();

        String schemaString = schema.toString();

        String expectedString =
                "{\n"
                        + "  schemaType: \"testSchema\",\n"
                        + "  description: \"a test schema\",\n"
                        + "  properties: [\n"
                        + "    {\n"
                        + "      name: \"boolean\",\n"
                        + "      description: \"a boolean\",\n"
                        + "      cardinality: CARDINALITY_REQUIRED,\n"
                        + "      dataType: DATA_TYPE_BOOLEAN,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"bytes\",\n"
                        + "      description: \"some bytes\",\n"
                        + "      cardinality: CARDINALITY_OPTIONAL,\n"
                        + "      dataType: DATA_TYPE_BYTES,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"document\",\n"
                        + "      description: \"a document\",\n"
                        + "      shouldIndexNestedProperties: true,\n"
                        + "      schemaType: \"builtin:Email\",\n"
                        + "      cardinality: CARDINALITY_REPEATED,\n"
                        + "      dataType: DATA_TYPE_DOCUMENT,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"double\",\n"
                        + "      description: \"a double\",\n"
                        + "      cardinality: CARDINALITY_REPEATED,\n"
                        + "      dataType: DATA_TYPE_DOUBLE,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"indexableLong\",\n"
                        + "      description: \"an indexed long\",\n"
                        + "      indexingType: INDEXING_TYPE_RANGE,\n"
                        + "      cardinality: CARDINALITY_OPTIONAL,\n"
                        + "      dataType: DATA_TYPE_LONG,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"long\",\n"
                        + "      description: \"a long\",\n"
                        + "      indexingType: INDEXING_TYPE_NONE,\n"
                        + "      cardinality: CARDINALITY_OPTIONAL,\n"
                        + "      dataType: DATA_TYPE_LONG,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"qualifiedId1\",\n"
                        + "      description: \"first qualifiedId\",\n"
                        + "      indexingType: INDEXING_TYPE_NONE,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_NONE,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_QUALIFIED_ID,\n"
                        + "      cardinality: CARDINALITY_REQUIRED,\n"
                        + "      dataType: DATA_TYPE_STRING,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"qualifiedId2\",\n"
                        + "      description: \"second qualifiedId\",\n"
                        + "      indexingType: INDEXING_TYPE_NONE,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_NONE,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_QUALIFIED_ID,\n"
                        + "      cardinality: CARDINALITY_OPTIONAL,\n"
                        + "      dataType: DATA_TYPE_STRING,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"string1\",\n"
                        + "      description: \"first string\",\n"
                        + "      indexingType: INDEXING_TYPE_NONE,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_NONE,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_NONE,\n"
                        + "      cardinality: CARDINALITY_REQUIRED,\n"
                        + "      dataType: DATA_TYPE_STRING,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"string2\",\n"
                        + "      description: \"second string\",\n"
                        + "      indexingType: INDEXING_TYPE_EXACT_TERMS,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_PLAIN,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_NONE,\n"
                        + "      cardinality: CARDINALITY_REQUIRED,\n"
                        + "      dataType: DATA_TYPE_STRING,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"string3\",\n"
                        + "      description: \"third string\",\n"
                        + "      indexingType: INDEXING_TYPE_PREFIXES,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_PLAIN,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_NONE,\n"
                        + "      cardinality: CARDINALITY_REQUIRED,\n"
                        + "      dataType: DATA_TYPE_STRING,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"string4\",\n"
                        + "      description: \"fourth string\",\n"
                        + "      indexingType: INDEXING_TYPE_PREFIXES,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_VERBATIM,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_NONE,\n"
                        + "      cardinality: CARDINALITY_REQUIRED,\n"
                        + "      dataType: DATA_TYPE_STRING,\n"
                        + "    },\n"
                        + "    {\n"
                        + "      name: \"string5\",\n"
                        + "      description: \"fifth string\",\n"
                        + "      indexingType: INDEXING_TYPE_PREFIXES,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_RFC822,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_NONE,\n"
                        + "      cardinality: CARDINALITY_REQUIRED,\n"
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
    public void testAppSearchSchema_toStringNoDescriptionSet() {
        AppSearchSchema schema =
                new AppSearchSchema.Builder("testSchema")
                        .addProperty(
                                new StringPropertyConfig.Builder("string1")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_NONE)
                                        .build())
                        .build();

        String schemaString = schema.toString();

        String expectedString =
                          "{\n"
                        + "  schemaType: \"testSchema\",\n"
                        + "  description: \"\",\n"
                        + "  properties: [\n"
                        + "    {\n"
                        + "      name: \"string1\",\n"
                        + "      description: \"\",\n"
                        + "      indexingType: INDEXING_TYPE_NONE,\n"
                        + "      tokenizerType: TOKENIZER_TYPE_NONE,\n"
                        + "      joinableValueType: JOINABLE_VALUE_TYPE_NONE,\n"
                        + "      cardinality: CARDINALITY_REQUIRED,\n"
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
    public void testStringPropertyConfig_setTokenizerType() {
        assertThrows(IllegalArgumentException.class, () ->
                new StringPropertyConfig.Builder("subject").setTokenizerType(5).build());
        assertThrows(IllegalArgumentException.class, () ->
                new StringPropertyConfig.Builder("subject").setTokenizerType(4).build());
        assertThrows(IllegalArgumentException.class, () ->
                new StringPropertyConfig.Builder("subject").setTokenizerType(-1).build());
    }

    @Test
    public void testStringPropertyConfig_setJoinableValueType() {
        assertThrows(IllegalArgumentException.class, () ->
                new StringPropertyConfig.Builder("qualifiedId").setJoinableValueType(5).build());
        assertThrows(IllegalArgumentException.class, () ->
                new StringPropertyConfig.Builder("qualifiedId").setJoinableValueType(2).build());
        assertThrows(IllegalArgumentException.class, () ->
                new StringPropertyConfig.Builder("qualifiedId").setJoinableValueType(-1).build());
    }

    @Test
    public void testLongPropertyConfig_setIndexingType() {
        assertThrows(IllegalArgumentException.class, () ->
                new LongPropertyConfig.Builder("timestamp").setIndexingType(5).build());
        assertThrows(IllegalArgumentException.class, () ->
                new LongPropertyConfig.Builder("timestamp").setIndexingType(2).build());
        assertThrows(IllegalArgumentException.class, () ->
                new LongPropertyConfig.Builder("timestamp").setIndexingType(-1).build());
    }

    @Test
    public void testInvalidDocumentPropertyConfig_indexableNestedProperties() {
        // Adding indexableNestedProperties with shouldIndexNestedProperties=true should fail.
        AppSearchSchema.DocumentPropertyConfig.Builder builder =
                new AppSearchSchema.DocumentPropertyConfig.Builder("prop1", "Schema1")
                        .setShouldIndexNestedProperties(true)
                        .addIndexableNestedProperties(Collections.singleton("prop1"));
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> builder.build());
        assertThat(e)
                .hasMessageThat()
                .contains(
                        "DocumentIndexingConfig#shouldIndexNestedProperties is required to be false"
                                + " when one or more indexableNestedProperties are provided.");

        builder.addIndexableNestedProperties(Collections.singleton("prop1.prop2"));
        e = assertThrows(IllegalArgumentException.class, () -> builder.build());
        assertThat(e)
                .hasMessageThat()
                .contains(
                        "DocumentIndexingConfig#shouldIndexNestedProperties is required to be false"
                                + " when one or more indexableNestedProperties are provided.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testEmbeddingPropertyConfig() {
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
                                new AppSearchSchema.LongPropertyConfig.Builder("indexableLong")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                AppSearchSchema.LongPropertyConfig
                                                        .INDEXING_TYPE_RANGE)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "document1", AppSearchEmail.SCHEMA_TYPE)
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                AppSearchSchema.EmbeddingPropertyConfig
                                                        .INDEXING_TYPE_NONE)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.EmbeddingPropertyConfig.Builder(
                                        "indexableEmbedding")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                AppSearchSchema.EmbeddingPropertyConfig
                                                        .INDEXING_TYPE_SIMILARITY)
                                        .build())
                        .build();

        assertThat(schema.getSchemaType()).isEqualTo("Test");
        List<AppSearchSchema.PropertyConfig> properties = schema.getProperties();
        assertThat(properties).hasSize(5);

        assertThat(properties.get(0).getName()).isEqualTo("string");
        assertThat(properties.get(0).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(((AppSearchSchema.StringPropertyConfig) properties.get(0)).getIndexingType())
                .isEqualTo(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThat(((AppSearchSchema.StringPropertyConfig) properties.get(0)).getTokenizerType())
                .isEqualTo(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN);

        assertThat(properties.get(1).getName()).isEqualTo("indexableLong");
        assertThat(properties.get(1).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(((AppSearchSchema.LongPropertyConfig) properties.get(1)).getIndexingType())
                .isEqualTo(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE);

        assertThat(properties.get(2).getName()).isEqualTo("document1");
        assertThat(properties.get(2).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED);
        assertThat(((AppSearchSchema.DocumentPropertyConfig) properties.get(2)).getSchemaType())
                .isEqualTo(AppSearchEmail.SCHEMA_TYPE);
        assertThat(
                ((AppSearchSchema.DocumentPropertyConfig) properties.get(2))
                        .shouldIndexNestedProperties())
                .isEqualTo(true);

        assertThat(properties.get(3).getName()).isEqualTo("embedding");
        assertThat(properties.get(3).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(((AppSearchSchema.EmbeddingPropertyConfig) properties.get(3)).getIndexingType())
                .isEqualTo(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE);

        assertThat(properties.get(4).getName()).isEqualTo("indexableEmbedding");
        assertThat(properties.get(4).getCardinality())
                .isEqualTo(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(((AppSearchSchema.EmbeddingPropertyConfig) properties.get(4)).getIndexingType())
                .isEqualTo(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testEmbeddingPropertyConfig_defaultValues() {
        AppSearchSchema.EmbeddingPropertyConfig builder =
                new AppSearchSchema.EmbeddingPropertyConfig.Builder("test").build();
        assertThat(builder.getIndexingType()).isEqualTo(
                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE);
        assertThat(builder.getCardinality()).isEqualTo(
                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testEmbeddingPropertyConfig_setIndexingType() {
        assertThrows(IllegalArgumentException.class, () ->
                new AppSearchSchema.EmbeddingPropertyConfig.Builder("titleEmbedding")
                        .setIndexingType(5).build());
        assertThrows(IllegalArgumentException.class, () ->
                new AppSearchSchema.EmbeddingPropertyConfig.Builder("titleEmbedding")
                        .setIndexingType(2).build());
        assertThrows(IllegalArgumentException.class, () ->
                new AppSearchSchema.EmbeddingPropertyConfig.Builder("titleEmbedding")
                        .setIndexingType(-1).build());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testAppSearchSchemaBuilder_copyConstructor() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                )
                .addParentType("Document").build();
        AppSearchSchema schemaCopy = new AppSearchSchema.Builder(schema).build();
        assertThat(schemaCopy.getSchemaType()).isEqualTo(schema.getSchemaType());
        assertThat(schemaCopy.getProperties()).isEqualTo(schema.getProperties());
        assertThat(schemaCopy.getParentTypes()).isEqualTo(schema.getParentTypes());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testAppSearchSchemaBuilder_setSchemaType() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .setSchemaType("Email2").build();
        assertThat(schema.getSchemaType()).isEqualTo("Email2");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testAppSearchSchemaBuilder_clearProperties() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                )
                .clearProperties().build();
        assertThat(schema.getSchemaType()).isEqualTo("Email");
        assertThat(schema.getProperties()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testAppSearchSchemaBuilder_clearParentTypes() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addParentType("Document").addParentType("Thing").clearParentTypes()
                .build();
        assertThat(schema.getSchemaType()).isEqualTo("Email");
        assertThat(schema.getParentTypes()).isEmpty();
    }
}
