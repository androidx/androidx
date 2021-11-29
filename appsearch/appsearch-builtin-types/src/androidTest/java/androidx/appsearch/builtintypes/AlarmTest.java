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

import java.util.Calendar;

public class AlarmTest {
    @Test
    public void testBuilder() {
        AlarmInstance alarmInstance1 = new AlarmInstance.Builder("namespace", "instanceId1")
                .build();
        AlarmInstance alarmInstance2 = new AlarmInstance.Builder("namespace", "instanceId2")
                .build();
        Alarm alarm = new Alarm.Builder("namespace", "id")
                .setScore(1)
                .setTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my alarm")
                .setEnabled(true)
                .setDaysOfWeek(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY)
                .setHour(12)
                .setMinute(0)
                .setBlackoutStartTimeMillis(1000)
                .setBlackoutEndTimeMillis(2000)
                .setRingtone("clock://ringtone/1")
                .setVibrate(true)
                .setPreviousInstance(alarmInstance1)
                .setNextInstance(alarmInstance2)
                .build();

        assertThat(alarm.getNamespace()).isEqualTo("namespace");
        assertThat(alarm.getId()).isEqualTo("id");
        assertThat(alarm.getScore()).isEqualTo(1);
        assertThat(alarm.getTtlMillis()).isEqualTo(20000);
        assertThat(alarm.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(alarm.getName()).isEqualTo("my alarm");
        assertThat(alarm.isEnabled()).isTrue();
        assertThat(alarm.getDaysOfWeek()).asList().containsExactly(Calendar.MONDAY,
                Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY);
        assertThat(alarm.getHour()).isEqualTo(12);
        assertThat(alarm.getMinute()).isEqualTo(0);
        assertThat(alarm.getBlackoutStartTimeMillis()).isEqualTo(1000);
        assertThat(alarm.getBlackoutEndTimeMillis()).isEqualTo(2000);
        assertThat(alarm.getRingtone()).isEqualTo("clock://ringtone/1");
        assertThat(alarm.isVibrate()).isTrue();
        assertThat(alarm.getPreviousInstance()).isEqualTo(alarmInstance1);
        assertThat(alarm.getNextInstance()).isEqualTo(alarmInstance2);
    }

    @Test
    public void testBuilderCopy_returnsAlarmWithAllFieldsCopied() {
        AlarmInstance alarmInstance1 = new AlarmInstance.Builder("namespace", "instanceId1")
                .build();
        AlarmInstance alarmInstance2 = new AlarmInstance.Builder("namespace", "instanceId2")
                .build();
        Alarm alarm1 = new Alarm.Builder("namespace", "id")
                .setScore(1)
                .setTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my alarm")
                .setEnabled(true)
                .setDaysOfWeek(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY)
                .setHour(12)
                .setMinute(0)
                .setBlackoutStartTimeMillis(1000)
                .setBlackoutEndTimeMillis(2000)
                .setRingtone("clock://ringtone/1")
                .setVibrate(true)
                .setPreviousInstance(alarmInstance1)
                .setNextInstance(alarmInstance2)
                .build();

        Alarm alarm2 = new Alarm.Builder(alarm1).build();
        assertThat(alarm1.getNamespace()).isEqualTo(alarm2.getNamespace());
        assertThat(alarm1.getId()).isEqualTo(alarm2.getId());
        assertThat(alarm1.getScore()).isEqualTo(alarm2.getScore());
        assertThat(alarm1.getTtlMillis()).isEqualTo(alarm2.getTtlMillis());
        assertThat(alarm1.getCreationTimestampMillis())
                .isEqualTo(alarm2.getCreationTimestampMillis());
        assertThat(alarm1.getName()).isEqualTo(alarm2.getName());
        assertThat(alarm1.isEnabled()).isEqualTo(alarm2.isEnabled());
        assertThat(alarm1.getDaysOfWeek()).isEqualTo(alarm2.getDaysOfWeek());
        assertThat(alarm1.getHour()).isEqualTo(alarm2.getHour());
        assertThat(alarm1.getMinute()).isEqualTo(alarm2.getMinute());
        assertThat(alarm1.getBlackoutStartTimeMillis())
                .isEqualTo(alarm2.getBlackoutStartTimeMillis());
        assertThat(alarm1.getBlackoutEndTimeMillis()).isEqualTo(alarm2.getBlackoutEndTimeMillis());
        assertThat(alarm1.getRingtone()).isEqualTo(alarm2.getRingtone());
        assertThat(alarm1.isVibrate()).isEqualTo(alarm2.isVibrate());
        assertThat(alarm1.getPreviousInstance()).isEqualTo(alarm2.getPreviousInstance());
        assertThat(alarm1.getNextInstance()).isEqualTo(alarm2.getNextInstance());
    }

    @Test
    public void testToGenericDocument() throws Exception {
        AlarmInstance alarmInstance1 = new AlarmInstance.Builder("namespace", "instanceId1")
                .setCreationTimestampMillis(100)
                .build();
        AlarmInstance alarmInstance2 = new AlarmInstance.Builder("namespace", "instanceId2")
                .setCreationTimestampMillis(100)
                .build();
        Alarm alarm = new Alarm.Builder("namespace", "id")
                .setScore(1)
                .setTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my alarm")
                .setEnabled(true)
                .setDaysOfWeek(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY)
                .setHour(12)
                .setMinute(0)
                .setBlackoutStartTimeMillis(1000)
                .setBlackoutEndTimeMillis(2000)
                .setRingtone("clock://ringtone/1")
                .setVibrate(true)
                .setPreviousInstance(alarmInstance1)
                .setNextInstance(alarmInstance2)
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(alarm);
        assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:Alarm");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getScore()).isEqualTo(1);
        assertThat(genericDocument.getTtlMillis()).isEqualTo(20000);
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(genericDocument.getPropertyString("name")).isEqualTo("my alarm");
        assertThat(genericDocument.getPropertyBoolean("enabled")).isTrue();
        assertThat(genericDocument.getPropertyLongArray("daysOfWeek")).asList()
                .containsExactly(2L, 3L, 4L, 5L, 6L);
        assertThat(genericDocument.getPropertyLong("hour")).isEqualTo(12);
        assertThat(genericDocument.getPropertyLong("minute")).isEqualTo(0);
        assertThat(genericDocument.getPropertyLong("blackoutStartTimeMillis")).isEqualTo(1000);
        assertThat(genericDocument.getPropertyLong("blackoutEndTimeMillis")).isEqualTo(2000);
        assertThat(genericDocument.getPropertyString("ringtone")).isEqualTo("clock://ringtone/1");
        assertThat(genericDocument.getPropertyBoolean("vibrate")).isTrue();
        assertThat(genericDocument.getPropertyDocument("previousInstance"))
                .isEqualTo(GenericDocument.fromDocumentClass(alarmInstance1));
        assertThat(genericDocument.getPropertyDocument("nextInstance"))
                .isEqualTo(GenericDocument.fromDocumentClass(alarmInstance2));
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
    }
}
