/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.provider;

import android.content.IntentFilter;
import android.content.UriMatcher;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The contract between the wearable calendar provider and applications. This API provides a subset
 * of the data available through {@link CalendarContract}, but is automatically synced to wearable
 * devices.
 */
public class WearableCalendarContract {
    private static final String AUTHORITY = "com.google.android.wearable.provider.calendar";

    /**
     * Adds uri to match to the given {@link UriMatcher} with calendar authority, {@link String}
     * path and the integer code to return when this Uri is matched.
     * @param uriMatcher    The UriMatcher holding matches.
     * @param path          The path to match in calendar authority. * may be used as a wild card
     *                      for any text, and # may be used as a wild card for numbers.
     * @param code          The code that is returned when a Uri is matched against the calendar
     *                      authority and path. Must be positive.
     */
    public static void addCalendarAuthorityUri(@NonNull UriMatcher uriMatcher, @NonNull String path,
            int code) {
        uriMatcher.addURI(AUTHORITY, path, code);
    }

    /**
     * Adds new {@link android.content.Intent} to the given {@link IntentFilter} with calendar data
     * authority and {@link String} host to match.
     * @param intentFilter  The IntentFilter holding matches.
     * @param port          Optional port part of the authority to match. If null, any port is
     *                      allowed.
     */
    public static void addCalendarDataAuthority(
            @NonNull IntentFilter intentFilter, @Nullable String port) {
        intentFilter.addDataAuthority(AUTHORITY, port);
    }

    /** The content:// style URL for the top-level wearable calendar authority. */
    @NonNull
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /** @see android.provider.CalendarContract.Instances */
    public static final class Instances {
        private Instances() {
        }

        @NonNull
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(WearableCalendarContract.CONTENT_URI, "instances/when");
    }

    /** @see android.provider.CalendarContract.Attendees */
    public static final class Attendees {
        private Attendees() {
        }

        @NonNull
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(WearableCalendarContract.CONTENT_URI, "attendees");
    }

    /** @see android.provider.CalendarContract.Reminders */
    public static final class Reminders {
        private Reminders() {
        }

        @NonNull
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(WearableCalendarContract.CONTENT_URI, "reminders");
    }

    private WearableCalendarContract() {}
}
