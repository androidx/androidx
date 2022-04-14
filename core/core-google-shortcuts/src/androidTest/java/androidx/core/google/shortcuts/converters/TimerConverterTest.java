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

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.AlarmClock;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.builtintypes.Timer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.firebase.appindexing.Indexable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.TimeZone;

@RunWith(AndroidJUnit4.class)
public class TimerConverterTest {
    @Mock private TimeModel mTimeModelMock;
    private TimerConverter mTimerConverter;
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTimerConverter = new TimerConverter(mTimeModelMock);
    }

    @Test
    @SmallTest
    public void testConvert_returnsIndexable() throws Exception {
        // Expire time is timezone sensitive. Force the default timezone to be GMT here.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        // mock 5 seconds passed since the timer startTime
        when(mTimeModelMock.getSystemCurrentTimeMillis()).thenReturn(8000L);
        when(mTimeModelMock.getSystemClockElapsedRealtime()).thenReturn(10000L);

        Timer timer = new Timer.Builder("namespace", "id")
                .setDocumentScore(10)
                .setDocumentTtlMillis(60000)
                .setCreationTimestampMillis(1)
                .setName("my timer")
                .setDurationMillis(1000)
                .setRemainingTimeMillisSinceUpdate(10000)
                .setRingtone(AlarmClock.VALUE_RINGTONE_SILENT)
                .setStatus(Timer.STATUS_STARTED)
                .setShouldVibrate(true)
                .setStartTimeMillis(
                        /*startTimeMillis=*/3000,
                        /*startTimeMillisInElapsedRealtime=*/5000,
                        /*bootCount=*/1)
                .setOriginalDurationMillis(800)
                .build();

        Indexable result = mTimerConverter.convertGenericDocument(
                mContext, GenericDocument.fromDocumentClass(timer))
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
                .put("message", "my timer")
                .put("length", 1000)
                .put("timerStatus", "Started")
                // Calculation: 3000 + 10000 = 13000 = 13 seconds since epoch
                .put("expireTime", "1970-01-01T00:00:13+0000")
                // Calculation: 8000 - (10000 - 5000) + 10000 = 13000 = 13 seconds since epoch
                .put("expireTimeCorrectedByStartTimeInElapsedRealtime",
                        "1970-01-01T00:00:13+0000")
                .put("remainingTime", 10000)
                .put("ringtone", "silent")
                .put("vibrate", true)
                .put("bootCount", 1)
                .put("originalDurationMillis", 800)
                .build();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @SmallTest
    public void testConvert_differentSystemCurrentTime_returnsIndexable() throws Exception {
        // Expire time is timezone sensitive. Force the default timezone to be GMT here.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        // 10 seconds has passed for System.CurrentTimeMillis
        when(mTimeModelMock.getSystemCurrentTimeMillis()).thenReturn(13000L);
        // 5 seconds has passed for SystemClock.elapsedRealtime
        when(mTimeModelMock.getSystemClockElapsedRealtime()).thenReturn(10000L);

        Timer timer = new Timer.Builder("namespace", "id")
                .setDocumentScore(10)
                .setDocumentTtlMillis(60000)
                .setCreationTimestampMillis(1)
                .setName("my timer")
                .setDurationMillis(1000)
                .setRemainingTimeMillisSinceUpdate(10000)
                .setRingtone(AlarmClock.VALUE_RINGTONE_SILENT)
                .setStatus(Timer.STATUS_STARTED)
                .setShouldVibrate(true)
                .setStartTimeMillis(
                        /*startTimeMillis=*/3000,
                        /*startTimeMillisInElapsedRealtime=*/5000,
                        /*bootCount=*/1)
                .setOriginalDurationMillis(800)
                .build();

        Indexable result = mTimerConverter.convertGenericDocument(
                mContext, GenericDocument.fromDocumentClass(timer))
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
                .put("message", "my timer")
                .put("length", 1000)
                .put("timerStatus", "Started")
                // Calculation: 3000 + 10000 = 13000 = 13 seconds since epoch
                .put("expireTime", "1970-01-01T00:00:13+0000")
                // Calculation: 13000 - (10000 - 5000) + 10000 = 18000 = 18 seconds since epoch
                // This is the more correct expire time
                .put("expireTimeCorrectedByStartTimeInElapsedRealtime",
                        "1970-01-01T00:00:18+0000")
                .put("remainingTime", 10000)
                .put("ringtone", "silent")
                .put("vibrate", true)
                .put("bootCount", 1)
                .put("originalDurationMillis", 800)
                .build();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @SmallTest
    public void testConvert_withoutOptionalFields_returnsIndexable() throws Exception {
        Timer timer = new Timer.Builder("namespace", "id")
                // need to override to a value, otherwise it will use current time
                .setCreationTimestampMillis(0)
                .build();

        Indexable result = mTimerConverter.convertGenericDocument(mContext,
                GenericDocument.fromDocumentClass(timer))
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
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS, 0)
                .put("length", 0)
                .put("timerStatus", "Unknown")
                .put("remainingTime", 0)
                .put("vibrate", false)
                .put("bootCount", 0)
                .put("originalDurationMillis", 0)
                .build();
        assertThat(result).isEqualTo(expectedResult);
    }
}
