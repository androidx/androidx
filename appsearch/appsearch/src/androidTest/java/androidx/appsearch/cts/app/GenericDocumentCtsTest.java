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

import androidx.appsearch.app.GenericDocument;

import org.junit.Test;

public class GenericDocumentCtsTest {
    private static final byte[] sByteArray1 = new byte[]{(byte) 1, (byte) 2, (byte) 3};
    private static final byte[] sByteArray2 = new byte[]{(byte) 4, (byte) 5, (byte) 6, (byte) 7};
    private static final GenericDocument sDocumentProperties1 = new GenericDocument
            .Builder<>("namespace", "sDocumentProperties1", "sDocumentPropertiesSchemaType1")
            .setCreationTimestampMillis(12345L)
            .build();
    private static final GenericDocument sDocumentProperties2 = new GenericDocument
            .Builder<>("namespace", "sDocumentProperties2", "sDocumentPropertiesSchemaType2")
            .setCreationTimestampMillis(6789L)
            .build();

    @Test
    public void testMaxIndexedProperties() {
        assertThat(GenericDocument.getMaxIndexedProperties()).isEqualTo(16);
    }

    @Test
    public void testDocumentEquals_identical() {
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setTtlMillis(1L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .setPropertyDouble("doubleKey1", 1.0, 2.0, 3.0)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .setPropertyString("stringKey1", "test-value1", "test-value2", "test-value3")
                .setPropertyBytes("byteKey1", sByteArray1, sByteArray2)
                .setPropertyDocument("documentKey1", sDocumentProperties1, sDocumentProperties2)
                .build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setTtlMillis(1L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .setPropertyDouble("doubleKey1", 1.0, 2.0, 3.0)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .setPropertyString("stringKey1", "test-value1", "test-value2", "test-value3")
                .setPropertyBytes("byteKey1", sByteArray1, sByteArray2)
                .setPropertyDocument("documentKey1", sDocumentProperties1, sDocumentProperties2)
                .build();
        assertThat(document1).isEqualTo(document2);
        assertThat(document1.hashCode()).isEqualTo(document2.hashCode());
    }

    @Test
    public void testDocumentEquals_differentOrder() {
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .setPropertyBytes("byteKey1", sByteArray1, sByteArray2)
                .setPropertyDouble("doubleKey1", 1.0, 2.0, 3.0)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .setPropertyDocument("documentKey1", sDocumentProperties1, sDocumentProperties2)
                .setPropertyString("stringKey1", "test-value1", "test-value2", "test-value3")
                .build();

        // Create second document with same parameter but different order.
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .setPropertyDocument("documentKey1", sDocumentProperties1, sDocumentProperties2)
                .setPropertyString("stringKey1", "test-value1", "test-value2", "test-value3")
                .setPropertyDouble("doubleKey1", 1.0, 2.0, 3.0)
                .setPropertyBytes("byteKey1", sByteArray1, sByteArray2)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .build();
        assertThat(document1).isEqualTo(document2);
        assertThat(document1.hashCode()).isEqualTo(document2.hashCode());
    }

    @Test
    public void testDocumentEquals_failure() {
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .build();

        // Create second document with same order but different value.
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 4L) // Different
                .build();
        assertThat(document1).isNotEqualTo(document2);
        assertThat(document1.hashCode()).isNotEqualTo(document2.hashCode());
    }

    @Test
    public void testDocumentEquals_repeatedFieldOrder_failure() {
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .build();

        // Create second document with same order but different value.
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyBoolean("booleanKey1", true, true, false) // Different
                .build();
        assertThat(document1).isNotEqualTo(document2);
        assertThat(document1.hashCode()).isNotEqualTo(document2.hashCode());
    }

    @Test
    public void testDocumentGetSingleValue() {
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "schemaType1")
                .setCreationTimestampMillis(5L)
                .setScore(1)
                .setTtlMillis(1L)
                .setPropertyLong("longKey1", 1L)
                .setPropertyDouble("doubleKey1", 1.0)
                .setPropertyBoolean("booleanKey1", true)
                .setPropertyString("stringKey1", "test-value1")
                .setPropertyBytes("byteKey1", sByteArray1)
                .setPropertyDocument("documentKey1", sDocumentProperties1)
                .build();
        assertThat(document.getId()).isEqualTo("id1");
        assertThat(document.getTtlMillis()).isEqualTo(1L);
        assertThat(document.getSchemaType()).isEqualTo("schemaType1");
        assertThat(document.getCreationTimestampMillis()).isEqualTo(5);
        assertThat(document.getScore()).isEqualTo(1);
        assertThat(document.getPropertyLong("longKey1")).isEqualTo(1L);
        assertThat(document.getPropertyDouble("doubleKey1")).isEqualTo(1.0);
        assertThat(document.getPropertyBoolean("booleanKey1")).isTrue();
        assertThat(document.getPropertyString("stringKey1")).isEqualTo("test-value1");
        assertThat(document.getPropertyBytes("byteKey1"))
                .asList().containsExactly((byte) 1, (byte) 2, (byte) 3).inOrder();
        assertThat(document.getPropertyDocument("documentKey1")).isEqualTo(sDocumentProperties1);

        assertThat(document.getProperty("longKey1")).isInstanceOf(long[].class);
        assertThat((long[]) document.getProperty("longKey1")).asList().containsExactly(1L);
        assertThat(document.getProperty("doubleKey1")).isInstanceOf(double[].class);
        assertThat((double[]) document.getProperty("doubleKey1")).usingTolerance(
                0.05).containsExactly(1.0);
        assertThat(document.getProperty("booleanKey1")).isInstanceOf(boolean[].class);
        assertThat((boolean[]) document.getProperty("booleanKey1")).asList().containsExactly(true);
        assertThat(document.getProperty("stringKey1")).isInstanceOf(String[].class);
        assertThat((String[]) document.getProperty("stringKey1")).asList().containsExactly(
                "test-value1");
        assertThat(document.getProperty("byteKey1")).isInstanceOf(byte[][].class);
        assertThat((byte[][]) document.getProperty("byteKey1")).asList().containsExactly(
                sByteArray1).inOrder();
        assertThat(document.getProperty("documentKey1")).isInstanceOf(GenericDocument[].class);
        assertThat(
                (GenericDocument[]) document.getProperty("documentKey1")).asList().containsExactly(
                sDocumentProperties1);
    }

    @Test
    public void testDocumentGetArrayValues() {
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .setPropertyDouble("doubleKey1", 1.0, 2.0, 3.0)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .setPropertyString("stringKey1", "test-value1", "test-value2", "test-value3")
                .setPropertyBytes("byteKey1", sByteArray1, sByteArray2)
                .setPropertyDocument("documentKey1", sDocumentProperties1, sDocumentProperties2)
                .build();

        assertThat(document.getId()).isEqualTo("id1");
        assertThat(document.getSchemaType()).isEqualTo("schemaType1");
        assertThat(document.getPropertyLongArray("longKey1")).asList()
                .containsExactly(1L, 2L, 3L).inOrder();
        assertThat(document.getPropertyDoubleArray("doubleKey1")).usingExactEquality()
                .containsExactly(1.0, 2.0, 3.0).inOrder();
        assertThat(document.getPropertyBooleanArray("booleanKey1")).asList()
                .containsExactly(true, false, true).inOrder();
        assertThat(document.getPropertyStringArray("stringKey1")).asList()
                .containsExactly("test-value1", "test-value2", "test-value3").inOrder();
        assertThat(document.getPropertyBytesArray("byteKey1")).asList()
                .containsExactly(sByteArray1, sByteArray2).inOrder();
        assertThat(document.getPropertyDocumentArray("documentKey1")).asList()
                .containsExactly(sDocumentProperties1, sDocumentProperties2).inOrder();
    }

    @Test
    public void testDocument_toString() {
        GenericDocument nestedDocValue = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace", "id2", "schemaType2")
                .setCreationTimestampMillis(1L)
                .setScore(1)
                .setTtlMillis(1L)
                .setPropertyString("stringKey1", "val1", "val2")
                .build();
        GenericDocument document =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("namespace", "id1",
                        "schemaType1")
                        .setCreationTimestampMillis(1L)
                        .setScore(1)
                        .setTtlMillis(1L)
                        .setPropertyString("stringKey1", "val1", "val2")
                        .setPropertyBytes("bytesKey1", new byte[]{(byte) 1, (byte) 2})
                        .setPropertyLong("longKey1", 1L, 2L)
                        .setPropertyDouble("doubleKey1", 1.0, 2.0)
                        .setPropertyBoolean("booleanKey1", true, false)
                        .setPropertyDocument("documentKey1", nestedDocValue)
                        .build();

        String documentString = document.toString();

        String expectedString = "{\n"
                + "  namespace: \"namespace\",\n"
                + "  id: \"id1\",\n"
                + "  score: 1,\n"
                + "  schemaType: \"schemaType1\",\n"
                + "  creationTimestampMillis: 1,\n"
                + "  timeToLiveMillis: 1,\n"
                + "  properties: {\n"
                + "    \"booleanKey1\": [true, false],\n"
                + "    \"bytesKey1\": [[1, 2]],\n"
                + "    \"documentKey1\": [\n"
                + "      {\n"
                + "        namespace: \"namespace\",\n"
                + "        id: \"id2\",\n"
                + "        score: 1,\n"
                + "        schemaType: \"schemaType2\",\n"
                + "        creationTimestampMillis: 1,\n"
                + "        timeToLiveMillis: 1,\n"
                + "        properties: {\n"
                + "          \"stringKey1\": [\"val1\", \"val2\"]\n"
                + "        }\n"
                + "      }\n"
                + "    ],\n"
                + "    \"doubleKey1\": [1.0, 2.0],\n"
                + "    \"longKey1\": [1, 2],\n"
                + "    \"stringKey1\": [\"val1\", \"val2\"]\n"
                + "  }\n"
                + "}";

        assertThat(documentString).isEqualTo(expectedString);
    }

    @Test
    public void testDocumentGetValues_differentTypes() {
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "schemaType1")
                .setScore(1)
                .setPropertyLong("longKey1", 1L)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .setPropertyString("stringKey1", "test-value1", "test-value2", "test-value3")
                .build();

        // Get a value for a key that doesn't exist
        assertThat(document.getPropertyDouble("doubleKey1")).isEqualTo(0.0);
        assertThat(document.getPropertyDoubleArray("doubleKey1")).isNull();

        // Get a value with a single element as an array and as a single value
        assertThat(document.getPropertyLong("longKey1")).isEqualTo(1L);
        assertThat(document.getPropertyLongArray("longKey1")).asList().containsExactly(1L);

        // Get a value with multiple elements as an array and as a single value
        assertThat(document.getPropertyString("stringKey1")).isEqualTo("test-value1");
        assertThat(document.getPropertyStringArray("stringKey1")).asList()
                .containsExactly("test-value1", "test-value2", "test-value3").inOrder();

        // Get a value of the wrong type
        assertThat(document.getPropertyDouble("longKey1")).isEqualTo(0.0);
        assertThat(document.getPropertyDoubleArray("longKey1")).isNull();
    }

    @Test
    public void testDocument_setEmptyValues() {
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "schemaType1")
                .setPropertyBoolean("booleanKey")
                .setPropertyString("stringKey")
                .setPropertyBytes("byteKey")
                .setPropertyDouble("doubleKey")
                .setPropertyDocument("documentKey")
                .setPropertyLong("longKey")
                .build();
        assertThat(document.getPropertyBooleanArray("booleanKey")).isEmpty();
        assertThat(document.getPropertyStringArray("stringKey")).isEmpty();
        assertThat(document.getPropertyBytesArray("byteKey")).isEmpty();
        assertThat(document.getPropertyDoubleArray("doubleKey")).isEmpty();
        assertThat(document.getPropertyDocumentArray("documentKey")).isEmpty();
        assertThat(document.getPropertyLongArray("longKey")).isEmpty();
    }

    @Test
    public void testDocumentInvalid() {
        GenericDocument.Builder<?> builder = new GenericDocument.Builder<>("namespace", "id1",
                "schemaType1");
        String nullString = null;

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setPropertyString("testKey", "string1", nullString));
    }

// @exportToFramework:startStrip()

    // TODO(b/171882200): Expose this test in Android T
    @Test
    public void testDocument_toBuilder() {
        GenericDocument document1 = new GenericDocument.Builder<>(
                /*namespace=*/"", "id1", "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .setPropertyDouble("doubleKey1", 1.0, 2.0, 3.0)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .setPropertyString("stringKey1", "String1", "String2", "String3")
                .setPropertyBytes("byteKey1", sByteArray1, sByteArray2)
                .setPropertyDocument("documentKey1", sDocumentProperties1, sDocumentProperties2)
                .build();
        GenericDocument document2 = document1.toBuilder()
                .setId("id2")
                .setNamespace("namespace2")
                .setPropertyBytes("byteKey1", sByteArray2)
                .setPropertyLong("longKey2", 10L)
                .clearProperty("booleanKey1")
                .build();

        // Make sure old doc hasn't changed
        assertThat(document1.getId()).isEqualTo("id1");
        assertThat(document1.getNamespace()).isEqualTo("");
        assertThat(document1.getPropertyLongArray("longKey1")).asList()
                .containsExactly(1L, 2L, 3L).inOrder();
        assertThat(document1.getPropertyBooleanArray("booleanKey1")).asList()
                .containsExactly(true, false, true).inOrder();
        assertThat(document1.getPropertyLongArray("longKey2")).isNull();

        // Make sure the new doc contains the expected values
        GenericDocument expectedDoc = new GenericDocument.Builder<>(
                "namespace2", "id2", "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .setPropertyLong("longKey2", 10L)
                .setPropertyDouble("doubleKey1", 1.0, 2.0, 3.0)
                .setPropertyString("stringKey1", "String1", "String2", "String3")
                .setPropertyBytes("byteKey1", sByteArray2)
                .setPropertyDocument("documentKey1", sDocumentProperties1, sDocumentProperties2)
                .build();
        assertThat(document2).isEqualTo(expectedDoc);
    }

// @exportToFramework:endStrip()

    @Test
    public void testRetrieveTopLevelProperties() {
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();

        // Top-level repeated properties should be retrievable
        assertThat(doc.getPropertyStringArray("propString")).asList()
                .containsExactly("Goodbye", "Hello").inOrder();
        assertThat(doc.getPropertyLongArray("propInts")).asList()
                .containsExactly(3L, 1L, 4L).inOrder();
        assertThat(doc.getPropertyDoubleArray("propDoubles")).usingTolerance(0.0001)
                .containsExactly(3.14, 0.42).inOrder();
        assertThat(doc.getPropertyBooleanArray("propBools")).asList().containsExactly(false);
        assertThat(doc.getPropertyBytesArray("propBytes")).isEqualTo(new byte[][]{{3, 4}});

        // Top-level repeated properties should retrieve the first element
        assertThat(doc.getPropertyString("propString")).isEqualTo("Goodbye");
        assertThat(doc.getPropertyLong("propInts")).isEqualTo(3);
        assertThat(doc.getPropertyDouble("propDoubles")).isWithin(0.0001)
                .of(3.14);
        assertThat(doc.getPropertyBoolean("propBools")).isFalse();
        assertThat(doc.getPropertyBytes("propBytes")).isEqualTo(new byte[]{3, 4});
    }

    @Test
    public void testRetrieveNestedProperties() {
        GenericDocument innerDoc = new GenericDocument.Builder<>("namespace", "id2", "schema2")
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyDocument("propDocument", innerDoc)
                .build();

        // Document should be retrievable via both array and single getters
        assertThat(doc.getPropertyDocument("propDocument")).isEqualTo(innerDoc);
        assertThat(doc.getPropertyDocumentArray("propDocument")).asList()
                .containsExactly(innerDoc);
        assertThat((GenericDocument[]) doc.getProperty("propDocument")).asList()
                .containsExactly(innerDoc);

        // Nested repeated properties should be retrievable
        assertThat(doc.getPropertyStringArray("propDocument.propString")).asList()
                .containsExactly("Goodbye", "Hello").inOrder();
        assertThat(doc.getPropertyLongArray("propDocument.propInts")).asList()
                .containsExactly(3L, 1L, 4L).inOrder();
        assertThat(doc.getPropertyDoubleArray("propDocument.propDoubles")).usingTolerance(0.0001)
                .containsExactly(3.14, 0.42).inOrder();
        assertThat(doc.getPropertyBooleanArray("propDocument.propBools")).asList()
                .containsExactly(false);
        assertThat(doc.getPropertyBytesArray("propDocument.propBytes")).isEqualTo(
                new byte[][]{{3, 4}});
        assertThat(doc.getProperty("propDocument.propBytes")).isEqualTo(
                new byte[][]{{3, 4}});

        // Nested properties should retrieve the first element
        assertThat(doc.getPropertyString("propDocument.propString"))
                .isEqualTo("Goodbye");
        assertThat(doc.getPropertyLong("propDocument.propInts")).isEqualTo(3);
        assertThat(doc.getPropertyDouble("propDocument.propDoubles")).isWithin(0.0001)
                .of(3.14);
        assertThat(doc.getPropertyBoolean("propDocument.propBools")).isFalse();
        assertThat(doc.getPropertyBytes("propDocument.propBytes")).isEqualTo(new byte[]{3, 4});
    }

    @Test
    public void testRetrieveNestedPropertiesMultipleNestedDocuments() {
        GenericDocument innerDoc0 = new GenericDocument.Builder<>("namespace", "id2", "schema2")
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyString("propStringTwo", "Fee", "Fi")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();
        GenericDocument innerDoc1 = new GenericDocument.Builder<>("namespace", "id3", "schema2")
                .setPropertyString("propString", "Aloha")
                .setPropertyLong("propInts", 7, 5, 6)
                .setPropertyLong("propIntsTwo", 8, 6)
                .setPropertyDouble("propDoubles", 7.14, 0.356)
                .setPropertyBoolean("propBools", true)
                .setPropertyBytes("propBytes", new byte[][]{{8, 9}})
                .build();
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyDocument("propDocument", innerDoc0, innerDoc1)
                .build();

        // Documents should be retrievable via both array and single getters
        assertThat(doc.getPropertyDocument("propDocument")).isEqualTo(innerDoc0);
        assertThat(doc.getPropertyDocumentArray("propDocument")).asList()
                .containsExactly(innerDoc0, innerDoc1).inOrder();
        assertThat((GenericDocument[]) doc.getProperty("propDocument")).asList()
                .containsExactly(innerDoc0, innerDoc1).inOrder();

        // Nested repeated properties should be retrievable and should merge the arrays from the
        // inner documents.
        assertThat(doc.getPropertyStringArray("propDocument.propString")).asList()
                .containsExactly("Goodbye", "Hello", "Aloha").inOrder();
        assertThat(doc.getPropertyLongArray("propDocument.propInts")).asList()
                .containsExactly(3L, 1L, 4L, 7L, 5L, 6L).inOrder();
        assertThat(doc.getPropertyDoubleArray("propDocument.propDoubles")).usingTolerance(0.0001)
                .containsExactly(3.14, 0.42, 7.14, 0.356).inOrder();
        assertThat(doc.getPropertyBooleanArray("propDocument.propBools")).asList()
                .containsExactly(false, true).inOrder();
        assertThat(doc.getPropertyBytesArray("propDocument.propBytes")).isEqualTo(
                new byte[][]{{3, 4}, {8, 9}});
        assertThat(doc.getProperty("propDocument.propBytes")).isEqualTo(
                new byte[][]{{3, 4}, {8, 9}});

        // Nested repeated properties should properly handle properties appearing in only one inner
        // document, but not the other.
        assertThat(
                doc.getPropertyStringArray("propDocument.propStringTwo")).asList()
                .containsExactly("Fee", "Fi").inOrder();
        assertThat(doc.getPropertyLongArray("propDocument.propIntsTwo")).asList()
                .containsExactly(8L, 6L).inOrder();

        // Nested properties should retrieve the first element
        assertThat(doc.getPropertyString("propDocument.propString"))
                .isEqualTo("Goodbye");
        assertThat(doc.getPropertyString("propDocument.propStringTwo"))
                .isEqualTo("Fee");
        assertThat(doc.getPropertyLong("propDocument.propInts")).isEqualTo(3);
        assertThat(doc.getPropertyLong("propDocument.propIntsTwo")).isEqualTo(8L);
        assertThat(doc.getPropertyDouble("propDocument.propDoubles")).isWithin(0.0001)
                .of(3.14);
        assertThat(doc.getPropertyBoolean("propDocument.propBools")).isFalse();
        assertThat(doc.getPropertyBytes("propDocument.propBytes")).isEqualTo(new byte[]{3, 4});
    }

    @Test
    public void testRetrieveTopLevelPropertiesIndex() {
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();

        // Top-level repeated properties should be retrievable
        assertThat(doc.getPropertyStringArray("propString[1]")).asList()
                .containsExactly("Hello");
        assertThat(doc.getPropertyLongArray("propInts[2]")).asList()
                .containsExactly(4L);
        assertThat(doc.getPropertyDoubleArray("propDoubles[0]")).usingTolerance(0.0001)
                .containsExactly(3.14);
        assertThat(doc.getPropertyBooleanArray("propBools[0]")).asList().containsExactly(false);
        assertThat(doc.getPropertyBytesArray("propBytes[0]")).isEqualTo(new byte[][]{{3, 4}});
        assertThat(doc.getProperty("propBytes[0]")).isEqualTo(new byte[][]{{3, 4}});

        // Top-level repeated properties should retrieve the first element
        assertThat(doc.getPropertyString("propString[1]")).isEqualTo("Hello");
        assertThat(doc.getPropertyLong("propInts[2]")).isEqualTo(4L);
        assertThat(doc.getPropertyDouble("propDoubles[0]")).isWithin(0.0001)
                .of(3.14);
        assertThat(doc.getPropertyBoolean("propBools[0]")).isFalse();
        assertThat(doc.getPropertyBytes("propBytes[0]")).isEqualTo(new byte[]{3, 4});
    }

    @Test
    public void testRetrieveTopLevelPropertiesIndexOutOfRange() {
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();

        // Array getters should return null when given a bad index.
        assertThat(doc.getPropertyStringArray("propString[5]")).isNull();

        // Single getters should return default when given a bad index.
        assertThat(doc.getPropertyDouble("propDoubles[7]")).isEqualTo(0.0);
    }

    @Test
    public void testNestedProperties_buildBlankPaths() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> new GenericDocument.Builder<>("namespace", "id1", "schema1")
                        .setPropertyString("", "foo"));
        assertThat(e.getMessage()).isEqualTo("Property name cannot be blank.");

        e = assertThrows(IllegalArgumentException.class,
                () -> new GenericDocument.Builder<>("namespace", "id1", "schema1")
                        .setPropertyDocument("propDoc",
                                new GenericDocument.Builder<>("namespace", "id2", "schema1")
                                        .setPropertyString("", "Bat", "Hawk")
                                        .build()));
        assertThat(e.getMessage()).isEqualTo("Property name cannot be blank.");
    }

    @Test
    public void testNestedProperties_invalidPaths() {
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .setPropertyDocument("propDocs",
                        new GenericDocument.Builder<>("namespace", "id2", "schema1")
                                .setPropertyString("propString", "Cat")
                                .build())
                .build();

        // These paths are invalid because they don't apply to the given document --- these should
        // return null. It's not the querier's fault.
        assertThat(doc.getPropertyStringArray("propString.propInts")).isNull();
        assertThat(doc.getPropertyStringArray("propDocs.propFoo")).isNull();
        assertThat(doc.getPropertyStringArray("propDocs.propNestedString.propFoo")).isNull();
    }

    @Test
    public void testNestedProperties_arrayTypesInvalidPath() {
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1").build();
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyString("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyDocument("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyBoolean("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyDouble("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyLong("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyBytes("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyStringArray("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyDocumentArray("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyBooleanArray("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyDoubleArray("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyLongArray("."));
        assertThrows(IllegalArgumentException.class, () -> doc.getPropertyBytesArray("."));
    }

    @Test
    public void testRetrieveNestedPropertiesIntermediateIndex() {
        GenericDocument innerDoc0 = new GenericDocument.Builder<>("namespace", "id2", "schema2")
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyString("propStringTwo", "Fee", "Fi")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();
        GenericDocument innerDoc1 = new GenericDocument.Builder<>("namespace", "id3", "schema2")
                .setPropertyString("propString", "Aloha")
                .setPropertyLong("propInts", 7, 5, 6)
                .setPropertyLong("propIntsTwo", 8, 6)
                .setPropertyDouble("propDoubles", 7.14, 0.356)
                .setPropertyBoolean("propBools", true)
                .setPropertyBytes("propBytes", new byte[][]{{8, 9}})
                .build();
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyDocument("propDocument", innerDoc0, innerDoc1)
                .build();

        // Documents should be retrievable via both array and single getters
        assertThat(doc.getPropertyDocument("propDocument[1]")).isEqualTo(innerDoc1);
        assertThat(doc.getPropertyDocumentArray("propDocument[1]")).asList()
                .containsExactly(innerDoc1);
        assertThat((GenericDocument[]) doc.getProperty("propDocument[1]")).asList()
                .containsExactly(innerDoc1);

        // Nested repeated properties should be retrievable and should merge the arrays from the
        // inner documents.
        assertThat(doc.getPropertyStringArray("propDocument[1].propString")).asList()
                .containsExactly("Aloha");
        assertThat(doc.getPropertyLongArray("propDocument[0].propInts")).asList()
                .containsExactly(3L, 1L, 4L).inOrder();
        assertThat(doc.getPropertyDoubleArray("propDocument[1].propDoubles")).usingTolerance(0.0001)
                .containsExactly(7.14, 0.356).inOrder();
        assertThat(doc.getPropertyBooleanArray("propDocument[0].propBools")).asList()
                .containsExactly(false);
        assertThat((boolean[]) doc.getProperty("propDocument[0].propBools")).asList()
                .containsExactly(false);
        assertThat(doc.getPropertyBytesArray("propDocument[1].propBytes")).isEqualTo(
                new byte[][]{{8, 9}});

        // Nested repeated properties should properly handle properties appearing in only one inner
        // document, but not the other.
        assertThat(doc.getPropertyStringArray("propDocument[0].propStringTwo")).asList()
                .containsExactly("Fee", "Fi").inOrder();
        assertThat(doc.getPropertyStringArray("propDocument[1].propStringTwo")).isNull();
        assertThat(doc.getPropertyLongArray("propDocument[0].propIntsTwo")).isNull();
        assertThat(doc.getPropertyLongArray("propDocument[1].propIntsTwo")).asList()
                .containsExactly(8L, 6L).inOrder();

        // Nested properties should retrieve the first element
        assertThat(doc.getPropertyString("propDocument[1].propString"))
                .isEqualTo("Aloha");
        assertThat(doc.getPropertyString("propDocument[0].propStringTwo"))
                .isEqualTo("Fee");
        assertThat(doc.getPropertyLong("propDocument[1].propInts")).isEqualTo(7L);
        assertThat(doc.getPropertyLong("propDocument[1].propIntsTwo")).isEqualTo(8L);
        assertThat(doc.getPropertyDouble("propDocument[0].propDoubles"))
                .isWithin(0.0001).of(3.14);
        assertThat(doc.getPropertyBoolean("propDocument[1].propBools")).isTrue();
        assertThat(doc.getPropertyBytes("propDocument[0].propBytes"))
                .isEqualTo(new byte[]{3, 4});
    }

    @Test
    public void testRetrieveNestedPropertiesLeafIndex() {
        GenericDocument innerDoc0 = new GenericDocument.Builder<>("namespace", "id2", "schema2")
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyString("propStringTwo", "Fee", "Fi")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();
        GenericDocument innerDoc1 = new GenericDocument.Builder<>("namespace", "id3", "schema2")
                .setPropertyString("propString", "Aloha")
                .setPropertyLong("propInts", 7, 5, 6)
                .setPropertyLong("propIntsTwo", 8, 6)
                .setPropertyDouble("propDoubles", 7.14, 0.356)
                .setPropertyBoolean("propBools", true)
                .setPropertyBytes("propBytes", new byte[][]{{8, 9}})
                .build();
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyDocument("propDocument", innerDoc0, innerDoc1)
                .build();

        // Nested repeated properties should be retrievable and should merge the arrays from the
        // inner documents.
        assertThat(doc.getPropertyStringArray("propDocument.propString[0]")).asList()
                .containsExactly("Goodbye", "Aloha").inOrder();
        assertThat(doc.getPropertyLongArray("propDocument.propInts[2]")).asList()
                .containsExactly(4L, 6L).inOrder();
        assertThat(doc.getPropertyDoubleArray("propDocument.propDoubles[1]"))
                .usingTolerance(0.0001).containsExactly(0.42, 0.356).inOrder();
        assertThat((double[]) doc.getProperty("propDocument.propDoubles[1]"))
                .usingTolerance(0.0001).containsExactly(0.42, 0.356).inOrder();
        assertThat(doc.getPropertyBooleanArray("propDocument.propBools[0]")).asList()
                .containsExactly(false, true).inOrder();
        assertThat(doc.getPropertyBytesArray("propDocument.propBytes[0]"))
                .isEqualTo(new byte[][]{{3, 4}, {8, 9}});

        // Nested repeated properties should properly handle properties appearing in only one inner
        // document, but not the other.
        assertThat(doc.getPropertyStringArray("propDocument.propStringTwo[0]")).asList()
                .containsExactly("Fee");
        assertThat((String[]) doc.getProperty("propDocument.propStringTwo[0]")).asList()
                .containsExactly("Fee");
        assertThat(doc.getPropertyLongArray("propDocument.propIntsTwo[1]")).asList()
                .containsExactly(6L);

        // Nested properties should retrieve the first element
        assertThat(doc.getPropertyString("propDocument.propString[1]"))
                .isEqualTo("Hello");
        assertThat(doc.getPropertyString("propDocument.propStringTwo[1]"))
                .isEqualTo("Fi");
        assertThat(doc.getPropertyLong("propDocument.propInts[1]"))
                .isEqualTo(1L);
        assertThat(doc.getPropertyLong("propDocument.propIntsTwo[1]")).isEqualTo(6L);
        assertThat(doc.getPropertyDouble("propDocument.propDoubles[1]"))
                .isWithin(0.0001).of(0.42);
        assertThat(doc.getPropertyBoolean("propDocument.propBools[0]")).isFalse();
        assertThat(doc.getPropertyBytes("propDocument.propBytes[0]"))
                .isEqualTo(new byte[]{3, 4});
    }

    @Test
    public void testRetrieveNestedPropertiesIntermediateAndLeafIndices() {
        GenericDocument innerDoc0 = new GenericDocument.Builder<>("namespace", "id2", "schema2")
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyString("propStringTwo", "Fee", "Fi")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();
        GenericDocument innerDoc1 = new GenericDocument.Builder<>("namespace", "id3", "schema2")
                .setPropertyString("propString", "Aloha")
                .setPropertyLong("propInts", 7, 5, 6)
                .setPropertyLong("propIntsTwo", 8, 6)
                .setPropertyDouble("propDoubles", 7.14, 0.356)
                .setPropertyBoolean("propBools", true)
                .setPropertyBytes("propBytes", new byte[][]{{8, 9}})
                .build();
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyDocument("propDocument", innerDoc0, innerDoc1)
                .build();

        // Nested repeated properties should be retrievable and should merge the arrays from the
        // inner documents.
        assertThat(doc.getPropertyStringArray("propDocument[1].propString[0]")).asList()
                .containsExactly("Aloha");
        assertThat(doc.getPropertyLongArray("propDocument[0].propInts[2]")).asList()
                .containsExactly(4L);
        assertThat((long[]) doc.getProperty("propDocument[0].propInts[2]")).asList()
                .containsExactly(4L);
        assertThat(doc.getPropertyDoubleArray("propDocument[1].propDoubles[1]"))
                .usingTolerance(0.0001).containsExactly(0.356);
        assertThat(doc.getPropertyBooleanArray("propDocument[0].propBools[0]")).asList()
                .containsExactly(false);
        assertThat(doc.getPropertyBytesArray("propDocument[1].propBytes[0]"))
                .isEqualTo(new byte[][]{{8, 9}});

        // Nested properties should retrieve the first element
        assertThat(doc.getPropertyString("propDocument[0].propString[1]"))
                .isEqualTo("Hello");
        assertThat(doc.getPropertyString("propDocument[0].propStringTwo[1]"))
                .isEqualTo("Fi");
        assertThat(doc.getPropertyLong("propDocument[1].propInts[1]"))
                .isEqualTo(5L);
        assertThat(doc.getPropertyLong("propDocument[1].propIntsTwo[1]"))
                .isEqualTo(6L);
        assertThat(doc.getPropertyDouble("propDocument[0].propDoubles[1]"))
                .isWithin(0.0001).of(0.42);
        assertThat(doc.getPropertyBoolean("propDocument[1].propBools[0]")).isTrue();
        assertThat(doc.getPropertyBytes("propDocument[0].propBytes[0]"))
                .isEqualTo(new byte[]{3, 4});
    }

    @Test
    public void testDocumentGetPropertyNamesSingleLevel() {
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "schemaType1")
                .setCreationTimestampMillis(5L)
                .setScore(1)
                .setTtlMillis(1L)
                .setPropertyLong("longKey1", 1L)
                .setPropertyDouble("doubleKey1", 1.0)
                .setPropertyBoolean("booleanKey1", true)
                .setPropertyString("stringKey1", "test-value1")
                .setPropertyBytes("byteKey1", sByteArray1)
                .build();
        assertThat(document.getPropertyNames()).containsExactly("longKey1", "doubleKey1",
                "booleanKey1", "stringKey1", "byteKey1");
    }

    @Test
    public void testDocumentGetPropertyNamesMultiLevel() {
        GenericDocument innerDoc0 = new GenericDocument.Builder<>("namespace", "id2", "schema2")
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyString("propStringTwo", "Fee", "Fi")
                .setPropertyLong("propInts", 3, 1, 4)
                .build();
        GenericDocument innerDoc1 = new GenericDocument.Builder<>("namespace", "id3", "schema2")
                .setPropertyString("propString", "Aloha")
                .setPropertyLong("propInts", 7, 5, 6)
                .setPropertyLong("propIntsTwo", 8, 6)
                .build();
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "schemaType1")
                .setCreationTimestampMillis(5L)
                .setScore(1)
                .setTtlMillis(1L)
                .setPropertyString("stringKey1", "test-value1")
                .setPropertyDocument("docKey1", innerDoc0, innerDoc1)
                .build();
        assertThat(document.getPropertyNames()).containsExactly("stringKey1", "docKey1");

        GenericDocument[] documents = document.getPropertyDocumentArray("docKey1");
        assertThat(documents).asList().containsExactly(innerDoc0, innerDoc1).inOrder();
        assertThat(documents[0].getPropertyNames()).containsExactly("propString", "propStringTwo",
                "propInts");
        assertThat(documents[1].getPropertyNames()).containsExactly("propString", "propInts",
                "propIntsTwo");
    }
}
