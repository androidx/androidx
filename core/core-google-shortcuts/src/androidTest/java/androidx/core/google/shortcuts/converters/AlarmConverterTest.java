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
import androidx.appsearch.builtintypes.Alarm;
import androidx.appsearch.builtintypes.AlarmInstance;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.firebase.appindexing.Indexable;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.TimeZone;

@RunWith(AndroidJUnit4.class)
public class AlarmConverterTest {
    private final AlarmConverter mAlarmConverter = new AlarmConverter();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    @SmallTest
    public void testConvert_returnsIndexable() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        AlarmInstance previousInstance = new AlarmInstance.Builder(
                "namespace", "instance1", "2020-01-01T00:00:00")
                .setCreationTimestampMillis(1000)
                .setStatus(AlarmInstance.STATUS_DISMISSED)
                .build();
        AlarmInstance nextInstance = new AlarmInstance.Builder(
                "namespace", "instance2", "2020-01-02T00:00:00")
                .setCreationTimestampMillis(1000)
                .setStatus(AlarmInstance.STATUS_SCHEDULED)
                .build();
        Alarm alarm = new Alarm.Builder("namespace", "id")
                .setCreationTimestampMillis(1000)
                .setDocumentTtlMillis(2000)
                .setDocumentScore(1)
                .setName("alarm")
                .setEnabled(true)
                .setDaysOfWeek(Calendar.MONDAY, Calendar.TUESDAY)
                .setHour(7)
                .setMinute(30)
                .setBlackoutPeriodStartDate("2020-02-01")
                .setBlackoutPeriodEndDate("2020-03-01")
                .setRingtone("clock://ringtone/1")
                .setShouldVibrate(true)
                .setPreviousInstance(previousInstance)
                .setNextInstance(nextInstance)
                .build();

        Indexable result = mAlarmConverter.convertGenericDocument(mContext,
                GenericDocument.fromDocumentClass(alarm)).build();
        Indexable expectedInstance1 = new Indexable.Builder("AlarmInstance")
                .setMetadata(new Indexable.Metadata.Builder().setScore(0))
                .setId("instance1")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=instance1;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 0)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1000)
                .put("scheduledTime", "2020-01-01T00:00:00+0000")
                .put("alarmStatus", "Dismissed")
                .put("snoozeLength", -1)
                .build();
        Indexable expectedInstance2 = new Indexable.Builder("AlarmInstance")
                .setMetadata(new Indexable.Metadata.Builder().setScore(0))
                .setId("instance2")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=instance2;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 0)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1000)
                .put("scheduledTime", "2020-01-02T00:00:00+0000")
                .put("alarmStatus", "Scheduled")
                .put("snoozeLength", -1)
                .build();
        Indexable expectedResult = new Indexable.Builder("Alarm")
                .setMetadata(new Indexable.Metadata.Builder().setScore(1))
                .setId("id")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=id;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 2000)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1000)
                .put("message", "alarm")
                .put("hour", 7)
                .put("minute", 30)
                .put("ringtone", "clock://ringtone/1")
                .put("vibrate", true)
                .put("enabled", true)
                .put("identifier", "id")
                .put("blackoutPeriodStartDate", "2020-02-01")
                .put("blackoutPeriodEndDate", "2020-03-01")
                .put("dayOfWeek", "Monday", "Tuesday")
                .put("alarmInstances", expectedInstance1, expectedInstance2)
                .build();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @SmallTest
    public void testConvert_withoutOptionalFields_returnsIndexable() throws Exception {
        Alarm alarm = new Alarm.Builder("namespace", "id")
                // CurrentTime will be used if not set.
                .setCreationTimestampMillis(1000)
                .build();

        Indexable result = mAlarmConverter.convertGenericDocument(mContext,
                GenericDocument.fromDocumentClass(alarm)).build();
        Indexable expectedResult = new Indexable.Builder("Alarm")
                .setMetadata(new Indexable.Metadata.Builder().setScore(0))
                .setId("id")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=id;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 0)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1000)
                .put("hour", 0)
                .put("minute", 0)
                .put("vibrate", false)
                .put("enabled", false)
                .put("identifier", "id")
                .build();
        assertThat(result).isEqualTo(expectedResult);
    }
}
