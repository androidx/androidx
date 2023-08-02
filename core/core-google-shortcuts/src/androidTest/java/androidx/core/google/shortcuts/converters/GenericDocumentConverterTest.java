/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.google.shortcuts.converters;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.appsearch.app.GenericDocument;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.android.gms.appindex.Indexable;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GenericDocumentConverterTest {
    private final GenericDocumentConverter mConverter = new GenericDocumentConverter();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    @SmallTest
    public void testConvertGenericDocument_returnsIndexable() throws Exception {
        GenericDocument childDocument1 = new GenericDocument.Builder<>(
                "namespace1",
                "child1",
                "childSchema")
                .setCreationTimestampMillis(1)
                .build();
        GenericDocument childDocument2 = new GenericDocument.Builder<>(
                "namespace1",
                "child2",
                "childSchema")
                .setCreationTimestampMillis(1)
                .build();
        GenericDocument genericDocument = new GenericDocument.Builder<>(
                "namespace2",
                "id1",
                "schema")
                .setScore(1)
                .setTtlMillis(1000)
                .setCreationTimestampMillis(1)
                .setPropertyString("stringArrayProperty", "s1", "s2")
                .setPropertyLong("longArrayProperty", 1L, 2L)
                .setPropertyBoolean("booleanArrayProperty", true, false)
                .setPropertyDocument("documentArrayProperty", childDocument1, childDocument2)
                .setPropertyString("stringProperty", "s3")
                .setPropertyLong("longProperty", 3L)
                .setPropertyBoolean("booleanProperty", true)
                .setPropertyDocument("documentProperty", childDocument1)
                .setPropertyString("emptyProperty")
                .build();

        Indexable indexable = mConverter.convertGenericDocument(mContext, genericDocument).build();

        Indexable expectedChild1 = new Indexable.Builder("childSchema")
                .setMetadata(new Indexable.Metadata.Builder().setScore(0))
                .setName("namespace1")
                .setId("child1")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=child1;end")
                .put(IndexableKeys.NAMESPACE, "namespace1")
                .put(IndexableKeys.TTL_MILLIS, 0)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1)
                .build();
        Indexable expectedChild2 = new Indexable.Builder("childSchema")
                .setMetadata(new Indexable.Metadata.Builder().setScore(0))
                .setName("namespace1")
                .setId("child2")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=child2;end")
                .put(IndexableKeys.NAMESPACE, "namespace1")
                .put(IndexableKeys.TTL_MILLIS, 0)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1)
                .build();
        Indexable expectedIndexable = new Indexable.Builder("schema")
                .setMetadata(new Indexable.Metadata.Builder().setScore(1))
                .setName("namespace2")
                .setId("id1")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=id1;end")
                .put(IndexableKeys.NAMESPACE, "namespace2")
                .put(IndexableKeys.TTL_MILLIS, 1000)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1)
                .put("stringArrayProperty", "s1", "s2")
                .put("longArrayProperty", 1L, 2L)
                .put("booleanArrayProperty", true, false)
                .put("stringProperty", "s3")
                .put("longProperty", 3L)
                .put("booleanProperty", true)
                .put("documentArrayProperty", expectedChild1, expectedChild2)
                .put("documentProperty", expectedChild1)
                .build();

        assertThat(indexable).isEqualTo(expectedIndexable);
    }
}
