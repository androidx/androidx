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

package androidx.appsearch.builtintypes;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;

import org.junit.Test;

import java.util.Arrays;

public class AlarmInstanceTest {
    @Test
    public void testBuilder() {
        AlarmInstance alarmInstance = new AlarmInstance.Builder(
                "namespace", "id", "2022-12-01T07:30:00")
                .setDocumentScore(1)
                .setDocumentTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my alarm instance")
                .addAlternateName("my alternate alarm instance")
                .addAlternateName("my alternate alarm instance 2")
                .setDescription("this is my alarm instance")
                .setImage("content://images/alarminstance1")
                .setUrl("content://alarminstance/1")
                .setStatus(AlarmInstance.STATUS_SCHEDULED)
                .setSnoozeDurationMillis(10000)
                .build();

        assertThat(alarmInstance.getNamespace()).isEqualTo("namespace");
        assertThat(alarmInstance.getId()).isEqualTo("id");
        assertThat(alarmInstance.getDocumentScore()).isEqualTo(1);
        assertThat(alarmInstance.getDocumentTtlMillis()).isEqualTo(20000);
        assertThat(alarmInstance.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(alarmInstance.getName()).isEqualTo("my alarm instance");
        assertThat(alarmInstance.getAlternateNames()).isNotNull();
        assertThat(alarmInstance.getAlternateNames())
                .containsExactly("my alternate alarm instance", "my alternate alarm instance 2");
        assertThat(alarmInstance.getDescription()).isEqualTo("this is my alarm instance");
        assertThat(alarmInstance.getImage()).isEqualTo("content://images/alarminstance1");
        assertThat(alarmInstance.getUrl()).isEqualTo("content://alarminstance/1");
        assertThat(alarmInstance.getScheduledTime()).isEqualTo("2022-12-01T07:30:00");
        assertThat(alarmInstance.getStatus()).isEqualTo(1);
        assertThat(alarmInstance.getSnoozeDurationMillis()).isEqualTo(10000);
    }

    @Test
    public void testBuilderCopy_returnsAlarmInstanceWithAllFieldsCopied() {
        AlarmInstance alarmInstance1 = new AlarmInstance.Builder(
                "namespace", "id", "2022-12-01T07:30:00")
                .setDocumentScore(1)
                .setDocumentTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my alarm instance")
                .addAlternateName("my alternate alarm instance")
                .addAlternateName("my alternate alarm instance 2")
                .setDescription("this is my alarm instance")
                .setImage("content://images/alarminstance1")
                .setUrl("content://alarminstance/1")
                .setStatus(AlarmInstance.STATUS_SCHEDULED)
                .setSnoozeDurationMillis(10000)
                .build();

        AlarmInstance alarmInstance2 = new AlarmInstance.Builder(alarmInstance1).build();
        assertThat(alarmInstance1.getNamespace()).isEqualTo(alarmInstance2.getNamespace());
        assertThat(alarmInstance1.getId()).isEqualTo(alarmInstance2.getId());
        assertThat(alarmInstance1.getDocumentScore()).isEqualTo(alarmInstance2.getDocumentScore());
        assertThat(alarmInstance1.getDocumentTtlMillis())
                .isEqualTo(alarmInstance2.getDocumentTtlMillis());
        assertThat(alarmInstance1.getCreationTimestampMillis())
                .isEqualTo(alarmInstance2.getCreationTimestampMillis());
        assertThat(alarmInstance1.getName()).isEqualTo(alarmInstance2.getName());
        assertThat(alarmInstance1.getAlternateNames())
                .containsExactlyElementsIn(alarmInstance2.getAlternateNames());
        assertThat(alarmInstance1.getDescription()).isEqualTo(alarmInstance2.getDescription());
        assertThat(alarmInstance1.getImage()).isEqualTo(alarmInstance2.getImage());
        assertThat(alarmInstance1.getUrl()).isEqualTo(alarmInstance2.getUrl());
        assertThat(alarmInstance1.getScheduledTime()).isEqualTo(alarmInstance2.getScheduledTime());
        assertThat(alarmInstance1.getStatus()).isEqualTo(alarmInstance2.getStatus());
        assertThat(alarmInstance1.getSnoozeDurationMillis())
                .isEqualTo(alarmInstance2.getSnoozeDurationMillis());
    }

    @Test
    public void testToGenericDocument() throws Exception {
        AlarmInstance alarmInstance = new AlarmInstance.Builder(
                "namespace", "id", "2022-12-01T07:30:00")
                .setDocumentScore(1)
                .setDocumentTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my alarm instance")
                .addAlternateName("my alternate alarm instance")
                .addAlternateName("my alternate alarm instance 2")
                .setDescription("this is my alarm instance")
                .setImage("content://images/alarminstance1")
                .setUrl("content://alarminstance/1")
                .setStatus(AlarmInstance.STATUS_SCHEDULED)
                .setSnoozeDurationMillis(10000)
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(alarmInstance);
        assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:AlarmInstance");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getScore()).isEqualTo(1);
        assertThat(genericDocument.getTtlMillis()).isEqualTo(20000);
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(genericDocument.getPropertyString("name")).isEqualTo("my alarm instance");
        assertThat(genericDocument.getPropertyStringArray("alternateNames")).isNotNull();
        assertThat(Arrays.asList(genericDocument.getPropertyStringArray("alternateNames")))
                .containsExactly("my alternate alarm instance", "my alternate alarm instance 2");
        assertThat(genericDocument.getPropertyString("description"))
                .isEqualTo("this is my alarm instance");
        assertThat(genericDocument.getPropertyString("image"))
                .isEqualTo("content://images/alarminstance1");
        assertThat(genericDocument.getPropertyString("url"))
                .isEqualTo("content://alarminstance/1");
        assertThat(genericDocument.getPropertyString("scheduledTime"))
                .isEqualTo("2022-12-01T07:30:00");
        assertThat(genericDocument.getPropertyLong("status"))
                .isEqualTo(AlarmInstance.STATUS_SCHEDULED);
        assertThat(genericDocument.getPropertyLong("snoozeDurationMillis"))
                .isEqualTo(10000);

        // Test that toDocumentClass doesn't lose information.
        GenericDocument newGenericDocument = GenericDocument.fromDocumentClass(
                genericDocument.toDocumentClass(AlarmInstance.class));
        assertThat(newGenericDocument).isEqualTo(genericDocument);
    }
}
