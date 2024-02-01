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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;
import android.os.Parcel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/** Tests for private APIs of {@link GenericDocument}. */
public class GenericDocumentInternalTest {
    @Test
    public void testRecreateFromParcel() {
        GenericDocument inDoc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setScore(42)
                .setPropertyString("propString", "Hello")
                .setPropertyBytes("propBytes", new byte[][]{{1, 2}})
                .setPropertyDocument(
                        "propDocument",
                        new GenericDocument.Builder<>("namespace", "id2", "schema2")
                                .setPropertyString("propString", "Goodbye")
                                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                                .build())
                .build();

        // Serialize the document
        Parcel inParcel = Parcel.obtain();
        inParcel.writeBundle(inDoc.getBundle());
        byte[] data = inParcel.marshall();
        inParcel.recycle();

        // Deserialize the document
        Parcel outParcel = Parcel.obtain();
        outParcel.unmarshall(data, 0, data.length);
        outParcel.setDataPosition(0);
        Bundle outBundle = outParcel.readBundle();
        outParcel.recycle();

        // Compare results
        GenericDocument outDoc = new GenericDocument(outBundle);
        assertThat(inDoc).isEqualTo(outDoc);
        assertThat(outDoc.getPropertyString("propString")).isEqualTo("Hello");
        assertThat(outDoc.getPropertyBytesArray("propBytes")).isEqualTo(new byte[][]{{1, 2}});
        assertThat(outDoc.getPropertyDocument("propDocument").getPropertyString("propString"))
                .isEqualTo("Goodbye");
        assertThat(outDoc.getPropertyDocument("propDocument").getPropertyBytesArray("propBytes"))
                .isEqualTo(new byte[][]{{3, 4}});
    }

    @Test
    public void testRecreateFromParcelWithParentTypes() {
        GenericDocument inDoc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setParentTypes(new ArrayList<>(Arrays.asList("Class1", "Class2")))
                .setScore(42)
                .setPropertyString("propString", "Hello")
                .setPropertyBytes("propBytes", new byte[][]{{1, 2}})
                .setPropertyDocument(
                        "propDocument",
                        new GenericDocument.Builder<>("namespace", "id2", "schema2")
                                .setPropertyString("propString", "Goodbye")
                                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                                .build())
                .build();

        // Serialize the document
        Parcel inParcel = Parcel.obtain();
        inParcel.writeBundle(inDoc.getBundle());
        byte[] data = inParcel.marshall();
        inParcel.recycle();

        // Deserialize the document
        Parcel outParcel = Parcel.obtain();
        outParcel.unmarshall(data, 0, data.length);
        outParcel.setDataPosition(0);
        Bundle outBundle = outParcel.readBundle();
        outParcel.recycle();

        // Compare results
        GenericDocument outDoc = new GenericDocument(outBundle);
        assertThat(inDoc).isEqualTo(outDoc);
        assertThat(outDoc.getParentTypes()).isEqualTo(Arrays.asList("Class1", "Class2"));
        assertThat(outDoc.getPropertyString("propString")).isEqualTo("Hello");
        assertThat(outDoc.getPropertyBytesArray("propBytes")).isEqualTo(new byte[][]{{1, 2}});
        assertThat(outDoc.getPropertyDocument("propDocument").getPropertyString("propString"))
                .isEqualTo("Goodbye");
        assertThat(outDoc.getPropertyDocument("propDocument").getPropertyBytesArray("propBytes"))
                .isEqualTo(new byte[][]{{3, 4}});
    }

    @Test
    public void testGenericDocumentBuilderDoesNotMutateOriginal() {
        GenericDocument oldDoc = new GenericDocument.Builder<>("namespace", "id1", "schema1")
                .setParentTypes(new ArrayList<>(Arrays.asList("Class1", "Class2")))
                .setScore(42)
                .setPropertyString("propString", "Hello")
                .setPropertyBytes("propBytes", new byte[][]{{1, 2}})
                .setPropertyDocument(
                        "propDocument",
                        new GenericDocument.Builder<>("namespace", "id2", "schema2")
                                .setPropertyString("propString", "Goodbye")
                                .setPropertyBytes("propBytes", new byte[][]{{3, 4}})
                                .build())
                .build();

        GenericDocument newDoc = new GenericDocument.Builder<>(oldDoc)
                .setParentTypes(new ArrayList<>(Arrays.asList("Class3", "Class4")))
                .setPropertyBytes("propBytes", new byte[][]{{1, 2}})
                .setPropertyDocument(
                        "propDocument",
                        new GenericDocument.Builder<>("namespace", "id3", "schema3")
                                .setPropertyString("propString", "Bye")
                                .setPropertyBytes("propBytes", new byte[][]{{5, 6}})
                                .build())
                .build();

        // Check that the original GenericDocument is unmodified.
        assertThat(oldDoc.getParentTypes()).isEqualTo(Arrays.asList("Class1", "Class2"));
        assertThat(oldDoc.getScore()).isEqualTo(42);
        assertThat(oldDoc.getPropertyString("propString")).isEqualTo("Hello");
        assertThat(oldDoc.getPropertyBytesArray("propBytes")).isEqualTo(new byte[][]{{1, 2}});
        assertThat(oldDoc.getPropertyDocument("propDocument").getPropertyString("propString"))
                .isEqualTo("Goodbye");
        assertThat(oldDoc.getPropertyDocument("propDocument").getPropertyBytesArray("propBytes"))
                .isEqualTo(new byte[][]{{3, 4}});

        // Check that the new GenericDocument has modified the original fields correctly.
        assertThat(newDoc.getParentTypes()).isEqualTo(Arrays.asList("Class3", "Class4"));
        assertThat(newDoc.getPropertyBytesArray("propBytes")).isEqualTo(new byte[][]{{1, 2}});
        assertThat(newDoc.getPropertyDocument("propDocument").getPropertyString("propString"))
                .isEqualTo("Bye");
        assertThat(newDoc.getPropertyDocument("propDocument").getPropertyBytesArray("propBytes"))
                .isEqualTo(new byte[][]{{5, 6}});

        // Check that the new GenericDocument copies fields that aren't set.
        assertThat(oldDoc.getScore()).isEqualTo(newDoc.getScore());
        assertThat(oldDoc.getPropertyString("propString")).isEqualTo(newDoc.getPropertyString(
                "propString"));
    }
}
