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
import android.provider.AlarmClock;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.builtintypes.Timer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.firebase.appindexing.Indexable;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.TimeZone;

@RunWith(AndroidJUnit4.class)
public class TimerConverterTest {
    private final TimerConverter mTimerConverter = new TimerConverter();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    @SmallTest
    public void testConvert_returnsIndexable() throws Exception {
        // Expire time is timezone sensitive. Force the default timezone to be GMT here.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        Timer timer = new Timer.Builder("namespace", "id")
                .setDurationMillis(1000)
                .setTimerStatus(Timer.STATUS_STARTED)
                .setExpireTimeMillis(3000)
                .setName("my timer")
                .setRemainingTimeMillis(500)
                .setRingtone(AlarmClock.VALUE_RINGTONE_SILENT)
                .setScore(10)
                .setTtlMillis(60000)
                .setVibrate(true)
                .build();

        Indexable result = mTimerConverter.convertGenericDocument(mContext,
                GenericDocument.fromDocumentClass(timer))
                // Override creation timestamp to a constant instead of System.currentTimeMillis.
                // TODO: add creation timestamp to timer.
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1)
                .build();
        Indexable expectedResult = new Indexable.Builder("Timer")
                .setMetadata(new Indexable.Metadata.Builder().setScore(10))
                .setId("id")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=id;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 60000)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1)
                .put("length", 1000)
                .put("timerStatus", "Started")
                .put("expireTime", "1970-01-01T00:00:03+0000")
                .put("message", "my timer")
                .put("remainingTime", 500)
                .put("ringtone", "silent")
                .put("vibrate", true)
                .build();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @SmallTest
    public void testConvert_withoutOptionalFields_returnsIndexable() throws Exception {
        Timer timer = new Timer.Builder("namespace", "id")
                .build();

        Indexable result = mTimerConverter.convertGenericDocument(mContext,
                GenericDocument.fromDocumentClass(timer))
                // Override creation timestamp to a constant instead of System.currentTimeMillis.
                // TODO: add creation timestamp to timer.
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1)
                .build();
        Indexable expectedResult = new Indexable.Builder("Timer")
                .setMetadata(new Indexable.Metadata.Builder().setScore(0))
                .setId("id")
                .setName("namespace")
                .setUrl("intent:#Intent;action=androidx.core.content.pm.SHORTCUT_LISTENER;"
                        + "component=androidx.core.google.shortcuts.test/androidx.core.google."
                        + "shortcuts.TrampolineActivity;S.id=id;end")
                .put(IndexableKeys.NAMESPACE, "namespace")
                .put(IndexableKeys.TTL_MILLIS, 0)
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 1)
                .put("length", 0)
                .put("timerStatus", "Unknown")
                .put("remainingTime", 0)
                .put("vibrate", false)
                .build();
        assertThat(result).isEqualTo(expectedResult);
    }
}
