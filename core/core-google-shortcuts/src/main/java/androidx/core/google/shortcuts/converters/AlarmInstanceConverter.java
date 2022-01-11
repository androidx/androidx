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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.builtintypes.AlarmInstance;
import androidx.core.google.shortcuts.utils.ConverterUtils;
import androidx.core.util.Preconditions;

import com.google.firebase.appindexing.Indexable;

import java.util.Calendar;

/**
 * Convert for the {@link AlarmInstance} built-in type.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class AlarmInstanceConverter implements AppSearchDocumentConverter{
    private static final String TAG = "AlarmInstanceConverter";

    // Keys from the AppSearch document
    private static final String YEAR_KEY = "year";
    private static final String MONTH_KEY = "month";
    private static final String DAY_KEY = "day";
    private static final String HOUR_KEY = "hour";
    private static final String MINUTE_KEY = "minute";
    private static final String STATUS_KEY = "status";
    private static final String SNOOZE_DURATION_MILLIS_KEY = "snoozeDurationMillis";

    // Keys for Indexables
    private static final String SCHEDULED_TIME_KEY = "scheduledTime";
    private static final String ALARM_STATUS_KEY = "alarmStatus";
    private static final String SNOOZE_LENGTH_KEY = "snoozeLength";

    // Enums for AlarmStatus
    private static final String SCHEDULED = "Scheduled";
    private static final String FIRED = "Fired";
    private static final String DISMISSED = "Dismissed";
    private static final String SNOOZED = "Snoozed";
    private static final String MISSED = "Missed";
    private static final String UNKNOWN = "Unknown";

    private static final String ALARM_INSTANCE_INDEXABLE_TYPE = "AlarmInstance";

    @NonNull
    @Override
    public Indexable.Builder convertGenericDocument(@NonNull Context context,
            @NonNull GenericDocument alarmInstance) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(alarmInstance);

        Indexable.Builder indexableBuilder =
                ConverterUtils.buildBaseIndexableFromGenericDocument(context,
                        ALARM_INSTANCE_INDEXABLE_TYPE, alarmInstance);

        int year = (int) alarmInstance.getPropertyLong(YEAR_KEY);
        int month = (int) alarmInstance.getPropertyLong(MONTH_KEY);
        int day = (int) alarmInstance.getPropertyLong(DAY_KEY);
        int hour = (int) alarmInstance.getPropertyLong(HOUR_KEY);
        int minute = (int) alarmInstance.getPropertyLong(MINUTE_KEY);
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        indexableBuilder.put(SCHEDULED_TIME_KEY, ConverterUtils.convertTimestampToISO8601Format(
                calendar.getTimeInMillis(), calendar.getTimeZone()));

        int status = (int) alarmInstance.getPropertyLong(STATUS_KEY);
        switch (status) {
            case AlarmInstance.STATUS_SCHEDULED:
                indexableBuilder.put(ALARM_STATUS_KEY, SCHEDULED);
                break;
            case AlarmInstance.STATUS_FIRING:
                indexableBuilder.put(ALARM_STATUS_KEY, FIRED);
                break;
            case AlarmInstance.STATUS_DISMISSED:
                indexableBuilder.put(ALARM_STATUS_KEY, DISMISSED);
                break;
            case AlarmInstance.STATUS_SNOOZED:
                indexableBuilder.put(ALARM_STATUS_KEY, SNOOZED);
                break;
            case AlarmInstance.STATUS_MISSED:
                indexableBuilder.put(ALARM_STATUS_KEY, MISSED);
                break;
            case AlarmInstance.STATUS_UNKNOWN:
                indexableBuilder.put(ALARM_STATUS_KEY, UNKNOWN);
                break;
            default:
                indexableBuilder.put(ALARM_STATUS_KEY, UNKNOWN);
                Log.w(TAG, "Invalid alarm instance status: " + status + ", defaulting to "
                        + "AlarmInstance.STATUS_UNKNOWN");
        }

        indexableBuilder.put(SNOOZE_LENGTH_KEY,
                alarmInstance.getPropertyLong(SNOOZE_DURATION_MILLIS_KEY));

        return indexableBuilder;
    }
}
