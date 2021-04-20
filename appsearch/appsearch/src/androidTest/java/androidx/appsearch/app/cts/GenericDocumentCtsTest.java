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

package androidx.appsearch.app.cts;

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
    public void testDocumentEquals_identical() {
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "uri1",
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
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "uri1",
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
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "uri1",
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
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "uri1",
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
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "uri1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .build();

        // Create second document with same order but different value.
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "uri1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 4L) // Different
                .build();
        assertThat(document1).isNotEqualTo(document2);
        assertThat(document1.hashCode()).isNotEqualTo(document2.hashCode());
    }

    @Test
    public void testDocumentEquals_repeatedFieldOrder_failure() {
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "uri1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .build();

        // Create second document with same order but different value.
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "uri1",
                "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyBoolean("booleanKey1", true, true, false) // Different
                .build();
        assertThat(document1).isNotEqualTo(document2);
        assertThat(document1.hashCode()).isNotEqualTo(document2.hashCode());
    }

    @Test
    public void testDocumentGetSingleValue() {
        GenericDocument document = new GenericDocument.Builder<>("namespace", "uri1", "schemaType1")
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
        assertThat(document.getUri()).isEqualTo("uri1");
        assertThat(document.getTtlMillis()).isEqualTo(1L);
        assertThat(document.getSchemaType()).isEqualTo("schemaType1");
        assertThat(document.getCreationTimestampMillis()).isEqualTo(5);
        assertThat(document.getScore()).isEqualTo(1);
        assertThat(document.getPropertyLong("longKey1")).isEqualTo(1L);
        assertThat(document.getPropertyDouble("doubleKey1")).isEqualTo(1.0);
        assertThat(document.getPropertyBoolean("booleanKey1")).isTrue();
        assertThat(document.getPropertyString("stringKey1")).isEqualTo("test-value1");
        assertThat(document.getPropertyBytes("byteKey1"))
                .asList().containsExactly((byte) 1, (byte) 2, (byte) 3);
        assertThat(document.getPropertyDocument("documentKey1")).isEqualTo(sDocumentProperties1);
    }

    @Test
    public void testDocumentGetArrayValues() {
        GenericDocument document = new GenericDocument.Builder<>("namespace", "uri1", "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .setPropertyDouble("doubleKey1", 1.0, 2.0, 3.0)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .setPropertyString("stringKey1", "test-value1", "test-value2", "test-value3")
                .setPropertyBytes("byteKey1", sByteArray1, sByteArray2)
                .setPropertyDocument("documentKey1", sDocumentProperties1, sDocumentProperties2)
                .build();

        assertThat(document.getUri()).isEqualTo("uri1");
        assertThat(document.getSchemaType()).isEqualTo("schemaType1");
        assertThat(document.getPropertyLongArray("longKey1")).asList().containsExactly(1L, 2L, 3L);
        assertThat(document.getPropertyDoubleArray("doubleKey1")).usingExactEquality()
                .containsExactly(1.0, 2.0, 3.0);
        assertThat(document.getPropertyBooleanArray("booleanKey1")).asList()
                .containsExactly(true, false, true);
        assertThat(document.getPropertyStringArray("stringKey1")).asList()
                .containsExactly("test-value1", "test-value2", "test-value3");
        assertThat(document.getPropertyBytesArray("byteKey1")).asList()
                .containsExactly(sByteArray1, sByteArray2);
        assertThat(document.getPropertyDocumentArray("documentKey1")).asList()
                .containsExactly(sDocumentProperties1, sDocumentProperties2);
    }

    @Test
    public void testDocument_toString() {
        GenericDocument document = new GenericDocument.Builder<>("", "uri1", "schemaType1")
                .setCreationTimestampMillis(5L)
                .setPropertyLong("longKey1", 1L, 2L, 3L)
                .setPropertyDouble("doubleKey1", 1.0, 2.0, 3.0)
                .setPropertyBoolean("booleanKey1", true, false, true)
                .setPropertyString("stringKey1", "String1", "String2", "String3")
                .setPropertyBytes("byteKey1", sByteArray1, sByteArray2)
                .setPropertyDocument("documentKey1", sDocumentProperties1, sDocumentProperties2)
                .build();
        String expectedString = "{ name: 'creationTimestampMillis' value: 5 } "
                + "{ name: 'namespace' value:  } "
                + "{ name: 'properties' value: "
                + "{ name: 'booleanKey1' value: [ 'true' 'false' 'true' ] } "
                + "{ name: 'byteKey1' value: "
                + "{ name: 'byteArray' value: [ '1' '2' '3' ] } "
                + "{ name: 'byteArray' value: [ '4' '5' '6' '7' ] }  } "
                + "{ name: 'documentKey1' value: [ '"
                + "{ name: 'creationTimestampMillis' value: 12345 } "
                + "{ name: 'namespace' value: namespace } "
                + "{ name: 'properties' value:  } "
                + "{ name: 'schemaType' value: sDocumentPropertiesSchemaType1 } "
                + "{ name: 'score' value: 0 } "
                + "{ name: 'ttlMillis' value: 0 } "
                + "{ name: 'uri' value: sDocumentProperties1 } ' '"
                + "{ name: 'creationTimestampMillis' value: 6789 } "
                + "{ name: 'namespace' value: namespace } "
                + "{ name: 'properties' value:  } "
                + "{ name: 'schemaType' value: sDocumentPropertiesSchemaType2 } "
                + "{ name: 'score' value: 0 } "
                + "{ name: 'ttlMillis' value: 0 } "
                + "{ name: 'uri' value: sDocumentProperties2 } ' ] } "
                + "{ name: 'doubleKey1' value: [ '1.0' '2.0' '3.0' ] } "
                + "{ name: 'longKey1' value: [ '1' '2' '3' ] } "
                + "{ name: 'stringKey1' value: [ 'String1' 'String2' 'String3' ] }  } "
                + "{ name: 'schemaType' value: schemaType1 } "
                + "{ name: 'score' value: 0 } "
                + "{ name: 'ttlMillis' value: 0 } "
                + "{ name: 'uri' value: uri1 } ";
        assertThat(document.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testDocumentGetValues_differentTypes() {
        GenericDocument document = new GenericDocument.Builder<>("namespace", "uri1", "schemaType1")
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
                .containsExactly("test-value1", "test-value2", "test-value3");

        // Get a value of the wrong type
        assertThat(document.getPropertyDouble("longKey1")).isEqualTo(0.0);
        assertThat(document.getPropertyDoubleArray("longKey1")).isNull();
    }

    @Test
    public void testDocument_setEmptyValues() {
        GenericDocument document = new GenericDocument.Builder<>("namespace", "uri1", "schemaType1")
                .setPropertyBoolean("testKey")
                .build();
        assertThat(document.getPropertyBooleanArray("testKey")).isEmpty();
    }

    @Test
    public void testDocumentInvalid() {
        GenericDocument.Builder<?> builder = new GenericDocument.Builder<>("namespace", "uri1",
                "schemaType1");
        String nullString = null;

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setPropertyString("testKey", "string1", nullString));
    }

    @Test
    public void testRetrieveTopLevelProperties() {
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "uri1", "schema1")
                .setScore(42)
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();

        // Top-level array properties should be retrievable
        assertThat(doc.getPropertyStringArray("propString")).asList().containsExactly(
                "Goodbye", "Hello");
        assertThat(doc.getPropertyLongArray("propInts")).asList().containsExactly(
                3L, 1L, 4L);
        assertThat(doc.getPropertyDoubleArray("propDoubles")).usingTolerance(
                0.0001).containsExactly(3.14, 0.42);
        assertThat(doc.getPropertyBooleanArray("propBools")).asList().containsExactly(
                false);
        assertThat(doc.getPropertyBytesArray("propBytes")).isEqualTo(new byte[][]{{3, 4}});

        // Top-level array properties should retrieve the first element
        assertThat(doc.getPropertyString("propString")).isEqualTo("Goodbye");
        assertThat(doc.getPropertyLong("propInts")).isEqualTo(3);
        assertThat(doc.getPropertyDouble("propDoubles")).isWithin(0.0001)
                .of(3.14);
        assertThat(doc.getPropertyBoolean("propBools")).isFalse();
        assertThat(doc.getPropertyBytes("propBytes")).isEqualTo(new byte[]{3, 4});
    }

    @Test
    public void testRetrieveNestedProperties() {
        GenericDocument innerDoc = new GenericDocument.Builder<>("namespace", "uri2", "schema2")
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "uri1", "schema1")
                .setScore(42)
                .setPropertyDocument("propDocument", innerDoc)
                .build();

        // Document should be retrievable via both array and single getters
        assertThat(doc.getPropertyDocument("propDocument")).isEqualTo(innerDoc);
        assertThat(doc.getPropertyDocumentArray("propDocument")).asList()
                .containsExactly(innerDoc);
        assertThat((GenericDocument[]) doc.getProperty("propDocument")).asList()
                .containsExactly(innerDoc);

        // Nested array properties should be retrievable
        assertThat(doc.getPropertyStringArray("propDocument.propString")).asList()
                .containsExactly("Goodbye", "Hello");
        assertThat(doc.getPropertyLongArray("propDocument.propInts")).asList().containsExactly(
                3L, 1L, 4L);
        assertThat(doc.getPropertyDoubleArray("propDocument.propDoubles")).usingTolerance(
                0.0001).containsExactly(3.14, 0.42);
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
        GenericDocument innerDoc0 = new GenericDocument.Builder<>("namespace", "uri2", "schema2")
                .setPropertyString("propString", "Goodbye", "Hello")
                .setPropertyString("propStringTwo", "Fee", "Fi")
                .setPropertyLong("propInts", 3, 1, 4)
                .setPropertyDouble("propDoubles", 3.14, 0.42)
                .setPropertyBoolean("propBools", false)
                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                .build();
        GenericDocument innerDoc1 = new GenericDocument.Builder<>("namespace", "uri3", "schema2")
                .setPropertyString("propString", "Aloha")
                .setPropertyLong("propInts", 7, 5, 6)
                .setPropertyLong("propIntsTwo", 8, 6)
                .setPropertyDouble("propDoubles", 7.14, 0.356)
                .setPropertyBoolean("propBools", true)
                .setPropertyBytes("propBytes", new byte[][]{{8, 9}})
                .build();
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "uri1", "schema1")
                .setScore(42)
                .setPropertyDocument("propDocument", innerDoc0, innerDoc1)
                .build();

        // Documents should be retrievable via both array and single getters
        assertThat(doc.getPropertyDocument("propDocument")).isEqualTo(innerDoc0);
        assertThat(doc.getPropertyDocumentArray("propDocument")).asList()
                .containsExactly(innerDoc0, innerDoc1);
        assertThat((GenericDocument[]) doc.getProperty("propDocument")).asList()
                .containsExactly(innerDoc0, innerDoc1);

        // Nested array properties should be retrievable and should merge the arrays from the
        // inner documents.
        assertThat(doc.getPropertyStringArray("propDocument.propString")).asList()
                .containsExactly("Goodbye", "Hello", "Aloha");
        assertThat(doc.getPropertyLongArray("propDocument.propInts")).asList().containsExactly(
                3L, 1L, 4L, 7L, 5L, 6L);
        assertThat(doc.getPropertyDoubleArray("propDocument.propDoubles")).usingTolerance(
                0.0001).containsExactly(3.14, 0.42, 7.14, 0.356);
        assertThat(doc.getPropertyBooleanArray("propDocument.propBools")).asList()
                .containsExactly(false, true);
        assertThat(doc.getPropertyBytesArray("propDocument.propBytes")).isEqualTo(
                new byte[][]{{3, 4}, {8, 9}});
        assertThat(doc.getProperty("propDocument.propBytes")).isEqualTo(
                new byte[][]{{3, 4}, {8, 9}});

        // Nested array properties should properly handle properties appearing in only one inner
        // document, but not the other.
        assertThat(
                doc.getPropertyStringArray("propDocument.propStringTwo")).asList()
                .containsExactly("Fee", "Fi");
        assertThat(doc.getPropertyLongArray("propDocument.propIntsTwo")).asList()
                .containsExactly(8L, 6L);

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
}
