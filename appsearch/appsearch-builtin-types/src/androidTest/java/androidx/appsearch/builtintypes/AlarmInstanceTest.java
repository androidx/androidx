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

public class AlarmInstanceTest {
    @Test
    public void testBuilder() {
        AlarmInstance alarmInstance = new AlarmInstance.Builder(
                "namespace", "id", "2022-12-01T07:30:00")
                .setDocumentScore(1)
                .setDocumentTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setStatus(AlarmInstance.STATUS_SCHEDULED)
                .setSnoozeDurationMillis(10000)
                .build();

        assertThat(alarmInstance.getNamespace()).isEqualTo("namespace");
        assertThat(alarmInstance.getId()).isEqualTo("id");
        assertThat(alarmInstance.getDocumentScore()).isEqualTo(1);
        assertThat(alarmInstance.getDocumentTtlMillis()).isEqualTo(20000);
        assertThat(alarmInstance.getCreationTimestampMillis()).isEqualTo(100);
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
        assertThat(genericDocument.getPropertyString("scheduledTime"))
                .isEqualTo("2022-12-01T07:30:00");
    }
}
