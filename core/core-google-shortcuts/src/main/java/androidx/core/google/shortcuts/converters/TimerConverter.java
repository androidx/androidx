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
import androidx.appsearch.builtintypes.Timer;
import androidx.core.google.shortcuts.utils.ConverterUtils;
import androidx.core.util.Preconditions;

import com.google.firebase.appindexing.Indexable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Convert for the {@link Timer} built-in type.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class TimerConverter implements AppSearchDocumentConverter {
    private static final String TAG = "TimerConverter";

    // Keys from the AppSearch document
    private static final String NAME_KEY = "name";
    private static final String DURATION_MILLIS_KEY = "durationMillis";
    private static final String REMAINING_TIME_MILLIS_KEY = "remainingTimeMillis";
    private static final String RINGTONE_KEY = "ringtone";
    private static final String VIBRATE_KEY = "vibrate";
    private static final String TIMER_STATUS_KEY = "timerStatus";
    private static final String EXPIRE_TIME_MILLIS_KEY = "expireTimeMillis";

    // Keys for Indexables
    private static final String MESSAGE_KEY = "message";
    private static final String LENGTH_KEY = "length";
    private static final String REMAINING_TIME_KEY = "remainingTime";
    private static final String EXPIRE_TIME_KEY = "expireTime";

    // Enums for TimerStatus
    private static final String STARTED = "Started";
    private static final String PAUSED = "Paused";
    private static final String EXPIRED = "Expired";
    private static final String MISSED = "Missed";
    private static final String RESET = "Reset";
    private static final String UNKNOWN = "Unknown";

    private static final ThreadLocal<DateFormat> ISO8601_DATE_TIME_FORMAT =
            new ThreadLocal<DateFormat>() {
        @Override
        public DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        }
    };

    @Override
    @NonNull
    public Indexable.Builder convertGenericDocument(@NonNull Context context,
            @NonNull GenericDocument timer) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(timer);

        Indexable.Builder indexableBuilder = ConverterUtils.buildBaseIndexableFromGenericDocument(
                    context, timer);

        indexableBuilder
                .put(MESSAGE_KEY, timer.getPropertyString(NAME_KEY))
                .put(LENGTH_KEY, timer.getPropertyLong(DURATION_MILLIS_KEY))
                .put(REMAINING_TIME_KEY, timer.getPropertyLong(REMAINING_TIME_MILLIS_KEY))
                .put(RINGTONE_KEY, timer.getPropertyString(RINGTONE_KEY))
                .put(VIBRATE_KEY, timer.getPropertyBoolean(VIBRATE_KEY));

        int timerStatus = (int) timer.getPropertyLong(TIMER_STATUS_KEY);
        switch (timerStatus) {
            case Timer.STATUS_UNKNOWN:
                indexableBuilder.put(TIMER_STATUS_KEY, UNKNOWN);
                break;
            case Timer.STATUS_STARTED:
                indexableBuilder.put(TIMER_STATUS_KEY, STARTED);
                break;
            case Timer.STATUS_PAUSED:
                indexableBuilder.put(TIMER_STATUS_KEY, PAUSED);
                break;
            case Timer.STATUS_EXPIRED:
                indexableBuilder.put(TIMER_STATUS_KEY, EXPIRED);
                break;
            case Timer.STATUS_MISSED:
                indexableBuilder.put(TIMER_STATUS_KEY, MISSED);
                break;
            case Timer.STATUS_RESET:
                indexableBuilder.put(TIMER_STATUS_KEY, RESET);
                break;
            default:
                indexableBuilder.put(TIMER_STATUS_KEY, UNKNOWN);
                Log.w(TAG, "Invalud time status: " + timerStatus + ", defaulting to "
                        + "Timer.STATUS_UNKNOWN");
        }

        // 0 means never expire.
        long expireTime = timer.getPropertyLong(EXPIRE_TIME_MILLIS_KEY);
        if (expireTime > 0) {
            Date date = new Date(expireTime);
            indexableBuilder.put(EXPIRE_TIME_KEY,
                    Preconditions.checkNotNull(ISO8601_DATE_TIME_FORMAT.get()).format(date));
        }
        return indexableBuilder;
    }
}
