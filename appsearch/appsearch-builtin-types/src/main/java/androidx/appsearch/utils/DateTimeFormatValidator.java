/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class used to validate date time formats.
 *
 * @exportToFramework:hide
 */
@RestrictTo(LIBRARY)
public final class DateTimeFormatValidator {
    private DateTimeFormatValidator() {}

    /**
     * Returns true if the date string matches yyyy-MM-dd
     */
    public static boolean validateISO8601Date(@NonNull String dateString) {
        return validateDateFormat("yyyy-MM-dd", dateString);
    }

    /**
     * Returns true if the date string matches yyyy-MM-ddTHH:mm:ss
     */
    public static boolean validateISO8601DateTime(@NonNull String dateString) {
        return validateDateFormat("yyyy-MM-dd'T'HH:mm", dateString)
                || validateDateFormat("yyyy-MM-dd'T'HH:mm:ss", dateString);
    }

    /**
     * Returns true if the date string matches the provided format exactly.
     */
    public static boolean validateDateFormat(@NonNull String format, @NonNull String dateString) {
        // ISO 8601 DateTime format must be represented using arabic numerals (0-9). en-US is
        // one of many locales that uses arabic numerals, therefore it is used during formatting.
        // Even if the user's device is not in the en-US locale, this will still work since ISO
        // 8601 is an international standard, and does not change based on locales.
        DateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
        dateFormat.setLenient(false);
        try {
            Date date = dateFormat.parse(dateString);
            // ensure exact match
            if (date == null || !dateString.equals(dateFormat.format(date))) {
                return false;
            }
        } catch (ParseException e) {
            return false;
        }

        return true;
    }
}
