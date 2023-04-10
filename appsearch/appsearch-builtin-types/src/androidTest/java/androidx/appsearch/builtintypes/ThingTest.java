/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;

import org.junit.Test;

import java.util.Arrays;

public class ThingTest {

    @Test
    public void testBuilder() {
        long now = System.currentTimeMillis();
        Thing thing = new Thing.Builder("namespace", "thing1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(now)
                .setDocumentTtlMillis(30000)
                .setName("my first thing")
                .addAlternateName("my first object")
                .addAlternateName("माझी पहिली गोष्ट")
                .setDescription("this is my first schema.org object")
                .setImage("content://images/thing1")
                .setUrl("content://things/1")
                .build();

        assertThat(thing.getNamespace()).isEqualTo("namespace");
        assertThat(thing.getId()).isEqualTo("thing1");
        assertThat(thing.getDocumentScore()).isEqualTo(1);
        assertThat(thing.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(thing.getDocumentTtlMillis()).isEqualTo(30000);
        assertThat(thing.getName()).isEqualTo("my first thing");
        assertThat(thing.getAlternateNames()).isNotNull();
        assertThat(thing.getAlternateNames())
                .containsExactly("my first object", "माझी पहिली गोष्ट");
        assertThat(thing.getDescription()).isEqualTo("this is my first schema.org object");
        assertThat(thing.getImage()).isEqualTo("content://images/thing1");
        assertThat(thing.getUrl()).isEqualTo("content://things/1");
    }

    @Test
    public void testBuilderCopy_allFieldsAreCopied() {
        long now = System.currentTimeMillis();
        Thing thing1 = new Thing.Builder("namespace", "thing1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(now)
                .setDocumentTtlMillis(30000)
                .setName("my first thing")
                .addAlternateName("my first object")
                .addAlternateName("माझी पहिली गोष्ट")
                .setDescription("this is my first schema.org object")
                .setImage("content://images/thing1")
                .setUrl("content://things/1")
                .build();
        Thing thing2 = new Thing.Builder(thing1).build();

        assertThat(thing2.getNamespace()).isEqualTo("namespace");
        assertThat(thing2.getId()).isEqualTo("thing1");
        assertThat(thing2.getDocumentScore()).isEqualTo(1);
        assertThat(thing2.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(thing2.getDocumentTtlMillis()).isEqualTo(30000);
        assertThat(thing2.getName()).isEqualTo("my first thing");
        assertThat(thing2.getAlternateNames()).isNotNull();
        assertThat(thing2.getAlternateNames())
                .containsExactly("my first object", "माझी पहिली गोष्ट");
        assertThat(thing2.getDescription()).isEqualTo("this is my first schema.org object");
        assertThat(thing2.getImage()).isEqualTo("content://images/thing1");
        assertThat(thing2.getUrl()).isEqualTo("content://things/1");
    }

    @Test
    public void testBuilderCopy_copiedFieldsCanBeUpdated() {
        long now = System.currentTimeMillis();
        Thing thing1 = new Thing.Builder("namespace", "thing1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(now)
                .setDocumentTtlMillis(30000)
                .setName("my first thing")
                .addAlternateName("my first object")
                .addAlternateName("माझी पहिली गोष्ट")
                .setDescription("this is my first schema.org object")
                .setImage("content://images/thing1")
                .setUrl("content://things/1")
                .build();
        Thing thing2 = new Thing.Builder(thing1)
                .clearAlternateNames()
                .setImage("content://images/thing2")
                .setUrl("content://things/2")
                .build();

        assertThat(thing2.getNamespace()).isEqualTo("namespace");
        assertThat(thing2.getId()).isEqualTo("thing1");
        assertThat(thing2.getDocumentScore()).isEqualTo(1);
        assertThat(thing2.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(thing2.getDocumentTtlMillis()).isEqualTo(30000);
        assertThat(thing2.getName()).isEqualTo("my first thing");
        assertThat(thing2.getAlternateNames()).isEmpty();
        assertThat(thing2.getDescription()).isEqualTo("this is my first schema.org object");
        assertThat(thing2.getImage()).isEqualTo("content://images/thing2");
        assertThat(thing2.getUrl()).isEqualTo("content://things/2");
    }

    @Test
    public void testToGenericDocument() throws Exception {
        long now = System.currentTimeMillis();
        Thing thing = new Thing.Builder("namespace", "thing1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(now)
                .setDocumentTtlMillis(30000)
                .setName("my first thing")
                .addAlternateName("my first object")
                .addAlternateName("माझी पहिली गोष्ट")
                .setDescription("this is my first schema.org object")
                .setImage("content://images/thing1")
                .setUrl("content://things/1")
                .build();

        GenericDocument document = GenericDocument.fromDocumentClass(thing);
        assertThat(document.getSchemaType()).isEqualTo("builtin:Thing");
        assertThat(document.getNamespace()).isEqualTo("namespace");
        assertThat(document.getId()).isEqualTo("thing1");
        assertThat(document.getScore()).isEqualTo(1);
        assertThat(document.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(document.getTtlMillis()).isEqualTo(30000);
        assertThat(document.getPropertyString("name")).isEqualTo("my first thing");
        assertThat(document.getPropertyStringArray("alternateNames")).isNotNull();
        assertThat(Arrays.asList(document.getPropertyStringArray("alternateNames")))
                .containsExactly("my first object", "माझी पहिली गोष्ट");
        assertThat(document.getPropertyString("description"))
                .isEqualTo("this is my first schema.org object");
        assertThat(document.getPropertyString("image")).isEqualTo("content://images/thing1");
        assertThat(document.getPropertyString("url")).isEqualTo("content://things/1");
    }
}
