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

import java.util.Calendar;

public class AlarmInstanceTest {
    @Test
    public void testBuilder() {
        AlarmInstance alarmInstance = new AlarmInstance.Builder("namespace", "id")
                .setScore(1)
                .setTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setYear(2022)
                .setMonth(Calendar.DECEMBER)
                .setDay(1)
                .setHour(7)
                .setMinute(30)
                .setStatus(AlarmInstance.STATUS_SCHEDULED)
                .setSnoozeDurationMillis(10000)
                .build();

        assertThat(alarmInstance.getNamespace()).isEqualTo("namespace");
        assertThat(alarmInstance.getId()).isEqualTo("id");
        assertThat(alarmInstance.getScore()).isEqualTo(1);
        assertThat(alarmInstance.getTtlMillis()).isEqualTo(20000);
        assertThat(alarmInstance.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(alarmInstance.getYear()).isEqualTo(2022);
        assertThat(alarmInstance.getMonth()).isEqualTo(Calendar.DECEMBER);
        assertThat(alarmInstance.getDay()).isEqualTo(1);
        assertThat(alarmInstance.getHour()).isEqualTo(7);
        assertThat(alarmInstance.getMinute()).isEqualTo(30);
        assertThat(alarmInstance.getStatus()).isEqualTo(1);
        assertThat(alarmInstance.getSnoozeDurationMillis()).isEqualTo(10000);
    }

    @Test
    public void testBuilderCopy_returnsAlarmInstanceWithAllFieldsCopied() {
        AlarmInstance alarmInstance1 = new AlarmInstance.Builder("namespace", "id")
                .setScore(1)
                .setTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setYear(2022)
                .setMonth(Calendar.DECEMBER)
                .setDay(1)
                .setHour(7)
                .setMinute(30)
                .setStatus(AlarmInstance.STATUS_SCHEDULED)
                .setSnoozeDurationMillis(10000)
                .build();

        AlarmInstance alarmInstance2 = new AlarmInstance.Builder(alarmInstance1).build();
        assertThat(alarmInstance1.getNamespace()).isEqualTo(alarmInstance2.getNamespace());
        assertThat(alarmInstance1.getId()).isEqualTo(alarmInstance2.getId());
        assertThat(alarmInstance1.getScore()).isEqualTo(alarmInstance2.getScore());
        assertThat(alarmInstance1.getTtlMillis()).isEqualTo(alarmInstance2.getTtlMillis());
        assertThat(alarmInstance1.getCreationTimestampMillis())
                .isEqualTo(alarmInstance2.getCreationTimestampMillis());
        assertThat(alarmInstance1.getYear()).isEqualTo(alarmInstance2.getYear());
        assertThat(alarmInstance1.getMonth()).isEqualTo(alarmInstance2.getMonth());
        assertThat(alarmInstance1.getDay()).isEqualTo(alarmInstance2.getDay());
        assertThat(alarmInstance1.getHour()).isEqualTo(alarmInstance2.getHour());
        assertThat(alarmInstance1.getMinute()).isEqualTo(alarmInstance2.getMinute());
        assertThat(alarmInstance1.getStatus()).isEqualTo(alarmInstance2.getStatus());
        assertThat(alarmInstance1.getSnoozeDurationMillis())
                .isEqualTo(alarmInstance2.getSnoozeDurationMillis());
    }

    @Test
    public void testToGenericDocument() throws Exception {
        AlarmInstance alarmInstance = new AlarmInstance.Builder("namespace", "id")
                .setScore(1)
                .setTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setYear(2022)
                .setMonth(Calendar.DECEMBER)
                .setDay(1)
                .setHour(7)
                .setMinute(30)
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
        assertThat(genericDocument.getPropertyLong("year")).isEqualTo(2022);
        assertThat(genericDocument.getPropertyLong("month")).isEqualTo(Calendar.DECEMBER);
        assertThat(genericDocument.getPropertyLong("day")).isEqualTo(1);
        assertThat(genericDocument.getPropertyLong("hour")).isEqualTo(7);
        assertThat(genericDocument.getPropertyLong("minute")).isEqualTo(30);
        assertThat(genericDocument.getPropertyLong("status")).isEqualTo(1);
        assertThat(genericDocument.getPropertyLong("snoozeDurationMillis")).isEqualTo(10000);
    }
}
