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

package androidx.appsearch.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.PropertyPath;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SchemaUtilTest {
    private Map<String, AppSearchSchema> mSchemas;

    private static final String SCHEMA_TYPE_PERSON = "Person";
    private static final String SCHEMA_TYPE_ADDRESS = "Address";
    private static final String SCHEMA_TYPE_CONTACT = "Contact";
    private static final String SCHEMA_TYPE_BOOK = "Book";
    private static final String SCHEMA_TYPE_LOCATION = "LocationDetails";

    private AppSearchSchema mPersonSchema;
    private AppSearchSchema mAddressSchema;

    @Before
    public void setUp() {
        AppSearchSchema locationSchema = new AppSearchSchema.Builder(SCHEMA_TYPE_LOCATION)
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("latitude")
                        .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
                        .build())
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("longitude")
                        .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
                        .build())
                .build();

        mAddressSchema = new AppSearchSchema.Builder(SCHEMA_TYPE_ADDRESS)
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("street")
                        .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
                        .build())
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("zip_code").build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "location_details", SCHEMA_TYPE_LOCATION)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();

        mPersonSchema = new AppSearchSchema.Builder(SCHEMA_TYPE_PERSON)
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("name")
                        .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
                        .build())
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("age").build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "home_address", SCHEMA_TYPE_ADDRESS)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "contacts", SCHEMA_TYPE_CONTACT)
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(false)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "work_address", SCHEMA_TYPE_ADDRESS)
                        .setShouldIndexNestedProperties(false)
                        .build())
                .build();

        mSchemas = new HashMap<>();
        mSchemas.put(SCHEMA_TYPE_PERSON, mPersonSchema);
        mSchemas.put(SCHEMA_TYPE_ADDRESS, mAddressSchema);
        mSchemas.put(SCHEMA_TYPE_LOCATION, locationSchema);

        mSchemas.put(SCHEMA_TYPE_CONTACT, new AppSearchSchema.Builder(SCHEMA_TYPE_CONTACT).build());
        mSchemas.put(SCHEMA_TYPE_BOOK, new AppSearchSchema.Builder(SCHEMA_TYPE_BOOK).build());
    }

    @Test
    public void testCheckPathRecursive_validSingleSegmentPath() {
        PropertyPath path = new PropertyPath("home_address");

        assertTrue(SchemaUtil.checkPathRecursive(
                mSchemas, mPersonSchema, SCHEMA_TYPE_ADDRESS, path, /*segmentIndex=*/0));
    }

    @Test
    public void testCheckPathRecursive_validMultiSegmentPath() {
        PropertyPath path = new PropertyPath("home_address.location_details");

        assertTrue(SchemaUtil.checkPathRecursive(
                mSchemas, mPersonSchema, SCHEMA_TYPE_LOCATION, path, /*segmentIndex=*/0));
    }

    @Test
    public void testCheckPathRecursive_validPathWithArraySegment() {
        PropertyPath path = new PropertyPath("contacts[0]");

        assertTrue(SchemaUtil.checkPathRecursive(
                mSchemas, mPersonSchema, SCHEMA_TYPE_CONTACT, path, /*segmentIndex=*/0));
    }

    @Test
    public void testCheckPathRecursive_invalidTargetSchemaType() {
        PropertyPath path = new PropertyPath("home_address");

        assertFalse(SchemaUtil.checkPathRecursive(
                mSchemas, mPersonSchema, SCHEMA_TYPE_BOOK, path, /*segmentIndex=*/0));
    }

    @Test
    public void testCheckPathRecursive_invalidSegmentNotFound() {
        PropertyPath path = new PropertyPath("home_address.city");

        assertFalse(SchemaUtil.checkPathRecursive(
                mSchemas, mPersonSchema, SCHEMA_TYPE_BOOK, path, /*segmentIndex=*/0));
    }

    @Test
    public void testCheckPathRecursive_pathTooLong() {
        PropertyPath path = new PropertyPath("home_address.street.invalid");

        assertFalse(SchemaUtil.checkPathRecursive(
                mSchemas, mPersonSchema, SCHEMA_TYPE_BOOK, path, /*segmentIndex=*/0));
    }

    @Test
    public void testCheckPathRecursive_nullCurrentSchemaOnStart() {
        PropertyPath path = new PropertyPath("home_address");
        assertFalse(SchemaUtil.checkPathRecursive(
                mSchemas, /*currentSchema=*/ null, SCHEMA_TYPE_ADDRESS, path, /*segmentIndex=*/0));
    }

    @Test
    public void testCheckPathRecursive_validPathToLeafPropertyLong() {
        PropertyPath path = new PropertyPath("age");

        assertFalse(SchemaUtil.checkPathRecursive(
                mSchemas, mPersonSchema, SCHEMA_TYPE_PERSON, path, /*segmentIndex=*/0));
    }

    @Test
    public void testVerifyPropertyPathSchemaTypes_validPaths() {
        // Setup paths:
        // Person.home_address -> Address (matches expected target)
        // Person.work_address -> Address (matches expected target)
        Map<String, Set<String>> pathsToVerify = new HashMap<>();
        pathsToVerify.put(SCHEMA_TYPE_PERSON, Set.of("home_address", "work_address"));

        // Verification should succeed without exception.
        SchemaUtil.verifyPropertyPathSchemaTypes(mSchemas, pathsToVerify, SCHEMA_TYPE_ADDRESS);
    }

    @Test
    public void testVerifyPropertyPathSchemaTypes_throwsOnInvalidPath() {
        // Path "home_address.non_existent" is invalid.
        Map<String, Set<String>> pathsToVerify = new HashMap<>();
        pathsToVerify.put(SCHEMA_TYPE_PERSON, Set.of("home_address.non_existent"));

        // Expect IllegalArgumentException.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                SchemaUtil.verifyPropertyPathSchemaTypes(
                        mSchemas,
                        pathsToVerify,
                        SCHEMA_TYPE_ADDRESS));

        assertThat(e).hasMessageThat().contains("The property path of: "
                + "home_address.non_existent is not the required property type: Address");
    }

    @Test
    public void testVerifyPropertyPathSchemaTypes_throwsOnWrongTargetType() {
        // Path "home_address" is valid, but it resolves to Address, and we expect LocationDetails.
        Map<String, Set<String>> pathsToVerify = new HashMap<>();
        pathsToVerify.put(SCHEMA_TYPE_PERSON, Set.of("home_address"));

        // Mismatch here: expecting LocationDetails.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                SchemaUtil.verifyPropertyPathSchemaTypes(
                        mSchemas,
                        pathsToVerify,
                        SCHEMA_TYPE_LOCATION));

        assertThat(e).hasMessageThat().contains("The property path of: "
                + "home_address is not the required property type: LocationDetails");
    }

    @Test
    public void testVerifyPropertyPathSchemaTypes_throwsWhenPathEndsAtLeafProperty() {
        // Path "age" is a leaf LongProperty, but we expect a Document type.
        Map<String, Set<String>> pathsToVerify = new HashMap<>();
        pathsToVerify.put(SCHEMA_TYPE_PERSON, Set.of("age"));

        // Expect IllegalArgumentException.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                SchemaUtil.verifyPropertyPathSchemaTypes(
                        mSchemas,
                        pathsToVerify,
                        SCHEMA_TYPE_ADDRESS));

        assertThat(e).hasMessageThat().contains(
                "The property path of: age is not the required property type: Address");
    }
}
