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

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.GenericDocument;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

public class AlarmTest {
    @Test
    public void testBuilder() {
        AlarmInstance alarmInstance1 = new AlarmInstance.Builder(
                "namespace", "instanceId1", "2022-01-01T00:00:00")
                .build();
        AlarmInstance alarmInstance2 = new AlarmInstance.Builder(
                "namespace", "instanceId2", "2022-01-02T00:00:00")
                .build();
        Alarm alarm = new Alarm.Builder("namespace", "id")
                .setDocumentScore(1)
                .setDocumentTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my alarm")
                .addAlternateName("my alternate alarm")
                .addAlternateName("my alternate alarm 2")
                .setDescription("this is my alarm")
                .setImage("content://images/alarm1")
                .setUrl("content://alarm/1")
                .setEnabled(true)
                .setDaysOfWeek(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY)
                .setHour(12)
                .setMinute(0)
                .setBlackoutPeriodStartDate("2022-01-14")
                .setBlackoutPeriodEndDate("2022-02-14")
                .setRingtone("clock://ringtone/1")
                .setShouldVibrate(true)
                .setPreviousInstance(alarmInstance1)
                .setNextInstance(alarmInstance2)
                .setOriginatingDevice(Alarm.ORIGINATING_DEVICE_SMART_WATCH)
                .build();

        assertThat(alarm.getNamespace()).isEqualTo("namespace");
        assertThat(alarm.getId()).isEqualTo("id");
        assertThat(alarm.getDocumentScore()).isEqualTo(1);
        assertThat(alarm.getDocumentTtlMillis()).isEqualTo(20000);
        assertThat(alarm.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(alarm.getName()).isEqualTo("my alarm");
        assertThat(alarm.getAlternateNames()).isNotNull();
        assertThat(alarm.getAlternateNames())
                .containsExactly("my alternate alarm", "my alternate alarm 2");
        assertThat(alarm.getDescription()).isEqualTo("this is my alarm");
        assertThat(alarm.getImage()).isEqualTo("content://images/alarm1");
        assertThat(alarm.getUrl()).isEqualTo("content://alarm/1");
        assertThat(alarm.isEnabled()).isTrue();
        assertThat(alarm.getDaysOfWeek()).asList().containsExactly(Calendar.MONDAY,
                Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY);
        assertThat(alarm.getHour()).isEqualTo(12);
        assertThat(alarm.getMinute()).isEqualTo(0);
        assertThat(alarm.getBlackoutPeriodStartDate()).isEqualTo("2022-01-14");
        assertThat(alarm.getBlackoutPeriodEndDate()).isEqualTo("2022-02-14");
        assertThat(alarm.getRingtone()).isEqualTo("clock://ringtone/1");
        assertThat(alarm.shouldVibrate()).isTrue();
        assertThat(alarm.getPreviousInstance()).isEqualTo(alarmInstance1);
        assertThat(alarm.getNextInstance()).isEqualTo(alarmInstance2);
        assertThat(alarm.getOriginatingDevice()).isEqualTo(Alarm.ORIGINATING_DEVICE_SMART_WATCH);
    }

    @Test
    public void testBuilderCopy_returnsAlarmWithAllFieldsCopied() {
        AlarmInstance alarmInstance1 = new AlarmInstance.Builder(
                "namespace", "instanceId1", "2022-01-01T00:00:00")
                .build();
        AlarmInstance alarmInstance2 = new AlarmInstance.Builder(
                "namespace", "instanceId2", "2022-01-02T00:00:00")
                .build();
        Alarm alarm1 = new Alarm.Builder("namespace", "id")
                .setDocumentScore(1)
                .setDocumentTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my alarm")
                .addAlternateName("my alternate alarm")
                .addAlternateName("my alternate alarm 2")
                .setDescription("this is my alarm")
                .setImage("content://images/alarm1")
                .setUrl("content://alarm/1")
                .setEnabled(true)
                .setDaysOfWeek(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY)
                .setHour(12)
                .setMinute(0)
                .setBlackoutPeriodStartDate("2022-01-14")
                .setBlackoutPeriodEndDate("2022-02-14")
                .setRingtone("clock://ringtone/1")
                .setShouldVibrate(true)
                .setPreviousInstance(alarmInstance1)
                .setNextInstance(alarmInstance2)
                .setOriginatingDevice(Alarm.ORIGINATING_DEVICE_SMART_WATCH)
                .build();

        Alarm alarm2 = new Alarm.Builder(alarm1).build();
        assertThat(alarm1.getNamespace()).isEqualTo(alarm2.getNamespace());
        assertThat(alarm1.getId()).isEqualTo(alarm2.getId());
        assertThat(alarm1.getDocumentScore()).isEqualTo(alarm2.getDocumentScore());
        assertThat(alarm1.getDocumentTtlMillis()).isEqualTo(alarm2.getDocumentTtlMillis());
        assertThat(alarm1.getCreationTimestampMillis())
                .isEqualTo(alarm2.getCreationTimestampMillis());
        assertThat(alarm1.getName()).isEqualTo(alarm2.getName());
        assertThat(alarm1.getAlternateNames())
                .containsExactlyElementsIn(alarm2.getAlternateNames());
        assertThat(alarm1.getDescription()).isEqualTo(alarm2.getDescription());
        assertThat(alarm1.getImage()).isEqualTo(alarm2.getImage());
        assertThat(alarm1.getUrl()).isEqualTo(alarm2.getUrl());
        assertThat(alarm1.isEnabled()).isEqualTo(alarm2.isEnabled());
        assertThat(alarm1.getDaysOfWeek()).isEqualTo(alarm2.getDaysOfWeek());
        assertThat(alarm1.getHour()).isEqualTo(alarm2.getHour());
        assertThat(alarm1.getMinute()).isEqualTo(alarm2.getMinute());
        assertThat(alarm1.getBlackoutPeriodStartDate())
                .isEqualTo(alarm2.getBlackoutPeriodStartDate());
        assertThat(alarm1.getBlackoutPeriodEndDate())
                .isEqualTo(alarm2.getBlackoutPeriodEndDate());
        assertThat(alarm1.getRingtone()).isEqualTo(alarm2.getRingtone());
        assertThat(alarm1.shouldVibrate()).isEqualTo(alarm2.shouldVibrate());
        assertThat(alarm1.getPreviousInstance()).isEqualTo(alarm2.getPreviousInstance());
        assertThat(alarm1.getNextInstance()).isEqualTo(alarm2.getNextInstance());
        assertThat(alarm1.getOriginatingDevice()).isEqualTo(alarm2.getOriginatingDevice());
    }

    @Test
    public void testToGenericDocument() throws Exception {
        AlarmInstance alarmInstance1 = new AlarmInstance.Builder(
                "namespace", "instanceId1", "2022-01-01T00:00:00")
                .setCreationTimestampMillis(100)
                .build();
        AlarmInstance alarmInstance2 = new AlarmInstance.Builder(
                "namespace", "instanceId2", "2022-01-02T00:00:00")
                .setCreationTimestampMillis(100)
                .build();
        Alarm alarm = new Alarm.Builder("namespace", "id")
                .setDocumentScore(1)
                .setDocumentTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my alarm")
                .addAlternateName("my alternate alarm")
                .addAlternateName("my alternate alarm 2")
                .setDescription("this is my alarm")
                .setImage("content://images/alarm1")
                .setUrl("content://alarm/1")
                .setEnabled(true)
                .setDaysOfWeek(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY)
                .setHour(12)
                .setMinute(0)
                .setBlackoutPeriodStartDate("2022-01-14")
                .setBlackoutPeriodEndDate("2022-02-14")
                .setRingtone("clock://ringtone/1")
                .setShouldVibrate(true)
                .setPreviousInstance(alarmInstance1)
                .setNextInstance(alarmInstance2)
                .setOriginatingDevice(Alarm.ORIGINATING_DEVICE_SMART_WATCH)
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(alarm);
        assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:Alarm");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getScore()).isEqualTo(1);
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(genericDocument.getTtlMillis()).isEqualTo(20000);
        assertThat(genericDocument.getPropertyString("name")).isEqualTo("my alarm");
        assertThat(genericDocument.getPropertyStringArray("alternateNames")).isNotNull();
        assertThat(Arrays.asList(genericDocument.getPropertyStringArray("alternateNames")))
                .containsExactly("my alternate alarm", "my alternate alarm 2");
        assertThat(genericDocument.getPropertyString("description"))
                .isEqualTo("this is my alarm");
        assertThat(genericDocument.getPropertyString("image"))
                .isEqualTo("content://images/alarm1");
        assertThat(genericDocument.getPropertyString("url"))
                .isEqualTo("content://alarm/1");
        assertThat(genericDocument.getPropertyBoolean("enabled")).isTrue();
        assertThat(genericDocument.getPropertyLongArray("daysOfWeek")).asList()
                .containsExactly(2L, 3L, 4L, 5L, 6L);
        assertThat(genericDocument.getPropertyLong("hour")).isEqualTo(12);
        assertThat(genericDocument.getPropertyLong("minute")).isEqualTo(0);
        assertThat(genericDocument.getPropertyString("blackoutPeriodStartDate"))
                .isEqualTo("2022-01-14");
        assertThat(genericDocument.getPropertyString("blackoutPeriodEndDate"))
                .isEqualTo("2022-02-14");
        assertThat(genericDocument.getPropertyString("ringtone")).isEqualTo("clock://ringtone/1");
        assertThat(genericDocument.getPropertyBoolean("shouldVibrate")).isTrue();
        assertThat(genericDocument.getPropertyDocument("previousInstance"))
                .isEqualTo(GenericDocument.fromDocumentClass(alarmInstance1));
        assertThat(genericDocument.getPropertyDocument("nextInstance"))
                .isEqualTo(GenericDocument.fromDocumentClass(alarmInstance2));
        assertThat(genericDocument.getPropertyLong("computingDevice"))
                .isEqualTo(Alarm.ORIGINATING_DEVICE_SMART_WATCH);

        // Test that toDocumentClass doesn't lose information.
        GenericDocument newGenericDocument = GenericDocument.fromDocumentClass(
                genericDocument.toDocumentClass(Alarm.class));
        assertThat(newGenericDocument).isEqualTo(genericDocument);
    }

    @Test
    public void testBuilder_invalidByDay_throwsError() {
        assertThrows(IllegalArgumentException.class, () -> new Alarm.Builder("namespace", "id")
                .setDaysOfWeek(Calendar.MONDAY, Calendar.SUNDAY, 8)
                .build());
    }

    @Test
    public void testBuilder_invalidHour_throwsError() {
        assertThrows(IllegalArgumentException.class, () -> new Alarm.Builder("namespace", "id")
                .setHour(24)
                .build());
    }

    @Test
    public void testBuilder_invalidMinute_throwsError() {
        assertThrows(IllegalArgumentException.class, () -> new Alarm.Builder("namespace", "id")
                .setMinute(60)
                .build());
    }

    @Test
    public void testAlarmWithNullDaysOfWeek_shouldReturnNullDaysOfWeek() throws Exception {
        Alarm alarm = new Alarm.Builder("namespace", "id")
                .build();
        GenericDocument alarmGenericDocument = GenericDocument.fromDocumentClass(alarm);

        assertThat(alarm.getDaysOfWeek()).isNull();
        assertThat(alarmGenericDocument.getPropertyLongArray("daysOfWeek")).isNull();

        // Test that toDocumentClass doesn't lose information.
        GenericDocument newGenericDocument = GenericDocument.fromDocumentClass(
                alarmGenericDocument.toDocumentClass(Alarm.class));
        assertThat(newGenericDocument).isEqualTo(alarmGenericDocument);
    }

    @Test
    public void testRenameComputingDevice_rename() throws Exception {
        GenericDocument genericAlarm =
                new GenericDocument.Builder<>("namespace1", "id1", "builtin:Alarm")
                        .setPropertyLong("computingDevice", 42)
                        .build();
        Alarm alarm = genericAlarm.toDocumentClass(Alarm.class);
        assertThat(alarm.getOriginatingDevice()).isEqualTo(42);
    }
}
