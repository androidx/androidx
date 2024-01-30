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

package androidx.appsearch.localstorage.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.exceptions.AppSearchException;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.PropertyProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;

import org.junit.Test;

public class PrefixUtilTest {
    @Test
    public void testCreatePrefix() {
        assertThat(PrefixUtil.createPrefix("foo", "bar")).isEqualTo("foo$bar/");
        assertThat(PrefixUtil.createPrefix("pkg", "")).isEqualTo("pkg$/");
        assertThat(PrefixUtil.createPrefix("", "db")).isEqualTo("$db/");
    }

    @Test
    public void testCreatePackagePrefix() {
        assertThat(PrefixUtil.createPackagePrefix("pkg")).isEqualTo("pkg$");
        assertThat(PrefixUtil.createPackagePrefix("")).isEqualTo("$");
    }

    @Test
    public void testGetPackageName() {
        assertThat(PrefixUtil.getPackageName("foo$bar")).isEqualTo("foo");
        assertThat(PrefixUtil.getPackageName("pkg$bar")).isEqualTo("pkg");
        assertThat(PrefixUtil.getPackageName("$bar")).isEqualTo("");
    }

    @Test
    public void testGetPackageName_missingDelimiter() {
        assertThat(PrefixUtil.getPackageName("foo/")).isEqualTo("");
        assertThat(PrefixUtil.getPackageName("pkg")).isEqualTo("");
    }

    @Test
    public void testGetDatabaseName() {
        assertThat(PrefixUtil.getDatabaseName("foo$bar/id")).isEqualTo("bar");
        assertThat(PrefixUtil.getDatabaseName("pkg$db/")).isEqualTo("db");
        assertThat(PrefixUtil.getDatabaseName("$db/id")).isEqualTo("db");
    }

    @Test
    public void testGetDatabaseName_missingPackageDelimiter() {
        assertThat(PrefixUtil.getDatabaseName("foo/")).isEqualTo("");
        assertThat(PrefixUtil.getDatabaseName("pkg")).isEqualTo("");
    }

    @Test
    public void testGetDatabaseName_missingDatabaseDelimiter() {
        assertThat(PrefixUtil.getDatabaseName("foo$bar")).isEqualTo("");
        assertThat(PrefixUtil.getDatabaseName("$db")).isEqualTo("");
    }

    @Test
    public void testGetDatabaseName_databaseDelimiterBeforePackageDelimiter() {
        assertThat(PrefixUtil.getDatabaseName("foo/bar$db")).isEqualTo("");
    }

    @Test
    public void testRemovePrefix() throws AppSearchException {
        assertThat(PrefixUtil.removePrefix("foo$bar/id")).isEqualTo("id");
        assertThat(PrefixUtil.removePrefix("foo/bar")).isEqualTo("bar");
    }

    @Test
    public void testRemovePrefix_missingDelimiter() {
        AppSearchException e = assertThrows(AppSearchException.class, () -> PrefixUtil.removePrefix(
                "foo$bar"));
        assertThat(e).hasMessageThat().isEqualTo(
                "The prefixed value \"foo$bar\" doesn't contain a valid database name");

        e = assertThrows(AppSearchException.class, () -> PrefixUtil.removePrefix("foo"));
        assertThat(e).hasMessageThat().isEqualTo(
                "The prefixed value \"foo\" doesn't contain a valid database name");
    }

    @Test
    public void testGetPrefix() throws AppSearchException {
        assertThat(PrefixUtil.getPrefix("foo$bar/id")).isEqualTo("foo$bar/");
        assertThat(PrefixUtil.getPrefix("foo/bar")).isEqualTo("foo/");
    }

    @Test
    public void testGetPrefix_missingDelimiter() {
        AppSearchException e = assertThrows(AppSearchException.class, () -> PrefixUtil.getPrefix(
                "foo$bar"));
        assertThat(e).hasMessageThat().isEqualTo(
                "The prefixed value \"foo$bar\" doesn't contain a valid database name");

        e = assertThrows(AppSearchException.class, () -> PrefixUtil.getPrefix("foo"));
        assertThat(e).hasMessageThat().isEqualTo(
                "The prefixed value \"foo\" doesn't contain a valid database name");
    }

    @Test
    public void testAddPrefixToDocument() {
        // set up un-prefixed document
        DocumentProto.Builder nestedBuilder = DocumentProto.newBuilder();
        nestedBuilder.setSchema("nestedSchema");
        nestedBuilder.setNamespace("nestedNamespace");

        PropertyProto.Builder propertyBuilder = PropertyProto.newBuilder();
        propertyBuilder.addDocumentValues(nestedBuilder);

        DocumentProto.Builder builder = DocumentProto.newBuilder();
        builder.setSchema("schema");
        builder.setNamespace("namespace");
        builder.addProperties(propertyBuilder);

        // set up prefixed document for assertion
        DocumentProto.Builder prefixedNestedBuilder = DocumentProto.newBuilder();
        prefixedNestedBuilder.setSchema("prefix/nestedSchema");
        prefixedNestedBuilder.setNamespace("prefix/nestedNamespace");

        PropertyProto.Builder prefixedPropertyBuilder = PropertyProto.newBuilder();
        prefixedPropertyBuilder.addDocumentValues(prefixedNestedBuilder);

        DocumentProto.Builder prefixedBuilder = DocumentProto.newBuilder();
        prefixedBuilder.setSchema("prefix/schema");
        prefixedBuilder.setNamespace("prefix/namespace");
        prefixedBuilder.addProperties(prefixedPropertyBuilder);

        // add prefix
        PrefixUtil.addPrefixToDocument(builder, "prefix/");
        assertThat(builder.build()).isEqualTo(prefixedBuilder.build());
    }

    @Test
    public void testRemovePrefixesFromDocument() throws AppSearchException {
        // set up prefixed document
        DocumentProto.Builder prefixedNestedBuilder = DocumentProto.newBuilder();
        prefixedNestedBuilder.setSchema("prefix/schema");
        prefixedNestedBuilder.setNamespace("prefix/namespace");

        PropertyProto.Builder prefixedPropertyBuilder = PropertyProto.newBuilder();
        prefixedPropertyBuilder.addDocumentValues(prefixedNestedBuilder);

        DocumentProto.Builder prefixedBuilder = DocumentProto.newBuilder();
        prefixedBuilder.setSchema("prefix/schema");
        prefixedBuilder.setNamespace("prefix/namespace");
        prefixedBuilder.addProperties(prefixedPropertyBuilder);

        // set up un-prefixed document for assertion
        DocumentProto.Builder nestedBuilder = DocumentProto.newBuilder();
        nestedBuilder.setSchema("schema");
        nestedBuilder.setNamespace("namespace");

        PropertyProto.Builder propertyBuilder = PropertyProto.newBuilder();
        propertyBuilder.addDocumentValues(nestedBuilder);

        DocumentProto.Builder builder = DocumentProto.newBuilder();
        builder.setSchema("schema");
        builder.setNamespace("namespace");
        builder.addProperties(propertyBuilder);

        // remove prefixes
        assertThat(PrefixUtil.removePrefixesFromDocument(prefixedBuilder)).isEqualTo("prefix/");
        assertThat(prefixedBuilder.build()).isEqualTo(builder.build());
    }

    @Test
    public void testRemovePrefixesFromDocument_multiplePrefixNames() {
        DocumentProto.Builder prefixedBuilder = DocumentProto.newBuilder();
        prefixedBuilder.setSchema("prefix/schema");
        prefixedBuilder.setNamespace("suffix/namespace");

        AppSearchException e = assertThrows(AppSearchException.class,
                () -> PrefixUtil.removePrefixesFromDocument(prefixedBuilder));
        assertThat(e).hasMessageThat().isEqualTo("Found unexpected multiple prefix names in "
                + "document: prefix/, suffix/");
    }

    @Test
    public void testRemovePrefixesFromDocument_multiplePrefixNamesFromNested() {
        // nested document prefix is different from outer level prefix
        DocumentProto.Builder prefixedNestedBuilder = DocumentProto.newBuilder();
        prefixedNestedBuilder.setSchema("suffix/schema");
        prefixedNestedBuilder.setNamespace("suffix/namespace");

        PropertyProto.Builder prefixedPropertyBuilder = PropertyProto.newBuilder();
        prefixedPropertyBuilder.addDocumentValues(prefixedNestedBuilder);

        DocumentProto.Builder prefixedBuilder = DocumentProto.newBuilder();
        prefixedBuilder.setSchema("prefix/schema");
        prefixedBuilder.setNamespace("prefix/namespace");
        prefixedBuilder.addProperties(prefixedPropertyBuilder);

        AppSearchException e = assertThrows(AppSearchException.class,
                () -> PrefixUtil.removePrefixesFromDocument(prefixedBuilder));
        assertThat(e).hasMessageThat().isEqualTo("Found unexpected multiple prefix names in "
                + "document: prefix/, suffix/");
    }

    @Test
    public void testRemovePrefixesFromSchemaType() throws AppSearchException {
        // set up prefixed schema type
        PropertyConfigProto.Builder propertyConfigBuilder = PropertyConfigProto.newBuilder();
        propertyConfigBuilder.setSchemaType("prefix/schema");

        SchemaTypeConfigProto.Builder builder = SchemaTypeConfigProto.newBuilder();
        builder.setSchemaType("prefix/schema");
        builder.addParentTypes("prefix/parentSchema1");
        builder.addParentTypes("prefix/parentSchema2");
        builder.addProperties(propertyConfigBuilder);

        // set up un-prefixed schema type for assertion
        PropertyConfigProto.Builder unprefixedPropertyConfigBuilder =
                PropertyConfigProto.newBuilder();
        unprefixedPropertyConfigBuilder.setSchemaType("schema");

        SchemaTypeConfigProto.Builder unprefixedBuilder = SchemaTypeConfigProto.newBuilder();
        unprefixedBuilder.setSchemaType("schema");
        unprefixedBuilder.addParentTypes("parentSchema1");
        unprefixedBuilder.addParentTypes("parentSchema2");
        unprefixedBuilder.addProperties(unprefixedPropertyConfigBuilder);

        // remove prefixes
        assertThat(PrefixUtil.removePrefixesFromSchemaType(builder)).isEqualTo("prefix/");
        assertThat(builder.build()).isEqualTo(unprefixedBuilder.build());
    }
}
