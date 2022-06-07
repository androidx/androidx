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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Convert for the {@link AlarmInstance} built-in type.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class AlarmInstanceConverter implements AppSearchDocumentConverter{
    private static final String TAG = "AlarmInstanceConverter";

    // Keys from the AppSearch document
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
    private static final String SCHEDULED_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    @NonNull
    @Override
    public Indexable.Builder convertGenericDocument(@NonNull Context context,
            @NonNull GenericDocument alarmInstance) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(alarmInstance);

        Indexable.Builder indexableBuilder =
                ConverterUtils.buildBaseIndexableFromGenericDocument(context,
                        ALARM_INSTANCE_INDEXABLE_TYPE, alarmInstance);

        // Convert the scheduled time into a timezone dependent format
        String scheduledTimeString = alarmInstance.getPropertyString(SCHEDULED_TIME_KEY);
        if (scheduledTimeString != null) {
            DateFormat format = new SimpleDateFormat(SCHEDULED_TIME_FORMAT, Locale.US);
            try {
                Date scheduledTime = format.parse(scheduledTimeString);
                indexableBuilder.put(SCHEDULED_TIME_KEY,
                        ConverterUtils.convertTimestampToISO8601Format(scheduledTime.getTime(),
                                null));
            } catch (ParseException e) {
                Log.w(TAG, "Failed to parse scheduledTime: " + scheduledTimeString);
            }
        }

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
