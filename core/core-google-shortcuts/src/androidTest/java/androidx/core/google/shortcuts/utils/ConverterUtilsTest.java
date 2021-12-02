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

package androidx.core.google.shortcuts.utils;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.appsearch.app.GenericDocument;
import androidx.core.google.shortcuts.converters.IndexableKeys;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.firebase.appindexing.Indexable;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ConverterUtilsTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    @SmallTest
    public void testBuildBaseIndexableFromGenericDocument_returnsIndexable() {
        GenericDocument genericDocument = new GenericDocument.Builder<>(
                "namespace", "id", "schema")
                .setScore(1)
                .setCreationTimestampMillis(1)
                .build();

        Indexable.Builder expectedIndexableBuilder = new Indexable.Builder("schema")
                .setMetadata(new Indexable.Metadata.Builder().setScore(1))
                .setId("id")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=id;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 0)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1);
        assertThat(ConverterUtils.buildBaseIndexableFromGenericDocument(
                mContext, genericDocument).build()).isEqualTo(expectedIndexableBuilder.build());
    }
}
