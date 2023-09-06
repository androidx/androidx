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

package androidx.appsearch.app.safeparcel;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

/** Tests for {@link androidx.appsearch.app.GenericDocument} related SafeParcels. */
public class GenericDocumentParcelTest {
    @Test
    public void testPropertyParcel_onePropertySet_success() {
        String[] stringValues = {"a", "b"};
        long[] longValues = {1L, 2L};
        double[] doubleValues = {1.0, 2.0};
        boolean[] booleanValues = {true, false};
        byte[][] bytesValues = {new byte[1]};
        GenericDocumentParcel[] docValues = {(new GenericDocumentParcel.Builder(
                "namespace", "id", "schemaType")).build()};

        assertThat(new PropertyParcel.Builder("name").setStringValues(
                stringValues).build().getStringValues()).isEqualTo(
                Arrays.copyOf(stringValues, stringValues.length));
        assertThat(new PropertyParcel.Builder("name").setLongValues(
                longValues).build().getLongValues()).isEqualTo(
                Arrays.copyOf(longValues, longValues.length));
        assertThat(new PropertyParcel.Builder("name").setDoubleValues(
                doubleValues).build().getDoubleValues()).isEqualTo(
                Arrays.copyOf(doubleValues, doubleValues.length));
        assertThat(new PropertyParcel.Builder("name").setBooleanValues(
                booleanValues).build().getBooleanValues()).isEqualTo(
                Arrays.copyOf(booleanValues, booleanValues.length));
        assertThat(new PropertyParcel.Builder("name").setBytesValues(
                bytesValues).build().getBytesValues()).isEqualTo(
                Arrays.copyOf(bytesValues, bytesValues.length));
        assertThat(new PropertyParcel.Builder("name").setDocumentValues(
                docValues).build().getDocumentValues()).isEqualTo(
                Arrays.copyOf(docValues, docValues.length));
    }

    @Test
    public void testPropertyParcel_moreThanOnePropertySet_exceptionThrown() {
        String[] stringValues = {"a", "b"};
        long[] longValues = {1L, 2L};
        PropertyParcel.Builder propertyParcelBuilder =
                new PropertyParcel.Builder("name")
                        .setStringValues(stringValues)
                        .setLongValues(longValues);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> propertyParcelBuilder.build());

        assertThat(exception.getMessage()).contains("One and only one type array");
    }

    @Test
    public void testGenericDocumentParcel_propertiesGeneratedCorrectly() {
        GenericDocumentParcel.Builder builder =
                new GenericDocumentParcel.Builder(
                        /*namespace=*/ "namespace",
                        /*id=*/ "id",
                        /*schemaType=*/ "schemaType");
        long[] longArray = new long[]{1L, 2L, 3L};
        String[] stringArray = new String[]{"hello", "world", "!"};
        builder.putInPropertyMap(/*name=*/ "longArray", /*values=*/ longArray);
        builder.putInPropertyMap(/*name=*/ "stringArray", /*values=*/ stringArray);
        GenericDocumentParcel genericDocumentParcel = builder.build();

        PropertyParcel[] properties = genericDocumentParcel.getProperties();
        Map<String, PropertyParcel> propertyMap = genericDocumentParcel.getPropertyMap();
        PropertyParcel longArrayProperty = new PropertyParcel.Builder(
                /*name=*/ "longArray").setLongValues(longArray).build();
        PropertyParcel stringArrayProperty = new PropertyParcel.Builder(
                /*name=*/ "stringArray").setStringValues(stringArray).build();

        assertThat(properties).asList().containsExactly(longArrayProperty, stringArrayProperty);
        assertThat(propertyMap).containsExactly("longArray", longArrayProperty,
                "stringArray", stringArrayProperty);
    }

    @Test
    public void testGenericDocumentParcel_buildFromAnotherDocumentParcelCorrectly() {
        GenericDocumentParcel.Builder builder =
                new GenericDocumentParcel.Builder(
                        /*namespace=*/ "namespace",
                        /*id=*/ "id",
                        /*schemaType=*/ "schemaType");
        long[] longArray = new long[]{1L, 2L, 3L};
        String[] stringArray = new String[]{"hello", "world", "!"};
        builder.putInPropertyMap(/*name=*/ "longArray", /*values=*/ longArray);
        builder.putInPropertyMap(/*name=*/ "stringArray", /*values=*/ stringArray);
        GenericDocumentParcel genericDocumentParcel = builder.build();

        GenericDocumentParcel genericDocumentParcelCopy =
                new GenericDocumentParcel.Builder(genericDocumentParcel).build();

        assertThat(genericDocumentParcelCopy.getNamespace()).isEqualTo(
                genericDocumentParcel.getNamespace());
        assertThat(genericDocumentParcelCopy.getId()).isEqualTo(genericDocumentParcel.getId());
        assertThat(genericDocumentParcelCopy.getSchemaType()).isEqualTo(
                genericDocumentParcel.getSchemaType());
        assertThat(genericDocumentParcelCopy.getCreationTimestampMillis()).isEqualTo(
                genericDocumentParcel.getCreationTimestampMillis());
        assertThat(genericDocumentParcelCopy.getTtlMillis()).isEqualTo(
                genericDocumentParcel.getTtlMillis());
        assertThat(genericDocumentParcelCopy.getScore()).isEqualTo(
                genericDocumentParcel.getScore());
        // Check it is a copy.
        assertThat(genericDocumentParcelCopy).isNotSameInstanceAs(genericDocumentParcel);
        assertThat(genericDocumentParcelCopy.getProperties()).isEqualTo(
                genericDocumentParcel.getProperties());
    }
}
