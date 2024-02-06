/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.platformstorage.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.BooleanPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.BytesPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.DocumentPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.exceptions.IllegalSchemaException;
import androidx.collection.ArraySet;

import org.junit.Test;

import java.util.Set;

public class SchemaValidationUtilTest {
    static final int MAX_SECTIONS_ALLOWED = 64;

    @Test
    public void testValidate_simpleSchemas() {
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new BooleanPropertyConfig.Builder("boolProperty")
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("age")
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).addProperty(new BytesPropertyConfig.Builder("byteProperty")
                        .build()
                ).build();

        AppSearchSchema[] schemas = {emailSchema, personSchema};
        // Test that schemas are valid and no exceptions are thrown
        SchemaValidationUtil.checkSchemasAreValidOrThrow(new ArraySet<>(schemas),
                MAX_SECTIONS_ALLOWED);
    }

    @Test
    public void testValidate_nestedSchemas() {
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("org", "Organization")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("sender", "Person")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("recipient", "Person")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).build();

        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("nickname")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_NONE)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("worksFor", "Organization")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("age")
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("address", "Address")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).build();

        AppSearchSchema addressSchema = new AppSearchSchema.Builder("Address")
                .addProperty(new StringPropertyConfig.Builder("streetName")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("zipcode")
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).build();

        AppSearchSchema orgSchema = new AppSearchSchema.Builder("Organization")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("address", "Address")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).build();

        AppSearchSchema[] schemas = {emailSchema, personSchema, addressSchema, orgSchema};
        // Test that schemas are valid and no exceptions are thrown
        SchemaValidationUtil.checkSchemasAreValidOrThrow(new ArraySet<>(schemas),
                MAX_SECTIONS_ALLOWED);
    }

    @Test
    public void testValidate_schemasWithValidCycle() {
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("nickname")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_NONE)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("address", "Address")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("age")
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("worksFor", "Organization")
                        .setShouldIndexNestedProperties(false)
                        .build()
                ).build();

        AppSearchSchema orgSchema = new AppSearchSchema.Builder("Organization")
                .addProperty(new DocumentPropertyConfig.Builder("funder", "Person")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("address", "Address")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("employees", "Person")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("name")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        AppSearchSchema addressSchema = new AppSearchSchema.Builder("Address")
                .addProperty(new StringPropertyConfig.Builder("streetName")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("zipcode")
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).build();

        AppSearchSchema[] schemas = {personSchema, orgSchema, addressSchema};
        // Test that schemas are valid and no exceptions are thrown
        SchemaValidationUtil.checkSchemasAreValidOrThrow(new ArraySet<>(schemas),
                MAX_SECTIONS_ALLOWED);
    }

    @Test
    public void testValidate_maxSections() {
        AppSearchSchema.Builder personSchemaBuilder = new AppSearchSchema.Builder("Person");
        for (int i = 0; i < MAX_SECTIONS_ALLOWED; i++) {
            personSchemaBuilder.addProperty(new StringPropertyConfig.Builder("string" + i)
                    .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                    .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                    .build());
        }
        Set<AppSearchSchema> schemas = new ArraySet<>();
        schemas.add(personSchemaBuilder.build());
        // Test that schemas are valid and no exceptions are thrown
        SchemaValidationUtil.checkSchemasAreValidOrThrow(schemas, MAX_SECTIONS_ALLOWED);

        // Add one more property to bring the number of sections over the max limit
        personSchemaBuilder.addProperty(new StringPropertyConfig.Builder(
                "string" + MAX_SECTIONS_ALLOWED + 1)
                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                .build());
        schemas.clear();
        schemas.add(personSchemaBuilder.build());
        IllegalSchemaException exception = assertThrows(IllegalSchemaException.class,
                () -> SchemaValidationUtil.checkSchemasAreValidOrThrow(schemas,
                        MAX_SECTIONS_ALLOWED));
        assertThat(exception.getMessage()).contains("Too many properties to be indexed");
    }

    @Test
    public void testValidate_schemasWithInvalidCycleThrowsError() {
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("nickname")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_NONE)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("age")
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("worksFor", "Organization")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).build();

        AppSearchSchema orgSchema = new AppSearchSchema.Builder("Organization")
                .addProperty(new DocumentPropertyConfig.Builder("funder", "Person")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("employees", "Person")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).build();

        AppSearchSchema[] schemas = {personSchema, orgSchema};
        IllegalSchemaException exception = assertThrows(IllegalSchemaException.class,
                () -> SchemaValidationUtil.checkSchemasAreValidOrThrow(new ArraySet<>(schemas),
                        MAX_SECTIONS_ALLOWED));
        assertThat(exception.getMessage()).contains("Invalid cycle");
    }

    @Test
    public void testValidate_unknownDocumentConfigThrowsError() {
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("unknown", "Unknown")
                        .setShouldIndexNestedProperties(true)
                        .build()
                ).addProperty(new DocumentPropertyConfig.Builder("unknown2", "Unknown")
                        .setShouldIndexNestedProperties(false)
                        .build()
                ).build();

        AppSearchSchema[] schemas = {emailSchema};
        IllegalSchemaException exception = assertThrows(IllegalSchemaException.class,
                () -> SchemaValidationUtil.checkSchemasAreValidOrThrow(new ArraySet<>(schemas),
                        MAX_SECTIONS_ALLOWED));
        assertThat(exception.getMessage()).contains("Undefined schema type");
    }
}
