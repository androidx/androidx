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
import androidx.appsearch.builtintypes.AlarmInstance;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.firebase.appindexing.Indexable;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.TimeZone;

@RunWith(AndroidJUnit4.class)
public class AlarmInstanceConverterTest {
    private final AlarmInstanceConverter mAlarmInstanceConverter = new AlarmInstanceConverter();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    @SmallTest
    public void testConvert_returnsIndexable() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        AlarmInstance alarmInstance = new AlarmInstance.Builder(
                "namespace", "id", "2020-01-02T07:30:00")
                .setCreationTimestampMillis(1000)
                .setDocumentTtlMillis(2000)
                .setDocumentScore(1)
                .setStatus(AlarmInstance.STATUS_SCHEDULED)
                .setSnoozeDurationMillis(3000)
                .build();

        Indexable result = mAlarmInstanceConverter.convertGenericDocument(mContext,
                GenericDocument.fromDocumentClass(alarmInstance)).build();
        Indexable expectedResult = new Indexable.Builder("AlarmInstance")
                .setMetadata(new Indexable.Metadata.Builder().setScore(1))
                .setId("id")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=id;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 2000)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1000)
                .put("scheduledTime", "2020-01-02T07:30:00+0000")
                .put("alarmStatus", "Scheduled")
                .put("snoozeLength", 3000)
                .build();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @SmallTest
    public void testConvert_differentTimeZone_returnsIndexableWithSameLocalTime() throws Exception {
        AlarmInstance alarmInstance = new AlarmInstance.Builder(
                "namespace", "id", "2020-01-02T07:30:00")
                .setCreationTimestampMillis(1000)
                .setStatus(AlarmInstance.STATUS_SCHEDULED)
                .build();
        Indexable.Builder expectedResultTemplate = new Indexable.Builder("AlarmInstance")
                .setMetadata(new Indexable.Metadata.Builder().setScore(0))
                .setId("id")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=id;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 0)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1000)
                .put("alarmStatus", "Scheduled")
                .put("snoozeLength", -1);

        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Indexable gmtResult = mAlarmInstanceConverter.convertGenericDocument(mContext,
                GenericDocument.fromDocumentClass(alarmInstance)).build();
        assertThat(gmtResult).isEqualTo(expectedResultTemplate
                .put("scheduledTime", "2020-01-02T07:30:00+0000").build());

        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        Indexable pstResult = mAlarmInstanceConverter.convertGenericDocument(mContext,
                GenericDocument.fromDocumentClass(alarmInstance)).build();
        assertThat(pstResult).isEqualTo(expectedResultTemplate
                .put("scheduledTime", "2020-01-02T07:30:00-0800").build());
    }

    @Test
    @SmallTest
    public void testConvert_withoutOptionalFields_returnsIndexable() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        AlarmInstance alarmInstance = new AlarmInstance.Builder(
                "namespace", "id", "2020-01-02T07:30:00")
                // CurrentTime will be used if not set.
                .setCreationTimestampMillis(1000)
                .build();

        Indexable result = mAlarmInstanceConverter.convertGenericDocument(mContext,
                GenericDocument.fromDocumentClass(alarmInstance)).build();
        Indexable expectedResult = new Indexable.Builder("AlarmInstance")
                .setMetadata(new Indexable.Metadata.Builder().setScore(0))
                .setId("id")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=id;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 0)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1000)
                .put("alarmStatus", "Unknown")
                .put("snoozeLength", -1)
                .put("scheduledTime", "2020-01-02T07:30:00+0000")
                .build();
        assertThat(result).isEqualTo(expectedResult);
    }
}
