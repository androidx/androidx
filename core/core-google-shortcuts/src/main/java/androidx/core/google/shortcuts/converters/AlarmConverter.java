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
import androidx.core.google.shortcuts.utils.ConverterUtils;
import androidx.core.util.Preconditions;

import com.google.firebase.appindexing.FirebaseAppIndexingInvalidArgumentException;
import com.google.firebase.appindexing.Indexable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Convert for the {@link androidx.appsearch.builtintypes.Alarm} built-in type.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class AlarmConverter implements AppSearchDocumentConverter {
    private static final String TAG = "AlarmConverter";

    // Keys from the AppSearch document
    private static final String NAME_KEY = "name";
    private static final String ENABLED_KEY = "enabled";
    private static final String DAYS_OF_WEEK_KEY = "daysOfWeek";
    private static final String HOUR_KEY = "hour";
    private static final String MINUTE_KEY = "minute";
    private static final String RINGTONE_KEY = "ringtone";
    private static final String SHOULD_VIBRATE_KEY = "shouldVibrate";
    private static final String BLACKOUT_PERIOD_START_DATE_KEY = "blackoutPeriodStartDate";
    private static final String BLACKOUT_PERIOD_END_DATE_KEY = "blackoutPeriodEndDate";
    private static final String PREVIOUS_INSTANCE_KEY = "previousInstance";
    private static final String NEXT_INSTANCE_KEY = "nextInstance";

    // Keys for Indexables
    private static final String IDENTIFIER_KEY = "identifier";
    private static final String VIBRATE_KEY = "vibrate";
    private static final String MESSAGE_KEY = "message";
    private static final String DAY_OF_WEEK_KEY = "dayOfWeek";
    private static final String ALARM_INSTANCES_KEY = "alarmInstances";

    // Enums for DayOfWeek
    private static final String MONDAY = "Monday";
    private static final String TUESDAY = "Tuesday";
    private static final String WEDNESDAY = "Wednesday";
    private static final String THURSDAY = "Thursday";
    private static final String FRIDAY = "Friday";
    private static final String SATURDAY = "Saturday";
    private static final String SUNDAY = "Sunday";

    private static final String ALARM_INDEXABLE_TYPE = "Alarm";

    private final AlarmInstanceConverter mAlarmInstanceConverter;

    public AlarmConverter() {
        mAlarmInstanceConverter = new AlarmInstanceConverter();
    }

    @NonNull
    @Override
    public Indexable.Builder convertGenericDocument(@NonNull Context context,
            @NonNull GenericDocument alarm) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(alarm);

        Indexable.Builder indexableBuilder =
                ConverterUtils.buildBaseIndexableFromGenericDocument(context,
                        ALARM_INDEXABLE_TYPE, alarm);

        indexableBuilder
                .put(MESSAGE_KEY, alarm.getPropertyString(NAME_KEY))
                .put(HOUR_KEY, alarm.getPropertyLong(HOUR_KEY))
                .put(MINUTE_KEY, alarm.getPropertyLong(MINUTE_KEY))
                .put(RINGTONE_KEY, alarm.getPropertyString(RINGTONE_KEY))
                .put(VIBRATE_KEY, alarm.getPropertyBoolean(SHOULD_VIBRATE_KEY))
                .put(ENABLED_KEY, alarm.getPropertyBoolean(ENABLED_KEY))
                .put(IDENTIFIER_KEY, alarm.getId())
                .put(BLACKOUT_PERIOD_START_DATE_KEY,
                        alarm.getPropertyString(BLACKOUT_PERIOD_START_DATE_KEY))
                .put(BLACKOUT_PERIOD_END_DATE_KEY,
                        alarm.getPropertyString(BLACKOUT_PERIOD_END_DATE_KEY));

        long[] daysOfWeek = alarm.getPropertyLongArray(DAYS_OF_WEEK_KEY);
        if (daysOfWeek != null) {
            String[] daysOfWeekString = new String[daysOfWeek.length];
            for (int i = 0; i < daysOfWeek.length; i++) {
                int day = (int) daysOfWeek[i];
                switch (day) {
                    case Calendar.MONDAY:
                        daysOfWeekString[i] = MONDAY;
                        break;
                    case Calendar.TUESDAY:
                        daysOfWeekString[i] = TUESDAY;
                        break;
                    case Calendar.WEDNESDAY:
                        daysOfWeekString[i] = WEDNESDAY;
                        break;
                    case Calendar.THURSDAY:
                        daysOfWeekString[i] = THURSDAY;
                        break;
                    case Calendar.FRIDAY:
                        daysOfWeekString[i] = FRIDAY;
                        break;
                    case Calendar.SATURDAY:
                        daysOfWeekString[i] = SATURDAY;
                        break;
                    case Calendar.SUNDAY:
                        daysOfWeekString[i] = SUNDAY;
                        break;
                    default:
                        Log.w(TAG, "Invalid DaysOfWeek: " + day + ", ignoring.");
                }
            }
            indexableBuilder.put(DAY_OF_WEEK_KEY, daysOfWeekString);
        }

        GenericDocument previousInstance = alarm.getPropertyDocument(PREVIOUS_INSTANCE_KEY);
        GenericDocument nextInstance = alarm.getPropertyDocument(NEXT_INSTANCE_KEY);
        List<Indexable> alarmInstances = new ArrayList<>();
        if (previousInstance != null) {
            alarmInstances.add(mAlarmInstanceConverter
                    .convertGenericDocument(context, previousInstance).build());
        }
        if (nextInstance != null) {
            alarmInstances.add(mAlarmInstanceConverter
                    .convertGenericDocument(context, nextInstance).build());
        }
        if (!alarmInstances.isEmpty()) {
            try {
                indexableBuilder.put(ALARM_INSTANCES_KEY, alarmInstances.toArray(new Indexable[0]));
            } catch (FirebaseAppIndexingInvalidArgumentException e) {
                Log.e(TAG, "Failed to add AlarmInstances to Alarm.", e);
            }
        }

        return indexableBuilder;
    }
}
