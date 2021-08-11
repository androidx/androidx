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
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.util.AppSearchEmail;

import org.junit.Test;

import java.util.List;

public class AppSearchSchemaCtsTest {
    @Test
    public void testInvalidEnums() {
        StringPropertyConfig.Builder builder = new StringPropertyConfig.Builder("test");
        assertThrows(IllegalArgumentException.class, () -> builder.setCardinality(99));
    }

    @Test
    public void testDefaultValues() {
        StringPropertyConfig builder = new StringPropertyConfig.Builder("test").build();
        assertThat(builder.getIndexingType()).isEqualTo(StringPropertyConfig.INDEXING_TYPE_NONE);
        assertThat(builder.getTokenizerType()).isEqualTo(StringPropertyConfig.TOKENIZER_TYPE_NONE);
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
    public void testPropertyConfig() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("string")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("double")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("boolean")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .build())
                .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("bytes")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "document", AppSearchEmail.SCHEMA_TYPE)
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();

        assertThat(schema.getSchemaType()).isEqualTo("Test");
        List<PropertyConfig> properties = schema.getProperties();
        assertThat(properties).hasSize(6);

        assertThat(properties.get(0).getName()).isEqualTo("string");
        assertThat(properties.get(0).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(((StringPropertyConfig) properties.get(0)).getIndexingType())
                .isEqualTo(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThat(((StringPropertyConfig) properties.get(0)).getTokenizerType())
                .isEqualTo(StringPropertyConfig.TOKENIZER_TYPE_PLAIN);

        assertThat(properties.get(1).getName()).isEqualTo("long");
        assertThat(properties.get(1).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(properties.get(1)).isInstanceOf(AppSearchSchema.LongPropertyConfig.class);

        assertThat(properties.get(2).getName()).isEqualTo("double");
        assertThat(properties.get(2).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REPEATED);
        assertThat(properties.get(2)).isInstanceOf(AppSearchSchema.DoublePropertyConfig.class);

        assertThat(properties.get(3).getName()).isEqualTo("boolean");
        assertThat(properties.get(3).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(properties.get(3)).isInstanceOf(AppSearchSchema.BooleanPropertyConfig.class);

        assertThat(properties.get(4).getName()).isEqualTo("bytes");
        assertThat(properties.get(4).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(properties.get(4)).isInstanceOf(AppSearchSchema.BytesPropertyConfig.class);

        assertThat(properties.get(5).getName()).isEqualTo("document");
        assertThat(properties.get(5).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REPEATED);
        assertThat(((AppSearchSchema.DocumentPropertyConfig) properties.get(5)).getSchemaType())
                .isEqualTo(AppSearchEmail.SCHEMA_TYPE);
        assertThat(((AppSearchSchema.DocumentPropertyConfig) properties.get(5))
                .shouldIndexNestedProperties()).isEqualTo(true);
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
    public void testInvalidStringPropertyConfigsTokenizerPlain() {
        // Setting indexing type to be NONE with tokenizer type PLAIN should fail. Regardless of
        // whether NONE is set explicitly or just kept as default.
        final StringPropertyConfig.Builder builder =
                new StringPropertyConfig.Builder("property")
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN);
        assertThrows(IllegalStateException.class, () -> builder.build());

        builder.setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE);
        assertThrows(IllegalStateException.class, () -> builder.build());

        // Setting indexing type to be something other than NONE with tokenizer type PLAIN should
        // be just fine.
        builder.setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThat(builder.build()).isNotNull();

        builder.setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES);
        assertThat(builder.build()).isNotNull();
    }

    @Test
    public void testAppSearchSchema_toString() {
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new StringPropertyConfig.Builder("string1")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_NONE)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("string2")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("string3")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("double")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("boolean")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .build())
                .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("bytes")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "document", AppSearchEmail.SCHEMA_TYPE)
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();

        String schemaString = schema.toString();

        String expectedString = "{\n"
                + "  schemaType: \"testSchema\",\n"
                + "  properties: [\n"
                + "    {\n"
                + "      name: \"boolean\",\n"
                + "      cardinality: CARDINALITY_REQUIRED,\n"
                + "      dataType: DATA_TYPE_BOOLEAN,\n"
                + "    },\n"
                + "    {\n"
                + "      name: \"bytes\",\n"
                + "      cardinality: CARDINALITY_OPTIONAL,\n"
                + "      dataType: DATA_TYPE_BYTES,\n"
                + "    },\n"
                + "    {\n"
                + "      name: \"document\",\n"
                + "      shouldIndexNestedProperties: true,\n"
                + "      schemaType: \"builtin:Email\",\n"
                + "      cardinality: CARDINALITY_REPEATED,\n"
                + "      dataType: DATA_TYPE_DOCUMENT,\n"
                + "    },\n"
                + "    {\n"
                + "      name: \"double\",\n"
                + "      cardinality: CARDINALITY_REPEATED,\n"
                + "      dataType: DATA_TYPE_DOUBLE,\n"
                + "    },\n"
                + "    {\n"
                + "      name: \"long\",\n"
                + "      cardinality: CARDINALITY_OPTIONAL,\n"
                + "      dataType: DATA_TYPE_LONG,\n"
                + "    },\n"
                + "    {\n"
                + "      name: \"string1\",\n"
                + "      indexingType: INDEXING_TYPE_NONE,\n"
                + "      tokenizerType: TOKENIZER_TYPE_NONE,\n"
                + "      cardinality: CARDINALITY_REQUIRED,\n"
                + "      dataType: DATA_TYPE_STRING,\n"
                + "    },\n"
                + "    {\n"
                + "      name: \"string2\",\n"
                + "      indexingType: INDEXING_TYPE_EXACT_TERMS,\n"
                + "      tokenizerType: TOKENIZER_TYPE_PLAIN,\n"
                + "      cardinality: CARDINALITY_REQUIRED,\n"
                + "      dataType: DATA_TYPE_STRING,\n"
                + "    },\n"
                + "    {\n"
                + "      name: \"string3\",\n"
                + "      indexingType: INDEXING_TYPE_PREFIXES,\n"
                + "      tokenizerType: TOKENIZER_TYPE_PLAIN,\n"
                + "      cardinality: CARDINALITY_REQUIRED,\n"
                + "      dataType: DATA_TYPE_STRING,\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        assertThat(schemaString).isEqualTo(expectedString);
    }
}
